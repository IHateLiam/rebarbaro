package connectx.RebarbaroHashish;

//ADESSO TOLGO I DEBUGG PER IL NULL POINTER (che probabilmente era solo una mancata inizializzazione delle liste delle freeWinningCells)

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
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;

import java.util.HashMap;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;


public class RebarbaroHashish implements CXPlayer {
    private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
	private int  TIMEOUT;
	private long START;
	private double[] columns_value;
	private int M, N, X;
	CXCellState first;
	CXCellState myCellState;
	CXCellState advCellState;
	private boolean debugMode;
	private HashMap<String, Float> transpositionTable = new HashMap<String, Float>();

	private LinkedList<Combo> myComboList;
	private LinkedList<Combo> advComboList;
	
	private LinkedList<CXCell> myWinningFreeCells;
	private LinkedList<CXCell> advWinningFreeCells;
	
	
	private int timeForColumn;
	private int DECISIONTREEDEPTH;

	private int totalBoardCells;    
	private int halfGameCells;      //numero di celle tale da determinare che siamo a meta' partita, serve piu' che altro a non doverla calcolare tutte le volte


	
    /*Default empty constructor*/
    public RebarbaroHashish() {

    }

	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

	public String playerName() {
		return "RebarbaroHashish";
	}

    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = timeout_in_secs / 2;
		columns_value = calculate_columns_value(N);
		this.M = M;
		this.N = N;
		this.X = X;
		this.first = first ? CXCellState.P1 : CXCellState.P2;
		this.myCellState = this.first;
		this.advCellState = first ? CXCellState.P2 : CXCellState.P1;

		this.timeForColumn = (int) ((TIMEOUT * 1000)/N) - 100/N;

		this.myComboList  = new LinkedList<Combo>();
		this.advComboList = new LinkedList<Combo>();
		this.myWinningFreeCells = new LinkedList<CXCell>();
		this.advWinningFreeCells = new LinkedList<CXCell>();

		
		this.DECISIONTREEDEPTH = 2;

		this.totalBoardCells = M * N;
		this.halfGameCells = this.totalBoardCells / 2 - X;     

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

		boolean halfBoardFull = board.numOfMarkedCells() > halfGameCells;

		float[] columnScores = new float[N];

		//aggiorno la comboList avversaria
		if(board.numOfMarkedCells() > 0)       //sostanzialmente entra in questo if se gioca come secondo
			refreshCombos(advComboList, board, board.getLastMove(), advCellState, true, halfBoardFull, advWinningFreeCells);
		
		//non uso le originali perche' il minimax fa delle "ipotesi". non voglio che le ipotesi vengano salvate: la lista diventerebbe enorme e non controllabile
		LinkedList<Combo> myComboListCopy = new LinkedList<Combo>(myComboList);
		LinkedList<Combo> advCombosCopy = new LinkedList<Combo>(advComboList);
		LinkedList<CXCell> myWinningFreeCellsCopy = new LinkedList<CXCell>(myWinningFreeCells);
		LinkedList<CXCell> advWinningFreeCellsCopy = new LinkedList<CXCell>(advWinningFreeCells);
		
