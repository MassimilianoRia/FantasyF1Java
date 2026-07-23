package it.unibo.fantasyf1.testutil;

import it.unibo.fantasyf1.model.database.ConnectionProvider;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Database H2 esclusivo di un singolo test.
 */
public final class TestDatabase implements ConnectionProvider {

    private static final String SCHEMA_RESOURCE = "/db/h2-schema.sql";

    private final String url;

    public TestDatabase() {
        final String name = "fantasyf1_"
            + UUID.randomUUID().toString().replace("-", "");
        url = "jdbc:h2:mem:%s;MODE=MySQL;DATABASE_TO_UPPER=TRUE;"
            .formatted(name)
            + "DB_CLOSE_DELAY=-1";
        initializeSchema();
    }

    @Override
    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    public String url() {
        return url;
    }

    public int insert(final String sql, final Object... parameters) {
        try (
            Connection connection = open();
            PreparedStatement statement = connection.prepareStatement(
                sql,
                Statement.RETURN_GENERATED_KEYS
            )
        ) {
            bind(statement, parameters);
            if (statement.executeUpdate() != 1) {
                throw new AssertionError("La fixture non ha inserito una riga");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new AssertionError(
                        "La fixture non ha ricevuto la chiave generata"
                    );
                }
                return keys.getInt(1);
            }
        } catch (SQLException exception) {
            throw new AssertionError("Inserimento fixture non riuscito", exception);
        }
    }

    public int update(final String sql, final Object... parameters) {
        try (
            Connection connection = open();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            bind(statement, parameters);
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new AssertionError("Aggiornamento fixture non riuscito", exception);
        }
    }

    public int queryInt(final String sql, final Object... parameters) {
        try (
            Connection connection = open();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new AssertionError(
                        "La query fixture non ha restituito righe"
                    );
                }
                return result.getInt(1);
            }
        } catch (SQLException exception) {
            throw new AssertionError("Query fixture non riuscita", exception);
        }
    }

    public String queryString(final String sql, final Object... parameters) {
        try (
            Connection connection = open();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            bind(statement, parameters);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new AssertionError(
                        "La query fixture non ha restituito righe"
                    );
                }
                return result.getString(1);
            }
        } catch (SQLException exception) {
            throw new AssertionError("Query fixture non riuscita", exception);
        }
    }

    private void initializeSchema() {
        try (
            InputStream input = TestDatabase.class.getResourceAsStream(
                SCHEMA_RESOURCE
            )
        ) {
            if (input == null) {
                throw new AssertionError(
                    "Risorsa schema H2 non trovata: " + SCHEMA_RESOURCE
                );
            }
            final String script = new String(
                input.readAllBytes(),
                StandardCharsets.UTF_8
            );
            try (
                Connection connection = open();
                Statement statement = connection.createStatement()
            ) {
                for (String sql : splitStatements(script)) {
                    statement.execute(sql);
                }
            }
        } catch (IOException | SQLException exception) {
            throw new AssertionError(
                "Inizializzazione dello schema H2 non riuscita",
                exception
            );
        }
    }

    private static List<String> splitStatements(final String script) {
        final String withoutLineComments = script.lines()
            .filter(line -> !line.stripLeading().startsWith("--"))
            .reduce("", (left, right) -> left + "\n" + right);
        return Arrays.stream(withoutLineComments.split(";"))
            .map(String::trim)
            .filter(statement -> !statement.isEmpty())
            .toList();
    }

    private static void bind(
        final PreparedStatement statement,
        final Object[] parameters
    ) throws SQLException {
        Objects.requireNonNull(parameters);
        for (int index = 0; index < parameters.length; index++) {
            statement.setObject(index + 1, parameters[index]);
        }
    }
}
