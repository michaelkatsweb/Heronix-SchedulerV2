package com.heronix.scheduler.controller.ui.dialogs;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Student;
import com.heronix.scheduler.model.enums.DayType;
import com.heronix.scheduler.service.BlockScheduleService;
import com.heronix.scheduler.util.DialogUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.heronix.scheduler.service.data.SISDataService;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for ODD/EVEN Day Assignment Dialog
 * Location: src/main/java/com/eduscheduler/ui/controller/dialogs/OddEvenDayAssignmentDialogController.java
 *
 * @author Heronix Scheduling System Team
 * @since Block Scheduling MVP - November 26, 2025
 */
@Slf4j
@Controller
public class OddEvenDayAssignmentDialogController {

    @Autowired
    private SISDataService sisDataService;
    // ==================== FXML Fields ====================

    @FXML private ComboBox<Student> studentComboBox;
    @FXML private ListView<Course> oddDayCoursesListView;
    @FXML private ListView<Course> evenDayCoursesListView;
    @FXML private ListView<Course> availableCoursesListView;
    @FXML private Label oddDayCountLabel;
    @FXML private Label evenDayCountLabel;
    @FXML private Label availableCountLabel;
    @FXML private Label statusLabel;
    @FXML private ButtonType saveButtonType;

    // ==================== Services & Repositories ====================

    @Autowired
    private BlockScheduleService blockScheduleService;

    // ==================== Data ====================

    private ObservableList<Student> allStudents;
    private ObservableList<Course> allCourses;
    private ObservableList<Course> oddDayCourses;
    private ObservableList<Course> evenDayCourses;
    private ObservableList<Course> availableCourses;

    private Student selectedStudent;

    // ==================== Initialization ====================

    @FXML
    private void initialize() {
        log.info("Initializing ODD/EVEN Day Assignment Dialog");

        loadData();
        setupStudentComboBox();
        setupListViews();
        setupButtonHandler();

        statusLabel.setText("Select a student to assign their ODD/EVEN day courses");
    }

    private void loadData() {
        log.info("Loading students and courses");

        // Load all active students
        allStudents = FXCollections.observableArrayList(
            sisDataService.getAllStudents().stream()
                .filter(Student::isActive)
                .collect(Collectors.toList())
        );

        // Load all active courses
        allCourses = FXCollections.observableArrayList(
            sisDataService.getAllCourses().stream()
                .filter(Course::isActive)
                .collect(Collectors.toList())
        );

        // Initialize course lists
        oddDayCourses = FXCollections.observableArrayList();
        evenDayCourses = FXCollections.observableArrayList();
        availableCourses = FXCollections.observableArrayList();

        log.info("Loaded {} students and {} courses", allStudents.size(), allCourses.size());
    }

    private void setupStudentComboBox() {
        studentComboBox.setItems(allStudents);

        // Custom string converter to display student name
        studentComboBox.setConverter(new StringConverter<Student>() {
            @Override
            public String toString(Student student) {
                if (student == null) {
                    return null;
                }
                return student.getFirstName() + " " + student.getLastName() +
                       " (" + student.getStudentId() + ")";
            }

            @Override
            public Student fromString(String string) {
                return null; // Not needed for display-only
            }
        });

        // Handle student selection
        studentComboBox.setOnAction(event -> handleStudentSelection());
    }

    private void setupListViews() {
        // Setup cell factories for course display
        oddDayCoursesListView.setCellFactory(lv -> new CourseListCell());
        evenDayCoursesListView.setCellFactory(lv -> new CourseListCell());
        availableCoursesListView.setCellFactory(lv -> new CourseListCell());

        // Enable multiple selection for available courses
        availableCoursesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        oddDayCoursesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        evenDayCoursesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // Set items
        oddDayCoursesListView.setItems(oddDayCourses);
        evenDayCoursesListView.setItems(evenDayCourses);
        availableCoursesListView.setItems(availableCourses);
    }

