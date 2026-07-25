package it.unibo.fantasyf1;

import it.unibo.fantasyf1.service.ApplicationServices;
import it.unibo.fantasyf1.ui.UiTheme;
import it.unibo.fantasyf1.ui.admin.AdminDashboard;
import it.unibo.fantasyf1.ui.user.FxTaskRunner;
import it.unibo.fantasyf1.ui.user.UserApplicationView;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.Objects;

public final class FantasyF1Application extends Application {

    private static final double INITIAL_WIDTH = 1180;
    private static final double INITIAL_HEIGHT = 820;
    private static final double APPLICATION_MIN_WIDTH = 960;
    private static final double APPLICATION_MIN_HEIGHT = 680;

    private ApplicationServices services;
    private FxTaskRunner taskRunner;
    private UserApplicationView applicationView;
    private Stage stage;
    private Scene scene;

    @Override
    public void start(final Stage primaryStage) {
        services = ApplicationServices.production();
        taskRunner = new FxTaskRunner();
        stage = primaryStage;
        scene = new Scene(
            new StackPane(),
            INITIAL_WIDTH,
            INITIAL_HEIGHT
        );
        UiTheme.apply(scene);

        stage.setScene(scene);
        stage.setMinWidth(APPLICATION_MIN_WIDTH);
        stage.setMinHeight(APPLICATION_MIN_HEIGHT);
        stage.setResizable(true);
        showModeSelection();
        stage.setMaximized(true);
        stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    private void showModeSelection() {
        closeUserApplication();

        final Label title = new Label("Fantasy Formula 1");
        title.getStyleClass().add("page-title");

        final Label prompt = new Label(
            "Scegli in quale modalità vuoi entrare nell'applicazione."
        );
        prompt.setWrapText(true);
        prompt.getStyleClass().add("muted");

        final Button userButton = new Button("Utente");
        final Button adminButton = new Button("Admin");
        for (Button button : new Button[] {userButton, adminButton}) {
            button.setMinWidth(180);
            button.setMinHeight(48);
            button.getStyleClass().add("mode-button");
        }

        userButton.setOnAction(event -> showUserApplication());
        adminButton.setOnAction(event -> showAdminApplication());

        final HBox choices = new HBox(18, userButton, adminButton);
        choices.setAlignment(Pos.CENTER);

        final VBox card = new VBox(24, title, prompt, choices);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(36));
        card.setMaxWidth(540);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        card.getStyleClass().addAll("card", "mode-card");

        final BorderPane root = new BorderPane(card);
        root.setPadding(new Insets(32));
        root.getStyleClass().add("app-root");
        BorderPane.setAlignment(card, Pos.CENTER);

        showRoot(
            root,
            "Fantasy Formula 1 — Selezione modalità"
        );
        userButton.requestFocus();
    }

    private void showUserApplication() {
        closeUserApplication();
        applicationView = new UserApplicationView(
            stage,
            services,
            taskRunner,
            this::showModeSelection
        );
        applicationView.show();
    }

    private void showAdminApplication() {
        closeUserApplication();
        final AdminDashboard dashboard = new AdminDashboard(
            services.admin(),
            this::showModeSelection
        );
        showRoot(
            dashboard.view(),
            "Fantasy Formula 1 — Amministrazione trusted"
        );
        dashboard.load();
    }

    private void showRoot(final Parent root, final String title) {
        scene.setRoot(Objects.requireNonNull(root));
        stage.setTitle(title);
    }

    private void closeUserApplication() {
        if (applicationView != null) {
            applicationView.close();
            applicationView = null;
        }
    }

    @Override
    public void stop() {
        closeUserApplication();
        if (taskRunner != null) {
            taskRunner.close();
        }
    }
}
