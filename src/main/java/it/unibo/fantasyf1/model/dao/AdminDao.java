package it.unibo.fantasyf1.model.dao;

import it.unibo.fantasyf1.model.ConstructorOption;
import it.unibo.fantasyf1.model.DriverRegistryOption;
import it.unibo.fantasyf1.model.EditionStatus;
import it.unibo.fantasyf1.model.EnrolledConstructorOption;
import it.unibo.fantasyf1.model.GrandPrixOption;
import it.unibo.fantasyf1.model.RaceWeekend;
import it.unibo.fantasyf1.model.WeekendPerformanceStatus;
import it.unibo.fantasyf1.scoring.PerformanceData;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Operazioni JDBC riservate all'area amministrativa trusted.
 *
 * <p>I limiti aggregati non sono nascosti nel DAO: il service blocca prima
 * l'edizione, interroga i conteggi e soltanto dopo invoca gli inserimenti.</p>
 */
public final class AdminDao {

    private static final String LOCK_EDITION = """
        SELECT IdEdizione
        FROM EDIZIONE
        WHERE IdEdizione = ?
        FOR UPDATE
        """;

    private static final String INSERT_EDITION = """
        INSERT INTO EDIZIONE (NumeroEdizione, Anno)
        VALUES (?, ?)
        """;

    private static final String INSERT_GRAND_PRIX = """
        INSERT INTO GRAN_PREMIO (Nome, Circuito, Nazione, `Città`)
        VALUES (?, ?, ?, ?)
        """;

    private static final String INSERT_WEEKEND = """
        INSERT INTO WEEKEND_DI_GARA
            (IdEdizione, IdGranPremio, NumeroRound, DataInizio, DataFine)
        VALUES (?, ?, ?, ?, ?)
        """;

    private static final String INSERT_CONSTRUCTOR = """
        INSERT INTO SCUDERIA (Nome)
        VALUES (?)
        """;

    private static final String ENROLL_CONSTRUCTOR = """
        INSERT INTO SCUDERIA_ISCRITTA
            (IdEdizione, IdScuderia, NomeIscrizione, NomeVettura)
        VALUES (?, ?, ?, ?)
        """;

    private static final String INSERT_DRIVER = """
        INSERT INTO PILOTA (Nome, Cognome, `Nazionalità`, DataNascita)
        VALUES (?, ?, ?, ?)
        """;

    private static final String ENROLL_DRIVER = """
        INSERT INTO PILOTA_ISCRITTO
            (IdEdizione, IdPilota, SiglaGara, NumeroInGara, IdScuderia)
        VALUES (?, ?, ?, ?, ?)
        """;

    private static final String UPSERT_PERFORMANCE = """
        INSERT INTO PRESTAZIONE_WEEKEND
            (IdGranPremio, IdEdizione, IdPilota,
             PosizionamentoQualifica, PosizionamentoGara,
             Penalizzato, RegistraGiroVeloce, PunteggioFantasy)
        SELECT
            W.IdGranPremio,
            W.IdEdizione,
            ?,
            ?, ?, ?, ?, NULL
        FROM WEEKEND_DI_GARA W
        WHERE W.IdEdizione = ?
          AND W.IdGranPremio = ?
          AND W.Concluso = FALSE
        ON DUPLICATE KEY UPDATE
            PosizionamentoQualifica = VALUES(PosizionamentoQualifica),
            PosizionamentoGara = VALUES(PosizionamentoGara),
            Penalizzato = VALUES(Penalizzato),
            RegistraGiroVeloce = VALUES(RegistraGiroVeloce),
            PunteggioFantasy = NULL
        """;

    private static final String DELETE_EDITION = """
        DELETE FROM EDIZIONE
        WHERE IdEdizione = ?
        """;

    private static final String DELETE_GRAND_PRIX = """
        DELETE FROM GRAN_PREMIO
        WHERE IdGranPremio = ?
        """;

