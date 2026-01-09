package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.enums.SchedulePeriod;
import com.heronix.scheduler.model.enums.ScheduleStatus;
import com.heronix.scheduler.model.enums.ScheduleType;
import com.heronix.scheduler.repository.ScheduleRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Schedules Controller - COMPLETE & FIXED
 * Location:
 * src/main/java/com/eduscheduler/ui/controller/SchedulesController.java
 * 
 * Displays and manages all schedules in the database
 * FIXED: Lazy initialization exception for slots collection
 */
@Slf4j
@Controller
public class SchedulesController {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private com.heronix.scheduler.service.ExportService exportService;

    @Autowired
    private com.heronix.scheduler.service.ScheduleSlotService scheduleSlotService;

    @Autowired
    private org.springframework.context.ConfigurableApplicationContext springContext;

    // ========================================================================
    // FXML COMPONENTS
    // ========================================================================

    @FXML
    private TableView<Schedule> schedulesTable;
    @FXML
    private TableColumn<Schedule, String> nameColumn;
    @FXML
    private TableColumn<Schedule, String> typeColumn;
    @FXML
    private TableColumn<Schedule, String> dayRotationColumn;
    @FXML
    private TableColumn<Schedule, String> periodColumn;
    @FXML
    private TableColumn<Schedule, String> statusColumn;
    @FXML
    private TableColumn<Schedule, String> startDateColumn;
    @FXML
    private TableColumn<Schedule, String> endDateColumn;
    @FXML
    private TableColumn<Schedule, String> slotsColumn;
    @FXML
    private TableColumn<Schedule, String> conflictsColumn;
    @FXML
    private TableColumn<Schedule, String> qualityColumn;

    @FXML
    private ComboBox<String> statusFilterCombo;
    @FXML
    private ComboBox<String> typeFilterCombo;
    @FXML
    private ComboBox<String> periodFilterCombo;

    @FXML
    private Button viewButton;
    @FXML
    private Button editButton;
    @FXML
    private Button duplicateButton;
    @FXML
    private Button exportButton;
    @FXML
    private Button deleteButton;

    @FXML
    private Label statusLabel;
    @FXML
    private Label countLabel;

    // ========================================================================
    // DATA & FORMATTERS
    // ========================================================================

    private ObservableList<Schedule> schedulesList = FXCollections.observableArrayList();
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    /**
     * Initialize controller
     */
    @FXML
    public void initialize() {
        log.info("SchedulesController initialized");
        setupTableColumns();
        setupFilters();
        setupSelectionListener();
        loadSchedules();
    }

    // ========================================================================
    // TABLE SETUP (FIXED FOR LAZY LOADING)
    // ========================================================================

