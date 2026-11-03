/* =============================================================================================
EQUIPMENT FORM:
    - Pop-up form used for both adding a new record and editing an existing one.
    - Add: all fields start blank. Edit: fields start with the record's current
      values, and any field (including the ID) can be changed.
    - Status and criticality use drop-down lists, so only valid choices can be
      picked. Dates use date pickers that also accept typed MM/dd/yyyy dates.
    - When Save is clicked, every field is checked. If anything is wrong, the form
      stays open and lists every problem in red, so bad data never gets saved.
============================================================================================= */
package com.example.aetrackergui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class EquipmentForm extends Dialog<EquipmentRecord> {

    // Private
    // =========================================================================================
    private final EquipmentManager manager;
    private final InputValidator validator = new InputValidator();

    // The record's ID before editing (0 when adding)
    // Lets an edited record keep its own ID
    private final int originalId;

    private final TextField idField = new TextField();
    private final TextField nameField = new TextField();
    private final TextField typeField = new TextField();
    private final DatePicker lastPicker = new DatePicker();
    private final DatePicker nextPicker = new DatePicker();
    private final ComboBox<String> statusBox = new ComboBox<>();
    private final ComboBox<Integer> criticalityBox = new ComboBox<>();
    private final Label errorLabel = new Label();

    private final DateTimeFormatter displayFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private final ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);

    // Make a date picker show and accept MM/dd/yyyy
    private DatePicker setUpDatePicker(DatePicker picker) {
        picker.setPromptText("MM/dd/yyyy");
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return (date == null) ? "" : date.format(displayFormat);
            }

            @Override
            public LocalDate fromString(String text) {
                return validator.parseDate(text);
            }
        });
        return picker;
    }

    // Read the date from text box
    private LocalDate readDate(DatePicker picker) {
        return validator.parseDate(picker.getEditor().getText());
    }

    // Return the typed ID, or null
    private Integer parseId() {
        try {
            return Integer.parseInt(idField.getText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Put an existing records values into the form
    private boolean fillFields(EquipmentRecord record) {
        idField.setText(String.valueOf(record.getEquipmentId()));
        nameField.setText(record.getEquipmentName());
        typeField.setText(record.getEquipmentType());
        lastPicker.setValue(record.getLastInspection());
        nextPicker.setValue(record.getNextInspection());
        statusBox.setValue(record.getStatus());
        criticalityBox.setValue(record.getCriticalityLevel());
        return true;
    }

    // Check every field
    // Shows all problems at once and returns true only if there are none
    private boolean validateForm() {
        ArrayList<String> errors = new ArrayList<>();

        Integer id = parseId();
        if (id == null) {
            errors.add("ID must be a whole number.");
        } else if (!validator.isValidId(id)) {
            errors.add("ID must be a positive number.");
        } else if (id != originalId && manager.idExists(id)) {
            errors.add("ID " + id + " is already used by another record.");
        }

        if (!validator.isValidText(nameField.getText())) {
            errors.add("Name cannot be blank or longer than 50 characters.");
        }
        if (!validator.isValidText(typeField.getText())) {
            errors.add("Type cannot be blank or longer than 50 characters.");
        }

        LocalDate last = readDate(lastPicker);
        LocalDate next = readDate(nextPicker);
        if (last == null) {
            errors.add("Last inspection must be a real date (MM/dd/yyyy).");
        }
        if (next == null) {
            errors.add("Next inspection must be a real date (MM/dd/yyyy).");
        }
        if (last != null && next != null && !validator.isNextAfterLast(last, next)) {
            errors.add("Next inspection must be after the last inspection.");
        }

        if (statusBox.getValue() == null) {
            errors.add("Choose a status.");
        }
        if (criticalityBox.getValue() == null) {
            errors.add("Choose a criticality level.");
        }

        errorLabel.setText(String.join("\n", errors));
        return errors.isEmpty();
    }

    // Build the record from the form
    // Only called after validateForm() passes
    private EquipmentRecord buildRecord() {
        return new EquipmentRecord(parseId(), nameField.getText().trim(), typeField.getText().trim(),
                readDate(lastPicker), readDate(nextPicker), statusBox.getValue(), criticalityBox.getValue());
    }

    // Public
    // =========================================================================================

     // Create the form
     // Pass null for "existing" to add a new record or pass a record to edit it
    public EquipmentForm(EquipmentManager manager, EquipmentRecord existing, Stage owner) {
        this.manager = manager;
        this.originalId = (existing == null) ? 0 : existing.getEquipmentId();

        initOwner(owner);
        setTitle(existing == null ? "Add Equipment" : "Edit Equipment");
        setHeaderText(existing == null
                ? "Enter the new equipment's information."
                : "Change any field, then click Save.");

        // Drop-down lists: only valid choices can be picked (Phase 1 feedback)
        statusBox.getItems().addAll(validator.getValidStatuses());
        statusBox.setPromptText("Choose a status");
        criticalityBox.getItems().addAll(1, 2, 3);
        criticalityBox.setPromptText("1 = Low, 3 = High");

        setUpDatePicker(lastPicker);
        setUpDatePicker(nextPicker);

        idField.setPromptText("Positive whole number");
        errorLabel.setStyle("-fx-text-fill: #c62828;");
        errorLabel.setWrapText(true);

        if (existing != null) {
            fillFields(existing);
        }

        // Layout: labels on the left, inputs on the right
        // errors underneath
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));
        grid.addRow(0, new Label("Equipment ID:"), idField);
        grid.addRow(1, new Label("Name:"), nameField);
        grid.addRow(2, new Label("Type:"), typeField);
        grid.addRow(3, new Label("Last inspection:"), lastPicker);
        grid.addRow(4, new Label("Next inspection:"), nextPicker);
        grid.addRow(5, new Label("Status:"), statusBox);
        grid.addRow(6, new Label("Criticality:"), criticalityBox);
        grid.add(errorLabel, 0, 7, 2, 1);

        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // When Save is clicked, check every field first
        // If anything is wrong = consume() stops the form from closing
        Button saveButton = (Button) getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            if (!validateForm()) {
                event.consume();
            }
        });

        // Save returns the new/updated record
        // Cancel returns nothing
        setResultConverter(button -> (button == saveButtonType) ? buildRecord() : null);
    }
}
