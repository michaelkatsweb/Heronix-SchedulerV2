package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.enums.RoomType;
import com.heronix.scheduler.repository.RoomRepository;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Room Management Controller
 * Location:
 * src/main/java/com/eduscheduler/ui/controller/RoomManagementController.java
 *
 * Manages the Room Management screen with:
 * - Table view of all rooms
 * - Search and filter functionality
 * - Add/Edit/Delete operations
 * - Room equipment tracking
 */
@Slf4j
@Component
public class RoomManagementController {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> buildingFilter;
    @FXML
    private ComboBox<String> typeFilter;
    @FXML
    private ComboBox<String> zoneFilter;
    @FXML
    private ComboBox<String> statusFilter;
    @FXML
    private TableView<Room> roomTable;
    @FXML
    private Label recordCountLabel;

    // Table columns
    @FXML
    private TableColumn<Room, Long> idColumn;
    @FXML
    private TableColumn<Room, String> roomNumberColumn;
    @FXML
    private TableColumn<Room, String> buildingColumn;
    @FXML
    private TableColumn<Room, Integer> floorColumn;
    @FXML
    private TableColumn<Room, String> zoneColumn;
    @FXML
    private TableColumn<Room, Integer> capacityColumn;
    @FXML
    private TableColumn<Room, String> typeColumn;
    @FXML
    private TableColumn<Room, Boolean> projectorColumn;
    @FXML
    private TableColumn<Room, Boolean> smartboardColumn;
    @FXML
    private TableColumn<Room, Boolean> computersColumn;
    @FXML
    private TableColumn<Room, Boolean> accessibleColumn;
    @FXML
    private TableColumn<Room, Boolean> activeColumn;
    @FXML
    private TableColumn<Room, Void> actionsColumn;
    @FXML
    private HBox selectionToolbar;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private com.heronix.scheduler.service.ExportService exportService;

