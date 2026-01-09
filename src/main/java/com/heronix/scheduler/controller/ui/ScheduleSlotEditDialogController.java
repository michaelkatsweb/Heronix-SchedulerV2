package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.CourseType;
import com.heronix.scheduler.model.enums.SpecialEventType;
import com.heronix.scheduler.model.enums.*;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.data.SISDataService;
import com.heronix.scheduler.service.ConflictDetectionService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller for Schedule Slot Edit Dialog
 * Enables manual editing of schedule slots with comprehensive validation
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/ScheduleSlotEditDialogController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-08
 */
@Slf4j
@Component
public class ScheduleSlotEditDialogController {

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private SISDataService sisDataService;

    // Removed: TeacherRepository

    @Autowired
    private RoomRepository roomRepository;

    

    @Autowired
    private ConflictDetectionService conflictDetectionService;

    // ========================================================================
    // FXML COMPONENTS
    // ========================================================================

    @FXML private Label dialogTitle;
    @FXML private Label courseNameLabel;
    @FXML private ComboBox<String> courseTypeCombo;
    @FXML private ComboBox<Teacher> primaryTeacherCombo;
    @FXML private ComboBox<Teacher> coTeacherCombo;
    @FXML private CheckBox enableCoTeacherCheckbox;
    @FXML private VBox teacherConflictBox;
    @FXML private Label teacherConflictLabel;
    @FXML private ComboBox<Room> roomCombo;
    @FXML private Label roomCapacityLabel;
    @FXML private Label capacityWarningLabel;
    @FXML private VBox roomConflictBox;
    @FXML private Label roomConflictLabel;
    @FXML private ComboBox<DayOfWeek> dayOfWeekCombo;
    @FXML private ComboBox<String> periodCombo;
    @FXML private CheckBox customTimeCheckbox;
    @FXML private HBox customTimeBox;
    @FXML private ComboBox<LocalTime> startTimeCombo;
    @FXML private ComboBox<LocalTime> endTimeCombo;
    @FXML private CheckBox specialEventCheckbox;
    @FXML private VBox eventDetailsBox;
    @FXML private ComboBox<String> eventTypeCombo;
    @FXML private TextArea eventDescriptionArea;
    @FXML private CheckBox blockMultiplePeriodsCheckbox;
    @FXML private TextArea notesArea;
    @FXML private TextArea conflictSummaryArea;
    @FXML private CheckBox ignoreConflictsCheckbox;

    // ========================================================================
    // STATE
    // ========================================================================

    private ScheduleSlot slot;
    private Stage dialogStage;
    private boolean saved = false;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        log.info("Initializing ScheduleSlotEditDialogController");

        setupCourseTypeCombo();
        setupTeacherCombos();
        setupRoomCombo();
        setupDayOfWeekCombo();
        setupPeriodCombo();
        setupTimeCombo();
        setupEventTypeCombo();

