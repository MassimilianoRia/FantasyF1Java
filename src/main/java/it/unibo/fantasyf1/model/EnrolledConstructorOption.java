package it.unibo.fantasyf1.model;

/**
 * Scuderia iscritta a una specifica edizione.
 */
public record EnrolledConstructorOption(
    int editionId,
    int constructorId,
    String registeredName,
    String carName
) {

    @Override
    public String toString() {
        return "%s — %s".formatted(registeredName, carName);
    }
}
