package it.unibo.fantasyf1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.fantasyf1.error.AppException;
import it.unibo.fantasyf1.error.ErrorCode;
import it.unibo.fantasyf1.model.EditionStatus;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class AdminLimitsH2Test {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-07-23T12:00:00Z"),
        ZoneOffset.UTC
    );

    private TestDatabase database;
    private TestFixtures fixtures;
    private AdminService admin;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();
        fixtures = new TestFixtures(database);
        admin = new ApplicationServices(
            database,
            CLOCK,
            new Pbkdf2PasswordHasher(),
            new SimpleScoringPolicy(),
            new SessionManager()
        ).admin();
    }

    @Test
    void a3AcceptsTheTwentyFourthWeekendAndRejectsTheTwentyFifth() {
        final int editionId = fixtures.edition(1, 2025);
        final List<Integer> grandPrixIds = new ArrayList<>();
        for (int round = 1; round <= 25; round++) {
            grandPrixIds.add(fixtures.grandPrix("W" + round));
        }
        for (int round = 1; round <= 23; round++) {
            addWeekendFixture(editionId, grandPrixIds.get(round - 1), round);
        }

        admin.addWeekend(
            editionId,
            grandPrixIds.get(23),
            24,
            LocalDate.of(2025, 12, 1),
            LocalDate.of(2025, 12, 3)
        );
        final AppException overflow = assertThrows(
            AppException.class,
            () -> admin.addWeekend(
                editionId,
                grandPrixIds.get(24),
                24,
                LocalDate.of(2025, 12, 8),
                LocalDate.of(2025, 12, 10)
            )
        );

        assertEquals(ErrorCode.CONFLICT, overflow.code());
        assertEquals(
            24,
            database.queryInt(
                "SELECT COUNT(*) FROM WEEKEND_DI_GARA WHERE IdEdizione = ?",
                editionId
            )
        );
    }

    @Test
    void a5AcceptsTheTenthConstructorAndRejectsTheEleventh() {
        final int editionId = fixtures.edition(1, 2025);
        final List<Integer> constructorIds = new ArrayList<>();
        for (int index = 0; index < 11; index++) {
            constructorIds.add(fixtures.racingConstructor("Limit" + index));
        }
        for (int index = 0; index < 9; index++) {
            fixtures.enrollConstructor(
                editionId,
                constructorIds.get(index),
                "Limit" + index
            );
        }

        admin.enrollConstructor(
            editionId,
            constructorIds.get(9),
            "Decima",
            "Car 10"
        );
        final AppException overflow = assertThrows(
            AppException.class,
            () -> admin.enrollConstructor(
                editionId,
                constructorIds.get(10),
                "Undicesima",
                "Car 11"
            )
        );

        assertEquals(ErrorCode.CONFLICT, overflow.code());
        assertEquals(
            10,
            database.queryInt(
                "SELECT COUNT(*) FROM SCUDERIA_ISCRITTA WHERE IdEdizione = ?",
                editionId
            )
        );
    }

    @Test
    void a7RejectsTheThirdDriverForTheSameConstructor() {
        final int editionId = fixtures.edition(1, 2025);
        final int constructorId = fixtures.racingConstructor("TwoDrivers");
        fixtures.enrollConstructor(
            editionId,
            constructorId,
            "TwoDrivers"
        );
        final int first = fixtures.driver("First");
        final int second = fixtures.driver("Second");
        final int third = fixtures.driver("Third");

        admin.enrollDriver(editionId, first, "AAA", 1, constructorId);
        admin.enrollDriver(editionId, second, "AAB", 2, constructorId);
        final AppException overflow = assertThrows(
            AppException.class,
            () -> admin.enrollDriver(
                editionId,
                third,
                "AAC",
                3,
                constructorId
            )
        );

        assertEquals(ErrorCode.CONFLICT, overflow.code());
        assertEquals(
            2,
            database.queryInt(
                """
                SELECT COUNT(*) FROM PILOTA_ISCRITTO
                WHERE IdEdizione = ? AND IdScuderia = ?
                """,
                editionId,
                constructorId
            )
        );
    }

    @Test
    void a7AcceptsTheTwentiethDriverAndRejectsTheTwentyFirst() {
        final int editionId = fixtures.edition(1, 2025);
        final List<Integer> constructors = createTenEnrolledConstructors(
            editionId,
            "Grid"
        );
        final List<Integer> drivers = createDrivers(21, "GridDriver");

        int driverIndex = 0;
        fixtures.enrollDriver(
            editionId,
            drivers.get(driverIndex),
            code(driverIndex),
            driverIndex + 1,
            constructors.get(0)
        );
        driverIndex++;
        for (int constructorIndex = 1;
             constructorIndex < constructors.size();
             constructorIndex++) {
            for (int slot = 0; slot < 2; slot++) {
                fixtures.enrollDriver(
                    editionId,
                    drivers.get(driverIndex),
                    code(driverIndex),
                    driverIndex + 1,
                    constructors.get(constructorIndex)
                );
                driverIndex++;
            }
        }
        assertEquals(19, driverIndex);

        admin.enrollDriver(
            editionId,
            drivers.get(19),
            code(19),
            20,
            constructors.get(0)
        );
        final AppException overflow = assertThrows(
            AppException.class,
            () -> admin.enrollDriver(
                editionId,
                drivers.get(20),
                code(20),
                21,
                constructors.get(0)
            )
        );

        assertEquals(ErrorCode.CONFLICT, overflow.code());
        assertEquals(
            20,
            database.queryInt(
                "SELECT COUNT(*) FROM PILOTA_ISCRITTO WHERE IdEdizione = ?",
                editionId
            )
        );
    }

    @Test
    void editionBecomesCompleteOnlyAtAllFourBoundaries() {
        final int editionId = fixtures.edition(1, 2025);
        final List<Integer> constructors = createTenEnrolledConstructors(
            editionId,
            "Complete"
        );
        final List<Integer> drivers = createDrivers(20, "CompleteDriver");
        for (int index = 0; index < drivers.size(); index++) {
            fixtures.enrollDriver(
                editionId,
                drivers.get(index),
                code(index),
                index + 1,
                constructors.get(index / 2)
            );
        }
        for (int round = 1; round <= 23; round++) {
            addWeekendFixture(
                editionId,
                fixtures.grandPrix("Complete" + round),
                round
            );
        }

        final EditionStatus incomplete = admin.editionStatus(editionId);
        assertEquals(23, incomplete.weekends());
        assertEquals(10, incomplete.constructors());
        assertEquals(20, incomplete.drivers());
        assertEquals(10, incomplete.constructorsWithTwoDrivers());
        assertFalse(incomplete.complete());

        addWeekendFixture(
            editionId,
            fixtures.grandPrix("Complete24"),
            24
        );
        final EditionStatus complete = admin.editionStatus(editionId);
        assertEquals(24, complete.weekends());
        assertTrue(complete.complete());
    }

    private List<Integer> createTenEnrolledConstructors(
        final int editionId,
        final String prefix
    ) {
        final List<Integer> ids = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            final int constructorId =
                fixtures.racingConstructor(prefix + index);
            fixtures.enrollConstructor(
                editionId,
                constructorId,
                prefix + index
            );
            ids.add(constructorId);
        }
        return ids;
    }

    private List<Integer> createDrivers(
        final int count,
        final String prefix
    ) {
        final List<Integer> ids = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            ids.add(fixtures.driver(prefix + index));
        }
        return ids;
    }

    private void addWeekendFixture(
        final int editionId,
        final int grandPrixId,
        final int round
    ) {
        final LocalDate start = LocalDate.of(2025, 1, 1)
            .plusDays((long) (round - 1) * 7);
        fixtures.weekend(
            editionId,
            grandPrixId,
            round,
            start,
            start.plusDays(2)
        );
    }

    private static String code(final int index) {
        return "%c%c%c".formatted(
            (char) ('A' + (index / (26 * 26)) % 26),
            (char) ('A' + (index / 26) % 26),
            (char) ('A' + index % 26)
        );
    }
}
