package it.unibo.fantasyf1.model.database;

/**
 * Configurazione necessaria per collegarsi al database MySQL.
 *
 * <p>URL e utente possono essere impostati tramite variabili d'ambiente oppure
 * tramite proprieta JVM. La password del database locale e inclusa nella
 * configurazione del progetto.</p>
 */
public record DatabaseConfig(String url, String user, String password) {

    public static final String DEFAULT_URL =
        "jdbc:mysql://localhost:3306/fantasy_f1";
    private static final String DEFAULT_PASSWORD = "Stellarium!23";

    private static final String URL_PROPERTY = "fantasyf1.db.url";
    private static final String USER_PROPERTY = "fantasyf1.db.user";

    private static final String URL_ENV = "FANTASY_F1_DB_URL";
    private static final String USER_ENV = "FANTASY_F1_DB_USER";

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
     * Carica la configurazione. I valori predefiniti sono l'URL locale del
     * progetto, l'utente {@code root} e la password del database di sviluppo.
     *
     * @return configurazione del database
     */
    public static DatabaseConfig load() {
        return new DatabaseConfig(
            value(URL_PROPERTY, URL_ENV, DEFAULT_URL),
            value(USER_PROPERTY, USER_ENV, "root"),
            DEFAULT_PASSWORD
        );
    }

    private static String value(
        final String propertyName,
        final String environmentName,
        final String defaultValue
    ) {
        final String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null) {
            return propertyValue;
        }

        final String environmentValue = System.getenv(environmentName);
        return environmentValue != null ? environmentValue : defaultValue;
    }
}
