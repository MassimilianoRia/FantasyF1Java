package it.unibo.fantasyf1.scoring;

/**
 * Dati ufficiali utilizzati per calcolare il punteggio fantasy di un pilota.
 *
 * @param qualifyingPosition posizione in qualifica, oppure {@code null}
 * @param racePosition posizione in gara, oppure {@code null}
 * @param penalized presenza di una penalizzazione
 * @param fastestLap registrazione del giro veloce
 */
public record PerformanceData(
    Integer qualifyingPosition,
    Integer racePosition,
    boolean penalized,
    boolean fastestLap
) {

    public PerformanceData {
        validatePosition(qualifyingPosition, "qualifica");
        validatePosition(racePosition, "gara");
    }

    private static void validatePosition(
        final Integer position,
        final String description
    ) {
        if (position != null && (position < 1 || position > 20)) {
            throw new IllegalArgumentException(
                "La posizione in %s deve essere compresa tra 1 e 20"
                    .formatted(description)
            );
        }
    }
}
