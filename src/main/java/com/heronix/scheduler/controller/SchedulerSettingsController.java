package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.SchedulerConfiguration;
import com.heronix.scheduler.model.domain.SpecialCondition;
import com.heronix.scheduler.repository.SchedulerConfigurationRepository;
import com.heronix.scheduler.repository.SpecialConditionRepository;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.value.ChangeListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SchedulerSettingsController {

    private final SchedulerConfigurationRepository configRepository;
    private final SpecialConditionRepository conditionRepository;

    // Configuration Selector
    @FXML private ComboBox<SchedulerConfiguration> configComboBox;

    // Solver Settings
    @FXML private Spinner<Integer> maxSolverMinutesSpinner;
    @FXML private Spinner<Integer> unimprovedSecondsSpinner;
    @FXML private CheckBox enableMultithreadingCheck;

    // Teacher Constraints
    @FXML private Spinner<Integer> minPeriodsTeacherSpinner;
    @FXML private Spinner<Integer> maxPeriodsTeacherSpinner;
    @FXML private Spinner<Integer> maxConsecutiveSpinner;
    @FXML private Spinner<Integer> minPlanningSpinner;
    @FXML private Spinner<Integer> maxPrepsSpinner;
    @FXML private CheckBox allowBackToBackCheck;
    @FXML private CheckBox minimizeTeacherMovesCheck;
    @FXML private CheckBox respectAvailabilityCheck;
    @FXML private CheckBox respectPreferencesCheck;

    // Student Constraints
    @FXML private Spinner<Integer> maxStudentsSpinner;
    @FXML private Spinner<Integer> minStudentsSpinner;
    @FXML private Spinner<Integer> maxBuildingTransitionsSpinner;
    @FXML private CheckBox balanceClassSizesCheck;
    @FXML private CheckBox honorStudentRequestsCheck;
    @FXML private CheckBox enforcePrerequisitesCheck;
    @FXML private CheckBox minimizeStudentMovesCheck;

    // Special Accommodations
    @FXML private CheckBox honorIepCheck;
    @FXML private CheckBox honor504Check;
    @FXML private CheckBox smallClassSpecialNeedsCheck;
    @FXML private CheckBox resourceRoomProximityCheck;

    // Time Preferences
    @FXML private CheckBox preferMorningCoreCheck;
    @FXML private CheckBox preferAfternoonElectivesCheck;
    @FXML private Spinner<Integer> passingTimeSpinner;

    // Constraint Weights
    @FXML private Slider weightTeacherConflictSlider;
    @FXML private Label weightTeacherConflictLabel;
    @FXML private Slider weightRoomConflictSlider;
    @FXML private Label weightRoomConflictLabel;
    @FXML private Slider weightCapacitySlider;
    @FXML private Label weightCapacityLabel;
    @FXML private Slider weightWorkloadSlider;
    @FXML private Label weightWorkloadLabel;
    @FXML private Slider weightQualificationSlider;
    @FXML private Label weightQualificationLabel;
    @FXML private Slider weightStudentPrefSlider;
    @FXML private Label weightStudentPrefLabel;

    // Special Conditions
    @FXML private TableView<SpecialCondition> conditionsTable;
    @FXML private TableColumn<SpecialCondition, String> conditionNameCol;
    @FXML private TableColumn<SpecialCondition, String> conditionTypeCol;
    @FXML private TableColumn<SpecialCondition, String> conditionTargetCol;
    @FXML private TableColumn<SpecialCondition, String> conditionSeverityCol;
    @FXML private TableColumn<SpecialCondition, String> conditionDescCol;
    @FXML private TableColumn<SpecialCondition, Void> conditionActionsCol;

    private SchedulerConfiguration currentConfig;

    @FXML
    public void initialize() {
        log.info("Initializing SchedulerSettingsController");

        loadConfigurations();
        setupWeightSliders();
        setupConditionsTable();
    }

    private void loadConfigurations() {
        List<SchedulerConfiguration> configs = configRepository.findAll();
        configComboBox.getItems().setAll(configs);

        configRepository.findByActiveTrue().ifPresentOrElse(
            config -> {
                configComboBox.setValue(config);
                loadConfigurationToUI(config);
            },
            this::createDefaultConfiguration
        );

        configComboBox.setConverter(new javafx.util.StringConverter<SchedulerConfiguration>() {
            @Override
            public String toString(SchedulerConfiguration config) {
                return config != null ? config.getName() : "";
            }
            @Override
            public SchedulerConfiguration fromString(String string) {
                return null;
            }
        });

        configComboBox.setOnAction(e -> {
            SchedulerConfiguration selected = configComboBox.getValue();
            if (selected != null) {
                loadConfigurationToUI(selected);
            }
        });
    }

    private void loadConfigurationToUI(SchedulerConfiguration config) {
        currentConfig = config;

        // Solver Settings
        maxSolverMinutesSpinner.getValueFactory().setValue(config.getMaxSolverMinutes());
        unimprovedSecondsSpinner.getValueFactory().setValue(config.getUnimprovedSecondsTermination());
        enableMultithreadingCheck.setSelected(config.getEnableMultithreading());

        // Teacher Constraints
        minPeriodsTeacherSpinner.getValueFactory().setValue(config.getMinPeriodsPerTeacher());
        maxPeriodsTeacherSpinner.getValueFactory().setValue(config.getMaxPeriodsPerTeacher());
        maxConsecutiveSpinner.getValueFactory().setValue(config.getMaxConsecutivePeriods());
        minPlanningSpinner.getValueFactory().setValue(config.getMinPlanningPeriods());
        maxPrepsSpinner.getValueFactory().setValue(config.getMaxPrepsPerTeacher());
        allowBackToBackCheck.setSelected(config.getAllowBackToBackClasses());
        minimizeTeacherMovesCheck.setSelected(config.getMinimizeTeacherMoves());
        respectAvailabilityCheck.setSelected(config.getRespectTeacherAvailability());
        respectPreferencesCheck.setSelected(config.getRespectTeacherPreferences());

        // Student Constraints
        maxStudentsSpinner.getValueFactory().setValue(config.getMaxStudentsPerClass());
        minStudentsSpinner.getValueFactory().setValue(config.getMinStudentsPerClass());
        maxBuildingTransitionsSpinner.getValueFactory().setValue(config.getMaxBuildingTransitions());
        balanceClassSizesCheck.setSelected(config.getBalanceClassSizes());
        honorStudentRequestsCheck.setSelected(config.getHonorStudentRequests());
        enforcePrerequisitesCheck.setSelected(config.getEnforcePrerequisiteOrder());
        minimizeStudentMovesCheck.setSelected(config.getMinimizeStudentMoves());

        // Special Accommodations
        honorIepCheck.setSelected(config.getHonorIepAccommodations());
        honor504Check.setSelected(config.getHonor504Accommodations());
        smallClassSpecialNeedsCheck.setSelected(config.getSmallClassForSpecialNeeds());
        resourceRoomProximityCheck.setSelected(config.getResourceRoomProximity());

        // Time Preferences
        preferMorningCoreCheck.setSelected(config.getPreferMorningCoreSubjects());
        preferAfternoonElectivesCheck.setSelected(config.getPreferAfternoonElectives());
        passingTimeSpinner.getValueFactory().setValue(config.getMinPassingTimeMinutes());

        // Constraint Weights
        weightTeacherConflictSlider.setValue(config.getWeightTeacherConflict());
        weightRoomConflictSlider.setValue(config.getWeightRoomConflict());
        weightCapacitySlider.setValue(config.getWeightCapacity());
        weightWorkloadSlider.setValue(config.getWeightWorkloadBalance());
        weightQualificationSlider.setValue(config.getWeightTeacherQualification());
        weightStudentPrefSlider.setValue(config.getWeightStudentPreference());
    }

    private void setupWeightSliders() {
        ChangeListener<Number> sliderListener = (obs, oldVal, newVal) -> {
            weightTeacherConflictLabel.setText(String.valueOf(weightTeacherConflictSlider.getValue()));
            weightRoomConflictLabel.setText(String.valueOf(weightRoomConflictSlider.getValue()));
            weightCapacityLabel.setText(String.valueOf(weightCapacitySlider.getValue()));
            weightWorkloadLabel.setText(String.valueOf(weightWorkloadSlider.getValue()));
            weightQualificationLabel.setText(String.valueOf(weightQualificationSlider.getValue()));
            weightStudentPrefLabel.setText(String.valueOf(weightStudentPrefSlider.getValue()));
        };

        weightTeacherConflictSlider.valueProperty().addListener(sliderListener);
        weightRoomConflictSlider.valueProperty().addListener(sliderListener);
        weightCapacitySlider.valueProperty().addListener(sliderListener);
        weightWorkloadSlider.valueProperty().addListener(sliderListener);
        weightQualificationSlider.valueProperty().addListener(sliderListener);
        weightStudentPrefSlider.valueProperty().addListener(sliderListener);
    }

    private void setupConditionsTable() {
        conditionNameCol.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleStringProperty(cd.getValue().getName()));
        conditionTypeCol.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleStringProperty(cd.getValue().getConditionType().toString()));
        conditionSeverityCol.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleStringProperty(cd.getValue().getSeverity().toString()));
        conditionDescCol.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleStringProperty(cd.getValue().getDescription()));

        loadConditions();
    }

    private void loadConditions() {
        List<SpecialCondition> conditions = conditionRepository.findByActiveTrue();
        conditionsTable.getItems().setAll(conditions);
    }

    @FXML
    private void handleNewConfig() {
        log.info("Creating new configuration");
        createDefaultConfiguration();
    }

    @FXML
    private void handleSaveConfig() {
        if (currentConfig == null) return;

        saveUIToConfiguration(currentConfig);
        configRepository.save(currentConfig);

        log.info("Configuration saved: {}", currentConfig.getName());
        showAlert("Success", "Configuration saved successfully", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleDeleteConfig() {
        if (currentConfig == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Configuration");
        confirm.setContentText("Are you sure you want to delete this configuration?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                configRepository.delete(currentConfig);
                log.info("Configuration deleted: {}", currentConfig.getName());
                loadConfigurations();
            }
        });
    }

    @FXML
    private void handleApplyChanges() {
        handleSaveConfig();
    }

    @FXML
    private void handleResetDefaults() {
        createDefaultConfiguration();
    }

    @FXML
    private void handleAddCondition() {
        log.info("Opening add condition dialog");

        // Create dialog for adding special condition
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add Special Condition");
        dialog.setHeaderText("Enter a new special scheduling condition");

        // Set button types
        ButtonType addButtonType = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addButtonType, ButtonType.CANCEL);

        // Create form
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

        TextField conditionField = new TextField();
        conditionField.setPromptText("e.g., No classes during lunch period");
        conditionField.setPrefWidth(300);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("Teacher Constraint", "Room Constraint", "Time Constraint", "Student Constraint");
        typeCombo.setValue("Teacher Constraint");

        grid.add(new Label("Condition Type:"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(conditionField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Enable/disable add button based on input
        javafx.scene.Node addButton = dialog.getDialogPane().lookupButton(addButtonType);
        addButton.setDisable(true);
        conditionField.textProperty().addListener((obs, old, newVal) -> {
            addButton.setDisable(newVal == null || newVal.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == addButtonType) {
                return typeCombo.getValue() + ": " + conditionField.getText().trim();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(condition -> {
            log.info("Added special condition: {}", condition);
            showInfo("Condition Added", "Special condition has been added:\n" + condition);
        });
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void saveUIToConfiguration(SchedulerConfiguration config) {
        // Solver Settings
        config.setMaxSolverMinutes(maxSolverMinutesSpinner.getValue());
        config.setUnimprovedSecondsTermination(unimprovedSecondsSpinner.getValue());
        config.setEnableMultithreading(enableMultithreadingCheck.isSelected());

        // Teacher Constraints
        config.setMinPeriodsPerTeacher(minPeriodsTeacherSpinner.getValue());
        config.setMaxPeriodsPerTeacher(maxPeriodsTeacherSpinner.getValue());
        config.setMaxConsecutivePeriods(maxConsecutiveSpinner.getValue());
        config.setMinPlanningPeriods(minPlanningSpinner.getValue());
        config.setMaxPrepsPerTeacher(maxPrepsSpinner.getValue());
        config.setAllowBackToBackClasses(allowBackToBackCheck.isSelected());
        config.setMinimizeTeacherMoves(minimizeTeacherMovesCheck.isSelected());
        config.setRespectTeacherAvailability(respectAvailabilityCheck.isSelected());
        config.setRespectTeacherPreferences(respectPreferencesCheck.isSelected());

        // Student Constraints
        config.setMaxStudentsPerClass(maxStudentsSpinner.getValue());
        config.setMinStudentsPerClass(minStudentsSpinner.getValue());
        config.setMaxBuildingTransitions(maxBuildingTransitionsSpinner.getValue());
        config.setBalanceClassSizes(balanceClassSizesCheck.isSelected());
        config.setHonorStudentRequests(honorStudentRequestsCheck.isSelected());
        config.setEnforcePrerequisiteOrder(enforcePrerequisitesCheck.isSelected());
        config.setMinimizeStudentMoves(minimizeStudentMovesCheck.isSelected());

        // Special Accommodations
        config.setHonorIepAccommodations(honorIepCheck.isSelected());
        config.setHonor504Accommodations(honor504Check.isSelected());
        config.setSmallClassForSpecialNeeds(smallClassSpecialNeedsCheck.isSelected());
        config.setResourceRoomProximity(resourceRoomProximityCheck.isSelected());

        // Time Preferences
        config.setPreferMorningCoreSubjects(preferMorningCoreCheck.isSelected());
        config.setPreferAfternoonElectives(preferAfternoonElectivesCheck.isSelected());
        config.setMinPassingTimeMinutes(passingTimeSpinner.getValue());

        // Constraint Weights
        config.setWeightTeacherConflict((int) weightTeacherConflictSlider.getValue());
        config.setWeightRoomConflict((int) weightRoomConflictSlider.getValue());
        config.setWeightCapacity((int) weightCapacitySlider.getValue());
        config.setWeightWorkloadBalance((int) weightWorkloadSlider.getValue());
        config.setWeightTeacherQualification((int) weightQualificationSlider.getValue());
        config.setWeightStudentPreference((int) weightStudentPrefSlider.getValue());

        config.setUpdatedAt(java.time.LocalDateTime.now());
    }

    private void createDefaultConfiguration() {
        SchedulerConfiguration defaultConfig = new SchedulerConfiguration();
        defaultConfig.setName("Default Configuration");
        defaultConfig.setDescription("Standard high school scheduling configuration");
        defaultConfig.setActive(true);

        currentConfig = configRepository.save(defaultConfig);
        loadConfigurationToUI(currentConfig);
        loadConfigurations();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
