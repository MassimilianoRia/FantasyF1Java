package it.unibo.fantasyf1.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unibo.fantasyf1.FantasyF1Application;
import it.unibo.fantasyf1.ui.admin.AdminDashboard;
import it.unibo.fantasyf1.ui.user.UserApplicationView;

import javafx.application.Application;

import java.lang.reflect.Method;
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

    @Test
    void a10IsExposedOnlyByTheTrustedAdminInterface()
        throws ClassNotFoundException {
        assertTrue(hasDeclaredMethod(AdminDashboard.class, "reopenWeekend"));
        for (Class<?> userView : List.of(
            UserApplicationView.class,
            Class.forName("it.unibo.fantasyf1.ui.user.EditionTabView"),
            Class.forName("it.unibo.fantasyf1.ui.user.TeamTabView"),
            Class.forName("it.unibo.fantasyf1.ui.user.LeagueTabView")
        )) {
            assertFalse(hasDeclaredMethod(userView, "reopenWeekend"));
        }
    }

    private static boolean hasDeclaredMethod(
        final Class<?> type,
        final String name
    ) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .anyMatch(name::equals);
    }
}
