# Java Commands
JC = javac
JR = java

# Directories
LIB_DIR = connectx
SRC_DIR = connectx
BUILD_DIR = build

# Packages and classes
MAIN_CLASS = connectx.CXGame
PLAYER_CLASS = connectx.Rebarbaro
RANDOM = connectx.L0.L0
QUASI_RANDOM = connectx.L1.L1
PLAYER_TESTER_CLASS = connectx.CXPlayerTester

# Command line options
OPTIONS = -cp "$(BUILD_DIR)/"

ALL_FILES = $(wildcard $(SRC_DIR)/*.java) $(wildcard $(SRC_DIR)/*/*.java) $(wildcard $(SRC_DIR)/*/*/*.java)

# Default parameters (can also be specified from command line)
CX = 6 7 4
REPS = 50

PLAYER_2 = connectx.L1.L1

app: build
	@$(JR) $(OPTIONS) $(MAIN_CLASS) $(CX) $(PLAYER_2) $(PLAYER_CLASS) 

test: build
	@echo "Testing..."
	@py tester.py

analyze: test
	@echo "Analyzing..."
	@py analyzer.py

run: build
	@echo "Running..."
	@$(JR) $(OPTIONS) $(PLAYER_TESTER_CLASS) $(CX) $(PLAYER_CLASS) $(PLAYER_2) -r $(REPS)
	@$(JR) $(OPTIONS) $(PLAYER_TESTER_CLASS) $(CX) $(PLAYER_2) $(PLAYER_CLASS) -r $(REPS)

build: clean $(ALL_FILES)
	@echo "Building..."
	@mkdir "$(BUILD_DIR)"
	@$(JC) -cp "$(SRC_DIR)/" -d "$(BUILD_DIR)/" -sourcepath "$(SRC_DIR)/" $(ALL_FILES)

# Removes both binaries and documentation
clean: clean-build 

# Removes binaries
clean-build:
	@rd /s /q $(BUILD_DIR)

clean-datasets:
	@rm -rf 