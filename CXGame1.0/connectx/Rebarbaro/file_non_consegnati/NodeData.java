package connectx.Rebarbaro;

import connectx.CXBoard;
import connectx.CXCell;

/**
 * NodeData e' una classe che permette di memorizzare
 * le mosse valutate durante l'esplorazione
 * 
 * Esse vengono salvate nella HashMap map,
 * che ha come chiave un long che rappresenta l'hash della board
 * e come valore un oggetto di tipo NodeData che contiene la mossa valutata
 * 
 * importanti sono i parametri:
 * - score: il valore della mossa
 * - column: la colonna in cui e' stata effettuata la mossa
 * - depth: la profondita' in cui e' stata valutata la mossa
 * - markedCell: ultima cella in cui e' stata effettuata la mossa
 * - markedCells: numero di celle marcate in totale
 * - lastMoveI: la riga in cui e' stata effettuata la mossa
 * - lastMoveJ: la colonna in cui e' stata effettuata la mossa
 * - startingMove: la cella in cui e' stata effettuata la mossa prima dell'inizio del minmax>()
 *                 utilizzata per verificare se la mossa e' stata valutata
 *                 in un iterazione precedente o meno
 */
public class NodeData {
    public float score;
    public int column;
    public int depth;
    public CXCell markedCell;
    public int markedCells;
    public int lastMoveI;
    public int lastMoveJ;
    public CXCell startingMove;
    public boolean maximizingPlayer;


    /**
     * Costruttore
     * @param board
     * @param score
     * @param column
     * @param maximizingPlayer
     * @param startingMove
     */
    public NodeData(CXBoard board, float score, int roundMarkedCell, boolean maximizingPlayer, CXCell startingMove) {
        this.score = score;
        this.startingMove = startingMove;
        this.markedCell = board.getLastMove();
        this.markedCells = roundMarkedCell;
        this.lastMoveI = markedCell.i;
        this.lastMoveJ = markedCell.j;
        this.column = markedCell.j;
        this.maximizingPlayer = maximizingPlayer;
    }
}
 
