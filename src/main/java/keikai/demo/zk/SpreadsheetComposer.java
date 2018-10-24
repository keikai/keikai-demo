/* KKComposer.java

	Purpose:
		
	Description:
		
	History:
		Created by Hawk Chen

Copyright (C) 2017 Potix Corporation. All Rights Reserved.
*/
package keikai.demo.zk;

import io.keikai.client.api.*;
import io.keikai.client.api.Fill.PatternFill;
import io.keikai.client.api.event.*;
import io.keikai.client.api.event.Events;
import io.keikai.client.api.ui.UiActivity;
import io.keikai.util.*;
import keikai.demo.*;
import org.apache.commons.io.FileUtils;
import org.slf4j.*;
import org.zkoss.zhtml.Script;
import org.zkoss.zk.scripting.Interpreters;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.event.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zkex.zul.Colorbox;
import org.zkoss.zul.*;
import org.zkoss.zul.ext.Selectable;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import static io.keikai.client.api.Borders.BorderIndex;
import static io.keikai.client.api.Range.*;

/**
 * Demonstrate API usage about: import, export, listen events, change styles, insert data <br>
 * * Accept keikai server IP from URL like http://localhost:8080/java-client-demo/zk/index.zul?server=10.1.3.201:7777
 *
 * @author Hawk
 */
public class SpreadsheetComposer extends SelectorComposer<Component> {
    private static final Logger logger = LoggerFactory.getLogger(SpreadsheetComposer.class);
    public static final String BLANK_XLSX = "blank.xlsx";
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
    private Label cellInfo;
    @Wire("#cellValue")
    private Textbox cellValueBox;
    @Wire
    private Colorbox borderColorBox;
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

    @Wire
    private Hlayout statusBar;
    @Wire
    private Popup info;
    @Wire
    private Popup imagePopup;

    private ListModelList<String> fileListModel;
    final private File BOOK_FOLDER = new File(getPage().getDesktop().getWebApp().getRealPath("/book/"));

    @Override
    public void doBeforeComposeChildren(Component comp) throws Exception {
        super.doBeforeComposeChildren(comp);
        initStatusBar();
    }

    @Override
    public void doAfterCompose(Component root) throws Exception {
        super.doAfterCompose(root);
//        KeikaiUtil.enableSocketIOLog();
        initSpreadsheet();
        initMenubar();
        //enable server push to update UI according to keikai async response
        root.getDesktop().enableServerPush(true);
        addEventListener();
//        initCellData();
    }

    private void initStatusBar() {
        getPage().getDesktop().setAttribute("serverAddress", getKeikaiServerAddress());
    }

    private void initCellData() {
        insertDataByRow(200);
    }

    private void importFile(String fileName) throws IOException, DuplicateNameException, AbortedException {
        File template = new File(BOOK_FOLDER, fileName);
        spreadsheet.importAndReplace(fileName, template);
    }

