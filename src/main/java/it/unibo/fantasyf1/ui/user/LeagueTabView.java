package it.unibo.fantasyf1.ui.user;

import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.model.JoinedLeague;
import it.unibo.fantasyf1.model.LegaDisponibile;
import it.unibo.fantasyf1.model.StandingRow;
import it.unibo.fantasyf1.model.TeamSummary;
import it.unibo.fantasyf1.service.ApplicationServices;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
 * Operazioni utente U4, U5, U6, U7 e U9.
 */
final class LeagueTabView {

    private final ApplicationServices services;
    private final FxTaskRunner tasks;
    private final BiConsumer<String, Boolean> status;

    private final VBox root = new VBox(10);
    private final ListView<LegaDisponibile> availableLeagues =
        new ListView<>();
    private final ComboBox<LegaDisponibile> joinLeague = new ComboBox<>();
    private final ComboBox<TeamSummary> joinTeam = new ComboBox<>();
    private final Button join = new Button("Iscrivi team");
    private final TextField leagueName = new TextField();
    private final Button createLeague = new Button("Crea lega");
    private final ListView<JoinedLeague> joinedLeagues = new ListView<>();
    private final ComboBox<LegaDisponibile> standingsLeague =
        new ComboBox<>();
    private final Button loadStandings = new Button("Mostra classifica");
    private final ListView<StandingRow> standings = new ListView<>();

    private Edizione edition;
    private long generation;

