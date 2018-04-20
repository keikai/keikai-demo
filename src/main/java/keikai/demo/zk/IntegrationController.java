package keikai.demo.zk;

import io.keikai.client.api.*;
import io.keikai.client.api.event.Events;
import keikai.demo.Configuration;
import org.zkoss.chart.Charts;
import org.zkoss.gmaps.*;
import org.zkoss.zhtml.Script;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Wire;

import java.io.File;
import java.text.NumberFormat;
import java.text.*;

public class IntegrationController extends SelectorComposer<Component> {
	
	private Spreadsheet fluSpreadsheet;
	private Range selectedRange;

	@Wire
	private Gmaps fluMap;
	
	@Wire
	private Charts fluChart;
	

	private Gmarker[] gmarkerArray;
	private final static int NUMBER_OF_GMARKER_ROW = 42;
	private int row, col;
	private String prevCellValue;
	private NumberFormat format;

	public void doAfterCompose(Component comp) throws Exception {
		super.doAfterCompose(comp);
		initSpreadsheet();
		registerListeners();
		gmarkerArray = new Gmarker[NUMBER_OF_GMARKER_ROW];
//		initChart();
//		updateChart();
//		initMap();
	}


	/* get a spreadsheet java client and getUpdateRunner spreadsheet on a browser
	 */
	private void initSpreadsheet() {
		fluSpreadsheet = Keikai.newClient(Configuration.DEMO_SERVER); //connect to keikai server
		getPage().getDesktop().setAttribute(SpreadsheetCleanUp.SPREADSHEET, fluSpreadsheet); //make spreadsheet get closed
		//pass target element's id and get keikai script URI
		String scriptUri = fluSpreadsheet.getURI(getSelf().getFellow("fluSpreadsheet").getUuid());
		//load the initial script to getUpdateRunner spreadsheet at the client
		Script initialScript = new Script();
		initialScript.setSrc(scriptUri);
		initialScript.setDefer(true);
		initialScript.setAsync(true);
		initialScript.setPage(getPage());
		selectedRange = fluSpreadsheet.getRange("A1");

		String fileName = "swineFlu.xlsx";
		File file = new File(WebApps.getCurrent().getRealPath(Configuration.DEMO_BOOK_PATH), fileName);
		fluSpreadsheet.imports(fileName, file);
	}

	private void registerListeners() {
		fluSpreadsheet.addEventListener(Events.ON_CELL_CLICK, (event) ->{

		});
	}

