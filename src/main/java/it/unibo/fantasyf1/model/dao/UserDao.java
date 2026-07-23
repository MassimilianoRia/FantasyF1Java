package it.unibo.fantasyf1.model.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Accesso JDBC agli account utente. Le password non vengono mai loggate.
 */
public final class UserDao {

    private static final String FIND_BY_USERNAME = """
        SELECT IdUtente, Username, PasswordHash
        FROM UTENTE
        WHERE Username = ?
        """;

    private static final String INSERT = """
        INSERT INTO UTENTE
            (Nome, Cognome, Username, PasswordHash, Email, Telefono)
        VALUES (?, ?, ?, ?, ?, ?)
        """;

    private static final String UPDATE_PASSWORD = """
        UPDATE UTENTE
        SET PasswordHash = ?
        WHERE IdUtente = ?
        """;

    private static final String EXISTS_EMAIL = """
        SELECT 1
        FROM UTENTE
        WHERE Email = ?
        """;

    public Optional<UserRow> findByUsername(
        final Connection connection,
        final String username
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_BY_USERNAME)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserRow(
                    result.getInt("IdUtente"),
                    result.getString("Username"),
                    result.getString("PasswordHash")
                ));
            }
        }
    }

    public int insert(
        final Connection connection,
        final String firstName,
        final String lastName,
        final String username,
        final String passwordHash,
        final String email,
        final String phone
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            INSERT,
            Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setString(3, username);
            statement.setString(4, passwordHash);
            statement.setString(5, email);
            statement.setString(6, phone);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Inserimento utente non riuscito");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Chiave utente non restituita");
                }
                return keys.getInt(1);
            }
        }
    }

    public void updatePasswordHash(
        final Connection connection,
        final int userId,
        final String newHash
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(UPDATE_PASSWORD)) {
            statement.setString(1, newHash);
            statement.setInt(2, userId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Aggiornamento password non riuscito");
            }
        }
    }

    public boolean existsEmail(
        final Connection connection,
        final String email
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(EXISTS_EMAIL)) {
            statement.setString(1, email);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    /**
     * Riga interna necessaria all'autenticazione.
     */
    public record UserRow(int id, String username, String passwordHash) {
    }
}
