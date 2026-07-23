package it.unibo.fantasyf1.model.dao;

import it.unibo.fantasyf1.model.DriverOption;
import it.unibo.fantasyf1.model.TeamDriver;
import it.unibo.fantasyf1.model.TeamSummary;
import it.unibo.fantasyf1.model.WeekendScoreRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Query relative ai team fantasy e alla loro composizione.
 */
public final class TeamDao {

    private static final String FIND_DRIVERS = """
        SELECT
            PI.IdPilota,
            PI.IdEdizione,
            P.Nome,
            P.Cognome,
            PI.SiglaGara,
            PI.NumeroInGara,
            SI.NomeIscrizione
        FROM PILOTA_ISCRITTO PI
        JOIN PILOTA P ON P.IdPilota = PI.IdPilota
        JOIN SCUDERIA_ISCRITTA SI
          ON SI.IdEdizione = PI.IdEdizione
         AND SI.IdScuderia = PI.IdScuderia
        WHERE PI.IdEdizione = ?
        ORDER BY P.Cognome, P.Nome
        """;

    private static final String INSERT_TEAM = """
        INSERT INTO TEAM_FANTASY
            (Nome, PunteggioTotale, IdUtente, IdEdizione)
        VALUES (?, 0, ?, ?)
        """;

    private static final String INSERT_COMPONENT = """
        INSERT INTO COMPOSIZIONE_TEAM (IdTeam, IdEdizione, IdPilota)
        VALUES (?, ?, ?)
        """;

    private static final String FIND_OWNED = """
        SELECT
            TF.IdTeam,
            TF.Nome AS NomeTeam,
            TF.PunteggioTotale,
            P.IdPilota,
            P.Nome,
            P.Cognome,
            PI.SiglaGara,
            PI.NumeroInGara
        FROM TEAM_FANTASY TF
        LEFT JOIN COMPOSIZIONE_TEAM CT
          ON CT.IdTeam = TF.IdTeam
         AND CT.IdEdizione = TF.IdEdizione
        LEFT JOIN PILOTA_ISCRITTO PI
          ON PI.IdEdizione = CT.IdEdizione
         AND PI.IdPilota = CT.IdPilota
        LEFT JOIN PILOTA P ON P.IdPilota = PI.IdPilota
        WHERE TF.IdUtente = ?
          AND TF.IdEdizione = ?
        ORDER BY TF.Nome, P.Cognome, P.Nome
        """;

    private static final String FIND_TEAM = """
        SELECT IdTeam, IdUtente, IdEdizione, Nome, PunteggioTotale
        FROM TEAM_FANTASY
        WHERE IdTeam = ?
        """;

    private static final String FIND_TEAM_FOR_UPDATE = FIND_TEAM
        + " FOR UPDATE";

    private static final String WEEKEND_BREAKDOWN = """
        SELECT
            P.IdPilota,
            P.Nome,
            P.Cognome,
            PI.SiglaGara,
            PW.PunteggioFantasy
        FROM TEAM_FANTASY TF
        JOIN RISULTATO_TEAM RT
          ON RT.IdTeam = TF.IdTeam
         AND RT.IdEdizione = TF.IdEdizione
        JOIN COMPOSIZIONE_TEAM CT
          ON CT.IdTeam = TF.IdTeam
         AND CT.IdEdizione = TF.IdEdizione
        JOIN PILOTA_ISCRITTO PI
          ON PI.IdEdizione = CT.IdEdizione
         AND PI.IdPilota = CT.IdPilota
        JOIN PILOTA P ON P.IdPilota = PI.IdPilota
        JOIN PRESTAZIONE_WEEKEND PW
          ON PW.IdEdizione = CT.IdEdizione
         AND PW.IdPilota = CT.IdPilota
         AND PW.IdGranPremio = RT.IdGranPremio
        WHERE TF.IdTeam = ?
          AND TF.IdUtente = ?
          AND TF.IdEdizione = ?
          AND RT.IdGranPremio = ?
          AND PW.PunteggioFantasy IS NOT NULL
        ORDER BY P.Cognome, P.Nome
        """;

