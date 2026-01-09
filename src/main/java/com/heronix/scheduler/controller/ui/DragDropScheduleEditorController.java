package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.CourseSection;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.repository.CourseSectionRepository;
import com.heronix.scheduler.repository.RoomRepository;
import com.heronix.scheduler.service.ConflictDetectionService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.heronix.scheduler.service.data.SISDataService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Drag-Drop Schedule Editor Controller
 *
 * Provides intuitive visual schedule editing with drag-and-drop functionality.
 * Features:
 * - Drag-and-drop section movement
 * - Conflict detection and highlighting
 * - Undo/redo support
 * - Teacher/room/period swapping
 * - Real-time validation
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/DragDropScheduleEditorController.java
 */
@Component
@Slf4j
public class DragDropScheduleEditorController {

    @Autowired
    private SISDataService sisDataService;
    @Autowired
    private CourseSectionRepository courseSectionRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ConflictDetectionService conflictDetectionService;

    // Header Controls
    @FXML private ToggleButton editModeToggle;
    @FXML private Button undoButton;
    @FXML private Button redoButton;

    // Status Labels
    @FXML private Label editModeStatusLabel;
    @FXML private Label changesStatusLabel;
    @FXML private Label conflictsStatusLabel;

    // Filters
    @FXML private ComboBox<String> teacherFilterComboBox;
    @FXML private ComboBox<String> roomFilterComboBox;
    @FXML private ComboBox<String> gradeFilterComboBox;
    @FXML private CheckBox showConflictsCheckbox;

    // Schedule Grid
    @FXML private GridPane scheduleGrid;
    @FXML private VBox scheduleGridContainer;

    // Details Panel
    @FXML private VBox sectionDetailsContainer;
    @FXML private Label noSelectionLabel;
    @FXML private VBox sectionDetailsBox;
    @FXML private Label detailCourseLabel;
    @FXML private Label detailSectionLabel;
    @FXML private Label detailTeacherLabel;
    @FXML private Label detailRoomLabel;
    @FXML private Label detailPeriodLabel;
    @FXML private Label detailEnrollmentLabel;

    // Lists
    @FXML private ListView<String> conflictsList;
    @FXML private ListView<String> recentChangesList;
    @FXML private VBox suggestionsContainer;

    private static final int NUM_PERIODS = 8;
    private static final DataFormat SECTION_DATA_FORMAT = new DataFormat("application/section-id");

    private boolean editMode = false;
    private CourseSection selectedSection;
    private Map<Long, CourseSection> modifiedSections = new HashMap<>();
    private Stack<ScheduleChange> undoStack = new Stack<>();
    private Stack<ScheduleChange> redoStack = new Stack<>();
    private Map<String, VBox> cellMap = new HashMap<>();

    @FXML
    public void initialize() {
        log.info("Initializing Drag-Drop Schedule Editor Controller");

        setupFilters();
        loadScheduleData();
    }

    private void setupFilters() {
        // Teacher filter
        if (teacherFilterComboBox != null) {
            List<Teacher> teachers = sisDataService.getAllTeachers();
            ObservableList<String> teacherNames = FXCollections.observableArrayList("All Teachers");
            teacherNames.addAll(teachers.stream()
                .map(t -> t.getFirstName() + " " + t.getLastName())
                .collect(Collectors.toList()));
            teacherFilterComboBox.setItems(teacherNames);
            teacherFilterComboBox.setValue("All Teachers");
        }

        // Room filter
        if (roomFilterComboBox != null) {
            List<Room> rooms = roomRepository.findAll();
            ObservableList<String> roomNumbers = FXCollections.observableArrayList("All Rooms");
            roomNumbers.addAll(rooms.stream()
                .map(Room::getRoomNumber)
                .collect(Collectors.toList()));
            roomFilterComboBox.setItems(roomNumbers);
            roomFilterComboBox.setValue("All Rooms");
        }

        // Grade filter
        if (gradeFilterComboBox != null) {
            ObservableList<String> grades = FXCollections.observableArrayList(
                "All Grades", "9", "10", "11", "12"
            );
            gradeFilterComboBox.setItems(grades);
            gradeFilterComboBox.setValue("All Grades");
        }
    }

