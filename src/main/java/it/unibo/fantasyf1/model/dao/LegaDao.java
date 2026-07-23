package it.unibo.fantasyf1.model.dao;

import it.unibo.fantasyf1.model.JoinedLeague;
import it.unibo.fantasyf1.model.LegaDisponibile;
import it.unibo.fantasyf1.model.StandingRow;
import it.unibo.fantasyf1.model.database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        FROM LEGA L
        JOIN UTENTE U ON U.IdUtente = L.IdUtente
        WHERE L.IdEdizione = ?
        ORDER BY L.Nome
        """;

    private static final String INSERT_LEAGUE = """
        INSERT INTO LEGA (Nome, IdUtente, IdEdizione)
        VALUES (?, ?, ?)
        """;

    private static final String LOCK_LEAGUE = """
        SELECT IdLega, Nome, IdUtente, IdEdizione
        FROM LEGA
        WHERE IdLega = ?
        FOR UPDATE
        """;

    private static final String FIND_LEAGUE = """
        SELECT IdLega, Nome, IdUtente, IdEdizione
        FROM LEGA
        WHERE IdLega = ?
        """;

    private static final String FIND_OWNER_PARTICIPATION = """
        SELECT TF.IdTeam
        FROM PARTECIPAZIONE_TEAM PT
        JOIN TEAM_FANTASY TF ON TF.IdTeam = PT.IdTeam
        WHERE PT.IdLega = ?
          AND TF.IdUtente = ?
        FOR UPDATE
        """;

    private static final String INSERT_PARTICIPATION = """
        INSERT INTO PARTECIPAZIONE_TEAM (IdLega, IdTeam)
        VALUES (?, ?)
        """;

    private static final String FIND_JOINED = """
        SELECT
            L.IdLega,
            L.Nome AS NomeLega,
            TF.IdTeam,
            TF.Nome AS NomeTeam
        FROM TEAM_FANTASY TF
        JOIN PARTECIPAZIONE_TEAM PT ON PT.IdTeam = TF.IdTeam
        JOIN LEGA L ON L.IdLega = PT.IdLega
        WHERE TF.IdUtente = ?
          AND TF.IdEdizione = ?
          AND L.IdEdizione = TF.IdEdizione
        ORDER BY L.Nome, TF.Nome
        """;

    private static final String FIND_OWNED = """
        SELECT
            L.IdLega,
            L.Nome,
            U.IdUtente AS IdAmministratore,
            U.Username AS Amministratore
        FROM LEGA L
        JOIN UTENTE U ON U.IdUtente = L.IdUtente
        WHERE L.IdUtente = ?
          AND L.IdEdizione = ?
        ORDER BY L.Nome
        """;

    private static final String FIND_STANDINGS = """
        SELECT
            TF.IdTeam,
            TF.Nome AS NomeTeam,
            U.Username AS Proprietario,
            TF.PunteggioTotale
        FROM PARTECIPAZIONE_TEAM PT
        JOIN TEAM_FANTASY TF ON TF.IdTeam = PT.IdTeam
        JOIN UTENTE U ON U.IdUtente = TF.IdUtente
        WHERE PT.IdLega = ?
        ORDER BY TF.PunteggioTotale DESC, TF.Nome
        """;

    /**
     * Metodo mantenuto per compatibilità con il prototipo iniziale.
     */
    public List<LegaDisponibile> findByEdition(final int editionId)
        throws SQLException {
        try (Connection connection = DatabaseConnection.open()) {
            return findByEdition(connection, editionId);
        }
    }

    public List<LegaDisponibile> findByEdition(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        final List<LegaDisponibile> leagues = new ArrayList<>();
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_BY_EDITION)) {
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

    public int insert(
        final Connection connection,
        final String name,
        final int administratorId,
        final int editionId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            INSERT_LEAGUE,
            Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, name);
            statement.setInt(2, administratorId);
            statement.setInt(3, editionId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Inserimento lega non riuscito");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Chiave lega non restituita");
                }
                return keys.getInt(1);
            }
        }
    }

    public Optional<LeagueRow> lockLeague(
        final Connection connection,
        final int leagueId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(LOCK_LEAGUE)) {
            statement.setInt(1, leagueId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new LeagueRow(
                    result.getInt("IdLega"),
                    result.getString("Nome"),
                    result.getInt("IdUtente"),
                    result.getInt("IdEdizione")
                ));
            }
        }
    }

    public Optional<LeagueRow> findLeague(
        final Connection connection,
        final int leagueId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_LEAGUE)) {
            statement.setInt(1, leagueId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new LeagueRow(
                    result.getInt("IdLega"),
                    result.getString("Nome"),
                    result.getInt("IdUtente"),
                    result.getInt("IdEdizione")
                ));
            }
        }
    }

    public Optional<Integer> findParticipatingTeamForOwner(
        final Connection connection,
        final int leagueId,
        final int ownerId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_OWNER_PARTICIPATION)) {
            statement.setInt(1, leagueId);
            statement.setInt(2, ownerId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                    ? Optional.of(result.getInt("IdTeam"))
                    : Optional.empty();
            }
        }
    }

    public void insertParticipation(
        final Connection connection,
        final int leagueId,
        final int teamId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(INSERT_PARTICIPATION)) {
            statement.setInt(1, leagueId);
            statement.setInt(2, teamId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Iscrizione alla lega non riuscita");
            }
        }
    }

    public List<JoinedLeague> findJoinedByOwner(
        final Connection connection,
        final int ownerId,
        final int editionId
    ) throws SQLException {
        final List<JoinedLeague> leagues = new ArrayList<>();
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_JOINED)) {
            statement.setInt(1, ownerId);
            statement.setInt(2, editionId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    leagues.add(new JoinedLeague(
                        result.getInt("IdLega"),
                        result.getString("NomeLega"),
                        result.getInt("IdTeam"),
                        result.getString("NomeTeam")
                    ));
                }
            }
        }
        return List.copyOf(leagues);
    }

    public List<LegaDisponibile> findOwnedByAdministrator(
        final Connection connection,
        final int administratorId,
        final int editionId
    ) throws SQLException {
        final List<LegaDisponibile> leagues = new ArrayList<>();
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_OWNED)) {
            statement.setInt(1, administratorId);
            statement.setInt(2, editionId);
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

    public List<StandingRow> findStandings(
        final Connection connection,
        final int leagueId
    ) throws SQLException {
        final List<StandingRow> standings = new ArrayList<>();
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_STANDINGS)) {
            statement.setInt(1, leagueId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    standings.add(new StandingRow(
                        result.getInt("IdTeam"),
                        result.getString("NomeTeam"),
                        result.getString("Proprietario"),
                        result.getInt("PunteggioTotale")
                    ));
                }
            }
        }
        return List.copyOf(standings);
    }

    /**
     * Dati bloccati durante U6.
     */
    public record LeagueRow(
        int id,
        String name,
        int administratorId,
        int editionId
    ) {
    }
}
