package com.heronix.scheduler.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Utility class for showing JavaFX dialogs
 * Location: src/main/java/com/eduscheduler/util/DialogUtils.java
 *
 * @author Heronix Scheduling System Team
 * @since Block Scheduling MVP - November 26, 2025
 */
@Slf4j
public class DialogUtils {

    /**
     * Show error dialog
     */
    public static void showError(String title, String message) {
        log.debug("Showing error dialog: {} - {}", title, message);
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show warning dialog
     */
    public static void showWarning(String title, String message) {
        log.debug("Showing warning dialog: {} - {}", title, message);
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show success dialog
     */
    public static void showSuccess(String title, String message) {
        log.debug("Showing success dialog: {} - {}", title, message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show information dialog
     */
    public static void showInfo(String title, String message) {
        log.debug("Showing info dialog: {} - {}", title, message);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Show confirmation dialog
     * Returns true if user clicked OK, false otherwise
     */
    public static boolean showConfirmation(String title, String message) {
        log.debug("Showing confirmation dialog: {} - {}", title, message);
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
}
