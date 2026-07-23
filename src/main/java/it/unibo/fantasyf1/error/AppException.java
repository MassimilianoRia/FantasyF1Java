package it.unibo.fantasyf1.error;

import java.util.Objects;

/**
 * Errore applicativo con categoria sicura da tradurre in un messaggio UI.
 */
public class AppException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final ErrorCode code;

    public AppException(final ErrorCode code, final String message) {
        super(message);
        this.code = Objects.requireNonNull(
            code,
            "Il codice errore non può essere null"
        );
    }

    public AppException(
        final ErrorCode code,
        final String message,
        final Throwable cause
    ) {
        super(message, cause);
        this.code = Objects.requireNonNull(
            code,
            "Il codice errore non può essere null"
        );
    }

    public ErrorCode code() {
        return code;
    }

    public ErrorCode getCode() {
        return code;
    }
}
