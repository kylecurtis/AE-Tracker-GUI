/* =============================================================================================
MAIN WINDOW:
    - The main screen of the GUI (replaces the Phase 1 command line Menu).
    - Top: title, file loader (type a path or browse), and search bar.
    - Center: table showing every record, including its priority score and level.
    - Bottom: buttons for Add, Edit, Delete, Calculate Priority, and Exit, plus a
      status line that shows the result of the last action.
    - Every button runs through runSafely(), so an unexpected error shows a
      message instead of crashing the program.
    - Button actions return true/false. Only the JavaFX event lambdas are void,
      because JavaFX requires it.
============================================================================================= */
package com.example.aetrackergui;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public class MainWindow {

    // Private
    // =========================================================================================
    private final EquipmentManager manager;
    private final Stage stage;

    // The table displays whatever is in tableData
    // (refreshTable() keeps it up to date)
    private final ObservableList<EquipmentRecord> tableData = FXCollections.observableArrayList();
    private final TableView<EquipmentRecord> table = new TableView<>(tableData);

    private final TextField filePathField = new TextField();
    private final TextField searchField = new TextField();
    private final Label statusLabel = new Label("Load a file or add equipment to get started.");
    private final DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    // Helpers
    // =========================================================================================

    // Run a buttons action and catch anything unexpected (so the program never crashes)
    private boolean runSafely(BooleanSupplier action) {
        try {
            return action.getAsBoolean();
        } catch (Exception e) {
            return showError("Something went wrong",
                    "The action could not be completed.\n" + e.getMessage());
        }
    }

    // Create a button that runs the given action safely when clicked
    private Button makeButton(String text, BooleanSupplier action) {
        Button button = new Button(text);
        button.setOnAction(event -> runSafely(action));
        return button;
    }

    // Show an error pop-up
    // Return false for actions
    private boolean showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(stage);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(message);
        // prevent messages from being cut off
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
        return false;
    }

    // Show an information pop-up
    private boolean showInfo(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(stage);
        alert.setTitle("Information");
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
        alert.showAndWait();
        return true;
    }

    // Show an OK/Cancel pop-up
    // Returns true only if the user clicks OK
    private boolean confirm(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(stage);
        alert.setTitle("Please Confirm");
        alert.setHeaderText(header);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // Return the selected row, or shows an error and returns null if nothing is selected
    private EquipmentRecord getSelectedRecord(String action) {
        EquipmentRecord selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No record selected", "Click a record in the table first, then " + action + ".");
        }
        return selected;
    }

    // True if the record matches the search text (exact ID, or part of the name, type, or status)
    private boolean matchesSearch(EquipmentRecord record, String text) {
        if (text.isEmpty()) {
            return true;
        }
        String lower = text.toLowerCase();
        return String.valueOf(record.getEquipmentId()).equals(text)
                || record.getEquipmentName().toLowerCase().contains(lower)
                || record.getEquipmentType().toLowerCase().contains(lower)
                || record.getStatus().toLowerCase().contains(lower);
    }

    // Reload the table from the manager (keeping the current search)
    // Return how many rows are shown
    private int refreshTable() {
        String search = searchField.getText().trim();
        ArrayList<EquipmentRecord> shown = new ArrayList<>();
        for (EquipmentRecord record : manager.getAllRecords()) {
            if (matchesSearch(record, search)) {
                shown.add(record);
            }
        }
        tableData.setAll(shown);
        return shown.size();
    }

    // Priority score rounded to 1 decimal place for the table
    private double roundedPriority(EquipmentRecord record) {
        return Math.round(manager.calculatePriority(record.getEquipmentId()) * 10) / 10.0;
    }

    // Layout
    // =========================================================================================

    // Build the table columns
    // Each column pulls one value from an EquipmentRecord
    private TableView<EquipmentRecord> buildTable() {
        TableColumn<EquipmentRecord, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(row -> new SimpleIntegerProperty(
                row.getValue().getEquipmentId()));

        TableColumn<EquipmentRecord, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getEquipmentName()));

        TableColumn<EquipmentRecord, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getEquipmentType()));

        TableColumn<EquipmentRecord, String> lastCol = new TableColumn<>("Last Inspection");
        lastCol.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getLastInspection().format(displayFormat)));

        TableColumn<EquipmentRecord, String> nextCol = new TableColumn<>("Next Inspection");
        nextCol.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getNextInspection().format(displayFormat)));

        TableColumn<EquipmentRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(row -> new SimpleStringProperty(
                row.getValue().getStatus()));

        TableColumn<EquipmentRecord, Number> critCol = new TableColumn<>("Criticality");
        critCol.setCellValueFactory(row -> new SimpleIntegerProperty(
                row.getValue().getCriticalityLevel()));

        // Custom action shown right in the table, so it's visible for every record
        TableColumn<EquipmentRecord, Number> scoreCol = new TableColumn<>("Priority Score");
        scoreCol.setCellValueFactory(row -> new SimpleDoubleProperty(
                roundedPriority(row.getValue())));

        TableColumn<EquipmentRecord, String> levelCol = new TableColumn<>("Priority Level");
        levelCol.setCellValueFactory(row -> new SimpleStringProperty(
                manager.getPriorityLevel(manager.calculatePriority(row.getValue().getEquipmentId()))));

        // Add cols
        table.getColumns().add(idCol);
        table.getColumns().add(nameCol);
        table.getColumns().add(typeCol);
        table.getColumns().add(lastCol);
        table.getColumns().add(nextCol);
        table.getColumns().add(statusCol);
        table.getColumns().add(critCol);
        table.getColumns().add(scoreCol);
        table.getColumns().add(levelCol);

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No records to display. Load a file or add equipment."));
        return table;
    }

    // Button actions (each returns true if it completed)
    // =========================================================================================

    // READ: load records from the file path typed in the text box
    private boolean loadFile() {
        // Remove quotes added by Windows "Copy as path"
        String path = filePathField.getText().replace("\"", "").trim();

        int added;
        try {
            added = manager.loadFromFile(path);
        } catch (IOException e) {
            // Blank names, missing files, folders, and unreadable files all end up here
            return showError("Could not load file", e.getMessage());
        }

        searchField.clear();
        refreshTable();
        String message = "Loaded " + added + " record(s) from " + path + ".";
        statusLabel.setText(message);

        ArrayList<String> errors = manager.getLoadErrors();
        if (errors.isEmpty()) {
            return showInfo("File loaded", message);
        }
        return showInfo("File loaded with skipped lines",
                message + "\n\nSkipped " + errors.size() + " line(s):\n" + String.join("\n", errors));
    }

    // READ: pick a file with a file browser instead of typing the path, then load it
    private boolean browseForFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose an equipment data file");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files (*.txt)", "*.txt"));

        File startFolder = new File(System.getProperty("user.dir"));
        if (startFolder.isDirectory()) {
            chooser.setInitialDirectory(startFolder);
        }

        File chosen = chooser.showOpenDialog(stage);
        if (chosen == null) {
            return false;   // user closed the file browser without picking a file
        }
        filePathField.setText(chosen.getAbsolutePath());
        return loadFile();
    }

    // Show only records that match the search
    private boolean search() {
        int shown = refreshTable();
        statusLabel.setText(shown + " record(s) match \"" + searchField.getText().trim() + "\".");
        return true;
    }

    // Clear the search and show every record
    private boolean showAll() {
        searchField.clear();
        int shown = refreshTable();
        statusLabel.setText("Showing all " + shown + " record(s).");
        return true;
    }

    // CREATE: open a blank form and add the new record
    private boolean addEquipment() {
        EquipmentForm form = new EquipmentForm(manager, null, stage);
        Optional<EquipmentRecord> result = form.showAndWait();
        if (result.isEmpty()) {
            statusLabel.setText("Add cancelled.");
            return false;
        }

        EquipmentRecord record = result.get();
        if (!manager.addRecord(record)) {
            return showError("Could not add record", "Check that the ID isn't already used and every field is valid.");
        }

        searchField.clear();
        refreshTable();
        table.getSelectionModel().select(record);
        table.scrollTo(record);
        statusLabel.setText("Added: " + record);
        return true;
    }

    // UPDATE: open the form filled in with the selected record (any field can be changed)
    private boolean editEquipment() {
        EquipmentRecord selected = getSelectedRecord("click Edit Selected");
        if (selected == null) {
            return false;
        }

        EquipmentForm form = new EquipmentForm(manager, selected, stage);
        Optional<EquipmentRecord> result = form.showAndWait();
        if (result.isEmpty()) {
            statusLabel.setText("Edit cancelled.");
            return false;
        }

        EquipmentRecord updated = result.get();
        if (!manager.updateRecord(selected.getEquipmentId(), updated)) {
            return showError("Could not update record",
                    "Check that the ID isn't already used and every field is valid.");
        }

        refreshTable();
        table.getSelectionModel().select(updated);
        statusLabel.setText("Updated: " + updated);
        return true;
    }

    // DELETE: remove the selected record after the user confirms
    private boolean deleteEquipment() {
        EquipmentRecord selected = getSelectedRecord("click Delete Selected");
        if (selected == null) {
            return false;
        }

        if (!confirm("Delete equipment?", "Delete " + selected.getEquipmentName()
                + " (ID " + selected.getEquipmentId() + ")? This cannot be undone.")) {
            statusLabel.setText("Delete cancelled. Record was kept.");
            return false;
        }

        if (!manager.deleteRecord(selected.getEquipmentId())) {
            return showError("Could not delete record", "That record no longer exists.");
        }

        refreshTable();
        statusLabel.setText("Deleted ID " + selected.getEquipmentId() + ". "
                + manager.getRecordCount() + " record(s) remaining.");
        return true;
    }

    // CUSTOM ACTION: show the priority score for the selected record, step by step
    private boolean calculatePriority() {
        EquipmentRecord selected = getSelectedRecord("click Calculate Priority");
        if (selected == null) {
            return false;
        }

        double score = manager.calculatePriority(selected.getEquipmentId());
        String level = manager.getPriorityLevel(score);

        // Same numbers EquipmentManager uses, shown so the user can see how the score was made
        long daysBetween = ChronoUnit.DAYS.between(selected.getLastInspection(), selected.getNextInspection());
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), selected.getNextInspection());
        double percentLeft = (double) daysLeft / daysBetween * 100;
        int criticality = selected.getCriticalityLevel();

        String details = String.format(
                "Days between inspections: %d%n"
                        + "Days until next inspection: %d%n"
                        + "Percent of window left: %.1f%%%n"
                        + "Criticality: %d%n%n"
                        + "Score = %.1f / %d = %.1f%n"
                        + "Priority level: %s%n%n"
                        + "(Lower score = more urgent. Below 0 = overdue.)",
                daysBetween, daysLeft, percentLeft, criticality,
                percentLeft, criticality, score, level);

        statusLabel.setText(selected.getEquipmentName() + " priority: "
                + String.format("%.1f", score) + " (" + level + ")");
        return showInfo("Priority for " + selected.getEquipmentName()
                + " (ID " + selected.getEquipmentId() + ")", details);
    }

    // Ask before closing
    // (Used by the exit button AND the X button)
    private boolean confirmExit() {
        return confirm("Exit program?", "Are you sure you want to exit? "
                + "Records added or changed in this session are not saved to a file.");
    }

    private boolean exitApplication() {
        if (!confirmExit()) {
            return false;
        }
        Platform.exit();
        return true;
    }

    // Public
    // =========================================================================================
    public MainWindow(EquipmentManager manager, Stage stage) {
        this.manager = manager;
        this.stage = stage;
    }

    // Builds the whole window: top (title, file, search), center (table), bottom (buttons, status)
    public Scene buildScene() {
        Label title = new Label("Aerospace Equipment Maintenance Tracker");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");

        // File row: type a path and press Enter / Load File, or use Browse
        filePathField.setPromptText("File name or path (example: sample_equipment.txt)");
        filePathField.setOnAction(event -> runSafely(this::loadFile));
        HBox.setHgrow(filePathField, Priority.ALWAYS);
        HBox fileRow = new HBox(8, new Label("Data file:"), filePathField,
                makeButton("Browse...", this::browseForFile),
                makeButton("Load File", this::loadFile));
        fileRow.setAlignment(Pos.CENTER_LEFT);

        // Search row: press Enter or Search to filter, Show All to clear
        searchField.setPromptText("Search by ID, name, type, or status");
        searchField.setOnAction(event -> runSafely(this::search));
        HBox.setHgrow(searchField, Priority.ALWAYS);
        HBox searchRow = new HBox(8, new Label("Search:"), searchField,
                makeButton("Search", this::search),
                makeButton("Show All", this::showAll));
        searchRow.setAlignment(Pos.CENTER_LEFT);

        VBox top = new VBox(10, title, fileRow, searchRow);

        // Action buttons, with Exit pushed to the far right by the spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actionRow = new HBox(8,
                makeButton("Add Equipment", this::addEquipment),
                makeButton("Edit Selected", this::editEquipment),
                makeButton("Delete Selected", this::deleteEquipment),
                makeButton("Calculate Priority", this::calculatePriority),
                spacer,
                makeButton("Exit", this::exitApplication));
        actionRow.setAlignment(Pos.CENTER_LEFT);

        VBox bottom = new VBox(8, actionRow, statusLabel);

        TableView<EquipmentRecord> tableView = buildTable();
        BorderPane.setMargin(tableView, new Insets(10, 0, 10, 0));

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));
        root.setTop(top);
        root.setCenter(tableView);
        root.setBottom(bottom);

        // The X button asks for confirmation too
        stage.setOnCloseRequest(event -> {
            if (!confirmExit()) {
                event.consume();   // cancels the close
            }
        });

        refreshTable();

        // default: 1200, 600
        return new Scene(root, 1200, 600);
    }
}
