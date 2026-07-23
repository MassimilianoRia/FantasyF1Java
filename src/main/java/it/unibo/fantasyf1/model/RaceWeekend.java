package it.unibo.fantasyf1.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Weekend di gara appartenente a una specifica edizione.
 */
public record RaceWeekend(
    int editionId,
    int grandPrixId,
    int round,
    String grandPrixName,
    LocalDate startDate,
    LocalDate endDate
) {

    public RaceWeekend {
        grandPrixName = Objects.requireNonNull(
            grandPrixName,
            "Il nome del Gran Premio non può essere null"
        );
        startDate = Objects.requireNonNull(
            startDate,
            "La data di inizio non può essere null"
        );
        endDate = Objects.requireNonNull(
            endDate,
            "La data di fine non può essere null"
        );
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                "La data di fine non può precedere quella di inizio"
            );
        }
    }

    @Override
    public String toString() {
        return "Round %d — %s (%s)".formatted(
            round,
            grandPrixName,
            endDate
        );
    }
}
