package it.unibo.fantasyf1.model.dao;

import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.model.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Accesso JDBC alle edizioni del campionato.
 */
public final class EdizioneDao {

    private static final String FIND_ALL = """
        SELECT IdEdizione, NumeroEdizione, Anno
        FROM EDIZIONE
        ORDER BY Anno DESC
        """;

    public List<Edizione> findAll() throws SQLException {
        final List<Edizione> editions = new ArrayList<>();

        try (
            Connection connection = DatabaseConnection.open();
            PreparedStatement statement = connection.prepareStatement(FIND_ALL);
            ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                editions.add(new Edizione(
                    result.getInt("IdEdizione"),
                    result.getInt("NumeroEdizione"),
                    result.getInt("Anno")
                ));
            }
        }

        return List.copyOf(editions);
    }
}
