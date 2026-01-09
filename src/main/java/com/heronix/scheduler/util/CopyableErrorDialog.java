// ============================================================================
// FILE: CopyableErrorDialog.java
// LOCATION: src/main/java/com/eduscheduler/ui/util/CopyableErrorDialog.java
// PURPOSE: Custom error dialog with copyable text area
// ============================================================================

package com.heronix.scheduler.util;

import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility class for displaying error dialogs with copyable text content.
 * Allows users to easily copy error messages and stack traces for troubleshooting.
 */
@Slf4j
public class CopyableErrorDialog {

    /**
     * Show an error dialog with copyable text content
     *
     * @param title   Dialog title
     * @param header  Header text (can be null)
     * @param message Error message
     */
    public static void showError(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(header);

        // Create expandable TextArea for the message
        TextArea textArea = new TextArea(message);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setPrefRowCount(10);
        textArea.setPrefColumnCount(50);

        // Make it focusable and selectable
        textArea.setFocusTraversable(true);
        textArea.selectAll(); // Auto-select all text for easy copying

        // Add copy button
        ButtonType copyButtonType = new ButtonType("Copy to Clipboard", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().add(0, copyButtonType);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        alert.getDialogPane().setContent(expContent);

        // Handle copy button click
        alert.showAndWait().ifPresent(response -> {
            if (response == copyButtonType) {
                copyToClipboard(message);
                // Show confirmation
                Alert confirmation = new Alert(Alert.AlertType.INFORMATION);
                confirmation.setTitle("Copied");
                confirmation.setHeaderText(null);
                confirmation.setContentText("Error message copied to clipboard!");
                confirmation.showAndWait();
            }
        });
    }

    /**
     * Show an error dialog with just title and message (no header)
     *
     * @param title   Dialog title
     * @param message Error message
     */
    public static void showError(String title, String message) {
        showError(title, null, message);
    }

    /**
     * Show an error dialog with exception details (includes stack trace)
     *
     * @param title     Dialog title
     * @param message   Error message
     * @param exception Exception to display
     */
    public static void showError(String title, String message, Throwable exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(message);
        alert.setContentText(exception.getMessage());

        // Create expandable Exception stack trace
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("Full stack trace:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setPrefRowCount(15);
        textArea.setPrefColumnCount(60);

        // Make it focusable and selectable
        textArea.setFocusTraversable(true);

        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Add copy button
        ButtonType copyButtonType = new ButtonType("Copy Stack Trace", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().add(0, copyButtonType);

        // Set expandable Exception into the dialog pane
        alert.getDialogPane().setExpandableContent(expContent);
        alert.getDialogPane().setExpanded(false); // Collapsed by default

        // Handle copy button click
        alert.showAndWait().ifPresent(response -> {
            if (response == copyButtonType) {
                copyToClipboard(exceptionText);
                // Show confirmation
                Alert confirmation = new Alert(Alert.AlertType.INFORMATION);
                confirmation.setTitle("Copied");
                confirmation.setHeaderText(null);
                confirmation.setContentText("Stack trace copied to clipboard!");
                confirmation.showAndWait();
            }
        });
    }

    /**
     * Copy text to system clipboard
     *
     * @param text Text to copy
     */
    private static void copyToClipboard(String text) {
        try {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
            log.info("Copied to clipboard: {} characters", text.length());
        } catch (Exception e) {
            log.error("Failed to copy to clipboard", e);
        }
    }

    /**
     * Show a warning dialog with copyable text
     *
     * @param title   Dialog title
     * @param message Warning message
     */
    public static void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(message);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setPrefRowCount(10);

        textArea.setFocusTraversable(true);

        ButtonType copyButtonType = new ButtonType("Copy to Clipboard", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().add(0, copyButtonType);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        alert.getDialogPane().setContent(expContent);

        alert.showAndWait().ifPresent(response -> {
            if (response == copyButtonType) {
                copyToClipboard(message);
                Alert confirmation = new Alert(Alert.AlertType.INFORMATION);
                confirmation.setTitle("Copied");
                confirmation.setHeaderText(null);
                confirmation.setContentText("Warning message copied to clipboard!");
                confirmation.showAndWait();
            }
        });
    }

    /**
     * Show an information dialog with copyable text
     *
     * @param title   Dialog title
     * @param message Information message
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);

        TextArea textArea = new TextArea(message);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setPrefRowCount(10);

        textArea.setFocusTraversable(true);

        ButtonType copyButtonType = new ButtonType("Copy to Clipboard", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().add(0, copyButtonType);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 0);

        alert.getDialogPane().setContent(expContent);

        alert.showAndWait().ifPresent(response -> {
            if (response == copyButtonType) {
                copyToClipboard(message);
                Alert confirmation = new Alert(Alert.AlertType.INFORMATION);
                confirmation.setTitle("Copied");
                confirmation.setHeaderText(null);
                confirmation.setContentText("Message copied to clipboard!");
                confirmation.showAndWait();
            }
        });
    }
}
