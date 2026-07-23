package it.unibo.fantasyf1.model;

/**
 * Conteggi necessari a mostrare la completezza di un'edizione.
 */
public record EditionStatus(
    int editionId,
    int weekends,
    int constructors,
    int drivers,
    int constructorsWithTwoDrivers,
    boolean complete
) {

    @Override
    public String toString() {
        return complete
            ? "Edizione completa"
            : "%d/24 weekend, %d/10 scuderie, %d/20 piloti"
                .formatted(weekends, constructors, drivers);
    }
}
