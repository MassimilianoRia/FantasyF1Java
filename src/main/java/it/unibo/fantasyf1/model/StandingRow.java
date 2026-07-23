package it.unibo.fantasyf1.model;

/**
 * Riga della classifica dinamica di una lega.
 */
public record StandingRow(
    int teamId,
    String teamName,
    String ownerUsername,
    int totalPoints
) {

    @Override
    public String toString() {
        return "%s — %s — %d punti".formatted(
            teamName,
            ownerUsername,
            totalPoints
        );
    }
}
