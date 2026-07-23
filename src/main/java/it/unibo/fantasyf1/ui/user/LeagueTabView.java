package it.unibo.fantasyf1.ui.user;

import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.model.JoinedLeague;
import it.unibo.fantasyf1.model.LegaDisponibile;
import it.unibo.fantasyf1.model.StandingRow;
import it.unibo.fantasyf1.model.TeamSummary;
import it.unibo.fantasyf1.service.ApplicationServices;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
 * Operazioni utente U4, U5, U6, U7 e U9.
 */
final class LeagueTabView {

    private final ApplicationServices services;
    private final FxTaskRunner tasks;
    private final BiConsumer<String, Boolean> status;

    private final VBox root = new VBox(10);
    private final TabPane tabs = new TabPane();
    private final Tab availableTab = new Tab("Leghe disponibili");
    private final ListView<LegaDisponibile> availableLeagues =
        new ListView<>();
    private final Label selectedLeagueTitle =
        new Label("Seleziona una lega");
    private final Label selectedLeagueDetail = new Label(
        "Clicca una lega per vedere subito team iscritti e classifica."
    );
    private final ComboBox<TeamSummary> joinTeam = new ComboBox<>();
    private final Button join = new Button("Iscrivi team");
    private final TextField leagueName = new TextField();
    private final Button createLeague = new Button("Crea lega");
    private final ListView<JoinedLeague> joinedLeagues = new ListView<>();
    private final ListView<LegaDisponibile> ownedLeagues = new ListView<>();
    private final ListView<StandingRow> standings = new ListView<>();

    private Edizione edition;
    private long generation;
    private Integer leagueToOpenAfterRefresh;

