package connectx.Rebarbaro;


import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;


public class Rebarbaro implements CXPlayer {
    private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
	private int  TIMEOUT;
	private long START;
	private int[] columns_value;

    /*Default empty constructor*/
    public Rebarbaro() {

    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;
		columns_value = calculate_columns_value(M);
    }

	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); //per il timeout
		int bestScore = Integer.MIN_VALUE; //per il minimax
		int bestCol = -1; //per il minimax
		int depth = 1;  //depth nei parametri di selectColumn non va bene perchE' java a quanto pare vuole che i parametri siano gli stessi di CXPlayer.selectColumn(..)
		Integer[] L = B.getAvailableColumns(); //lista delle colonne disponibili

		for (int col : L) {
			System.err.print("\n marked column: " + B.numOfMarkedCells()); //debug
			System.err.println("\n\n"); //debug

			int score = minimax(B, depth, col, Integer.MIN_VALUE, Integer.MAX_VALUE, true); //minimax

			System.err.print("score: " + score); //debug



			if (score > bestScore) { //se il punteggio E' migliore di quello attuale
				bestScore = score; //lo aggiorno
				bestCol = col; //e aggiorno la colonna migliore
			}
			
		}

		if (bestCol == -1) { //se non ho trovato nessuna mossa vincente
			try { 
				bestCol = singleMoveBlock(B, L); //provo a bloccare l'avversario
			} catch(TimeoutException e) { //se non riesco
				System.err.println("Timeout!!! singleMoveBlock ritorna -1 in selectColumn"); //debug
			}
		}

		System.err.print("\n--- passo il turno ---\n\n"); //debug
		return bestCol; //ritorno la colonna migliore
	}


//Il codice implementa l'algoritmo minimax con potatura alpha-beta, con una profondita' massima di 4 (scelta arbitraria). La funzione minimax ritorna 1 se il giocatore che sta massimizzando ha vinto, -1 altrimenti; ritorna -1 se il giocatore che sta massimizzando ha perso, 1 altrimenti; 0 in caso di pareggio. La funzione minimax e' ricorsiva, e viene eseguita una volta per ogni colonna disponibile. La funzione minimax riceve come parametri: l'oggetto CXBoard, la profondita' di ricerca, la prima mossa da eseguire, i valori di alpha e beta e una variabile booleana che indica quale giocatore sta massimizzando. La funzione ritorna l'intero corrispondente al punteggio ottenuto dalla mossa.

public int minimax(CXBoard B, int depth, int firstMove, int alpha, int beta, boolean maximizingPlayer) {
	Integer[] L = B.getAvailableColumns(); //lista delle colonne disponibili
	CXGameState state = B.markColumn(firstMove); //marco la prima mossa

	System.err.print("col: " + firstMove + " "); //debug
	System.err.print("depth:" + depth + "\t\t"); //debug

	if (state == myWin) { //se ho vinto
		B.unmarkColumn(); //tolgo la mossa
		return (depth + 1) * (1); //ritorno 1 se sono il giocatore che sta massimizzando, -1 altrimenti
	}

	else if (state == yourWin) { //se ha vinto l'avversario
		B.unmarkColumn(); //tolgo la mossa
		return (depth + 1) * (-1); //ritorno -1 se sono il giocatore che sta massimizzando, 1 altrimenti
	}

	else if(depth == 0 || state == CXGameState.DRAW){ //se sono arrivato alla profondita' massima o se ho pareggiato
		B.unmarkColumn(); //tolgo la mossa
		return 0; //ritorno 0
	}

	L = B.getAvailableColumns(); //aggiorno la lista delle colonne disponibili

	if (maximizingPlayer) { 
		// Maximize player 1's score
		int minScore = Integer.MAX_VALUE;
		for (int col : L) {
			
			int score = minimax(B, depth - 1, col, alpha, beta, false);

			score *= columns_value[col];
			
			minScore = Math.min(minScore, score);
			beta = Math.max(beta, minScore);
			if (beta <= alpha) {
				// Beta cutoff
				break;
			}
			
		}

		B.unmarkColumn();
		return minScore;
	} else {
		// Minimize player 2's score
		int maxScore = Integer.MIN_VALUE;
		for (int col : L) {
			
			int score = -minimax(B, depth - 1, col, alpha, beta, true);

			score *= columns_value[col];

			maxScore = Math.max(maxScore, score);
			alpha = Math.max(beta, maxScore);
			if (beta <= alpha) {
				// Alpha cutoff
				break;
			}
		}

		B.unmarkColumn();
		return maxScore;
	}
}

	public int evaluate(CXBoard B) {
		/*
		 * 
		 // Evaluate the score of the current board position
		 // This implementation simply counts the number of 1-in-a-row, 2-in-a-row, and 3-in-a-row for each player
		 int[] scores = B.getScores();
		 int score1 = scores[0] + 2 * scores[1] + 10 * scores[2];
		 int score2 = scores[0] + 2 * scores[2] + 10 * scores[4];
		 return score1 - score2;
		 */
		Integer[] L = B.getAvailableColumns();

		int winningColumn = -1;
		try {
			winningColumn = singleMoveWin(B, L);
		} catch(TimeoutException e) {
			winningColumn = -1;
			System.err.println("Timeout!!! singleMoveWin ritorna -1 in evaluate");
		}
		if(winningColumn != -1) {
			return 1;
		}
		else {
			return 0;
		}
	}    



	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

	/**
	 * Check if we can win in a single move
	 *
	 * Returns the winning column if there is one, otherwise -1
	 */	
	private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
    for(int i : L) {
			checktime(); // Check timeout at every iteration
      CXGameState state = B.markColumn(i);
      if (state == myWin)
        return i; // Winning column found: return immediately
      B.unmarkColumn();
    }
		return -1;
	}

	/**
   * Check if we can block adversary's victory 
   *
   * Returns a blocking column if there is one, otherwise a random one
   */
	private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
		TreeSet<Integer> T = new TreeSet<Integer>(); // We collect here safe column indexes

		for(int i : L) {
			checktime();
			T.add(i); // We consider column i as a possible move
			B.markColumn(i);

			int j;
			boolean stop;

			for(j = 0, stop=false; j < L.length && !stop; j++) {
				//try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} // Uncomment to test timeout
				checktime();
				if(!B.fullColumn(L[j])) {
					CXGameState state = B.markColumn(L[j]);
					if (state == yourWin) {
						T.remove(i); // We ignore the i-th column as a possible move
						stop = true; // We don't need to check more
					}
					B.unmarkColumn(); // 
				}
			}
			B.unmarkColumn();
		}

		if (T.size() > 0) {
			Integer[] X = T.toArray(new Integer[T.size()]);
            return X[X.length / 2];    //central columns are better
			
		} else {
			return L[rand.nextInt(L.length)];
		}
	}

	public String playerName() {
		return "Rebarbaro";
	}


public int[] calculate_columns_value(int boardWidth){
	int[] columns_value = new int[boardWidth];
	for(int i = 0; i < boardWidth; i++){
		columns_value[i] =  i < boardWidth/2 ? 1 + i/(boardWidth/2) : 1 + (boardWidth - i)/(boardWidth/2);
	}
	return columns_value;
}



}






