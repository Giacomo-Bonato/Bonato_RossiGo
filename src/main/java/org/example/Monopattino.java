package org.example;

public class Monopattino extends Veicolo {
    private final double velocitaMassima;

    public Monopattino(String codice, double livelloBatteria, double velocitaMassima) {
        super(codice, livelloBatteria);
        if (velocitaMassima <= 0) {
            throw new IllegalArgumentException("La velocità massima deve essere positiva");
        }
        this.velocitaMassima = velocitaMassima;
    }

    @Override
    public double calcolaCosto(int minuti) {
        return minuti * 0.25;
    }

    @Override
    public double consumoBatteriaPerMinuto() {
        return 0.1;
    }

    public double getVelocitaMassima() {
        return velocitaMassima;
    }
}
