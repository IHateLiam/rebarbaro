package connectx.RebarbaroMCR;


import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.TreeSet;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.w3c.dom.Node;


public class RebarbaroMCR implements CXPlayer {
    private Random rand;
	private CXGameState myWin;
	private CXGameState yourWin;
	private int  TIMEOUT;
	private long START;
	private double[] columns_value;
	int M, N, K;
	CXCellState first;
	private boolean debugMode;

	//private List<Combo> combinations;

	private int DECISIONTREEDEPTH;




    /*Default empty constructor*/
    public RebarbaroMCR() {

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


    private static final int DECISION_TREE_DEPTH = 15;
    private static final int SIMULATION_COUNT = 500;

    public int selectColumn(CXBoard board) {
        long startTime = System.currentTimeMillis();
       Integer[] availableColumns = board.getAvailableColumns();
		Node root = new Node((CXBoard) board.copy(), -1);
        for (int i = 0; i < availableColumns.length; i++) {
			try {
				Node child = root.getChild(availableColumns[i]);
				child.getBoard().markColumn(availableColumns[i]);  // Esegui la mossa sulla scheda del nodo figlio
				for (int j = 0; j < SIMULATION_COUNT; j++) {
					simulate(child);
					if(System.currentTimeMillis() - startTime > 9500){
						throw new TimeoutException();
					}
				}
			}
			catch(TimeoutException e) {
				break;
			}
        }
        float bestScore = Float.NEGATIVE_INFINITY;
        int bestColumn = -1;
        for (int i = 0; i < availableColumns.length; i++) {
            Node child = root.getChild(availableColumns[i]);
            float score = child.getScore();
            if (score > bestScore) {
                bestScore = score;
                bestColumn = availableColumns[i];
            }
        }
        long endTime = System.currentTimeMillis();
        //System.out.println("Time taken: " + (endTime - startTime) + "ms");
        return bestColumn;
    }

    private void simulate(Node node) {
   
		// Crea una copia della board del nodo
		CXBoard board = (CXBoard) node.getBoard().copy();

		// Ottieni il giocatore corrente dal nodo
		int currentPlayer = node.getCurrentPlayer();

		// Ottieni le colonne disponibili sulla board
		Integer[] availableColumns = board.getAvailableColumns();

		// Crea un oggetto Random per selezionare mosse casuali
		Random random = new Random();
		
		int depth = 0;
		// Continua a giocare finché la partita non è finita
		while (board.gameState() == CXGameState.OPEN && depth < DECISION_TREE_DEPTH ) {
			// Seleziona una colonna casuale tra quelle disponibili
			int column = availableColumns[random.nextInt(availableColumns.length)];

			// Esegui la mossa sulla board
			board.markColumn(column);

			// Cambia il giocatore corrente
			currentPlayer = (currentPlayer + 1) % 2;

			// Aggiorna le colonne disponibili
			availableColumns = board.getAvailableColumns();
		}

		// Aggiorna il nodo con lo stato finale della partita
		//node.scoreUpdate(evaluationFunction(board));
		node.update(board.gameState());
	}


private int selectMove(CXBoard board, int currentPlayer) {
    Integer[] availableColumns = board.getAvailableColumns();
    int bestColumn = availableColumns[0];
    float bestScore = Float.NEGATIVE_INFINITY;
    for (int i = 0; i < availableColumns.length; i++) {
        int column = availableColumns[i];
        float score = evaluationFunction(board);
        if (score > bestScore) {
            bestScore = score;
            bestColumn = column;
        }
    }
    return bestColumn;
}

    private class Node {
        private CXBoard board;
        private int column;       //(ale pensa che sia) la colonna dell'ultima mossa
        private int currentPlayer;
        private int visitCount;    //profondita' del nodo (forse)
        private float winCount;
		private float nodeScore;
        private List<Node> children;

        public Node(CXBoard board, int column) {
            this.board = board;
            this.column = column;
            this.currentPlayer = board.currentPlayer();;
            this.visitCount = 0;
            this.winCount = 0;
            this.children = new ArrayList<>();
        }

        public CXBoard getBoard() {
            return board;
        }

        public int getColumn() {
            return column;
        }

        public int getCurrentPlayer() {
            return currentPlayer;
        }

        public float getScore() {
            return winCount / visitCount;
        }

		public float getNodeScore() {
			return nodeScore;
		}

        public Node getChild(int column) {
			for (Node child : children) {
				if (child.getColumn() == column) {
					return child;
				}
			}
			// Se il nodo figlio non esiste, crealo
			Node child = new Node(board.copy(), column);
            children.add(child);
            return child;
		}

        public void update(CXGameState gameState) {
            visitCount++;
            if (gameState == CXGameState.WINP1) {
                winCount += currentPlayer == 0 ? 1 : 0;
            } else if (gameState == CXGameState.WINP2) {
                winCount += currentPlayer == 1 ? 1 : 0;
            } else if(gameState == CXGameState.DRAW) {
                winCount += 0.5;
            }
			//else
				//winCount += evaluationFunction(this.board);
        }

		public void scoreUpdate(int score) {
			this.nodeScore = score;
		}
    }


	
	//Il codice implementa l'algoritmo minimax con potatura alpha-beta, con una profondita' massima di 4 (scelta arbitraria). La funzione minimax ritorna 1 se il giocatore che sta massimizzando ha vinto, -1 altrimenti; ritorna -1 se il giocatore che sta massimizzando ha perso, 1 altrimenti; 0 in caso di pareggio. La funzione minimax e' ricorsiva, e viene eseguita una volta per ogni colonna disponibile. La funzione minimax riceve come parametri: l'oggetto CXBoard, la profondita' di ricerca, la prima mossa da eseguire, i valori di alpha e beta e una variabile booleana che indica quale giocatore sta massimizzando. La funzione ritorna l'intero corrispondente al punteggio ottenuto dalla mossa.


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

	


	private void checktime() throws TimeoutException {
		if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
			throw new TimeoutException();
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
		return "RebarbaroMCR";
	}


	public double[] calculate_columns_value(int boardWidth){
		double[] columns_value = new double[boardWidth];
		for(float i = 0; i < boardWidth; i++){
			columns_value[(int)i] =  i < boardWidth/2 ? ( 1 + (i + 1)/(boardWidth/2) ) / 2 : ( 1 + (boardWidth - i)/(boardWidth/2) ) / 2;
		}
		return columns_value;
	}

/*
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
*/
}
