package it.unibo.fantasyf1.model;

import java.util.List;
import java.util.Objects;

/**
 * Riepilogo di un team e della sua composizione.
 */
public record TeamSummary(
    int id,
    String name,
    int totalPoints,
    List<TeamDriver> drivers
) {

    public TeamSummary {
        drivers = List.copyOf(Objects.requireNonNull(
            drivers,
            "La composizione del team non può essere null"
        ));
    }

    @Override
    public String toString() {
        return "%s — %d punti".formatted(name, totalPoints);
    }
}
