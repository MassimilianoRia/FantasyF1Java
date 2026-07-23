package it.unibo.fantasyf1.ui.admin;

import it.unibo.fantasyf1.error.AppException;
import it.unibo.fantasyf1.error.ErrorCode;
import it.unibo.fantasyf1.model.ConstructorOption;
import it.unibo.fantasyf1.model.DriverOption;
import it.unibo.fantasyf1.model.DriverRegistryOption;
import it.unibo.fantasyf1.model.EditionStatus;
import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.model.EnrolledConstructorOption;
import it.unibo.fantasyf1.model.GrandPrixOption;
import it.unibo.fantasyf1.model.RaceWeekend;
import it.unibo.fantasyf1.service.AdminService;
import it.unibo.fantasyf1.service.PerformanceRequest;
import it.unibo.fantasyf1.service.ProcessingOutcome;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Dashboard JavaFX per A1-A8. Dipende esclusivamente dal service trusted.
 */
public final class AdminDashboard {

    private static final double FIELD_WIDTH = 360;
    private static final double COMPLETION_TOTAL = 64.0;

    private final AdminService admin;
    private final Runnable showModeSelection;
    private final BorderPane root = new BorderPane();
    private final BooleanProperty busy = new SimpleBooleanProperty();

    private final ComboBox<Edizione> editionCombo = new ComboBox<>();
    private final Button refreshButton = new Button("Aggiorna cataloghi");
    private final Label completionTitle = new Label("Nessuna edizione selezionata");
    private final Label completionDetail = new Label(
        "Crea o seleziona un'edizione per visualizzarne lo stato."
    );
    private final ProgressBar completionProgress = new ProgressBar(0);
    private final TabPane tabs = new TabPane();
    private final List<Tab> editionScopedTabs = new ArrayList<>();
    private final ProgressIndicator progressIndicator =
        new ProgressIndicator();
    private final Label operationStatus = new Label(
        "Caricamento dei dati amministrativi non ancora avviato."
    );

    private final TextField editionNumberField = new TextField();
    private final TextField editionYearField = new TextField();

    private final ComboBox<GrandPrixOption> grandPrixEditCombo =
        new ComboBox<>();
    private final TextField grandPrixNameField = new TextField();
    private final TextField circuitField = new TextField();
    private final TextField countryField = new TextField();
    private final TextField cityField = new TextField();

    private final ComboBox<GrandPrixOption> weekendGrandPrixCombo =
        new ComboBox<>();
    private final TextField weekendRoundField = new TextField();
    private final DatePicker weekendStartDate = new DatePicker();
    private final DatePicker weekendEndDate = new DatePicker();

    private final TextField constructorNameField = new TextField();

    private final ComboBox<ConstructorOption> constructorEnrollmentCombo =
        new ComboBox<>();
    private final TextField registeredConstructorNameField = new TextField();
    private final TextField carNameField = new TextField();

    private final TextField driverFirstNameField = new TextField();
    private final TextField driverLastNameField = new TextField();
    private final TextField driverNationalityField = new TextField();
    private final DatePicker driverBirthDate = new DatePicker();

    private final ComboBox<DriverRegistryOption> driverEnrollmentCombo =
        new ComboBox<>();
    private final TextField driverCodeField = new TextField();
    private final TextField driverRaceNumberField = new TextField();
    private final ComboBox<EnrolledConstructorOption>
        enrolledConstructorCombo = new ComboBox<>();

    private final ComboBox<RaceWeekend> performanceWeekendCombo =
        new ComboBox<>();
    private final ComboBox<DriverOption> performanceDriverCombo =
        new ComboBox<>();
    private final TextField qualifyingPositionField = new TextField();
    private final TextField racePositionField = new TextField();
    private final CheckBox penalizedCheck = new CheckBox(
        "Il pilota ha ricevuto una penalizzazione"
    );
    private final CheckBox fastestLapCheck = new CheckBox(
        "Il pilota ha registrato il giro veloce"
    );
    private final Button recalculateButton =
        new Button("Ricalcola weekend (O1-O3)");
    private final Label processingOutcomeLabel = new Label(
        "Nessuna prestazione registrata in questa sessione."
    );

    private List<GrandPrixOption> grandPrixCatalog = List.of();
    private List<ConstructorOption> constructorCatalog = List.of();
    private List<DriverRegistryOption> driverCatalog = List.of();
    private boolean updatingEditionSelection;

    public AdminDashboard(
        final AdminService admin,
        final Runnable showModeSelection
    ) {
        this.admin = Objects.requireNonNull(admin);
        this.showModeSelection = Objects.requireNonNull(showModeSelection);
        configureControls();
        buildLayout();
    }

    public Parent view() {
        return root;
    }

    public void load() {
        refreshAll(null, "Cataloghi amministrativi caricati.");
    }

