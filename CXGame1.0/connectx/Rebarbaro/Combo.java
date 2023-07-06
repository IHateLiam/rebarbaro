package connectx.Rebarbaro;

import java.util.TreeSet;
import java.util.LinkedList;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;


/**
 * Rappresenta una combo nel gioco ConnectX, che e' costituita da una sequenza di celle
 * in una particolare direzione e appartenenti a uno specifico stato di cella (rosso o giallo).
 */
public class Combo {
    private CXCellState myCellState; // Stato delle pedine della combo (rosso o giallo)
    private LinkedList<CXCell> cellList; // Lista delle celle che compongono la combo
    private Direction direction; // Direzione della combo
    private int value; // Valore associato alla combo

    protected int N_mie; // Numero di pedine appartenenti al giocatore corrente nella combo
    protected int N_vuote; // Numero di caselle vuote nella combo
    protected int N_interruzioni; // Numero di interruzioni nella combo

    private int length; // Lunghezza della combo
    private int freeEnds; // Numero di estremita' aperte della combo

    protected boolean deadCombo;   //se e' chiusa da entrambi i lati e non e' lunga abbastanza da vincere

    public Combo() {
        cellList = new LinkedList<CXCell>();
        length = 0;
        value = 0;

        this.N_mie = 0;
        this.N_vuote = 0;
        this.N_interruzioni = 0;

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
     * @return il valore aggiornato della combo
     */

    public int calculateComboValue(int value, int X) {
        //int N_mie = 0;  // Contatore delle pedine del giocatore corrente
        //int N_vuote = 0;  // Contatore delle caselle vuote
        //int N_interruzioni = 0;  // Contatore delle interruzioni nella combo
/*
 * 
 
        int length_weight = 2;  // Peso per la lunghezza della combo
        int N_mie_weight = 5;  // Peso per il numero di pedine del giocatore corrente
        int N_vuote_weight = 1;  // Peso per il numero di caselle vuote
        int N_interruzioni_weight = 10;  // Peso per il numero di interruzioni
        
        // Itera attraverso tutte le celle nella combo
        for (CXCell cell : cellList) {
            if (cell.state == myCellState) {
                N_mie++;  // Incrementa il contatore delle pedine del giocatore corrente
            } else if (cell.state == CXCellState.FREE) {
                N_vuote++;  // Incrementa il contatore delle caselle vuote
            } else {
                N_interruzioni++;  // Incrementa il contatore delle interruzioni
            }
        }
        
        // Calcola il valore della combo considerando i pesi dei diversi fattori
        int calculatedValue = length * length_weight + N_mie * N_mie_weight + N_vuote * N_vuote_weight - N_interruzioni * N_interruzioni_weight;
        
        // Aggiorna il valore esterno "value" sommando il valore calcolato
        return value + calculatedValue;
        */

        if(this.deadCombo) {
            //System.err.print(" DEBUG sono dentro calculateComboValue e questa combo e' morta, direzione: " + this.direction + " \n"); //DEBUGG
            return 0;
        }

        //parametri che si possono regolare:
        int MOLTIPLICATORE_VALORE_COMBO = 5;
        float AUMENTATORE_PUNTEGGIO_APERTURA = (float)0.25;

        int interruzioni_effettive = N_interruzioni - freeEnds;
        int lung_striscia = N_mie - interruzioni_effettive;
        float util_striscia = (float)lung_striscia / (float)X;

        float pienezza = (float)N_mie / (float)this.length;

        float pdv = pienezza * util_striscia;

        float combo_value_passaggio_intermedio = pdv * MOLTIPLICATORE_VALORE_COMBO;
        
        float value_finale = combo_value_passaggio_intermedio * (1 + AUMENTATORE_PUNTEGGIO_APERTURA * freeEnds);

        /*
         * debug totale delle combo quindi lo commento invece di cancellarlo, giusto perche' magari torna utile
         
        System.err.print(" - DEBUG valutando combo di direzione: " + this.direction + "\n"); //DEBUGG
        System.err.print(" - DEBUG valutando combo prima casella: i: " + this.firstCell().i + " j: " + this.firstCell().j + " state: " + this.firstCell().state + "\n"); //DEBUGG

        System.err.print(" t DEBUG this.length.: " + this.length + "\n"); //DEBUGG
        System.err.print(" t DEBUG this.N_interruzioni.: " + this.N_interruzioni + "\n"); //DEBUGG
        System.err.print(" t DEBUG this.N_mie: " + this.N_mie + "\n"); //DEBUGG
        System.err.print(" t DEBUG this.N_vuote: " + this.N_vuote + "\n"); //DEBUGG
        System.err.print(" t DEBUG this.freeEnds: " + this.freeEnds + "\n"); //DEBUGG
        System.err.print(" - DEBUG interruzioni_effettive: " + interruzioni_effettive + "\n"); //DEBUGG
        System.err.print(" - DEBUG lung_striscia: " + lung_striscia + "\n"); //DEBUGG
        System.err.print(" - DEBUG X         : " + X + "\n"); //DEBUGG
        System.err.print(" - DEBUG util_striscia: " + util_striscia + "\n"); //DEBUGG
        System.err.print(" - DEBUG pienezza: " + pienezza + "\n"); //DEBUGG
        System.err.print(" - DEBUG pdv: " + pdv + "\n"); //DEBUGG
        System.err.print(" - ---- -- -\n\n"); //DEBUGG

        //DEBUGG tutto l'if sotto
        if(this.direction == Direction.Horizontal) {
            System.err.print("coordinate celle della combo:\n");
            for (CXCell celli : this.cellList) {
                System.err.print("i: " + celli.i + " j: " + celli.j + "\n");
            }
            System.err.print(" - ---- -- -\n\n"); //DEBUGG
        }


        if(value_finale != 0) {   //tutto l'if //DEBUGG

            System.err.print(" - DEBUG sono dentro combo calcuate value e value_finale = " + value_finale+ " - \n"); //DEBUGG
        }
        */

        return (int)value_finale;
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

    /**
     * Calcola il numero di estremita' libere nella combo in base alla direzione.
     * Questo metodo dovrebbe essere chiamato dopo l'aggiunta o la rimozione di celle nella combo.
     */
    public int calculateFreeEnds(CXBoard B) {
        int[] dir_p = this.direction.positiveDirection();
        int[] dir_n = this.direction.negativeDirection();

        CXCell firstCell = cellList.getFirst();
        CXCell lastCell  = cellList.getLast();

        int n_free_ends = 0;

       return 0;
    }

    public CXCell firstCell() {
        return cellList.getFirst();
    }
    

}