    /**
     * add event listeners for spreadsheet
     */
    private void addEventListener() {
        spreadsheet.addExceptionHandler(throwable -> {
            logger.error("Error inside spreadsheet", throwable);
        });

        final Desktop desktop = getSelf().getDesktop();
        ExceptionalConsumer<RangeEvent> listener = (e) -> {
            RangeSelectEvent event = (RangeSelectEvent) e;
            selectedRange = event.getActiveSelection();
            RangeValue rangeValue = event.getRange().getRangeValue();
            Optional<CellValue> optionalCellValue = Optional.ofNullable(rangeValue.getCellValue());

            StringBuffer cellInfoBuffer = new StringBuffer();
            Consumer appendInfo = (value) -> {
                cellInfoBuffer.append(value.toString() + ", ");
            };
            optionalCellValue.map(CellValue::getDoubleValue)
                    .ifPresent(appendInfo);
            optionalCellValue.map(CellValue::getFormula)
                    .ifPresent(appendInfo);
            optionalCellValue.map(CellValue::getDateValue)
                    .ifPresent(appendInfo);
            optionalCellValue.map(CellValue::getBooleanValue)
                    .ifPresent(appendInfo);
            optionalCellValue.map(CellValue::getStringValue)
                    .ifPresent(appendInfo);
            AsyncRender.getUpdateRunner(desktop, () -> {
                if (optionalCellValue.isPresent()
                    && optionalCellValue.get().isFormula()) {
                    cellValueBox.setRawValue(rangeValue.getCellValue().getFormula());
                } else {
                    cellValueBox.setRawValue(rangeValue.getValue());
                }
                cellInfo.setValue(cellInfoBuffer.toString());
                ((Label) getSelf().getFellow("cell")).setValue(selectedRange.getA1Notation());
            }).run();
        };

        //register spreadsheet event listeners
        spreadsheet.addEventListener(Events.ON_SELECTION_CHANGE, listener::accept);

        ExceptionalConsumer<RangeEvent> keyListener = (e) -> {
            RangeKeyEvent keyEvent = (RangeKeyEvent) e;
            AsyncRender.getUpdateRunner(desktop, () -> {
                String range = keyEvent.getRange().getA1Notation().split(":")[0];
                keyCode.setValue(range + "[keyCode=" + keyEvent.getKeyCode() + "], shift: " + keyEvent.isShiftKey()
                        + ", ctrl: " + keyEvent.isCtrlKey() + ", alt: " + keyEvent.isAltKey() + ", meta: "
                        + keyEvent.isMetaKey());
            }).run();
        };
        // open a context menu
        ExceptionalConsumer<RangeEvent> mouseListener = (e) -> {
            CellMouseEvent mouseEvent = (CellMouseEvent) e;
            AsyncRender.getUpdateRunner(desktop, () -> {
                contextMenu.open(mouseEvent.getPageX(), mouseEvent.getPageY());
            }).run();
        };
        spreadsheet.addEventListener(Events.ON_KEY_DOWN, keyListener::accept);
        spreadsheet.addEventListener(Events.ON_CELL_RIGHT_CLICK, mouseListener::accept);
        spreadsheet.setUiActivityCallback(new UiActivity() {
            public void onConnect() {
                logger.debug(">>connected");
            }

            public void onDisconnect() {
                logger.debug(">>disconnected");
                spreadsheet.close();
            }
        });


        ExceptionalConsumer<RangeEvent> mouseHoverListener = (e) -> {
            CellMouseEvent mouseEvent = (CellMouseEvent) e;
            if (e.getName().equals(Events.ON_CELL_MOUSE_ENTER)){
                if (e.getRange().getValue().equals("zk")) {
                    AsyncRender.getUpdateRunner(desktop, () -> {
                        imagePopup.open(mouseEvent.getPageX(), mouseEvent.getPageY());
                    }).run();
                }
            }else{
                if (imagePopup.isVisible()) {
                    AsyncRender.getUpdateRunner(desktop, () -> {
                        imagePopup.close();
                    }).run();
                }
            }
        };
//        spreadsheet.addEventListener(Events.ON_CELL_MOUSE_ENTER, mouseHoverListener::accept);
//        spreadsheet.addEventListener(Events.ON_CELL_MOUSE_LEAVE, mouseHoverListener::accept);
    }

    /**
     * get a spreadsheet java client and getUpdateRunner spreadsheet on a browser
     */
    private void initSpreadsheet() {
        Settings settings = Settings.DEFAULT_SETTINGS.clone();
        enableMouseHover(settings);
        spreadsheet = Keikai.newClient(getKeikaiServerAddress(), settings); //connect to keikai server
        getPage().getDesktop().setAttribute(SpreadsheetCleanUp.SPREADSHEET, spreadsheet); //make spreadsheet get closed
        //pass target element's id and get keikai script URI
        String scriptUri = spreadsheet.getURI(getSelf().getFellow("myss").getUuid());
        //load the initial script to getUpdateRunner spreadsheet at the client
        Script initialScript = new Script();
        initialScript.setSrc(scriptUri);
        initialScript.setDefer(true);
        initialScript.setAsync(true);
        initialScript.setPage(getPage());
        selectedRange = spreadsheet.getRange("A1");
    }

