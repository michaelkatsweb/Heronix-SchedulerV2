package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.ScheduleStatus;
import com.heronix.scheduler.model.enums.ScheduleType;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.*;
import com.heronix.scheduler.service.data.SISDataService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import com.heronix.scheduler.model.dto.ScheduleGenerationRequest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Schedule Generator Controller - AI-Powered Schedule Generation
 * Location:
 * src/main/java/com/eduscheduler/ui/controller/ScheduleGeneratorController.java
 * 
 * Provides intelligent, automated schedule generation using OptaPlanner AI
 * solver:
 * - Configure schedule parameters (type, dates, constraints)
 * - Validate resources (teachers, courses, rooms)
 * - Generate optimized schedules using AI
 * - Display real-time generation progress
 * - Show optimization results and statistics
 * - Save and publish generated schedules
 * 
 * Features:
 * - Multiple schedule types (Traditional, Block, Rotating, etc.)
 * - Constraint configuration (lunch, breaks, max hours)
 * - Real-time progress tracking
 * - Optimization score calculation
 * - Conflict detection and resolution
 * - Teacher workload balancing
 * - Room utilization optimization
 * - Quick Setup for instant configuration
 * 
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-10-10
 */
@Slf4j
@Controller
public class ScheduleGeneratorController {

    // ========================================================================
    // DEPENDENCIES - SERVICES & REPOSITORIES
    // ========================================================================

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private ScheduleGenerationService scheduleGenerationService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private ScheduleSlotService scheduleSlotService;

    @Autowired
    private RoomService roomService;

    @Autowired
    private ScheduleDiagnosticDialogController diagnosticDialogController;

    @Autowired
    private ScheduleViolationAnalyzer violationAnalyzer;

    @Autowired
    private com.heronix.scheduler.ui.dialog.FixViolationsDialog fixViolationsDialog;

    // ========================================================================
    // FXML INJECTED COMPONENTS - CONFIGURATION PANEL
    // ========================================================================

    @FXML
    private TextField scheduleNameField;
    @FXML
    private ComboBox<ScheduleType> scheduleTypeComboBox;
    @FXML
    private DatePicker startDatePicker;
    @FXML
    private DatePicker endDatePicker;

    @FXML
    private Spinner<Integer> startHourSpinner;
    @FXML
    private Spinner<Integer> endHourSpinner;
    @FXML
    private Spinner<Integer> periodDurationSpinner;
    @FXML
    private Spinner<Integer> lunchDurationSpinner;
    @FXML
    private Spinner<Integer> passingPeriodSpinner;  // NEW - Phase 2

    // REMOVED: enableLunchCheckBox - lunch is now mandatory
    // @FXML
    // private CheckBox enableLunchCheckBox;
    @FXML
    private Spinner<Integer> lunchStartHourSpinner;
    @FXML
    private Spinner<Integer> maxConsecutiveHoursSpinner;
    @FXML
    private Spinner<Integer> maxDailyHoursSpinner;
    // NEW: Minute-precise time fields
    @FXML
    private TextField firstPeriodStartTimeField;
    @FXML
    private TextField schoolEndTimeField;
    @FXML
    private TextField lunchStartTimeField;
    @FXML
    private TextField schoolStartTimeField;  // Phase 3 - Breakfast period

    @FXML
    private Slider optimizationTimeSlider;
    @FXML
    private Label optimizationTimeLabel;

    @FXML
    private Button quickSetupButton;

    // ========================================================================
    // FXML INJECTED COMPONENTS - RESOURCE OVERVIEW
    // ========================================================================

    @FXML
    private Label teacherCountLabel;
    @FXML
    private Label courseCountLabel;
    @FXML
    private Label roomCountLabel;
    @FXML
    private Label studentCountLabel;
    @FXML
    private VBox validationMessagesBox;
    @FXML
    private Button refreshResourcesButton;

    // ========================================================================
    // FXML INJECTED COMPONENTS - GENERATION PANEL
    // ========================================================================

    @FXML
    private Button generateButton;
    @FXML
    private Button cancelButton;
    @FXML
    private ProgressBar generationProgressBar;
    @FXML
    private Label generationStatusLabel;
    @FXML
    private TextArea generationLogArea;

    // ========================================================================
    // FXML INJECTED COMPONENTS - RESULTS PANEL
    // ========================================================================

    @FXML
    private VBox resultsPanel;
    @FXML
    private Label optimizationScoreLabel;
    @FXML
    private Label conflictCountLabel;
    @FXML
    private Label assignmentRateLabel;
    @FXML
    private Label teacherUtilizationLabel;
    @FXML
    private Label roomUtilizationLabel;
    @FXML
    private TextArea resultsSummaryArea;

    @FXML
    private Button saveDraftButton;
    @FXML
    private Button publishButton;
    @FXML
    private Button reOptimizeButton;
    @FXML
    private Button viewScheduleButton;

    // ========================================================================
    // STATE VARIABLES
    // ========================================================================

    private Schedule generatedSchedule;
    private Task<Schedule> generationTask;
    private long resourceTeacherCount;
    private long resourceCourseCount;
    private long resourceRoomCount;
    private long resourceStudentCount;
    private boolean isInitialized = false;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    /**
     * Initialize the Schedule Generator controller with null-safe checks
     */
    @FXML
    public void initialize() {
        log.info("====================================");
        log.info(" INITIALIZING SCHEDULE GENERATOR CONTROLLER   ║");
        log.info("====================================");

        try {
            // Initialize components with null-safety
            initializeScheduleTypeComboBox();
            initializeSpinners();
            initializeDatePickers();
            initializeOptimizationSlider();
            initializeButtons();
            initializeTextAreas();

            // Load data
            loadResourceCounts();
            validateResources();
            setupResultsPanel();

            isInitialized = true;
            log.info("✓ ScheduleGeneratorController initialized successfully");
            log.info("====================================\n");

        } catch (Exception e) {
            log.error("❌ CRITICAL ERROR during ScheduleGeneratorController initialization", e);
            Platform.runLater(() -> {
                showError("Initialization Error",
                        "Failed to initialize Schedule Generator.\n\n" +
                                "Error: " + e.getMessage() + "\n\n" +
                                "Please check the logs and restart the application.");
            });
        }
    }