    @FXML
    private void handleEditModeToggle() {
        editMode = editModeToggle.isSelected();

        if (editMode) {
            safeSetText(editModeStatusLabel, "🔓 Edit Mode ACTIVE");
            editModeStatusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
            editModeToggle.setText("🔒 Lock Mode");
        } else {
            safeSetText(editModeStatusLabel, "📌 View Mode");
            editModeStatusLabel.setStyle("-fx-text-fill: #388e3c; -fx-font-weight: bold;");
            editModeToggle.setText("🔓 Edit Mode");
        }

        updateGridInteractivity();
    }

    @FXML
    private void handleUndo() {
        if (undoStack.isEmpty()) return;

        ScheduleChange change = undoStack.pop();
        applyChange(change.reverse());
        redoStack.push(change);

        updateUndoRedoButtons();
        addToRecentChanges("Undo: " + change.getDescription());
        refreshScheduleGrid();
    }

    @FXML
    private void handleRedo() {
        if (redoStack.isEmpty()) return;

        ScheduleChange change = redoStack.pop();
        applyChange(change);
        undoStack.push(change);

        updateUndoRedoButtons();
        addToRecentChanges("Redo: " + change.getDescription());
        refreshScheduleGrid();
    }

    @FXML
    private void handleRefresh() {
        loadScheduleData();
    }

