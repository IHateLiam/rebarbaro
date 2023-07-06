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
import java.util.LinkedList;
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
	private int M, N, X;
	CXCellState first;
	private boolean debugMode;
	private HashMap<String, Float> transpositionTable = new HashMap<String, Float>();

	//private List<Combo> comboList;

	private int DECISIONTREEDEPTH;




    /*Default empty constructor*/
    public Rebarbaro() {

    }

	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

	public String playerName() {
		return "Rebarbaro";
	}

    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs;
		columns_value = calculate_columns_value(N);
		this.M = M;
		this.N = N;
		this.X = X;
		this.first = first ? CXCellState.P1 : CXCellState.P2;

		//this.combinations = Combo();
		
		this.DECISIONTREEDEPTH = 4;


		// (---)   (---)   (---)   (---)   (---)   (---)   
		debugMode = false;
		// (---)   (---)   (---)   (---)   (---)   (---)   
    }

	public int selectColumn(CXBoard board) {
		long start = System.currentTimeMillis(); //per il timeout
		float bestScore = Integer.MIN_VALUE; //per il minimax
		int bestCol = -1; //per il minimax
		int depth = DECISIONTREEDEPTH;
		Integer[] availableColumns = board.getAvailableColumns();

		float[] columnScores = new float[N];

		for (int col : availableColumns) {
			try {
				transpositionTable.clear();

				if (debugMode) {
					System.err.print("\n marked column: " + board.numOfMarkedCells()); //debug
					System.err.println("\n\n"); //debug
				}

				float score = minimax(board, depth, col, Integer.MIN_VALUE, Integer.MAX_VALUE, false, new LinkedList<Combo>(), new LinkedList<Combo>()); //minimax
				//per adesso le combo le passiamo come vuote, volendo si potrebbe fare che il minimax alla fine ti ritorna una lista cosi' ce l'hai buona per dopo bho
				//magari prima del return di selectColumn ci salviamo nei campi di rebarbaro le combo di rebarbaro e dell'avversario, aggiungendoci l'ultima mossa
				score += 0.01; //se e' tutto a 0, la moltiplicazione dei valori delle colonne viene annullata
				score *= columns_value[col];

				if (score >= bestScore) { //se il punteggio E' migliore di quello attuale
					bestScore = score; //lo aggiorno
					bestCol = col; //e aggiorno la colonna migliore
				}

				if (debugMode) {
					System.err.print("\nscore: " + score);
					columnScores[col] = score;
				}

				if (System.currentTimeMillis() - start > TIMEOUT * 9700) { //se ho superato il timeout
					throw new TimeoutException(); //lancio un'eccezione
				}
			} catch (TimeoutException e) {
				System.err.println("Timeout!!! minimax ritorna -1 in selectColumn"); //debug
				break;
			}
		}

		if (debugMode) {
			System.err.print("\n bestCol: " + bestCol + " bestScore: " + bestScore);
		}

		if (bestCol == -1) { //se non ho trovato nessuna mossa vincente
			/*try {
				bestCol = singleMoveBlock(board, availableColumns); //provo a bloccare l'avversario
			} catch (TimeoutException e)*/ { //se non riesco
				System.err.println("Timeout!!! singleMoveBlock ritorna -1 in selectColumn"); //debug
			}
		}

		if (debugMode) {
			System.err.print("\n bestCol: " + bestCol + " bestScore: " + bestScore);

			System.err.print("\n" + "punteggi colonne:\n");
			for (int i = 0; i < N; i++) {
				System.err.print("colonna " + i + ": " + String.format("%9f", columnScores[i]) + "\tvalore colonna: " + columns_value[i] + "\n");
			}
		}

		return bestCol; //ritorno la colonna migliore
	}

	//Il codice implementa l'algoritmo minimax con potatura alpha-beta, con una profondita' massima di 4 (scelta arbitraria). La funzione minimax ritorna 1 se il giocatore che sta massimizzando ha vinto, -1 altrimenti; ritorna -1 se il giocatore che sta massimizzando ha perso, 1 altrimenti; 0 in caso di pareggio. La funzione minimax e' ricorsiva, e viene eseguita una volta per ogni colonna disponibile. La funzione minimax riceve come parametri: l'oggetto CXBoard, la profondita' di ricerca, la prima mossa da eseguire, i valori di alpha e beta e una variabile booleana che indica quale giocatore sta massimizzando. La funzione ritorna l'intero corrispondente al punteggio ottenuto dalla mossa.
	public float minimax(CXBoard board, int depth, int firstMove, float alpha, float beta, boolean maximizingPlayer, LinkedList<Combo> rebarbaroCombos, LinkedList<Combo> advCombos) {
		Integer[] L = board.getAvailableColumns(); // lista delle colonne disponibili
		CXGameState state = board.markColumn(firstMove); // marco la prima mossa

		
		
		

		if (debugMode) {
			System.err.print("\n");
			for (int i = DECISIONTREEDEPTH; i > depth; i--) {
				System.err.print("\t");
			}
			System.err.print("depth: " + (DECISIONTREEDEPTH - depth) + " "); // debug
			System.err.print("col: " + firstMove + "\t\t"); // debug
		}

		if (state == myWin) { // se ho vinto
			if (debugMode) {
				System.err.print("|won | evaluate: " + (maximizingPlayer ? -1 * depth : 1 * depth) + " ");
			}
			board.unmarkColumn(); // tolgo la mossa
			return maximizingPlayer ? -(X * 4) * depth : (X * 4) * depth; // ritorno 1 se sono il giocatore che sta massimizzando, -1 altrimenti

		} else if (state == yourWin) { // se ha vinto l'avversario
			if (debugMode) {
				System.err.print("|lost| evaluate: " + (maximizingPlayer ? -1 * depth : 1 * depth) + " ");
			}
			board.unmarkColumn(); // tolgo la mossa
			return maximizingPlayer ? -(X * 4) * depth : (X * 4) * depth; // ritorno -1 se sono il giocatore che sta massimizzando, 1 altrimenti
		}

		/*
		 * SE esiste una comboList dall'hash prendila da li'
		 * ALTRIMENTI calcola cosi':
		 * refreshCombos(
		 * 				comboList       -> presa dai parametri passati dal minmax (ho le combo del nodo di sopra)
		 * 								   -nel caso questa sia la prima mossa in assoluto posso fare tipo un controllo if comboList.length == 0 allora creaCombo(prima mossa)
		 * 				board			    -> presa dai parametri del minmax
		 * 				cell		    -> new CXCell(RP[firstMove] + 1, firstMove, board.getLastMove().state)
		 * 				myState         -> first
		 * 				lastMoveWasMine -> board.getLastMove().state == first ? true : false
		 * )
		 */

		 //questo if poi va sostituito quando abbiamo le hash table
		if(maximizingPlayer) {
			if(advCombos.size() == 0 && board.getMarkedCells().length > 0) {
				advCombos = calculateAllCombos(advCombos, first == CXCellState.P1 ? CXCellState.P2 : CXCellState.P1, board);
			}
			else if(board.getMarkedCells().length > 0){
				refreshCombos(advCombos, board, board.getLastMove(), first, true);
			}	
		}

		else {		
			if(rebarbaroCombos.size() == 0 && board.getMarkedCells().length > 0) {
				rebarbaroCombos = calculateAllCombos(rebarbaroCombos, first, board);
			}
			else if(board.getMarkedCells().length > 0){
				refreshCombos(rebarbaroCombos, board, board.getLastMove(), first, true);
			}
			if(debugMode) {
				System.err.print("depth: " + (DECISIONTREEDEPTH - depth) + " "); // debug
				System.err.print("col: " + firstMove + "\t\t"); // debug
			}
		}

		 

		if (depth == 0 || state == CXGameState.DRAW) { // se sono arrivato alla profondita' massima o se ho pareggiato
			//float score = maximizingPlayer ? -evaluationFunction(board) * (depth + 1) : evaluationFunction(board) * (depth + 1);    //per adesso commentiamo per provare le combo
			
			/*
			 * questa evaluationFunction verra' implementata con gli evaluate delle combo
			 * sommiamo tutti i valori delle combo nella lista delle combo (lista che ci portiamo dietro come parametro di minimax)
			 * questo da' un indicazione di quanto siamo messi bene a sequenze, ossia possibilita' di vittoria
			 */
			
			float score = maximizingPlayer ? -evaluationFunctionCombos(advCombos) : evaluationFunctionCombos(rebarbaroCombos);

			if (debugMode) {
				System.err.print("evaluate: " + score);
			}
			board.unmarkColumn(); // tolgo la mossa
			return score;
		}

		L = board.getAvailableColumns(); // aggiorno la lista delle colonne disponibili

		if (maximizingPlayer) {
			// Maximize player 1's score
			float maxScore = Integer.MIN_VALUE;
			for (int col : L) {

				float score = minimax(board, depth - 1, col, alpha, beta, false, rebarbaroCombos, advCombos);

				if (debugMode) {
					System.err.print("evaluate: " + score + " ");
				}

				maxScore = Math.max(maxScore, score);
				alpha = Math.max(alpha, score);
				if (beta <= alpha) {
					// Beta cutoff
					break;
				}

			}
			board.unmarkColumn();
			return maxScore;

		} else {
			// Minimize player 2's score
			float minScore = Integer.MAX_VALUE;
			for (int col : L) {

				float score = minimax(board, depth - 1, col, alpha, beta, true, rebarbaroCombos, advCombos);
				minScore = Math.min(minScore, score);
				beta = Math.min(beta, score);
				if (beta <= alpha) {
					// Alpha cutoff
					break;
				}
			}
			board.unmarkColumn();
			return minScore;
		}
	}

	public double[] calculate_columns_value(int boardWidth){
		double[] columns_value = new double[boardWidth];
		for(float i = 0; i < boardWidth; i++){
			columns_value[(int)i] =  i < boardWidth/2 ? ( 1 + (i + 1)/(boardWidth/2) ) / 2 : ( 1 + (boardWidth - i)/(boardWidth/2) ) / 2;
		}
		return columns_value;
	}

	
	//----------funzioni combo---------

	public Combo createCombo(CXBoard board, CXCell cell, Direction direction) {
		//PRECONDIZIONE:  cell DEVE contenere una pedina e il suo state deve essere == first   //non per forza first?
		Combo newCombo = new Combo(first, direction);
		int N_mie = 1;
        int N_vuote = 0;
        int N_interruzioni = 0;
		newCombo.add(cell);
		int N_free_ends = 0;

		CXCellState advCellState = first == CXCellState.P1 ? CXCellState.P2 : CXCellState.P1;

		int[] dir = {0, 0};
		CXCell x = new CXCell(cell.i, cell.j, cell.state);   //questo valore viene poi modificato appena entra nei cicli
		CXCellState old_xCellState = x.state;




		//vedo se ci sono caselle in direzione positiva

		dir = direction.positiveDirection();

		int xi = cell.i + dir[0];
		int xj = cell.j + dir[1];

		//se non entra in questo if non entra nemmeno nel while quindi non importa che non sia inizializzata la x
		if(insideBorders(xi, xj)) {
			x = new CXCell(xi, xj, board.cellState(xi, xj));
			old_xCellState = board.cellState(cell.i, cell.j);
		}

		while(insideBorders(xi, xj) && x.state != advCellState) {

			if (x.state == CXCellState.FREE && old_xCellState == first) {
				N_interruzioni++;
			}

			old_xCellState = x.state;
			x = new CXCell(xi, xj, board.cellState(xi, xj));

			newCombo.add(x);
			
			if (x.state == first) {
				N_mie++;
			}

			else if (x.state == CXCellState.FREE) {
				N_vuote++;
			}

			xi = x.i + dir[0];
			xj = x.j + dir[1];
			
		}
		//se l'ultima casella ad essere stata aggiunta alla combo non e' una casella vuota, significa che quella successiva e' avversaria o fuori dai bordi
		//quindi la combo e' "chiusa" da quel lato
		if(x.state == CXCellState.FREE) 
			N_free_ends++;
		

		//vedo se ci sono caselle in direzione negativa

		dir = direction.negativeDirection();

		xi = cell.i + dir[0];
		xj = cell.j + dir[1];
		
		//se non entra in questo if non entra nemmeno nel while quindi non importa che non sia inizializzata la x
		if(xi >= 0 && xi < M && xj >= 0 && xj < N) {
			x = new CXCell(xi, xj, board.cellState(xi, xj));
			old_xCellState = board.cellState(cell.i, cell.j);
		}
		
		while(x.state != advCellState && xi >= 0 && xi < M && xj >= 0 && xj < N) {
			
			if (x.state == CXCellState.FREE && old_xCellState == first) {
				N_interruzioni++;
			}
			
			old_xCellState = x.state;
			x = new CXCell(xi, xj, board.cellState(xi, xj));

			newCombo.addFirst(x);    //aggiungo all'inizio cosi' ho alle due estremita' della lista le due estremita' della sequenza di caselle

			if (x.state == first) {
				N_mie++;
			}

			else if (x.state == CXCellState.FREE) {
				N_vuote++;
			}

			xi = x.i + dir[0];
			xj = x.j + dir[1];
			
		}
		//se l'ultima casella ad essere stata aggiunta alla combo non e' una casella vuota, significa che quella successiva e' avversaria o fuori dai bordi
		//quindi la combo e' "chiusa" da quel lato
		if(x.state == CXCellState.FREE) 
			N_free_ends++;
		
		
		newCombo.N_mie = N_mie;
		newCombo.N_vuote = N_vuote;
		newCombo.N_interruzioni = N_interruzioni;
		newCombo.setNumberOfFreeEnds(N_free_ends);    
		//controllo se la combo e' utilizzabile per vincere
		if(newCombo.getLength() < X)
			newCombo.deadCombo = true;

		newCombo.setValue(newCombo.calculateComboValue(0));

		return newCombo;
	}



	public int sign(int number) {
		if(number > 0) return +1;
		else if(number == 0) return 0;
		else return -1;
	}

	//due coordinate sono lungo la stessa retta di orientamento direzione
	public boolean aligned(int i1, int j1, int i2, int j2, Direction direction) {
		int i_r = i1 -i2;
		int j_r = j1 - j2;
		int[] dir = direction.positiveDirection();

		if (sign(i_r) == sign(dir[0]) && sign(j_r) == sign(dir[1])) {
			return true;
		}
		else {
			dir = direction.negativeDirection();
			if (sign(i_r) == sign(dir[0]) && sign(j_r) == sign(dir[1])) {
				return true;
			}

			else return false;
		}
	}

	//nel caso la combo non esista, ne ritorna una di lunghezza 0
	public Combo findCombo(LinkedList<Combo> comboList, CXCell cell, Direction direction) {
		for(Combo combo : comboList) {
			if (combo.getDirection() == direction) {
				if (aligned(cell.i, cell.j, combo.firstCell().i, combo.firstCell().i, direction)) {
					for(CXCell comboCell : combo.getCells()) {
						if (cell == comboCell) {
							return combo;
						}
					}
				} 
			}
		}

		return null;   //ha lista vuota e lunghezza 0, come se fosse un NULL
	}

	public LinkedList<Combo> refreshCombos(LinkedList<Combo> comboList, CXBoard board, CXCell cell, CXCellState myState, boolean lastMoveWasMine) {
		//PRECONDIZIONE: cell.state DEVE essere == first 

		Direction[] directions = {  Direction.Vertical,
									Direction.Horizontal,
									Direction.Diagonal, 
									Direction.AntiDiagonal};
		CXCellState advCellState = myState == CXCellState.P1 ? CXCellState.P2 : CXCellState.P1;
		if(lastMoveWasMine) {
			for(Direction direction : directions) {
				int[] dir_pos = direction.positiveDirection();
				int[] dir_neg = direction.negativeDirection();

				CXCellState cell_p_state;
				CXCellState cell_n_state;


				//essere fuori dai bordi in questa funzione e' equivalente all'essere avversario
				if(insideBorders(cell.i + dir_pos[0], cell.j + dir_pos[1])) 
					cell_p_state = board.cellState(cell.i + dir_pos[0], cell.j + dir_pos[1]);
				else 
					cell_p_state = advCellState;    

				if(insideBorders(cell.i + dir_neg[0], cell.j + dir_neg[1]))
					 cell_n_state = board.cellState(cell.i + dir_neg[0], cell.j + dir_neg[1]);
				else 
					cell_n_state = advCellState;    
				


				if(cell_p_state == cell_n_state && cell_n_state == advCellState) {
					//praticamente ho messo una pedina in un posto isolato
					comboList.add(createCombo(board, cell, direction));
				}
				else {
					//tolgo e rimetto la combo, ossia la ricalcolo
					comboList.remove(findCombo(comboList, cell, direction));
					comboList.add(createCombo(board, cell, direction));
				}
			}

		}

		//se l'ultima messa e' dell'avversario
		else {

			for(Direction direction : directions) {
				int[] dir_pos = direction.positiveDirection();
				int[] dir_neg = direction.negativeDirection();

				
				CXCell cell_p = new CXCell(cell.i + dir_pos[0], cell.j + dir_pos[1], board.cellState(cell.i + dir_pos[0], cell.j + dir_pos[1]));
				CXCell cell_n = new CXCell(cell.i + dir_neg[0], cell.j + dir_neg[1], board.cellState(cell.i + dir_neg[0], cell.j + dir_neg[1]));

				if(cell_p.state == cell_n.state && cell_n.state == advCellState) {
					//non succede nulla 
				}
				else if(cell_p.state == cell_n.state && cell_n.state == myState) {
					//cell_p e cell_n facevano sicuramente parte della stessa combo
					//quindi la spezzo
					comboList.remove(findCombo(comboList, cell_p, direction));
					createCombo(board, cell_p, direction);
					createCombo(board, cell_n, direction);
				}
				else if(cell_p.state != advCellState && cell_n.state == advCellState) {
					//controllo che lungo la direzione positiva ci sia una mia pedina
					while(cell_p.state != myState && 
					cell_p.i >= 0 && cell_p.i < M && cell_p.j >= 0 && cell_p.j < N) {
						cell_p = new CXCell(cell_p.i + dir_pos[0], cell_p.j + dir_pos[1], board.cellState(cell_p.i + dir_pos[0], cell_p.j + dir_pos[1]));
					}
					//se alla fine una mia pedina c'era
					if(cell_p.state == myState) {
						//tolgo e ricalcolo la combo
						comboList.remove(findCombo(comboList, cell_p, direction));   
						comboList.add(createCombo(board, cell_p, direction));
					}
				}
				else if(cell_p.state == advCellState && cell_n.state != advCellState) {
					//controllo che lungo la direzione positiva ci sia una mia pedina
					while(cell_n.state != myState && 
					cell_n.i >= 0 && cell_n.i < M && cell_n.j >= 0 && cell_n.j < N) {
						cell_n = new CXCell(cell_n.i + dir_neg[0], cell_n.j + dir_neg[1], board.cellState(cell_n.i + dir_neg[0], cell_n.j + dir_neg[1]));
					}
					//se alla fine una mia pedina c'era
					if(cell_n.state == myState) {
						//tolgo e ricalcolo la combo
						comboList.remove(findCombo(comboList, cell_n, direction));   
						comboList.add(createCombo(board, cell_n, direction));
					}
				}
			}
		}


		return comboList;
	}

	public LinkedList<Combo> calculateAllCombos(LinkedList<Combo> comboList, CXCellState myState, CXBoard board) {
		CXCell[] markedCells = board.getMarkedCells();

		Direction[] directions = {  Direction.Vertical,
									Direction.Horizontal,
									Direction.Diagonal, 
									Direction.AntiDiagonal};

		for(CXCell cell : markedCells) {
			for(Direction direction : directions) {
				if(cell.state == myState) {
					if(findCombo(comboList, cell, direction) == null) {
						comboList.add(createCombo(board, cell, direction));
						
					}
				}
			}
		}
		return comboList;
	}

	public int evaluationFunctionCombos(LinkedList<Combo> comboList) {
		int somma = 0;
		for(Combo combo : comboList) {
			somma += combo.getValue();
		}

		return somma;
	}

	public boolean insideBorders(int row, int col) {
		return (row >= 0 && row < M && col >= 0 && col < N);
	}
}

	
	/*This function is used to determine how many pieces of the same color are in the direction specified by the parameter direction. The variable direction can be 1, 2, 3, or 4, representing the four diagonal directions. The variable state represents the color of the pieces, and the variable k represents how many pieces in a row will win the game. The variable row and col represent the row and column of the cell where the current piece is located. The function returns the number of pieces of the same color as the current piece in the direction specified by the parameter direction.*/
	/*
	private int nearPieces(int row, int col, CXBoard board, CXCellState state, int k, int direction) {
		int pieces = 0;
		int i = row;
		int j = col;
		int count = 0;
		while (count < k) {
			if (direction == 1) {
				i--;
			} else if (direction == 2) {
				j--;
			} else if (direction == 3) {
				i--;
				j--;
			} else if (direction == 4) {
				i--;
				j++;
			}
			if (i < 0 || j < 0 || i >= M || j >= N) {
				break;
			}
			if (board.cellState(i,j) == state) {
				pieces++;
			} else {
				break;
			}
			count++;
		}
		return pieces;
	}
	*/

		/*Questa funzione calcola il bonus per le colonne che contengono pezzi dell'avversario (cioe' dello stato opposto a quello passato come parametro). La funzione conta il numero di pezzi dell'avversario in ogni colonna e calcola il bonus come 2^(n-1), dove n e' il numero di pezzi dell'avversario nella colonna. In questo modo, le colonne con piu' pezzi dell'avversario ricevono un bonus maggiore rispetto alle colonne con meno pezzi dell'avversario. Infine, la funzione restituisce il bonus calcolato. */
	/*
	private int getOpponentColumns(CXBoard board, CXCellState state) {
		int bonus = 0;
		CXCellState opponentState = (state == CXCellState.P1) ? CXCellState.P2 : CXCellState.P1; // stato dell'avversario
		for (int col = 0; col < board.N; col++) {
			int count = 0;
			for (int row = 0; row < board.M; row++) {
				if (board.cellState(row, col) == opponentState) {
					count++;
				}
			}
			if (count > 0) {
				bonus += (int) Math.pow(2, count - 1);
			}
		}
		return bonus;
	}
	*/

	/*
	 * 		private float evaluationFunction(CXBoard board) {
		CXCell lastCell = board.getLastMove();
		int row = lastCell.i;
		int col = lastCell.j;
		int score = 0;

		/*
		int verticalPieces = nearPieces(row, col, board, lastCell.state, X, 1);
		int orizzontalPieces = nearPieces(row, col, board, lastCell.state, X, 2);
		int diagonalPieces = nearPieces(row, col, board, lastCell.state, X, 3);
		int antiDiagonalPieces = nearPieces(row, col, board, lastCell.state, X, 4);
		
		score = verticalPieces + orizzontalPieces + diagonalPieces + antiDiagonalPieces;
		*/

		/*
		//buono ma gia' calcolato da un'altra funzione
		// Aggiungi un bonus per le colonne centrali
		int centerCol = N / 2;
		if (col == centerCol) {
			score += 2;
		} else if (col == centerCol - 1 || col == centerCol + 1) {
			score += 1;
		}
		*/

		/*
		//non capisco perche' le righe sotto dovrebbero essere meglio, se effettivamente danno vantaggio scommentate
		// Aggiungi un bonus per le righe inferiori
		int bottomRow = M - 1;
		if (row == bottomRow) {
			score += 2;
		} else if (row == bottomRow - 1) {
			score += 1;
		}
		*/
