package it.unibo.fantasyf1.model;

import java.util.Objects;

/**
 * Stato della prestazione di un pilota iscritto in uno specifico GP.
 */
public record WeekendPerformanceStatus(
    int editionId,
    int grandPrixId,
    int driverId,
    String firstName,
    String lastName,
    String code,
    String constructorName,
    boolean recorded,
    Integer qualifyingPosition,
    Integer racePosition,
    boolean penalized,
    boolean fastestLap,
    Integer fantasyPoints
) {

    public WeekendPerformanceStatus {
        firstName = Objects.requireNonNull(
            firstName,
            "Il nome del pilota non può essere null"
        );
        lastName = Objects.requireNonNull(
            lastName,
            "Il cognome del pilota non può essere null"
        );
        code = Objects.requireNonNull(
            code,
            "La sigla del pilota non può essere null"
        );
        constructorName = Objects.requireNonNull(
            constructorName,
            "La scuderia non può essere null"
        );
    }

    @Override
    public String toString() {
        final String driver = "%s %s (%s) — %s".formatted(
            firstName,
            lastName,
            code,
            constructorName
        );
        if (!recorded) {
            return "MANCANTE · " + driver;
        }
        final String qualifying = qualifyingPosition == null
            ? "N/D"
            : qualifyingPosition.toString();
        final String race = racePosition == null
            ? "N/D"
            : racePosition.toString();
        final String points = fantasyPoints == null
            ? "da calcolare"
            : fantasyPoints.toString();
        return "REGISTRATA · %s · Q: %s · Gara: %s · Penalità: %s · "
            .formatted(
                driver,
                qualifying,
                race,
                penalized ? "sì" : "no"
            )
            + "Giro veloce: %s · Punti: %s"
                .formatted(fastestLap ? "sì" : "no", points);
    }
}
