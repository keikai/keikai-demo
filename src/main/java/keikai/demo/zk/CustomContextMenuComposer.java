package keikai.demo.zk;

import io.keikai.client.api.*;
import io.keikai.client.api.event.*;
import io.keikai.client.api.ui.UIActivity;
import keikai.demo.*;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.Popup;

/**
 * @author Hawk
 */
public class CustomContextMenuComposer extends SelectorComposer<Component> {
    private Spreadsheet spreadsheet;

    @Wire
    org.zkoss.zul.Script kkScript;

    @Override
    public void doBeforeComposeChildren(Component comp) throws Exception {
        super.doBeforeComposeChildren(comp);
        spreadsheet = Keikai.newClient(KeikaiUtil.getKeikaiServerAddress(Executions.getCurrent())); //connect to keikai server
        comp.getDesktop().setAttribute("spreadsheet", spreadsheet); //share to the custom context menu
    }

    @Override
    public void doAfterCompose(Component root) throws Exception {
        super.doAfterCompose(root);
//        KeikaiUtil.enableSocketIOLog();
        initSpreadsheet();
    }


    /**
     * get a spreadsheet java client and getUpdateRunner spreadsheet on a browser
     */
    private void initSpreadsheet() {
        //pass target element's id and get keikai script URI
        String scriptUri = spreadsheet.getURI(getSelf().getFellow("ss").getUuid());
        //load the initial script to getUpdateRunner spreadsheet at the client
        kkScript.setSrc(scriptUri);
        spreadsheet.setUIActivityCallback(new CloseSpreadsheetActivity(spreadsheet));
    }



}
