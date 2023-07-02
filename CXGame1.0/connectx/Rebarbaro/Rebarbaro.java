package connectx.Rebarbaro;


import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import java.util.HashMap;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;


public class Rebarbaro implements CXPlayer {
    private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
	private int  TIMEOUT;
	private long START;
	private double[] columns_value;
	private int M, N, K;
	CXCellState first;
	private boolean debugMode;
	private HashMap<Long, Float> transpositionTable = new HashMap<Long, Float>();

	//private List<Combo> combinations;

	private int DECISIONTREEDEPTH;


    /*Default empty constructor*/
    public Rebarbaro() {

    }


    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;
		columns_value = calculate_columns_value(N);
		this.M = M;
		this.N = N;
		this.K = K;
		this.first = first ? CXCellState.P1 : CXCellState.P2;

		//this.combinations = Combo();
		
		this.DECISIONTREEDEPTH = 4;

		debugMode = false;
    }


	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); //per il timeout
		float bestScore = Integer.MIN_VALUE; //per il minimax
		int bestCol = -1; //per il minimax
		int depth = DECISIONTREEDEPTH;  //depth nei parametri di selectColumn non va bene perchE' java a quanto pare vuole che i parametri siano gli stessi di CXPlayer.selectColumn(..)
		Integer[] L = B.getAvailableColumns(); //lista delle colonne disponibili

		float[] column_scores = new float[N];    //DEBUG debugMode     (la dichiarazione dentro l'if non va bene)
		transpositionTable.clear();

		for (int col : L) {
			try{

				if(debugMode){
					System.err.print("\n marked column: " + B.numOfMarkedCells()); //debug
					System.err.println("\n\n"); //debug
				}

				float score = minimax(B, depth, col, Integer.MIN_VALUE, Integer.MAX_VALUE, false); //minimax
				score += 0.01;     //se e' tutto a 0, la moltiplicazione dei valori delle colonne viene annullata
				score *= columns_value[col];

				if (score >= bestScore) { //se il punteggio E' migliore di quello attuale
					bestScore = score; //lo aggiorno
					bestCol = col; //e aggiorno la colonna migliore
				}


				if(debugMode) {
					System.err.print("\nscore: " + score);
					column_scores[col] = score;
				}

				if(System.currentTimeMillis() - START > (TIMEOUT * 9700)) { //se ho superato il timeout
					throw new TimeoutException(); //lancio un'eccezione
				}
			} catch(TimeoutException e) {
				System.err.println("Timeout!!! minimax ritorna -1 in selectColumn"); //debug
				break;
			}
			//System.err.println();
			
		}

		if(debugMode){System.err.print("\n bestCol: " + bestCol + " bestScore: " + bestScore);}

		if (bestCol == -1) { //se non ho trovato nessuna mossa vincente
			try { 
				bestCol = singleMoveBlock(B, L); //provo a bloccare l'avversario
			} catch(TimeoutException e) { //se non riesco
				System.err.println("Timeout!!! singleMoveBlock ritorna -1 in selectColumn"); //debug
			}
		}

		if(debugMode){
			System.err.print("\n bestCol: " + bestCol + " bestScore: " + bestScore);

			System.err.print("\n" + "punteggi colonne:\n");
			for(int i = 0; i < N; i++) {
				System.err.print("colonna " + i + ": " + String.format("%9f", column_scores[i]) + "\tvalore colonna: " + columns_value[i] + "\n");
			}
		}
		
		//System.err.println("L1 " + (System.currentTimeMillis() - START)); //debug
		return bestCol; //ritorno la colonna migliore
	}


	//Il codice implementa l'algoritmo minimax con potatura alpha-beta, con una profondita' massima di 4 (scelta arbitraria). La funzione minimax ritorna 1 se il giocatore che sta massimizzando ha vinto, -1 altrimenti; ritorna -1 se il giocatore che sta massimizzando ha perso, 1 altrimenti; 0 in caso di pareggio. La funzione minimax e' ricorsiva, e viene eseguita una volta per ogni colonna disponibile. La funzione minimax riceve come parametri: l'oggetto CXBoard, la profondita' di ricerca, la prima mossa da eseguire, i valori di alpha e beta e una variabile booleana che indica quale giocatore sta massimizzando. La funzione ritorna l'intero corrispondente al punteggio ottenuto dalla mossa.

	public float minimax(CXBoard B, int depth, int firstMove, float alpha, float beta, boolean maximizingPlayer) {
		Integer[] L = B.getAvailableColumns(); //lista delle colonne disponibili
		CXGameState state = B.markColumn(firstMove); //marco la prima mossa
		float score = 0;

		if(debugMode){
			System.err.print("\n");
			for (int i = DECISIONTREEDEPTH; i > depth; i--) { System.err.print("\t");}
			System.err.print("depth: " + (DECISIONTREEDEPTH - depth) + " "); //debug
			System.err.print("col: " + firstMove + "\t\t" ); //debug
		}

		if(depth == 0 || state != CXGameState.OPEN){ //se sono arrivato alla profondita' massima o se ho pareggiato
			score = maximizingPlayer ? -evaluationFunction(B) : evaluationFunction(B);
			B.unmarkColumn(); //tolgo la mossa
			//System.err.println("Score: " + score);
			return score;
			//return 0;
		}

		long key = getKey(B); //chiave per la transposition table
		if(transpositionTable.containsKey(key)){
			score = transpositionTable.get(key);
			return score;
		} 
		
		

		L = B.getAvailableColumns(); //aggiorno la lista delle colonne disponibili

		if (maximizingPlayer) { 
			// Maximize player 1's score
			float maxScore = Float.NEGATIVE_INFINITY;
			for (int col : L) {
				//System.err.println("col: " + col + " " + depth + " " + key + " " + transpositionTable.get(key));
				score = minimax(B, depth - 1, col, alpha, beta, false);
				if(debugMode){System.err.print("evaluate: " + score + " ");}
				maxScore = Math.max(maxScore, score);
				alpha = Math.max(alpha, score);
				if (beta <= alpha) {
					// Beta cutoff
					break;
				}
				
			}
			transpositionTable.put(key, score);
			B.unmarkColumn();
			return maxScore;
			
		} else {
			// Minimize player 2's score
			float minScore = Float.POSITIVE_INFINITY;
			for (int col : L) {
				score = minimax(B, depth - 1, col, alpha, beta, true);
				minScore = Math.min(minScore, score);
				beta = Math.min(beta, score);
				if (beta <= alpha) {
					// Alpha cutoff
					break;
				}
			}
			transpositionTable.put(key, score);
			B.unmarkColumn();
			return minScore;
		}

	}


	public int evaluationFunction(CXBoard board) {
		CXCell lastCell = board.getLastMove();
		int row = lastCell.i;
		int col = lastCell.j;

		int verticalPieces = nearPieces(row, col, board, lastCell.state, K, 1);
		int orizzontalPieces = nearPieces(row, col, board, lastCell.state, K, 2);
		int diagonalPieces = nearPieces(row, col, board, lastCell.state, K, 3);
		int antiDiagonalPieces = nearPieces(row, col, board, lastCell.state, K, 4);
		
		int score = verticalPieces + orizzontalPieces + diagonalPieces + antiDiagonalPieces;
		//score *= columns_value[col];

		if(board.gameState() == CXGameState.WINP1 || board.gameState() == CXGameState.WINP2){
			score *= 20;
		}
		else if(board.gameState() == CXGameState.DRAW){
			score *= 2;
		}
		return score;
	}

	public int nearPieces(int col, int row, CXBoard board, CXCellState player, int n, int direction) {
    int count = 0;
    int deltaRow = 0;
    int deltaCol = 0;
	int multiplier = 2;

    switch (direction) {
        case 1: // verticale
            deltaRow = 1;
            break;
        case 2: // orizzontale
            deltaCol = 1;
            break;
        case 3: // diagonale
            deltaRow = 1;
            deltaCol = 1;
            break;
        case 4: // diagonale inversa
            deltaRow = -1;
            deltaCol = 1;
            break;
        default:
            break;
    }

    int currentRow = row + deltaRow;
    int currentCol = col + deltaCol;

    while (currentRow >= 0 && currentRow < M && currentCol >= 0 && currentCol < N && count < n) {
        if (board.cellState(currentRow, currentCol) == player) {
            count += multiplier;
        } else {
            if(multiplier > 0) multiplier--;
			else break;
        }
        currentRow += deltaRow;
        currentCol += deltaCol;
    }

	if(direction == 3 || direction == 4 || direction == 2){
		//caso mosse opposte
		deltaRow = -deltaCol;
		deltaCol = -deltaCol;
		currentRow = row + deltaCol;
		currentCol = col + deltaCol;
		while (currentRow >= 0 && currentRow < M && currentCol >= 0 && currentCol < N && count < n) {
			if (board.cellState(currentRow, currentCol) == player) {
				count++;
			} else {
				if(multiplier > 0) multiplier--;
				else break;
			}
			currentRow += deltaRow;
			currentCol += deltaCol;
		}
    }
    return count;
	}
 

	public double[] calculate_columns_value(int boardWidth){
		double[] columns_value = new double[boardWidth];
		for(float i = 0; i < boardWidth; i++){
			columns_value[(int)i] =  i < boardWidth/2 ? ( 1 + (i + 1)/(boardWidth/2) ) / 2 : ( 1 + (boardWidth - i)/(boardWidth/2) ) / 2;
		}
		return columns_value;
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





	public void calculateComboFreeEnds(CXBoard B, Combo combo) {
		Direction comboDirection = combo.getDirection();
		CXCell lastCell = combo.getCells().last();
		int nFreeEnds = 0;

		if(comboDirection == Direction.Vertical) {
			if(!B.fullColumn(lastCell.j)) {
				combo.setNumberOfFreeEnds(1);
			}
			return;
		}

		CXCell firstCell = combo.getCells().first();

		if(comboDirection == Direction.Horizontal) {
			if(B.cellState(lastCell.i + 1, lastCell.j) == CXCellState.FREE) {
				nFreeEnds++;
			}
			if(B.cellState(firstCell.i - 1, firstCell.j) == CXCellState.FREE) {
				nFreeEnds++;
			}
			combo.setNumberOfFreeEnds(nFreeEnds);
			return;
		}

		if(comboDirection == Direction.Diagonal) {
			if(B.cellState(lastCell.i + 1, lastCell.j + 1) == CXCellState.FREE) {
				nFreeEnds++;
			}
			if(B.cellState(firstCell.i - 1, firstCell.j - 1) == CXCellState.FREE) {
				nFreeEnds++;
			}
			combo.setNumberOfFreeEnds(nFreeEnds);
			return;
		}
	}

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
}












		/*
		if (state == myWin) { //se ho vinto
			//int eval = evaluationFunction(B);
			if(debugMode) {
				System.err.print("|won | evaluate: " + (maximizingPlayer ? -1*depth : 1*depth) + " ");
			}
			B.unmarkColumn(); //tolgo la mossa
			//System.err.println("Score: " + K*1000);
			return maximizingPlayer ? -(K*1000) : (K*1000); //ritorno 1 se sono il giocatore che sta massimizzando, -1 altrimenti
		}
		else if (state == yourWin) { //se ha vinto l'avversario
			//int eval = evaluationFunction(B);
			if(debugMode) {
				System.err.print("|lost| evaluate: " + (maximizingPlayer ? -1*depth : 1*depth) + " ");
			}
			B.unmarkColumn(); //tolgo la mossa
			return maximizingPlayer ? -(K*1000) : (K*1000); //ritorno -1 se sono il giocatore che sta massimizzando, 1 altrimenti
		}
		else if(depth == 0 || state == CXGameState.DRAW){ //se sono arrivato alla profondita' massima o se ho pareggiato
			score = maximizingPlayer ? -evaluationFunction(B) : evaluationFunction(B);
			B.unmarkColumn(); //tolgo la mossa
			//System.err.println("Score: " + score);
			return score;
			//return 0;
		}
		
	public int evaluationFunction(CXBoard board) {
		CXCell lastCell = board.getLastMove();
		int row = lastCell.i;
		int col = lastCell.j;

		int verticalPieces = nearPieces(row, col, board, lastCell.state, K, 1);
		int orizzontalPieces = nearPieces(row, col, board, lastCell.state, K, 2);
		int diagonalPieces = nearPieces(row, col, board, lastCell.state, K, 3);
		int antiDiagonalPieces = nearPieces(row, col, board, lastCell.state, K, 4);
		
		int score = verticalPieces + orizzontalPieces + diagonalPieces + antiDiagonalPieces;
		score *= columns_value[col];

		if(board.gameState() == CXGameState.WINP1 || board.gameState() == CXGameState.WINP2){
			score *= 10;
		}
		return score;
	}

		
		
		
		
		
		
		
		
		
		
		
		
		
		*/


