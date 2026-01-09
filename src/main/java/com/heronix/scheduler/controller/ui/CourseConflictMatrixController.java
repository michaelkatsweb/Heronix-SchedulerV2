package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.ConflictMatrix;
import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.repository.ConflictMatrixRepository;
import com.heronix.scheduler.service.ConflictMatrixService;
import com.heronix.scheduler.service.data.SISDataService;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Course Conflict Matrix Visualization
 * Displays conflict matrices generated from student course requests
 * Features: heatmap display, filtering, singleton highlighting, CSV export
 */
@Component
@Slf4j
public class CourseConflictMatrixController {

    @Autowired
    private ConflictMatrixService conflictMatrixService;

    @Autowired
    private ConflictMatrixRepository conflictMatrixRepository;

    @Autowired
    private SISDataService sisDataService;

    // ========== FXML Components ==========

    @FXML
    private ComboBox<Integer> yearComboBox;

    @FXML
    private ComboBox<String> courseFilterComboBox;

    @FXML
    private Spinner<Integer> thresholdSpinner;

    @FXML
    private CheckBox singletonsOnlyCheckBox;

    @FXML
    private TextField searchField;

    @FXML
    private Button generateButton;

    @FXML
    private Button exportButton;

    @FXML
    private TableView<ConflictMatrix> conflictsTable;

    @FXML
    private TableColumn<ConflictMatrix, String> course1Column;

    @FXML
    private TableColumn<ConflictMatrix, String> course2Column;

    @FXML
    private TableColumn<ConflictMatrix, String> conflictCountColumn;

    @FXML
    private TableColumn<ConflictMatrix, String> percentageColumn;

    @FXML
    private TableColumn<ConflictMatrix, String> singletonColumn;

    @FXML
    private TableColumn<ConflictMatrix, String> priorityColumn;

    @FXML
    private Label totalConflictsLabel;

    @FXML
    private Label singletonConflictsLabel;

    @FXML
    private Label highConflictsLabel;

    @FXML
    private Label averageConflictLabel;

    @FXML
    private VBox detailsPane;

    @FXML
    private Label detailsCourse1Label;

    @FXML
    private Label detailsCourse2Label;

    @FXML
    private Label detailsConflictCountLabel;

    @FXML
    private Label detailsPercentageLabel;

    @FXML
    private Label detailsSingletonLabel;

    @FXML
    private ProgressIndicator progressIndicator;

    // ========== Data ==========

    private ObservableList<ConflictMatrix> allConflicts = FXCollections.observableArrayList();
    private ObservableList<ConflictMatrix> filteredConflicts = FXCollections.observableArrayList();

    // ========== Initialization ==========

    @FXML
    public void initialize() {
        log.info("Initializing Course Conflict Matrix Controller");

        setupYearComboBox();
        setupCourseFilterComboBox();
        setupThresholdSpinner();
        setupTableColumns();
        setupTableSelection();
        setupFilterListeners();

        // Initially disable export until data is loaded
        exportButton.setDisable(true);
        progressIndicator.setVisible(false);
        detailsPane.setVisible(false);

        log.info("Course Conflict Matrix Controller initialized");
    }

    // ========== Setup Methods ==========

    private void setupYearComboBox() {
        int currentYear = LocalDate.now().getYear();
        ObservableList<Integer> years = FXCollections.observableArrayList();

        // Add current year and next 2 years
        for (int i = 0; i <= 2; i++) {
            years.add(currentYear + i);
        }

        yearComboBox.setItems(years);
        yearComboBox.setValue(currentYear + 1); // Default to next academic year
    }

    private void setupCourseFilterComboBox() {
        courseFilterComboBox.setPromptText("All Courses");

        // Populate with all courses
        try {
            List<Course> courses = sisDataService.getAllCourses().stream()
                    .filter(c -> Boolean.TRUE.equals(c.getActive()))
                    .collect(Collectors.toList());
            ObservableList<String> courseNames = FXCollections.observableArrayList("All Courses");
            courseNames.addAll(courses.stream()
                .map(c -> c.getCourseCode() + " - " + c.getCourseName())
                .sorted()
                .collect(Collectors.toList()));
            courseFilterComboBox.setItems(courseNames);
            courseFilterComboBox.setValue("All Courses");
        } catch (Exception e) {
            log.error("Failed to load courses for filter", e);
            showError("Failed to load courses", e.getMessage());
        }
    }

