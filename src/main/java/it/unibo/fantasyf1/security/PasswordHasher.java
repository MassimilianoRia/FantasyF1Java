package it.unibo.fantasyf1.security;

/**
 * Contratto per la memorizzazione e la verifica sicura delle password.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    boolean verify(String rawPassword, String encodedHash);

    boolean isLegacy(String encodedHash);

    boolean needsRehash(String encodedHash);
}
