package org.example;

public class Bici extends Veicolo {
    int marce;

    public Bici(int codice, double batteria, boolean stato, int marce) {
        super(codice, batteria, stato);
        this.marce = marce;
    }

    double calcolaCosto(int minuti) {
        return minuti * 0.25;
    }

    double consumoBatteriaPerMinuto() {
        return 0.5;
    }
    int getMarce() {
        return marce;
    }

}
