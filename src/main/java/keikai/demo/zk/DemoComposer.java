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
import io.keikai.client.api.ui.UIActivity;
import keikai.demo.*;
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
 * Demonstrate Keikai main features <br>
 * @author Hawk
 */
public class DemoComposer extends SelectorComposer<Component> {
    public static final String BLANK_XLSX = "blank.xlsx";
    private static final int MAX_COLUMN = 10; //the max column to insert data
	private Spreadsheet spreadsheet;
	private Range selectedRange;

	@Wire
	private Listbox filelistBox;
	@Wire
	private Popup filePopup;
	@Wire
	private Label fileNameLabel;
	@Wire
	private Textbox cellValueBox;
	@Wire
	private Div fileNameBox;

	private int currentDataRowIndex = 0; //current row index to insert data


	private ListModelList<String> fileListModel;
	final File BOOK_FOLDER = new File(getPage().getDesktop().getWebApp().getRealPath("/book/demo"));

	@Override
	public void doAfterCompose(Component root) throws Exception {
		super.doAfterCompose(root);
//		KeikaiUtil.enableSocketIOLog();
		initSpreadsheet();
		initMenubar();
		initFormulaBar();
		//enable server push to update UI according to keikai async response
		root.getDesktop().enableServerPush(true);
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
		spreadsheet.setUIActivityCallback(new CloseSpreadsheetActivity(spreadsheet));
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

	private void initMenubar() {
		fileListModel = new ListModelList(generateBookList());
		filelistBox.setModel(fileListModel);
	}

	private String[] generateBookList() {
		return BOOK_FOLDER.list();
	}

	/*
	 * register an event listener to show cell value and address on the formula bar
	 */
	private void initFormulaBar() {
		final Desktop desktop = getSelf().getDesktop();
		ExceptionalConsumer<RangeEvent> listener = (e) -> {
			RangeSelectEvent event = (RangeSelectEvent) e;
			selectedRange = event.getActiveSelection();
			//display the current cell value/formula
			RangeValue rangeValue = event.getRange().getRangeValue();
			AsyncRender.getUpdateRunner(desktop, () -> {
				// ignore validation on null value
				if (rangeValue.getCellValue() != null && rangeValue.getCellValue().isFormula()) {
					cellValueBox.setRawValue(rangeValue.getCellValue().getFormula());
				} else {
					cellValueBox.setRawValue(rangeValue.getValue());
				}
				((Label) getSelf().getFellow("cellAddress")).setValue(selectedRange.getA1Notation());
			}).run();
		};
		spreadsheet.addEventListener(Events.ON_SELECTION_CHANGE,  listener::accept);
	}

	@Listen("onSelect = #filelistBox")
	public void loadServerFile() throws IOException {
		filePopup.close();
		String fileName = fileListModel.getSelection().iterator().next();
		try {
			importFile(fileName);
		} catch (AbortedException e) {
			Clients.showNotification(e.getMessage());
		}
		fileListModel.clearSelection();
	}

	/**
	 * Can't import a book more than once, we should delete the previous book first.
	 */
	@Listen("onClick = menuitem[iconSclass='z-icon-file']")
	public void newFile() {
		try{
			importFile(BLANK_XLSX);
		}catch(Exception e){
			Clients.showNotification(e.getMessage());
		}
	}

	@Listen("onClick = menuitem[iconSclass='z-icon-upload']")
	public void openUploadDialog(){
		Fileupload.get(new HashMap(), null, "Excel (xlsx) File Upload", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", -1, -1, true,null);
	}

	@Listen("onUpload = #root")
	public void upload(UploadEvent e) throws IOException, DuplicateNameException, AbortedException {
		String name = e.getMedia().getName();
		InputStream streamData = e.getMedia().getStreamData();
		spreadsheet.imports(name, streamData);
		FileUtils.copyInputStreamToFile(streamData, new File(BOOK_FOLDER, name));
	}

	@Listen("onClick = menuitem[iconSclass='z-icon-file-excel-o']")
	public void export() throws FileNotFoundException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		spreadsheet.export(spreadsheet.getWorkbook().getName(), outputStream);
		Filedownload.save(outputStream.toByteArray(), "application/excel", spreadsheet.getWorkbook().getName());
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
