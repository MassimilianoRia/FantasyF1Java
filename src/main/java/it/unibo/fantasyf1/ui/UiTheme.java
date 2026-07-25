package it.unibo.fantasyf1.ui;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;

import java.util.Objects;

/**
 * Applica il foglio di stile condiviso alle scene e ai dialoghi JavaFX.
 */
public final class UiTheme {

    private static final String STYLESHEET = Objects.requireNonNull(
        UiTheme.class.getResource("fantasy-f1.css"),
        "Foglio di stile dell'applicazione non trovato"
    ).toExternalForm();

    private UiTheme() {
    }

    public static void apply(final Scene scene) {
        addIfMissing(scene.getStylesheets());
    }

    public static void apply(final DialogPane dialogPane) {
        addIfMissing(dialogPane.getStylesheets());
    }

    /**
     * Mantiene scorrevole una lista informativa, ma impedisce che le sue
     * righe sembrino azionabili o possano essere selezionate.
     *
     * @param listView lista da configurare in sola lettura
     */
    public static void configureReadOnly(final ListView<?> listView) {
        Objects.requireNonNull(listView);
        if (!listView.getStyleClass().contains("read-only-list")) {
            listView.getStyleClass().add("read-only-list");
        }
        listView.setFocusTraversable(false);
        listView.getSelectionModel().clearSelection();
        listView.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            Node target = event.getPickResult().getIntersectedNode();
            while (target != null && target != listView) {
                if (target instanceof ListCell<?>) {
                    event.consume();
                    return;
                }
                target = target.getParent();
            }
        });
    }

    private static void addIfMissing(
        final java.util.List<String> stylesheets
    ) {
        if (!stylesheets.contains(STYLESHEET)) {
            stylesheets.add(STYLESHEET);
        }
    }
}
