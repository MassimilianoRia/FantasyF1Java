package it.unibo.fantasyf1.service;

import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.model.dao.EdizioneDao;
import it.unibo.fantasyf1.model.database.TransactionManager;

import java.util.List;
import java.util.Objects;

/**
 * Selezione dell'edizione corrente e consultazione dello storico.
 */
public final class EditionService {

    private final TransactionManager transactions;
    private final EdizioneDao editions;

    public EditionService(
        final TransactionManager transactions,
        final EdizioneDao editions
    ) {
        this.transactions = Objects.requireNonNull(transactions);
        this.editions = Objects.requireNonNull(editions);
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
}
