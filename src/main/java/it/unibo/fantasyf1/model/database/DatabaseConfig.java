package it.unibo.fantasyf1.model.database;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configurazione necessaria per collegarsi al database MySQL.
 *
 * <p>Tutti i parametri vengono letti dall'unico file di configurazione
 * {@code src/main/resources/database.properties}.</p>
 */
public record DatabaseConfig(String url, String user, String password) {

    private static final String CONFIG_RESOURCE = "/database.properties";
    private static final String URL_KEY = "database.url";
    private static final String USER_KEY = "database.user";
    private static final String PASSWORD_KEY = "database.password";

    public DatabaseConfig {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("L'URL JDBC non puo essere vuoto");
        }
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("L'utente MySQL non puo essere vuoto");
        }
        if (password == null) {
            throw new IllegalArgumentException("La password MySQL non puo essere null");
        }
    }

    /**
     * Carica URL, utente e password dal file di configurazione centralizzato.
     *
     * @return configurazione del database
     * @throws IllegalStateException se il file manca, non e leggibile o e
     *         incompleto
     */
    public static DatabaseConfig load() {
        final Properties properties = new Properties();
        try (
            InputStream input = DatabaseConfig.class.getResourceAsStream(
                CONFIG_RESOURCE
            )
        ) {
            if (input == null) {
                throw new IllegalStateException(
                    "File di configurazione non trovato: " + CONFIG_RESOURCE
                );
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException(
                "Impossibile leggere " + CONFIG_RESOURCE,
                exception
            );
        }

        return new DatabaseConfig(
            requiredProperty(properties, URL_KEY),
            requiredProperty(properties, USER_KEY),
            requiredProperty(properties, PASSWORD_KEY)
        );
    }

    private static String requiredProperty(
        final Properties properties,
        final String key
    ) {
        final String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalStateException(
                "Proprieta mancante in " + CONFIG_RESOURCE + ": " + key
            );
        }
        return value;
    }
}
