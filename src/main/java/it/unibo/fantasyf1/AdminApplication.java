package it.unibo.fantasyf1;

import it.unibo.fantasyf1.service.AdminService;
import it.unibo.fantasyf1.service.ApplicationServices;
import it.unibo.fantasyf1.ui.admin.AdminDashboard;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Applicazione JavaFX riservata all'amministratore trusted della piattaforma.
 */
public final class AdminApplication extends Application {

    private static final double INITIAL_WIDTH = 1180;
    private static final double INITIAL_HEIGHT = 820;

    @Override
    public void start(final Stage stage) {
        final AdminService admin = ApplicationServices.production().admin();
        final AdminDashboard dashboard = new AdminDashboard(admin);
        final Scene scene = new Scene(
            dashboard.view(),
            INITIAL_WIDTH,
            INITIAL_HEIGHT
        );

        stage.setTitle("Fantasy Formula 1 — Amministrazione trusted");
        stage.setMinWidth(960);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();

        dashboard.load();
    }
}
