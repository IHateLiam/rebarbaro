# rebarbaro
progetto algoritmi 2023/2024


file readme
che ne so

se volete potete aggiungere il .gitignore

direi che gli unici file che modifichiamo sono quelli nella cartella Rebarbaro
quindi se nel commit avete altri file non caricateli ma rimetteteli come erano all'inizio

cominciamo a scrivere tutto quello che ci serve:

Rebarbaro:
    // costruttore per adesso non serve

void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs):
	- M - number of rows
	- N - number of columns
	- K - number of consecutive pieces needed to win
	- first - whether or not we are player 1
	- timeout_in_secs - number of seconds we have to make a move

    // The initPlayer function is called at the beginning of the game.

int minimax(CXBoard B, int depth, int alpha, int beta, boolean maximizingPlayer):
    - B - board
    - depth - depth of the search tree
    - alpha - alpha value
    - beta - beta value
    - maximizingPlayer - whether or not we are maximizing the score
  
    // The minimax function returns the score of the best move it can find. 
	// It is called recursively until the depth is 0.
	// The score is calculated by evaluating the board, which is done by counting the number of 4-in-a-row, 3-in-a-row, 2-in-a-row, and 1-in-a-row sequences for both players.
	// In the end, the score is the difference between the two players' 4-in-a-row sequences.
	// The function is called with the maximizing player set to true, so that the maximizing player's score is returned.
	// If the depth is 0, the board is evaluated and the score is returned.
	// If the board is full, the board is evaluated and the score is returned.
	// If the maximizing player can win in the next move, the board is evaluated and the score is returned.
	// If the minimizing player can win in the next move, the board is evaluated and the score is returned.
	// If none of these conditions are met, the available columns are iterated over and the minimax function is called recursively for each column.
	// The maximizing player's score is the maximum of the scores returned by the recursive calls.
	// The minimizing player's score is the minimum of the scores returned by the recursive calls.
	// The alpha-beta pruning technique is used to speed up the function.
	// The alpha and beta parameters are used to keep track of the best score for the maximizing player and the minimizing player, respectively.
	// The alpha and beta parameters are initialized with the smallest possible value for an integer and the largest possible value for an integer, respectively.
	// The alpha parameter is updated with the best score for the maximizing player.
	// The beta parameter is updated with the best score for the minimizing player.
	// If the alpha parameter is greater than or equal to the beta parameter, the alpha-beta pruning technique is used to skip the rest of the available columns.
	// The alpha-beta pruning technique is used to speed up the function and avoid exploring branches that cannot possibly influence the result of the function call.

    