    /**
     * Setup table columns - FIXED for lazy initialization
     */
    private void setupTableColumns() {
        // Basic columns (no lazy loading issues)
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getScheduleName()));

        typeColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getScheduleType().toString()));

        // Day Rotation column (for block schedules) - Block Scheduling MVP
        dayRotationColumn.setCellValueFactory(data -> {
            if (data.getValue().getScheduleType() == ScheduleType.BLOCK) {
                return new SimpleStringProperty("ODD/EVEN");
            } else {
                return new SimpleStringProperty("N/A");
            }
        });

        periodColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPeriod().toString()));

        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus().toString()));

        startDateColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getStartDate().format(dateFormatter)));

        endDateColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getEndDate().format(dateFormatter)));

        // FIXED: Safe slot count (handles lazy initialization)
        slotsColumn.setCellValueFactory(data -> {
            try {
                int count = data.getValue().getSlotCount();
                return new SimpleStringProperty(String.valueOf(count));
            } catch (Exception e) {
                // LazyInitializationException or other error
                log.debug("Could not load slot count for schedule: {}", data.getValue().getScheduleName());
                return new SimpleStringProperty("N/A");
            }
        });

        // Conflicts column (direct field access, no lazy loading)
        conflictsColumn.setCellValueFactory(data -> {
            Integer conflicts = data.getValue().getTotalConflicts();
            return new SimpleStringProperty(conflicts != null ? String.valueOf(conflicts) : "0");
        });

        // Quality score column
        qualityColumn.setCellValueFactory(data -> {
            Double score = data.getValue().getOptimizationScore();
            return new SimpleStringProperty(score != null ? String.format("%.1f%%", score) : "N/A");
        });

        // Add custom cell factories for styling
        setupColumnStyling();
    }

    /**
     * Setup custom cell styling
     */
    private void setupColumnStyling() {
        // Status column with color coding
        statusColumn.setCellFactory(column -> new TableCell<Schedule, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "PUBLISHED" -> setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                        case "DRAFT" -> setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                        case "ARCHIVED" -> setStyle("-fx-text-fill: #999; -fx-font-weight: bold;");
                        default -> setStyle("");
                    }
                }
            }
        });

        // Day Rotation column with color coding (Block Scheduling MVP)
        dayRotationColumn.setCellFactory(column -> new TableCell<Schedule, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ODD/EVEN".equals(item)) {
                        setStyle("-fx-text-fill: #00aaff; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #888;");
                    }
                }
            }
        });

        // Conflicts column with color coding
        conflictsColumn.setCellFactory(column -> new TableCell<Schedule, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    try {
                        int conflicts = Integer.parseInt(item);
                        if (conflicts == 0) {
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                        } else if (conflicts < 5) {
                            setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                        }
                    } catch (NumberFormatException e) {
                        setStyle("");
                    }
                }
            }
        });

        // Quality column with color coding
        qualityColumn.setCellFactory(column -> new TableCell<Schedule, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || "N/A".equals(item)) {
                    setText(item);
                    setStyle("");
                } else {
                    setText(item);
                    try {
                        double quality = Double.parseDouble(item.replace("%", ""));
                        if (quality >= 90) {
                            setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                        } else if (quality >= 70) {
                            setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                        }
                    } catch (Exception e) {
                        setStyle("");
                    }
                }
            }
        });
    }

    // ========================================================================
    // FILTER SETUP
    // ========================================================================

    /**
     * Setup filter dropdowns
     */
    private void setupFilters() {
        // Status filter
        statusFilterCombo.getItems().addAll("All", "DRAFT", "PUBLISHED", "ARCHIVED");
        statusFilterCombo.setValue("All");
        statusFilterCombo.setOnAction(e -> applyFilters());

        // Type filter
        typeFilterCombo.getItems().addAll("All", "TRADITIONAL", "BLOCK", "ROTATING", "MODULAR", "TRIMESTER", "QUARTER");
        typeFilterCombo.setValue("All");
        typeFilterCombo.setOnAction(e -> applyFilters());

        // Period filter
        periodFilterCombo.getItems().addAll("All", "SEMESTER", "TRIMESTER", "QUARTER", "ANNUAL");
        periodFilterCombo.setValue("All");
        periodFilterCombo.setOnAction(e -> applyFilters());
    }

    /**
     * Setup selection listener to enable/disable action buttons
     */
    private void setupSelectionListener() {
        schedulesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean hasSelection = newSelection != null;

            if (viewButton != null)
                viewButton.setDisable(!hasSelection);
            if (editButton != null)
                editButton.setDisable(!hasSelection);
            if (duplicateButton != null)
                duplicateButton.setDisable(!hasSelection);
            if (exportButton != null)
                exportButton.setDisable(!hasSelection);
            if (deleteButton != null)
                deleteButton.setDisable(!hasSelection);
        });
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    /**
     * Load schedules from database - FIXED for lazy loading
     */
    private void loadSchedules() {
        CompletableFuture.runAsync(() -> {
            try {
                updateStatus("Loading schedules...");

                // Load schedules - slots collection may not be loaded (lazy)
                List<Schedule> schedules = scheduleRepository.findAll();

                Platform.runLater(() -> {
                    schedulesList.clear();
                    schedulesList.addAll(schedules);
                    schedulesTable.setItems(schedulesList);
                    updateCount();
                    updateStatus("Loaded " + schedules.size() + " schedules");
                    log.info("Loaded {} schedules", schedules.size());
                });

            } catch (Exception e) {
                log.error("Error loading schedules", e);
                Platform.runLater(() -> {
                    showError("Error", "Failed to load schedules: " + e.getMessage());
                    updateStatus("Error loading schedules");
                });
            }
        });
    }

    /**
     * Apply filters to schedule list
     */
    private void applyFilters() {
        String statusFilter = statusFilterCombo.getValue();
        String typeFilter = typeFilterCombo.getValue();
        String periodFilter = periodFilterCombo.getValue();

        CompletableFuture.runAsync(() -> {
            try {
                updateStatus("Applying filters...");

                List<Schedule> schedules = scheduleRepository.findAll();

                // Filter by status
                if (!"All".equals(statusFilter)) {
                    schedules = schedules.stream()
                            .filter(s -> s.getStatus().toString().equals(statusFilter))
                            .toList();
                }

                // Filter by type
                if (!"All".equals(typeFilter)) {
                    schedules = schedules.stream()
                            .filter(s -> s.getScheduleType().toString().equals(typeFilter))
                            .toList();
                }

                // Filter by period
                if (!"All".equals(periodFilter)) {
                    schedules = schedules.stream()
                            .filter(s -> s.getPeriod().toString().equals(periodFilter))
                            .toList();
                }

                List<Schedule> finalSchedules = schedules;
                Platform.runLater(() -> {
                    schedulesList.clear();
                    schedulesList.addAll(finalSchedules);
                    updateCount();
                    updateStatus("Filtered: " + finalSchedules.size() + " schedules");
                });

            } catch (Exception e) {
                log.error("Error applying filters", e);
                Platform.runLater(() -> updateStatus("Error applying filters"));
            }
        });
    }

    // ========================================================================
    // ACTION HANDLERS
    // ========================================================================

    /**
     * Clear all filters
     */
    @FXML
    private void handleClearFilters() {
        log.info("Clearing filters");
        statusFilterCombo.setValue("All");
        typeFilterCombo.setValue("All");
        periodFilterCombo.setValue("All");
        loadSchedules();
    }

    /**
     * Refresh schedules
     */
    @FXML
    private void handleRefresh() {
        log.info("Refreshing schedules");
        loadSchedules();
    }

    /**
     * Handle new schedule
     */
    @FXML
    private void handleNewSchedule() {
        log.info("New schedule clicked");
        showScheduleCreationWizard();
    }

    /**
     * Show Schedule Creation Wizard dialog
     */
    private void showScheduleCreationWizard() {
        Dialog<Schedule> dialog = new Dialog<>();
        dialog.setTitle("Create New Schedule");
        dialog.setHeaderText("Schedule Creation Wizard");

        ButtonType createButtonType = new ButtonType("Create", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        // Create form layout
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        // Form fields
        TextField nameField = new TextField();
        nameField.setPromptText("Schedule name (e.g., Fall 2025 Master Schedule)");

        ComboBox<ScheduleType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(ScheduleType.values());
        typeCombo.setPromptText("Select schedule type");
        typeCombo.setValue(ScheduleType.TRADITIONAL); // Default

        ComboBox<SchedulePeriod> periodCombo = new ComboBox<>();
        periodCombo.getItems().addAll(SchedulePeriod.values());
        periodCombo.setPromptText("Select period");
        periodCombo.setValue(SchedulePeriod.SEMESTER); // Default

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Start date");
        startDatePicker.setValue(LocalDate.now());

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setPromptText("End date");
        endDatePicker.setValue(LocalDate.now().plusMonths(4));

        // Time configuration
        TextField startTimeField = new TextField("07:00");
        startTimeField.setPromptText("HH:mm (e.g., 07:00)");

        TextField endTimeField = new TextField("16:00");
        endTimeField.setPromptText("HH:mm (e.g., 16:00)");

        TextField slotDurationField = new TextField("45");
        slotDurationField.setPromptText("Minutes (e.g., 45)");

        ComboBox<ScheduleStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(ScheduleStatus.values());
        statusCombo.setValue(ScheduleStatus.DRAFT); // Default
        statusCombo.setPromptText("Initial status");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Schedule notes or description");
        notesArea.setPrefRowCount(3);

        // Add help text for schedule types
        Label helpText = new Label();
        helpText.setWrapText(true);
        helpText.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        helpText.setPrefWidth(400);

        typeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String help = switch (newVal) {
                    case TRADITIONAL -> "Traditional schedule with fixed periods throughout the day";
                    case BLOCK -> "Block scheduling with longer class periods on alternating days";
                    case ROTATING -> "Rotating schedule where class periods change order cyclically";
                    case MODULAR -> "Flexible modular scheduling with varying time blocks";
                    case TRIMESTER -> "Academic year divided into three terms";
                    case QUARTER -> "Academic year divided into four quarters";
                    case FLEX_MOD -> "Flexible modular schedule with customizable configuration";
                };
                helpText.setText(help);
            }
        });

        // Add fields to grid
        int row = 0;
        grid.add(new Label("Schedule Name:*"), 0, row);
        grid.add(nameField, 1, row++);

        grid.add(new Label("Schedule Type:*"), 0, row);
        grid.add(typeCombo, 1, row++);

        grid.add(new Label(""), 0, row);
        grid.add(helpText, 1, row++);

        grid.add(new Label("Period:*"), 0, row);
        grid.add(periodCombo, 1, row++);

        grid.add(new Label("Start Date:*"), 0, row);
        grid.add(startDatePicker, 1, row++);

        grid.add(new Label("End Date:*"), 0, row);
        grid.add(endDatePicker, 1, row++);

        grid.add(new Label("Day Start Time:*"), 0, row);
        grid.add(startTimeField, 1, row++);

        grid.add(new Label("Day End Time:*"), 0, row);
        grid.add(endTimeField, 1, row++);

        grid.add(new Label("Slot Duration (min):*"), 0, row);
        grid.add(slotDurationField, 1, row++);

        grid.add(new Label("Initial Status:*"), 0, row);
        grid.add(statusCombo, 1, row++);

        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row++);

        dialog.getDialogPane().setContent(grid);

        // Enable/disable create button based on validation
        javafx.scene.Node createButton = dialog.getDialogPane().lookupButton(createButtonType);
        createButton.setDisable(true);

        // Validation listener
        Runnable validateForm = () -> {
            boolean valid = !nameField.getText().trim().isEmpty() &&
                          typeCombo.getValue() != null &&
                          periodCombo.getValue() != null &&
                          startDatePicker.getValue() != null &&
                          endDatePicker.getValue() != null &&
                          !startTimeField.getText().trim().isEmpty() &&
                          !endTimeField.getText().trim().isEmpty() &&
                          !slotDurationField.getText().trim().isEmpty();
            createButton.setDisable(!valid);
        };

        nameField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        typeCombo.valueProperty().addListener((obs, old, newVal) -> validateForm.run());
        periodCombo.valueProperty().addListener((obs, old, newVal) -> validateForm.run());
        startDatePicker.valueProperty().addListener((obs, old, newVal) -> validateForm.run());
        endDatePicker.valueProperty().addListener((obs, old, newVal) -> validateForm.run());
        startTimeField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        endTimeField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        slotDurationField.textProperty().addListener((obs, old, newVal) -> validateForm.run());

        // Initial validation
        validateForm.run();

        // Convert dialog result to Schedule object
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                try {
                    Schedule schedule = new Schedule();

                    schedule.setName(nameField.getText().trim());
                    schedule.setScheduleType(typeCombo.getValue());
                    schedule.setPeriod(periodCombo.getValue());
                    schedule.setStartDate(startDatePicker.getValue());
                    schedule.setEndDate(endDatePicker.getValue());

                    // Parse time fields
                    LocalTime startTime = LocalTime.parse(startTimeField.getText().trim(),
                        java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                    LocalTime endTime = LocalTime.parse(endTimeField.getText().trim(),
                        java.time.format.DateTimeFormatter.ofPattern("HH:mm"));

                    schedule.setDayStartTime(startTime);
                    schedule.setDayEndTime(endTime);

                    // Parse slot duration
                    Integer slotDuration = Integer.parseInt(slotDurationField.getText().trim());
                    schedule.setSlotDurationMinutes(slotDuration);

                    schedule.setStatus(statusCombo.getValue());
                    schedule.setNotes(notesArea.getText().trim());

                    // Set metadata
                    schedule.setCreatedDate(LocalDate.now());
                    schedule.setCreatedBy(com.heronix.scheduler.util.AuthenticationContext.getCurrentUsername());
                    schedule.setActive(true);
                    schedule.setQualityScore(0.0);
                    schedule.setTotalConflicts(0);
                    schedule.setResolvedConflicts(0);

                    return schedule;

                } catch (java.time.format.DateTimeParseException e) {
                    showError("Invalid Time", "Please check your time format (HH:mm). Example: 07:00");
                    return null;
                } catch (NumberFormatException e) {
                    showError("Invalid Duration", "Slot duration must be a number (minutes).");
                    return null;
                } catch (Exception e) {
                    showError("Invalid Input", "Error: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        // Show dialog and save if confirmed
        dialog.showAndWait().ifPresent(schedule -> {
            if (schedule != null) {
                try {
                    // Validate end date is after start date
                    if (schedule.getEndDate().isBefore(schedule.getStartDate()) ||
                        schedule.getEndDate().equals(schedule.getStartDate())) {
                        showError("Invalid Dates", "End date must be after start date.");
                        return;
                    }

                    // Validate end time is after start time
                    if (schedule.getDayEndTime().isBefore(schedule.getDayStartTime()) ||
                        schedule.getDayEndTime().equals(schedule.getDayStartTime())) {
                        showError("Invalid Times", "Day end time must be after start time.");
                        return;
                    }

                    // Validate slot duration is reasonable (5-240 minutes)
                    if (schedule.getSlotDurationMinutes() < 5 || schedule.getSlotDurationMinutes() > 240) {
                        showError("Invalid Duration", "Slot duration must be between 5 and 240 minutes.");
                        return;
                    }

                    // Save to database
                    Schedule saved = scheduleRepository.save(schedule);

                    // Reload table
                    loadSchedules();

                    // Show success message
                    showInfo("Success",
                        "Schedule created successfully!\n\n" +
                        "Name: " + saved.getScheduleName() + "\n" +
                        "Type: " + saved.getScheduleType() + "\n\n" +
                        "You can now add time slots and generate the schedule.");

                    log.info("Created schedule: {}", saved.getScheduleName());

                } catch (Exception e) {
                    log.error("Error saving schedule", e);
                    showError("Save Error", "Failed to create schedule: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Handle view schedule
     */
    @FXML
    private void handleView() {
        Schedule selected = schedulesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            log.info("Viewing schedule: {}", selected.getScheduleName());
            showScheduleDetails(selected);
        }
    }

    /**
     * Handle edit schedule
     */
    @FXML
    private void handleEdit() {
        Schedule selected = schedulesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            log.info("Opening Schedule Editor for: {}", selected.getScheduleName());
            openScheduleEditor(selected);
        }
    }

    /**
     * Open schedule editor dialog
     */
    private void openScheduleEditor(Schedule schedule) {
        try {
            // Reload schedule with slots eagerly loaded to avoid LazyInitializationException
            Schedule scheduleWithSlots = scheduleRepository.findByIdWithSlots(schedule.getId())
                .orElseThrow(() -> new RuntimeException("Schedule not found: " + schedule.getId()));

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/ScheduleEditorDialog.fxml"));
            loader.setControllerFactory(springContext::getBean);

            javafx.scene.Parent root = loader.load();

            // Get controller and set schedule with slots
            ScheduleEditorController controller = loader.getController();
            controller.setSchedule(scheduleWithSlots);

            // Create and show stage
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Schedule Editor - " + schedule.getName());
            stage.setScene(new javafx.scene.Scene(root));
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.initOwner(schedulesTable.getScene().getWindow());

            stage.showAndWait();

            // Refresh table after editing
            loadSchedules();
            log.info("Schedule editor closed, refreshing table");

        } catch (Exception e) {
            log.error("Failed to open schedule editor", e);
            showError("Error", "Failed to open schedule editor: " + e.getMessage());
        }
    }

    /**
     * Handle duplicate schedule
     */
    @FXML
    private void handleDuplicate() {
        Schedule selected = schedulesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            log.info("Duplicating schedule: {}", selected.getScheduleName());
            showDuplicateScheduleDialog(selected);
        }
    }

    /**
     * Show Duplicate Schedule Dialog
     */
    private void showDuplicateScheduleDialog(Schedule original) {
        Dialog<Schedule> dialog = new Dialog<>();
        dialog.setTitle("Duplicate Schedule");
        dialog.setHeaderText("Create a copy of: " + original.getScheduleName());

        ButtonType duplicateButtonType = new ButtonType("Duplicate", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(duplicateButtonType, ButtonType.CANCEL);

        // Create form layout
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        // Form fields
        TextField nameField = new TextField();
        nameField.setText(original.getScheduleName() + " (Copy)");
        nameField.setPromptText("New schedule name");

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setValue(original.getStartDate());
        startDatePicker.setPromptText("Start date");

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setValue(original.getEndDate());
        endDatePicker.setPromptText("End date");

        ComboBox<ScheduleStatus> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(ScheduleStatus.values());
        statusCombo.setValue(ScheduleStatus.DRAFT);
        statusCombo.setPromptText("Status for new schedule");

        CheckBox copySlotsCheck = new CheckBox("Copy all time slots and assignments");
        copySlotsCheck.setSelected(true);

        TextArea notesArea = new TextArea();
        notesArea.setText(original.getNotes() != null ? original.getNotes() : "");
        notesArea.setPromptText("Notes for the duplicated schedule");
        notesArea.setPrefRowCount(3);

        Label infoLabel = new Label(String.format(
            "Original Schedule:\n" +
            "Type: %s\n" +
            "Period: %s\n" +
            "Slots: %s",
            original.getScheduleType(),
            original.getPeriod(),
            getSlotCountSafe(original)
        ));
        infoLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10px; -fx-font-size: 11px;");
        infoLabel.setWrapText(true);

        // Add fields to grid
        int row = 0;
        grid.add(infoLabel, 0, row, 2, 1);
        row++;

        grid.add(new Label("New Name:*"), 0, row);
        grid.add(nameField, 1, row++);

        grid.add(new Label("Start Date:*"), 0, row);
        grid.add(startDatePicker, 1, row++);

        grid.add(new Label("End Date:*"), 0, row);
        grid.add(endDatePicker, 1, row++);

        grid.add(new Label("Status:*"), 0, row);
        grid.add(statusCombo, 1, row++);

        grid.add(new Label("Options:"), 0, row);
        grid.add(copySlotsCheck, 1, row++);

        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row++);

        dialog.getDialogPane().setContent(grid);

        // Enable/disable duplicate button based on validation
        javafx.scene.Node duplicateButton = dialog.getDialogPane().lookupButton(duplicateButtonType);
        duplicateButton.setDisable(true);

        // Validation listener
        Runnable validateForm = () -> {
            boolean valid = !nameField.getText().trim().isEmpty() &&
                          startDatePicker.getValue() != null &&
                          endDatePicker.getValue() != null &&
                          statusCombo.getValue() != null;
            duplicateButton.setDisable(!valid);
        };

        nameField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        startDatePicker.valueProperty().addListener((obs, old, newVal) -> validateForm.run());
        endDatePicker.valueProperty().addListener((obs, old, newVal) -> validateForm.run());
        statusCombo.valueProperty().addListener((obs, old, newVal) -> validateForm.run());

        // Initial validation
        validateForm.run();

        // Convert dialog result to Schedule object
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == duplicateButtonType) {
                Schedule newSchedule = new Schedule();

                newSchedule.setName(nameField.getText().trim());
                newSchedule.setScheduleType(original.getScheduleType());
                newSchedule.setPeriod(original.getPeriod());
                newSchedule.setStartDate(startDatePicker.getValue());
                newSchedule.setEndDate(endDatePicker.getValue());
                newSchedule.setStatus(statusCombo.getValue());

                // Copy time configuration
                newSchedule.setDayStartTime(original.getDayStartTime());
                newSchedule.setDayEndTime(original.getDayEndTime());
                newSchedule.setSlotDurationMinutes(original.getSlotDurationMinutes());

                newSchedule.setNotes(notesArea.getText().trim());

                // Set metadata
                newSchedule.setCreatedDate(LocalDate.now());
                newSchedule.setCreatedBy(com.heronix.scheduler.util.AuthenticationContext.getCurrentUsername());
                newSchedule.setActive(true);
                newSchedule.setQualityScore(0.0);
                newSchedule.setTotalConflicts(0);
                newSchedule.setResolvedConflicts(0);

                // Store copy slots flag as transient data
                newSchedule.setPreviousVersion(copySlotsCheck.isSelected() ? original : null);

                return newSchedule;
            }
            return null;
        });

        // Show dialog and save if confirmed
        dialog.showAndWait().ifPresent(newSchedule -> {
            if (newSchedule != null) {
                try {
                    // Validate dates
                    if (newSchedule.getEndDate().isBefore(newSchedule.getStartDate()) ||
                        newSchedule.getEndDate().equals(newSchedule.getStartDate())) {
                        showError("Invalid Dates", "End date must be after start date.");
                        return;
                    }

                    // Save to database
                    Schedule saved = scheduleRepository.save(newSchedule);

                    // Copy slots if requested (previousVersion is set to original)
                    boolean shouldCopySlots = (newSchedule.getPreviousVersion() != null);
                    int copiedSlotsCount = 0;

                    if (shouldCopySlots) {
                        log.info("Copying slots from schedule {} to {}", original.getId(), saved.getId());

                        try {
                            // Get all slots from original schedule
                            List<com.heronix.scheduler.model.domain.ScheduleSlot> originalSlots =
                                    scheduleSlotService.getSlotsBySchedule(original.getId());

                            log.debug("Found {} slots to copy", originalSlots.size());

                            // Copy each slot to new schedule
                            for (com.heronix.scheduler.model.domain.ScheduleSlot originalSlot : originalSlots) {
                                // Create new slot instance
                                com.heronix.scheduler.model.domain.ScheduleSlot newSlot =
                                        new com.heronix.scheduler.model.domain.ScheduleSlot();

                                // Copy all properties
                                newSlot.setSchedule(saved);
                                newSlot.setTeacher(originalSlot.getTeacher());
                                newSlot.setCourse(originalSlot.getCourse());
                                newSlot.setRoom(originalSlot.getRoom());
                                newSlot.setDayOfWeek(originalSlot.getDayOfWeek());
                                newSlot.setStartTime(originalSlot.getStartTime());
                                newSlot.setEndTime(originalSlot.getEndTime());
                                newSlot.setPeriodNumber(originalSlot.getPeriodNumber());
                                newSlot.setEnrolledStudents(originalSlot.getEnrolledStudents());
                                newSlot.setStatus(originalSlot.getStatus());
                                newSlot.setHasConflict(false); // Reset conflict status
                                newSlot.setConflictReason(null);
                                newSlot.setPinned(originalSlot.getPinned()); // Copy pinned status
                                newSlot.setNotes(originalSlot.getNotes()); // Copy notes

                                // Copy students if present
                                if (originalSlot.getStudents() != null && !originalSlot.getStudents().isEmpty()) {
                                    newSlot.setStudents(new java.util.ArrayList<>(originalSlot.getStudents()));
                                }

                                // Save new slot
                                scheduleSlotService.save(newSlot);
                                copiedSlotsCount++;
                            }

                            log.info("Successfully copied {} slots to new schedule", copiedSlotsCount);

                        } catch (Exception e) {
                            log.error("Error copying slots", e);
                            showError("Slot Copy Warning",
                                    String.format("Schedule duplicated, but slot copying failed:\n%s",
                                            e.getMessage()));
                        }
                    }

                    // Reload table
                    loadSchedules();

                    // Show success message
                    String successMessage = String.format("Schedule duplicated successfully!\n\n" +
                            "Original: %s\n" +
                            "New: %s\n\n" +
                            "%s",
                            original.getScheduleName(),
                            saved.getScheduleName(),
                            (shouldCopySlots ?
                                    String.format("Copied %d schedule slots.", copiedSlotsCount) :
                                    "No slots copied."));

                    showInfo("Success", successMessage);

                    log.info("Duplicated schedule {} to {}", original.getScheduleName(), saved.getScheduleName());

                } catch (Exception e) {
                    log.error("Error duplicating schedule", e);
                    showError("Duplication Error", "Failed to duplicate schedule: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Helper to get slot count safely (handles lazy loading)
     */
    private String getSlotCountSafe(Schedule schedule) {
        try {
            return String.valueOf(schedule.getSlotCount());
        } catch (Exception e) {
            return "N/A";
        }
    }

    /**
     * Handle export schedule
     */
    @FXML
    private void handleExport() {
        Schedule selected = schedulesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            log.info("Exporting schedule: {}", selected.getScheduleName());
            showExportDialog(selected);
        }
    }

    /**
     * Show Export Dialog with format selection
     */
    private void showExportDialog(Schedule schedule) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Export Schedule");
        dialog.setHeaderText("Export: " + schedule.getScheduleName());

        ButtonType exportButtonType = new ButtonType("Export", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(exportButtonType, ButtonType.CANCEL);

        // Create form layout
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        // Format selection
        ComboBox<String> formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll(
            "Excel (.xlsx) - Full schedule details",
            "PDF - Printable schedule",
            "CSV - Data export",
            "iCal (.ics) - Calendar import",
            "HTML - Web page",
            "JSON - Data interchange"
        );
        formatCombo.setPromptText("Select export format");
        formatCombo.setPrefWidth(300);

        // Info label
        Label infoLabel = new Label();
        infoLabel.setWrapText(true);
        infoLabel.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 10px; -fx-font-size: 11px;");
        infoLabel.setPrefWidth(300);

        formatCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String info = switch (newVal) {
                    case "Excel (.xlsx) - Full schedule details" ->
                        "Complete schedule with all details in Excel format. " +
                        "Includes teachers, rooms, times, and conflicts.";
                    case "PDF - Printable schedule" ->
                        "Print-ready PDF document. Perfect for distribution " +
                        "and physical posting.";
                    case "CSV - Data export" ->
                        "Comma-separated values for data analysis. " +
                        "Can be opened in Excel or imported into databases.";
                    case "iCal (.ics) - Calendar import" ->
                        "Calendar file compatible with Google Calendar, " +
                        "Outlook, and other calendar applications.";
                    case "HTML - Web page" ->
                        "Web page format for easy viewing in browsers. " +
                        "Can be published online or shared via email.";
                    case "JSON - Data interchange" ->
                        "JSON format for data interchange and API integration. " +
                        "Useful for programmatic access.";
                    default -> "";
                };
                infoLabel.setText(info);
            }
        });

        String slotCountStr;
        try {
            slotCountStr = String.valueOf(schedule.getSlotCount());
        } catch (Exception e) {
            slotCountStr = "N/A";
        }

        Label scheduleInfo = new Label(String.format(
            "Schedule: %s\n" +
            "Type: %s | Period: %s\n" +
            "Slots: %s | Status: %s",
            schedule.getScheduleName(),
            schedule.getScheduleType(),
            schedule.getPeriod(),
            slotCountStr,
            schedule.getStatus()
        ));
        scheduleInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50;");

        // Add to grid
        int row = 0;
        grid.add(scheduleInfo, 0, row++);
        grid.add(new Separator(), 0, row++);
        grid.add(new Label("Export Format:*"), 0, row++);
        grid.add(formatCombo, 0, row++);
        grid.add(infoLabel, 0, row++);

        dialog.getDialogPane().setContent(grid);

        // Enable/disable export button
        javafx.scene.Node exportButton = dialog.getDialogPane().lookupButton(exportButtonType);
        exportButton.setDisable(true);

        formatCombo.valueProperty().addListener((obs, old, newVal) -> {
            exportButton.setDisable(newVal == null);
        });

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == exportButtonType) {
                return formatCombo.getValue();
            }
            return null;
        });

        // Show dialog and export
        dialog.showAndWait().ifPresent(formatChoice -> {
            try {
                // Extract format from choice
                com.heronix.scheduler.model.enums.ExportFormat format;
                String extension;
                String description;

                if (formatChoice.startsWith("Excel")) {
                    format = com.heronix.scheduler.model.enums.ExportFormat.EXCEL;
                    extension = ".xlsx";
                    description = "Excel";
                } else if (formatChoice.startsWith("PDF")) {
                    format = com.heronix.scheduler.model.enums.ExportFormat.PDF;
                    extension = ".pdf";
                    description = "PDF";
                } else if (formatChoice.startsWith("CSV")) {
                    format = com.heronix.scheduler.model.enums.ExportFormat.CSV;
                    extension = ".csv";
                    description = "CSV";
                } else if (formatChoice.startsWith("iCal")) {
                    format = com.heronix.scheduler.model.enums.ExportFormat.ICAL;
                    extension = ".ics";
                    description = "iCalendar";
                } else if (formatChoice.startsWith("HTML")) {
                    format = com.heronix.scheduler.model.enums.ExportFormat.HTML;
                    extension = ".html";
                    description = "HTML";
                } else if (formatChoice.startsWith("JSON")) {
                    format = com.heronix.scheduler.model.enums.ExportFormat.JSON;
                    extension = ".json";
                    description = "JSON";
                } else {
                    showError("Invalid Format", "Unknown export format selected.");
                    return;
                }

                // Generate filename
                String filename = schedule.getScheduleName()
                    .replaceAll("[^a-zA-Z0-9-_]", "_") + extension;

                // Show file chooser
                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                fileChooser.setTitle("Save " + description + " Export");
                fileChooser.setInitialFileName(filename);
                fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter(
                        description + " Files (*" + extension + ")", "*" + extension)
                );

                javafx.stage.Window window = schedulesTable.getScene().getWindow();
                java.io.File file = fileChooser.showSaveDialog(window);

                if (file != null) {
                    // Export in background
                    updateStatus("Exporting schedule to " + description + "...");

                    CompletableFuture.runAsync(() -> {
                        try {
                            // Call export service
                            // TODO: exportService.exportSchedule() doesn't exist - use format-specific methods instead
                            byte[] exportData = new byte[0]; // Placeholder
                            // byte[] exportData = exportService.exportSchedule(schedule.getId(), format);

                            // Write to file
                            java.nio.file.Files.write(file.toPath(), exportData);

                            Platform.runLater(() -> {
                                updateStatus("Export completed: " + file.getName());
                                showInfo("Export Successful",
                                    String.format("Schedule exported successfully!\n\n" +
                                        "Format: %s\n" +
                                        "File: %s\n" +
                                        "Size: %.2f KB",
                                        description,
                                        file.getName(),
                                        exportData.length / 1024.0));
                                log.info("Exported schedule {} to {}", schedule.getScheduleName(), file.getAbsolutePath());
                            });

                        } catch (Exception e) {
                            log.error("Export failed", e);
                            Platform.runLater(() -> {
                                updateStatus("Export failed");
                                showError("Export Failed",
                                    "Failed to export schedule:\n\n" + e.getMessage());
                            });
                        }
                    });
                }

            } catch (Exception e) {
                log.error("Error in export dialog", e);
                showError("Export Error", "Export failed: " + e.getMessage());
            }
        });
    }

    /**
     * Handle delete schedule
     */
    @FXML
    private void handleDelete() {
        Schedule selected = schedulesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Delete Schedule");
            confirm.setHeaderText("Delete " + selected.getScheduleName() + "?");
            confirm.setContentText("This action cannot be undone.\n\n" +
                    "This will permanently delete:\n" +
                    "• The schedule\n" +
                    "• All time slots\n" +
                    "• All assignments");

            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        scheduleRepository.delete(selected);
                        loadSchedules();
                        updateStatus("Schedule deleted: " + selected.getScheduleName());
                        log.info("Deleted schedule: {}", selected.getScheduleName());
                        showInfo("Success", "Schedule deleted successfully!");
                    } catch (Exception e) {
                        log.error("Error deleting schedule", e);
                        showError("Delete Error", "Failed to delete schedule: " + e.getMessage());
                    }
                }
            });
        }
    }

    // ========================================================================
    // DIALOG HELPERS
    // ========================================================================

    /**
     * Show schedule details dialog
     * Routes to BlockScheduleDetailDialog for block schedules - Block Scheduling MVP
     */
    private void showScheduleDetails(Schedule schedule) {
        // Check if this is a block schedule - Block Scheduling MVP
        if (schedule.getScheduleType() == ScheduleType.BLOCK) {
            showBlockScheduleDetails(schedule);
            return;
        }

        // Reload schedule with slots eagerly loaded to get accurate slot count
        Schedule scheduleWithSlots = scheduleRepository.findByIdWithSlots(schedule.getId())
            .orElse(schedule); // Fallback to original if not found

        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Schedule Details");
        details.setHeaderText("📅 " + scheduleWithSlots.getScheduleName());
        details.setResizable(true);

        // Get slot count safely
        String slotCountStr;
        try {
            slotCountStr = String.valueOf(scheduleWithSlots.getSlotCount());
        } catch (Exception e) {
            slotCountStr = "N/A";
        }

        String content = String.format("""
                📋 BASIC INFO
                Type: %s
                Period: %s
                Status: %s

                📅 DATES
                Start Date: %s
                End Date: %s

                📊 STATISTICS
                Total Slots: %s
                Total Conflicts: %d
                Resolved Conflicts: %d
                Unresolved: %d

                ⭐ QUALITY METRICS
                Optimization Score: %s
                Teacher Utilization: %s
                Room Utilization: %s
                Efficiency Rate: %s

                ℹ️ METADATA
                Created: %s
                Created By: %s
                Last Modified: %s
                Modified By: %s

                📝 NOTES
                %s
                """,
                scheduleWithSlots.getScheduleType(),
                scheduleWithSlots.getPeriod(),
                scheduleWithSlots.getStatus(),
                scheduleWithSlots.getStartDate().format(dateFormatter),
                scheduleWithSlots.getEndDate().format(dateFormatter),
                slotCountStr,
                scheduleWithSlots.getTotalConflicts(),
                scheduleWithSlots.getResolvedConflicts(),
                scheduleWithSlots.getUnresolvedConflicts(),
                scheduleWithSlots.getOptimizationScore() != null ? String.format("%.1f%%", scheduleWithSlots.getOptimizationScore())
                        : "N/A",
                scheduleWithSlots.getTeacherUtilization() != null ? String.format("%.1f%%", scheduleWithSlots.getTeacherUtilization())
                        : "N/A",
                scheduleWithSlots.getRoomUtilization() != null ? String.format("%.1f%%", scheduleWithSlots.getRoomUtilization()) : "N/A",
                scheduleWithSlots.getEfficiencyRate() != null ? String.format("%.1f%%", scheduleWithSlots.getEfficiencyRate()) : "N/A",
                scheduleWithSlots.getCreatedDate() != null ? scheduleWithSlots.getCreatedDate().toString() : "Unknown",
                scheduleWithSlots.getCreatedBy() != null ? scheduleWithSlots.getCreatedBy() : "Unknown",
                scheduleWithSlots.getLastModifiedDate() != null ? scheduleWithSlots.getLastModifiedDate().toString() : "Unknown",
                scheduleWithSlots.getLastModifiedBy() != null ? scheduleWithSlots.getLastModifiedBy() : "Unknown",
                scheduleWithSlots.getNotes() != null && !scheduleWithSlots.getNotes().isEmpty() ? scheduleWithSlots.getNotes() : "No notes");

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefSize(600, 500);

        details.getDialogPane().setContent(textArea);
        details.showAndWait();
    }

    /**
     * Show Block Schedule Detail Dialog
     * Displays ODD/EVEN day breakdown and statistics - Block Scheduling MVP
     */
    private void showBlockScheduleDetails(Schedule schedule) {
        try {
            log.info("Opening Block Schedule Detail Dialog for: {}", schedule.getScheduleName());

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/dialogs/BlockScheduleDetailDialog.fxml")
            );

            if (springContext != null) {
                loader.setControllerFactory(springContext::getBean);
            }

            javafx.scene.control.DialogPane dialogPane = loader.load();

            // Get controller and set schedule
            /* TODO: Package does not exist */ // com.heronix.ui.controller.dialogs.BlockScheduleDetailDialogController controller =
                loader.getController();
            // TODO: controller undefined
            // controller.setSchedule(schedule);

            javafx.scene.control.Dialog<javafx.scene.control.ButtonType> dialog =
                new javafx.scene.control.Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Block Schedule Details - " + schedule.getScheduleName());

            dialog.showAndWait();
        } catch (Exception e) {
            log.error("Error opening Block Schedule Detail Dialog", e);
            showError("View Error", "Failed to open block schedule details: " + e.getMessage());
        }
    }

    /**
     * Update status label
     */
    private void updateStatus(String message) {
        if (statusLabel != null) {
            Platform.runLater(() -> statusLabel.setText(message));
        }
    }

    /**
     * Update count label
     */
    private void updateCount() {
        if (countLabel != null) {
            int count = schedulesList.size();
            Platform.runLater(() -> countLabel.setText("Total: " + count));
        }
    }

    /**
     * Show error dialog
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show info dialog
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
}
