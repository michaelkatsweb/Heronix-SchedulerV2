package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.dto.RoomPreferences;
import com.heronix.scheduler.repository.RoomRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Teacher Room Preferences Dialog
 * Phase 6B-4: Room Preferences UI
 *
 * Allows administrators to configure teacher room preferences:
 * - Preferences (SOFT) - scheduler will prefer these rooms
 * - Restrictions (HARD) - teacher can ONLY use these rooms
 * - Preference strength (LOW/MEDIUM/HIGH)
 *
 * @since Phase 6B-4 - December 3, 2025
 */
@Component
public class TeacherRoomPreferencesDialogController {

    @FXML private Label teacherNameLabel;
    @FXML private RadioButton preferenceRadioButton;
    @FXML private RadioButton restrictionRadioButton;
    @FXML private ToggleGroup preferenceTypeToggle;
    @FXML private Label strengthLabel;
    @FXML private ComboBox<String> strengthComboBox;
    @FXML private Label strengthDescriptionLabel;
    @FXML private Label selectedCountLabel;
    @FXML private TextField searchField;
    @FXML private VBox roomListContainer;
    @FXML private ComboBox<String> roomTypeFilterCombo;
    @FXML private Label summaryLabel;

    @Autowired
    private RoomRepository roomRepository;

    private Teacher teacher;
    private List<Room> allRooms;
    private Map<Long, CheckBox> roomCheckBoxes = new HashMap<>();

    /**
     * Initialize the dialog with teacher data
     *
     * @param teacher Teacher to configure preferences for
     */
    public void initializeWithTeacher(Teacher teacher) {
        this.teacher = teacher;
        this.allRooms = roomRepository.findAll();

        // Populate strength ComboBox
        strengthComboBox.getItems().addAll("LOW", "MEDIUM", "HIGH");

        // Set teacher name
        teacherNameLabel.setText(teacher.getName());

        // Load existing preferences
        loadExistingPreferences();

        // Setup listeners
        setupListeners();

        // Populate room list
        populateRoomList();

        // Update UI state
        updateUIState();
        updateSummary();
    }

    /**
     * Load existing room preferences from teacher
     */
    private void loadExistingPreferences() {
        List<Room> roomList = teacher.getRoomPreferences();

        if (roomList == null || roomList.isEmpty()) {
            // No preferences - use defaults
            preferenceRadioButton.setSelected(true);
            strengthComboBox.setValue("MEDIUM");
            return;
        }

        // Set preference type (default to preference mode)
        preferenceRadioButton.setSelected(true);

        // Set strength
        strengthComboBox.setValue("MEDIUM");
    }

