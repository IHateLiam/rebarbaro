package connectx.ReginaDegliArcieri;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.Random;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.concurrent.TimeoutException;



public class ReginaDegliArcieri implements CXPlayer {
    private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
	private float  TIMEOUT;
	private long START;
	private double[] columns_value;
	private int M, N, X;

	CXCellState first;
	CXCellState myCellState;
	CXCellState advCellState;

	private boolean debugMode;

	//variabili hash table
	//private IncantesimoClonazione transpositionTable;
	private int markedCells;
	private CXCell lastMove; 
	
	//variabili combo
	private LinkedList<Combo> myComboList;
	private LinkedList<Combo> advComboList;

	//numero di celle tale da determinare che siamo a meta' partita
	//serve piu' che altro a non doverla calcolare tutte le volte
	private int totalBoardCells;    
	private int halfGameCells;      
	
	//variabili per la variazione dell'altezza dell'albero di esplorazione delle mosse
	private int timeForColumn;
	private int DECISIONTREEDEPTH;


    /*Default empty constructor*/
    public ReginaDegliArcieri() {

    }


	public String playerName() {
		return "ReginaDegliArcieri";
	}

    public void initPlayer(int M, int N, int X, boolean first, int timeout_in_secs) {
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
		yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
		TIMEOUT = 1.0f;
		columns_value = calculate_columns_value(N);
		this.M = M;
		this.N = N;
		this.X = X;
		this.first = first ? CXCellState.P1 : CXCellState.P2;
		this.myCellState = this.first;
		this.advCellState = first ? CXCellState.P2 : CXCellState.P1;

		this.timeForColumn = (int) ((TIMEOUT * 1000)/N);

		this.myComboList  = new LinkedList<Combo>();
		this.advComboList = new LinkedList<Combo>();

		//this.transpositionTable = new IncantesimoClonazione(M, N);

		this.DECISIONTREEDEPTH = 2;

		this.totalBoardCells = M * N;
		this.halfGameCells = this.totalBoardCells / 2 - X;     

		// (---)   (---)   (---)   (---)   (---)   (---)   
		debugMode = false;
		// (---)   (---)   (---)   (---)   (---)   (---)   

    }