    private void setupButtonHandler() {
        // Get the dialog pane
        DialogPane dialogPane = (DialogPane) studentComboBox.getScene().getRoot();

        // Get the save button
        Button saveButton = (Button) dialogPane.lookupButton(saveButtonType);
        if (saveButton != null) {
            saveButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                if (!validateAndSave()) {
                    event.consume(); // Prevent dialog from closing
                }
            });
        }
    }

    // ==================== Student Selection ====================

    private void handleStudentSelection() {
        selectedStudent = studentComboBox.getValue();

        if (selectedStudent == null) {
            return;
        }

        log.info("Selected student: {} {}", selectedStudent.getFirstName(), selectedStudent.getLastName());

        // Load student's current ODD/EVEN day assignments
        loadStudentAssignments();
    }

    private void loadStudentAssignments() {
        try {
            // Get courses for ODD days
            List<Course> oddCourses = blockScheduleService.getCoursesForDayType(
                selectedStudent, DayType.ODD
            );

            // Get courses for EVEN days
            List<Course> evenCourses = blockScheduleService.getCoursesForDayType(
                selectedStudent, DayType.EVEN
            );

            // Update lists
            oddDayCourses.clear();
            oddDayCourses.addAll(oddCourses);

            evenDayCourses.clear();
            evenDayCourses.addAll(evenCourses);

            // Calculate available courses (not assigned to either day)
            availableCourses.clear();
            for (Course course : allCourses) {
                if (!oddCourses.contains(course) && !evenCourses.contains(course)) {
                    availableCourses.add(course);
                }
            }

            updateCounts();
            statusLabel.setText(String.format("Loaded assignments for %s %s",
                selectedStudent.getFirstName(), selectedStudent.getLastName()));

            log.info("Loaded {} ODD courses, {} EVEN courses, {} available for student {}",
                oddCourses.size(), evenCourses.size(), availableCourses.size(),
                selectedStudent.getStudentId());

        } catch (Exception e) {
            log.error("Error loading student assignments", e);
            DialogUtils.showError("Error", "Failed to load student assignments: " + e.getMessage());
        }
    }

    // ==================== Course Assignment Handlers ====================

    @FXML
    private void handleAddToOddDay() {
        List<Course> selectedCourses = new ArrayList<>(
            availableCoursesListView.getSelectionModel().getSelectedItems()
        );

        if (selectedCourses.isEmpty()) {
            DialogUtils.showWarning("No Selection", "Please select courses to add to ODD days");
            return;
        }

        // Move courses from available to ODD day
        availableCourses.removeAll(selectedCourses);
        oddDayCourses.addAll(selectedCourses);

        updateCounts();
        statusLabel.setText(String.format("Added %d course(s) to ODD days", selectedCourses.size()));
        log.info("Added {} courses to ODD days", selectedCourses.size());
    }

    @FXML
    private void handleAddToEvenDay() {
        List<Course> selectedCourses = new ArrayList<>(
            availableCoursesListView.getSelectionModel().getSelectedItems()
        );

        if (selectedCourses.isEmpty()) {
            DialogUtils.showWarning("No Selection", "Please select courses to add to EVEN days");
            return;
        }

        // Move courses from available to EVEN day
        availableCourses.removeAll(selectedCourses);
        evenDayCourses.addAll(selectedCourses);

        updateCounts();
        statusLabel.setText(String.format("Added %d course(s) to EVEN days", selectedCourses.size()));
        log.info("Added {} courses to EVEN days", selectedCourses.size());
    }

    @FXML
    private void handleRemoveFromOddDay() {
        List<Course> selectedCourses = new ArrayList<>(
            oddDayCoursesListView.getSelectionModel().getSelectedItems()
        );

        if (selectedCourses.isEmpty()) {
            DialogUtils.showWarning("No Selection", "Please select courses to remove from ODD days");
            return;
        }

        // Move courses from ODD day back to available
        oddDayCourses.removeAll(selectedCourses);
        availableCourses.addAll(selectedCourses);

        updateCounts();
        statusLabel.setText(String.format("Removed %d course(s) from ODD days", selectedCourses.size()));
        log.info("Removed {} courses from ODD days", selectedCourses.size());
    }

    @FXML
    private void handleRemoveFromEvenDay() {
        List<Course> selectedCourses = new ArrayList<>(
            evenDayCoursesListView.getSelectionModel().getSelectedItems()
        );

        if (selectedCourses.isEmpty()) {
            DialogUtils.showWarning("No Selection", "Please select courses to remove from EVEN days");
            return;
        }

        // Move courses from EVEN day back to available
        evenDayCourses.removeAll(selectedCourses);
        availableCourses.addAll(selectedCourses);

        updateCounts();
        statusLabel.setText(String.format("Removed %d course(s) from EVEN days", selectedCourses.size()));
        log.info("Removed {} courses from EVEN days", selectedCourses.size());
    }

    private void updateCounts() {
        oddDayCountLabel.setText(oddDayCourses.size() + " selected");
        evenDayCountLabel.setText(evenDayCourses.size() + " selected");
        availableCountLabel.setText(availableCourses.size() + " available");
    }

    // ==================== Validation & Save ====================

    private boolean validateAndSave() {
        if (selectedStudent == null) {
            DialogUtils.showError("Validation Error", "Please select a student");
            return false;
        }

        // Validate that assignments make sense
        if (oddDayCourses.isEmpty() && evenDayCourses.isEmpty()) {
            DialogUtils.showWarning("No Assignments",
                "No courses assigned to either ODD or EVEN days.\nDo you want to save anyway?");
            // Allow saving with no assignments (student might be new)
        }

        try {
            log.info("Saving ODD/EVEN day assignments for student: {}", selectedStudent.getStudentId());

            // Save assignments via service
            blockScheduleService.assignCoursesToDays(
                selectedStudent,
                new ArrayList<>(oddDayCourses),
                new ArrayList<>(evenDayCourses)
            );

            DialogUtils.showSuccess(
                "Success",
                String.format("Course assignments saved for %s %s\n\n" +
                              "• %d ODD day courses\n" +
                              "• %d EVEN day courses",
                    selectedStudent.getFirstName(),
                    selectedStudent.getLastName(),
                    oddDayCourses.size(),
                    evenDayCourses.size()
                )
            );

            log.info("Successfully saved assignments: {} ODD, {} EVEN",
                oddDayCourses.size(), evenDayCourses.size());

            return true;

        } catch (Exception e) {
            log.error("Error saving assignments", e);
            DialogUtils.showError(
                "Save Error",
                "Failed to save course assignments: " + e.getMessage()
            );
            return false;
        }
    }

    // ==================== Custom Cell Factory ====================

    /**
     * Custom ListCell for Course display
     */
    private static class CourseListCell extends ListCell<Course> {
        @Override
        protected void updateItem(Course course, boolean empty) {
            super.updateItem(course, empty);

            if (empty || course == null) {
                setText(null);
                setGraphic(null);
            } else {
                setText(String.format("%s (%s)",
                    course.getCourseName(),
                    course.getCourseCode()
                ));
            }
        }
    }
}
