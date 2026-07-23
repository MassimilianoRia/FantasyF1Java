package it.unibo.fantasyf1.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.fantasyf1.error.AppException;
import it.unibo.fantasyf1.error.ErrorCode;
import it.unibo.fantasyf1.scoring.SimpleScoringPolicy;
import it.unibo.fantasyf1.security.Pbkdf2PasswordHasher;
import it.unibo.fantasyf1.session.SessionManager;
import it.unibo.fantasyf1.session.UserSession;
import it.unibo.fantasyf1.testutil.TestDatabase;
import it.unibo.fantasyf1.testutil.TestFixtures;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class AuthenticationServiceH2Test {

    private static final String PASSWORD = "password-demo-2026";

    private TestDatabase database;
    private TestFixtures fixtures;
    private Pbkdf2PasswordHasher hasher;
    private AuthenticationService authentication;

    @BeforeEach
    void setUp() {
        database = new TestDatabase();
        fixtures = new TestFixtures(database);
        hasher = new Pbkdf2PasswordHasher();
        final ApplicationServices services = new ApplicationServices(
            database,
            Clock.systemUTC(),
            hasher,
            new SimpleScoringPolicy(),
            new SessionManager()
        );
        authentication = services.authentication();
    }

    @Test
    void registrationHashesPasswordAndValidLoginCreatesSession() {
        final int userId = authentication.register(registration(
            "mario.rossi",
            "mario.rossi@example.test"
        ));

        final String storedHash = database.queryString(
            "SELECT PasswordHash FROM UTENTE WHERE IdUtente = ?",
            userId
        );
        assertNotEquals(PASSWORD, storedHash);
        assertTrue(hasher.verify(PASSWORD, storedHash));

        final UserSession session =
            authentication.login("mario.rossi", PASSWORD);
        assertEquals(userId, session.userId());
        assertEquals("mario.rossi", session.username());
        assertTrue(authentication.isAuthenticated());

        authentication.logout();
        assertFalse(authentication.isAuthenticated());
    }

    @Test
    void invalidUsernameAndPasswordDoNotCreateSession() {
        authentication.register(registration(
            "mario.rossi",
            "mario.rossi@example.test"
        ));

        final AppException wrongPassword = assertThrows(
            AppException.class,
            () -> authentication.login("mario.rossi", "password-errata")
        );
        final AppException missingUser = assertThrows(
            AppException.class,
            () -> authentication.login("utente.assente", PASSWORD)
        );

        assertEquals(ErrorCode.INVALID_CREDENTIALS, wrongPassword.code());
        assertEquals(ErrorCode.INVALID_CREDENTIALS, missingUser.code());
        assertFalse(authentication.isAuthenticated());
    }

    @Test
    void validLegacyLoginMigratesSha256InsideTheTransaction() {
        final String legacyHash = sha256(PASSWORD);
        final int userId = fixtures.user("legacy.user", legacyHash);

        authentication.login("legacy.user", PASSWORD);

        final String migratedHash = database.queryString(
            "SELECT PasswordHash FROM UTENTE WHERE IdUtente = ?",
            userId
        );
        assertNotEquals(legacyHash, migratedHash);
        assertFalse(hasher.isLegacy(migratedHash));
        assertFalse(hasher.needsRehash(migratedHash));
        assertTrue(hasher.verify(PASSWORD, migratedHash));
    }

    @Test
    void duplicateUsernameAndEmailAreRejectedWithoutExtraRows() {
        authentication.register(registration(
            "unique.user",
            "unique@example.test"
        ));

        final AppException duplicateUsername = assertThrows(
            AppException.class,
            () -> authentication.register(registration(
                "unique.user",
                "different@example.test"
            ))
        );
        final AppException duplicateEmail = assertThrows(
            AppException.class,
            () -> authentication.register(registration(
                "different.user",
                "unique@example.test"
            ))
        );

        assertEquals(ErrorCode.DUPLICATE, duplicateUsername.code());
        assertEquals(ErrorCode.DUPLICATE, duplicateEmail.code());
        assertEquals(1, database.queryInt("SELECT COUNT(*) FROM UTENTE"));
    }

    private static RegistrationRequest registration(
        final String username,
        final String email
    ) {
        return new RegistrationRequest(
            "Mario",
            "Rossi",
            username,
            PASSWORD,
            email,
            "+39 333 1234567"
        );
    }

    private static String sha256(final String value) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(
                    value.getBytes(StandardCharsets.UTF_8)
                )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new AssertionError("SHA-256 deve essere disponibile", exception);
        }
    }
}
