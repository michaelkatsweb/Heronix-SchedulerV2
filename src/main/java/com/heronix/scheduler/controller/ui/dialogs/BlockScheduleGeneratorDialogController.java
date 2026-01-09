package com.heronix.scheduler.controller.ui.dialogs;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.BlockScheduleService;
import com.heronix.scheduler.service.data.SISDataService;
import com.heronix.scheduler.util.DialogUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxListCell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for Block Schedule Generator Dialog
 * Location: src/main/java/com/eduscheduler/ui/controller/dialogs/BlockScheduleGeneratorDialogController.java
 *
 * @author Heronix Scheduling System Team
 * @since Block Scheduling MVP - November 26, 2025
 */
@Slf4j
@Controller
public class BlockScheduleGeneratorDialogController {

    // ==================== FXML Fields ====================

    @FXML private TextField scheduleNameField;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ListView<Student> studentListView;
    @FXML private ListView<Course> courseListView;
    @FXML private Label studentCountLabel;
    @FXML private Label courseCountLabel;
    @FXML private Spinner<Integer> periodDurationSpinner;
    @FXML private Spinner<Integer> periodsPerWeekSpinner;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label statusLabel;
    @FXML private ButtonType generateButtonType;

    // ==================== Services & Repositories ====================

    @Autowired
    private BlockScheduleService blockScheduleService;

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    // ==================== Data ====================

    private ObservableList<Student> allStudents;
    private ObservableList<Course> allCourses;
    private List<Teacher> allTeachers;
    private List<Room> allRooms;

    private Schedule generatedSchedule;

    // ==================== Initialization ====================

    @FXML
    private void initialize() {
        log.info("Initializing Block Schedule Generator Dialog");

        setupAcademicYearDates();
        setupScheduleName();
        setupSpinners();
        loadData();
        setupSelectionLists();
        setupButtonHandler();

        statusLabel.setText("Ready to generate block schedule");
    }

    private void setupAcademicYearDates() {
        LocalDate today = LocalDate.now();

        // Calculate academic year (Aug 1 - Jun 30)
        LocalDate startDate = LocalDate.of(
            today.getMonthValue() >= 8 ? today.getYear() : today.getYear() - 1,
            8, 1
        );
        LocalDate endDate = LocalDate.of(startDate.getYear() + 1, 6, 30);

        startDatePicker.setValue(startDate);
        endDatePicker.setValue(endDate);
    }

    private void setupScheduleName() {
        LocalDate startDate = startDatePicker.getValue();
        String defaultName = String.format("Block Schedule %d-%d",
            startDate.getYear(),
            startDate.getYear() + 1
        );
        scheduleNameField.setText(defaultName);
    }

