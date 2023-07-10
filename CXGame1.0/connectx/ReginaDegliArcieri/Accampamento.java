package connectx.ReginaDegliArcieri;

import connectx.CXBoard;
import connectx.CXCell;
import connectx.CXGameState;
import connectx.CXCellState;

import java.util.Random;
import java.util.HashMap;

public class Accampamento {
    
    public enum Flag {
        EXACT,
        LOWER_BOUND,
        UPPER_BOUND
    }

    private class Truppa {
        public float score;
        public int depth;   //la depth e' il numero di mosse fatte (realmente) al momento della valutazione
        public Flag flag;

        public Truppa(float score, int depth, Flag flag) {
            this.score = score;
            this.depth = depth;
            this.flag = flag;
        }
    }

    protected long[][][] zobristTable;
    protected HashMap<Long, Truppa> TTable;

    protected int size = 2 << 20;
    protected int M, N;
    protected Random rand;

    public Accampamento(int M, int N, int size) {
        this.size = size;
        TTable = new HashMap<Long, Truppa>(size);

        this.M = M;
        this.N = N;
        this.zobristTable = new long[M][N][2];

        rand = new Random(System.currentTimeMillis());

        for(int i = 0; i < M; i++) {
            for(int j = 0; j < N; j++) {
                for(int k = 0; k < 2; k++) {
                    zobristTable[i][j][k] = rand.nextLong();
                }
            }
        }
    }

    public long getHash(CXBoard B) {
        long hash = 0;
        for(int i = 0; i < M; i++) {
            for(int j = 0; j < N; j++) {
                if(B.cellState(i, j) != CXCellState.FREE) {
                    hash ^= zobristTable[i][j][B.cellState(i, j).ordinal()];
                }
            }
        }
        return hash;
    }

    public void storeNewTroop(long hash, float value, int depth, Flag flag) {
        Truppa t = TTable.get(hash);
        if(t == null) {
            TTable.put(hash, new Truppa(value, depth, flag));
        } else {
            if(t.depth <= depth) {
                TTable.replace(hash, new Truppa(value, depth, flag));
            }
        }
    }

    public float searchTroop(long hash, int depth, float alpha, float beta) {
        float value = Integer.MAX_VALUE;
        Truppa t = TTable.get(hash);
        if(t != null) {
            if(t.depth >= depth) {
                if(t.flag == Flag.EXACT) {
                    value = t.score;
                } else if( t.flag == Flag.LOWER_BOUND && t.score <= alpha) {
                    value = alpha;
                } else if(t.flag == Flag.UPPER_BOUND && t.score >= beta) {
                    value = beta;
                }
            }
        }

        return value;
    }
}