package it.unibo.fantasyf1.model.database;

/**
 * Configurazione necessaria per collegarsi al database MySQL.
 *
 * <p>I valori possono essere impostati tramite variabili d'ambiente oppure
 * tramite proprieta JVM. Le proprieta JVM hanno la precedenza.</p>
 */
public record DatabaseConfig(String url, String user, String password) {

    public static final String DEFAULT_URL =
        "jdbc:mysql://localhost:3306/fantasy_f1";

    private static final String URL_PROPERTY = "fantasyf1.db.url";
    private static final String USER_PROPERTY = "fantasyf1.db.user";
    private static final String PASSWORD_PROPERTY = "fantasyf1.db.password";

    private static final String URL_ENV = "FANTASY_F1_DB_URL";
    private static final String USER_ENV = "FANTASY_F1_DB_USER";
    private static final String PASSWORD_ENV = "FANTASY_F1_DB_PASSWORD";

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
     * progetto, l'utente {@code root} e una password vuota.
     *
     * @return configurazione del database
     */
    public static DatabaseConfig load() {
        return new DatabaseConfig(
            value(URL_PROPERTY, URL_ENV, DEFAULT_URL),
            value(USER_PROPERTY, USER_ENV, "root"),
            value(PASSWORD_PROPERTY, PASSWORD_ENV, "")
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
