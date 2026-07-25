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
import it.unibo.fantasyf1.model.WeekendPerformanceStatus;
import it.unibo.fantasyf1.service.AdminService;
import it.unibo.fantasyf1.service.PerformanceRequest;
import it.unibo.fantasyf1.ui.UiTheme;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Dashboard JavaFX per A1-A9. Dipende esclusivamente dal service trusted.
 */
public final class AdminDashboard {

    private static final double FIELD_WIDTH = 360;
    private static final double COMPLETION_TOTAL = 64.0;

    private final AdminService admin;
    private final Runnable showModeSelection;
    private final BorderPane root = new BorderPane();
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private final BooleanProperty selectedWeekendConcluded =
        new SimpleBooleanProperty();

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
    private final Button removeEditionButton =
        new Button("Elimina edizione");

    private final Button saveGrandPrixButton =
        new Button("Inserisci Gran Premio");
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
    private final Button concludeWeekendButton =
        new Button("Convalida e concludi weekend");
    private final Button reopenWeekendButton =
        new Button("Riapri weekend");
    private final Button recordPerformanceButton =
        new Button("Registra / correggi prestazione");
    private final ListView<RaceWeekend> editionWeekendList =
        new ListView<>();
    private final ListView<GrandPrixOption> availableGrandPrixList =
        new ListView<>();
    private final ListView<EnrolledConstructorOption>
        editionConstructorList = new ListView<>();
    private final ListView<ConstructorOption> availableConstructorList =
        new ListView<>();
    private final ListView<DriverOption> editionDriverList =
        new ListView<>();
    private final ListView<DriverRegistryOption> availableDriverList =
        new ListView<>();
    private final ComboBox<RaceWeekend> overviewPerformanceWeekendCombo =
        new ComboBox<>();
    private final Label overviewPerformanceSummary = new Label(
        "Seleziona un GP per verificare le prestazioni."
    );
    private final ListView<WeekendPerformanceStatus>
        overviewPerformanceList = new ListView<>();

    private List<GrandPrixOption> grandPrixCatalog = List.of();
    private List<ConstructorOption> constructorCatalog = List.of();
    private List<DriverRegistryOption> driverCatalog = List.of();
    private boolean updatingEditionSelection;
    private boolean updatingOverviewPerformanceSelection;

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
        configureCombo(
            overviewPerformanceWeekendCombo,
            "Seleziona un GP da controllare"
        );

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
        performanceWeekendCombo.valueProperty().addListener(
            (observable, previous, selected) -> {
                selectedWeekendConcluded.set(
                    selected != null && selected.concluded()
                );
            }
        );
        overviewPerformanceWeekendCombo.valueProperty().addListener(
            (observable, previous, selected) -> {
                if (!updatingOverviewPerformanceSelection) {
                    refreshOverviewPerformanceStatus();
                }
            }
        );
        configureRemovalList(
            editionWeekendList,
            this::removeWeekend,
            ignored -> true
        );
        configureRemovalList(
            availableGrandPrixList,
            this::removeGrandPrix,
            ignored -> true
        );
        configureRemovalList(
            editionConstructorList,
            this::removeConstructorEnrollment,
            ignored -> true
        );
        configureRemovalList(
            availableConstructorList,
            this::removeConstructor,
            ignored -> true
        );
        configureRemovalList(
            editionDriverList,
            this::removeDriverEnrollment,
            ignored -> true
        );
        configureRemovalList(
            availableDriverList,
            this::removeDriver,
            ignored -> true
        );
        configureRemovalList(
            overviewPerformanceList,
            this::removePerformance,
            WeekendPerformanceStatus::recorded
        );
        overviewPerformanceSummary.setWrapText(true);
        editionWeekendList.setPlaceholder(
            new Label("Nessun GP inserito nell'edizione.")
        );
        availableGrandPrixList.setPlaceholder(
            new Label("Nessun GP anagrafico disponibile.")
        );
        editionConstructorList.setPlaceholder(
            new Label("Nessuna scuderia iscritta.")
        );
        availableConstructorList.setPlaceholder(
            new Label("Nessuna scuderia anagrafica disponibile.")
        );
        editionDriverList.setPlaceholder(
            new Label("Nessun pilota iscritto.")
        );
        availableDriverList.setPlaceholder(
            new Label("Nessun pilota anagrafico disponibile.")
        );
        overviewPerformanceList.setPlaceholder(
            new Label("Nessun pilota iscritto da verificare.")
        );
        for (ListView<?> list : List.<ListView<?>>of(
            editionWeekendList,
            availableGrandPrixList,
            editionConstructorList,
            availableConstructorList,
            editionDriverList,
            availableDriverList,
            overviewPerformanceList
        )) {
            UiTheme.configureReadOnly(list);
        }
        refreshButton.setOnAction(
            event -> refreshAll(selectedEditionId(), "Cataloghi aggiornati.")
        );
        removeEditionButton.getStyleClass().add("danger-button");
        removeEditionButton.setOnAction(event -> removeEdition());