    private void setupThresholdSpinner() {
        SpinnerValueFactory<Integer> valueFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 5);
        thresholdSpinner.setValueFactory(valueFactory);
    }

    private void setupTableColumns() {
        // Course 1 column
        course1Column.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse1();
            return new SimpleStringProperty(
                course.getCourseCode() + "\n" + course.getCourseName());
        });

        // Course 2 column
        course2Column.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse2();
            return new SimpleStringProperty(
                course.getCourseCode() + "\n" + course.getCourseName());
        });

        // Conflict count column
        conflictCountColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(String.valueOf(cellData.getValue().getConflictCount())));

        // Percentage column
        percentageColumn.setCellValueFactory(cellData -> {
            Double pct = cellData.getValue().getConflictPercentage();
            return new SimpleStringProperty(
                pct != null ? String.format("%.1f%%", pct) : "N/A");
        });

        // Singleton column
        singletonColumn.setCellValueFactory(cellData -> {
            boolean isSingleton = Boolean.TRUE.equals(cellData.getValue().getIsSingletonConflict());
            return new SimpleStringProperty(isSingleton ? "YES" : "");
        });

        // Priority column
        priorityColumn.setCellValueFactory(cellData -> {
            ConflictMatrix cm = cellData.getValue();
            boolean isSingleton = Boolean.TRUE.equals(cm.getIsSingletonConflict());
            Integer count = cm.getConflictCount();

            String priority;
            if (isSingleton && count > 10) {
                priority = "CRITICAL";
            } else if (isSingleton) {
                priority = "HIGH";
            } else if (count > 20) {
                priority = "MEDIUM";
            } else {
                priority = "LOW";
            }

            return new SimpleStringProperty(priority);
        });

        // Add row factory for color coding
        conflictsTable.setRowFactory(tv -> new TableRow<ConflictMatrix>() {
            @Override
            protected void updateItem(ConflictMatrix item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                } else {
                    boolean isSingleton = Boolean.TRUE.equals(item.getIsSingletonConflict());
                    Integer count = item.getConflictCount();

                    if (isSingleton && count > 10) {
                        setStyle("-fx-background-color: #ffcccc;"); // Light red for critical
                    } else if (isSingleton) {
                        setStyle("-fx-background-color: #ffe6cc;"); // Light orange for high
                    } else if (count > 20) {
                        setStyle("-fx-background-color: #ffffcc;"); // Light yellow for medium
                    } else {
                        setStyle("");
                    }
                }
            }
        });
    }

    private void setupTableSelection() {
        conflictsTable.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    showConflictDetails(newValue);
                } else {
                    detailsPane.setVisible(false);
                }
            }
        );
    }

    private void setupFilterListeners() {
        // Add listeners to filter controls
        courseFilterComboBox.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        thresholdSpinner.valueProperty().addListener((obs, old, newVal) -> applyFilters());
        singletonsOnlyCheckBox.selectedProperty().addListener((obs, old, newVal) -> applyFilters());
        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    // ========== Action Handlers ==========

    @FXML
    private void handleGenerate() {
        Integer year = yearComboBox.getValue();
        if (year == null) {
            showError("Validation Error", "Please select a year");
            return;
        }

        log.info("Generating conflict matrix for year: {}", year);

        // Show progress indicator
        progressIndicator.setVisible(true);
        generateButton.setDisable(true);
        exportButton.setDisable(true);

        // Run in background thread
        Task<List<ConflictMatrix>> task = new Task<>() {
            @Override
            protected List<ConflictMatrix> call() throws Exception {
                // Generate conflict matrix
                conflictMatrixService.generateConflictMatrix(year);

                // Load all conflicts for this year
                return conflictMatrixRepository.findByScheduleYear(year);
            }

            @Override
            protected void succeeded() {
                allConflicts.setAll(getValue());
                applyFilters();
                updateStatistics();

                progressIndicator.setVisible(false);
                generateButton.setDisable(false);
                exportButton.setDisable(false);

                log.info("Loaded {} conflicts for year {}", allConflicts.size(), year);
                showInfo("Success", "Conflict matrix generated successfully.\n" +
                    allConflicts.size() + " conflicts found.");
            }

            @Override
            protected void failed() {
                progressIndicator.setVisible(false);
                generateButton.setDisable(false);

                log.error("Failed to generate conflict matrix", getException());
                showError("Generation Failed", getException().getMessage());
            }
        };

        new Thread(task).start();
    }

    @FXML
    private void handleRefresh() {
        Integer year = yearComboBox.getValue();
        if (year == null) {
            return;
        }

        log.info("Refreshing conflicts for year: {}", year);
        loadConflicts(year);
    }

    @FXML
    private void handleExport() {
        if (filteredConflicts.isEmpty()) {
            showError("No Data", "No conflicts to export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Conflicts to CSV");
        fileChooser.setInitialFileName("conflict_matrix_" + yearComboBox.getValue() + ".csv");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());
        if (file != null) {
            exportToCSV(file);
        }
    }

    @FXML
    private void handleClearFilters() {
        courseFilterComboBox.setValue("All Courses");
        thresholdSpinner.getValueFactory().setValue(5);
        singletonsOnlyCheckBox.setSelected(false);
        searchField.clear();
    }

    // ========== Business Logic ==========

    private void loadConflicts(Integer year) {
        progressIndicator.setVisible(true);
        exportButton.setDisable(true);

        Task<List<ConflictMatrix>> task = new Task<>() {
            @Override
            protected List<ConflictMatrix> call() throws Exception {
                return conflictMatrixRepository.findByScheduleYear(year);
            }

            @Override
            protected void succeeded() {
                allConflicts.setAll(getValue());
                applyFilters();
                updateStatistics();

                progressIndicator.setVisible(false);
                exportButton.setDisable(false);

                log.info("Loaded {} conflicts for year {}", allConflicts.size(), year);
            }

            @Override
            protected void failed() {
                progressIndicator.setVisible(false);
                log.error("Failed to load conflicts", getException());
                showError("Load Failed", getException().getMessage());
            }
        };

        new Thread(task).start();
    }

    private void applyFilters() {
        List<ConflictMatrix> filtered = new ArrayList<>(allConflicts);

        // Filter by course
        String courseFilter = courseFilterComboBox.getValue();
        if (courseFilter != null && !"All Courses".equals(courseFilter)) {
            String courseCode = courseFilter.split(" - ")[0];
            filtered = filtered.stream()
                .filter(cm -> cm.getCourse1().getCourseCode().equals(courseCode) ||
                            cm.getCourse2().getCourseCode().equals(courseCode))
                .collect(Collectors.toList());
        }

        // Filter by threshold
        Integer threshold = thresholdSpinner.getValue();
        if (threshold != null) {
            filtered = filtered.stream()
                .filter(cm -> cm.getConflictCount() >= threshold)
                .collect(Collectors.toList());
        }

        // Filter by singletons only
        if (singletonsOnlyCheckBox.isSelected()) {
            filtered = filtered.stream()
                .filter(cm -> Boolean.TRUE.equals(cm.getIsSingletonConflict()))
                .collect(Collectors.toList());
        }

        // Filter by search text
        String searchText = searchField.getText();
        if (searchText != null && !searchText.trim().isEmpty()) {
            String searchLower = searchText.toLowerCase();
            filtered = filtered.stream()
                .filter(cm ->
                    cm.getCourse1().getCourseCode().toLowerCase().contains(searchLower) ||
                    cm.getCourse1().getCourseName().toLowerCase().contains(searchLower) ||
                    cm.getCourse2().getCourseCode().toLowerCase().contains(searchLower) ||
                    cm.getCourse2().getCourseName().toLowerCase().contains(searchLower))
                .collect(Collectors.toList());
        }

        filteredConflicts.setAll(filtered);
        conflictsTable.setItems(filteredConflicts);
        updateStatistics();

        log.debug("Filtered {} conflicts from {} total", filtered.size(), allConflicts.size());
    }

    private void updateStatistics() {
        if (filteredConflicts.isEmpty()) {
            totalConflictsLabel.setText("Total: 0");
            singletonConflictsLabel.setText("Singletons: 0");
            highConflictsLabel.setText("High (>20): 0");
            averageConflictLabel.setText("Average: 0.0");
            return;
        }

        int total = filteredConflicts.size();
        long singletonCount = filteredConflicts.stream()
            .filter(cm -> Boolean.TRUE.equals(cm.getIsSingletonConflict()))
            .count();
        long highCount = filteredConflicts.stream()
            .filter(cm -> cm.getConflictCount() > 20)
            .count();
        double average = filteredConflicts.stream()
            .mapToInt(ConflictMatrix::getConflictCount)
            .average()
            .orElse(0.0);

        totalConflictsLabel.setText("Total: " + total);
        singletonConflictsLabel.setText("Singletons: " + singletonCount);
        highConflictsLabel.setText("High (>20): " + highCount);
        averageConflictLabel.setText(String.format("Average: %.1f", average));
    }

    private void showConflictDetails(ConflictMatrix conflict) {
        detailsCourse1Label.setText(conflict.getCourse1().getCourseCode() + " - " +
                                    conflict.getCourse1().getCourseName());
        detailsCourse2Label.setText(conflict.getCourse2().getCourseCode() + " - " +
                                    conflict.getCourse2().getCourseName());
        detailsConflictCountLabel.setText(String.valueOf(conflict.getConflictCount()));

        Double pct = conflict.getConflictPercentage();
        detailsPercentageLabel.setText(pct != null ? String.format("%.1f%%", pct) : "N/A");

        detailsSingletonLabel.setText(Boolean.TRUE.equals(conflict.getIsSingletonConflict()) ?
            "YES - High Priority" : "No");

        detailsPane.setVisible(true);
    }

    private void exportToCSV(File file) {
        try (FileWriter writer = new FileWriter(file)) {
            // Write header
            writer.append("Course 1 Code,Course 1 Name,Course 2 Code,Course 2 Name,");
            writer.append("Conflict Count,Percentage,Is Singleton,Priority\n");

            // Write data
            for (ConflictMatrix cm : filteredConflicts) {
                writer.append(escapeCSV(cm.getCourse1().getCourseCode())).append(",");
                writer.append(escapeCSV(cm.getCourse1().getCourseName())).append(",");
                writer.append(escapeCSV(cm.getCourse2().getCourseCode())).append(",");
                writer.append(escapeCSV(cm.getCourse2().getCourseName())).append(",");
                writer.append(String.valueOf(cm.getConflictCount())).append(",");

                Double pct = cm.getConflictPercentage();
                writer.append(pct != null ? String.format("%.1f", pct) : "").append(",");

                boolean isSingleton = Boolean.TRUE.equals(cm.getIsSingletonConflict());
                writer.append(isSingleton ? "YES" : "NO").append(",");

                // Priority
                String priority;
                if (isSingleton && cm.getConflictCount() > 10) {
                    priority = "CRITICAL";
                } else if (isSingleton) {
                    priority = "HIGH";
                } else if (cm.getConflictCount() > 20) {
                    priority = "MEDIUM";
                } else {
                    priority = "LOW";
                }
                writer.append(priority).append("\n");
            }

            log.info("Exported {} conflicts to {}", filteredConflicts.size(), file.getAbsolutePath());
            showInfo("Export Successful", "Exported " + filteredConflicts.size() +
                " conflicts to:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            log.error("Failed to export to CSV", e);
            showError("Export Failed", e.getMessage());
        }
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // ========== UI Helpers ==========

    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
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
