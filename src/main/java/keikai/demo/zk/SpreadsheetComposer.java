/* KKComposer.java

	Purpose:
		
	Description:
		
	History:
		4:50 PM 16/05/2017, Created by jumperchen

Copyright (C) 2017 Potix Corporation. All Rights Reserved.
*/
package keikai.demo.zk;

import static com.keikai.util.Converter.numToAbc;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.logging.*;

import kk.socket.thread.EventThread;

import org.zkoss.zhtml.Script;
import org.zkoss.zk.device.*;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.*;
import org.zkoss.zul.ext.Selectable;

import com.keikai.client.api.*;
import com.keikai.client.api.event.*;
import com.keikai.client.api.event.Events;

/**
 * @author jumperchen, Hawk
 */
public class SpreadsheetComposer extends SelectorComposer<Component> {
	private static final String KEIKAI_SERVER = "http://114.34.173.199:8888";
	private int counts;
	private Spreadsheet spreadsheet;
	private String selectedRange = "A1"; //cell reference

	@Wire
	private Selectbox borderIndexBox;
	@Wire
	private Selectbox borderLineStyleBox;
	@Wire
	private Selectbox borderWeightBox;

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
	private Textbox filterCriteria2;
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
	private Popup contextMenu;
	@Wire 
	private Label contextMsg;


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
		comp.getDesktop().enableServerPush(true);
		initSpreadsheet();
		addEventListener();
		initCellData();
		
	}

	private void initCellData() {
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
	}

	/**
	 * add event listeners for spreadsheet
	 */
	private void addEventListener() {
		final Desktop desktop = getSelf().getDesktop();
		MyEventListener listener = (e) -> {
			RangeSelectEvent event = (RangeSelectEvent) e;
			selectedRange = event.getActiveSelection().getA1Notation(); //get cell reference string
			// get value first.
			event.getRange().loadValue().thenApply(activate()).thenAccept((rangeValue) -> {
				// ignore validation on null value
				cellValueBox.setRawValue(rangeValue.getValue());
				CellValue cellValue = rangeValue.getCellValue();
				cellInfo.setValue(" [formula: " + cellValue.getFormula() +
						", number: " + cellValue.getDoubleValue() +
						", text: " + cellValue.getStringValue() +
						", format: " + cellValue.getFormat() +
						", date: " + cellValue.getDateValue() + "]");
			}).whenComplete(deactivate());

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

		MyEventListener keyListener = (e) -> {
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
		
		/*
		 * open a context menu
		 */
		MyEventListener mouseListener = (e) -> {
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
		spreadsheet = Keikai.newClient(KEIKAI_SERVER); //connect to keikai server
		//pass target element's id and get keikai script URI
		String scriptUri = spreadsheet.getURI(getSelf().getFellow("mywin").getFellow("myss").getUuid());
		//load the initial script to render spreadsheet at the client
		Script initalScript = new Script();
		initalScript.setSrc(scriptUri);
		initalScript.setDefer(true);
		initalScript.setAsync(true);
		initalScript.setPage(getPage());
	}

	static String[] borderIndexList = { "none", "diagonalDown", "diagonalUp", "edgeBottom", "edgeLeft", "edgeRight", "edgeTop",
			"insideHorizontal", "insideVertical" };
	static String[] borderLineStyleList = { "continuous", "dash", "dashDot", "dashDotDot", "dot", "double", "none",
			"slantDashDot" };
	static String[] autoFilterList = { "and", "bottom10Items", "bottom10Percent", "filterCellColor", "filterDynamic",
			"filterFontColor", "filterIcon", "filterValues", "top10Items", "top10Percent" };
	static String[] borderWeightList = { "hairline", "medium", "thick", "thin" };

	private void initControlArea() {
		borderIndexBox.setModel(new ListModelArray<Object>(borderIndexList));
		borderIndexBox.setSelectedIndex(0);
		borderLineStyleBox.setModel(new ListModelArray<Object>(borderLineStyleList));
		borderLineStyleBox.addEventListener("onSelect", (evt) -> {
			((Selectable)borderWeightBox.getModel()).clearSelection();
		});

		borderWeightBox.setModel(new ListModelArray<Object>(borderWeightList));
		borderWeightBox.addEventListener("onSelect", (evt) -> {
			((Selectable)borderLineStyleBox.getModel()).clearSelection();
		});
		filterOperator.setModel(new ListModelArray<Object>(autoFilterList));
		((Selectable) filterOperator.getModel()).addToSelection(autoFilterList[7]);
	}

	@Listen("onClick = #enter")
	public void onClick(Event event) {
		String cellReference = ((Label) getSelf().getFellow("cell")).getValue();
		String cellValue = ((Textbox) event.getTarget().getFellow("cellValue")).getValue();
		spreadsheet.getRange(cellReference).applyValue(cellValue);
	}

	@Listen("onChange = #focusTo")
	public void onChange(Event event) {
		String cellReference = ((Textbox) event.getTarget()).getText();
		spreadsheet.getRange(cellReference).activate();
	}

	@Listen("onClick = #focusCell")
	public void focusCell(Event event) {
		spreadsheet.loadActiveCell().thenApply(activate()).thenAccept((range) -> {
			Clients.showNotification("Focus Cell: " + range.getA1Notation());
		}).whenComplete(deactivate());
	}


	@Listen("onClick = #clearContents")
	public void clearContents(Event event) {
		spreadsheet.getRange(selectedRange).clearContents();
	}

	@Listen("onUpload = #uploader")
	public void uploadFile(UploadEvent event) throws IOException {
		String name = event.getMedia().getName();
		spreadsheet.imports(name, event.getMedia().getStreamData());
	}
	@Listen("onClick = #applyBorder")
	public void applyBorders(Event evt) {
		String borderIndex = (String) borderIndexBox.getModel().getElementAt(borderIndexBox.getSelectedIndex());
		String borderLineStyle = borderLineStyleBox.getSelectedIndex() > -1
				? (String) borderLineStyleBox.getModel().getElementAt(borderLineStyleBox.getSelectedIndex()) : null;
		String borderWeight = borderWeightBox.getSelectedIndex() > -1
				? (String) borderWeightBox.getModel().getElementAt(borderWeightBox.getSelectedIndex()) : null;
		String color = ((Textbox) evt.getTarget().getFellow("borderColor")).getText();
		Range range = spreadsheet.getRange(selectedRange);
		Borders borders = range.createBorders("none".equals(borderIndex) ? null : borderIndex);
		if (borderLineStyle != null) {
			borders.setStyle(borderLineStyle);
		}
		if (borderWeight != null) {
			borders.setStyle(borderWeight);
		}
		borders.setColor(color);
		range.applyBorders(borders);
		CompletableFuture<CellStyle> cellStyle = range.loadCellStyle();
		cellStyle.thenAccept((cellStyle1 -> {
			System.out.println(cellStyle1.getBorders().toString());
		}));
	}

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
		String fcStr2 = filterCriteria2.getText();
		Object criterial1 = null;
		Object criterial2 = null;
		if (fcStr1 != null && fcStr1.contains(",")) {
			criterial1 = fcStr1.split(",");
		} else {
			criterial1 = fcStr1.isEmpty() ? null : fcStr1;
		}
		if (fcStr2 != null && fcStr2.contains(",")) {
			criterial2 = fcStr2.split(",");
		} else {
			criterial2 = fcStr2.isEmpty() ? null : fcStr2;
		}

		spreadsheet.getRange(selectedRange).applyAutoFilter(field, criterial1,
				autoFilterList[filterOperator.getSelectedIndex()], criterial2, filterDropDown.getSelectedIndex() == 0);
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

	@Listen("onClick = #applyFont")
	public void applyFont() {
		Range range = spreadsheet.getRange(selectedRange);
		CellStyle cellStyle = range.createCellStyle();
		Font font = cellStyle.createFont();
		font.setBold(true);
		font.setName("Calibri");
		font.setSize(20);
		cellStyle.setFont(font);
		range.applyCellStyle(cellStyle);


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
	public interface MyEventListener extends Consumer<RangeEvent>{
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