    private void configureControls() {
        configureCombo(editionCombo, "Seleziona un'edizione");
        configureCombo(
            grandPrixEditCombo,
            "Seleziona un Gran Premio da aggiornare (facoltativo)"
        );
        configureCombo(weekendGrandPrixCombo, "Seleziona un Gran Premio");
        configureCombo(
            constructorEnrollmentCombo,
            "Seleziona una scuderia anagrafica"
        );
        configureCombo(
            driverEnrollmentCombo,
            "Seleziona un pilota anagrafico"
        );
        configureCombo(
            enrolledConstructorCombo,
            "Seleziona una scuderia iscritta"
        );
        configureCombo(performanceWeekendCombo, "Seleziona un weekend");
        configureCombo(performanceDriverCombo, "Seleziona un pilota iscritto");

        editionNumberField.setPromptText("es. 2");
        editionYearField.setPromptText("es. 2026");
        grandPrixNameField.setPromptText("Nome ufficiale");
        circuitField.setPromptText("Circuito");
        countryField.setPromptText("Nazione");
        cityField.setPromptText("Città");
        weekendRoundField.setPromptText("1-24");
        constructorNameField.setPromptText("Nome anagrafico");
        registeredConstructorNameField.setPromptText("Nome stagionale");
        carNameField.setPromptText("Vettura");
        driverFirstNameField.setPromptText("Nome");
        driverLastNameField.setPromptText("Cognome");
        driverNationalityField.setPromptText("Nazionalità");
        driverCodeField.setPromptText("Tre lettere");
        driverRaceNumberField.setPromptText("Numero in gara");
        qualifyingPositionField.setPromptText("1-20 oppure vuoto");
        racePositionField.setPromptText("1-20 oppure vuoto");

        for (TextField field : List.of(
            editionNumberField,
            editionYearField,
            grandPrixNameField,
            circuitField,
            countryField,
            cityField,
            weekendRoundField,
            constructorNameField,
            registeredConstructorNameField,
            carNameField,
            driverFirstNameField,
            driverLastNameField,
            driverNationalityField,
            driverCodeField,
            driverRaceNumberField,
            qualifyingPositionField,
            racePositionField
        )) {
            field.setMaxWidth(FIELD_WIDTH);
        }
        weekendStartDate.setMaxWidth(FIELD_WIDTH);
        weekendEndDate.setMaxWidth(FIELD_WIDTH);
        driverBirthDate.setMaxWidth(FIELD_WIDTH);

        editionCombo.valueProperty().addListener(
            (observable, previous, selected) -> {
                if (!updatingEditionSelection) {
                    setEditionScopedTabsEnabled(selected != null);
                    if (selected == null) {
                        clearEditionScopedCatalogs();
                    } else {
                        refreshEditionScope(
                            selected,
                            "Edizione selezionata: " + selected
                        );
                    }
                }
            }
        );
        grandPrixEditCombo.valueProperty().addListener(
            (observable, previous, selected) -> populateGrandPrixForm(selected)
        );
        refreshButton.setOnAction(
            event -> refreshAll(selectedEditionId(), "Cataloghi aggiornati.")
        );

        progressIndicator.setMaxSize(22, 22);
        progressIndicator.visibleProperty().bind(busy);
        progressIndicator.managedProperty().bind(busy);
        tabs.disableProperty().bind(busy);
        editionCombo.disableProperty().bind(busy);
        refreshButton.disableProperty().bind(busy);
        recalculateButton.disableProperty().bind(
            busy.or(performanceWeekendCombo.valueProperty().isNull())
        );
    }

    private void buildLayout() {
        root.setPadding(new Insets(20));
        root.setTop(createHeader());
        root.setCenter(createTabs());
        root.setBottom(createStatusBar());
        BorderPane.setMargin(tabs, new Insets(16, 0, 12, 0));
    }

    private Node createHeader() {
        final Label title = new Label("Amministrazione Fantasy Formula 1");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");
        final Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        final Button changeMode = new Button("← Selezione modalità");
        changeMode.disableProperty().bind(busy);
        changeMode.setOnAction(event -> showModeSelection.run());
        final HBox titleRow = new HBox(
            10,
            title,
            titleSpacer,
            changeMode
        );
        titleRow.setAlignment(Pos.CENTER_LEFT);

        final Label trustedNotice = new Label(
            "Modalità amministratore trusted: nessun account o ruolo "
                + "amministratore è memorizzato nel database."
        );
        trustedNotice.setWrapText(true);
        trustedNotice.setStyle(
            "-fx-text-fill: #7a3e00; -fx-background-color: #fff4df; "
                + "-fx-padding: 9px; -fx-background-radius: 4px;"
        );

        final Label editionLabel = new Label("Edizione operativa:");
        editionLabel.setStyle("-fx-font-weight: bold;");
        final HBox editionRow = new HBox(
            10,
            editionLabel,
            editionCombo,
            refreshButton
        );
        editionRow.setAlignment(Pos.CENTER_LEFT);

        completionTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        completionDetail.setWrapText(true);
        completionProgress.setPrefWidth(420);
        completionProgress.setMaxWidth(Double.MAX_VALUE);

        final VBox completionBox = new VBox(
            5,
            completionTitle,
            completionDetail,
            completionProgress
        );
        completionBox.setPadding(new Insets(10));
        completionBox.setStyle(
            "-fx-border-color: #d4d4d4; -fx-border-radius: 4px;"
        );

        return new VBox(
            10,
            titleRow,
            trustedNotice,
            editionRow,
            completionBox
        );
    }

