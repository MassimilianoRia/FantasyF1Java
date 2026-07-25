package it.unibo.fantasyf1.service;

import it.unibo.fantasyf1.model.dao.AdminDao;
import it.unibo.fantasyf1.model.dao.ResultDao;
import it.unibo.fantasyf1.model.database.TransactionManager;
import it.unibo.fantasyf1.scoring.PerformanceData;
import it.unibo.fantasyf1.scoring.ScoringPolicy;
import it.unibo.fantasyf1.validation.InputValidator;

import java.util.Objects;

/**
 * Workflow atomici A8 e A9 → O1 → O2 → O3.
 */
public final class WeekendProcessingService {

    private final TransactionManager transactions;
    private final AdminDao admin;
    private final ResultDao results;
    private final ScoringPolicy scoringPolicy;

    public WeekendProcessingService(
        final TransactionManager transactions,
        final AdminDao admin,
        final ResultDao results,
        final ScoringPolicy scoringPolicy
    ) {
        this.transactions = Objects.requireNonNull(transactions);
        this.admin = Objects.requireNonNull(admin);
        this.results = Objects.requireNonNull(results);
        this.scoringPolicy = Objects.requireNonNull(scoringPolicy);
    }

    public void recordPerformance(
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
        transactions.inTransaction(connection -> {
            if (!admin.lockEdition(connection, request.editionId())) {
                throw ServiceGuards.notFound("Edizione non trovata.");
            }
            final boolean concluded = admin.lockWeekendConclusion(
                connection,
                request.editionId(),
                request.grandPrixId()
            ).orElseThrow(() -> ServiceGuards.notFound(
                "Weekend non trovato."
            ));
            if (concluded) {
                throw ServiceGuards.conflict(
                    "Il weekend è già concluso: le prestazioni ufficiali "
                        + "non possono più essere modificate."
                );
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
            if (admin.isPerformancePresent(
                connection,
                request.editionId(),
                request.grandPrixId(),
                request.driverId()
            )) {
                throw ServiceGuards.conflict(
                    "La prestazione del pilota per questo weekend è già "
                        + "registrata e non può essere modificata o "
                        + "sovrascritta."
                );
            }

            admin.insertPerformance(
                connection,
                request.editionId(),
                request.grandPrixId(),
                request.driverId(),
                data
            );
            // A8 mantiene il punteggio non calcolato. La pulizia protegge
            // anche database legacy che contenevano risultati provvisori.
            results.clearWeekendResults(
                connection,
                request.editionId(),
                request.grandPrixId()
            );
            results.recalculateEditionTotals(
                connection,
                request.editionId()
            );
        });
    }

    /**
     * A9 convalida i risultati e, nella stessa transazione, esegue O1-O3.
     */
    public void concludeWeekend(
        final int editionId,
        final int grandPrixId
    ) {
        if (editionId <= 0 || grandPrixId <= 0) {
            throw ServiceGuards.invalid(
                "Seleziona un'edizione e un weekend validi."
            );
        }
        transactions.inTransaction(connection -> {
            if (!admin.lockEdition(connection, editionId)) {
                throw ServiceGuards.notFound("Edizione non trovata.");
            }
            final boolean concluded = admin.lockWeekendConclusion(
                connection,
                editionId,
                grandPrixId
            ).orElseThrow(() -> ServiceGuards.notFound(
                "Weekend non trovato."
            ));
            if (concluded) {
                throw ServiceGuards.conflict(
                    "Il weekend è già stato concluso."
                );
            }

            final ResultDao.CompletionStatus completion =
                results.completionStatus(
                    connection,
                    editionId,
                    grandPrixId
                );
            if (completion.enrolledDrivers() == 0) {
                throw ServiceGuards.conflict(
                    "Non è possibile concludere il weekend: l'edizione "
                        + "non contiene piloti iscritti."
                );
            }
            if (!completion.ready()) {
                throw ServiceGuards.conflict(
                    "Non è possibile concludere il weekend: mancano "
                        + completion.missingPerformances()
                        + " prestazioni ufficiali su "
                        + completion.enrolledDrivers() + "."
                );
            }

            if (admin.concludeWeekend(
                connection,
                editionId,
                grandPrixId
            ) != 1) {
                throw ServiceGuards.conflict(
                    "Il weekend non è stato concluso perché il suo stato "
                        + "è cambiato."
                );
            }

            // O1 è ammessa soltanto dopo l'UPDATE amministrativo. Qualsiasi
            // errore annulla anche Concluso grazie alla transazione comune.
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
            results.recalculateWeekendResults(
                connection,
                editionId,
                grandPrixId
            );
            results.recalculateEditionTotals(connection, editionId);
        });
    }

    /**
     * A10 riapre eccezionalmente un weekend e invalida tutti i dati fantasy
     * derivati, mantenendo intatti i risultati sportivi ufficiali.
     */
    public void reopenWeekend(
        final int editionId,
        final int grandPrixId
    ) {
        if (editionId <= 0 || grandPrixId <= 0) {
            throw ServiceGuards.invalid(
                "Seleziona un'edizione e un weekend validi."
            );
        }
        transactions.inTransaction(connection -> {
            if (!admin.lockEdition(connection, editionId)) {
                throw ServiceGuards.notFound("Edizione non trovata.");
            }
            final boolean concluded = admin.lockWeekendConclusion(
                connection,
                editionId,
                grandPrixId
            ).orElseThrow(() -> ServiceGuards.notFound(
                "Weekend non trovato."
            ));
            if (!concluded) {
                throw ServiceGuards.conflict(
                    "Il weekend è già aperto."
                );
            }

            if (admin.reopenWeekend(
                connection,
                editionId,
                grandPrixId
            ) != 1) {
                throw ServiceGuards.conflict(
                    "Il weekend non è stato riaperto perché il suo stato "
                        + "è cambiato."
                );
            }
            results.clearWeekendFantasyScores(
                connection,
                editionId,
                grandPrixId
            );
            results.clearWeekendResults(
                connection,
                editionId,
                grandPrixId
            );
            results.recalculateEditionTotals(connection, editionId);
        });
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
