package it.unibo.fantasyf1.ui.user;

import it.unibo.fantasyf1.model.DriverOption;
import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.model.RaceWeekend;
import it.unibo.fantasyf1.model.TeamDriver;
import it.unibo.fantasyf1.model.TeamSummary;
import it.unibo.fantasyf1.model.WeekendScoreRow;
import it.unibo.fantasyf1.service.ApplicationServices;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Operazioni utente U2, U3 e U8.
 */
final class TeamTabView {

    private final ApplicationServices services;
    private final FxTaskRunner tasks;
    private final BiConsumer<String, Boolean> status;
    private final Runnable onTeamCreated;

    private final VBox root = new VBox(10);
    private final ListView<TeamSummary> teamList = new ListView<>();
    private final ListView<TeamDriver> rosterList = new ListView<>();
    private final Label totalPoints = new Label("Seleziona un team.");
    private final TextField teamName = new TextField();
    private final ListView<DriverOption> driverOptions = new ListView<>();
    private final Label selectedDrivers = new Label(
        "Selezionati: 0 di 4"
    );
    private final Button createTeam = new Button("Crea team");
    private final ComboBox<TeamSummary> scoreTeam = new ComboBox<>();
    private final ComboBox<RaceWeekend> scoreWeekend = new ComboBox<>();
    private final ListView<WeekendScoreRow> scoreRows = new ListView<>();
    private final Button loadScores = new Button("Mostra punteggi");

    private Edizione edition;
    private long generation;

    TeamTabView(
        final ApplicationServices services,
        final FxTaskRunner tasks,
        final BiConsumer<String, Boolean> status,
        final Runnable onTeamCreated
    ) {
        this.services = services;
        this.tasks = tasks;
        this.status = status;
        this.onTeamCreated = onTeamCreated;
        configureControls();

        final Tab overview = new Tab("I miei team", createOverview());
        final Tab creation = new Tab("Crea team", createTeamForm());
        final Tab scores = new Tab("Punteggi weekend", createScores());
        overview.setClosable(false);
        creation.setClosable(false);
        scores.setClosable(false);
        final TabPane tabs = new TabPane(overview, creation, scores);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.getChildren().add(tabs);
        root.setPadding(new Insets(10, 0, 0, 0));
    }

    Node content() {
        return root;
    }

    void setEdition(final Edizione selectedEdition) {
        edition = selectedEdition;
        generation++;
        clearData();
        if (selectedEdition != null) {
            refresh();
        }
    }

    private void configureControls() {
        teamList.setPlaceholder(new Label("Nessun team nell'edizione"));
        rosterList.setPlaceholder(new Label("Nessuna composizione"));
        driverOptions.setPlaceholder(
            new Label("Nessun pilota iscritto all'edizione")
        );
        scoreRows.setPlaceholder(
            new Label("Seleziona team e weekend elaborato")
        );

        UserViewSupport.renderList(
            teamList,
            team -> "%s — %d punti".formatted(
                team.name(),
                team.totalPoints()
            )
        );
        UserViewSupport.renderList(
            rosterList,
            TeamDriver::toString
        );
        UserViewSupport.renderList(
            driverOptions,
            DriverOption::toString
        );
        UserViewSupport.renderCombo(
            scoreTeam,
            team -> "%s — %d punti".formatted(
                team.name(),
                team.totalPoints()
            )
        );
        UserViewSupport.renderCombo(
            scoreWeekend,
            RaceWeekend::toString
        );
        UserViewSupport.renderList(
            scoreRows,
            row -> "%s — %s %s: %s punti".formatted(
                row.code(),
                row.firstName(),
                row.lastName(),
                row.fantasyPoints() == null ? "non calcolato"
                    : row.fantasyPoints()
            )
        );

        teamList.getSelectionModel().selectedItemProperty().addListener(
            (observable, previous, selected) -> showTeam(selected)
        );
        driverOptions.getSelectionModel().setSelectionMode(
            SelectionMode.MULTIPLE
        );
        driverOptions.getSelectionModel().getSelectedItems().addListener(
            (ListChangeListener<DriverOption>) change ->
                updateCreateAvailability()
        );
        teamName.textProperty().addListener(
            (observable, previous, current) -> updateCreateAvailability()
        );
        scoreTeam.valueProperty().addListener(
            (observable, previous, current) -> updateScoreAvailability()
        );
        scoreWeekend.valueProperty().addListener(
            (observable, previous, current) -> updateScoreAvailability()
        );

        createTeam.setOnAction(event -> createTeam());
        loadScores.setOnAction(event -> loadWeekendScores());
        updateCreateAvailability();
        updateScoreAvailability();
    }

