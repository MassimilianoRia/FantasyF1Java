package it.unibo.fantasyf1.model;

/**
 * Lega disponibile nell'edizione selezionata.
 */
public record LeagueSummary(
    int id,
    String name,
    int administratorId,
    String administratorUsername,
    int editionId
) {

    @Override
    public String toString() {
        return "%s — amministratore: %s".formatted(
            name,
            administratorUsername
        );
    }
}
