package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.CourseRoomAssignment;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.enums.RoomAssignmentType;
import com.heronix.scheduler.model.enums.UsagePattern;
import com.heronix.scheduler.repository.CourseRoomAssignmentRepository;
import com.heronix.scheduler.repository.RoomRepository;
import com.heronix.scheduler.service.MultiRoomSchedulingService;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for Multi-Room Assignment Dialog
 * Phase 6E-4: Multi-Room Courses UI
 *
 * Allows administrators to assign multiple rooms to a course with:
 * - Room selection and assignment types
 * - Usage patterns (always, alternating, time-based)
 * - Priority ordering
 * - Proximity preferences
 *
 * Supports:
 * - Team teaching
 * - Lab/lecture splits
 * - Overflow rooms
 * - Co-teaching models
 * - Rotating schedules
 *
 * @since Phase 6E-4 - December 3, 2025
 */
@Slf4j
@Component
public class MultiRoomAssignmentDialogController {

    @FXML private Label courseNameLabel;
    @FXML private CheckBox usesMultipleRoomsCheckBox;
    @FXML private HBox distancePreferenceBox;
    @FXML private Spinner<Integer> maxDistanceSpinner;
    @FXML private VBox assignmentsSection;
    @FXML private Button addRoomButton;
    @FXML private TableView<RoomAssignmentRow> roomAssignmentsTable;
    @FXML private TableColumn<RoomAssignmentRow, Room> roomColumn;
    @FXML private TableColumn<RoomAssignmentRow, RoomAssignmentType> typeColumn;
    @FXML private TableColumn<RoomAssignmentRow, UsagePattern> patternColumn;
    @FXML private TableColumn<RoomAssignmentRow, Integer> priorityColumn;
    @FXML private TableColumn<RoomAssignmentRow, Boolean> activeColumn;
    @FXML private TableColumn<RoomAssignmentRow, String> notesColumn;
    @FXML private TableColumn<RoomAssignmentRow, Void> actionsColumn;
    @FXML private VBox validationMessageBox;
    @FXML private Label validationMessageLabel;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private CourseRoomAssignmentRepository assignmentRepository;

    @Autowired
    private MultiRoomSchedulingService multiRoomService;

    private Course course;
    private List<Room> availableRooms;
    private ObservableList<RoomAssignmentRow> assignments;

    /**
     * Initialize the dialog with course data
     *
     * @param course Course to configure multi-room assignments for
     */
    public void initializeWithCourse(Course course) {
        this.course = course;
        this.availableRooms = roomRepository.findAll();
        this.assignments = FXCollections.observableArrayList();

        // Set course name
        courseNameLabel.setText(course.getCourseCode() + " - " + course.getCourseName());

        // Load existing state
        loadExistingAssignments();

        // Setup UI components
        setupSpinner();
        setupTable();
        setupListeners();

        // Update UI state
        updateUIState();
    }

    /**
     * Load existing multi-room assignments from course
     */
    private void loadExistingAssignments() {
        // Set multi-room checkbox
        usesMultipleRoomsCheckBox.setSelected(
            Boolean.TRUE.equals(course.getUsesMultipleRooms())
        );

        // Set max distance
        if (course.getMaxRoomDistanceMinutes() != null) {
            maxDistanceSpinner.getValueFactory().setValue(course.getMaxRoomDistanceMinutes());
        }

        // Load room assignments
        List<CourseRoomAssignment> existingAssignments = assignmentRepository.findByCourse(course);
        for (CourseRoomAssignment assignment : existingAssignments) {
            assignments.add(new RoomAssignmentRow(assignment));
        }

        // If no assignments exist and multi-room is enabled, add a default primary room
        if (assignments.isEmpty() && usesMultipleRoomsCheckBox.isSelected()) {
            addNewRoomAssignment();
        }
    }

    /**
     * Setup spinner for max distance
     */
    private void setupSpinner() {
        SpinnerValueFactory<Integer> valueFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(
            1, 15, 5, 1
        );
        maxDistanceSpinner.setValueFactory(valueFactory);
        maxDistanceSpinner.setEditable(true);
    }

    /**
     * Setup table columns with editors
     */
    private void setupTable() {
        roomAssignmentsTable.setItems(assignments);
        roomAssignmentsTable.setEditable(true);

        // Room column with ComboBox editor
        roomColumn.setCellValueFactory(cellData -> cellData.getValue().roomProperty());
        roomColumn.setCellFactory(column -> new ComboBoxTableCell<>(
            FXCollections.observableArrayList(availableRooms)
        ) {
            @Override
            public void updateItem(Room room, boolean empty) {
                super.updateItem(room, empty);
                if (!empty && room != null) {
                    setText(room.getRoomNumber());
                } else {
                    setText(null);
                }
            }
        });
        roomColumn.setOnEditCommit(event -> {
            event.getRowValue().setRoom(event.getNewValue());
        });

        // Type column with ComboBox editor
        typeColumn.setCellValueFactory(cellData -> cellData.getValue().assignmentTypeProperty());
        typeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(
            FXCollections.observableArrayList(RoomAssignmentType.values())
        ));
        typeColumn.setOnEditCommit(event -> {
            event.getRowValue().setAssignmentType(event.getNewValue());
        });

