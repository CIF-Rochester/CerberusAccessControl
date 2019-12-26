package edu.rochester.cif.cerberus.settings;

/**
 * An enum which specifies what mode the program is running as
 * Help - show program usage
 * Version - show about and version information
 * Debug - run the program in debug mode (not actually connected to a reader)
 * Run - run the program as in production
 */
public enum EnumRunMode {
    HELP,
    VERSION,
    DEBUG,
    RUN
}
