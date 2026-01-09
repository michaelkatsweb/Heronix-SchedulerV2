package com.heronix.scheduler.ui.util;

import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Schedule Color Scheme Utility
 * Provides consistent color coding for subjects across the application
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-12-22
 */
public class ScheduleColorScheme {

    // Subject-based color palette (soft, professional colors)
    private static final Map<String, Color> SUBJECT_COLORS = new HashMap<>();

    static {
        // Core Academic Subjects
        SUBJECT_COLORS.put("MATHEMATICS", Color.rgb(66, 133, 244));      // Blue
        SUBJECT_COLORS.put("MATH", Color.rgb(66, 133, 244));
        SUBJECT_COLORS.put("ALGEBRA", Color.rgb(66, 133, 244));
        SUBJECT_COLORS.put("GEOMETRY", Color.rgb(66, 133, 244));
        SUBJECT_COLORS.put("CALCULUS", Color.rgb(66, 133, 244));
        SUBJECT_COLORS.put("STATISTICS", Color.rgb(66, 133, 244));

        SUBJECT_COLORS.put("SCIENCE", Color.rgb(52, 168, 83));           // Green
        SUBJECT_COLORS.put("BIOLOGY", Color.rgb(52, 168, 83));
        SUBJECT_COLORS.put("CHEMISTRY", Color.rgb(52, 168, 83));
        SUBJECT_COLORS.put("PHYSICS", Color.rgb(52, 168, 83));
        SUBJECT_COLORS.put("ENVIRONMENTAL", Color.rgb(52, 168, 83));

        SUBJECT_COLORS.put("ENGLISH", Color.rgb(251, 188, 5));           // Yellow/Gold
        SUBJECT_COLORS.put("LITERATURE", Color.rgb(251, 188, 5));
        SUBJECT_COLORS.put("WRITING", Color.rgb(251, 188, 5));
        SUBJECT_COLORS.put("LANGUAGE ARTS", Color.rgb(251, 188, 5));

        SUBJECT_COLORS.put("HISTORY", Color.rgb(234, 67, 53));           // Red
        SUBJECT_COLORS.put("SOCIAL STUDIES", Color.rgb(234, 67, 53));
        SUBJECT_COLORS.put("GOVERNMENT", Color.rgb(234, 67, 53));
        SUBJECT_COLORS.put("CIVICS", Color.rgb(234, 67, 53));
        SUBJECT_COLORS.put("GEOGRAPHY", Color.rgb(234, 67, 53));

        // Languages
        SUBJECT_COLORS.put("FOREIGN LANGUAGE", Color.rgb(156, 39, 176)); // Purple
        SUBJECT_COLORS.put("SPANISH", Color.rgb(156, 39, 176));
        SUBJECT_COLORS.put("FRENCH", Color.rgb(156, 39, 176));
        SUBJECT_COLORS.put("GERMAN", Color.rgb(156, 39, 176));
        SUBJECT_COLORS.put("CHINESE", Color.rgb(156, 39, 176));
        SUBJECT_COLORS.put("LATIN", Color.rgb(156, 39, 176));

        // Arts & Music
        SUBJECT_COLORS.put("ART", Color.rgb(255, 112, 67));              // Orange
        SUBJECT_COLORS.put("MUSIC", Color.rgb(255, 112, 67));
        SUBJECT_COLORS.put("THEATER", Color.rgb(255, 112, 67));
        SUBJECT_COLORS.put("DRAMA", Color.rgb(255, 112, 67));
        SUBJECT_COLORS.put("VISUAL ARTS", Color.rgb(255, 112, 67));

        // Physical Education & Health
        SUBJECT_COLORS.put("PHYSICAL EDUCATION", Color.rgb(0, 150, 136)); // Teal
        SUBJECT_COLORS.put("PE", Color.rgb(0, 150, 136));
        SUBJECT_COLORS.put("HEALTH", Color.rgb(0, 150, 136));
        SUBJECT_COLORS.put("ATHLETICS", Color.rgb(0, 150, 136));

        // Technology & Computer Science
        SUBJECT_COLORS.put("COMPUTER SCIENCE", Color.rgb(63, 81, 181));  // Indigo
        SUBJECT_COLORS.put("TECHNOLOGY", Color.rgb(63, 81, 181));
        SUBJECT_COLORS.put("PROGRAMMING", Color.rgb(63, 81, 181));
        SUBJECT_COLORS.put("ROBOTICS", Color.rgb(63, 81, 181));
        SUBJECT_COLORS.put("CODING", Color.rgb(63, 81, 181));

        // Electives & Other
        SUBJECT_COLORS.put("BUSINESS", Color.rgb(121, 85, 72));          // Brown
        SUBJECT_COLORS.put("ECONOMICS", Color.rgb(121, 85, 72));
        SUBJECT_COLORS.put("PSYCHOLOGY", Color.rgb(158, 158, 158));      // Gray
        SUBJECT_COLORS.put("PHILOSOPHY", Color.rgb(158, 158, 158));
        SUBJECT_COLORS.put("ELECTIVE", Color.rgb(158, 158, 158));

        // Special Categories
        SUBJECT_COLORS.put("SPECIAL EVENT", Color.rgb(255, 193, 7));     // Amber
        SUBJECT_COLORS.put("ASSEMBLY", Color.rgb(255, 193, 7));
        SUBJECT_COLORS.put("TESTING", Color.rgb(255, 87, 34));           // Deep Orange
        SUBJECT_COLORS.put("LUNCH", Color.rgb(139, 195, 74));            // Light Green
        SUBJECT_COLORS.put("BREAK", Color.rgb(139, 195, 74));
        SUBJECT_COLORS.put("FREE PERIOD", Color.rgb(224, 224, 224));     // Light Gray
    }

