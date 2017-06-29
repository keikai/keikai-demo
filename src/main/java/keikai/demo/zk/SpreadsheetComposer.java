/* KKComposer.java

	Purpose:
		
	Description:
		
	History:
		4:50 PM 16/05/2017, Created by jumperchen

Copyright (C) 2017 Potix Corporation. All Rights Reserved.
*/
package keikai.demo.zk;

import static com.keikai.util.Converter.numToAbc;

import java.io.*;
import java.util.function.*;
import java.util.logging.*;

import keikai.demo.Configuration;

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
import com.keikai.client.api.Fill.PatternFill;
import com.keikai.client.api.event.*;
import com.keikai.client.api.event.Events;

/**
 * @author Hawk
 */
public class SpreadsheetComposer extends SelectorComposer<Component> {
	private Spreadsheet spreadsheet;
	private String selectedRange = "A1"; //cell reference

	@Wire
	private Selectbox borderIndexBox;
	@Wire
	private Selectbox borderLineStyleBox;

	@Wire
	private Label keyCode;

	// filter
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
	private Popup contextMenu;
	@Wire 
	private Label contextMsg;
	@Wire
	private Selectbox fillPatternBox;


	private int dataRowIndex = 0;

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
		initControlArea();
		//enable server push to update UI according to keikai async response 
		comp.getDesktop().enableServerPush(true);
		initSpreadsheet();
		addEventListener();
		initCellData();
	}

	private void initCellData() {
		/*
		spreadsheet.ready(() -> {
			long t1 = System.nanoTime();
			int total = 0;
			for (int k = 0; k < 1; k++) {
				for (int row = 0; row < 30; row++) {
					for (int col = 0; col < 10; col++) {
						total++;
						spreadsheet.getRange(row, col).applyValue(numToAbc(col) + (dataRowIndex + 1));
					}
					dataRowIndex++;
				}
			}
			System.out.println("Sending time: "
					+ TimeUnit.MILLISECONDS.convert(System.nanoTime() - t1, TimeUnit.NANOSECONDS) + "ms"
					+ " for "+total+" cells");
		});
		*/
		spreadsheet.ready()
//		.thenRun(() -> {
//			try {
//				importTemplate();
//			} catch (Exception e) {
//				throw new RuntimeException(e);
//			}
//		})
		.exceptionally((ex) -> {
			System.out.println("Spreadsheet encounters an error: " + ex.getMessage());
			return null;
		});
	}

	private void importTemplate() throws IOException {
		File template = new File(WebApps.getCurrent().getRealPath("/book/template.xlsx"));
		spreadsheet.imports("template.xlsx", template);
	}

	/**
	 * add event listeners for spreadsheet
	 */
	private void addEventListener() {
		final Desktop desktop = getSelf().getDesktop();
		ThrowingFunction listener = (e) -> {
			RangeSelectEvent event = (RangeSelectEvent) e;
			selectedRange = event.getActiveSelection().getA1Notation(); //get cell reference string
			// get value first.
			event.getRange().loadValue()
			.thenApply(activate())
			.thenAccept((rangeValue) -> {
				// ignore validation on null value
				cellValueBox.setRawValue(rangeValue.getValue());
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
				String a1 = event.getRange().getA1Notation();
				String[] refs = a1.split(":");
				a1 = refs.length > 1 ? refs[0].equals(refs[1]) ? refs[0] : a1 : a1;

				Label label = ((Label) getSelf().getFellow("cell"));

				if (!label.getValue().equals(a1)) {
					label.setValue(a1);

				}
			} finally {
				Executions.deactivate(desktop);
			}
		};
		
		//register spreadsheet event listeners
		spreadsheet.addEventListener(Events.ON_UPDATE_SELECTION, listener::accept);
		spreadsheet.addEventListener(Events.ON_MOVE_FOCUS, listener::accept);

		ThrowingFunction keyListener = (e) -> {
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
		ThrowingFunction mouseListener = (e) -> {
			CellMouseEvent mouseEvent = (CellMouseEvent) e;
			try {
				Executions.activate(desktop);
				String range = mouseEvent.getRange().getA1Notation();
				contextMsg.setValue("Cell: " + range);
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
		String scriptUri = spreadsheet.getURI(getSelf().getFellow("mywin").getFellow("myss").getUuid());
		//load the initial script to render spreadsheet at the client
		Script initalScript = new Script();
		initalScript.setSrc(scriptUri);
		initalScript.setDefer(true);
		initalScript.setAsync(true);
		initalScript.setPage(getPage());
	}

	private void initControlArea() {
		borderIndexBox.setModel(new ListModelArray<Object>(Configuration.borderIndexList));
		((Selectable)borderIndexBox.getModel()).addToSelection(Configuration.borderIndexList[3]);
		
		borderLineStyleBox.setModel(new ListModelArray<Object>(Configuration.borderLineStyleList));
		((Selectable)borderLineStyleBox.getModel()).addToSelection(Configuration.borderLineStyleList[1]);
		
		filterOperator.setModel(new ListModelArray<Object>(Configuration.autoFilterList));
		((Selectable) filterOperator.getModel()).addToSelection(Configuration.autoFilterList[7]);
		
		fontSizeBox.setModel(new ListModelArray<String>(Configuration.fontSizes));
		((Selectable) fontSizeBox.getModel()).addToSelection("12");
		
		fillPatternBox.setModel(new ListModelArray<Object>(Configuration.fillPatternTypes));
		((Selectable<String>)fillPatternBox.getModel()).addToSelection(Configuration.fillPatternTypes[1]);
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
	//FIXME how to change the style?
	@Listen("onClick = toolbarbutton[iconSclass='z-icon-bold']")
	public void makeBold(){
		Range range = spreadsheet.getRange(selectedRange);
		Font font = range.createFont();
		font.setBold(true);
		range.applyFont(font); 
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
	//FIXME
	@Listen("onClick = toolbarbutton[iconSclass='z-icon-italic']")
	public void makeItalic(){
		Range range = spreadsheet.getRange(selectedRange);
		Font font = range.createFont();
		font.setItalic(true);
		range.applyFont(font);
	}	
	
	//FIXME
	@Listen("onClick = toolbarbutton[iconSclass='z-icon-underline']")
	public void makeUnderline(){
		Range range = spreadsheet.getRange(selectedRange);
		Font font = range.createFont();
		font.setUnderline("single");;
		range.applyFont(font);
	}	
	
	//FIXME
	@Listen("onSelect = #fontSizeBox")
	public void changeFontSize(){
		Range range = spreadsheet.getRange(selectedRange);
		String fontSize = ((Selectable<String>)fontSizeBox.getModel()).getSelection().iterator().next();
		Font font = range.createFont();
		font.setSize(Integer.valueOf(fontSize));
		range.applyFont(font);
		/*
		range.loadCellStyle().thenAccept((style) ->{
			style.getFont().setSize(Integer.valueOf(fontSize));			
			range.applyCellStyle(style);
		});
		*/
	}	
	
	@Listen("onSelect = #fillPatternBox")
	public void changeFillPattern(){
		Range range = spreadsheet.getRange(selectedRange);
		PatternFill fill = range.createPatternFill();
		fill.setPatternType(((Selectable<String>)fillPatternBox.getModel()).getSelection().iterator().next());
		fill.setForegroundColor("#363636");
		fill.setBackgroundColor("#363636");
		range.applyFill(fill);
	}
	
	@Listen("onChange = #cellValue")
	public void onClick(Event event) {
		String cellReference = ((Label) getSelf().getFellow("cell")).getValue();
		spreadsheet.getRange(cellReference).applyValue(cellValueBox.getValue());
	}
	
	@Listen("onClick = #clearContents")
	public void clearContents(Event event) {
		spreadsheet.getRange(selectedRange).clearContents();
	}
	
	@Listen("onClick = toolbarbutton[label='wrap']")
	public void wrap(){
		Range range = spreadsheet.getRange(selectedRange);
		range.applyWrapText(true);;
	}	

	@Listen("onChange = #focusTo")
	public void onChange(Event event) {
		String cellReference = ((Textbox) event.getTarget()).getText();
		spreadsheet.getRange(cellReference).activate();
	}

	@Listen("onClick = #focusCell")
	public void focusCell(Event event) {
		spreadsheet.loadActiveCell()
			.thenApply(activate())
			.thenAccept((range) -> {
				Clients.showNotification("Focus Cell: " + range.getA1Notation());
			})
			.whenComplete(deactivate());
	}



	@Listen("onClick = #applyBorder")
	public void applyBorders(Event evt) {
		Range range = spreadsheet.getRange(selectedRange);
		String borderIndex = (String) borderIndexBox.getModel().getElementAt(borderIndexBox.getSelectedIndex());
		Borders borders = range.createBorders("none".equals(borderIndex) ? null : borderIndex);

		String borderLineStyle = borderLineStyleBox.getSelectedIndex() > -1
				? (String) borderLineStyleBox.getModel().getElementAt(borderLineStyleBox.getSelectedIndex()) : null;
		borders.setStyle(borderLineStyle);
		
		borders.setColor(borderColorBox.getValue());
		
		range.applyBorders(borders);
		
		//debug 
		range.loadCellStyle()
			.thenAccept((cellStyle -> {
				System.out.println(cellStyle.getBorders().toString());
			}));
	}

	//FIXME has bug
	@Listen("onClick = #clearBorder")
	public void clearBorders(Event evt) {
		spreadsheet.getRange(selectedRange).clearBorders();
	}

	@Listen("onClick = #filterClear")
	public void clearFilter() {
		spreadsheet.getRange(selectedRange).clearAutoFilter();
	}

	@Listen("onClick = #filterBtn")
	public void applyFilter(Event evt) {
		Integer field = filterField.getValue();
		String fcStr1 = filterCriteria1.getText();
		Object criterial1 = null;
		if (fcStr1 != null && fcStr1.contains(",")) {
			criterial1 = fcStr1.split(",");
		} else {
			criterial1 = fcStr1.isEmpty() ? null : fcStr1;
		}

		spreadsheet.getRange(selectedRange).applyAutoFilter(field, criterial1,
				Configuration.autoFilterList[filterOperator.getSelectedIndex()], null, true);
	}

	@Listen("onClick=#addMore")
	public void addMore() {
		Clients.showBusy("send data...");
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 100; j++) {
				spreadsheet.getRange(dataRowIndex, j).applyValue(numToAbc(j) + (dataRowIndex + 1));
			}
			dataRowIndex++;
		}
		spreadsheet.ready(() -> {
			try {
				Executions.activate(getSelf().getDesktop());
				Clients.clearBusy();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				Executions.deactivate(getSelf().getDesktop());
			}
		});
	}

	@Listen("onClick = #applyFormat")
	public void applyFormat(Event evt) {
		Range range = spreadsheet.getRange(selectedRange);
		range.applyNumberFormat(((Textbox) evt.getTarget().getFellow("numfmt")).getText());
	}

	@Listen("onClick = #isReady")
	public void isReady() {
		spreadsheet.isReady().thenApply(activate()).thenAccept(isReady -> {
			Clients.showNotification("isReady > " + isReady);
		}).whenComplete(deactivate());
	}
	
	private <T> ExceptionableFunction<T, T> activate() {
		Desktop desktop = getSelf().getDesktop();
		return result -> {
			Executions.activate(desktop);
			return result;
		};
	}

	private <T> BiConsumer<T, ? super Throwable> deactivate() {
		Desktop desktop = getSelf().getDesktop();
		return (v, ex) -> {
			if (ex != null) {
				ex.printStackTrace();
			}
			Executions.deactivate(desktop);
		};
	}

	public interface ExceptionableFunction<T, R> extends Function<T, R> {
		default R apply(T val) {
			try {
				return applyWithException(val);
			} catch (Exception e) {
				throw new RuntimeException("wrapped", e);
			}
		}

		R applyWithException(T val) throws Exception;
	}
	
	/**
	 * Since lambda can't throw exception so we need to catch it and throw an unchecked exception
	 * @author hawk
	 *
	 */
	public interface ThrowingFunction extends Consumer<RangeEvent>{
		default void accept(RangeEvent event){
			try {
				acceptWithException(event);
			} catch (Exception e) {
				throw new RuntimeException("wrapped", e);
			}
		}

		void acceptWithException(RangeEvent event) throws Exception;
	}
}
