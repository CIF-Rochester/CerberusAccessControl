package edu.rochester.cif.cerberus;

import edu.rochester.cif.cerberus.ldap.LDAPServer;
import edu.rochester.cif.cerberus.readers.ICardReader;
import edu.rochester.cif.cerberus.readers.debug.DebugCardReader;
import edu.rochester.cif.cerberus.readers.elcom.ElcomCardReader;
import edu.rochester.cif.cerberus.settings.EnumRunMode;
import edu.rochester.cif.cerberus.settings.Reference;
import edu.rochester.cif.cerberus.settings.Settings;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.lookup.MainMapLookup;

import javax.naming.NamingException;
import java.io.IOException;

/**
 * Main class for the program
 */
public class Cerberus {
    private static Reference ref = Reference.getInstance();

    private static Logger appLog;
    private static Logger accessLog;

    /**
     * Gets the application log for the program
     * The logger will output to both /var/log/cerberus as well as stdout
     * See log4j2.xml for more details
     * @return the logger instance requested
     */
    public static Logger getAppLog() {
        if (appLog == null)
            appLog = LogManager.getLogger("app");
        return appLog;
    }

    /**
     * Gets the access log for the door
     * The access log contains information on who was denied/granted access
     * It outputs in both /var/log/cerberus as well as stdout
     * See log4j2.xml for more details
     * @return the access logger instance
     */
    public static Logger getAccessLog() {
        if (accessLog == null)
            accessLog = LogManager.getLogger("access");
        return accessLog;
    }

    /**
     * The main function for this program
     * It processes parameters, initializes the loggers and calls the settings loader
     * Then depending on the run mode, it chooses what the program should do
     * @param args the command line arguments provided
     */
    public static void main(String[] args) {
        //Proper arg parse! Who could of thought of that? That's right, me.
        Options options = new Options();
        Option configPath = new Option("c", "config", true, "use specified config path");
        Option debugMode = new Option("d", "debug", false, "enable debug mode");
        Option verbose = new Option("V", "verbose", false, "verbose logging");
        Option printUsage = new Option("h", "help", false, "print usage");
        Option printVersion = new Option("v", "version", false, "print version info");

        options.addOption(configPath);
        options.addOption(debugMode);
        options.addOption(verbose);
        options.addOption(printVersion);
        options.addOption(printUsage);

        CommandLine cli = null;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter help = new HelpFormatter();

        //Try to parse arguments
        try {
            cli = parser.parse(options, args);
        } catch (ParseException err) {
            System.err.println(err.getMessage());
            help.printHelp(ref.PROG_NAME.toLowerCase(), options, true);
            System.exit(-1);
        }
        MainMapLookup.setMainArguments(
                cli.hasOption("verbose") ? "trace" : "info"
        );

        //Don't call the app log until the verbosity has been choosen
        Logger log = getAppLog();
        log.trace("Cerberus says hello!");

        //Load application configuration files
        Settings.init(cli);
        Settings settings = Settings.getInstance();

        //Determine what arguments do
        switch (settings.RUN_MODE) {
            case HELP:
                log.trace("Printing help");
                help.printHelp(ref.PROG_NAME.toLowerCase(), options, true);
                System.exit(0);
            case VERSION:
                log.trace("Printing version information");
                System.out.println(ref.ABOUT_MSG);
                System.exit(0);
            default:
                log.info("Starting Cerberus in {} mode",
                        settings.RUN_MODE == EnumRunMode.RUN ? "normal" : "debug");
                startListenLoop();
        }
    }

    /**
     * Starts the main listening loop of the program
     * Most of the card reader logic is in this function
     * If debug is specified, the program will run in debug mode
     * which takes stdin as the card reader
     */
    private static void startListenLoop() {
        Logger log = getAppLog();
        Logger access = getAccessLog();
        Settings settings = Settings.getInstance();
        LDAPServer server = new LDAPServer();
        try {
            server.connect();
        } catch (NamingException e) {
            log.fatal("Failed to establish initial connection to LDAP server!", e);
            System.exit(-1);
        }

        ICardReader reader;
        // Initialize the reader and start things up
        reader = settings.RUN_MODE == EnumRunMode.RUN ? new ElcomCardReader(settings.DEVICE) : new DebugCardReader();
        reader.registerStatusChangedCallback((newStatus) -> {
            // When a change in status occurs, act on the new status
            // Note that the actual state is not stored - only changes are acted upon
            // Any real concept of "state" is maintained by the card reader class
            switch (newStatus) {
                case IDLE:
                    log.trace("Reader is now ready");
                    break;
                case CARD_WAITING:
                    // Wait a moment before querying the ID, to prevent the reader from locking up
                    // TODO maybe look for a cleaner solution to this issue?
                    try {
                        Thread.sleep(200);
                    }
                    catch (InterruptedException e) {
                        log.trace("Card wait was interrupted!", e);
                    }

                    String data = reader.getID();
                    if (data == null)
                        break;
                    log.trace("Checking ID format");
                    String id;
                    String lcc;

                    // If data from card reader is not the right format of a UR ID card, reject it
                    // The day the school starts using non-numeric swipe cards is the day I eat my hat - Jack
                    if (data.matches("^[0-9]{19}$")) { // UofR ID
                        id = data.substring(1, 9);
                        lcc = data.substring(9, 11);
                    } else if (data.matches("^\\d{9}D\\d047$")) { // RIT ID
                        id = data.substring(0, 9);
                        lcc = data.substring(10, 11);
                    } else {
                        access.warn("Denied access to ID of wrong format: {}", data);
                        reader.denyAccess();
                        break;
                    }

                    try {
                        String result = server.queryUsername(id, lcc);
                        if (result == null) {
                            access.warn("Denied access to ID: {} (LCC {})", id, lcc);
                            reader.denyAccess();
                        }
                        else {
                            access.info("Granted access to {} (ID: {} LCC: {})", result, id, lcc);
                            reader.grantAccess();
                        }
                    }
                    catch (Exception e) {
                        log.error("LDAP query failed with the following error:", e);
                        reader.denyAccess();
                    }
                    break;
                case TAMPER:
                    access.warn("Tamper switch has been tripped!");
                    break;
                case FORCED_OPEN:
                    access.warn("Door has been forced open!");
                    break;
                case LINK_LOST:
                    access.warn("Link to reader has been lost!");
                    break;
                case RECOVERED_FROM_POWER_FAILURE:
                    access.warn("The reader has recovered from a power failure!");
            }
        });
        reader.open();

        // Close all connections on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.trace("Exit signal received, closing connection to reader");
                reader.close();
                log.trace("Closing ldap connection");
                server.closeConnection();
            } catch (IOException e) {
                log.error("An IO error occured while closing the reader connection on shutdown", e);
            } catch (NamingException e) {
                log.trace("An error occurred while closing the ldap connection on shutdown", e);
            }
        }));

    }
}
