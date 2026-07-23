package it.unibo.fantasyf1.session;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Mantiene la sessione applicativa corrente senza esporre identificativi
 * modificabili dalla UI.
 */
public final class SessionManager {

    private final AtomicReference<UserSession> currentSession =
        new AtomicReference<>();

    /**
     * Imposta la sessione autenticata corrente.
     *
     * @param session sessione immutabile
     */
    public void login(final UserSession session) {
        currentSession.set(Objects.requireNonNull(
            session,
            "La sessione non può essere null"
        ));
    }

    /**
     * Crea e imposta una sessione autenticata.
     *
     * @param userId identificativo dell'utente
     * @param username username dell'utente
     * @return sessione creata
     */
    public UserSession login(final int userId, final String username) {
        final UserSession session = new UserSession(userId, username);
        login(session);
        return session;
    }

    public Optional<UserSession> current() {
        return Optional.ofNullable(currentSession.get());
    }

    public UserSession requireAuthenticated() {
        final UserSession session = currentSession.get();
        if (session == null) {
            throw new IllegalStateException(
                "È necessario autenticarsi per continuare"
            );
        }
        return session;
    }

    public boolean isAuthenticated() {
        return currentSession.get() != null;
    }

    public void logout() {
        currentSession.set(null);
    }
}
