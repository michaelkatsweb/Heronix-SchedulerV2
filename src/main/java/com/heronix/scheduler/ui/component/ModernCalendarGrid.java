package com.heronix.scheduler.ui.component;

import javafx.scene.layout.GridPane;

/**
 * Modern Calendar Grid Component
 * Displays a calendar-style grid for schedule visualization
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-12-22
 */
public class ModernCalendarGrid extends GridPane {

    public ModernCalendarGrid() {
        // Basic initialization
        setHgap(2);
        setVgap(2);
        setStyle("-fx-background-color: white; -fx-padding: 10;");
    }

    /**
     * Color scheme enum for subject coloring
     */
    public enum ColorScheme {
        MATHEMATICS("#4285F4"),
        SCIENCE("#34A853"),
        ENGLISH("#FBBC05"),
        HISTORY("#EA4335"),
        PHYSICAL_EDUCATION("#009688"),
        ARTS("#FF7043"),
        TECHNOLOGY("#3F51B5"),
        BUSINESS("#795548"),
        LUNCH("#8BC34A"),
        OTHER("#9E9E9E");

        private final String colorHex;

        ColorScheme(String colorHex) {
            this.colorHex = colorHex;
        }

        public String getColorHex() {
            return colorHex;
        }
    }

    public static ModernCalendarGrid create() {
        return new ModernCalendarGrid();
    }
}
