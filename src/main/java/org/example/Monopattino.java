package org.example;

public class Monopattino extends Veicolo {

    double velocitamax;

    public Monopattino(int codice, double batteria, boolean stato, double velocitamax) {
        super(codice, batteria, stato);
        this.velocitamax = velocitamax;
    }

    double calcolaCosto(int minuti) {
        return minuti * 0.5;
    }

    double consumoBatteriaPerMinuto() {
        return 1;
    }
}