    // Default color for unknown subjects
    private static final Color DEFAULT_COLOR = Color.rgb(158, 158, 158); // Gray

    /**
     * Get color for a subject
     */
    public static Color getColorForSubject(String subject) {
        if (subject == null || subject.trim().isEmpty()) {
            return DEFAULT_COLOR;
        }

        String upperSubject = subject.toUpperCase().trim();
        if (SUBJECT_COLORS.containsKey(upperSubject)) {
            return SUBJECT_COLORS.get(upperSubject);
        }

        // Try partial match
        for (Map.Entry<String, Color> entry : SUBJECT_COLORS.entrySet()) {
            if (upperSubject.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return DEFAULT_COLOR;
    }

    /**
     * Get CSS style string for a subject background
     */
    public static String getBackgroundStyle(String subject) {
        Color color = getColorForSubject(subject);
        return String.format("-fx-background-color: rgb(%d, %d, %d);",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }

    /**
     * Get CSS style string for a subject with lighter background and border
     */
    public static String getLightBackgroundStyle(String subject) {
        Color color = getColorForSubject(subject);

        return String.format(
            "-fx-background-color: rgba(%d, %d, %d, 0.3); " +
            "-fx-border-color: rgb(%d, %d, %d); " +
            "-fx-border-width: 2px;",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255),
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255)
        );
    }

    /**
     * Get hex color string for a subject
     */
    public static String getHexColor(String subject) {
        Color color = getColorForSubject(subject);
        return String.format("#%02X%02X%02X",
            (int)(color.getRed() * 255),
            (int)(color.getGreen() * 255),
            (int)(color.getBlue() * 255));
    }

    /**
     * Get contrasting text color (black or white) for a subject background
     */
    public static String getContrastingTextColor(String subject) {
        Color color = getColorForSubject(subject);

        // Calculate relative luminance
        double luminance = 0.2126 * color.getRed() +
                          0.7152 * color.getGreen() +
                          0.0722 * color.getBlue();

        return luminance > 0.5 ? "black" : "white";
    }

    /**
     * Get complete CSS style for a subject cell (background + text color)
     */
    public static String getFullCellStyle(String subject) {
        String bgStyle = getBackgroundStyle(subject);
        String textColor = getContrastingTextColor(subject);

        return bgStyle + " -fx-text-fill: " + textColor + ";";
    }

    /**
     * Register a custom color for a subject
     */
    public static void registerCustomColor(String subject, Color color) {
        if (subject != null && !subject.trim().isEmpty() && color != null) {
            SUBJECT_COLORS.put(subject.toUpperCase().trim(), color);
        }
    }

    /**
     * Get all registered subjects
     */
    public static Map<String, Color> getAllSubjectColors() {
        return new HashMap<>(SUBJECT_COLORS);
    }
}