    @FXML
    private void handleSaveChanges() {
        if (modifiedSections.isEmpty()) {
            showInfo("No changes to save");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Save Changes");
        confirm.setHeaderText("Save Schedule Changes");
        confirm.setContentText(String.format("Save %d modified section(s)?", modifiedSections.size()));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                saveChanges();
            }
        });
    }

    @FXML
    private void handleDiscardChanges() {
        if (modifiedSections.isEmpty()) {
            showInfo("No changes to discard");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Discard Changes");
        confirm.setHeaderText("Discard All Changes");
        confirm.setContentText(String.format("Discard %d unsaved change(s)?", modifiedSections.size()));

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                discardChanges();
            }
        });
    }

    @FXML
    private void handleFilterChange() {
        refreshScheduleGrid();
    }

    @FXML
    private void handleShowConflictsToggle() {
        refreshScheduleGrid();
    }

    @FXML
    private void handleSwapTeacher() {
        if (selectedSection == null) return;

        List<Teacher> teachers = sisDataService.getAllTeachers();
        ChoiceDialog<Teacher> dialog = new ChoiceDialog<>(selectedSection.getAssignedTeacher(), teachers);
        dialog.setTitle("Swap Teacher");
        dialog.setHeaderText("Select New Teacher");
        dialog.setContentText("Teacher:");

        dialog.showAndWait().ifPresent(newTeacher -> {
            ScheduleChange change = new ScheduleChange(
                selectedSection.getId(),
                "teacher",
                selectedSection.getAssignedTeacher(),
                newTeacher,
                String.format("Change teacher for %s to %s",
                    selectedSection.getCourse().getCourseName(),
                    newTeacher.getFirstName() + " " + newTeacher.getLastName())
            );

            applyChange(change);
            undoStack.push(change);
            redoStack.clear();
            updateUndoRedoButtons();
            addToRecentChanges(change.getDescription());
            refreshScheduleGrid();
        });
    }

    @FXML
    private void handleSwapRoom() {
        if (selectedSection == null) return;

        List<Room> rooms = roomRepository.findAll();
        ChoiceDialog<Room> dialog = new ChoiceDialog<>(selectedSection.getAssignedRoom(), rooms);
        dialog.setTitle("Swap Room");
        dialog.setHeaderText("Select New Room");
        dialog.setContentText("Room:");

        dialog.showAndWait().ifPresent(newRoom -> {
            ScheduleChange change = new ScheduleChange(
                selectedSection.getId(),
                "room",
                selectedSection.getAssignedRoom(),
                newRoom,
                String.format("Change room for %s to %s",
                    selectedSection.getCourse().getCourseName(),
                    newRoom.getRoomNumber())
            );

            applyChange(change);
            undoStack.push(change);
            redoStack.clear();
            updateUndoRedoButtons();
            addToRecentChanges(change.getDescription());
            refreshScheduleGrid();
        });
    }

    @FXML
    private void handleChangePeriod() {
        if (selectedSection == null) return;

        List<Integer> periods = new ArrayList<>();
        for (int i = 1; i <= NUM_PERIODS; i++) {
            periods.add(i);
        }

        ChoiceDialog<Integer> dialog = new ChoiceDialog<>(selectedSection.getAssignedPeriod(), periods);
        dialog.setTitle("Change Period");
        dialog.setHeaderText("Select New Period");
        dialog.setContentText("Period:");

        dialog.showAndWait().ifPresent(newPeriod -> {
            ScheduleChange change = new ScheduleChange(
                selectedSection.getId(),
                "period",
                selectedSection.getAssignedPeriod(),
                newPeriod,
                String.format("Change period for %s to P%d",
                    selectedSection.getCourse().getCourseName(),
                    newPeriod)
            );

            applyChange(change);
            undoStack.push(change);
            redoStack.clear();
            updateUndoRedoButtons();
            addToRecentChanges(change.getDescription());
            refreshScheduleGrid();
        });
    }

    @FXML
    private void handleUnassignSection() {
        if (selectedSection == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Unassign Section");
        confirm.setContentText("Remove all assignments (teacher, room, period) from this section?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                unassignSection(selectedSection);
            }
        });
    }

    private void loadScheduleData() {
        log.info("Loading schedule data");

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Platform.runLater(() -> {
                    buildScheduleGrid();
                    updateConflictsList();
                    updateSuggestions();
                });
                return null;
            }

            @Override
            protected void failed() {
                log.error("Failed to load schedule data", getException());
                Platform.runLater(() -> showError("Failed to load schedule"));
            }
        };

        new Thread(loadTask).start();
    }

    private void buildScheduleGrid() {
        if (scheduleGrid == null) return;

        scheduleGrid.getChildren().clear();
        cellMap.clear();

        // Build header row (periods)
        Label cornerLabel = new Label("Teacher");
        cornerLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10; -fx-border-color: #ccc;");
        cornerLabel.setMinWidth(150);
        scheduleGrid.add(cornerLabel, 0, 0);

        for (int period = 1; period <= NUM_PERIODS; period++) {
            Label periodLabel = new Label("Period " + period);
            periodLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10; -fx-alignment: center; -fx-border-color: #ccc;");
            periodLabel.setMinWidth(150);
            scheduleGrid.add(periodLabel, period, 0);
        }

        // Get all teachers and sections
        List<Teacher> teachers = sisDataService.getAllTeachers();
        List<CourseSection> sections = courseSectionRepository.findAll();

        // Build rows for each teacher
        int row = 1;
        for (Teacher teacher : teachers) {
            // Teacher name label
            Label teacherLabel = new Label(teacher.getFirstName() + " " + teacher.getLastName());
            teacherLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10; -fx-border-color: #ccc;");
            teacherLabel.setMinWidth(150);
            scheduleGrid.add(teacherLabel, 0, row);

            // Create cells for each period
            for (int period = 1; period <= NUM_PERIODS; period++) {
                VBox cell = createScheduleCell(teacher, period, sections);
                scheduleGrid.add(cell, period, row);

                String cellKey = teacher.getId() + "-" + period;
                cellMap.put(cellKey, cell);
            }

            row++;
        }
    }

    private VBox createScheduleCell(Teacher teacher, int period, List<CourseSection> allSections) {
        VBox cell = new VBox(5);
        cell.setAlignment(Pos.TOP_LEFT);
        cell.setStyle("-fx-border-color: #ccc; -fx-padding: 8; -fx-background-color: white;");
        cell.setMinWidth(150);
        cell.setMinHeight(80);

        // Find section for this teacher/period
        final int p = period;
        Optional<CourseSection> sectionOpt = allSections.stream()
            .filter(s -> s.getAssignedTeacher() != null &&
                       s.getAssignedTeacher().getId().equals(teacher.getId()) &&
                       s.getAssignedPeriod() != null &&
                       s.getAssignedPeriod() == p)
            .findFirst();

        if (sectionOpt.isPresent()) {
            CourseSection section = sectionOpt.get();
            addSectionToCell(cell, section);
        } else {
            Label emptyLabel = new Label("(empty)");
            emptyLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            cell.getChildren().add(emptyLabel);
        }

        // Setup drag-and-drop if in edit mode
        setupCellDragAndDrop(cell, teacher, period);

        return cell;
    }

    private void addSectionToCell(VBox cell, CourseSection section) {
        // Course name
        Label courseLabel = new Label(section.getCourse().getCourseName());
        courseLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");

        // Section number
        Label sectionLabel = new Label("Sec: " + section.getSectionNumber());
        sectionLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        // Room
        Label roomLabel = new Label("Room: " +
            (section.getAssignedRoom() != null ? section.getAssignedRoom().getRoomNumber() : "TBA"));
        roomLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        // Enrollment
        Label enrollmentLabel = new Label(String.format("Students: %d/%d",
            section.getCurrentEnrollment() != null ? section.getCurrentEnrollment() : 0,
            section.getMaxEnrollment() != null ? section.getMaxEnrollment() : 0));
        enrollmentLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");

        cell.getChildren().addAll(courseLabel, sectionLabel, roomLabel, enrollmentLabel);

        // Highlight if modified
        if (modifiedSections.containsKey(section.getId())) {
            cell.setStyle(cell.getStyle() + "-fx-background-color: #fff9c4;");
        }

        // Highlight conflicts if enabled
        if (showConflictsCheckbox != null && showConflictsCheckbox.isSelected()) {
            if (hasConflict(section)) {
                cell.setStyle(cell.getStyle() + "-fx-background-color: #ffebee; -fx-border-color: #f44336; -fx-border-width: 2;");
            }
        }

        // Click to select
        cell.setOnMouseClicked(event -> selectSection(section));
    }

    private void setupCellDragAndDrop(VBox cell, Teacher teacher, int period) {
        // Drag source
        cell.setOnDragDetected(event -> {
            if (!editMode) return;

            // Find section in this cell
            Optional<CourseSection> sectionOpt = findSectionInCell(teacher, period);
            if (sectionOpt.isEmpty()) return;

            Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(SECTION_DATA_FORMAT, sectionOpt.get().getId());
            db.setContent(content);

            event.consume();
        });

        // Drag over
        cell.setOnDragOver(event -> {
            if (!editMode) return;

            if (event.getGestureSource() != cell && event.getDragboard().hasContent(SECTION_DATA_FORMAT)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }

            event.consume();
        });

        // Drag entered
        cell.setOnDragEntered(event -> {
            if (!editMode) return;

            if (event.getGestureSource() != cell && event.getDragboard().hasContent(SECTION_DATA_FORMAT)) {
                cell.setStyle(cell.getStyle() + "-fx-background-color: #e3f2fd;");
            }

            event.consume();
        });

        // Drag exited
        cell.setOnDragExited(event -> {
            cell.setStyle(cell.getStyle().replace("-fx-background-color: #e3f2fd;", ""));
            event.consume();
        });

        // Drag dropped
        cell.setOnDragDropped(event -> {
            if (!editMode) return;

            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasContent(SECTION_DATA_FORMAT)) {
                Long sectionId = (Long) db.getContent(SECTION_DATA_FORMAT);
                handleSectionDrop(sectionId, teacher, period);
                success = true;
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void handleSectionDrop(Long sectionId, Teacher newTeacher, int newPeriod) {
        CourseSection section = courseSectionRepository.findById(sectionId).orElse(null);
        if (section == null) return;

        Teacher oldTeacher = section.getAssignedTeacher();
        Integer oldPeriod = section.getAssignedPeriod();

        ScheduleChange change = new ScheduleChange(
            sectionId,
            "move",
            new TeacherPeriod(oldTeacher, oldPeriod),
            new TeacherPeriod(newTeacher, newPeriod),
            String.format("Move %s from %s P%d to %s P%d",
                section.getCourse().getCourseName(),
                oldTeacher != null ? oldTeacher.getLastName() : "?",
                oldPeriod != null ? oldPeriod : 0,
                newTeacher.getLastName(),
                newPeriod)
        );

        applyChange(change);
        undoStack.push(change);
        redoStack.clear();
        updateUndoRedoButtons();
        addToRecentChanges(change.getDescription());
        refreshScheduleGrid();
    }

    private void applyChange(ScheduleChange change) {
        CourseSection section = courseSectionRepository.findById(change.getSectionId()).orElse(null);
        if (section == null) return;

        switch (change.getField()) {
            case "teacher":
                section.setAssignedTeacher((Teacher) change.getNewValue());
                break;
            case "room":
                section.setAssignedRoom((Room) change.getNewValue());
                break;
            case "period":
                section.setAssignedPeriod((Integer) change.getNewValue());
                break;
            case "move":
                TeacherPeriod tp = (TeacherPeriod) change.getNewValue();
                section.setAssignedTeacher(tp.teacher);
                section.setAssignedPeriod(tp.period);
                break;
        }

        modifiedSections.put(section.getId(), section);
        updateChangesStatus();
    }

    private void selectSection(CourseSection section) {
        selectedSection = section;

        if (sectionDetailsBox != null) {
            sectionDetailsBox.setVisible(true);
            sectionDetailsBox.setManaged(true);
        }

        if (noSelectionLabel != null) {
            noSelectionLabel.setVisible(false);
            noSelectionLabel.setManaged(false);
        }

        safeSetText(detailCourseLabel, section.getCourse().getCourseName() + " - " +
            section.getCourse().getCourseCode());
        safeSetText(detailSectionLabel, section.getSectionNumber());
        safeSetText(detailTeacherLabel,
            section.getAssignedTeacher() != null ?
                section.getAssignedTeacher().getFirstName() + " " + section.getAssignedTeacher().getLastName() :
                "Not Assigned");
        safeSetText(detailRoomLabel,
            section.getAssignedRoom() != null ?
                section.getAssignedRoom().getRoomNumber() :
                "Not Assigned");
        safeSetText(detailPeriodLabel,
            section.getAssignedPeriod() != null ?
                "Period " + section.getAssignedPeriod() :
                "Not Assigned");
        safeSetText(detailEnrollmentLabel,
            String.format("%d / %d",
                section.getCurrentEnrollment() != null ? section.getCurrentEnrollment() : 0,
                section.getMaxEnrollment() != null ? section.getMaxEnrollment() : 30));
    }

    private void saveChanges() {
        try {
            for (CourseSection section : modifiedSections.values()) {
                courseSectionRepository.save(section);
            }

            modifiedSections.clear();
            undoStack.clear();
            redoStack.clear();

            updateChangesStatus();
            updateUndoRedoButtons();

            showInfo(String.format("Successfully saved all changes"));
            refreshScheduleGrid();

        } catch (Exception e) {
            log.error("Failed to save changes", e);
            showError("Failed to save changes: " + e.getMessage());
        }
    }

    private void discardChanges() {
        modifiedSections.clear();
        undoStack.clear();
        redoStack.clear();

        updateChangesStatus();
        updateUndoRedoButtons();
        refreshScheduleGrid();

        showInfo("All changes discarded");
    }

    private void unassignSection(CourseSection section) {
        section.setAssignedTeacher(null);
        section.setAssignedRoom(null);
        section.setAssignedPeriod(null);

        modifiedSections.put(section.getId(), section);
        updateChangesStatus();
        refreshScheduleGrid();

        addToRecentChanges("Unassigned " + section.getCourse().getCourseName());
    }

    private void refreshScheduleGrid() {
        buildScheduleGrid();
        updateConflictsList();
    }

    private void updateGridInteractivity() {
        // Grid interactivity is handled by drag-and-drop setup
        buildScheduleGrid();
    }

    private void updateChangesStatus() {
        int count = modifiedSections.size();
        if (count == 0) {
            safeSetText(changesStatusLabel, "No unsaved changes");
            changesStatusLabel.setStyle("-fx-text-fill: #388e3c;");
        } else {
            safeSetText(changesStatusLabel, count + " unsaved change(s)");
            changesStatusLabel.setStyle("-fx-text-fill: #f57c00; -fx-font-weight: bold;");
        }
    }

    private void updateUndoRedoButtons() {
        if (undoButton != null) {
            undoButton.setDisable(undoStack.isEmpty());
        }
        if (redoButton != null) {
            redoButton.setDisable(redoStack.isEmpty());
        }
    }

    private void updateConflictsList() {
        if (conflictsList == null) return;

        ObservableList<String> conflicts = FXCollections.observableArrayList();

        // Detect conflicts using the service
        List<CourseSection> allSections = courseSectionRepository.findAll();

        for (CourseSection section : allSections) {
            if (hasConflict(section)) {
                conflicts.add(String.format("%s (P%d): Conflict detected",
                    section.getCourse().getCourseName(),
                    section.getAssignedPeriod() != null ? section.getAssignedPeriod() : 0));
            }
        }

        conflictsList.setItems(conflicts);

        // Update status
        if (conflicts.isEmpty()) {
            safeSetText(conflictsStatusLabel, "No conflicts detected");
            conflictsStatusLabel.setStyle("-fx-text-fill: #388e3c;");
        } else {
            safeSetText(conflictsStatusLabel, conflicts.size() + " conflict(s) detected");
            conflictsStatusLabel.setStyle("-fx-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }

    private void updateSuggestions() {
        if (suggestionsContainer == null) return;

        suggestionsContainer.getChildren().clear();

        List<String> suggestions = new ArrayList<>();
        suggestions.add("💡 Drag sections to rearrange schedule");
        suggestions.add("🔄 Use Ctrl+Z to undo changes");
        suggestions.add("💾 Remember to save your changes");

        for (String suggestion : suggestions) {
            Label label = new Label(suggestion);
            label.setWrapText(true);
            label.setStyle("-fx-font-size: 11px; -fx-padding: 3;");
            suggestionsContainer.getChildren().add(label);
        }
    }

    private void addToRecentChanges(String change) {
        if (recentChangesList == null) return;

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String entry = timestamp + " - " + change;

        recentChangesList.getItems().add(0, entry);

        if (recentChangesList.getItems().size() > 20) {
            recentChangesList.getItems().remove(20, recentChangesList.getItems().size());
        }
    }

    private Optional<CourseSection> findSectionInCell(Teacher teacher, int period) {
        return courseSectionRepository.findAll().stream()
            .filter(s -> s.getAssignedTeacher() != null &&
                       s.getAssignedTeacher().getId().equals(teacher.getId()) &&
                       s.getAssignedPeriod() != null &&
                       s.getAssignedPeriod() == period)
            .findFirst();
    }

    private boolean hasConflict(CourseSection section) {
        // Simple conflict detection - can be enhanced
        if (section.getAssignedTeacher() == null || section.getAssignedPeriod() == null) {
            return false;
        }

        // Check for teacher double-booking
        long teacherConflicts = courseSectionRepository.findAll().stream()
            .filter(s -> s.getId() != section.getId() &&
                       s.getAssignedTeacher() != null &&
                       s.getAssignedTeacher().getId().equals(section.getAssignedTeacher().getId()) &&
                       s.getAssignedPeriod() != null &&
                       s.getAssignedPeriod().equals(section.getAssignedPeriod()))
            .count();

        return teacherConflicts > 0;
    }

    // Helper methods
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

    // Inner classes
    @Data
    private static class ScheduleChange {
        private final Long sectionId;
        private final String field;
        private final Object oldValue;
        private final Object newValue;
        private final String description;

        public ScheduleChange reverse() {
            return new ScheduleChange(sectionId, field, newValue, oldValue, "Undo: " + description);
        }
    }

    @Data
    private static class TeacherPeriod {
        private final Teacher teacher;
        private final Integer period;
    }
}
