/* KKComposer.java

	Purpose:
		
	Description:
		
	History:
		Created by Hawk Chen

Copyright (C) 2017 Potix Corporation. All Rights Reserved.
*/
package keikai.demo.zk;

import io.keikai.client.api.*;
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
import java.util.concurrent.ExecutionException;
import java.util.function.*;
import java.util.logging.*;


/**
 * Demonstrate Keikai main features <br>
 * @author Hawk
 */
public class DemoComposer extends SelectorComposer<Component> {
    public static final String BLANK_XLSX = "blank.xlsx";
    private static final int MAX_COLUMN = 10; //the max column to insert data
	private Spreadsheet spreadsheet;

	@Wire
	private Listbox filelistBox;
	@Wire
	private Popup filePopup;
	@Wire
	private Label fileNameLabel;

	private int currentDataRowIndex = 0; //current row index to insert data


	private ListModelList<String> fileListModel;
	final private File BOOK_FOLDER = new File(getPage().getDesktop().getWebApp().getRealPath("/book/"));

	@Override
	public void doAfterCompose(Component root) throws Exception {
		super.doAfterCompose(root);
//		enableSocketIOLog();
		initSpreadsheet();
		initMenubar();
		//enable server push to update UI according to keikai async response
		root.getDesktop().enableServerPush(true);

	}


	private void importFile(String fileName) throws IOException {
		File template = new File(BOOK_FOLDER, fileName);
		spreadsheet.imports(fileName, template);
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
        return ip == null ? Configuration.INTERNAL_KEIKAI_SERVER : "http://"+ip;
	}

	private void initMenubar() {
		fileListModel = new ListModelList(generateBookList());
		filelistBox.setModel(fileListModel);
	}

	private String[] generateBookList() {
		return BOOK_FOLDER.list();
	}

	@Listen("onSelect = #filelistBox")
	public void loadServerFile() throws IOException, ExecutionException, InterruptedException {
		filePopup.close();
		String fileName = fileListModel.getSelection().iterator().next();
        if (spreadsheet.containsWorkbook(fileName).get()){
            spreadsheet.deleteWorkbook(fileName);
        }
		importFile(fileName);
		fileListModel.clearSelection();
	}

	/**
	 * Can't import a book more than once, we should delete the previous book first.
	 */
	@Listen("onClick = menuitem[iconSclass='z-icon-file']")
	public void newFile() throws ExecutionException, InterruptedException {
		try{
		    if (spreadsheet.containsWorkbook(BLANK_XLSX).get()){
		        spreadsheet.deleteWorkbook(BLANK_XLSX);
            }
			importFile(BLANK_XLSX);
		}catch(IOException e){
			Clients.showNotification(e.getMessage());
		}
	}

	@Listen("onClick = menuitem[iconSclass='z-icon-upload']")
	public void openUploadDialog(){
		Fileupload.get(new HashMap(), null, "Excel (xlsx) File Upload", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", -1, -1, true,null);
	}

	@Listen("onUpload = #root")
	public void upload(UploadEvent e) throws IOException {
		String name = e.getMedia().getName();
		InputStream streamData = e.getMedia().getStreamData();
		spreadsheet.imports(name, streamData);
		FileUtils.copyInputStreamToFile(streamData, new File(BOOK_FOLDER, name));
	}

	@Listen("onClick = menuitem[iconSclass='z-icon-file-excel-o']")
	public void export() throws FileNotFoundException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		spreadsheet.export(spreadsheet.getCurrentWorkbook(), outputStream).whenComplete((a, b) -> {
			try {
				Executions.activate(getSelf().getDesktop());
				Filedownload.save(outputStream.toByteArray(), "application/excel", spreadsheet.getCurrentWorkbook());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {

				Executions.deactivate(getSelf().getDesktop());
			}
		});

	}


	private void enableSocketIOLog() {
		Logger log = Logger.getLogger("");
		log.setLevel(Level.FINER);

		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		handler.setLevel(Level.ALL);
		log.addHandler(handler);
	}	
	
	/**
	 * Since a lambda can't throw exceptions so we need to catch it and throw an unchecked exception
	 * @author hawk
	 *
	 */
	public interface ExceptionalConsumer<T> extends Consumer<T>{
		default void accept(T v){
			try {
				acceptWithException(v);
			} catch (Exception e) {
				throw new RuntimeException("wrapped", e);
			}
		}

		void acceptWithException(T v) throws Exception;
	}

	/**
	 * Represents a function that handles checked exception by throwing a runtime exception.
	 * @param <T> input
	 * @param <R> result
	 */
	public interface ExceptionableFunction<T, R> extends Function<T, R> {
		default R apply(T val) {
			try {
				return applyWithException(val);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
		R applyWithException(T val) throws Exception;
	}
}
