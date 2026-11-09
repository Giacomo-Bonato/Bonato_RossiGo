package org.example;

import org.apache.commons.lang3.Validate;

public abstract class Veicolo {
    private final String codice;
    private double livelloBatteria;
    private StatoVeicolo stato;

    protected Veicolo(String codice, double livelloBatteria) {
        Validate.isTrue(codice != null && codice.matches("[A-Z]{2}-\\d{4}"),
                "Il codice veicolo deve avere formato AA-0000");
        Validate.isTrue(livelloBatteria >= 0 && livelloBatteria <= 100,
                "La batteria deve essere compresa tra 0 e 100");
        this.codice = codice;
        this.livelloBatteria = livelloBatteria;
        this.stato = StatoVeicolo.LIBERO;
    }

    public String getCodice() {
        return codice;
    }

    public double getLivelloBatteria() {
        return livelloBatteria;
    }

    public StatoVeicolo getStato() {
        return stato;
    }

    public void setStato(StatoVeicolo stato) {
        this.stato = stato;
    }

    public void consumaBatteria(double consumo) {
        livelloBatteria = Math.max(0, livelloBatteria - consumo);
    }

    public abstract double calcolaCosto(int minuti);

    public abstract double consumoBatteriaPerMinuto();
}
