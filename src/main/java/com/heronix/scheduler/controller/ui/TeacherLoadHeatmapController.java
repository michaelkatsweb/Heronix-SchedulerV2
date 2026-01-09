package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.CourseSection;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.repository.CourseSectionRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.heronix.scheduler.service.data.SISDataService;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javafx.stage.FileChooser;

/**
 * Teacher Load Heatmap Controller
 *
 * Provides visual heatmap of teacher workload distribution across time periods.
 * Shows load intensity, identifies overloaded/underutilized teachers,
 * and provides balancing recommendations.
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/TeacherLoadHeatmapController.java
 */
@Component
@Slf4j
public class TeacherLoadHeatmapController {

    @Autowired
    private SISDataService sisDataService;
    @Autowired
    private CourseSectionRepository courseSectionRepository;

    // Header Controls
    @FXML private ComboBox<String> departmentFilterComboBox;
    @FXML private ComboBox<String> termFilterComboBox;

    // Summary Metrics
    @FXML private Label averageLoadLabel;
    @FXML private Label averageLoadDescLabel;
    @FXML private Label overloadedTeachersLabel;
    @FXML private Label overloadedDescLabel;
    @FXML private Label underutilizedTeachersLabel;
    @FXML private Label underutilizedDescLabel;
    @FXML private Label balanceScoreLabel;
    @FXML private Label balanceDescLabel;

    // Heatmap Controls
    @FXML private GridPane heatmapGrid;
    @FXML private CheckBox showPrepsCheckbox;
    @FXML private CheckBox showPercentagesCheckbox;

    // Teacher Workload Table
    @FXML private TableView<TeacherWorkload> teacherWorkloadTable;
    @FXML private TableColumn<TeacherWorkload, String> teacherNameColumn;
    @FXML private TableColumn<TeacherWorkload, String> departmentColumn;
    @FXML private TableColumn<TeacherWorkload, Integer> totalPeriodsColumn;
    @FXML private TableColumn<TeacherWorkload, Integer> uniqueCoursesColumn;
    @FXML private TableColumn<TeacherWorkload, Integer> uniquePrepsColumn;
    @FXML private TableColumn<TeacherWorkload, Integer> totalStudentsColumn;
    @FXML private TableColumn<TeacherWorkload, Integer> avgClassSizeColumn;
    @FXML private TableColumn<TeacherWorkload, String> loadStatusColumn;
    @FXML private TableColumn<TeacherWorkload, Void> detailsActionColumn;
    @FXML private ComboBox<String> workloadFilterComboBox;

    // Period Labels
    @FXML private Label period1LoadLabel;
    @FXML private Label period1DescLabel;
    @FXML private Label period2LoadLabel;
    @FXML private Label period2DescLabel;
    @FXML private Label period3LoadLabel;
    @FXML private Label period3DescLabel;
    @FXML private Label period4LoadLabel;
    @FXML private Label period4DescLabel;
    @FXML private Label period5LoadLabel;
    @FXML private Label period5DescLabel;
    @FXML private Label period6LoadLabel;
    @FXML private Label period6DescLabel;
    @FXML private Label period7LoadLabel;
    @FXML private Label period7DescLabel;
    @FXML private Label period8LoadLabel;
    @FXML private Label period8DescLabel;

    // Recommendations
    @FXML private VBox recommendationsContainer;

    private static final int OPTIMAL_LOAD = 5; // periods per teacher
    private static final int MAX_LOAD = 6;
    private static final int MIN_LOAD = 3;
    private static final int NUM_PERIODS = 8;

    private Map<Long, Map<Integer, Integer>> teacherPeriodLoads;
    private List<TeacherWorkload> workloadData;

    @FXML
    public void initialize() {
        log.info("Initializing Teacher Load Heatmap Controller");

        setupFilters();
        setupTables();
        loadHeatmapData();
    }

    private void setupFilters() {
        // Department filter
        if (departmentFilterComboBox != null) {
            ObservableList<String> departments = FXCollections.observableArrayList(
                "All Departments",
                "Mathematics",
                "English",
                "Science",
                "Social Studies",
                "Foreign Language",
                "Arts",
                "Physical Education",
                "Special Education"
            );
            departmentFilterComboBox.setItems(departments);
            departmentFilterComboBox.setValue("All Departments");
        }

        // Term filter
        if (termFilterComboBox != null) {
            ObservableList<String> terms = FXCollections.observableArrayList(
                "Current Term",
                "Fall 2024",
                "Spring 2025"
            );
            termFilterComboBox.setItems(terms);
            termFilterComboBox.setValue("Current Term");
        }

        // Workload filter
        if (workloadFilterComboBox != null) {
            ObservableList<String> filters = FXCollections.observableArrayList(
                "All Teachers",
                "Overloaded Only",
                "Underutilized Only",
                "Optimal Load"
            );
            workloadFilterComboBox.setItems(filters);
            workloadFilterComboBox.setValue("All Teachers");
        }
    }

