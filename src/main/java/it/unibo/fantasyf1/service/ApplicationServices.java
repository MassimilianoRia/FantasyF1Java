package it.unibo.fantasyf1.service;

import it.unibo.fantasyf1.model.dao.AdminDao;
import it.unibo.fantasyf1.model.dao.EdizioneDao;
import it.unibo.fantasyf1.model.dao.LegaDao;
import it.unibo.fantasyf1.model.dao.ResultDao;
import it.unibo.fantasyf1.model.dao.TeamDao;
import it.unibo.fantasyf1.model.dao.UserDao;
import it.unibo.fantasyf1.model.database.ConnectionProvider;
import it.unibo.fantasyf1.model.database.DatabaseConnection;
import it.unibo.fantasyf1.model.database.TransactionManager;
import it.unibo.fantasyf1.scoring.ScoringPolicy;
import it.unibo.fantasyf1.scoring.SimpleScoringPolicy;
import it.unibo.fantasyf1.security.PasswordHasher;
import it.unibo.fantasyf1.security.Pbkdf2PasswordHasher;
import it.unibo.fantasyf1.session.SessionManager;

import java.time.Clock;
import java.util.Objects;

/**
 * Composition root condiviso dalle view; nessuna view riceve un DAO.
 */
public final class ApplicationServices {

    private final AuthenticationService authentication;
    private final EditionService editions;
    private final TeamService teams;
    private final LeagueService leagues;
    private final AdminService admin;

    public ApplicationServices(
        final ConnectionProvider connectionProvider,
        final Clock clock,
        final PasswordHasher passwordHasher,
        final ScoringPolicy scoringPolicy,
        final SessionManager sessions
    ) {
        Objects.requireNonNull(connectionProvider);
        final Clock selectedClock = Objects.requireNonNull(clock);
        final TransactionManager transactions =
            new TransactionManager(connectionProvider);
        final UserDao userDao = new UserDao();
        final EdizioneDao editionDao = new EdizioneDao();
        final TeamDao teamDao = new TeamDao();
        final LegaDao leagueDao = new LegaDao();
        final AdminDao adminDao = new AdminDao();
        final ResultDao resultDao = new ResultDao();

        authentication = new AuthenticationService(
            transactions,
            userDao,
            Objects.requireNonNull(passwordHasher),
            Objects.requireNonNull(sessions)
        );
        editions = new EditionService(transactions, editionDao);
        teams = new TeamService(
            transactions,
            teamDao,
            adminDao,
            resultDao,
            sessions,
            selectedClock
        );
        leagues = new LeagueService(
            transactions,
            leagueDao,
            teamDao,
            sessions
        );
        final WeekendProcessingService processing =
            new WeekendProcessingService(
                transactions,
                adminDao,
                resultDao,
                Objects.requireNonNull(scoringPolicy),
                selectedClock
            );
        admin = new AdminService(
            transactions,
            adminDao,
            editionDao,
            teamDao,
            resultDao,
            processing,
            selectedClock
        );
    }

    public static ApplicationServices production() {
        return new ApplicationServices(
            DatabaseConnection.provider(),
            Clock.systemDefaultZone(),
            new Pbkdf2PasswordHasher(),
            new SimpleScoringPolicy(),
            new SessionManager()
        );
    }

    public AuthenticationService authentication() {
        return authentication;
    }

    public EditionService editions() {
        return editions;
    }

    public TeamService teams() {
        return teams;
    }

    public LeagueService leagues() {
        return leagues;
    }

    public AdminService admin() {
        return admin;
    }
}
