package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.enums.RoomType;
import com.heronix.scheduler.repository.RoomRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Rooms Controller - Complete Room Management
 * Location: src/main/java/com/eduscheduler/ui/controller/RoomsController.java
 * 
 * Features:
 * - Full CRUD operations for rooms
 * - Search and filter by building, type, availability
 * - Real-time room utilization tracking
 * - Equipment and accessibility management
 * - Async data loading for responsiveness
 */
@Slf4j
@Controller
public class RoomsController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private com.heronix.scheduler.service.ExportService exportService;

    @Autowired
    private com.heronix.scheduler.service.DistrictSettingsService districtSettingsService;

    @Autowired
    private com.heronix.scheduler.service.data.SISDataService sisDataService;

    // ========== FXML UI COMPONENTS ==========
    
    @FXML
    private TextField searchField;
    
    @FXML
    private ComboBox<String> buildingFilter;
    
    @FXML
    private ComboBox<String> typeFilter;
    
    @FXML
    private ComboBox<String> availabilityFilter;
    
    @FXML
    private TableView<Room> roomsTable;
    
    @FXML
    private TableColumn<Room, String> numberColumn;
    
    @FXML
    private TableColumn<Room, String> buildingColumn;
    
    @FXML
    private TableColumn<Room, String> typeColumn;

    @FXML
    private TableColumn<Room, String> teacherColumn;

    @FXML
    private TableColumn<Room, String> capacityColumn;
    
    @FXML
    private TableColumn<Room, String> floorColumn;
    
    @FXML
    private TableColumn<Room, String> equipmentColumn;
    
    @FXML
    private TableColumn<Room, String> accessibleColumn;
    
    @FXML
    private TableColumn<Room, String> availableColumn;
    
    @FXML
    private TableColumn<Room, Void> actionsColumn;
    
    @FXML
    private Label recordCountLabel;

    // ========== DATA ==========
    
    private ObservableList<Room> roomsList = FXCollections.observableArrayList();

    // ========== INITIALIZATION ==========

    /**
     * Initialize controller - called automatically by JavaFX
     */
    @FXML
    public void initialize() {
        log.info("Initializing RoomsController");
        setupTableColumns();
        setupFilters();
        setupActionsColumn();
        loadRooms();
    }

    // ========== TABLE SETUP ==========

    /**
     * Configure table columns with cell value factories and inline editing
     */
    private void setupTableColumns() {
        // Room Number - Required field, always present
        numberColumn.setCellValueFactory(data ->
            new SimpleStringProperty(data.getValue().getRoomNumber()));

        // Building - May be null
        buildingColumn.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getBuilding() != null
                    ? data.getValue().getBuilding()
                    : "N/A"));

        // Room Type - Default to CLASSROOM if null
        typeColumn.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getRoomType() != null
                    ? data.getValue().getRoomType().toString()
                    : "CLASSROOM"));

        // Assigned Teacher - Display current teacher name or "Unassigned"
        teacherColumn.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getCurrentTeacher() != null
                    ? data.getValue().getCurrentTeacher().getName()
                    : "Unassigned"));

        // Capacity - EDITABLE TextField for quick data entry
        capacityColumn.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getCapacity() != null
                    ? String.valueOf(data.getValue().getCapacity())
                    : "0"));
        capacityColumn.setCellFactory(col -> new TableCell<Room, String>() {
            private final TextField textField = new TextField();

            {
                textField.setOnAction(e -> {
                    try {
                        int capacity = Integer.parseInt(textField.getText());
                        Room room = getTableRow().getItem();
                        if (room != null) {
                            room.setCapacity(capacity);
                            roomRepository.save(room);
                            commitEdit(textField.getText());
                            log.info("Updated capacity for room {} to {}", room.getRoomNumber(), capacity);
                        }
                    } catch (NumberFormatException ex) {
                        textField.setText(getItem());
                    }
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        textField.fireEvent(new javafx.event.ActionEvent());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    textField.setText(item);
                    textField.setStyle("-fx-max-width: 60px;");
                    setGraphic(textField);
                }
            }
        });

        // Floor - May be null
        floorColumn.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getFloor() != null
                    ? String.valueOf(data.getValue().getFloor())
                    : "N/A"));

        // Equipment - EDITABLE TextField for quick data entry
        equipmentColumn.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().getEquipment() != null
                    ? data.getValue().getEquipment()
                    : "None"));
        equipmentColumn.setCellFactory(col -> new TableCell<Room, String>() {
            private final TextField textField = new TextField();

            {
                textField.setOnAction(e -> {
                    Room room = getTableRow().getItem();
                    if (room != null) {
                        room.setEquipment(textField.getText());
                        roomRepository.save(room);
                        commitEdit(textField.getText());
                        log.info("Updated equipment for room {} to '{}'", room.getRoomNumber(), textField.getText());
                    }
                });

                textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused) {
                        textField.fireEvent(new javafx.event.ActionEvent());
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    textField.setText(item);
                    textField.setPromptText("e.g., Projector, Whiteboard");
                    setGraphic(textField);
                }
            }
        });

        // Accessibility - EDITABLE ComboBox for quick Yes/No selection
        accessibleColumn.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().isAccessible() ? "✓ Yes" : "✗ No"));
        accessibleColumn.setCellFactory(col -> new TableCell<Room, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>();

            {
                comboBox.getItems().addAll("✓ Yes", "✗ No");
                comboBox.setOnAction(e -> {
                    Room room = getTableRow().getItem();
                    if (room != null) {
                        boolean isAccessible = comboBox.getValue().startsWith("✓");
                        room.setAccessible(isAccessible);
                        roomRepository.save(room);
                        commitEdit(comboBox.getValue());
                        log.info("Updated accessibility for room {} to {}", room.getRoomNumber(), isAccessible);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setStyle("");
                } else {
                    comboBox.setValue(item);
                    // Set green glow on the ComboBox itself for "Yes" (Accessible)
                    if (item != null && item.startsWith("✓")) {
                        comboBox.setStyle("-fx-max-width: 90px; -fx-border-color: #4CAF50; -fx-border-width: 2px; -fx-border-radius: 3px; -fx-effect: dropshadow(gaussian, rgba(76, 175, 80, 0.4), 6, 0, 0, 0);");
                    } else {
                        comboBox.setStyle("-fx-max-width: 90px;");
                    }
                    setGraphic(comboBox);
                }
            }
        });

        // Availability - EDITABLE ComboBox for quick Available/Unavailable selection
        availableColumn.setCellValueFactory(data ->
            new SimpleStringProperty(
                data.getValue().isAvailable() ? "✓ Yes" : "✗ No"));
        availableColumn.setCellFactory(col -> new TableCell<Room, String>() {
            private final ComboBox<String> comboBox = new ComboBox<>();

            {
                comboBox.getItems().addAll("✓ Yes", "✗ No");
                comboBox.setOnAction(e -> {
                    Room room = getTableRow().getItem();
                    if (room != null) {
                        boolean isAvailable = comboBox.getValue().startsWith("✓");
                        room.setAvailable(isAvailable);
                        roomRepository.save(room);
                        commitEdit(comboBox.getValue());
                        log.info("Updated availability for room {} to {}", room.getRoomNumber(), isAvailable);
                    }
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setStyle("");
                } else {
                    comboBox.setValue(item);
                    // Set green glow on the ComboBox itself for "Yes" (Available)
                    if (item != null && item.startsWith("✓")) {
                        comboBox.setStyle("-fx-max-width: 90px; -fx-border-color: #4CAF50; -fx-border-width: 2px; -fx-border-radius: 3px; -fx-effect: dropshadow(gaussian, rgba(76, 175, 80, 0.4), 6, 0, 0, 0);");
                    } else {
                        comboBox.setStyle("-fx-max-width: 90px;");
                    }
                    setGraphic(comboBox);
                }
            }
        });
    }

    /**
     * Setup filter dropdown options
     */
    private void setupFilters() {
        // Building filter options
        buildingFilter.getItems().addAll(
            "All", 
            "Main Building", 
            "North Wing",
            "South Wing", 
            "Science Building", 
            "Arts Building", 
            "Gymnasium"
        );
        buildingFilter.setValue("All");

        // Room type filter options
        typeFilter.getItems().addAll(
            "All", 
            "CLASSROOM", 
            "LAB", 
            "COMPUTER_LAB",
            "GYMNASIUM", 
            "AUDITORIUM", 
            "LIBRARY", 
            "CONFERENCE_ROOM"
        );
        typeFilter.setValue("All");

        // Availability filter options
        availabilityFilter.getItems().addAll("All", "Available", "Unavailable");
        availabilityFilter.setValue("All");
    }

    /**
     * Setup actions column with View, Edit, Delete buttons
     */
    private void setupActionsColumn() {
        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("👁️ View");
            private final Button editBtn = new Button("✏️ Edit");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(5, viewBtn, editBtn, deleteBtn);

            {
                pane.setAlignment(Pos.CENTER);
                
                // Button actions
                viewBtn.setOnAction(e -> handleView(getTableRow().getItem()));
                editBtn.setOnAction(e -> handleEdit(getTableRow().getItem()));
                deleteBtn.setOnAction(e -> handleDelete(getTableRow().getItem()));
                
                // Style delete button as red
                deleteBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    // ========== DATA LOADING ==========

    /**
     * Load all rooms from database asynchronously
     */
    private void loadRooms() {
        CompletableFuture.runAsync(() -> {
            try {
                // ✅ FIXED: Use findAllWithTeacher() to eagerly load teacher relationship
                // Prevents LazyInitializationException when displaying teacher names in table
                List<Room> rooms = roomRepository.findAllWithTeacher();
                
                Platform.runLater(() -> {
                    roomsList.clear();
                    roomsList.addAll(rooms);
                    roomsTable.setItems(roomsList);
                    updateRecordCount();
                    log.info("Loaded {} rooms", rooms.size());
                });
            } catch (Exception e) {
                log.error("Error loading rooms", e);
                Platform.runLater(() -> 
                    showError("Load Error", "Failed to load rooms: " + e.getMessage()));
            }
        });
    }

    // ========== SEARCH & FILTER HANDLERS ==========

    /**
     * Handle search by room number or building
     */
    @FXML
    private void handleSearch() {
        String query = searchField.getText().toLowerCase().trim();
        
        if (query.isEmpty()) {
            loadRooms();
            return;
        }

        CompletableFuture.runAsync(() -> {
            // ✅ FIXED: Use findAllWithTeacher() to prevent LazyInitializationException
            List<Room> filtered = roomRepository.findAllWithTeacher().stream()
                .filter(r ->
                    r.getRoomNumber().toLowerCase().contains(query) ||
                    (r.getBuilding() != null && r.getBuilding().toLowerCase().contains(query))
                )
                .toList();

            Platform.runLater(() -> {
                roomsList.clear();
                roomsList.addAll(filtered);
                updateRecordCount();
            });
        });
    }

    /**
     * Handle filter by building, type, and availability
     */
    @FXML
    private void handleFilter() {
        String building = buildingFilter.getValue();
        String type = typeFilter.getValue();
        String availability = availabilityFilter.getValue();

        CompletableFuture.runAsync(() -> {
            // ✅ FIXED: Use findAllWithTeacher() to prevent LazyInitializationException
            List<Room> filtered = roomRepository.findAllWithTeacher().stream()
                // Filter by building
                .filter(r -> 
                    "All".equals(building) || 
                    (r.getBuilding() != null && r.getBuilding().equals(building))
                )
                // Filter by room type
                .filter(r -> 
                    "All".equals(type) || 
                    (r.getRoomType() != null && r.getRoomType().toString().equals(type))
                )
                // Filter by availability
                .filter(r -> 
                    "All".equals(availability) ||
                    ("Available".equals(availability) && r.isAvailable()) ||
                    ("Unavailable".equals(availability) && !r.isAvailable())
                )
                .toList();

            Platform.runLater(() -> {
                roomsList.clear();
                roomsList.addAll(filtered);
                updateRecordCount();
            });
        });
    }

    /**
     * Clear all filters and search, reload all rooms
     */
    @FXML
    private void handleClearFilters() {
        searchField.clear();
        buildingFilter.setValue("All");
        typeFilter.setValue("All");
        availabilityFilter.setValue("All");
        loadRooms();
    }

    // ========== CRUD OPERATIONS ==========

    /**
     * Handle Add Room button - opens dialog for new room
     */
    @FXML
    private void handleAddRoom() {
        log.info("Opening Add Room dialog");

        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Add New Room");
        dialog.setHeaderText("Enter room details");

        // Dialog buttons
        ButtonType addBtn = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        // Create and set form
        GridPane grid = createRoomForm(null);
        dialog.getDialogPane().setContent(grid);

        // Result converter
        dialog.setResultConverter(btn -> {
            if (btn == addBtn) {
                return extractRoomFromForm(grid, null);
            }
            return null;
        });

        // Show dialog and save if OK clicked
        Optional<Room> result = dialog.showAndWait();
        result.ifPresent(room -> {
            try {
                roomRepository.save(room);
                loadRooms();
                log.info("Room added: {}", room.getRoomNumber());
                showInfo("Success", "Room added successfully!");
            } catch (Exception e) {
                log.error("Error adding room", e);
                showError("Add Error", "Failed to add room: " + e.getMessage());
            }
        });
    }

    /**
     * Handle View button - shows room details in read-only dialog
     */
    private void handleView(Room room) {
        if (room == null) return;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Room Details");
        alert.setHeaderText("Room " + room.getRoomNumber());

        String content = String.format("""
                Building: %s
                Floor: %s
                Type: %s
                Capacity: %d

                Equipment:
                %s

                Accessible: %s
                Available: %s

                Notes:
                %s
                """,
                room.getBuilding() != null ? room.getBuilding() : "N/A",
                room.getFloor() != null ? room.getFloor() : "N/A",
                room.getRoomType() != null ? room.getRoomType() : "CLASSROOM",
                room.getCapacity() != null ? room.getCapacity() : 0,
                room.getEquipment() != null ? room.getEquipment() : "None",
                room.isAccessible() ? "Yes" : "No",
                room.isAvailable() ? "Yes" : "No",
                room.getNotes() != null ? room.getNotes() : "No notes");

        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Handle Edit button - opens dialog to edit existing room
     */
    private void handleEdit(Room room) {
        if (room == null) return;

        Dialog<Room> dialog = new Dialog<>();
        dialog.setTitle("Edit Room");
        dialog.setHeaderText("Edit Room " + room.getRoomNumber());

        // Dialog buttons
        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        // Create and set form with existing data
        GridPane grid = createRoomForm(room);
        dialog.getDialogPane().setContent(grid);

        // Result converter
        dialog.setResultConverter(btn -> {
            if (btn == saveBtn) {
                return extractRoomFromForm(grid, room);
            }
            return null;
        });

        // Show dialog and save if OK clicked
        Optional<Room> result = dialog.showAndWait();
        result.ifPresent(updated -> {
            try {
                roomRepository.save(updated);
                loadRooms();
                log.info("Room updated: {}", updated.getRoomNumber());
                showInfo("Success", "Room updated successfully!");
            } catch (Exception e) {
                log.error("Error updating room", e);
                showError("Update Error", "Failed to update room: " + e.getMessage());
            }
        });
    }

    /**
     * Handle Delete button - confirms and deletes room
     */
    private void handleDelete(Room room) {
        if (room == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Room");
        confirm.setHeaderText("Delete Room " + room.getRoomNumber() + "?");
        confirm.setContentText("This action cannot be undone.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    roomRepository.delete(room);
                    loadRooms();
                    log.info("Room deleted: {}", room.getRoomNumber());
                    showInfo("Success", "Room deleted successfully!");
                } catch (Exception e) {
                    log.error("Error deleting room", e);
                    showError("Delete Error", "Failed to delete room: " + e.getMessage());
                }
            }
        });
    }

    // ========== FORM HELPERS ==========

    /**
     * Create form grid for adding/editing room
     * @param room Existing room (for edit) or null (for add)
     * @return GridPane with form fields
     */
    private GridPane createRoomForm(Room room) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        // Create form fields with existing data or defaults
        TextField numberField = new TextField(
            room != null ? room.getRoomNumber() : "");
        
        TextField buildingField = new TextField(
            room != null ? room.getBuilding() : "");
        
        Spinner<Integer> floorSpinner = new Spinner<>(0, 20,
            room != null && room.getFloor() != null ? room.getFloor() : 1);
        
        ComboBox<RoomType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(RoomType.values());
        typeCombo.setValue(room != null ? room.getRoomType() : RoomType.CLASSROOM);
        
        Spinner<Integer> capacitySpinner = new Spinner<>(1, 500,
            room != null && room.getCapacity() != null ? room.getCapacity() : 30);

        // Min Capacity - minimum students required for efficient use
        Spinner<Integer> minCapacitySpinner = new Spinner<>(0, 500,
            room != null && room.getMinCapacity() != null ? room.getMinCapacity() : 0);
        minCapacitySpinner.setEditable(true);

        // Max Capacity - hard limit for safety/fire code
        Spinner<Integer> maxCapacitySpinner = new Spinner<>(1, 500,
            room != null && room.getMaxCapacity() != null ? room.getMaxCapacity() : 35);
        maxCapacitySpinner.setEditable(true);

        TextField equipmentField = new TextField(
            room != null ? room.getEquipment() : "");

        // Phone Number field - supports extension OR full number
        TextField phoneField = new TextField(
            room != null ? room.getPhoneNumber() : "");
        phoneField.setPromptText("(352) 754-4101 or 4101");

        CheckBox accessibleCheck = new CheckBox();
        accessibleCheck.setSelected(room != null && room.isAccessible());

        CheckBox availableCheck = new CheckBox();
        availableCheck.setSelected(room == null || room.isAvailable());

        TextArea notesArea = new TextArea(
            room != null ? room.getNotes() : "");
        notesArea.setPrefRowCount(3);

        // Teacher Assignment ComboBox - Load all active teachers
        ComboBox<com.heronix.scheduler.model.domain.Teacher> teacherCombo = new ComboBox<>();
        teacherCombo.setPromptText("Unassigned");

        // Load teachers from database
        try {
            List<com.heronix.scheduler.model.domain.Teacher> teachers = sisDataService.getAllTeachers();
            teacherCombo.getItems().add(null); // Add "Unassigned" option
            teacherCombo.getItems().addAll(teachers);

            // Set current teacher if editing
            if (room != null && room.getCurrentTeacher() != null) {
                teacherCombo.setValue(room.getCurrentTeacher());
            }

            // Display teacher name in dropdown
            teacherCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(com.heronix.scheduler.model.domain.Teacher teacher, boolean empty) {
                    super.updateItem(teacher, empty);
                    if (empty || teacher == null) {
                        setText("Unassigned");
                    } else {
                        setText(teacher.getName());
                    }
                }
            });

            teacherCombo.setCellFactory(param -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(com.heronix.scheduler.model.domain.Teacher teacher, boolean empty) {
                    super.updateItem(teacher, empty);
                    if (empty || teacher == null) {
                        setText("Unassigned");
                    } else {
                        setText(teacher.getName());
                    }
                }
            });
        } catch (Exception e) {
            log.error("Failed to load teachers for room assignment", e);
        }

        // ========================================================================
        // AUTO-GENERATION SETUP FOR PHONE NUMBER
        // ========================================================================

        boolean isEdit = (room != null);

        // Auto-generate phone number when room number is entered (for new rooms only)
        if (!isEdit && districtSettingsService != null) {
            numberField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.trim().isEmpty()) {
                    try {
                        // TODO: Method generateRoomPhoneNumber() does not exist
                        String generatedPhone = null; // districtSettingsService.generateRoomPhoneNumber(newVal);
                        if (generatedPhone != null && !generatedPhone.isEmpty()) {
                            phoneField.setText(generatedPhone);
                            phoneField.setStyle("-fx-text-fill: #2196F3;"); // Blue = auto-generated
                        }
                    } catch (Exception e) {
                        log.error("Failed to generate phone number", e);
                    }
                }
            });

            // Reset text color when user manually edits phone
            phoneField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals(oldVal) && oldVal != null && !oldVal.isEmpty()) {
                    phoneField.setStyle(""); // User edited - normal color
                }
            });
        }

        // Add fields to grid
        grid.add(new Label("Room Number:"), 0, 0);
        grid.add(numberField, 1, 0);

        grid.add(new Label("Building:"), 0, 1);
        grid.add(buildingField, 1, 1);

        grid.add(new Label("Floor:"), 0, 2);
        grid.add(floorSpinner, 1, 2);

        grid.add(new Label("Type:"), 0, 3);
        grid.add(typeCombo, 1, 3);

        grid.add(new Label("Standard Capacity:"), 0, 4);
        grid.add(capacitySpinner, 1, 4);

        grid.add(new Label("Min Capacity:"), 0, 5);
        grid.add(minCapacitySpinner, 1, 5);

        grid.add(new Label("Max Capacity:"), 0, 6);
        grid.add(maxCapacitySpinner, 1, 6);

        grid.add(new Label("Equipment:"), 0, 7);
        grid.add(equipmentField, 1, 7);

        grid.add(new Label("Phone Number:"), 0, 8);
        grid.add(phoneField, 1, 8);

        grid.add(new Label("Accessible:"), 0, 9);
        grid.add(accessibleCheck, 1, 9);

        grid.add(new Label("Available:"), 0, 10);
        grid.add(availableCheck, 1, 10);

        grid.add(new Label("Assigned Teacher:"), 0, 11);
        grid.add(teacherCombo, 1, 11);

        grid.add(new Label("Notes:"), 0, 12);
        grid.add(notesArea, 1, 12);

        return grid;
    }

    /**
     * Extract room data from form grid
     * @param grid Form grid with input fields
     * @param existing Existing room to update, or null for new room
     * @return Room object with form data
     */
    @SuppressWarnings("unchecked")
    private Room extractRoomFromForm(GridPane grid, Room existing) {
        Room room = existing != null ? existing : new Room();

        // Extract values from form fields in order they were added
        room.setRoomNumber(((TextField) grid.getChildren().get(1)).getText());
        room.setBuilding(((TextField) grid.getChildren().get(3)).getText());
        room.setFloor(((Spinner<Integer>) grid.getChildren().get(5)).getValue());
        room.setRoomType(((ComboBox<RoomType>) grid.getChildren().get(7)).getValue());
        room.setCapacity(((Spinner<Integer>) grid.getChildren().get(9)).getValue());
        room.setMinCapacity(((Spinner<Integer>) grid.getChildren().get(11)).getValue());
        room.setMaxCapacity(((Spinner<Integer>) grid.getChildren().get(13)).getValue());
        room.setEquipment(((TextField) grid.getChildren().get(15)).getText());
        room.setPhoneNumber(((TextField) grid.getChildren().get(17)).getText()); // Phone field added
        room.setAccessible(((CheckBox) grid.getChildren().get(19)).isSelected());
        room.setAvailable(((CheckBox) grid.getChildren().get(21)).isSelected());
        room.setCurrentTeacher(((ComboBox<com.heronix.scheduler.model.domain.Teacher>) grid.getChildren().get(23)).getValue()); // Teacher assignment
        room.setNotes(((TextArea) grid.getChildren().get(25)).getText());

        return room;
    }

    // ========== TOOLBAR ACTIONS ==========

    /**
     * Handle Refresh button - reload all rooms
     */
    @FXML
    private void handleRefresh() {
        log.info("Refreshing room data");
        loadRooms();
    }

    /**
     * Handle Export button - export rooms to file
     */
        @FXML
    private void handleExport() {
        log.info("Export clicked");

        try {
            if (roomsList.isEmpty()) {{
                showWarning("No Data", "There are no rooms to export.");
                return;
            }}

            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Export Rooms");
            fileChooser.setInitialFileName("rooms_export.csv");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("CSV Files", "*.csv")
            );

            java.io.File file = fileChooser.showSaveDialog(roomsTable.getScene().getWindow());

            if (file != null) {
                // TODO: Method exportRoomsToCSV() does not exist - implement when available
                // byte[] data = exportService.exportRoomsToCSV(roomsList);
                // java.nio.file.Files.write(file.toPath(), data);
                showError("Export Error", "CSV export not yet implemented");

                showInfo("Export Successful",
                    String.format("Exported %d rooms to %s", roomsList.size(), file.getName()));
                log.info("Exported {} rooms to {}", roomsList.size(), file.getAbsolutePath());
            }

        } catch (Exception e) {
            log.error("Failed to export rooms", e);
            showError("Export Failed", "Failed to export rooms: " + e.getMessage());
        }
    }

    /**
     * Handle Utilization button - show room usage statistics
     */
    @FXML
    private void handleUtilization() {
        log.info("Calculating room utilization");

        long total = roomsList.size();
        long available = roomsList.stream().filter(Room::isAvailable).count();
        long accessible = roomsList.stream().filter(Room::isAccessible).count();

        showInfo("Room Utilization Statistics",
            String.format("""
                Total Rooms: %d
                Available: %d
                In Use: %d
                Accessible: %d
                
                Utilization Rate: %.1f%%
                """,
                total, 
                available, 
                total - available, 
                accessible,
                total > 0 ? ((total - available) * 100.0 / total) : 0.0));
    }

    // ========== UTILITY METHODS ==========

    /**
     * Update record count label
     */
    private void updateRecordCount() {
        recordCountLabel.setText("Total: " + roomsList.size());
    }

    /**
     * Show error alert
     */
    private void showError(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Show info alert
     */
    private void showInfo(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    private void showWarning(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}