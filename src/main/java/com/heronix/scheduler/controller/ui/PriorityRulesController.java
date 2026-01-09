package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.PriorityRule;
import com.heronix.scheduler.model.enums.PriorityRuleType;
import com.heronix.scheduler.repository.PriorityRuleRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Priority Rules Controller
 *
 * Allows administrators to configure priority rules for course assignment.
 * Rules define bonus points awarded to students based on various criteria
 * (GPA, behavior, special needs, seniority, etc.)
 *
 * Features:
 * - View all priority rules
 * - Create new rules
 * - Edit existing rules
 * - Enable/disable rules
 * - Delete rules
 * - Preview rule effects
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 5 - November 20, 2025
 */
@Component
public class PriorityRulesController {

    @Autowired
    private PriorityRuleRepository priorityRuleRepository;

    // ========================================================================
    // FXML COMPONENTS
    // ========================================================================

    @FXML
    private TableView<PriorityRule> rulesTable;

    @FXML
    private TableColumn<PriorityRule, String> ruleNameColumn;

    @FXML
    private TableColumn<PriorityRule, String> ruleTypeColumn;

    @FXML
    private TableColumn<PriorityRule, Integer> weightColumn;

    @FXML
    private TableColumn<PriorityRule, Integer> bonusPointsColumn;

    @FXML
    private TableColumn<PriorityRule, String> activeColumn;

    @FXML
    private TableColumn<PriorityRule, String> criteriaColumn;

    @FXML
    private Button addButton;

    @FXML
    private Button editButton;

    @FXML
    private Button deleteButton;

    @FXML
    private Button toggleActiveButton;

    @FXML
    private Button closeButton;

    @FXML
    private Label totalBonusLabel;

    @FXML
    private Label activeRulesLabel;

    // ========================================================================
    // DATA
    // ========================================================================

    private ObservableList<PriorityRule> rules;

    // ========================================================================
    // INITIALIZATION
    // ========================================================================

    @FXML
    public void initialize() {
        setupTable();
        loadRules();
        setupListeners();
        updateStatistics();
    }

