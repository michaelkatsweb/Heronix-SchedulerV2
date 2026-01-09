package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.ConflictDetectionService;
import com.heronix.scheduler.service.data.SISDataService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ScheduleOverrideController {

    private final ScheduleSlotRepository scheduleSlotRepository;
    private final ScheduleOverrideRepository scheduleOverrideRepository;
    private final SISDataService sisDataService;
    private final RoomRepository roomRepository;
    private final ConflictDetectionService conflictDetectionService;

    @FXML private ComboBox<Teacher> teacherComboBox;
    @FXML private ComboBox<Room> roomComboBox;
    @FXML private TextField reasonField;
    @FXML private TextArea conflictArea;
    @FXML private CheckBox pinAssignmentCheckBox;
    @FXML private Button applyButton;
    @FXML private Button cancelButton;
    @FXML private Label slotInfoLabel;

    private ScheduleSlot currentSlot;
    private String currentUsername;

    public void initialize() {
        loadTeachers();
        loadRooms();

        teacherComboBox.setOnAction(e -> checkConflicts());
        roomComboBox.setOnAction(e -> checkConflicts());
    }

    public void setScheduleSlot(ScheduleSlot slot, String username) {
        this.currentSlot = slot;
        this.currentUsername = username;

        if (slot != null) {
            slotInfoLabel.setText(String.format("Period %d - %s - %s",
                slot.getPeriodNumber(),
                slot.getCourse() != null ? slot.getCourse().getCourseName() : "N/A",
                slot.getDayType()));

            teacherComboBox.setValue(slot.getTeacher());
            roomComboBox.setValue(slot.getRoom());
            pinAssignmentCheckBox.setSelected(slot.getPinnedBy() != null);
        }
    }

    private void loadTeachers() {
        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
                .filter(t -> Boolean.TRUE.equals(t.getActive()))
                .collect(Collectors.toList());
        teacherComboBox.getItems().setAll(teachers);
        teacherComboBox.setConverter(new javafx.util.StringConverter<Teacher>() {
            @Override
            public String toString(Teacher teacher) {
                return teacher != null ? teacher.getName() : "";
            }
            @Override
            public Teacher fromString(String string) {
                return null;
            }
        });
    }

    private void loadRooms() {
        List<Room> rooms = roomRepository.findAll();
        roomComboBox.getItems().setAll(rooms);
        roomComboBox.setConverter(new javafx.util.StringConverter<Room>() {
            @Override
            public String toString(Room room) {
                return room != null ? room.getRoomNumber() + " - " + room.getRoomType() : "";
            }
            @Override
            public Room fromString(String string) {
                return null;
            }
        });
    }

    private void checkConflicts() {
        if (currentSlot == null) return;

        Teacher selectedTeacher = teacherComboBox.getValue();
        Room selectedRoom = roomComboBox.getValue();

        List<String> conflicts = conflictDetectionService.detectConflicts(
            currentSlot, selectedTeacher, selectedRoom);

        if (conflicts.isEmpty()) {
            conflictArea.setText("No conflicts detected.");
            conflictArea.setStyle("-fx-text-fill: green;");
            applyButton.setDisable(false);
        } else {
            conflictArea.setText("CONFLICTS:\n" + String.join("\n", conflicts));
            conflictArea.setStyle("-fx-text-fill: red;");
            applyButton.setDisable(true);
        }
    }

    @FXML
    @Transactional
    private void handleApply() {
        if (currentSlot == null) return;

        Teacher newTeacher = teacherComboBox.getValue();
        Room newRoom = roomComboBox.getValue();
        String reason = reasonField.getText();

        // Create audit record
        ScheduleOverride override = new ScheduleOverride();
        override.setScheduleSlot(currentSlot);
        override.setChangedBy(currentUsername);
        override.setChangedAt(LocalDateTime.now());
        override.setOldTeacher(currentSlot.getTeacher());
        override.setNewTeacher(newTeacher);
        override.setOldRoom(currentSlot.getRoom());
        override.setNewRoom(newRoom);
        override.setReason(reason);

        // Determine override type
        String overrideType = "";
        if (!currentSlot.getTeacher().equals(newTeacher)) {
            overrideType = "TEACHER";
        }
        if (!currentSlot.getRoom().equals(newRoom)) {
            overrideType = overrideType.isEmpty() ? "ROOM" : "TEACHER_ROOM";
        }
        if (pinAssignmentCheckBox.isSelected() && currentSlot.getPinnedBy() == null) {
            overrideType = overrideType.isEmpty() ? "PIN" : overrideType + "_PIN";
        }
        override.setOverrideType(overrideType);

        // Apply changes
        currentSlot.setTeacher(newTeacher);
        currentSlot.setRoom(newRoom);

        if (pinAssignmentCheckBox.isSelected()) {
            currentSlot.setPinnedBy(currentUsername);
            currentSlot.setPinnedAt(LocalDateTime.now());
        } else {
            currentSlot.setPinnedBy(null);
            currentSlot.setPinnedAt(null);
        }

        scheduleSlotRepository.save(currentSlot);
        scheduleOverrideRepository.save(override);

        log.info("Manual override applied: {} by {}", overrideType, currentUsername);

        closeDialog();
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }
}
