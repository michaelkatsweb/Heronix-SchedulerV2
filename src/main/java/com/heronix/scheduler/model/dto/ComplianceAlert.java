package com.heronix.scheduler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Compliance Alert DTO
 *
 * Represents a notification about compliance violations that requires
 * administrator attention. Can trigger email/dashboard alerts.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 18 - Compliance Alerts
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceAlert {

    private Long id;
    private LocalDateTime detectedAt;
    private LocalDateTime notifiedAt;
    private boolean acknowledged;
    private LocalDateTime acknowledgedAt;
    private String acknowledgedBy;

    /**
     * Alert severity
     */
    private AlertSeverity severity;

    /**
     * Summary message
     */
    private String summary;

    /**
     * Detailed description
     */
    private String details;

    /**
     * Related violations
     */
    @Builder.Default
    private List<ComplianceViolation> violations = new ArrayList<>();

    /**
     * Recommended actions
     */
    @Builder.Default
    private List<String> recommendedActions = new ArrayList<>();

    /**
     * Temporary solutions available
     */
    @Builder.Default
    private List<TemporarySolution> temporarySolutions = new ArrayList<>();

    /**
     * Days until action required
     */
    private Integer daysUntilDeadline;

    public enum AlertSeverity {
        IMMEDIATE_ACTION_REQUIRED,  // Must fix today
        URGENT,                      // Fix within 3 days
        HIGH_PRIORITY,              // Fix within 1 week
        MODERATE,                   // Fix within 2 weeks
        LOW_PRIORITY               // Fix when possible
    }

    /**
     * Temporary solutions while finding certified teacher
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TemporarySolution {
        private TemporarySolutionType type;
        private String description;
        private String legalBasis;
        private Integer maxDurationDays;
        private List<String> requirements;
        private String applicationProcess;
        private boolean available;

        public enum TemporarySolutionType {
            SUBSTITUTE_TEACHER,           // Assign substitute until certified teacher hired
            EMERGENCY_CERTIFICATION,      // Apply for emergency cert (teacher in training)
            TEMPORARY_CERTIFICATE,        // State temporary certificate (30-90 days)
            OUT_OF_FIELD_WAIVER,         // Waiver for teaching out of certification area
            ALTERNATIVE_CERTIFICATION,    // Fast-track cert program while teaching
            CO_TEACHING_ARRANGEMENT,      // Pair with certified teacher
            DISTANCE_LEARNING,           // Certified teacher via video
            COURSE_CANCELLATION          // Last resort - cancel course
        }
    }
}
