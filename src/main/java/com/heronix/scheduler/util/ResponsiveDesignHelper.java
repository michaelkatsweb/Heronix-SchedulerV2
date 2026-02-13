package com.heronix.scheduler.util;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;
import javafx.geometry.Rectangle2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Responsive Design Helper
 *
 * Utility class for implementing responsive UI patterns across the application.
 * Handles dialog sizing, table column auto-sizing, and screen-aware layouts.
 *
 * Features:
 * - Auto-size dialogs to fit within screen bounds
 * - Configure table columns to show full text (except emails)
 * - Add ScrollPanes to oversized content
 * - Screen size detection and adaptation
 *
 * Location: src/main/java/com/eduscheduler/util/ResponsiveDesignHelper.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-12
 */
public class ResponsiveDesignHelper {

    private static final Logger log = LoggerFactory.getLogger(ResponsiveDesignHelper.class);

    // Screen size thresholds
    private static final double SMALL_SCREEN_WIDTH = 1366;
    private static final double MEDIUM_SCREEN_WIDTH = 1920;
    private static final double LARGE_SCREEN_WIDTH = 2560;

    // Dialog size constraints (percentage of screen)
    private static final double MAX_DIALOG_WIDTH_PERCENT = 0.85;
    private static final double MAX_DIALOG_HEIGHT_PERCENT = 0.85;
    private static final double MIN_DIALOG_WIDTH = 400;
    private static final double MIN_DIALOG_HEIGHT = 300;

    // Table column sizing
    private static final double MIN_COLUMN_WIDTH = 80;
    private static final double MAX_COLUMN_WIDTH = 300;
    private static final double EMAIL_COLUMN_WIDTH = 200; // Fixed width for email columns

    /**
     * Make a dialog responsive by setting appropriate size constraints.
     * Dialog will automatically size to fit content but not exceed screen bounds.
     *
     * @param stage Dialog stage to configure
     * @param preferredWidth Preferred width (will be clamped to screen size)
     * @param preferredHeight Preferred height (will be clamped to screen size)
     */
    public static void makeDialogResponsive(Stage stage, double preferredWidth, double preferredHeight) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        double maxWidth = screenBounds.getWidth() * MAX_DIALOG_WIDTH_PERCENT;
        double maxHeight = screenBounds.getHeight() * MAX_DIALOG_HEIGHT_PERCENT;

        // Clamp preferred size to screen bounds
        double finalWidth = Math.max(MIN_DIALOG_WIDTH, Math.min(preferredWidth, maxWidth));
        double finalHeight = Math.max(MIN_DIALOG_HEIGHT, Math.min(preferredHeight, maxHeight));

        stage.setWidth(finalWidth);
        stage.setHeight(finalHeight);
        stage.setMaxWidth(maxWidth);
        stage.setMaxHeight(maxHeight);
        stage.setMinWidth(MIN_DIALOG_WIDTH);
        stage.setMinHeight(MIN_DIALOG_HEIGHT);

        // Center on screen
        stage.setX((screenBounds.getWidth() - finalWidth) / 2);
        stage.setY((screenBounds.getHeight() - finalHeight) / 2);

