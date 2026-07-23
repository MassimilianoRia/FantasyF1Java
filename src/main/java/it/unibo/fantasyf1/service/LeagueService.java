package it.unibo.fantasyf1.service;

import it.unibo.fantasyf1.error.AppException;
import it.unibo.fantasyf1.error.ErrorCode;
import it.unibo.fantasyf1.model.JoinedLeague;
import it.unibo.fantasyf1.model.LegaDisponibile;
import it.unibo.fantasyf1.model.StandingRow;
import it.unibo.fantasyf1.model.dao.LegaDao;
import it.unibo.fantasyf1.model.dao.TeamDao;
import it.unibo.fantasyf1.model.database.TransactionManager;
import it.unibo.fantasyf1.session.SessionManager;
import it.unibo.fantasyf1.session.UserSession;
import it.unibo.fantasyf1.validation.InputValidator;

import java.util.List;
import java.util.Objects;

/**
 * U4–U7 e U9.
 */
public final class LeagueService {

    private final TransactionManager transactions;
    private final LegaDao leagues;
    private final TeamDao teams;
    private final SessionManager sessions;

    public LeagueService(
        final TransactionManager transactions,
        final LegaDao leagues,
        final TeamDao teams,
        final SessionManager sessions
    ) {
        this.transactions = Objects.requireNonNull(transactions);
        this.leagues = Objects.requireNonNull(leagues);
        this.teams = Objects.requireNonNull(teams);
        this.sessions = Objects.requireNonNull(sessions);
    }

    public int createLeague(final String nameValue, final int editionId) {
        final UserSession session = ServiceGuards.authenticated(sessions);
        final String name = InputValidator.max(
            InputValidator.required(nameValue, "Il nome della lega"),
            100,
            "Il nome della lega"
        );
        if (editionId <= 0) {
            throw ServiceGuards.invalid("Seleziona un'edizione valida.");
        }
        return transactions.executeInTransaction(connection -> leagues.insert(
            connection,
            name,
            session.userId(),
            editionId
        ));
    }

    public List<LegaDisponibile> availableLeagues(final int editionId) {
        ServiceGuards.authenticated(sessions);
        return transactions.query(
            connection -> leagues.findByEdition(connection, editionId)
        );
    }

    /**
     * U6 serializza tutte le iscrizioni della stessa lega bloccando per prima
     * la riga LEGA. Solo dopo blocca il TEAM e legge le partecipazioni.
     */
    public void joinLeague(final int leagueId, final int teamId) {
        final UserSession session = ServiceGuards.authenticated(sessions);
        transactions.inTransaction(connection -> {
            final LegaDao.LeagueRow league = leagues
                .lockLeague(connection, leagueId)
                .orElseThrow(() -> ServiceGuards.notFound(
                    "La lega selezionata non esiste."
                ));
            final TeamDao.TeamRow team = teams
                .lockTeam(connection, teamId)
                .orElseThrow(() -> ServiceGuards.notFound(
                    "Il team selezionato non esiste."
                ));
            if (team.ownerId() != session.userId()) {
                throw new AppException(
                    ErrorCode.AUTHENTICATION_REQUIRED,
                    "Puoi iscrivere soltanto un tuo team."
                );
            }
            if (team.editionId() != league.editionId()) {
                throw ServiceGuards.invalid(
                    "Team e lega devono appartenere alla stessa edizione."
                );
            }
            if (
                leagues.findParticipatingTeamForOwner(
                    connection,
                    leagueId,
                    session.userId()
                ).isPresent()
            ) {
                throw ServiceGuards.conflict(
                    "Hai già un team iscritto a questa lega."
                );
            }
            leagues.insertParticipation(connection, leagueId, teamId);
        });
    }

    public List<JoinedLeague> joinedLeagues(final int editionId) {
        final UserSession session = ServiceGuards.authenticated(sessions);
        return transactions.query(connection -> leagues.findJoinedByOwner(
            connection,
            session.userId(),
            editionId
        ));
    }

    public List<LegaDisponibile> myLeagues(final int editionId) {
        final UserSession session = ServiceGuards.authenticated(sessions);
        return transactions.query(
            connection -> leagues.findOwnedByAdministrator(
                connection,
                session.userId(),
                editionId
            )
        );
    }

    public List<StandingRow> standings(
        final int leagueId,
        final int editionId
    ) {
        ServiceGuards.authenticated(sessions);
        return transactions.query(connection -> {
            final LegaDao.LeagueRow league = leagues
                .findLeague(connection, leagueId)
                .orElseThrow(() -> ServiceGuards.notFound(
                    "La lega selezionata non esiste."
                ));
            if (league.editionId() != editionId) {
                throw ServiceGuards.invalid(
                    "La lega non appartiene all'edizione selezionata."
                );
            }
            return leagues.findStandings(connection, leagueId);
        });
    }
}
