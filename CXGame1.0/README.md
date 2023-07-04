# ConnectX

Algorithms and data structures project 22/23

## Commands

- `make run` - runs 50 games of ConnectX with the default players, both as player 1 and player 2. The results are printed to the console. One can modify the number of games or the configuration by changing the `REPS` and  `CX` variables in the Makefile, like this:
  - `make run REPS="100" CX="4 4 3"`  

- `make build` - builds the project. By default, all sources are in the `src` directory, including the connectx library. The classes are placed inside the build directory.
- `make clean` - removes all the object files and the executable
- `make test` - runs a python script which is essentially a tournament between indicated players.  
- `make analyze` - runs a python script which analyzes the results of the tournament and plots the data (only for players that produce a .csv file).  

### To do

- Killer moves
- Move ordering
- fix transposition table
- try negamax or negascout
- fix zobrist hashing (optimization)
