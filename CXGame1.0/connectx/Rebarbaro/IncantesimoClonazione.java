package connectx.Rebarbaro;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXGameState;
import connectx.CXCellState;

import java.util.HashMap;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;


/*
 * IncantesimoClonazione è una classe che permette di memorizzare le mosse valutate durante l'esplorazione
 * Esse vengono salvate nella HashMap map, che ha come chiave un long che rappresenta l'hash della board
 * e come valore un oggetto di tipo Mongolfiera che contiene la mossa valutata e il suo valore, oltre 
 * ad altri parametri utili per la valutazione e l'esplorazione.
 * 
 * al momento sono presenti due metodi per gestire le chiavi:
 * - uno utilizzando zoobrist hashing
 * - uno utilizzando fnv1a hashing
 * 
 * da valutare quale sia più performante
 */

public class IncantesimoClonazione {
    private HashMap<Long, Mongolfiera> map;
    private int BOARD_COL_SIZE;
    private int BOARD_ROW_SIZE;
    private long[][] ZOBRIST_TABLE;
    private static final long EMPTY_BOARD_HASH = 0L;
    private Long[] firstChildren; 

    static {
        
    }

    /**
     * Costruttore
     */
    public IncantesimoClonazione(int M, int N) {
        map = new HashMap<Long, Mongolfiera>();
        BOARD_ROW_SIZE = M;
        BOARD_COL_SIZE = N;
        ZOBRIST_TABLE = new long[BOARD_ROW_SIZE][BOARD_COL_SIZE];
        Random random = new Random();
        for (int i = 0; i < BOARD_ROW_SIZE; i++) {
            for (int j = 0; j < BOARD_COL_SIZE; j++) {
                ZOBRIST_TABLE[i][j] = random.nextLong();
            }
        }
    }

    public void addBoard(CXBoard board, Mongolfiera mongolfiera) {
        long hash = getBoardHash(board);
        map.put(hash, mongolfiera);
    }

    /**
     * Restituisce la Mongolfiera associata alla board passata come parametro
     * @param board
     * @return
     */
    public Mongolfiera getMongolfiera(CXBoard board) {
        long hash = getBoardHash(board);
        if(!map.containsKey(hash)){
            return null;
        }
        return map.get(hash);
    }

    /**
     * Metodo ZoobristHash
     * Restituisce l'hash associato alla board passata come parametro
     * @param board
     * @return hash
     */
    public long getBoardHash(CXBoard board) {
        long hash = EMPTY_BOARD_HASH;
        CXCell[] markedCells = board.getMarkedCells();
        /*
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                CXCellState state = board.cellState(i, j);
                if (state != CXCellState.FREE) {
                    hash ^= ZOBRIST_TABLE[i][j];
                }
            }
        }
        */
        for(CXCell i : markedCells){
            hash ^= ZOBRIST_TABLE[i.i][i.j];
        }
        return hash;
    }

    /**
     * Metodo fnv1aHash
     * Restituisce l'hash associato alla board passata come parametro
     * @param board
     * @return hash
     */
    public long getKey(CXBoard board) {
		byte[] hash = new byte[board.numOfMarkedCells() * 2];
		int index = 0;
		for (CXCell cell : board.getMarkedCells()) {
			hash[index++] = (byte) cell.i;
			hash[index++] = (byte) cell.j;
		}
		long key = fnv1aHash(hash);
		return key;
	}

    /**
     * Metodo fnv1aHash
     * Restituisce l'hash associato ai byte passati come parametro
     * @param byte[] data
     * @return hash
     */
	public long fnv1aHash(byte[] data) {
		final long FNV_OFFSET_BASIS = 0xcbf29ce484222325L;
		final long FNV_PRIME = 0x100000001b3L;
		long hash = FNV_OFFSET_BASIS;
		for (byte b : data) {
			hash ^= b;
			hash *= FNV_PRIME;
		}
		return hash;
	}


    /**
     * Verifica per una determinata mossa se sono presenti i suoi figli e verifica che:
     * - se sono stati visitati nella stessa iterazione allora prende direttamente il value
     * - altrimenti li inserisce in una lista 
     * 
     * La lista servirà successivamente per organizzare gli elementi per valori in modo tale
     * da scegliere le mosse in ordine di "bontà" dell'ultima
     * @param board
     * @param List L
     * @param maximizingPlayer
     * @param roundMarkedCell
     * 
     * @return maxValue
     */
    public float getChildrenScore(CXBoard board, List<Mongolfiera> L, boolean maximizingPlayer,int roundMarkedCell){
        float maxScore = maximizingPlayer ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        float score;
        Integer[] availableColumns = board.getAvailableColumns();
        Mongolfiera mongolfiera;
        for(int i : availableColumns){
            board.markColumn(i);
            long key = getKey(board);
            if(map.containsKey(key)){
                mongolfiera = map.get(key);
                if(map.get(key).markedCells == roundMarkedCell){
                    score = mongolfiera.score;
                    if(maximizingPlayer)
                        maxScore = Math.max(score, maxScore);
                    else
                        maxScore = Math.min(score, maxScore);
                }
                else{
                    /**
                     * Inserisco una mongolfiera nella lista
                     * e la ordino in base al suo score
                     */
                    L.add(mongolfiera);
                    Collections.sort(L, new Comparator<Mongolfiera>() {
                        @Override
                        public int compare(Mongolfiera m1, Mongolfiera m2) {
                            return Float.compare(m1.score, m2.score);
                        }
                    });
                }
            }
            board.unmarkColumn();
        }
        return maxScore;
    }


    /**
     * Rimuove tutti i figli di primo livello ad eccezione di quello
     * relativo alla mossa scelta
     * @param board
     * @param col colonna scelta per la mossa successiva (quella da tenere)
     */
    public void deleteChildren(CXBoard board, int col){
       List<Integer> availableColumns = new ArrayList<>(Arrays.asList(board.getAvailableColumns()));
       availableColumns.remove(col);
       for(int i : availableColumns){
            board.markColumn(i);
            long key = getBoardHash(board);
            if(map.containsKey(key))
                map.remove(key);
       }
    }

    /**
     * Crea una nuova Mongolfiera
     * @param board
     * @param col
     * @param score
     * @param roundMarkedCell
     * @param maximizingPlayer
     * @return
     */
    public Mongolfiera createMongolfiera(CXBoard board, int col, float score, int roundMarkedCell, boolean maximizingPlayer, CXCell startingMove){
        Mongolfiera mongolfiera = new Mongolfiera(board, score, col, maximizingPlayer, startingMove);
        return mongolfiera;
    }

}