    private Node createTabs() {
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
            createTab("A1 · Edizione", createEditionForm(), false),
            createTab("A2 · Gran Premio", createGrandPrixForm(), false),
            createTab("A3 · Weekend", createWeekendForm(), true),
            createTab("A4 · Scuderia", createConstructorForm(), false),
            createTab(
                "A5 · Iscrivi scuderia",
                createConstructorEnrollmentForm(),
                true
            ),
            createTab("A6 · Pilota", createDriverForm(), false),
            createTab(
                "A7 · Iscrivi pilota",
                createDriverEnrollmentForm(),
                true
            ),
            createTab("A8 · Prestazione", createPerformanceForm(), true)
        );
        setEditionScopedTabsEnabled(false);
        return tabs;
    }

    private Tab createTab(
        final String title,
        final Node content,
        final boolean editionScoped
    ) {
        final Tab tab = new Tab(title, content);
        if (editionScoped) {
            editionScopedTabs.add(tab);
        }
        return tab;
    }

    private Node createEditionForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Numero edizione", editionNumberField);
        addRow(form, 1, "Anno", editionYearField);

        final Button save = new Button("Crea edizione");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> createEdition());
        return formPage(
            "A1 — Nuova edizione",
            "Registra un'edizione annuale. Numero e anno devono essere unici.",
            form,
            new HBox(10, save)
        );
    }

    private Node createGrandPrixForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Gran Premio esistente", grandPrixEditCombo);
        addRow(form, 1, "Nome", grandPrixNameField);
        addRow(form, 2, "Circuito", circuitField);
        addRow(form, 3, "Nazione", countryField);
        addRow(form, 4, "Città", cityField);

        final Button save = new Button("Salva / aggiorna Gran Premio");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> upsertGrandPrix());
        final Button clear = new Button("Nuovo");
        clear.disableProperty().bind(busy);
        clear.setOnAction(event -> clearGrandPrixForm());
        return formPage(
            "A2 — Inserimento o aggiornamento Gran Premio",
            "Seleziona un elemento esistente per modificarlo oppure lascia "
                + "vuota la selezione e inserisci un nuovo nome.",
            form,
            new HBox(10, save, clear)
        );
    }

    private Node createWeekendForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Gran Premio", weekendGrandPrixCombo);
        addRow(form, 1, "Numero round", weekendRoundField);
        addRow(form, 2, "Data inizio", weekendStartDate);
        addRow(form, 3, "Data fine", weekendEndDate);

        final Button save = new Button("Inserisci weekend");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> addWeekend());
        return formPage(
            "A3 — Weekend nell'edizione selezionata",
            "Il Gran Premio e il round devono essere liberi nell'edizione; "
                + "la data di fine non può precedere quella di inizio.",
            form,
            new HBox(10, save)
        );
    }

    private Node createConstructorForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Nome scuderia", constructorNameField);

        final Button save = new Button("Registra scuderia");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> createConstructor());
        return formPage(
            "A4 — Scuderia anagrafica",
            "Registra una scuderia riutilizzabile in più edizioni.",
            form,
            new HBox(10, save)
        );
    }

    private Node createConstructorEnrollmentForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Scuderia", constructorEnrollmentCombo);
        addRow(
            form,
            1,
            "Nome d'iscrizione",
            registeredConstructorNameField
        );
        addRow(form, 2, "Vettura", carNameField);

        final Button save = new Button("Iscrivi scuderia");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> enrollConstructor());
        return formPage(
            "A5 — Iscrizione scuderia",
            "Iscrive la scuderia all'edizione operativa, fino a un massimo "
                + "di dieci.",
            form,
            new HBox(10, save)
        );
    }

    private Node createDriverForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Nome", driverFirstNameField);
        addRow(form, 1, "Cognome", driverLastNameField);
        addRow(form, 2, "Nazionalità", driverNationalityField);
        addRow(form, 3, "Data di nascita", driverBirthDate);

        final Button save = new Button("Registra pilota");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> createDriver());
        return formPage(
            "A6 — Pilota anagrafico",
            "Registra l'anagrafica generale di un pilota.",
            form,
            new HBox(10, save)
        );
    }

    private Node createDriverEnrollmentForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Pilota", driverEnrollmentCombo);
        addRow(form, 1, "Sigla gara", driverCodeField);
        addRow(form, 2, "Numero in gara", driverRaceNumberField);
        addRow(form, 3, "Scuderia iscritta", enrolledConstructorCombo);

        final Button save = new Button("Iscrivi pilota");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> enrollDriver());
        return formPage(
            "A7 — Iscrizione pilota e assegnazione scuderia",
            "La sigla contiene tre lettere. La scuderia deve appartenere "
                + "all'edizione e può avere al massimo due piloti.",
            form,
            new HBox(10, save)
        );
    }

    private Node createPerformanceForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Weekend", performanceWeekendCombo);
        addRow(form, 1, "Pilota iscritto", performanceDriverCombo);
        addRow(form, 2, "Posizione qualifica", qualifyingPositionField);
        addRow(form, 3, "Posizione gara", racePositionField);
        addRow(form, 4, "Penalizzazione", penalizedCheck);
        addRow(form, 5, "Giro veloce", fastestLapCheck);

        final Button save = new Button("Registra / correggi prestazione");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> recordPerformance());
        recalculateButton.setOnAction(event -> recalculateWeekend());

        processingOutcomeLabel.setWrapText(true);
        processingOutcomeLabel.setStyle(
            "-fx-background-color: #f3f6f8; -fx-padding: 10px;"
        );
        final VBox actions = new VBox(
            10,
            new HBox(10, save, recalculateButton),
            processingOutcomeLabel
        );
        return formPage(
            "A8 — Prestazione ufficiale e ricalcolo",
            "L'upsert corregge una prestazione esistente ed esegue "
                + "atomicamente O1-O3. Il ricalcolo manuale è idempotente.",
            form,
            actions
        );
    }

    private Node createStatusBar() {
        operationStatus.setWrapText(true);
        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox statusBar = new HBox(
            10,
            progressIndicator,
            operationStatus,
            spacer
        );
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(8, 0, 0, 0));
        return statusBar;
    }

    private void createEdition() {
        try {
            final int number = parseInt(
                editionNumberField,
                "Il numero dell'edizione"
            );
            final int year = parseInt(editionYearField, "L'anno");
            executeMutation(
                "creazione-edizione",
                "Creazione dell'edizione in corso…",
                () -> admin.createEdition(number, year),
                editionId -> {
                    editionNumberField.clear();
                    editionYearField.clear();
                    confirm("Edizione creata correttamente.");
                    refreshAll(
                        editionId,
                        "Nuova edizione selezionata e cataloghi aggiornati."
                    );
                }
            );
        } catch (RuntimeException exception) {
            showInputError(exception);
        }
    }

    private void upsertGrandPrix() {
        final String name = grandPrixNameField.getText();
        final String circuit = circuitField.getText();
        final String country = countryField.getText();
        final String city = cityField.getText();
        executeMutation(
            "salvataggio-gran-premio",
            "Salvataggio del Gran Premio in corso…",
            () -> admin.upsertGrandPrix(name, circuit, country, city),
            ignored -> {
                clearGrandPrixForm();
                confirm("Gran Premio salvato correttamente.");
                refreshAll(
                    selectedEditionId(),
                    "Catalogo dei Gran Premi aggiornato."
                );
            }
        );
    }

    private void addWeekend() {
        try {
            final Edizione edition = requireSelection(
                editionCombo,
                "Seleziona un'edizione."
            );
            final GrandPrixOption grandPrix = requireSelection(
                weekendGrandPrixCombo,
                "Seleziona un Gran Premio."
            );
            final int round = parseInt(
                weekendRoundField,
                "Il numero del round"
            );
            final LocalDate start = requireDate(
                weekendStartDate,
                "Seleziona la data di inizio."
            );
            final LocalDate end = requireDate(
                weekendEndDate,
                "Seleziona la data di fine."
            );
            executeMutation(
                "inserimento-weekend",
                "Inserimento del weekend in corso…",
                () -> {
                    admin.addWeekend(
                        edition.id(),
                        grandPrix.id(),
                        round,
                        start,
                        end
                    );
                    return Boolean.TRUE;
                },
                ignored -> {
                    weekendRoundField.clear();
                    weekendStartDate.setValue(null);
                    weekendEndDate.setValue(null);
                    confirm("Weekend inserito correttamente.");
                    refreshAll(
                        edition.id(),
                        "Calendario e completezza aggiornati."
                    );
                }
            );
        } catch (RuntimeException exception) {
            showInputError(exception);
        }
    }

    private void createConstructor() {
        final String name = constructorNameField.getText();
        executeMutation(
            "creazione-scuderia",
            "Registrazione della scuderia in corso…",
            () -> admin.createConstructor(name),
            ignored -> {
                constructorNameField.clear();
                confirm("Scuderia registrata correttamente.");
                refreshAll(
                    selectedEditionId(),
                    "Catalogo delle scuderie aggiornato."
                );
            }
        );
    }

    private void enrollConstructor() {
        try {
            final Edizione edition = requireSelection(
                editionCombo,
                "Seleziona un'edizione."
            );
            final ConstructorOption constructor = requireSelection(
                constructorEnrollmentCombo,
                "Seleziona una scuderia."
            );
            final String registeredName =
                registeredConstructorNameField.getText();
            final String carName = carNameField.getText();
            executeMutation(
                "iscrizione-scuderia",
                "Iscrizione della scuderia in corso…",
                () -> {
                    admin.enrollConstructor(
                        edition.id(),
                        constructor.id(),
                        registeredName,
                        carName
                    );
                    return Boolean.TRUE;
                },
                ignored -> {
                    registeredConstructorNameField.clear();
                    carNameField.clear();
                    confirm("Scuderia iscritta correttamente.");
                    refreshAll(
                        edition.id(),
                        "Scuderie iscritte e completezza aggiornate."
                    );
                }
            );
        } catch (RuntimeException exception) {
            showInputError(exception);
        }
    }

    private void createDriver() {
        try {
            final String firstName = driverFirstNameField.getText();
            final String lastName = driverLastNameField.getText();
            final String nationality = driverNationalityField.getText();
            final LocalDate birthDate = requireDate(
                driverBirthDate,
                "Seleziona la data di nascita."
            );
            executeMutation(
                "creazione-pilota",
                "Registrazione del pilota in corso…",
                () -> admin.createDriver(
                    firstName,
                    lastName,
                    nationality,
                    birthDate
                ),
                ignored -> {
                    driverFirstNameField.clear();
                    driverLastNameField.clear();
                    driverNationalityField.clear();
                    driverBirthDate.setValue(null);
                    confirm("Pilota registrato correttamente.");
                    refreshAll(
                        selectedEditionId(),
                        "Catalogo dei piloti aggiornato."
                    );
                }
            );
        } catch (RuntimeException exception) {
            showInputError(exception);
        }
    }

    private void enrollDriver() {
        try {
            final Edizione edition = requireSelection(
                editionCombo,
                "Seleziona un'edizione."
            );
            final DriverRegistryOption driver = requireSelection(
                driverEnrollmentCombo,
                "Seleziona un pilota."
            );
            final EnrolledConstructorOption constructor = requireSelection(
                enrolledConstructorCombo,
                "Seleziona una scuderia iscritta."
            );
            final String code = driverCodeField.getText();
            final int raceNumber = parseInt(
                driverRaceNumberField,
                "Il numero di gara"
            );
            executeMutation(
                "iscrizione-pilota",
                "Iscrizione del pilota in corso…",
                () -> {
                    admin.enrollDriver(
                        edition.id(),
                        driver.id(),
                        code,
                        raceNumber,
                        constructor.constructorId()
                    );
                    return Boolean.TRUE;
                },
                ignored -> {
                    driverCodeField.clear();
                    driverRaceNumberField.clear();
                    confirm("Pilota iscritto correttamente.");
                    refreshAll(
                        edition.id(),
                        "Piloti iscritti, risultati e completezza aggiornati."
                    );
                }
            );
        } catch (RuntimeException exception) {
            showInputError(exception);
        }
    }

    private void recordPerformance() {
        try {
            final Edizione edition = requireSelection(
                editionCombo,
                "Seleziona un'edizione."
            );
            final RaceWeekend weekend = requireSelection(
                performanceWeekendCombo,
                "Seleziona un weekend."
            );
            final DriverOption driver = requireSelection(
                performanceDriverCombo,
                "Seleziona un pilota."
            );
            final Integer qualifying = parseOptionalPosition(
                qualifyingPositionField,
                "La posizione in qualifica"
            );
            final Integer race = parseOptionalPosition(
                racePositionField,
                "La posizione in gara"
            );
            final PerformanceRequest request = new PerformanceRequest(
                edition.id(),
                weekend.grandPrixId(),
                driver.id(),
                qualifying,
                race,
                penalizedCheck.isSelected(),
                fastestLapCheck.isSelected()
            );
            executeMutation(
                "registrazione-prestazione",
                "Registrazione e ricalcolo della prestazione in corso…",
                () -> admin.recordPerformance(request),
                outcome -> {
                    applyProcessingOutcome(outcome);
                    confirm("Prestazione salvata e punteggi riallineati.");
                    refreshAll(
                        edition.id(),
                        outcome.weekendProcessable()
                            ? "Weekend elaborabile: O1-O3 completate."
                            : "Prestazione salvata; il weekend è ancora "
                                + "incompleto o non terminato."
                    );
                }
            );
        } catch (RuntimeException exception) {
            showInputError(exception);
        }
    }

    private void recalculateWeekend() {
        try {
            final Edizione edition = requireSelection(
                editionCombo,
                "Seleziona un'edizione."
            );
            final RaceWeekend weekend = requireSelection(
                performanceWeekendCombo,
                "Seleziona un weekend."
            );
            executeMutation(
                "ricalcolo-weekend",
                "Ricalcolo idempotente del weekend in corso…",
                () -> admin.processWeekend(
                    edition.id(),
                    weekend.grandPrixId()
                ),
                processable -> {
                    processingOutcomeLabel.setText(
                        processable
                            ? "Weekend elaborabile: punteggi dei piloti, "
                                + "risultati dei team e totali riallineati."
                            : "Weekend non elaborabile: i risultati parziali "
                                + "sono stati rimossi e i totali riallineati."
                    );
                    confirm("Ricalcolo concluso.");
                    refreshAll(
                        edition.id(),
                        processable
                            ? "Ricalcolo O1-O3 completato."
                            : "Ricalcolo completato: weekend non elaborabile."
                    );
                }
            );
        } catch (RuntimeException exception) {
            showInputError(exception);
        }
    }

    private void refreshAll(
        final Integer preferredEditionId,
        final String successMessage
    ) {
        execute(
            "caricamento-cataloghi",
            "Caricamento dei cataloghi amministrativi…",
            () -> new GlobalCatalogs(
                admin.editions(),
                admin.grandPrix(),
                admin.constructors(),
                admin.drivers()
            ),
            catalogs -> {
                applyGlobalCatalogs(catalogs, preferredEditionId);
                final Edizione selected = editionCombo.getValue();
                if (selected == null) {
                    clearEditionScopedCatalogs();
                    setOperationStatus(
                        "Nessuna edizione disponibile. Usa A1 per crearne una.",
                        false
                    );
                } else {
                    refreshEditionScope(selected, successMessage);
                }
            }
        );
    }

    private void refreshEditionScope(
        final Edizione edition,
        final String successMessage
    ) {
        execute(
            "caricamento-edizione",
            "Aggiornamento di stato e cataloghi per " + edition + "…",
            () -> new EditionCatalogs(
                admin.editionStatus(edition.id()),
                admin.enrolledConstructors(edition.id()),
                admin.enrolledDrivers(edition.id()),
                admin.weekends(edition.id())
            ),
            catalogs -> {
                if (
                    editionCombo.getValue() == null
                        || editionCombo.getValue().id() != edition.id()
                ) {
                    return;
                }
                applyEditionCatalogs(catalogs);
                setOperationStatus(successMessage, false);
            }
        );
    }

    private void applyGlobalCatalogs(
        final GlobalCatalogs catalogs,
        final Integer preferredEditionId
    ) {
        grandPrixCatalog = List.copyOf(catalogs.grandPrix());
        constructorCatalog = List.copyOf(catalogs.constructors());
        driverCatalog = List.copyOf(catalogs.drivers());

        final Edizione selected = selectEdition(
            catalogs.editions(),
            preferredEditionId
        );
        updatingEditionSelection = true;
        try {
            editionCombo.setItems(
                FXCollections.observableArrayList(catalogs.editions())
            );
            editionCombo.setValue(selected);
        } finally {
            updatingEditionSelection = false;
        }
        setEditionScopedTabsEnabled(selected != null);

        replaceItems(
            grandPrixEditCombo,
            grandPrixCatalog,
            GrandPrixOption::id
        );
        replaceItems(
            weekendGrandPrixCombo,
            grandPrixCatalog,
            GrandPrixOption::id
        );
        replaceItems(
            constructorEnrollmentCombo,
            constructorCatalog,
            ConstructorOption::id
        );
        replaceItems(
            driverEnrollmentCombo,
            driverCatalog,
            DriverRegistryOption::id
        );

        if (editionNumberField.getText().isBlank()) {
            final int nextNumber = catalogs.editions().stream()
                .mapToInt(Edizione::numero)
                .max()
                .orElse(0) + 1;
            editionNumberField.setText(Integer.toString(nextNumber));
        }
        if (editionYearField.getText().isBlank()) {
            final int suggestedYear = catalogs.editions().stream()
                .mapToInt(Edizione::anno)
                .max()
                .orElse(LocalDate.now().getYear() - 1) + 1;
            editionYearField.setText(Integer.toString(suggestedYear));
        }
    }

    private void applyEditionCatalogs(final EditionCatalogs catalogs) {
        applyEditionStatus(catalogs.status());

        final Set<Integer> scheduledGrandPrix = new HashSet<>();
        for (RaceWeekend weekend : catalogs.weekends()) {
            scheduledGrandPrix.add(weekend.grandPrixId());
        }
        final List<GrandPrixOption> availableGrandPrix =
            grandPrixCatalog.stream()
                .filter(option -> !scheduledGrandPrix.contains(option.id()))
                .toList();
        replaceItems(
            weekendGrandPrixCombo,
            availableGrandPrix,
            GrandPrixOption::id
        );

        final Set<Integer> enrolledConstructorIds = new HashSet<>();
        for (EnrolledConstructorOption constructor
            : catalogs.enrolledConstructors()) {
            enrolledConstructorIds.add(constructor.constructorId());
        }
        final List<ConstructorOption> availableConstructors =
            constructorCatalog.stream()
                .filter(
                    option -> !enrolledConstructorIds.contains(option.id())
                )
                .toList();
        replaceItems(
            constructorEnrollmentCombo,
            availableConstructors,
            ConstructorOption::id
        );
        replaceItems(
            enrolledConstructorCombo,
            catalogs.enrolledConstructors(),
            EnrolledConstructorOption::constructorId
        );

        final Set<Integer> enrolledDriverIds = new HashSet<>();
        for (DriverOption driver : catalogs.enrolledDrivers()) {
            enrolledDriverIds.add(driver.id());
        }
        final List<DriverRegistryOption> availableDrivers =
            driverCatalog.stream()
                .filter(option -> !enrolledDriverIds.contains(option.id()))
                .toList();
        replaceItems(
            driverEnrollmentCombo,
            availableDrivers,
            DriverRegistryOption::id
        );
        replaceItems(
            performanceDriverCombo,
            catalogs.enrolledDrivers(),
            DriverOption::id
        );
        replaceItems(
            performanceWeekendCombo,
            catalogs.weekends(),
            RaceWeekend::grandPrixId
        );
    }

    private void applyEditionStatus(final EditionStatus status) {
        final Edizione edition = editionCombo.getValue();
        completionTitle.setText(
            status.complete()
                ? edition + " — completa"
                : edition + " — popolamento in corso"
        );
        completionDetail.setText(
            "%d/24 weekend · %d/10 scuderie · %d/20 piloti · "
                .formatted(
                    status.weekends(),
                    status.constructors(),
                    status.drivers()
                )
                + "%d/10 scuderie con due piloti"
                    .formatted(status.constructorsWithTwoDrivers())
        );
        final double completed = Math.min(status.weekends(), 24)
            + Math.min(status.constructors(), 10)
            + Math.min(status.drivers(), 20)
            + Math.min(status.constructorsWithTwoDrivers(), 10);
        completionProgress.setProgress(completed / COMPLETION_TOTAL);
        completionTitle.setStyle(
            status.complete()
                ? "-fx-font-size: 16px; -fx-font-weight: bold; "
                    + "-fx-text-fill: #176b2c;"
                : "-fx-font-size: 16px; -fx-font-weight: bold;"
        );
    }

    private void clearEditionScopedCatalogs() {
        weekendGrandPrixCombo.getItems().clear();
        constructorEnrollmentCombo.getItems().clear();
        driverEnrollmentCombo.getItems().clear();
        enrolledConstructorCombo.getItems().clear();
        performanceDriverCombo.getItems().clear();
        performanceWeekendCombo.getItems().clear();
        completionTitle.setText("Nessuna edizione selezionata");
        completionDetail.setText(
            "Crea o seleziona un'edizione per visualizzarne lo stato."
        );
        completionProgress.setProgress(0);
    }

    private void applyProcessingOutcome(final ProcessingOutcome outcome) {
        processingOutcomeLabel.setText(
            "Punteggio fantasy calcolato: %d. %s".formatted(
                outcome.driverFantasyPoints(),
                outcome.weekendProcessable()
                    ? "Il weekend è terminato e completo: O2 e O3 sono "
                        + "state aggiornate."
                    : "Il weekend non è ancora elaborabile; non sono "
                        + "memorizzati risultati parziali."
            )
        );
    }

    private <T> void executeMutation(
        final String operationName,
        final String progressMessage,
        final Supplier<T> operation,
        final Consumer<T> onSuccess
    ) {
        execute(
            operationName,
            progressMessage,
            operation,
            onSuccess
        );
    }

    private <T> void execute(
        final String operationName,
        final String progressMessage,
        final Supplier<T> operation,
        final Consumer<T> onSuccess
    ) {
        if (busy.get()) {
            return;
        }
        busy.set(true);
        setOperationStatus(progressMessage, false);
        AdminTasks.run(
            operationName,
            operation,
            result -> {
                busy.set(false);
                onSuccess.accept(result);
            },
            failure -> {
                busy.set(false);
                showOperationError(operationName, failure);
            }
        );
    }

    private void showOperationError(
        final String operationName,
        final Throwable failure
    ) {
        final String message = friendlyMessage(failure);
        setOperationStatus(message, true);
        showAlert(
            Alert.AlertType.ERROR,
            "Operazione non riuscita",
            readableOperation(operationName),
            message
        );
    }

    private void showInputError(final RuntimeException exception) {
        final String message = friendlyMessage(exception);
        setOperationStatus(message, true);
        showAlert(
            Alert.AlertType.WARNING,
            "Dati non validi",
            "Controlla i campi del form",
            message
        );
    }

    private void confirm(final String message) {
        showAlert(
            Alert.AlertType.INFORMATION,
            "Operazione completata",
            null,
            message
        );
    }

    private void showAlert(
        final Alert.AlertType type,
        final String title,
        final String header,
        final String message
    ) {
        final Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        if (root.getScene() != null && root.getScene().getWindow() != null) {
            alert.initOwner(root.getScene().getWindow());
        }
        alert.show();
    }

    private void setOperationStatus(
        final String message,
        final boolean error
    ) {
        operationStatus.setText(message);
        operationStatus.setStyle(
            error
                ? "-fx-text-fill: #a00018; -fx-font-weight: bold;"
                : ""
        );
    }

    private void populateGrandPrixForm(final GrandPrixOption grandPrix) {
        if (grandPrix == null) {
            return;
        }
        grandPrixNameField.setText(grandPrix.name());
        circuitField.setText(grandPrix.circuit());
        countryField.setText(grandPrix.country());
        cityField.setText(grandPrix.city());
    }

    private void clearGrandPrixForm() {
        grandPrixEditCombo.setValue(null);
        grandPrixNameField.clear();
        circuitField.clear();
        countryField.clear();
        cityField.clear();
    }

    private void setEditionScopedTabsEnabled(final boolean enabled) {
        for (Tab tab : editionScopedTabs) {
            tab.setDisable(!enabled);
        }
    }

    private Integer selectedEditionId() {
        final Edizione selected = editionCombo.getValue();
        return selected == null ? null : selected.id();
    }

    private static Edizione selectEdition(
        final List<Edizione> editions,
        final Integer preferredId
    ) {
        if (preferredId != null) {
            for (Edizione edition : editions) {
                if (edition.id() == preferredId) {
                    return edition;
                }
            }
        }
        return editions.stream()
            .max(
                Comparator.comparingInt(Edizione::anno)
                    .thenComparingInt(Edizione::numero)
            )
            .orElse(null);
    }

    private static <T> T requireSelection(
        final ComboBox<T> comboBox,
        final String message
    ) {
        final T value = comboBox.getValue();
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static LocalDate requireDate(
        final DatePicker datePicker,
        final String message
    ) {
        final LocalDate value = datePicker.getValue();
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static int parseInt(
        final TextField field,
        final String label
    ) {
        final String value = field.getText();
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " è obbligatorio.");
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                label + " deve essere un numero intero.",
                exception
            );
        }
    }

    private static Integer parseOptionalPosition(
        final TextField field,
        final String label
    ) {
        final String value = field.getText();
        if (value == null || value.isBlank()) {
            return null;
        }
        final int parsed;
        try {
            parsed = Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                label + " deve essere un numero intero.",
                exception
            );
        }
        if (parsed < 1 || parsed > 20) {
            throw new IllegalArgumentException(
                label + " deve essere compresa tra 1 e 20."
            );
        }
        return parsed;
    }

    private static String friendlyMessage(final Throwable failure) {
        if (failure instanceof AppException appException) {
            if (appException.code() == ErrorCode.CONNECTION) {
                return "Impossibile collegarsi a MySQL. Verifica servizio e "
                    + "configurazione del database.";
            }
            if (
                appException.getMessage() != null
                    && !appException.getMessage().isBlank()
            ) {
                return appException.getMessage();
            }
        }
        if (
            failure instanceof IllegalArgumentException
                && failure.getMessage() != null
                && !failure.getMessage().isBlank()
        ) {
            return failure.getMessage();
        }
        return "Operazione non completata. Verifica la connessione e riprova.";
    }

    private static String readableOperation(final String operationName) {
        return operationName.replace('-', ' ');
    }

    private static GridPane createGrid() {
        final GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        final ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(180);
        final ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(labels, fields);
        return grid;
    }

    private static void addRow(
        final GridPane grid,
        final int row,
        final String labelText,
        final Node field
    ) {
        final Label label = new Label(labelText + ":");
        label.setStyle("-fx-font-weight: bold;");
        GridPane.setHgrow(field, Priority.ALWAYS);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    private static Node formPage(
        final String titleText,
        final String descriptionText,
        final Node form,
        final Node actions
    ) {
        final Label title = new Label(titleText);
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        final Label description = new Label(descriptionText);
        description.setWrapText(true);
        final VBox content = new VBox(
            14,
            title,
            description,
            new Separator(),
            form,
            actions
        );
        content.setPadding(new Insets(20));
        content.setMaxWidth(760);

        final ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        return scrollPane;
    }

    private static <T> void configureCombo(
        final ComboBox<T> combo,
        final String prompt
    ) {
        combo.setPromptText(prompt);
        combo.setPrefWidth(FIELD_WIDTH);
        combo.setMaxWidth(Double.MAX_VALUE);
    }

    private static <T> void replaceItems(
        final ComboBox<T> combo,
        final List<T> items,
        final java.util.function.ToIntFunction<T> idExtractor
    ) {
        final T selected = combo.getValue();
        final Integer selectedId = selected == null
            ? null
            : idExtractor.applyAsInt(selected);
        combo.setItems(FXCollections.observableArrayList(items));
        if (selectedId != null) {
            for (T item : items) {
                if (idExtractor.applyAsInt(item) == selectedId) {
                    combo.setValue(item);
                    return;
                }
            }
        }
        combo.setValue(null);
    }

    private record GlobalCatalogs(
        List<Edizione> editions,
        List<GrandPrixOption> grandPrix,
        List<ConstructorOption> constructors,
        List<DriverRegistryOption> drivers
    ) {
    }

    private record EditionCatalogs(
        EditionStatus status,
        List<EnrolledConstructorOption> enrolledConstructors,
        List<DriverOption> enrolledDrivers,
        List<RaceWeekend> weekends
    ) {
    }
}