    private void enableMouseHover(Settings settings ) {
        settings.set(Settings.Key.SPREADSHEET_CONFIG, Maps.toMap("enableMouseEvent", true));
    }

    private String getKeikaiServerAddress() {
        String ip = Executions.getCurrent().getParameter("server");
        return ip == null ? Configuration.LOCAL_KEIKAI_SERVER: "http://" + ip;
    }

    private void initMenubar() {
        borderIndexBox.setModel(new ListModelArray<>(BorderIndex.values()));
        ((Selectable<BorderIndex>) borderIndexBox.getModel()).addToSelection(BorderIndex.EdgeBottom);

        borderLineStyleBox.setModel(new ListModelArray<>(Border.Style.values()));
        ((Selectable<Border.Style>) borderLineStyleBox.getModel()).addToSelection(Border.Style.Thin);

        filterOperator.setModel(new ListModelArray<>(AutoFilterOperator.values()));
        ((Selectable<AutoFilterOperator>) filterOperator.getModel()).addToSelection(AutoFilterOperator.FilterValues);


        fillPatternBox.setModel(new ListModelArray<>(PatternFill.PatternType.values()));
        ((Selectable<PatternFill.PatternType>) fillPatternBox.getModel()).addToSelection(PatternFill.PatternType.Solid);

        fileListModel = new ListModelList(generateBookList());
        filelistBox.setModel(fileListModel);
    }

    private String[] generateBookList() {
        return BOOK_FOLDER.list((dir, name) -> {
            return name.endsWith("xlsx");
        });
    }

    @Listen("onSelect = #filelistBox")
    public void loadServerFile() throws IOException, ExecutionException, InterruptedException, DuplicateNameException, AbortedException {
        filePopup.close();
        String fileName = fileListModel.getSelection().iterator().next();
        if (spreadsheet.containsWorkbook(fileName)) {
            spreadsheet.deleteWorkbook(fileName);
        }
        importFile(fileName);
        fileListModel.clearSelection();
    }

    /**
     * Can't import a book more than once, we should delete the previous book first.
     */
    @Listen("onClick = menuitem[iconSclass='z-icon-file']")
    public void newFile() throws ExecutionException, InterruptedException, DuplicateNameException, AbortedException {
        try {
            importFile(BLANK_XLSX);
        } catch (IOException e) {
            Clients.showNotification(e.getMessage());
        }
    }

    @Listen("onClick = menuitem[iconSclass='z-icon-upload']")
    public void openDialog() {
        Fileupload.get(new HashMap(), null, "Excel (xlsx) File Upload", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", -1, -1, true, null);
    }

    @Listen("onUpload = #root")
    public void upload(UploadEvent e) throws IOException, DuplicateNameException, AbortedException {
        String name = e.getMedia().getName();
        InputStream streamData = e.getMedia().getStreamData();
        spreadsheet.importAndReplace(name, streamData);
        FileUtils.copyInputStreamToFile(streamData, new File(BOOK_FOLDER, name));
    }

    @Listen("onClick = menuitem[iconSclass='z-icon-file-excel-o']")
    public void export() throws FileNotFoundException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        spreadsheet.export(spreadsheet.getWorkbook().getName(), outputStream);
        Filedownload.save(outputStream.toByteArray(), "application/excel", spreadsheet.getWorkbook().getName());
    }

    /**
     * demonstrate how to change a font style
     */
    @Listen("onClick = menuitem[iconSclass='z-icon-bold']")
    public void makeBold() {
        Font font = selectedRange.createFont();
        font.setBold(true);
        selectedRange.setFont(font);
    }


    @Listen("onClick = #applyFill")
    public void changeFillPattern(Event e) {
        PatternFill fill = selectedRange.createPatternFill();
        fill.setPatternType(((Selectable<PatternFill.PatternType>) fillPatternBox.getModel()).getSelection().iterator().next());
        fill.setForegroundColor(((Colorbox) e.getTarget().getFellow("foregroundColorBox")).getValue());
        fill.setBackgroundColor(((Colorbox) e.getTarget().getFellow("backgroundColorBox")).getValue());
        selectedRange.setFill(fill);
    }

