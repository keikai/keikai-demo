/* KKComposer.java

	Purpose:
		
	Description:
		
	History:
		Created by Hawk Chen

Copyright (C) 2017 Potix Corporation. All Rights Reserved.
*/
package keikai.demo.zk;

import static com.keikai.util.Converter.numToAbc;

import java.io.*;
import java.util.concurrent.ExecutionException;
import java.util.function.*;
import java.util.logging.*;

import keikai.demo.Configuration;
import keikai.demo.zk.SpreadsheetComposer.ExceptionableFunction;

import org.zkoss.zhtml.Script;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkex.zul.Colorbox;
import org.zkoss.zul.*;
import org.zkoss.zul.ext.Selectable;

import com.keikai.client.api.*;
import com.keikai.client.api.Borders.BorderIndex;
import com.keikai.client.api.Fill.PatternFill;
import com.keikai.client.api.Fill.PatternFill.PatternType;
import com.keikai.client.api.Font.Underline;
import com.keikai.client.api.Range.*;
import com.keikai.client.api.event.*;
import com.keikai.client.api.event.Events;

/**
 * Demonstrate API usage about: import, export, listen events, change styles, insert data,
 * @author Hawk
 */
public class SpreadsheetComposer extends SelectorComposer<Component> {
	private static final int MAX_COLUMN = 10; //the max column to insert data
	private Spreadsheet spreadsheet;
	private Range selectedRange; //cell reference

	@Wire
	private Selectbox borderIndexBox;
	@Wire
	private Selectbox borderLineStyleBox;
	@Wire
	private Listbox filelistBox;
	
	@Wire
	private Label keyCode;

	// filter
	@Wire
	private Popup filterPopup;
	@Wire
	private Selectbox filterOperator;
	@Wire
	private Intbox filterField;
	@Wire
	private Textbox filterCriteria1;
	@Wire
	private Radiogroup filterDropDown;
	@Wire
	private Button filterBtn;

	@Wire
	private Button applyFont;
	@Wire
	private Label cellInfo;
	@Wire("#cellValue")
	private Textbox cellValueBox;
	@Wire
	private Colorbox borderColorBox;
	@Wire
	private Selectbox fontSizeBox;
	@Wire
	private Popup filePopup;

	@Wire 
	private Popup contextMenu;
	@Wire
	private Selectbox fillPatternBox;
	@Wire
	private Textbox validationFormulaBox;
	@Wire
	private Textbox inputTitleBox;
	@Wire
	private Textbox inputMsgBox;
	@Wire
	private Textbox errorTitleBox;
	@Wire
	private Textbox errorMsgBox;
	private int currentDataRowIndex = 0; //current row index to insert data
	
	private ListModelList<String> fileListModel;
	final private File BOOK_FOLDER = new File(getPage().getDesktop().getWebApp().getRealPath("/book/"));

	@Override
	public void doAfterCompose(Component root) throws Exception {
		super.doAfterCompose(root);
//		enableSocketIOLog();
		initSpreadsheet();
		initControlArea();
		//enable server push to update UI according to keikai async response 
		root.getDesktop().enableServerPush(true);
		addEventListener();
		initCellData();
	}

	private void initCellData() {
		spreadsheet.ready()
		/*
		.thenRun(() -> {
			try {
				importFile("template.xlsx");
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		})
		 */
		.exceptionally((ex) -> {
			System.out.println("Spreadsheet encounters an error: " + ex.getMessage());
			return null;
		});
//		initializeMassiveCells();
	}

	/**
	 * demonstrate the way to import massive data
	 */
	private void initializeMassiveCells() {
		spreadsheet.ready()
		.thenRun(() -> insertDataByRow(200));
		spreadsheet.ready()
		.thenRun(() -> insertDataByRow(200));
		// add more statements of spreadsheet.ready().thenRun()
	}

	private void importFile(String fileName) throws IOException {
		File template = new File(BOOK_FOLDER, fileName);
		spreadsheet.imports(fileName, template);
	}

