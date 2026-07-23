package it.unibo.fantasyf1.ui.admin;

import javafx.concurrent.Task;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Esegue il lavoro amministrativo fuori dal JavaFX Application Thread.
 */
final class AdminTasks {

    private AdminTasks() {
    }

    static <T> void run(
        final String operationName,
        final Supplier<T> operation,
        final Consumer<T> onSuccess,
        final Consumer<Throwable> onFailure
    ) {
        Objects.requireNonNull(operationName);
        Objects.requireNonNull(operation);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onFailure);

        final Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return operation.get();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> onFailure.accept(task.getException()));

        final Thread worker = new Thread(
            task,
            "fantasy-f1-admin-" + operationName
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
        );
        worker.setDaemon(true);
        worker.start();
    }
}