        log.debug("Configured responsive dialog: {}x{} (max: {}x{})",
                finalWidth, finalHeight, maxWidth, maxHeight);
    }

    /**
     * Make a dialog responsive with default size (medium dialog).
     *
     * @param stage Dialog stage to configure
     */
    public static void makeDialogResponsive(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double defaultWidth = screenBounds.getWidth() * 0.5;
        double defaultHeight = screenBounds.getHeight() * 0.6;
        makeDialogResponsive(stage, defaultWidth, defaultHeight);
    }

    /**
     * Make a small dialog responsive (40% width, 50% height).
     *
     * @param stage Dialog stage to configure
     */
    public static void makeSmallDialogResponsive(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = screenBounds.getWidth() * 0.4;
        double height = screenBounds.getHeight() * 0.5;
        makeDialogResponsive(stage, width, height);
    }

    /**
     * Make a large dialog responsive (70% width, 80% height).
     *
     * @param stage Dialog stage to configure
     */
    public static void makeLargeDialogResponsive(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = screenBounds.getWidth() * 0.7;
        double height = screenBounds.getHeight() * 0.8;
        makeDialogResponsive(stage, width, height);
    }

    /**
     * Configure a TableView to auto-size columns to fit content.
     * Email columns will have fixed width, other columns expand to show full text.
     *
     * @param tableView TableView to configure
     * @param emailColumnNames Names of columns containing email addresses (will get fixed width)
     */
    public static void configureTableAutoSize(TableView<?> tableView, String... emailColumnNames) {
        List<String> emailColumns = Arrays.asList(emailColumnNames);

        for (TableColumn<?, ?> column : tableView.getColumns()) {
            String columnName = column.getText().toLowerCase();

            if (emailColumns.contains(column.getText()) ||
                columnName.contains("email") ||
                columnName.contains("e-mail")) {

                // Email columns get fixed width
                column.setPrefWidth(EMAIL_COLUMN_WIDTH);
                column.setMinWidth(EMAIL_COLUMN_WIDTH);
                column.setMaxWidth(EMAIL_COLUMN_WIDTH);
                column.setResizable(false);

                log.debug("Set fixed width for email column: {}", column.getText());

            } else {
                // Other columns auto-size to content
                column.setMinWidth(MIN_COLUMN_WIDTH);
                column.setMaxWidth(MAX_COLUMN_WIDTH);
                column.setPrefWidth(USE_COMPUTED_SIZE);
                column.setResizable(true);

                // Enable text wrapping for long content
                column.setStyle("-fx-alignment: CENTER-LEFT;");
            }
        }

        // Set table resize policy to constrained (columns fill available space)
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        log.debug("Configured auto-sizing for table with {} columns", tableView.getColumns().size());
    }

    /**
     * Configure a TableView with default auto-sizing (detects email columns automatically).
     *
     * @param tableView TableView to configure
     */
    public static void configureTableAutoSize(TableView<?> tableView) {
        configureTableAutoSize(tableView, new String[0]);
    }

    /**
     * Wrap a Region in a ScrollPane with responsive settings.
     *
     * @param content Content to wrap
     * @param fitToWidth Whether content should stretch to full width
     * @param fitToHeight Whether content should stretch to full height
     * @return Configured ScrollPane
     */
    public static ScrollPane wrapInScrollPane(Region content, boolean fitToWidth, boolean fitToHeight) {
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(fitToWidth);
        scrollPane.setFitToHeight(fitToHeight);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        log.debug("Wrapped content in responsive ScrollPane");
        return scrollPane;
    }

    /**
     * Get current screen size category.
     *
     * @return "small", "medium", "large", or "extra-large"
     */
    public static String getScreenSizeCategory() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double width = screenBounds.getWidth();

        if (width < SMALL_SCREEN_WIDTH) {
            return "small";
        } else if (width < MEDIUM_SCREEN_WIDTH) {
            return "medium";
        } else if (width < LARGE_SCREEN_WIDTH) {
            return "large";
        } else {
            return "extra-large";
        }
    }

    /**
     * Get maximum safe dialog width for current screen.
     *
     * @return Maximum dialog width in pixels
     */
    public static double getMaxDialogWidth() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        return screenBounds.getWidth() * MAX_DIALOG_WIDTH_PERCENT;
    }

    /**
     * Get maximum safe dialog height for current screen.
     *
     * @return Maximum dialog height in pixels
     */
    public static double getMaxDialogHeight() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        return screenBounds.getHeight() * MAX_DIALOG_HEIGHT_PERCENT;
    }

    /**
     * Check if current screen is considered small (< 1366px).
     *
     * @return true if screen is small
     */
    public static boolean isSmallScreen() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        return screenBounds.getWidth() < SMALL_SCREEN_WIDTH;
    }

    /**
     * Log current screen information for debugging.
     */
    public static void logScreenInfo() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        String category = getScreenSizeCategory();

        log.info("╔══════════════════════════════════════════════════════════╗");
        log.info("║ SCREEN INFORMATION                                       ║");
        log.info("╠══════════════════════════════════════════════════════════╣");
        log.info("║ Resolution: {}x{} px", screenBounds.getWidth(), screenBounds.getHeight());
        log.info("║ Category: {}", category);
        log.info("║ Max Dialog: {}x{} px",
                (int)getMaxDialogWidth(), (int)getMaxDialogHeight());
        log.info("╚══════════════════════════════════════════════════════════╝");
    }

    // Constant for JavaFX USE_COMPUTED_SIZE
    private static final double USE_COMPUTED_SIZE = Region.USE_COMPUTED_SIZE;
}
