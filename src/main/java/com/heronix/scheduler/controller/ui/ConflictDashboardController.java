package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.ConflictSeverity;
import com.heronix.scheduler.model.enums.ConflictType;
import com.heronix.scheduler.service.ConflictDetectorService;
import com.heronix.scheduler.service.ConflictResolverService;
import com.heronix.scheduler.service.ScheduleService;
import com.heronix.scheduler.service.UserService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Conflict Dashboard Controller
 * Manages the conflict detection and resolution UI
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/ConflictDashboardController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7B - Conflict Detection & Resolution
 */
@Component
@Slf4j
public class ConflictDashboardController {

    // ========================================================================
    // SERVICES
    // ========================================================================

    @Autowired
    private ConflictDetectorService conflictDetectorService;

    @Autowired
    private ConflictResolverService conflictResolverService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private UserService userService;

    @Autowired(required = false)
    private com.heronix.scheduler.service.ConflictResolutionSuggestionService aiSuggestionService;

    // ========================================================================
    // FXML COMPONENTS - TOP SECTION
    // ========================================================================

    @FXML private ComboBox<Schedule> scheduleComboBox;
    @FXML private Button refreshButton;
    @FXML private Button autoResolveButton;
    @FXML private javafx.scene.control.CheckBox autoRefreshCheckBox;

    // Statistics Labels
    @FXML private Label totalConflictsLabel;
    @FXML private Label criticalCountLabel;
    @FXML private Label highCountLabel;
    @FXML private Label mediumCountLabel;
    @FXML private Label lowCountLabel;
    @FXML private Label infoCountLabel;

    // Filter Components
    @FXML private ComboBox<ConflictSeverity> severityFilterComboBox;
    @FXML private ComboBox<ConflictType> typeFilterComboBox;
    @FXML private ComboBox<ConflictType.ConflictCategory> categoryFilterComboBox;
    @FXML private ComboBox<String> statusFilterComboBox;
    @FXML private TextField searchField;

    // ========================================================================
    // FXML COMPONENTS - CENTER SECTION
    // ========================================================================

    // Conflicts Table
    @FXML private TableView<Conflict> conflictsTable;
    @FXML private TableColumn<Conflict, String> severityColumn;
    @FXML private TableColumn<Conflict, String> typeColumn;
    @FXML private TableColumn<Conflict, String> titleColumn;
    @FXML private TableColumn<Conflict, String> detectedAtColumn;
    @FXML private TableColumn<Conflict, String> statusColumn;

    // Action Buttons
    @FXML private Button resolveButton;
    @FXML private Button ignoreButton;
    @FXML private Button viewDetailsButton;
    @FXML private Button exportButton;

    // Details Pane
    @FXML private VBox detailsPane;
    @FXML private Label detailSeverityLabel;
    @FXML private Label detailTypeLabel;
    @FXML private Label detailCategoryLabel;
    @FXML private Label detailDetectedAtLabel;
    @FXML private Label detailStatusLabel;
    @FXML private TextArea detailDescriptionArea;

    // Affected Entities
    @FXML private ListView<String> affectedTeachersListView;
    @FXML private ListView<String> affectedStudentsListView;
    @FXML private ListView<String> affectedRoomsListView;
    @FXML private ListView<String> affectedCoursesListView;

    // Resolution Suggestions (Legacy)
    @FXML private ListView<ConflictResolverService.ResolutionSuggestion> resolutionSuggestionsListView;
    @FXML private Button applySuggestionButton;
    @FXML private Button rejectSuggestionButton;

    // AI-Powered Resolution Suggestions
    @FXML private ListView<com.heronix.scheduler.model.dto.ConflictResolutionSuggestion> aiSuggestionsListView;
    @FXML private Label aiConfidenceLabel;
    @FXML private Label aiImpactLabel;
    @FXML private Label aiSuccessProbLabel;
    @FXML private TextArea aiExplanationArea;
    @FXML private Button applyAISuggestionButton;
    @FXML private Button viewAIDetailsButton;
    @FXML private Label priorityScoreLabel;
    @FXML private ProgressBar priorityScoreBar;

    // Manual Resolution
    @FXML private TextArea resolutionNotesArea;
    @FXML private Button markResolvedButton;
    @FXML private Button markIgnoredButton;

    // ========================================================================
    // FXML COMPONENTS - BOTTOM SECTION
    // ========================================================================

    @FXML private Label statusLabel;
    @FXML private Label lastRefreshLabel;
    @FXML private ProgressIndicator progressIndicator;

    // ========================================================================
    // DATA MODELS
    // ========================================================================

    private final ObservableList<Conflict> allConflicts = FXCollections.observableArrayList();
    private FilteredList<Conflict> filteredConflicts;
    private Conflict selectedConflict;
    private LocalDateTime lastRefreshTime;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // Auto-refresh settings
    private javafx.animation.Timeline autoRefreshTimeline;
    private static final int AUTO_REFRESH_INTERVAL_SECONDS = 30;
    private boolean autoRefreshEnabled = false;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing Conflict Dashboard Controller");

