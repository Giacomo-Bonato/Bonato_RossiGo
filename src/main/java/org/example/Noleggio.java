package org.example;

public class Noleggio {
    String utente;
    Veicolo veicolo;
    int orainizio;
    int orafine;

    public Noleggio(String utente, Veicolo veicolo, int orainizio, int orafine) {
        this.utente = utente;
        this.veicolo = veicolo;
        this.orainizio = orainizio;
        this.orafine = orafine;
    }
}
