package edu.rochester.cif.cerberus.readers.elcom;

import java.io.IOException;
import java.util.Properties;

import com.fazecast.jSerialComm.SerialPort;

import edu.rochester.cif.cerberus.Cerberus;
import edu.rochester.cif.cerberus.readers.ICardReader;
import edu.rochester.cif.cerberus.readers.EnumReaderStatus;
import edu.rochester.cif.cerberus.readers.IStatusChangedCallback;
import org.apache.logging.log4j.Logger;

// GrimReaper - Access controller for the CIF lab
// Written by Ben Ackerman '18 - Summer 2017

// This class (and the rest of its package) defines the Elcom MAG-742/MAG-7042 (polled RS-485) reader type

public class ElcomCardReader implements ICardReader {

	private ElcomDataLink link = null;
	private volatile boolean shouldHalt = false;
	private EnumReaderStatus status;
	private IStatusChangedCallback callback = null;
	private String port;
	
	public ElcomCardReader(String port) {
		this.port = port;
	}
	
	@Override
	public void grantAccess() {
		String res = null;
	    try {
			res = link.sendCommand("OA");
		} catch (IOException e) {
			Cerberus.getAppLog().error("[Elcom] Failed to read from serial port", e);
			System.exit(1);
		}
		updateStatus(res);
	}

	@Override
	public void denyAccess() {
		String res = null;
		try {
			res = link.sendCommand("OD");
		} catch (IOException e) {
			Cerberus.getAppLog().error("[Elcom] Failed to read from serial port", e);
			System.exit(1);
		}
		updateStatus(res);
	}

	@Override
	public void registerStatusChangedCallback(IStatusChangedCallback callback) {
		this.callback = callback;
	}

	@Override
	public String getID() {
	    try {
			return link.sendCommand("R");
		} catch (IOException e) {
			Cerberus.getAppLog().error("[Elcom] Failed to read from serial port", e);
			System.exit(1);
		}
	    return null;
	}

	@Override
	public void parseReaderParams(Properties config) {
	}

	@Override
	public void close() {
	    shouldHalt = true;
		link.close();
	}

	@Override
	public void open() {
	    // I rewrote this to fail properly if a connection cannot be established
		// No more waiting in limbo polluting the logs!
		new Thread(() -> {
		    Logger log = Cerberus.getAppLog();
			link = new ElcomDataLink();
			link.open(port, 9600, 7, SerialPort.ONE_STOP_BIT, SerialPort.ODD_PARITY, SerialPort.FLOW_CONTROL_DISABLED);
			status = EnumReaderStatus.IDLE;
			do {
				String statusChars = null;
				try {
					statusChars = link.sendCommand("?");
				} catch (IOException e) {
					Cerberus.getAppLog().error("[Elcom] Failed to read from serial port", e);
					System.exit(1);
				}
				updateStatus(statusChars);

				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					log.trace("Reader wait was interrupted!", e);
				}
			} while (!shouldHalt);
		}).start();
	}
	
	// Convert the reader's response to a reader state, and run the status-changed callback if necessary
	private void updateStatus(String statusChars) {
		// Default to "idle" state
		EnumReaderStatus newStatus = EnumReaderStatus.IDLE;
		
		// If no status characters were received, our link has been lost
		if (statusChars == null) {
			newStatus = EnumReaderStatus.LINK_LOST;
		}
		// Is there a card in memory?
		else if (statusChars.contains("D")) {
			newStatus = EnumReaderStatus.CARD_WAITING;
		}
		// Has the tamper switch been tripped?
		else if (statusChars.contains("T")) {
			newStatus = EnumReaderStatus.TAMPER;
		}
		// Is the unit reporting that there was a power failure?
		else if (statusChars.contains("P")) {
			newStatus = EnumReaderStatus.RECOVERED_FROM_POWER_FAILURE;
		}

		if (newStatus != status) {
			status = newStatus;
			if (callback != null) {
				callback.statusChanged(newStatus);
			}
		}
	}

}
