package it.unibo.fantasyf1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.scoring.SimpleScoringPolicy;
import it.unibo.fantasyf1.security.Pbkdf2PasswordHasher;
import it.unibo.fantasyf1.session.SessionManager;
import it.unibo.fantasyf1.testutil.TestDatabase;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Walkthrough JDBC delle operazioni amministrative anagrafiche A1–A7.
 */
final class AdminCrudH2Test {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-07-23T12:00:00Z"),
        ZoneOffset.UTC
    );

    @Test
    void a1ThroughA7CreateUpdateAndExposeTheOfficialCatalogs() {
        final TestDatabase database = new TestDatabase();
        final AdminService admin = new ApplicationServices(
            database,
            CLOCK,
            new Pbkdf2PasswordHasher(),
            new SimpleScoringPolicy(),
            new SessionManager()
        ).admin();

        final int editionId = admin.createEdition(1, 2025);
        final List<Edizione> editions = admin.editions();
        assertEquals(1, editions.size());
        assertEquals(editionId, editions.getFirst().id());

        final int grandPrixId = admin.createGrandPrix(
            "Gran Premio Demo",
            "Circuito iniziale",
            "Italia",
            "Imola"
        );
        admin.updateGrandPrix(
            grandPrixId,
            "Gran Premio Demo aggiornato",
            "Circuito aggiornato",
            "Italia",
            "Monza"
        );
        assertEquals(
            "Circuito aggiornato",
            database.queryString(
                "SELECT Circuito FROM GRAN_PREMIO WHERE IdGranPremio = ?",
                grandPrixId
            )
        );
        assertEquals(
            "Gran Premio Demo aggiornato",
            database.queryString(
                "SELECT Nome FROM GRAN_PREMIO WHERE IdGranPremio = ?",
                grandPrixId
            )
        );

        admin.addWeekend(
            editionId,
            grandPrixId,
            1,
            LocalDate.of(2025, 6, 1),
            LocalDate.of(2025, 6, 3)
        );
        final int constructorId = admin.createConstructor("Scuderia Demo");
        admin.enrollConstructor(
            editionId,
            constructorId,
            "Scuderia Demo F1",
            "DF-01"
        );
        final int driverId = admin.createDriver(
            "Ada",
            "Lovelace",
            "Britannica",
            LocalDate.of(1990, 12, 10)
        );
        admin.enrollDriver(
            editionId,
            driverId,
            "ADA",
            7,
            constructorId
        );

        assertEquals(1, admin.grandPrix().size());
        assertEquals(1, admin.constructors().size());
        assertEquals(1, admin.enrolledConstructors(editionId).size());
        assertEquals(1, admin.drivers().size());
        assertEquals(1, admin.enrolledDrivers(editionId).size());
        assertEquals(1, admin.weekends(editionId).size());
        assertFalse(admin.editionStatus(editionId).complete());
        assertTrue(
            database.queryInt(
                "SELECT COUNT(*) FROM PILOTA_ISCRITTO WHERE IdEdizione = ?",
                editionId
            ) == 1
        );
    }
}