    /**
     * Setup UI listeners
     */
    private void setupListeners() {
        // Preference type toggle
        preferenceTypeToggle.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            updateUIState();
            updateSummary();
        });

        // Strength combo box
        strengthComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            updateStrengthDescription();
            updateSummary();
        });

        // Search field
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filterRoomList();
        });

        // Room type filter - populate items
        roomTypeFilterCombo.getItems().addAll(
            "All Rooms",
            "Classroom",
            "Laboratory",
            "Gymnasium",
            "Library",
            "Auditorium",
            "Computer Lab",
            "Art Room",
            "Music Room"
        );
        roomTypeFilterCombo.setValue("All Rooms");
    }

    /**
     * Populate the room list with checkboxes
     */
    private void populateRoomList() {
        roomListContainer.getChildren().clear();
        roomCheckBoxes.clear();

        List<Room> existingPrefs = teacher.getRoomPreferences();
        List<Long> selectedRoomIds = new ArrayList<>();
        if (existingPrefs != null && !existingPrefs.isEmpty()) {
            for (Room room : existingPrefs) {
                if (room != null && room.getId() != null) {
                    selectedRoomIds.add(room.getId());
                }
            }
        }

        for (Room room : allRooms) {
            CheckBox checkBox = createRoomCheckBox(room);

            // Select if in existing preferences
            if (selectedRoomIds.contains(room.getId())) {
                checkBox.setSelected(true);
            }

            // Add listener for selection changes
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                updateSelectedCount();
                updateSummary();
            });

            roomCheckBoxes.put(room.getId(), checkBox);
            roomListContainer.getChildren().add(checkBox);
        }

        updateSelectedCount();
    }

    /**
     * Create a checkbox for a room
     */
    private CheckBox createRoomCheckBox(Room room) {
        CheckBox checkBox = new CheckBox();

        // Build label: "Room 101 [Classroom] (Capacity: 30)"
        StringBuilder label = new StringBuilder();
        label.append("Room ").append(room.getRoomNumber());

        if (room.getRoomType() != null) {
            label.append(" [").append(room.getRoomType()).append("]");
        }

        if (room.getCapacity() != null && room.getCapacity() > 0) {
            label.append(" (Capacity: ").append(room.getCapacity()).append(")");
        }

        checkBox.setText(label.toString());
        checkBox.setUserData(room);
        checkBox.getStyleClass().add("room-checkbox");

        return checkBox;
    }

    /**
     * Update UI state based on preference type
     */
    private void updateUIState() {
        boolean isPreference = preferenceRadioButton.isSelected();

        // Enable/disable strength controls
        strengthLabel.setDisable(!isPreference);
        strengthComboBox.setDisable(!isPreference);
        strengthDescriptionLabel.setDisable(!isPreference);

        updateStrengthDescription();
    }

    /**
     * Update strength description label
     */
    private void updateStrengthDescription() {
        if (restrictionRadioButton.isSelected()) {
            strengthDescriptionLabel.setText("(Not applicable for restrictions)");
            return;
        }

        String strength = strengthComboBox.getValue();
        if (strength == null) {
            strengthDescriptionLabel.setText("");
            return;
        }

        switch (strength) {
            case "LOW":
                strengthDescriptionLabel.setText("(1 point penalty if not met)");
                break;
            case "MEDIUM":
                strengthDescriptionLabel.setText("(3 point penalty if not met)");
                break;
            case "HIGH":
                strengthDescriptionLabel.setText("(5 point penalty if not met)");
                break;
            default:
                strengthDescriptionLabel.setText("");
        }
    }

    /**
     * Update selected count label
     */
    private void updateSelectedCount() {
        long count = roomCheckBoxes.values().stream()
                .filter(CheckBox::isSelected)
                .count();

        selectedCountLabel.setText(String.format("(%d selected)", count));
    }

    /**
     * Update summary label
     */
    private void updateSummary() {
        long count = roomCheckBoxes.values().stream()
                .filter(CheckBox::isSelected)
                .count();

        if (count == 0) {
            summaryLabel.setText("No room preferences configured - teacher can be assigned to any room");
            return;
        }

        if (restrictionRadioButton.isSelected()) {
            summaryLabel.setText(String.format(
                    "✓ HARD CONSTRAINT: Teacher is RESTRICTED to %d specific room(s). " +
                    "Scheduler will NEVER assign to other rooms.",
                    count
            ));
        } else {
            String strength = strengthComboBox.getValue();
            summaryLabel.setText(String.format(
                    "✓ SOFT CONSTRAINT: Teacher PREFERS %d room(s) with %s priority. " +
                    "Scheduler will prefer these but can use others if needed.",
                    count, strength
            ));
        }
    }

    /**
     * Filter room list based on search text and type filter
     */
    private void filterRoomList() {
        String searchText = searchField.getText().toLowerCase();
        String typeFilter = roomTypeFilterCombo.getValue();

        for (Map.Entry<Long, CheckBox> entry : roomCheckBoxes.entrySet()) {
            CheckBox checkBox = entry.getValue();
            Room room = (Room) checkBox.getUserData();

            boolean matchesSearch = searchText.isEmpty() ||
                    checkBox.getText().toLowerCase().contains(searchText);

            boolean matchesType = "All Rooms".equals(typeFilter) ||
                    (room.getRoomType() != null && room.getRoomType().equals(typeFilter));

            checkBox.setVisible(matchesSearch && matchesType);
            checkBox.setManaged(matchesSearch && matchesType);
        }
    }

    /**
     * Handle Select All button
     */
    @FXML
    private void handleSelectAll() {
        roomCheckBoxes.values().stream()
                .filter(cb -> cb.isVisible() && cb.isManaged())
                .forEach(cb -> cb.setSelected(true));
    }

    /**
     * Handle Clear All button
     */
    @FXML
    private void handleClearAll() {
        roomCheckBoxes.values().forEach(cb -> cb.setSelected(false));
    }

    /**
     * Handle filter change
     */
    @FXML
    private void handleFilterChange() {
        filterRoomList();
    }

    /**
     * Get the configured room preferences
     *
     * @return RoomPreferences object with user selections
     */
    public RoomPreferences getRoomPreferences() {
        // Get selected room IDs
        List<Long> selectedRoomIds = roomCheckBoxes.entrySet().stream()
                .filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // If no rooms selected, return null (no preferences)
        if (selectedRoomIds.isEmpty()) {
            return null;
        }

        // Build preferences object
        RoomPreferences prefs = new RoomPreferences();
        prefs.setPreferredRoomIds(selectedRoomIds);
        prefs.setRestrictedToRooms(restrictionRadioButton.isSelected());

        // Set strength (only applies to preferences, not restrictions)
        if (preferenceRadioButton.isSelected()) {
            String strengthStr = strengthComboBox.getValue();
            RoomPreferences.PreferenceStrength strength = strengthStr != null
                    ? RoomPreferences.PreferenceStrength.valueOf(strengthStr)
                    : RoomPreferences.PreferenceStrength.MEDIUM;
            prefs.setStrength(strength);
        } else {
            // Restrictions don't use strength, but set default
            prefs.setStrength(RoomPreferences.PreferenceStrength.MEDIUM);
        }

        return prefs;
    }

    /**
     * Validate the form
     *
     * @return true if valid, false otherwise
     */
    public boolean validate() {
        // For restrictions, ensure at least one room is selected
        if (restrictionRadioButton.isSelected()) {
            long count = roomCheckBoxes.values().stream()
                    .filter(CheckBox::isSelected)
                    .count();

            if (count == 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Validation Error");
                alert.setHeaderText("No Rooms Selected");
                alert.setContentText(
                        "When using room restrictions (HARD constraint), you must select at least one room.\n\n" +
                        "If you don't want any room preferences, select 'Preferences (SOFT)' mode and leave no rooms selected."
                );
                alert.showAndWait();
                return false;
            }
        }

        return true;
    }
}