	/**
	 * Calcola il valore di ogni colonna tramite l'algoritmo del MinMax
	 * @param board
	 * @return colonna migliore
	 */
	public int selectColumn(CXBoard board) {
		float start = System.currentTimeMillis(); //per il timeout
		float bestScore = Integer.MIN_VALUE; //per il minimax
		int bestCol = -1; //per il minimax
		int depth = DECISIONTREEDEPTH;
		List<Integer> availableColumns = new ArrayList<>(Arrays.asList(board.getAvailableColumns()));
		float score;

		boolean halfBoardFull = board.numOfMarkedCells() > halfGameCells;

		markedCells = board.numOfMarkedCells();
		lastMove = board.getLastMove();
		
		float[] columnScores = new float[N];
		//score = organizeColumns(availableColumns, board, true);

		int randomEventualChoice = availableColumns.get(0);   //calcola gia' la scelta casuale caso mai andasse in timeout

		//aggiorno la comboList avversaria
		if(board.numOfMarkedCells() > 0)       //sostanzialmente entra in questo if se gioca come secondo
			refreshCombos(advComboList, board, board.getLastMove(), advCellState, true);
		
		//non uso le originali perche' il minimax fa delle "ipotesi". non voglio che le ipotesi vengano salvate: la lista diventerebbe enorme e non controllabile
		LinkedList<Combo> myComboListCopy = new LinkedList<Combo>(myComboList);
		LinkedList<Combo> advCombosCopy = new LinkedList<Combo>(advComboList);

		for (int col : availableColumns) {
			try {
				
				if (debugMode) {
					System.err.print("\n marked column: " + board.numOfMarkedCells()); //debug
					System.err.println("\n\n"); //debug
				}

				depth = DECISIONTREEDEPTH;
				long columnMinmaxTime = System.currentTimeMillis();

				score = minimax(board, depth, col, Integer.MIN_VALUE, Integer.MAX_VALUE, false, myComboListCopy, advCombosCopy, markedCells); //minimax

				if(!halfBoardFull) {    //voglio che le colonne centrali le scelga solo all'inizio della partita
					score += 0.01; 	//se e' tutto a 0, la moltiplicazione dei valori delle colonne viene annullata
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

				if(System.currentTimeMillis() - columnMinmaxTime < (timeForColumn/3))
					DECISIONTREEDEPTH++;
				else if(DECISIONTREEDEPTH > 2)
					DECISIONTREEDEPTH--;

				if (System.currentTimeMillis() - start > (TIMEOUT+8) * 9700) { //se ho superato il timeout
					throw new TimeoutException(); //lancio un'eccezione
				}
			} catch (TimeoutException e) {
				//System.err.println("Timeout!!! minimax ritorna -1 in selectColumn"); //debug
				break;
			}

		}

		if (debugMode) {
			System.err.print("\n bestCol: " + bestCol + " bestScore: " + bestScore);
		}

		//se bestCol e' ancora uguale a 1, significa che sta andando in timeout
		if (bestCol == -1) { 
			bestCol = randomEventualChoice;
		}

		//aggiorno le mie combo
		board.markColumn(bestCol);
		refreshCombos(myComboList, board, board.getLastMove(), myCellState, true);
		board.unmarkColumn();


		if (debugMode) {
			System.err.print("\n bestCol: " + bestCol + " bestScore: " + bestScore);

			System.err.print("\n" + "punteggi colonne:    (");
			if(halfBoardFull) System.err.print("NON ");
			System.err.print("considero i valori colonna)\n");
			for (int i = 0; i < N; i++) {
				System.err.print("colonna " + i + ": " + String.format("%9f", columnScores[i]) + "\tvalore colonna: " + columns_value[i] + "\n");
			}

			System.err.print("myCombosList.size(): " + myComboList.size() + "\n");
			System.err.print("advComboList.size(): " + advComboList.size() + "\n");

		}


		return bestCol; //ritorno la colonna migliore
	}

	//Il codice implementa l'algoritmo minimax con potatura alpha-beta, con una profondita' massima di 4 (scelta arbitraria). La funzione minimax ritorna 1 se il giocatore che sta massimizzando ha vinto, -1 altrimenti; ritorna -1 se il giocatore che sta massimizzando ha perso, 1 altrimenti; 0 in caso di pareggio. La funzione minimax e' ricorsiva, e viene eseguita una volta per ogni colonna disponibile. La funzione minimax riceve come parametri: l'oggetto CXBoard, la profondita' di ricerca, la prima mossa da eseguire, i valori di alpha e beta e una variabile booleana che indica quale giocatore sta massimizzando. La funzione ritorna l'intero corrispondente al punteggio ottenuto dalla mossa.
	
	/**
	 * Funzione che implementa l'algoritmo del Minimax con potatura Alpha-Beta
	 * cerca di massimizzare il punteggio tramite la funzione di valutazione e una ricerca con una profondita' variabile
	 * scelta in base al tempo disponibile dentro selectColumn
	 * @param board
	 * @param depth
	 * @param firstMove
	 * @param alpha
	 * @param beta
	 * @param maximizingPlayer
	 * @param originalRebarbaroCombos
	 * @param originalAdvCombos
	 * @param originalRebWinningFreeCells
	 * @param originalAdvWinningFreeCells
	 * @param halfBoardFull
	 * @param numOfMarkedCellsStart il numero di celle presenti sulla board al momento della chiamata di selectColumn
	 * @return score per la colonna firstMove
	 */
	public float minimax(CXBoard board, int depth, int firstMove, float alpha, float beta, boolean maximizingPlayer, LinkedList<Combo> originalRebarbaroCombos, 
						LinkedList<Combo> originalAdvCombos, int numOfMarkedCellsStart) {
		//tempo
		long startTime = System.currentTimeMillis();
		
		float score;
		List<Integer> availableColumns = new ArrayList<>(Arrays.asList(board.getAvailableColumns())); //lista delle colonne disponibili
		CXGameState state = board.markColumn(firstMove); // marco la prima mossa

		//copio le liste di combo per evitare che mi modifichi le originale che stanno nei campi di rebarbaro
		LinkedList<Combo> rebarbaroCombos      = new LinkedList<Combo>(originalRebarbaroCombos);
		LinkedList<Combo> advCombos            = new LinkedList<Combo>(originalAdvCombos);

	
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
				System.err.print("|won | evaluate: " + (maximizingPlayer ? -(X) * (depth + 1) * 10 : (X) * (depth + 1) * 10) + " ");
			}
			score = maximizingPlayer ? -(X) * (depth + 1) * 10 : (X) * (depth + 1) * 10;  
			board.unmarkColumn(); // tolgo la mossa
			return score;
			// ritorno 1 se sono il giocatore che sta massimizzando, -1 altrimenti




		} else if (state == yourWin) { // se ha vinto l'avversario
			if (debugMode) {
				System.err.print("|lost| evaluate: " + (maximizingPlayer ? -(X) * (depth + 1) * 10 : (X) * (depth + 1) * 10) + " ");
			}
			score = maximizingPlayer ? -(X) * (depth + 1) * 10 : (X) * (depth + 1) * 10;
			board.unmarkColumn(); // tolgo la mossa
			return score;
			// ritorno -1 se sono il giocatore che sta massimizzando, 1 altrimenti
		}


