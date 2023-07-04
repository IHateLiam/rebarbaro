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
    private static final int BOARD_SIZE = 6;
    private static final long[][] ZOBRIST_TABLE = new long[BOARD_SIZE][BOARD_SIZE];
    private static final long EMPTY_BOARD_HASH = 0L;
    private Long[] firstChildren; 

    static {
        Random random = new Random();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                ZOBRIST_TABLE[i][j] = random.nextLong();
            }
        }
    }

    /**
     * Costruttore
     */
    public IncantesimoClonazione() {
        map = new HashMap<Long, Mongolfiera>();
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
        return map.get(hash);
    }

    /**
     * Metodo ZoobristHash
     * Restituisce l'hash associato alla board passata come parametro
     * @param board
     * @return hash
     */
    public static long getBoardHash(CXBoard board) {
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
    public float getChildrenScore(CXBoard board, List L, boolean maximizingPlayer,int roundMarkedCell){
        float maxScore = maximizingPlayer ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
        float score;
        Integer[] availableColumns = board.getAvailableColumns();
        Mongolfiera mongolfiera;
        for(int i : availableColumns){
            board.markColumn(i);
            long key = getKey(board);
            if(map.containsKey(key)){
                mongolfiera = map.get(key);
                if(map.get(key).markedCell == roundMarkedCell){
                    score = mongolfiera.score;
                    if(maximizingPlayer)
                        maxScore = Math.max(score, maxScore);
                    else
                        maxScore = Math.min(score, maxScore);
                }
                else{
                    /**
                     * Il warning di "Type Safety" che stai vedendo è un avviso comune quando si lavora con le collezioni
                     * in Java. Questo perché Java non può garantire a tempo di compilazione che tutti gli elementi
                     * nella tua lista siano del tipo specificato (in questo caso, Mongolfiera).
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
     * @param col
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


}
