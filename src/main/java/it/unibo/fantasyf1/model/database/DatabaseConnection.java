package it.unibo.fantasyf1.model.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Punto di accesso centralizzato alle connessioni JDBC dell'applicazione.
 */
public final class DatabaseConnection {

    private static final int LOGIN_TIMEOUT_SECONDS = 5;

    private DatabaseConnection() {
    }

    /**
     * Apre una nuova connessione. Chi invoca questo metodo deve chiuderla,
     * preferibilmente tramite try-with-resources.
     *
     * @return una nuova connessione a fantasy_f1
     * @throws SQLException se MySQL non e raggiungibile o rifiuta l'accesso
     */
    public static Connection open() throws SQLException {
        final DatabaseConfig config = DatabaseConfig.load();
        final Properties properties = new Properties();
        properties.setProperty("user", config.user());
        properties.setProperty("password", config.password());

        DriverManager.setLoginTimeout(LOGIN_TIMEOUT_SECONDS);
        return DriverManager.getConnection(config.url(), properties);
    }

    /**
     * Restituisce il provider predefinito, iniettabile nei service e nei
     * componenti transazionali.
     *
     * @return provider delle connessioni configurate
     */
    public static ConnectionProvider provider() {
        return DatabaseConnection::open;
    }
}
