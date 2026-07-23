package it.unibo.fantasyf1.testutil;

import java.sql.Date;
import java.time.LocalDate;

/**
 * Fixture minime create esclusivamente con statement preparati.
 */
public final class TestFixtures {

    private final TestDatabase database;

    public TestFixtures(final TestDatabase database) {
        this.database = database;
    }

    public int edition(final int number, final int year) {
        return database.insert(
            "INSERT INTO EDIZIONE (NumeroEdizione, Anno) VALUES (?, ?)",
            number,
            year
        );
    }

    public int user(final String username, final String passwordHash) {
        return user(username, passwordHash, username + "@example.test");
    }

    public int user(
        final String username,
        final String passwordHash,
        final String email
    ) {
        return database.insert(
            """
            INSERT INTO UTENTE
                (Nome, Cognome, Username, PasswordHash, Email, Telefono)
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            "Nome",
            "Cognome",
            username,
            passwordHash,
            email,
            "+39 333 0000000"
        );
    }

    public int grandPrix(final String suffix) {
        return database.insert(
            """
            INSERT INTO GRAN_PREMIO
                (Nome, Circuito, Nazione, Città)
            VALUES (?, ?, ?, ?)
            """,
            "Gran Premio " + suffix,
            "Circuito " + suffix,
            "Italia",
            "Città " + suffix
        );
    }

    public int racingConstructor(final String suffix) {
        return database.insert(
            "INSERT INTO SCUDERIA (Nome) VALUES (?)",
            "Scuderia " + suffix
        );
    }

    public void enrollConstructor(
        final int editionId,
        final int constructorId,
        final String suffix
    ) {
        database.update(
            """
            INSERT INTO SCUDERIA_ISCRITTA
                (IdEdizione, IdScuderia, NomeIscrizione, NomeVettura)
            VALUES (?, ?, ?, ?)
            """,
            editionId,
            constructorId,
            "Iscrizione " + suffix,
            "Vettura " + suffix
        );
    }

    public int driver(final String suffix) {
        return database.insert(
            """
            INSERT INTO PILOTA
                (Nome, Cognome, Nazionalità, DataNascita)
            VALUES (?, ?, ?, ?)
            """,
            "Nome" + suffix,
            "Cognome" + suffix,
            "Italiana",
            Date.valueOf(LocalDate.of(1990, 1, 1))
        );
    }

    public void enrollDriver(
        final int editionId,
        final int driverId,
        final String code,
        final int raceNumber,
        final int constructorId
    ) {
        database.update(
            """
            INSERT INTO PILOTA_ISCRITTO
                (IdEdizione, IdPilota, SiglaGara, NumeroInGara, IdScuderia)
            VALUES (?, ?, ?, ?, ?)
            """,
            editionId,
            driverId,
            code,
            raceNumber,
            constructorId
        );
    }

    public void weekend(
        final int editionId,
        final int grandPrixId,
        final int round,
        final LocalDate startDate,
        final LocalDate endDate
    ) {
        database.update(
            """
            INSERT INTO WEEKEND_DI_GARA
                (IdEdizione, IdGranPremio, NumeroRound, DataInizio, DataFine)
            VALUES (?, ?, ?, ?, ?)
            """,
            editionId,
            grandPrixId,
            round,
            Date.valueOf(startDate),
            Date.valueOf(endDate)
        );
    }

    public int team(
        final String name,
        final int totalPoints,
        final int ownerId,
        final int editionId
    ) {
        return database.insert(
            """
            INSERT INTO TEAM_FANTASY
                (Nome, PunteggioTotale, IdUtente, IdEdizione)
            VALUES (?, ?, ?, ?)
            """,
            name,
            totalPoints,
            ownerId,
            editionId
        );
    }

    public void component(
        final int teamId,
        final int editionId,
        final int driverId
    ) {
        database.update(
            """
            INSERT INTO COMPOSIZIONE_TEAM
                (IdTeam, IdEdizione, IdPilota)
            VALUES (?, ?, ?)
            """,
            teamId,
            editionId,
            driverId
        );
    }

    public int league(
        final String name,
        final int administratorId,
        final int editionId
    ) {
        return database.insert(
            """
            INSERT INTO LEGA (Nome, IdUtente, IdEdizione)
            VALUES (?, ?, ?)
            """,
            name,
            administratorId,
            editionId
        );
    }

    public void participation(final int leagueId, final int teamId) {
        database.update(
            """
            INSERT INTO PARTECIPAZIONE_TEAM (IdLega, IdTeam)
            VALUES (?, ?)
            """,
            leagueId,
            teamId
        );
    }

    public void performance(
        final int editionId,
        final int grandPrixId,
        final int driverId,
        final Integer qualifyingPosition,
        final Integer racePosition,
        final boolean penalized,
        final boolean fastestLap,
        final Integer fantasyPoints
    ) {
        database.update(
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
            qualifyingPosition,
            racePosition,
            penalized,
            fastestLap,
            fantasyPoints
        );
    }
}
