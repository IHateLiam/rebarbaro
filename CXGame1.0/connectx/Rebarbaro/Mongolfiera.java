package connectx.Rebarbaro;

import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCellState;


public class Mongolfiera {
    public float score;
    public int column;
    public int depth;
    public int markedCell;
    public int lastMoveI;
    public int lastMoveJ;

    public Mongolfiera(float score, int column) {
        this.score = score;
        this.column = column;
        this.depth = 0;
    }

    public float getScore(){
        return score;
    }

}
