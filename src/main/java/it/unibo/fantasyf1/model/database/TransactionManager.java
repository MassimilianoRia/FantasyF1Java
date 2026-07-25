package it.unibo.fantasyf1.model.database;

import it.unibo.fantasyf1.error.SqlExceptionMapper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * Confine comune per query e workflow JDBC transazionali.
 *
 * <p>Il callback transazionale riceve una sola {@link Connection}, che deve
 * essere passata a tutti i DAO coinvolti nel workflow.</p>
 */
public final class TransactionManager {

    private final ConnectionProvider connectionProvider;

    public TransactionManager(final ConnectionProvider connectionProvider) {
        this.connectionProvider = Objects.requireNonNull(
            connectionProvider,
            "Il provider di connessioni non può essere null"
        );
    }

    /**
     * Esegue una query su una connessione gestita dal manager.
     */
    public <T> T query(final SqlFunction<T> query) {
        Objects.requireNonNull(query, "La query non può essere null");
        try (Connection connection = connectionProvider.open()) {
            return query.apply(connection);
        } catch (SQLException exception) {
            throw SqlExceptionMapper.map(exception);
        }
    }

    /**
     * Alias descrittivo di {@link #query(SqlFunction)} per letture o singole
     * operazioni che non richiedono una transazione esplicita.
     */
    public <T> T withConnection(final SqlFunction<T> operation) {
        return query(operation);
    }

    /**
     * Esegue un workflow atomico usando la stessa connessione.
     */
    public <T> T inTransaction(final SqlFunction<T> operation) {
        Objects.requireNonNull(operation, "L'operazione non può essere null");

        try (Connection connection = connectionProvider.open()) {
            final boolean originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            Throwable operationFailure = null;
            try {
                final T result = operation.apply(connection);
                connection.commit();
                return result;
            } catch (SQLException exception) {
                operationFailure = exception;
                rollback(connection, exception);
                throw SqlExceptionMapper.map(exception);
            } catch (RuntimeException exception) {
                operationFailure = exception;
                rollback(connection, exception);
                throw exception;
            } finally {
                restoreAutoCommit(
                    connection,
                    originalAutoCommit,
                    operationFailure
                );
            }
        } catch (SQLException exception) {
            throw SqlExceptionMapper.map(exception);
        }
    }

    /**
     * Alias di {@link #inTransaction(SqlFunction)}.
     */
    public <T> T executeInTransaction(final SqlFunction<T> operation) {
        return inTransaction(operation);
    }

    public void inTransaction(final SqlConsumer operation) {
        Objects.requireNonNull(operation, "L'operazione non può essere null");
        inTransaction(connection -> {
            operation.accept(connection);
            return null;
        });
    }

    private static void rollback(
        final Connection connection,
        final Throwable originalFailure
    ) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            originalFailure.addSuppressed(rollbackFailure);
        }
    }

    private static void restoreAutoCommit(
        final Connection connection,
        final boolean originalAutoCommit,
        final Throwable originalFailure
    ) {
        try {
            connection.setAutoCommit(originalAutoCommit);
        } catch (SQLException restoreFailure) {
            if (originalFailure != null) {
                originalFailure.addSuppressed(restoreFailure);
            } else {
                throw SqlExceptionMapper.map(restoreFailure);
            }
        }
    }

    @FunctionalInterface
    public interface SqlFunction<T> {

        T apply(Connection connection) throws SQLException;
    }

    @FunctionalInterface
    public interface SqlConsumer {

        void accept(Connection connection) throws SQLException;
    }
}
