import subprocess;
import re
import threading;
import tabulate;
from alive_progress import alive_bar; import time
import concurrent.futures

"""
pip install alive-progress
pip install tabulate
"""

build_dir = "build/"

configs = []

with open("configs.txt", "r") as f:
	for line in f:
		if '#' not in line and line.strip() != "":
			configs.append(line.replace("\n", ""))

lock = threading.Lock()

class player:
	name = ""
	score = 0
	wins = 0
	wins_as_first = 0
	wins_as_second = 0
	losses = 0
	losses_as_first = 0
	losses_as_second = 0
	ties = 0
	errors = 0

	def __init__(self, name):
		self.name = name
		self.score = 0
		self.wins = 0
		self.wins_as_first = 0
		self.wins_as_second = 0
		self.losses = 0
		self.losses_as_first = 0
		self.losses_as_second = 0
		self.ties = 0
		self.errors = 0

	def update(self, score, wins, losses, ties, errors, first = True):
		lock.acquire()
		self.score += score
		self.wins += wins
		self.losses += losses
		self.ties += ties
		self.errors += errors

		if first:
			if(wins == 1):
				self.wins_as_first += 1
			elif(losses == 1):
				self.losses_as_first += 1
		else:
			if(wins == 1):
				self.wins_as_second += 1
			elif(losses == 1):
				self.losses_as_second += 1

		lock.release()

	def __lt__(self, other):
		return self.score < other.score
	
	def __eq__(self, other):
		return self.score == other.score

	def to_print(self):
		return [self.name[self.name.rfind(".")+1:], self.score, self.wins, self.losses, self.ties, self.wins_as_first, self.wins_as_second, self.losses_as_first,self.losses_as_second, self.errors]

players = []

with open("players.txt", "r") as f:
	for line in f:
		if '#' not in line and line.strip() != "":
			players.append(player(line.strip()))

totGames = len(configs) * len(players) * (len(players)-1)

def play(player1: player, player2: player, bar):
	for config in configs:
		ret = subprocess.check_output("java -cp " + build_dir + " connectx.CXPlayerTester " + config + " " + player1.name + " " + player2.name, shell=True)
		ret = ret.decode("utf-8")

		name1 = player1.name[player1.name.rfind(".")+1:]
		name2 = player2.name[player2.name.rfind(".")+1:]

		ret = ret.replace(name1, "PLAYER")
		ret = ret.replace(name2, "PLAYER")

		arr = re.findall("[0-9]+", ret)
		arr = arr[-10:]
		player1.update(int(arr[0]), int(arr[1]), int(arr[2]), int(arr[3]), int(arr[4]), True)
		player2.update(int(arr[5]), int(arr[6]), int(arr[7]), int(arr[8]), int(arr[9]), False)

		bar()

def subsets(arr, r):
	return _subsets(arr, r, 0, [], [])

def _subsets(arr, r, index, data, result):
	if len(data) == r:
		result.append(data.copy())
		return

	if index >= len(arr):
		return

	data.append(arr[index])
	_subsets(arr, r, index+1, data, result)
	data.pop()
	_subsets(arr, r, index+1, data, result)

	return result

games = subsets(range(len(players)), 2)

def play_games(game, bar):
	play(players[game[0]], players[game[1]], bar)
	play(players[game[1]], players[game[0]], bar)

with alive_bar(totGames) as bar:
	with concurrent.futures.ThreadPoolExecutor() as executor:
		executor.map(play_games, games, [bar]*len(games))

players.sort(reverse=True)

table = [["Name", "Score", "Wins", "Losses", "Ties","Wins as first", "Wins as second", "Losses as first", "Losses as second", "Errors"]]
for player in players:
	table.append(player.to_print())

print(tabulate.tabulate(table, headers="firstrow", tablefmt="fancy_grid"))