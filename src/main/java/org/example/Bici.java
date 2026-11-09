package org.example;

public class Bici extends Veicolo {
    private final int numeroMarce;

    public Bici(String codice, double livelloBatteria, int numeroMarce) {
        super(codice, livelloBatteria);
        if (numeroMarce < 1) {
            throw new IllegalArgumentException("La bici deve avere almeno una marcia");
        }
        this.numeroMarce = numeroMarce;
    }

    @Override
    public double calcolaCosto(int minuti) {
        return minuti * 0.15;
    }

    @Override
    public double consumoBatteriaPerMinuto() {
        return 0.05;
    }

    public int getNumeroMarce() {
        return numeroMarce;
    }
}