	/**
	 * add event listeners for spreadsheet
	 */
	private void addEventListener() {
		final Desktop desktop = getSelf().getDesktop();
		ExceptionalConsumer<RangeEvent> listener = (e) -> {
			RangeSelectEvent event = (RangeSelectEvent) e;
			selectedRange = event.getActiveSelection();
			// get value first.
			event.getRange().loadValue()
			.thenApply(activate())
			.thenAccept((rangeValue) -> {
				// ignore validation on null value
				if (rangeValue.getCellValue().isFormula()){
					cellValueBox.setRawValue(rangeValue.getCellValue().getFormula());
				}else{
					cellValueBox.setRawValue(rangeValue.getValue());
				}
				CellValue cellValue = rangeValue.getCellValue();
				cellInfo.setValue(" [formula: " + cellValue.getFormula() +
						", number: " + cellValue.getDoubleValue() +
						", text: " + cellValue.getStringValue() +
						", format: " + cellValue.getFormat() +
						", date: " + cellValue.getDateValue() + "]");
			})
			.whenComplete(deactivate());

			//update control area with server push, since this code is run in a separate thread
			try { 
				Executions.activate(desktop);
				((Label) getSelf().getFellow("msg")).setValue(event.toString());
				String cellAddress = event.getRange().getA1Notation();
				String[] refs = cellAddress.split(":");
				cellAddress = refs.length > 1 ? refs[0].equals(refs[1]) ? refs[0] : cellAddress : cellAddress;

				Label label = ((Label) getSelf().getFellow("cell"));

				if (!label.getValue().equals(cellAddress)) {
					label.setValue(cellAddress);

				}
			} finally {
				Executions.deactivate(desktop);
			}
		};
		
		//register spreadsheet event listeners
		spreadsheet.addEventListener(Events.ON_UPDATE_SELECTION, listener::accept);
		spreadsheet.addEventListener(Events.ON_MOVE_FOCUS, listener::accept);

		ExceptionalConsumer<RangeEvent> keyListener = (e) -> {
			RangeKeyEvent keyEvent = (RangeKeyEvent) e;
			try {
				Executions.activate(desktop);
				//just left-top corner cell
				String range = keyEvent.getRange().getA1Notation().split(":")[0];
				keyCode.setValue(range + "[keyCode=" + keyEvent.getKeyCode() + "], shift: " + keyEvent.isShiftKey()
						+ ", ctrl: " + keyEvent.isCtrlKey() + ", alt: " + keyEvent.isAltKey() + ", meta: "
						+ keyEvent.isMetaKey());
			} finally {
				Executions.deactivate(desktop);
			}
		};
		
		// open a context menu
		ExceptionalConsumer<RangeEvent> mouseListener = (e) -> {
			CellMouseEvent mouseEvent = (CellMouseEvent) e;
			try {
				Executions.activate(desktop);
				contextMenu.open(mouseEvent.getPageX(), mouseEvent.getPageY());
			} finally {
				Executions.deactivate(desktop);
			}
		};
		spreadsheet.addEventListener(Events.ON_KEY_DOWN, keyListener::accept);
		spreadsheet.addEventListener(Events.ON_CELL_RIGHT_CLICK, mouseListener::accept);
	}

	/**
	 * get a spreadsheet java client and render spreadsheet on a browser 
	 */
	private void initSpreadsheet() {
		spreadsheet = Keikai.newClient(Configuration.KEIKAI_SERVER); //connect to keikai server
		//pass target element's id and get keikai script URI
		String scriptUri = spreadsheet.getURI(getSelf().getFellow("myss").getUuid());
		//load the initial script to render spreadsheet at the client
		Script initalScript = new Script();
		initalScript.setSrc(scriptUri);
		initalScript.setDefer(true);
		initalScript.setAsync(true);
		initalScript.setPage(getPage());
		selectedRange = spreadsheet.getRange("A1");
	}

	@SuppressWarnings("unchecked")
	private void initControlArea() {
		borderIndexBox.setModel(new ListModelArray<BorderIndex>(BorderIndex.values()));
		((Selectable<BorderIndex>)borderIndexBox.getModel()).addToSelection(BorderIndex.EdgeBottom);
		
		borderLineStyleBox.setModel(new ListModelArray<Border.Style>(Border.Style.values()));
		((Selectable<Border.Style>)borderLineStyleBox.getModel()).addToSelection(Border.Style.Thin);
		
		filterOperator.setModel(new ListModelArray<AutoFilterOperator>(AutoFilterOperator.values()));
		((Selectable<AutoFilterOperator>) filterOperator.getModel()).addToSelection(AutoFilterOperator.FilterValues);
		
		fontSizeBox.setModel(new ListModelArray<String>(Configuration.fontSizes));
		((Selectable<String>) fontSizeBox.getModel()).addToSelection("12");
		
		fillPatternBox.setModel(new ListModelArray<PatternType>(PatternType.values()));
		((Selectable<PatternType>)fillPatternBox.getModel()).addToSelection(PatternType.Solid);

		fileListModel = new ListModelList(generateBookList());
		filelistBox.setModel(fileListModel);
	}

	private String[] generateBookList() {
		return BOOK_FOLDER.list();
	}

	@Listen("onSelect = #filelistBox")
	public void loadServerFile() throws IOException{
		filePopup.close();
		String fileName = fileListModel.getSelection().iterator().next();
		importFile(fileName);
	}

