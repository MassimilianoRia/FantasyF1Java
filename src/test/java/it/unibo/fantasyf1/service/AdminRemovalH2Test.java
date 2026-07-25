package it.unibo.fantasyf1.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unibo.fantasyf1.error.AppException;
import it.unibo.fantasyf1.error.ErrorCode;
import it.unibo.fantasyf1.scoring.SimpleScoringPolicy;
import it.unibo.fantasyf1.security.Pbkdf2PasswordHasher;
import it.unibo.fantasyf1.session.SessionManager;
import it.unibo.fantasyf1.testutil.TestDatabase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class AdminRemovalH2Test {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-07-23T12:00:00Z"),
        ZoneOffset.UTC
    );

    private TestDatabase database;
    private AdminService admin;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();
        admin = new ApplicationServices(
            database,
            CLOCK,
            new Pbkdf2PasswordHasher(),
            new SimpleScoringPolicy(),
            new SessionManager()
        ).admin();
    }

    @Test
    void populationCanBeRemovedCompletelyInReverseDependencyOrder() {
        final Population population = createPopulation();

        assertConflict(() -> admin.removeEdition(population.editionId()));
        assertConflict(() -> admin.removeGrandPrix(population.grandPrixId()));
        assertConflict(() -> admin.removeWeekend(
            population.editionId(),
            population.grandPrixId()
        ));
        assertConflict(() -> admin.removeConstructor(
            population.constructorId()
        ));
        assertConflict(() -> admin.removeConstructorEnrollment(
            population.editionId(),
            population.constructorId()
        ));
        assertConflict(() -> admin.removeDriver(population.driverId()));
        assertConflict(() -> admin.removeDriverEnrollment(
            population.editionId(),
            population.driverId()
        ));

        admin.removePerformance(
            population.editionId(),
            population.grandPrixId(),
            population.driverId()
        );
        admin.removeDriverEnrollment(
            population.editionId(),
            population.driverId()
        );
        admin.removeDriver(population.driverId());
        admin.removeConstructorEnrollment(
            population.editionId(),
            population.constructorId()
        );
        admin.removeConstructor(population.constructorId());
        admin.removeWeekend(
            population.editionId(),
            population.grandPrixId()
        );
        admin.removeGrandPrix(population.grandPrixId());
        admin.removeEdition(population.editionId());

        assertAll(
            () -> assertEquals(
                0,
                database.queryInt("SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND")
            ),
            () -> assertEquals(
                0,
                database.queryInt("SELECT COUNT(*) FROM PILOTA_ISCRITTO")
            ),
            () -> assertEquals(
                0,
                database.queryInt("SELECT COUNT(*) FROM PILOTA")
            ),
            () -> assertEquals(
                0,
                database.queryInt("SELECT COUNT(*) FROM SCUDERIA_ISCRITTA")
            ),
            () -> assertEquals(
                0,
                database.queryInt("SELECT COUNT(*) FROM SCUDERIA")
            ),
            () -> assertEquals(
                0,
                database.queryInt("SELECT COUNT(*) FROM WEEKEND_DI_GARA")
            ),
            () -> assertEquals(
                0,
                database.queryInt("SELECT COUNT(*) FROM GRAN_PREMIO")
            ),
            () -> assertEquals(
                0,
                database.queryInt("SELECT COUNT(*) FROM EDIZIONE")
            )
        );
    }

    @Test
    void concludedWeekendMustBeReopenedBeforeRemovingPerformance() {
        final Population population = createPopulation();
        admin.concludeWeekend(
            population.editionId(),
            population.grandPrixId()
        );

        assertConflict(() -> admin.removePerformance(
            population.editionId(),
            population.grandPrixId(),
            population.driverId()
        ));

        admin.reopenWeekend(
            population.editionId(),
            population.grandPrixId()
        );
        admin.removePerformance(
            population.editionId(),
            population.grandPrixId(),
            population.driverId()
        );

        assertEquals(
            0,
            database.queryInt("SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND")
        );
    }

    @Test
    void anagraphicEntryCannotBeRemovedWhileUsedByAnotherEdition() {
        final int firstEdition = admin.createEdition(1, 2025);
        admin.createEdition(2, 2026);
        final int grandPrix = admin.createGrandPrix(
            "Gran Premio condiviso",
            "Circuito",
            "Italia",
            "Imola"
        );
        admin.addWeekend(
            firstEdition,
            grandPrix,
            1,
            LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 5, 3)
        );

        assertConflict(() -> admin.removeGrandPrix(grandPrix));
        assertEquals(
            1,
            database.queryInt("SELECT COUNT(*) FROM GRAN_PREMIO")
        );
    }

    private Population createPopulation() {
        final int edition = admin.createEdition(1, 2025);
        final int grandPrix = admin.createGrandPrix(
            "Gran Premio reversibile",
            "Circuito reversibile",
            "Italia",
            "Imola"
        );
        admin.addWeekend(
            edition,
            grandPrix,
            1,
            LocalDate.of(2025, 5, 1),
            LocalDate.of(2025, 5, 3)
        );
        final int constructor = admin.createConstructor(
            "Scuderia reversibile"
        );
        admin.enrollConstructor(
            edition,
            constructor,
            "Scuderia reversibile F1",
            "REV-01"
        );
        final int driver = admin.createDriver(
            "Ada",
            "Lovelace",
            "Britannica",
            LocalDate.of(1990, 12, 10)
        );
        admin.enrollDriver(edition, driver, "ADA", 7, constructor);
        admin.recordPerformance(new PerformanceRequest(
            edition,
            grandPrix,
            driver,
            1,
            1,
            false,
            true
        ));
        return new Population(edition, grandPrix, constructor, driver);
    }

    private static void assertConflict(final Runnable operation) {
        final AppException exception = assertThrows(
            AppException.class,
            operation::run
        );
        assertEquals(ErrorCode.CONFLICT, exception.code());
    }

    private record Population(
        int editionId,
        int grandPrixId,
        int constructorId,
        int driverId
    ) {
    }
}
