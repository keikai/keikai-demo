package keikai.demo;

import io.keikai.client.api.Spreadsheet;
import io.keikai.client.api.ui.UIActivity;

/**
 * Close Keikai Java client when UI client is closed.
 */
public class CloseSpreadsheetActivity implements UIActivity {

    private Spreadsheet spreadsheet;

    public CloseSpreadsheetActivity(Spreadsheet spreadsheet){
        this.spreadsheet = spreadsheet;
    }

    @Override
    public void onConnect() {

    }

    @Override
    public void onDisconnect() {
        spreadsheet.close();
    }
}
