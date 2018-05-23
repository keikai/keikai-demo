package keikai.demo;

import java.util.logging.*;

public class KeikaiUtil {
    static public void enableSocketIOLog() {
        Logger log = Logger.getLogger("");
        log.setLevel(Level.FINER);

        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(Level.ALL);
        log.addHandler(handler);
    }
}
