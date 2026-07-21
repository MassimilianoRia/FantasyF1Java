package it.unibo.fantasyf1.model;

/**
 * Edizione annuale del campionato di Formula 1.
 */
public record Edizione(int id, int numero, int anno) {

    @Override
    public String toString() {
        return "Edizione %d (%d)".formatted(numero, anno);
    }
}
