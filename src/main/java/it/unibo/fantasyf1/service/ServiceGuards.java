package it.unibo.fantasyf1.service;

import it.unibo.fantasyf1.error.AppException;
import it.unibo.fantasyf1.error.ErrorCode;
import it.unibo.fantasyf1.session.SessionManager;
import it.unibo.fantasyf1.session.UserSession;

/**
 * Guardie comuni ai service.
 */
final class ServiceGuards {

    private ServiceGuards() {
    }

    static UserSession authenticated(final SessionManager sessions) {
        return sessions.current().orElseThrow(() -> new AppException(
            ErrorCode.AUTHENTICATION_REQUIRED,
            "È necessario effettuare il login per continuare."
        ));
    }

    static AppException notFound(final String message) {
        return new AppException(ErrorCode.NOT_FOUND, message);
    }

    static AppException conflict(final String message) {
        return new AppException(ErrorCode.CONFLICT, message);
    }

    static AppException invalid(final String message) {
        return new AppException(ErrorCode.VALIDATION, message);
    }
}
