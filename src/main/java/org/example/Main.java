package org.example;

import com.github.lalyos.jfiglet.FigletFont;

import java.io.IOException;
import java.sql.SQLException;

public class Main {
    private Main() {
    }

    public static void main(String[] args) {
        stampaTitolo();
        try (Database database = new Database()) {
            FlottaService flotta = new FlottaService(database);
            if (flotta.archivioVuoto()) {
                registraDatiDimostrativi(flotta);
            }
            simulaGiornata(flotta);
        } catch (SQLException | RossiGoException e) {
            System.out.println("Impossibile avviare RossiGo: " + e.getMessage());
        }
    }

    private static void registraDatiDimostrativi(FlottaService flotta) throws RossiGoException {
        flotta.registraVeicolo(new Bici("BK-0042", 85, 7));
        flotta.registraVeicolo(new Bici("BK-0043", 75, 6));
        flotta.registraVeicolo(new Monopattino("SC-0107", 90, 25));
        flotta.registraVeicolo(new Monopattino("SC-0108", 10, 20));

        flotta.registraUtente(new Utente("1", "Mario Rossi", "mario@example.it",
                "RSSMRA85T10A562S", 20));
        flotta.registraUtente(new Utente("2", "Giulia Bianchi", "giulia@example.it",
                "RSSMRA85T10A562S", 0.50));
        flotta.registraUtente(new Utente("3", "Luca Verdi", "luca@example.it",
                "RSSMRA85T10A562S", 20));
    }

    private static void simulaGiornata(FlottaService flotta) {
        sezione("1. Noleggio bici completato");
        try {
            flotta.sbloccaVeicolo("BK-0042", "1");
            double costo = flotta.terminaNoleggio("BK-0042", 10);
            System.out.printf("Noleggio concluso. Costo: %.2f €. Batteria residua: %.1f%%.%n",
                    costo, flotta.getVeicolo("BK-0042").getLivelloBatteria());
        } catch (RossiGoException e) {
            mostraErrore(e);
        }

        sezione("2. Sblocco con credito insufficiente");
        try {
            flotta.sbloccaVeicolo("SC-0107", "2");
        } catch (RossiGoException e) {
            mostraErrore(e);
        }

        sezione("3. Registrazione con codice fiscale errato");
        try {
            flotta.registraUtente(new Utente("4", "Anna Neri", "anna@example.it",
                    "CODICEFISCALEERRATO", 10));
        } catch (RossiGoException e) {
            mostraErrore(e);
        }

        sezione("4. Tentativo di sbloccare una bici già in uso");
        try {
            flotta.sbloccaVeicolo("BK-0043", "1");
            flotta.sbloccaVeicolo("BK-0043", "3");
        } catch (RossiGoException e) {
            mostraErrore(e);
        }

        sezione("5. Mezzo con batteria scarica");
        try {
            flotta.sbloccaVeicolo("SC-0108", "1");
        } catch (RossiGoException e) {
            mostraErrore(e);
        }

        sezione("6. Utente non registrato");
        try {
            flotta.sbloccaVeicolo("SC-0107", "999");
        } catch (RossiGoException e) {
            mostraErrore(e);
        }

        sezione("7. Termine del noleggio e messa in ricarica");
        try {
            if (flotta.getVeicolo("BK-0043").getStato() == StatoVeicolo.IN_USO) {
                flotta.terminaNoleggio("BK-0043", 5);
            }
            flotta.mettiInRicarica("BK-0043");
            System.out.println("La bici BK-0043 è ora in ricarica.");
        } catch (RossiGoException e) {
            mostraErrore(e);
        }
    }

    private static void stampaTitolo() {
        try {
            System.out.println(FigletFont.convertOneLine("RossiGo"));
        } catch (IOException e) {
            System.out.println("RossiGo");
        }
    }

    private static void sezione(String titolo) {
        System.out.println("\n=== " + titolo + " ===");
    }

    private static void mostraErrore(RossiGoException e) {
        System.out.println("Operazione non riuscita: " + e.getMessage());
    }
}
