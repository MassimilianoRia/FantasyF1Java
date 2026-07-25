package it.unibo.fantasyf1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.fantasyf1.error.AppException;
import it.unibo.fantasyf1.error.ErrorCode;
import it.unibo.fantasyf1.model.StandingRow;
import it.unibo.fantasyf1.model.WeekendScoreRow;
import it.unibo.fantasyf1.model.dao.AdminDao;
import it.unibo.fantasyf1.model.dao.ResultDao;
import it.unibo.fantasyf1.model.database.TransactionManager;
import it.unibo.fantasyf1.scoring.PerformanceData;
import it.unibo.fantasyf1.scoring.SimpleScoringPolicy;
import it.unibo.fantasyf1.security.Pbkdf2PasswordHasher;
import it.unibo.fantasyf1.session.SessionManager;
import it.unibo.fantasyf1.testutil.TestDatabase;
import it.unibo.fantasyf1.testutil.TestFixtures;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class WeekendProcessingH2Test {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-07-23T12:00:00Z"),
        ZoneOffset.UTC
    );

    private TestDatabase database;
    private TestFixtures fixtures;
    private SessionManager sessions;
    private SimpleScoringPolicy scoring;
    private AdminService admin;
    private TeamService teams;
    private LeagueService leagues;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();
        fixtures = new TestFixtures(database);
        sessions = new SessionManager();
        scoring = new SimpleScoringPolicy();
        final ApplicationServices services = new ApplicationServices(
            database,
            CLOCK,
            new Pbkdf2PasswordHasher(),
            scoring,
            sessions
        );
        admin = services.admin();
        teams = services.teams();
        leagues = services.leagues();
    }

    @Test
    void a8StoresEachOpenPerformanceOnlyOnceAndU8WaitsForA9() {
        final WeekendFixture weekend = completeWeekendFixture();
        final List<PerformanceData> performances = performances();

        for (int index = 0; index < performances.size(); index++) {
            record(weekend, index, performances.get(index));
        }
        final AppException duplicate = assertThrows(
            AppException.class,
            () -> record(
                weekend,
                0,
                new PerformanceData(20, 20, false, false)
            )
        );
        assertEquals(ErrorCode.CONFLICT, duplicate.code());
        assertEquals(
            performances.getFirst().qualifyingPosition(),
            database.queryInt(
                """
                SELECT PosizionamentoQualifica
                FROM PRESTAZIONE_WEEKEND
                WHERE IdEdizione = ? AND IdGranPremio = ? AND IdPilota = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId(),
                weekend.driverIds().getFirst()
            )
        );

        assertFalse(admin.weekends(weekend.editionId()).getFirst().concluded());
        assertEquals(
            4,
            database.queryInt(
                """
                SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND
                WHERE IdEdizione = ? AND IdGranPremio = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            0,
            database.queryInt(
                """
                SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND
                WHERE IdEdizione = ? AND IdGranPremio = ?
                  AND PunteggioFantasy IS NOT NULL
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            0,
            database.queryInt(
                """
                SELECT COUNT(*) FROM RISULTATO_TEAM
                WHERE IdEdizione = ? AND IdGranPremio = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            0,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                weekend.teamId()
            )
        );
        assertThrows(
            AppException.class,
            () -> new TransactionManager(database).inTransaction(
                connection -> {
                    new ResultDao().updateFantasyScore(
                        connection,
                        weekend.editionId(),
                        weekend.grandPrixId(),
                        weekend.driverIds().getFirst(),
                        999
                    );
                    return null;
                }
            )
        );

        final AppException unavailable = assertThrows(
            AppException.class,
            () -> teams.weekendBreakdown(
                weekend.teamId(),
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(ErrorCode.CONFLICT, unavailable.code());
    }

    @Test
    void a9RequiresEveryPerformanceThenCalculatesAndLocksTheWeekend() {
        final WeekendFixture weekend = completeWeekendFixture();
        final List<PerformanceData> performances = performances();
        for (int index = 0; index < 3; index++) {
            record(weekend, index, performances.get(index));
        }

        final AppException incomplete = assertThrows(
            AppException.class,
            () -> admin.concludeWeekend(
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(ErrorCode.CONFLICT, incomplete.code());
        assertEquals(
            0,
            database.queryInt(
                """
                SELECT COUNT(*) FROM WEEKEND_DI_GARA
                WHERE IdEdizione = ? AND IdGranPremio = ?
                  AND Concluso = TRUE
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );

        record(weekend, 3, performances.get(3));
        admin.concludeWeekend(
            weekend.editionId(),
            weekend.grandPrixId()
        );

        final int expected = performances.stream()
            .mapToInt(scoring::score)
            .sum();
        assertTrue(admin.weekends(weekend.editionId()).getFirst().concluded());
        assertEquals(
            4,
            database.queryInt(
                """
                SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND
                WHERE IdEdizione = ? AND IdGranPremio = ?
                  AND PunteggioFantasy IS NOT NULL
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            expected,
            database.queryInt(
                """
                SELECT PunteggioWeekend FROM RISULTATO_TEAM
                WHERE IdTeam = ? AND IdGranPremio = ?
                """,
                weekend.teamId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            expected,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                weekend.teamId()
            )
        );

        final List<WeekendScoreRow> breakdown = teams.weekendBreakdown(
            weekend.teamId(),
            weekend.editionId(),
            weekend.grandPrixId()
        );
        assertEquals(4, breakdown.size());
        assertEquals(
            performances.stream().map(scoring::score).sorted().toList(),
            breakdown.stream()
                .map(WeekendScoreRow::fantasyPoints)
                .sorted()
                .toList()
        );
        for (WeekendScoreRow row : breakdown) {
            final int driverIndex = weekend.driverIds().indexOf(row.driverId());
            assertTrue(driverIndex >= 0);
            final PerformanceData performance = performances.get(driverIndex);
            assertEquals(
                performance.qualifyingPosition(),
                row.qualifyingPosition()
            );
            assertEquals(performance.racePosition(), row.racePosition());
            assertEquals(performance.penalized(), row.penalized());
            assertEquals(performance.fastestLap(), row.fastestLap());
        }

        final AppException secondConclusion = assertThrows(
            AppException.class,
            () -> admin.concludeWeekend(
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(ErrorCode.CONFLICT, secondConclusion.code());

        final AppException correction = assertThrows(
            AppException.class,
            () -> record(
                weekend,
                0,
                new PerformanceData(20, 20, false, false)
            )
        );
        assertEquals(ErrorCode.CONFLICT, correction.code());
        assertThrows(
            AppException.class,
            () -> new TransactionManager(database).inTransaction(connection -> {
                new AdminDao().insertPerformance(
                    connection,
                    weekend.editionId(),
                    weekend.grandPrixId(),
                    weekend.driverIds().getFirst(),
                    new PerformanceData(20, 20, false, false)
                );
                return null;
            })
        );
        assertEquals(
            1,
            database.queryInt(
                """
                SELECT PosizionamentoQualifica
                FROM PRESTAZIONE_WEEKEND
                WHERE IdEdizione = ? AND IdGranPremio = ? AND IdPilota = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId(),
                weekend.driverIds().getFirst()
            )
        );
        assertEquals(
            expected,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                weekend.teamId()
            )
        );
    }

    @Test
    void a10ReopensInvalidatesDerivedDataAndSupportsRecalculation() {
        final WeekendFixture weekend = completeWeekendFixture();
        final List<PerformanceData> initial = performances();
        for (int index = 0; index < initial.size(); index++) {
            record(weekend, index, initial.get(index));
        }
        final int zeroTeam = fixtures.team(
            "Team senza altri risultati",
            0,
            weekend.ownerId(),
            weekend.editionId()
        );
        for (int driverId : weekend.driverIds()) {
            fixtures.component(zeroTeam, weekend.editionId(), driverId);
        }
        admin.concludeWeekend(
            weekend.editionId(),
            weekend.grandPrixId()
        );

        final int otherGrandPrix = fixtures.grandPrix("Altro concluso");
        fixtures.weekend(
            weekend.editionId(),
            otherGrandPrix,
            2,
            LocalDate.of(2030, 8, 1),
            LocalDate.of(2030, 8, 3)
        );
        fixtures.concludeWeekend(weekend.editionId(), otherGrandPrix);
        database.update(
            """
            INSERT INTO RISULTATO_TEAM
                (IdEdizione, IdGranPremio, IdTeam, PunteggioWeekend)
            VALUES (?, ?, ?, 37)
            """,
            weekend.editionId(),
            otherGrandPrix,
            weekend.teamId()
        );
        database.update(
            "UPDATE TEAM_FANTASY SET PunteggioTotale = ? WHERE IdTeam = ?",
            initial.stream().mapToInt(scoring::score).sum() + 37,
            weekend.teamId()
        );

        admin.reopenWeekend(
            weekend.editionId(),
            weekend.grandPrixId()
        );

        assertFalse(admin.weekends(weekend.editionId()).getFirst().concluded());
        assertEquals(
            4,
            database.queryInt(
                """
                SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND
                WHERE IdEdizione = ? AND IdGranPremio = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            0,
            database.queryInt(
                """
                SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND
                WHERE IdEdizione = ? AND IdGranPremio = ?
                  AND PunteggioFantasy IS NOT NULL
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            0,
            database.queryInt(
                """
                SELECT COUNT(*) FROM RISULTATO_TEAM
                WHERE IdEdizione = ? AND IdGranPremio = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            37,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                weekend.teamId()
            )
        );
        assertEquals(
            0,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                zeroTeam
            )
        );
        assertEquals(
            initial.getFirst().racePosition(),
            database.queryInt(
                """
                SELECT PosizionamentoGara
                FROM PRESTAZIONE_WEEKEND
                WHERE IdEdizione = ? AND IdGranPremio = ? AND IdPilota = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId(),
                weekend.driverIds().getFirst()
            )
        );

        final AppException correction = assertThrows(
            AppException.class,
            () -> record(
                weekend,
                0,
                new PerformanceData(20, 20, false, false)
            )
        );
        assertEquals(ErrorCode.CONFLICT, correction.code());
        assertEquals(
            0,
            database.queryInt(
                """
                SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND
                WHERE IdEdizione = ? AND IdGranPremio = ?
                  AND PunteggioFantasy IS NOT NULL
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        admin.concludeWeekend(
            weekend.editionId(),
            weekend.grandPrixId()
        );

        final int recalculated = initial.stream()
            .mapToInt(scoring::score)
            .sum();
        assertEquals(
            2,
            database.queryInt(
                """
                SELECT COUNT(*) FROM RISULTATO_TEAM
                WHERE IdEdizione = ? AND IdGranPremio = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            1,
            database.queryInt(
                """
                SELECT COUNT(*) FROM RISULTATO_TEAM
                WHERE IdEdizione = ? AND IdGranPremio = ? AND IdTeam = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId(),
                weekend.teamId()
            )
        );
        assertEquals(
            recalculated + 37,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                weekend.teamId()
            )
        );
    }

    @Test
    void a10RejectsOpenAndMissingWeekends() {
        final WeekendFixture weekend = completeWeekendFixture();

        final AppException open = assertThrows(
            AppException.class,
            () -> admin.reopenWeekend(
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(ErrorCode.CONFLICT, open.code());

        final AppException missing = assertThrows(
            AppException.class,
            () -> admin.reopenWeekend(weekend.editionId(), 999_999)
        );
        assertEquals(ErrorCode.NOT_FOUND, missing.code());
    }

    @Test
    void a10FailureRollsBackStateScoresResultsAndTotals() {
        final WeekendFixture weekend = completeWeekendFixture();
        final List<PerformanceData> values = performances();
        for (int index = 0; index < values.size(); index++) {
            record(weekend, index, values.get(index));
        }
        admin.concludeWeekend(
            weekend.editionId(),
            weekend.grandPrixId()
        );
        final int originalTotal = values.stream()
            .mapToInt(scoring::score)
            .sum();
        database.update(
            """
            CREATE TRIGGER FAIL_A10_DELETE
            BEFORE DELETE ON RISULTATO_TEAM
            FOR EACH ROW CALL
            'it.unibo.fantasyf1.testutil.FailingDeleteTrigger'
            """
        );

        assertThrows(
            AppException.class,
            () -> admin.reopenWeekend(
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertTrue(admin.weekends(weekend.editionId()).getFirst().concluded());
        assertEquals(
            4,
            database.queryInt(
                """
                SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND
                WHERE IdEdizione = ? AND IdGranPremio = ?
                  AND PunteggioFantasy IS NOT NULL
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            1,
            database.queryInt(
                """
                SELECT COUNT(*) FROM RISULTATO_TEAM
                WHERE IdEdizione = ? AND IdGranPremio = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            originalTotal,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                weekend.teamId()
            )
        );
    }

    @Test
    void administrativeConclusionDoesNotDependOnEndDate() {
        final WeekendFixture weekend = completeWeekendFixture();
        for (int index = 0; index < performances().size(); index++) {
            record(weekend, index, performances().get(index));
        }

        // La fixture termina nel 2030, molto dopo il Clock fisso del test.
        admin.concludeWeekend(
            weekend.editionId(),
            weekend.grandPrixId()
        );

        assertTrue(admin.weekends(weekend.editionId()).getFirst().concluded());
        final AppException missing = assertThrows(
            AppException.class,
            () -> admin.concludeWeekend(weekend.editionId(), 999_999)
        );
        assertEquals(ErrorCode.NOT_FOUND, missing.code());
    }

    @Test
    void o1FailureRollsBackA9IncludingTheConclusionFlag() {
        final WeekendFixture weekend = completeWeekendFixture();
        for (int index = 0; index < performances().size(); index++) {
            record(weekend, index, performances().get(index));
        }

        final AtomicInteger invocations = new AtomicInteger();
        final var failingPolicy =
            (it.unibo.fantasyf1.scoring.ScoringPolicy) performance -> {
                if (invocations.incrementAndGet() == 2) {
                    throw new IllegalStateException("Errore policy simulato");
                }
                return 10;
            };
        final AdminService failingAdmin = new ApplicationServices(
            database,
            CLOCK,
            new Pbkdf2PasswordHasher(),
            failingPolicy,
            new SessionManager()
        ).admin();

        assertThrows(
            IllegalStateException.class,
            () -> failingAdmin.concludeWeekend(
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            0,
            database.queryInt(
                """
                SELECT COUNT(*) FROM WEEKEND_DI_GARA
                WHERE IdEdizione = ? AND IdGranPremio = ?
                  AND Concluso = TRUE
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            0,
            database.queryInt(
                """
                SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND
                WHERE IdEdizione = ? AND IdGranPremio = ?
                  AND PunteggioFantasy IS NOT NULL
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
        assertEquals(
            0,
            database.queryInt(
                """
                SELECT COUNT(*) FROM RISULTATO_TEAM
                WHERE IdEdizione = ? AND IdGranPremio = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId()
            )
        );
    }

    @Test
    void o2O3AndStandingsIgnoreEveryOpenWeekend() {
        final WeekendFixture weekend = completeWeekendFixture();
        final List<PerformanceData> performances = performances();
        for (int index = 0; index < performances.size(); index++) {
            record(weekend, index, performances.get(index));
        }
        admin.concludeWeekend(
            weekend.editionId(),
            weekend.grandPrixId()
        );
        final int expected = performances.stream()
            .mapToInt(scoring::score)
            .sum();

        final int openGrandPrix = fixtures.grandPrix("Aperto");
        fixtures.weekend(
            weekend.editionId(),
            openGrandPrix,
            2,
            LocalDate.of(2020, 1, 1),
            LocalDate.of(2020, 1, 3)
        );
        database.update(
            """
            INSERT INTO RISULTATO_TEAM
                (IdEdizione, IdGranPremio, IdTeam, PunteggioWeekend)
            VALUES (?, ?, ?, 999)
            """,
            weekend.editionId(),
            openGrandPrix,
            weekend.teamId()
        );

        final ResultDao resultDao = new ResultDao();
        new TransactionManager(database).inTransaction(
            connection -> {
                resultDao.recalculateEditionTotals(
                    connection,
                    weekend.editionId()
                );
                return null;
            }
        );
        assertEquals(
            expected,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                weekend.teamId()
            )
        );

        final int leagueId = fixtures.league(
            "Classifica conclusi",
            weekend.ownerId(),
            weekend.editionId()
        );
        fixtures.participation(leagueId, weekend.teamId());
        final List<StandingRow> standings = leagues.standings(
            leagueId,
            weekend.editionId()
        );
        assertEquals(1, standings.size());
        assertEquals(expected, standings.getFirst().totalPoints());

        final int laterOwner = fixtures.user("later.owner", "unused-hash");
        sessions.login(laterOwner, "later.owner");
        final int laterTeam = teams.createTeam(
            "Team creato dopo A9",
            weekend.editionId(),
            weekend.driverIds()
        );
        assertEquals(
            expected,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                laterTeam
            )
        );
        assertEquals(
            0,
            database.queryInt(
                """
                SELECT COUNT(*) FROM RISULTATO_TEAM
                WHERE IdTeam = ? AND IdGranPremio = ?
                """,
                laterTeam,
                openGrandPrix
            )
        );
    }

    private WeekendFixture completeWeekendFixture() {
        final int ownerId = fixtures.user("weekend.owner", "unused-hash");
        sessions.login(ownerId, "weekend.owner");
        final int editionId = fixtures.edition(1, 2025);
        final List<Integer> driverIds = new ArrayList<>();
        for (int constructorIndex = 0; constructorIndex < 2;
             constructorIndex++) {
            final int constructorId =
                fixtures.racingConstructor("Weekend" + constructorIndex);
            fixtures.enrollConstructor(
                editionId,
                constructorId,
                "Weekend" + constructorIndex
            );
            for (int slot = 0; slot < 2; slot++) {
                final int index = constructorIndex * 2 + slot;
                final int driverId = fixtures.driver("Weekend" + index);
                fixtures.enrollDriver(
                    editionId,
                    driverId,
                    "W%c%c".formatted(
                        (char) ('A' + constructorIndex),
                        (char) ('A' + slot)
                    ),
                    index + 1,
                    constructorId
                );
                driverIds.add(driverId);
            }
        }
        final int grandPrixId = fixtures.grandPrix("Convalidabile");
        fixtures.weekend(
            editionId,
            grandPrixId,
            1,
            LocalDate.of(2030, 7, 17),
            LocalDate.of(2030, 7, 20)
        );
        final int teamId = fixtures.team(
            "Weekend Team",
            0,
            ownerId,
            editionId
        );
        for (int driverId : driverIds) {
            fixtures.component(teamId, editionId, driverId);
        }
        return new WeekendFixture(
            ownerId,
            editionId,
            grandPrixId,
            teamId,
            List.copyOf(driverIds)
        );
    }

    private void record(
        final WeekendFixture weekend,
        final int driverIndex,
        final PerformanceData data
    ) {
        admin.recordPerformance(new PerformanceRequest(
            weekend.editionId(),
            weekend.grandPrixId(),
            weekend.driverIds().get(driverIndex),
            data.qualifyingPosition(),
            data.racePosition(),
            data.penalized(),
            data.fastestLap()
        ));
    }

    private static List<PerformanceData> performances() {
        return List.of(
            new PerformanceData(1, 1, false, true),
            new PerformanceData(2, 2, false, false),
            new PerformanceData(3, 3, true, false),
            new PerformanceData(4, 4, false, false)
        );
    }

    private record WeekendFixture(
        int ownerId,
        int editionId,
        int grandPrixId,
        int teamId,
        List<Integer> driverIds
    ) {
    }
}
