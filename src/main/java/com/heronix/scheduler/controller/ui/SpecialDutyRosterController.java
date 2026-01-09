package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.SpecialDutyAssignment;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.enums.DayOfWeek;
import com.heronix.scheduler.model.enums.DutyType;
import com.heronix.scheduler.repository.SpecialDutyAssignmentRepository;
import com.heronix.scheduler.ui.components.TimePickerField;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.heronix.scheduler.service.data.SISDataService;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.print.PrinterJob;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.FileChooser;

/**
 * Special Duty Roster Controller
 * Manages daily duties and special event assignments for staff
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-06
 */
@Component
public class SpecialDutyRosterController {

    @Autowired
    private SISDataService sisDataService;
    // ========================================================================
    // FXML FIELDS - Daily Duties Tab
    // ========================================================================

    @FXML private TabPane dutyTabPane;

    // Daily Duties Table
    @FXML private TableView<SpecialDutyAssignment> dailyDutiesTable;
    @FXML private TableColumn<SpecialDutyAssignment, Long> dailyIdColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> dailyDayColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> dailyDutyTypeColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> dailyLocationColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> dailyTimeColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> dailyAssignedToColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> dailyStaffTypeColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> dailyPriorityColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> dailyConfirmedColumn;
    @FXML private TableColumn<SpecialDutyAssignment, Void> dailyActionsColumn;

    // Daily Filters
    @FXML private ComboBox<DayOfWeek> dailyDayFilter;
    @FXML private ComboBox<DutyType> dailyDutyTypeFilter;
    @FXML private TextField dailyStaffSearch;
    @FXML private Label dailyCountLabel;
    @FXML private Label weeklyLoadLabel;
    @FXML private Label commonDutyLabel;

    // ========================================================================
    // FXML FIELDS - Special Events Tab
    // ========================================================================

    // Special Events Table
    @FXML private TableView<SpecialDutyAssignment> specialEventsTable;
    @FXML private TableColumn<SpecialDutyAssignment, Long> eventIdColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> eventDateColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> eventDutyTypeColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> eventLocationColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> eventTimeColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> eventAssignedToColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> eventStaffTypeColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> eventPriorityColumn;
    @FXML private TableColumn<SpecialDutyAssignment, String> eventCompletedColumn;
    @FXML private TableColumn<SpecialDutyAssignment, Void> eventActionsColumn;

    // Event Filters
    @FXML private DatePicker eventStartDateFilter;
    @FXML private DatePicker eventEndDateFilter;
    @FXML private ComboBox<DutyType> eventTypeFilter;
    @FXML private Label eventsCountLabel;
    @FXML private Label nextEventLabel;
    @FXML private Label monthEventsLabel;
    @FXML private Label unconfirmedLabel;

    // ========================================================================
    // FXML FIELDS - Workload Tab
    // ========================================================================

    @FXML private TableView<WorkloadData> workloadTable;
    @FXML private TableColumn<WorkloadData, String> workloadStaffColumn;
    @FXML private TableColumn<WorkloadData, String> workloadTypeColumn;
    @FXML private TableColumn<WorkloadData, Integer> workloadDailyColumn;
    @FXML private TableColumn<WorkloadData, Integer> workloadEventsColumn;
    @FXML private TableColumn<WorkloadData, Integer> workloadTotalColumn;
    @FXML private TableColumn<WorkloadData, String> workloadHoursColumn;

    // ========================================================================
    // FXML FIELDS - Status
    // ========================================================================

    @FXML private Label lastUpdatedLabel;

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    @Autowired
    private SpecialDutyAssignmentRepository dutyRepository;

    // ========================================================================
    // DATA
    // ========================================================================

