package edu.rochester.cif.cerberus.ldap;

import edu.rochester.cif.cerberus.Cerberus;
import edu.rochester.cif.cerberus.settings.Settings;
import edu.rochester.cif.cerberus.settings.Reference;
import org.apache.logging.log4j.Logger;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import java.util.Hashtable;

/**
 * Object that represents a live LDAP server, which can be connected to perform queries
 */
public class LDAPServer {

    private static final Reference ref = Reference.getInstance();
    private static final Settings settings = Settings.getInstance();
    private static final Logger log = Cerberus.getAppLog();
    private static Hashtable<String, String> credentials;

    /**
     * Gets the credentials needed for instantiating a context object
     * @return the credentials, stored inside a HashTable
     */
    private static Hashtable<String, String> getCredentials() {
      if (credentials == null) {
        credentials = new Hashtable<>();
        credentials.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        credentials.put(Context.PROVIDER_URL, settings.LDAP_HOST);
        credentials.put(Context.SECURITY_AUTHENTICATION, "simple");
        credentials.put(Context.SECURITY_PRINCIPAL, settings.LDAP_BIND_DN);
        credentials.put(Context.SECURITY_CREDENTIALS, settings.LDAP_PASSWORD);
      }
      return credentials;
    }

    /**
     * Gets the search control needed to query access information
     * @return a search control object that has the username as the return attribute
     */
    private static SearchControls getAccessSearchControl() {
        SearchControls ctrl = new SearchControls();
        ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
        ctrl.setReturningAttributes(new String[]{ref.LDAP_USERNAME_FIELD});
        return ctrl;
    }

    /**
     * Gets the query filter used to perform access queries
     * @param id the student id to query
     * @param lcc the lcc number to query
     * @return string that is the query, which checks id, lcc and whether an account is locked
     */
    private static String getQueryFilter(String id, String lcc) {
        return String.format("(&(%s=%s)(%s=%s)(!(%s=TRUE)))",
                ref.LDAP_ID_FIELD, id,
                ref.LDAP_LCC_FIELD, lcc,
                ref.LDAP_DISABLED_FIELD);
    }

    private DirContext connection = null;

    /**
     * Connects to the server
     * If a connection already exists, then attempt to close the current connection and try again
     * @throws NamingException when an error occurs while performing the connection
     */
    public void connect() throws NamingException {
        log.trace("Establishing connection to LDAP server");
        if (connection != null) {
            try {
                log.trace("Previous connection detected, closing first");
                connection.close();
            } catch (NamingException e) {
                log.warn("Could not close previous connection, ignoring and continuing");
                log.trace("error was", e);
            }
        }
        connection = new InitialDirContext(getCredentials());
        log.trace("Connection established");
    }

    /**
     * Closes the existing connection
     * @throws NamingException when an error occurs while performing the close action
     */
    public void closeConnection() throws NamingException {
        if (connection == null)
            return;
        log.trace("Closing connection to LDAP server");
        connection.close();
    }

    /**
     * Queries the LDAP server once for a username, if a connection error occurs then try to re-establish a connection
     * and try again
     * @param studentid the student numeric id of obtained from the card
     * @param lcc the lcc of the university id
     * @return the string username of the person, null if none exists
     * @throws NamingException when there is an error performing the ldap query multiple times
     */
    public String queryUsername (String studentid, String lcc) throws NamingException {
        try {
            return queryOnce(studentid, lcc);
        } catch (NamingException e) {
            log.warn("LDAP connection error on query, attempting new connection...");
            log.trace("error was", e);
            connect();
            return queryOnce(studentid, lcc);
        }
    }

    /**
     * Queries the LDAP server once for a username, throws an error if problems occur
     * @param studentid the student numeric id of obtained from the card
     * @param lcc the lcc of the university id
     * @return the string username of the person, null if none exists
     * @throws NamingException when there is an error performing the ldap query
     */
    private String queryOnce (String studentid, String lcc) throws NamingException {
        log.trace("Querying {} with lcc of {}", studentid, lcc);
        NamingEnumeration<SearchResult> en = connection.search(
                settings.LDAP_SEARCH_BASE,
                getQueryFilter(studentid,lcc),
                getAccessSearchControl());

        if (!en.hasMore())
            return null;
        return (String) en.next().getAttributes().get(ref.LDAP_USERNAME_FIELD).get();
    }

}
