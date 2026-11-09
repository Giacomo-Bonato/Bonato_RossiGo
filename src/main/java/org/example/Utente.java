package org.example;

import org.apache.commons.lang3.StringUtils;

public class Utente {
    private final String id;
    private final String nome;
    private final String email;
    private final String codiceFiscale;
    private double credito;

    public Utente(String id, String nome, String email, String codiceFiscale, double credito) {
        if (StringUtils.isBlank(id) || StringUtils.isBlank(nome) || StringUtils.isBlank(email)) {
            throw new IllegalArgumentException("Id, nome ed email sono obbligatori");
        }
        if (credito < 0) {
            throw new IllegalArgumentException("Il credito non può essere negativo");
        }
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.codiceFiscale = codiceFiscale;
        this.credito = credito;
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getCodiceFiscale() {
        return codiceFiscale;
    }

    public double getCredito() {
        return credito;
    }

    public void addebita(double importo) {
        credito -= importo;
    }
}