    private ObservableList<SpecialDutyAssignment> dailyDuties = FXCollections.observableArrayList();
    private ObservableList<SpecialDutyAssignment> specialEvents = FXCollections.observableArrayList();
    private ObservableList<WorkloadData> workloadData = FXCollections.observableArrayList();

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        setupDailyDutiesTable();
        setupSpecialEventsTable();
        setupWorkloadTable();
        setupFilters();
        loadData();
    }

    private void setupDailyDutiesTable() {
        dailyDutiesTable.setItems(dailyDuties);

        dailyIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        dailyDayColumn.setCellValueFactory(cellData -> {
            DayOfWeek day = cellData.getValue().getDayOfWeek();
            return new SimpleStringProperty(day != null ? day.getDisplayName() : "N/A");
        });

        dailyDutyTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDisplayName()));

        dailyLocationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDutyLocation() != null ?
                        cellData.getValue().getDutyLocation() : "Not specified"));

        dailyTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTimeRange()));

        dailyAssignedToColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAssignedPersonName()));

        dailyStaffTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStaffTypeDisplay()));

        dailyPriorityColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPriorityDisplay()));

        dailyConfirmedColumn.setCellValueFactory(cellData -> {
            boolean confirmed = Boolean.TRUE.equals(cellData.getValue().getConfirmedByStaff());
            return new SimpleStringProperty(confirmed ? "✓ Yes" : "⏳ Pending");
        });

        // Actions column
        dailyActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setOnAction(event -> {
                    SpecialDutyAssignment duty = getTableView().getItems().get(getIndex());
                    handleEditDuty(duty);
                });
                deleteBtn.setOnAction(event -> {
                    SpecialDutyAssignment duty = getTableView().getItems().get(getIndex());
                    handleDeleteDuty(duty);
                });
                editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void setupSpecialEventsTable() {
        specialEventsTable.setItems(specialEvents);

        eventIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));

        eventDateColumn.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getEventDate();
            return new SimpleStringProperty(date != null ? date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "N/A");
        });

        eventDutyTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDisplayName()));

        eventLocationColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDutyLocation() != null ?
                        cellData.getValue().getDutyLocation() : "Not specified"));

        eventTimeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getTimeRange()));

        eventAssignedToColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getAssignedPersonName()));

        eventStaffTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getStaffTypeDisplay()));

        eventPriorityColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getPriorityDisplay()));

        eventCompletedColumn.setCellValueFactory(cellData -> {
            boolean completed = Boolean.TRUE.equals(cellData.getValue().getCompleted());
            return new SimpleStringProperty(completed ? "✓ Done" : "📋 Pending");
        });

        // Actions column
        eventActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setOnAction(event -> {
                    SpecialDutyAssignment duty = getTableView().getItems().get(getIndex());
                    handleEditDuty(duty);
                });
                deleteBtn.setOnAction(event -> {
                    SpecialDutyAssignment duty = getTableView().getItems().get(getIndex());
                    handleDeleteDuty(duty);
                });
                editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void setupWorkloadTable() {
        workloadTable.setItems(workloadData);

        workloadStaffColumn.setCellValueFactory(new PropertyValueFactory<>("staffName"));
        workloadTypeColumn.setCellValueFactory(new PropertyValueFactory<>("staffType"));
        workloadDailyColumn.setCellValueFactory(new PropertyValueFactory<>("dailyDutiesCount"));
        workloadEventsColumn.setCellValueFactory(new PropertyValueFactory<>("specialEventsCount"));
        workloadTotalColumn.setCellValueFactory(new PropertyValueFactory<>("totalDuties"));
        workloadHoursColumn.setCellValueFactory(new PropertyValueFactory<>("estimatedHours"));
    }

    private void setupFilters() {
        // Daily duty filters
        dailyDayFilter.setItems(FXCollections.observableArrayList(DayOfWeek.values()));
        dailyDayFilter.setConverter(new StringConverter<DayOfWeek>() {
            @Override
            public String toString(DayOfWeek day) {
                return day != null ? day.getDisplayName() : "";
            }
            @Override
            public DayOfWeek fromString(String string) {
                return null;
            }
        });

        dailyDutyTypeFilter.setItems(FXCollections.observableArrayList(DutyType.getDailyDuties()));
        dailyDutyTypeFilter.setConverter(new StringConverter<DutyType>() {
            @Override
            public String toString(DutyType type) {
                return type != null ? type.getDisplayName() : "";
            }
            @Override
            public DutyType fromString(String string) {
                return null;
            }
        });

        // Event filters
        eventTypeFilter.setItems(FXCollections.observableArrayList(DutyType.getSpecialEventDuties()));
        eventTypeFilter.setConverter(new StringConverter<DutyType>() {
            @Override
            public String toString(DutyType type) {
                return type != null ? type.getDisplayName() : "";
            }
            @Override
            public DutyType fromString(String string) {
                return null;
            }
        });

        // Set default date range for events (this month)
        eventStartDateFilter.setValue(LocalDate.now().withDayOfMonth(1));
        eventEndDateFilter.setValue(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    private void loadData() {
        loadDailyDuties();
        loadSpecialEvents();
        loadWorkloadData();
        updateSummaries();
        updateLastUpdatedLabel();
    }

    private void loadDailyDuties() {
        List<SpecialDutyAssignment> duties = dutyRepository.findAllActiveDailyDuties();
        dailyDuties.setAll(duties);
        dailyCountLabel.setText("Total: " + duties.size());
    }

    private void loadSpecialEvents() {
        LocalDate start = eventStartDateFilter.getValue();
        LocalDate end = eventEndDateFilter.getValue();

        List<SpecialDutyAssignment> events;
        if (start != null && end != null) {
            events = dutyRepository.findSpecialEventsBetweenDates(start, end);
        } else {
            events = dutyRepository.findAllActiveSpecialEvents();
        }
        specialEvents.setAll(events);
        eventsCountLabel.setText("Total: " + events.size());
    }

    private void loadWorkloadData() {
        // Implementation continues in next part...
        workloadData.clear();
        List<SpecialDutyAssignment> allDuties = dutyRepository.findByActiveTrue();

        Map<String, List<SpecialDutyAssignment>> byStaff = allDuties.stream()
                .collect(Collectors.groupingBy(SpecialDutyAssignment::getAssignedPersonName));

        for (Map.Entry<String, List<SpecialDutyAssignment>> entry : byStaff.entrySet()) {
            String staffName = entry.getKey();
            List<SpecialDutyAssignment> duties = entry.getValue();

            long dailyCount = duties.stream().filter(SpecialDutyAssignment::isDaily).count();
            long eventCount = duties.stream().filter(SpecialDutyAssignment::isSpecialEvent).count();

            WorkloadData data = new WorkloadData();
            data.setStaffName(staffName);
            data.setStaffType(!duties.isEmpty() ? duties.get(0).getStaffTypeDisplay() : "Unknown");
            data.setDailyDutiesCount((int) dailyCount);
            data.setSpecialEventsCount((int) eventCount);
            data.setTotalDuties(duties.size());
            data.setEstimatedHours(String.format("%.1f hrs", dailyCount * 0.5)); // Estimate

            workloadData.add(data);
        }
    }

    private void updateSummaries() {
        // Weekly load
        weeklyLoadLabel.setText(dailyDuties.size() + " duties across 7 days");

        // Most common duty
        if (!dailyDuties.isEmpty()) {
            Map<DutyType, Long> dutyCounts = dailyDuties.stream()
                    .collect(Collectors.groupingBy(SpecialDutyAssignment::getDutyType, Collectors.counting()));
            Optional<Map.Entry<DutyType, Long>> mostCommon = dutyCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue());
            mostCommon.ifPresent(entry ->
                    commonDutyLabel.setText(entry.getKey().getDisplayName() + " (" + entry.getValue() + ")"));
        }

        // Next event
        LocalDate today = LocalDate.now();
        Optional<SpecialDutyAssignment> nextEvent = specialEvents.stream()
                .filter(e -> e.getEventDate() != null && !e.getEventDate().isBefore(today))
                .min((a, b) -> a.getEventDate().compareTo(b.getEventDate()));
        nextEvent.ifPresent(event ->
                nextEventLabel.setText(event.getDisplayName() + " on " + event.getEventDate()));

        // This month count
        int thisMonth = (int) specialEvents.stream()
                .filter(e -> e.getEventDate() != null &&
                        e.getEventDate().getMonth() == today.getMonth() &&
                        e.getEventDate().getYear() == today.getYear())
                .count();
        monthEventsLabel.setText(thisMonth + " event" + (thisMonth != 1 ? "s" : ""));

        // Unconfirmed
        long unconfirmed = specialEvents.stream()
                .filter(e -> !Boolean.TRUE.equals(e.getConfirmedByStaff()))
                .count();
        unconfirmedLabel.setText(unconfirmed + " pending");
    }

    private void updateLastUpdatedLabel() {
        LocalTime now = LocalTime.now();
        lastUpdatedLabel.setText("Last updated: " + now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    // ========================================================================
    // EVENT HANDLERS
    // ========================================================================

    @FXML
    private void handleAddDailyDuty() {
        showDutyDialog(null, true);
    }

    @FXML
    private void handleAddSpecialEvent() {
        showDutyDialog(null, false);
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleDailyFilter() {
        // Apply filters to daily duties
        List<SpecialDutyAssignment> filtered = dutyRepository.findAllActiveDailyDuties();

        if (dailyDayFilter.getValue() != null) {
            filtered = filtered.stream()
                    .filter(d -> d.getDayOfWeek() == dailyDayFilter.getValue())
                    .collect(Collectors.toList());
        }

        if (dailyDutyTypeFilter.getValue() != null) {
            filtered = filtered.stream()
                    .filter(d -> d.getDutyType() == dailyDutyTypeFilter.getValue())
                    .collect(Collectors.toList());
        }

        String search = dailyStaffSearch.getText();
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            filtered = filtered.stream()
                    .filter(d -> d.getAssignedPersonName().toLowerCase().contains(searchLower))
                    .collect(Collectors.toList());
        }

        dailyDuties.setAll(filtered);
        dailyCountLabel.setText("Total: " + filtered.size());
    }

    @FXML
    private void handleClearDailyFilters() {
        dailyDayFilter.setValue(null);
        dailyDutyTypeFilter.setValue(null);
        dailyStaffSearch.clear();
        loadDailyDuties();
    }

    @FXML
    private void handleEventFilter() {
        loadSpecialEvents();
        DutyType type = eventTypeFilter.getValue();
        if (type != null) {
            List<SpecialDutyAssignment> filtered = specialEvents.stream()
                    .filter(e -> e.getDutyType() == type)
                    .collect(Collectors.toList());
            specialEvents.setAll(filtered);
            eventsCountLabel.setText("Total: " + filtered.size());
        }
    }

    @FXML
    private void handleClearEventFilters() {
        eventStartDateFilter.setValue(LocalDate.now().withDayOfMonth(1));
        eventEndDateFilter.setValue(LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()));
        eventTypeFilter.setValue(null);
        loadSpecialEvents();
    }

    @FXML
    private void handleExportRoster() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Duty Roster");
        fileChooser.setInitialFileName("duty_roster_" + LocalDate.now() + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = fileChooser.showSaveDialog(dutyTabPane.getScene().getWindow());
        if (file == null) return;

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Special Duty Roster Export");
            writer.println("Generated: " + LocalDate.now());
            writer.println();

            // Export Daily Duties
            writer.println("DAILY DUTIES");
            writer.println("Day,Duty Type,Location,Time,Assigned To,Staff Type,Priority,Confirmed");
            for (SpecialDutyAssignment duty : dailyDutiesTable.getItems()) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                    escapeCSV(duty.getDayOfWeek() != null ? duty.getDayOfWeek().name() : ""),
                    escapeCSV(duty.getDutyType() != null ? duty.getDutyType().getDisplayName() : ""),
                    escapeCSV(duty.getDutyLocation() != null ? duty.getDutyLocation() : ""),
                    formatTimeRange(duty.getStartTime(), duty.getEndTime()),
                    escapeCSV(duty.getAssignedPersonName()),
                    escapeCSV(duty.getStaffTypeDisplay()),
                    duty.getPriority() != null ? String.valueOf(duty.getPriority()) : "",
                    Boolean.TRUE.equals(duty.getConfirmedByStaff()) ? "Yes" : "No"
                ));
            }

            writer.println();
            writer.println("SPECIAL EVENTS");
            writer.println("Date,Duty Type,Location,Time,Assigned To,Staff Type,Priority,Completed");
            for (SpecialDutyAssignment event : specialEventsTable.getItems()) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s",
                    event.getEventDate() != null ? event.getEventDate().toString() : "",
                    escapeCSV(event.getDutyType() != null ? event.getDutyType().getDisplayName() : ""),
                    escapeCSV(event.getDutyLocation() != null ? event.getDutyLocation() : ""),
                    formatTimeRange(event.getStartTime(), event.getEndTime()),
                    escapeCSV(event.getAssignedPersonName()),
                    escapeCSV(event.getStaffTypeDisplay()),
                    event.getPriority() != null ? String.valueOf(event.getPriority()) : "",
                    Boolean.TRUE.equals(event.getCompleted()) ? "Yes" : "No"
                ));
            }

            showInfo("Export successful:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("Failed to export: " + e.getMessage());
        }
    }

    @FXML
    private void handlePrintSchedule() {
        String content = buildDutyRosterReport();

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            showError("No printer available.");
            return;
        }

        if (job.showPrintDialog(dutyTabPane.getScene().getWindow())) {
            Text text = new Text(content);
            text.setStyle("-fx-font-family: monospace; -fx-font-size: 10px;");
            TextFlow textFlow = new TextFlow(text);
            textFlow.setMaxWidth(500);

            boolean success = job.printPage(textFlow);
            if (success) {
                job.endJob();
                showInfo("Duty roster printed successfully.");
            } else {
                showError("Print job failed.");
            }
        }
    }

    private String buildDutyRosterReport() {
        StringBuilder report = new StringBuilder();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");

        report.append("================================================================================\n");
        report.append("                        SPECIAL DUTY ROSTER                                     \n");
        report.append("================================================================================\n\n");
        report.append("Generated: ").append(LocalDate.now().format(dateFormatter)).append("\n\n");

        report.append("DAILY DUTIES\n");
        report.append("--------------------------------------------------------------------------------\n");
        for (SpecialDutyAssignment duty : dailyDutiesTable.getItems()) {
            report.append(String.format("%-10s %-15s %-20s %s%n",
                duty.getDayOfWeek() != null ? duty.getDayOfWeek().name() : "",
                duty.getDutyType() != null ? duty.getDutyType().getDisplayName() : "",
                duty.getAssignedPersonName(),
                formatTimeRange(duty.getStartTime(), duty.getEndTime())
            ));
        }

        report.append("\nSPECIAL EVENTS\n");
        report.append("--------------------------------------------------------------------------------\n");
        for (SpecialDutyAssignment event : specialEventsTable.getItems()) {
            report.append(String.format("%-12s %-15s %-20s %s%n",
                event.getEventDate() != null ? event.getEventDate().toString() : "",
                event.getDutyType() != null ? event.getDutyType().getDisplayName() : "",
                event.getAssignedPersonName(),
                formatTimeRange(event.getStartTime(), event.getEndTime())
            ));
        }

        report.append("\n================================================================================\n");
        return report.toString();
    }

    private String formatTimeRange(LocalTime start, LocalTime end) {
        if (start == null && end == null) return "";
        String s = start != null ? start.format(DateTimeFormatter.ofPattern("h:mm a")) : "";
        String e = end != null ? end.format(DateTimeFormatter.ofPattern("h:mm a")) : "";
        return s + " - " + e;
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void handleEditDuty(SpecialDutyAssignment duty) {
        boolean isDaily = duty.isDaily();
        showDutyDialog(duty, isDaily);
    }

    private void handleDeleteDuty(SpecialDutyAssignment duty) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Duty");
        confirm.setHeaderText("Are you sure you want to delete this duty?");
        confirm.setContentText(duty.getDisplayName() + " assigned to " + duty.getAssignedPersonName());

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    dutyRepository.delete(duty);
                    loadData();
                    showSuccess("Duty deleted successfully!");
                } catch (Exception e) {
                    showError("Failed to delete duty: " + e.getMessage());
                }
            }
        });
    }

    // ========================================================================
    // DIALOG HELPERS
    // ========================================================================

    private void showDutyDialog(SpecialDutyAssignment existingDuty, boolean isDaily) {
        Dialog<SpecialDutyAssignment> dialog = new Dialog<>();
        dialog.setTitle(existingDuty == null ?
                (isDaily ? "Add Daily Duty" : "Add Special Event") :
                "Edit Duty Assignment");
        dialog.setHeaderText(isDaily ?
                "Create a recurring daily duty" :
                "Create a one-time special event duty");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Duty Type
        ComboBox<DutyType> dutyTypeCombo = new ComboBox<>();
        dutyTypeCombo.setItems(FXCollections.observableArrayList(
                isDaily ? DutyType.getDailyDuties() : DutyType.getSpecialEventDuties()));
        dutyTypeCombo.setConverter(new StringConverter<DutyType>() {
            @Override
            public String toString(DutyType type) {
                return type != null ? type.getDisplayName() : "";
            }
            @Override
            public DutyType fromString(String string) {
                return null;
            }
        });

        TextField locationField = new TextField();
        locationField.setPromptText("e.g., Main Entrance, Cafeteria");

        // Schedule fields
        ComboBox<DayOfWeek> dayCombo = new ComboBox<>();
        dayCombo.setItems(FXCollections.observableArrayList(DayOfWeek.values()));
        DatePicker eventDatePicker = new DatePicker(LocalDate.now().plusDays(1));

        // Use TimePickerField instead of TextField for better UX
        TimePickerField startTimeField = new TimePickerField();
        startTimeField.setTime(LocalTime.of(7, 30));
        TimePickerField endTimeField = new TimePickerField();
        endTimeField.setTime(LocalTime.of(8, 0));

        // Staff assignment
        ComboBox<Teacher> teacherCombo = new ComboBox<>();
        List<Teacher> teachers = sisDataService.getAllTeachers();
        teacherCombo.setItems(FXCollections.observableArrayList(teachers));
        teacherCombo.setConverter(new StringConverter<Teacher>() {
            @Override
            public String toString(Teacher teacher) {
                return teacher != null ? teacher.getName() : "";
            }
            @Override
            public Teacher fromString(String string) {
                return null;
            }
        });

        TextField staffNameField = new TextField();
        staffNameField.setPromptText("Or enter non-teacher staff name");

        ComboBox<Integer> priorityCombo = new ComboBox<>();
        priorityCombo.setItems(FXCollections.observableArrayList(1, 2, 3));
        priorityCombo.setConverter(new StringConverter<Integer>() {
            @Override
            public String toString(Integer priority) {
                if (priority == null) return "";
                return priority == 1 ? "High" : priority == 2 ? "Medium" : "Low";
            }
            @Override
            public Integer fromString(String string) {
                return null;
            }
        });
        priorityCombo.setValue(3);

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Additional notes...");
        notesArea.setPrefRowCount(2);

        // Populate if editing
        if (existingDuty != null) {
            dutyTypeCombo.setValue(existingDuty.getDutyType());
            locationField.setText(existingDuty.getDutyLocation());
            if (existingDuty.getDayOfWeek() != null) dayCombo.setValue(existingDuty.getDayOfWeek());
            if (existingDuty.getEventDate() != null) eventDatePicker.setValue(existingDuty.getEventDate());
            if (existingDuty.getStartTime() != null) startTimeField.setTime(existingDuty.getStartTime());
            if (existingDuty.getEndTime() != null) endTimeField.setTime(existingDuty.getEndTime());
            if (existingDuty.getTeacher() != null) teacherCombo.setValue(existingDuty.getTeacher());
            if (existingDuty.getStaffName() != null) staffNameField.setText(existingDuty.getStaffName());
            priorityCombo.setValue(existingDuty.getPriority());
            notesArea.setText(existingDuty.getNotes());
        }

        int row = 0;
        grid.add(new Label("Duty Type: *"), 0, row);
        grid.add(dutyTypeCombo, 1, row++);
        grid.add(new Label("Location:"), 0, row);
        grid.add(locationField, 1, row++);

        if (isDaily) {
            grid.add(new Label("Day of Week: *"), 0, row);
            grid.add(dayCombo, 1, row++);
        } else {
            grid.add(new Label("Event Date: *"), 0, row);
            grid.add(eventDatePicker, 1, row++);
        }

        grid.add(new Label("Start Time: *"), 0, row);
        grid.add(startTimeField, 1, row++);
        grid.add(new Label("End Time: *"), 0, row);
        grid.add(endTimeField, 1, row++);
        grid.add(new Label("Assign Teacher:"), 0, row);
        grid.add(teacherCombo, 1, row++);
        grid.add(new Label("Or Staff Name:"), 0, row);
        grid.add(staffNameField, 1, row++);
        grid.add(new Label("Priority:"), 0, row);
        grid.add(priorityCombo, 1, row++);
        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    SpecialDutyAssignment duty = existingDuty != null ? existingDuty : new SpecialDutyAssignment();

                    duty.setDutyType(dutyTypeCombo.getValue());
                    duty.setDutyLocation(locationField.getText());
                    duty.setIsRecurring(isDaily);

                    if (isDaily) {
                        duty.setDayOfWeek(dayCombo.getValue());
                    } else {
                        duty.setEventDate(eventDatePicker.getValue());
                    }

                    duty.setStartTime(startTimeField.getTime());
                    duty.setEndTime(endTimeField.getTime());
                    duty.setTeacher(teacherCombo.getValue());
                    duty.setStaffName(staffNameField.getText());
                    duty.setPriority(priorityCombo.getValue());
                    duty.setNotes(notesArea.getText());
                    duty.setActive(true);

                    return duty;
                } catch (Exception e) {
                    showError("Invalid input: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(duty -> {
            try {
                dutyRepository.save(duty);
                loadData();
                showSuccess("Duty saved successfully!");
            } catch (Exception e) {
                showError("Failed to save duty: " + e.getMessage());
            }
        });
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Operation Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========================================================================
    // INNER CLASS - WorkloadData
    // ========================================================================

    public static class WorkloadData {
        private String staffName;
        private String staffType;
        private Integer dailyDutiesCount = 0;
        private Integer specialEventsCount = 0;
        private Integer totalDuties = 0;
        private String estimatedHours = "0.0 hrs";

        // Getters and setters
        public String getStaffName() { return staffName; }
        public void setStaffName(String staffName) { this.staffName = staffName; }
        public String getStaffType() { return staffType; }
        public void setStaffType(String staffType) { this.staffType = staffType; }
        public Integer getDailyDutiesCount() { return dailyDutiesCount; }
        public void setDailyDutiesCount(Integer dailyDutiesCount) { this.dailyDutiesCount = dailyDutiesCount; }
        public Integer getSpecialEventsCount() { return specialEventsCount; }
        public void setSpecialEventsCount(Integer specialEventsCount) { this.specialEventsCount = specialEventsCount; }
        public Integer getTotalDuties() { return totalDuties; }
        public void setTotalDuties(Integer totalDuties) { this.totalDuties = totalDuties; }
        public String getEstimatedHours() { return estimatedHours; }
        public void setEstimatedHours(String estimatedHours) { this.estimatedHours = estimatedHours; }
    }
}
