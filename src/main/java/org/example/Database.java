package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Database implements AutoCloseable {
    private static final String JDBC_URL = "jdbc:sqlite:rossigo.db";
    private static Database instance;
    private final Connection connection;

    public Database() throws SQLException {
        connection = DriverManager.getConnection(JDBC_URL);
        inizializzaSchema();
    }

    public static synchronized Database getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new Database();
        }
        return instance;
    }

    private void inizializzaSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS UTENTI (
                        ID INTEGER PRIMARY KEY,
                        NOME TEXT NOT NULL,
                        EMAIL TEXT NOT NULL,
                        CODICE_FISCALE TEXT NOT NULL,
                        CREDITO REAL NOT NULL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS Veicoli (
                        Codice TEXT PRIMARY KEY,
                        TIPO TEXT NOT NULL,
                        LIVELLO_BATTERIA REAL,
                        STATO TEXT,
                        NUMERO_MARCE INTEGER,
                        VELOCITA_MASSIMA REAL
                    )
                    """);
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS NOLEGGI (
                        ID INTEGER PRIMARY KEY AUTOINCREMENT,
                        ID_UTENTE TEXT NOT NULL,
                        CODICE_VEICOLO TEXT NOT NULL,
                        ORA_INIZIO TEXT NOT NULL,
                        ORA_FINE TEXT NOT NULL DEFAULT '',
                        COSTO REAL NOT NULL DEFAULT 0,
                        MINUTI_USATI INTEGER NOT NULL DEFAULT 0
                    )
                    """);
        }
        aggiungiColonnaSeManca("COSTO", "REAL NOT NULL DEFAULT 0");
        aggiungiColonnaSeManca("MINUTI_USATI", "INTEGER NOT NULL DEFAULT 0");
    }

    private void aggiungiColonnaSeManca(String nome, String definizione) throws SQLException {
        boolean esiste = false;
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(NOLEGGI)")) {
            while (columns.next()) {
                if (nome.equalsIgnoreCase(columns.getString("name"))) {
                    esiste = true;
                    break;
                }
            }
        }
        if (!esiste) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("ALTER TABLE NOLEGGI ADD COLUMN " + nome + " " + definizione);
            }
        }
    }

    public List<Utente> caricaUtenti() throws SQLException {
        List<Utente> utenti = new ArrayList<>();
        String sql = "SELECT ID, NOME, EMAIL, CODICE_FISCALE, CREDITO FROM UTENTI";
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(sql)) {
            while (results.next()) {
                utenti.add(new Utente(
                        Integer.toString(results.getInt("ID")),
                        results.getString("NOME"),
                        results.getString("EMAIL"),
                        results.getString("CODICE_FISCALE"),
                        results.getDouble("CREDITO")));
            }
        }
        return utenti;
    }

    public List<Veicolo> caricaVeicoli() throws SQLException {
        List<Veicolo> veicoli = new ArrayList<>();
        String sql = """
                SELECT Codice, TIPO, LIVELLO_BATTERIA, STATO, NUMERO_MARCE, VELOCITA_MASSIMA
                FROM Veicoli
                """;
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(sql)) {
            while (results.next()) {
                String tipo = results.getString("TIPO");
                String codice = results.getString("Codice");
                double batteria = results.getDouble("LIVELLO_BATTERIA");
                Veicolo veicolo;
                if ("BICI".equalsIgnoreCase(tipo)) {
                    veicolo = new Bici(codice, batteria, results.getInt("NUMERO_MARCE"));
                } else if ("MONOPATTINO".equalsIgnoreCase(tipo)) {
                    veicolo = new Monopattino(codice, batteria, results.getDouble("VELOCITA_MASSIMA"));
                } else {
                    throw new SQLException("Tipo di veicolo non riconosciuto nel database: " + tipo);
                }
                veicolo.setStato(StatoVeicolo.valueOf(results.getString("STATO")));
                veicoli.add(veicolo);
            }
        }
        return veicoli;
    }

    public List<Noleggio> caricaNoleggi(List<Utente> utenti, List<Veicolo> veicoli) throws SQLException {
        List<Noleggio> noleggi = new ArrayList<>();
        String sql = """
                SELECT ID, ID_UTENTE, CODICE_VEICOLO, ORA_INIZIO, ORA_FINE, COSTO, MINUTI_USATI
                FROM NOLEGGI ORDER BY ID
                """;
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(sql)) {
            while (results.next()) {
                 String idUtente = results.getString("ID_UTENTE");
                 String codiceVeicolo = results.getString("CODICE_VEICOLO");
                 Utente utente = utenti.stream()
                         .filter(value -> value.getId().equals(idUtente))
                         .findFirst()
                         .orElseThrow(() -> new SQLException("Utente di un noleggio non trovato"));
                 Veicolo veicolo = veicoli.stream()
                         .filter(value -> value.getCodice().equals(codiceVeicolo))
                         .findFirst()
                         .orElseThrow(() -> new SQLException("Veicolo di un noleggio non trovato"));
                String fine = results.getString("ORA_FINE");
                noleggi.add(new Noleggio(
                        utente,
                        veicolo,
                        LocalDateTime.parse(results.getString("ORA_INIZIO")),
                        fine == null || fine.isBlank() ? null : LocalDateTime.parse(fine),
                        results.getInt("MINUTI_USATI"),
                        results.getDouble("COSTO"),
                        results.getLong("ID")));
            }
        }
        return noleggi;
    }

    public boolean archivioVuoto() throws SQLException {
        String sql = """
                SELECT NOT EXISTS (SELECT 1 FROM UTENTI)
                   AND NOT EXISTS (SELECT 1 FROM Veicoli)
                """;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return result.next() && result.getBoolean(1);
        }
    }

    public void salvaUtente(Utente utente) throws SQLException {
        int id = idNumerico(utente.getId());
        String sql = """
                INSERT INTO UTENTI (ID, NOME, EMAIL, CODICE_FISCALE, CREDITO)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT(ID) DO UPDATE SET NOME=excluded.NOME, EMAIL=excluded.EMAIL,
                    CODICE_FISCALE=excluded.CODICE_FISCALE, CREDITO=excluded.CREDITO
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setString(2, utente.getNome());
            statement.setString(3, utente.getEmail());
            statement.setString(4, utente.getCodiceFiscale());
            statement.setDouble(5, utente.getCredito());
            statement.executeUpdate();
        }
    }

    public void salvaVeicolo(Veicolo veicolo) throws SQLException {
        String tipo;
        Integer marce = null;
        Double velocita = null;
        if (veicolo instanceof Bici bici) {
            tipo = "BICI";
            marce = bici.getNumeroMarce();
        } else if (veicolo instanceof Monopattino monopattino) {
            tipo = "MONOPATTINO";
            velocita = monopattino.getVelocitaMassima();
        } else {
            throw new SQLException("Sottotipo di veicolo non supportato: " + veicolo.getClass().getName());
        }
        String sql = """
                INSERT INTO Veicoli (Codice, TIPO, LIVELLO_BATTERIA, STATO, NUMERO_MARCE, VELOCITA_MASSIMA)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT(Codice) DO UPDATE SET TIPO=excluded.TIPO,
                    LIVELLO_BATTERIA=excluded.LIVELLO_BATTERIA, STATO=excluded.STATO,
                    NUMERO_MARCE=excluded.NUMERO_MARCE, VELOCITA_MASSIMA=excluded.VELOCITA_MASSIMA
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, veicolo.getCodice());
            statement.setString(2, tipo);
            statement.setDouble(3, veicolo.getLivelloBatteria());
            statement.setString(4, veicolo.getStato().name());
            if (marce == null) {
                statement.setNull(5, java.sql.Types.INTEGER);
            } else {
                statement.setInt(5, marce);
            }
            if (velocita == null) {
                statement.setNull(6, java.sql.Types.REAL);
            } else {
                statement.setDouble(6, velocita);
            }
            statement.executeUpdate();
        }
    }

    public long avviaNoleggio(Noleggio noleggio) throws SQLException {
        return inTransazione(() -> {
            try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE Veicoli SET STATO=? WHERE Codice=?")) {
                update.setString(1, StatoVeicolo.IN_USO.name());
                update.setString(2, noleggio.getVeicolo().getCodice());
                if (update.executeUpdate() != 1) {
                    throw new SQLException("Veicolo non trovato durante l'avvio del noleggio");
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO NOLEGGI (ID_UTENTE, CODICE_VEICOLO, ORA_INIZIO, ORA_FINE)
                    VALUES (?, ?, ?, '')
                    """)) {
                insert.setString(1, noleggio.getUtente().getId());
                insert.setString(2, noleggio.getVeicolo().getCodice());
                insert.setString(3, noleggio.getOrarioInizio().toString());
                insert.executeUpdate();
            }
            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT last_insert_rowid()")) {
                if (!result.next()) {
                    throw new SQLException("Impossibile recuperare l'id del noleggio");
                }
                return result.getLong(1);
            }
        });
    }

    public void terminaNoleggio(Noleggio noleggio, int minuti, double costo,
                                double nuovoCredito, double nuovaBatteria,
                                LocalDateTime oraFine) throws SQLException {
        inTransazione(() -> {
            try (PreparedStatement updateUtente = connection.prepareStatement(
                    "UPDATE UTENTI SET CREDITO=? WHERE ID=?")) {
                updateUtente.setDouble(1, nuovoCredito);
                updateUtente.setInt(2, idNumerico(noleggio.getUtente().getId()));
                if (updateUtente.executeUpdate() != 1) {
                    throw new SQLException("Utente non trovato durante l'addebito");
                }
            }
            try (PreparedStatement updateVeicolo = connection.prepareStatement(
                    "UPDATE Veicoli SET LIVELLO_BATTERIA=?, STATO=? WHERE Codice=?")) {
                updateVeicolo.setDouble(1, nuovaBatteria);
                updateVeicolo.setString(2, StatoVeicolo.LIBERO.name());
                updateVeicolo.setString(3, noleggio.getVeicolo().getCodice());
                if (updateVeicolo.executeUpdate() != 1) {
                    throw new SQLException("Veicolo non trovato durante la chiusura del noleggio");
                }
            }
            try (PreparedStatement updateNoleggio = connection.prepareStatement("""
                    UPDATE NOLEGGI SET ORA_FINE=?, COSTO=?, MINUTI_USATI=?
                    WHERE ID=? AND ORA_FINE=''
                    """)) {
                updateNoleggio.setString(1, oraFine.toString());
                updateNoleggio.setDouble(2, costo);
                updateNoleggio.setInt(3, minuti);
                updateNoleggio.setLong(4, noleggio.getIdDatabase());
                if (updateNoleggio.executeUpdate() != 1) {
                    throw new SQLException("Noleggio attivo non trovato durante la chiusura");
                }
            }
            return null;
        });
    }

    public void mettiInRicarica(Veicolo veicolo) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Veicoli SET STATO=? WHERE Codice=?")) {
            statement.setString(1, StatoVeicolo.IN_RICARICA.name());
            statement.setString(2, veicolo.getCodice());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Veicolo non trovato durante la messa in ricarica");
            }
        }
    }

    private static int idNumerico(String id) throws SQLException {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new SQLException("L'id utente deve essere numerico per il database esistente: " + id, e);
        }
    }

    private <T> T inTransazione(SqlOperation<T> operation) throws SQLException {
        boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            T value = operation.run();
            connection.commit();
            return value;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        if (!connection.isClosed()) {
            connection.close();
        }
        if (instance == this) {
            instance = null;
        }
    }

    @FunctionalInterface
    private interface SqlOperation<T> {
        T run() throws SQLException;
    }
}
