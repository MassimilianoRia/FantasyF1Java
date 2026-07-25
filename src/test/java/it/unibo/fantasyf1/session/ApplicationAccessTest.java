package it.unibo.fantasyf1.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.fantasyf1.FantasyF1Application;

import javafx.application.Application;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Verifica l'accesso unificato senza introdurre ruoli nella sessione utente.
 */
final class ApplicationAccessTest {

    @Test
    void unifiedApplicationKeepsAdminOutsideTheUserSession() {
        assertTrue(
            Application.class.isAssignableFrom(FantasyF1Application.class)
        );
        assertTrue(UserSession.class.isRecord());
        assertEquals(
            List.of("userId", "username"),
            Arrays.stream(UserSession.class.getRecordComponents())
                .map(component -> component.getName())
                .toList()
        );
        assertFalse(
            Arrays.stream(UserSession.class.getDeclaredFields())
                .map(field -> field.getName().toLowerCase())
                .anyMatch(name -> name.contains("admin")
                    || name.contains("role"))
        );
    }
}
