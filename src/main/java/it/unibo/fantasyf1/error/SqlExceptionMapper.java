package it.unibo.fantasyf1.error;

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;

/**
 * Traduce gli errori JDBC in categorie applicative senza esporre query,
 * credenziali o dettagli del DBMS all'utente.
 */
public final class SqlExceptionMapper {

    private static final int MYSQL_DUPLICATE_KEY = 1062;

    private SqlExceptionMapper() {
    }

    public static AppException map(final SQLException exception) {
        return map(exception, "completare l'operazione");
    }

    public static AppException map(
        final SQLException exception,
        final String operation
    ) {
        if (exception == null) {
            return new AppException(
                ErrorCode.DATABASE,
                "Impossibile %s a causa di un errore del database."
                    .formatted(safeOperation(operation))
            );
        }

        final String sqlState = exception.getSQLState();
        if (isConnectionFailure(exception, sqlState)) {
            return new AppException(
                ErrorCode.CONNECTION,
                "Impossibile collegarsi al database. "
                    + "Verifica la configurazione e riprova.",
                exception
            );
        }
        if (exception.getErrorCode() == MYSQL_DUPLICATE_KEY) {
            return new AppException(
                ErrorCode.DUPLICATE,
                "Esiste già un elemento con gli stessi dati univoci.",
                exception
            );
        }
        if (startsWith(sqlState, "23")) {
            return new AppException(
                ErrorCode.CONSTRAINT_VIOLATION,
                "I dati indicati non rispettano i vincoli del sistema.",
                exception
            );
        }
        if (startsWith(sqlState, "40")) {
            return new AppException(
                ErrorCode.CONFLICT,
                "L'operazione è entrata in conflitto con un altro "
                    + "aggiornamento. Riprova.",
                exception
            );
        }
        return new AppException(
            ErrorCode.DATABASE,
            "Impossibile %s a causa di un errore del database."
                .formatted(safeOperation(operation)),
            exception
        );
    }

    private static boolean isConnectionFailure(
        final SQLException exception,
        final String sqlState
    ) {
        return exception instanceof SQLTransientConnectionException
            || exception instanceof SQLNonTransientConnectionException
            || exception instanceof SQLRecoverableException
            || startsWith(sqlState, "08");
    }

    private static boolean startsWith(
        final String value,
        final String prefix
    ) {
        return value != null && value.startsWith(prefix);
    }

    private static String safeOperation(final String operation) {
        return operation == null || operation.isBlank()
            ? "completare l'operazione"
            : operation.trim();
    }
}
