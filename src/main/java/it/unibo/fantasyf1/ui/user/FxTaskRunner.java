package it.unibo.fantasyf1.ui.user;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Esegue tutte le chiamate ai service fuori dal JavaFX Application Thread.
 */
public final class FxTaskRunner implements AutoCloseable {

    private final AtomicInteger threadCounter = new AtomicInteger();
    private final ExecutorService executor = Executors.newCachedThreadPool(
        runnable -> {
            final Thread thread = new Thread(
                runnable,
                "fantasy-f1-user-worker-" + threadCounter.incrementAndGet()
            );
            thread.setDaemon(true);
            return thread;
        }
    );
    private final Map<Node, Integer> busyCounts = new IdentityHashMap<>();

    public <T> void run(
        final Node busyTarget,
        final Callable<T> operation,
        final Consumer<T> onSuccess,
        final Consumer<Throwable> onFailure
    ) {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException(
                "I task JavaFX devono essere avviati dal thread grafico"
            );
        }
        Objects.requireNonNull(operation, "L'operazione non può essere null");
        Objects.requireNonNull(onSuccess, "Il callback non può essere null");
        Objects.requireNonNull(onFailure, "Il callback non può essere null");

        markBusy(busyTarget);
        final Task<T> task = new Task<>() {
            @Override
            protected T call() throws Exception {
                return operation.call();
            }
        };
        task.setOnSucceeded(event -> {
            releaseBusy(busyTarget);
            onSuccess.accept(task.getValue());
        });
        task.setOnFailed(event -> {
            releaseBusy(busyTarget);
            onFailure.accept(task.getException());
        });
        task.setOnCancelled(event -> releaseBusy(busyTarget));

        try {
            executor.execute(task);
        } catch (RejectedExecutionException exception) {
            releaseBusy(busyTarget);
            onFailure.accept(exception);
        }
    }

    private void markBusy(final Node target) {
        if (target == null) {
            return;
        }
        final int count = busyCounts.getOrDefault(target, 0) + 1;
        busyCounts.put(target, count);
        target.setDisable(true);
    }

    private void releaseBusy(final Node target) {
        if (target == null) {
            return;
        }
        final int count = busyCounts.getOrDefault(target, 1) - 1;
        if (count <= 0) {
            busyCounts.remove(target);
            target.setDisable(false);
        } else {
            busyCounts.put(target, count);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
