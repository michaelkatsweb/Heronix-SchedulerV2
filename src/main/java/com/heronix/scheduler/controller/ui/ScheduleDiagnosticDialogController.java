package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.dto.ScheduleDiagnosticReport;
import com.heronix.scheduler.model.dto.ScheduleDiagnosticReport.*;
import com.heronix.scheduler.service.ScheduleDiagnosticService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;

/**
 * Schedule Diagnostic Dialog Controller
 * Location: src/main/java/com/eduscheduler/ui/controller/ScheduleDiagnosticDialogController.java
 *
 * Displays user-friendly diagnostic report for schedule generation issues
 * Shows administrators exactly what's wrong and how to fix it
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since November 18, 2025
 */
@Controller
@Slf4j
public class ScheduleDiagnosticDialogController {

    @Autowired
    private ScheduleDiagnosticService diagnosticService;

    @FXML
    private Label statusLabel;

    @FXML
    private Label timestampLabel;

    @FXML
    private TabPane diagnosticTabs;

    @FXML
    private TextArea summaryTextArea;

    @FXML
    private GridPane resourceGrid;

    @FXML
    private VBox actionsBox;

    @FXML
    private TextArea actionsTextArea;

    @FXML
    private Tab criticalTab;

    @FXML
    private VBox criticalIssuesBox;

    @FXML
    private Tab warningsTab;

    @FXML
    private VBox warningsBox;

    @FXML
    private VBox detailedReportBox;

    @FXML
    private Button refreshButton;

    @FXML
    private Button exportButton;

    @FXML
    private Button closeButton;

    private Stage dialogStage;
    private ScheduleDiagnosticReport currentReport;

    /**
     * Initialize the dialog
     */
    @FXML
    private void initialize() {
        log.info("Initializing Schedule Diagnostic Dialog");
        // Initialization will be completed when report is loaded
    }

    /**
     * Set the dialog stage
     */
    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    /**
     * Load and display the diagnostic report
     */
    public void loadReport() {
        Platform.runLater(() -> {
            try {
                log.info("Generating diagnostic report...");
                currentReport = diagnosticService.generateDiagnosticReport();
                displayReport(currentReport);
                log.info("Diagnostic report displayed successfully");
            } catch (Exception e) {
                log.error("Failed to generate diagnostic report", e);
                showError("Failed to generate diagnostic report: " + e.getMessage());
            }
        });
    }

    /**
     * Display the diagnostic report in the UI
     */
    private void displayReport(ScheduleDiagnosticReport report) {
        // Update header
        updateHeader(report);

        // Update summary tab
        updateSummaryTab(report);

        // Update critical issues tab
        updateCriticalIssuesTab(report);

        // Update warnings tab
        updateWarningsTab(report);

        // Update detailed report tab
        updateDetailedReportTab(report);

        // Update tab badges
        updateTabBadges(report);
    }

    /**
     * Update header section
     */
    private void updateHeader(ScheduleDiagnosticReport report) {
        // Status
        String statusText = report.getOverallStatus().getDisplayName();
        String statusStyle = getStatusStyle(report.getOverallStatus());
        statusLabel.setText("Status: " + statusText);
        statusLabel.setStyle(statusStyle);

        // Timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a");
        timestampLabel.setText("Report generated: " + report.getDiagnosticTimestamp().format(formatter));
    }

