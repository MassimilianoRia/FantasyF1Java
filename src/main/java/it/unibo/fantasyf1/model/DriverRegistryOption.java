package it.unibo.fantasyf1.model;

import java.time.LocalDate;

/**
 * Pilota anagrafico selezionabile nell'area amministrativa.
 */
public record DriverRegistryOption(
    int id,
    String firstName,
    String lastName,
    String nationality,
    LocalDate birthDate
) {

    @Override
    public String toString() {
        return "%s %s — %s".formatted(firstName, lastName, nationality);
    }
}