    private static final String DELETE_WEEKEND = """
        DELETE FROM WEEKEND_DI_GARA
        WHERE IdEdizione = ? AND IdGranPremio = ?
        """;

    private static final String DELETE_CONSTRUCTOR = """
        DELETE FROM SCUDERIA
        WHERE IdScuderia = ?
        """;

    private static final String DELETE_CONSTRUCTOR_ENROLLMENT = """
        DELETE FROM SCUDERIA_ISCRITTA
        WHERE IdEdizione = ? AND IdScuderia = ?
        """;

    private static final String DELETE_DRIVER = """
        DELETE FROM PILOTA
        WHERE IdPilota = ?
        """;

    private static final String DELETE_DRIVER_ENROLLMENT = """
        DELETE FROM PILOTA_ISCRITTO
        WHERE IdEdizione = ? AND IdPilota = ?
        """;

    private static final String DELETE_PERFORMANCE = """
        DELETE FROM PRESTAZIONE_WEEKEND
        WHERE IdEdizione = ?
          AND IdGranPremio = ?
          AND IdPilota = ?
        """;

    private static final String FIND_WEEKEND_CONCLUSION = """
        SELECT Concluso
        FROM WEEKEND_DI_GARA
        WHERE IdEdizione = ? AND IdGranPremio = ?
        """;

    private static final String LOCK_WEEKEND_CONCLUSION =
        FIND_WEEKEND_CONCLUSION + " FOR UPDATE";

    private static final String CONCLUDE_WEEKEND = """
        UPDATE WEEKEND_DI_GARA
        SET Concluso = TRUE
        WHERE IdEdizione = ?
          AND IdGranPremio = ?
          AND Concluso = FALSE
        """;

    private static final String REOPEN_WEEKEND = """
        UPDATE WEEKEND_DI_GARA
        SET Concluso = FALSE
        WHERE IdEdizione = ?
          AND IdGranPremio = ?
          AND Concluso = TRUE
        """;

    private static final String FIND_GRAND_PRIX = """
        SELECT IdGranPremio, Nome, Circuito, Nazione, `Città`
        FROM GRAN_PREMIO
        ORDER BY Nome
        """;

    private static final String FIND_CONSTRUCTORS = """
        SELECT IdScuderia, Nome
        FROM SCUDERIA
        ORDER BY Nome
        """;

    private static final String FIND_ENROLLED_CONSTRUCTORS = """
        SELECT IdEdizione, IdScuderia, NomeIscrizione, NomeVettura
        FROM SCUDERIA_ISCRITTA
        WHERE IdEdizione = ?
        ORDER BY NomeIscrizione
        """;

    private static final String FIND_DRIVERS = """
        SELECT IdPilota, Nome, Cognome, `Nazionalità`, DataNascita
        FROM PILOTA
        ORDER BY Cognome, Nome
        """;

    private static final String FIND_WEEKENDS = """
        SELECT
            W.IdEdizione,
            W.IdGranPremio,
            W.NumeroRound,
            G.Nome,
            W.DataInizio,
            W.DataFine,
            W.Concluso
        FROM WEEKEND_DI_GARA W
        JOIN GRAN_PREMIO G ON G.IdGranPremio = W.IdGranPremio
        WHERE W.IdEdizione = ?
        ORDER BY W.NumeroRound
        """;