    /**
     * Update summary tab
     */
    private void updateSummaryTab(ScheduleDiagnosticReport report) {
        // Summary message
        summaryTextArea.setText(report.getSummaryMessage());

        // Resource grid
        resourceGrid.getChildren().clear();
        ResourceSummary summary = report.getResourceSummary();

        int row = 0;

        // Row 1: Basic resources
        addResourceItem(resourceGrid, 0, row, "Active Teachers", summary.getActiveTeachers());
        addResourceItem(resourceGrid, 1, row, "Active Courses", summary.getActiveCourses());
        addResourceItem(resourceGrid, 2, row, "Available Rooms", summary.getAvailableRooms());
        row++;

        // Row 2: Course assignments
        addResourceItem(resourceGrid, 0, row, "Courses with Teachers",
                summary.getCoursesWithTeachers() + " / " + summary.getActiveCourses());
        addResourceItem(resourceGrid, 1, row, "Active Students", summary.getActiveStudents());
        addResourceItem(resourceGrid, 2, row, "Total Enrollments", summary.getTotalEnrollments());
        row++;

        // Row 3: Room types
        addResourceItem(resourceGrid, 0, row, "GYM Rooms", summary.getGymRooms());
        addResourceItem(resourceGrid, 1, row, "LAB Rooms", summary.getLabRooms());
        addResourceItem(resourceGrid, 2, row, "Auditorium/Music", summary.getAuditoriumRooms() + summary.getMusicRooms());

        // Recommended actions
        if (report.getRecommendedActions() != null && !report.getRecommendedActions().isEmpty()) {
            StringBuilder actions = new StringBuilder();
            int count = 1;
            for (String action : report.getRecommendedActions()) {
                actions.append(count++).append(". ").append(action).append("\n");
            }
            actionsTextArea.setText(actions.toString());
            actionsBox.setVisible(true);
        } else {
            actionsBox.setVisible(false);
        }
    }

    /**
     * Add resource item to grid
     */
    private void addResourceItem(GridPane grid, int col, int row, String label, Object value) {
        VBox box = new VBox(5);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");

        Label labelText = new Label(label);
        labelText.setFont(Font.font("System", FontWeight.NORMAL, 11));
        labelText.setStyle("-fx-text-fill: #666;");

        Label valueText = new Label(value.toString());
        valueText.setFont(Font.font("System", FontWeight.BOLD, 20));

        box.getChildren().addAll(labelText, valueText);
        grid.add(box, col, row);
    }