    private Node createOverview() {
        final Button refresh = new Button("Aggiorna team");
        refresh.setOnAction(event -> refresh());
        final VBox left = new VBox(8, new Label("Team"), teamList, refresh);
        VBox.setVgrow(teamList, Priority.ALWAYS);
        final VBox right = new VBox(
            8,
            new Label("Composizione"),
            totalPoints,
            rosterList
        );
        VBox.setVgrow(rosterList, Priority.ALWAYS);

        final SplitPane split = new SplitPane(left, right);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.44);
        return padded(split);
    }

    private Node createTeamForm() {
        teamName.setPromptText("Nome del team");
        final HBox nameRow = new HBox(
            10,
            new Label("Nome"),
            teamName
        );
        HBox.setHgrow(teamName, Priority.ALWAYS);
        final Label instructions = new Label(
            "Seleziona esattamente quattro piloti distinti "
                + "dell'edizione corrente."
        );
        instructions.setWrapText(true);
        final HBox actions = new HBox(12, selectedDrivers, createTeam);
        final VBox form = new VBox(
            10,
            instructions,
            nameRow,
            driverOptions,
            actions
        );
        VBox.setVgrow(driverOptions, Priority.ALWAYS);
        return padded(form);
    }

    private Node createScores() {
        scoreTeam.setPromptText("Team");
        scoreWeekend.setPromptText("Weekend terminato");
        scoreTeam.setPrefWidth(280);
        scoreWeekend.setPrefWidth(360);
        final HBox filters = new HBox(
            10,
            new Label("Team"),
            scoreTeam,
            new Label("Weekend"),
            scoreWeekend,
            loadScores
        );
        final VBox content = new VBox(
            10,
            new Label(
                "Dettaglio dei quattro punteggi fantasy del team."
            ),
            filters,
            scoreRows
        );
        VBox.setVgrow(scoreRows, Priority.ALWAYS);
        return padded(content);
    }

    private void refresh() {
        final Edizione requestedEdition = edition;
        if (requestedEdition == null) {
            return;
        }
        final long requestedGeneration = ++generation;
        status.accept("Caricamento dei team…", false);
        tasks.run(
            root,
            () -> new TeamData(
                services.teams().selectableDrivers(requestedEdition.id()),
                services.teams().myTeams(requestedEdition.id()),
                services.teams().processedWeekends(requestedEdition.id())
            ),
            data -> {
                if (!isCurrent(requestedEdition, requestedGeneration)) {
                    return;
                }
                applyData(data);
                status.accept(
                    "%d team e %d piloti disponibili nell'edizione %d."
                        .formatted(
                            data.teams().size(),
                            data.drivers().size(),
                            requestedEdition.anno()
                        ),
                    false
                );
            },
            failure -> {
                if (isCurrent(requestedEdition, requestedGeneration)) {
                    clearData();
                    status.accept(
                        UserViewSupport.errorMessage(failure),
                        true
                    );
                }
            }
        );
    }

    private void createTeam() {
        final Edizione requestedEdition = edition;
        if (requestedEdition == null) {
            return;
        }
        final String requestedName = teamName.getText();
        final List<Integer> driverIds = driverOptions
            .getSelectionModel()
            .getSelectedItems()
            .stream()
            .map(DriverOption::id)
            .toList();
        status.accept("Creazione del team in corso…", false);
        tasks.run(
            root,
            () -> services.teams().createTeam(
                requestedName,
                requestedEdition.id(),
                driverIds
            ),
            ignoredId -> {
                status.accept(
                    "Team '%s' creato correttamente."
                        .formatted(requestedName.trim()),
                    false
                );
                if (edition != null && edition.id() == requestedEdition.id()) {
                    teamName.clear();
                    driverOptions.getSelectionModel().clearSelection();
                    refresh();
                    onTeamCreated.run();
                }
            },
            failure -> status.accept(
                UserViewSupport.errorMessage(failure),
                true
            )
        );
    }

    private void loadWeekendScores() {
        final Edizione requestedEdition = edition;
        final TeamSummary requestedTeam = scoreTeam.getValue();
        final RaceWeekend requestedWeekend = scoreWeekend.getValue();
        if (
            requestedEdition == null
                || requestedTeam == null
                || requestedWeekend == null
        ) {
            return;
        }
        status.accept("Caricamento dei punteggi…", false);
        tasks.run(
            root,
            () -> services.teams().weekendBreakdown(
                requestedTeam.id(),
                requestedEdition.id(),
                requestedWeekend.grandPrixId()
            ),
            rows -> {
                if (edition != null
                    && edition.id() == requestedEdition.id()) {
                    scoreRows.setItems(
                        FXCollections.observableArrayList(rows)
                    );
                    status.accept(
                        "Visualizzati i quattro punteggi di '%s'."
                            .formatted(requestedTeam.name()),
                        false
                    );
                }
            },
            failure -> {
                scoreRows.getItems().clear();
                status.accept(
                    UserViewSupport.errorMessage(failure),
                    true
                );
            }
        );
    }

    private void applyData(final TeamData data) {
        teamList.setItems(
            FXCollections.observableArrayList(data.teams())
        );
        scoreTeam.setItems(
            FXCollections.observableArrayList(data.teams())
        );
        driverOptions.setItems(
            FXCollections.observableArrayList(data.drivers())
        );
        scoreWeekend.setItems(
            FXCollections.observableArrayList(data.weekends())
        );
        if (!data.teams().isEmpty()) {
            teamList.getSelectionModel().selectFirst();
            scoreTeam.getSelectionModel().selectFirst();
        }
        if (!data.weekends().isEmpty()) {
            scoreWeekend.getSelectionModel().selectFirst();
        }
        updateCreateAvailability();
        updateScoreAvailability();
    }

    private void clearData() {
        teamList.getItems().clear();
        rosterList.getItems().clear();
        driverOptions.getItems().clear();
        scoreTeam.getItems().clear();
        scoreWeekend.getItems().clear();
        scoreRows.getItems().clear();
        totalPoints.setText("Seleziona un team.");
        updateCreateAvailability();
        updateScoreAvailability();
    }

    private void showTeam(final TeamSummary team) {
        if (team == null) {
            rosterList.getItems().clear();
            totalPoints.setText("Seleziona un team.");
            return;
        }
        rosterList.setItems(
            FXCollections.observableArrayList(team.drivers())
        );
        totalPoints.setText(
            "%s — totale: %d punti".formatted(
                team.name(),
                team.totalPoints()
            )
        );
    }

    private void updateCreateAvailability() {
        final int count = driverOptions
            .getSelectionModel()
            .getSelectedItems()
            .size();
        selectedDrivers.setText("Selezionati: %d di 4".formatted(count));
        createTeam.setDisable(
            edition == null
                || teamName.getText() == null
                || teamName.getText().isBlank()
                || count != 4
        );
    }

    private void updateScoreAvailability() {
        loadScores.setDisable(
            edition == null
                || scoreTeam.getValue() == null
                || scoreWeekend.getValue() == null
        );
    }

    private boolean isCurrent(
        final Edizione requestedEdition,
        final long requestedGeneration
    ) {
        return edition != null
            && edition.id() == requestedEdition.id()
            && generation == requestedGeneration;
    }

    private static BorderPane padded(final Node content) {
        final BorderPane pane = new BorderPane(content);
        pane.setPadding(new Insets(12));
        return pane;
    }

    private record TeamData(
        List<DriverOption> drivers,
        List<TeamSummary> teams,
        List<RaceWeekend> weekends
    ) {

        TeamData {
            drivers = List.copyOf(drivers);
            teams = List.copyOf(teams);
            weekends = List.copyOf(weekends);
        }
    }
}