        // Pattern column with ComboBox editor
        patternColumn.setCellValueFactory(cellData -> cellData.getValue().usagePatternProperty());
        patternColumn.setCellFactory(ComboBoxTableCell.forTableColumn(
            FXCollections.observableArrayList(UsagePattern.values())
        ));
        patternColumn.setOnEditCommit(event -> {
            event.getRowValue().setUsagePattern(event.getNewValue());
        });

        // Priority column with text editor
        priorityColumn.setCellValueFactory(cellData -> cellData.getValue().priorityProperty().asObject());
        priorityColumn.setCellFactory(TextFieldTableCell.forTableColumn(
            new StringConverter<Integer>() {
                @Override
                public String toString(Integer value) {
                    return value != null ? value.toString() : "1";
                }

                @Override
                public Integer fromString(String string) {
                    try {
                        return Integer.parseInt(string);
                    } catch (NumberFormatException e) {
                        return 1;
                    }
                }
            }
        ));
        priorityColumn.setOnEditCommit(event -> {
            event.getRowValue().setPriority(event.getNewValue());
        });

        // Active column with checkbox
        activeColumn.setCellValueFactory(cellData -> cellData.getValue().activeProperty());
        activeColumn.setCellFactory(CheckBoxTableCell.forTableColumn(activeColumn));

        // Notes column with text editor
        notesColumn.setCellValueFactory(cellData -> cellData.getValue().notesProperty());
        notesColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        notesColumn.setOnEditCommit(event -> {
            event.getRowValue().setNotes(event.getNewValue());
        });

        // Actions column with delete button
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");

