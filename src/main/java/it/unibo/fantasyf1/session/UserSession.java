package it.unibo.fantasyf1.session;

import java.util.Objects;

/**
 * Identità immutabile dell'utente autenticato.
 */
public record UserSession(int userId, String username) {

    public UserSession {
        if (userId <= 0) {
            throw new IllegalArgumentException(
                "L'identificativo utente deve essere positivo"
            );
        }
        username = Objects.requireNonNull(
            username,
            "Lo username non può essere null"
        ).trim();
        if (username.isEmpty()) {
            throw new IllegalArgumentException(
                "Lo username non può essere vuoto"
            );
        }
        if (username.length() > 50) {
            throw new IllegalArgumentException(
                "Lo username non può superare 50 caratteri"
            );
        }
    }
}
