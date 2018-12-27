package keikai.demo.zk;

import io.keikai.client.api.*;
import io.keikai.client.api.event.*;
import org.zkoss.zk.ui.*;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zul.Popup;

import static io.keikai.client.api.Range.*;


/**
 * implement logic for a context menu
 * @author Hawk
 */
public class ContextMenuComposer extends SelectorComposer<Component> {
    private Spreadsheet spreadsheet;

    @Wire
    org.zkoss.zul.Script kkScript;
    @Wire
    private Popup contextMenu;

    @Override
    public void doAfterCompose(Component root) throws Exception {
        super.doAfterCompose(root);
        spreadsheet = (Spreadsheet)root.getDesktop().getAttribute("spreadsheet");

        //initialize the context menu
        final Desktop desktop = getSelf().getDesktop();
        // open a context menu
        ExceptionalConsumer<RangeEvent> mouseListener = (e) -> {
            CellMouseEvent mouseEvent = (CellMouseEvent) e;
            AsyncRender.getUpdateRunner(desktop, () -> {
                contextMenu.open(mouseEvent.getPageX(), mouseEvent.getPageY());
            }).run();
        };
    }


    /**
     * selected range should be entire rows
     */
    @Listen("onClick = menuitem[label='Delete row']")
    public void deleteEntireRow() {
        spreadsheet.getActiveSelection().getEntireRow().delete(DeleteShiftDirection.ShiftUp);
    }

    /**
     * selected range should be entire columns
     */
    @Listen("onClick = menuitem[label='Delete column']")
    public void deleteEntireColumn() {
        spreadsheet.getActiveSelection().getEntireColumn().delete(DeleteShiftDirection.ShiftToLeft);
    }

    @Listen("onClick = menuitem[label='Shift up']")
    public void deleteShiftUp() {
        spreadsheet.getActiveSelection().delete(DeleteShiftDirection.ShiftUp);
    }

    @Listen("onClick = menuitem[label='Shift left']")
    public void deleteShiftLeft() {
        spreadsheet.getActiveSelection().delete(DeleteShiftDirection.ShiftToLeft);
    }


    /**
     * selected range should be entire columns
     */
    @Listen("onClick = menuitem[label='Insert column']")
    public void insertColumn() {
        spreadsheet.getActiveSelection().getEntireColumn().insert(InsertShiftDirection.ShiftToRight, InsertFormatOrigin.LeftOrAbove);
    }

    /**
     * selected range should be entire rows
     */
    @Listen("onClick = menuitem[label='Insert row']")
    public void insertRow() {
        spreadsheet.getActiveSelection().getEntireRow().insert(InsertShiftDirection.ShiftDown, InsertFormatOrigin.LeftOrAbove);
    }

    @Listen("onClick = menuitem[label='Shift right']")
    public void insertShiftRight() {
        spreadsheet.getActiveSelection().insert(InsertShiftDirection.ShiftToRight, InsertFormatOrigin.LeftOrAbove);
    }

    @Listen("onClick = menuitem[label='Shift down']")
    public void insertShiftDown() {
        spreadsheet.getActiveSelection().insert(InsertShiftDirection.ShiftDown, InsertFormatOrigin.LeftOrAbove);
    }
}