/* 
		// Aggiungi un bonus per le sequenze di pezzi gia' presenti sul tabellone
		int sequenceBonus = getSequenceBonus(board, lastCell.state);
		score += sequenceBonus;

		// Aggiungi un bonus per le opportunita' di creare blocchi
		int blockBonus = getBlockBonus(board, lastCell.state);
		score += blockBonus;

		//                 succoso
		// Aggiungi un bonus per le opportunita' di creare sequenze
		int sequenceOpportunityBonus = getSequenceOpportunityBonus(board, lastCell.state);
		score += sequenceOpportunityBonus;

		// Aggiungi un bonus per le colonne con pochi pezzi
		int emptyColumns = getEmptyColumns(board);
		int emptyColumnBonus = emptyColumns * 2;
		score += emptyColumnBonus;

		// Aggiungi un bonus per le colonne con pezzi dello stesso colore
		int sameColorColumns = getSameColorColumns(board, lastCell.state);
		int sameColorColumnBonus = sameColorColumns * 2;
		score += sameColorColumnBonus;

		/*
		// Aggiungi un bonus per le colonne con pezzi dell'avversario
		int opponentColumns = getOpponentColumns(board, lastCell.state);
		int opponentColumnBonus = opponentColumns * 2;
		score += opponentColumnBonus;
		*/
	/*
		// Aggiungi un bonus per le righe con pezzi dello stesso colore
		int sameColorRows = getSameColorRows(board, lastCell.state);
		int sameColorRowBonus = sameColorRows * 2;
		score += sameColorRowBonus;

		/*
		// Aggiungi un bonus per le righe con pezzi dell'avversario
		int opponentRows = getOpponentRows(board, lastCell.state);
		int opponentRowBonus = opponentRows * 2;
		score += opponentRowBonus;
		*/

		/*
		// Aggiungi un bonus per le celle vicine ai bordi del tabellone
		int edgeBonus = getEdgeBonus(board, lastCell);
		score += edgeBonus;
		*/

		/*
		//non e' male ma fa getMobilityBonus fa lo stesso calcolo ma considerando le celle vuote, mentre questa calcola solo le celle nostre
		//ma in realta' noi preferiamo avere tante pedine amiche vicino, perche' aumenta la probabilita' di fare blocchi/lunghe sequenze
		//quindi il calcolo di getMobilityBonus ci sta, questo ci penalizza
		// Aggiungi un bonus per i pezzi isolati
		int isolatedPiecesBonus = getIsolatedPiecesBonus(board, lastCell);
		score += isolatedPiecesBonus;
		*/
		
		/*
		// Aggiungi un bonus per i pezzi in angoli o bordi
		int cornerAndEdgePiecesBonus = getCornerAndEdgePiecesBonus(board, lastCell);
		score += cornerAndEdgePiecesBonus;
		*/
