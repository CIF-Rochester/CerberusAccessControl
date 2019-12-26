package edu.rochester.cif.cerberus.readers.debug;

import edu.rochester.cif.cerberus.Cerberus;
import edu.rochester.cif.cerberus.readers.EnumReaderStatus;
import edu.rochester.cif.cerberus.readers.ICardReader;
import edu.rochester.cif.cerberus.readers.IStatusChangedCallback;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

/**
 * Card reader emulator that reads from stdin
 */
public class DebugCardReader implements ICardReader {
    private Logger log = Cerberus.getAppLog();
    private IStatusChangedCallback callback = null;
    private volatile boolean shouldExit;
    private String data = "";

    @Override
    public void grantAccess() {
        log.info("[DebugCardReader]Granted access");
        callback.statusChanged(EnumReaderStatus.IDLE);
    }

    @Override
    public void denyAccess() {
        log.info("[DebugCardReader]Denied access");
        callback.statusChanged(EnumReaderStatus.IDLE);
    }

    @Override
    public void registerStatusChangedCallback(IStatusChangedCallback callback) {
        this.callback = callback;
    }

    @Override
    public String getID() {
        return data;
    }

    @Override
    public void parseReaderParams(Properties config) {
    }

    @Override
    public void open() {
        log.trace("[DebugCardReader]Connection opened");
        Thread listenThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            do {
                System.out.println("Enter a 19 digit serial number ('.quit' to exit): ");
                System.out.flush();
                if (scanner.hasNext()) {
                    data = scanner.nextLine();
                    if (data.equals(".quit"))
                        return;
                    callback.statusChanged(EnumReaderStatus.CARD_WAITING);
                }
            } while (!shouldExit);
        });
        listenThread.start();
    }

    @Override
    public void close() {
        log.trace("[DebugCardReader]Connection closed");
        shouldExit = true;
    }
}
