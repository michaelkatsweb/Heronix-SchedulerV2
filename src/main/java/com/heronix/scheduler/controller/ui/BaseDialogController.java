package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.util.ResponsiveDesignHelper;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base Dialog Controller
 *
 * Base class for all dialog controllers providing common responsive design functionality.
 * All dialog controllers should extend this class to automatically get:
 * - Responsive dialog sizing
 * - Table auto-sizing
 * - Screen-aware layouts
 *
 * Usage:
 * 1. Extend this class in your dialog controller
 * 2. Call setDialogStage() after creating the dialog
 * 3. Call configureTableColumns() for any TableViews
 *
 * Location: src/main/java/com/eduscheduler/ui/controller/BaseDialogController.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-12
 */
public abstract class BaseDialogController {

    protected static final Logger log = LoggerFactory.getLogger(BaseDialogController.class);

    protected Stage dialogStage;

    /**
     * Set the dialog stage and configure responsive sizing.
     * Call this method immediately after loading the FXML.
     *
     * @param stage Dialog stage
     */
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
        configureResponsiveSize();
    }

    /**
     * Configure responsive sizing for this dialog.
     * Override this method to customize dialog size.
     * Default: medium dialog (50% width, 60% height)
     */
    protected void configureResponsiveSize() {
        if (dialogStage != null) {
            ResponsiveDesignHelper.makeDialogResponsive(dialogStage);
        }
    }

    /**
     * Configure responsive sizing for small dialogs.
     */
    protected void configureSmallDialogSize() {
        if (dialogStage != null) {
            ResponsiveDesignHelper.makeSmallDialogResponsive(dialogStage);
        }
    }

    /**
     * Configure responsive sizing for large dialogs.
     */
    protected void configureLargeDialogSize() {
        if (dialogStage != null) {
            ResponsiveDesignHelper.makeLargeDialogResponsive(dialogStage);
        }
    }

    /**
     * Configure responsive sizing with custom dimensions.
     *
     * @param preferredWidth Preferred width (will be clamped to screen)
     * @param preferredHeight Preferred height (will be clamped to screen)
     */
    protected void configureCustomDialogSize(double preferredWidth, double preferredHeight) {
        if (dialogStage != null) {
            ResponsiveDesignHelper.makeDialogResponsive(dialogStage, preferredWidth, preferredHeight);
        }
    }

    /**
     * Configure table columns to auto-size (except email columns).
     *
     * @param tableView TableView to configure
     * @param emailColumnNames Names of email columns (will get fixed width)
     */
    protected void configureTableColumns(TableView<?> tableView, String... emailColumnNames) {
        ResponsiveDesignHelper.configureTableAutoSize(tableView, emailColumnNames);
    }

    /**
     * Configure table columns to auto-size (auto-detect email columns).
     *
     * @param tableView TableView to configure
     */
    protected void configureTableColumns(TableView<?> tableView) {
        ResponsiveDesignHelper.configureTableAutoSize(tableView);
    }

    /**
     * Close this dialog.
     */
    @FXML
    protected void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    /**
     * Close this dialog (alias for handleClose).
     */
    @FXML
    protected void handleCancel() {
        handleClose();
    }

    /**
     * Get the dialog stage.
     *
     * @return Dialog stage
     */
    public Stage getDialogStage() {
        return dialogStage;
    }

    /**
     * Check if dialog is showing.
     *
     * @return true if dialog is visible
     */
    public boolean isShowing() {
        return dialogStage != null && dialogStage.isShowing();
    }
}
