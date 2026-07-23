package it.unibo.fantasyf1;

import it.unibo.fantasyf1.service.ApplicationServices;

import javafx.application.Application;

import java.util.Arrays;

public final class App {

    private App() {
    }

    public static void main(final String[] args) {
        if (Arrays.asList(args).contains("--smoke")) {
            smokeCompositionRoot();
            return;
        }
        Application.launch(FantasyF1Application.class, args);
    }

    private static void smokeCompositionRoot() {
        final ApplicationServices services = ApplicationServices.production();
        if (
            services.authentication() == null
                || services.editions() == null
                || services.teams() == null
                || services.leagues() == null
                || services.admin() == null
                || services.authentication().isAuthenticated()
        ) {
            throw new IllegalStateException(
                "Composition root utente non valido"
            );
        }
        System.out.println(
            "Smoke test area utente riuscito: "
                + "composition root creato senza accesso al database."
        );
    }
}
