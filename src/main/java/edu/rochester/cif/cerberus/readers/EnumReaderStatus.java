package edu.rochester.cif.cerberus.readers;

// GrimReaper - Access controller for the CIF lab
// Written by Ben Ackerman '18 - Summer 2017

// This enum defines the possible states the reader can be in

public enum EnumReaderStatus {
	IDLE,
	CARD_WAITING,
	LINK_LOST,
	RECOVERED_FROM_POWER_FAILURE,
	TAMPER,
	FORCED_OPEN
}
