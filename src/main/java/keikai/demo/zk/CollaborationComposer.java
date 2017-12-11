/* KKComposer.java

	Purpose:
		
	Description:
		
	History:
		4:50 PM 16/05/2017, Created by jumperchen

Copyright (C) 2017 Potix Corporation. All Rights Reserved.
*/
package keikai.demo.zk;

import io.keikai.client.api.*;
import keikai.demo.*;
import org.zkoss.zhtml.*;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.select.*;

import java.util.logging.*;


/**
 * FIXME this feature is not yet ready.
 * @author Hawk
 */
public class CollaborationComposer extends SelectorComposer<Component> {
	private Spreadsheet spreadsheet;
	private String applicationId = "collaboration";

	static {
		// Debug socket.io
		Logger log = java.util.logging.Logger.getLogger("");
		log.setLevel(Level.WARNING);

		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		handler.setLevel(Level.ALL);
		log.addHandler(handler);
	}
	
	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		//enable server push to update UI according to keikai async response 
		initSpreadsheet();
	}


	/**
	 * get a spreadsheet java client and render spreadsheet on a browser 
	 */
	private void initSpreadsheet() {
		//every client with the same application ID that share the same spreadsheet data at the keikai server
		spreadsheet = Keikai.newClient(Configuration.KEIKAI_SERVER, applicationId); 
		//pass target element's id and get keikai script URI
		String scriptUri = spreadsheet.getURI(getSelf().getFellow("mywin").getFellow("myss").getUuid());
		//load the initial script to render spreadsheet at the client
		Script initalScript = new Script();
		initalScript.setSrc(scriptUri);
		initalScript.setDefer(true);
		initalScript.setAsync(true);
		initalScript.setPage(getPage());
	}
}
