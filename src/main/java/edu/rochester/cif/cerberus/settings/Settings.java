package edu.rochester.cif.cerberus.settings;

import edu.rochester.cif.cerberus.Cerberus;
import org.apache.commons.cli.CommandLine;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * A singleton that contains command line and config file options
 */
public class Settings {
    private static final Reference ref = Reference.getInstance();
    private static final Logger log = Cerberus.getAppLog();
    private static Settings instance;

    /**
     * Initializes settings from a configuration file
     * This needs to be called before getInstance otherwise no settings are instantiated
     * @param opts command line arguments passed to the function
     */
    public static void init(CommandLine opts) {
        if (instance == null)
            instance = new Settings(opts);
    }

    /**
     * Gets a instance of the settings object
     * @return the settings instance
     */
    public static Settings getInstance() {
        return instance;
    }

    public final EnumRunMode RUN_MODE;
    public final String DEVICE;
    public final String LDAP_HOST;
    public final String LDAP_BIND_DN;
    public final String LDAP_PASSWORD;
    public final String LDAP_SEARCH_BASE;

    /**
     * Create a new settings object given by the config location in the cmd parameters
     * If none is found, the program will use the fallback location specified in app.properties
     * If a critical config option is not specified, the program will exit
     * This function also re-parses the command line arguments into a more usable format
     * @param opts the command line arguments given
     */
    private Settings (CommandLine opts) {

        if (opts.hasOption("help"))
            RUN_MODE = EnumRunMode.HELP;
        else if (opts.hasOption("version"))
            RUN_MODE = EnumRunMode.VERSION;
        else if (opts.hasOption("debug"))
            RUN_MODE = EnumRunMode.DEBUG;
        else
            RUN_MODE = EnumRunMode.RUN;

        String configPath;
        if (opts.hasOption("config"))
            configPath = opts.getOptionValue("config");
        else
            configPath = ref.DEFAULT_CONFIG_PATH;

        log.trace("Attempting to load configuration file: {}", configPath);
        Properties config = new Properties();
        try (FileInputStream fis = new FileInputStream(configPath)){
            config.load(fis);
        } catch (IOException e) {
            log.error("An IO error occurred while loading configuration", e);
            System.exit(-1);
        }

        DEVICE = config.getProperty("Device");
        LDAP_HOST = config.getProperty("LDAPHost");
        LDAP_BIND_DN = config.getProperty("LDAPBindDN");
        LDAP_PASSWORD = config.getProperty("LDAPPassword");
        LDAP_SEARCH_BASE = config.getProperty("LDAPSearchBase");
        if (DEVICE == null || DEVICE.isEmpty()) {
            log.error("No 'Device' attribute set in configuration file!");
            System.exit(-1);
        } else if (LDAP_HOST == null || LDAP_HOST.isEmpty()) {
            log.error("No 'LDAPHost' attribute set in configuration file!");
            System.exit(-1);
        } else if (LDAP_BIND_DN == null || LDAP_BIND_DN.isEmpty()) {
            log.error("No 'LDAPBindDN' attribute set in configuration file!");
            System.exit(-1);
        } else if (LDAP_PASSWORD == null || LDAP_PASSWORD.isEmpty()) {
            log.error("No 'LDAPPassword' attribute set in configuration file!");
            System.exit(-1);
        } else if (LDAP_SEARCH_BASE == null || LDAP_SEARCH_BASE.isEmpty()) {
            log.error("No 'LDAPSearchBase' attribute set in configuration file!");
            System.exit(-1);
        }

        log.trace("Configuration loaded:");
        log.trace("Device='" + DEVICE + "'");
        log.trace("LDAPHost='" + LDAP_HOST + "'");
        log.trace("LDAPBindDN='" + LDAP_BIND_DN + "'");
        log.trace("LDAPPassword='" + LDAP_PASSWORD + "'");
        log.trace("LDAPSearchBase='" + LDAP_SEARCH_BASE + "'");
    }

}
