package it.unibo.fantasyf1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.fantasyf1.model.EditionOverview;
import it.unibo.fantasyf1.model.WeekendPerformanceStatus;
import it.unibo.fantasyf1.scoring.SimpleScoringPolicy;
import it.unibo.fantasyf1.security.Pbkdf2PasswordHasher;
import it.unibo.fantasyf1.session.SessionManager;
import it.unibo.fantasyf1.testutil.TestDatabase;
import it.unibo.fantasyf1.testutil.TestFixtures;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Verifica la fotografia dell'edizione e il controllo delle prestazioni.
 */
final class EditionOverviewH2Test {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-07-25T12:00:00Z"),
        ZoneOffset.UTC
    );

    @Test
    void overviewShowsPopulationAndMissingWeekendPerformances() {
        final TestDatabase database = new TestDatabase();
        final TestFixtures fixtures = new TestFixtures(database);
        final ApplicationServices services = new ApplicationServices(
            database,
            CLOCK,
            new Pbkdf2PasswordHasher(),
            new SimpleScoringPolicy(),
            new SessionManager()
        );

        final int editionId = fixtures.edition(1, 2026);
        final int grandPrixId = fixtures.grandPrix("Overview");
        fixtures.weekend(
            editionId,
            grandPrixId,
            1,
            LocalDate.of(2026, 3, 6),
            LocalDate.of(2026, 3, 8)
        );
        final int constructorId = fixtures.racingConstructor("Overview");
        fixtures.enrollConstructor(
            editionId,
            constructorId,
            "Overview"
        );
        final int recordedDriverId = fixtures.driver("Registrato");
        final int missingDriverId = fixtures.driver("Mancante");
        fixtures.enrollDriver(
            editionId,
            recordedDriverId,
            "REG",
            11,
            constructorId
        );
        fixtures.enrollDriver(
            editionId,
            missingDriverId,
            "MAN",
            12,
            constructorId
        );
        fixtures.performance(
            editionId,
            grandPrixId,
            recordedDriverId,
            2,
            1,
            false,
            true,
            null
        );

        final EditionOverview overview =
            services.editions().overview(editionId);
        assertEquals(editionId, overview.edition().id());
        assertEquals(1, overview.weekends().size());
        assertEquals(1, overview.constructors().size());
        assertEquals(2, overview.drivers().size());
        assertEquals(1, overview.status().constructorsWithTwoDrivers());
        assertFalse(overview.status().complete());

        final List<WeekendPerformanceStatus> statuses =
            services.admin().weekendPerformanceStatus(
                editionId,
                grandPrixId
            );
        assertEquals(2, statuses.size());

        final WeekendPerformanceStatus recorded = statuses.stream()
            .filter(status -> status.driverId() == recordedDriverId)
            .findFirst()
            .orElseThrow();
        assertTrue(recorded.recorded());
        assertEquals(2, recorded.qualifyingPosition());
        assertEquals(1, recorded.racePosition());
        assertTrue(recorded.fastestLap());
        assertNull(recorded.fantasyPoints());

        final WeekendPerformanceStatus missing = statuses.stream()
            .filter(status -> status.driverId() == missingDriverId)
            .findFirst()
            .orElseThrow();
        assertFalse(missing.recorded());
        assertNull(missing.qualifyingPosition());
        assertNull(missing.racePosition());
    }
}
