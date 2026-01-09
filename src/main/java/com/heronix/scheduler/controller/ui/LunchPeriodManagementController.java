package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.LunchPeriod;
import com.heronix.scheduler.service.LunchPeriodService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Controller for Lunch Period Management UI
 * Manages lunch periods/rotations for student scheduling
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-11-07
 */
@Controller
public class LunchPeriodManagementController {

    private static final Logger logger = LoggerFactory.getLogger(LunchPeriodManagementController.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private LunchPeriodService lunchPeriodService;

    // Lunch Periods Tab
    @FXML private TextField lunchPeriodSearchField;
    @FXML private TableView<LunchPeriod> lunchPeriodsTable;
    @FXML private TableColumn<LunchPeriod, String> lpNameColumn;
    @FXML private TableColumn<LunchPeriod, String> lpGroupColumn;
    @FXML private TableColumn<LunchPeriod, String> lpTimeColumn;
    @FXML private TableColumn<LunchPeriod, Integer> lpCapacityColumn;
    @FXML private TableColumn<LunchPeriod, Integer> lpCurrentColumn;
    @FXML private TableColumn<LunchPeriod, String> lpLocationColumn;
    @FXML private TableColumn<LunchPeriod, String> lpStatusColumn;
    @FXML private TableColumn<LunchPeriod, Void> lpActionsColumn;
    @FXML private Label lunchPeriodCountLabel;

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        logger.info("Initializing Lunch Period Management Controller");
        setupLunchPeriodsTab();
        loadLunchPeriods();
    }

