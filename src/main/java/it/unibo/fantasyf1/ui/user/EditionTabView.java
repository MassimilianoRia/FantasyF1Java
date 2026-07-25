package it.unibo.fantasyf1.ui.user;

import it.unibo.fantasyf1.model.DriverOption;
import it.unibo.fantasyf1.model.EditionOverview;
import it.unibo.fantasyf1.model.EditionStatus;
import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.model.EnrolledConstructorOption;
import it.unibo.fantasyf1.model.RaceWeekend;
import it.unibo.fantasyf1.service.ApplicationServices;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.function.BiConsumer;

/**
 * Consultazione del calendario e degli iscritti all'edizione selezionata.
 */
final class EditionTabView {

    private static final double COMPLETION_TOTAL = 64.0;
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ApplicationServices services;
    private final FxTaskRunner tasks;
    private final BiConsumer<String, Boolean> status;

    private final VBox root = new VBox(14);
    private final Label title = new Label("Edizione");
    private final Label summary = new Label(
        "Seleziona un'edizione per visualizzarne il contenuto."
    );
    private final ProgressBar progress = new ProgressBar(0);
    private final Button refreshButton = new Button("Aggiorna edizione");
    private final ListView<RaceWeekend> weekends = new ListView<>();
    private final ListView<DriverOption> drivers = new ListView<>();
    private final ListView<EnrolledConstructorOption> constructors =
        new ListView<>();

    private Edizione edition;

    EditionTabView(
        final ApplicationServices services,
        final FxTaskRunner tasks,
        final BiConsumer<String, Boolean> status
    ) {
        this.services = services;
        this.tasks = tasks;
        this.status = status;
        configure();
    }

    Node content() {
        return root;
    }

    void setEdition(final Edizione selected) {
        edition = selected;
        clear();
        if (selected != null) {
            refresh();
        }
    }

    void refresh() {
        final Edizione selected = edition;
        if (selected == null) {
            return;
        }
        status.accept("Caricamento dell'edizione…", false);
        tasks.run(
            root,
            () -> services.editions().overview(selected.id()),
            overview -> {
                if (edition == null || edition.id() != selected.id()) {
                    return;
                }
                applyOverview(overview);
                status.accept(
                    "Contenuto di " + selected + " aggiornato.",
                    false
                );
            },
            failure -> status.accept(
                UserViewSupport.errorMessage(failure),
                true
            )
        );
    }

    private void configure() {
        root.setPadding(new Insets(12));

        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        summary.setWrapText(true);
        progress.setPrefWidth(360);
        progress.setMaxWidth(Double.MAX_VALUE);
        refreshButton.setOnAction(event -> refresh());

        UserViewSupport.renderList(weekends, this::weekendText);
        UserViewSupport.renderList(
            drivers,
            driver -> "%s %s (%s, #%d) — %s".formatted(
                driver.firstName(),
                driver.lastName(),
                driver.code(),
                driver.raceNumber(),
                driver.constructorName()
            )
        );
        UserViewSupport.renderList(
            constructors,
            constructor -> "%s — Vettura: %s".formatted(
                constructor.registeredName(),
                constructor.carName()
            )
        );

        weekends.setPlaceholder(new Label("Nessun GP previsto."));
        drivers.setPlaceholder(new Label("Nessun pilota iscritto."));
        constructors.setPlaceholder(new Label("Nessuna scuderia iscritta."));

        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox heading = new HBox(
            12,
            title,
            spacer,
            refreshButton
        );
        heading.setAlignment(Pos.CENTER_LEFT);

        final VBox summaryBox = new VBox(7, summary, progress);
        summaryBox.setPadding(new Insets(12));
        summaryBox.setStyle(
            "-fx-background-color: #f7f7f7;"
                + "-fx-border-color: #d7d7d7;"
                + "-fx-border-radius: 5;"
                + "-fx-background-radius: 5;"
        );

        final TabPane details = new TabPane(
            detailTab(
                "Calendario",
                "GP previsti, date e stato di conclusione.",
                weekends
            ),
            detailTab(
                "Piloti",
                "Piloti iscritti e relativa scuderia.",
                drivers
            ),
            detailTab(
                "Scuderie",
                "Scuderie e vetture iscritte.",
                constructors
            )
        );
        details.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(details, Priority.ALWAYS);

        root.getChildren().addAll(heading, summaryBox, details);
    }

    private void applyOverview(final EditionOverview overview) {
        final EditionStatus state = overview.status();
        title.setText(overview.edition().toString());
        final int missingWeekends = Math.max(0, 24 - state.weekends());
        final int missingConstructors = Math.max(
            0,
            10 - state.constructors()
        );
        final int missingDrivers = Math.max(0, 20 - state.drivers());
        summary.setText(
            state.complete()
                ? "Edizione completamente popolata: 24 GP, 10 scuderie "
                    + "e 20 piloti."
                : "%d/24 GP · %d/10 scuderie · %d/20 piloti. "
                    .formatted(
                        state.weekends(),
                        state.constructors(),
                        state.drivers()
                    )
        );
        final double completed = Math.min(state.weekends(), 24)
            + Math.min(state.constructors(), 10)
            + Math.min(state.drivers(), 20)
            + Math.min(state.constructorsWithTwoDrivers(), 10);
        progress.setProgress(completed / COMPLETION_TOTAL);
        weekends.setItems(
            FXCollections.observableArrayList(overview.weekends())
        );
        drivers.setItems(
            FXCollections.observableArrayList(overview.drivers())
        );
        constructors.setItems(
            FXCollections.observableArrayList(overview.constructors())
        );
    }

    private void clear() {
        title.setText(edition == null ? "Edizione" : edition.toString());
        summary.setText(
            edition == null
                ? "Seleziona un'edizione per visualizzarne il contenuto."
                : "Caricamento del contenuto…"
        );
        progress.setProgress(0);
        weekends.getItems().clear();
        drivers.getItems().clear();
        constructors.getItems().clear();
    }

    private String weekendText(final RaceWeekend weekend) {
        return "Round %d · %s · %s–%s · %s".formatted(
            weekend.round(),
            weekend.grandPrixName(),
            DATE_FORMAT.format(weekend.startDate()),
            DATE_FORMAT.format(weekend.endDate()),
            weekend.concluded() ? "Concluso" : "Non concluso"
        );
    }

    private static Tab detailTab(
        final String tabTitle,
        final String description,
        final ListView<?> list
    ) {
        final Label label = new Label(description);
        label.setWrapText(true);
        final VBox content = new VBox(10, label, list);
        content.setPadding(new Insets(14));
        VBox.setVgrow(list, Priority.ALWAYS);
        return new Tab(tabTitle, content);
    }
}
