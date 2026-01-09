package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.dto.UnavailableTimeBlock;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.heronix.scheduler.service.data.SISDataService;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controller for Teacher Availability Dialog
 * Phase 6A: Teacher Availability Constraints
 *
 * Allows administrators to manage when teachers are unavailable (meetings, appointments, etc.)
 *
 * @since Phase 6A-4 - December 3, 2025
 */
@Component
public class TeacherAvailabilityDialogController {

    @Autowired
    private SISDataService sisDataService;
    @FXML private Label teacherNameLabel;
    @FXML private TableView<UnavailableTimeBlock> blocksTable;
    @FXML private TableColumn<UnavailableTimeBlock, String> dayColumn;
    @FXML private TableColumn<UnavailableTimeBlock, String> startTimeColumn;
    @FXML private TableColumn<UnavailableTimeBlock, String> endTimeColumn;
    @FXML private TableColumn<UnavailableTimeBlock, String> reasonColumn;
    @FXML private TableColumn<UnavailableTimeBlock, String> recurringColumn;
    @FXML private Label blockCountLabel;

    private Teacher teacher;
    private ObservableList<UnavailableTimeBlock> blocks;
    private boolean changesMade = false;

    /**
     * Initialize the dialog
     */
    @FXML
    public void initialize() {
        // Set up table columns
        dayColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDayOfWeek().toString()));

        startTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"))));

        endTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getEndTime().format(DateTimeFormatter.ofPattern("HH:mm"))));

        reasonColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getReason() != null ? cellData.getValue().getReason() : ""));

        recurringColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().isRecurring() ? "Yes" : "No"));

        // Enable multiple selection for batch delete
        blocksTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    /**
     * Set the teacher for this dialog
     *
     * @param teacher Teacher to manage availability for
     */
    public void setTeacher(Teacher teacher) {
        this.teacher = teacher;
        teacherNameLabel.setText(teacher.getName());

        // Load existing blocks
        // Note: Teacher.unavailableTimeBlocks is List<String>, not List<UnavailableTimeBlock>
        // This would need conversion from String format to UnavailableTimeBlock objects
        // For now, start with empty list
        blocks = FXCollections.observableArrayList(new ArrayList<>());

        // TODO: Parse teacher.getUnavailableTimeBlocks() strings into UnavailableTimeBlock objects
        // Expected format: "MONDAY,09:00,10:00,Meeting,true"

        blocksTable.setItems(blocks);

        updateBlockCount();
    }

    /**
     * Handle Add Block button
     */
    @FXML
    private void handleAddBlock() {
        try {
            // Load the block dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/heronix/scheduler/view/UnavailableTimeBlockDialog.fxml"));
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add Unavailable Time Block");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(teacherNameLabel.getScene().getWindow());
            dialogStage.setScene(new Scene(loader.load()));

            // Set up controller
            UnavailableTimeBlockDialogController controller = loader.getController();
            controller.setupForAdd();

            // Show and wait
            dialogStage.showAndWait();

            // If confirmed, add the block
            if (controller.isConfirmed()) {
                UnavailableTimeBlock newBlock = controller.getResult();

                // Check for overlaps
                if (hasOverlap(newBlock, -1)) {
                    showError("Overlap Detected", "This time block overlaps with an existing unavailable period.");
                    return;
                }

                blocks.add(newBlock);
                changesMade = true;
                updateBlockCount();
            }
        } catch (Exception e) {
            showError("Error", "Failed to open add block dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle Edit Block button
     */
    @FXML
    private void handleEditBlock() {
        UnavailableTimeBlock selectedBlock = blocksTable.getSelectionModel().getSelectedItem();
        if (selectedBlock == null) {
            showWarning("No Selection", "Please select a time block to edit.");
            return;
        }

        try {
            // Load the block dialog
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/heronix/scheduler/view/UnavailableTimeBlockDialog.fxml"));
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Edit Unavailable Time Block");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(teacherNameLabel.getScene().getWindow());
            dialogStage.setScene(new Scene(loader.load()));

            // Set up controller
            UnavailableTimeBlockDialogController controller = loader.getController();
            controller.setupForEdit(selectedBlock);

            // Show and wait
            dialogStage.showAndWait();

            // If confirmed, update the block
            if (controller.isConfirmed()) {
                UnavailableTimeBlock updatedBlock = controller.getResult();

                // Check for overlaps (excluding current block)
                int currentIndex = blocks.indexOf(selectedBlock);
                if (hasOverlap(updatedBlock, currentIndex)) {
                    showError("Overlap Detected", "This time block overlaps with another unavailable period.");
                    return;
                }

                // Update the block in place
                blocks.set(currentIndex, updatedBlock);
                blocksTable.refresh();
                changesMade = true;
            }
        } catch (Exception e) {
            showError("Error", "Failed to open edit block dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle Remove Block button
     */
    @FXML
    private void handleRemoveBlock() {
        List<UnavailableTimeBlock> selectedBlocks = new ArrayList<>(blocksTable.getSelectionModel().getSelectedItems());

        if (selectedBlocks.isEmpty()) {
            showWarning("No Selection", "Please select one or more time blocks to remove.");
            return;
        }

        // Confirm deletion
        String message = selectedBlocks.size() == 1
                ? "Are you sure you want to remove this unavailable time block?"
                : "Are you sure you want to remove " + selectedBlocks.size() + " unavailable time blocks?";

        Optional<ButtonType> result = showConfirmation("Confirm Removal", message);

        if (result.isPresent() && result.get() == ButtonType.OK) {
            blocks.removeAll(selectedBlocks);
            changesMade = true;
            updateBlockCount();
        }
    }

    /**
     * Handle Save button
     */
    @FXML
    private void handleSave() {
        try {
            // Note: Teacher availability blocks cannot be saved in SchedulerV2
            // This data must be managed through SIS API
            // For now, we show a warning to the user

            showWarning("Not Supported",
                    "Teacher availability must be managed through the main SIS system. " +
                    "These changes cannot be saved directly from SchedulerV2.");

            // TODO: Implement SIS API call to update teacher availability
            // sisApiClient.updateTeacherAvailability(teacher.getId(), blocks);

            // Close dialog
            closeDialog();

        } catch (Exception e) {
            showError("Save Failed", "Failed to save teacher availability: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle Cancel button
     */
    @FXML
    private void handleCancel() {
        if (changesMade) {
            Optional<ButtonType> result = showConfirmation("Unsaved Changes",
                    "You have unsaved changes. Are you sure you want to cancel?");

            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        closeDialog();
    }

    /**
     * Check if a block overlaps with existing blocks
     *
     * @param block Block to check
     * @param excludeIndex Index to exclude from check (-1 for none)
     * @return true if overlap detected
     */
    private boolean hasOverlap(UnavailableTimeBlock block, int excludeIndex) {
        for (int i = 0; i < blocks.size(); i++) {
            if (i == excludeIndex) continue;

            UnavailableTimeBlock existing = blocks.get(i);
            if (existing.overlaps(block)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Update block count label
     */
    private void updateBlockCount() {
        int count = blocks.size();
        blockCountLabel.setText(count + (count == 1 ? " block defined" : " blocks defined"));
    }

    /**
     * Close the dialog
     */
    private void closeDialog() {
        Stage stage = (Stage) teacherNameLabel.getScene().getWindow();
        stage.close();
    }

    /**
     * Show error alert
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show warning alert
     */
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show confirmation dialog
     */
    private Optional<ButtonType> showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        return alert.showAndWait();
    }
}
