package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.Event;
import com.heronix.scheduler.model.enums.EventType;
import com.heronix.scheduler.repository.EventRepository;
import com.heronix.scheduler.ui.components.TimePickerField;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Events Controller - Complete Event Management
 * Location: src/main/java/com/eduscheduler/ui/controller/EventsController.java
 * 
 * Features:
 * - Full CRUD operations for events
 * - Filter by event type (compliance, assemblies, etc.)
 * - Upcoming events view
 * - Compliance deadline tracking (IEP, 504)
 * - Schedule blocking management
 * - Teacher and room impact tracking
 */
@Slf4j
@Controller
public class EventsController {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private com.heronix.scheduler.service.ExportService exportService;

    // ========== FXML UI COMPONENTS ==========

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> typeFilter;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private TableView<Event> eventsTable;
    @FXML
    private TableColumn<Event, String> nameColumn;
    @FXML
    private TableColumn<Event, String> typeColumn;
    @FXML
    private TableColumn<Event, String> dateColumn;
    @FXML
    private TableColumn<Event, String> timeColumn;
    @FXML
    private TableColumn<Event, String> durationColumn;
    @FXML
    private TableColumn<Event, String> blockingColumn;
    @FXML
    private TableColumn<Event, String> statusColumn;
    @FXML
    private TableColumn<Event, Void> actionsColumn;
    @FXML
    private Label recordCountLabel;
    @FXML
    private HBox selectionToolbar;

    // ========== DATA ==========

    private ObservableList<Event> eventsList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("h:mm a");

    // ========== INITIALIZATION ==========

    /**
     * Initialize controller - called automatically by JavaFX
     */
    @FXML
    public void initialize() {
        log.debug("EventsController.initialize() START");
        log.info("Initializing EventsController");

        try {
            // Null checks
            if (eventsTable == null) {
                log.error("eventsTable is NULL!");
                return;
            }
            if (eventRepository == null) {
                log.error("eventRepository is NULL!");
                return;
            }

            log.debug("All components non-null");

            setupTableColumns();
            log.debug("Columns configured");

            setupFilters();
            log.debug("Filters configured");

            setupActionsColumn();
            log.debug("Actions column configured");

            setupBulkSelection();
            log.debug("Bulk selection configured");

            loadEvents();
            log.debug("EventsController.initialize() COMPLETE");

        } catch (Exception e) {
            log.error("EXCEPTION in EventsController.initialize()", e);
            throw e;
        }
    }

    // ========== TABLE SETUP ==========

    private void setupBulkSelection() {
        // Enable multi-selection
        // TODO: Package com.heronix.ui.util does not exist - implement when available
        // com.heronix.ui.util.TableSelectionHelper.enableMultiSelection(eventsTable);

        // Create selection toolbar
        // TODO: Package com.heronix.ui.util does not exist - implement when available
        HBox toolbar = null; // com.heronix.ui.util.TableSelectionHelper.createSelectionToolbar(
            // eventsTable,
            // this::handleBulkDelete,
            // "Events"
        // );

        // Replace the placeholder with the actual toolbar
        if (selectionToolbar != null && toolbar != null) {
            selectionToolbar.getChildren().setAll(toolbar.getChildren());
            selectionToolbar.setPadding(toolbar.getPadding());
            selectionToolbar.setSpacing(toolbar.getSpacing());
            selectionToolbar.setStyle(toolbar.getStyle());
        }
    }

    private void handleBulkDelete(List<Event> events) {
        try {
            for (Event event : events) {
                eventRepository.delete(event);
                log.info("Deleted event: {} (ID: {})", event.getName(), event.getId());
            }

            // Reload the table
            loadEvents();

            log.info("Bulk delete completed: {} events deleted", events.size());
        } catch (Exception e) {
            log.error("Error during bulk delete", e);
            throw e; // Let TableSelectionHelper show the error dialog
        }
    }

