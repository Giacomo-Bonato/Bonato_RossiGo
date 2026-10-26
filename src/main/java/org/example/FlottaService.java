package org.example;
import it.kamaladafrica.codicefiscale.CodiceFiscale;
import java.util.List;

public class FlottaService {
    static List<Veicolo> veicoli;
    static List<Utente> utenti;
    Noleggio noleggio;

    public FlottaService(List<Veicolo> veicoli, List<Utente> utenti, Noleggio noleggio) {
        FlottaService.veicoli = veicoli;
        FlottaService.utenti = utenti;
        this.noleggio = noleggio;
    }

    public static void registraVeicolo(Veicolo veicolo1){
        veicoli.add(veicolo1);
    }
    public static void registraUtente (Utente utente1) {
        CodiceFiscale.isFormatValid(utente1.codicefiscale);
        utenti.add(utente1);
    }
}