    /**
     * Setup Lunch Periods Tab
     */
    private void setupLunchPeriodsTab() {
        lpNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));

        lpGroupColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLunchGroup()));

        lpTimeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTimeRangeFormatted()));

        lpCapacityColumn.setCellValueFactory(new PropertyValueFactory<>("maxCapacity"));

        lpCurrentColumn.setCellValueFactory(new PropertyValueFactory<>("currentCount"));

        lpLocationColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLocation()));

        lpStatusColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getActive() ? "Active" : "Inactive"));

        // Add actions column
        addLunchPeriodActionsColumn();
    }

    /**
     * Add actions column to lunch periods table
     */
    private void addLunchPeriodActionsColumn() {
        lpActionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button viewButton = new Button("View");

            {
                editButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8;");
                viewButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-cursor: hand; -fx-padding: 4 8;");

                editButton.setOnAction(event -> {
                    LunchPeriod lunchPeriod = getTableView().getItems().get(getIndex());
                    handleEditLunchPeriod(lunchPeriod);
                });

                viewButton.setOnAction(event -> {
                    LunchPeriod lunchPeriod = getTableView().getItems().get(getIndex());
                    handleViewLunchPeriod(lunchPeriod);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox buttons = new javafx.scene.layout.HBox(5, editButton, viewButton);
                    setGraphic(buttons);
                }
            }
        });
    }

    /**
     * Load lunch periods
     */
    private void loadLunchPeriods() {
        List<LunchPeriod> lunchPeriods = lunchPeriodService.getAllLunchPeriods();
        lunchPeriodsTable.setItems(FXCollections.observableArrayList(lunchPeriods));
        lunchPeriodCountLabel.setText("Total: " + lunchPeriods.size() + " lunch periods");
        logger.info("Loaded {} lunch periods", lunchPeriods.size());
    }

    /**
     * Handle search lunch periods
     */
    @FXML
    private void handleSearchLunchPeriods() {
        String query = lunchPeriodSearchField.getText();
        if (query == null || query.trim().isEmpty()) {
            loadLunchPeriods();
            return;
        }

        // Search by filtering all lunch periods
        String queryLower = query.trim().toLowerCase();
        List<LunchPeriod> allPeriods = lunchPeriodService.getAllLunchPeriods();
        List<LunchPeriod> results = allPeriods.stream()
                .filter(lp -> lp.getName() != null && lp.getName().toLowerCase().contains(queryLower))
                .toList();

        lunchPeriodsTable.setItems(FXCollections.observableArrayList(results));
        lunchPeriodCountLabel.setText("Found: " + results.size() + " lunch periods");
    }

    /**
     * Handle add lunch period
     */
    @FXML
    private void handleAddLunchPeriod() {
        Dialog<LunchPeriod> dialog = createLunchPeriodDialog(null);
        Optional<LunchPeriod> result = dialog.showAndWait();

        result.ifPresent(lunchPeriod -> {
            try {
                LunchPeriod saved = lunchPeriodService.createLunchPeriod(lunchPeriod);
                showInfo("Success", "Lunch Period " + saved.getName() + " added successfully!");
                loadLunchPeriods();
            } catch (Exception e) {
                logger.error("Error adding lunch period", e);
                showError("Error", "Failed to add lunch period: " + e.getMessage());
            }
        });
    }

    /**
     * Handle refresh lunch periods
     */
    @FXML
    private void handleRefreshLunchPeriods() {
        lunchPeriodSearchField.clear();
        loadLunchPeriods();
    }

    /**
     * Handle edit lunch period
     */
    private void handleEditLunchPeriod(LunchPeriod lunchPeriod) {
        Dialog<LunchPeriod> dialog = createLunchPeriodDialog(lunchPeriod);
        Optional<LunchPeriod> result = dialog.showAndWait();

        result.ifPresent(updated -> {
            try {
                LunchPeriod saved = lunchPeriodService.updateLunchPeriod(updated.getId(), updated);
                showInfo("Success", "Lunch Period " + saved.getName() + " updated successfully!");
                loadLunchPeriods();
            } catch (Exception e) {
                logger.error("Error updating lunch period", e);
                showError("Error", "Failed to update lunch period: " + e.getMessage());
            }
        });
    }

    /**
     * Handle view lunch period
     */
    private void handleViewLunchPeriod(LunchPeriod lunchPeriod) {
        StringBuilder details = new StringBuilder();
        details.append("Name: ").append(lunchPeriod.getName()).append("\n");
        details.append("Group: ").append(lunchPeriod.getLunchGroup()).append("\n");
        details.append("Time: ").append(lunchPeriod.getTimeRangeFormatted()).append("\n");
        details.append("Duration: ").append(lunchPeriod.getDurationFormatted()).append("\n");
        details.append("Capacity: ").append(lunchPeriod.getCurrentCount()).append("/").append(lunchPeriod.getMaxCapacity()).append("\n");
        details.append("Utilization: ").append(String.format("%.1f%%", lunchPeriod.getUtilizationRate())).append("\n");
        if (lunchPeriod.getLocation() != null) {
            details.append("Location: ").append(lunchPeriod.getLocation()).append("\n");
        }
        if (lunchPeriod.getGradeLevels() != null) {
            details.append("Grade Levels: ").append(lunchPeriod.getGradeLevels()).append("\n");
        }
        if (lunchPeriod.getNotes() != null && !lunchPeriod.getNotes().isEmpty()) {
            details.append("Notes: ").append(lunchPeriod.getNotes()).append("\n");
        }

        showInfo("Lunch Period Details", details.toString());
    }

    /**
     * Create lunch period dialog for add/edit operations
     */
    private Dialog<LunchPeriod> createLunchPeriodDialog(LunchPeriod existingLP) {
        Dialog<LunchPeriod> dialog = new Dialog<>();
        dialog.setTitle(existingLP == null ? "Add New Lunch Period" : "Edit Lunch Period");
        dialog.setHeaderText(existingLP == null ? "Enter lunch period information" : "Modify lunch period information");

        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Form fields
        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Lunch 1, A Lunch");

        TextField lunchGroupField = new TextField();
        lunchGroupField.setPromptText("e.g., Period 4/5");

        TextField startTimeField = new TextField();
        startTimeField.setPromptText("HH:mm (e.g., 11:30)");

        TextField endTimeField = new TextField();
        endTimeField.setPromptText("HH:mm (e.g., 12:00)");

        TextField maxCapacityField = new TextField();
        maxCapacityField.setPromptText("e.g., 150");

        TextField locationField = new TextField();
        locationField.setPromptText("e.g., Cafeteria, Commons");

        TextField gradeLevelsField = new TextField();
        gradeLevelsField.setPromptText("e.g., 9,10 or 11,12");

        TextField displayOrderField = new TextField();
        displayOrderField.setPromptText("e.g., 1, 2, 3");

        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes or special instructions");
        notesArea.setPrefRowCount(3);

        CheckBox activeCheckBox = new CheckBox("Active");
        activeCheckBox.setSelected(true);

        // Populate if editing
        if (existingLP != null) {
            nameField.setText(existingLP.getName());
            lunchGroupField.setText(existingLP.getLunchGroup());
            startTimeField.setText(existingLP.getStartTime() != null ? existingLP.getStartTime().format(TIME_FORMATTER) : "");
            endTimeField.setText(existingLP.getEndTime() != null ? existingLP.getEndTime().format(TIME_FORMATTER) : "");
            maxCapacityField.setText(existingLP.getMaxCapacity() != null ? String.valueOf(existingLP.getMaxCapacity()) : "");
            locationField.setText(existingLP.getLocation());
            gradeLevelsField.setText(existingLP.getGradeLevels());
            displayOrderField.setText(existingLP.getDisplayOrder() != null ? String.valueOf(existingLP.getDisplayOrder()) : "");
            notesArea.setText(existingLP.getNotes());
            activeCheckBox.setSelected(existingLP.getActive());
        }

        // Add fields to grid
        int row = 0;
        grid.add(new Label("Name: *"), 0, row);
        grid.add(nameField, 1, row);
        row++;

        grid.add(new Label("Lunch Group:"), 0, row);
        grid.add(lunchGroupField, 1, row);
        row++;

        grid.add(new Label("Start Time: *"), 0, row);
        grid.add(startTimeField, 1, row);
        row++;

        grid.add(new Label("End Time: *"), 0, row);
        grid.add(endTimeField, 1, row);
        row++;

        grid.add(new Label("Max Capacity: *"), 0, row);
        grid.add(maxCapacityField, 1, row);
        row++;

        grid.add(new Label("Location:"), 0, row);
        grid.add(locationField, 1, row);
        row++;

        grid.add(new Label("Grade Levels:"), 0, row);
        grid.add(gradeLevelsField, 1, row);
        row++;

        grid.add(new Label("Display Order:"), 0, row);
        grid.add(displayOrderField, 1, row);
        row++;

        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row);
        row++;

        grid.add(activeCheckBox, 1, row);

        dialog.getDialogPane().setContent(grid);

        // Validation and result converter
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        // Enable save button when required fields are filled
        nameField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateRequiredFields(saveButton, nameField, startTimeField, endTimeField, maxCapacityField);
        });
        startTimeField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateRequiredFields(saveButton, nameField, startTimeField, endTimeField, maxCapacityField);
        });
        endTimeField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateRequiredFields(saveButton, nameField, startTimeField, endTimeField, maxCapacityField);
        });
        maxCapacityField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateRequiredFields(saveButton, nameField, startTimeField, endTimeField, maxCapacityField);
        });

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    LunchPeriod lp = existingLP != null ? existingLP : new LunchPeriod();

                    lp.setName(nameField.getText().trim());
                    lp.setLunchGroup(lunchGroupField.getText().trim().isEmpty() ? null : lunchGroupField.getText().trim());
                    lp.setStartTime(LocalTime.parse(startTimeField.getText().trim(), TIME_FORMATTER));
                    lp.setEndTime(LocalTime.parse(endTimeField.getText().trim(), TIME_FORMATTER));
                    lp.setMaxCapacity(Integer.parseInt(maxCapacityField.getText().trim()));
                    lp.setLocation(locationField.getText().trim().isEmpty() ? null : locationField.getText().trim());
                    lp.setGradeLevels(gradeLevelsField.getText().trim().isEmpty() ? null : gradeLevelsField.getText().trim());
                    lp.setNotes(notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim());
                    lp.setActive(activeCheckBox.isSelected());

                    // Parse display order
                    if (!displayOrderField.getText().trim().isEmpty()) {
                        try {
                            lp.setDisplayOrder(Integer.parseInt(displayOrderField.getText().trim()));
                        } catch (NumberFormatException e) {
                            lp.setDisplayOrder(0);
                        }
                    } else {
                        lp.setDisplayOrder(0);
                    }

                    return lp;
                } catch (DateTimeParseException e) {
                    showError("Invalid Time Format", "Please enter times in HH:mm format (e.g., 11:30)");
                    return null;
                } catch (NumberFormatException e) {
                    showError("Invalid Number", "Please enter a valid number for capacity");
                    return null;
                } catch (Exception e) {
                    logger.error("Error converting dialog result", e);
                    showError("Error", "Failed to process lunch period data: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });

        // Request focus on first field
        javafx.application.Platform.runLater(() -> nameField.requestFocus());

        return dialog;
    }

    /**
     * Validate required fields
     */
    private void validateRequiredFields(Node saveButton, TextField... fields) {
        boolean allFilled = true;
        for (TextField field : fields) {
            if (field.getText() == null || field.getText().trim().isEmpty()) {
                allFilled = false;
                break;
            }
        }
        saveButton.setDisable(!allFilled);
    }

    /**
     * Show error message
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show info message
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
