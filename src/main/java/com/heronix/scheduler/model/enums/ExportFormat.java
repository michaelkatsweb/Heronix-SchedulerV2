package com.heronix.scheduler.model.enums;

/**
 * Export Format Enumeration
 * Defines the available export formats for schedules
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since December 25, 2025
 */
public enum ExportFormat {
    /**
     * Portable Document Format - Universal document format
     */
    PDF("PDF", "application/pdf", ".pdf"),

    /**
     * Microsoft Excel Format (XLSX)
     */
    EXCEL("Excel", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),

    /**
     * Comma-Separated Values - Text-based format
     */
    CSV("CSV", "text/csv", ".csv"),

    /**
     * iCalendar Format - Calendar interchange format
     */
    ICAL("iCal", "text/calendar", ".ics"),

    /**
     * HyperText Markup Language - Web format
     */
    HTML("HTML", "text/html", ".html"),

    /**
     * JavaScript Object Notation - Data interchange format
     */
    JSON("JSON", "application/json", ".json");

    private final String displayName;
    private final String mimeType;
    private final String fileExtension;

    /**
     * Constructor
     *
     * @param displayName User-friendly name
     * @param mimeType MIME type for HTTP headers
     * @param fileExtension File extension including dot
     */
    ExportFormat(String displayName, String mimeType, String fileExtension) {
        this.displayName = displayName;
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
    }

    /**
     * Get display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get MIME type
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Get file extension
     */
    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * Parse from string (case-insensitive)
     */
    public static ExportFormat fromString(String value) {
        if (value == null) {
            return PDF; // Default
        }

        for (ExportFormat format : values()) {
            if (format.name().equalsIgnoreCase(value) ||
                format.displayName.equalsIgnoreCase(value)) {
                return format;
            }
        }

        return PDF; // Default fallback
    }

    @Override
    public String toString() {
        return displayName;
    }
}