		//aggiorno le liste di combo. mi serve per quando arrivo alla foglia per fare l'evaluate
		
		//maximizingPlayer significa sostanzialmente che sta giocando l'avversario
		if(maximizingPlayer) {
			refreshCombos(advCombos, board, board.getLastMove(), advCellState, true);
			refreshCombos(rebarbaroCombos, board, board.getLastMove(), myCellState, false);
		}

		//minimizingPlayer sostanzialmente sta giocando rebarbaro
		else {		
			refreshCombos(rebarbaroCombos, board, board.getLastMove(), myCellState, true);
			refreshCombos(advCombos, board, board.getLastMove(), advCellState, false);

		}


		if(debugMode) {
			System.err.print("depth: " + (DECISIONTREEDEPTH - depth) + " "); // debug
			System.err.print("col: " + firstMove + "\t\t"); // debug
		}

		 

		//valuto foglia
		if (depth == 0 || state == CXGameState.DRAW) { // se sono arrivato alla profondita' massima o se ho pareggiato
			
			float score_adv_combos = evaluationFunctionCombos(advCombos);
			float score_me_combos  = evaluationFunctionCombos(rebarbaroCombos);
			
			score = maximizingPlayer ? - (score_adv_combos - score_me_combos) : score_me_combos - score_adv_combos;
			score = score / numOfMarkedCells;
			
			if (debugMode) {
				System.err.print("evaluate: " + score + " (my combos score: " + score_me_combos + ", adv combos score: " + score_adv_combos + " ) ");
			}
			board.unmarkColumn(); // tolgo la mossa
			return score;
			
		}

		availableColumns = Arrays.asList(board.getAvailableColumns()); //aggiorno la lista delle colonne disponibil
		//score = organizeColumns(availableColumns, board, maximizingPlayer);

		if (maximizingPlayer) {
			// Maximize player 1's score
			float maxScore = Integer.MIN_VALUE;
			for (int col : availableColumns) {

				if (System.currentTimeMillis() - startTime > timeForColumn) { // check if time is up
					DECISIONTREEDEPTH--;
                    break;
                }
				score = minimax(board, depth - 1, col, alpha, beta, false, rebarbaroCombos, advCombos, numOfMarkedCellsStart);

				if (debugMode) {
					System.err.print("(max's child) evaluate: " + score + " ");
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
			for (int col : availableColumns) {

				if (System.currentTimeMillis() - startTime > timeForColumn) { // check if time is up
					DECISIONTREEDEPTH--;
                    break;
                }

				score = minimax(board, depth - 1, col, alpha, beta, true, rebarbaroCombos, advCombos, numOfMarkedCellsStart);

				if (debugMode) {
					System.err.print("(min's child) evaluate: " + score + " ");
				}

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

	/**
	 * Funzione di valutazione delle colonne della board
	 * piu' e' centrale la colonna piu' e' alto il punteggio
	 * @param boardWidth larghezza della board
	 * @return array di double che rappresenta il punteggio di ogni colonna
	 */
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
	public float organizeColumns(List<Integer> L, CXBoard board, boolean maximizingPlayer){
		// Copy the contents of L into a new ArrayList
		ArrayList<Integer> copyOfL = new ArrayList<>(Arrays.asList(L.toArray(new Integer[L.size()])));

		List<NodeData> lastMoveChildren = new ArrayList<NodeData>();
		float score = 0;
		try{
			//System.err.print("Size: " + lastMoveChildren.size() + " ");
			if(lastMoveChildren.size() > 0){	
				for(NodeData child : lastMoveChildren){
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

	
	//----------funzioni combo---------

	//data una casella e una direzione, crea la combo corrispondente
	public Combo createCombo(CXBoard board, CXCell cell, Direction direction) {
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


		newCombo.setValue(newCombo.calculateComboValue(0, X, M));
		

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

	public LinkedList<Combo> refreshCombos(LinkedList<Combo> comboList, CXBoard board, CXCell cell, CXCellState myState, boolean lastMoveWasMine) {

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
					createCombo(board, cell_p, direction);
					createCombo(board, cell_n, direction);
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
						comboList.add(createCombo(board, cell_p, direction));
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
						comboList.add(createCombo(board, cell_n, direction));
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

	public float evaluationFunctionCombos(LinkedList<Combo> comboList) {
		float somma = 0;
		for(Combo combo : comboList) {
			somma += combo.getValue();
		}

		return somma ;
	}


}
	

 
