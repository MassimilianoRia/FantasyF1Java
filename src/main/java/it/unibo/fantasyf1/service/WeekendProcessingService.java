package it.unibo.fantasyf1.service;

import it.unibo.fantasyf1.model.dao.AdminDao;
import it.unibo.fantasyf1.model.dao.ResultDao;
import it.unibo.fantasyf1.model.database.TransactionManager;
import it.unibo.fantasyf1.scoring.PerformanceData;
import it.unibo.fantasyf1.scoring.ScoringPolicy;
import it.unibo.fantasyf1.validation.InputValidator;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Workflow atomico e idempotente A8 → O1 → O2 → O3.
 */
public final class WeekendProcessingService {

    private final TransactionManager transactions;
    private final AdminDao admin;
    private final ResultDao results;
    private final ScoringPolicy scoringPolicy;
    private final Clock clock;

    public WeekendProcessingService(
        final TransactionManager transactions,
        final AdminDao admin,
        final ResultDao results,
        final ScoringPolicy scoringPolicy,
        final Clock clock
    ) {
        this.transactions = Objects.requireNonNull(transactions);
        this.admin = Objects.requireNonNull(admin);
        this.results = Objects.requireNonNull(results);
        this.scoringPolicy = Objects.requireNonNull(scoringPolicy);
        this.clock = Objects.requireNonNull(clock);
    }

    public ProcessingOutcome recordPerformance(
        final PerformanceRequest request
    ) {
        Objects.requireNonNull(request, "La prestazione non può essere null");
        validatePosition(
            request.qualifyingPosition(),
            "La posizione in qualifica"
        );
        validatePosition(request.racePosition(), "La posizione in gara");
        if (
            request.editionId() <= 0
                || request.grandPrixId() <= 0
                || request.driverId() <= 0
        ) {
            throw ServiceGuards.invalid(
                "Edizione, weekend e pilota devono essere selezionati."
            );
        }

        final PerformanceData data = new PerformanceData(
            request.qualifyingPosition(),
            request.racePosition(),
            request.penalized(),
            request.fastestLap()
        );
        return transactions.inTransaction(connection -> {
            if (!admin.lockEdition(connection, request.editionId())) {
                throw ServiceGuards.notFound("Edizione non trovata.");
            }
            if (!admin.performanceContextExists(
                connection,
                request.editionId(),
                request.grandPrixId(),
                request.driverId()
            )) {
                throw ServiceGuards.invalid(
                    "Il pilota e il weekend devono appartenere "
                        + "all'edizione selezionata."
                );
            }

            final int requestedScore = scoringPolicy.score(data);
            admin.upsertPerformance(
                connection,
                request.editionId(),
                request.grandPrixId(),
                request.driverId(),
                data,
                requestedScore
            );

            // O1 viene rieseguita su tutte le prestazioni del weekend:
            // l'operazione è idempotente e rende sostituibile la policy.
            for (ResultDao.PerformanceRow row : results.findPerformances(
                connection,
                request.editionId(),
                request.grandPrixId()
            )) {
                results.updateFantasyScore(
                    connection,
                    request.editionId(),
                    request.grandPrixId(),
                    row.driverId(),
                    scoringPolicy.score(row.data())
                );
            }

            final boolean processable = updateDerivedData(
                connection,
                request.editionId(),
                request.grandPrixId()
            );
            return new ProcessingOutcome(requestedScore, processable);
        });
    }

    /**
     * Comando amministrativo idempotente per riallineare un weekend.
     */
    public boolean processWeekend(
        final int editionId,
        final int grandPrixId
    ) {
        if (editionId <= 0 || grandPrixId <= 0) {
            throw ServiceGuards.invalid(
                "Seleziona un'edizione e un weekend validi."
            );
        }
        return transactions.inTransaction(connection -> {
            if (!admin.lockEdition(connection, editionId)) {
                throw ServiceGuards.notFound("Edizione non trovata.");
            }
            if (!admin.weekendExists(connection, editionId, grandPrixId)) {
                throw ServiceGuards.notFound("Weekend non trovato.");
            }
            for (ResultDao.PerformanceRow row :
                results.findPerformances(connection, editionId, grandPrixId)) {
                results.updateFantasyScore(
                    connection,
                    editionId,
                    grandPrixId,
                    row.driverId(),
                    scoringPolicy.score(row.data())
                );
            }
            return updateDerivedData(connection, editionId, grandPrixId);
        });
    }

    private boolean updateDerivedData(
        final java.sql.Connection connection,
        final int editionId,
        final int grandPrixId
    ) throws java.sql.SQLException {
        final boolean processable = results.isProcessable(
            connection,
            editionId,
            grandPrixId,
            LocalDate.now(clock)
        );
        if (processable) {
            results.recalculateWeekendResults(
                connection,
                editionId,
                grandPrixId
            );
        } else {
            // Una correzione o un nuovo iscritto non devono lasciare un
            // risultato precedentemente definitivo ma ora incompleto.
            results.clearWeekendResults(
                connection,
                editionId,
                grandPrixId
            );
        }
        results.recalculateEditionTotals(connection, editionId);
        return processable;
    }

    private static void validatePosition(
        final Integer position,
        final String label
    ) {
        if (position != null) {
            InputValidator.intRange(position, 1, 20, label);
        }
    }
}
