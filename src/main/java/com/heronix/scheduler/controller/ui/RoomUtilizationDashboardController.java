package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.CourseSection;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.repository.CourseSectionRepository;
import com.heronix.scheduler.repository.RoomRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
 * Room Utilization Dashboard Controller
 *
 * Provides visualization of room usage, availability, and capacity metrics.
 * Shows utilization by building, room type, and period.
 * Generates optimization recommendations.
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/RoomUtilizationDashboardController.java
 */
@Component
@Slf4j
public class RoomUtilizationDashboardController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    // Header Controls
    @FXML private ComboBox<String> buildingFilterComboBox;
    @FXML private ComboBox<String> roomTypeFilterComboBox;

    // Summary Metrics
    @FXML private Label totalRoomsLabel;
    @FXML private Label totalRoomsDescLabel;
    @FXML private Label avgUtilizationLabel;
    @FXML private Label avgUtilizationDescLabel;
    @FXML private Label underutilizedRoomsLabel;
    @FXML private Label underutilizedDescLabel;
    @FXML private Label overutilizedRoomsLabel;
    @FXML private Label overutilizedDescLabel;

    // Building Utilization Chart
    @FXML private BarChart<String, Number> buildingUtilizationChart;
    @FXML private CategoryAxis buildingXAxis;
    @FXML private NumberAxis buildingYAxis;

    // Room Utilization Table
    @FXML private TableView<RoomUtilization> roomUtilizationTable;
    @FXML private TableColumn<RoomUtilization, String> roomNumberColumn;
    @FXML private TableColumn<RoomUtilization, String> buildingColumn;
    @FXML private TableColumn<RoomUtilization, String> roomTypeColumn;
    @FXML private TableColumn<RoomUtilization, Integer> capacityColumn;
    @FXML private TableColumn<RoomUtilization, Integer> periodsUsedColumn;
    @FXML private TableColumn<RoomUtilization, Integer> periodsAvailableColumn;
    @FXML private TableColumn<RoomUtilization, String> utilizationPercentColumn;
    @FXML private TableColumn<RoomUtilization, String> statusColumn;
    @FXML private TableColumn<RoomUtilization, Void> viewDetailsColumn;
    @FXML private ComboBox<String> utilizationFilterComboBox;

    // Period Availability Labels
    @FXML private Label period1AvailableLabel;
    @FXML private Label period1AvailDescLabel;
    @FXML private Label period2AvailableLabel;
    @FXML private Label period2AvailDescLabel;
    @FXML private Label period3AvailableLabel;
    @FXML private Label period3AvailDescLabel;
    @FXML private Label period4AvailableLabel;
    @FXML private Label period4AvailDescLabel;
    @FXML private Label period5AvailableLabel;
    @FXML private Label period5AvailDescLabel;
    @FXML private Label period6AvailableLabel;
    @FXML private Label period6AvailDescLabel;
    @FXML private Label period7AvailableLabel;
    @FXML private Label period7AvailDescLabel;
    @FXML private Label period8AvailableLabel;
    @FXML private Label period8AvailDescLabel;

    // Room Type Utilization Labels
    @FXML private Label standardUtilizationLabel;
    @FXML private Label standardDescLabel;
    @FXML private Label labUtilizationLabel;
    @FXML private Label labDescLabel;
    @FXML private Label computerLabUtilizationLabel;
    @FXML private Label computerLabDescLabel;
    @FXML private Label gymUtilizationLabel;
    @FXML private Label gymDescLabel;
    @FXML private Label artUtilizationLabel;
    @FXML private Label artDescLabel;
    @FXML private Label musicUtilizationLabel;
    @FXML private Label musicDescLabel;
    @FXML private Label auditoriumUtilizationLabel;
    @FXML private Label auditoriumDescLabel;
    @FXML private Label libraryUtilizationLabel;
    @FXML private Label libraryDescLabel;

    // Recommendations
    @FXML private VBox recommendationsContainer;

    private static final int NUM_PERIODS = 8;
    private static final double UNDERUTILIZED_THRESHOLD = 0.60; // 60%
    private static final double OVERUTILIZED_THRESHOLD = 0.90; // 90%

    private List<RoomUtilization> utilizationData;

    @FXML
    public void initialize() {
        log.info("Initializing Room Utilization Dashboard Controller");

        setupFilters();
        setupTables();
        setupCharts();
        loadUtilizationData();
    }

    private void setupFilters() {
        // Building filter
        if (buildingFilterComboBox != null) {
            ObservableList<String> buildings = FXCollections.observableArrayList(
                "All Buildings",
                "Main Building",
                "Science Wing",
                "Arts Building",
                "Athletic Center",
                "Annex"
            );
            buildingFilterComboBox.setItems(buildings);
            buildingFilterComboBox.setValue("All Buildings");
        }

        // Room type filter
        if (roomTypeFilterComboBox != null) {
            ObservableList<String> roomTypes = FXCollections.observableArrayList(
                "All Room Types",
                "STANDARD_CLASSROOM",
                "LAB",
                "COMPUTER_LAB",
                "GYMNASIUM",
                "ART_ROOM",
                "MUSIC_ROOM",
                "AUDITORIUM",
                "LIBRARY"
            );
            roomTypeFilterComboBox.setItems(roomTypes);
            roomTypeFilterComboBox.setValue("All Room Types");
        }

        // Utilization filter
        if (utilizationFilterComboBox != null) {
            ObservableList<String> filters = FXCollections.observableArrayList(
                "All Rooms",
                "Underutilized (<60%)",
                "Optimal (60-90%)",
                "Overutilized (>90%)"
            );
            utilizationFilterComboBox.setItems(filters);
            utilizationFilterComboBox.setValue("All Rooms");
        }
    }

    private void setupTables() {
        if (roomNumberColumn != null) {
            roomNumberColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRoomNumber()));
        }
        if (buildingColumn != null) {
            buildingColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getBuilding()));
        }
        if (roomTypeColumn != null) {
            roomTypeColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getRoomType()));
        }
        if (capacityColumn != null) {
            capacityColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getCapacity()).asObject());
        }
        if (periodsUsedColumn != null) {
            periodsUsedColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getPeriodsUsed()).asObject());
        }
        if (periodsAvailableColumn != null) {
            periodsAvailableColumn.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getPeriodsAvailable()).asObject());
        }
        if (utilizationPercentColumn != null) {
            utilizationPercentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getUtilizationPercent()));
        }
        if (statusColumn != null) {
            statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStatus()));
        }

        // Add View Details button
        if (viewDetailsColumn != null) {
            viewDetailsColumn.setCellFactory(param -> new TableCell<>() {
                private final Button viewButton = new Button("View");

                {
                    viewButton.setOnAction(event -> {
                        RoomUtilization utilization = getTableView().getItems().get(getIndex());
                        showRoomDetails(utilization);
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

    private void setupCharts() {
        if (buildingUtilizationChart != null) {
            buildingUtilizationChart.setAnimated(true);
        }
    }

    @FXML
    private void handleBuildingFilterChange() {
        filterUtilizationData();
    }

    @FXML
    private void handleRoomTypeFilterChange() {
        filterUtilizationData();
    }

    @FXML
    private void handleRefresh() {
        loadUtilizationData();
    }

    @FXML
    private void handleExportReport() {
        if (utilizationData == null || utilizationData.isEmpty()) {
            showInfo("No data to export. Please refresh the dashboard first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Room Utilization Report");
        fileChooser.setInitialFileName("room_utilization_" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showSaveDialog(roomUtilizationTable.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            // Write CSV header
            writer.println("Room Number,Building,Room Type,Capacity,Periods Used,Periods Available,Utilization %,Status");

            // Write data rows
            for (RoomUtilization util : utilizationData) {
                writer.printf("\"%s\",\"%s\",\"%s\",%d,%d,%d,\"%s\",\"%s\"%n",
                    util.getRoomNumber(),
                    util.getBuilding(),
                    util.getRoomType(),
                    util.getCapacity(),
                    util.getPeriodsUsed(),
                    util.getPeriodsAvailable(),
                    util.getUtilizationPercent(),
                    util.getStatus()
                );
            }

            // Add summary section
            writer.println();
            writer.println("SUMMARY");
            writer.printf("\"Total Rooms\",%d%n", utilizationData.size());

            double avgUtil = utilizationData.stream()
                .mapToDouble(u -> parseUtilization(u.getUtilizationPercent()))
                .average()
                .orElse(0.0);
            writer.printf("\"Average Utilization\",\"%.1f%%\"%n", avgUtil);

            long underutilized = utilizationData.stream()
                .filter(u -> parseUtilization(u.getUtilizationPercent()) < UNDERUTILIZED_THRESHOLD * 100)
                .count();
            writer.printf("\"Underutilized Rooms (<60%%)\",%d%n", underutilized);

            long overutilized = utilizationData.stream()
                .filter(u -> parseUtilization(u.getUtilizationPercent()) > OVERUTILIZED_THRESHOLD * 100)
                .count();
            writer.printf("\"Overutilized Rooms (>90%%)\",%d%n", overutilized);

            showInfo("Exported " + utilizationData.size() + " rooms to:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            showError("Failed to export report: " + e.getMessage());
        }
    }

    @FXML
    private void handleUtilizationFilterChange() {
        filterUtilizationData();
    }

    private void loadUtilizationData() {
        log.info("Loading room utilization data");

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                utilizationData = new ArrayList<>();

                // Get all rooms
                List<Room> allRooms = roomRepository.findAll();

                // Get all course sections
                List<CourseSection> allSections = courseSectionRepository.findAll();

                // Calculate utilization for each room
                for (Room room : allRooms) {
                    List<CourseSection> roomSections = allSections.stream()
                        .filter(section -> section.getAssignedRoom() != null &&
                                         section.getAssignedRoom().getId().equals(room.getId()))
                        .collect(Collectors.toList());

                    int periodsUsed = roomSections.size();
                    int periodsAvailable = NUM_PERIODS - periodsUsed;
                    double utilizationPercent = (periodsUsed * 100.0) / NUM_PERIODS;

                    String status = determineStatus(utilizationPercent);

                    utilizationData.add(new RoomUtilization(
                        room.getId(),
                        room.getRoomNumber(),
                        room.getBuilding() != null ? room.getBuilding() : "N/A",
                        room.getRoomType() != null ? room.getRoomType().toString() : "STANDARD",
                        room.getCapacity() != null ? room.getCapacity() : 0,
                        periodsUsed,
                        periodsAvailable,
                        String.format("%.1f%%", utilizationPercent),
                        status
                    ));
                }

                // Update UI on JavaFX thread
                Platform.runLater(() -> {
                    updateSummaryMetrics();
                    updateBuildingChart();
                    updatePeriodAvailability();
                    updateRoomTypeBreakdown();
                    updateUtilizationTable();
                    updateRecommendations();
                });

                return null;
            }

            @Override
            protected void failed() {
                log.error("Failed to load utilization data", getException());
                Platform.runLater(() -> showError("Failed to load room utilization data"));
            }
        };

        new Thread(loadTask).start();
    }

    private void updateSummaryMetrics() {
        if (utilizationData.isEmpty()) return;

        // Total rooms
        safeSetText(totalRoomsLabel, String.valueOf(utilizationData.size()));

        // Average utilization
        double avgUtil = utilizationData.stream()
            .mapToDouble(u -> parseUtilization(u.getUtilizationPercent()))
            .average()
            .orElse(0.0);
        safeSetText(avgUtilizationLabel, String.format("%.1f%%", avgUtil));

        // Underutilized rooms
        long underutilized = utilizationData.stream()
            .filter(u -> parseUtilization(u.getUtilizationPercent()) < UNDERUTILIZED_THRESHOLD * 100)
            .count();
        safeSetText(underutilizedRoomsLabel, String.valueOf(underutilized));

        // Overutilized rooms
        long overutilized = utilizationData.stream()
            .filter(u -> parseUtilization(u.getUtilizationPercent()) > OVERUTILIZED_THRESHOLD * 100)
            .count();
        safeSetText(overutilizedRoomsLabel, String.valueOf(overutilized));
    }

    private void updateBuildingChart() {
        if (buildingUtilizationChart == null) return;

        buildingUtilizationChart.getData().clear();

        // Group by building
        Map<String, Double> buildingUtilization = utilizationData.stream()
            .collect(Collectors.groupingBy(
                RoomUtilization::getBuilding,
                Collectors.averagingDouble(u -> parseUtilization(u.getUtilizationPercent()))
            ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Utilization %");

        for (Map.Entry<String, Double> entry : buildingUtilization.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        buildingUtilizationChart.getData().add(series);
    }

    private void updatePeriodAvailability() {
        List<CourseSection> allSections = courseSectionRepository.findAll();
        int totalRooms = utilizationData.size();

        Label[] availLabels = {
            period1AvailableLabel, period2AvailableLabel, period3AvailableLabel, period4AvailableLabel,
            period5AvailableLabel, period6AvailableLabel, period7AvailableLabel, period8AvailableLabel
        };

        for (int period = 1; period <= NUM_PERIODS; period++) {
            final int p = period;
            long roomsUsed = allSections.stream()
                .filter(s -> s.getAssignedPeriod() != null && s.getAssignedPeriod() == p)
                .map(s -> s.getAssignedRoom() != null ? s.getAssignedRoom().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

            long roomsAvailable = totalRooms - roomsUsed;
            safeSetText(availLabels[period - 1], String.valueOf(roomsAvailable));
        }
    }

    private void updateRoomTypeBreakdown() {
        Map<String, Double> typeUtilization = utilizationData.stream()
            .collect(Collectors.groupingBy(
                RoomUtilization::getRoomType,
                Collectors.averagingDouble(u -> parseUtilization(u.getUtilizationPercent()))
            ));

        safeSetText(standardUtilizationLabel,
            String.format("%.1f%%", typeUtilization.getOrDefault("STANDARD_CLASSROOM", 0.0)));
        safeSetText(labUtilizationLabel,
            String.format("%.1f%%", typeUtilization.getOrDefault("LAB", 0.0)));
        safeSetText(computerLabUtilizationLabel,
            String.format("%.1f%%", typeUtilization.getOrDefault("COMPUTER_LAB", 0.0)));
        safeSetText(gymUtilizationLabel,
            String.format("%.1f%%", typeUtilization.getOrDefault("GYMNASIUM", 0.0)));
        safeSetText(artUtilizationLabel,
            String.format("%.1f%%", typeUtilization.getOrDefault("ART_ROOM", 0.0)));
        safeSetText(musicUtilizationLabel,
            String.format("%.1f%%", typeUtilization.getOrDefault("MUSIC_ROOM", 0.0)));
        safeSetText(auditoriumUtilizationLabel,
            String.format("%.1f%%", typeUtilization.getOrDefault("AUDITORIUM", 0.0)));
        safeSetText(libraryUtilizationLabel,
            String.format("%.1f%%", typeUtilization.getOrDefault("LIBRARY", 0.0)));
    }

    private void updateUtilizationTable() {
        if (roomUtilizationTable == null) return;

        ObservableList<RoomUtilization> data = FXCollections.observableArrayList(utilizationData);

        // Sort by utilization (descending)
        data.sort((a, b) ->
            Double.compare(parseUtilization(b.getUtilizationPercent()),
                         parseUtilization(a.getUtilizationPercent())));

        roomUtilizationTable.setItems(data);
    }

    private void updateRecommendations() {
        if (recommendationsContainer == null) return;

        recommendationsContainer.getChildren().clear();

        List<String> recommendations = new ArrayList<>();

        // Check for underutilized rooms
        List<RoomUtilization> underutilized = utilizationData.stream()
            .filter(u -> parseUtilization(u.getUtilizationPercent()) < UNDERUTILIZED_THRESHOLD * 100)
            .collect(Collectors.toList());

        if (!underutilized.isEmpty()) {
            recommendations.add(String.format("📉 %d room(s) are underutilized (<60%%). Consider:",
                underutilized.size()));
            for (RoomUtilization u : underutilized.stream().limit(3).collect(Collectors.toList())) {
                recommendations.add(String.format("   • %s (%s): %s utilization",
                    u.getRoomNumber(), u.getRoomType(), u.getUtilizationPercent()));
            }
            recommendations.add("   → Reassign sections to maximize usage");
            recommendations.add("   → Consider room type conversion");
        }

        // Check for overutilized rooms
        List<RoomUtilization> overutilized = utilizationData.stream()
            .filter(u -> parseUtilization(u.getUtilizationPercent()) > OVERUTILIZED_THRESHOLD * 100)
            .collect(Collectors.toList());

        if (!overutilized.isEmpty()) {
            recommendations.add(String.format("🔥 %d room(s) are overutilized (>90%%). Consider:",
                overutilized.size()));
            for (RoomUtilization u : overutilized.stream().limit(3).collect(Collectors.toList())) {
                recommendations.add(String.format("   • %s (%s): %s utilization",
                    u.getRoomNumber(), u.getRoomType(), u.getUtilizationPercent()));
            }
            recommendations.add("   → Add additional rooms");
            recommendations.add("   → Distribute load to underutilized spaces");
        }

        // Check for unbalanced buildings
        Map<String, Double> buildingUtil = utilizationData.stream()
            .collect(Collectors.groupingBy(
                RoomUtilization::getBuilding,
                Collectors.averagingDouble(u -> parseUtilization(u.getUtilizationPercent()))
            ));

        double variance = calculateVariance(new ArrayList<>(buildingUtil.values()));
        if (variance > 20) {
            recommendations.add("⚖️ Building utilization is unbalanced.");
            recommendations.add("   → Redistribute sections across buildings");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("✓ Room utilization is well-balanced. No major issues detected.");
        }

        // Add recommendations to UI
        for (String rec : recommendations) {
            Label recLabel = new Label(rec);
            recLabel.setWrapText(true);
            recLabel.setStyle("-fx-padding: 5; -fx-font-size: 12px;");
            recommendationsContainer.getChildren().add(recLabel);
        }
    }

    private void filterUtilizationData() {
        if (utilizationFilterComboBox == null || roomUtilizationTable == null) return;

        String filter = utilizationFilterComboBox.getValue();
        if (filter == null || utilizationData == null) return;

        ObservableList<RoomUtilization> filtered = FXCollections.observableArrayList();

        switch (filter) {
            case "Underutilized (<60%)":
                filtered.addAll(utilizationData.stream()
                    .filter(u -> parseUtilization(u.getUtilizationPercent()) < UNDERUTILIZED_THRESHOLD * 100)
                    .collect(Collectors.toList()));
                break;
            case "Optimal (60-90%)":
                filtered.addAll(utilizationData.stream()
                    .filter(u -> {
                        double util = parseUtilization(u.getUtilizationPercent());
                        return util >= UNDERUTILIZED_THRESHOLD * 100 && util <= OVERUTILIZED_THRESHOLD * 100;
                    })
                    .collect(Collectors.toList()));
                break;
            case "Overutilized (>90%)":
                filtered.addAll(utilizationData.stream()
                    .filter(u -> parseUtilization(u.getUtilizationPercent()) > OVERUTILIZED_THRESHOLD * 100)
                    .collect(Collectors.toList()));
                break;
            default:
                filtered.addAll(utilizationData);
        }

        filtered.sort((a, b) ->
            Double.compare(parseUtilization(b.getUtilizationPercent()),
                         parseUtilization(a.getUtilizationPercent())));

        roomUtilizationTable.setItems(filtered);
    }

    private void showRoomDetails(RoomUtilization utilization) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Room Details");
        alert.setHeaderText(utilization.getRoomNumber() + " - Utilization Details");

        StringBuilder content = new StringBuilder();
        content.append("Building: ").append(utilization.getBuilding()).append("\n");
        content.append("Room Type: ").append(utilization.getRoomType()).append("\n");
        content.append("Capacity: ").append(utilization.getCapacity()).append("\n");
        content.append("Periods Used: ").append(utilization.getPeriodsUsed()).append("/").append(NUM_PERIODS).append("\n");
        content.append("Periods Available: ").append(utilization.getPeriodsAvailable()).append("\n");
        content.append("Utilization: ").append(utilization.getUtilizationPercent()).append("\n");
        content.append("Status: ").append(utilization.getStatus()).append("\n");

        alert.setContentText(content.toString());
        alert.showAndWait();
    }

    // Helper methods
    private String determineStatus(double utilizationPercent) {
        if (utilizationPercent < UNDERUTILIZED_THRESHOLD * 100) return "Underutilized";
        if (utilizationPercent > OVERUTILIZED_THRESHOLD * 100) return "Overutilized";
        return "Optimal";
    }

    private double parseUtilization(String utilization) {
        try {
            return Double.parseDouble(utilization.replace("%", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double calculateVariance(List<Double> values) {
        if (values.isEmpty()) return 0;

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
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
    public static class RoomUtilization {
        private final Long roomId;
        private final String roomNumber;
        private final String building;
        private final String roomType;
        private final int capacity;
        private final int periodsUsed;
        private final int periodsAvailable;
        private final String utilizationPercent;
        private final String status;
    }
}
