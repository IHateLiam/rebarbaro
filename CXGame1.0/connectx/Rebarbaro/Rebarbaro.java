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

	public int minimax(CXBoard B, int depth, int alpha, int beta, boolean maximizingPlayer) {
		Integer[] L = B.getAvailableColumns();
		//if (depth == 0 || B.checkForWin(1) || B.checkForWin(2) || B.getAvailableColumns().length == 0) {
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


		if (maximizingPlayer) {
			// Maximize player 1's score
			int maxScore = Integer.MIN_VALUE;
			for (int col : L) {
				//CXBoard newBoard = B.getDeepCopy();
				//newBoard.makeMove(col, 1);

				B.markColumn(col);
				int score = minimax(B, depth - 1, alpha, beta, false);
				B.unmarkColumn();

				//int score = minimax(newBoard, depth - 1, alpha, beta, false);
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
				//CXBoard newBoard = B.getDeepCopy();
				//newBoard.makeMove(col, 2);

				B.markColumn(col);
				int score = -minimax(B, depth - 1, alpha, beta, true);
				B.unmarkColumn();
				
				//int score = minimax(newBoard, depth - 1, alpha, beta, true);
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

	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis();

		int bestScore = Integer.MIN_VALUE;
		int bestCol = -1;
		int depth = 1;  //depth nei parametri di selectColumn non va bene perchE' java a quanto pare vuole che i parametri siano gli stessi di CXPlayer.selectColumn(..)
		Integer[] L = B.getAvailableColumns();

		for (int col : L) {
			//CXBoard newBoard = B.getDeepCopy();
			//newBoard.makeMove(col, 1);
			int score = minimax(B, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
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
		return bestCol;
	}

	//esiste singleMoveWin() del prof che funziona meglio
	//quella cambia che invece che ritornare un booleano ti ritorna la colonna se puoi vincere altrimenti -1
	/*
	 * 
	 public boolean checkForWin(int player) {    
		 int[][] directions = {{1, 0}, {0, 1}, {1, 1}, {-1, 1}}; // directions to search for win
		 for (int row = 0; row < board.length; row++) {
			 for (int col = 0; col < board[0].length; col++) {
				if (board[row][col] == player) {
					for (int[] dir : directions) {
						int count = 1; // count number of connected tokens in a direction
						int r = row + dir[0];
						int c = col + dir[1];
						while (r >= 0 && r < board.length && c >= 0 && c < board[0].length && board[r][c] == player) {
							count++;
							r += dir[0];
							c += dir[1];
						}
						if (count >= 4) {
							return true; // found a win
						}
					}
				}
			}
		}
		return false; // no win found
	}
	*/
	
	//non ci serve copiare una nuova tabella quando possiamo direttamente fare markcolumn e e unmarkcolumn e vedere il gamestate direttamente dalla tabella originale
	/*
	 * 
	 public CXBoard getDeepCopy() {
		 int[][] newGrid = new int[getHeight()][getWidth()];
		 
		 // Copy the grid
		 for (int row = 0; row < getHeight(); row++) {
			 for (int col = 0; col < getWidth(); col++) {
				 newGrid[row][col] = getCell(row, col);
				}
			}
			
			// Copy the other properties
			CXBoard newBoard = new CXBoard(newGrid, getNextPlayer(), getLastMove(), getNumMoves());
			
			return newBoard;
		}
		*/
/*
 * 
 private int[] getScores(CXBoard B, int player) {
	 int[] scores = new int[L];
	 
	 for (int col = 0; col < B.getN(); col++) {
		 if (B.fullColumn(col)) {
			 scores[col] = -1;
			 continue;
			}
			
			// Place the piece in the column
			CXCell cell = B.placePiece(col, player);
			int score = 0;
			
			// Check horizontal score
			for (int i = -k + 1; i < k; i++) {
				int count = 0;
				for (int j = i; j < i + k; j++) {
					if (B.isInside(cell.getRow(), cell.getCol() + j) && B.getCell(cell.getRow(), cell.getCol() + j) == player) {
						count++;
					}
				}
				if (count == k) {
					score = Integer.MAX_VALUE; // Win condition
					break;
				} else {
					score += count * count;
				}
			}
	
			if (score == Integer.MAX_VALUE) {
				scores[col] = Integer.MAX_VALUE;
				B.undoLastMove();
				continue;
			}
	
			// Check vertical score
			for (int i = -k + 1; i < k; i++) {
				int count = 0;
				for (int j = i; j < i + k; j++) {
					if (B.isInside(cell.getRow() + j, cell.getCol()) && B.getCell(cell.getRow() + j, cell.getCol()) == player) {
						count++;
					}
				}
				score += count * count;
			}
	
			// Check diagonal score (top-left to bottom-right)
			for (int i = -k + 1; i < k; i++) {
				int count = 0;
				for (int j = i; j < i + k; j++) {
					if (B.isInside(cell.getRow() + j, cell.getCol() + j) && B.getCell(cell.getRow() + j, cell.getCol() + j) == player) {
						count++;
					}
				}
				score += count * count;
			}
	
			// Check diagonal score (top-right to bottom-left)
			for (int i = -k + 1; i < k; i++) {
				int count = 0;
				for (int j = i; j < i + k; j++) {
					if (B.isInside(cell.getRow() + j, cell.getCol() - j) && B.getCell(cell.getRow() + j, cell.getCol() - j) == player) {
						count++;
					}
				}
				score += count * count;
			}
			
			scores[col] = score;
			B.undoLastMove();
		}
		
		return scores;
	}
	*/
	
    /**
	 * Selects a free colum on game board.
	 * <p>
	 * Selects a winning column (if any), otherwise selects a column (if any) 
	 * that prevents the adversary to win with his next move. If both previous
	 * cases do not apply, selects a random column.
	 * </p>
	 */
	/* 	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); // Save starting time
		
		Integer[] L = B.getAvailableColumns();
		int save    = L[rand.nextInt(L.length)]; // Save a random column 
		
		try {
			int col = singleMoveWin(B,L);
			if(col != -1) 
			return col;
			else
			return singleMoveBlock(B,L);
		} catch (TimeoutException e) {
			System.err.println("Timeout!!! Random column selected");
			return save;
		}
	} */

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