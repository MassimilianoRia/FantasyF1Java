package it.unibo.fantasyf1.model.dao;

import it.unibo.fantasyf1.model.LegaDisponibile;
import it.unibo.fantasyf1.model.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accesso JDBC alle leghe fantasy.
 */
public final class LegaDao {

    private static final String FIND_BY_EDITION = """
        SELECT
            L.IdLega,
            L.Nome,
            U.IdUtente AS IdAmministratore,
            U.Username AS Amministratore
        FROM LEGA AS L
        JOIN UTENTE AS U
            ON U.IdUtente = L.IdUtente
        WHERE L.IdEdizione = ?
        ORDER BY L.Nome
        """;

    public List<LegaDisponibile> findByEdition(final int editionId)
        throws SQLException {
        final List<LegaDisponibile> leagues = new ArrayList<>();

        try (
            Connection connection = DatabaseConnection.open();
            PreparedStatement statement =
                connection.prepareStatement(FIND_BY_EDITION)
        ) {
            statement.setInt(1, editionId);

            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    leagues.add(new LegaDisponibile(
                        result.getInt("IdLega"),
                        result.getString("Nome"),
                        result.getInt("IdAmministratore"),
                        result.getString("Amministratore")
                    ));
                }
            }
        }

        return List.copyOf(leagues);
    }
}
