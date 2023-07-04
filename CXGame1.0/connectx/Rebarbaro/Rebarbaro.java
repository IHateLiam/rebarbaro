package connectx.Rebarbaro;


import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.Collections;
import java.util.TreeSet;
import java.util.Random;
import java.util.ArrayList;
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
	private HashMap<Long, Float> transpositionTable2 = new HashMap<Long, Float>();
	private int timeForColumn;
	private boolean treeView = false;
	private CXCell lastMove;

	//private List<Combo> combinations;

	private int DECISIONTREEDEPTH;
	private IncantesimoClonazione transpositionTable;

    /*Default empty constructor*/
    public Rebarbaro() {

    }


    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;
		TIMEOUT = TIMEOUT - 2;
		columns_value = calculate_columns_value(N);
		this.M = M;
		this.N = N;
		this.K = K;
		this.first = first ? CXCellState.P1 : CXCellState.P2;
		this.timeForColumn = (int) ((TIMEOUT * 1000)/N) - 100/N;
		//this.combinations = Combo();
		//System.err.println("timeForColumn: " + timeForColumn);

		//scelta di un valore basso visto che viene modificato dinamicamente
		this.DECISIONTREEDEPTH = 2;	
		this.transpositionTable = new IncantesimoClonazione(M, N);
		debugMode = false;
    }


	public int selectColumn(CXBoard B) {
		START = System.currentTimeMillis(); //per il timeout

		//per il minmax
		float bestScore = Integer.MIN_VALUE; //per il minimax
		int bestCol = -1; //per il minimax
		float score = 0; //per il minimax
		lastMove = B.getLastMove(); //per il minimax

		//per il ciclo for
		int depth;  //depth nei parametri di selectColumn non va bene perchE' java a quanto pare vuole che i parametri siano gli stessi di CXPlayer.selectColumn(..)
		List<Integer> L = new ArrayList<>(Arrays.asList(B.getAvailableColumns())); //lista delle colonne disponibili
		float[] column_scores = new float[N];    //DEBUG debugMode     (la dichiarazione dentro l'if non va bene)

		score = organizeColumns(L, B, true);
		//TODO aggiungere la verifica dei value per creare una lista ordinata in base agli score
		for (int col : L) {
			try{
				depth = DECISIONTREEDEPTH;
				if(debugMode){
					System.err.print("\n marked column: " + B.numOfMarkedCells()); //debug
					System.err.println("\n\n"); //debug
				}

				long columnMinmaxTime = System.currentTimeMillis();
				
				score = minimax(B, depth, col, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, false); //minimax
				score += 0.01;     //se e' tutto a 0, la moltiplicazione dei valori delle colonne viene annullata
				score *= columns_value[col];

				if(System.currentTimeMillis() - columnMinmaxTime < (timeForColumn/2))
					DECISIONTREEDEPTH++;
				else if(DECISIONTREEDEPTH > 2)
					DECISIONTREEDEPTH--;
				

				if (score >= bestScore) { //se il punteggio E' migliore di quello attuale
					bestScore = score; //lo aggiorno
					bestCol = col; //e aggiorno la colonna migliore
				}
				

				//System.err.println(depth);
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
				bestCol = singleMoveBlock(B, B.getAvailableColumns()); //provo a bloccare l'avversario
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
		
		//System.err.println("mossa scelta: " + bestCol); //debug
		//TODO aggiungere la rimozione dei figli di primo livello
		if(B.gameState() != CXGameState.OPEN)
				transpositionTable.deleteChildren(B, bestCol);
		return bestCol; //ritorno la colonna migliore
	}


	//Il codice implementa l'algoritmo minimax con potatura alpha-beta, con una profondita' massima di 4 (scelta arbitraria). La funzione minimax ritorna 1 se il giocatore che sta massimizzando ha vinto, -1 altrimenti; ritorna -1 se il giocatore che sta massimizzando ha perso, 1 altrimenti; 0 in caso di pareggio. La funzione minimax e' ricorsiva, e viene eseguita una volta per ogni colonna disponibile. La funzione minimax riceve come parametri: l'oggetto CXBoard, la profondita' di ricerca, la prima mossa da eseguire, i valori di alpha e beta e una variabile booleana che indica quale giocatore sta massimizzando. La funzione ritorna l'intero corrispondente al punteggio ottenuto dalla mossa.
	//TODO implementare l'utilizzo del incantesimo clonazione
	public float minimax(CXBoard B, int depth, int firstMove, float alpha, float beta, boolean maximizingPlayer){
		long startTime = System.currentTimeMillis(); //per il timeout
		List<Integer> L = new ArrayList<>(Arrays.asList(B.getAvailableColumns())); //lista delle colonne disponibili
		CXGameState state = B.markColumn(firstMove); //marco la prima mossa
		float score = 0;
		
		Mongolfiera gameMove = transpositionTable.getMongolfiera(B);
		
		if(gameMove != null && gameMove.startingMove == lastMove){
			B.unmarkColumn(); //tolgo la mossa
			return gameMove.score;
		}
		

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
			if(treeView){
				for(int i = depth; i < DECISIONTREEDEPTH; i++) { System.err.print("\t");}
				if(maximizingPlayer)
					System.err.println("evaluateM: " + score + " depth: " + depth + " beta: " + beta + " alpha: " + alpha);
				else
					System.err.println("evaluatem: " + score + " depth: " + depth + " beta: " + beta + " alpha: " + alpha);
			}
			return score;
			//return 0;
		}

		L = Arrays.asList(B.getAvailableColumns()); //aggiorno la lista delle colonne disponibili
		if(gameMove != null){
			score = organizeColumns(L, B, maximizingPlayer);
		}

		if (maximizingPlayer) { 
			// Maximize player 1's score
			float maxScore = Float.NEGATIVE_INFINITY;
			for (int col : L) {
				//System.err.println("col: " + col + " " + depth + " " + key + " " + transpositionTable.get(key));
				//System.err.println("for minmax colonna: " + col + " profondità: " + depth); //debug
				if (System.currentTimeMillis() - startTime > timeForColumn) { // check if time is up
					break;
				}
				maxScore = Math.max(maxScore, score);
				alpha = Math.max(alpha, maxScore);
				if (beta <= alpha) {
					// Beta cutoff
					B.unmarkColumn();
					return beta;
				}
				score = minimax(B, depth - 1, col, alpha, beta, false);
				if(treeView){
					for(int i = depth; i < DECISIONTREEDEPTH; i++) { System.err.print("\t");}
					System.err.println("evaluateM: " + score + " depth: " + depth + " beta: " + beta + " alpha: " + alpha);
				}
				
				
			}
			transpositionTable.addBoard(B, gameMove);
			B.unmarkColumn();
			return maxScore;
			
		} else {
			// Minimize player 2's score
			float minScore = Float.POSITIVE_INFINITY;
			for (int col : L) {
				
				//System.err.println("for minmax colonna: " + col); //debug
				if (System.currentTimeMillis() - startTime > timeForColumn) { // check if time is up

					break;
				}
				minScore = Math.min(minScore, score);
				beta = Math.min(beta, minScore);
				if (beta <= alpha) {
					// Alpha cutoff
					B.unmarkColumn();
					return alpha;
				}
				score = minimax(B, depth - 1, col, alpha, beta, true);
				if(treeView){
					for(int i = depth; i < DECISIONTREEDEPTH; i++) { System.err.print("\t");} //debug
					System.err.println("evaluatem: " + score + " depth: " + depth + " beta: " + beta + " alpha:" + alpha);
				}
				
			}
			//transpositionTable.put(key, score);
			B.unmarkColumn();
			return minScore;
		}

	}


	public float evaluationFunction(CXBoard board) {
		CXCell lastCell = board.getLastMove();
		int row = lastCell.i;
		int col = lastCell.j;
		Integer[] L = board.getAvailableColumns();

		if((board.gameState() == CXGameState.WINP1 && CXGameState.WINP1 == myWin)
			|| (board.gameState() == CXGameState.WINP2 && CXGameState.WINP2 == myWin)){
			return 100000;
		}
		else if((board.gameState() == CXGameState.WINP1 && CXGameState.WINP1 != myWin)
			|| (board.gameState() == CXGameState.WINP2 && CXGameState.WINP2 != myWin)){
			return 100000;
		}

		int verticalPieces = nearPieces(row, col, board, lastCell.state, K, 1);
		int orizzontalPieces = nearPieces(row, col, board, lastCell.state, K, 2);
		int diagonalPieces = nearPieces(row, col, board, lastCell.state, K, 3);
		int antiDiagonalPieces = nearPieces(row, col, board, lastCell.state, K, 4);
		
		float score = verticalPieces + orizzontalPieces + diagonalPieces + antiDiagonalPieces;
		//score *= columns_value[col];

		if(board.gameState() == CXGameState.DRAW){
			score *= 2;
		}
		else if(singleMoveWin(board, L) != -1){
			//System.err.println("singleMoveWin"); //debug
			score /= 2;
		}

		score /= board.numOfMarkedCells()/M+N;

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

	if(count == K-1) count *= 20;
	if(count == K-2) count *= 10;
	if(count == K-3) count *= 5;

	return count;
	}
 
	/**
	 * Calcolo il valore di ogni colonna in base alla distanza dal centro
	 * @param boardWidth
	 * @return array di double con i valori delle colonne
	 */
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
	* Verifico se sono già state valutate le colonne
	* ed eventualmente le ordino in base al punteggio
	* @param L lista delle colonne
	* @param B stato attuale della board
	* @return score più alto tra i figli
	* implicitamente ordina la lista L in quanto passata per parametro
	*/
	public float organizeColumns(List<Integer> L, CXBoard board, boolean maximizingPlayer){
		List<Mongolfiera> lastMoveChildren = new ArrayList<Mongolfiera>();
		float score;
		score = transpositionTable.getChildrenScore(board, lastMoveChildren, true, board.numOfMarkedCells());
		if(lastMoveChildren.size() > 0){
			for(Mongolfiera child : lastMoveChildren){
				L.remove(child.column);
				L.add(0, child.column);
			}
		}
		return score;
	}

	/**
	 * Check if we can win in a single move
	 *
	 * Returns the winning column if there is one, otherwise -1
	 */	
	private int singleMoveWin(CXBoard B, Integer[] L) {
		int winCount = 0;
		if (L.length != 0)
			for(int i : L) {
				
				CXGameState state = B.markColumn(i);
				if (state == myWin){
					winCount++;
				}
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


