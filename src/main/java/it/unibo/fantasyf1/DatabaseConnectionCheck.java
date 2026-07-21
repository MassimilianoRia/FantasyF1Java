package it.unibo.fantasyf1;

import it.unibo.fantasyf1.model.database.DatabaseConfig;
import it.unibo.fantasyf1.model.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Controllo manuale della configurazione JDBC, eseguibile tramite Gradle.
 */
public final class DatabaseConnectionCheck {

    private DatabaseConnectionCheck() {
    }

    public static void main(final String[] args) {
        final DatabaseConfig config = DatabaseConfig.load();

        try (
            Connection connection = DatabaseConnection.open();
            Statement statement = connection.createStatement();
            ResultSet result = statement.executeQuery("SELECT DATABASE()")
        ) {
            result.next();
            System.out.printf(
                "Connessione MySQL riuscita: %s (utente: %s, database: %s)%n",
                config.url(),
                config.user(),
                result.getString(1)
            );
        } catch (SQLException exception) {
            System.err.printf(
                "Connessione MySQL fallita verso %s con l'utente %s.%n%s%n",
                config.url(),
                config.user(),
                exception.getMessage()
            );
            System.exit(1);
        }
    }
}
