package it.unibo.fantasyf1.validation;

import it.unibo.fantasyf1.error.AppException;
import it.unibo.fantasyf1.error.ErrorCode;

import java.time.LocalDate;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validazioni condivise tra form e service. I metodi restituiscono il valore
 * normalizzato quando ciò è utile e sollevano un errore applicativo con un
 * messaggio in italiano in caso di input non valido.
 */
public final class InputValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+"
            + "@[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?"
            + "(?:\\.[A-Z0-9](?:[A-Z0-9-]{0,61}[A-Z0-9])?)+$",
        Pattern.CASE_INSENSITIVE
    );
    private static final Pattern PHONE_CHARACTERS = Pattern.compile(
        "^\\+?[0-9 ()-]+$"
    );
    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 128;

    public InputValidator() {
    }

    public static String required(
        final String value,
        final String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw invalid("%s è obbligatorio.".formatted(label(fieldName)));
        }
        return value.trim();
    }

    public static <T> T required(
        final T value,
        final String fieldName
    ) {
        if (value == null) {
            throw invalid("%s è obbligatorio.".formatted(label(fieldName)));
        }
        return value;
    }

    public static String max(
        final String value,
        final int maximum,
        final String fieldName
    ) {
        positiveLength(maximum);
        if (value != null && value.length() > maximum) {
            throw invalid(
                "%s non può superare %d caratteri."
                    .formatted(label(fieldName), maximum)
            );
        }
        return value;
    }

    public static String max(
        final String value,
        final String fieldName,
        final int maximum
    ) {
        return max(value, maximum, fieldName);
    }

    public static String maxLength(
        final String value,
        final int maximum,
        final String fieldName
    ) {
        return max(value, maximum, fieldName);
    }

    public static String exact(
        final String value,
        final int expectedLength,
        final String fieldName
    ) {
        positiveLength(expectedLength);
        final String normalized = required(value, fieldName);
        if (normalized.length() != expectedLength) {
            throw invalid(
                "%s deve contenere esattamente %d caratteri."
                    .formatted(label(fieldName), expectedLength)
            );
        }
        return normalized;
    }

    public static String exact(
        final String value,
        final String fieldName,
        final int expectedLength
    ) {
        return exact(value, expectedLength, fieldName);
    }

    public static String exactLength(
        final String value,
        final int expectedLength,
        final String fieldName
    ) {
        return exact(value, expectedLength, fieldName);
    }

    public static String email(final String value) {
        return email(value, "L'email");
    }

    public static String email(
        final String value,
        final String fieldName
    ) {
        final String normalized = required(value, fieldName);
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw invalid("%s non è valida.".formatted(label(fieldName)));
        }
        return normalized;
    }

    public static String phone(final String value) {
        return phone(value, "Il telefono");
    }

    public static String phone(
        final String value,
        final String fieldName
    ) {
        final String normalized = required(value, fieldName);
        final long digitCount = normalized.chars()
            .filter(Character::isDigit)
            .count();
        if (
            normalized.length() > 20
                || digitCount < 6
                || digitCount > 15
                || !PHONE_CHARACTERS.matcher(normalized).matches()
        ) {
            throw invalid("%s non è valido.".formatted(label(fieldName)));
        }
        return normalized;
    }

    public static String password(final String value) {
        return password(value, "La password");
    }

    public static String password(
        final String value,
        final String fieldName
    ) {
        if (value == null || value.isBlank()) {
            throw invalid("%s è obbligatoria.".formatted(label(fieldName)));
        }
        if (
            value.length() < MIN_PASSWORD_LENGTH
                || value.length() > MAX_PASSWORD_LENGTH
        ) {
            throw invalid(
                "%s deve contenere tra %d e %d caratteri."
                    .formatted(
                        label(fieldName),
                        MIN_PASSWORD_LENGTH,
                        MAX_PASSWORD_LENGTH
                    )
            );
        }
        return value;
    }

    public static int intRange(
        final int value,
        final int minimum,
        final int maximum,
        final String fieldName
    ) {
        validateRange(minimum, maximum);
        if (value < minimum || value > maximum) {
            throw invalid(
                "%s deve essere compreso tra %d e %d."
                    .formatted(label(fieldName), minimum, maximum)
            );
        }
        return value;
    }

    public static int intRange(
        final String value,
        final int minimum,
        final int maximum,
        final String fieldName
    ) {
        final String normalized = required(value, fieldName);
        try {
            return intRange(
                Integer.parseInt(normalized),
                minimum,
                maximum,
                fieldName
            );
        } catch (NumberFormatException exception) {
            throw invalid(
                "%s deve essere un numero intero.".formatted(label(fieldName))
            );
        }
    }

    public static void dateRange(
        final LocalDate startDate,
        final LocalDate endDate
    ) {
        dateRange(startDate, endDate, "La data di inizio", "La data di fine");
    }

    public static void dateRange(
        final LocalDate startDate,
        final LocalDate endDate,
        final String startFieldName,
        final String endFieldName
    ) {
        required(startDate, startFieldName);
        required(endDate, endFieldName);
        if (endDate.isBefore(startDate)) {
            throw invalid(
                "%s non può precedere %s."
                    .formatted(
                        label(endFieldName),
                        label(startFieldName).toLowerCase()
                    )
            );
        }
    }

    private static AppException invalid(final String message) {
        return new AppException(ErrorCode.VALIDATION, message);
    }

    private static String label(final String fieldName) {
        final String normalized = Objects.requireNonNullElse(
            fieldName,
            "Il campo"
        ).trim();
        return normalized.isEmpty() ? "Il campo" : normalized;
    }

    private static void positiveLength(final int length) {
        if (length <= 0) {
            throw new IllegalArgumentException(
                "La lunghezza deve essere positiva"
            );
        }
    }

    private static void validateRange(
        final int minimum,
        final int maximum
    ) {
        if (minimum > maximum) {
            throw new IllegalArgumentException(
                "Il limite minimo non può superare quello massimo"
            );
        }
    }
}
