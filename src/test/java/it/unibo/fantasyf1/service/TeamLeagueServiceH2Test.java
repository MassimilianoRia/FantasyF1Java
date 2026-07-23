package it.unibo.fantasyf1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.fantasyf1.error.AppException;
import it.unibo.fantasyf1.error.ErrorCode;
import it.unibo.fantasyf1.model.JoinedLeague;
import it.unibo.fantasyf1.model.LegaDisponibile;
import it.unibo.fantasyf1.model.StandingRow;
import it.unibo.fantasyf1.model.TeamSummary;
import it.unibo.fantasyf1.model.dao.TeamDao;
import it.unibo.fantasyf1.model.database.TransactionManager;
import it.unibo.fantasyf1.scoring.SimpleScoringPolicy;
import it.unibo.fantasyf1.security.Pbkdf2PasswordHasher;
import it.unibo.fantasyf1.session.SessionManager;
import it.unibo.fantasyf1.testutil.TestDatabase;
import it.unibo.fantasyf1.testutil.TestFixtures;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class TeamLeagueServiceH2Test {

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-07-23T12:00:00Z"),
        ZoneOffset.UTC
    );

    private TestDatabase database;
    private TestFixtures fixtures;
    private SessionManager sessions;
    private TeamService teams;
    private LeagueService leagues;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();
        fixtures = new TestFixtures(database);
        sessions = new SessionManager();
        final ApplicationServices services = new ApplicationServices(
            database,
            CLOCK,
            new Pbkdf2PasswordHasher(),
            new SimpleScoringPolicy(),
            sessions
        );
        teams = services.teams();
        leagues = services.leagues();
    }

    @Test
    void u2RejectsThreeFiveDuplicateAndCrossEditionDrivers() {
        final int ownerId = fixtures.user("team.owner", "unused-hash");
        sessions.login(ownerId, "team.owner");
        final Season first = season(1, 2025, 5, "A");
        final Season second = season(2, 2024, 1, "B");

        assertValidation(() -> teams.createTeam(
            "Solo tre",
            first.editionId(),
            first.driverIds().subList(0, 3)
        ));
        assertValidation(() -> teams.createTeam(
            "Cinque",
            first.editionId(),
            first.driverIds()
        ));
        assertValidation(() -> teams.createTeam(
            "Duplicati",
            first.editionId(),
            List.of(
                first.driverIds().get(0),
                first.driverIds().get(1),
                first.driverIds().get(2),
                first.driverIds().get(2)
            )
        ));
        assertValidation(() -> teams.createTeam(
            "Altra edizione",
            first.editionId(),
            List.of(
                first.driverIds().get(0),
                first.driverIds().get(1),
                first.driverIds().get(2),
                second.driverIds().get(0)
            )
        ));

        assertEquals(0, database.queryInt("SELECT COUNT(*) FROM TEAM_FANTASY"));
        assertEquals(
            0,
            database.queryInt("SELECT COUNT(*) FROM COMPOSIZIONE_TEAM")
        );
    }

    @Test
    void u2ReturnsGeneratedKeyAndU3ReturnsExactlyFourDrivers() {
        final int ownerId = fixtures.user("mario", "unused-hash");
        sessions.login(ownerId, "mario");
        final Season season = season(1, 2025, 4, "C");

        final int teamId = teams.createTeam(
            "Pole Position Club",
            season.editionId(),
            season.driverIds()
        );

        assertTrue(teamId > 0);
        assertEquals(
            teamId,
            database.queryInt(
                "SELECT IdTeam FROM TEAM_FANTASY WHERE Nome = ?",
                "Pole Position Club"
            )
        );
        assertEquals(
            4,
            database.queryInt(
                "SELECT COUNT(*) FROM COMPOSIZIONE_TEAM WHERE IdTeam = ?",
                teamId
            )
        );

        final List<TeamSummary> owned = teams.myTeams(season.editionId());
        assertEquals(1, owned.size());
        assertEquals(teamId, owned.getFirst().id());
        assertEquals(4, owned.getFirst().drivers().size());
        assertEquals(0, owned.getFirst().totalPoints());
    }

    @Test
    void generatedTeamAndPartialCompositionAreRolledBackOnJdbcFailure() {
        final int ownerId = fixtures.user("rollback.owner", "unused-hash");
        final Season season = season(1, 2025, 3, "D");
        final TransactionManager transactions = new TransactionManager(database);
        final TeamDao teamDao = new TeamDao();

        final AppException failure = assertThrows(
            AppException.class,
            () -> transactions.inTransaction(connection -> {
                final int generatedId = teamDao.insertTeam(
                    connection,
                    "Team da annullare",
                    ownerId,
                    season.editionId()
                );
                assertTrue(generatedId > 0);
                teamDao.insertComponents(
                    connection,
                    generatedId,
                    season.editionId(),
                    List.of(
                        season.driverIds().get(0),
                        season.driverIds().get(1),
                        season.driverIds().get(2),
                        999_999
                    )
                );
                return generatedId;
            })
        );

        assertEquals(ErrorCode.CONSTRAINT_VIOLATION, failure.code());
        assertEquals(0, database.queryInt("SELECT COUNT(*) FROM TEAM_FANTASY"));
        assertEquals(
            0,
            database.queryInt("SELECT COUNT(*) FROM COMPOSIZIONE_TEAM")
        );
    }

    @Test
    void u4AndU5CreateAndFilterLeaguesByEdition() {
        final int ownerId = fixtures.user("league.admin", "unused-hash");
        sessions.login(ownerId, "league.admin");
        final int edition2025 = fixtures.edition(1, 2025);
        final int edition2024 = fixtures.edition(2, 2024);

        final int zeta = leagues.createLeague("Zeta League", edition2025);
        final int alpha = leagues.createLeague("Alpha League", edition2025);
        leagues.createLeague("Storica", edition2024);

        final List<LegaDisponibile> available =
            leagues.availableLeagues(edition2025);
        assertEquals(2, available.size());
        assertEquals(List.of("Alpha League", "Zeta League"), available.stream()
            .map(LegaDisponibile::nome)
            .toList());
        assertNotEquals(zeta, alpha);
        assertTrue(zeta > 0 && alpha > 0);
    }

    @Test
    void u6EnforcesOwnershipEditionAndOneTeamPerOwnerButAllowsCreator()
        throws Exception {
        final int ownerId = fixtures.user("owner", "unused-hash");
        final int otherId = fixtures.user("other", "unused-hash");
        sessions.login(ownerId, "owner");
        final Season current = season(1, 2025, 4, "E");
        final Season historic = season(2, 2024, 4, "F");

        final int firstTeam = completeTeam(
            "First",
            ownerId,
            current,
            0
        );
        final int secondTeam = completeTeam(
            "Second",
            ownerId,
            current,
            0
        );
        final int otherTeam = completeTeam(
            "Other",
            otherId,
            current,
            0
        );
        final int historicTeam = completeTeam(
            "Historic",
            ownerId,
            historic,
            0
        );
        final int firstLeague =
            fixtures.league("Creator League", ownerId, current.editionId());
        final int secondLeague =
            fixtures.league("Other League", otherId, current.editionId());

        leagues.joinLeague(firstLeague, firstTeam);
        final AppException secondOwnTeam = assertThrows(
            AppException.class,
            () -> leagues.joinLeague(firstLeague, secondTeam)
        );
        final AppException foreignTeam = assertThrows(
            AppException.class,
            () -> leagues.joinLeague(firstLeague, otherTeam)
        );
        final AppException wrongEdition = assertThrows(
            AppException.class,
            () -> leagues.joinLeague(firstLeague, historicTeam)
        );
        leagues.joinLeague(secondLeague, firstTeam);

        assertEquals(ErrorCode.CONFLICT, secondOwnTeam.code());
        assertEquals(ErrorCode.AUTHENTICATION_REQUIRED, foreignTeam.code());
        assertEquals(ErrorCode.VALIDATION, wrongEdition.code());
        assertEquals(
            2,
            database.queryInt(
                "SELECT COUNT(*) FROM PARTECIPAZIONE_TEAM WHERE IdTeam = ?",
                firstTeam
            )
        );

        final List<JoinedLeague> joined =
            leagues.joinedLeagues(current.editionId());
        assertEquals(2, joined.size());
        assertEquals(
            List.of("Creator League", "Other League"),
            joined.stream().map(JoinedLeague::leagueName).toList()
        );
    }

    @Test
    void u9OrdersByTotalDescendingThenTeamName() {
        final int adminId = fixtures.user("admin", "unused-hash");
        final int alphaOwner = fixtures.user("alpha", "unused-hash");
        final int zetaOwner = fixtures.user("zeta", "unused-hash");
        final int lowOwner = fixtures.user("low", "unused-hash");
        sessions.login(adminId, "admin");
        final int editionId = fixtures.edition(1, 2025);
        final int leagueId = fixtures.league("Classifica", adminId, editionId);
        final int zeta = fixtures.team("Zeta", 50, zetaOwner, editionId);
        final int alpha = fixtures.team("Alpha", 50, alphaOwner, editionId);
        final int low = fixtures.team("Low", 10, lowOwner, editionId);
        fixtures.participation(leagueId, zeta);
        fixtures.participation(leagueId, low);
        fixtures.participation(leagueId, alpha);

        final List<StandingRow> standings =
            leagues.standings(leagueId, editionId);

        assertEquals(
            List.of("Alpha", "Zeta", "Low"),
            standings.stream().map(StandingRow::teamName).toList()
        );
        assertEquals(
            List.of(50, 50, 10),
            standings.stream().map(StandingRow::totalPoints).toList()
        );
    }

    @Test
    void concurrentU6AttemptsLeaveOnlyOneTeamForTheOwner() throws Exception {
        final int ownerId = fixtures.user(
            "concurrent.owner",
            "unused-hash"
        );
        sessions.login(ownerId, "concurrent.owner");
        final Season season = season(1, 2025, 4, "G");
        final int firstTeam = completeTeam(
            "Concurrent First",
            ownerId,
            season,
            0
        );
        final int secondTeam = completeTeam(
            "Concurrent Second",
            ownerId,
            season,
            0
        );
        final int leagueId = fixtures.league(
            "Concurrent League",
            ownerId,
            season.editionId()
        );
        final CountDownLatch ready = new CountDownLatch(2);
        final CountDownLatch start = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Future<AppException> first = executor.submit(
                () -> concurrentJoin(ready, start, leagueId, firstTeam)
            );
            final Future<AppException> second = executor.submit(
                () -> concurrentJoin(ready, start, leagueId, secondTeam)
            );
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            final AppException firstFailure =
                first.get(10, TimeUnit.SECONDS);
            final AppException secondFailure =
                second.get(10, TimeUnit.SECONDS);
            final long successes = java.util.stream.Stream.of(
                    firstFailure,
                    secondFailure
                )
                .filter(java.util.Objects::isNull)
                .count();
            final AppException failure =
                firstFailure != null ? firstFailure : secondFailure;

            assertEquals(1, successes);
            assertEquals(ErrorCode.CONFLICT, failure.code());
            assertEquals(
                1,
                database.queryInt(
                    """
                    SELECT COUNT(*)
                    FROM PARTECIPAZIONE_TEAM PT
                    JOIN TEAM_FANTASY TF ON TF.IdTeam = PT.IdTeam
                    WHERE PT.IdLega = ? AND TF.IdUtente = ?
                    """,
                    leagueId,
                    ownerId
                )
            );
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Season season(
        final int number,
        final int year,
        final int driverCount,
        final String prefix
    ) {
        final int editionId = fixtures.edition(number, year);
        final List<Integer> driverIds = new ArrayList<>();
        int constructorId = 0;
        for (int index = 0; index < driverCount; index++) {
            if (index % 2 == 0) {
                constructorId = fixtures.racingConstructor(
                    prefix + "C" + index
                );
                fixtures.enrollConstructor(
                    editionId,
                    constructorId,
                    prefix + "C" + index
                );
            }
            final int driverId = fixtures.driver(prefix + "D" + index);
            fixtures.enrollDriver(
                editionId,
                driverId,
                code(prefix, index),
                index + 1,
                constructorId
            );
            driverIds.add(driverId);
        }
        return new Season(editionId, List.copyOf(driverIds));
    }

    private int completeTeam(
        final String name,
        final int ownerId,
        final Season season,
        final int totalPoints
    ) {
        final int teamId = fixtures.team(
            name,
            totalPoints,
            ownerId,
            season.editionId()
        );
        for (int driverId : season.driverIds().subList(0, 4)) {
            fixtures.component(teamId, season.editionId(), driverId);
        }
        return teamId;
    }

    private static String code(final String prefix, final int index) {
        final char first = Character.toUpperCase(prefix.charAt(0));
        return "%c%c%c".formatted(
            first,
            (char) ('A' + (index / 26) % 26),
            (char) ('A' + index % 26)
        );
    }

    private static void assertValidation(final Runnable operation) {
        final AppException exception = assertThrows(
            AppException.class,
            operation::run
        );
        assertEquals(ErrorCode.VALIDATION, exception.code());
    }

    private AppException concurrentJoin(
        final CountDownLatch ready,
        final CountDownLatch start,
        final int leagueId,
        final int teamId
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            leagues.joinLeague(leagueId, teamId);
            return null;
        } catch (AppException exception) {
            return exception;
        }
    }

    private record Season(int editionId, List<Integer> driverIds) {
    }
}
