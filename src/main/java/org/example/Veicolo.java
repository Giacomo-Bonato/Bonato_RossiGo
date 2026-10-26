package org.example;

public abstract class Veicolo {
    int codice;
    double batteria;
    boolean stato;

    public Veicolo(int codice, double batteria, boolean stato) {
        this.codice = codice;
        this.batteria = batteria;
        this.stato = stato;
    }

    abstract double calcolaCosto(int minuti);
    abstract double consumoBatteriaPerMinuto();

}
