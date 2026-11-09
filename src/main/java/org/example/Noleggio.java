package org.example;

import java.time.LocalDateTime;

public class Noleggio {
    private final Utente utente;
    private final Veicolo veicolo;
    private final LocalDateTime orarioInizio;
    private LocalDateTime orarioFine;
    private int minutiUsati;
    private double costo;
    private long idDatabase;

    public Noleggio(Utente utente, Veicolo veicolo) {
        this(utente, veicolo, LocalDateTime.now(), null, 0, 0, 0);
    }

    public Noleggio(Utente utente, Veicolo veicolo, LocalDateTime orarioInizio,
                    LocalDateTime orarioFine, int minutiUsati, double costo, long idDatabase) {
        this.utente = utente;
        this.veicolo = veicolo;
        this.orarioInizio = orarioInizio;
        this.orarioFine = orarioFine;
        this.minutiUsati = minutiUsati;
        this.costo = costo;
        this.idDatabase = idDatabase;
    }

    public void termina(int minutiUsati, double costo) {
        termina(minutiUsati, costo, LocalDateTime.now());
    }

    public void termina(int minutiUsati, double costo, LocalDateTime orarioFine) {
        this.minutiUsati = minutiUsati;
        this.costo = costo;
        this.orarioFine = orarioFine;
    }

    public Utente getUtente() {
        return utente;
    }

    public Veicolo getVeicolo() {
        return veicolo;
    }

    public LocalDateTime getOrarioInizio() {
        return orarioInizio;
    }

    public LocalDateTime getOrarioFine() {
        return orarioFine;
    }

    public int getMinutiUsati() {
        return minutiUsati;
    }

    public double getCosto() {
        return costo;
    }

    public long getIdDatabase() {
        return idDatabase;
    }

    public void setIdDatabase(long idDatabase) {
        this.idDatabase = idDatabase;
    }
}
