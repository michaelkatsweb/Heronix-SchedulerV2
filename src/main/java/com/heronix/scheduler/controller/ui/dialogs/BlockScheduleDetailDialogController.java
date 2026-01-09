package com.heronix.scheduler.controller.ui.dialogs;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.domain.ScheduleSlot;
import com.heronix.scheduler.model.enums.DayType;
import com.heronix.scheduler.repository.ScheduleSlotRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controller for Block Schedule Detail Dialog
 * Location: src/main/java/com/eduscheduler/ui/controller/dialogs/BlockScheduleDetailDialogController.java
 *
 * @author Heronix Scheduling System Team
 * @since Block Scheduling MVP - November 26, 2025
 */
@Slf4j
@Controller
public class BlockScheduleDetailDialogController {

    // ==================== FXML Fields ====================

    @FXML private Label scheduleNameLabel;
    @FXML private Label dateRangeLabel;
    @FXML private Label statusLabel;
    @FXML private Label slotsLabel;
    @FXML private Label oddDaySlotsLabel;
    @FXML private Label oddDayCoursesLabel;
    @FXML private Label evenDaySlotsLabel;
    @FXML private Label evenDayCoursesLabel;

    @FXML private TableView<ScheduleSlot> coursesTable;
    @FXML private TableColumn<ScheduleSlot, String> courseCodeColumn;
    @FXML private TableColumn<ScheduleSlot, String> courseNameColumn;
    @FXML private TableColumn<ScheduleSlot, String> dayTypeColumn;
    @FXML private TableColumn<ScheduleSlot, String> teacherColumn;
    @FXML private TableColumn<ScheduleSlot, String> studentsColumn;

    // ==================== Services & Repositories ====================

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    // ==================== Data ====================

    private Schedule schedule;
    private ObservableList<ScheduleSlot> slots;
    private DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");

    // ==================== Initialization ====================

    @FXML
    private void initialize() {
        log.info("Initializing Block Schedule Detail Dialog");
        setupTable();
    }

    /**
     * Set the schedule to display
     */
    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
        loadScheduleData();
    }

    // ==================== Data Loading ====================

    private void loadScheduleData() {
        if (schedule == null) {
            log.warn("No schedule provided to detail dialog");
            return;
        }

        log.info("Loading details for schedule: {}", schedule.getScheduleName());

        // Set header information
        scheduleNameLabel.setText(schedule.getScheduleName());
        dateRangeLabel.setText(schedule.getStartDate().format(dateFormatter) + " - " +
                              schedule.getEndDate().format(dateFormatter));
        statusLabel.setText(schedule.getStatus().toString());

        // Set status color
        switch (schedule.getStatus()) {
            case PUBLISHED -> statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            case DRAFT -> statusLabel.setStyle("-fx-text-fill: #FF9800; -fx-font-weight: bold;");
            case ARCHIVED -> statusLabel.setStyle("-fx-text-fill: #999; -fx-font-weight: bold;");
        }

        // Load slots
        List<ScheduleSlot> slotList = scheduleSlotRepository.findByScheduleId(schedule.getId());
        slots = FXCollections.observableArrayList(slotList);

        // Calculate statistics
        int oddDaySlots = (int) slotList.stream()
            .filter(slot -> slot.getDayType() == DayType.ODD)
            .count();

        int evenDaySlots = (int) slotList.stream()
            .filter(slot -> slot.getDayType() == DayType.EVEN)
            .count();

        Set<Course> oddDayCourses = slotList.stream()
            .filter(slot -> slot.getDayType() == DayType.ODD)
            .map(ScheduleSlot::getCourse)
            .collect(Collectors.toSet());

        Set<Course> evenDayCourses = slotList.stream()
            .filter(slot -> slot.getDayType() == DayType.EVEN)
            .map(ScheduleSlot::getCourse)
            .collect(Collectors.toSet());

        // Update labels
        slotsLabel.setText(String.valueOf(slotList.size()));
        oddDaySlotsLabel.setText(oddDaySlots + " slots");
        oddDayCoursesLabel.setText(oddDayCourses.size() + " courses");
        evenDaySlotsLabel.setText(evenDaySlots + " slots");
        evenDayCoursesLabel.setText(evenDayCourses.size() + " courses");

        // Populate table (show unique courses with their day types)
        Set<String> uniqueCourses = new HashSet<>();
        List<ScheduleSlot> uniqueSlots = slotList.stream()
            .filter(slot -> {
                String key = slot.getCourse().getCourseCode() + "-" + slot.getDayType();
                return uniqueCourses.add(key);
            })
            .collect(Collectors.toList());

        coursesTable.setItems(FXCollections.observableArrayList(uniqueSlots));

        log.info("Loaded {} total slots, {} ODD, {} EVEN",
            slotList.size(), oddDaySlots, evenDaySlots);
    }

    // ==================== Table Setup ====================

    private void setupTable() {
        // Course Code column
        courseCodeColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCourse().getCourseCode()));

        // Course Name column
        courseNameColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getCourse().getCourseName()));

        // Day Type column with color coding
        dayTypeColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getDayType().toString()));

        dayTypeColumn.setCellFactory(column -> new TableCell<ScheduleSlot, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ODD".equals(item)) {
                        setStyle("-fx-text-fill: #00aaff; -fx-font-weight: bold;");
                    } else if ("EVEN".equals(item)) {
                        setStyle("-fx-text-fill: #ff8800; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            }
        });

        // Teacher column
        teacherColumn.setCellValueFactory(data -> {
            if (data.getValue().getTeacher() != null) {
                return new SimpleStringProperty(
                    data.getValue().getTeacher().getFirstName() + " " +
                    data.getValue().getTeacher().getLastName()
                );
            } else {
                return new SimpleStringProperty("N/A");
            }
        });

        // Students column
        studentsColumn.setCellValueFactory(data -> {
            int studentCount = data.getValue().getStudents() != null ?
                data.getValue().getStudents().size() : 0;
            return new SimpleStringProperty(String.valueOf(studentCount));
        });
    }
}
