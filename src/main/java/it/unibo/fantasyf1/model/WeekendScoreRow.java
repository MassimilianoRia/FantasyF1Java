package it.unibo.fantasyf1.model;

/**
 * Dettaglio U8 del punteggio di un pilota in un weekend elaborato.
 */
public record WeekendScoreRow(
    int driverId,
    String firstName,
    String lastName,
    String code,
    Integer fantasyPoints
) {

    @Override
    public String toString() {
        return "%s %s (%s): %s".formatted(
            firstName,
            lastName,
            code,
            fantasyPoints == null ? "non calcolato" : fantasyPoints + " punti"
        );
    }
}
