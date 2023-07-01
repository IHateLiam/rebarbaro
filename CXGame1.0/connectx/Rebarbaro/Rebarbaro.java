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
import java.util.concurrent.Timeo	utException;

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
	private HashMap<String, Float> transpositionTable = new HashMap<String, Float>();

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
		long start = System.currentTimeMillis(); //per il timeout
		float bestScore = Integer.MIN_VALUE; //per il minimax
		int bestCol = -1; //per il minimax
		int depth = DECISIONTREEDEPTH;
		Integer[] availableColumns = B.getAvailableColumns();

		float[] columnScores = new float[N];

		for (int col : availableColumns) {
			try {
				transpositionTable.clear();

				if (debugMode) {
					System.err.print("\n marked column: " + B.numOfMarkedCells()); //debug
					System.err.println("\n\n"); //debug
				}

				float score = minimax(B, depth, col, Integer.MIN_VALUE, Integer.MAX_VALUE, false); //minimax
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
				bestCol = singleMoveBlock(B, availableColumns); //provo a bloccare l'avversario
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

	public float minimax(CXBoard B, int depth, int firstMove, float alpha, float beta, boolean maximizingPlayer) {
		Integer[] L = B.getAvailableColumns(); // lista delle colonne disponibili
		CXGameState state = B.markColumn(firstMove); // marco la prima mossa

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
			B.unmarkColumn(); // tolgo la mossa
			return maximizingPlayer ? -(K * 4) * depth : (K * 4) * depth; // ritorno 1 se sono il giocatore che sta massimizzando, -1 altrimenti
		} else if (state == yourWin) { // se ha vinto l'avversario
			if (debugMode) {
				System.err.print("|lost| evaluate: " + (maximizingPlayer ? -1 * depth : 1 * depth) + " ");
			}
			B.unmarkColumn(); // tolgo la mossa
			return maximizingPlayer ? -(K * 4) * depth : (K * 4) * depth; // ritorno -1 se sono il giocatore che sta massimizzando, 1 altrimenti
		} else if (depth == 0 || state == CXGameState.DRAW) { // se sono arrivato alla profondita' massima o se ho pareggiato
			float score = maximizingPlayer ? -evaluationFunction(B) * depth : evaluationFunction(B) * depth;
			if (debugMode) {
				System.err.print("evaluate: " + score);
			}
			B.unmarkColumn(); // tolgo la mossa
			return score;
		}

		L = B.getAvailableColumns(); // aggiorno la lista delle colonne disponibili

		if (maximizingPlayer) {
			// Maximize player 1's score
			float maxScore = Integer.MIN_VALUE;
			for (int col : L) {

				float score = minimax(B, depth - 1, col, alpha, beta, false);

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
			B.unmarkColumn();
			return maxScore;

		} else {
			// Minimize player 2's score
			float minScore = Integer.MAX_VALUE;
			for (int col : L) {

				float score = minimax(B, depth - 1, col, alpha, beta, true);
				minScore = Math.min(minScore, score);
				beta = Math.min(beta, score);
				if (beta <= alpha) {
					// Alpha cutoff
					break;
				}
			}
			B.unmarkColumn();
			return minScore;
		}
	}

	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
	}

	public String playerName() {
		return "Rebarbaro";
	}


	private float evaluationFunction(CXBoard board) {
		CXCell lastCell = board.getLastMove();
		int row = lastCell.i;
		int col = lastCell.j;

		int verticalPieces = nearPieces(row, col, board, lastCell.state, K, 1);
		int orizzontalPieces = nearPieces(row, col, board, lastCell.state, K, 2);
		int diagonalPieces = nearPieces(row, col, board, lastCell.state, K, 3);
		int antiDiagonalPieces = nearPieces(row, col, board, lastCell.state, K, 4);

		int score = verticalPieces + orizzontalPieces + diagonalPieces + antiDiagonalPieces;

		// Aggiungi un bonus per le colonne centrali
		int centerCol = N / 2;
		if (col == centerCol) {
			score += 2;
		} else if (col == centerCol - 1 || col == centerCol + 1) {
			score += 1;
		}

		// Aggiungi un bonus per le righe inferiori
		int bottomRow = M - 1;
		if (row == bottomRow) {
			score += 2;
		} else if (row == bottomRow - 1) {
			score += 1;
		}

		// Aggiungi un bonus per le sequenze di pezzi già presenti sul tabellone
		int sequenceBonus = getSequenceBonus(board, lastCell.state);
		score += sequenceBonus;

		// Aggiungi un bonus per le opportunità di creare blocchi
		int blockBonus = getBlockBonus(board, lastCell.state);
		score += blockBonus;

		// Aggiungi un bonus per le opportunità di creare sequenze
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

		// Aggiungi un bonus per le colonne con pezzi dell'avversario
		int opponentColumns = getOpponentColumns(board, lastCell.state);
		int opponentColumnBonus = opponentColumns * 2;
		score += opponentColumnBonus;

		// Aggiungi un bonus per le righe con pezzi dello stesso colore
		int sameColorRows = getSameColorRows(board, lastCell.state);
		int sameColorRowBonus = sameColorRows * 2;
		score += sameColorRowBonus;

		// Aggiungi un bonus per le righe con pezzi dell'avversario
		int opponentRows = getOpponentRows(board, lastCell.state);
		int opponentRowBonus = opponentRows * 2;
		score += opponentRowBonus;

		// Aggiungi un bonus per le celle vicine ai bordi del tabellone
		int edgeBonus = getEdgeBonus(board, lastCell);
		score += edgeBonus;

		// Aggiungi un bonus per i pezzi isolati
		int isolatedPiecesBonus = getIsolatedPiecesBonus(board, lastCell);
		score += isolatedPiecesBonus;

		// Aggiungi un bonus per i pezzi in angoli o bordi
		int cornerAndEdgePiecesBonus = getCornerAndEdgePiecesBonus(board, lastCell);
		score += cornerAndEdgePiecesBonus;

		// Aggiungi un bonus per la mobilità
		int mobilityBonus = getMobilityBonus(board, lastCell);
		score += mobilityBonus;

		// Aggiungi un bonus per la presenza di pezzi in posizioni chiave
		int keyPiecesBonus = getKeyPiecesBonus(board, lastCell.state);
		score += keyPiecesBonus;

		// Aggiungi un bonus per la presenza di pezzi che possono catturare o bloccare
		int captureAndBlockBonus = getCaptureAndBlockBonus(board, lastCell.state);
		score += captureAndBlockBonus;

		// Aggiungi un bonus per la presenza di pezzi che possono vincere la partita
		int winningPiecesBonus = getWinningPiecesBonus(board, lastCell.state);
		score += winningPiecesBonus;

		return score;
	}

	/*This function is used to determine how many pieces of the same color are in the direction specified by the parameter direction. The variable direction can be 1, 2, 3, or 4, representing the four diagonal directions. The variable state represents the color of the pieces, and the variable k represents how many pieces in a row will win the game. The variable row and col represent the row and column of the cell where the current piece is located. The function returns the number of pieces of the same color as the current piece in the direction specified by the parameter direction.*/
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

	/*	Questa funzione calcola il bonus per le sequenze di pezzi dello stesso stato (1 o 2) che passano per l'ultima mossa effettuata sul tabellone. Per ogni direzione (orizzontale, verticale, diagonale e antidiagonale), la funzione conta il numero di pezzi dello stesso stato consecutivi a partire dalla cella dell'ultima mossa e in quella direzione. Se il numero di pezzi consecutivi è maggiore o uguale a K (la lunghezza della sequenza richiesta per vincere), allora la funzione aggiunge al bonus il quadrato del numero di pezzi consecutivi. In questo modo, le sequenze più lunghe ricevono un bonus maggiore rispetto alle sequenze più corte. Infine, la funzione restituisce il totale dei bonus per tutte le direzioni. */
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
			if (count >= K) {
				bonus += count * count;
			}
		}
		return bonus;
	}

	/*Questa funzione calcola il bonus per le posizioni che bloccano le sequenze di pezzi dell'avversario che passano per l'ultima mossa effettuata sul tabellone. Per ogni direzione (orizzontale, verticale, diagonale e antidiagonale), la funzione conta il numero di pezzi dell'avversario consecutivi a partire dalla cella dell'ultima mossa e in quella direzione. Se la cella successiva alla sequenza di pezzi dell'avversario è vuota, allora la funzione aggiunge al bonus il quadrato del numero di pezzi dell'avversario consecutivi. In questo modo, le posizioni che bloccano sequenze più lunghe dell'avversario ricevono un bonus maggiore rispetto alle posizioni che bloccano sequenze più corte. Infine, la funzione restituisce il totale dei bonus per tutte le direzioni. */
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

	/*Questa funzione calcola il bonus per le posizioni che permettono di creare sequenze di pezzi dello stesso stato (1 o 2) che passano per l'ultima mossa effettuata sul tabellone. Per ogni direzione (orizzontale, verticale, diagonale e antidiagonale), la funzione conta il numero di pezzi dello stesso stato consecutivi a partire dalla cella dell'ultima mossa e in quella direzione. Se la cella successiva alla sequenza di pezzi dello stesso stato è vuota, allora la funzione cerca di contare il numero di pezzi dello stesso stato consecutivi a partire dalla cella successiva e in quella direzione. Se il numero di pezzi consecutivi più il numero di pezzi consecutivi nella cella successiva è maggiore o uguale a K (la lunghezza della sequenza richiesta per vincere), allora la funzione aggiunge al bonus il quadrato del numero di pezzi dello stesso stato consecutivi nella cella successiva. In questo modo, le posizioni che permettono di creare sequenze più lunghe ricevono un bonus maggiore rispetto alle posizioni che permettono di creare sequenze più corte. Infine, la funzione restituisce il totale dei bonus per tutte le direzioni. */
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
				if (count + newCount >= K) {
					bonus += newCount * newCount;
				}
			}
		}
		return bonus;
	}

	/*Questa funzione calcola il bonus per le colonne che hanno pochi pezzi. La funzione conta il numero di colonne vuote sul tabellone e calcola il bonus come 2^(n-1), dove n è il numero di colonne vuote. In questo modo, le colonne con meno pezzi ricevono un bonus maggiore rispetto alle colonne con più pezzi. Infine, la funzione restituisce il bonus calcolato. */
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

	/*Questa funzione calcola il bonus per le colonne che contengono pezzi dello stesso colore dello stato passato come parametro. La funzione conta il numero di pezzi dello stesso colore in ogni colonna e calcola il bonus come 2^(n-1), dove n è il numero di pezzi dello stesso colore nella colonna. In questo modo, le colonne con più pezzi dello stesso colore ricevono un bonus maggiore rispetto alle colonne con meno pezzi dello stesso colore. Infine, la funzione restituisce il bonus calcolato. */
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

	/*Questa funzione calcola il bonus per le colonne che contengono pezzi dell'avversario (cioè dello stato opposto a quello passato come parametro). La funzione conta il numero di pezzi dell'avversario in ogni colonna e calcola il bonus come 2^(n-1), dove n è il numero di pezzi dell'avversario nella colonna. In questo modo, le colonne con più pezzi dell'avversario ricevono un bonus maggiore rispetto alle colonne con meno pezzi dell'avversario. Infine, la funzione restituisce il bonus calcolato. */
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

	/*Questa funzione calcola il bonus per le righe che contengono pezzi dello stesso colore dello stato passato come parametro. La funzione conta il numero di pezzi dello stesso colore in ogni riga e calcola il bonus come 2^(n-1), dove n è il numero di pezzi dello stesso colore nella riga. In questo modo, le righe con più pezzi dello stesso colore ricevono un bonus maggiore rispetto alle righe con meno pezzi dello stesso colore. Infine, la funzione restituisce il bonus calcolato. */
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

	/*Questa funzione calcola il bonus per le righe che contengono pezzi dell'avversario (cioè dello stato opposto a quello passato come parametro). La funzione conta il numero di pezzi dell'avversario in ogni riga e calcola il bonus come 2^(n-1), dove n è il numero di pezzi dell'avversario nella riga. In questo modo, le righe con più pezzi dell'avversario ricevono un bonus maggiore rispetto alle righe con meno pezzi dell'avversario. Infine, la funzione restituisce il bonus calcolato */
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

	/*Questa funzione calcola il bonus per le celle vicine ai bordi del tabellone. La funzione controlla se la cella dell'ultima mossa si trova sul bordo del tabellone o vicino ad esso e assegna un bonus di 2 punti se la cella si trova sul bordo e un bonus di 1 punto se la cella si trova vicino all'angolo del tabellone. In questo modo, le posizioni vicine ai bordi del tabellone ricevono un bonus maggiore rispetto alle posizioni al centro del tabellone. Infine, la funzione restituisce il bonus calcolato. */
	private int getEdgeBonus(CXBoard board, CXCell lastMove) {
		int bonus = 0;
		int row = lastMove.i;
		int col = lastMove.j;
		if (row == 0 || row == board.M - 1) {
			bonus += 2;
		}
		if (col == 0 || col == board.N - 1) {
			bonus += 2;
		}
		if ((row == 1 || row == board.M - 2) && (col == 1 || col == board.N - 2)) {
			bonus += 1;
		}
		return bonus;
	}

	/*Questa funzione calcola il bonus per i pezzi isolati, cioè i pezzi che non hanno altri pezzi dello stesso colore nelle otto direzioni adiacenti. La funzione controlla se ci sono altri pezzi dello stesso colore nelle otto direzioni adiacenti alla cella dell'ultima mossa e assegna un bonus di -1 punto per ogni direzione in cui non ci sono altri pezzi dello stesso colore. In questo modo, i pezzi isolati ricevono un bonus negativo, che penalizza il giocatore che li ha posizionati. Infine, la funzione restituisce il bonus calcolato. */
	private int getIsolatedPiecesBonus(CXBoard board, CXCell lastMove) {
		int bonus = 0;
		int row = lastMove.i;
		int col = lastMove.j;
		CXCellState state = lastMove.state;
		int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
		for (int[] dir : directions) {
			int r = row + dir[0];
			int c = col + dir[1];
			if (r >= 0 && r < board.M && c >= 0 && c < board.N && board.cellState(r, c) == state) {
				bonus--;
			}
		}
		return bonus;
	}
	/*Questa funzione calcola il bonus per i pezzi che si trovano in angoli o bordi del tabellone. La funzione controlla se la cella dell'ultima mossa si trova in un angolo o su un bordo del tabellone e assegna un bonus di 5 punti se la cella si trova in un angolo e un bonus di 3 punti se la cella si trova su un bordo. In questo modo, le posizioni in angoli e sui bordi del tabellone ricevono un bonus maggiore rispetto alle posizioni al centro del tabellone. Infine, la funzione restituisce il bonus calcolato. */
	private int getCornerAndEdgePiecesBonus(CXBoard board, CXCell lastMove) {
		int bonus = 0;
		int row = lastMove.i;
		int col = lastMove.j;
		CXCellState state = lastMove.state;
		if ((row == 0 || row == board.M - 1) && (col == 0 || col == board.N - 1)) {
			bonus += 5; // angolo
		} else if (row == 0 || row == board.M - 1 || col == 0 || col == board.N - 1) {
			bonus += 3; // bordo
		}
		return bonus;
	}

	/*Questa funzione calcola il bonus per la mobilità, cioè il numero di posizioni vuote adiacenti alla cella dell'ultima mossa. La funzione controlla se ci sono posizioni vuote nelle otto direzioni adiacenti alla cella dell'ultima mossa e assegna un bonus di 1 punto per ogni posizione vuota trovata. In questo modo, le posizioni che consentono di avere più opzioni per la mossa successiva ricevono un bonus maggiore. Infine, la funzione restituisce il bonus calcolato. */
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

	/*Questa funzione calcola il bonus per la presenza di pezzi in posizioni chiave del tabellone. La funzione controlla se ci sono pezzi dello stato passato come parametro in alcune posizioni chiave del tabellone e assegna un bonus di 2 punti per ogni posizione chiave in cui è presente un pezzo dello stato. In questo modo, le posizioni chiave del tabellone ricevono un bonus maggiore rispetto alle altre posizioni. Le posizioni chiave utilizzate in questo esempio sono {0, 3}, {1, 2}, {1, 3}, {1, 4}, ma puoi modificare questa lista a seconda delle tue esigenze. Infine, la funzione restituisce il bonus calcolato */
	private int getKeyPiecesBonus(CXBoard board, CXCellState state) {
		int bonus = 0;
		int[][] keyPositions = {{0, 3}, {1, 2}, {1, 3}, {1, 4}};
		for (int[] pos : keyPositions) {
			if (board.cellState(pos[0], pos[1]) == state) {
				bonus += 2;
			}
		}
		return bonus;
	}

	/*Questa funzione calcola il bonus per la cattura e il blocco di pezzi dell'avversario. La funzione controlla se ci sono tre pezzi dello stato passato come parametro in una qualsiasi delle otto direzioni adiacenti alla cella dell'ultima mossa e assegna un bonus di 3 punti se la cattura è possibile (cioè non ci sono pezzi dell'avversario nella stessa direzione), oppure un bonus di 1 punto se il blocco è possibile (cioè c'è un solo pezzo dell'avversario nella stessa direzione). In questo modo, le posizioni che consentono di catturare o bloccare i pezzi dell'avversario ricevono un bonus maggiore. Infine, la funzione restituisce il bonus calcolato. */
	private int getCaptureAndBlockBonus(CXBoard board, CXCellState state) {
		int bonus = 0;
		CXCellState opponentState = (state == CXCellState.P1) ? CXCellState.P2 : CXCellState.P1; // stato dell'avversario
		int[][] directions = {{-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1}};
		for (int[] dir : directions) {
			int count = 0;
			boolean hasOpponent = false;
			int r = board.getLastMove().i + dir[0];
			int c = board.getLastMove().j + dir[1];
			while (r >= 0 && r < board.M && c >= 0 && c < board.N && count < 3) {
				if (board.cellState(r, c) == state) {
					count++;
				} else if (board.cellState(r, c) == opponentState) {
					hasOpponent = true;
					break;
				}
				r += dir[0];
				c += dir[1];
			}
			if (count == 3 && !hasOpponent) {
				bonus += 3; // cattura
			} else if (count == 2 && !hasOpponent) {
				bonus += 1; // blocco
			}
		}
		return bonus;
	}

	/*Questa funzione calcola il bonus per i pezzi vincenti, cioè i pezzi che fanno parte di una sequenza vincente di almeno 4 pezzi dello stesso colore. La funzione controlla se ci sono almeno 4 pezzi dello stato passato come parametro in una qualsiasi delle otto direzioni adiacenti alla cella dell'ultima mossa e assegna un bonus pari al numero di pezzi vincenti trovati. In questo modo, i pezzi che fanno parte di una sequenza vincente ricevono un bonus maggiore rispetto agli altri pezzi. Infine, la funzione restituisce il bonus calcolato. */
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

	/* 
	public void addHash(CXBoard B, Float value){
		String board = "";
		CXCell[] L = B.getMarkedCells(); 
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

	public double[] calculate_columns_value(int boardWidth){
		double[] columns_value = new double[boardWidth];
		for(float i = 0; i < boardWidth; i++){
			columns_value[(int)i] =  i < boardWidth/2 ? ( 1 + (i + 1)/(boardWidth/2) ) / 2 : ( 1 + (boardWidth - i)/(boardWidth/2) ) / 2;
		}
		return columns_value;
	}
}


