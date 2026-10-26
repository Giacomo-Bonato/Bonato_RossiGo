package org.example;

public abstract class RossiGoException extends Exception {
    public RossiGoException(String message) {
        super(message);
    }
}
class VeicoloNonDisponibileException extends RossiGoException {
    public VeicoloNonDisponibileException(String message) {
        super(message + "il veicolo è già in uso o in ricarica\n");
    }
}
class BatteriaScaricaException extends RossiGoException {
    public BatteriaScaricaException(String message, Veicolo veicolo) {
        super(message + "\"Batteria al" + veicolo.batteria + "%, scegli un altro mezzo\"\n");
    }
}
class CreditoInsufficienteException extends RossiGoException {
    public CreditoInsufficienteException(String message) {
        super(message + "credito sotto la soglia di sblocco (es. 1 €)\n");
    }
}
class UtenteNonTrovatoException extends RossiGoException {
    public UtenteNonTrovatoException(String message) {
        super(message + "l'id utente non esiste\n");
    }
}
class CodiceFiscaleNonValidoException extends RossiGoException {
    public CodiceFiscaleNonValidoException(String message) {
        super(message + "il codice fiscale fornito in registrazione non è valido\n");
    }
}