    @Listen("onChange = #cellValue")
    public void onClick(Event event) {
        String cellReference = ((Label) getSelf().getFellow("cell")).getValue();
        spreadsheet.getRange(cellReference).setValue(cellValueBox.getValue());
    }

    @Listen("onClick = #clearContents")
    public void clearContents(Event event) {
        selectedRange.clearContents();
    }

    @Listen("onClick = menuitem[label='wrap']")
    public void wrap() {
        selectedRange.setWrapText(true);
    }

    @Listen("onChange = #focusTo")
    public void onChange(Event event) {
        String cellReference = ((Textbox) event.getTarget()).getText();
        spreadsheet.getRange(cellReference).activate();
    }

    @Listen("onClick = #focusCell")
    public void focusCell(Event event) {
        Range activeCell = spreadsheet.getActiveCell();
        Clients.showNotification("Focus Cell: " + activeCell.getA1Notation());
    }


    @Listen("onClick = #applyBorder")
    public void applyBorders(Event evt) {
        BorderIndex borderIndex = (BorderIndex) borderIndexBox.getModel().getElementAt(borderIndexBox.getSelectedIndex());
        Borders borders = selectedRange.createBorders(borderIndex);

        Border.Style borderLineStyle = (Border.Style) borderLineStyleBox.getModel().getElementAt(borderLineStyleBox.getSelectedIndex());
        borders.setStyle(borderLineStyle);

        borders.setColor(borderColorBox.getValue());
        selectedRange.setBorders(borders);

        //debug
//		selectedRange.loadCellStyle()
//			.thenAccept((cellStyle -> {
//				System.out.println(cellStyle.getBorders().toString());
//			}));
    }

    @Listen("onClick = #clearBorder")
    public void clearBorders(Event evt) throws ExecutionException, InterruptedException {
        //FIXME doesn't clear inside vertical and horizontal
        Borders borders = selectedRange.getCellStyle().getBorders();
        borders.setStyle(Border.Style.None);
        selectedRange.setBorders(borders);
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

        selectedRange.setAutoFilter(field, criterial1,
                ((Selectable<AutoFilterOperator>) filterOperator.getModel()).getSelection().iterator().next(), null, true);
        filterPopup.close();
    }

    @Listen("onClick=#addMore")
    public void insertData() {
        insertDataByRow(100);
    }

    private void insertDataByRow(int row) {
        for (int j = 0; j < row; j++) {
            for (int i = 0; i < MAX_COLUMN; i++) {
                spreadsheet.getRange(currentDataRowIndex, i).setValue(Converter.numToAbc(i) + (currentDataRowIndex + 1));
            }
            currentDataRowIndex++;
        }
    }

    @Listen("onClick = #applyFormat")
    public void applyFormat(Event e) {
        selectedRange.setNumberFormat(((Textbox) e.getTarget().getFellow("numfmt")).getText());
        ((Popup) e.getTarget().getFellow("formatPopup")).close();
    }

    @Listen("onClick = #applyValidation")
    public void applyValidation(Event e) {
        DataValidation validation = selectedRange.createDataValidation();
        validation.setFormula1(validationFormulaBox.getValue());
        validation.setType(DataValidation.Type.List); //currently-supported type
        validation.setAlertStyle(DataValidation.AlertStyle.Stop);
        validation.setInputTitle(inputTitleBox.getValue());
        validation.setInputMessage(inputMsgBox.getValue());
        validation.setErrorTitle(errorTitleBox.getValue());
        validation.setErrorMessage(errorMsgBox.getValue());
        selectedRange.setDataValidation(validation);
        ((Popup) e.getTarget().getFellow("validationPopup")).close();
    }

    @Listen("onClick = #clearValidation")
    public void clearValidation(Event e) {
        selectedRange.clearDataValidation();
    }

