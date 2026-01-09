package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.SpecialEventBlock;
import com.heronix.scheduler.model.domain.SpecialEventBlock.EventBlockType;
import com.heronix.scheduler.service.SpecialEventBlockService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Special Event Block Controller
 * Location:
 * src/main/java/com/eduscheduler/ui/controller/SpecialEventBlockController.java
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class SpecialEventBlockController {

    private final SpecialEventBlockService eventBlockService;

    @FXML
    private TableView<SpecialEventBlock> eventBlocksTable;
    @FXML
    private TableColumn<SpecialEventBlock, EventBlockType> typeColumn;
    @FXML
    private TableColumn<SpecialEventBlock, DayOfWeek> dayColumn;
    @FXML
    private TableColumn<SpecialEventBlock, LocalTime> startTimeColumn;
    @FXML
    private TableColumn<SpecialEventBlock, LocalTime> endTimeColumn;
    @FXML
    private TableColumn<SpecialEventBlock, Boolean> blocksTeachingColumn;
    @FXML
    private TableColumn<SpecialEventBlock, Boolean> activeColumn;

    @FXML
    private ComboBox<EventBlockType> eventTypeCombo;
    @FXML
    private ComboBox<DayOfWeek> dayOfWeekCombo;
    @FXML
    private TextField startTimeField;
    @FXML
    private TextField endTimeField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private CheckBox blocksTeachingCheckBox;
    @FXML
    private CheckBox activeCheckBox;

    @FXML
    private Button addButton;
    @FXML
    private Button updateButton;
    @FXML
    private Button deleteButton;

    private Stage dialogStage;
    private ObservableList<SpecialEventBlock> eventBlockList = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Setup table columns
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("blockType"));
        dayColumn.setCellValueFactory(new PropertyValueFactory<>("dayOfWeek"));
        startTimeColumn.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        endTimeColumn.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        blocksTeachingColumn.setCellValueFactory(new PropertyValueFactory<>("blocksTeaching"));
        activeColumn.setCellValueFactory(new PropertyValueFactory<>("active"));

        // Format time columns
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        startTimeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalTime time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? null : time.format(timeFormatter));
            }
        });
        endTimeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalTime time, boolean empty) {
                super.updateItem(time, empty);
                setText(empty || time == null ? null : time.format(timeFormatter));
            }
        });

        // Checkbox columns
        blocksTeachingColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean blocks, boolean empty) {
                super.updateItem(blocks, empty);
                if (empty || blocks == null) {
                    setGraphic(null);
                } else {
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected(blocks);
                    checkBox.setDisable(true);
                    setGraphic(checkBox);
                }
            }
        });

        activeColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean active, boolean empty) {
                super.updateItem(active, empty);
                if (empty || active == null) {
                    setGraphic(null);
                } else {
                    CheckBox checkBox = new CheckBox();
                    checkBox.setSelected(active);
                    checkBox.setDisable(true);
                    setGraphic(checkBox);
                }
            }
        });

        eventBlocksTable.setItems(eventBlockList);

        // Setup dropdowns
        eventTypeCombo.setItems(FXCollections.observableArrayList(EventBlockType.values()));
        dayOfWeekCombo.setItems(FXCollections.observableArrayList(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY));

        // Selection listener
        eventBlocksTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, newVal) -> loadEventBlockDetails(newVal));

        // Load data
        refreshData();
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    private void refreshData() {
        eventBlockList.clear();
        eventBlockList.addAll(eventBlockService.getAllActiveBlocks());
    }

    private void loadEventBlockDetails(SpecialEventBlock eventBlock) {
        if (eventBlock == null) {
            clearForm();
            return;
        }

        eventTypeCombo.setValue(eventBlock.getBlockType());
        dayOfWeekCombo.setValue(eventBlock.getDayOfWeek());
        startTimeField.setText(eventBlock.getStartTime() != null ? eventBlock.getStartTime().toString() : "");
        endTimeField.setText(eventBlock.getEndTime() != null ? eventBlock.getEndTime().toString() : "");
        descriptionArea.setText(eventBlock.getDescription());
        blocksTeachingCheckBox.setSelected(eventBlock.isBlocksTeaching());
        activeCheckBox.setSelected(eventBlock.isActive());
    }

    private void clearForm() {
        eventTypeCombo.setValue(null);
        dayOfWeekCombo.setValue(null);
        startTimeField.clear();
        endTimeField.clear();
        descriptionArea.clear();
        blocksTeachingCheckBox.setSelected(true);
        activeCheckBox.setSelected(true);
    }

    @FXML
    private void handleAdd() {
        try {
            SpecialEventBlock eventBlock = new SpecialEventBlock();
            eventBlock.setBlockType(eventTypeCombo.getValue());
            eventBlock.setDayOfWeek(dayOfWeekCombo.getValue());
            eventBlock.setStartTime(LocalTime.parse(startTimeField.getText()));
            eventBlock.setEndTime(LocalTime.parse(endTimeField.getText()));
            eventBlock.setDescription(descriptionArea.getText());
            eventBlock.setBlocksTeaching(blocksTeachingCheckBox.isSelected());
            eventBlock.setActive(activeCheckBox.isSelected());

            eventBlockService.createEventBlock(eventBlock);

            showSuccess("Event Block Added", "Event block created successfully.");
            refreshData();
            clearForm();

        } catch (Exception e) {
            log.error("Error adding event block", e);
            showError("Error", "Failed to add event block: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdate() {
        SpecialEventBlock selected = eventBlocksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select an event block to update.");
            return;
        }

        try {
            selected.setBlockType(eventTypeCombo.getValue());
            selected.setDayOfWeek(dayOfWeekCombo.getValue());
            selected.setStartTime(LocalTime.parse(startTimeField.getText()));
            selected.setEndTime(LocalTime.parse(endTimeField.getText()));
            selected.setDescription(descriptionArea.getText());
            selected.setBlocksTeaching(blocksTeachingCheckBox.isSelected());
            selected.setActive(activeCheckBox.isSelected());

            eventBlockService.updateEventBlock(selected.getId(), selected);

            showSuccess("Event Block Updated", "Event block updated successfully.");
            refreshData();

        } catch (Exception e) {
            log.error("Error updating event block", e);
            showError("Error", "Failed to update event block: " + e.getMessage());
        }
    }

    @FXML
    private void handleDelete() {
        SpecialEventBlock selected = eventBlocksTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showWarning("No Selection", "Please select an event block to delete.");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText("Delete Event Block?");
        confirmAlert.setContentText("Are you sure you want to delete this event block?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        try {
            eventBlockService.deleteEventBlock(selected.getId());
            showSuccess("Event Block Deleted", "Event block deleted successfully.");
            refreshData();
            clearForm();

        } catch (Exception e) {
            log.error("Error deleting event block", e);
            showError("Error", "Failed to delete event block: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        dialogStage.close();
    }

    private void showSuccess(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showWarning(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}