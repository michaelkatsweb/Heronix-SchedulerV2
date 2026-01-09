package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.SlotStatus;
import com.heronix.scheduler.service.*;
import com.heronix.scheduler.service.data.SISDataService;
import com.heronix.scheduler.ui.util.ScheduleColorScheme;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Schedule Editor Controller
 * Allows editing of schedule slots - modify teachers, rooms, and time slots
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ScheduleEditorController {

    private final ScheduleSlotService scheduleSlotService;
    private final SISDataService sisDataService;
    private final RoomService roomService;
    private final TimeSlotService timeSlotService;
    private final ConflictDetectionService conflictDetectionService;
    private final org.springframework.context.ApplicationContext applicationContext;

    @FXML
    private Label scheduleNameLabel;

    @FXML
    private TableView<ScheduleSlot> slotsTable;

    @FXML
    private TableColumn<ScheduleSlot, String> courseColumn;

    @FXML
    private TableColumn<ScheduleSlot, String> teacherColumn;

    @FXML
    private TableColumn<ScheduleSlot, String> roomColumn;

    @FXML
    private TableColumn<ScheduleSlot, String> dayColumn;

    @FXML
    private TableColumn<ScheduleSlot, String> timeColumn;

    @FXML
    private ComboBox<Teacher> teacherCombo;

    @FXML
    private ComboBox<Room> roomCombo;

    @FXML
    private ComboBox<TimeSlot> timeSlotCombo;

    @FXML
    private CheckBox conflictWarningCheck;

    @FXML
    private Label conflictLabel;

    @FXML
    private Button saveButton;

    @FXML
    private Button swapButton;

    @FXML
    private Button deleteSlotButton;

    @FXML
    private Button advancedEditButton;

    private Schedule schedule;
    private ObservableList<ScheduleSlot> slotsList;
    private ScheduleSlot selectedSlot;
    private ScheduleSlot slotForSwap;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        log.info("Initializing Schedule Editor Controller");

        // Set up table columns
        courseColumn.setCellValueFactory(cellData -> {
            Course course = cellData.getValue().getCourse();
            return new SimpleStringProperty(course != null ? course.getCourseName() : "N/A");
        });

        teacherColumn.setCellValueFactory(cellData -> {
            Teacher teacher = cellData.getValue().getTeacher();
            return new SimpleStringProperty(teacher != null ? teacher.getName() : "Unassigned");
        });

        roomColumn.setCellValueFactory(cellData -> {
            Room room = cellData.getValue().getRoom();
            return new SimpleStringProperty(room != null ? room.getRoomNumber() : "Unassigned");
        });

        dayColumn.setCellValueFactory(cellData -> {
            DayOfWeek day = cellData.getValue().getDayOfWeek();
            return new SimpleStringProperty(day != null ?
                day.getDisplayName(TextStyle.FULL, Locale.getDefault()) : "N/A");
        });

        timeColumn.setCellValueFactory(cellData -> {
            LocalTime start = cellData.getValue().getStartTime();
            LocalTime end = cellData.getValue().getEndTime();
            if (start != null && end != null) {
                return new SimpleStringProperty(start + " - " + end);
            }
            return new SimpleStringProperty("N/A");
        });

        // Set up row factory for subject-based color coding
        slotsTable.setRowFactory(tv -> new TableRow<ScheduleSlot>() {
            @Override
            protected void updateItem(ScheduleSlot slot, boolean empty) {
                super.updateItem(slot, empty);

                if (empty || slot == null || slot.getCourse() == null) {
                    setStyle("");
                } else {
                    // Get course subject
                    String subject = slot.getCourse().getSubject();

                    // Apply color based on subject
                    if (subject != null && !subject.trim().isEmpty()) {
                        String style = ScheduleColorScheme.getLightBackgroundStyle(subject);
                        setStyle(style);
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Set up selection listener
        slotsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            handleSlotSelection(newSel);
        });

        // Set up double-click listener for advanced edit
        slotsTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && slotsTable.getSelectionModel().getSelectedItem() != null) {
                handleAdvancedEdit();
            }
        });

        // Set up combo box converters
        teacherCombo.setConverter(new StringConverter<Teacher>() {
            @Override
            public String toString(Teacher teacher) {
                return teacher == null ? "" : teacher.getName();
            }

            @Override
            public Teacher fromString(String string) {
                return null;
            }
        });

        roomCombo.setConverter(new StringConverter<Room>() {
            @Override
            public String toString(Room room) {
                return room == null ? "" : room.getRoomNumber() + " (Cap: " + room.getCapacity() + ")";
            }

            @Override
            public Room fromString(String string) {
                return null;
            }
        });

        timeSlotCombo.setConverter(new StringConverter<TimeSlot>() {
            @Override
            public String toString(TimeSlot slot) {
                if (slot == null) return "";
                return slot.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()) +
                       " " + slot.getStartTime() + "-" + slot.getEndTime();
            }

            @Override
            public TimeSlot fromString(String string) {
                return null;
            }
        });

        // Set up conflict filter checkbox
        conflictWarningCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            applyConflictFilter();
        });

        // Disable buttons initially
        saveButton.setDisable(true);
        swapButton.setDisable(true);
        deleteSlotButton.setDisable(true);

        loadComboBoxData();
    }

    /**
     * Set the schedule to edit
     */
    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
        scheduleNameLabel.setText(schedule.getName());
        loadScheduleSlots();
    }

    /**
     * Load schedule slots into table
     */
    private void loadScheduleSlots() {
        try {
            log.info("Loading slots for schedule: {}", schedule.getId());
            slotsList = FXCollections.observableArrayList(schedule.getSlots());
            slotsTable.setItems(slotsList);
            updateConflictLabel();
        } catch (Exception e) {
            log.error("Error loading schedule slots", e);
            showError("Error", "Failed to load schedule slots: " + e.getMessage());
        }
    }

    /**
     * Load combo box data
     */
    private void loadComboBoxData() {
        try {
            // Load teachers
            List<Teacher> teachers = sisDataService.getAllTeachers().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getActive()))
                    .collect(Collectors.toList());
            teacherCombo.setItems(FXCollections.observableArrayList(teachers));

            // Load rooms
            List<Room> rooms = roomService.findAll();
            roomCombo.setItems(FXCollections.observableArrayList(rooms));

            // Load time slots
            List<TimeSlot> timeSlots = timeSlotService.getAllTimeSlots();
            timeSlotCombo.setItems(FXCollections.observableArrayList(timeSlots));

            log.info("Loaded {} teachers, {} rooms, {} time slots",
                     teachers.size(), rooms.size(), timeSlots.size());
        } catch (Exception e) {
            log.error("Error loading combo box data", e);
            showError("Error", "Failed to load data: " + e.getMessage());
        }
    }

    /**
     * Handle slot selection
     */
    private void handleSlotSelection(ScheduleSlot slot) {
        selectedSlot = slot;
        boolean hasSelection = slot != null;

        // Enable/disable buttons
        saveButton.setDisable(!hasSelection);
        deleteSlotButton.setDisable(!hasSelection);
        swapButton.setDisable(slotForSwap == null && !hasSelection);

        if (slot != null) {
            // Populate combo boxes with current values
            teacherCombo.setValue(slot.getTeacher());
            roomCombo.setValue(slot.getRoom());

            // Find matching time slot
            if (slot.getDayOfWeek() != null && slot.getStartTime() != null) {
                Optional<TimeSlot> matchingTimeSlot = timeSlotCombo.getItems().stream()
                    .filter(ts -> ts.getDayOfWeek() == slot.getDayOfWeek() &&
                                  ts.getStartTime().equals(slot.getStartTime()))
                    .findFirst();
                matchingTimeSlot.ifPresent(timeSlotCombo::setValue);
            }

            // Check for conflicts
            checkConflicts(slot);
        } else {
            clearForm();
        }
    }

    /**
     * Clear form fields
     */
    private void clearForm() {
        teacherCombo.setValue(null);
        roomCombo.setValue(null);
        timeSlotCombo.setValue(null);
        conflictLabel.setText("");
    }

    /**
     * Check for conflicts in selected slot
     */
    private void checkConflicts(ScheduleSlot slot) {
        try {
            List<com.heronix.scheduler.model.dto.Conflict> conflicts = conflictDetectionService.checkSlotConflicts(slot.getId());

            if (!conflicts.isEmpty()) {
                String conflictText = conflicts.stream()
                    .map(c -> c.getDescription())
                    .collect(Collectors.joining("\n"));
                conflictLabel.setText("CONFLICTS DETECTED:\n" + conflictText);
                conflictLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            } else {
                conflictLabel.setText("No conflicts detected");
                conflictLabel.setStyle("-fx-text-fill: #16a34a;");
            }
        } catch (Exception e) {
            log.error("Error checking conflicts", e);
            conflictLabel.setText("Error checking conflicts");
        }
    }

    /**
     * Update conflict label with total conflicts
     */
    private void updateConflictLabel() {
        try {
            long conflictCount = slotsList.stream()
                .filter(slot -> slot.getStatus() == SlotStatus.CONFLICT ||
                                slot.getHasConflict() != null && slot.getHasConflict())
                .count();

            if (conflictCount > 0) {
                conflictLabel.setText(conflictCount + " slot(s) have conflicts");
                conflictLabel.setStyle("-fx-text-fill: #dc2626;");
            } else {
                conflictLabel.setText("No conflicts in schedule");
                conflictLabel.setStyle("-fx-text-fill: #16a34a;");
            }
        } catch (Exception e) {
            log.error("Error updating conflict label", e);
        }
    }

    /**
     * Apply conflict filter
     */
    private void applyConflictFilter() {
        if (conflictWarningCheck.isSelected()) {
            ObservableList<ScheduleSlot> conflictSlots = FXCollections.observableArrayList(
                schedule.getSlots().stream()
                    .filter(slot -> slot.getStatus() == SlotStatus.CONFLICT ||
                                    slot.getHasConflict() != null && slot.getHasConflict())
                    .collect(Collectors.toList())
            );
            slotsTable.setItems(conflictSlots);
        } else {
            slotsTable.setItems(slotsList);
        }
    }

    /**
     * Handle save button
     */
    @FXML
    private void handleSave() {
        if (selectedSlot == null) {
            return;
        }

        try {
            log.info("Saving changes to slot: {}", selectedSlot.getId());

            // Update slot with form values
            Teacher selectedTeacher = teacherCombo.getValue();
            Room selectedRoom = roomCombo.getValue();
            TimeSlot selectedTimeSlot = timeSlotCombo.getValue();

            if (selectedTeacher != null) {
                selectedSlot.setTeacher(selectedTeacher);
            }

            if (selectedRoom != null) {
                selectedSlot.setRoom(selectedRoom);
            }

            if (selectedTimeSlot != null) {
                selectedSlot.setTimeSlot(selectedTimeSlot);
                selectedSlot.setDayOfWeek(selectedTimeSlot.getDayOfWeek());
                selectedSlot.setStartTime(selectedTimeSlot.getStartTime());
                selectedSlot.setEndTime(selectedTimeSlot.getEndTime());
            }

            // Save first to update database, then check for conflicts
            scheduleSlotService.save(selectedSlot);

            // Check for conflicts
            List<com.heronix.scheduler.model.dto.Conflict> conflicts = conflictDetectionService.checkSlotConflicts(selectedSlot.getId());

            if (!conflicts.isEmpty()) {
                String conflictText = conflicts.stream()
                    .map(c -> c.getDescription())
                    .collect(Collectors.joining("\n"));

                Alert warning = new Alert(Alert.AlertType.WARNING);
                warning.setTitle("Conflicts Detected");
                warning.setHeaderText("This change created conflicts:");
                warning.setContentText(conflictText);
                warning.showAndWait();

                selectedSlot.setStatus(SlotStatus.CONFLICT);
                selectedSlot.setHasConflict(true);
                selectedSlot.setConflictReason(conflictText.length() > 255 ? conflictText.substring(0, 255) : conflictText);
            } else {
                selectedSlot.setStatus(SlotStatus.SCHEDULED);
                selectedSlot.setHasConflict(false);
                selectedSlot.setConflictReason(null);
            }

            // Save to database
            scheduleSlotService.save(selectedSlot);

            // Refresh table
            slotsTable.refresh();
            updateConflictLabel();

            showInfo("Success", "Slot updated successfully");
            log.info("Slot saved: {}", selectedSlot.getId());

        } catch (Exception e) {
            log.error("Error saving slot", e);
            showError("Error", "Failed to save slot: " + e.getMessage());
        }
    }

    /**
     * Handle swap button
     */
    @FXML
    private void handleSwap() {
        if (selectedSlot == null) {
            return;
        }

        if (slotForSwap == null) {
            // First slot selected for swap
            slotForSwap = selectedSlot;
            swapButton.setText("= Complete Swap");
            showInfo("Swap Mode", "Now select the second slot to swap with");
        } else {
            // Second slot selected - perform swap
            try {
                log.info("Swapping slots: {} <-> {}", slotForSwap.getId(), selectedSlot.getId());

                // Swap teachers
                Teacher tempTeacher = slotForSwap.getTeacher();
                slotForSwap.setTeacher(selectedSlot.getTeacher());
                selectedSlot.setTeacher(tempTeacher);

                // Swap rooms
                Room tempRoom = slotForSwap.getRoom();
                slotForSwap.setRoom(selectedSlot.getRoom());
                selectedSlot.setRoom(tempRoom);

                // Swap time slots
                TimeSlot tempTimeSlot = slotForSwap.getTimeSlot();
                DayOfWeek tempDay = slotForSwap.getDayOfWeek();
                LocalTime tempStart = slotForSwap.getStartTime();
                LocalTime tempEnd = slotForSwap.getEndTime();

                slotForSwap.setTimeSlot(selectedSlot.getTimeSlot());
                slotForSwap.setDayOfWeek(selectedSlot.getDayOfWeek());
                slotForSwap.setStartTime(selectedSlot.getStartTime());
                slotForSwap.setEndTime(selectedSlot.getEndTime());

                selectedSlot.setTimeSlot(tempTimeSlot);
                selectedSlot.setDayOfWeek(tempDay);
                selectedSlot.setStartTime(tempStart);
                selectedSlot.setEndTime(tempEnd);

                // Save both slots
                scheduleSlotService.save(slotForSwap);
                scheduleSlotService.save(selectedSlot);

                // Refresh and reset
                slotsTable.refresh();
                updateConflictLabel();
                slotForSwap = null;
                swapButton.setText("= Swap Slots");

                showInfo("Success", "Slots swapped successfully");

            } catch (Exception e) {
                log.error("Error swapping slots", e);
                showError("Error", "Failed to swap slots: " + e.getMessage());
                slotForSwap = null;
                swapButton.setText("= Swap Slots");
            }
        }
    }

    /**
     * Handle advanced edit button - Opens comprehensive edit dialog
     */
    @FXML
    private void handleAdvancedEdit() {
        if (selectedSlot == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText(null);
            alert.setContentText("Please select a schedule slot to edit.");
            alert.showAndWait();
            return;
        }

        try {
            log.info("Opening advanced edit dialog for slot: {}", selectedSlot.getId());

            // Load the FXML
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                getClass().getResource("/fxml/ScheduleSlotEditDialog.fxml")
            );
            loader.setControllerFactory(applicationContext::getBean);

            javafx.scene.Parent root = loader.load();

            // Get controller and set the slot
            ScheduleSlotEditDialogController controller = loader.getController();
            controller.setScheduleSlot(selectedSlot);

            // Create and show dialog
            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
            dialogStage.setTitle("Advanced Edit - " +
                (selectedSlot.getCourse() != null ? selectedSlot.getCourse().getCourseName() : "Schedule Slot"));
            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            dialogStage.initOwner(slotsTable.getScene().getWindow());
            dialogStage.setScene(new javafx.scene.Scene(root));

            // Set the stage in controller
            controller.setDialogStage(dialogStage);

            // Show and wait
            dialogStage.showAndWait();

            // If changes were saved, reload the schedule slots
            if (controller.isSaved()) {
                log.info("Slot was modified in advanced edit, reloading schedule slots");
                loadScheduleSlots(); // Reload to get fresh data
                showInfo("Success", "Schedule slot updated successfully");
            }

        } catch (Exception e) {
            log.error("Error opening advanced edit dialog", e);
            showError("Error", "Failed to open advanced edit dialog: " + e.getMessage());
        }
    }

    /**
     * Handle delete slot button
     */
    @FXML
    private void handleDeleteSlot() {
        if (selectedSlot == null) {
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Schedule Slot");
        confirm.setContentText("Are you sure you want to delete this slot?\n\n" +
                "Course: " + (selectedSlot.getCourse() != null ? selectedSlot.getCourse().getCourseName() : "N/A"));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                log.info("Deleting slot: {}", selectedSlot.getId());
                scheduleSlotService.deleteScheduleSlot(selectedSlot.getId());

                slotsList.remove(selectedSlot);
                schedule.getSlots().remove(selectedSlot);

                slotsTable.refresh();
                updateConflictLabel();

                showInfo("Success", "Slot deleted successfully");
            } catch (Exception e) {
                log.error("Error deleting slot", e);
                showError("Error", "Failed to delete slot: " + e.getMessage());
            }
        }
    }

    /**
     * Handle cancel/close button
     */
    @FXML
    private void handleCancel() {
        log.info("Closing Schedule Editor");

        // Close the dialog
        if (slotsTable != null && slotsTable.getScene() != null) {
            javafx.stage.Stage stage = (javafx.stage.Stage) slotsTable.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * Show info alert
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error alert
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