    /**
     * an example of synchronous-style API usage, just calling get().
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    @Listen("onClick = #showStyle")
    public void showCellStyle() throws InterruptedException, ExecutionException {
        CellStyle style = selectedRange.getCellStyle();
        ((Label) info.getFirstChild()).setValue(style.toString());
        info.open(info.getPreviousSibling(), "after_center");
    }

    @Listen("onClick = #lockSelection")
    public void lockSelection() throws ExecutionException, InterruptedException {
        PatternFill fill = selectedRange.createPatternFill();
        fill.setBackgroundColor("#00ff00");
        selectedRange.setFill(fill);
        CellStyle style = selectedRange.createCellStyle();
        Protection protection = style.createProtection();
        protection.setLocked(true);
        style.setProtection(protection);
        selectedRange.setCellStyle(style);
    }

    @Listen("onClick = #unlockSelection")
    public void unlockSelection() throws ExecutionException, InterruptedException {
        PatternFill fill = selectedRange.createPatternFill();
        fill.setBackgroundColor("#00ff00");
        selectedRange.setFill(fill);
        CellStyle style = selectedRange.createCellStyle();
        Protection protection = style.createProtection();
        protection.setLocked(false);
        style.setProtection(protection);
        selectedRange.setCellStyle(style);
    }

    @Listen("onClick = #hideSheet")
    public void hideSheet() throws ExecutionException, InterruptedException {
        spreadsheet.getWorksheet().setVisible(Worksheet.Visibility.Hidden);
    }

    /**
     * selected range should be entire rows
     */
    @Listen("onClick = menuitem[label='Delete row']")
    public void deleteEntireRow() {
        selectedRange.getEntireRow().delete(DeleteShiftDirection.ShiftUp);
    }

    /**
     * selected range should be entire columns
     */
    @Listen("onClick = menuitem[label='Delete column']")
    public void deleteEntireColumn() {
        selectedRange.getEntireColumn().delete(DeleteShiftDirection.ShiftToLeft);
    }

    @Listen("onClick = menuitem[label='Shift up']")
    public void deleteShiftUp() {
        selectedRange.delete(DeleteShiftDirection.ShiftUp);
    }

    @Listen("onClick = menuitem[label='Shift left']")
    public void deleteShiftLeft() {
        selectedRange.delete(DeleteShiftDirection.ShiftToLeft);
    }


    /**
     * selected range should be entire columns
     */
    @Listen("onClick = menuitem[label='Insert column']")
    public void insertColumn() {
        selectedRange.getEntireColumn().insert(InsertShiftDirection.ShiftToRight, InsertFormatOrigin.LeftOrAbove);
    }

    /**
     * selected range should be entire rows
     */
    @Listen("onClick = menuitem[label='Insert row']")
    public void insertRow() {
        selectedRange.getEntireRow().insert(InsertShiftDirection.ShiftDown, InsertFormatOrigin.LeftOrAbove);
    }

    @Listen("onClick = menuitem[label='Shift right']")
    public void insertShiftRight() {
        selectedRange.insert(InsertShiftDirection.ShiftToRight, InsertFormatOrigin.LeftOrAbove);
    }

    @Listen("onClick = menuitem[label='Shift down']")
    public void insertShiftDown() {
        selectedRange.insert(InsertShiftDirection.ShiftDown, InsertFormatOrigin.LeftOrAbove);
    }

    @Listen("onClick = menuitem[label='Add Sheet']")
    public void addSheet() throws ExecutionException, InterruptedException {
        selectedRange.getWorkbook().insertWorksheet();
    }

    @Listen("onClick = #freezePane")
    public void freezePane() {
        selectedRange.freezePanes();
    }

    @Listen("onClick = #autoFill")
    public void autoFill() {
        selectedRange.setAutoFill(Ranges.expandToRow(selectedRange, 5), AutoFillType.FillDefault);
    }

    @Listen("onClick = #sort")
    public void sort() {
        selectedRange.sort(selectedRange.getColumns(), SortOrder.Ascending, null, null, null, null, YesNoGuess.No, false, SortOrientation.SortColumns, null, null, null);
    }

}
