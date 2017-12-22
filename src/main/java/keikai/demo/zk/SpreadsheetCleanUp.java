package keikai.demo.zk;

import io.keikai.client.api.Spreadsheet;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.util.DesktopCleanup;

/**
 * close {@link Spreadsheet} when a desktop is dropped to avoid occupying Keikai server's resources.
 */
public class SpreadsheetCleanUp implements DesktopCleanup{
    static final public String SPREADSHEET = "spreadsheet";

    @Override
    public void cleanup(Desktop desktop) throws Exception {
        Spreadsheet spreadsheet = (Spreadsheet)desktop.getAttribute(SPREADSHEET);
        if (spreadsheet != null){
            spreadsheet.close();
        }
    }
}