    /**
     * Initialize schedule type combo box with null check
     */
    private void initializeScheduleTypeComboBox() {
        if (scheduleTypeComboBox == null) {
            log.warn("⚠ scheduleTypeComboBox is null - check FXML fx:id='scheduleTypeComboBox'");
            return;
        }

        try {
            scheduleTypeComboBox.getItems().clear();
            scheduleTypeComboBox.getItems().addAll(ScheduleType.values());
            scheduleTypeComboBox.setValue(ScheduleType.TRADITIONAL);
            scheduleTypeComboBox.setOnAction(e -> updateScheduleTypeInfo());
            log.debug("✓ Schedule type combo box configured");
        } catch (Exception e) {
            log.error("Error initializing schedule type combo box", e);
        }
    }

    /**
     * Initialize spinner controls with null checks and safe defaults
     */
    private void initializeSpinners() {
        try {
            // Time spinners (hours)
            if (startHourSpinner != null) {
                startHourSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(6, 12, 7));
            } else {
                log.warn("⚠ startHourSpinner is null");
            }

            if (endHourSpinner != null) {
                endHourSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(12, 20, 15));
            } else {
                log.warn("⚠ endHourSpinner is null");
            }

            if (lunchStartHourSpinner != null) {
                lunchStartHourSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(11, 14, 12));
            } else {
                log.warn("⚠ lunchStartHourSpinner is null");
            }

            // Duration spinners (minutes)
            if (periodDurationSpinner != null) {
                periodDurationSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(30, 120, 50));
            } else {
                log.warn("⚠ periodDurationSpinner is null");
            // Phase 2: Passing Period Spinner
            if (passingPeriodSpinner != null) {
                passingPeriodSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 5));
            } else {
                log.warn("⚠ passingPeriodSpinner is null");
            }
            }

            if (lunchDurationSpinner != null) {
                lunchDurationSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(20, 90, 30));
            } else {
                log.warn("⚠ lunchDurationSpinner is null");
            }

            // Constraint spinners (hours)
            if (maxConsecutiveHoursSpinner != null) {
                maxConsecutiveHoursSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 3));
            } else {
                log.warn("⚠ maxConsecutiveHoursSpinner is null");
            }

            if (maxDailyHoursSpinner != null) {
                maxDailyHoursSpinner.setValueFactory(
                        new SpinnerValueFactory.IntegerSpinnerValueFactory(4, 10, 7));
            } else {
                log.warn("⚠ maxDailyHoursSpinner is null");
            }

            log.debug("✓ Spinners configured");
        } catch (Exception e) {
            log.error("Error initializing spinners", e);
        }
    }

    /**
     * Initialize date pickers with smart defaults
     */
    private void initializeDatePickers() {
        if (startDatePicker == null || endDatePicker == null) {
            log.warn("⚠ Date pickers are null");
            return;
        }

        try {
            LocalDate today = LocalDate.now();

            // Default to next Monday
            LocalDate nextMonday = today.plusDays(1);
            while (nextMonday.getDayOfWeek().getValue() != 1) {
                nextMonday = nextMonday.plusDays(1);
            }

            startDatePicker.setValue(nextMonday);
            endDatePicker.setValue(nextMonday.plusWeeks(16)); // 16-week semester

            log.debug("✓ Date pickers configured: {} to {}", nextMonday, nextMonday.plusWeeks(16));
        } catch (Exception e) {
            log.error("Error initializing date pickers", e);
        }
    }

    /**
     * Initialize optimization time slider
     */
    private void initializeOptimizationSlider() {
        if (optimizationTimeSlider == null) {
            log.warn("⚠ optimizationTimeSlider is null");
            return;
        }

        try {
            optimizationTimeSlider.setMin(30);
            optimizationTimeSlider.setMax(600);
            optimizationTimeSlider.setValue(120);
            optimizationTimeSlider.setMajorTickUnit(60);
            optimizationTimeSlider.setMinorTickCount(5);
            optimizationTimeSlider.setShowTickLabels(true);
            optimizationTimeSlider.setShowTickMarks(true);

            optimizationTimeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                updateOptimizationTimeLabel(newVal.intValue());
            });

            updateOptimizationTimeLabel(120);
            log.debug("✓ Optimization slider configured");
        } catch (Exception e) {
            log.error("Error initializing optimization slider", e);
        }
    }

    /**
     * Initialize button states
     */
    private void initializeButtons() {
        try {
            if (cancelButton != null) {
                cancelButton.setDisable(true);
            }
            if (generateButton != null) {
                generateButton.setDisable(false);
            }
            log.debug("✓ Buttons configured");
        } catch (Exception e) {
            log.error("Error initializing buttons", e);
        }
    }

    /**
     * Initialize text areas
     */
    private void initializeTextAreas() {
        try {
            if (generationLogArea != null) {
                generationLogArea.clear();
                generationLogArea.setEditable(false);
            }
            if (resultsSummaryArea != null) {
                resultsSummaryArea.setEditable(false);
            }
            log.debug("✓ Text areas configured");
        } catch (Exception e) {
            log.error("Error initializing text areas", e);
        }
    }

    /**
     * Update optimization time label
     */
    private void updateOptimizationTimeLabel(int seconds) {
        if (optimizationTimeLabel == null)
            return;

        try {
            int minutes = seconds / 60;
            int secs = seconds % 60;
            optimizationTimeLabel.setText(String.format("%d min %d sec", minutes, secs));
        } catch (Exception e) {
            log.error("Error updating optimization time label", e);
        }
    }

    /**
     * Setup results panel (hidden initially)
     */
    private void setupResultsPanel() {
        if (resultsPanel != null) {
            resultsPanel.setVisible(false);
            resultsPanel.setManaged(false);
            log.debug("✓ Results panel configured (hidden)");
        } else {
            log.warn("⚠ resultsPanel is null");
        }
    }

    // ========================================================================
    // RESOURCE LOADING & VALIDATION
    // ========================================================================

    /**
     * Load resource counts from database
     */
    private void loadResourceCounts() {
        log.info("Loading resource counts...");

        try {
            resourceTeacherCount = sisDataService.getAllTeachers().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getActive()))
                    .count();
            resourceCourseCount = sisDataService.getAllCourses().size();
            resourceRoomCount = roomRepository != null ? roomRepository.count() : 0;
            resourceStudentCount = sisDataService.getAllStudents().size();

            updateResourceCountLabels();

            log.info("✓ Resources loaded: {} teachers, {} courses, {} rooms, {} students",
                    resourceTeacherCount, resourceCourseCount, resourceRoomCount, resourceStudentCount);

        } catch (Exception e) {
            log.error("Error loading resource counts", e);
            showError("Load Error", "Failed to load resource counts: " + e.getMessage());
        }
    }

    /**
     * Update resource count labels in UI
     */
    private void updateResourceCountLabels() {
        Platform.runLater(() -> {
            if (teacherCountLabel != null)
                teacherCountLabel.setText(String.valueOf(resourceTeacherCount));
            if (courseCountLabel != null)
                courseCountLabel.setText(String.valueOf(resourceCourseCount));
            if (roomCountLabel != null)
                roomCountLabel.setText(String.valueOf(resourceRoomCount));
            if (studentCountLabel != null)
                studentCountLabel.setText(String.valueOf(resourceStudentCount));
        });
    }

    /**
     * Validate resources before generation
     */
    private void validateResources() {
        if (validationMessagesBox == null) {
            log.warn("⚠ validationMessagesBox is null");
            return;
        }

        Platform.runLater(() -> {
            validationMessagesBox.getChildren().clear();
            boolean hasErrors = false;

            // Check teachers
            if (resourceTeacherCount == 0) {
                addValidationMessage("❌ No teachers found. Please add teachers first.", "error");
                hasErrors = true;
            } else if (resourceTeacherCount < 5) {
                addValidationMessage("⚠ Only " + resourceTeacherCount + " teacher(s) found. Consider adding more.",
                        "warning");
            } else {
                addValidationMessage("✓ " + resourceTeacherCount + " teachers available", "success");
            }

            // Check courses
            if (resourceCourseCount == 0) {
                addValidationMessage("❌ No courses found. Please add courses first.", "error");
                hasErrors = true;
            } else if (resourceCourseCount < 10) {
                addValidationMessage("⚠ Only " + resourceCourseCount + " course(s) found. Consider adding more.",
                        "warning");
            } else {
                addValidationMessage("✓ " + resourceCourseCount + " courses available", "success");
            }

            // Check rooms
            if (resourceRoomCount == 0) {
                addValidationMessage("❌ No rooms found. Please add rooms first.", "error");
                hasErrors = true;
            } else if (resourceRoomCount < 5) {
                addValidationMessage("⚠ Only " + resourceRoomCount + " room(s) found. Consider adding more.",
                        "warning");
            } else {
                addValidationMessage("✓ " + resourceRoomCount + " rooms available", "success");
            }

            // Check students (optional)
            if (resourceStudentCount == 0) {
                addValidationMessage("ℹ No students found. Students are optional for schedule generation.", "info");
            } else {
                addValidationMessage("✓ " + resourceStudentCount + " students available", "success");
            }

            // Enable/disable generate button
            if (generateButton != null) {
                generateButton.setDisable(hasErrors);
            }

            log.debug("Validation complete. Errors: {}", hasErrors);
        });
    }

    /**
     * Add validation message to UI
     */
    private void addValidationMessage(String message, String type) {
        Label label = new Label(message);

        String style = switch (type) {
            case "error" -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
            case "warning" -> "-fx-text-fill: #f39c12; -fx-font-weight: bold;";
            case "success" -> "-fx-text-fill: #27ae60;";
            case "info" -> "-fx-text-fill: #3498db;";
            default -> "";
        };

        label.setStyle(style + " -fx-padding: 2 0 2 0;");
        label.setWrapText(true);

        validationMessagesBox.getChildren().add(label);
    }

    /**
     * Update schedule type information and adjust defaults
     */
    private void updateScheduleTypeInfo() {
        if (scheduleTypeComboBox == null || periodDurationSpinner == null)
            return;

        ScheduleType selected = scheduleTypeComboBox.getValue();
        if (selected == null)
            return;

        // Adjust default values based on schedule type
        int duration = switch (selected) {
            case TRADITIONAL -> 50;
            case BLOCK -> 90;
            case ROTATING -> 50;
            case MODULAR -> 30;
            case TRIMESTER, QUARTER -> 50;
            case FLEX_MOD -> 25;
        };

        periodDurationSpinner.getValueFactory().setValue(duration);
        log.debug("Schedule type changed to: {} (period duration: {} min)", selected, duration);
    }

    // ========================================================================
    // QUICK SETUP
    // ========================================================================

    /**
     * Handle Quick Setup button click
     * Pre-fills common configuration settings for fast schedule generation
     */
    @FXML
    private void handleQuickSetup() {
        log.info("====================================");
        log.info("QUICK SETUP INITIATED");
        log.info("====================================");

        try {
            // Set schedule type to Traditional (most common)
            if (scheduleTypeComboBox != null) {
                scheduleTypeComboBox.setValue(ScheduleType.TRADITIONAL);
            }

            // Set date range: next Monday for 16 weeks (one semester)
            LocalDate today = LocalDate.now();
            LocalDate nextMonday = today.plusDays(1);
            while (nextMonday.getDayOfWeek().getValue() != 1) {
                nextMonday = nextMonday.plusDays(1);
            }

            if (startDatePicker != null) {
                startDatePicker.setValue(nextMonday);
            }
            if (endDatePicker != null) {
                endDatePicker.setValue(nextMonday.plusWeeks(16));
            }

            // Set default school hours: 7:30 AM - 3:00 PM
            if (startHourSpinner != null) {
                startHourSpinner.getValueFactory().setValue(7);
            }
            if (endHourSpinner != null) {
                endHourSpinner.getValueFactory().setValue(15);
            }

            // Set standard 50-minute periods
            if (periodDurationSpinner != null) {
                periodDurationSpinner.getValueFactory().setValue(50);
            }
            if (passingPeriodSpinner != null) {
                passingPeriodSpinner.getValueFactory().setValue(5);  // Phase 2: 5-minute passing periods
            }

            // Phase 3: Set default times (breakfast period from 07:00-07:20)
            if (schoolStartTimeField != null) {
                schoolStartTimeField.setText("07:00");  // School opens (breakfast/arrival)
            }
            if (firstPeriodStartTimeField != null) {
                firstPeriodStartTimeField.setText("07:20");  // Classes start
            }
            if (schoolEndTimeField != null) {
                schoolEndTimeField.setText("14:10");  // School ends (2:10 PM)
            }

            // Enable lunch: 12:00 PM, 30 minutes
            if (lunchStartHourSpinner != null) {
            // Lunch is now always enabled by law
            if (lunchStartTimeField != null) {
                lunchStartTimeField.setText("10:50");  // Default lunch time
            }
                lunchStartHourSpinner.getValueFactory().setValue(12);
            }
            if (lunchDurationSpinner != null) {
                lunchDurationSpinner.getValueFactory().setValue(30);
            }

            // Set reasonable teacher constraints
            if (maxConsecutiveHoursSpinner != null) {
                maxConsecutiveHoursSpinner.getValueFactory().setValue(3);
            }
            if (maxDailyHoursSpinner != null) {
                maxDailyHoursSpinner.getValueFactory().setValue(7);
            }

            // Set optimization time: 2 minutes (120 seconds)
            if (optimizationTimeSlider != null) {
                optimizationTimeSlider.setValue(120);
            }

            // Generate default schedule name
            String defaultName = "Schedule - " + nextMonday.format(
                    DateTimeFormatter.ofPattern("MMM yyyy"));
            if (scheduleNameField != null) {
                scheduleNameField.setText(defaultName);
            }

            appendToLog("✓ Quick Setup applied - Configuration ready");

            showSuccess("Quick Setup Applied",
                    "Default configuration has been applied successfully!\n\n" +
                            "📋 Configuration Summary:\n" +
                            "• Traditional schedule type\n" +
                            "• 16-week semester starting "
                            + nextMonday.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) + "\n" +
                            "• School day: 7:00 AM - 3:00 PM\n" +
                            "• 50-minute class periods\n" +
                            "• 30-minute lunch at 12:00 PM\n" +
                            "• Max 3 consecutive hours per teacher\n" +
                            "• Max 7 hours per day per teacher\n\n" +
                            "You can modify these settings or click Generate to create your schedule.");

            log.info("✓ Quick Setup completed successfully");
            log.info("====================================\n");

        } catch (Exception e) {
            log.error("❌ Error during Quick Setup", e);
            showError("Quick Setup Error",
                    "Failed to apply quick setup configuration.\n\n" +
                            "Error: " + e.getMessage());
        }
    }

    // ========================================================================
    // SCHEDULE GENERATION
    // ========================================================================

    /**
     * Handle generate button click - start AI generation
     */
    @FXML
    private void handleGenerate() {
        log.info("====================================");
        log.info("STARTING SCHEDULE GENERATION");
        log.info("====================================");

        // Validate inputs
        if (!validateInputs()) {
            return;
        }

        // Hide results panel
        if (resultsPanel != null) {
            resultsPanel.setVisible(false);
            resultsPanel.setManaged(false);
        }

        // Build schedule configuration
        ScheduleGenerationRequest request = buildGenerationRequest();

        // Start generation task
        startGenerationTask(request);
    }

    /**
     * Validate user inputs before generation
     */
    private boolean validateInputs() {
        StringBuilder errors = new StringBuilder();

        // Schedule name
        if (scheduleNameField == null || scheduleNameField.getText() == null ||
                scheduleNameField.getText().trim().isEmpty()) {
            errors.append("• Schedule name is required\n");
        }

        // Dates
        if (startDatePicker == null || startDatePicker.getValue() == null) {
            errors.append("• Start date is required\n");
        }
        if (endDatePicker == null || endDatePicker.getValue() == null) {
            errors.append("• End date is required\n");
        }
        if (startDatePicker != null && endDatePicker != null &&
                startDatePicker.getValue() != null && endDatePicker.getValue() != null) {
            if (endDatePicker.getValue().isBefore(startDatePicker.getValue())) {
                errors.append("• End date must be after start date\n");
            }
            if (startDatePicker.getValue().isBefore(LocalDate.now())) {
                errors.append("• Start date should not be in the past\n");
            }
        }

        // Time validation
        if (startHourSpinner != null && endHourSpinner != null) {
            if (endHourSpinner.getValue() <= startHourSpinner.getValue()) {
                errors.append("• End hour must be after start hour\n");
            }
        }

        // Resource validation
        if (resourceTeacherCount == 0) {
            errors.append("• No teachers available. Add teachers before generating.\n");
        }
        if (resourceCourseCount == 0) {
            errors.append("• No courses available. Add courses before generating.\n");
        }
        if (resourceRoomCount == 0) {
            errors.append("• No rooms available. Add rooms before generating.\n");
        }

        if (errors.length() > 0) {
            showError("Validation Error", "Please fix the following issues:\n\n" + errors.toString());
            return false;
        }

        return true;
    }

    /**
     * Build schedule generation request object
     */
    private ScheduleGenerationRequest buildGenerationRequest() {
        ScheduleGenerationRequest request = new ScheduleGenerationRequest();

        // Basic info
        request.setScheduleName(scheduleNameField != null ? scheduleNameField.getText().trim() : "Unnamed Schedule");
        request.setScheduleType(
                scheduleTypeComboBox != null ? scheduleTypeComboBox.getValue() : ScheduleType.TRADITIONAL);
        request.setStartDate(startDatePicker != null ? startDatePicker.getValue() : LocalDate.now());
        request.setEndDate(endDatePicker != null ? endDatePicker.getValue() : LocalDate.now().plusMonths(4));

        // Time configuration (Legacy hour-based)
        request.setStartHour(startHourSpinner != null && startHourSpinner.getValue() != null ? startHourSpinner.getValue() : 7);
        request.setEndHour(endHourSpinner != null && endHourSpinner.getValue() != null ? endHourSpinner.getValue() : 15);
        request.setPeriodDuration(periodDurationSpinner != null && periodDurationSpinner.getValue() != null ? periodDurationSpinner.getValue() : 50);
        request.setPassingPeriodDuration(passingPeriodSpinner != null && passingPeriodSpinner.getValue() != null ? passingPeriodSpinner.getValue() : 0);  // Phase 2

        // NEW: Minute-precise time configuration (takes priority over hour-based)
        // Phase 3: School start time (breakfast/arrival period)
        if (schoolStartTimeField != null && !schoolStartTimeField.getText().trim().isEmpty()) {
            try {
                LocalTime time = LocalTime.parse(schoolStartTimeField.getText().trim());
                request.setSchoolStartTime(time);
                log.debug("Using minute-precise school start time (breakfast): {}", time);
            } catch (Exception e) {
                log.warn("Invalid school start time format: {}", schoolStartTimeField.getText());
            }
        }
        if (firstPeriodStartTimeField != null && !firstPeriodStartTimeField.getText().trim().isEmpty()) {
            try {
                LocalTime time = LocalTime.parse(firstPeriodStartTimeField.getText().trim());
                request.setFirstPeriodStartTime(time);
                log.debug("Using minute-precise start time: {}", time);
            } catch (Exception e) {
                log.warn("Invalid first period start time format: {}", firstPeriodStartTimeField.getText());
            }
        }
        if (schoolEndTimeField != null && !schoolEndTimeField.getText().trim().isEmpty()) {
            try {
                LocalTime time = LocalTime.parse(schoolEndTimeField.getText().trim());
                request.setSchoolEndTime(time);
                log.debug("Using minute-precise end time: {}", time);
            } catch (Exception e) {
                log.warn("Invalid school end time format: {}", schoolEndTimeField.getText());
            }
        }

        // Lunch configuration (MANDATORY - Always Enabled by Law)
        request.setEnableLunch(true);  // Always true - lunch is required by law
        request.setLunchStartHour(12);  // Default 12:00 PM (overridden by minute-precise if set)
        request.setLunchDuration(lunchDurationSpinner != null ? lunchDurationSpinner.getValue() : 30);

        // NEW: Minute-precise lunch time (takes priority over hour-based)
        if (lunchStartTimeField != null && !lunchStartTimeField.getText().trim().isEmpty()) {
            try {
                LocalTime time = LocalTime.parse(lunchStartTimeField.getText().trim());
                request.setLunchStartTime(time);
                log.debug("Using minute-precise lunch start time: {}", time);
            } catch (Exception e) {
                log.warn("Invalid lunch start time format: {}", lunchStartTimeField.getText());
            }
        }

        // Constraints
        request.setMaxConsecutiveHours(maxConsecutiveHoursSpinner != null ? maxConsecutiveHoursSpinner.getValue() : 3);
        request.setMaxDailyHours(maxDailyHoursSpinner != null ? maxDailyHoursSpinner.getValue() : 7);

        // Optimization
        request.setOptimizationTimeSeconds(
                optimizationTimeSlider != null ? (int) optimizationTimeSlider.getValue() : 120);

        log.info("Generation request configured: {}", request);

        return request;
    }

    /**
     * Start the generation task in background thread
     */
    private void startGenerationTask(ScheduleGenerationRequest request) {
        // Create background task
        generationTask = new Task<>() {
            @Override
            protected Schedule call() throws Exception {
                log.info("Generation task started in background thread");

                updateMessage("Initializing schedule generation...");
                updateProgress(0, 100);

                // Call the generation service
                Schedule result = scheduleGenerationService.generateSchedule(
                        request,
                        (progress, message) -> {
                            updateProgress(progress, 100);
                            updateMessage(message);

                            Platform.runLater(() -> {
                                appendToLog(message);
                            });
                        });

                updateProgress(100, 100);
                updateMessage("Schedule generation complete!");

                return result;
            }
        };

        // Bind UI to task
        if (generationProgressBar != null) {
            generationProgressBar.progressProperty().bind(generationTask.progressProperty());
        }
        if (generationStatusLabel != null) {
            generationStatusLabel.textProperty().bind(generationTask.messageProperty());
        }

        // Handle task completion
        generationTask.setOnSucceeded(e -> {
            generatedSchedule = generationTask.getValue();
            handleGenerationSuccess();
        });

        generationTask.setOnFailed(e -> {
            Throwable error = generationTask.getException();
            log.error("Generation task failed", error);
            handleGenerationFailure(error);
        });

        generationTask.setOnCancelled(e -> {
            log.info("Generation task cancelled by user");
            handleGenerationCancelled();
        });

        // Update UI state
        if (generateButton != null)
            generateButton.setDisable(true);
        if (cancelButton != null)
            cancelButton.setDisable(false);
        if (generationLogArea != null)
            generationLogArea.clear();

        // Start task
        Thread taskThread = new Thread(generationTask);
        taskThread.setDaemon(true);
        taskThread.start();

        log.info("Generation task thread started");
    }

    /**
     * Handle successful generation
     */
    private void handleGenerationSuccess() {
        log.info("====================================");
        log.info("SCHEDULE GENERATION SUCCESSFUL");
        log.info("====================================");

        Platform.runLater(() -> {
            // Reset UI
            if (generateButton != null)
                generateButton.setDisable(false);
            if (cancelButton != null)
                cancelButton.setDisable(true);

            // Show results
            displayResults();

            // Show success message
            String scorePct = generatedSchedule != null
                    ? String.format("%.1f%%", generatedSchedule.getOptimizationScore() * 100)
                    : "N/A";

            showSuccess("Generation Complete",
                    "✓ Schedule generated successfully!\n\n" +
                            "Schedule: " + (generatedSchedule != null ? generatedSchedule.getName() : "Unknown") + "\n"
                            +
                            "Optimization Score: " + scorePct + "\n\n" +
                            "Review the results below and click 'Publish' when ready.");
        });
    }

    /**
     * Handle generation failure
     */
    private void handleGenerationFailure(Throwable error) {
        log.error("====================================");
        log.error("SCHEDULE GENERATION FAILED");
        log.error("====================================");

        Platform.runLater(() -> {
            if (generateButton != null)
                generateButton.setDisable(false);
            if (cancelButton != null)
                cancelButton.setDisable(true);

            String errorMessage = error != null ? error.getMessage() : "Unknown error";
            appendToLog("❌ Generation failed: " + errorMessage);

            // Check if this is a constraint violation error
            boolean isConstraintViolation = errorMessage != null &&
                (errorMessage.contains("hard constraint") ||
                 errorMessage.contains("constraint violation") ||
                 errorMessage.contains("cannot be resolved"));

            if (isConstraintViolation) {
                // Analyze violations and show detailed dialog
                showConstraintViolationDialog(errorMessage);
            } else {
                // Show generic error
                showError("Generation Failed",
                        "Failed to generate schedule.\n\n" +
                                "Error: " + errorMessage + "\n\n" +
                                "Please check:\n" +
                                "• All required resources are available\n" +
                                "• Configuration parameters are valid\n" +
                                "• Application logs for details");
            }
        });
    }

    /**
     * Show constraint violation dialog with analysis and fix options
     */
    private void showConstraintViolationDialog(String errorMessage) {
        log.info("Analyzing constraint violations...");

        // Run analysis
        ScheduleViolationAnalyzer.AnalysisResult analysis = violationAnalyzer.analyzePreSchedule();

        // Build detailed message
        StringBuilder details = new StringBuilder();
        details.append("Schedule generation failed due to hard constraint violations.\n\n");
        details.append("Error: ").append(errorMessage).append("\n\n");

        if (analysis.getTotalViolations() > 0) {
            details.append("Analysis found ").append(analysis.getTotalViolations()).append(" potential issue(s):\n\n");

            // Summarize by type
            for (var entry : analysis.getViolationsByType().entrySet()) {
                details.append("• ").append(entry.getKey().getDescription())
                       .append(": ").append(entry.getValue()).append("\n");
            }

            details.append("\nWould you like to view and fix these issues?");
        } else {
            details.append("No specific issues found in current data.\n");
            details.append("The constraint violations may be due to scheduling conflicts.\n\n");
            details.append("Try:\n");
            details.append("• Reducing the number of courses to schedule\n");
            details.append("• Adding more rooms or teachers\n");
            details.append("• Adjusting time constraints");
        }

        // Show dialog with fix option
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Schedule Generation Failed");
        alert.setHeaderText("Hard Constraint Violations Detected");
        alert.setContentText(details.toString());
        alert.getDialogPane().setMinWidth(500);
        alert.getDialogPane().setMinHeight(400);

        // Add buttons
        ButtonType viewDiagnosticBtn = new ButtonType("View Diagnostic Report", ButtonBar.ButtonData.LEFT);
        ButtonType fixIssuesBtn = new ButtonType("Fix Issues", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeBtn = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);

        if (analysis.getTotalViolations() > 0) {
            alert.getButtonTypes().setAll(fixIssuesBtn, viewDiagnosticBtn, closeBtn);
        } else {
            alert.getButtonTypes().setAll(viewDiagnosticBtn, closeBtn);
        }

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent()) {
            if (result.get() == fixIssuesBtn) {
                // Show fix violations dialog
                // FixViolationsDialog.show()/showAndWait() not implemented — dialog display disabled
                // Comment out until FixViolationsDialog is properly implemented
                // fixViolationsDialog.show();
                log.warn("Fix violations dialog not yet implemented");
            } else if (result.get() == viewDiagnosticBtn) {
                // Show diagnostic report
                handleViewDiagnosticReport();
            }
        }
    }

    /**
     * Handle View Diagnostic Report button - shows detailed analysis in a dialog
     */
    private void handleViewDiagnosticReport() {
        // Get analysis results
        ScheduleViolationAnalyzer.AnalysisResult analysis = violationAnalyzer.analyzePreSchedule();

        // Build detailed report
        StringBuilder report = new StringBuilder();
        report.append("=== SCHEDULE DIAGNOSTIC REPORT ===\n\n");
        report.append(String.format("Total Violations: %d (Critical: %d)\n\n",
            analysis.getTotalViolations(), analysis.getCriticalCount()));

        // Group violations by type
        java.util.Map<ScheduleViolationAnalyzer.ViolationType, java.util.List<ScheduleViolationAnalyzer.Violation>> byType =
            analysis.getViolations().stream()
                .collect(java.util.stream.Collectors.groupingBy(ScheduleViolationAnalyzer.Violation::getType));

        for (ScheduleViolationAnalyzer.ViolationType type : ScheduleViolationAnalyzer.ViolationType.values()) {
            java.util.List<ScheduleViolationAnalyzer.Violation> violations = byType.get(type);
            if (violations != null && !violations.isEmpty()) {
                report.append(String.format("--- %s (%d) ---\n", type.getDescription(), violations.size()));
                for (ScheduleViolationAnalyzer.Violation v : violations) {
                    report.append(String.format("  • %s: %s\n", v.getEntityName(), v.getDescription()));
                    report.append(String.format("    Suggested Fix: %s\n", v.getSuggestedFix()));
                }
                report.append("\n");
            }
        }

        // Show in a text area dialog
        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Schedule Diagnostic Report");
        dialog.setHeaderText("Pre-Generation Analysis");
        dialog.getDialogPane().setPrefWidth(700);
        dialog.getDialogPane().setPrefHeight(500);

        TextArea textArea = new TextArea(report.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 12px;");

        dialog.getDialogPane().setContent(textArea);
        dialog.showAndWait();
    }

    /**
     * Handle generation cancellation
     */
    private void handleGenerationCancelled() {
        Platform.runLater(() -> {
            if (generateButton != null)
                generateButton.setDisable(false);
            if (cancelButton != null)
                cancelButton.setDisable(true);

            appendToLog("⚠ Generation cancelled by user");

            showWarning("Generation Cancelled", "Schedule generation was cancelled.");
        });
    }

    /**
     * Handle cancel button click
     */
    @FXML
    private void handleCancel() {
        if (generationTask != null && generationTask.isRunning()) {
            log.info("Cancelling generation task...");

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Cancel Generation");
            confirm.setHeaderText("Cancel schedule generation?");
            confirm.setContentText("This will stop the current generation process.\n\nAre you sure?");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                generationTask.cancel();
            }
        }
    }

    /**
     * Append message to generation log with timestamp
     */
    private void appendToLog(String message) {
        if (generationLogArea == null)
            return;

        try {
            String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String logEntry = String.format("[%s] %s\n", timestamp, message);
            generationLogArea.appendText(logEntry);
        } catch (Exception e) {
            log.error("Error appending to log", e);
        }
    }

    // ========================================================================
    // RESULTS DISPLAY
    // ========================================================================

    /**
     * Display generation results with comprehensive statistics
     */
    private void displayResults() {
        if (generatedSchedule == null) {
            log.warn("No schedule to display");
            return;
        }

        log.info("Displaying generation results...");

        // Show results panel
        if (resultsPanel != null) {
            resultsPanel.setVisible(true);
            resultsPanel.setManaged(true);
        }

        // Load schedule slots for analysis
        List<ScheduleSlot> slots = scheduleSlotService.getSlotsBySchedule(generatedSchedule.getId());

        // Calculate statistics
        long totalSlots = slots.size();
        long assignedSlots = slots.stream().filter(ScheduleSlot::isAssigned).count();
        long conflictSlots = slots.stream().filter(slot -> Boolean.TRUE.equals(slot.getHasConflict())).count();

        double assignmentRate = totalSlots > 0 ? (assignedSlots * 100.0 / totalSlots) : 0;
        double optimizationScore = generatedSchedule.getOptimizationScore() * 100;

        // Update labels
        if (optimizationScoreLabel != null) {
            optimizationScoreLabel.setText(String.format("%.1f%%", optimizationScore));

            // Color-code optimization score
            if (optimizationScore >= 90) {
                optimizationScoreLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 24px;");
            } else if (optimizationScore >= 70) {
                optimizationScoreLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 24px;");
            } else {
                optimizationScoreLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 24px;");
            }
        }

        if (conflictCountLabel != null) {
            conflictCountLabel.setText(String.valueOf(conflictSlots));
        }
        if (assignmentRateLabel != null) {
            assignmentRateLabel.setText(String.format("%.1f%%", assignmentRate));
        }

        // Calculate actual teacher and room utilization
        double teacherUtilization = calculateTeacherUtilization(slots);
        double roomUtilization = calculateRoomUtilization(slots);

        if (teacherUtilizationLabel != null) {
            teacherUtilizationLabel.setText(String.format("%.1f%%", teacherUtilization));
        }
        if (roomUtilizationLabel != null) {
            roomUtilizationLabel.setText(String.format("%.1f%%", roomUtilization));
        }

        // Build comprehensive summary text
        buildResultsSummary(totalSlots, assignedSlots, conflictSlots, assignmentRate, optimizationScore);

        log.info("✓ Results displayed successfully");
    }

    /**
     * Build detailed results summary text
     */
    private void buildResultsSummary(long totalSlots, long assignedSlots, long conflictSlots,
            double assignmentRate, double optimizationScore) {
        if (resultsSummaryArea == null)
            return;

        StringBuilder summary = new StringBuilder();
        summary.append("═".repeat(60)).append("\n");
        summary.append("  SCHEDULE GENERATION SUMMARY\n");
        summary.append("═".repeat(60)).append("\n\n");

        summary.append("📋 SCHEDULE INFORMATION\n");
        summary.append("─".repeat(60)).append("\n");
        summary.append(String.format("  Name: %s\n", generatedSchedule.getName()));
        summary.append(String.format("  Type: %s\n", generatedSchedule.getScheduleType()));
        summary.append(String.format("  Status: %s\n", generatedSchedule.getStatus()));
        summary.append(String.format("  Period: %s to %s\n\n",
                generatedSchedule.getStartDate(), generatedSchedule.getEndDate()));

        summary.append("📊 GENERATION STATISTICS\n");
        summary.append("─".repeat(60)).append("\n");
        summary.append(String.format("  Total Slots: %,d\n", totalSlots));
        summary.append(String.format("  Assigned: %,d (%.1f%%)\n", assignedSlots, assignmentRate));
        summary.append(String.format("  Unassigned: %,d\n", totalSlots - assignedSlots));
        summary.append(String.format("  Conflicts: %,d\n", conflictSlots));
        summary.append(String.format("  Optimization Score: %.1f%%\n\n", optimizationScore));

        summary.append("✨ QUALITY ASSESSMENT\n");
        summary.append("─".repeat(60)).append("\n");
        if (optimizationScore >= 90) {
            summary.append("  ⭐⭐⭐⭐⭐ EXCELLENT\n\n");
            summary.append("  This schedule is highly optimized with minimal conflicts\n");
            summary.append("  and excellent resource utilization. Ready to publish!\n");
        } else if (optimizationScore >= 75) {
            summary.append("  ⭐⭐⭐⭐ VERY GOOD\n\n");
            summary.append("  This schedule is well-optimized with few conflicts.\n");
            summary.append("  You may publish as-is or re-optimize for better results.\n");
        } else if (optimizationScore >= 60) {
            summary.append("  ⭐⭐⭐ GOOD\n\n");
            summary.append("  This schedule has some conflicts and room for improvement.\n");
            summary.append("  Consider re-optimization before publishing.\n");
        } else {
            summary.append("  ⭐⭐ NEEDS IMPROVEMENT\n\n");
            summary.append("  This schedule has significant conflicts. Re-optimization\n");
            summary.append("  is strongly recommended before publishing.\n");
        }

        summary.append("\n");
        summary.append("💡 RECOMMENDATIONS\n");
        summary.append("─".repeat(60)).append("\n");
        if (conflictSlots > 0) {
            summary.append(String.format("  • Review %d conflict(s) before publishing\n", conflictSlots));
        }
        if (assignmentRate < 95) {
            summary.append("  • Some slots remain unassigned - review coverage\n");
        }
        if (optimizationScore < 75) {
            summary.append("  • Try re-optimization with more time for better results\n");
        }
        if (conflictSlots == 0 && optimizationScore >= 90) {
            summary.append("  • Excellent quality - ready to publish!\n");
        }

        summary.append("\n═".repeat(60)).append("\n");

        resultsSummaryArea.setText(summary.toString());
    }

    /**
     * Calculate teacher utilization percentage
     * Utilization = (Total assigned teaching hours) / (Total available teaching hours) * 100
     *
     * @param slots List of schedule slots
     * @return Teacher utilization percentage (0-100)
     */
    private double calculateTeacherUtilization(List<ScheduleSlot> slots) {
        try {
            // Get all active teachers
            List<Teacher> allTeachers = sisDataService.getAllTeachers().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getActive()))
                    .collect(java.util.stream.Collectors.toList());
            if (allTeachers.isEmpty()) {
                return 0.0;
            }

            // Count unique teachers assigned to slots
            Set<Long> assignedTeacherIds = slots.stream()
                    .filter(slot -> slot.getTeacher() != null)
                    .map(slot -> slot.getTeacher().getId())
                    .collect(Collectors.toSet());

            // Calculate total assigned hours
            long totalAssignedHours = slots.stream()
                    .filter(slot -> slot.getTeacher() != null && slot.getStartTime() != null && slot.getEndTime() != null)
                    .count(); // Each slot represents one teaching period

            // Estimate total available teaching hours
            // Assume 5 days/week, 6 periods/day = 30 periods/teacher/week
            int periodsPerTeacher = 30;
            long totalAvailableHours = allTeachers.size() * periodsPerTeacher;

            if (totalAvailableHours == 0) {
                return 0.0;
            }

            double utilization = (totalAssignedHours * 100.0) / totalAvailableHours;

            log.debug("Teacher utilization: {} assigned teachers out of {}, {} assigned hours out of {} available",
                    assignedTeacherIds.size(), allTeachers.size(), totalAssignedHours, totalAvailableHours);

            return Math.min(100.0, utilization); // Cap at 100%

        } catch (Exception e) {
            log.error("Error calculating teacher utilization", e);
            return 0.0;
        }
    }

    /**
     * Calculate room utilization percentage
     * Utilization = (Total room usage periods) / (Total available room periods) * 100
     *
     * @param slots List of schedule slots
     * @return Room utilization percentage (0-100)
     */
    private double calculateRoomUtilization(List<ScheduleSlot> slots) {
        try {
            // Get all active rooms
            List<Room> allRooms = roomService.getAllActiveRooms();
            if (allRooms.isEmpty()) {
                return 0.0;
            }

            // Count unique rooms assigned to slots
            Set<Long> assignedRoomIds = slots.stream()
                    .filter(slot -> slot.getRoom() != null)
                    .map(slot -> slot.getRoom().getId())
                    .collect(Collectors.toSet());

            // Calculate total room usage periods
            long totalRoomUsagePeriods = slots.stream()
                    .filter(slot -> slot.getRoom() != null)
                    .count(); // Each slot uses one room for one period

            // Estimate total available room periods
            // Assume 5 days/week, 6 periods/day = 30 periods/room/week
            int periodsPerRoom = 30;
            long totalAvailablePeriods = allRooms.size() * periodsPerRoom;

            if (totalAvailablePeriods == 0) {
                return 0.0;
            }

            double utilization = (totalRoomUsagePeriods * 100.0) / totalAvailablePeriods;

            log.debug("Room utilization: {} assigned rooms out of {}, {} usage periods out of {} available",
                    assignedRoomIds.size(), allRooms.size(), totalRoomUsagePeriods, totalAvailablePeriods);

            return Math.min(100.0, utilization); // Cap at 100%

        } catch (Exception e) {
            log.error("Error calculating room utilization", e);
            return 0.0;
        }
    }

    // ========================================================================
    // ACTION HANDLERS
    // ========================================================================

    /**
     * Save schedule as draft
     */
    @FXML
    private void handleSaveDraft() {
        if (generatedSchedule == null) {
            showWarning("No Schedule", "Please generate a schedule first.");
            return;
        }

        log.info("Saving schedule as draft: {}", generatedSchedule.getName());

        try {
            generatedSchedule.setStatus(ScheduleStatus.DRAFT);
            scheduleService.saveSchedule(generatedSchedule);

            showSuccess("Saved",
                    "Schedule saved as draft successfully!\n\n" +
                            "You can continue editing or publish when ready.");

            appendToLog("✓ Schedule saved as draft");

        } catch (Exception e) {
            log.error("Error saving draft", e);
            showError("Save Error", "Failed to save schedule: " + e.getMessage());
        }
    }

    /**
     * Publish schedule
     */
    @FXML
    private void handlePublish() {
        if (generatedSchedule == null) {
            showWarning("No Schedule", "Please generate a schedule first.");
            return;
        }

        // Confirmation dialog with details
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Publish Schedule");
        confirm.setHeaderText("Ready to publish this schedule?");
        confirm.setContentText(
                "This will make the schedule visible to all users.\n\n" +
                        "Schedule: " + generatedSchedule.getName() + "\n" +
                        "Optimization Score: " + String.format("%.1f%%",
                                generatedSchedule.getOptimizationScore() * 100)
                        + "\n" +
                        "Period: " + generatedSchedule.getStartDate() + " to " +
                        generatedSchedule.getEndDate() + "\n\n" +
                        "Continue with publication?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                publishSchedule();
            }
        });
    }

    /**
     * Publish the schedule
     */
    private void publishSchedule() {
        log.info("Publishing schedule: {}", generatedSchedule.getName());

        try {
            generatedSchedule.setStatus(ScheduleStatus.PUBLISHED);
            scheduleService.saveSchedule(generatedSchedule);

            appendToLog("✓ Schedule published successfully");

            showSuccess("Published",
                    "✓ Schedule published successfully!\n\n" +
                            "The schedule is now visible to all users and can be\n" +
                            "viewed in the Schedule Viewer.");

        } catch (Exception e) {
            log.error("Error publishing schedule", e);
            showError("Publish Error", "Failed to publish schedule: " + e.getMessage());
        }
    }

    /**
     * Re-optimize current schedule with extended time
     */
    @FXML
    private void handleReOptimize() {
        if (generatedSchedule == null) {
            showWarning("No Schedule", "Please generate a schedule first.");
            return;
        }

        log.info("Re-optimizing schedule: {}", generatedSchedule.getName());

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Re-Optimize Schedule");
        info.setHeaderText("Extended Optimization");
        info.setContentText(
                "Re-optimization will improve the current schedule\n" +
                        "using extended AI solver time (up to 10 minutes).\n\n" +
                        "This feature will be fully implemented in the next release.\n\n" +
                        "For now, you can:\n" +
                        "• Adjust the optimization time slider\n" +
                        "• Click Generate again for a new schedule");
        info.showAndWait();
    }

    /**
     * View schedule in calendar viewer
     */
    @FXML
    private void handleViewSchedule() {
        if (generatedSchedule == null) {
            showWarning("No Schedule", "Please generate a schedule first.");
            return;
        }

        log.info("Opening schedule in viewer: {}", generatedSchedule.getName());

        showInfo("View Schedule",
                "This will open the Schedule Viewer to display\n" +
                        "your generated schedule in calendar format.\n\n" +
                        "Schedule Viewer integration coming in next release!");
    }

    /**
     * Refresh resource counts from database
     */
    @FXML
    private void handleRefreshResources() {
        log.info("Refreshing resource counts...");
        loadResourceCounts();
        validateResources();
        showSuccess("Refreshed", "Resource counts updated successfully!");
        appendToLog("✓ Resources refreshed");
    }

    // ========================================================================
    // DIALOG HELPERS
    // ========================================================================

    /**
     * Show information dialog
     */
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show success dialog
     */
    private void showSuccess(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText("✓ Success");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show warning dialog
     */
    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText("⚠ Warning");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show error dialog
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText("✗ Error");
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
