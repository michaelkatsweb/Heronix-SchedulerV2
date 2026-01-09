package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.dto.ScheduleRequest;
import com.heronix.scheduler.model.enums.SchedulePeriod;
import com.heronix.scheduler.model.enums.ScheduleType;
import com.heronix.scheduler.service.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Schedule Generation Dialog Controller - Enhanced with Resource Display
 *
 * Features:
 * - Multi-step wizard for schedule generation
 * - Resource availability display
 * - AI-powered scheduling options
 * - Responsive design (fits all screen sizes)
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/ScheduleGenerationDialogController.java
 *
 * @version 3.0.0 - Added responsive design support
 * @since 2025-11-04
 */
@Slf4j
@Component
public class ScheduleGenerationDialogController extends BaseDialogController {

    // Configuration Controls (Left Column)
    @FXML
    private TextField scheduleNameField;
    @FXML
    private ComboBox<ScheduleType> scheduleTypeCombo;
    @FXML
    private ComboBox<SchedulePeriod> periodCombo;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;
    @FXML
    private CheckBox useAICheckbox;
    @FXML
    private CheckBox applyLeanCheckbox;
    @FXML
    private CheckBox applyKanbanCheckbox;
    @FXML
    private Spinner<Integer> maxConsecutiveSpinner;
    @FXML
    private Spinner<Integer> breakDurationSpinner;

    // Resource Labels (Center Column)
    @FXML
    private Label teacherCountLabel;
    @FXML
    private Label courseCountLabel;
    @FXML
    private Label roomCountLabel;
    @FXML
    private Label studentCountLabel;
    @FXML
    private Label timeSlotCountLabel;
    @FXML
    private Label resourceStatusLabel;

    // Progress Controls (Right Column)
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressPercentLabel;
    @FXML
    private Label progressStatusLabel;
    @FXML
    private Label currentPhaseLabel;
    @FXML
    private TextArea generationLogArea;
    @FXML
    private VBox statisticsBox;
    @FXML
    private Label statisticsLabel;
    @FXML
    private Button generateButton;

    // Service for loading resource counts
    @Autowired
    private com.heronix.scheduler.service.data.SISDataService sisDataService;
    @Autowired
    private com.heronix.scheduler.repository.RoomRepository roomRepository;

    private ScheduleRequest scheduleRequest;
    private boolean confirmed = false;

    @FXML
    public void initialize() {
        log.info("Initializing Schedule Generation Dialog");

        // Schedule types
        scheduleTypeCombo.setItems(FXCollections.observableArrayList(ScheduleType.values()));
        scheduleTypeCombo.setValue(ScheduleType.TRADITIONAL);

        // Periods
        periodCombo.setItems(FXCollections.observableArrayList(SchedulePeriod.values()));
        periodCombo.setValue(SchedulePeriod.MASTER);

        // Default dates
        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusMonths(4));

        // Spinners
        maxConsecutiveSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 3));
        breakDurationSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 60, 15, 5));

        // Default schedule name
        scheduleNameField.setText("Schedule " + LocalDate.now());

        // Load resource counts asynchronously
        loadResourceCounts();
    }

    /**
     * Load and display resource counts in the center column
     */
    private void loadResourceCounts() {
        // Run in background thread to avoid blocking UI
        new Thread(() -> {
            try {
                log.info("Loading resource counts...");

                // Load counts from database
                long teacherCount = sisDataService.getAllTeachers().stream()
                        .filter(t -> Boolean.TRUE.equals(t.getActive()))
                        .count();
                long courseCount = sisDataService.getAllCourses().size();
                long roomCount = roomRepository.count();
                long studentCount = sisDataService.getAllStudents().size();

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    teacherCountLabel.setText(String.valueOf(teacherCount));
                    courseCountLabel.setText(String.valueOf(courseCount));
                    roomCountLabel.setText(String.valueOf(roomCount));
                    studentCountLabel.setText(String.valueOf(studentCount));

                    // Update status message
                    if (teacherCount > 0 && courseCount > 0 && roomCount > 0) {
                        resourceStatusLabel.setText("✓ All resources loaded and ready for scheduling");
                        resourceStatusLabel.setStyle("-fx-text-fill: -fx-accent-success;");
                    } else {
                        resourceStatusLabel.setText("⚠ Warning: Some resources are missing. Please add data before generating.");
                        resourceStatusLabel.setStyle("-fx-text-fill: -fx-accent-warning;");
                    }

                    log.info("Resource counts loaded: {} teachers, {} courses, {} rooms, {} students",
                        teacherCount, courseCount, roomCount, studentCount);
                });

            } catch (Exception e) {
                log.error("Failed to load resource counts", e);
                Platform.runLater(() -> {
                    teacherCountLabel.setText("Error");
                    courseCountLabel.setText("Error");
                    roomCountLabel.setText("Error");
                    studentCountLabel.setText("Error");
                    resourceStatusLabel.setText("⚠ Failed to load resource data");
                    resourceStatusLabel.setStyle("-fx-text-fill: -fx-accent-danger;");
                });
            }
        }).start();
    }

    @FXML
    private void handleGenerate() {
        if (validateInput()) {
            buildScheduleRequest();
            confirmed = true;
            closeDialog();
        }
    }

    @FXML
    @Override
    protected void handleCancel() {
        confirmed = false;
        closeDialog();
    }

    private boolean validateInput() {
        if (scheduleNameField.getText().trim().isEmpty()) {
            showError("Schedule name is required");
            return false;
        }

        if (startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
            showError("Start and end dates are required");
            return false;
        }

        if (startDatePicker.getValue().isAfter(endDatePicker.getValue())) {
            showError("Start date must be before end date");
            return false;
        }

        return true;
    }

    private void buildScheduleRequest() {
        scheduleRequest = new ScheduleRequest();
        scheduleRequest.setScheduleName(scheduleNameField.getText().trim());
        scheduleRequest.setScheduleType(scheduleTypeCombo.getValue());
        scheduleRequest.setPeriod(periodCombo.getValue());
        scheduleRequest.setStartDate(startDatePicker.getValue());
        scheduleRequest.setEndDate(endDatePicker.getValue());
        scheduleRequest.setUseAIOptimization(useAICheckbox.isSelected());
        scheduleRequest.setApplyLeanPrinciples(applyLeanCheckbox.isSelected());
        scheduleRequest.setApplyKanban(applyKanbanCheckbox.isSelected());
        scheduleRequest.setMaxConsecutiveHours(maxConsecutiveSpinner.getValue());
        scheduleRequest.setBreakDurationMinutes(breakDurationSpinner.getValue());
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Validation Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        } else {
            // Fallback if dialogStage not set
            Stage stage = (Stage) scheduleNameField.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * Configure responsive dialog size
     */
    @Override
    protected void configureResponsiveSize() {
        // ScheduleGeneration is a wizard with multiple steps - use large dialog (70% x 80%)
        configureLargeDialogSize();
    }

    public ScheduleRequest getScheduleRequest() {
        return scheduleRequest;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}