	/*
	@Listen("onCellFocus = #fluSpreadsheet")
	public void onFocus(CellEvent event) throws ParseException {
		if (fluMap == null || sheet == null)
			return;

		Sheet sheet = event.getSheet();
		row = event.getRow();
		col = event.getColumn();
		prevCellValue = Ranges.range(sheet, row, col).getCellEditText();
		if (row < 2 || row > 41)// the header row
			return;

		double lat = parseDouble(Ranges.range(sheet, row, 4).getCellEditText());
		double lng = parseDouble(Ranges.range(sheet, row, 5).getCellEditText());

		fluMap.setLat(lat);
		fluMap.setLng(lng);
		for (Gmarker gmarker : gmarkerArray) {
			if (gmarker != null && gmarker.isOpen())
				gmarker.setOpen(false);
		}
		gmarkerArray[row].setOpen(true);
	}

	@Listen("onEditboxEditing = #fluSpreadsheet")
	public void onEditboxEditingEvent(EditboxEditingEvent event)
			throws ParseException {
		if (sheet == null || fluMap == null)
			return;

		String str = (String) event.getEditingValue();
		if (col != 1 && col != 2) {
			setCellEditText(sheet, row, col, str);
		}
		if (row > 0 && row < NUMBER_OF_GMARKER_ROW) {
			updateRow(row, false);
		}
	}

	@Listen("onStopEditing = #fluSpreadsheet")
	public void onStopEditingEvent(StopEditingEvent event)
			throws ParseException {
		if (sheet == null || fluChart == null)
			return;
		event.cancel();// set data manually;
		row = event.getRow();
		col = event.getColumn();
		String str = (String) event.getEditingValue();
		if (col == 1 || col == 2) {
			Double val = null;
			try {
				val = format.parse(str).doubleValue();
				setCellEditText(sheet, row, col, str);
			} catch (ParseException e) {
				final Integer rowIdx = Integer.valueOf(row);
				final Integer colIdx = Integer.valueOf(col);
				final String prevValue = prevCellValue;
				Messagebox.show("Cell value has to be number format", "Error",
						Messagebox.OK, Messagebox.EXCLAMATION,
						new SerializableEventListener<Event>() {
							private static final long serialVersionUID = 846217931006240721L;

							public void onEvent(Event event) throws Exception {
								setCellEditText(sheet, rowIdx, colIdx,
										prevValue);
							}
						});
				return;
			}
		} else {
			setCellEditText(sheet, row, col, str);
		}
		if (row != 0) {// the header row
			updateRow(row, true);
		}
		if (col == 1) {
			updateChart();
		}
	}
	
	@Listen("onMapClick = #fluMap")
	public void selectMarker(MapMouseEvent event) {
		Gmarker marker = event.getGmarker();
		if (marker != null) {
			for (Gmarker gmarker : gmarkerArray) {
				if (gmarker != null){
					if (marker == gmarker) {
						gmarker.setOpen(!marker.isOpen());
					} else if (gmarker.isOpen()) {
						gmarker.setOpen(false);
					}
				}
			}
		}
	}
	
	private void initMap() {
		for (int row = 2; row < NUMBER_OF_GMARKER_ROW; row++) {
			String state = Ranges.range(sheet, row, 0).getCellEditText();
			// String division = sheet.getCell(row, 1).getCellEditText();

			int numOfCase = parseInt(Ranges.range(sheet, row, 1)
					.getCellEditText());

			int numOfDeath = parseInt(Ranges.range(sheet, row, 2)
					.getCellEditText());

			String description = Ranges.range(sheet, row, 3).getCellEditText();
			double lat = parseDouble(Ranges.range(sheet, row, 4)
					.getCellEditText());
			double lng = parseDouble(Ranges.range(sheet, row, 5)
					.getCellEditText());
			String content = "<span style=\"color:#346b93;font-weight:bold\">" +
					state + 
					"</span><br/><span style=\"color:red\">" +
					numOfCase +
					"</span> cases<br/><span style=\"color:red\">" +
					numOfDeath +
					"</span> death<div style=\"background-color:#E8F5Cf;padding:2px\">" +
					description + "</div>";

			Gmarker gmarker = new Gmarker();
			gmarkerArray[row] = gmarker;
			gmarker.setLat(lat);
			gmarker.setLng(lng);
			gmarker.setContent(content);
			fluMap.appendChild(gmarker);
			
			if (row == 2) {
				fluMap.setLat(lat);
				fluMap.setLng(lng);
				gmarkerArray[row].setOpen(true);
			}
			fluMap.setZoom(5);
		}
	}

	private void initChart() {
		if (fluChart == null)
			return;
		fluChart.setTitle("");
		fluChart.getExporting().setEnabled(false);
		fluChart.getPlotOptions().getPie().getDataLabels().setEnabled(false);
		fluChart.getPlotOptions().getPie().setShowInLegend(true);
		fluChart.setModel(new DefaultPieModel());
	}

	private void updateChart() {
		if (fluChart == null || sheet == null)
			return;

		PieModel model = (PieModel) fluChart.getModel();
		model.clear();
		for (int row = 45; row < 53; row++) {
			String name = Ranges.range(sheet, row, 0).getCellData().isBlank() ? ""
					: Ranges.range(sheet, row, 0).getCellEditText();
			CellData cd = Ranges.range(sheet, row, 1).getCellData();
			Double value;
			if (cd.getResultType() == CellType.NUMERIC) {
				value = (Double) cd.getValue();
			} else {
				value = 0D;
			}
			model.setValue(name, value);
		}
	}

	private void updateRow(int row, boolean evalValue) throws ParseException {
		if (fluMap == null || sheet == null
				|| Ranges.range(sheet, row, 3).getCellData().isBlank())
			return;

		String state = Ranges.range(sheet, row, 0).getCellEditText();
		int numOfCase = parseInt(Ranges.range(sheet, row, 1).getCellEditText());
		int numOfDeath = parseInt(Ranges.range(sheet, row, 2).getCellEditText());

		String description = Ranges.range(sheet, row, 3).getCellEditText();

		double lat = parseDouble(Ranges.range(sheet, row, 4).getCellEditText());
		double lng = parseDouble(Ranges.range(sheet, row, 5).getCellEditText());
		String content = "<span style=\"color:#346b93;font-weight:bold\">" +
				state +
				"</span><br/><span style=\"color:red\">" +
				numOfCase +
				"</span> cases<br/><span style=\"color:red\">" +
				numOfDeath +
				"</span> death<div style=\"background-color:#E8F5Cf;padding:2px\">" +
				description + "</div>";

		gmarkerArray[row].setContent(content);
		fluMap.setLat(lat);
		fluMap.setLng(lng);
		gmarkerArray[row].setOpen(true);
	}
	
	private void setCellEditText(Sheet sheet, int row, int col, String text) {
		try {
			Ranges.range(sheet, row, col).setCellEditText(text);
		} catch (IllegalFormulaException x) {
		}
	}
	*/

	private double parseDouble(String text) {
		try {
			return format.parse(text).doubleValue();
		} catch (ParseException e) {
			return 0D;
		}
	}

	private int parseInt(String text) {
		try {
			return format.parse(text).intValue();
		} catch (ParseException e) {
			return 0;
		}
	}
}