    private static final String FIND_WEEKEND_PERFORMANCE_STATUS = """
        SELECT
            W.IdEdizione,
            W.IdGranPremio,
            PI.IdPilota,
            P.Nome,
            P.Cognome,
            PI.SiglaGara,
            SI.NomeIscrizione,
            PW.IdPilota AS IdPrestazione,
            PW.PosizionamentoQualifica,
            PW.PosizionamentoGara,
            PW.Penalizzato,
            PW.RegistraGiroVeloce,
            PW.PunteggioFantasy
        FROM WEEKEND_DI_GARA W
        JOIN PILOTA_ISCRITTO PI
          ON PI.IdEdizione = W.IdEdizione
        JOIN PILOTA P
          ON P.IdPilota = PI.IdPilota
        JOIN SCUDERIA_ISCRITTA SI
          ON SI.IdEdizione = PI.IdEdizione
         AND SI.IdScuderia = PI.IdScuderia
        LEFT JOIN PRESTAZIONE_WEEKEND PW
          ON PW.IdEdizione = W.IdEdizione
         AND PW.IdGranPremio = W.IdGranPremio
         AND PW.IdPilota = PI.IdPilota
        WHERE W.IdEdizione = ?
          AND W.IdGranPremio = ?
        ORDER BY P.Cognome, P.Nome
        """;

    public boolean lockEdition(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(LOCK_EDITION)) {
            statement.setInt(1, editionId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public int insertEdition(
        final Connection connection,
        final int number,
        final int year
    ) throws SQLException {
        return insertReturningKey(connection, INSERT_EDITION, statement -> {
            statement.setInt(1, number);
            statement.setInt(2, year);
        });
    }

    public int insertGrandPrix(
        final Connection connection,
        final String name,
        final String circuit,
        final String country,
        final String city
    ) throws SQLException {
        return insertReturningKey(
            connection,
            INSERT_GRAND_PRIX,
            statement -> {
                statement.setString(1, name);
                statement.setString(2, circuit);
                statement.setString(3, country);
                statement.setString(4, city);
            }
        );
    }

    public void insertWeekend(
        final Connection connection,
        final int editionId,
        final int grandPrixId,
        final int round,
        final LocalDate startDate,
        final LocalDate endDate
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(INSERT_WEEKEND)) {
            statement.setInt(1, editionId);
            statement.setInt(2, grandPrixId);
            statement.setInt(3, round);
            statement.setDate(4, Date.valueOf(startDate));
            statement.setDate(5, Date.valueOf(endDate));
            requireSingleUpdate(statement, "Inserimento weekend non riuscito");
        }
    }

    public int insertConstructor(
        final Connection connection,
        final String name
    ) throws SQLException {
        return insertReturningKey(
            connection,
            INSERT_CONSTRUCTOR,
            statement -> statement.setString(1, name)
        );
    }

    public void enrollConstructor(
        final Connection connection,
        final int editionId,
        final int constructorId,
        final String registeredName,
        final String carName
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(ENROLL_CONSTRUCTOR)) {
            statement.setInt(1, editionId);
            statement.setInt(2, constructorId);
            statement.setString(3, registeredName);
            statement.setString(4, carName);
            requireSingleUpdate(
                statement,
                "Iscrizione scuderia non riuscita"
            );
        }
    }

    public int insertDriver(
        final Connection connection,
        final String firstName,
        final String lastName,
        final String nationality,
        final LocalDate birthDate
    ) throws SQLException {
        return insertReturningKey(connection, INSERT_DRIVER, statement -> {
            statement.setString(1, firstName);
            statement.setString(2, lastName);
            statement.setString(3, nationality);
            statement.setDate(4, Date.valueOf(birthDate));
        });
    }