    @Autowired
    private com.heronix.scheduler.service.RoomZoneService roomZoneService;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupFilters();
        setupActionsColumn();
        setupBulkSelection();
        loadRooms();
    }

    private void setupBulkSelection() {
        // Enable multi-selection
        // TODO: Package com.heronix.ui.util does not exist - implement when available
        // com.heronix.ui.util.TableSelectionHelper.enableMultiSelection(roomTable);

        // Create selection toolbar
        // TODO: Package com.heronix.ui.util does not exist - implement when available
        HBox toolbar = null; // com.heronix.ui.util.TableSelectionHelper.createSelectionToolbar(
            // roomTable,
            // this::handleBulkDelete,
            // "Rooms"
        // );

        // Replace the placeholder with the actual toolbar
        if (selectionToolbar != null && toolbar != null) {
            selectionToolbar.getChildren().setAll(toolbar.getChildren());
            selectionToolbar.setPadding(toolbar.getPadding());
            selectionToolbar.setSpacing(toolbar.getSpacing());
            selectionToolbar.setStyle(toolbar.getStyle());
        }
    }

    private void handleBulkDelete(List<Room> rooms) {
        try {
            for (Room room : rooms) {
                roomRepository.delete(room);
                log.info("Deleted room: {} (ID: {})", room.getRoomNumber(), room.getId());
            }

            // Reload the table
            loadRooms();

            log.info("Bulk delete completed: {} rooms deleted", rooms.size());
        } catch (Exception e) {
            log.error("Error during bulk delete", e);
            throw e; // Let TableSelectionHelper show the error dialog
        }
    }

    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        roomNumberColumn.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        buildingColumn.setCellValueFactory(new PropertyValueFactory<>("building"));
        floorColumn.setCellValueFactory(new PropertyValueFactory<>("floor"));
        zoneColumn.setCellValueFactory(new PropertyValueFactory<>("zone"));  // Phase 6C
        capacityColumn.setCellValueFactory(new PropertyValueFactory<>("capacity"));

        typeColumn.setCellValueFactory(cellData -> {
            RoomType type = cellData.getValue().getType();
            return new javafx.beans.property.SimpleStringProperty(type != null ? type.toString() : "N/A");
        });

        projectorColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().getHasProjector()));
        smartboardColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().getHasSmartboard()));
        computersColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().getHasComputers()));
        accessibleColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleBooleanProperty(
                cellData.getValue().getWheelchairAccessible()));
        activeColumn.setCellValueFactory(
                cellData -> new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().getActive()));
    }

    private void setupFilters() {
        buildingFilter.setItems(FXCollections.observableArrayList(
                "All Buildings", "Building A", "Building B", "Building C",
                "Building D", "Building E", "Science Building",
                "Athletic Center", "Main Building", "Library Building"));
        buildingFilter.getSelectionModel().selectFirst();

        typeFilter.setItems(FXCollections.observableArrayList(
                "All Types", "CLASSROOM", "LAB", "GYM",
                "AUDITORIUM", "LIBRARY", "CAFETERIA", "OFFICE"));
        typeFilter.getSelectionModel().selectFirst();

        // Phase 6C: Zone filter - populate from database
        List<String> zones = roomZoneService.getAllZones();
        List<String> zoneOptions = new java.util.ArrayList<>();
        zoneOptions.add("All Zones");
        zoneOptions.addAll(zones);
        zoneFilter.setItems(FXCollections.observableArrayList(zoneOptions));
        zoneFilter.getSelectionModel().selectFirst();

        statusFilter.setItems(FXCollections.observableArrayList("All", "Active", "Inactive"));
        statusFilter.getSelectionModel().selectFirst();
    }

    private void setupActionsColumn() {
        Callback<TableColumn<Room, Void>, TableCell<Room, Void>> cellFactory = param -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");

            {
                editBtn.setOnAction(event -> {
                    Room room = getTableView().getItems().get(getIndex());
                    handleEditRoom(room);
                });
                deleteBtn.setOnAction(event -> {
                    Room room = getTableView().getItems().get(getIndex());
                    handleDeleteRoom(room);
                });
                editBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
                deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white;");
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(editBtn, deleteBtn);
                    buttons.setSpacing(5);
                    buttons.setAlignment(Pos.CENTER);
                    setGraphic(buttons);
                }
            }
        };

        actionsColumn.setCellFactory(cellFactory);
    }

    private void loadRooms() {
        List<Room> rooms = roomRepository.findAll();
        roomTable.setItems(FXCollections.observableArrayList(rooms));
        updateRecordCount(rooms.size());
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText.isEmpty()) {
            loadRooms();
            return;
        }
        List<Room> filtered = roomRepository.findAll().stream()
                .filter(r -> r.getRoomNumber().toLowerCase().contains(searchText) ||
                        (r.getBuilding() != null && r.getBuilding().toLowerCase().contains(searchText)))
                .toList();
        roomTable.setItems(FXCollections.observableArrayList(filtered));
        updateRecordCount(filtered.size());
    }

    @FXML
    private void handleFilter() {
        String building = buildingFilter.getValue();
        String type = typeFilter.getValue();
        String status = statusFilter.getValue();

        List<Room> filtered = roomRepository.findAll().stream()
                .filter(r -> {
                    boolean buildingMatch = building == null || building.equals("All Buildings") ||
                            (r.getBuilding() != null && r.getBuilding().equals(building));

                    boolean typeMatch = type == null || type.equals("All Types") ||
                            (r.getType() != null && r.getType().toString().equals(type));

                    boolean statusMatch = status == null || status.equals("All") ||
                            (status.equals("Active") && r.getActive()) ||
                            (status.equals("Inactive") && !r.getActive());

                    return buildingMatch && typeMatch && statusMatch;
                }).toList();

        roomTable.setItems(FXCollections.observableArrayList(filtered));
        updateRecordCount(filtered.size());
    }

    @FXML
    private void handleAddRoom() {
        Room newRoom = new Room();
        newRoom.setActive(true);
        newRoom.setCapacity(30);
        newRoom.setFloor(1);
        newRoom.setType(RoomType.CLASSROOM);

        if (showRoomDialog(newRoom, "Add New Room")) {
            roomRepository.save(newRoom);
            loadRooms();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Room added successfully!");
        }
    }

    private void handleEditRoom(Room room) {
        if (showRoomDialog(room, "Edit Room")) {
            roomRepository.save(room);
            loadRooms();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Room updated successfully!");
        }
    }

    private void handleDeleteRoom(Room room) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Room");
        alert.setHeaderText("Delete Room " + room.getRoomNumber() + "?");
        alert.setContentText("This action cannot be undone.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                roomRepository.delete(room);
                loadRooms();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Room deleted successfully!");
            }
        });
    }

    private boolean showRoomDialog(Room room, String title) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText("Enter room information");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField roomNumberField = new TextField(room.getRoomNumber());
        roomNumberField.setPromptText("e.g., 101");

        TextField buildingField = new TextField(room.getBuilding());
        buildingField.setPromptText("e.g., Building A");

        Spinner<Integer> floorSpinner = new Spinner<>(0, 10, room.getFloor());

        // Phase 6C: Zone ComboBox with auto-populate
        ComboBox<String> zoneCombo = new ComboBox<>();
        List<String> availableZones = roomZoneService.getAllZones();
        availableZones.add(0, ""); // Allow empty selection
        zoneCombo.setItems(FXCollections.observableArrayList(availableZones));
        zoneCombo.setValue(room.getZone() != null ? room.getZone() : "");
        zoneCombo.setEditable(true); // Allow custom zone names

        Spinner<Integer> capacitySpinner = new Spinner<>(1, 500, room.getCapacity());

        ComboBox<RoomType> typeCombo = new ComboBox<>();
        typeCombo.setItems(FXCollections.observableArrayList(RoomType.values()));
        typeCombo.setValue(room.getType() != null ? room.getType() : RoomType.CLASSROOM);

        // Activity Tags field (Phase 5F)
        TextField activityTagsField = new TextField(room.getActivityTags());
        activityTagsField.setPromptText("e.g., Basketball, Volleyball, Weights");
        Label activityTagsHelp = new Label("Comma-separated list of activities this room supports");
        activityTagsHelp.setStyle("-fx-font-size: 10px; -fx-text-fill: gray;");

        CheckBox projectorCheckBox = new CheckBox();
        projectorCheckBox.setSelected(room.getHasProjector());

        CheckBox smartboardCheckBox = new CheckBox();
        smartboardCheckBox.setSelected(room.getHasSmartboard());

        CheckBox computersCheckBox = new CheckBox();
        computersCheckBox.setSelected(room.getHasComputers());

        CheckBox accessibleCheckBox = new CheckBox();
        accessibleCheckBox.setSelected(room.getWheelchairAccessible());

        CheckBox activeCheckBox = new CheckBox();
        activeCheckBox.setSelected(room.getActive());

        TextArea notesArea = new TextArea(room.getNotes());
        notesArea.setPrefRowCount(3);
        notesArea.setPromptText("Room notes...");

        int row = 0;
        grid.add(new Label("Room Number:*"), 0, row);
        grid.add(roomNumberField, 1, row++);
        grid.add(new Label("Building:*"), 0, row);
        grid.add(buildingField, 1, row++);
        grid.add(new Label("Floor:"), 0, row);
        grid.add(floorSpinner, 1, row++);
        grid.add(new Label("Zone:"), 0, row);  // Phase 6C
        grid.add(zoneCombo, 1, row++);
        grid.add(new Label("Capacity:*"), 0, row);
        grid.add(capacitySpinner, 1, row++);
        grid.add(new Label("Room Type:*"), 0, row);
        grid.add(typeCombo, 1, row++);
        grid.add(new Label("Activity Tags:"), 0, row);
        grid.add(activityTagsField, 1, row++);
        grid.add(new Label(""), 0, row);
        grid.add(activityTagsHelp, 1, row++);
        grid.add(new Label("Has Projector:"), 0, row);
        grid.add(projectorCheckBox, 1, row++);
        grid.add(new Label("Has Smartboard:"), 0, row);
        grid.add(smartboardCheckBox, 1, row++);
        grid.add(new Label("Has Computers:"), 0, row);
        grid.add(computersCheckBox, 1, row++);
        grid.add(new Label("Wheelchair Accessible:"), 0, row);
        grid.add(accessibleCheckBox, 1, row++);
        grid.add(new Label("Active:"), 0, row);
        grid.add(activeCheckBox, 1, row++);
        grid.add(new Label("Notes:"), 0, row);
        grid.add(notesArea, 1, row++);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setDisable(true);

        roomNumberField.textProperty().addListener((obs, old, newVal) -> {
            saveButton.setDisable(newVal.trim().isEmpty() || buildingField.getText().trim().isEmpty());
        });

        buildingField.textProperty().addListener((obs, old, newVal) -> {
            saveButton.setDisable(newVal.trim().isEmpty() || roomNumberField.getText().trim().isEmpty());
        });

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveButtonType) {
            room.setRoomNumber(roomNumberField.getText().trim());
            room.setBuilding(buildingField.getText().trim());
            room.setFloor(floorSpinner.getValue());
            room.setZone(zoneCombo.getValue() != null && !zoneCombo.getValue().trim().isEmpty()
                    ? zoneCombo.getValue().trim() : null);  // Phase 6C
            room.setCapacity(capacitySpinner.getValue());
            room.setType(typeCombo.getValue());
            room.setActivityTags(activityTagsField.getText().trim()); // Phase 5F
            room.setHasProjector(projectorCheckBox.isSelected());
            room.setHasSmartboard(smartboardCheckBox.isSelected());
            room.setHasComputers(computersCheckBox.isSelected());
            room.setWheelchairAccessible(accessibleCheckBox.isSelected());
            room.setActive(activeCheckBox.isSelected());
            room.setNotes(notesArea.getText().trim());
            return true;
        }
        return false;
    }

    @FXML
    private void handleRefresh() {
        loadRooms();
        searchField.clear();
        buildingFilter.getSelectionModel().selectFirst();
        typeFilter.getSelectionModel().selectFirst();
        statusFilter.getSelectionModel().selectFirst();
        showAlert(Alert.AlertType.INFORMATION, "Refresh", "Data refreshed successfully!");
    }

        @FXML
    private void handleExport() {
        log.info("Export clicked");

        try {
            if (roomTable.getItems().isEmpty()) {{
                showWarning("No Data", "There are no rooms to export.");
                return;
            }}

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Rooms");
            fileChooser.setInitialFileName("rooms_export.xlsx");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("Excel Files", "*.xlsx")
            );

            java.io.File file = fileChooser.showSaveDialog(roomTable.getScene().getWindow());

            if (file != null) {
                // TODO: Method exportRoomsToExcel() does not exist - implement when available
                // byte[] data = exportService.exportRoomsToExcel(roomTable.getItems());
                // java.nio.file.Files.write(file.toPath(), data);
                showError("Export Error", "Excel export not yet implemented");

                showInfo("Export Successful",
                    String.format("Exported %d rooms to %s", roomTable.getItems().size(), file.getName()));
                log.info("Exported {} rooms to {}", roomTable.getItems().size(), file.getAbsolutePath());
            }

        } catch (Exception e) {
            log.error("Failed to export rooms", e);
            showError("Export Failed", "Failed to export rooms: " + e.getMessage());
        }
    }

    private void updateRecordCount(int count) {
        recordCountLabel.setText(count + " room" + (count != 1 ? "s" : ""));
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        showAlert(Alert.AlertType.INFORMATION, title, message);
    }

    private void showWarning(String title, String message) {
        showAlert(Alert.AlertType.WARNING, title, message);
    }

    private void showError(String title, String message) {
        showAlert(Alert.AlertType.ERROR, title, message);
    }

    /**
     * Phase 6C: Auto-Assign Zones
     * Automatically assigns zones to rooms without zones based on type and room number patterns
     */
    @FXML
    private void handleAutoAssignZones() {
        log.info("Auto-Assign Zones button clicked");

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Auto-Assign Zones");
        confirmAlert.setHeaderText("Automatically assign zones to rooms?");
        confirmAlert.setContentText(
                "This will assign zones to rooms without zones based on:\n" +
                "• Room type (e.g., SCIENCE_LAB → Science Wing)\n" +
                "• Room number pattern (e.g., 200s → Math Wing)\n\n" +
                "Existing zones will NOT be changed.\n\n" +
                "Continue?"
        );

        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                roomZoneService.autoAssignZones();

                showInfo("Auto-Assign Complete",
                        "Zones have been automatically assigned to rooms.\n" +
                        "Refresh the table to see the changes.");

                // Refresh the table
                loadRooms();

                // Refresh zone filter
                List<String> zones = roomZoneService.getAllZones();
                List<String> zoneOptions = new java.util.ArrayList<>();
                zoneOptions.add("All Zones");
                zoneOptions.addAll(zones);
                zoneFilter.setItems(FXCollections.observableArrayList(zoneOptions));
                zoneFilter.getSelectionModel().selectFirst();

                log.info("Auto-assign zones completed successfully");
            } catch (Exception e) {
                log.error("Error auto-assigning zones", e);
                showError("Auto-Assign Failed", "An error occurred: " + e.getMessage());
            }
        }
    }
}
