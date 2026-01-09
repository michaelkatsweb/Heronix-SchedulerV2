package com.heronix.scheduler.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * FlexibleTimeParser - Parse time input in various user-friendly formats
 *
 * Supports:
 * - 12-hour format with AM/PM (am/pm, AM/PM, Am/Pm, aM/pM)
 * - 24-hour format
 * - Various separators (: or .)
 * - Flexible spacing
 *
 * Examples:
 * - "9:00 AM", "9:00am", "9:00 Am", "9:00 aM" → 09:00
 * - "2:30 PM", "2:30pm", "2:30 Pm", "2:30 pM" → 14:30
 * - "14:30", "1430", "14.30" → 14:30
 * - "9", "9 AM", "9AM" → 09:00
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-17
 */
public class FlexibleTimeParser {

    // Patterns for flexible time parsing
    private static final Pattern TIME_12H_PATTERN = Pattern.compile(
        "^\\s*(\\d{1,2})(?:[:.]?(\\d{2}))?\\s*([AaPp][Mm]?)\\s*$"
    );

    private static final Pattern TIME_24H_PATTERN = Pattern.compile(
        "^\\s*(\\d{1,2})[:.]?(\\d{2})?\\s*$"
    );

    /**
     * Parse time string in flexible format
     *
     * @param timeStr Time string to parse
     * @return LocalTime object, or null if parsing fails
     */
    public static LocalTime parse(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return null;
        }

        timeStr = timeStr.trim();

        // Try 12-hour format first (with AM/PM)
        Matcher matcher12h = TIME_12H_PATTERN.matcher(timeStr);
        if (matcher12h.matches()) {
            return parse12HourFormat(matcher12h);
        }

        // Try 24-hour format
        Matcher matcher24h = TIME_24H_PATTERN.matcher(timeStr);
        if (matcher24h.matches()) {
            return parse24HourFormat(matcher24h);
        }

        // Try standard ISO formats as fallback
        return tryStandardFormats(timeStr);
    }

    /**
     * Parse 12-hour format (with AM/PM)
     */
    private static LocalTime parse12HourFormat(Matcher matcher) {
        try {
            int hour = Integer.parseInt(matcher.group(1));
            String minuteStr = matcher.group(2);
            int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;
            String ampm = matcher.group(3).toUpperCase();

            // Validate ranges
            if (hour < 1 || hour > 12) {
                return null;
            }
            if (minute < 0 || minute > 59) {
                return null;
            }

            // Convert to 24-hour format
            if (ampm.startsWith("P")) {  // PM
                if (hour != 12) {
                    hour += 12;
                }
            } else {  // AM
                if (hour == 12) {
                    hour = 0;
                }
            }

            return LocalTime.of(hour, minute);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse 24-hour format
     */
    private static LocalTime parse24HourFormat(Matcher matcher) {
        try {
            int hour = Integer.parseInt(matcher.group(1));
            String minuteStr = matcher.group(2);
            int minute = minuteStr != null ? Integer.parseInt(minuteStr) : 0;

            // Validate ranges
            if (hour < 0 || hour > 23) {
                return null;
            }
            if (minute < 0 || minute > 59) {
                return null;
            }

            return LocalTime.of(hour, minute);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Try standard ISO time formats
     */
    private static LocalTime tryStandardFormats(String timeStr) {
        // Try ISO format (HH:mm:ss or HH:mm)
        try {
            return LocalTime.parse(timeStr);
        } catch (DateTimeParseException e1) {
            // Try HH:mm format
            try {
                return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
            } catch (DateTimeParseException e2) {
                // Try H:mm format
                try {
                    return LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("H:mm"));
                } catch (DateTimeParseException e3) {
                    return null;
                }
            }
        }
    }

    /**
     * Format LocalTime to 12-hour format with AM/PM
     *
     * @param time Time to format
     * @return Formatted string (e.g., "9:00 AM", "2:30 PM")
     */
    public static String format12Hour(LocalTime time) {
        if (time == null) {
            return "";
        }

        int hour = time.getHour();
        int minute = time.getMinute();
        String ampm = hour < 12 ? "AM" : "PM";

        if (hour == 0) {
            hour = 12;
        } else if (hour > 12) {
            hour -= 12;
        }

        return String.format("%d:%02d %s", hour, minute, ampm);
    }

    /**
     * Format LocalTime to 24-hour format
     *
     * @param time Time to format
     * @return Formatted string (e.g., "09:00", "14:30")
     */
    public static String format24Hour(LocalTime time) {
        if (time == null) {
            return "";
        }
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    /**
     * Check if string is valid time format
     *
     * @param timeStr Time string to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValid(String timeStr) {
        return parse(timeStr) != null;
    }
}