	/**
	 * TODO implement by changing client URI after 8.5
	 */
	@Listen("onClick = toolbarbutton[iconSclass='z-icon-file']")
	public void newFile(){
		try{
			importFile("blank.xlsx");
		}catch(IOException e){
			Clients.showNotification(e.getMessage());
		}
	}

	@Listen("onClick = toolbarbutton[iconSclass='z-icon-upload']")
	public void openDialog(){
		Fileupload.get(-1);
	}
	
	@Listen("onUpload = #root")
	public void upload(UploadEvent e) throws IOException {
		String name = e.getMedia().getName();
		spreadsheet.imports(name, e.getMedia().getStreamData());
	}	
	
	@Listen("onUpload = #uploader")
	public void uploadFile(UploadEvent event) throws IOException {
		String name = event.getMedia().getName();
		spreadsheet.imports(name, event.getMedia().getStreamData());
	}

	@Listen("onClick = toolbarbutton[iconSclass='z-icon-file-excel-o']")
	public void export() throws FileNotFoundException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		spreadsheet.export(spreadsheet.getActiveWorkbook(), outputStream).whenComplete((a, b) -> {
			try {
				Executions.activate(getSelf().getDesktop());
				Filedownload.save(outputStream.toByteArray(), "application/excel", spreadsheet.getActiveWorkbook());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {

				Executions.deactivate(getSelf().getDesktop());
			}
		});

	}		
	@Listen("onClick = toolbarbutton[iconSclass='z-icon-bold']")
	public void makeBold(){
		Font font = selectedRange.createFont();
		font.setBold(true);
		selectedRange.applyFont(font); 
		/* debug
		range.loadCellStyle().thenAccept((style) ->{
			style.getFont().setBold(true);
			range.applyCellStyle(style);
		});
		CompletableFuture<List<RangeValue>> values = range.loadValues();
		values.thenAccept((vals) -> {
			for (RangeValue v : vals) {
				CellValue cellValue = v.getCellValue();
				System.out.print(v);
				System.out.println(" [formula: " + cellValue.getFormula() +
						", number: " + cellValue.getDoubleValue() +
						", text: " + cellValue.getStringValue() +
						", format: " + cellValue.getFormat() + "]");
			}
		});
		 */
	}

	@Listen("onClick = toolbarbutton[iconSclass='z-icon-italic']")
	public void makeItalic(){
		Font font = selectedRange.createFont();
		font.setItalic(true);
		selectedRange.applyFont(font);
	}	

	@Listen("onClick = toolbarbutton[iconSclass='z-icon-underline']")
	public void makeUnderline(){
		Font font = selectedRange.createFont();
		font.setUnderline(Underline.Single);;
		selectedRange.applyFont(font);
	}
	
	@Listen("onSelect = #fontSizeBox")
	public void changeFontSize(){
		String fontSize = ((Selectable<String>)fontSizeBox.getModel()).getSelection().iterator().next();
		Font font = selectedRange.createFont();
		font.setSize(Integer.valueOf(fontSize));
		selectedRange.applyFont(font);
		/*
		range.loadCellStyle().thenAccept((style) ->{
			style.getFont().setSize(Integer.valueOf(fontSize));			
			range.applyCellStyle(style);
		});
		*/
	}	
	
	@Listen("onClick = #applyFill")
	public void changeFillPattern(Event e){
		PatternFill fill = selectedRange.createPatternFill();
		fill.setPatternType(((Selectable<PatternType>)fillPatternBox.getModel()).getSelection().iterator().next());
		fill.setForegroundColor(((Colorbox)e.getTarget().getFellow("foregroundColorBox")).getValue());
		fill.setBackgroundColor(((Colorbox)e.getTarget().getFellow("backgroundColorBox")).getValue());
		selectedRange.applyFill(fill);
	}
	
	@Listen("onChange = #cellValue")
	public void onClick(Event event) {
		String cellReference = ((Label) getSelf().getFellow("cell")).getValue();
		spreadsheet.getRange(cellReference).applyValue(cellValueBox.getValue());
	}
	
	@Listen("onClick = #clearContents")
	public void clearContents(Event event) {
		selectedRange.clearContents();
	}
	
	@Listen("onClick = toolbarbutton[label='wrap']")
	public void wrap(){
		selectedRange.applyWrapText(true);;
	}	

	@Listen("onChange = #focusTo")
	public void onChange(Event event) {
		String cellReference = ((Textbox) event.getTarget()).getText();
		spreadsheet.getRange(cellReference).activate();
	}

	@Listen("onClick = #focusCell")
	public void focusCell(Event event) {
		spreadsheet.loadActiveCell()
			.thenAccept(AsyncRender.acceptUpdate((Range range) ->{
				//implement logic to update ZK components
				Clients.showNotification("Focus Cell: " + range.getA1Notation());
			})).exceptionally((ex) ->{
				//handle an exception
				ex.printStackTrace();
				return null;
			});
	}



