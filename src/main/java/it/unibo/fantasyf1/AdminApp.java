package it.unibo.fantasyf1;

import it.unibo.fantasyf1.service.ApplicationServices;

import javafx.application.Application;

import java.util.Arrays;

/**
 * Entry point separato per l'area amministrativa trusted.
 */
public final class AdminApp {

    private AdminApp() {
    }

    public static void main(final String[] args) {
        if (Arrays.asList(args).contains("--smoke")) {
            final ApplicationServices services =
                ApplicationServices.production();
            if (services.admin() == null) {
                throw new IllegalStateException(
                    "Composition root amministrativo non valido"
                );
            }
            System.out.println(
                "Smoke test area admin riuscito: composition root creato "
                    + "senza accesso al database."
            );
            return;
        }
        Application.launch(AdminApplication.class, args);
    }
}
