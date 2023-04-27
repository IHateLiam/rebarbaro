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

    /*Default empty constructor*/
    public Rebarbaro() {

    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;
    }

	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis();		
		int bestScore = Integer.MIN_VALUE;
		int bestCol = -1;
		int depth = 1;  //depth nei parametri di selectColumn non va bene perchE' java a quanto pare vuole che i parametri siano gli stessi di CXPlayer.selectColumn(..)
		Integer[] L = B.getAvailableColumns();

		for (int col : L) {
			System.err.print("\n marked column: " + B.numOfMarkedCells());
			System.err.println("\n\n");

			int score = minimax(B, depth, col, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
			if (score > bestScore) {
				bestScore = score;
				bestCol = col;
			}
			
			}

		if (bestCol == -1) {
			try {
				bestCol = singleMoveBlock(B, L);
			} catch(TimeoutException e) {
				System.err.println("Timeout!!! singleMoveBlock ritorna -1 in selectColumn");
			}
		}

		System.err.print("\n--- passo il turno ---\n\n");
		return bestCol;
	}

	public int minimax(CXBoard B, int depth, int firstMove, int alpha, int beta, boolean maximizingPlayer) {
		Integer[] L = B.getAvailableColumns();
		CXGameState state = B.markColumn(firstMove);       //marcamento numero 1 
		int score = 0;
		System.err.print("col: " + firstMove + " ");
		System.err.print("depth:" + depth + "\t\t");

		if (state == yourWin) {
			B.unmarkColumn();
			return maximizingPlayer ? -1 : 1;
		}
		
		if (state == myWin) {
			B.unmarkColumn();
			return maximizingPlayer ? 1 : -1;
		}

		else if(depth == 0 || state == CXGameState.DRAW){
			B.unmarkColumn();
			return 0;
		}

		/*
		i controlli sopra implementano gia' tuta questa merda qua sotto
		
		int winningColumn = -1;
		try {
			winningColumn = singleMoveWin(B, L);
		} catch (TimeoutException e) {
			System.err.println("Timeout!!! singleMoveWin ritorna -1 in minimax");
			winningColumn = -1;
		}
		if (depth == 0 || winningColumn != -1 || L.length == 0) {
			// Leaf node or game over
			return evaluate(B);
		}
		*/
		
		L = B.getAvailableColumns();
		if (maximizingPlayer) {
			// Maximize player 1's score
			int maxScore = Integer.MIN_VALUE;
			for (int col : L) {
				/*
				 
				state = B.markColumn(col);                 //marcamento numero 2
				if (state == myWin) {
					B.unmarkColumn();					//smarcamento condizionale numero 2
					return 1;
				}
				B.unmarkColumn();                      //smarcamento condizionale (else) numero 2
				*/
				score = minimax(B, depth - 1, col, alpha, beta, false);
				
				maxScore = Math.max(maxScore, score);
				alpha = Math.max(alpha, score);
				if (beta <= alpha) {
					// Beta cutoff
					break;
				}
			}
			return maxScore;
		} else {
			// Minimize player 2's score
			int minScore = Integer.MAX_VALUE;
			for (int col : L) {
				/*
				 
				state = B.markColumn(col);
				if (state == myWin) {
					B.unmarkColumn();
					return 1;
				}
				B.unmarkColumn();
				*/
				score = -minimax(B, depth - 1, col, alpha, beta, true);

				minScore = Math.min(minScore, score);
				beta = Math.min(beta, score);
				if (beta <= alpha) {
					// Alpha cutoff
					break;
				}
			}
			return minScore;
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
}