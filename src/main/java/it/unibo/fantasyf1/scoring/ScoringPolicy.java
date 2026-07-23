package it.unibo.fantasyf1.scoring;

/**
 * Strategia sostituibile per il calcolo del punteggio fantasy.
 */
@FunctionalInterface
public interface ScoringPolicy {

    /**
     * Calcola il punteggio associato a una prestazione ufficiale.
     *
     * @param performance dati della prestazione
     * @return punteggio fantasy
     */
    int score(PerformanceData performance);
}
