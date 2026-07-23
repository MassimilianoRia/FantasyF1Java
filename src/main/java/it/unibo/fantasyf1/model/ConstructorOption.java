package it.unibo.fantasyf1.model;

/**
 * Scuderia anagrafica selezionabile nei form amministrativi.
 */
public record ConstructorOption(int id, String name) {

    @Override
    public String toString() {
        return name;
    }
}
