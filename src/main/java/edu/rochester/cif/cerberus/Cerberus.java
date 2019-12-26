package edu.rochester.cif.cerberus;

import edu.rochester.cif.cerberus.ldap.LDAPServer;
import edu.rochester.cif.cerberus.settings.EnumRunMode;
import edu.rochester.cif.cerberus.settings.Reference;
import edu.rochester.cif.cerberus.settings.Settings;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.lookup.MainMapLookup;

import javax.naming.NamingException;

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
                cli.hasOption("debug") ? "trace" : "info"
        );

        //Don't call the app log until the verbosity has been choosen
        Logger log = getAppLog();
        log.info("Starting Cerberus...");

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
                log.trace("Starting listen loop in {} mode",
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
        LDAPServer server = new LDAPServer();
        try {
            server.connect();
        } catch (NamingException e) {
            log.fatal("Failed to establish initial connection to LDAP server!", e);
            System.exit(-1);
        }

        // Close all connections on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.closeConnection();
            } catch (NamingException e) {
                log.trace("An error occurred while closing the ldap connection on shutdown", e);
            }
        }));

    }
}
