package it.unibo.fantasyf1;

import it.unibo.fantasyf1.model.Edizione;
import it.unibo.fantasyf1.model.LegaDisponibile;
import it.unibo.fantasyf1.model.dao.EdizioneDao;
import it.unibo.fantasyf1.model.dao.LegaDao;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public final class FantasyF1Application extends Application {

    private final EdizioneDao edizioneDao = new EdizioneDao();
    private final LegaDao legaDao = new LegaDao();

    private final ComboBox<Edizione> edizioneComboBox = new ComboBox<>();
    private final TableView<LegaDisponibile> legheTable = new TableView<>();
    private final Label statusLabel = new Label();
    private boolean updatingEditions;

    @Override
    public void start(final Stage stage) {
        final BorderPane root = new BorderPane();
        root.setPadding(new Insets(24));
        root.setTop(createHeader());
        root.setCenter(createLeagueTable());
        root.setBottom(statusLabel);

        BorderPane.setMargin(legheTable, new Insets(20, 0, 16, 0));

        final Scene scene = new Scene(root, 1000, 700);

        stage.setTitle("Fantasy Formula 1");
        stage.setScene(scene);
        stage.show();

        loadEditions();
    }

    private VBox createHeader() {
        final Label title = new Label("Fantasy Formula 1");
        title.setStyle("-fx-font-size: 26px; -fx-font-weight: bold;");

        final Label description = new Label(
            "Seleziona un'edizione per consultare le leghe disponibili."
        );

        edizioneComboBox.setPromptText("Edizione");
        edizioneComboBox.setPrefWidth(240);
        edizioneComboBox.valueProperty().addListener(
            (observable, previous, selected) -> {
                if (!updatingEditions) {
                    loadLeagues(selected);
                }
            }
        );

        final Button refreshButton = new Button("Aggiorna");
        refreshButton.setOnAction(event -> loadEditions());

        final HBox filters = new HBox(10, edizioneComboBox, refreshButton);
        filters.setPadding(new Insets(14, 0, 0, 0));

        return new VBox(5, title, description, filters);
    }

    private TableView<LegaDisponibile> createLeagueTable() {
        final TableColumn<LegaDisponibile, String> nameColumn =
            new TableColumn<>("Lega");
        nameColumn.setCellValueFactory(
            row -> new ReadOnlyStringWrapper(row.getValue().nome())
        );
        nameColumn.setPrefWidth(480);

        final TableColumn<LegaDisponibile, String> administratorColumn =
            new TableColumn<>("Amministratore");
        administratorColumn.setCellValueFactory(
            row -> new ReadOnlyStringWrapper(
                row.getValue().usernameAmministratore()
            )
        );
        administratorColumn.setPrefWidth(420);

        legheTable.getColumns().add(nameColumn);
        legheTable.getColumns().add(administratorColumn);
        legheTable.setPlaceholder(new Label("Nessuna lega disponibile"));
        return legheTable;
    }

    private void loadEditions() {
        final Edizione previousSelection = edizioneComboBox.getValue();

        try {
            final List<Edizione> editions = edizioneDao.findAll();
            updatingEditions = true;
            try {
                edizioneComboBox.setItems(
                    FXCollections.observableArrayList(editions)
                );

                if (editions.isEmpty()) {
                    legheTable.getItems().clear();
                    setStatus(
                        "Il database non contiene ancora edizioni.",
                        false
                    );
                    return;
                }

                editions.stream()
                    .filter(edition -> edition.equals(previousSelection))
                    .findFirst()
                    .ifPresentOrElse(
                        edizioneComboBox::setValue,
                        () -> edizioneComboBox.getSelectionModel().selectFirst()
                    );
            } finally {
                updatingEditions = false;
            }

            loadLeagues(edizioneComboBox.getValue());
        } catch (SQLException exception) {
            showDatabaseError("caricare le edizioni", exception);
        }
    }

    private void loadLeagues(final Edizione edition) {
        if (edition == null) {
            return;
        }

        try {
            final List<LegaDisponibile> leagues =
                legaDao.findByEdition(edition.id());
            legheTable.setItems(FXCollections.observableArrayList(leagues));
            setStatus(
                "Edizione %d: %d leghe disponibili."
                    .formatted(edition.anno(), leagues.size()),
                false
            );
        } catch (SQLException exception) {
            showDatabaseError("caricare le leghe", exception);
        }
    }

    private void showDatabaseError(
        final String operation,
        final SQLException exception
    ) {
        legheTable.getItems().clear();
        setStatus(
            "Impossibile %s. Verifica MySQL e la configurazione in DATABASE.md."
                .formatted(operation),
            true
        );
        System.err.printf("Errore JDBC durante l'operazione '%s': %s%n",
            operation, exception.getMessage());
    }

    private void setStatus(final String message, final boolean error) {
        statusLabel.setText(message);
        statusLabel.setStyle(error ? "-fx-text-fill: #b00020;" : "");
    }
}