    LeagueTabView(
        final ApplicationServices services,
        final FxTaskRunner tasks,
        final BiConsumer<String, Boolean> status
    ) {
        this.services = services;
        this.tasks = tasks;
        this.status = status;
        configureControls();

        availableTab.setContent(createAvailableLeagues());
        final Tab creation = new Tab("Crea lega", createLeagueForm());
        final Tab joined =
            new Tab("Le mie partecipazioni", createJoined());
        final Tab owned = new Tab("Le mie leghe", createOwned());
        for (Tab tab : List.of(availableTab, creation, joined, owned)) {
            tab.setClosable(false);
        }
        tabs.getTabs().addAll(availableTab, creation, joined, owned);
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
        ownedLeagues.setPlaceholder(
            new Label("Non hai ancora creato leghe nell'edizione")
        );
        standings.setPlaceholder(
            new Label("La lega non contiene ancora team")
        );

        UserViewSupport.renderList(
            availableLeagues,
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
        UserViewSupport.renderList(
            ownedLeagues,
            LeagueTabView::leagueText
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

        availableLeagues.getSelectionModel()
            .selectedItemProperty()
            .addListener(
                (observable, previous, selected) -> showLeague(selected)
            );
        joinedLeagues.getSelectionModel()
            .selectedItemProperty()
            .addListener((observable, previous, selected) -> {
                if (selected != null) {
                    openLeague(selected.leagueId());
                    joinedLeagues.getSelectionModel().clearSelection();
                }
            });
        ownedLeagues.getSelectionModel()
            .selectedItemProperty()
            .addListener((observable, previous, selected) -> {
                if (selected != null) {
                    openLeague(selected.id());
                    ownedLeagues.getSelectionModel().clearSelection();
                }
            });
        leagueName.textProperty().addListener(
            (observable, previous, current) -> updateCreateAvailability()
        );
        joinTeam.valueProperty().addListener(
            (observable, previous, current) -> updateJoinAvailability()
        );

        createLeague.setOnAction(event -> createLeague());
        join.setOnAction(event -> joinSelectedLeague());
        selectedLeagueTitle.setStyle(
            "-fx-font-size: 18px; -fx-font-weight: bold;"
        );
        selectedLeagueDetail.setWrapText(true);
        updateCreateAvailability();
        updateJoinAvailability();
    }

    private Node createAvailableLeagues() {
        final Button refresh = new Button("Aggiorna leghe");
        refresh.setOnAction(event -> refresh());
        final VBox leagueList = new VBox(
            8,
            new Label("Leghe dell'edizione"),
            availableLeagues,
            refresh
        );
        VBox.setVgrow(availableLeagues, Priority.ALWAYS);

        joinTeam.setPromptText("Scegli uno dei tuoi team");
        joinTeam.setPrefWidth(300);
        final HBox enrollment = new HBox(
            10,
            new Label("Team"),
            joinTeam,
            join
        );
        final VBox details = new VBox(
            10,
            selectedLeagueTitle,
            selectedLeagueDetail,
            new Label("Classifica e team iscritti"),
            standings,
            enrollment
        );
        VBox.setVgrow(standings, Priority.ALWAYS);

        final SplitPane split = new SplitPane(leagueList, details);
        split.setOrientation(Orientation.HORIZONTAL);
        split.setDividerPositions(0.37);
        return padded(split);
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
                    + "La ritroverai sempre nella sezione “Le mie leghe”."
            ),
            form
        ));
    }

    private Node createJoined() {
        final Button refresh = new Button("Aggiorna partecipazioni");
        refresh.setOnAction(event -> refresh());
        final Label instructions = new Label(
            "Clicca una partecipazione per entrare nella lega e aprirne "
                + "automaticamente la classifica."
        );
        instructions.setWrapText(true);
        final VBox content = new VBox(
            10,
            instructions,
            joinedLeagues,
            refresh
        );
        VBox.setVgrow(joinedLeagues, Priority.ALWAYS);
        return padded(content);
    }

    private Node createOwned() {
        final Button refresh = new Button("Aggiorna le mie leghe");
        refresh.setOnAction(event -> refresh());
        final Label instructions = new Label(
            "Le leghe che amministri. Clicca una riga per aprirne "
                + "classifica e partecipanti."
        );
        instructions.setWrapText(true);
        final VBox content = new VBox(
            10,
            instructions,
            ownedLeagues,
            refresh
        );
        VBox.setVgrow(ownedLeagues, Priority.ALWAYS);
        return padded(content);
    }

    void refresh() {
        final Edizione requestedEdition = edition;
        if (requestedEdition == null) {
            return;
        }
        final LegaDisponibile selected =
            availableLeagues.getSelectionModel().getSelectedItem();
        final Integer selectedLeagueId = leagueToOpenAfterRefresh != null
            ? leagueToOpenAfterRefresh
            : selected == null ? null : selected.id();
        leagueToOpenAfterRefresh = null;
        final long requestedGeneration = ++generation;
        status.accept("Caricamento delle leghe…", false);
        tasks.run(
            root,
            () -> new LeagueData(
                services.leagues().availableLeagues(requestedEdition.id()),
                services.leagues().joinedLeagues(requestedEdition.id()),
                services.leagues().myLeagues(requestedEdition.id()),
                services.teams().myTeams(requestedEdition.id())
            ),
            data -> {
                if (!isCurrent(requestedEdition, requestedGeneration)) {
                    return;
                }
                applyData(data, selectedLeagueId);
                status.accept(
                    ("%d leghe disponibili, %d partecipazioni, "
                        + "%d leghe amministrate.")
                        .formatted(
                            data.available().size(),
                            data.joined().size(),
                            data.owned().size()
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
            createdId -> {
                status.accept(
                    "Lega '%s' creata correttamente."
                        .formatted(requestedName.trim()),
                    false
                );
                if (edition != null && edition.id() == requestedEdition.id()) {
                    leagueName.clear();
                    refreshAndOpen(createdId);
                }
            },
            failure -> status.accept(
                UserViewSupport.errorMessage(failure),
                true
            )
        );
    }

    private void joinSelectedLeague() {
        final Edizione requestedEdition = edition;
        final LegaDisponibile requestedLeague =
            availableLeagues.getSelectionModel().getSelectedItem();
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
                    refreshAndOpen(requestedLeague.id());
                }
            },
            failure -> status.accept(
                UserViewSupport.errorMessage(failure),
                true
            )
        );
    }

    private void refreshAndOpen(final int leagueId) {
        leagueToOpenAfterRefresh = leagueId;
        refresh();
    }

    private void showLeague(final LegaDisponibile league) {
        standings.getItems().clear();
        if (league == null) {
            selectedLeagueTitle.setText("Seleziona una lega");
            selectedLeagueDetail.setText(
                "Clicca una lega per vedere subito team iscritti "
                    + "e classifica."
            );
            updateJoinAvailability();
            return;
        }
        selectedLeagueTitle.setText(league.nome());
        final JoinedLeague participation = joinedLeagues.getItems().stream()
            .filter(joined -> joined.leagueId() == league.id())
            .findFirst()
            .orElse(null);
        selectedLeagueDetail.setText(
            participation == null
                ? "Amministratore: %s. Puoi iscrivere uno dei tuoi team."
                    .formatted(league.usernameAmministratore())
                : "Amministratore: %s. Partecipi con il team “%s”."
                    .formatted(
                        league.usernameAmministratore(),
                        participation.teamName()
                    )
        );
        updateJoinAvailability();
        loadStandings(league);
    }

    private void loadStandings(final LegaDisponibile requestedLeague) {
        final Edizione requestedEdition = edition;
        final long requestedGeneration = generation;
        if (requestedEdition == null) {
            return;
        }
        standings.setPlaceholder(new Label("Caricamento classifica…"));
        tasks.run(
            standings,
            () -> services.leagues().standings(
                requestedLeague.id(),
                requestedEdition.id()
            ),
            rows -> {
                if (
                    isCurrent(requestedEdition, requestedGeneration)
                        && isSelected(requestedLeague.id())
                ) {
                    standings.setItems(
                        FXCollections.observableArrayList(rows)
                    );
                    standings.setPlaceholder(
                        new Label("La lega non contiene ancora team")
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
                if (isSelected(requestedLeague.id())) {
                    standings.getItems().clear();
                    standings.setPlaceholder(
                        new Label("Classifica non disponibile")
                    );
                    status.accept(
                        UserViewSupport.errorMessage(failure),
                        true
                    );
                }
            }
        );
    }

    private void openLeague(final int leagueId) {
        final LegaDisponibile league = findAvailableLeague(leagueId);
        if (league == null) {
            return;
        }
        tabs.getSelectionModel().select(availableTab);
        if (isSelected(leagueId)) {
            showLeague(league);
        } else {
            availableLeagues.getSelectionModel().select(league);
        }
        availableLeagues.scrollTo(league);
    }

    private LegaDisponibile findAvailableLeague(final int leagueId) {
        return availableLeagues.getItems().stream()
            .filter(league -> league.id() == leagueId)
            .findFirst()
            .orElse(null);
    }

    private boolean isSelected(final int leagueId) {
        final LegaDisponibile selected =
            availableLeagues.getSelectionModel().getSelectedItem();
        return selected != null && selected.id() == leagueId;
    }

    private void applyData(
        final LeagueData data,
        final Integer preferredLeagueId
    ) {
        availableLeagues.setItems(
            FXCollections.observableArrayList(data.available())
        );
        joinTeam.setItems(
            FXCollections.observableArrayList(data.teams())
        );
        joinedLeagues.setItems(
            FXCollections.observableArrayList(data.joined())
        );
        ownedLeagues.setItems(
            FXCollections.observableArrayList(data.owned())
        );
        standings.getItems().clear();

        final LegaDisponibile preferred = preferredLeagueId == null
            ? null
            : findAvailableLeague(preferredLeagueId);
        if (preferred != null) {
            availableLeagues.getSelectionModel().select(preferred);
        } else if (!data.available().isEmpty()) {
            availableLeagues.getSelectionModel().selectFirst();
        }
        if (!data.teams().isEmpty()) {
            joinTeam.getSelectionModel().selectFirst();
        }
        updateCreateAvailability();
        updateJoinAvailability();
    }

    private void clearData() {
        availableLeagues.getItems().clear();
        joinTeam.getItems().clear();
        joinedLeagues.getItems().clear();
        ownedLeagues.getItems().clear();
        standings.getItems().clear();
        selectedLeagueTitle.setText("Seleziona una lega");
        selectedLeagueDetail.setText(
            "Clicca una lega per vedere subito team iscritti e classifica."
        );
        updateCreateAvailability();
        updateJoinAvailability();
    }

    private void updateCreateAvailability() {
        createLeague.setDisable(
            edition == null
                || leagueName.getText() == null
                || leagueName.getText().isBlank()
        );
    }

    private void updateJoinAvailability() {
        final LegaDisponibile selected =
            availableLeagues.getSelectionModel().getSelectedItem();
        final boolean alreadyJoined = selected != null
            && joinedLeagues.getItems().stream()
                .anyMatch(joined -> joined.leagueId() == selected.id());
        joinTeam.setDisable(edition == null || selected == null
            || alreadyJoined);
        join.setDisable(
            edition == null
                || selected == null
                || joinTeam.getValue() == null
                || alreadyJoined
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
        List<LegaDisponibile> owned,
        List<TeamSummary> teams
    ) {

        LeagueData {
            available = List.copyOf(available);
            joined = List.copyOf(joined);
            owned = List.copyOf(owned);
            teams = List.copyOf(teams);
        }
    }
}