        // Initialize table columns
        initializeTableColumns();

        // Initialize filter components
        initializeFilters();

        // Initialize filtered list
        filteredConflicts = new FilteredList<>(allConflicts, p -> true);
        conflictsTable.setItems(filteredConflicts);

        // Set up selection listener
        conflictsTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> onConflictSelected(newSelection)
        );

        // Load schedules
        loadSchedules();

        // Set up schedule selection listener
        scheduleComboBox.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSchedule, newSchedule) -> {
                if (newSchedule != null) {
                    loadConflictsForSchedule(newSchedule);
                }
            }
        );

        // Set up search listener
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());

        // Set up auto-refresh
        initializeAutoRefresh();

        log.info("Conflict Dashboard Controller initialized successfully");
    }

    private void initializeTableColumns() {
        severityColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getSeverity().getIcon() + " " +
                                   cellData.getValue().getSeverity().getDisplayName()));

        typeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getConflictType().getDisplayName()));

        titleColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getTitle()));

        detectedAtColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getDetectedAt().format(DATE_TIME_FORMATTER)));

        statusColumn.setCellValueFactory(cellData -> {
            Conflict conflict = cellData.getValue();
            String status = conflict.getIsResolved() ? "✅ Resolved" :
                           conflict.getIsIgnored() ? "🚫 Ignored" : "⚠️ Active";
            return new SimpleStringProperty(status);
        });

        // Add row colors based on severity
        conflictsTable.setRowFactory(tv -> new TableRow<Conflict>() {
            @Override
            protected void updateItem(Conflict conflict, boolean empty) {
                super.updateItem(conflict, empty);
                if (empty || conflict == null) {
                    setStyle("");
                } else {
                    String color = switch (conflict.getSeverity()) {
                        case CRITICAL -> "-fx-background-color: #ffebee;";
                        case HIGH -> "-fx-background-color: #fff3e0;";
                        case MEDIUM -> "-fx-background-color: #fffde7;";
                        case LOW -> "-fx-background-color: #e3f2fd;";
                        case INFO -> "-fx-background-color: #e8f5e9;";
                    };
                    setStyle(color);
                }
            }
        });
    }

    private void initializeFilters() {
        // Severity filter
        severityFilterComboBox.setItems(FXCollections.observableArrayList(ConflictSeverity.values()));
        severityFilterComboBox.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldValue, newValue) -> applyFilters()
        );

        // Type filter
        typeFilterComboBox.setItems(FXCollections.observableArrayList(ConflictType.values()));
        typeFilterComboBox.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldValue, newValue) -> applyFilters()
        );

        // Category filter
        categoryFilterComboBox.setItems(FXCollections.observableArrayList(ConflictType.ConflictCategory.values()));
        categoryFilterComboBox.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldValue, newValue) -> applyFilters()
        );

        // Status filter
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
            "Active Only", "Resolved Only", "Ignored Only", "All Conflicts"
        );
        statusFilterComboBox.setItems(statusOptions);
        statusFilterComboBox.getSelectionModel().select("Active Only");
        statusFilterComboBox.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldValue, newValue) -> applyFilters()
        );
    }

    /**
     * Initialize auto-refresh functionality
     */
    private void initializeAutoRefresh() {
        if (autoRefreshCheckBox != null) {
            // Set up checkbox listener
            autoRefreshCheckBox.selectedProperty().addListener((obs, oldValue, newValue) -> {
                autoRefreshEnabled = newValue;
                if (newValue) {
                    startAutoRefresh();
                } else {
                    stopAutoRefresh();
                }
            });

            // Initialize as unchecked
            autoRefreshCheckBox.setSelected(false);
            autoRefreshCheckBox.setText("Auto-Refresh (every " + AUTO_REFRESH_INTERVAL_SECONDS + "s)");
        }
    }

    /**
     * Start auto-refresh timer
     */
    private void startAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
        }

        autoRefreshTimeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(
                javafx.util.Duration.seconds(AUTO_REFRESH_INTERVAL_SECONDS),
                event -> {
                    if (autoRefreshEnabled) {
                        Schedule selectedSchedule = scheduleComboBox.getValue();
                        if (selectedSchedule != null) {
                            log.debug("Auto-refreshing conflict dashboard");
                            loadConflictsForSchedule(selectedSchedule);
                        }
                    }
                }
            )
        );

        autoRefreshTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        autoRefreshTimeline.play();

        log.info("Auto-refresh started (interval: {}s)", AUTO_REFRESH_INTERVAL_SECONDS);
        updateStatus("Auto-refresh enabled (every " + AUTO_REFRESH_INTERVAL_SECONDS + " seconds)");
    }

    /**
     * Stop auto-refresh timer
     */
    private void stopAutoRefresh() {
        if (autoRefreshTimeline != null) {
            autoRefreshTimeline.stop();
            autoRefreshTimeline = null;
        }

        log.info("Auto-refresh stopped");
        updateStatus("Auto-refresh disabled");
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    private void loadSchedules() {
        Task<List<Schedule>> task = new Task<>() {
            @Override
            protected List<Schedule> call() {
                return scheduleService.getAllSchedules();
            }
        };

        task.setOnSucceeded(event -> {
            List<Schedule> schedules = task.getValue();
            scheduleComboBox.setItems(FXCollections.observableArrayList(schedules));

            // Auto-select first schedule if available
            if (!schedules.isEmpty()) {
                scheduleComboBox.getSelectionModel().selectFirst();
            }
        });

        task.setOnFailed(event -> {
            log.error("Failed to load schedules", task.getException());
            showError("Failed to load schedules", task.getException().getMessage());
        });

        new Thread(task).start();
    }

    private void loadConflictsForSchedule(Schedule schedule) {
        if (schedule == null) {
            return;
        }

        setLoading(true);
        updateStatus("Detecting conflicts for " + schedule.getScheduleName() + "...");

        Task<List<Conflict>> task = new Task<>() {
            @Override
            protected List<Conflict> call() {
                return conflictDetectorService.detectAllConflicts(schedule);
            }
        };

        task.setOnSucceeded(event -> {
            List<Conflict> conflicts = task.getValue();
            allConflicts.setAll(conflicts);
            applyFilters();
            updateStatistics();
            lastRefreshTime = LocalDateTime.now();
            lastRefreshLabel.setText("Last refresh: " + lastRefreshTime.format(DATE_TIME_FORMATTER));
            updateStatus(String.format("Loaded %d conflicts", conflicts.size()));
            setLoading(false);

            log.info("Loaded {} conflicts for schedule {}", conflicts.size(), schedule.getScheduleName());
        });

        task.setOnFailed(event -> {
            log.error("Failed to detect conflicts", task.getException());
            showError("Failed to detect conflicts", task.getException().getMessage());
            setLoading(false);
        });

        new Thread(task).start();
    }

    // ========================================================================
    // FILTERING
    // ========================================================================

    private void applyFilters() {
        filteredConflicts.setPredicate(conflict -> {
            // Severity filter
            ConflictSeverity severityFilter = severityFilterComboBox.getValue();
            if (severityFilter != null && conflict.getSeverity() != severityFilter) {
                return false;
            }

            // Type filter
            ConflictType typeFilter = typeFilterComboBox.getValue();
            if (typeFilter != null && conflict.getConflictType() != typeFilter) {
                return false;
            }

            // Category filter
            ConflictType.ConflictCategory categoryFilter = categoryFilterComboBox.getValue();
            if (categoryFilter != null && conflict.getCategory() != categoryFilter) {
                return false;
            }

            // Status filter
            String statusFilter = statusFilterComboBox.getValue();
            if (statusFilter != null) {
                switch (statusFilter) {
                    case "Active Only":
                        if (!conflict.isActive()) return false;
                        break;
                    case "Resolved Only":
                        if (!conflict.getIsResolved()) return false;
                        break;
                    case "Ignored Only":
                        if (!conflict.getIsIgnored()) return false;
                        break;
                }
            }

            // Search filter
            String searchText = searchField.getText();
            if (searchText != null && !searchText.isEmpty()) {
                String lowerSearch = searchText.toLowerCase();
                return conflict.getTitle().toLowerCase().contains(lowerSearch) ||
                       conflict.getDescription().toLowerCase().contains(lowerSearch) ||
                       conflict.getConflictType().getDisplayName().toLowerCase().contains(lowerSearch);
            }

            return true;
        });

        updateStatistics();
    }

    @FXML
    private void handleClearFilters() {
        severityFilterComboBox.getSelectionModel().clearSelection();
        typeFilterComboBox.getSelectionModel().clearSelection();
        categoryFilterComboBox.getSelectionModel().clearSelection();
        statusFilterComboBox.getSelectionModel().select("Active Only");
        searchField.clear();
        applyFilters();
    }

    // ========================================================================
    // STATISTICS
    // ========================================================================

    private void updateStatistics() {
        List<Conflict> displayedConflicts = filteredConflicts.stream().collect(Collectors.toList());

        totalConflictsLabel.setText(String.valueOf(displayedConflicts.size()));

        long critical = displayedConflicts.stream().filter(c -> c.getSeverity() == ConflictSeverity.CRITICAL).count();
        long high = displayedConflicts.stream().filter(c -> c.getSeverity() == ConflictSeverity.HIGH).count();
        long medium = displayedConflicts.stream().filter(c -> c.getSeverity() == ConflictSeverity.MEDIUM).count();
        long low = displayedConflicts.stream().filter(c -> c.getSeverity() == ConflictSeverity.LOW).count();
        long info = displayedConflicts.stream().filter(c -> c.getSeverity() == ConflictSeverity.INFO).count();

        criticalCountLabel.setText(String.valueOf(critical));
        highCountLabel.setText(String.valueOf(high));
        mediumCountLabel.setText(String.valueOf(medium));
        lowCountLabel.setText(String.valueOf(low));
        infoCountLabel.setText(String.valueOf(info));
    }

    // ========================================================================
    // CONFLICT SELECTION
    // ========================================================================

    private void onConflictSelected(Conflict conflict) {
        selectedConflict = conflict;

        if (conflict == null) {
            clearDetailsPane();
            disableActionButtons();
            return;
        }

        enableActionButtons();
        displayConflictDetails(conflict);
        loadResolutionSuggestions(conflict);
        loadAISuggestions(conflict);
        loadPriorityScore(conflict);
    }

    private void displayConflictDetails(Conflict conflict) {
        detailSeverityLabel.setText(conflict.getSeverity().getIcon() + " " + conflict.getSeverity().getDisplayName());
        detailTypeLabel.setText(conflict.getConflictType().getDisplayName());
        detailCategoryLabel.setText(conflict.getCategory().toString());
        detailDetectedAtLabel.setText(conflict.getDetectedAt().format(DATE_TIME_FORMATTER));

        String status = conflict.getIsResolved() ? "✅ Resolved" :
                       conflict.getIsIgnored() ? "🚫 Ignored" : "⚠️ Active";
        detailStatusLabel.setText(status);

        detailDescriptionArea.setText(conflict.getDescription());

        // Load affected entities
        affectedTeachersListView.setItems(FXCollections.observableArrayList(
            conflict.getAffectedTeachers().stream()
                .map(Teacher::getName)
                .collect(Collectors.toList())
        ));

        affectedStudentsListView.setItems(FXCollections.observableArrayList(
            conflict.getAffectedStudents().stream()
                .map(Student::getFullName)
                .collect(Collectors.toList())
        ));

        affectedRoomsListView.setItems(FXCollections.observableArrayList(
            conflict.getAffectedRooms().stream()
                .map(Room::getRoomNumber)
                .collect(Collectors.toList())
        ));

        affectedCoursesListView.setItems(FXCollections.observableArrayList(
            conflict.getAffectedCourses().stream()
                .map(Course::getCourseName)
                .collect(Collectors.toList())
        ));
    }

    private void loadResolutionSuggestions(Conflict conflict) {
        Task<List<ConflictResolverService.ResolutionSuggestion>> task = new Task<>() {
            @Override
            protected List<ConflictResolverService.ResolutionSuggestion> call() {
                return conflictResolverService.getSuggestions(conflict);
            }
        };

        task.setOnSucceeded(event -> {
            List<ConflictResolverService.ResolutionSuggestion> suggestions = task.getValue();
            resolutionSuggestionsListView.setItems(FXCollections.observableArrayList(suggestions));

            // Custom cell factory to display suggestions
            resolutionSuggestionsListView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(ConflictResolverService.ResolutionSuggestion item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(String.format("%s (Confidence: %.0f%%)\n%s",
                            item.getStrategy(),
                            item.getConfidence() * 100,
                            item.getDescription()));
                    }
                }
            });

            // Enable suggestion buttons
            resolutionSuggestionsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSuggestion, newSuggestion) -> {
                    applySuggestionButton.setDisable(newSuggestion == null);
                    rejectSuggestionButton.setDisable(newSuggestion == null);
                }
            );
        });

        task.setOnFailed(event -> {
            log.error("Failed to load resolution suggestions", task.getException());
            resolutionSuggestionsListView.setItems(FXCollections.observableArrayList());
        });

        new Thread(task).start();
    }

    /**
     * Load AI-powered resolution suggestions
     */
    private void loadAISuggestions(Conflict conflict) {
        if (aiSuggestionService == null || aiSuggestionsListView == null) {
            log.debug("AI suggestion service or UI components not available");
            return;
        }

        Task<List<com.heronix.scheduler.model.dto.ConflictResolutionSuggestion>> task = new Task<>() {
            @Override
            protected List<com.heronix.scheduler.model.dto.ConflictResolutionSuggestion> call() {
                return aiSuggestionService.generateSuggestions(conflict);
            }
        };

        task.setOnSucceeded(event -> {
            List<com.heronix.scheduler.model.dto.ConflictResolutionSuggestion> suggestions = task.getValue();
            aiSuggestionsListView.setItems(FXCollections.observableArrayList(suggestions));

            // Custom cell factory to display AI suggestions
            aiSuggestionsListView.setCellFactory(lv -> new ListCell<>() {
                @Override
                protected void updateItem(com.heronix.scheduler.model.dto.ConflictResolutionSuggestion item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        // Format: [Type] Description (Success: X%, Impact: Y)
                        String displayText = String.format("[%s] %s\nSuccess: %d%% | Impact: %d | Confidence: %d%%",
                            item.getType().getDisplayName(),
                            item.getDescription(),
                            item.getSuccessProbability(),
                            item.getImpactScore(),
                            item.getConfidenceLevel());
                        setText(displayText);

                        // Color code by confidence
                        String bgColor = item.getConfidenceLevel() >= 80 ? "#e8f5e9;" :
                                       item.getConfidenceLevel() >= 60 ? "#fff9c4;" :
                                       "#ffebee;";
                        setStyle("-fx-background-color: " + bgColor);
                    }
                }
            });

            // Enable AI suggestion buttons when selection changes
            aiSuggestionsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSuggestion, newSuggestion) -> {
                    if (newSuggestion != null) {
                        displayAISuggestionDetails(newSuggestion);
                        if (applyAISuggestionButton != null) {
                            applyAISuggestionButton.setDisable(false);
                        }
                        if (viewAIDetailsButton != null) {
                            viewAIDetailsButton.setDisable(false);
                        }
                    } else {
                        clearAISuggestionDetails();
                        if (applyAISuggestionButton != null) {
                            applyAISuggestionButton.setDisable(true);
                        }
                        if (viewAIDetailsButton != null) {
                            viewAIDetailsButton.setDisable(true);
                        }
                    }
                }
            );

            log.info("Loaded {} AI suggestions for conflict {}", suggestions.size(), conflict.getId());
        });

        task.setOnFailed(event -> {
            log.error("Failed to load AI suggestions", task.getException());
            if (aiSuggestionsListView != null) {
                aiSuggestionsListView.setItems(FXCollections.observableArrayList());
            }
        });

        new Thread(task).start();
    }

    /**
     * Load ML-based priority score for conflict
     */
    private void loadPriorityScore(Conflict conflict) {
        if (aiSuggestionService == null || priorityScoreLabel == null) {
            log.debug("AI suggestion service or priority UI components not available");
            return;
        }

        Task<com.heronix.scheduler.model.dto.ConflictPriorityScore> task = new Task<>() {
            @Override
            protected com.heronix.scheduler.model.dto.ConflictPriorityScore call() {
                return aiSuggestionService.calculatePriorityScore(conflict);
            }
        };

        task.setOnSucceeded(event -> {
            com.heronix.scheduler.model.dto.ConflictPriorityScore score = task.getValue();
            if (score != null) {
                priorityScoreLabel.setText(String.format("Priority Score: %d/100 (%s)",
                    score.getTotalScore(),
                    score.getPriorityLevel()));

                // Update progress bar if available
                if (priorityScoreBar != null) {
                    priorityScoreBar.setProgress(score.getTotalScore() / 100.0);

                    // Color code the progress bar
                    String barColor = score.getTotalScore() >= 75 ? "-fx-accent: #f44336;" :
                                    score.getTotalScore() >= 50 ? "-fx-accent: #ff9800;" :
                                    "-fx-accent: #4caf50;";
                    priorityScoreBar.setStyle(barColor);
                }

                log.info("Priority score for conflict {}: {}/100", conflict.getId(), score.getTotalScore());
            }
        });

        task.setOnFailed(event -> {
            log.error("Failed to load priority score", task.getException());
            if (priorityScoreLabel != null) {
                priorityScoreLabel.setText("Priority Score: N/A");
            }
        });

        new Thread(task).start();
    }

    /**
     * Display details of selected AI suggestion
     */
    private void displayAISuggestionDetails(com.heronix.scheduler.model.dto.ConflictResolutionSuggestion suggestion) {
        if (suggestion == null) {
            clearAISuggestionDetails();
            return;
        }

        if (aiConfidenceLabel != null) {
            aiConfidenceLabel.setText(String.format("Confidence: %d%%", suggestion.getConfidenceLevel()));
        }

        if (aiImpactLabel != null) {
            aiImpactLabel.setText(String.format("Impact Score: %d", suggestion.getImpactScore()));
        }

        if (aiSuccessProbLabel != null) {
            aiSuccessProbLabel.setText(String.format("Success Probability: %d%%", suggestion.getSuccessProbability()));
        }

        if (aiExplanationArea != null) {
            StringBuilder details = new StringBuilder();
            details.append(suggestion.getExplanation()).append("\n\n");

            if (suggestion.getAffectedEntities() != null && !suggestion.getAffectedEntities().isEmpty()) {
                details.append("Affected Entities:\n");
                for (String entity : suggestion.getAffectedEntities()) {
                    details.append("  - ").append(entity).append("\n");
                }
                details.append("\n");
            }

            if (suggestion.getWarnings() != null && !suggestion.getWarnings().isEmpty()) {
                details.append("Warnings:\n");
                for (String warning : suggestion.getWarnings()) {
                    details.append("  ⚠ ").append(warning).append("\n");
                }
            }

            aiExplanationArea.setText(details.toString());
        }
    }

    /**
     * Clear AI suggestion details
     */
    private void clearAISuggestionDetails() {
        if (aiConfidenceLabel != null) {
            aiConfidenceLabel.setText("Confidence: -");
        }
        if (aiImpactLabel != null) {
            aiImpactLabel.setText("Impact: -");
        }
        if (aiSuccessProbLabel != null) {
            aiSuccessProbLabel.setText("Success: -");
        }
        if (aiExplanationArea != null) {
            aiExplanationArea.clear();
        }
    }

    private void clearDetailsPane() {
        detailSeverityLabel.setText("-");
        detailTypeLabel.setText("-");
        detailCategoryLabel.setText("-");
        detailDetectedAtLabel.setText("-");
        detailStatusLabel.setText("-");
        detailDescriptionArea.clear();

        affectedTeachersListView.setItems(FXCollections.observableArrayList());
        affectedStudentsListView.setItems(FXCollections.observableArrayList());
        affectedRoomsListView.setItems(FXCollections.observableArrayList());
        affectedCoursesListView.setItems(FXCollections.observableArrayList());

        resolutionSuggestionsListView.setItems(FXCollections.observableArrayList());
        resolutionNotesArea.clear();

        // Clear AI suggestions
        if (aiSuggestionsListView != null) {
            aiSuggestionsListView.setItems(FXCollections.observableArrayList());
        }
        clearAISuggestionDetails();
        if (priorityScoreLabel != null) {
            priorityScoreLabel.setText("Priority Score: -");
        }
        if (priorityScoreBar != null) {
            priorityScoreBar.setProgress(0);
        }
    }

    private void enableActionButtons() {
        resolveButton.setDisable(false);
        ignoreButton.setDisable(false);
        viewDetailsButton.setDisable(false);
        markResolvedButton.setDisable(false);
        markIgnoredButton.setDisable(false);
    }

    private void disableActionButtons() {
        resolveButton.setDisable(true);
        ignoreButton.setDisable(true);
        viewDetailsButton.setDisable(true);
        markResolvedButton.setDisable(true);
        markIgnoredButton.setDisable(true);
        applySuggestionButton.setDisable(true);
        rejectSuggestionButton.setDisable(true);
    }

    // ========================================================================
    // ACTION HANDLERS
    // ========================================================================

    @FXML
    private void handleRefresh() {
        Schedule selectedSchedule = scheduleComboBox.getValue();
        if (selectedSchedule != null) {
            loadConflictsForSchedule(selectedSchedule);
        }
    }

    @FXML
    private void handleAutoResolve() {
        Schedule selectedSchedule = scheduleComboBox.getValue();
        if (selectedSchedule == null) {
            showWarning("No Schedule Selected", "Please select a schedule first.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Auto-Resolve Conflicts");
        confirmation.setHeaderText("Automatically resolve conflicts?");
        confirmation.setContentText("This will attempt to automatically resolve all conflicts. Continue?");

        confirmation.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                autoResolveConflicts(selectedSchedule);
            }
        });
    }

    private void autoResolveConflicts(Schedule schedule) {
        setLoading(true);
        updateStatus("Auto-resolving conflicts...");

        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                User currentUser = getCurrentUser();
                return conflictResolverService.autoResolveAll(schedule, currentUser);
            }
        };

        task.setOnSucceeded(event -> {
            int resolvedCount = task.getValue();
            showInfo("Auto-Resolve Complete",
                String.format("Successfully resolved %d conflicts automatically.", resolvedCount));
            loadConflictsForSchedule(schedule);
            setLoading(false);
        });

        task.setOnFailed(event -> {
            log.error("Failed to auto-resolve conflicts", task.getException());
            showError("Auto-Resolve Failed", task.getException().getMessage());
            setLoading(false);
        });

        new Thread(task).start();
    }

    @FXML
    private void handleResolve() {
        if (selectedConflict == null) {
            return;
        }

        ConflictResolverService.ResolutionSuggestion bestSuggestion =
            conflictResolverService.getBestSuggestion(selectedConflict);

        if (bestSuggestion != null) {
            applyResolutionSuggestion(bestSuggestion);
        } else {
            showWarning("No Suggestions", "No automatic resolution available. Please resolve manually.");
        }
    }

    @FXML
    private void handleIgnore() {
        if (selectedConflict == null) {
            return;
        }

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Ignore Conflict");
        dialog.setHeaderText("Ignore this conflict?");
        dialog.setContentText("Reason for ignoring:");

        dialog.showAndWait().ifPresent(reason -> {
            selectedConflict.ignore(reason);
            conflictDetectorService.saveConflicts(List.of(selectedConflict));
            refreshCurrentView();
            showInfo("Conflict Ignored", "Conflict has been marked as ignored.");
        });
    }

    @FXML
    private void handleViewDetails() {
        // Already showing details in the right pane
        // Could open a separate detailed dialog if needed
    }

    @FXML
    private void handleExportReport() {
        log.info("Exporting conflict report...");

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Conflict Report");
        fileChooser.setInitialFileName("conflict_report_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        java.io.File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());

        if (file != null) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file))) {
                // Header
                writer.println("Conflict Report - Generated: " + LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println("Schedule: " + (scheduleComboBox.getValue() != null ?
                    scheduleComboBox.getValue().getScheduleName() : "All"));
                writer.println();

                // Summary
                writer.println("=== SUMMARY ===");
                writer.println("Total Conflicts," + totalConflictsLabel.getText());
                writer.println("Critical," + criticalCountLabel.getText());
                writer.println("High," + highCountLabel.getText());
                writer.println("Medium," + mediumCountLabel.getText());
                writer.println("Low," + lowCountLabel.getText());
                writer.println("Info," + infoCountLabel.getText());
                writer.println();

                // Detail header
                writer.println("=== CONFLICT DETAILS ===");
                writer.println("Severity,Type,Title,Detected At,Status,Description");

                // Export all conflicts in the filtered list
                for (Conflict conflict : conflictsTable.getItems()) {
                    String status = conflict.getIsResolved() != null && conflict.getIsResolved() ? "RESOLVED" :
                                   (conflict.getIsIgnored() != null && conflict.getIsIgnored() ? "IGNORED" : "OPEN");
                    writer.println(String.format("%s,%s,\"%s\",%s,%s,\"%s\"",
                        conflict.getSeverity() != null ? conflict.getSeverity().name() : "",
                        conflict.getConflictType() != null ? conflict.getConflictType().name() : "",
                        escapeCSV(conflict.getTitle()),
                        conflict.getDetectedAt() != null ?
                            conflict.getDetectedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "",
                        status,
                        escapeCSV(conflict.getDescription())
                    ));
                }

                showInfo("Export Complete", "Conflict report exported to:\n" + file.getAbsolutePath());
                log.info("Conflict report exported successfully to: {}", file.getAbsolutePath());

            } catch (java.io.IOException e) {
                log.error("Failed to export conflict report", e);
                showError("Export Failed", "Failed to export report: " + e.getMessage());
            }
        }
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"").replace("\n", " ").replace("\r", "");
    }

    @FXML
    private void handleApplySuggestion() {
        ConflictResolverService.ResolutionSuggestion selectedSuggestion =
            resolutionSuggestionsListView.getSelectionModel().getSelectedItem();

        if (selectedSuggestion != null) {
            applyResolutionSuggestion(selectedSuggestion);
        }
    }

    private void applyResolutionSuggestion(ConflictResolverService.ResolutionSuggestion suggestion) {
        setLoading(true);
        updateStatus("Applying resolution...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                User currentUser = getCurrentUser();
                return conflictResolverService.applyResolution(selectedConflict, suggestion, currentUser);
            }
        };

        task.setOnSucceeded(event -> {
            boolean success = task.getValue();
            if (success) {
                showInfo("Resolution Applied", "Conflict has been resolved successfully.");
                refreshCurrentView();
            } else {
                showWarning("Resolution Failed", "Could not apply the selected resolution.");
            }
            setLoading(false);
        });

        task.setOnFailed(event -> {
            log.error("Failed to apply resolution", task.getException());
            showError("Resolution Failed", task.getException().getMessage());
            setLoading(false);
        });

        new Thread(task).start();
    }

    @FXML
    private void handleRejectSuggestion() {
        resolutionSuggestionsListView.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleMarkResolved() {
        if (selectedConflict == null) {
            return;
        }

        String notes = resolutionNotesArea.getText();
        if (notes == null || notes.trim().isEmpty()) {
            showWarning("Resolution Notes Required", "Please enter notes about how this conflict was resolved.");
            return;
        }

        User currentUser = getCurrentUser();
        selectedConflict.resolve(currentUser, notes);
        conflictDetectorService.saveConflicts(List.of(selectedConflict));

        showInfo("Conflict Resolved", "Conflict has been marked as resolved.");
        refreshCurrentView();
        resolutionNotesArea.clear();
    }

    @FXML
    private void handleMarkIgnored() {
        handleIgnore();
    }

    /**
     * Handle apply AI suggestion button
     */
    @FXML
    private void handleApplyAISuggestion() {
        if (selectedConflict == null || aiSuggestionService == null) {
            return;
        }

        com.heronix.scheduler.model.dto.ConflictResolutionSuggestion selectedSuggestion =
            aiSuggestionsListView.getSelectionModel().getSelectedItem();

        if (selectedSuggestion == null) {
            showWarning("No Suggestion Selected", "Please select an AI suggestion to apply.");
            return;
        }

        // Check if suggestion requires confirmation
        if (selectedSuggestion.isRequiresConfirmation()) {
            Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
            confirmation.setTitle("Confirm AI Suggestion");
            confirmation.setHeaderText("Apply this resolution?");

            StringBuilder content = new StringBuilder();
            content.append(selectedSuggestion.getDescription()).append("\n\n");
            content.append("Confidence: ").append(selectedSuggestion.getConfidenceLevel()).append("%\n");
            content.append("Success Probability: ").append(selectedSuggestion.getSuccessProbability()).append("%\n");
            content.append("Impact Score: ").append(selectedSuggestion.getImpactScore()).append("\n");

            if (selectedSuggestion.getWarnings() != null && !selectedSuggestion.getWarnings().isEmpty()) {
                content.append("\nWarnings:\n");
                for (String warning : selectedSuggestion.getWarnings()) {
                    content.append("  ⚠ ").append(warning).append("\n");
                }
            }

            confirmation.setContentText(content.toString());

            confirmation.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    applyAISuggestion(selectedSuggestion);
                }
            });
        } else {
            applyAISuggestion(selectedSuggestion);
        }
    }

    /**
     * Apply an AI suggestion
     */
    private void applyAISuggestion(com.heronix.scheduler.model.dto.ConflictResolutionSuggestion suggestion) {
        setLoading(true);
        updateStatus("Applying AI suggestion...");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return aiSuggestionService.applySuggestion(selectedConflict, suggestion);
            }
        };

        task.setOnSucceeded(event -> {
            boolean success = task.getValue();
            if (success) {
                showInfo("AI Resolution Applied",
                    "Conflict has been resolved successfully using AI suggestion.");
                refreshCurrentView();
            } else {
                showWarning("AI Resolution Failed",
                    "Could not apply the selected AI suggestion. Please try another suggestion or resolve manually.");
            }
            setLoading(false);
        });

        task.setOnFailed(event -> {
            log.error("Failed to apply AI suggestion", task.getException());
            showError("AI Resolution Failed", task.getException().getMessage());
            setLoading(false);
        });

        new Thread(task).start();
    }

    /**
     * Handle view AI details button
     */
    @FXML
    private void handleViewAIDetails() {
        if (selectedConflict == null || aiSuggestionService == null) {
            return;
        }

        com.heronix.scheduler.model.dto.ConflictResolutionSuggestion selectedSuggestion =
            aiSuggestionsListView.getSelectionModel().getSelectedItem();

        if (selectedSuggestion == null) {
            showWarning("No Suggestion Selected", "Please select an AI suggestion to view details.");
            return;
        }

        // Show detailed information dialog
        Alert detailsDialog = new Alert(Alert.AlertType.INFORMATION);
        detailsDialog.setTitle("AI Suggestion Details");
        detailsDialog.setHeaderText(selectedSuggestion.getType().getDisplayName());

        StringBuilder details = new StringBuilder();
        details.append("Description: ").append(selectedSuggestion.getDescription()).append("\n\n");
        details.append("Explanation: ").append(selectedSuggestion.getExplanation()).append("\n\n");
        details.append("Metrics:\n");
        details.append("  • Confidence Level: ").append(selectedSuggestion.getConfidenceLevel()).append("%\n");
        details.append("  • Success Probability: ").append(selectedSuggestion.getSuccessProbability()).append("%\n");
        details.append("  • Impact Score: ").append(selectedSuggestion.getImpactScore()).append("\n");
        details.append("  • Priority Ranking: ").append(selectedSuggestion.getPriorityRanking()).append("\n");
        details.append("  • Estimated Time: ").append(selectedSuggestion.getEstimatedTimeSeconds()).append(" seconds\n\n");

        if (selectedSuggestion.getAffectedEntities() != null && !selectedSuggestion.getAffectedEntities().isEmpty()) {
            details.append("Affected Entities (").append(selectedSuggestion.getAffectedEntitiesCount()).append("):\n");
            for (String entity : selectedSuggestion.getAffectedEntities()) {
                details.append("  - ").append(entity).append("\n");
            }
            details.append("\n");
        }

        if (selectedSuggestion.getWarnings() != null && !selectedSuggestion.getWarnings().isEmpty()) {
            details.append("Warnings:\n");
            for (String warning : selectedSuggestion.getWarnings()) {
                details.append("  ⚠ ").append(warning).append("\n");
            }
            details.append("\n");
        }

        details.append("Actions:\n");
        if (selectedSuggestion.getActions() != null) {
            for (com.heronix.scheduler.model.dto.ConflictResolutionSuggestion.ResolutionAction action : selectedSuggestion.getActions()) {
                details.append("  → ").append(action.getChangeDescription()).append("\n");
            }
        }

        detailsDialog.setContentText(details.toString());
        detailsDialog.showAndWait();
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Get current user from security context
     * Integrates with authentication service
     */
    private User getCurrentUser() {
        // Package com.heronix.security not available — returns default user
        // try {
        //     // Get current user from security context (returns Optional)
        //     java.util.Optional<User> userOptional = com.heronix.security.SecurityContext.getCurrentUser();
        //     if (userOptional.isPresent()) {
        //         return userOptional.get();
        //     }
        // } catch (Exception e) {
        //     log.warn("Could not get user from security context", e);
        // }

        // Fallback: try to get first admin user from database
        try {
            List<User> users = userService.findAll();
            if (!users.isEmpty()) {
                return users.get(0);
            }
        } catch (Exception e) {
            log.warn("Could not load users from database", e);
        }

        // Final fallback: create a temporary system user object
        User tempUser = new User();
        tempUser.setId(1L);
        tempUser.setUsername("system");
        tempUser.setFullName("System Administrator");
        return tempUser;
    }

    private void refreshCurrentView() {
        Schedule selectedSchedule = scheduleComboBox.getValue();
        if (selectedSchedule != null) {
            loadConflictsForSchedule(selectedSchedule);
        }
    }

    private void setLoading(boolean loading) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(loading);
            refreshButton.setDisable(loading);
            autoResolveButton.setDisable(loading);
        });
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
