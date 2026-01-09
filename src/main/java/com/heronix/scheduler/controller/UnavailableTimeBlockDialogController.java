package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.dto.UnavailableTimeBlock;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Controller for Add/Edit Unavailable Time Block Dialog
 * Phase 6A: Teacher Availability Constraints
 *
 * @since Phase 6A-4 - December 3, 2025
 */
public class UnavailableTimeBlockDialogController {

    @FXML private Label titleLabel;
    @FXML private ComboBox<DayOfWeek> dayComboBox;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;
    @FXML private TextField reasonField;
    @FXML private CheckBox recurringCheckBox;
    @FXML private Label validationLabel;

    private UnavailableTimeBlock block;
    private boolean confirmed = false;
    private boolean editMode = false;

    /**
     * Initialize the dialog
     */
    @FXML
    public void initialize() {
        // Populate day of week dropdown
        dayComboBox.setItems(FXCollections.observableArrayList(DayOfWeek.values()));

        // Set default selection
        if (dayComboBox.getItems().size() > 0) {
            dayComboBox.getSelectionModel().selectFirst();
        }
    }

    /**
     * Set up dialog for adding a new block
     */
    public void setupForAdd() {
        editMode = false;
        titleLabel.setText("Add Unavailable Time Block");
        block = new UnavailableTimeBlock();
        block.setRecurring(true);
        recurringCheckBox.setSelected(true);
    }

    /**
     * Set up dialog for editing an existing block
     *
     * @param existingBlock Block to edit
     */
    public void setupForEdit(UnavailableTimeBlock existingBlock) {
        editMode = true;
        titleLabel.setText("Edit Unavailable Time Block");
        this.block = existingBlock;

        // Populate fields
        dayComboBox.setValue(existingBlock.getDayOfWeek());
        startTimeField.setText(existingBlock.getStartTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        endTimeField.setText(existingBlock.getEndTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        reasonField.setText(existingBlock.getReason() != null ? existingBlock.getReason() : "");
        recurringCheckBox.setSelected(existingBlock.isRecurring());
    }

    /**
     * Handle Save button
     */
    @FXML
    private void handleSave() {
        if (validateInput()) {
            // Update block with form values
            block.setDayOfWeek(dayComboBox.getValue());
            block.setStartTime(parseTime(startTimeField.getText()));
            block.setEndTime(parseTime(endTimeField.getText()));
            block.setReason(reasonField.getText().trim().isEmpty() ? null : reasonField.getText().trim());
            block.setRecurring(recurringCheckBox.isSelected());

            confirmed = true;
            closeDialog();
        }
    }

    /**
     * Handle Cancel button
     */
    @FXML
    private void handleCancel() {
        confirmed = false;
        closeDialog();
    }

    /**
     * Validate user input
     *
     * @return true if valid, false otherwise
     */
    private boolean validateInput() {
        StringBuilder errors = new StringBuilder();

        // Validate day
        if (dayComboBox.getValue() == null) {
            errors.append("• Please select a day of week\n");
        }

        // Validate start time
        LocalTime startTime = null;
        try {
            startTime = parseTime(startTimeField.getText());
        } catch (Exception e) {
            errors.append("• Start time must be in format HH:mm (e.g., 09:00)\n");
        }

        // Validate end time
        LocalTime endTime = null;
        try {
            endTime = parseTime(endTimeField.getText());
        } catch (Exception e) {
            errors.append("• End time must be in format HH:mm (e.g., 10:00)\n");
        }

        // Validate time order
        if (startTime != null && endTime != null) {
            if (!startTime.isBefore(endTime)) {
                errors.append("• Start time must be before end time\n");
            }
        }

        // Show validation errors if any
        if (errors.length() > 0) {
            validationLabel.setText(errors.toString().trim());
            validationLabel.setVisible(true);
            validationLabel.setManaged(true);
            return false;
        }

        validationLabel.setVisible(false);
        validationLabel.setManaged(false);
        return true;
    }

    /**
     * Parse time string to LocalTime
     *
     * Supports multiple formats:
     * - HH:mm (09:00)
     * - H:mm (9:00)
     * - HH:mm:ss (09:00:00)
     *
     * @param timeStr Time string
     * @return LocalTime
     * @throws DateTimeParseException if invalid
     */
    private LocalTime parseTime(String timeStr) {
        timeStr = timeStr.trim();

        // Try HH:mm format first
        try {
            return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
        } catch (DateTimeParseException e1) {
            // Try H:mm format
            try {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:mm"));
            } catch (DateTimeParseException e2) {
                // Try HH:mm:ss format
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
            }
        }
    }

    /**
     * Get the result block (only valid if confirmed)
     *
     * @return UnavailableTimeBlock or null if cancelled
     */
    public UnavailableTimeBlock getResult() {
        return confirmed ? block : null;
    }

    /**
     * Check if dialog was confirmed
     *
     * @return true if Save was clicked, false if Cancel
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Close the dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) titleLabel.getScene().getWindow();
        stage.close();
    }
}
