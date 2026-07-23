package it.unibo.fantasyf1.model;

/**
 * Pilota mostrato nella composizione di un team fantasy.
 */
public record TeamDriver(
    int driverId,
    String firstName,
    String lastName,
    String code,
    int raceNumber
) {

    @Override
    public String toString() {
        return "%s %s (%s, #%d)".formatted(
            firstName,
            lastName,
            code,
            raceNumber
        );
    }
}
