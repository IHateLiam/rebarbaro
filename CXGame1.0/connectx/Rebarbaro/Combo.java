package connectx.Rebarbaro;

import java.util.TreeSet;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;


public class Combo {
    private TreeSet<CXCell> cellList;
    private Direction direction;
    private int length;
    private int value;
    private int freeEnds;    //se e' aperto sia da un'estremita' che dall'altra

    public Combo() {
        cellList = new TreeSet<CXCell>();
        length = 0;
        value = 0;
    }

    public void add(CXCell newCell) {
        cellList.add(newCell);
        length++;
    }

    public void remove(CXCell undesiredCell) {
        cellList.remove(undesiredCell);
        length--;
    }

    public int getLength() {
        return length;
    }

    public int getValue() {
        return value;
    }

    public int getNumberOfFreeEnds() {
        return freeEnds;
    }

    public Direction getDirection() {
        return direction;
    }

    public void calculateComboValue() {
        //int calculatedValue = 0;
        value = length*2;
    }

    public void calculateFreeEnds() {
        return;
    }
}
