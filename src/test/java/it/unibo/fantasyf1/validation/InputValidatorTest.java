package it.unibo.fantasyf1.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import it.unibo.fantasyf1.error.AppException;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

final class InputValidatorTest {

    @Test
    void requiredLengthAndExactBoundariesAreEnforced() {
        assertEquals("testo", InputValidator.required("  testo  ", "Campo"));
        assertDoesNotThrow(() -> InputValidator.max("12345", 5, "Campo"));
        assertDoesNotThrow(() -> InputValidator.exact("ABC", 3, "Sigla"));
        assertThrows(
            AppException.class,
            () -> InputValidator.required("   ", "Campo")
        );
        assertThrows(
            AppException.class,
            () -> InputValidator.max("123456", 5, "Campo")
        );
        assertThrows(
            AppException.class,
            () -> InputValidator.exact("AB", 3, "Sigla")
        );
    }

    @Test
    void emailPhoneAndPasswordRulesAcceptReasonableValues() {
        assertEquals(
            "user@example.test",
            InputValidator.email("user@example.test")
        );
        assertEquals(
            "+39 333 1234567",
            InputValidator.phone("+39 333 1234567")
        );
        assertEquals(
            "password",
            InputValidator.password("password")
        );
        assertThrows(AppException.class, () -> InputValidator.email("bad"));
        assertThrows(AppException.class, () -> InputValidator.phone("123"));
        assertThrows(
            AppException.class,
            () -> InputValidator.password("short")
        );
    }

    @Test
    void numericAndDateRangesIncludeTheirBoundaries() {
        assertEquals(1, InputValidator.intRange(1, 1, 20, "Posizione"));
        assertEquals(20, InputValidator.intRange(20, 1, 20, "Posizione"));
        assertDoesNotThrow(() -> InputValidator.dateRange(
            LocalDate.of(2025, 1, 1),
            LocalDate.of(2025, 1, 1)
        ));
        assertThrows(
            AppException.class,
            () -> InputValidator.intRange(21, 1, 20, "Posizione")
        );
        assertThrows(
            AppException.class,
            () -> InputValidator.dateRange(
                LocalDate.of(2025, 1, 2),
                LocalDate.of(2025, 1, 1)
            )
        );
    }
}