    /**
     * Update critical issues tab
     */
    private void updateCriticalIssuesTab(ScheduleDiagnosticReport report) {
        criticalIssuesBox.getChildren().clear();

        boolean hasCritical = false;
        for (DiagnosticIssue issue : report.getIssues()) {
            if (issue.getSeverity() == IssueSeverity.CRITICAL) {
                criticalIssuesBox.getChildren().add(createIssueCard(issue));
                hasCritical = true;
            }
        }

        if (!hasCritical) {
            Label noIssues = new Label("✓ No critical issues found!");
            noIssues.setStyle("-fx-font-size: 16; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            criticalIssuesBox.getChildren().add(noIssues);
        }
    }

    /**
     * Update warnings tab
     */
    private void updateWarningsTab(ScheduleDiagnosticReport report) {
        warningsBox.getChildren().clear();

        boolean hasWarnings = false;
        for (DiagnosticIssue issue : report.getIssues()) {
            if (issue.getSeverity() == IssueSeverity.WARNING) {
                warningsBox.getChildren().add(createIssueCard(issue));
                hasWarnings = true;
            }
        }

        if (!hasWarnings) {
            Label noWarnings = new Label("✓ No warnings found!");
            noWarnings.setStyle("-fx-font-size: 16; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            warningsBox.getChildren().add(noWarnings);
        }
    }

    /**
     * Create issue card
     */
    private VBox createIssueCard(DiagnosticIssue issue) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle(getIssueCardStyle(issue.getSeverity()));

        // Title
        Label title = new Label(issue.getTitle());
        title.setFont(Font.font("System", FontWeight.BOLD, 14));
        title.setWrapText(true);

        // Category and severity
        HBox meta = new HBox(10);
        Label category = new Label(issue.getCategory().getDisplayName());
        category.setStyle("-fx-background-color: #e0e0e0; -fx-padding: 3 8; -fx-background-radius: 3; -fx-font-size: 11;");

        Label severity = new Label(issue.getSeverity().getDisplayName());
        severity.setStyle(getSeverityBadgeStyle(issue.getSeverity()));

        meta.getChildren().addAll(category, severity);

        // Description
        Label desc = new Label(issue.getDescription());
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 12; -fx-text-fill: #333;");

        // Explanation
        if (issue.getUserFriendlyExplanation() != null) {
            Label explanation = new Label("Why this matters:");
            explanation.setFont(Font.font("System", FontWeight.BOLD, 12));

            Label explanationText = new Label(issue.getUserFriendlyExplanation());
            explanationText.setWrapText(true);
            explanationText.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");

            card.getChildren().addAll(title, meta, desc, new Separator(), explanation, explanationText);
        } else {
            card.getChildren().addAll(title, meta, desc);
        }

        // How to fix
        if (issue.getHowToFix() != null) {
            Label howToLabel = new Label("How to fix:");
            howToLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            howToLabel.setStyle("-fx-text-fill: #1976D2;");

            Label howToText = new Label(issue.getHowToFix());
            howToText.setWrapText(true);
            howToText.setStyle("-fx-font-size: 12; -fx-text-fill: #1976D2; -fx-font-style: italic;");

            card.getChildren().addAll(new Separator(), howToLabel, howToText);
        }

        // Affected items
        if (issue.getAffectedItems() != null && !issue.getAffectedItems().isEmpty()) {
            Label affectedLabel = new Label("Affected items:");
            affectedLabel.setFont(Font.font("System", FontWeight.BOLD, 11));

            VBox affectedList = new VBox(3);
            for (String item : issue.getAffectedItems()) {
                Label itemLabel = new Label("• " + item);
                itemLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #666;");
                itemLabel.setWrapText(true);
                affectedList.getChildren().add(itemLabel);
            }

            card.getChildren().addAll(new Separator(), affectedLabel, affectedList);
        }

        return card;
    }

    /**
     * Update detailed report tab
     */
    private void updateDetailedReportTab(ScheduleDiagnosticReport report) {
        detailedReportBox.getChildren().clear();

        // Group issues by category
        for (IssueCategory category : IssueCategory.values()) {
            VBox categoryBox = new VBox(10);
            Label categoryLabel = new Label(category.getDisplayName());
            categoryLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

            boolean hasIssues = false;
            for (DiagnosticIssue issue : report.getIssues()) {
                if (issue.getCategory() == category) {
                    categoryBox.getChildren().add(createIssueCard(issue));
                    hasIssues = true;
                }
            }

            if (hasIssues) {
                detailedReportBox.getChildren().addAll(categoryLabel, categoryBox, new Separator());
            }
        }

        if (detailedReportBox.getChildren().isEmpty()) {
            Label noIssues = new Label("✓ No issues found in any category!");
            noIssues.setStyle("-fx-font-size: 16; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
            detailedReportBox.getChildren().add(noIssues);
        }
    }

    /**
     * Update tab badges with counts
     */
    private void updateTabBadges(ScheduleDiagnosticReport report) {
        criticalTab.setText(String.format("Critical Issues (%d)", report.getCriticalIssuesCount()));
        warningsTab.setText(String.format("Warnings (%d)", report.getWarningsCount()));
    }

    /**
     * Get status style based on diagnostic status
     */
    private String getStatusStyle(DiagnosticStatus status) {
        switch (status) {
            case READY:
                return "-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 16;";
            case WARNING:
                return "-fx-text-fill: #FF9800; -fx-font-weight: bold; -fx-font-size: 16;";
            case CRITICAL:
            case INCOMPLETE_DATA:
                return "-fx-text-fill: #F44336; -fx-font-weight: bold; -fx-font-size: 16;";
            default:
                return "-fx-font-weight: bold; -fx-font-size: 16;";
        }
    }

    /**
     * Get issue card style based on severity
     */
    private String getIssueCardStyle(IssueSeverity severity) {
        String baseStyle = "-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-width: 2;";
        switch (severity) {
            case CRITICAL:
                return baseStyle + " -fx-background-color: #FFEBEE; -fx-border-color: #F44336;";
            case WARNING:
                return baseStyle + " -fx-background-color: #FFF3E0; -fx-border-color: #FF9800;";
            case INFO:
                return baseStyle + " -fx-background-color: #E3F2FD; -fx-border-color: #2196F3;";
            default:
                return baseStyle + " -fx-background-color: #F5F5F5; -fx-border-color: #BDBDBD;";
        }
    }

    /**
     * Get severity badge style
     */
    private String getSeverityBadgeStyle(IssueSeverity severity) {
        String baseStyle = "-fx-padding: 3 8; -fx-background-radius: 3; -fx-font-size: 11; -fx-font-weight: bold;";
        switch (severity) {
            case CRITICAL:
                return baseStyle + " -fx-background-color: #F44336; -fx-text-fill: white;";
            case WARNING:
                return baseStyle + " -fx-background-color: #FF9800; -fx-text-fill: white;";
            case INFO:
                return baseStyle + " -fx-background-color: #2196F3; -fx-text-fill: white;";
            default:
                return baseStyle + " -fx-background-color: #9E9E9E; -fx-text-fill: white;";
        }
    }

    /**
     * Handle refresh button
     */
    @FXML
    private void handleRefresh() {
        loadReport();
    }

    /**
     * Handle export button - exports diagnostic report to text/CSV file
     */
    @FXML
    private void handleExport() {
        log.info("Exporting diagnostic report...");

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Export Diagnostic Report");
        fileChooser.setInitialFileName("schedule_diagnostic_" +
            java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt");
        fileChooser.getExtensionFilters().addAll(
            new javafx.stage.FileChooser.ExtensionFilter("Text Files", "*.txt"),
            new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
        );

        java.io.File file = fileChooser.showSaveDialog(exportButton.getScene().getWindow());

        if (file != null) {
            try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file))) {
                // Header
                writer.println("===============================================================================");
                writer.println("SCHEDULE DIAGNOSTIC REPORT");
                writer.println("===============================================================================");
                writer.println("Generated: " + java.time.LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                writer.println("Status: " + (statusLabel != null ? statusLabel.getText() : "N/A"));
                writer.println();

                // Summary
                writer.println("=== SUMMARY ===");
                if (summaryTextArea != null && summaryTextArea.getText() != null) {
                    writer.println(summaryTextArea.getText());
                }
                writer.println();

                // Actions Required
                writer.println("=== RECOMMENDED ACTIONS ===");
                if (actionsTextArea != null && actionsTextArea.getText() != null) {
                    writer.println(actionsTextArea.getText());
                }
                writer.println();

                // Critical Issues
                writer.println("=== CRITICAL ISSUES ===");
                if (criticalIssuesBox != null) {
                    for (javafx.scene.Node node : criticalIssuesBox.getChildren()) {
                        if (node instanceof Label) {
                            writer.println("- " + ((Label) node).getText());
                        } else if (node instanceof VBox) {
                            for (javafx.scene.Node inner : ((VBox) node).getChildren()) {
                                if (inner instanceof Label) {
                                    writer.println("  " + ((Label) inner).getText());
                                }
                            }
                        }
                    }
                }
                writer.println();

                // Warnings
                writer.println("=== WARNINGS ===");
                if (warningsBox != null) {
                    for (javafx.scene.Node node : warningsBox.getChildren()) {
                        if (node instanceof Label) {
                            writer.println("- " + ((Label) node).getText());
                        }
                    }
                }
                writer.println();

                writer.println("===============================================================================");
                writer.println("End of Report");

                showInfo("Diagnostic report exported to:\n" + file.getAbsolutePath());
                log.info("Diagnostic report exported successfully to: {}", file.getAbsolutePath());

            } catch (java.io.IOException e) {
                log.error("Failed to export diagnostic report", e);
                showError("Failed to export report: " + e.getMessage());
            }
        }
    }

    /**
     * Handle close button
     */
    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
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
}