            {
                deleteButton.getStyleClass().add("delete-button");
                deleteButton.setOnAction(event -> {
                    RoomAssignmentRow row = getTableRow().getItem();
                    if (row != null) {
                        deleteRoomAssignment(row);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(deleteButton);
                }
            }
        });
    }

    /**
     * Setup event listeners
     */
    private void setupListeners() {
        // Enable/disable sections based on checkbox
        usesMultipleRoomsCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            updateUIState();
            if (newVal && assignments.isEmpty()) {
                addNewRoomAssignment();
            }
        });

        // Add room button
        addRoomButton.setOnAction(event -> addNewRoomAssignment());
    }

    /**
     * Update UI state based on multi-room checkbox
     */
    private void updateUIState() {
        boolean enabled = usesMultipleRoomsCheckBox.isSelected();
        distancePreferenceBox.setDisable(!enabled);
        assignmentsSection.setDisable(!enabled);
    }

    /**
     * Add a new room assignment row
     */
    private void addNewRoomAssignment() {
        RoomAssignmentRow newRow = new RoomAssignmentRow();

        // Set defaults
        newRow.setAssignmentType(
            assignments.isEmpty() ? RoomAssignmentType.PRIMARY : RoomAssignmentType.SECONDARY
        );
        newRow.setUsagePattern(UsagePattern.ALWAYS);
        newRow.setPriority(assignments.size() + 1);
        newRow.setActive(true);

        assignments.add(newRow);
        roomAssignmentsTable.scrollTo(newRow);
        roomAssignmentsTable.getSelectionModel().select(newRow);
    }

    /**
     * Delete a room assignment row
     */
    private void deleteRoomAssignment(RoomAssignmentRow row) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Room Assignment?");
        confirmAlert.setContentText("Remove " +
            (row.getRoom() != null ? row.getRoom().getRoomNumber() : "this room") +
            " from course assignments?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                assignments.remove(row);
            }
        });
    }

    /**
     * Validate assignments before saving
     */
    public boolean validateAssignments() {
        validationMessageBox.setVisible(false);
        validationMessageBox.setManaged(false);

        if (!usesMultipleRoomsCheckBox.isSelected()) {
            return true; // No validation needed if multi-room is disabled
        }

        // Check for at least one primary room
        boolean hasPrimary = assignments.stream()
            .anyMatch(row -> row.getAssignmentType() == RoomAssignmentType.PRIMARY && row.getActive());

        if (!hasPrimary) {
            showValidationError("At least one PRIMARY room assignment is required");
            return false;
        }

        // Check for duplicate rooms
        long uniqueRooms = assignments.stream()
            .filter(RoomAssignmentRow::getActive)
            .map(RoomAssignmentRow::getRoom)
            .filter(room -> room != null)
            .distinct()
            .count();

        long totalActiveRooms = assignments.stream()
            .filter(RoomAssignmentRow::getActive)
            .filter(row -> row.getRoom() != null)
            .count();

        if (uniqueRooms < totalActiveRooms) {
            showValidationError("Cannot assign the same room multiple times");
            return false;
        }

        // Check that all active assignments have rooms selected
        boolean missingRooms = assignments.stream()
            .filter(RoomAssignmentRow::getActive)
            .anyMatch(row -> row.getRoom() == null);

        if (missingRooms) {
            showValidationError("All active room assignments must have a room selected");
            return false;
        }

        return true;
    }

    /**
     * Show validation error message
     */
    private void showValidationError(String message) {
        validationMessageLabel.setText("⚠ " + message);
        validationMessageBox.setVisible(true);
        validationMessageBox.setManaged(true);
        validationMessageLabel.getStyleClass().setAll("error-message");
    }

    /**
     * Save assignments back to course
     */
    public void saveAssignments() {
        // Update course fields
        course.setUsesMultipleRooms(usesMultipleRoomsCheckBox.isSelected());
        course.setMaxRoomDistanceMinutes(maxDistanceSpinner.getValue());

        if (!usesMultipleRoomsCheckBox.isSelected()) {
            // Clear all assignments if multi-room is disabled
            assignmentRepository.deleteByCourse(course);
            return;
        }

        // Convert rows to entities
        List<CourseRoomAssignment> newAssignments = new ArrayList<>();
        for (RoomAssignmentRow row : assignments) {
            if (row.getRoom() == null) {
                continue; // Skip incomplete rows
            }

            CourseRoomAssignment assignment = new CourseRoomAssignment();
            assignment.setCourse(course);
            assignment.setRoom(row.getRoom());
            assignment.setAssignmentType(row.getAssignmentType());
            assignment.setUsagePattern(row.getUsagePattern());
            assignment.setPriority(row.getPriority());
            assignment.setActive(row.getActive());
            assignment.setNotes(row.getNotes());

            newAssignments.add(assignment);
        }

        // Delete existing assignments and save new ones
        assignmentRepository.deleteByCourse(course);
        assignmentRepository.saveAll(newAssignments);

        log.info("Saved {} room assignments for course {}",
            newAssignments.size(), course.getCourseCode());
    }

    /**
     * Table row model for room assignments
     */
    public static class RoomAssignmentRow {
        private final ObjectProperty<Room> room = new SimpleObjectProperty<>();
        private final ObjectProperty<RoomAssignmentType> assignmentType = new SimpleObjectProperty<>();
        private final ObjectProperty<UsagePattern> usagePattern = new SimpleObjectProperty<>();
        private final IntegerProperty priority = new SimpleIntegerProperty();
        private final BooleanProperty active = new SimpleBooleanProperty();
        private final StringProperty notes = new SimpleStringProperty();

        public RoomAssignmentRow() {
            // Default constructor
        }

        public RoomAssignmentRow(CourseRoomAssignment assignment) {
            this.room.set(assignment.getRoom());
            this.assignmentType.set(assignment.getAssignmentType());
            this.usagePattern.set(assignment.getUsagePattern());
            this.priority.set(assignment.getPriority() != null ? assignment.getPriority() : 1);
            this.active.set(assignment.getActive() != null ? assignment.getActive() : true);
            this.notes.set(assignment.getNotes());
        }

        // Property getters
        public ObjectProperty<Room> roomProperty() { return room; }
        public ObjectProperty<RoomAssignmentType> assignmentTypeProperty() { return assignmentType; }
        public ObjectProperty<UsagePattern> usagePatternProperty() { return usagePattern; }
        public IntegerProperty priorityProperty() { return priority; }
        public BooleanProperty activeProperty() { return active; }
        public StringProperty notesProperty() { return notes; }

        // Value getters/setters
        public Room getRoom() { return room.get(); }
        public void setRoom(Room value) { room.set(value); }

        public RoomAssignmentType getAssignmentType() { return assignmentType.get(); }
        public void setAssignmentType(RoomAssignmentType value) { assignmentType.set(value); }

        public UsagePattern getUsagePattern() { return usagePattern.get(); }
        public void setUsagePattern(UsagePattern value) { usagePattern.set(value); }

        public Integer getPriority() { return priority.get(); }
        public void setPriority(Integer value) { priority.set(value); }

        public Boolean getActive() { return active.get(); }
        public void setActive(Boolean value) { active.set(value); }

        public String getNotes() { return notes.get(); }
        public void setNotes(String value) { notes.set(value); }
    }
}
