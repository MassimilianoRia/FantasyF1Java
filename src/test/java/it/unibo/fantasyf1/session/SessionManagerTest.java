package it.unibo.fantasyf1.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();
    }

    @Test
    void startsWithoutAnAuthenticatedUser() {
        assertFalse(sessionManager.isAuthenticated());
        assertTrue(sessionManager.current().isEmpty());
        assertThrows(
            IllegalStateException.class,
            sessionManager::requireAuthenticated
        );
    }

    @Test
    void exposesTheAuthenticatedImmutableSession() {
        final UserSession session = new UserSession(42, "mario.rossi");

        sessionManager.login(session);

        assertTrue(sessionManager.isAuthenticated());
        assertEquals(session, sessionManager.current().orElseThrow());
        assertEquals(session, sessionManager.requireAuthenticated());
        assertEquals(42, session.userId());
        assertEquals("mario.rossi", session.username());
    }

    @Test
    void logoutClearsTheSession() {
        sessionManager.login(new UserSession(7, "giulia.bianchi"));

        sessionManager.logout();

        assertFalse(sessionManager.isAuthenticated());
        assertTrue(sessionManager.current().isEmpty());
        assertThrows(
            IllegalStateException.class,
            sessionManager::requireAuthenticated
        );
    }
}
