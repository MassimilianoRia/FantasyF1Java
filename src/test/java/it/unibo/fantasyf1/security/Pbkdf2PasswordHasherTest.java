package it.unibo.fantasyf1.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class Pbkdf2PasswordHasherTest {

    private static final String PASSWORD = "demo-password-2026";

    private Pbkdf2PasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new Pbkdf2PasswordHasher();
    }

    @Test
    void createsSaltedHashesAndVerifiesOnlyTheCorrectPassword() {
        final String firstHash = hasher.hash(PASSWORD);
        final String secondHash = hasher.hash(PASSWORD);

        assertNotEquals(PASSWORD, firstHash);
        assertNotEquals(firstHash, secondHash);
        assertTrue(hasher.verify(PASSWORD, firstHash));
        assertTrue(hasher.verify(PASSWORD, secondHash));
        assertFalse(hasher.verify("wrong-password", firstHash));
        assertFalse(hasher.isLegacy(firstHash));
        assertFalse(hasher.needsRehash(firstHash));
    }

    @Test
    void acceptsLegacySha256AndMarksItForMigration() {
        final String legacyHash = sha256(PASSWORD);

        assertTrue(hasher.isLegacy(legacyHash));
        assertTrue(hasher.verify(PASSWORD, legacyHash));
        assertFalse(hasher.verify("wrong-password", legacyHash));
        assertTrue(hasher.needsRehash(legacyHash));

        final String migratedHash = hasher.hash(PASSWORD);
        assertFalse(hasher.isLegacy(migratedHash));
        assertFalse(hasher.needsRehash(migratedHash));
        assertTrue(hasher.verify(PASSWORD, migratedHash));
    }

    private static String sha256(final String value) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 deve essere disponibile", exception);
        }
    }
}
