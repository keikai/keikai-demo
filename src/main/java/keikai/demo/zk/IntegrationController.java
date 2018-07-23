package keikai.demo.zk;

import io.keikai.client.api.*;
import io.keikai.client.api.event.*;
import keikai.demo.*;
import org.zkoss.chart.Charts;
import org.zkoss.chart.model.*;
import org.zkoss.gmaps.*;
import org.zkoss.gmaps.event.MapMouseEvent;
import org.zkoss.zhtml.Script;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zul.Div;

import java.io.*;
import java.text.NumberFormat;

public class IntegrationController extends SelectorComposer<Component> {

    private Spreadsheet fluSpreadsheet;

    @Wire
    private Div spreadsheetBlock;
    @Wire
    private Gmaps fluMap;
    @Wire
    private Charts fluChart;


    private Gmarker[] gmarkerArray;
    private final static int NUMBER_OF_GMARKER_ROW = 42;
    private static final int LATITUDE_COLUMN = 4;
    private static final int LONGITUDE_COLUMN = 5;

    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        getPage().getDesktop().enableServerPush(true);
        initSpreadsheet();
        registerListeners();
        gmarkerArray = new Gmarker[NUMBER_OF_GMARKER_ROW];
        initChart();
        refreshChartData();
        initMapMarkers();
    }


    /* get a spreadsheet java client and getUpdateRunner spreadsheet on a browser
     */
    private void initSpreadsheet() {
        fluSpreadsheet = Keikai.newClient(Configuration.DEMO_SERVER); //connect to keikai server
        getPage().getDesktop().setAttribute(SpreadsheetCleanUp.SPREADSHEET, fluSpreadsheet); //make spreadsheet get closed
        //pass target element's id and get keikai script URI
        String scriptUri = fluSpreadsheet.getURI(spreadsheetBlock.getUuid());
        //load the initial script to getUpdateRunner spreadsheet at the client
        Script initialScript = new Script();
        initialScript.setSrc(scriptUri);
        initialScript.setDefer(true);
        initialScript.setAsync(true);
        initialScript.setPage(getPage());

        String fileName = "swineFlu.xlsx";
        File file = new File(WebApps.getCurrent().getRealPath(Configuration.DEMO_BOOK_PATH), fileName);
        final Desktop desktop = getPage().getDesktop();
        try {
            fluSpreadsheet.imports(fileName, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerListeners() {
        fluSpreadsheet.addEventListener(Events.ON_CELL_CLICK, (RangeEvent event) -> {
            int row = event.getRange().getRow();
            if (row < 2 || row > 41) // check valid rows
                return;
            Double latitude = fluSpreadsheet.getRange(row, LATITUDE_COLUMN).getRangeValue().getCellValue().getDoubleValue();
            Double longitude = fluSpreadsheet.getRange(row, LONGITUDE_COLUMN).getRangeValue().getCellValue().getDoubleValue();
            AsyncRender.runUpdate(getPage().getDesktop(), () -> {
                openMarker(row, latitude, longitude);
            });

        });

        fluSpreadsheet.addEventListener(Events.ON_EDIT_SAVE, (RangeEvent event) -> {
            AsyncRender.runUpdate(getPage().getDesktop(), () -> {
                refreshChartData();
            });
        });
    }


    private void initChart() {
        if (fluChart == null)
            return;
        fluChart.setTitle("Swine Flu Cases");
        fluChart.getExporting().setEnabled(false);
        fluChart.getPlotOptions().getPie().getDataLabels().setEnabled(false);
        fluChart.getPlotOptions().getPie().setShowInLegend(true);
        fluChart.setModel(new DefaultPieModel());
    }

    private void refreshChartData() {
        PieModel model = (PieModel) fluChart.getModel();
        model.clear();

        for (int row = 45; row < 53; row++) {
            String name = fluSpreadsheet.getRange(row, 0).getValue();
            Double nCases = fluSpreadsheet.getRange(row, 1).getRangeValue().getCellValue().getDoubleValue();
            model.setValue(name, nCases);
        }
    }

    private void initMapMarkers() {
        fluMap.setZoom(5);
        for (int row = 2; row < NUMBER_OF_GMARKER_ROW; row++) {
            String state = fluSpreadsheet.getRange(row, 0).getValue();
            Double nCase = fluSpreadsheet.getRange(row, 1).getRangeValue().getCellValue().getDoubleValue();
            Double nDeath = fluSpreadsheet.getRange(row, 2).getRangeValue().getCellValue().getDoubleValue();
            String description = fluSpreadsheet.getRange(row, 3).getValue();
            Double latitude = fluSpreadsheet.getRange(row, LATITUDE_COLUMN).getRangeValue().getCellValue().getDoubleValue();
            Double longitude = fluSpreadsheet.getRange(row, LONGITUDE_COLUMN).getRangeValue().getCellValue().getDoubleValue();

            Gmarker gmarker = new Gmarker();
            gmarker.setLat(latitude);
            gmarker.setLng(longitude);
            String content = "<span style=\"color:#346b93;font-weight:bold\">" +
                    state +
                    "</span><br/><span style=\"color:red\">" +
                    nCase +
                    "</span> cases<br/><span style=\"color:red\">" +
                    nDeath +
                    "</span> death<div style=\"background-color:#E8F5Cf;padding:2px\">" +
                    description + "</div>";
            gmarker.setContent(content);

            gmarkerArray[row] = gmarker;
            fluMap.appendChild(gmarker);

            if (row == 2) {
                gmarkerArray[row].setOpen(true);
            }
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

	private void openMarker(int row, Double latitude, Double longitude) {
		fluMap.setLat(latitude);
		fluMap.setLng(longitude);
		for (Gmarker gmarker : gmarkerArray) {
			if (gmarker != null && gmarker.isOpen())
				gmarker.setOpen(false);
		}
		gmarkerArray[row].setOpen(true);
	}
}