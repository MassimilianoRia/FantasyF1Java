package it.unibo.fantasyf1.model.dao;

import it.unibo.fantasyf1.scoring.PerformanceData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Mantiene le ridondanze deliberate RISULTATO_TEAM e PunteggioTotale.
 */
public final class ResultDao {

    private static final String FIND_PERFORMANCES = """
        SELECT
            IdPilota,
            PosizionamentoQualifica,
            PosizionamentoGara,
            Penalizzato,
            RegistraGiroVeloce
        FROM PRESTAZIONE_WEEKEND PW
        JOIN WEEKEND_DI_GARA W
          ON W.IdEdizione = PW.IdEdizione
         AND W.IdGranPremio = PW.IdGranPremio
        WHERE PW.IdEdizione = ?
          AND PW.IdGranPremio = ?
          AND W.Concluso = TRUE
        ORDER BY PW.IdPilota
        """;

    private static final String UPDATE_SCORE = """
        UPDATE PRESTAZIONE_WEEKEND
        SET PunteggioFantasy = ?
        WHERE IdEdizione = ?
          AND IdGranPremio = ?
          AND IdPilota = ?
          AND EXISTS (
              SELECT 1
              FROM WEEKEND_DI_GARA W
              WHERE W.IdEdizione = PRESTAZIONE_WEEKEND.IdEdizione
                AND W.IdGranPremio = PRESTAZIONE_WEEKEND.IdGranPremio
                AND W.Concluso = TRUE
          )
        """;

    private static final String COMPLETION_STATUS = """
        SELECT
            (SELECT COUNT(*)
             FROM PILOTA_ISCRITTO PI
             WHERE PI.IdEdizione = W.IdEdizione) AS PilotiIscritti,
            (SELECT COUNT(*)
             FROM PRESTAZIONE_WEEKEND PW
             JOIN PILOTA_ISCRITTO PI
               ON PI.IdEdizione = PW.IdEdizione
              AND PI.IdPilota = PW.IdPilota
             WHERE PW.IdEdizione = W.IdEdizione
               AND PW.IdGranPremio = W.IdGranPremio
               AND PW.Penalizzato IS NOT NULL
               AND PW.RegistraGiroVeloce IS NOT NULL) AS PrestazioniComplete
        FROM WEEKEND_DI_GARA W
        WHERE W.IdEdizione = ? AND W.IdGranPremio = ?
        """;

    private static final String DELETE_WEEKEND_RESULTS = """
        DELETE FROM RISULTATO_TEAM
        WHERE IdEdizione = ? AND IdGranPremio = ?
        """;

    private static final String UPSERT_WEEKEND_RESULTS = """
        INSERT INTO RISULTATO_TEAM
            (IdEdizione, IdGranPremio, IdTeam, PunteggioWeekend)
        SELECT
            CT.IdEdizione,
            ?,
            CT.IdTeam,
            SUM(PW.PunteggioFantasy)
        FROM COMPOSIZIONE_TEAM CT
        JOIN WEEKEND_DI_GARA W
          ON W.IdEdizione = CT.IdEdizione
         AND W.IdGranPremio = ?
         AND W.Concluso = TRUE
        JOIN PRESTAZIONE_WEEKEND PW
          ON PW.IdEdizione = CT.IdEdizione
         AND PW.IdPilota = CT.IdPilota
         AND PW.IdGranPremio = ?
        WHERE CT.IdEdizione = ?
        GROUP BY CT.IdEdizione, CT.IdTeam
        HAVING COUNT(*) = 4
           AND COUNT(PW.PunteggioFantasy) = 4
        ON DUPLICATE KEY UPDATE
            PunteggioWeekend = VALUES(PunteggioWeekend)
        """;

    private static final String RECALCULATE_EDITION_TOTALS = """
        UPDATE TEAM_FANTASY TF
        SET TF.PunteggioTotale = COALESCE((
            SELECT SUM(RT.PunteggioWeekend)
            FROM RISULTATO_TEAM RT
            JOIN WEEKEND_DI_GARA W
              ON W.IdEdizione = RT.IdEdizione
             AND W.IdGranPremio = RT.IdGranPremio
            WHERE RT.IdTeam = TF.IdTeam
              AND RT.IdEdizione = TF.IdEdizione
              AND W.Concluso = TRUE
        ), 0)
        WHERE TF.IdEdizione = ?
        """;

    private static final String RECALCULATE_TEAM_TOTAL = """
        UPDATE TEAM_FANTASY TF
        SET TF.PunteggioTotale = COALESCE((
            SELECT SUM(RT.PunteggioWeekend)
            FROM RISULTATO_TEAM RT
            JOIN WEEKEND_DI_GARA W
              ON W.IdEdizione = RT.IdEdizione
             AND W.IdGranPremio = RT.IdGranPremio
            WHERE RT.IdTeam = TF.IdTeam
              AND RT.IdEdizione = TF.IdEdizione
              AND W.Concluso = TRUE
        ), 0)
        WHERE TF.IdTeam = ?
        """;

    private static final String FIND_CONCLUDED_WEEKENDS = """
        SELECT IdGranPremio
        FROM WEEKEND_DI_GARA
        WHERE IdEdizione = ? AND Concluso = TRUE
        ORDER BY NumeroRound
        """;

