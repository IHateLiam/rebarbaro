package connectx.ReginaDegliArcieri;

public enum Direction {

    Vertical,

    Horizontal,

    Diagonal,

    AntiDiagonal;



    public int[] positiveDirection() {
        switch(this) {
            case Vertical:
                int[] verticalArray = {+1, 0};
                return verticalArray;
            case Horizontal:
                int[] horizontalArray = {0, +1};
                return horizontalArray;
            case Diagonal:
                int[] diagonalArray = {+1, +1};
                return diagonalArray;
            case AntiDiagonal:
                int[] antiDiagonalArray = {+1, -1};
                return antiDiagonalArray;
            default:
                //se (0,0) viene ritornato significa che l'argomento della funzione non era una direzione valida
                int[] errorArray = {0, 0};    
                return errorArray;
        }
    }

    public int[] negativeDirection() {
        switch(this) {
            case Vertical:
                int[] verticalArray = {-1, 0};
                return verticalArray;
            case Horizontal:
                int[] horizontalArray = {0, -1};
                return horizontalArray;
            case Diagonal:
                int[] diagonalArray = {-1, -1};
                return diagonalArray;
            case AntiDiagonal:
                int[] antiDiagonalArray = {-1, +1};
                return antiDiagonalArray;
            default:
                //se (0,0) viene ritornato significa che l'argomento della funzione non era una direzione valida
                int[] errorArray = {0, 0};    
                return errorArray;
        }
    }

} 
