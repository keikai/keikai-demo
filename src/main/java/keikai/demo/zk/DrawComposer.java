/* KKComposer.java

	Purpose:
		
	Description:
		
	History:
		Created by Hawk Chen

Copyright (C) 2017 Potix Corporation. All Rights Reserved.
*/
package keikai.demo.zk;

import io.keikai.client.api.*;
import io.keikai.client.api.event.*;
import keikai.demo.Configuration;
import org.apache.commons.io.FileUtils;
import org.zkoss.zhtml.Script;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.event.UploadEvent;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;

import java.io.*;
import java.util.HashMap;
import java.util.function.*;


/**
 * a small prize draw demo <br>
 * @author Hawk
 */
public class DrawComposer extends SelectorComposer<Component> {
	private Spreadsheet spreadsheet;

	@Wire
	private Label fileNameLabel;
	final File BOOK_FOLDER = new File(getPage().getDesktop().getWebApp().getRealPath("/book/demo/"));

	@Override
	public void doAfterCompose(Component root) throws Exception {
		super.doAfterCompose(root);
		initSpreadsheet();
		importFile("PrizeDraw.xlsx");
		spreadsheet.getWorksheet().getButton("draw").addAction(buttonShapeMouseEvent -> {
			refreshFormula();
		});
	}

	private void refreshFormula() {
		Range numberCell = spreadsheet.getRange("C12");
		String formula = numberCell.getRangeValue().getFormula();
		if (formula.endsWith(" ")){
			numberCell.setValue(formula.trim());
		}else{
			numberCell.setValue(formula+" ");
		}
	}


	private void importFile(String fileName) throws IOException, AbortedException {
		File template = new File(BOOK_FOLDER, fileName);
		final Desktop desktop = getPage().getDesktop();
		spreadsheet.importAndReplace(fileName, template);
		fileNameLabel.setValue(fileName);
	}

	/**
	 * get a spreadsheet java client and getUpdateRunner spreadsheet on a browser
	 */
	private void initSpreadsheet() {
		spreadsheet = Keikai.newClient(getKeikaiServerAddress()); //connect to keikai server
		getPage().getDesktop().setAttribute(SpreadsheetCleanUp.SPREADSHEET, spreadsheet); //make spreadsheet get closed
		//pass target element's id and get keikai script URI
		String scriptUri = spreadsheet.getURI(getSelf().getFellow("myss").getUuid());
		//load the initial script to getUpdateRunner spreadsheet at the client
		Script initialScript = new Script();
		initialScript.setSrc(scriptUri);
		initialScript.setDefer(true);
		initialScript.setAsync(true);
		initialScript.setPage(getPage());
	}

	private String getKeikaiServerAddress() {
	    String ip = Executions.getCurrent().getParameter("server");
        return ip == null ? Configuration.LOCAL_KEIKAI_SERVER : "http://"+ip;
	}
}