		for (int col : availableColumns) {
			try {
				
				if (debugMode) {
					System.err.print("\n marked column: " + board.numOfMarkedCells()); //debug
					System.err.println("\n\n"); //debug
				}

				depth = DECISIONTREEDEPTH;
				long timeForColumn = System.currentTimeMillis();

				float score = minimax(board, depth, col, Integer.MIN_VALUE, Integer.MAX_VALUE, false, myComboListCopy, advCombosCopy, 
										myWinningFreeCellsCopy, advWinningFreeCellsCopy, halfBoardFull); //minimax
				//per adesso le combo le passiamo come vuote, volendo si potrebbe fare che il minimax alla fine ti ritorna una lista cosi' ce l'hai buona per dopo bho
				//magari prima del return di selectColumn ci salviamo nei campi di rebarbaro le combo di rebarbaro e dell'avversario, aggiungendoci l'ultima mossa

				if(!halfBoardFull) {    //voglio che le colonne centrali le scelga solo all'inizio della partita
					score += 0.01; //se e' tutto a 0, la moltiplicazione dei valori delle colonne viene annullata
					score *= columns_value[col];
				}

				if (score >= bestScore) { //se il punteggio E' migliore di quello attuale
					bestScore = score; //lo aggiorno
					bestCol = col; //e aggiorno la colonna migliore
				}

				if (debugMode) {
					System.err.print("\nscore: " + score);
					columnScores[col] = score;    //gia' moltiplicato per il columns_value[i]
				}

				if(System.currentTimeMillis() - timeForColumn < (timeForColumn/3))
					DECISIONTREEDEPTH++;
				else if(DECISIONTREEDEPTH > 2)
					DECISIONTREEDEPTH--;

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

		//sarebbe da fare che se entra in quest'if ne sceglie una a caso, -1 meglio non ritornarlo
		if (bestCol == -1) { //se non ho trovato nessuna mossa vincente    TODO
			/*try {
				bestCol = singleMoveBlock(board, availableColumns); //provo a bloccare l'avversario
			} catch (TimeoutException e)*/ { //se non riesco
				System.err.println("Timeout!!! singleMoveBlock ritorna -1 in selectColumn"); //debug
			}
		}

		//aggiorno le mie combo
		board.markColumn(bestCol);
		refreshCombos(myComboList, board, board.getLastMove(), myCellState, true, halfBoardFull, myWinningFreeCells);
		board.unmarkColumn();


		if (debugMode) {
			System.err.print("\n bestCol: " + bestCol + " bestScore: " + bestScore);

			System.err.print("\n" + "punteggi colonne:    (");
			if(halfBoardFull) System.err.print("NON ");
			System.err.print("considero i valori colonna)\n");
			for (int i = 0; i < N; i++) {
				System.err.print("colonna " + i + ": " + String.format("%9f", columnScores[i]) + "\tvalore colonna: " + columns_value[i] + "\n");
			}
		}

		//System.err.print("--- faccio la mossa, bestCol = " + bestCol + "\n");   //DEBUGG DECISIONTREEDEPTH
		return bestCol; //ritorno la colonna migliore
	}




	//Il codice implementa l'algoritmo minimax con potatura alpha-beta, con una profondita' massima di 4 (scelta arbitraria). La funzione minimax ritorna 1 se il giocatore che sta massimizzando ha vinto, -1 altrimenti; ritorna -1 se il giocatore che sta massimizzando ha perso, 1 altrimenti; 0 in caso di pareggio. La funzione minimax e' ricorsiva, e viene eseguita una volta per ogni colonna disponibile. La funzione minimax riceve come parametri: l'oggetto CXBoard, la profondita' di ricerca, la prima mossa da eseguire, i valori di alpha e beta e una variabile booleana che indica quale giocatore sta massimizzando. La funzione ritorna l'intero corrispondente al punteggio ottenuto dalla mossa.
	public float minimax(CXBoard board, int depth, int firstMove, float alpha, float beta, boolean maximizingPlayer, LinkedList<Combo> originalRebarbaroCombos, 
						LinkedList<Combo> originalAdvCombos, LinkedList<CXCell> originalRebWinningFreeCells, LinkedList<CXCell> originalAdvWinningFreeCells, boolean halfBoardFull) {
		//tempo
		long startTime = System.currentTimeMillis();

		Integer[] L = board.getAvailableColumns(); // lista delle colonne disponibili
		CXGameState state = board.markColumn(firstMove); // marco la prima mossa

		//copio le liste di combo per evitare che mi modifichi le originale che stanno nei campi di rebarbaro
		LinkedList<Combo> rebarbaroCombos = new LinkedList<Combo>(originalRebarbaroCombos);
		LinkedList<Combo> advCombos = new LinkedList<Combo>(originalAdvCombos);
		LinkedList<CXCell> rebWinningFreeCells = new LinkedList<CXCell>(originalRebWinningFreeCells);
		LinkedList<CXCell> advWinningFreeCells = new LinkedList<CXCell>(originalAdvWinningFreeCells);

		if (debugMode) {
			System.err.print("\n");
			for (int i = DECISIONTREEDEPTH; i > depth; i--) {
				System.err.print("\t");
			}
			System.err.print("depth: " + (DECISIONTREEDEPTH - depth) + " "); // debug
			System.err.print("col: " + firstMove + "\t\t"); // debug
		}

		int numOfMarkedCells = board.numOfMarkedCells();   //lo uso per l'evaluate della vittoria/sconfitta come modificatore del punteggio per evitare che la somma delle combo con l'avanzare della partita superino il valore della vittoria

		if (state == myWin) { // se ho vinto
			if (debugMode) {
				System.err.print("|won | evaluate: " + (maximizingPlayer ? -(X) * (depth + 1) : (X) * (depth + 1)) + " ");
			}
			board.unmarkColumn(); // tolgo la mossa
			return maximizingPlayer ? -(X) * (depth + 1) : (X) * (depth + 1);
			// ritorno 1 se sono il giocatore che sta massimizzando, -1 altrimenti

		} else if (state == yourWin) { // se ha vinto l'avversario
			if (debugMode) {
				System.err.print("|lost| evaluate: " + (maximizingPlayer ? -(X) * (depth + 1) : (X) * (depth + 1)) + " ");
			}
			board.unmarkColumn(); // tolgo la mossa
			return maximizingPlayer ? -(X) * (depth + 1) : (X) * (depth + 1);
			// ritorno -1 se sono il giocatore che sta massimizzando, 1 altrimenti
		}

		//aggiorno le liste di combo. mi serve per quando arrivo alla foglia per fare l'evaluate
		//maximizingPlayer significa sostanzialmente che sta giocando l'avversario
		if(maximizingPlayer) {
			refreshCombos(advCombos, board, board.getLastMove(), advCellState, true, halfBoardFull, advWinningFreeCells);
			refreshCombos(rebarbaroCombos, board, board.getLastMove(), myCellState, false, halfBoardFull, rebWinningFreeCells);
		}

		//minimizingPlayer sostanzialmente sta giocando rebarbaro
		else {		
			refreshCombos(rebarbaroCombos, board, board.getLastMove(), myCellState, true, halfBoardFull, rebWinningFreeCells);
			refreshCombos(advCombos, board, board.getLastMove(), advCellState, false, halfBoardFull, advWinningFreeCells);

		}

		if(debugMode) {
			System.err.print("depth: " + (DECISIONTREEDEPTH - depth) + " "); // debug
			System.err.print("col: " + firstMove + "\t\t"); // debug
		}

		if (depth == 0 || state == CXGameState.DRAW) { // se sono arrivato alla profondita' massima o se ho pareggiato
			
			float score_adv_combos = evaluationFunctionCombos(advCombos);
			float score_me_combos  = evaluationFunctionCombos(rebarbaroCombos);
			
			float score = maximizingPlayer ? - (score_adv_combos - score_me_combos) : score_me_combos - score_adv_combos;
			score = score / numOfMarkedCells;

			if (debugMode) {
				System.err.print("evaluate: " + score + " (my combos score: " + score_me_combos + ", adv combos score: " + score_adv_combos + " ) ");
			}
			board.unmarkColumn(); // tolgo la mossa
			return score;
		}

		L = board.getAvailableColumns(); // aggiorno la lista delle colonne disponibili

		if (maximizingPlayer) { // se e' il turno dell'avversario
			float maxScore = Integer.MIN_VALUE;
			for (int col : L) {

				if (System.currentTimeMillis() - startTime > timeForColumn) { //controllo il tempo
					DECISIONTREEDEPTH--;
                    break;
                }

				float score = minimax(board, depth - 1, col, alpha, beta, false, rebarbaroCombos, advCombos, rebWinningFreeCells, advWinningFreeCells, halfBoardFull);

				if (debugMode) {
					System.err.print("(max's child) evaluate: " + score + " ");
				}

				maxScore = Math.max(maxScore, score);
				alpha = Math.max(alpha, score);
				if (beta <= alpha) {
					// Pruning su Beta
					break;
				}

			}
			board.unmarkColumn();
			return maxScore;

		} else { // se e' il mio turno
			float minScore = Integer.MAX_VALUE;
			for (int col : L) {

				if (System.currentTimeMillis() - startTime > timeForColumn) { //controllo il tempo
					DECISIONTREEDEPTH--;
                    break;
                }

				float score = minimax(board, depth - 1, col, alpha, beta, true, rebarbaroCombos, advCombos, rebWinningFreeCells, advWinningFreeCells, halfBoardFull);

				if (debugMode) {
					System.err.print("(min's child) evaluate: " + score + " ");
				}

				minScore = Math.min(minScore, score);
				beta = Math.min(beta, score);
				if (beta <= alpha) {
					// Pruning su Alpha
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


	/**
	* Verifico se sono gia' state valutate le colonne
	* ed eventualmente le ordino in base al punteggio
	* @param L lista delle colonne
	* @param B stato attuale della board
	* @return score piu' alto tra i figli
	* implicitamente ordina la lista L in quanto passata per parametro
	*/
	/* 
	public float organizeColumns(List<Integer> L, CXBoard board, boolean maximizingPlayer){
		// Copy the contents of L into a new ArrayList
		ArrayList<Integer> copyOfL = new ArrayList<>(Arrays.asList(L.toArray(new Integer[L.size()])));

		List<Mongolfiera> lastMoveChildren = new ArrayList<Mongolfiera>();
		float score = 0;
		try{
			score = transpositionTable.getChildrenScore(board, lastMoveChildren, true, board.numOfMarkedCells());
			//System.err.print("Size: " + lastMoveChildren.size() + " ");
			if(lastMoveChildren.size() > 0){	
				for(Mongolfiera child : lastMoveChildren){
    				//System.err.println("Sto riordinando la lista");
					if(child.column >= 0 && copyOfL.contains(child.column) && child.score != 0){
						//System.err.print(child.column + " (" + child.score + ")");
						copyOfL.remove(Integer.valueOf(child.column));
						if(child.score > 0)
							copyOfL.add(0, child.column);
						else 
							copyOfL.add(child.column);
					}
				}
			}
		} catch (Exception e){
			System.err.println("Exception in organizeColumns: " + e.getMessage());
		}
		// Copy the contents of the ArrayList back into the array
		for(int i = 0; i < L.size(); i++){
			L.set(i, copyOfL.get(i));
		}
		//System.err.println(L);
		return score;
	}
*/
	
	//----------funzioni combo---------

	public Combo createCombo(CXBoard board, CXCell cell, Direction direction, LinkedList<CXCell> winningFreeCells, boolean halfBoardFull) {
		//PRECONDIZIONE:  cell DEVE contenere una pedina e il suo state deve essere != FREE  
		if(cell.state == CXCellState.FREE) {    //ERRORE: non deve mai stampare questo
			System.err.print("ATTENZIONE: hai chiamato createCombo su una casella vuota. rivedere il codice e correggere\nho ritornato null.\n"); 
			return null;
		}
		CXCellState newComboState = cell.state;
		Combo newCombo = new Combo(newComboState, direction);
		newCombo.add(cell);
		int N_mie = 1;   //aggiungo direttamente cell
        int N_vuote = 0;
        int N_interruzioni = 0;
		int N_free_ends = 0;
		int somma_altezze_pedine_mie = cell.i;

		CXCellState advCellState = newComboState == CXCellState.P1 ? CXCellState.P2 : CXCellState.P1;

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

		while(insideBorders(xi, xj) && board.cellState(xi, xj) != advCellState) {

			if (x.state == CXCellState.FREE && old_xCellState == newComboState) {
				N_interruzioni++;
			}

			old_xCellState = x.state;
			x = new CXCell(xi, xj, board.cellState(xi, xj));

			newCombo.add(x);
			
			if (x.state == newComboState) {
				N_mie++;
				somma_altezze_pedine_mie += M - x.i;
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
		if(insideBorders(xi, xj)) {
			x = new CXCell(xi, xj, board.cellState(xi, xj));
			old_xCellState = board.cellState(cell.i, cell.j);
		}
		
		while(insideBorders(xi, xj) && board.cellState(xi, xj) != advCellState) {
			
			if (x.state == CXCellState.FREE && old_xCellState == newComboState) {
				N_interruzioni++;
			}
			
			old_xCellState = x.state;
			x = new CXCell(xi, xj, board.cellState(xi, xj));

			newCombo.addFirst(x);    //aggiungo all'inizio cosi' ho alle due estremita' della lista le due estremita' della sequenza di caselle

			if (x.state == newComboState) {
				N_mie++;
				somma_altezze_pedine_mie += M - x.i;
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
		newCombo.somma_altezze_pedine_mie = somma_altezze_pedine_mie;   
		//controllo se la combo e' utilizzabile per vincere
		if(newCombo.getLength() < X)
			newCombo.deadCombo = true;
		
		//vedo se ha caselle vuote vincenti, che uso poi per calcolare i valori di combo vicine
		//se non ce ne sono la funzione esce subito, quindi ha costo irrisorio
		LinkedList<CXCell> winningCellsFound = newCombo.findFreeWinningCells(X, N_mie + N_interruzioni - N_free_ends);
		

		if(winningCellsFound.size() > 0) {
			winningFreeCells.addAll(winningCellsFound);
		}


		//System.err.print("\n---------\n--------\n");    //DEBUGG COMBO
		//System.err.print("adesso calcolo il valore della combo creata a partire da i: " + cell.i + " j: " + cell.j + " state: " + cell.state + "\n"); //DEBUGG COMBO
		newCombo.setValue(newCombo.calculateComboValue(0, X, M, halfBoardFull));
		//System.err.print(" - DEBUG trovate " + (winningCellsFound.size()) + " caselle vuote vincenti\n"); //DEBUGG COMBO
		//System.err.print(" - ---- -- -\n\n"); //DEBUGG COMBO
		
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
		//puo' prendere anche celle vuote come input
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

	public LinkedList<Combo> refreshCombos(LinkedList<Combo> comboList, CXBoard board, CXCell cell, CXCellState myState, boolean lastMoveWasMine, boolean halfBoardFull, 
											LinkedList<CXCell> freeWinningCells) {

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
					comboList.add(createCombo(board, cell, direction, freeWinningCells, halfBoardFull));
				}
				else {
					//tolgo e rimetto la combo, ossia la ricalcolo
					comboList.remove(findCombo(comboList, cell, direction));
					comboList.add(createCombo(board, cell, direction, freeWinningCells, halfBoardFull));
				}
			}

		}

		//se l'ultima messa e' dell'avversario
		else {

			for(Direction direction : directions) {
				int[] dir_pos = direction.positiveDirection();
				int[] dir_neg = direction.negativeDirection();

				CXCell cell_p;    //le inizializzo nelle prossime righe in base a se sono dentro o meno i bordi
				CXCell cell_n;

				//se la pedina e' fuori dai bordi, creo una pedina con posizione fittizia e state "avversario"
				//questo posso farlo perche' ai fini dei casi qua sotto avere una pedina avversaria e' equivalente ad avere una pedina fuori dai bordi
				//inoltre la posizione puo' essere "sbagliata" perche' tanto i casi qua sotto quando vedono che la cella e' avversaria non guardano mai la posizione
				if(insideBorders(cell.i + dir_pos[0], cell.j + dir_pos[1])) {
					cell_p = new CXCell(cell.i + dir_pos[0], cell.j + dir_pos[1], board.cellState(cell.i + dir_pos[0], cell.j + dir_pos[1]));
				}
				else {
					cell_p = new CXCell(cell.i, cell.j, advCellState);
				}

				if(insideBorders(cell.i + dir_neg[0], cell.j + dir_neg[1])) {
					cell_n = new CXCell(cell.i + dir_neg[0], cell.j + dir_neg[1], board.cellState(cell.i + dir_neg[0], cell.j + dir_neg[1]));
				}
				else {
					cell_n = new CXCell(cell.i, cell.j, advCellState);
				}


				if(cell_p.state == cell_n.state && cell_n.state == advCellState) {
					//per quel che ci riguarda non succede nulla
					//le nostre combo non cambiano
					//ci pensera' poi l'avversario a cambiare le sue in questa posizione
				}
				else if(cell_p.state == cell_n.state && cell_n.state == myState) {
					//cell_p e cell_n facevano sicuramente parte della stessa combo
					//quindi la spezzo
					comboList.remove(findCombo(comboList, cell_p, direction));
					createCombo(board, cell_p, direction, freeWinningCells, halfBoardFull);
					createCombo(board, cell_n, direction, freeWinningCells, halfBoardFull);
				}
				else if(cell_p.state != advCellState && cell_n.state == advCellState) {
					//controllo che lungo la direzione positiva ci sia una mia pedina
					while(cell_p.state != myState && 
					insideBorders(cell_p.i + dir_pos[0], cell_p.j + dir_pos[1])) {
						cell_p = new CXCell(cell_p.i + dir_pos[0], cell_p.j + dir_pos[1], board.cellState(cell_p.i + dir_pos[0], cell_p.j + dir_pos[1]));
					}
					//se alla fine una mia pedina c'era
					if(cell_p.state == myState) {
						//tolgo e ricalcolo la combo
						comboList.remove(findCombo(comboList, cell_p, direction));   
						comboList.add(createCombo(board, cell_p, direction, freeWinningCells, halfBoardFull));
					}
				}
				else if(cell_p.state == advCellState && cell_n.state != advCellState) {
					//controllo che lungo la direzione positiva ci sia una mia pedina
					while(cell_n.state != myState && 
					insideBorders(cell_n.i + dir_neg[0], cell_n.j + dir_neg[1])) {
						cell_n = new CXCell(cell_n.i + dir_neg[0], cell_n.j + dir_neg[1], board.cellState(cell_n.i + dir_neg[0], cell_n.j + dir_neg[1]));
					}
					//se alla fine una mia pedina c'era
					if(cell_n.state == myState) {
						//tolgo e ricalcolo la combo
						comboList.remove(findCombo(comboList, cell_n, direction));   
						comboList.add(createCombo(board, cell_n, direction, freeWinningCells, halfBoardFull));
					}
				}

				//nel caso in cui la mossa e' dell'avversario, e non tocca nessuna nostra pedina (ad esempio ha intorno solo caselle vuote)
				//non fa nulla
			}
		}


		return comboList;
	}

	public boolean insideBorders(int row, int col) {
		return (row >= 0 && row < M && col >= 0 && col < N);
	}

	public int absValue(int x) {
		if(x >= 0)
			return x;
		else
			return -x;
	}

	public int evaluationFunctionCombos(LinkedList<Combo> comboList) {
		int somma = 0;
		for(Combo combo : comboList) {
			somma += combo.getValue();
		}
		return somma;
	}

	//     combo value con le caselle vuote vincenti adiacenti verticalmente
	//calcola quante coppie di caselle vuote "vincenti" disposte una sopra l'altra ci sono
	public int calculateNumberOfWinningAdiacentCells(LinkedList<CXCell> winningFreeCells) {
		int N_WAC = 0;

		//il doppio ciclo non e' notoriamente molto efficiente, ma dato che le liste di caselle vuote vincenti sono piuttosto corte 
		//(si parla di max 3-4 combinazioni "quasi vincenti" a partita media)
		//quindi sarebbe stato meno costoso e piu' contorto cercare di tenere ordinate le caselle man mano che venivano aggiunte, piuttosto che fare una ventina di controlli

		for(CXCell cell_x : winningFreeCells) {
			for(CXCell cell_y : winningFreeCells) {
				//stessa colonna e riga di differenza 1  -->  adiacenti verticalmente
				if(cell_x.j == cell_y.j && absValue(cell_x.i - cell_y.i) == 1) 
					N_WAC++;
				
			}
		}

		return N_WAC;
	}

	//  ---   combo value con gli heap
	//modifica l'array in input riempiendolo con i valori delle combo di comboList
	private void makeComboValuesArray(LinkedList<Combo> comboList, int[] array) {
		int i = 1;
		array[0] = -1;  //per la costruzione del minheap array e' piu' comodo che l'elemento 0 non venga usato
		for(Combo combo : comboList) {
			array[i] = combo.getValue();
			i++;
		}
	}

	//S array heap
	//c indice dell'ultimo elemento di S (fine dell'heap)
	//i nodo radice dell'heap
	private void fixHeap(int S[], int c, int i) {
		int max = 2 * i; // figlio sinistro
		if (2 * i > c) return;                //se questo nodo non ha figli 
		if (2 * i + 1 <= c && S[2 * i] < S[2 * i + 1])           //sceglie il maggiore tra figlio destro e sinistro
			max = 2 * i + 1; // figlio destro

		if (S[i] < S[max]) {                                    //scambia la radice con il figlio maggiore, se necessario 
			int temp = S[max];                                  
			S[max] = S[i];
			S[i] = temp;
			fixHeap(S, c, max);                            //e richiama la funzione sul sottoalbero del figlio maggiore (adesso in posizione max c'e' la vecchia radice)
		}
	}

	//crea un max-heap di S
	//n lunghezza array
	//i = 1 se chiamata da fuori
	private void heapify(int S[], int n, int i) {
		if (i > n) return;
		heapify(S, n, 2 * i); // crea heap radicato in S[2*i]
		heapify(S, n, 2 * i + 1); // crea heap radicato in S[2*i+1]
		fixHeap(S, n, i);
	}

	//prende le 3 combinazioni con valore piu' alto da comboList e le somma
	public int evaluationFunctionCombos2(LinkedList<Combo> comboList, LinkedList<CXCell> winningFreeCells) {
		int somma = 0;
		int n_combos_to_evaluate = 3;

		int[] comboValues = new int[comboList.size() + 1];   //+1 perche' l'elemento 0 non verra' usato
		makeComboValuesArray(comboList, comboValues);
		heapify(comboValues, comboList.size(), 1);

		//so che i primi 3 elementi dell'heap sono la radice e i suoi due figli, che sono i 3 elementi maggiori
		for(int i = 1; i <= n_combos_to_evaluate && i <= comboList.size(); i++) {    //so che l'elemento 0 non e' da utilizzare
			somma += comboValues[i];
		}
		/* 
		//aggiungo la quarta combo da considerare
		if(comboList.size() >= 4)
			if(comboList.size() >= 5)
				if(comboValues[4] > comboValues[5])
					somma += comboValues[4];
				else
					somma += comboValues[5];
			else
				somma += comboValues[4];
		*/

		int winning_adiacent_free_cells_multiplier = calculateNumberOfWinningAdiacentCells(winningFreeCells) * 3 + 1;
		// + 1 cosi' quando lo moltiplico per somma non lo azzera
		// * 3 perche' do valore triplo alle board con questa casistica, visto che sono un'ottima probabilita' di vittoria

		somma *= winning_adiacent_free_cells_multiplier;

		return somma;
	}
}

/*
	public LinkedList<Combo> calculateAllCombos(LinkedList<Combo> comboList, CXCellState myState, CXBoard board, LinkedList<CXCell> freeWinningCells, boolean halfBoardFull) {
		CXCell[] markedCells = board.getMarkedCells();

		Direction[] directions = {  Direction.Vertical,
									Direction.Horizontal,
									Direction.Diagonal, 
									Direction.AntiDiagonal};

		for(CXCell cell : markedCells) {
			for(Direction direction : directions) {
				if(cell.state == myState) {
					if(findCombo(comboList, cell, direction) == null) {
						comboList.add(createCombo(board, cell, direction, freeWinningCells, halfBoardFull));
						
					}
				}
			}
		}
		return comboList;
	} 
	
*/