	@Listen("onClick = #applyBorder")
	public void applyBorders(Event evt) {
		BorderIndex borderIndex = (BorderIndex) borderIndexBox.getModel().getElementAt(borderIndexBox.getSelectedIndex());
		Borders borders = selectedRange.createBorders(borderIndex);

		Border.Style borderLineStyle = (Border.Style)borderLineStyleBox.getModel().getElementAt(borderLineStyleBox.getSelectedIndex());
		borders.setStyle(borderLineStyle);
		
		borders.setColor(borderColorBox.getValue());
		selectedRange.applyBorders(borders);
		
		//debug 
		selectedRange.loadCellStyle()
			.thenAccept((cellStyle -> {
				System.out.println(cellStyle.getBorders().toString());
			}));
	}

	@Listen("onClick = #clearBorder")
	public void clearBorders(Event evt) {
		selectedRange.clearBorders();
	}

	@Listen("onClick = #filterClear")
	public void clearFilter() {
		selectedRange.clearAutoFilter();
	}

	@Listen("onClick = #filterBtn")
	public void applyFilter(Event e) {
		Integer field = filterField.getValue();
		String fcStr1 = filterCriteria1.getText();
		Object criterial1 = null;
		if (fcStr1 != null && fcStr1.contains(",")) {
			criterial1 = fcStr1.split(",");
		} else {
			criterial1 = fcStr1.isEmpty() ? null : fcStr1;
		}

		selectedRange.applyAutoFilter(field, criterial1,
				((Selectable<AutoFilterOperator>)filterOperator.getModel()).getSelection().iterator().next(), null, true);
		filterPopup.close();
	}

	@Listen("onClick=#addMore")
	public void insertData() {
		Clients.showBusy("send data...");
		insertDataByRow(100);
//		spreadsheet.ready(() -> {
//			try {
//				Executions.activate(getSelf().getDesktop());
//				Clients.clearBusy();
//			} catch (Exception e) {
//				e.printStackTrace();
//			} finally {
//				Executions.deactivate(getSelf().getDesktop());
//			}
//		});
		spreadsheet.ready(AsyncRender.runUpdate(() -> {
			Clients.clearBusy();
		}));
	}

	private void insertDataByRow(int row) {
		for (int j = 0; j < row; j++) {
			for (int i = 0; i < MAX_COLUMN; i++) {
				spreadsheet.getRange(currentDataRowIndex, i).applyValue(numToAbc(i) + (currentDataRowIndex + 1));
			}
			currentDataRowIndex++;
		}
	}

	@Listen("onClick = #applyFormat")
	public void applyFormat(Event e) {
		selectedRange.applyNumberFormat(((Textbox) e.getTarget().getFellow("numfmt")).getText());
		((Popup)e.getTarget().getFellow("formatPopup")).close();
	}

	@Listen("onClick = #applyValidation")
	public void applyValidation(Event e){
		DataValidation validation = selectedRange.createDataValidation();
		validation.setFormula1(validationFormulaBox.getValue());
		validation.setType(DataValidation.Type.List); //currently-supported type
		validation.setAlertStyle(DataValidation.AlertStyle.Stop);
		validation.setInputTitle(inputTitleBox.getValue());
		validation.setInputMessage(inputMsgBox.getValue());
		validation.setErrorTitle(errorTitleBox.getValue());
		validation.setErrorMessage(errorMsgBox.getValue());
		selectedRange.applyDataValidation(validation);
		((Popup)e.getTarget().getFellow("validationPopup")).close();
	}
	
	/**
	 * an example of synchronous-style API usage, just calling get().
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	@Listen("onClick = #showStyle")
	public void showCellStyle() throws InterruptedException, ExecutionException{
		CellStyle style = selectedRange.loadCellStyle().get();
		Clients.showNotification(style.toString());
	}
	
	private void enableSocketIOLog() {
		Logger log = java.util.logging.Logger.getLogger("");
		log.setLevel(Level.FINER);

		ConsoleHandler handler = new ConsoleHandler();
		handler.setFormatter(new SimpleFormatter());
		handler.setLevel(Level.ALL);
		log.addHandler(handler);
	}	
	
	public <T> BiConsumer<T, ? super Throwable> deactivate() {
		Desktop desktop = getSelf().getDesktop();
		return (v, ex) -> {
			if (ex != null) {
				ex.printStackTrace();
			}
			Executions.deactivate(desktop);
		};
	}

	/**
	 * Returns a function that activate a desktop and handles exceptions
	 * @return
	 */
	public <T> ExceptionableFunction<T, T> activate() {
		Desktop desktop = getSelf().getDesktop();
		return result -> {
			Executions.activate(desktop);
			return result;
		};
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
