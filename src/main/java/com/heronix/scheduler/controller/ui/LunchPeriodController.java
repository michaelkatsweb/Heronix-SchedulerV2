package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.LunchPeriod;
import com.heronix.scheduler.service.LunchPeriodService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalTime;
import java.util.List;

@Component
public class LunchPeriodController {

    @Autowired
    private LunchPeriodService lunchPeriodService;

    @FXML
    private TableView<LunchPeriod> lunchPeriodTable;
    @FXML
    private TextField nameField;
    @FXML
    private TextField lunchGroupField;
    @FXML
    private TextField startTimeField;
    @FXML
    private TextField endTimeField;
    @FXML
    private TextField maxStudentsField;
    @FXML
    private TextField locationField;
    @FXML
    private TextField gradeRestrictionField; // Keep field name, just change getter/setter
    @FXML
    private CheckBox activeCheckBox;
    @FXML
    private TextArea notesArea;

    @FXML
    public void initialize() {
        setupTable();
        loadLunchPeriods();
    }

    private void setupTable() {
        // Setup table columns
        TableColumn<LunchPeriod, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getName()));

        TableColumn<LunchPeriod, String> groupCol = new TableColumn<>("Group");
        groupCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getLunchGroup()));

        TableColumn<LunchPeriod, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getStartTime() + " - " + data.getValue().getEndTime()));

        TableColumn<LunchPeriod, String> locationCol = new TableColumn<>("Location");
        locationCol.setCellValueFactory(
                data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getLocation()));

        lunchPeriodTable.getColumns().addAll(List.of(nameCol, groupCol, timeCol, locationCol));
    }

    private void loadLunchPeriods() {
        // FIX: Changed from getAllActiveLunchPeriods() to getActiveLunchPeriods()
        List<LunchPeriod> periods = lunchPeriodService.getActiveLunchPeriods();
        lunchPeriodTable.setItems(FXCollections.observableArrayList(periods));
    }

    @FXML
    private void handleAdd() {
        try {
            LunchPeriod lunchPeriod = new LunchPeriod();
            lunchPeriod.setName(nameField.getText());
            lunchPeriod.setLunchGroup(lunchGroupField.getText());
            lunchPeriod.setStartTime(LocalTime.parse(startTimeField.getText()));
            lunchPeriod.setEndTime(LocalTime.parse(endTimeField.getText()));
            lunchPeriod.setMaxStudents(Integer.parseInt(maxStudentsField.getText()));
            lunchPeriod.setLocation(locationField.getText());
            // FIX: Changed from setGradeRestrictions() to setGradeLevels()
            lunchPeriod.setGradeLevels(gradeRestrictionField.getText());
            lunchPeriod.setActive(activeCheckBox.isSelected());
            lunchPeriod.setNotes(notesArea.getText());

            lunchPeriodService.createLunchPeriod(lunchPeriod);
            loadLunchPeriods();
            clearForm();

            showSuccess("Lunch period added successfully!");
        } catch (Exception e) {
            showError("Error adding lunch period: " + e.getMessage());
        }
    }

    @FXML
    private void handleEdit() {
        LunchPeriod selected = lunchPeriodTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a lunch period to edit");
            return;
        }

        try {
            selected.setName(nameField.getText());
            selected.setLunchGroup(lunchGroupField.getText());
            selected.setStartTime(LocalTime.parse(startTimeField.getText()));
            selected.setEndTime(LocalTime.parse(endTimeField.getText()));
            selected.setMaxStudents(Integer.parseInt(maxStudentsField.getText()));
            selected.setLocation(locationField.getText());
            // FIX: Changed from setGradeRestrictions() to setGradeLevels()
            selected.setGradeLevels(gradeRestrictionField.getText());
            selected.setActive(activeCheckBox.isSelected());
            selected.setNotes(notesArea.getText());

            // FIX: Changed from getId() to getLunchPeriodId()
            lunchPeriodService.updateLunchPeriod(selected.getId(), selected);
            loadLunchPeriods();
            clearForm();

            showSuccess("Lunch period updated successfully!");
        } catch (Exception e) {
            showError("Error updating lunch period: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        LunchPeriod selected = lunchPeriodTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("Please select a lunch period to delete");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete lunch period?");
        confirm.setContentText("This will mark the lunch period as inactive.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // FIX: Changed from getId() to getLunchPeriodId()
            lunchPeriodService.deleteLunchPeriod(selected.getId());
            loadLunchPeriods();
            clearForm();
            showSuccess("Lunch period deleted successfully!");
        }
    }

    @FXML
    private void handleSelectionChange() {
        LunchPeriod selected = lunchPeriodTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            nameField.setText(selected.getName());
            lunchGroupField.setText(selected.getLunchGroup());
            startTimeField.setText(selected.getStartTime().toString());
            endTimeField.setText(selected.getEndTime().toString());
            maxStudentsField.setText(String.valueOf(selected.getMaxStudents()));
            locationField.setText(selected.getLocation());
            // FIX: Changed from getGradeRestrictions() to getGradeLevels()
            gradeRestrictionField.setText(selected.getGradeLevels());
            activeCheckBox.setSelected(selected.getActive());
            notesArea.setText(selected.getNotes());
        }
    }

    private void clearForm() {
        nameField.clear();
        lunchGroupField.clear();
        startTimeField.clear();
        endTimeField.clear();
        maxStudentsField.clear();
        locationField.clear();
        gradeRestrictionField.clear();
        activeCheckBox.setSelected(true);
        notesArea.clear();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setContentText(message);
        alert.showAndWait();
    }
}