        // Initially hide optional sections
        coTeacherCombo.setManaged(false);
        coTeacherCombo.setVisible(false);
        teacherConflictBox.setManaged(false);
        teacherConflictBox.setVisible(false);
        roomConflictBox.setManaged(false);
        roomConflictBox.setVisible(false);
        customTimeBox.setManaged(false);
        customTimeBox.setVisible(false);
        eventDetailsBox.setManaged(false);
        eventDetailsBox.setVisible(false);
    }

    private void setupCourseTypeCombo() {
        courseTypeCombo.setItems(FXCollections.observableArrayList(
            "Regular",
            "Honors",
            "AP (Advanced Placement)",
            "IB (International Baccalaureate)",
            "Dual Enrollment",
            "Remedial",
            "Gifted & Talented"
        ));
    }

    private void setupTeacherCombos() {
        List<Teacher> teachers = sisDataService.getAllTeachers();

        primaryTeacherCombo.setItems(FXCollections.observableArrayList(teachers));
        coTeacherCombo.setItems(FXCollections.observableArrayList(teachers));

        // String converter for teachers
        StringConverter<Teacher> converter = new StringConverter<>() {
            @Override
            public String toString(Teacher teacher) {
                return teacher == null ? "" : teacher.getName() + " (" + teacher.getDepartment() + ")";
            }

            @Override
            public Teacher fromString(String string) {
                return null;
            }
        };

        primaryTeacherCombo.setConverter(converter);
        coTeacherCombo.setConverter(converter);
    }

    private void setupRoomCombo() {
        List<Room> rooms = roomRepository.findAll();
        roomCombo.setItems(FXCollections.observableArrayList(rooms));

        roomCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Room room) {
                return room == null ? "" : room.getRoomNumber() + " (Capacity: " + room.getCapacity() + ")";
            }

            @Override
            public Room fromString(String string) {
                return null;
            }
        });
    }

    private void setupDayOfWeekCombo() {
        dayOfWeekCombo.setItems(FXCollections.observableArrayList(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        ));

        dayOfWeekCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(DayOfWeek day) {
                return day == null ? "" : day.getDisplayName(TextStyle.FULL, Locale.getDefault());
            }

            @Override
            public DayOfWeek fromString(String string) {
                return null;
            }
        });
    }

    private void setupPeriodCombo() {
        periodCombo.setItems(FXCollections.observableArrayList(
            "Period 1 (8:00-8:50)",
            "Period 2 (9:00-9:50)",
            "Period 3 (10:00-10:50)",
            "Period 4 (11:00-11:50)",
            "Lunch (12:00-12:30)",
            "Period 5 (12:30-1:20)",
            "Period 6 (1:30-2:20)",
            "Period 7 (2:30-3:20)"
        ));
    }

    private void setupTimeCombo() {
        // Generate time slots from 7:00 AM to 5:00 PM in 10-minute intervals
        List<LocalTime> times = new ArrayList<>();
        LocalTime time = LocalTime.of(7, 0);
        LocalTime end = LocalTime.of(17, 0);

        while (!time.isAfter(end)) {
            times.add(time);
            time = time.plusMinutes(10);
        }

        startTimeCombo.setItems(FXCollections.observableArrayList(times));
        endTimeCombo.setItems(FXCollections.observableArrayList(times));

        StringConverter<LocalTime> converter = new StringConverter<>() {
            @Override
            public String toString(LocalTime time) {
                return time == null ? "" : time.toString();
            }

            @Override
            public LocalTime fromString(String string) {
                return LocalTime.parse(string);
            }
        };

        startTimeCombo.setConverter(converter);
        endTimeCombo.setConverter(converter);
    }

    private void setupEventTypeCombo() {
        eventTypeCombo.setItems(FXCollections.observableArrayList(
            "Assembly",
            "Testing Day",
            "Field Trip",
            "Professional Development",
            "Parent-Teacher Conference",
            "School Closure",
            "Half Day",
            "Graduation",
            "Sports Event",
            "Other"
        ));
    }

    // ========================================================================
    // PUBLIC METHODS
    // ========================================================================

    /**
     * Set the schedule slot to edit
     */
    public void setScheduleSlot(ScheduleSlot slot) {
        this.slot = slot;
        loadSlotData();
    }

    /**
     * Set the dialog stage
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    /**
     * Check if slot was saved
     */
    public boolean isSaved() {
        return saved;
    }

    // ========================================================================
    // DATA LOADING
    // ========================================================================

    private void loadSlotData() {
        if (slot == null) return;

        try {
            // Course info
            if (slot.getCourse() != null) {
                courseNameLabel.setText(slot.getCourse().getCourseName());
                // Display course type (difficulty level)
                if (slot.getCourse().getCourseType() != null) {
                    courseTypeCombo.setValue(slot.getCourse().getCourseType().toString());
                } else {
                    courseTypeCombo.setValue("Regular");
                }
            }

            // Teacher
            if (slot.getTeacher() != null) {
                primaryTeacherCombo.setValue(slot.getTeacher());
            }

            // Co-teacher (if supported by entity)
            // Note: Need to check if ScheduleSlot has coTeacher field
            // If not, this feature will be disabled

            // Room
            if (slot.getRoom() != null) {
                roomCombo.setValue(slot.getRoom());
                updateRoomCapacity();
            }

            // Day/Time
            if (slot.getDayOfWeek() != null) {
                dayOfWeekCombo.setValue(slot.getDayOfWeek());
            }

            // Period - try to match from time
            if (slot.getStartTime() != null && slot.getEndTime() != null) {
                String period = matchPeriod(slot.getStartTime(), slot.getEndTime());
                if (period != null) {
                    periodCombo.setValue(period);
                } else {
                    // Use custom time
                    customTimeCheckbox.setSelected(true);
                    handleCustomTimeToggle();
                    startTimeCombo.setValue(slot.getStartTime());
                    endTimeCombo.setValue(slot.getEndTime());
                }
            }

            // Notes
            if (slot.getNotes() != null) {
                notesArea.setText(slot.getNotes());
            }

            log.info("Loaded slot data for editing: {}", slot.getId());

        } catch (Exception e) {
            log.error("Error loading slot data", e);
            showError("Load Error", "Failed to load slot data: " + e.getMessage());
        }
    }

    private String matchPeriod(LocalTime start, LocalTime end) {
        // Match standard periods
        if (start.equals(LocalTime.of(8, 0)) && end.equals(LocalTime.of(8, 50)))
            return "Period 1 (8:00-8:50)";
        if (start.equals(LocalTime.of(9, 0)) && end.equals(LocalTime.of(9, 50)))
            return "Period 2 (9:00-9:50)";
        if (start.equals(LocalTime.of(10, 0)) && end.equals(LocalTime.of(10, 50)))
            return "Period 3 (10:00-10:50)";
        if (start.equals(LocalTime.of(11, 0)) && end.equals(LocalTime.of(11, 50)))
            return "Period 4 (11:00-11:50)";
        if (start.equals(LocalTime.of(12, 0)) && end.equals(LocalTime.of(12, 30)))
            return "Lunch (12:00-12:30)";
        if (start.equals(LocalTime.of(12, 30)) && end.equals(LocalTime.of(13, 20)))
            return "Period 5 (12:30-1:20)";
        if (start.equals(LocalTime.of(13, 30)) && end.equals(LocalTime.of(14, 20)))
            return "Period 6 (1:30-2:20)";
        if (start.equals(LocalTime.of(14, 30)) && end.equals(LocalTime.of(15, 20)))
            return "Period 7 (2:30-3:20)";

        return null; // Custom time
    }

    // ========================================================================
    // EVENT HANDLERS
    // ========================================================================

    @FXML
    private void handleTeacherChange() {
        checkTeacherConflicts();
        updateConflictSummary();
    }

    @FXML
    private void handleCoTeacherChange() {
        // Validate co-teacher is different from primary
        Teacher primary = primaryTeacherCombo.getValue();
        Teacher coTeacher = coTeacherCombo.getValue();

        if (primary != null && coTeacher != null && primary.getId().equals(coTeacher.getId())) {
            showWarning("Invalid Selection", "Co-teacher must be different from primary teacher");
            coTeacherCombo.setValue(null);
            return;
        }

        checkTeacherConflicts();
        updateConflictSummary();
    }

    @FXML
    private void handleCoTeacherToggle() {
        boolean enabled = enableCoTeacherCheckbox.isSelected();
        coTeacherCombo.setManaged(enabled);
        coTeacherCombo.setVisible(enabled);

        if (!enabled) {
            coTeacherCombo.setValue(null);
        }
    }

    @FXML
    private void handleRoomChange() {
        updateRoomCapacity();
        checkRoomConflicts();
        updateConflictSummary();
    }

    @FXML
    private void handleTimeChange() {
        if (!customTimeCheckbox.isSelected() && periodCombo.getValue() != null) {
            // Parse period to get times
            String period = periodCombo.getValue();
            // Extract times from period string
            // This is already handled by period selection
        }

        checkAllConflicts();
        updateConflictSummary();
    }

    @FXML
    private void handleCustomTimeToggle() {
        boolean custom = customTimeCheckbox.isSelected();
        customTimeBox.setManaged(custom);
        customTimeBox.setVisible(custom);

        periodCombo.setDisable(custom);

        if (custom) {
            periodCombo.setValue(null);
        }
    }

    @FXML
    private void handleSpecialEventToggle() {
        boolean isEvent = specialEventCheckbox.isSelected();
        eventDetailsBox.setManaged(isEvent);
        eventDetailsBox.setVisible(isEvent);

        if (isEvent) {
            // Special events may override conflict rules
            updateConflictSummary();
        }
    }

    @FXML
    private void handleCheckConflicts() {
        checkAllConflicts();
        updateConflictSummary();

        if (conflictSummaryArea.getText().isEmpty() || conflictSummaryArea.getText().contains("No conflicts")) {
            showSuccess("No Conflicts", "This schedule slot has no conflicts!");
        } else {
            showWarning("Conflicts Detected", "Please review the conflict summary below.");
        }
    }

    @FXML
    private void handleSave() {
        if (!validateForm()) {
            return;
        }

        // Check for conflicts
        List<String> conflicts = collectConflicts();

        if (!conflicts.isEmpty() && !ignoreConflictsCheckbox.isSelected()) {
            Alert confirmDialog = new Alert(Alert.AlertType.WARNING);
            confirmDialog.setTitle("Conflicts Detected");
            confirmDialog.setHeaderText("There are scheduling conflicts");
            confirmDialog.setContentText(
                "Found " + conflicts.size() + " conflict(s). Do you want to save anyway?\n\n" +
                "Check 'Ignore Conflicts' to bypass this warning."
            );

            ButtonType saveAnyway = new ButtonType("Save Anyway");
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmDialog.getButtonTypes().setAll(saveAnyway, cancel);

            Optional<ButtonType> result = confirmDialog.showAndWait();
            if (result.isEmpty() || result.get() != saveAnyway) {
                return;
            }
        }

        try {
            // Update slot with form data
            slot.setTeacher(primaryTeacherCombo.getValue());

            // Set co-teacher if enabled (requires coTeacher field in ScheduleSlot entity)
            // slot.setCoTeacher(enableCoTeacherCheckbox.isSelected() ? coTeacherCombo.getValue() : null);

            slot.setRoom(roomCombo.getValue());
            slot.setDayOfWeek(dayOfWeekCombo.getValue());

            // Set time from period or custom time
            if (customTimeCheckbox.isSelected()) {
                slot.setStartTime(startTimeCombo.getValue());
                slot.setEndTime(endTimeCombo.getValue());
            } else {
                setTimeFromPeriod();
            }

            // Update course type if changed
            if (courseTypeCombo.getValue() != null && slot.getCourse() != null) {
                String selectedTypeName = courseTypeCombo.getValue();
                CourseType courseType = mapCourseTypeStringToEnum(selectedTypeName);
                slot.getCourse().setCourseType(courseType.name());
                // Note: Cannot save Course entity directly - managed by SIS
                // TODO: Implement SIS API call to update course type
                log.info("Updated course type to: {} (not persisted - requires SIS API)", courseType);
            }

            // Update special event fields
            if (specialEventCheckbox.isSelected()) {
                slot.setIsSpecialEvent(true);

                // Set event type from combo
                String eventTypeStr = eventTypeCombo.getValue();
                if (eventTypeStr != null) {
                    SpecialEventType eventType = mapEventTypeStringToEnum(eventTypeStr);
                    slot.setSpecialEventType(eventType);
                }

                // Set event description
                slot.setSpecialEventDescription(eventDescriptionArea.getText());
            } else {
                slot.setIsSpecialEvent(false);
                slot.setSpecialEventType(null);
                slot.setSpecialEventDescription(null);
            }

            // Set notes
            slot.setNotes(notesArea.getText());

            // Save
            scheduleSlotRepository.save(slot);

            log.info("Schedule slot updated successfully: {}", slot.getId());
            saved = true;

            showSuccess("Saved", "Schedule slot updated successfully!");
            closeDialog();

        } catch (Exception e) {
            log.error("Error saving schedule slot", e);
            showError("Save Failed", "Failed to save schedule slot: " + e.getMessage());
        }
    }

    private void setTimeFromPeriod() {
        String period = periodCombo.getValue();
        if (period == null) return;

        if (period.contains("8:00-8:50")) {
            slot.setStartTime(LocalTime.of(8, 0));
            slot.setEndTime(LocalTime.of(8, 50));
        } else if (period.contains("9:00-9:50")) {
            slot.setStartTime(LocalTime.of(9, 0));
            slot.setEndTime(LocalTime.of(9, 50));
        } else if (period.contains("10:00-10:50")) {
            slot.setStartTime(LocalTime.of(10, 0));
            slot.setEndTime(LocalTime.of(10, 50));
        } else if (period.contains("11:00-11:50")) {
            slot.setStartTime(LocalTime.of(11, 0));
            slot.setEndTime(LocalTime.of(11, 50));
        } else if (period.contains("12:00-12:30")) {
            slot.setStartTime(LocalTime.of(12, 0));
            slot.setEndTime(LocalTime.of(12, 30));
        } else if (period.contains("12:30-1:20")) {
            slot.setStartTime(LocalTime.of(12, 30));
            slot.setEndTime(LocalTime.of(13, 20));
        } else if (period.contains("1:30-2:20")) {
            slot.setStartTime(LocalTime.of(13, 30));
            slot.setEndTime(LocalTime.of(14, 20));
        } else if (period.contains("2:30-3:20")) {
            slot.setStartTime(LocalTime.of(14, 30));
            slot.setEndTime(LocalTime.of(15, 20));
        }
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    // ========================================================================
    // CONFLICT DETECTION
    // ========================================================================

    private void checkTeacherConflicts() {
        Teacher teacher = primaryTeacherCombo.getValue();
        if (teacher == null) {
            teacherConflictBox.setManaged(false);
            teacherConflictBox.setVisible(false);
            return;
        }

        // Get current time slot
        LocalTime start = getSelectedStartTime();
        LocalTime end = getSelectedEndTime();
        DayOfWeek day = dayOfWeekCombo.getValue();

        if (start == null || end == null || day == null) {
            return;
        }

        // Check for conflicts
        List<ScheduleSlot> conflicts = scheduleSlotRepository.findAll().stream()
            .filter(s -> s.getTeacher() != null && s.getTeacher().getId().equals(teacher.getId()))
            .filter(s -> s.getDayOfWeek() == day)
            .filter(s -> !s.getId().equals(slot.getId())) // Exclude current slot
            .filter(s -> timesOverlap(start, end, s.getStartTime(), s.getEndTime()))
            .collect(Collectors.toList());

        if (!conflicts.isEmpty()) {
            teacherConflictLabel.setText(
                String.format("%s is already teaching during this time:\n%s",
                    teacher.getName(),
                    conflicts.stream()
                        .map(s -> s.getCourse().getCourseName() + " (" + s.getStartTime() + "-" + s.getEndTime() + ")")
                        .collect(Collectors.joining("\n")))
            );
            teacherConflictBox.setManaged(true);
            teacherConflictBox.setVisible(true);
        } else {
            teacherConflictBox.setManaged(false);
            teacherConflictBox.setVisible(false);
        }
    }

    private void checkRoomConflicts() {
        Room room = roomCombo.getValue();
        if (room == null) {
            roomConflictBox.setManaged(false);
            roomConflictBox.setVisible(false);
            return;
        }

        LocalTime start = getSelectedStartTime();
        LocalTime end = getSelectedEndTime();
        DayOfWeek day = dayOfWeekCombo.getValue();

        if (start == null || end == null || day == null) {
            return;
        }

        List<ScheduleSlot> conflicts = scheduleSlotRepository.findAll().stream()
            .filter(s -> s.getRoom() != null && s.getRoom().getId().equals(room.getId()))
            .filter(s -> s.getDayOfWeek() == day)
            .filter(s -> !s.getId().equals(slot.getId()))
            .filter(s -> timesOverlap(start, end, s.getStartTime(), s.getEndTime()))
            .collect(Collectors.toList());

        if (!conflicts.isEmpty()) {
            roomConflictLabel.setText(
                String.format("Room %s is already in use during this time:\n%s",
                    room.getRoomNumber(),
                    conflicts.stream()
                        .map(s -> s.getCourse().getCourseName() + " (" + s.getStartTime() + "-" + s.getEndTime() + ")")
                        .collect(Collectors.joining("\n")))
            );
            roomConflictBox.setManaged(true);
            roomConflictBox.setVisible(true);
        } else {
            roomConflictBox.setManaged(false);
            roomConflictBox.setVisible(false);
        }
    }

    private void checkAllConflicts() {
        checkTeacherConflicts();
        checkRoomConflicts();
    }

    private List<String> collectConflicts() {
        List<String> conflicts = new ArrayList<>();

        if (teacherConflictBox.isVisible()) {
            conflicts.add("Teacher conflict: " + teacherConflictLabel.getText());
        }

        if (roomConflictBox.isVisible()) {
            conflicts.add("Room conflict: " + roomConflictLabel.getText());
        }

        if (!capacityWarningLabel.getText().isEmpty()) {
            conflicts.add("Capacity warning: " + capacityWarningLabel.getText());
        }

        return conflicts;
    }

    private void updateConflictSummary() {
        List<String> conflicts = collectConflicts();

        if (conflicts.isEmpty()) {
            conflictSummaryArea.setText("No conflicts detected. Safe to save.");
            conflictSummaryArea.setStyle("-fx-text-fill: green;");
        } else {
            conflictSummaryArea.setText(
                "⚠ " + conflicts.size() + " conflict(s) detected:\n\n" +
                String.join("\n\n", conflicts)
            );
            conflictSummaryArea.setStyle("-fx-text-fill: #d32f2f;");
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private void updateRoomCapacity() {
        Room room = roomCombo.getValue();
        if (room == null) {
            roomCapacityLabel.setText("--");
            capacityWarningLabel.setText("");
            return;
        }

        roomCapacityLabel.setText(String.valueOf(room.getCapacity()));

        // Check if course enrollment exceeds capacity
        if (slot.getCourse() != null && slot.getCourse().getCurrentEnrollment() != null) {
            int enrollment = slot.getCourse().getCurrentEnrollment();
            if (enrollment > room.getCapacity()) {
                capacityWarningLabel.setText(
                    String.format("⚠ Enrollment (%d) exceeds capacity (%d)",
                        enrollment, room.getCapacity())
                );
            } else {
                capacityWarningLabel.setText("");
            }
        } else {
            capacityWarningLabel.setText("");
        }
    }

    private LocalTime getSelectedStartTime() {
        if (customTimeCheckbox.isSelected()) {
            return startTimeCombo.getValue();
        } else {
            String period = periodCombo.getValue();
            if (period == null) return null;

            if (period.contains("8:00")) return LocalTime.of(8, 0);
            if (period.contains("9:00")) return LocalTime.of(9, 0);
            if (period.contains("10:00")) return LocalTime.of(10, 0);
            if (period.contains("11:00")) return LocalTime.of(11, 0);
            if (period.contains("12:00")) return LocalTime.of(12, 0);
            if (period.contains("12:30")) return LocalTime.of(12, 30);
            if (period.contains("1:30")) return LocalTime.of(13, 30);
            if (period.contains("2:30")) return LocalTime.of(14, 30);
        }
        return null;
    }

    private LocalTime getSelectedEndTime() {
        if (customTimeCheckbox.isSelected()) {
            return endTimeCombo.getValue();
        } else {
            String period = periodCombo.getValue();
            if (period == null) return null;

            if (period.contains("8:50")) return LocalTime.of(8, 50);
            if (period.contains("9:50")) return LocalTime.of(9, 50);
            if (period.contains("10:50")) return LocalTime.of(10, 50);
            if (period.contains("11:50")) return LocalTime.of(11, 50);
            if (period.contains("12:30")) return LocalTime.of(12, 30);
            if (period.contains("1:20")) return LocalTime.of(13, 20);
            if (period.contains("2:20")) return LocalTime.of(14, 20);
            if (period.contains("3:20")) return LocalTime.of(15, 20);
        }
        return null;
    }

    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    private boolean validateForm() {
        List<String> errors = new ArrayList<>();

        if (primaryTeacherCombo.getValue() == null) {
            errors.add("Please select a primary teacher");
        }

        if (roomCombo.getValue() == null) {
            errors.add("Please select a room");
        }

        if (dayOfWeekCombo.getValue() == null) {
            errors.add("Please select a day of week");
        }

        if (customTimeCheckbox.isSelected()) {
            if (startTimeCombo.getValue() == null || endTimeCombo.getValue() == null) {
                errors.add("Please select start and end times");
            } else if (!startTimeCombo.getValue().isBefore(endTimeCombo.getValue())) {
                errors.add("Start time must be before end time");
            }
        } else {
            if (periodCombo.getValue() == null) {
                errors.add("Please select a period");
            }
        }

        if (!errors.isEmpty()) {
            showError("Validation Failed",
                "Please correct the following errors:\n\n• " +
                String.join("\n• ", errors));
            return false;
        }

        return true;
    }


    /**
     * Map course type display string to CourseType enum
     */
    private CourseType mapCourseTypeStringToEnum(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return CourseType.REGULAR;
        }

        // Direct enum matching
        for (CourseType type : CourseType.values()) {
            if (type.toString().equalsIgnoreCase(typeStr) ||
                type.name().equalsIgnoreCase(typeStr)) {
                return type;
            }
        }

        // Partial matching
        String upperType = typeStr.toUpperCase();
        if (upperType.contains("AP") || upperType.contains("ADVANCED PLACEMENT")) {
            return CourseType.AP;
        } else if (upperType.contains("HONORS")) {
            return CourseType.HONORS;
        } else if (upperType.contains("IB") || upperType.contains("BACCALAUREATE")) {
            return CourseType.IB;
        } else if (upperType.contains("DUAL") || upperType.contains("ENROLLMENT")) {
            return CourseType.DUAL_ENROLLMENT;
        } else if (upperType.contains("REMEDIAL") || upperType.contains("SUPPORT")) {
            return CourseType.REMEDIAL;
        } else if (upperType.contains("GIFTED") || upperType.contains("TALENTED")) {
            return CourseType.GIFTED;
        } else if (upperType.contains("ELECTIVE")) {
            return CourseType.ELECTIVE;
        }

        return CourseType.REGULAR;
    }

    /**
     * Map event type display string to SpecialEventType enum
     */
    private SpecialEventType mapEventTypeStringToEnum(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return SpecialEventType.OTHER;
        }

        // Direct enum matching
        for (SpecialEventType type : SpecialEventType.values()) {
            if (type.toString().equalsIgnoreCase(typeStr) ||
                type.name().equalsIgnoreCase(typeStr)) {
                return type;
            }
        }

        // Partial matching
        String upperType = typeStr.toUpperCase();
        if (upperType.contains("ASSEMBLY")) {
            return SpecialEventType.ASSEMBLY;
        } else if (upperType.contains("TEST")) {
            return SpecialEventType.TESTING;
        } else if (upperType.contains("FIELD") || upperType.contains("TRIP")) {
            return SpecialEventType.FIELD_TRIP;
        } else if (upperType.contains("PROFESSIONAL") || upperType.contains("DEVELOPMENT")) {
            return SpecialEventType.PROFESSIONAL_DEVELOPMENT;
        } else if (upperType.contains("PARENT") || upperType.contains("CONFERENCE")) {
            return SpecialEventType.PARENT_CONFERENCE;
        } else if (upperType.contains("SPORTS")) {
            return SpecialEventType.SPORTS_EVENT;
        } else if (upperType.contains("CLUB")) {
            return SpecialEventType.CLUB_MEETING;
        } else if (upperType.contains("GRADUATION")) {
            return SpecialEventType.GRADUATION;
        }

        return SpecialEventType.OTHER;
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    // ========================================================================
    // ALERT HELPERS
    // ========================================================================

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText("Operation Failed");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
