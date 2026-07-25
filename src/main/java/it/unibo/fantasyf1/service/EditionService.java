package it.unibo.fantasyf1.service;

import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.model.EditionOverview;
import it.unibo.fantasyf1.model.dao.AdminDao;
import it.unibo.fantasyf1.model.dao.EdizioneDao;
import it.unibo.fantasyf1.model.dao.TeamDao;
import it.unibo.fantasyf1.model.database.TransactionManager;

import java.util.List;
import java.util.Objects;

/**
 * Selezione dell'edizione corrente e consultazione dello storico.
 */
public final class EditionService {

    private final TransactionManager transactions;
    private final EdizioneDao editions;
    private final AdminDao admin;
    private final TeamDao teams;

    public EditionService(
        final TransactionManager transactions,
        final EdizioneDao editions,
        final AdminDao admin,
        final TeamDao teams
    ) {
        this.transactions = Objects.requireNonNull(transactions);
        this.editions = Objects.requireNonNull(editions);
        this.admin = Objects.requireNonNull(admin);
        this.teams = Objects.requireNonNull(teams);
    }

    public List<Edizione> findAll() {
        return transactions.query(editions::findAll);
    }

    /**
     * L'ordine del DAO è per anno decrescente: il primo elemento è la regola
     * deterministica per l'edizione iniziale.
     */
    public Edizione current() {
        return findAll().stream().findFirst().orElseThrow(() ->
            ServiceGuards.notFound(
                "Non sono presenti edizioni. L'amministratore deve crearne una."
            )
        );
    }

    public EditionOverview overview(final int editionId) {
        return transactions.query(connection -> {
            final Edizione edition = editions.findById(
                connection,
                editionId
            ).orElseThrow(() ->
                ServiceGuards.notFound("Edizione non trovata.")
            );
            return new EditionOverview(
                edition,
                admin.editionStatus(connection, editionId),
                admin.findWeekends(connection, editionId),
                admin.findEnrolledConstructors(connection, editionId),
                teams.findDriversByEdition(connection, editionId)
            );
        });
    }
}
