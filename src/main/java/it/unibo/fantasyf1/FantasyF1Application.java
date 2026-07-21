package it.unibo.fantasyf1;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public final class FantasyF1Application extends Application {

    @Override
    public void start(final Stage stage) {
        final StackPane root = new StackPane(
            new Label("Fantasy Formula 1")
        );

        final Scene scene = new Scene(root, 1000, 700);

        stage.setTitle("Fantasy Formula 1");
        stage.setScene(scene);
        stage.show();
    }
}
