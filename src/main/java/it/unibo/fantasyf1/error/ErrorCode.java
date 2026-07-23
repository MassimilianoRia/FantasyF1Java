package it.unibo.fantasyf1.error;

/**
 * Categorie stabili di errore utilizzabili dai service e dalla UI.
 */
public enum ErrorCode {
    VALIDATION,
    DUPLICATE,
    NOT_FOUND,
    CONFLICT,
    AUTHENTICATION_REQUIRED,
    INVALID_CREDENTIALS,
    CONNECTION,
    CONSTRAINT_VIOLATION,
    DATABASE,
    INTERNAL
}