    private void setupTables() {
        if (teacherNameColumn != null) {
            teacherNameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTeacherName()));
        }
        if (departmentColumn != null) {
            departmentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getDepartment()));
        }
        if (totalPeriodsColumn != null) {
            totalPeriodsColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getTotalPeriods()).asObject());
        }
        if (uniqueCoursesColumn != null) {
            uniqueCoursesColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getUniqueCourses()).asObject());
        }
        if (uniquePrepsColumn != null) {
            uniquePrepsColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getUniquePreps()).asObject());
        }
        if (totalStudentsColumn != null) {
            totalStudentsColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getTotalStudents()).asObject());
        }
        if (avgClassSizeColumn != null) {
            avgClassSizeColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getAvgClassSize()).asObject());
        }
        if (loadStatusColumn != null) {
            loadStatusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getLoadStatus()));
        }

        // Add View Details button
        if (detailsActionColumn != null) {
            detailsActionColumn.setCellFactory(param -> new TableCell<>() {
                private final Button viewButton = new Button("View");

                {
                    viewButton.setOnAction(event -> {
                        TeacherWorkload workload = getTableView().getItems().get(getIndex());
                        showTeacherDetails(workload);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        setGraphic(viewButton);
                    }
                }
            });
        }
    }

    @FXML
    private void handleDepartmentFilterChange() {
        loadHeatmapData();
    }

    @FXML
    private void handleTermFilterChange() {
        loadHeatmapData();
    }

    @FXML
    private void handleRefresh() {
        loadHeatmapData();
    }

    @FXML
    private void handleExport() {
        if (workloadData == null || workloadData.isEmpty()) {
            showInfo("No data to export. Please refresh the heatmap first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Teacher Load Report");
        fileChooser.setInitialFileName("teacher_load_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(teacherWorkloadTable.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write CSV header
            writer.println("Teacher Name,Department,Total Periods,Unique Courses,Unique Preps,Total Students,Avg Class Size,Load Status,P1,P2,P3,P4,P5,P6,P7,P8");

            // Write data rows
            for (TeacherWorkload workload : workloadData) {
                Map<Integer, Integer> periodLoads = teacherPeriodLoads.get(workload.getTeacherId());

                writer.printf("\"%s\",\"%s\",%d,%d,%d,%d,%d,\"%s\",%d,%d,%d,%d,%d,%d,%d,%d%n",
                    workload.getTeacherName(),
                    workload.getDepartment(),
                    workload.getTotalPeriods(),
                    workload.getUniqueCourses(),
                    workload.getUniquePreps(),
                    workload.getTotalStudents(),
                    workload.getAvgClassSize(),
                    workload.getLoadStatus(),
                    periodLoads != null ? periodLoads.getOrDefault(1, 0) : 0,
                    periodLoads != null ? periodLoads.getOrDefault(2, 0) : 0,
                    periodLoads != null ? periodLoads.getOrDefault(3, 0) : 0,
                    periodLoads != null ? periodLoads.getOrDefault(4, 0) : 0,
                    periodLoads != null ? periodLoads.getOrDefault(5, 0) : 0,
                    periodLoads != null ? periodLoads.getOrDefault(6, 0) : 0,
                    periodLoads != null ? periodLoads.getOrDefault(7, 0) : 0,
                    periodLoads != null ? periodLoads.getOrDefault(8, 0) : 0
                );
            }

            // Add summary section
            writer.println();
            writer.println("SUMMARY");
            writer.printf("\"Total Teachers\",%d%n", workloadData.size());

            double avgLoad = workloadData.stream()
                .mapToInt(TeacherWorkload::getTotalPeriods)
                .average()
                .orElse(0.0);
            writer.printf("\"Average Load\",\"%.1f periods\"%n", avgLoad);

            long overloaded = workloadData.stream()
                .filter(w -> w.getTotalPeriods() > MAX_LOAD)
                .count();
            writer.printf("\"Overloaded Teachers (>%d periods)\",%d%n", MAX_LOAD, overloaded);

            long underutilized = workloadData.stream()
                .filter(w -> w.getTotalPeriods() < MIN_LOAD)
                .count();
            writer.printf("\"Underutilized Teachers (<%d periods)\",%d%n", MIN_LOAD, underutilized);

            showInfo("Exported " + workloadData.size() + " teachers to:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            showError("Failed to export report: " + e.getMessage());
        }
    }

    @FXML
    private void handleTogglePreps() {
        updateHeatmapDisplay();
    }

    @FXML
    private void handleTogglePercentages() {
        updateHeatmapDisplay();
    }

    @FXML
    private void handleWorkloadFilterChange() {
        filterWorkloadTable();
    }

    private void loadHeatmapData() {
        log.info("Loading teacher load heatmap data");

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Initialize data structures
                teacherPeriodLoads = new HashMap<>();
                workloadData = new ArrayList<>();

                // Get all teachers
                List<Teacher> allTeachers = sisDataService.getAllTeachers();

                // Get all course sections
                List<CourseSection> allSections = courseSectionRepository.findAll();

                // Calculate workload for each teacher
                for (Teacher teacher : allTeachers) {
                    List<CourseSection> teacherSections = allSections.stream()
                        .filter(section -> section.getAssignedTeacher() != null &&
                                         section.getAssignedTeacher().getId().equals(teacher.getId()))
                        .collect(Collectors.toList());

                    if (teacherSections.isEmpty()) continue;

                    // Calculate period loads
                    Map<Integer, Integer> periodLoads = new HashMap<>();
                    for (int period = 1; period <= NUM_PERIODS; period++) {
                        final int p = period;
                        long count = teacherSections.stream()
                            .filter(s -> s.getAssignedPeriod() != null && s.getAssignedPeriod() == p)
                            .count();
                        periodLoads.put(period, (int) count);
                    }
                    teacherPeriodLoads.put(teacher.getId(), periodLoads);

                    // Calculate workload metrics
                    int totalPeriods = teacherSections.size();
                    int uniqueCourses = (int) teacherSections.stream()
                        .map(s -> s.getCourse().getId())
                        .distinct()
                        .count();
                    int uniquePreps = (int) teacherSections.stream()
                        .map(s -> s.getCourse().getCourseCode())
                        .distinct()
                        .count();
                    int totalStudents = teacherSections.stream()
                        .mapToInt(s -> s.getCurrentEnrollment() != null ? s.getCurrentEnrollment() : 0)
                        .sum();
                    int avgClassSize = totalPeriods > 0 ? totalStudents / totalPeriods : 0;

                    String loadStatus = determineLoadStatus(totalPeriods);

                    workloadData.add(new TeacherWorkload(
                        teacher.getId(),
                        teacher.getFirstName() + " " + teacher.getLastName(),
                        teacher.getDepartment() != null ? teacher.getDepartment() : "N/A",
                        totalPeriods,
                        uniqueCourses,
                        uniquePreps,
                        totalStudents,
                        avgClassSize,
                        loadStatus
                    ));
                }

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    updateSummaryMetrics();
                    updateHeatmapDisplay();
                    updatePeriodBreakdown();
                    updateWorkloadTable();
                    updateRecommendations();
                });

                return null;
            }

            @Override
            protected void failed() {
                log.error("Failed to load heatmap data", getException());
                Platform.runLater(() -> showError("Failed to load teacher load data"));
            }
        };

        new Thread(loadTask).start();
    }

    private void updateSummaryMetrics() {
        if (workloadData.isEmpty()) return;

        // Average load
        double avgLoad = workloadData.stream()
            .mapToInt(TeacherWorkload::getTotalPeriods)
            .average()
            .orElse(0.0);
        safeSetText(averageLoadLabel, String.format("%.1f", avgLoad));

        // Overloaded teachers
        long overloaded = workloadData.stream()
            .filter(w -> w.getTotalPeriods() > MAX_LOAD)
            .count();
        safeSetText(overloadedTeachersLabel, String.valueOf(overloaded));
        safeSetText(overloadedDescLabel, String.format("%d teachers above %d periods", overloaded, MAX_LOAD));

        // Underutilized teachers
        long underutilized = workloadData.stream()
            .filter(w -> w.getTotalPeriods() < MIN_LOAD)
            .count();
        safeSetText(underutilizedTeachersLabel, String.valueOf(underutilized));
        safeSetText(underutilizedDescLabel, String.format("%d teachers below %d periods", underutilized, MIN_LOAD));

        // Balance score (0-100, where 100 is perfect balance)
        double variance = calculateVariance(workloadData.stream()
            .mapToInt(TeacherWorkload::getTotalPeriods)
            .boxed()
            .collect(Collectors.toList()));
        double balanceScore = Math.max(0, 100 - (variance * 10));
        safeSetText(balanceScoreLabel, String.format("%.0f/100", balanceScore));

        String balanceText = balanceScore >= 80 ? "Excellent" :
                            balanceScore >= 60 ? "Good" :
                            balanceScore >= 40 ? "Fair" : "Poor";
        safeSetText(balanceDescLabel, balanceText + " distribution");
    }

    private void updateHeatmapDisplay() {
        if (heatmapGrid == null || teacherPeriodLoads == null) return;

        heatmapGrid.getChildren().clear();

        // Add header row (periods)
        Label cornerLabel = new Label("Teacher");
        cornerLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");
        heatmapGrid.add(cornerLabel, 0, 0);

        for (int period = 1; period <= NUM_PERIODS; period++) {
            Label periodLabel = new Label("P" + period);
            periodLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5; -fx-alignment: center;");
            periodLabel.setMinWidth(60);
            heatmapGrid.add(periodLabel, period, 0);
        }

        // Add teacher rows
        int row = 1;
        for (TeacherWorkload workload : workloadData) {
            // Teacher name
            Label teacherLabel = new Label(workload.getTeacherName());
            teacherLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");
            heatmapGrid.add(teacherLabel, 0, row);

            Map<Integer, Integer> periodLoads = teacherPeriodLoads.get(workload.getTeacherId());

            // Period cells
            for (int period = 1; period <= NUM_PERIODS; period++) {
                int load = periodLoads.getOrDefault(period, 0);
                Label cellLabel = new Label(String.valueOf(load));
                cellLabel.setMinWidth(60);
                cellLabel.setMinHeight(30);
                cellLabel.setAlignment(Pos.CENTER);

                // Color code based on load
                String bgColor = getLoadColor(load);
                cellLabel.setStyle(String.format(
                    "-fx-background-color: %s; -fx-border-color: #cccccc; -fx-padding: 5; -fx-alignment: center;",
                    bgColor
                ));

                heatmapGrid.add(cellLabel, period, row);
            }

            row++;
        }
    }

    private void updatePeriodBreakdown() {
        if (teacherPeriodLoads == null) return;

        Label[] loadLabels = {
            period1LoadLabel, period2LoadLabel, period3LoadLabel, period4LoadLabel,
            period5LoadLabel, period6LoadLabel, period7LoadLabel, period8LoadLabel
        };

        Label[] descLabels = {
            period1DescLabel, period2DescLabel, period3DescLabel, period4DescLabel,
            period5DescLabel, period6DescLabel, period7DescLabel, period8DescLabel
        };

        for (int period = 1; period <= NUM_PERIODS; period++) {
            final int p = period;
            long teachersAssigned = teacherPeriodLoads.values().stream()
                .filter(loads -> loads.getOrDefault(p, 0) > 0)
                .count();

            safeSetText(loadLabels[period - 1], String.valueOf(teachersAssigned));
            safeSetText(descLabels[period - 1], "teachers assigned");
        }
    }

    private void updateWorkloadTable() {
        if (teacherWorkloadTable == null) return;

        ObservableList<TeacherWorkload> data = FXCollections.observableArrayList(workloadData);

        // Sort by total periods (descending)
        data.sort((a, b) -> Integer.compare(b.getTotalPeriods(), a.getTotalPeriods()));

        teacherWorkloadTable.setItems(data);
    }

    private void updateRecommendations() {
        if (recommendationsContainer == null) return;

        recommendationsContainer.getChildren().clear();

        List<String> recommendations = new ArrayList<>();

        // Check for overloaded teachers
        List<TeacherWorkload> overloaded = workloadData.stream()
            .filter(w -> w.getTotalPeriods() > MAX_LOAD)
            .collect(Collectors.toList());

        if (!overloaded.isEmpty()) {
            recommendations.add(String.format("⚠️ %d teacher(s) are overloaded. Consider redistributing sections:",
                overloaded.size()));
            for (TeacherWorkload w : overloaded.stream().limit(3).collect(Collectors.toList())) {
                recommendations.add(String.format("   • %s: %d periods (-%d needed)",
                    w.getTeacherName(), w.getTotalPeriods(), w.getTotalPeriods() - OPTIMAL_LOAD));
            }
        }

        // Check for underutilized teachers
        List<TeacherWorkload> underutilized = workloadData.stream()
            .filter(w -> w.getTotalPeriods() < MIN_LOAD)
            .collect(Collectors.toList());

        if (!underutilized.isEmpty()) {
            recommendations.add(String.format("📉 %d teacher(s) are underutilized. Consider assigning more sections:",
                underutilized.size()));
            for (TeacherWorkload w : underutilized.stream().limit(3).collect(Collectors.toList())) {
                recommendations.add(String.format("   • %s: %d periods (+%d available)",
                    w.getTeacherName(), w.getTotalPeriods(), OPTIMAL_LOAD - w.getTotalPeriods()));
            }
        }

        // Check for prep overload
        List<TeacherWorkload> tooManyPreps = workloadData.stream()
            .filter(w -> w.getUniquePreps() > 3)
            .collect(Collectors.toList());

        if (!tooManyPreps.isEmpty()) {
            recommendations.add(String.format("📚 %d teacher(s) have excessive preps (>3). Consider consolidating:",
                tooManyPreps.size()));
            for (TeacherWorkload w : tooManyPreps.stream().limit(3).collect(Collectors.toList())) {
                recommendations.add(String.format("   • %s: %d preps",
                    w.getTeacherName(), w.getUniquePreps()));
            }
        }

        if (recommendations.isEmpty()) {
            recommendations.add("✓ Load distribution is balanced. No major issues detected.");
        }

        // Add recommendations to UI
        for (String rec : recommendations) {
            Label recLabel = new Label(rec);
            recLabel.setWrapText(true);
            recLabel.setStyle("-fx-padding: 5; -fx-font-size: 12px;");
            recommendationsContainer.getChildren().add(recLabel);
        }
    }

    private void filterWorkloadTable() {
        if (workloadFilterComboBox == null || teacherWorkloadTable == null) return;

        String filter = workloadFilterComboBox.getValue();
        if (filter == null) return;

        ObservableList<TeacherWorkload> filtered = FXCollections.observableArrayList();

        switch (filter) {
            case "Overloaded Only":
                filtered.addAll(workloadData.stream()
                    .filter(w -> w.getTotalPeriods() > MAX_LOAD)
                    .collect(Collectors.toList()));
                break;
            case "Underutilized Only":
                filtered.addAll(workloadData.stream()
                    .filter(w -> w.getTotalPeriods() < MIN_LOAD)
                    .collect(Collectors.toList()));
                break;
            case "Optimal Load":
                filtered.addAll(workloadData.stream()
                    .filter(w -> w.getTotalPeriods() >= MIN_LOAD && w.getTotalPeriods() <= MAX_LOAD)
                    .collect(Collectors.toList()));
                break;
            default:
                filtered.addAll(workloadData);
        }

        filtered.sort((a, b) -> Integer.compare(b.getTotalPeriods(), a.getTotalPeriods()));
        teacherWorkloadTable.setItems(filtered);
    }

    private void showTeacherDetails(TeacherWorkload workload) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Teacher Details");
        alert.setHeaderText(workload.getTeacherName() + " - Workload Details");

        StringBuilder content = new StringBuilder();
        content.append("Department: ").append(workload.getDepartment()).append("\n");
        content.append("Total Periods: ").append(workload.getTotalPeriods()).append("\n");
        content.append("Unique Courses: ").append(workload.getUniqueCourses()).append("\n");
        content.append("Unique Preps: ").append(workload.getUniquePreps()).append("\n");
        content.append("Total Students: ").append(workload.getTotalStudents()).append("\n");
        content.append("Average Class Size: ").append(workload.getAvgClassSize()).append("\n");
        content.append("Load Status: ").append(workload.getLoadStatus()).append("\n");

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    // Helper methods
    private String determineLoadStatus(int periods) {
        if (periods > MAX_LOAD) return "Overloaded";
        if (periods < MIN_LOAD) return "Underutilized";
        if (periods == OPTIMAL_LOAD) return "Optimal";
        return "Normal";
    }

    private String getLoadColor(int load) {
        if (load == 0) return "#f0f0f0";  // No assignment
        if (load <= 2) return "#e8f5e9";  // Light
        if (load <= 4) return "#fff9c4";  // Moderate
        if (load <= 6) return "#ffcc80";  // High
        return "#ff9999";                  // Overloaded
    }

    private double calculateVariance(List<Integer> values) {
        if (values.isEmpty()) return 0;

        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average()
            .orElse(0);

        return Math.sqrt(variance);
    }

    private void safeSetText(Label label, String text) {
        if (label != null && text != null) {
            label.setText(text);
        }
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Inner class for table data
    @Data
    public static class TeacherWorkload {
        private final Long teacherId;
        private final String teacherName;
        private final String department;
        private final int totalPeriods;
        private final int uniqueCourses;
        private final int uniquePreps;
        private final int totalStudents;
        private final int avgClassSize;
        private final String loadStatus;
    }
}
