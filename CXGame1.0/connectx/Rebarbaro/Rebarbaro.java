package connectx.Rebarbaro;


import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

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
	private double[] columns_value;
	int M, N, K;
	CXCellState first;
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
		
		this.DECISIONTREEDEPTH = 4;
    }

	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); //per il timeout
		int bestScore = Integer.MIN_VALUE; //per il minimax
		int bestCol = -1; //per il minimax
		int depth = DECISIONTREEDEPTH;  //depth nei parametri di selectColumn non va bene perchE' java a quanto pare vuole che i parametri siano gli stessi di CXPlayer.selectColumn(..)
		Integer[] L = B.getAvailableColumns(); //lista delle colonne disponibili

		for (int col : L) {
			System.err.print("\n marked column: " + B.numOfMarkedCells()); //debug
			System.err.println("\n\n"); //debug

			int score = minimax(B, depth, col, Integer.MIN_VALUE, Integer.MAX_VALUE, true); //minimax
			System.err.print("\nscore: " + score); //debug
			if (score > bestScore) { //se il punteggio E' migliore di quello attuale
				bestScore = score; //lo aggiorno
				bestCol = col; //e aggiorno la colonna migliore
			}
			
		}

		System.err.print("\n bestCol: " + bestCol + " bestScore: " + bestScore);

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

		System.err.print("\n");
		for (int i = DECISIONTREEDEPTH; i > depth; i--) { System.err.print("\t");}
		System.err.print("col: " + firstMove + " "); //debug
		System.err.print("depth: " + depth + "\t\t"); //debug

		
		if (state == myWin) { //se ho vinto
			//int eval = evaluationFunction(B);
			B.unmarkColumn(); //tolgo la mossa
			return 10; //ritorno 1 se sono il giocatore che sta massimizzando, -1 altrimenti
		}

		else if (state == yourWin) { //se ha vinto l'avversario
			//int eval = evaluationFunction(B);
			B.unmarkColumn(); //tolgo la mossa
			return -10; //ritorno -1 se sono il giocatore che sta massimizzando, 1 altrimenti
		}
		

		if(depth == 0 || state != CXGameState.OPEN){ //se sono arrivato alla profondita' massima o se ho pareggiato
			//B.unmarkColumn(); //tolgo la mossa
			//return evaluationFunction(B);
			int score = maximizingPlayer ? evaluationFunction(B) : -evaluationFunction(B);
			//System.err.print("evaluate: " + score);
			B.unmarkColumn(); //tolgo la mossa
			return score;
			//return maximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE; //ritorno 0
		} else if( depth == 0 && state == CXGameState.DRAW){
			B.unmarkColumn(); //tolgo la mossa
			return maximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE; 
		}


		L = B.getAvailableColumns(); //aggiorno la lista delle colonne disponibili

		if (maximizingPlayer) { 
			// Maximize player 1's score
			int minScore = Integer.MAX_VALUE;
			for (int col : L) {
				
				int score = minimax(B, depth - 1, col, alpha, beta, false);
				System.err.print("evaluate: " + score + " ");
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
				
				int score = minimax(B, depth - 1, col, alpha, beta, true);
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

	public int evaluationFunction(CXBoard board) {
		int myPieces = 0;
		int myThrees = 0;
		int myTwos = 0;
		int myVerticalWins = 0;
		CXCellState myPiece = first;
		CXCell lastCell = board.getLastMove();
		int row = lastCell.i;
		int col = lastCell.j;

		//for (int col = 0; col < N; col++) {
			//for (int row = 0; row < M; row++) {
				if (board.cellState(row, col) == myPiece) {
					//System.err.print("\nmyPiece " + row + " " + col);
					myPieces++;
					if (isVerticalWin(board, col, row, myPiece)) {
						myVerticalWins++;
					}
					else if (isHorizontalWin(board, col, row, myPiece) || isDiagonalWin(board, col, row, myPiece)) {
						myThrees++;
					} else if (isTwoInARow(board, col, row, myPiece)) {
						myTwos++;
					}
				}
					//System.err.print("\nempty " + row + " " + col);
				//}
			//}
		

		int score = (myThrees * 5) + (myTwos * 3) + (2* myVerticalWins);
		return score;
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


	public double[] calculate_columns_value(int boardWidth){
		double[] columns_value = new double[boardWidth];
		for(int i = 0; i < boardWidth; i++){
			columns_value[i] =  i < boardWidth/2 ? ( 1 + i/(boardWidth/2) ) / 2 : ( 1 + (boardWidth - i)/(boardWidth/2) ) / 2;
		}
		return columns_value;
	}

	public boolean isVerticalWin(CXBoard B, int col, int row, CXCellState piece){
		//System.err.print("verticale\t"+ col + "\t" + row + "\t" + piece + "\t");
		int count = 1;
		int i = row + 1;
		while(i < M){
			if(B.cellState(i, col) == piece){
				count++;
			}
			else{
				break;
			}
			i++;
		}
		i = row - 1;
		while(i >= 0){
			if(B.cellState(i, col) == piece){
				count++;
			}
			else{
				break;
			}
			i--;
		}
		//System.err.print("\n");
		return count >= K;
	}

	public boolean isDiagonalWin(CXBoard B, int col, int row, CXCellState piece){
		//System.err.print("diagonale\t");
		int count = 1;
		int i = row + 1;
		int j = col + 1;
		while(i < M && j < N){
			if(B.cellState(i, j) == piece){
				count++;
			}
			else{
				break;
			}
			i++;
			j++;
		}
		i = row - 1;
		j = col - 1;
		while(i >= 0 && j >= 0){
			if(B.cellState(i, j) == piece){
				count++;
			}
			else{
				break;
			}
			i--;
			j--;
		}
		//System.err.print("\n");
		return count >= K;
	}

	public boolean isHorizontalWin(CXBoard B, int col, int row, CXCellState piece){
		//System.err.print("orizzontale\t");
		int count = 1;
		int i = col + 1;
		while(i < N){
			if(B.cellState(row, i) == piece){
				count++;
			}
			else{
				break;
			}
			i++;
		}
		i = col - 1;
		while(i >= 0){
			if(B.cellState(row, i) == piece){
				count++;
			}
			else{
				break;
			}
			i--;
		}
		//System.err.print("\n");
		return count >= K;
	}

	public boolean isTwoInARow(CXBoard B, int col, int row, CXCellState piece){
		//System.err.print("due in linea\t");
		if(row + 1 < M && B.cellState(row, col) == piece){
			return true;
		} else if(row - 1 >= 0 && B.cellState(row, col) == piece){
			return true;
		} else if(col + 1 < N && B.cellState(row, col) == piece){
			return true;
		} else if(col - 1 < 0 && B.cellState(row, col) == piece){
			return true;
		}
			
		return false;
	}

}
