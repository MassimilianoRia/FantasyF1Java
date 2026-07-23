package it.unibo.fantasyf1.model;

/**
 * Pilota iscritto selezionabile per un team nell'edizione corrente.
 */
public record DriverOption(
    int id,
    int editionId,
    String firstName,
    String lastName,
    String code,
    int raceNumber,
    String constructorName
) {

    @Override
    public String toString() {
        return "%s %s (%s, #%d) — %s".formatted(
            firstName,
            lastName,
            code,
            raceNumber,
            constructorName
        );
    }
}
