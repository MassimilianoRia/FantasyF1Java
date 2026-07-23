package it.unibo.fantasyf1.ui.user;

import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.service.ApplicationServices;
import it.unibo.fantasyf1.service.RegistrationRequest;
import it.unibo.fantasyf1.session.UserSession;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

/**
 * Navigazione dell'area utente: autenticazione, sessione e dashboard.
 */
public final class UserApplicationView implements AutoCloseable {

    private static final String ERROR_STYLE = "-fx-text-fill: #b00020;";
    private static final String SUCCESS_STYLE = "-fx-text-fill: #176b2c;";

    private final Stage stage;
    private final ApplicationServices services;
    private final FxTaskRunner tasks;
    private final Runnable showModeSelection;

    private Label dashboardStatus;
    private TeamTabView teamView;
    private LeagueTabView leagueView;
    private boolean active;

    public UserApplicationView(
        final Stage stage,
        final ApplicationServices services,
        final FxTaskRunner tasks,
        final Runnable showModeSelection
    ) {
        this.stage = Objects.requireNonNull(stage);
        this.services = Objects.requireNonNull(services);
        this.tasks = Objects.requireNonNull(tasks);
        this.showModeSelection = Objects.requireNonNull(showModeSelection);
    }

    public void show() {
        active = true;
        stage.setTitle("Fantasy Formula 1");
        showAuthentication();
    }

    private void showAuthentication() {
        final Label title = new Label("Fantasy Formula 1");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");
        final Label subtitle = new Label(
            "Accedi oppure crea un account per gestire team e leghe."
        );
        final Label status = new Label("Inserisci le tue credenziali.");
        status.setWrapText(true);

        final TextField loginUsername = new TextField();
        loginUsername.setPromptText("Username");
        final PasswordField loginPassword = new PasswordField();
        loginPassword.setPromptText("Password");
        final Button loginButton = new Button("Accedi");
        loginButton.setDefaultButton(true);

        final GridPane loginForm = formGrid();
        loginForm.addRow(0, new Label("Username"), loginUsername);
        loginForm.addRow(1, new Label("Password"), loginPassword);
        loginForm.add(loginButton, 1, 2);

        final TextField firstName = new TextField();
        final TextField lastName = new TextField();
        final TextField registrationUsername = new TextField();
        final PasswordField registrationPassword = new PasswordField();
        final TextField email = new TextField();
        final TextField phone = new TextField();
        firstName.setPromptText("Nome");
        lastName.setPromptText("Cognome");
        registrationUsername.setPromptText("Username");
        registrationPassword.setPromptText("Almeno 8 caratteri");
        email.setPromptText("nome@example.com");
        phone.setPromptText("+39 333 1234567");
        final Button registerButton = new Button("Crea account");

        final GridPane registrationForm = formGrid();
        registrationForm.addRow(0, new Label("Nome"), firstName);
        registrationForm.addRow(1, new Label("Cognome"), lastName);
        registrationForm.addRow(
            2,
            new Label("Username"),
            registrationUsername
        );
        registrationForm.addRow(
            3,
            new Label("Password"),
            registrationPassword
        );
        registrationForm.addRow(4, new Label("Email"), email);
        registrationForm.addRow(5, new Label("Telefono"), phone);
        registrationForm.add(registerButton, 1, 6);

        final Tab loginTab = new Tab("Accedi", loginForm);
        final Tab registrationTab =
            new Tab("Registrati", registrationForm);
        loginTab.setClosable(false);
        registrationTab.setClosable(false);
        final TabPane authTabs = new TabPane(loginTab, registrationTab);
        authTabs.setPrefWidth(560);

        loginButton.setOnAction(event -> {
            setStatus(status, "Accesso in corso…", false);
            final String username = loginUsername.getText();
            final String password = loginPassword.getText();
            tasks.run(
                authTabs,
                () -> services.authentication().login(username, password),
                session -> {
                    loginPassword.clear();
                    if (active) {
                        showDashboard(session);
                    } else {
                        services.authentication().logout();
                    }
                },
                failure -> {
                    loginPassword.clear();
                    setStatus(
                        status,
                        UserViewSupport.errorMessage(failure),
                        true
                    );
                }
            );
        });

        registerButton.setOnAction(event -> {
            setStatus(status, "Registrazione in corso…", false);
            final RegistrationRequest request = new RegistrationRequest(
                firstName.getText(),
                lastName.getText(),
                registrationUsername.getText(),
                registrationPassword.getText(),
                email.getText(),
                phone.getText()
            );
            tasks.run(
                authTabs,
                () -> services.authentication().register(request),
                ignoredId -> {
                    loginUsername.setText(request.username().trim());
                    registrationPassword.clear();
                    firstName.clear();
                    lastName.clear();
                    registrationUsername.clear();
                    email.clear();
                    phone.clear();
                    authTabs.getSelectionModel().select(loginTab);
                    setStatus(
                        status,
                        "Account creato. Ora puoi accedere.",
                        false
                    );
                },
                failure -> {
                    registrationPassword.clear();
                    setStatus(
                        status,
                        UserViewSupport.errorMessage(failure),
                        true
                    );
                }
            );
        });

        final VBox card = new VBox(12, title, subtitle, authTabs, status);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setMaxWidth(620);
        card.setPadding(new Insets(28));
        card.setStyle(
            "-fx-background-color: white;"
                + "-fx-border-color: #d7d7d7;"
                + "-fx-border-radius: 6;"
                + "-fx-background-radius: 6;"
        );

        final BorderPane root = new BorderPane(card);
        root.setPadding(new Insets(32));
        BorderPane.setAlignment(card, Pos.CENTER);
        final Button changeMode = createChangeModeButton();
        changeMode.disableProperty().bind(authTabs.disableProperty());
        root.setTop(changeMode);
        BorderPane.setAlignment(changeMode, Pos.CENTER_LEFT);
        stage.getScene().setRoot(root);
        loginUsername.requestFocus();
    }

