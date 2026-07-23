package it.unibo.fantasyf1.scoring;

import java.util.Objects;

/**
 * Policy provvisoria e isolata usata finché il regolamento numerico non viene
 * definito dalla specifica.
 *
 * <p>Assegna {@code max(0, 21 - posizione)} per la gara,
 * {@code max(0, 6 - posizione)} per la qualifica, due punti per il giro veloce
 * e cinque punti di malus in caso di penalizzazione. Una posizione assente
 * vale zero.</p>
 */
public final class SimpleScoringPolicy implements ScoringPolicy {

    private static final int FASTEST_LAP_BONUS = 2;
    private static final int PENALTY_MALUS = 5;

    @Override
    public int score(final PerformanceData performance) {
        Objects.requireNonNull(performance, "La prestazione non può essere null");

        int score = positionPoints(performance.racePosition(), 21);
        score += positionPoints(performance.qualifyingPosition(), 6);
        if (performance.fastestLap()) {
            score += FASTEST_LAP_BONUS;
        }
        if (performance.penalized()) {
            score -= PENALTY_MALUS;
        }
        return score;
    }

    private static int positionPoints(
        final Integer position,
        final int baseline
    ) {
        return position == null ? 0 : Math.max(0, baseline - position);
    }
}