    public List<DriverOption> findDriversByEdition(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        final List<DriverOption> drivers = new ArrayList<>();
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_DRIVERS)) {
            statement.setInt(1, editionId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    drivers.add(new DriverOption(
                        result.getInt("IdPilota"),
                        result.getInt("IdEdizione"),
                        result.getString("Nome"),
                        result.getString("Cognome"),
                        result.getString("SiglaGara"),
                        result.getInt("NumeroInGara"),
                        result.getString("NomeIscrizione")
                    ));
                }
            }
        }
        return List.copyOf(drivers);
    }

    public int insertTeam(
        final Connection connection,
        final String name,
        final int ownerId,
        final int editionId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            INSERT_TEAM,
            Statement.RETURN_GENERATED_KEYS
        )) {
            statement.setString(1, name);
            statement.setInt(2, ownerId);
            statement.setInt(3, editionId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Inserimento team non riuscito");
            }
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Chiave team non restituita");
                }
                return keys.getInt(1);
            }
        }
    }

    public void insertComponents(
        final Connection connection,
        final int teamId,
        final int editionId,
        final List<Integer> driverIds
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(INSERT_COMPONENT)) {
            for (Integer driverId : driverIds) {
                statement.setInt(1, teamId);
                statement.setInt(2, editionId);
                statement.setInt(3, driverId);
                statement.addBatch();
            }
            final int[] counts = statement.executeBatch();
            if (counts.length != driverIds.size()) {
                throw new SQLException("Composizione team incompleta");
            }
            for (int count : counts) {
                if (count != 1 && count != Statement.SUCCESS_NO_INFO) {
                    throw new SQLException("Composizione team incompleta");
                }
            }
        }
    }

    public List<TeamSummary> findOwnedWithRoster(
        final Connection connection,
        final int ownerId,
        final int editionId
    ) throws SQLException {
        final Map<Integer, TeamAccumulator> teams = new LinkedHashMap<>();
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_OWNED)) {
            statement.setInt(1, ownerId);
            statement.setInt(2, editionId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final int teamId = result.getInt("IdTeam");
                    final TeamAccumulator team = teams.computeIfAbsent(
                        teamId,
                        ignored -> new TeamAccumulator(
                            teamId,
                            readString(result, "NomeTeam"),
                            readInt(result, "PunteggioTotale")
                        )
                    );
                    final int driverId = result.getInt("IdPilota");
                    if (!result.wasNull()) {
                        team.drivers.add(new TeamDriver(
                            driverId,
                            result.getString("Nome"),
                            result.getString("Cognome"),
                            result.getString("SiglaGara"),
                            result.getInt("NumeroInGara")
                        ));
                    }
                }
            }
        }
        return teams.values().stream()
            .map(TeamAccumulator::toSummary)
            .toList();
    }

    public Optional<TeamRow> findTeam(
        final Connection connection,
        final int teamId
    ) throws SQLException {
        return findTeam(connection, teamId, false);
    }

    public Optional<TeamRow> lockTeam(
        final Connection connection,
        final int teamId
    ) throws SQLException {
        return findTeam(connection, teamId, true);
    }

    private Optional<TeamRow> findTeam(
        final Connection connection,
        final int teamId,
        final boolean lock
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            lock ? FIND_TEAM_FOR_UPDATE : FIND_TEAM
        )) {
            statement.setInt(1, teamId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(new TeamRow(
                    result.getInt("IdTeam"),
                    result.getInt("IdUtente"),
                    result.getInt("IdEdizione"),
                    result.getString("Nome"),
                    result.getInt("PunteggioTotale")
                ));
            }
        }
    }

    public List<WeekendScoreRow> findWeekendBreakdown(
        final Connection connection,
        final int teamId,
        final int ownerId,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        final List<WeekendScoreRow> scores = new ArrayList<>();
        try (PreparedStatement statement =
                 connection.prepareStatement(WEEKEND_BREAKDOWN)) {
            statement.setInt(1, teamId);
            statement.setInt(2, ownerId);
            statement.setInt(3, editionId);
            statement.setInt(4, grandPrixId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    scores.add(new WeekendScoreRow(
                        result.getInt("IdPilota"),
                        result.getString("Nome"),
                        result.getString("Cognome"),
                        result.getString("SiglaGara"),
                        nullableInt(result, "PunteggioFantasy")
                    ));
                }
            }
        }
        return List.copyOf(scores);
    }

    private static Integer nullableInt(
        final ResultSet result,
        final String column
    ) throws SQLException {
        final int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }

    private static String readString(
        final ResultSet result,
        final String column
    ) {
        try {
            return result.getString(column);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static int readInt(
        final ResultSet result,
        final String column
    ) {
        try {
            return result.getInt(column);
        } catch (SQLException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public record TeamRow(
        int id,
        int ownerId,
        int editionId,
        String name,
        int totalPoints
    ) {
    }

    private static final class TeamAccumulator {
        private final int id;
        private final String name;
        private final int total;
        private final List<TeamDriver> drivers = new ArrayList<>();

        TeamAccumulator(final int id, final String name, final int total) {
            this.id = id;
            this.name = name;
            this.total = total;
        }

        TeamSummary toSummary() {
            return new TeamSummary(id, name, total, drivers);
        }
    }
}