    LeagueTabView(
        final ApplicationServices services,
        final FxTaskRunner tasks,
        final BiConsumer<String, Boolean> status
    ) {
        this.services = services;
        this.tasks = tasks;
        this.status = status;
        configureControls();

        final Tab available =
            new Tab("Leghe disponibili", createAvailableLeagues());
        final Tab creation = new Tab("Crea lega", createLeagueForm());
        final Tab joined = new Tab("Le mie partecipazioni", createJoined());
        final Tab ranking = new Tab("Classifica", createStandings());
        available.setClosable(false);
        creation.setClosable(false);
        joined.setClosable(false);
        ranking.setClosable(false);
        final TabPane tabs =
            new TabPane(available, creation, joined, ranking);
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
        availableLeagues.setPlaceholder(
            new Label("Nessuna lega disponibile nell'edizione")
        );
        joinedLeagues.setPlaceholder(
            new Label("Nessun tuo team partecipa a una lega")
        );
        standings.setPlaceholder(
            new Label("Seleziona una lega per consultarne la classifica")
        );

        UserViewSupport.renderList(
            availableLeagues,
            LeagueTabView::leagueText
        );
        UserViewSupport.renderCombo(
            joinLeague,
            LeagueTabView::leagueText
        );
        UserViewSupport.renderCombo(
            standingsLeague,
            LeagueTabView::leagueText
        );
        UserViewSupport.renderCombo(
            joinTeam,
            team -> "%s — %d punti".formatted(
                team.name(),
                team.totalPoints()
            )
        );
        UserViewSupport.renderList(
            joinedLeagues,
            joined -> "%s — team: %s".formatted(
                joined.leagueName(),
                joined.teamName()
            )
        );
        standings.setCellFactory(ignored -> new ListCell<>() {
            @Override
            protected void updateItem(
                final StandingRow row,
                final boolean empty
            ) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setText(null);
                } else {
                    setText(
                        "%d. %s — %s — %d punti".formatted(
                            getIndex() + 1,
                            row.teamName(),
                            row.ownerUsername(),
                            row.totalPoints()
                        )
                    );
                }
            }
        });

        leagueName.textProperty().addListener(
            (observable, previous, current) -> updateCreateAvailability()
        );
        joinLeague.valueProperty().addListener(
            (observable, previous, current) -> updateJoinAvailability()
        );
        joinTeam.valueProperty().addListener(
            (observable, previous, current) -> updateJoinAvailability()
        );
        standingsLeague.valueProperty().addListener(
            (observable, previous, current) ->
                updateStandingsAvailability()
        );

        createLeague.setOnAction(event -> createLeague());
        join.setOnAction(event -> joinLeague());
        loadStandings.setOnAction(event -> loadStandings());
        updateCreateAvailability();
        updateJoinAvailability();
        updateStandingsAvailability();
    }

    private Node createAvailableLeagues() {
        joinLeague.setPromptText("Lega");
        joinLeague.setPrefWidth(330);
        joinTeam.setPromptText("Uno dei tuoi team");
        joinTeam.setPrefWidth(300);
        final HBox actions = new HBox(
            10,
            new Label("Lega"),
            joinLeague,
            new Label("Team"),
            joinTeam,
            join
        );
        final Button refresh = new Button("Aggiorna leghe");
        refresh.setOnAction(event -> refresh());
        final VBox content = new VBox(
            10,
            new Label(
                "Leghe dell'edizione selezionata e relativa "
                    + "amministrazione."
            ),
            availableLeagues,
            actions,
            refresh
        );
        VBox.setVgrow(availableLeagues, Priority.ALWAYS);
        return padded(content);
    }

    private Node createLeagueForm() {
        leagueName.setPromptText("Nome della nuova lega");
        final HBox form = new HBox(
            10,
            new Label("Nome"),
            leagueName,
            createLeague
        );
        HBox.setHgrow(leagueName, Priority.ALWAYS);
        return padded(new VBox(
            12,
            new Label(
                "Creando la lega ne diventerai l'amministratore. "
                    + "Potrai comunque partecipare con un tuo team."
            ),
            form
        ));
    }

    private Node createJoined() {
        final Button refresh = new Button("Aggiorna partecipazioni");
        refresh.setOnAction(event -> refresh());
        final VBox content = new VBox(
            10,
            new Label(
                "Leghe a cui partecipano i tuoi team nell'edizione."
            ),
            joinedLeagues,
            refresh
        );
        VBox.setVgrow(joinedLeagues, Priority.ALWAYS);
        return padded(content);
    }

    private Node createStandings() {
        standingsLeague.setPromptText("Lega");
        standingsLeague.setPrefWidth(360);
        final HBox filters = new HBox(
            10,
            new Label("Lega"),
            standingsLeague,
            loadStandings
        );
        final VBox content = new VBox(
            10,
            new Label(
                "Classifica ordinata per punti totali e nome del team."
            ),
            filters,
            standings
        );
        VBox.setVgrow(standings, Priority.ALWAYS);
        return padded(content);
    }

    void refresh() {
        final Edizione requestedEdition = edition;
        if (requestedEdition == null) {
            return;
        }
        final long requestedGeneration = ++generation;
        status.accept("Caricamento delle leghe…", false);
        tasks.run(
            root,
            () -> new LeagueData(
                services.leagues().availableLeagues(requestedEdition.id()),
                services.leagues().joinedLeagues(requestedEdition.id()),
                services.teams().myTeams(requestedEdition.id())
            ),
            data -> {
                if (!isCurrent(requestedEdition, requestedGeneration)) {
                    return;
                }
                applyData(data);
                status.accept(
                    "%d leghe disponibili, %d partecipazioni personali."
                        .formatted(
                            data.available().size(),
                            data.joined().size()
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

    private void createLeague() {
        final Edizione requestedEdition = edition;
        if (requestedEdition == null) {
            return;
        }
        final String requestedName = leagueName.getText();
        status.accept("Creazione della lega in corso…", false);
        tasks.run(
            root,
            () -> services.leagues().createLeague(
                requestedName,
                requestedEdition.id()
            ),
            ignoredId -> {
                status.accept(
                    "Lega '%s' creata correttamente."
                        .formatted(requestedName.trim()),
                    false
                );
                if (edition != null && edition.id() == requestedEdition.id()) {
                    leagueName.clear();
                    refresh();
                }
            },
            failure -> status.accept(
                UserViewSupport.errorMessage(failure),
                true
            )
        );
    }

    private void joinLeague() {
        final Edizione requestedEdition = edition;
        final LegaDisponibile requestedLeague = joinLeague.getValue();
        final TeamSummary requestedTeam = joinTeam.getValue();
        if (
            requestedEdition == null
                || requestedLeague == null
                || requestedTeam == null
        ) {
            return;
        }
        status.accept("Iscrizione del team in corso…", false);
        tasks.run(
            root,
            () -> {
                services.leagues().joinLeague(
                    requestedLeague.id(),
                    requestedTeam.id()
                );
                return null;
            },
            ignored -> {
                status.accept(
                    "Team '%s' iscritto a '%s'."
                        .formatted(
                            requestedTeam.name(),
                            requestedLeague.nome()
                        ),
                    false
                );
                if (edition != null && edition.id() == requestedEdition.id()) {
                    refresh();
                }
            },
            failure -> status.accept(
                UserViewSupport.errorMessage(failure),
                true
            )
        );
    }

    private void loadStandings() {
        final Edizione requestedEdition = edition;
        final LegaDisponibile requestedLeague = standingsLeague.getValue();
        if (requestedEdition == null || requestedLeague == null) {
            return;
        }
        status.accept("Caricamento della classifica…", false);
        tasks.run(
            root,
            () -> services.leagues().standings(
                requestedLeague.id(),
                requestedEdition.id()
            ),
            rows -> {
                if (edition != null
                    && edition.id() == requestedEdition.id()) {
                    standings.setItems(
                        FXCollections.observableArrayList(rows)
                    );
                    status.accept(
                        rows.isEmpty()
                            ? "La lega non contiene ancora team."
                            : "Classifica di '%s' aggiornata."
                                .formatted(requestedLeague.nome()),
                        false
                    );
                }
            },
            failure -> {
                standings.getItems().clear();
                status.accept(
                    UserViewSupport.errorMessage(failure),
                    true
                );
            }
        );
    }

    private void applyData(final LeagueData data) {
        availableLeagues.setItems(
            FXCollections.observableArrayList(data.available())
        );
        joinLeague.setItems(
            FXCollections.observableArrayList(data.available())
        );
        standingsLeague.setItems(
            FXCollections.observableArrayList(data.available())
        );
        joinTeam.setItems(
            FXCollections.observableArrayList(data.teams())
        );
        joinedLeagues.setItems(
            FXCollections.observableArrayList(data.joined())
        );
        standings.getItems().clear();

        if (!data.available().isEmpty()) {
            joinLeague.getSelectionModel().selectFirst();
            standingsLeague.getSelectionModel().selectFirst();
        }
        if (!data.teams().isEmpty()) {
            joinTeam.getSelectionModel().selectFirst();
        }
        updateCreateAvailability();
        updateJoinAvailability();
        updateStandingsAvailability();
    }

    private void clearData() {
        availableLeagues.getItems().clear();
        joinLeague.getItems().clear();
        standingsLeague.getItems().clear();
        joinTeam.getItems().clear();
        joinedLeagues.getItems().clear();
        standings.getItems().clear();
        updateCreateAvailability();
        updateJoinAvailability();
        updateStandingsAvailability();
    }

    private void updateCreateAvailability() {
        createLeague.setDisable(
            edition == null
                || leagueName.getText() == null
                || leagueName.getText().isBlank()
        );
    }

    private void updateJoinAvailability() {
        join.setDisable(
            edition == null
                || joinLeague.getValue() == null
                || joinTeam.getValue() == null
        );
    }

    private void updateStandingsAvailability() {
        loadStandings.setDisable(
            edition == null || standingsLeague.getValue() == null
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

    private static String leagueText(final LegaDisponibile league) {
        return "%s — amministratore: %s".formatted(
            league.nome(),
            league.usernameAmministratore()
        );
    }

    private static BorderPane padded(final Node content) {
        final BorderPane pane = new BorderPane(content);
        pane.setPadding(new Insets(12));
        return pane;
    }

    private record LeagueData(
        List<LegaDisponibile> available,
        List<JoinedLeague> joined,
        List<TeamSummary> teams
    ) {

        LeagueData {
            available = List.copyOf(available);
            joined = List.copyOf(joined);
            teams = List.copyOf(teams);
        }
    }
}