    public void enrollDriver(
        final Connection connection,
        final int editionId,
        final int driverId,
        final String code,
        final int raceNumber,
        final int constructorId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(ENROLL_DRIVER)) {
            statement.setInt(1, editionId);
            statement.setInt(2, driverId);
            statement.setString(3, code);
            statement.setInt(4, raceNumber);
            statement.setInt(5, constructorId);
            requireSingleUpdate(statement, "Iscrizione pilota non riuscita");
        }
    }

    public void upsertPerformance(
        final Connection connection,
        final int editionId,
        final int grandPrixId,
        final int driverId,
        final PerformanceData data
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(UPSERT_PERFORMANCE)) {
            statement.setInt(1, driverId);
            setNullableInt(statement, 2, data.qualifyingPosition());
            setNullableInt(statement, 3, data.racePosition());
            statement.setBoolean(4, data.penalized());
            statement.setBoolean(5, data.fastestLap());
            statement.setInt(6, editionId);
            statement.setInt(7, grandPrixId);
            statement.executeUpdate();
        }
    }

    public void deleteEdition(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        deleteOne(
            connection,
            DELETE_EDITION,
            "Edizione non trovata durante la rimozione",
            editionId
        );
    }

    public void deleteGrandPrix(
        final Connection connection,
        final int grandPrixId
    ) throws SQLException {
        deleteOne(
            connection,
            DELETE_GRAND_PRIX,
            "Gran Premio non trovato durante la rimozione",
            grandPrixId
        );
    }

    public void deleteWeekend(
        final Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        deleteOne(
            connection,
            DELETE_WEEKEND,
            "Weekend non trovato durante la rimozione",
            editionId,
            grandPrixId
        );
    }

    public void deleteConstructor(
        final Connection connection,
        final int constructorId
    ) throws SQLException {
        deleteOne(
            connection,
            DELETE_CONSTRUCTOR,
            "Scuderia non trovata durante la rimozione",
            constructorId
        );
    }

    public void deleteConstructorEnrollment(
        final Connection connection,
        final int editionId,
        final int constructorId
    ) throws SQLException {
        deleteOne(
            connection,
            DELETE_CONSTRUCTOR_ENROLLMENT,
            "Iscrizione della scuderia non trovata durante la rimozione",
            editionId,
            constructorId
        );
    }

    public void deleteDriver(
        final Connection connection,
        final int driverId
    ) throws SQLException {
        deleteOne(
            connection,
            DELETE_DRIVER,
            "Pilota non trovato durante la rimozione",
            driverId
        );
    }

    public void deleteDriverEnrollment(
        final Connection connection,
        final int editionId,
        final int driverId
    ) throws SQLException {
        deleteOne(
            connection,
            DELETE_DRIVER_ENROLLMENT,
            "Iscrizione del pilota non trovata durante la rimozione",
            editionId,
            driverId
        );
    }

    public void deletePerformance(
        final Connection connection,
        final int editionId,
        final int grandPrixId,
        final int driverId
    ) throws SQLException {
        deleteOne(
            connection,
            DELETE_PERFORMANCE,
            "Prestazione non trovata durante la rimozione",
            editionId,
            grandPrixId,
            driverId
        );
    }

    public int countWeekends(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        return count(connection, """
            SELECT COUNT(*) FROM WEEKEND_DI_GARA WHERE IdEdizione = ?
            """, editionId);
    }

    public int countConstructors(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        return count(connection, """
            SELECT COUNT(*) FROM SCUDERIA_ISCRITTA WHERE IdEdizione = ?
            """, editionId);
    }

    public int countDrivers(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        return count(connection, """
            SELECT COUNT(*) FROM PILOTA_ISCRITTO WHERE IdEdizione = ?
            """, editionId);
    }

    public int countDriversForConstructor(
        final Connection connection,
        final int editionId,
        final int constructorId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT COUNT(*)
            FROM PILOTA_ISCRITTO
            WHERE IdEdizione = ? AND IdScuderia = ?
            """)) {
            statement.setInt(1, editionId);
            statement.setInt(2, constructorId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    public int countGrandPrixWeekends(
        final Connection connection,
        final int grandPrixId
    ) throws SQLException {
        return count(connection, """
            SELECT COUNT(*)
            FROM WEEKEND_DI_GARA
            WHERE IdGranPremio = ?
            """, grandPrixId);
    }

    public int countConstructorEnrollments(
        final Connection connection,
        final int constructorId
    ) throws SQLException {
        return count(connection, """
            SELECT COUNT(*)
            FROM SCUDERIA_ISCRITTA
            WHERE IdScuderia = ?
            """, constructorId);
    }

    public int countDriverEnrollments(
        final Connection connection,
        final int driverId
    ) throws SQLException {
        return count(connection, """
            SELECT COUNT(*)
            FROM PILOTA_ISCRITTO
            WHERE IdPilota = ?
            """, driverId);
    }

    public int countWeekendPerformances(
        final Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        return count(
            connection,
            """
            SELECT COUNT(*)
            FROM PRESTAZIONE_WEEKEND
            WHERE IdEdizione = ? AND IdGranPremio = ?
            """,
            editionId,
            grandPrixId
        );
    }

    public int countDriverPerformances(
        final Connection connection,
        final int editionId,
        final int driverId
    ) throws SQLException {
        return count(
            connection,
            """
            SELECT COUNT(*)
            FROM PRESTAZIONE_WEEKEND
            WHERE IdEdizione = ? AND IdPilota = ?
            """,
            editionId,
            driverId
        );
    }

    public int countDriverTeamSelections(
        final Connection connection,
        final int editionId,
        final int driverId
    ) throws SQLException {
        return count(
            connection,
            """
            SELECT COUNT(*)
            FROM COMPOSIZIONE_TEAM
            WHERE IdEdizione = ? AND IdPilota = ?
            """,
            editionId,
            driverId
        );
    }

    public int countEditionTeams(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        return count(connection, """
            SELECT COUNT(*)
            FROM TEAM_FANTASY
            WHERE IdEdizione = ?
            """, editionId);
    }

    public int countEditionLeagues(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        return count(connection, """
            SELECT COUNT(*)
            FROM LEGA
            WHERE IdEdizione = ?
            """, editionId);
    }

    public boolean isGrandPrixPresent(
        final Connection connection,
        final int grandPrixId
    ) throws SQLException {
        return exists(connection, """
            SELECT 1 FROM GRAN_PREMIO WHERE IdGranPremio = ?
            """, grandPrixId);
    }

    public boolean isConstructorPresent(
        final Connection connection,
        final int constructorId
    ) throws SQLException {
        return exists(connection, """
            SELECT 1 FROM SCUDERIA WHERE IdScuderia = ?
            """, constructorId);
    }

    public boolean isDriverPresent(
        final Connection connection,
        final int driverId
    ) throws SQLException {
        return exists(connection, """
            SELECT 1 FROM PILOTA WHERE IdPilota = ?
            """, driverId);
    }

    public boolean isConstructorEnrolled(
        final Connection connection,
        final int editionId,
        final int constructorId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT 1
            FROM SCUDERIA_ISCRITTA
            WHERE IdEdizione = ? AND IdScuderia = ?
            """)) {
            statement.setInt(1, editionId);
            statement.setInt(2, constructorId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public boolean isDriverEnrolled(
        final Connection connection,
        final int editionId,
        final int driverId
    ) throws SQLException {
        return exists(
            connection,
            """
            SELECT 1
            FROM PILOTA_ISCRITTO
            WHERE IdEdizione = ? AND IdPilota = ?
            """,
            editionId,
            driverId
        );
    }

    public boolean isPerformancePresent(
        final Connection connection,
        final int editionId,
        final int grandPrixId,
        final int driverId
    ) throws SQLException {
        return exists(
            connection,
            """
            SELECT 1
            FROM PRESTAZIONE_WEEKEND
            WHERE IdEdizione = ?
              AND IdGranPremio = ?
              AND IdPilota = ?
            """,
            editionId,
            grandPrixId,
            driverId
        );
    }

    public boolean performanceContextExists(
        final Connection connection,
        final int editionId,
        final int grandPrixId,
        final int driverId
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT 1
            FROM WEEKEND_DI_GARA W
            JOIN PILOTA_ISCRITTO PI ON PI.IdEdizione = W.IdEdizione
            WHERE W.IdEdizione = ?
              AND W.IdGranPremio = ?
              AND PI.IdPilota = ?
            """)) {
            statement.setInt(1, editionId);
            statement.setInt(2, grandPrixId);
            statement.setInt(3, driverId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    public Optional<Boolean> findWeekendConclusion(
        final Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        return findWeekendConclusion(
            connection,
            editionId,
            grandPrixId,
            false
        );
    }

    public Optional<Boolean> lockWeekendConclusion(
        final Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        return findWeekendConclusion(
            connection,
            editionId,
            grandPrixId,
            true
        );
    }

    private Optional<Boolean> findWeekendConclusion(
        final Connection connection,
        final int editionId,
        final int grandPrixId,
        final boolean lock
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            lock ? LOCK_WEEKEND_CONCLUSION : FIND_WEEKEND_CONCLUSION
        )) {
            statement.setInt(1, editionId);
            statement.setInt(2, grandPrixId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                    ? Optional.of(result.getBoolean("Concluso"))
                    : Optional.empty();
            }
        }
    }

    public int concludeWeekend(
        final Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(CONCLUDE_WEEKEND)) {
            statement.setInt(1, editionId);
            statement.setInt(2, grandPrixId);
            return statement.executeUpdate();
        }
    }

    public int reopenWeekend(
        final Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        try (PreparedStatement statement =
                 connection.prepareStatement(REOPEN_WEEKEND)) {
            statement.setInt(1, editionId);
            statement.setInt(2, grandPrixId);
            return statement.executeUpdate();
        }
    }

    public List<GrandPrixOption> findGrandPrix(
        final Connection connection
    ) throws SQLException {
        final List<GrandPrixOption> options = new ArrayList<>();
        try (
            PreparedStatement statement =
                connection.prepareStatement(FIND_GRAND_PRIX);
            ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                options.add(new GrandPrixOption(
                    result.getInt("IdGranPremio"),
                    result.getString("Nome"),
                    result.getString("Circuito"),
                    result.getString("Nazione"),
                    result.getString("Città")
                ));
            }
        }
        return List.copyOf(options);
    }

    public List<ConstructorOption> findConstructors(
        final Connection connection
    ) throws SQLException {
        final List<ConstructorOption> options = new ArrayList<>();
        try (
            PreparedStatement statement =
                connection.prepareStatement(FIND_CONSTRUCTORS);
            ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                options.add(new ConstructorOption(
                    result.getInt("IdScuderia"),
                    result.getString("Nome")
                ));
            }
        }
        return List.copyOf(options);
    }

    public List<EnrolledConstructorOption> findEnrolledConstructors(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        final List<EnrolledConstructorOption> options = new ArrayList<>();
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_ENROLLED_CONSTRUCTORS)) {
            statement.setInt(1, editionId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    options.add(new EnrolledConstructorOption(
                        result.getInt("IdEdizione"),
                        result.getInt("IdScuderia"),
                        result.getString("NomeIscrizione"),
                        result.getString("NomeVettura")
                    ));
                }
            }
        }
        return List.copyOf(options);
    }

    public List<DriverRegistryOption> findDrivers(
        final Connection connection
    ) throws SQLException {
        final List<DriverRegistryOption> options = new ArrayList<>();
        try (
            PreparedStatement statement =
                connection.prepareStatement(FIND_DRIVERS);
            ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                options.add(new DriverRegistryOption(
                    result.getInt("IdPilota"),
                    result.getString("Nome"),
                    result.getString("Cognome"),
                    result.getString("Nazionalità"),
                    result.getDate("DataNascita").toLocalDate()
                ));
            }
        }
        return List.copyOf(options);
    }

    public List<RaceWeekend> findWeekends(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        final List<RaceWeekend> weekends = new ArrayList<>();
        try (PreparedStatement statement =
                 connection.prepareStatement(FIND_WEEKENDS)) {
            statement.setInt(1, editionId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    weekends.add(new RaceWeekend(
                        result.getInt("IdEdizione"),
                        result.getInt("IdGranPremio"),
                        result.getInt("NumeroRound"),
                        result.getString("Nome"),
                        result.getDate("DataInizio").toLocalDate(),
                        result.getDate("DataFine").toLocalDate(),
                        result.getBoolean("Concluso")
                    ));
                }
            }
        }
        return List.copyOf(weekends);
    }

    public List<WeekendPerformanceStatus> findWeekendPerformanceStatus(
        final Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws SQLException {
        final List<WeekendPerformanceStatus> statuses = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(
            FIND_WEEKEND_PERFORMANCE_STATUS
        )) {
            statement.setInt(1, editionId);
            statement.setInt(2, grandPrixId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    final boolean recorded = !result.wasNull();
                    statuses.add(new WeekendPerformanceStatus(
                        result.getInt("IdEdizione"),
                        result.getInt("IdGranPremio"),
                        result.getInt("IdPilota"),
                        result.getString("Nome"),
                        result.getString("Cognome"),
                        result.getString("SiglaGara"),
                        result.getString("NomeIscrizione"),
                        recorded,
                        nullableInt(result, "PosizionamentoQualifica"),
                        nullableInt(result, "PosizionamentoGara"),
                        recorded && result.getBoolean("Penalizzato"),
                        recorded && result.getBoolean("RegistraGiroVeloce"),
                        nullableInt(result, "PunteggioFantasy")
                    ));
                }
            }
        }
        return List.copyOf(statuses);
    }

    public EditionStatus editionStatus(
        final Connection connection,
        final int editionId
    ) throws SQLException {
        final int weekends = countWeekends(connection, editionId);
        final int constructors = countConstructors(connection, editionId);
        final int drivers = countDrivers(connection, editionId);
        final int withTwo;
        try (PreparedStatement statement = connection.prepareStatement("""
            SELECT COUNT(*)
            FROM (
                SELECT IdScuderia
                FROM PILOTA_ISCRITTO
                WHERE IdEdizione = ?
                GROUP BY IdScuderia
                HAVING COUNT(*) = 2
            ) DUE_PILOTI
            """)) {
            statement.setInt(1, editionId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                withTwo = result.getInt(1);
            }
        }
        final boolean complete = weekends == 24
            && constructors == 10
            && drivers == 20
            && withTwo == 10;
        return new EditionStatus(
            editionId,
            weekends,
            constructors,
            drivers,
            withTwo,
            complete
        );
    }

    private static int count(
        final Connection connection,
        final String sql,
        final int... ids
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setIds(statement, ids);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static boolean exists(
        final Connection connection,
        final String sql,
        final int... ids
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setIds(statement, ids);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void setNullableInt(
        final PreparedStatement statement,
        final int parameter,
        final Integer value
    ) throws SQLException {
        if (value == null) {
            statement.setNull(parameter, java.sql.Types.INTEGER);
        } else {
            statement.setInt(parameter, value);
        }
    }

    private static Integer nullableInt(
        final ResultSet result,
        final String column
    ) throws SQLException {
        final int value = result.getInt(column);
        return result.wasNull() ? null : value;
    }

    private static void requireSingleUpdate(
        final PreparedStatement statement,
        final String failureMessage
    ) throws SQLException {
        if (statement.executeUpdate() != 1) {
            throw new SQLException(failureMessage);
        }
    }

    private static void deleteOne(
        final Connection connection,
        final String sql,
        final String failureMessage,
        final int... ids
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            setIds(statement, ids);
            requireSingleUpdate(statement, failureMessage);
        }
    }

    private static void setIds(
        final PreparedStatement statement,
        final int... ids
    ) throws SQLException {
        for (int index = 0; index < ids.length; index++) {
            statement.setInt(index + 1, ids[index]);
        }
    }

    private static int insertReturningKey(
        final Connection connection,
        final String sql,
        final StatementBinder binder
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
            sql,
            Statement.RETURN_GENERATED_KEYS
        )) {
            binder.bind(statement);
            requireSingleUpdate(statement, "Inserimento non riuscito");
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Chiave generata non restituita");
                }
                return keys.getInt(1);
            }
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