    private static final String UPSERT_SINGLE_TEAM_RESULT = """
        INSERT INTO RISULTATO_TEAM
            (IdEdizione, IdGranPremio, IdTeam, PunteggioWeekend)
        SELECT
            CT.IdEdizione,
            ?,
            CT.IdTeam,
            SUM(PW.PunteggioFantasy)
        FROM COMPOSIZIONE_TEAM CT
        JOIN WEEKEND_DI_GARA W
          ON W.IdEdizione = CT.IdEdizione
         AND W.IdGranPremio = ?
         AND W.Concluso = TRUE
        JOIN PRESTAZIONE_WEEKEND PW
          ON PW.IdEdizione = CT.IdEdizione
         AND PW.IdPilota = CT.IdPilota
         AND PW.IdGranPremio = ?
        WHERE CT.IdTeam = ? AND CT.IdEdizione = ?
        GROUP BY CT.IdEdizione, CT.IdTeam
        HAVING COUNT(*) = 4
           AND COUNT(PW.PunteggioFantasy) = 4
        ON DUPLICATE KEY UPDATE
            PunteggioWeekend = VALUES(PunteggioWeekend)
        """;

    public List<PerformanceRow> findPerformances(
        final Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        final List<PerformanceRow> rows = new ArrayList<>();
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_PERFORMANCES)) {
            statement.setInt(1, editionId);
            statement.setInt(2, grandPrixId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    rows.add(new PerformanceRow(
                        result.getInt("IdPilota"),
                        new PerformanceData(
                            nullableInt(
                                result,
                                "PosizionamentoQualifica"
                            ),
                            nullableInt(result, "PosizionamentoGara"),
                            result.getBoolean("Penalizzato"),
                            result.getBoolean("RegistraGiroVeloce")
                        )
                    ));
                }
            }
        }
        return List.copyOf(rows);
    }

    public void updateFantasyScore(
        final Connection connection,
        final int editionId,
        final int grandPrixId,
        final int driverId,
        final int score
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(UPDATE_SCORE)) {
            statement.setInt(1, score);
            statement.setInt(2, editionId);
            statement.setInt(3, grandPrixId);
            statement.setInt(4, driverId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException(
                    "Prestazione non trovata durante il calcolo"
                );
            }
        }
    }

    public CompletionStatus completionStatus(
        final Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(COMPLETION_STATUS)) {
            statement.setInt(1, editionId);
            statement.setInt(2, grandPrixId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return new CompletionStatus(0, 0);
                }
                return new CompletionStatus(
                    result.getInt("PilotiIscritti"),
                    result.getInt("PrestazioniComplete")
                );
            }
        }
    }

    public void recalculateWeekendResults(
        final Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        try (PreparedStatement delete =
                 connection.prepareStatement(DELETE_WEEKEND_RESULTS)) {
            delete.setInt(1, editionId);
            delete.setInt(2, grandPrixId);
            delete.executeUpdate();
        }
        try (PreparedStatement insert =
                 connection.prepareStatement(UPSERT_WEEKEND_RESULTS)) {
            insert.setInt(1, grandPrixId);
            insert.setInt(2, grandPrixId);
            insert.setInt(3, grandPrixId);
            insert.setInt(4, editionId);
            insert.executeUpdate();
        }
    }

    public void clearWeekendResults(
        final Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(DELETE_WEEKEND_RESULTS)) {
            statement.setInt(1, editionId);
            statement.setInt(2, grandPrixId);
            statement.executeUpdate();
        }
    }

    public void recalculateEditionTotals(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(RECALCULATE_EDITION_TOTALS)) {
            statement.setInt(1, editionId);
            statement.executeUpdate();
        }
    }

    public void recalculateTeamTotal(
        final Connection connection,
        final int teamId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(RECALCULATE_TEAM_TOTAL)) {
            statement.setInt(1, teamId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Team non trovato durante O3");
            }
        }
    }

    public List<Integer> findConcludedWeekendIds(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        final List<Integer> ids = new ArrayList<>();
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_CONCLUDED_WEEKENDS)) {
            statement.setInt(1, editionId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    ids.add(result.getInt("IdGranPremio"));
                }
            }
        }
        return List.copyOf(ids);
    }

    private static Integer nullableInt(
        final ResultSet result,
        final String column
    ) throws SQLException {
        final int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }

    public void upsertResultForTeam(
        final Connection connection,
        final int teamId,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(UPSERT_SINGLE_TEAM_RESULT)) {
            statement.setInt(1, grandPrixId);
            statement.setInt(2, grandPrixId);
            statement.setInt(3, grandPrixId);
            statement.setInt(4, teamId);
            statement.setInt(5, editionId);
            statement.executeUpdate();
        }
    }

    public record PerformanceRow(int driverId, PerformanceData data) {
    }

    public record CompletionStatus(
        int enrolledDrivers,
        int completedPerformances
    ) {

        public boolean ready() {
            return enrolledDrivers > 0
                && enrolledDrivers == completedPerformances;
        }

        public int missingPerformances() {
            return Math.max(0, enrolledDrivers - completedPerformances);
        }
    }
}