    private void showDashboard(final UserSession session) {
        final BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));

        final Label title = new Label("Fantasy Formula 1");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        final Label user = new Label("Utente: " + session.username());
        final ComboBox<Edizione> editionCombo = new ComboBox<>();
        editionCombo.setPromptText("Seleziona edizione");
        editionCombo.setPrefWidth(220);
        final Button refreshEditions = new Button("Aggiorna");
        final Button logout = new Button("Esci");
        final Button changeMode = createChangeModeButton();

        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox header = new HBox(
            10,
            title,
            spacer,
            user,
            new Label("Edizione"),
            editionCombo,
            refreshEditions,
            logout,
            changeMode
        );
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));

        dashboardStatus = new Label("Caricamento delle edizioni…");
        dashboardStatus.setWrapText(true);
        dashboardStatus.setPadding(new Insets(10, 0, 0, 0));

        teamView = new TeamTabView(
            services,
            tasks,
            this::dashboardStatus,
            () -> {
                if (leagueView != null) {
                    leagueView.refresh();
                }
            }
        );
        leagueView =
            new LeagueTabView(services, tasks, this::dashboardStatus);
        final Tab teamTab = new Tab("Team", teamView.content());
        final Tab leagueTab = new Tab("Leghe", leagueView.content());
        teamTab.setClosable(false);
        leagueTab.setClosable(false);
        final TabPane sections = new TabPane(teamTab, leagueTab);

        root.setTop(header);
        root.setCenter(sections);
        root.setBottom(dashboardStatus);

        final boolean[] updatingEditions = {false};
        editionCombo.valueProperty().addListener(
            (observable, previous, selected) -> {
                if (!updatingEditions[0]) {
                    selectEdition(selected, sections);
                }
            }
        );

        final Runnable loadEditions = () -> {
            final Edizione previous = editionCombo.getValue();
            dashboardStatus("Caricamento delle edizioni…", false);
            tasks.run(
                root,
                () -> services.editions().findAll(),
                editions -> {
                    updatingEditions[0] = true;
                    try {
                        editionCombo.setItems(
                            FXCollections.observableArrayList(editions)
                        );
                        selectPreviousOrFirst(
                            editionCombo,
                            editions,
                            previous
                        );
                    } finally {
                        updatingEditions[0] = false;
                    }
                    selectEdition(editionCombo.getValue(), sections);
                },
                failure -> {
                    editionCombo.getItems().clear();
                    selectEdition(null, sections);
                    dashboardStatus(
                        UserViewSupport.errorMessage(failure),
                        true
                    );
                }
            );
        };
        refreshEditions.setOnAction(event -> loadEditions.run());

        logout.setOnAction(event -> {
            dashboardStatus("Chiusura della sessione…", false);
            tasks.run(
                root,
                () -> {
                    services.authentication().logout();
                    return null;
                },
                ignored -> showAuthentication(),
                failure -> dashboardStatus(
                    UserViewSupport.errorMessage(failure),
                    true
                )
            );
        });

        stage.getScene().setRoot(root);
        loadEditions.run();
    }

    private Button createChangeModeButton() {
        final Button button = new Button("← Selezione modalità");
        button.setOnAction(event -> {
            services.authentication().logout();
            close();
            showModeSelection.run();
        });
        return button;
    }

    private void selectEdition(
        final Edizione edition,
        final TabPane sections
    ) {
        final boolean available = edition != null;
        sections.setDisable(!available);
        teamView.setEdition(edition);
        leagueView.setEdition(edition);
        if (available) {
            dashboardStatus(
                "Edizione %d selezionata.".formatted(edition.anno()),
                false
            );
        } else {
            dashboardStatus(
                "Non sono presenti edizioni. "
                    + "L'amministratore deve crearne una.",
                true
            );
        }
    }

    private static void selectPreviousOrFirst(
        final ComboBox<Edizione> combo,
        final List<Edizione> editions,
        final Edizione previous
    ) {
        if (editions.isEmpty()) {
            combo.setValue(null);
            return;
        }
        editions.stream()
            .filter(edition ->
                previous != null && edition.id() == previous.id()
            )
            .findFirst()
            .ifPresentOrElse(
                combo::setValue,
                () -> combo.getSelectionModel().selectFirst()
            );
    }

    private void dashboardStatus(
        final String message,
        final boolean error
    ) {
        if (dashboardStatus != null) {
            setStatus(dashboardStatus, message, error);
        }
    }

    private static GridPane formGrid() {
        final GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(18));
        return grid;
    }

    private static void setStatus(
        final Label label,
        final String message,
        final boolean error
    ) {
        label.setText(message);
        label.setStyle(error ? ERROR_STYLE : SUCCESS_STYLE);
    }

    @Override
    public void close() {
        active = false;
        teamView = null;
        leagueView = null;
    }
}
