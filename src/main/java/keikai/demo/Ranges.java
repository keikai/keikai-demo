package keikai.demo;

import io.keikai.client.api.Range;

/**
 * A utility class that manipulates {@link io.keikai.client.api.Range}
 */
public class Ranges {

    /**
     * Return a new Range whose row index is calculated by specified offset
     * @param range the original Range to be moved
     * @param rowOffset  rows to move, can be negative or positive
     * @return a new range object after moving
     */
    static public Range moveToRow(Range range, int rowOffset){
        int targetRow = range.getRow() + rowOffset;
        return range.getSpreadsheet().getRange(targetRow, range.getColumn());
    }

    static public Range moveToCol(Range range, int colOffset){
        int targetCol = range.getRow() + colOffset;
        return range.getSpreadsheet().getRange(range.getRow(), targetCol);
    }

    /**
     * Return a new Range whose row index is calculated by specified offset
     */
    static public Range expandToRow(Range range, int nRow){
        return range.getSpreadsheet().getRange(range.getRow(), range.getColumn(), nRow, 1);
    }
}
