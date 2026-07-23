package it.unibo.fantasyf1.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.fantasyf1.AdminApp;
import it.unibo.fantasyf1.App;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Blocca la scelta normativa: l'admin è un entry point trusted, non un ruolo
 * assegnabile alla sessione utente.
 */
final class AdminSeparationTest {

    @Test
    void userSessionHasNoAdminRoleAndEntryPointsRemainSeparate() {
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
        assertNotEquals(App.class, AdminApp.class);
    }
}
