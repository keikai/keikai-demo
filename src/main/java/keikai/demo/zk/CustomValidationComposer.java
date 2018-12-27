package keikai.demo.zk;

import io.keikai.client.api.*;
import io.keikai.client.api.ui.UIActivity;
import keikai.demo.KeikaiUtil;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;

/**
 * @author Hawk
 */
public class CustomValidationComposer extends SelectorComposer<Component> {
    private Spreadsheet spreadsheet;

    @Wire
    org.zkoss.zul.Script kkScript;

    @Override
    public void doAfterCompose(Component root) throws Exception {
        super.doAfterCompose(root);
        spreadsheet = Keikai.newClient(KeikaiUtil.getKeikaiServerAddress(Executions.getCurrent())); //connect to keikai server
//        KeikaiUtil.enableSocketIOLog();
        initSpreadsheet();
        initDataValidation();
    }


    /**
     * get a spreadsheet java client and getUpdateRunner spreadsheet on a browser
     */
    private void initSpreadsheet() {
        //pass target element's id and get keikai script URI
        String scriptUri = spreadsheet.getURI(getSelf().getFellow("ss").getUuid());
        //load the initial script to getUpdateRunner spreadsheet at the client
        kkScript.setSrc(scriptUri);
        spreadsheet.setUIActivityCallback(new UIActivity() {
            public void onConnect() {
            }

            public void onDisconnect() {
                spreadsheet.close();
            }
        });

    }

    /**
     * create a business related validation that can't be achieved by built-in validations.
     */
    private void initDataValidation() {
        Range range = spreadsheet.getRange("A1:D4");

        DataValidation dataValidation = range.createDataValidation();
        dataValidation.setErrorMessage("the value should contain \"zk\"");
        dataValidation.setValidator((text) -> {
            //implement the business related validating logic
            return text.toString().contains("zk"); // return true to accept, false to reject.
        });
        range.setDataValidation(dataValidation);
    }

}
