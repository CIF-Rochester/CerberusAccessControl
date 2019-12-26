package edu.rochester.cif.cerberus.settings;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * A singleton that contains program constants
 */
public class Reference {

    private static Reference instance;
    private static final String resPath = "app.properties";

    /**
     * Gets a reference object containing constants defined in app.properties
     * @return the reference instance
     */
    public static Reference getInstance() {
        if (instance == null)
            instance = new Reference();
        return instance;
    }

    public final String PROG_NAME;
    public final String VERSION;
    public final String DEFAULT_CONFIG_PATH;
    public final String ABOUT_MSG;
    public final String LDAP_ID_FIELD;
    public final String LDAP_LCC_FIELD;
    public final String LDAP_USERNAME_FIELD;
    public final String LDAP_DISABLED_FIELD;

    /**
     * Creates an object containing constants defined in app.properties
     */
    private Reference() {
        Properties props = new Properties();
        try (InputStream propFile = getClass().getClassLoader().getResourceAsStream(resPath)){
            if (propFile != null)
                props.load(propFile);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        PROG_NAME = props.getProperty("AppName");
        VERSION = props.getProperty("Version");
        DEFAULT_CONFIG_PATH = props.getProperty("DefaultConfigPath");
        ABOUT_MSG = props.getProperty("AboutMsg");
        LDAP_ID_FIELD = props.getProperty("LDAPIDField");
        LDAP_LCC_FIELD = props.getProperty("LDAPLCCField");
        LDAP_USERNAME_FIELD = props.getProperty("LDAPUsernameField");
        LDAP_DISABLED_FIELD = props.getProperty("LDAPDisabledField");
    }

}
