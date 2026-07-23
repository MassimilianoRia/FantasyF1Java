package it.unibo.fantasyf1.model.dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unibo.fantasyf1.testutil.TestDatabase;
import it.unibo.fantasyf1.testutil.TestFixtures;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DatabaseConstraintsH2Test {

    private TestDatabase database;
    private TestFixtures fixtures;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();
        fixtures = new TestFixtures(database);
    }

    @Test
    void uniqueUsernameEmailAndTeamNameContextAreEnforced() {
        final int editionId = fixtures.edition(1, 2025);
        final int ownerId = fixtures.user(
            "unique.user",
            "hash",
            "unique@example.test"
        );
        fixtures.team("Unique Team", 0, ownerId, editionId);

        assertThrows(SQLException.class, () -> execute(
            """
            INSERT INTO UTENTE
                (Nome, Cognome, Username, PasswordHash, Email, Telefono)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            "Other",
            "User",
            "unique.user",
            "hash",
            "other@example.test",
            "+39 333 1111111"
        ));
        assertThrows(SQLException.class, () -> execute(
            """
            INSERT INTO UTENTE
                (Nome, Cognome, Username, PasswordHash, Email, Telefono)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            "Other",
            "User",
            "other.user",
            "hash",
            "unique@example.test",
            "+39 333 1111111"
        ));
        assertThrows(SQLException.class, () -> execute(
            """
            INSERT INTO TEAM_FANTASY
                (Nome, PunteggioTotale, IdUtente, IdEdizione)
            VALUES (?, ?, ?, ?)
            """,
            "Unique Team",
            0,
            ownerId,
            editionId
        ));

        assertEquals(1, database.queryInt("SELECT COUNT(*) FROM UTENTE"));
        assertEquals(
            1,
            database.queryInt("SELECT COUNT(*) FROM TEAM_FANTASY")
        );
    }

    @Test
    void weekendAndPerformanceChecksRejectOutOfRangeData() {
        final int editionId = fixtures.edition(1, 2025);
        final int grandPrixId = fixtures.grandPrix("Checks");
        final int constructorId = fixtures.racingConstructor("Checks");
        fixtures.enrollConstructor(editionId, constructorId, "Checks");
        final int driverId = fixtures.driver("Checks");
        fixtures.enrollDriver(
            editionId,
            driverId,
            "CHK",
            1,
            constructorId
        );

        assertThrows(SQLException.class, () -> execute(
            """
            INSERT INTO WEEKEND_DI_GARA
                (IdEdizione, IdGranPremio, NumeroRound, DataInizio, DataFine)
            VALUES (?, ?, ?, ?, ?)
            """,
            editionId,
            grandPrixId,
            0,
            Date.valueOf(LocalDate.of(2025, 1, 2)),
            Date.valueOf(LocalDate.of(2025, 1, 3))
        ));
        assertThrows(SQLException.class, () -> execute(
            """
            INSERT INTO WEEKEND_DI_GARA
                (IdEdizione, IdGranPremio, NumeroRound, DataInizio, DataFine)
            VALUES (?, ?, ?, ?, ?)
            """,
            editionId,
            grandPrixId,
            1,
            Date.valueOf(LocalDate.of(2025, 1, 3)),
            Date.valueOf(LocalDate.of(2025, 1, 2))
        ));

        fixtures.weekend(
            editionId,
            grandPrixId,
            1,
            LocalDate.of(2025, 1, 2),
            LocalDate.of(2025, 1, 3)
        );
        assertThrows(SQLException.class, () -> execute(
            """
            INSERT INTO PRESTAZIONE_WEEKEND
                (IdGranPremio, IdEdizione, IdPilota,
                 PosizionamentoQualifica, PosizionamentoGara,
                 Penalizzato, RegistraGiroVeloce, PunteggioFantasy)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """,
            grandPrixId,
            editionId,
            driverId,
            21,
            0,
            false,
            false,
            0
        ));
        assertEquals(
            0,
            database.queryInt("SELECT COUNT(*) FROM PRESTAZIONE_WEEKEND")
        );
    }

    @Test
    void compositeForeignKeysPreventCrossEditionComposition() {
        final int firstEdition = fixtures.edition(1, 2025);
        final int secondEdition = fixtures.edition(2, 2024);
        final int ownerId = fixtures.user("fk.owner", "hash");
        final int constructorId = fixtures.racingConstructor("FK");
        fixtures.enrollConstructor(
            secondEdition,
            constructorId,
            "FK"
        );
        final int driverId = fixtures.driver("FK");
        fixtures.enrollDriver(
            secondEdition,
            driverId,
            "FKK",
            1,
            constructorId
        );
        final int teamId = fixtures.team(
            "First edition team",
            0,
            ownerId,
            firstEdition
        );

        assertThrows(SQLException.class, () -> execute(
            """
            INSERT INTO COMPOSIZIONE_TEAM
                (IdTeam, IdEdizione, IdPilota)
            VALUES (?, ?, ?)
            """,
            teamId,
            firstEdition,
            driverId
        ));
        assertEquals(
            0,
            database.queryInt("SELECT COUNT(*) FROM COMPOSIZIONE_TEAM")
        );
    }

    private int execute(final String sql, final Object... parameters)
        throws SQLException {
        try (
            Connection connection = database.open();
            PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            for (int index = 0; index < parameters.length; index++) {
                statement.setObject(index + 1, parameters[index]);
            }
            return statement.executeUpdate();
        }
    }
}