/* 
		//sbrodolone
		// Aggiungi un bonus per la mobilita'
		int mobilityBonus = getMobilityBonus(board, lastCell);
		score += mobilityBonus;

		/*
		-----------
		// DA SCOMMENTARE SCEGLIENDO BENE LE POSIZIONI CHIAVE
		----------
		// Aggiungi un bonus per la presenza di pezzi in posizioni chiave
		int keyPiecesBonus = getKeyPiecesBonus(board, lastCell.state);
		score += keyPiecesBonus;
		*/

		/*
		//carino ma finisce per favorire posizioni vicino a pedine avversiarie solo per il fatto di esistere
		// Aggiungi un bonus per la presenza di pezzi che possono catturare o bloccare
		int captureAndBlockBonus = getCaptureAndBlockBonus(board, lastCell.state);
		score += captureAndBlockBonus;
		*/
	/* 
		// Aggiungi un bonus per la presenza di pezzi che possono vincere la partita
		int winningPiecesBonus = getWinningPiecesBonus(board, lastCell.state);
		score += winningPiecesBonus;

		return score;
	}


	 */
	/*
	 	private float evaluationFunction(CXBoard board) {
		CXCell lastCell = board.getLastMove();
		int row = lastCell.i;
		int col = lastCell.j;
		int score = 0;

		// Aggiungi un bonus per le sequenze di pezzi gia' presenti sul tabellone
		int sequenceBonus = getSequenceBonus(board, lastCell.state);
		score += sequenceBonus;

		// Aggiungi un bonus per le opportunita' di creare blocchi
		int blockBonus = getBlockBonus(board, lastCell.state);
		score += blockBonus;

		// Aggiungi un bonus per le opportunita' di creare sequenze
		int sequenceOpportunityBonus = getSequenceOpportunityBonus(board, lastCell.state);
		score += sequenceOpportunityBonus;

		// Aggiungi un bonus per le colonne con pochi pezzi
		int emptyColumns = getEmptyColumns(board);
		int emptyColumnBonus = emptyColumns * 2;
		score += emptyColumnBonus;

		// Aggiungi un bonus per le colonne con pezzi dello stesso colore
		int sameColorColumns = getSameColorColumns(board, lastCell.state);
		int sameColorColumnBonus = sameColorColumns * 2;
		score += sameColorColumnBonus;

		// Aggiungi un bonus per le righe con pezzi dello stesso colore
		int sameColorRows = getSameColorRows(board, lastCell.state);
		int sameColorRowBonus = sameColorRows * 2;
		score += sameColorRowBonus;
		
		// Aggiungi un bonus per la mobilita'
		int mobilityBonus = getMobilityBonus(board, lastCell);
		score += mobilityBonus;

		/*
		-----------
		// DA SCOMMENTARE SCEGLIENDO BENE LE POSIZIONI CHIAVE
		----------
		// Aggiungi un bonus per la presenza di pezzi in posizioni chiave
		int keyPiecesBonus = getKeyPiecesBonus(board, lastCell.state);
		score += keyPiecesBonus;
		*/