    private void setupSpinners() {
        // Period duration spinner
        SpinnerValueFactory<Integer> durationFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(45, 180, 90, 15);
        periodDurationSpinner.setValueFactory(durationFactory);

        // Periods per week spinner
        SpinnerValueFactory<Integer> periodsFactory =
            new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 5, 3, 1);
        periodsPerWeekSpinner.setValueFactory(periodsFactory);
    }

    private void loadData() {
        log.info("Loading students, courses, teachers, and rooms");

        // Load students
        allStudents = FXCollections.observableArrayList(
            sisDataService.getAllStudents().stream()
                .filter(s -> Boolean.TRUE.equals(s.getActive()))
                .collect(Collectors.toList())
        );

        // Load courses
        allCourses = FXCollections.observableArrayList(
            sisDataService.getAllCourses().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .collect(Collectors.toList())
        );

        // Load teachers
        allTeachers = sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());

        // Load rooms
        allRooms = roomRepository.findAll();

        log.info("Loaded {} students, {} courses, {} teachers, {} rooms",
            allStudents.size(), allCourses.size(), allTeachers.size(), allRooms.size());
    }

    private void setupSelectionLists() {
        // Setup student list with multiple selection
        studentListView.setItems(allStudents);
        studentListView.setCellFactory(lv -> new ListCell<Student>() {
            @Override
            protected void updateItem(Student student, boolean empty) {
                super.updateItem(student, empty);
                if (empty || student == null) {
                    setText(null);
                } else {
                    setText(student.getFirstName() + " " + student.getLastName() + " (" + student.getStudentId() + ")");
                }
            }
        });
        studentListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        studentListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateSelectionCounts());

        // Setup course list with multiple selection
        courseListView.setItems(allCourses);
        courseListView.setCellFactory(lv -> new ListCell<Course>() {
            @Override
            protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                } else {
                    setText(course.getCourseName() + " (" + course.getCourseCode() + ")");
                }
            }
        });
        courseListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        courseListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateSelectionCounts());

        // Initial count update
        updateSelectionCounts();
    }

    private void setupButtonHandler() {
        // Get the dialog pane
        DialogPane dialogPane = (DialogPane) scheduleNameField.getScene().getRoot();

        // Get the generate button
        Button generateButton = (Button) dialogPane.lookupButton(generateButtonType);
        if (generateButton != null) {
            generateButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                if (!validateAndGenerate()) {
                    event.consume(); // Prevent dialog from closing
                }
            });
        }
    }

    // ==================== Selection Handlers ====================

    @FXML
    private void handleSelectAllStudents() {
        studentListView.getSelectionModel().selectAll();
        updateSelectionCounts();
    }

    @FXML
    private void handleClearStudents() {
        studentListView.getSelectionModel().clearSelection();
        updateSelectionCounts();
    }

    @FXML
    private void handleSelectAllCourses() {
        courseListView.getSelectionModel().selectAll();
        updateSelectionCounts();
    }

    @FXML
    private void handleClearCourses() {
        courseListView.getSelectionModel().clearSelection();
        updateSelectionCounts();
    }

    private void updateSelectionCounts() {
        int studentCount = studentListView.getSelectionModel().getSelectedItems().size();
        int courseCount = courseListView.getSelectionModel().getSelectedItems().size();

        studentCountLabel.setText(studentCount + " selected");
        courseCountLabel.setText(courseCount + " selected");
    }

    // ==================== Generation ====================

    private boolean validateAndGenerate() {
        // Validate inputs
        if (scheduleNameField.getText().trim().isEmpty()) {
            DialogUtils.showError("Validation Error", "Please enter a schedule name");
            return false;
        }

        List<Student> selectedStudents = new ArrayList<>(studentListView.getSelectionModel().getSelectedItems());
        List<Course> selectedCourses = new ArrayList<>(courseListView.getSelectionModel().getSelectedItems());

        if (selectedStudents.isEmpty()) {
            DialogUtils.showError("Validation Error", "Please select at least one student");
            return false;
        }

        if (selectedCourses.isEmpty()) {
            DialogUtils.showError("Validation Error", "Please select at least one course");
            return false;
        }

        // Show progress
        progressIndicator.setVisible(true);
        statusLabel.setText("Generating block schedule...");

        // Generate schedule in background
        new Thread(() -> {
            try {
                log.info("Generating block schedule with {} students and {} courses",
                    selectedStudents.size(), selectedCourses.size());

                Schedule schedule = blockScheduleService.generateBlockSchedule(
                    selectedStudents,
                    selectedCourses,
                    allTeachers,
                    allRooms
                );

                // Update schedule name and dates
                schedule.setName(scheduleNameField.getText().trim());
                schedule.setStartDate(startDatePicker.getValue());
                schedule.setEndDate(endDatePicker.getValue());
                Schedule savedSchedule = scheduleRepository.save(schedule);

                generatedSchedule = savedSchedule;

                // Create final variables for lambda
                final int coursesCount = selectedCourses.size();
                final int studentsCount = selectedStudents.size();
                final String scheduleName = savedSchedule.getName();
                final int slotsCount = savedSchedule.getSlots() != null ? savedSchedule.getSlots().size() : 0;

                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Schedule generated successfully!");

                    DialogUtils.showSuccess(
                        "Success",
                        String.format("Block schedule '%s' created successfully!\n\n" +
                                "• %d courses\n" +
                                "• %d students\n" +
                                "• %d schedule slots\n" +
                                "• ODD/EVEN day rotation",
                            scheduleName,
                            coursesCount,
                            studentsCount,
                            slotsCount
                        )
                    );

                    // Close dialog
                    closeDialog();
                });

            } catch (Exception e) {
                log.error("Error generating block schedule", e);
                Platform.runLater(() -> {
                    progressIndicator.setVisible(false);
                    statusLabel.setText("Error generating schedule");
                    DialogUtils.showError(
                        "Generation Error",
                        "Failed to generate block schedule: " + e.getMessage()
                    );
                });
            }
        }).start();

        return true; // Return true to allow dialog to close after async operation
    }

    private void closeDialog() {
        DialogPane dialogPane = (DialogPane) scheduleNameField.getScene().getRoot();
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        if (cancelButton != null) {
            cancelButton.fire();
        }
    }

    // ==================== Getters ====================

    public Schedule getGeneratedSchedule() {
        return generatedSchedule;
    }
}
