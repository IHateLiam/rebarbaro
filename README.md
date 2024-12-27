# Rebarbaro: A Connect X Bot

This repository contains an *AI Opponent* named __Rebarbaro__ for a generalized Connect X game, extending the classic “Connect Four” concept to grids of up to 100×100 and up to 30 required consecutive pieces for a win condition. The code in [Rebarbaro.java](CXGame1.0/connectx/Rebarbaro/Rebarbaro.java) is part of a project for an _"Algoritmi e Strutture Dati"_ (Algorithms and Data Structures) exam.

The Computer Player is able to play any game configuration, with any _n_, _m_ (between 4 and 100), and _x_ (between 3 and 30), being respectively the number of columns of the board, the number of rows, and the number of consecutive pieces (horizontal, vertical or diagonal) needed to win.

In a standard Connect Four board (7×6), there are around 5.3×10<sup>14</sup> possible board states, whereas a 100×100 board can reach about 2.53×10<sup>3040</sup> possible states. This sheer number of configurations calls for efficient heuristics and pruning strategies to make decision-making computationally feasible.

## Project Overview
The objective is to determine the best column to place a piece, returning the column index as the chosen move. This approach uses a MinMax tree (with alpha-beta pruning) and a combo-based evaluation system to score potential lines on the board. Partial transposition/caching features appear in the code, though these are not fully integrated.

This project was developed entirely in Java. The graphical interface and game engine were provided by the instructor.

The project was evaluated through a comprehensive tournament that included every student’s bot as well as the instructor’s reference opponents. The tournament’s final scoreboard formed the backbone of the exam grading.


## Highlights
- ### MinMax tree with alpha-beta pruning.
   Rebarbaro’s decision-making revolves around a MinMax function that:
  - Recursively simulates moves for both sides (maximizing) and opponent (minimizing).
  - Uses alpha and beta thresholds at each level to prune moves that do not affect the final outcome.
  - Evaluates terminal or depth-limited states using combo-based scoring.
  - Returns the best score, which maps back to the best column.

- ### Dynamic search depth that adjusts based on remaining time.
  Each column search is given a time budget, which is determined by the total allowed time divided among all columns. If a column’s evaluation is completed quickly, the search depth for the subsequent columns may increase. Conversely, if it approaches the time limit, the depth may be reduced to ensure timely results. This allows our bot to adapt its search depth based on the computational overhead of each move.

- ### Evaluation function built around the concept of combos (sequences of aligned cells).
  Rebarbaro identifies potential lines of pieces through “combos”. Each combo tracks how many pieces belong to the Player, how many slots remain open, and whether the line is blocked on either side. These statistics (like free ends, filled cells, and interruptions) inform a scoring function that measures the strategic value of each sequence. The Player then sums up the scores of all combos to evaluate a board state.
  
  This evaluation function is used to navigate through the decision tree, and to decide which branches will be pruned.

- ### Partial transposition table support (not fully completed).
  While not fully integrated, the idea is, using transposition tables, to cache previously analyzed board positions. The objective is to reduce redundant MinMax calculations by storing move evaluations in a hash map keyed to a unique board state (using Zobrist or FNV-1a hashing). On encountering a repeat position, the Player would retrieve its score from the cache instead of recalculating it, saving time.


## Code Structure
- [Rebarbaro.java](CXGame1.0/connectx/Rebarbaro/Rebarbaro.java)  
  Contains the main Player logic, including MinMax calls, time management, and combo updates.
- [Combo.java](CXGame1.0/connectx/Rebarbaro/Combo.java) and [Direction.java](CXGame1.0/connectx/Rebarbaro/Direction.java)  
  Manage the concept of sequences of cells and directions on the board.
- [NodeData.java](CXGame1.0/connectx/Rebarbaro/file_non_consegnati/NodeData.java), [Clonazione.java](CXGame1.0/connectx/Rebarbaro/file_non_consegnati/Clonazione.java), [Mongolfiera.java](CXGame1.0/connectx/Rebarbaro/file_non_consegnati/Mongolfiera.java)  
  Provide partial implementations for transposition-based move caching.
- [L0](CXGame1.0/connectx/L0/L0.java), [L1](CXGame1.0/connectx/L1/L1.java)  
  Some basic opponents were provided by the instructor. These being the random player (L0) and a random player that only checks if he can win in the current turn, and, if not, checks if he's gonna lose just after his move (L1). These were used for testing, while more skilled players' code were kept secret by the instructor.


## Further Reading
A summary is also available in the final paper (presentazione.pdf), used for the oral discussion part of the exam. This repository showcases practical usage of search algorithms and data structures in game-playing artificial opponents. Instructions on how to compile and play against Rebarbaro can be found in the inner [README](CXGame1.0/connectx/README). Enjoy!