        progressIndicator.setMaxSize(22, 22);
        progressIndicator.visibleProperty().bind(busy);
        progressIndicator.managedProperty().bind(busy);
        tabs.disableProperty().bind(busy);
        editionCombo.disableProperty().bind(busy);
        refreshButton.disableProperty().bind(busy);
        removeEditionButton.disableProperty().bind(
            busy.or(editionCombo.valueProperty().isNull())
        );
        concludeWeekendButton.disableProperty().bind(
            busy
                .or(editionCombo.valueProperty().isNull())
                .or(performanceWeekendCombo.valueProperty().isNull())
                .or(selectedWeekendConcluded)
        );
        reopenWeekendButton.disableProperty().bind(
            busy
                .or(editionCombo.valueProperty().isNull())
                .or(performanceWeekendCombo.valueProperty().isNull())
                .or(selectedWeekendConcluded.not())
        );
        recordPerformanceButton.disableProperty().bind(
            busy
                .or(editionCombo.valueProperty().isNull())
                .or(performanceWeekendCombo.valueProperty().isNull())
                .or(performanceDriverCombo.valueProperty().isNull())
                .or(selectedWeekendConcluded)
        );
        for (Node control : List.of(
            performanceDriverCombo,
            qualifyingPositionField,
            racePositionField,
            penalizedCheck,
            fastestLapCheck
        )) {
            control.disableProperty().bind(
                busy.or(selectedWeekendConcluded)
            );
        }
    }

    private void buildLayout() {
        root.setPadding(new Insets(20));
        root.getStyleClass().addAll("app-root", "admin-root");
        root.setTop(createHeader());
        root.setCenter(createTabs());
        root.setBottom(createStatusBar());
        BorderPane.setMargin(tabs, new Insets(16, 0, 12, 0));
    }

    private Node createHeader() {
        final Label title = new Label("Amministrazione Fantasy Formula 1");
        title.getStyleClass().add("page-title");
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

        final Label editionLabel = new Label("Edizione operativa:");
        editionLabel.getStyleClass().add("form-label");
        final HBox editionRow = new HBox(
            10,
            editionLabel,
            editionCombo,
            refreshButton
        );
        editionRow.setAlignment(Pos.CENTER_LEFT);

        completionTitle.getStyleClass().add("subsection-title");
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
        completionBox.getStyleClass().add("summary-card");

        final VBox header = new VBox(
            10,
            titleRow,
            editionRow,
            completionBox
        );
        header.getStyleClass().add("app-header");
        return header;
    }

    private Node createTabs() {
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
            createTab(
                "Stato edizione",
                createEditionOverview(),
                false
            ),
            createTab("Edizione", createEditionForm(), false),
            createTab("Gran Premio", createGrandPrixForm(), false),
            createTab("Weekend", createWeekendForm(), true),
            createTab("Scuderia", createConstructorForm(), false),
            createTab(
                "Iscrivi scuderia",
                createConstructorEnrollmentForm(),
                true
            ),
            createTab("Pilota", createDriverForm(), false),
            createTab(
                "Iscrivi pilota",
                createDriverEnrollmentForm(),
                true
            ),
            createTab("Risultati", createPerformanceForm(), true)
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

    private Node createEditionOverview() {
        final Label title = new Label(
            "Contenuto e completezza dell'edizione"
        );
        title.getStyleClass().add("section-title");
        final Region titleSpacer = new Region();
        HBox.setHgrow(titleSpacer, Priority.ALWAYS);
        final HBox titleRow = new HBox(
            10,
            title,
            titleSpacer,
            removeEditionButton
        );
        titleRow.setAlignment(Pos.CENTER_LEFT);
        final TabPane overviewTabs = new TabPane(
            overviewTab(
                "Calendario",
                comparisonPane(
                    "GP previsti nell'edizione",
                    editionWeekendList,
                    "GP in catalogo non ancora inseriti",
                    availableGrandPrixList
                )
            ),
            overviewTab(
                "Scuderie",
                comparisonPane(
                    "Scuderie iscritte",
                    editionConstructorList,
                    "Scuderie anagrafiche non iscritte",
                    availableConstructorList
                )
            ),
            overviewTab(
                "Piloti",
                comparisonPane(
                    "Piloti iscritti",
                    editionDriverList,
                    "Piloti anagrafici non iscritti",
                    availableDriverList
                )
            ),
            overviewTab(
                "Prestazioni GP",
                createPerformanceOverview()
            )
        );
        overviewTabs.setTabClosingPolicy(
            TabPane.TabClosingPolicy.UNAVAILABLE
        );
        VBox.setVgrow(overviewTabs, Priority.ALWAYS);

        final VBox content = new VBox(
            12,
            titleRow,
            new Separator(),
            overviewTabs
        );
        content.setPadding(new Insets(20));
        content.getStyleClass().add("content-pane");
        return content;
    }

    private Node createPerformanceOverview() {
        final Label selectorLabel = new Label("Gran Premio:");
        selectorLabel.getStyleClass().add("form-label");
        final HBox selector = new HBox(
            10,
            selectorLabel,
            overviewPerformanceWeekendCombo
        );
        selector.setAlignment(Pos.CENTER_LEFT);

        final VBox content = new VBox(
            12,
            selector,
            overviewPerformanceSummary,
            overviewPerformanceList
        );
        content.setPadding(new Insets(14));
        VBox.setVgrow(overviewPerformanceList, Priority.ALWAYS);
        return content;
    }

    private static Node comparisonPane(
        final String presentTitle,
        final ListView<?> present,
        final String availableTitle,
        final ListView<?> available
    ) {
        final VBox presentBox = listBox(presentTitle, present);
        final VBox availableBox = listBox(availableTitle, available);
        HBox.setHgrow(presentBox, Priority.ALWAYS);
        HBox.setHgrow(availableBox, Priority.ALWAYS);
        presentBox.setMaxWidth(Double.MAX_VALUE);
        availableBox.setMaxWidth(Double.MAX_VALUE);

        final HBox content = new HBox(14, presentBox, availableBox);
        content.setPadding(new Insets(14));
        return content;
    }

    private static VBox listBox(
        final String title,
        final ListView<?> list
    ) {
        final Label label = new Label(title);
        label.getStyleClass().add("form-label");
        final VBox box = new VBox(8, label, list);
        box.getStyleClass().add("comparison-card");
        VBox.setVgrow(list, Priority.ALWAYS);
        return box;
    }

    private static Tab overviewTab(
        final String title,
        final Node content
    ) {
        return new Tab(title, content);
    }

    private Node createEditionForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Numero edizione", editionNumberField);
        addRow(form, 1, "Anno", editionYearField);

        final Button save = new Button("Crea edizione");
        save.getStyleClass().add("primary-button");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> createEdition());
        return formPage(
            "A1 — Nuova edizione",
            form,
            new HBox(10, save)
        );
    }

    private Node createGrandPrixForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Nome", grandPrixNameField);
        addRow(form, 1, "Circuito", circuitField);
        addRow(form, 2, "Nazione", countryField);
        addRow(form, 3, "Città", cityField);

        saveGrandPrixButton.setOnAction(event -> saveGrandPrix());
        saveGrandPrixButton.getStyleClass().add("primary-button");
        for (TextField field : List.of(
            grandPrixNameField,
            circuitField,
            countryField,
            cityField
        )) {
            field.disableProperty().bind(busy);
        }
        saveGrandPrixButton.disableProperty().bind(busy);
        return formPage(
            "A2 — Inserimento Gran Premio",
            form,
            new HBox(10, saveGrandPrixButton)
        );
    }

    private Node createWeekendForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Gran Premio", weekendGrandPrixCombo);
        addRow(form, 1, "Numero round", weekendRoundField);
        addRow(form, 2, "Data inizio", weekendStartDate);
        addRow(form, 3, "Data fine", weekendEndDate);

        final Button save = new Button("Inserisci weekend");
        save.getStyleClass().add("primary-button");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> addWeekend());
        return formPage(
            "A3 — Weekend nell'edizione selezionata",
            form,
            new HBox(10, save)
        );
    }

    private Node createConstructorForm() {
        final GridPane form = createGrid();
        addRow(form, 0, "Nome scuderia", constructorNameField);

        final Button save = new Button("Registra scuderia");
        save.getStyleClass().add("primary-button");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> createConstructor());
        return formPage(
            "A4 — Scuderia anagrafica",
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
        save.getStyleClass().add("primary-button");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> enrollConstructor());
        return formPage(
            "A5 — Iscrizione scuderia",
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
        save.getStyleClass().add("primary-button");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> createDriver());
        return formPage(
            "A6 — Pilota anagrafico",
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
        save.getStyleClass().add("primary-button");
        save.disableProperty().bind(busy);
        save.setOnAction(event -> enrollDriver());
        return formPage(
            "A7 — Iscrizione pilota e assegnazione scuderia",
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

        recordPerformanceButton.setOnAction(event -> recordPerformance());
        concludeWeekendButton.setOnAction(event -> concludeWeekend());
        reopenWeekendButton.setOnAction(event -> reopenWeekend());
        recordPerformanceButton.getStyleClass().add("primary-button");
        concludeWeekendButton.getStyleClass().add("primary-button");
        reopenWeekendButton.getStyleClass().add("danger-button");

        final HBox actions = new HBox(
            10,
            recordPerformanceButton,
            concludeWeekendButton,
            reopenWeekendButton
        );
        return formPage(
            "A8/A9/A10 — Risultati ufficiali e stato del weekend",
            form,
            actions
        );
    }

    private Node createStatusBar() {
        operationStatus.setWrapText(true);
        operationStatus.getStyleClass().add("log-text");
        final Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final HBox statusBar = new HBox(
            10,
            progressIndicator,
            operationStatus,
            spacer
        );
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setPadding(new Insets(8, 12, 8, 12));
        statusBar.getStyleClass().add("status-bar");
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
                    refreshAll(
                        editionId,
                        "Edizione creata correttamente."
                    );
                }
            );
        } catch (RuntimeException exception) {
            showInputError(exception);
        }
    }

    private void saveGrandPrix() {
        final String name = grandPrixNameField.getText();
        final String circuit = circuitField.getText();
        final String country = countryField.getText();
        final String city = cityField.getText();
        executeMutation(
            "inserimento-gran-premio",
            "Inserimento del Gran Premio in corso…",
            () -> admin.createGrandPrix(
                name,
                circuit,
                country,
                city
            ),
            ignored -> {
                clearGrandPrixForm();
                refreshAll(
                    selectedEditionId(),
                    "Gran Premio inserito correttamente."
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
                    refreshAll(
                        edition.id(),
                        "Weekend inserito correttamente."
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
                refreshAll(
                    selectedEditionId(),
                    "Scuderia registrata correttamente."
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
                    refreshAll(
                        edition.id(),
                        "Scuderia iscritta correttamente."
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
                    refreshAll(
                        selectedEditionId(),
                        "Pilota registrato correttamente."
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
                    refreshAll(
                        edition.id(),
                        "Pilota iscritto correttamente."
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
                "Registrazione della prestazione in corso…",
                () -> {
                    admin.recordPerformance(request);
                    return Boolean.TRUE;
                },
                ignored -> {
                    refreshAll(
                        edition.id(),
                        "Prestazione salvata correttamente."
                    );
                }
            );
        } catch (RuntimeException exception) {
            showInputError(exception);
        }
    }

    private void concludeWeekend() {
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
                "conclusione-weekend",
                "Convalida, conclusione e calcolo dei punteggi in corso…",
                () -> {
                    admin.concludeWeekend(
                        edition.id(),
                        weekend.grandPrixId()
                    );
                    return Boolean.TRUE;
                },
                ignored -> {
                    refreshAll(
                        edition.id(),
                        "Weekend concluso. I punteggi fantasy sono ora "
                            + "disponibili."
                    );
                }
            );
        } catch (RuntimeException exception) {
            showInputError(exception);
        }
    }

    private void reopenWeekend() {
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
                "riapertura-weekend",
                "Riapertura e invalidazione dei dati fantasy in corso…",
                () -> {
                    admin.reopenWeekend(
                        edition.id(),
                        weekend.grandPrixId()
                    );
                    return Boolean.TRUE;
                },
                ignored -> {
                    refreshAll(
                        edition.id(),
                        "Weekend riaperto correttamente."
                    );
                }
            );
        } catch (RuntimeException exception) {
            showInputError(exception);
        }
    }

    private void removeEdition() {
        final Edizione edition = editionCombo.getValue();
        if (edition == null) {
            return;
        }
        executeMutation(
            "rimozione-edizione",
            "Rimozione dell'edizione in corso…",
            () -> {
                admin.removeEdition(edition.id());
                return Boolean.TRUE;
            },
            ignored -> {
                refreshAll(null, "Edizione eliminata correttamente.");
            }
        );
    }

    private void removeGrandPrix(final GrandPrixOption grandPrix) {
        final Integer editionId = selectedEditionId();
        executeMutation(
            "rimozione-gran-premio",
            "Rimozione del Gran Premio in corso…",
            () -> {
                admin.removeGrandPrix(grandPrix.id());
                return Boolean.TRUE;
            },
            ignored -> {
                refreshAll(
                    editionId,
                    "Gran Premio eliminato correttamente."
                );
            }
        );
    }

    private void removeWeekend(final RaceWeekend weekend) {
        if (weekend.concluded()) {
            showInputError(new IllegalArgumentException(
                "Riapri il weekend concluso prima di rimuoverlo."
            ));
            return;
        }
        executeMutation(
            "rimozione-weekend",
            "Rimozione del weekend in corso…",
            () -> {
                admin.removeWeekend(
                    weekend.editionId(),
                    weekend.grandPrixId()
                );
                return Boolean.TRUE;
            },
            ignored -> {
                refreshAll(
                    weekend.editionId(),
                    "Weekend rimosso correttamente."
                );
            }
        );
    }

    private void removeConstructor(
        final ConstructorOption constructor
    ) {
        final Integer editionId = selectedEditionId();
        executeMutation(
            "rimozione-scuderia",
            "Rimozione della scuderia in corso…",
            () -> {
                admin.removeConstructor(constructor.id());
                return Boolean.TRUE;
            },
            ignored -> {
                refreshAll(
                    editionId,
                    "Scuderia anagrafica eliminata correttamente."
                );
            }
        );
    }

    private void removeConstructorEnrollment(
        final EnrolledConstructorOption constructor
    ) {
        executeMutation(
            "rimozione-iscrizione-scuderia",
            "Rimozione dell'iscrizione della scuderia in corso…",
            () -> {
                admin.removeConstructorEnrollment(
                    constructor.editionId(),
                    constructor.constructorId()
                );
                return Boolean.TRUE;
            },
            ignored -> {
                refreshAll(
                    constructor.editionId(),
                    "Iscrizione della scuderia rimossa correttamente."
                );
            }
        );
    }

    private void removeDriver(final DriverRegistryOption driver) {
        final Integer editionId = selectedEditionId();
        executeMutation(
            "rimozione-pilota",
            "Rimozione del pilota in corso…",
            () -> {
                admin.removeDriver(driver.id());
                return Boolean.TRUE;
            },
            ignored -> {
                refreshAll(
                    editionId,
                    "Pilota anagrafico eliminato correttamente."
                );
            }
        );
    }

    private void removeDriverEnrollment(final DriverOption driver) {
        executeMutation(
            "rimozione-iscrizione-pilota",
            "Rimozione dell'iscrizione del pilota in corso…",
            () -> {
                admin.removeDriverEnrollment(
                    driver.editionId(),
                    driver.id()
                );
                return Boolean.TRUE;
            },
            ignored -> {
                refreshAll(
                    driver.editionId(),
                    "Iscrizione del pilota rimossa correttamente."
                );
            }
        );
    }

    private void removePerformance(
        final WeekendPerformanceStatus performance
    ) {
        if (!performance.recorded()) {
            return;
        }
        final RaceWeekend weekend =
            overviewPerformanceWeekendCombo.getValue();
        if (weekend != null && weekend.concluded()) {
            showInputError(new IllegalArgumentException(
                "Riapri il weekend concluso prima di rimuovere "
                    + "una prestazione."
            ));
            return;
        }
        executeMutation(
            "rimozione-prestazione",
            "Rimozione della prestazione in corso…",
            () -> {
                admin.removePerformance(
                    performance.editionId(),
                    performance.grandPrixId(),
                    performance.driverId()
                );
                return Boolean.TRUE;
            },
            ignored -> {
                refreshAll(
                    performance.editionId(),
                    "Prestazione sportiva rimossa correttamente."
                );
            }
        );
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
                    setOperationStatus(successMessage, false);
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
                refreshOverviewPerformanceStatus(successMessage);
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
        editionWeekendList.setItems(
            FXCollections.observableArrayList(catalogs.weekends())
        );
        availableGrandPrixList.setItems(
            FXCollections.observableArrayList(availableGrandPrix)
        );
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
        editionConstructorList.setItems(
            FXCollections.observableArrayList(
                catalogs.enrolledConstructors()
            )
        );
        availableConstructorList.setItems(
            FXCollections.observableArrayList(availableConstructors)
        );
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
        editionDriverList.setItems(
            FXCollections.observableArrayList(catalogs.enrolledDrivers())
        );
        availableDriverList.setItems(
            FXCollections.observableArrayList(availableDrivers)
        );
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
        updatingOverviewPerformanceSelection = true;
        try {
            replaceItems(
                overviewPerformanceWeekendCombo,
                catalogs.weekends(),
                RaceWeekend::grandPrixId
            );
            selectFirstIfEmpty(overviewPerformanceWeekendCombo);
        } finally {
            updatingOverviewPerformanceSelection = false;
        }
        selectFirstIfEmpty(performanceDriverCombo);
        selectFirstIfEmpty(performanceWeekendCombo);
    }

    private void refreshOverviewPerformanceStatus() {
        refreshOverviewPerformanceStatus(null);
    }

    private void refreshOverviewPerformanceStatus(
        final String completionMessage
    ) {
        final Edizione edition = editionCombo.getValue();
        final RaceWeekend weekend =
            overviewPerformanceWeekendCombo.getValue();
        if (edition == null || weekend == null) {
            overviewPerformanceList.getItems().clear();
            overviewPerformanceSummary.setText(
                "Nessun GP disponibile da controllare."
            );
            if (completionMessage != null) {
                setOperationStatus(completionMessage, false);
            }
            return;
        }
        overviewPerformanceList.getItems().clear();
        overviewPerformanceSummary.setText(
            "Controllo delle prestazioni in corso…"
        );
        execute(
            "controllo-prestazioni",
            "Controllo delle prestazioni di "
                + weekend.grandPrixName()
                + "…",
            () -> admin.weekendPerformanceStatus(
                edition.id(),
                weekend.grandPrixId()
            ),
            statuses -> {
                final Edizione currentEdition = editionCombo.getValue();
                final RaceWeekend currentWeekend =
                    overviewPerformanceWeekendCombo.getValue();
                if (
                    currentEdition == null
                        || currentWeekend == null
                        || currentEdition.id() != edition.id()
                        || currentWeekend.grandPrixId()
                            != weekend.grandPrixId()
                ) {
                    return;
                }
                overviewPerformanceList.setItems(
                    FXCollections.observableArrayList(statuses)
                );
                final long recorded = statuses.stream()
                    .filter(WeekendPerformanceStatus::recorded)
                    .count();
                final long missing = statuses.size() - recorded;
                overviewPerformanceSummary.setText(
                    "%s · %d/%d prestazioni registrate · %d mancanti · %s"
                        .formatted(
                            weekend.grandPrixName(),
                            recorded,
                            statuses.size(),
                            missing,
                            weekend.concluded()
                                ? "GP concluso"
                                : "GP non concluso"
                        )
                );
                setOperationStatus(
                    completionMessage == null
                        ? "Controllo prestazioni aggiornato."
                        : completionMessage,
                    false
                );
            }
        );
    }

    private void applyEditionStatus(final EditionStatus status) {
        final Edizione edition = editionCombo.getValue();
        completionTitle.setText(
            status.complete()
                ? edition + " — Completa"
                : edition + " — Popolamento in corso"
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
        completionTitle.getStyleClass().remove("success-text");
        if (status.complete()) {
            completionTitle.getStyleClass().add("success-text");
        }
    }

    private void clearEditionScopedCatalogs() {
        weekendGrandPrixCombo.getItems().clear();
        constructorEnrollmentCombo.getItems().clear();
        driverEnrollmentCombo.getItems().clear();
        enrolledConstructorCombo.getItems().clear();
        performanceDriverCombo.getItems().clear();
        performanceWeekendCombo.getItems().clear();
        editionWeekendList.getItems().clear();
        availableGrandPrixList.setItems(
            FXCollections.observableArrayList(grandPrixCatalog)
        );
        editionConstructorList.getItems().clear();
        availableConstructorList.setItems(
            FXCollections.observableArrayList(constructorCatalog)
        );
        editionDriverList.getItems().clear();
        availableDriverList.setItems(
            FXCollections.observableArrayList(driverCatalog)
        );
        updatingOverviewPerformanceSelection = true;
        try {
            overviewPerformanceWeekendCombo.getItems().clear();
            overviewPerformanceWeekendCombo.setValue(null);
        } finally {
            updatingOverviewPerformanceSelection = false;
        }
        overviewPerformanceList.getItems().clear();
        overviewPerformanceSummary.setText(
            "Seleziona un GP per verificare le prestazioni."
        );
        completionTitle.setText("Nessuna edizione selezionata");
        completionTitle.getStyleClass().remove("success-text");
        completionDetail.setText(
            "Crea o seleziona un'edizione per visualizzarne lo stato."
        );
        completionProgress.setProgress(0);
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
                showOperationError(failure);
            }
        );
    }

    private void showOperationError(final Throwable failure) {
        final String message = friendlyMessage(failure);
        setOperationStatus(message, true);
    }

    private void showInputError(final RuntimeException exception) {
        final String message = friendlyMessage(exception);
        setOperationStatus(message, true);
    }

    private void setOperationStatus(
        final String message,
        final boolean error
    ) {
        operationStatus.setText(capitalizeSentence(message));
        operationStatus.getStyleClass().remove("error-text");
        if (error) {
            operationStatus.getStyleClass().add("error-text");
        }
    }

    private void clearGrandPrixForm() {
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

    private static String capitalizeSentence(final String message) {
        if (message == null || message.isBlank()) {
            return "";
        }
        final StringBuilder result = new StringBuilder(message.length());
        boolean sentenceStart = true;
        for (int index = 0; index < message.length(); index++) {
            final char character = message.charAt(index);
            if (sentenceStart && Character.isLetter(character)) {
                result.append(Character.toUpperCase(character));
                sentenceStart = false;
            } else {
                result.append(character);
            }
            if (character == '.' || character == '!' || character == '?') {
                sentenceStart = true;
            } else if (!Character.isWhitespace(character)
                && !Character.isLetter(character)) {
                sentenceStart = false;
            }
        }
        return result.toString();
    }

    private static GridPane createGrid() {
        final GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.getStyleClass().add("form-grid");
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
        label.getStyleClass().add("form-label");
        GridPane.setHgrow(field, Priority.ALWAYS);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
    }

    private static Node formPage(
        final String titleText,
        final Node form,
        final Node actions
    ) {
        final Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        final VBox content = new VBox(
            14,
            title,
            new Separator(),
            form,
            actions
        );
        content.setPadding(new Insets(20));
        content.setMaxWidth(760);
        content.getStyleClass().add("form-page");

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

    private <T> void configureRemovalList(
        final ListView<T> list,
        final Consumer<T> removal,
        final Predicate<T> removable
    ) {
        list.setCellFactory(ignored -> new ListCell<>() {
            private final Label description = new Label();
            private final Region spacer = new Region();
            private final Button remove = new Button("Rimuovi");
            private final HBox content = new HBox(
                10,
                description,
                spacer,
                remove
            );

            {
                description.setWrapText(true);
                description.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(description, Priority.ALWAYS);
                HBox.setHgrow(spacer, Priority.ALWAYS);
                content.setAlignment(Pos.CENTER_LEFT);
                remove.getStyleClass().addAll(
                    "compact-button",
                    "danger-button"
                );
                remove.disableProperty().bind(busy);
                remove.setOnAction(event -> {
                    final T current = getItem();
                    if (current != null && removable.test(current)) {
                        removal.accept(current);
                    }
                });
            }

            @Override
            protected void updateItem(final T item, final boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                description.getStyleClass().removeAll(
                    "success-text",
                    "error-text"
                );
                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }
                description.setText(item.toString());
                final boolean canRemove = removable.test(item);
                remove.setVisible(canRemove);
                remove.setManaged(canRemove);
                if (item instanceof WeekendPerformanceStatus status) {
                    description.getStyleClass().add(
                        status.recorded()
                            ? "success-text"
                            : "error-text"
                    );
                }
                setGraphic(content);
            }
        });
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

    private static <T> void selectFirstIfEmpty(final ComboBox<T> combo) {
        if (combo.getValue() == null && !combo.getItems().isEmpty()) {
            combo.getSelectionModel().selectFirst();
        }
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