    /**
     * Setup table columns
     */
    private void setupTable() {
        ruleNameColumn.setCellValueFactory(new PropertyValueFactory<>("ruleName"));

        ruleTypeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getRuleType().getDisplayName())
        );

        weightColumn.setCellValueFactory(new PropertyValueFactory<>("weight"));

        bonusPointsColumn.setCellValueFactory(new PropertyValueFactory<>("bonusPoints"));

        activeColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(
                Boolean.TRUE.equals(cellData.getValue().getActive()) ? "✓ Active" : "✗ Inactive"
            )
        );

        criteriaColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getCriteriaExplanation())
        );

        // Enable row selection
        rulesTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    /**
     * Load rules from database
     */
    private void loadRules() {
        List<PriorityRule> allRules = priorityRuleRepository.findAll();
        rules = FXCollections.observableArrayList(allRules);
        rulesTable.setItems(rules);
    }

    /**
     * Setup event listeners
     */
    private void setupListeners() {
        // Enable/disable buttons based on selection
        rulesTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                boolean hasSelection = newSelection != null;
                editButton.setDisable(!hasSelection);
                deleteButton.setDisable(!hasSelection);
                toggleActiveButton.setDisable(!hasSelection);

                if (hasSelection) {
                    toggleActiveButton.setText(
                        Boolean.TRUE.equals(newSelection.getActive()) ? "Deactivate" : "Activate"
                    );
                }
            }
        );

        // Double-click to edit
        rulesTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && rulesTable.getSelectionModel().getSelectedItem() != null) {
                handleEdit();
            }
        });
    }

    /**
     * Update statistics labels
     */
    private void updateStatistics() {
        long activeCount = rules.stream().filter(r -> Boolean.TRUE.equals(r.getActive())).count();
        Integer totalBonus = priorityRuleRepository.getTotalPossibleBonusPoints();

        activeRulesLabel.setText("Active Rules: " + activeCount + " / " + rules.size());
        totalBonusLabel.setText("Total Possible Bonus: " + (totalBonus != null ? totalBonus : 0) + " pts");
    }

    // ========================================================================
    // EVENT HANDLERS
    // ========================================================================

    /**
     * Handle Add button click
     */
    @FXML
    private void handleAdd() {
        PriorityRule newRule = showRuleDialog(null);
        if (newRule != null) {
            PriorityRule saved = priorityRuleRepository.save(newRule);
            rules.add(saved);
            updateStatistics();
            showInfo("Priority rule created successfully.");
        }
    }

    /**
     * Handle Edit button click
     */
    @FXML
    private void handleEdit() {
        PriorityRule selected = rulesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        PriorityRule updated = showRuleDialog(selected);
        if (updated != null) {
            priorityRuleRepository.save(updated);
            rulesTable.refresh();
            updateStatistics();
            showInfo("Priority rule updated successfully.");
        }
    }

    /**
     * Handle Delete button click
     */
    @FXML
    private void handleDelete() {
        PriorityRule selected = rulesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Priority Rule");
        confirm.setContentText("Are you sure you want to delete rule: " + selected.getRuleName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            priorityRuleRepository.delete(selected);
            rules.remove(selected);
            updateStatistics();
            showInfo("Priority rule deleted successfully.");
        }
    }

    /**
     * Handle Toggle Active button click
     */
    @FXML
    private void handleToggleActive() {
        PriorityRule selected = rulesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        boolean newActiveState = !Boolean.TRUE.equals(selected.getActive());
        selected.setActive(newActiveState);
        priorityRuleRepository.save(selected);

        rulesTable.refresh();
        updateStatistics();

        showInfo("Rule " + (newActiveState ? "activated" : "deactivated") + " successfully.");
    }

    /**
     * Handle Close button click
     */
    @FXML
    private void handleClose() {
        Stage stage = (Stage) closeButton.getScene().getWindow();
        stage.close();
    }

    // ========================================================================
    // DIALOG METHODS
    // ========================================================================

    /**
     * Show rule edit/create dialog
     */
    private PriorityRule showRuleDialog(PriorityRule existingRule) {
        boolean isEdit = existingRule != null;
        PriorityRule rule = isEdit ? existingRule : new PriorityRule();

        Dialog<PriorityRule> dialog = new Dialog<>();
        dialog.setTitle(isEdit ? "Edit Priority Rule" : "Create Priority Rule");
        dialog.setHeaderText(isEdit ? "Edit rule: " + rule.getRuleName() : "Create a new priority rule");

        // Set button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        // Rule Name
        TextField nameField = new TextField(rule.getRuleName());
        nameField.setPromptText("Rule Name");

        // Description
        TextArea descField = new TextArea(rule.getDescription());
        descField.setPromptText("Description");
        descField.setPrefRowCount(2);

        // Rule Type
        ComboBox<PriorityRuleType> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll(PriorityRuleType.values());
        typeCombo.setValue(rule.getRuleType() != null ? rule.getRuleType() : PriorityRuleType.GPA_THRESHOLD);

        // Weight
        Spinner<Integer> weightSpinner = new Spinner<>(1, 100, rule.getWeight() != null ? rule.getWeight() : 50);

        // Bonus Points
        Spinner<Integer> bonusSpinner = new Spinner<>(0, 500, rule.getBonusPoints() != null ? rule.getBonusPoints() : 0);

        // Active
        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(Boolean.TRUE.equals(rule.getActive()));

        // Min GPA
        TextField minGPAField = new TextField(rule.getMinGPAThreshold() != null ? rule.getMinGPAThreshold().toString() : "");
        minGPAField.setPromptText("e.g., 3.5");

        // Max GPA
        TextField maxGPAField = new TextField(rule.getMaxGPAThreshold() != null ? rule.getMaxGPAThreshold().toString() : "");
        maxGPAField.setPromptText("e.g., 4.0");

        // Min Behavior Score
        Spinner<Integer> behaviorSpinner = new Spinner<>(1, 5, rule.getMinBehaviorScore() != null ? rule.getMinBehaviorScore() : 3);

        // Grade Levels
        TextField gradeLevelsField = new TextField(rule.getGradeLevels());
        gradeLevelsField.setPromptText("e.g., 11,12");

        // Special population checkboxes
        CheckBox iepCheck = new CheckBox("Apply to IEP students");
        iepCheck.setSelected(Boolean.TRUE.equals(rule.getApplyToIEP()));

        CheckBox plan504Check = new CheckBox("Apply to 504 plan students");
        plan504Check.setSelected(Boolean.TRUE.equals(rule.getApplyTo504()));

        CheckBox giftedCheck = new CheckBox("Apply to gifted students");
        giftedCheck.setSelected(Boolean.TRUE.equals(rule.getApplyToGifted()));

        // Add to grid
        int row = 0;
        grid.add(new Label("Rule Name:"), 0, row);
        grid.add(nameField, 1, row++);

        grid.add(new Label("Description:"), 0, row);
        grid.add(descField, 1, row++);

        grid.add(new Label("Rule Type:"), 0, row);
        grid.add(typeCombo, 1, row++);

        grid.add(new Label("Weight (1-100):"), 0, row);
        grid.add(weightSpinner, 1, row++);

        grid.add(new Label("Bonus Points:"), 0, row);
        grid.add(bonusSpinner, 1, row++);

        grid.add(activeCheck, 1, row++);

        grid.add(new Label("Min GPA:"), 0, row);
        grid.add(minGPAField, 1, row++);

        grid.add(new Label("Max GPA:"), 0, row);
        grid.add(maxGPAField, 1, row++);

        grid.add(new Label("Min Behavior (1-5):"), 0, row);
        grid.add(behaviorSpinner, 1, row++);

        grid.add(new Label("Grade Levels:"), 0, row);
        grid.add(gradeLevelsField, 1, row++);

        grid.add(iepCheck, 1, row++);
        grid.add(plan504Check, 1, row++);
        grid.add(giftedCheck, 1, row++);

        dialog.getDialogPane().setContent(grid);

        // Convert result
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                rule.setRuleName(nameField.getText());
                rule.setDescription(descField.getText());
                rule.setRuleType(typeCombo.getValue());
                rule.setWeight(weightSpinner.getValue());
                rule.setBonusPoints(bonusSpinner.getValue());
                rule.setActive(activeCheck.isSelected());

                // Parse GPA
                if (!minGPAField.getText().isEmpty()) {
                    try {
                        rule.setMinGPAThreshold(Double.parseDouble(minGPAField.getText()));
                    } catch (NumberFormatException e) {
                        showError("Invalid Min GPA format");
                        return null;
                    }
                }

                if (!maxGPAField.getText().isEmpty()) {
                    try {
                        rule.setMaxGPAThreshold(Double.parseDouble(maxGPAField.getText()));
                    } catch (NumberFormatException e) {
                        showError("Invalid Max GPA format");
                        return null;
                    }
                }

                rule.setMinBehaviorScore(behaviorSpinner.getValue());
                rule.setGradeLevels(gradeLevelsField.getText());
                rule.setApplyToIEP(iepCheck.isSelected() ? true : null);
                rule.setApplyTo504(plan504Check.isSelected() ? true : null);
                rule.setApplyToGifted(giftedCheck.isSelected() ? true : null);

                return rule;
            }
            return null;
        });

        Optional<PriorityRule> result = dialog.showAndWait();
        return result.orElse(null);
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Show info message
     */
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
