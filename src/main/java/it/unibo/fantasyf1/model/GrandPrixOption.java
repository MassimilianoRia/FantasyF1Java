package it.unibo.fantasyf1.model;

/**
 * Gran Premio anagrafico selezionabile nei form amministrativi.
 */
public record GrandPrixOption(
    int id,
    String name,
    String circuit,
    String country,
    String city
) {

    @Override
    public String toString() {
        return "%s — %s, %s".formatted(name, circuit, country);
    }
}
