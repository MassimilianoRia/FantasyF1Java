package it.unibo.fantasyf1.model;

/**
 * Dettaglio U8 del punteggio di un pilota in un weekend concluso.
 */
public record WeekendScoreRow(
    int driverId,
    String firstName,
    String lastName,
    String code,
    Integer qualifyingPosition,
    Integer racePosition,
    boolean penalized,
    boolean fastestLap,
    Integer fantasyPoints
) {

    @Override
    public String toString() {
        return "%s %s (%s) — Q: %s · Gara: %s · Penalità: %s · "
            .formatted(
                firstName,
                lastName,
                code,
                positionText(qualifyingPosition),
                positionText(racePosition),
                penalized ? "sì" : "no"
            )
            + "Giro veloce: %s · %s".formatted(
                fastestLap ? "sì" : "no",
                fantasyPoints == null
                    ? "punteggio non calcolato"
                    : fantasyPoints + " punti"
            );
    }

    private static String positionText(final Integer position) {
        return position == null ? "N/D" : position + "°";
    }
}
