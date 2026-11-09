package org.example;

import it.kamaladafrica.codicefiscale.CodiceFiscale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlottaService {
    private static final double SOGLIA_SBLOCCO = 1.0;
    private final Database database;
    private final Map<String, Veicolo> veicoli = new LinkedHashMap<>();
    private final Map<String, Utente> utenti = new LinkedHashMap<>();
    private final Map<String, Noleggio> noleggiAttivi = new LinkedHashMap<>();
    private final List<Noleggio> storicoNoleggi = new ArrayList<>();

    public FlottaService() throws RossiGoException {
        this(connettiDatabase());
    }

    public FlottaService(Database database) throws RossiGoException {
        this.database = database;
        try {
            for (Utente utente : database.caricaUtenti()) {
                utenti.put(utente.getId(), utente);
            }
            for (Veicolo veicolo : database.caricaVeicoli()) {
                veicoli.put(veicolo.getCodice(), veicolo);
            }
            for (Noleggio noleggio : database.caricaNoleggi(
                    new ArrayList<>(utenti.values()), new ArrayList<>(veicoli.values()))) {
                if (noleggio.getOrarioFine() == null) {
                    noleggio.getVeicolo().setStato(StatoVeicolo.IN_USO);
                    noleggiAttivi.put(noleggio.getVeicolo().getCodice(), noleggio);
                } else {
                    storicoNoleggi.add(noleggio);
                }
            }
        } catch (java.sql.SQLException | IllegalArgumentException e) {
            throw new RossiGoException("Impossibile caricare i dati della flotta dal database.", e);
        }
    }

    private static Database connettiDatabase() throws RossiGoException {
        try {
            return Database.getInstance();
        } catch (java.sql.SQLException e) {
            throw new RossiGoException("Impossibile collegarsi al database RossiGo.", e);
        }
    }

    public void registraVeicolo(Veicolo veicolo) throws RossiGoException {
        if (veicolo == null) {
            throw new RossiGoException("Il veicolo da registrare non può essere nullo");
        }
        if (veicoli.containsKey(veicolo.getCodice())) {
            throw new RossiGoException("Esiste già un veicolo con codice " + veicolo.getCodice());
        }
        try {
            database.salvaVeicolo(veicolo);
            veicoli.put(veicolo.getCodice(), veicolo);
        } catch (java.sql.SQLException e) {
            throw new RossiGoException("Impossibile salvare il veicolo nel database.", e);
        }
    }

    public void registraUtente(Utente utente) throws CodiceFiscaleNonValidoException, RossiGoException {
        if (utente == null) {
            throw new RossiGoException("L'utente da registrare non può essere nullo");
        }
        if (!CodiceFiscale.isFormatValid(utente.getCodiceFiscale())) {
            throw new CodiceFiscaleNonValidoException(
                    "Il codice fiscale inserito non è valido: " + utente.getCodiceFiscale());
        }
        if (utenti.containsKey(utente.getId())) {
            throw new RossiGoException("Esiste già un utente con id " + utente.getId());
        }
        try {
            database.salvaUtente(utente);
            utenti.put(utente.getId(), utente);
        } catch (java.sql.SQLException e) {
            throw new RossiGoException("Impossibile salvare l'utente nel database.", e);
        }
    }

    public void sbloccaVeicolo(String codice, String idUtente)
            throws RossiGoException {
        Veicolo veicolo = trovaVeicolo(codice);
        Utente utente = utenti.get(idUtente);
        if (utente == null) {
            throw new UtenteNonTrovatoException("Utente " + idUtente + " non trovato.");
        }
        if (veicolo.getStato() != StatoVeicolo.LIBERO) {
            throw new VeicoloNonDisponibileException("Il veicolo " + codice + " non è disponibile.");
        }
        if (veicolo.getLivelloBatteria() < 15) {
            throw new BatteriaScaricaException(
                    "Batteria al " + veicolo.getLivelloBatteria() + "%, scegli un altro mezzo.",
                    veicolo.getLivelloBatteria());
        }
        if (utente.getCredito() < SOGLIA_SBLOCCO) {
            throw new CreditoInsufficienteException(
                    "Credito insufficiente: serve almeno 1 € per sbloccare il mezzo.");
        }

        Noleggio noleggio = new Noleggio(utente, veicolo);
        try {
            noleggio.setIdDatabase(database.avviaNoleggio(noleggio));
            veicolo.setStato(StatoVeicolo.IN_USO);
            noleggiAttivi.put(codice, noleggio);
        } catch (java.sql.SQLException e) {
            throw new RossiGoException("Impossibile registrare l'avvio del noleggio nel database.", e);
        }
    }

    public double terminaNoleggio(String codice, int minutiUsati) throws RossiGoException {
        if (minutiUsati <= 0) {
            throw new RossiGoException("La durata del noleggio deve essere maggiore di zero.");
        }
        Noleggio noleggio = noleggiAttivi.get(codice);
        if (noleggio == null) {
            throw new VeicoloNonDisponibileException("Non risulta un noleggio attivo per " + codice + ".");
        }

        Veicolo veicolo = noleggio.getVeicolo();
        Utente utente = noleggio.getUtente();
        double costo = 1.0 + veicolo.calcolaCosto(minutiUsati);
        if (utente.getCredito() < costo) {
            throw new CreditoInsufficienteException(
                    String.format("Credito insufficiente per terminare il noleggio: costo %.2f €.", costo));
        }

        double nuovaBatteria = Math.max(0, veicolo.getLivelloBatteria()
                - minutiUsati * veicolo.consumoBatteriaPerMinuto());
        double nuovoCredito = utente.getCredito() - costo;
        java.time.LocalDateTime oraFine = java.time.LocalDateTime.now();
        try {
            database.terminaNoleggio(noleggio, minutiUsati, costo, nuovoCredito,
                    nuovaBatteria, oraFine);
        } catch (java.sql.SQLException e) {
            throw new RossiGoException("Impossibile salvare la chiusura del noleggio nel database.", e);
        }
        utente.addebita(costo);
        veicolo.consumaBatteria(minutiUsati * veicolo.consumoBatteriaPerMinuto());
        veicolo.setStato(StatoVeicolo.LIBERO);
        noleggio.termina(minutiUsati, costo, oraFine);
        noleggiAttivi.remove(codice);
        storicoNoleggi.add(noleggio);
        return costo;
    }

    public void mettiInRicarica(String codice) throws RossiGoException {
        Veicolo veicolo = trovaVeicolo(codice);
        if (veicolo.getStato() == StatoVeicolo.IN_USO) {
            throw new VeicoloNonDisponibileException("Il veicolo " + codice + " è ancora in uso.");
        }
        try {
            database.mettiInRicarica(veicolo);
            veicolo.setStato(StatoVeicolo.IN_RICARICA);
        } catch (java.sql.SQLException e) {
            throw new RossiGoException("Impossibile salvare lo stato di ricarica nel database.", e);
        }
    }

    public Veicolo getVeicolo(String codice) throws RossiGoException {
        return trovaVeicolo(codice);
    }

    public List<Noleggio> getStoricoNoleggi() {
        return Collections.unmodifiableList(storicoNoleggi);
    }

    public boolean archivioVuoto() throws RossiGoException {
        try {
            return database.archivioVuoto();
        } catch (java.sql.SQLException e) {
            throw new RossiGoException("Impossibile verificare lo stato del database.", e);
        }
    }

    private Veicolo trovaVeicolo(String codice) throws RossiGoException {
        Veicolo veicolo = veicoli.get(codice);
        if (veicolo == null) {
            throw new RossiGoException("Veicolo " + codice + " non trovato.");
        }
        return veicolo;
    }
}
