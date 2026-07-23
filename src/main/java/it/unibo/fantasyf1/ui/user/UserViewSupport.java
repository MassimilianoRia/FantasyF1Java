package it.unibo.fantasyf1.ui.user;

import it.unibo.fantasyf1.error.AppException;

import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;

import java.util.function.Function;

final class UserViewSupport {

    private UserViewSupport() {
    }

    static String errorMessage(final Throwable failure) {
        Throwable current = failure;
        while (
            current != null
                && !(current instanceof AppException)
                && current.getCause() != null
                && current.getCause() != current
        ) {
            current = current.getCause();
        }
        if (current instanceof AppException appException) {
            return appException.getMessage();
        }
        if (
            current instanceof IllegalArgumentException
                && current.getMessage() != null
                && !current.getMessage().isBlank()
        ) {
            return current.getMessage();
        }
        final String type = current == null
            ? "sconosciuto"
            : current.getClass().getSimpleName();
        System.err.println(
            "Errore inatteso nell'area utente (" + type + ")."
        );
        return "Si è verificato un errore inatteso. Riprova.";
    }

    static <T> void renderList(
        final ListView<T> list,
        final Function<T, String> renderer
    ) {
        list.setCellFactory(ignored -> new TextCell<>(renderer));
    }

    static <T> void renderCombo(
        final ComboBox<T> combo,
        final Function<T, String> renderer
    ) {
        combo.setCellFactory(ignored -> new TextCell<>(renderer));
        combo.setButtonCell(new TextCell<>(renderer));
    }

    private static final class TextCell<T> extends ListCell<T> {

        private final Function<T, String> renderer;

        TextCell(final Function<T, String> renderer) {
            this.renderer = renderer;
        }

        @Override
        protected void updateItem(final T item, final boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : renderer.apply(item));
        }
    }
}