/*
		// Aggiungi un bonus per la presenza di pezzi che possono vincere la partita
		int winningPiecesBonus = getWinningPiecesBonus(board, lastCell.state);
		score += winningPiecesBonus;

		return score;
	}

	
	/*	Questa funzione calcola il bonus per le sequenze di pezzi dello stesso stato (1 o 2) che passano per l'ultima mossa effettuata sul tabellone. Per ogni direzione (orizzontale, verticale, diagonale e antidiagonale), la funzione conta il numero di pezzi dello stesso stato consecutivi a partire dalla cella dell'ultima mossa e in quella direzione. Se il numero di pezzi consecutivi e' maggiore o uguale a X (la lunghezza della sequenza richiesta per vincere), allora la funzione aggiunge al bonus il quadrato del numero di pezzi consecutivi. In questo modo, le sequenze piu' lunghe ricevono un bonus maggiore rispetto alle sequenze piu' corte. Infine, la funzione restituisce il totale dei bonus per tutte le direzioni. */
	/*
	private int getSequenceBonus(CXBoard board, CXCellState state) {
		int bonus = 0;
		int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {-1, 1}}; // direzioni orizzontale, verticale, diagonale e antidiagonale
		for (int[] dir : directions) {
			int row = board.getLastMove().i;
			int col = board.getLastMove().j;
			int count = 0;
			while (row >= 0 && row < M && col >= 0 && col < board.N && board.cellState(row, col) == state) {
				count++;
				row += dir[0];
				col += dir[1];
			}
			if (count >= X) {
				bonus += count * count;
			}
		}
		return bonus;
	}
	*/
	/*Questa funzione calcola il bonus per le posizioni che bloccano le sequenze di pezzi dell'avversario che passano per l'ultima mossa effettuata sul tabellone. Per ogni direzione (orizzontale, verticale, diagonale e antidiagonale), la funzione conta il numero di pezzi dell'avversario consecutivi a partire dalla cella dell'ultima mossa e in quella direzione. Se la cella successiva alla sequenza di pezzi dell'avversario e' vuota, allora la funzione aggiunge al bonus il quadrato del numero di pezzi dell'avversario consecutivi. In questo modo, le posizioni che bloccano sequenze piu' lunghe dell'avversario ricevono un bonus maggiore rispetto alle posizioni che bloccano sequenze piu' corte. Infine, la funzione restituisce il totale dei bonus per tutte le direzioni. */
	/*
	private int getBlockBonus(CXBoard board, CXCellState state) {
		int bonus = 0;
		int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {-1, 1}}; // direzioni orizzontale, verticale, diagonale e antidiagonale
		for (int[] dir : directions) {
			int row = board.getLastMove().i;
			int col = board.getLastMove().j;
			int count = 0;
			while (row >= 0 && row < board.M && col >= 0 && col < board.N && board.cellState(row, col) != CXCellState.FREE) {
				count++;
				row += dir[0];
				col += dir[1];
			}
			if (row >= 0 && row < board.M && col >= 0 && col < board.N && board.cellState(row, col)== CXCellState.FREE) {
				bonus += count * count;
			}
		}
		return bonus;
	}
	*/
	/*Questa funzione calcola il bonus per le posizioni che permettono di creare sequenze di pezzi dello stesso stato (1 o 2) che passano per l'ultima mossa effettuata sul tabellone. Per ogni direzione (orizzontale, verticale, diagonale e antidiagonale), la funzione conta il numero di pezzi dello stesso stato consecutivi a partire dalla cella dell'ultima mossa e in quella direzione. Se la cella successiva alla sequenza di pezzi dello stesso stato e' vuota, allora la funzione cerca di contare il numero di pezzi dello stesso stato consecutivi a partire dalla cella successiva e in quella direzione. Se il numero di pezzi consecutivi piu' il numero di pezzi consecutivi nella cella successiva e' maggiore o uguale a X (la lunghezza della sequenza richiesta per vincere), allora la funzione aggiunge al bonus il quadrato del numero di pezzi dello stesso stato consecutivi nella cella successiva. In questo modo, le posizioni che permettono di creare sequenze piu' lunghe ricevono un bonus maggiore rispetto alle posizioni che permettono di creare sequenze piu' corte. Infine, la funzione restituisce il totale dei bonus per tutte le direzioni. */
	/*
	private int getSequenceOpportunityBonus(CXBoard board, CXCellState state) {
		int bonus = 0;
		int[][] directions = {{0, 1}, {1, 0}, {1, 1}, {-1, 1}}; // direzioni orizzontale, verticale, diagonale e antidiagonale
		for (int[] dir : directions) {
			int row = board.getLastMove().i;
			int col = board.getLastMove().j;
			int count = 0;
			while (row >= 0 && row < board.M && col >= 0 && col < board.N && board.cellState(row, col) == state) {
				count++;
				row += dir[0];
				col += dir[1];
			}
			int newRow = row + dir[0];
			int newCol = col + dir[1];
			if (newRow >= 0 && newRow < board.M && newCol >= 0 && newCol < board.N && board.cellState(newRow, newCol) == CXCellState.FREE) {
				int newCount = 1;
				newRow += dir[0];
				newCol += dir[1];
				while (newRow >= 0 && newRow < board.M && newCol >= 0 && newCol < board.N && board.cellState(newRow, newCol) == state) {
					newCount++;
					newRow += dir[0];
					newCol += dir[1];
				}
				if (count + newCount >= X) {
					bonus += newCount * newCount;
				}
			}
		}
		return bonus;
	}
	*/
	/*Questa funzione calcola il bonus per le colonne che hanno pochi pezzi. La funzione conta il numero di colonne vuote sul tabellone e calcola il bonus come 2^(n-1), dove n e' il numero di colonne vuote. In questo modo, le colonne con meno pezzi ricevono un bonus maggiore rispetto alle colonne con piu' pezzi. Infine, la funzione restituisce il bonus calcolato. */
	/*
	private int getEmptyColumns(CXBoard board) {
		int bonus = 0;
		int emptyColumns = 0;
		for (int col = 0; col < board.N; col++) {
			if (board.cellState(0, col) == CXCellState.FREE) {
				emptyColumns++;
			}
		}
		if (emptyColumns > 0) {
			bonus = (int) Math.pow(2, emptyColumns - 1);
		}
		return bonus;
	}
	*/
	/*Questa funzione calcola il bonus per le colonne che contengono pezzi dello stesso colore dello stato passato come parametro. La funzione conta il numero di pezzi dello stesso colore in ogni colonna e calcola il bonus come 2^(n-1), dove n e' il numero di pezzi dello stesso colore nella colonna. In questo modo, le colonne con piu' pezzi dello stesso colore ricevono un bonus maggiore rispetto alle colonne con meno pezzi dello stesso colore. Infine, la funzione restituisce il bonus calcolato. */
	/*
	private int getSameColorColumns(CXBoard board, CXCellState state) {
		int bonus = 0;
		for (int col = 0; col < board.N; col++) {
			int count = 0;
			for (int row = 0; row < board.M; row++) {
				if (board.cellState(row, col) == state) {
					count++;
				}
			}
			if (count > 0) {
				bonus += (int) Math.pow(2, count - 1);
			}
		}
		return bonus;
	}
	*/
	/*Questa funzione calcola il bonus per le righe che contengono pezzi dello stesso colore dello stato passato come parametro. La funzione conta il numero di pezzi dello stesso colore in ogni riga e calcola il bonus come 2^(n-1), dove n e' il numero di pezzi dello stesso colore nella riga. In questo modo, le righe con piu' pezzi dello stesso colore ricevono un bonus maggiore rispetto alle righe con meno pezzi dello stesso colore. Infine, la funzione restituisce il bonus calcolato. */
	/*
	private int getSameColorRows(CXBoard board, CXCellState state) {
		int bonus = 0;
		for (int row = 0; row < board.M; row++) {
			int count = 0;
			for (int col = 0; col < board.N; col++) {
				if (board.cellState(row, col) == state) {
					count++;
				}
			}
			if (count > 0) {
				bonus += (int) Math.pow(2, count - 1);
			}
		}
		return bonus;
	}
	*/
	/*Questa funzione calcola il bonus per le righe che contengono pezzi dell'avversario (cioe' dello stato opposto a quello passato come parametro). La funzione conta il numero di pezzi dell'avversario in ogni riga e calcola il bonus come 2^(n-1), dove n e' il numero di pezzi dell'avversario nella riga. In questo modo, le righe con piu' pezzi dell'avversario ricevono un bonus maggiore rispetto alle righe con meno pezzi dell'avversario. Infine, la funzione restituisce il bonus calcolato */
	/*
	private int getOpponentRows(CXBoard board, CXCellState state) {
		int bonus = 0;
		CXCellState opponentState = (state == CXCellState.P1) ? CXCellState.P2 : CXCellState.P1; // stato dell'avversario
		for (int row = 0; row < board.M; row++) {
			int count = 0;
			for (int col = 0; col < board.N; col++) {
				if (board.cellState(row, col) == opponentState) {
					count++;
				}
			}
			if (count > 0) {
				bonus += (int) Math.pow(2, count - 1);
			}
		}
		return bonus;
	}
	*/
	/*Questa funzione calcola il bonus per la mobilita', cioe' il numero di posizioni vuote adiacenti alla cella dell'ultima mossa. La funzione controlla se ci sono posizioni vuote nelle otto direzioni adiacenti alla cella dell'ultima mossa e assegna un bonus di 1 punto per ogni posizione vuota trovata. In questo modo, le posizioni che consentono di avere piu' opzioni per la mossa successiva ricevono un bonus maggiore. Infine, la funzione restituisce il bonus calcolato. */
	/*
	private int getMobilityBonus(CXBoard board, CXCell lastMove) {
		int bonus = 0;
		CXCellState state = lastMove.state;
		int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
		for (int[] dir : directions) {
			int r = lastMove.i + dir[0];
			int c = lastMove.j + dir[1];
			if (r >= 0 && r < board.M && c >= 0 && c < board.N && board.cellState(r, c) == CXCellState.FREE) {
				bonus++;
			}
		}
		return bonus;
	}
	*/
	/*Questa funzione calcola il bonus per la presenza di pezzi in posizioni chiave del tabellone. La funzione controlla se ci sono pezzi dello stato passato come parametro in alcune posizioni chiave del tabellone e assegna un bonus di 2 punti per ogni posizione chiave in cui e' presente un pezzo dello stato. In questo modo, le posizioni chiave del tabellone ricevono un bonus maggiore rispetto alle altre posizioni. Le posizioni chiave utilizzate in questo esempio sono {0, 3}, {1, 2}, {1, 3}, {1, 4}, ma puoi modificare questa lista a seconda delle tue esigenze. Infine, la funzione restituisce il bonus calcolato */
	/*
	private int getKeyPiecesBonus(CXBoard board) {
		int bonus = 0;
		int[][] keyPositions = getKeyPositions(board.M, board.N, board.X);
		for (int[] pos : keyPositions) {
			if (board.cellState(pos[0], pos[1]) == board.getLastMove().state) {
				bonus += 2;
			}
		}
		return bonus;
	}
	*/
	/*
	private int[][] getKeyPositions(int M, int N, int X) {
		int[][] keyPositions = new int[M * N][2];
		int index = 0;

		// Check horizontal key positions
		for (int row = 0; row < M; row++) {
			for (int col = 0; col <= N - X; col++) {
				for (int i = 0; i < X; i++) {
					keyPositions[index][0] = row;
					keyPositions[index][1] = col + i;
					index++;
				}
			}
		}

		// Check vertical key positions
		for (int row = 0; row <= M - X; row++) {
			for (int col = 0; col < N; col++) {
				for (int i = 0; i < X; i++) {
					keyPositions[index][0] = row + i;
					keyPositions[index][1] = col;
					index++;
				}
			}
		}

		// Check diagonal key positions (top-left to bottom-right)
		for (int row = 0; row <= M - X; row++) {
			for (int col = 0; col <= N - X; col++) {
				for (int i = 0; i < X; i++) {
					keyPositions[index][0] = row + i;
					keyPositions[index][1] = col + i;
					index++;
				}
			}
		}

		// Check diagonal key positions (top-right to bottom-left)
		for (int row = 0; row <= M - X; row++) {
			for (int col = X - 1; col < X; col++) {
				for (int i = 0; i < X; i++) {
					keyPositions[index][0] = row + i;
					keyPositions[index][1] = col - i;
					index++;
				}
			}
		}

		return keyPositions;
	}

	*/
	/*Questa funzione calcola il bonus per i pezzi vincenti, cioe' i pezzi che fanno parte di una sequenza vincente di almeno 4 pezzi dello stesso colore. La funzione controlla se ci sono almeno 4 pezzi dello stato passato come parametro in una qualsiasi delle otto direzioni adiacenti alla cella dell'ultima mossa e assegna un bonus pari al numero di pezzi vincenti trovati. In questo modo, i pezzi che fanno parte di una sequenza vincente ricevono un bonus maggiore rispetto agli altri pezzi. Infine, la funzione restituisce il bonus calcolato. */
	/*
	private int getWinningPiecesBonus(CXBoard board, CXCellState state) {
		int bonus = 0;
		int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
		for (int[] dir : directions) {
			int count = 0;
			int r = board.getLastMove().i + dir[0];
			int c = board.getLastMove().j + dir[1];
			while (r >= 0 && r < board.M && c >= 0 && c < board.N && board.cellState(r, c) == state) {
				count++;
				r += dir[0];
				c += dir[1];
			}
			if (count >= 3) {
				bonus += count; // bonus per pezzi vincenti
			}
		}
		return bonus;
	}
	*/
	/* 
	public void addHash(CXBoard board, Float value){
		String board = "";
		CXCell[] L = board.getMarkedCells(); 
		for(CXCell i : L){
			board += i.i;
			board += i.j;
		}
		try{
			board = hash(board);
			transpositionTable.put(board, value);
			System.err.println(board);
		} catch(Exception e){
			System.err.println("Errore nell'aggiunta della board alla transposition table");
		}
	}


	public String hash(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        BigInteger number = new BigInteger(1, hash);
        StringBuilder hexString = new StringBuilder(number.toString(16));
        while (hexString.length() < 32) {
            hexString.insert(0, '0');
        }
        return hexString.toString();
    }
	*/
	

