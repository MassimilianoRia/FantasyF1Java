package it.unibo.fantasyf1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.fantasyf1.model.WeekendScoreRow;
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
    }

    @Test
    void o2IsAbsentUntilFourScoresThenO3AndU8AreAvailable() {
        final WeekendFixture weekend = completeWeekendFixture();
        final List<PerformanceData> performances = performances();

        for (int index = 0; index < 3; index++) {
            final ProcessingOutcome outcome = record(
                weekend,
                index,
                performances.get(index)
            );
            assertFalse(outcome.weekendProcessable());
        }
        assertEquals(
            0,
            database.queryInt(
                "SELECT COUNT(*) FROM RISULTATO_TEAM WHERE IdTeam = ?",
                weekend.teamId()
            )
        );
        assertEquals(
            0,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                weekend.teamId()
            )
        );

        final ProcessingOutcome fourth = record(
            weekend,
            3,
            performances.get(3)
        );
        final int expected = performances.stream()
            .mapToInt(scoring::score)
            .sum();
        assertTrue(fourth.weekendProcessable());
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
            performances.stream()
                .map(scoring::score)
                .sorted()
                .toList(),
            breakdown.stream()
                .map(WeekendScoreRow::fantasyPoints)
                .sorted()
                .toList()
        );
    }

    @Test
    void processingIsIdempotentCorrectionCascadesAndHistoricalTeamCatchesUp() {
        final WeekendFixture weekend = completeWeekendFixture();
        final List<PerformanceData> original = performances();
        for (int index = 0; index < original.size(); index++) {
            record(weekend, index, original.get(index));
        }
        final int originalTotal = original.stream()
            .mapToInt(scoring::score)
            .sum();

        assertTrue(admin.processWeekend(
            weekend.editionId(),
            weekend.grandPrixId()
        ));
        assertTrue(admin.processWeekend(
            weekend.editionId(),
            weekend.grandPrixId()
        ));
        assertEquals(
            1,
            database.queryInt(
                """
                SELECT COUNT(*) FROM RISULTATO_TEAM
                WHERE IdTeam = ? AND IdGranPremio = ?
                """,
                weekend.teamId(),
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

        final PerformanceData correction =
            new PerformanceData(20, 20, false, false);
        final ProcessingOutcome corrected = record(
            weekend,
            0,
            correction
        );
        final int correctedTotal = originalTotal
            - scoring.score(original.get(0))
            + scoring.score(correction);
        assertTrue(corrected.weekendProcessable());
        assertEquals(
            correctedTotal,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                weekend.teamId()
            )
        );

        final int historicalOwner =
            fixtures.user("historical.owner", "unused-hash");
        sessions.login(historicalOwner, "historical.owner");
        final int historicalTeam = teams.createTeam(
            "Team creato dopo il weekend",
            weekend.editionId(),
            weekend.driverIds()
        );
        assertEquals(
            correctedTotal,
            database.queryInt(
                "SELECT PunteggioTotale FROM TEAM_FANTASY WHERE IdTeam = ?",
                historicalTeam
            )
        );
        assertEquals(
            correctedTotal,
            database.queryInt(
                """
                SELECT PunteggioWeekend FROM RISULTATO_TEAM
                WHERE IdTeam = ? AND IdGranPremio = ?
                """,
                historicalTeam,
                weekend.grandPrixId()
            )
        );
    }

    @Test
    void aFailureDuringO1RollsBackThePerformanceAndDerivedData() {
        final WeekendFixture weekend = completeWeekendFixture();
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
            () -> failingAdmin.recordPerformance(new PerformanceRequest(
                weekend.editionId(),
                weekend.grandPrixId(),
                weekend.driverIds().getFirst(),
                1,
                1,
                false,
                false
            ))
        );

        assertEquals(
            0,
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
                SELECT COUNT(*) FROM RISULTATO_TEAM
                WHERE IdEdizione = ? AND IdGranPremio = ?
                """,
                weekend.editionId(),
                weekend.grandPrixId()
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
        final int grandPrixId = fixtures.grandPrix("Elaborabile");
        fixtures.weekend(
            editionId,
            grandPrixId,
            1,
            LocalDate.of(2026, 7, 17),
            LocalDate.of(2026, 7, 20)
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
            editionId,
            grandPrixId,
            teamId,
            List.copyOf(driverIds)
        );
    }

    private ProcessingOutcome record(
        final WeekendFixture weekend,
        final int driverIndex,
        final PerformanceData data
    ) {
        return admin.recordPerformance(new PerformanceRequest(
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
        int editionId,
        int grandPrixId,
        int teamId,
        List<Integer> driverIds
    ) {
    }
}
