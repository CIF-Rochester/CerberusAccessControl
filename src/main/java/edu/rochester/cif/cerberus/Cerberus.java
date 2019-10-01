package edu.rochester.cif.cerberus;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Cerberus {
    public static final Logger LOG = LogManager.getLogger(Cerberus.class);

    public static void main(String[] args) {
        LOG.error("Logging");
    }
}
