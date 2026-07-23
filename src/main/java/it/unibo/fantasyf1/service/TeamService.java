package it.unibo.fantasyf1.service;

import it.unibo.fantasyf1.error.AppException;
import it.unibo.fantasyf1.error.ErrorCode;
import it.unibo.fantasyf1.model.DriverOption;
import it.unibo.fantasyf1.model.RaceWeekend;
import it.unibo.fantasyf1.model.TeamSummary;
import it.unibo.fantasyf1.model.WeekendScoreRow;
import it.unibo.fantasyf1.model.dao.AdminDao;
import it.unibo.fantasyf1.model.dao.ResultDao;
import it.unibo.fantasyf1.model.dao.TeamDao;
import it.unibo.fantasyf1.model.database.TransactionManager;
import it.unibo.fantasyf1.session.SessionManager;
import it.unibo.fantasyf1.session.UserSession;
import it.unibo.fantasyf1.validation.InputValidator;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * U2, U3 e U8.
 */
public final class TeamService {

    private static final int TEAM_SIZE = 4;

    private final TransactionManager transactions;
    private final TeamDao teams;
    private final AdminDao admin;
    private final ResultDao results;
    private final SessionManager sessions;
    private final Clock clock;

    public TeamService(
        final TransactionManager transactions,
        final TeamDao teams,
        final AdminDao admin,
        final ResultDao results,
        final SessionManager sessions,
        final Clock clock
    ) {
        this.transactions = Objects.requireNonNull(transactions);
        this.teams = Objects.requireNonNull(teams);
        this.admin = Objects.requireNonNull(admin);
        this.results = Objects.requireNonNull(results);
        this.sessions = Objects.requireNonNull(sessions);
        this.clock = Objects.requireNonNull(clock);
    }

    public List<DriverOption> selectableDrivers(final int editionId) {
        ServiceGuards.authenticated(sessions);
        return transactions.query(
            connection -> teams.findDriversByEdition(connection, editionId)
        );
    }

    public int createTeam(
        final String nameValue,
        final int editionId,
        final List<Integer> driverIdsValue
    ) {
        final UserSession session = ServiceGuards.authenticated(sessions);
        final String name = InputValidator.max(
            InputValidator.required(nameValue, "Il nome del team"),
            100,
            "Il nome del team"
        );
        if (editionId <= 0) {
            throw ServiceGuards.invalid("Seleziona un'edizione valida.");
        }
        final List<Integer> driverIds = List.copyOf(
            Objects.requireNonNullElse(driverIdsValue, List.of())
        );
        if (driverIds.size() != TEAM_SIZE) {
            throw ServiceGuards.invalid(
                "Il team deve contenere esattamente quattro piloti."
            );
        }
        final Set<Integer> distinct = new HashSet<>(driverIds);
        if (distinct.size() != TEAM_SIZE || distinct.contains(null)) {
            throw ServiceGuards.invalid(
                "I quattro piloti del team devono essere distinti."
            );
        }

        return transactions.inTransaction(connection -> {
            if (!admin.lockEdition(connection, editionId)) {
                throw ServiceGuards.notFound("Edizione non trovata.");
            }
            final Set<Integer> eligible = teams
                .findDriversByEdition(connection, editionId)
                .stream()
                .map(DriverOption::id)
                .collect(java.util.stream.Collectors.toSet());
            if (!eligible.containsAll(distinct)) {
                throw ServiceGuards.invalid(
                    "Tutti i piloti devono essere iscritti all'edizione "
                        + "selezionata."
                );
            }
            final int teamId = teams.insertTeam(
                connection,
                name,
                session.userId(),
                editionId
            );
            teams.insertComponents(connection, teamId, editionId, driverIds);

            final LocalDate today = LocalDate.now(clock);
            for (int grandPrixId :
                results.findEndedWeekendIds(connection, editionId, today)) {
                if (results.isProcessable(
                    connection,
                    editionId,
                    grandPrixId,
                    today
                )) {
                    results.upsertResultForTeam(
                        connection,
                        teamId,
                        editionId,
                        grandPrixId
                    );
                }
            }
            results.recalculateTeamTotal(connection, teamId);
            return teamId;
        });
    }

    public List<TeamSummary> myTeams(final int editionId) {
        final UserSession session = ServiceGuards.authenticated(sessions);
        return transactions.query(connection -> teams.findOwnedWithRoster(
            connection,
            session.userId(),
            editionId
        ));
    }

    public List<RaceWeekend> processedWeekends(final int editionId) {
        ServiceGuards.authenticated(sessions);
        final LocalDate today = LocalDate.now(clock);
        return transactions.query(connection -> {
            final List<RaceWeekend> all =
                admin.findWeekends(connection, editionId);
            final java.util.ArrayList<RaceWeekend> processable =
                new java.util.ArrayList<>();
            for (RaceWeekend weekend : all) {
                if (results.isProcessable(
                    connection,
                    editionId,
                    weekend.grandPrixId(),
                    today
                )) {
                    processable.add(weekend);
                }
            }
            return List.copyOf(processable);
        });
    }

    public List<WeekendScoreRow> weekendBreakdown(
        final int teamId,
        final int editionId,
        final int grandPrixId
    ) {
        final UserSession session = ServiceGuards.authenticated(sessions);
        return transactions.query(connection -> {
            final TeamDao.TeamRow team = teams
                .findTeam(connection, teamId)
                .orElseThrow(() -> ServiceGuards.notFound("Team non trovato."));
            if (
                team.ownerId() != session.userId()
                    || team.editionId() != editionId
            ) {
                throw new AppException(
                    ErrorCode.AUTHENTICATION_REQUIRED,
                    "Il team selezionato non appartiene all'utente o "
                        + "all'edizione corrente."
                );
            }
            final List<WeekendScoreRow> rows =
                teams.findWeekendBreakdown(
                    connection,
                    teamId,
                    session.userId(),
                    editionId,
                    grandPrixId
                );
            if (rows.size() != TEAM_SIZE) {
                throw ServiceGuards.conflict(
                    "Il weekend non è ancora elaborato definitivamente "
                        + "per questo team."
                );
            }
            return rows;
        });
    }
}
