package connectx.Rebarbaro;

import java.util.LinkedList;

import connectx.CXCell;
import connectx.CXCellState;


/**
 * Rappresenta una combo nel gioco ConnectX, che e' costituita da una sequenza di celle
 * in una particolare direzione e appartenenti a uno specifico stato di cella (rosso o giallo).
 */
public class Combo {
    protected CXCellState myCellState; // Stato delle pedine della combo (rosso o giallo)
    protected LinkedList<CXCell> cellList; // Lista delle celle che compongono la combo
    protected Direction direction; // Direzione della combo
    protected int value; // Valore associato alla combo

    protected int N_mie; // Numero di pedine appartenenti al giocatore corrente nella combo
    protected int N_vuote; // Numero di caselle vuote nella combo
    protected int N_interruzioni; // Numero di interruzioni nella combo

    protected int length; // Lunghezza della combo
    protected int freeEnds; // Numero di estremita' aperte della combo

    protected boolean deadCombo;   //se e' chiusa da entrambi i lati e non e' lunga abbastanza da vincere

    public Combo() {
        this.cellList = new LinkedList<CXCell>();
        this.length = 0;
        this.value = 0;

        this.N_mie = 0;
        this.N_vuote = 0;
        this.N_interruzioni = 0;
        this.freeEnds = 0;

        this.deadCombo = false;
    }

    /**
     * Crea una combo con lo stato delle pedine specificato.
     *
     * @param myCellState lo stato delle pedine della combo (rosso o giallo)
     */
    public Combo(CXCellState myCellState, Direction direction) {
        this.myCellState = myCellState;
        this.cellList = new LinkedList<CXCell>();
        this.direction = direction;
        length = 0;
        value = 0;

        this.N_mie = 0;
        this.N_vuote = 0;
        this.N_interruzioni = 0;

        this.deadCombo = false;
    }

    /**
     * Calcola il valore della combo considerando diversi fattori come la lunghezza, il numero di pedine del giocatore corrente,
     * il numero di caselle vuote e il numero di interruzioni nella combo.
     * 
     * @param value il valore iniziale a cui aggiungere il valore calcolato della combo
     * @param X numero di pedine da mettere in file per vincere
     * @param M numero righe board
     * @return il valore aggiornato della combo
     */

    public int calculateComboValue(int value, int X, int M) {
        //int N_mie = 0;  // Contatore delle pedine del giocatore corrente
        //int N_vuote = 0;  // Contatore delle caselle vuote
        //int N_interruzioni = 0;  // Contatore delle interruzioni nella combo

        if(this.deadCombo) {
            
            if((this.length == X - 1) && (this.length == N_mie)) 
                return -1;   //voglio che le vittorie mancate per un pelo vengano penalizzate

            return 0;
        }



        //parametri che si possono regolare:
        int MOLTIPLICATORE_VALORE_COMBO = 5;
        float AUMENTATORE_PUNTEGGIO_APERTURA = (float)0.25;

        int interruzioni_effettive = N_interruzioni - freeEnds;
        int lung_striscia = N_mie + interruzioni_effettive;

            float util_striscia = (float)lung_striscia / (float)X;

            float pienezza = (float)N_mie / (float)lung_striscia;   

                float pdv = pienezza * util_striscia;

                    float combo_value_passaggio_intermedio = pdv * MOLTIPLICATORE_VALORE_COMBO;
                    
        
        float value_finale = combo_value_passaggio_intermedio * (1 + (freeEnds * AUMENTATORE_PUNTEGGIO_APERTURA));
        value_finale /= X;
       
        return (int)value_finale;

      
    }


    //ritorna le caselle vuote vincenti della striscia
    //e' una lista perche' con due estremita' vuote potrebbero essercene due
    public LinkedList<CXCell> findFreeWinningCells(int X, int lung_striscia) {

        LinkedList<CXCell> winningCells = new LinkedList<CXCell>();

        //se ha una casella vuota vincente in mezzo
        if(lung_striscia == X && this.N_mie == X - 1) {
            boolean myFirstCellFound = false;
            for(CXCell cell : this.cellList) {
                if(cell.state == myCellState) 
                    myFirstCellFound = true;
                
                if(myFirstCellFound && cell.state == CXCellState.FREE) 
                    winningCells.add(cell);
                
            }
        }

        //se ha una o due caselle vincenti agli estremi
        else if(!this.deadCombo && this.N_mie == X - 1 && lung_striscia == X - 1){
            CXCell old_Cell = this.cellList.getFirst();
            int winningCellsFound = 0;

            for(CXCell cell : this.cellList) {
                if(winningCellsFound >= this.freeEnds)   //se ho trovato tutte le caselle esco dal ciclo
                    break;

                //casella vuota prima della sequenza
                if(old_Cell.state == CXCellState.FREE && cell.state == this.myCellState) {
                    winningCells.add(old_Cell);
                    winningCellsFound++;
                }
                //casella vuota dopo la sequenza
                else if(old_Cell.state == this.myCellState && cell.state == CXCellState.FREE) {
                    winningCells.add(cell);
                    winningCellsFound++;
                }

                old_Cell = cell;
            }
        }

        return winningCells;
    }
    
    /**
     * Aggiunge (in fondo) una nuova cella alla combo e incrementa la lunghezza della combo.
     *
     * @param newCell la nuova cella da aggiungere alla combo
     */
    public void add(CXCell newCell) {
        cellList.add(newCell);
        length++;
    }

    public void addFirst(CXCell newCell) {
        cellList.addFirst(newCell);
        length++;
    }

    /**
     * Rimuove una cella non desiderata dalla combo e decrementa la lunghezza della combo.
     *
     * @param undesiredCell la cella da rimuovere dalla combo
     */
    public void remove(CXCell undesiredCell) {
        cellList.remove(undesiredCell);
        length--;
    }

    
    /**
     * Restituisce la lista delle celle che compongono la combo.
     *
     * @return la lista delle celle della combo
     */
    public LinkedList<CXCell> getCells() {
        return cellList;
    }

    /**
     * Restituisce la lunghezza della combo.
     *
     * @return la lunghezza della combo
     */
    public int getLength() {
        return length;
    }

    /**
     * Restituisce il valore associato alla combo.
     *
     * @return il valore della combo
     */
    public int getValue() {
        return this.value;
    }

    public void setValue(int newValue) {
        this.value = newValue;
    }

    /**
     * Restituisce il numero di estremita' libere della combo.
     *
     * @return il numero di estremita' libere della combo
     */
    public int getNumberOfFreeEnds() {
        return freeEnds;
    }

    /**
     * Imposta il numero di estremita' libere della combo.
     *
     * @param n il nuovo numero di estremita' libere della combo
     */
    public void setNumberOfFreeEnds(int n) {
        freeEnds = n;
    }

    /**
     * Restituisce la direzione della combo.
     *
     * @return la direzione della combo
     */
    public Direction getDirection() {
        return direction;
    }

    public CXCell firstCell() {
        return cellList.getFirst();
    }
    

}

 