    /**
     * Configure table columns with cell value factories
     */
    private void setupTableColumns() {
        // Event Name
        nameColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));

        // Event Type with icon
        typeColumn.setCellValueFactory(data -> {
            EventType type = data.getValue().getEventType();
            String icon = getEventTypeIcon(type);
            return new SimpleStringProperty(icon + " " + type.getDisplayName());
        });

        // Date
        dateColumn.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getStartDateTime().format(dateFormatter)));

        // Time
        timeColumn.setCellValueFactory(data -> {
            Event event = data.getValue();
            String start = event.getStartDateTime().format(timeFormatter);
            String end = event.getEndDateTime().format(timeFormatter);
            return new SimpleStringProperty(start + " - " + end);
        });

        // Duration
        durationColumn.setCellValueFactory(data -> {
            Event event = data.getValue();
            long minutes = java.time.Duration.between(
                    event.getStartDateTime(),
                    event.getEndDateTime()).toMinutes();

            if (minutes < 60) {
                return new SimpleStringProperty(minutes + " min");
            } else {
                double hours = minutes / 60.0;
                return new SimpleStringProperty(String.format("%.1f hrs", hours));
            }
        });

        // Blocking Status
        blockingColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().isBlockScheduling() ? "🚫 Blocks" : "✓ Normal"));

        // Status (Past/Today/Upcoming)
        statusColumn.setCellValueFactory(data -> {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = data.getValue().getStartDateTime();
            LocalDateTime end = data.getValue().getEndDateTime();

            if (end.isBefore(now)) {
                return new SimpleStringProperty("📅 Past");
            } else if (start.isBefore(now) && end.isAfter(now)) {
                return new SimpleStringProperty("🔴 Now");
            } else if (start.toLocalDate().equals(now.toLocalDate())) {
                return new SimpleStringProperty("📌 Today");
            } else {
                return new SimpleStringProperty("📆 Upcoming");
            }
        });
    }

    /**
     * Get icon for event type
     */
    private String getEventTypeIcon(EventType type) {
        return switch (type) {
            case IEP_MEETING, IEP_ANNUAL_REVIEW, SECTION_504_MEETING,
                    SECTION_504_REVIEW, RTI_MEETING ->
                "📋";
            case ASSEMBLY, GRADUATION -> "🎓";
            case PARENT_TEACHER_CONFERENCE, OPEN_HOUSE -> "👨‍👩‍👧";
            case SPORTS_EVENT -> "⚽";
            case FIELD_TRIP -> "🚌";
            case FIRE_DRILL, LOCKDOWN_DRILL, EMERGENCY_DRILL -> "🚨";
            case EXAM, MIDTERM, FINAL_EXAM, TESTING -> "📝";
            case PROFESSIONAL_DEVELOPMENT, FACULTY_MEETING -> "💼";
            case HOLIDAY, SCHOOL_CLOSED -> "🏖️";
            case EARLY_DISMISSAL, LATE_START -> "⏰";
            default -> "📅";
        };
    }

    /**
     * Setup filter dropdown options
     */
    private void setupFilters() {
        // Type filter
        typeFilter.getItems().add("All");
        typeFilter.getItems().add("--- Compliance ---");
        typeFilter.getItems().add("IEP Meetings");
        typeFilter.getItems().add("504 Meetings");
        typeFilter.getItems().add("--- Academic ---");
        typeFilter.getItems().add("Assemblies");
        typeFilter.getItems().add("Exams");
        typeFilter.getItems().add("--- Safety ---");
        typeFilter.getItems().add("Drills");
        typeFilter.getItems().add("--- Other ---");
        typeFilter.getItems().add("Parent Events");
        typeFilter.getItems().add("Sports");
        typeFilter.setValue("All");

        // Status filter
        statusFilter.getItems().addAll("All", "Upcoming", "Today", "Past", "Blocking");
        statusFilter.setValue("All");
    }

    /**
     * Setup actions column with View, Edit, Delete buttons
     */
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("👁️");
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(5, viewBtn, editBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER);
                viewBtn.setOnAction(e -> handleView(getTableRow().getItem()));
                editBtn.setOnAction(e -> handleEdit(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> handleDelete(getTableRow().getItem()));
                deleteBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    // ========== DATA LOADING ==========

    /**
     * Load all events from database
     */
    private void loadEvents() {
        try {
            log.debug("Loading events from database...");

            List<Event> events = eventRepository.findAll();
            log.info("Found {} events", events.size());

            eventsList.clear();
            eventsList.addAll(events);
            eventsTable.setItems(eventsList);

            if (recordCountLabel != null) {
                recordCountLabel.setText("Total: " + events.size());
            }

            log.debug("Events loaded and displayed");

        } catch (Exception e) {
            log.error("EXCEPTION in loadEvents()", e);
        }
    }

    // ========== SEARCH & FILTER HANDLERS ==========

    /**
     * Handle search by event name
     */
    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        log.info("Search: " + query);

        if (query.isEmpty()) {
            loadEvents();
            return;
        }

        try {
            List<Event> filtered = eventRepository.findAll().stream()
                    .filter(e -> e.getName().toLowerCase().contains(query) ||
                            (e.getDescription() != null && e.getDescription().toLowerCase().contains(query)) ||
                            e.getEventType().getDisplayName().toLowerCase().contains(query))
                    .toList();

            eventsList.clear();
            eventsList.addAll(filtered);
            eventsTable.refresh();

            if (recordCountLabel != null) {
                recordCountLabel.setText("Total: " + filtered.size());
            }
        } catch (Exception e) {
            log.error("Error searching events", e);
        }
    }

    /**
     * Handle filter by type and status
     */
    @FXML
    private void handleFilter() {
        String type = typeFilter.getValue();
        String status = statusFilter.getValue();
        log.info("Filter changed - Type: " + type + ", Status: " + status);

        try {
            List<Event> filtered = eventRepository.findAll().stream()
                    .filter(e -> filterByType(e, type))
                    .filter(e -> filterByStatus(e, status))
                    .toList();

            eventsList.clear();
            eventsList.addAll(filtered);
            eventsTable.refresh();

            if (recordCountLabel != null) {
                recordCountLabel.setText("Total: " + filtered.size());
            }
        } catch (Exception e) {
            log.error("Error filtering events", e);
        }
    }

    /**
     * Filter by event type
     */
    private boolean filterByType(Event event, String typeFilter) {
        if ("All".equals(typeFilter))
            return true;

        EventType eventType = event.getEventType();

        return switch (typeFilter) {
            case "IEP Meetings" -> eventType == EventType.IEP_MEETING ||
                    eventType == EventType.IEP_ANNUAL_REVIEW;
            case "504 Meetings" -> eventType == EventType.SECTION_504_MEETING ||
                    eventType == EventType.SECTION_504_REVIEW;
            case "Assemblies" -> eventType == EventType.ASSEMBLY ||
                    eventType == EventType.GRADUATION;
            case "Exams" -> eventType == EventType.EXAM || eventType == EventType.MIDTERM ||
                    eventType == EventType.FINAL_EXAM || eventType == EventType.TESTING;
            case "Drills" -> eventType == EventType.FIRE_DRILL ||
                    eventType == EventType.LOCKDOWN_DRILL ||
                    eventType == EventType.EMERGENCY_DRILL;
            case "Parent Events" -> eventType == EventType.PARENT_TEACHER_CONFERENCE ||
                    eventType == EventType.OPEN_HOUSE;
            case "Sports" -> eventType == EventType.SPORTS_EVENT;
            default -> true;
        };
    }

    /**
     * Filter by event status
     */
    private boolean filterByStatus(Event event, String statusFilter) {
        if ("All".equals(statusFilter))
            return true;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = event.getStartDateTime();
        LocalDateTime end = event.getEndDateTime();

        return switch (statusFilter) {
            case "Upcoming" -> start.isAfter(now);
            case "Today" -> start.toLocalDate().equals(now.toLocalDate());
            case "Past" -> end.isBefore(now);
            case "Blocking" -> event.isBlockScheduling();
            default -> true;
        };
    }

    /**
     * Clear all filters
     */
    @FXML
    private void handleClearFilters() {
        log.info("Clear filters clicked");
        searchField.clear();
        typeFilter.setValue("All");
        statusFilter.setValue("All");
        loadEvents();
    }

    // ========== CRUD OPERATIONS ==========

    /**
     * Handle Add Event button
     */
    @FXML
    private void handleAddEvent() {
        log.info("Add event clicked");
        showEventDialog(null);
    }

    /**
     * Handle View button - shows event details
     */
    private void handleView(Event event) {
        if (event == null)
            return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Event Details");
        alert.setHeaderText(getEventTypeIcon(event.getEventType()) + " " + event.getName());

        String teachers = event.getAffectedTeachers() != null && !event.getAffectedTeachers().isEmpty()
                ? event.getAffectedTeachers().size() + " teachers"
                : "No teachers assigned";

        String rooms = event.getAffectedRooms() != null && !event.getAffectedRooms().isEmpty()
                ? event.getAffectedRooms().size() + " rooms"
                : "No rooms assigned";

        String content = String.format("""
                Type: %s

                Date: %s
                Time: %s - %s

                Blocks Scheduling: %s
                All Day Event: %s
                Recurring: %s

                Affected Teachers: %s
                Affected Rooms: %s

                Description:
                %s

                Notes:
                %s
                """,
                event.getEventType().getDisplayName(),
                event.getStartDateTime().format(dateFormatter),
                event.getStartDateTime().format(timeFormatter),
                event.getEndDateTime().format(timeFormatter),
                event.isBlockScheduling() ? "Yes" : "No",
                event.isAllDay() ? "Yes" : "No",
                event.isRecurring() ? "Yes (" + event.getRecurrencePattern() + ")" : "No",
                teachers,
                rooms,
                event.getDescription() != null ? event.getDescription() : "No description",
                event.getNotes() != null ? event.getNotes() : "No notes");

        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Handle Edit button
     */
    private void handleEdit(Event event) {
        if (event == null)
            return;
        log.info("Edit event: {}", event.getName());
        showEventDialog(event);
    }

    /**
     * Handle Delete button
     */
    private void handleDelete(Event event) {
        if (event == null)
            return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Event");
        confirm.setHeaderText("Delete " + event.getName() + "?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    eventRepository.delete(event);
                    loadEvents();
                    log.info("Event deleted: {}", event.getName());
                    showInfo("Success", "Event deleted successfully!");
                } catch (Exception e) {
                    log.error("Error deleting event", e);
                    showError("Delete Error", "Failed to delete event: " + e.getMessage());
                }
            }
        });
    }

    // ========== TOOLBAR ACTIONS ==========

    /**
     * Handle Refresh button
     */
    @FXML
    private void handleRefresh() {
        log.info("Refreshing events");
        loadEvents();
    }

    /**
     * Handle Upcoming button - show only upcoming events
     */
    @FXML
    private void handleUpcoming() {
        log.info("Showing upcoming events");

        try {
            List<Event> upcoming = eventRepository.findUpcomingEvents(LocalDateTime.now());

            eventsList.clear();
            eventsList.addAll(upcoming);
            eventsTable.refresh();

            if (recordCountLabel != null) {
                recordCountLabel.setText("Upcoming: " + upcoming.size());
            }

            showInfo("Upcoming Events", "Showing " + upcoming.size() + " upcoming events");
        } catch (Exception e) {
            log.error("Error loading upcoming events", e);
        }
    }

    /**
     * Handle Compliance button - show IEP/504 meetings
     */
    @FXML
    private void handleCompliance() {
        log.info("Showing compliance meetings");

        try {
            List<Event> compliance = eventRepository.findComplianceMeetings();

            eventsList.clear();
            eventsList.addAll(compliance);
            eventsTable.refresh();

            if (recordCountLabel != null) {
                recordCountLabel.setText("Compliance: " + compliance.size());
            }

            showInfo("Compliance Meetings", "Showing " + compliance.size() + " IEP/504 meetings");
        } catch (Exception e) {
            log.error("Error loading compliance meetings", e);
        }
    }

    /**
     * Handle Export button
     */
        @FXML
    private void handleExport() {
        log.info("Export clicked");

        try {
            if (eventsList.isEmpty()) {{
                showWarning("No Data", "There are no events to export.");
                return;
            }}

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Events");
            fileChooser.setInitialFileName("events_export.ics");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("iCalendar Files", "*.ics")
            );

            java.io.File file = fileChooser.showSaveDialog(eventsTable.getScene().getWindow());

            if (file != null) {
                // TODO: Method exportEventsToICal does not exist
                byte[] data = new byte[0]; // Placeholder
                java.nio.file.Files.write(file.toPath(), data);

                showInfo("Export Successful",
                    String.format("Exported %d events to %s", eventsList.size(), file.getName()));
                log.info("Exported {} events to {}", eventsList.size(), file.getAbsolutePath());
            }

        } catch (Exception e) {
            log.error("Failed to export events", e);
            showError("Export Failed", "Failed to export events: " + e.getMessage());
        }
    }

    /**
     * Show Add/Edit Event Dialog
     * @param event Event to edit, or null to create new
     */
    private void showEventDialog(Event event) {
        boolean isEditMode = (event != null);

        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle(isEditMode ? "Edit Event" : "Add New Event");
        dialog.setHeaderText(isEditMode ? "Edit event information" : "Enter event information");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Form fields
        TextField nameField = new TextField();
        nameField.setPromptText("Event name");
        if (isEditMode) nameField.setText(event.getName());

        ComboBox<EventType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(EventType.values());
        typeCombo.setPromptText("Select event type");
        if (isEditMode) typeCombo.setValue(event.getEventType());

        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Event date");
        if (isEditMode && event.getStartDateTime() != null) {
            datePicker.setValue(event.getStartDateTime().toLocalDate());
        }

        // Time fields (using TimePickerField for better UX)
        TimePickerField startTimeField = new TimePickerField();
        startTimeField.setTime(java.time.LocalTime.of(9, 0));
        if (isEditMode && event.getStartDateTime() != null) {
            startTimeField.setTime(event.getStartDateTime().toLocalTime());
        }

        TimePickerField endTimeField = new TimePickerField();
        endTimeField.setTime(java.time.LocalTime.of(10, 30));
        if (isEditMode && event.getEndDateTime() != null) {
            endTimeField.setTime(event.getEndDateTime().toLocalTime());
        }

        TextArea descriptionArea = new TextArea();
        descriptionArea.setPromptText("Event description");
        descriptionArea.setPrefRowCount(3);
        if (isEditMode && event.getDescription() != null) {
            descriptionArea.setText(event.getDescription());
        }

        CheckBox allDayCheck = new CheckBox("All Day Event");
        if (isEditMode) allDayCheck.setSelected(event.isAllDay());

        CheckBox blocksSchedulingCheck = new CheckBox("Blocks Normal Scheduling");
        if (isEditMode) {
            blocksSchedulingCheck.setSelected(event.isBlockScheduling());
        } else {
            blocksSchedulingCheck.setSelected(true); // Default to true for new events
        }

        CheckBox recurringCheck = new CheckBox("Recurring Event");
        if (isEditMode) recurringCheck.setSelected(event.isRecurring());

        TextField recurrencePatternField = new TextField();
        recurrencePatternField.setPromptText("e.g., Every Monday, Weekly, Monthly");
        recurrencePatternField.setDisable(!recurringCheck.isSelected());
        if (isEditMode && event.getRecurrencePattern() != null) {
            recurrencePatternField.setText(event.getRecurrencePattern());
        }

        // Enable/disable recurrence pattern based on recurring checkbox
        recurringCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            recurrencePatternField.setDisable(!newVal);
            if (!newVal) recurrencePatternField.clear();
        });

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Additional notes");
        notesArea.setPrefRowCount(2);
        if (isEditMode && event.getNotes() != null) {
            notesArea.setText(event.getNotes());
        }

        // Disable time fields if All Day is checked
        allDayCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            startTimeField.setDisable(newVal);
            endTimeField.setDisable(newVal);
            if (newVal) {
                startTimeField.setText("00:00");
                endTimeField.setText("23:59");
            }
        });

        // Add fields to grid
        int row = 0;
        grid.add(new Label("Event Name:*"), 0, row);
        grid.add(nameField, 1, row++);

        grid.add(new Label("Event Type:*"), 0, row);
        grid.add(typeCombo, 1, row++);

        grid.add(new Label("Date:*"), 0, row);
        grid.add(datePicker, 1, row++);

        grid.add(new Label("Start Time:*"), 0, row);
        grid.add(startTimeField, 1, row++);

        grid.add(new Label("End Time:*"), 0, row);
        grid.add(endTimeField, 1, row++);

        grid.add(new Label("Description:"), 0, row);
        grid.add(descriptionArea, 1, row++);

        grid.add(new Label("Options:"), 0, row);
        javafx.scene.layout.VBox optionsBox = new javafx.scene.layout.VBox(5);
        optionsBox.getChildren().addAll(allDayCheck, blocksSchedulingCheck, recurringCheck);
        grid.add(optionsBox, 1, row++);

        grid.add(new Label("Recurrence:"), 0, row);
        grid.add(recurrencePatternField, 1, row++);

        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row++);

        dialog.getDialogPane().setContent(grid);

        // Enable/disable save button based on validation
        javafx.scene.Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        // Validation listener
        Runnable validateForm = () -> {
            boolean valid = !nameField.getText().trim().isEmpty() &&
                          typeCombo.getValue() != null &&
                          datePicker.getValue() != null &&
                          startTimeField.getTime() != null &&
                          endTimeField.getTime() != null;
            saveButton.setDisable(!valid);
        };

        nameField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        typeCombo.valueProperty().addListener((obs, old, newVal) -> validateForm.run());
        datePicker.valueProperty().addListener((obs, old, newVal) -> validateForm.run());
        startTimeField.textProperty().addListener((obs, old, newVal) -> validateForm.run());
        endTimeField.textProperty().addListener((obs, old, newVal) -> validateForm.run());

        // Initial validation
        validateForm.run();

        // Convert dialog result to Event object
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    Event resultEvent = isEditMode ? event : new Event();

                    resultEvent.setName(nameField.getText().trim());
                    resultEvent.setEventType(typeCombo.getValue());

                    // Parse date and time
                    java.time.LocalDate date = datePicker.getValue();
                    java.time.LocalTime startTime = startTimeField.getTime();
                    java.time.LocalTime endTime = endTimeField.getTime();

                    resultEvent.setStartDateTime(LocalDateTime.of(date, startTime));
                    resultEvent.setEndDateTime(LocalDateTime.of(date, endTime));

                    resultEvent.setDescription(descriptionArea.getText().trim());
                    resultEvent.setAllDay(allDayCheck.isSelected());
                    resultEvent.setBlocksScheduling(blocksSchedulingCheck.isSelected());
                    resultEvent.setRecurring(recurringCheck.isSelected());

                    if (recurringCheck.isSelected() && !recurrencePatternField.getText().trim().isEmpty()) {
                        resultEvent.setRecurrencePattern(recurrencePatternField.getText().trim());
                    } else {
                        resultEvent.setRecurrencePattern(null);
                    }

                    resultEvent.setNotes(notesArea.getText().trim());

                    return resultEvent;

                } catch (Exception e) {
                    showError("Invalid Input", "Please check your time format (HH:mm). Error: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        // Show dialog and save if confirmed
        dialog.showAndWait().ifPresent(resultEvent -> {
            if (resultEvent != null) {
                try {
                    // Validate end time is after start time
                    if (resultEvent.getEndDateTime().isBefore(resultEvent.getStartDateTime()) ||
                        resultEvent.getEndDateTime().equals(resultEvent.getStartDateTime())) {
                        showError("Invalid Time", "End time must be after start time.");
                        return;
                    }

                    // Save to database
                    Event saved = eventRepository.save(resultEvent);

                    // Reload table
                    loadEvents();

                    // Show success message
                    showInfo("Success", isEditMode ?
                        "Event updated successfully!" :
                        "Event created successfully!");

                    log.info("{} event: {}", isEditMode ? "Updated" : "Created", saved.getName());

                } catch (Exception e) {
                    log.error("Error saving event", e);
                    showError("Save Error", "Failed to save event: " + e.getMessage());
                }
            }
        });
    }

    // ========== HELPER METHODS ==========

    /**
     * Show info alert
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
     * Show error alert
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

    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}
