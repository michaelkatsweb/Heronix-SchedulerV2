package com.heronix.scheduler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for individual AI-detected issue
 *
 * @since Phase 4 - AI Integration
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIIssue {

    /**
     * Issue severity
     */
    public enum Severity {
        CRITICAL,  // Requires immediate attention
        WARNING,   // Should be addressed
        INFO       // Informational/suggestion
    }

    /**
     * Issue type
     */
    public enum Type {
        CERTIFICATION_MISMATCH,
        TEACHER_OVERLOAD,
        ROOM_CAPACITY,
        ROOM_TYPE_MISMATCH,
        SCHEDULE_CONFLICT,
        SEQUENCE_INCONSISTENCY,
        RESOURCE_UNDERUTILIZATION,
        OTHER
    }

    /**
     * Issue severity level
     */
    private Severity severity;

    /**
     * Issue type
     */
    private Type type;

    /**
     * Short title
     */
    private String title;

    /**
     * Detailed description
     */
    private String description;

    /**
     * Entity involved (e.g., "Teacher: John Smith", "Course: ENG101")
     */
    private String affectedEntity;

    /**
     * Suggested action to resolve
     */
    private String suggestedAction;

    /**
     * Priority score (1-10, higher = more urgent)
     */
    private Integer priority;

    /**
     * Get emoji icon for severity
     */
    public String getSeverityIcon() {
        return switch (severity) {
            case CRITICAL -> "🔴";
            case WARNING -> "⚠️";
            case INFO -> "ℹ️";
        };
    }

    /**
     * Get formatted display string
     */
    public String getDisplayString() {
        return String.format("%s %s: %s",
            getSeverityIcon(),
            affectedEntity != null ? affectedEntity : type.name(),
            description);
    }
}
