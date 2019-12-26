package edu.rochester.cif.cerberus.readers.elcom;

import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import com.fazecast.jSerialComm.SerialPort;

import edu.rochester.cif.cerberus.readers.ICardReader;
import edu.rochester.cif.cerberus.readers.EnumReaderStatus;
import edu.rochester.cif.cerberus.readers.IStatusChangedCallback;

// GrimReaper - Access controller for the CIF lab
// Written by Ben Ackerman '18 - Summer 2017

// This class (and the rest of its package) defines the Elcom MAG-742/MAG-7042 (polled RS-485) reader type

public class ElcomCardReader implements ICardReader {

	private ElcomDataLink link = null;
	private Timer pollTimer = null;
	private EnumReaderStatus status;
	private IStatusChangedCallback callback = null;
	private String port;
	
	public ElcomCardReader(String port) {
		this.port = port;
	}
	
	@Override
	public void grantAccess() {
		String res = link.sendCommand("OA");
		updateStatus(res);
	}

	@Override
	public void denyAccess() {
		String res = link.sendCommand("OD");
		updateStatus(res);
	}

	@Override
	public void registerStatusChangedCallback(IStatusChangedCallback callback) {
		this.callback = callback;
	}

	@Override
	public String getID() {
		return link.sendCommand("R");
	}

	@Override
	public void parseReaderParams(Properties config) {
	}

	@Override
	public void close() {
		pollTimer.cancel();
		link.close();
	}

	@Override
	public void open() {
		// Open the data link (TODO configurable params)
		link = new ElcomDataLink();
		link.open(port, 9600, 7, SerialPort.ONE_STOP_BIT, SerialPort.ODD_PARITY, SerialPort.FLOW_CONTROL_DISABLED);
		status = EnumReaderStatus.IDLE;
		
		// Set up polling
		pollTimer = new Timer();
		TimerTask pollFunc = new TimerTask() {
			@Override
			public void run() {
				// Query reader status (if no card is waiting)
				String statusChars = link.sendCommand("?");
				updateStatus(statusChars);
			}
		};
		// TODO make poll freq configurable
		pollTimer.schedule(pollFunc, 0, 400);
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
