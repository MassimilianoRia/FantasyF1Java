package it.unibo.fantasyf1.service;

/**
 * Dati del form U1. La password vive soltanto per la durata della richiesta e
 * non viene mai memorizzata né stampata.
 */
public record RegistrationRequest(
    String firstName,
    String lastName,
    String username,
    String password,
    String email,
    String phone
) {
}
