package keikai.demo;

import org.zkoss.zk.ui.*;

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

    static public String getKeikaiServerAddress(Execution execution) {
        String ip = execution.getParameter("server");
        return ip == null ? Configuration.LOCAL_KEIKAI_SERVER : "http://" + ip;
    }
}
