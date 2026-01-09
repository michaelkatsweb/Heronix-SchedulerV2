package com.heronix.scheduler.model.dto;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedule Diagnostic Report - USER-FRIENDLY REPORT
 * Location: src/main/java/com/eduscheduler/model/dto/ScheduleDiagnosticReport.java
 *
 * Provides clear, actionable information about what's preventing schedule generation
 * Designed for school administrators, not developers
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since November 18, 2025
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleDiagnosticReport {

    /**
     * Overall status of the schedule readiness
     */
    private DiagnosticStatus overallStatus;

    /**
     * Timestamp when diagnostic was run
     */
    private LocalDateTime diagnosticTimestamp;

    /**
     * Total number of critical issues found
     */
    private int criticalIssuesCount;

    /**
     * Total number of warnings found
     */
    private int warningsCount;

    /**
     * List of all diagnostic issues found
     */
    @Builder.Default
    private List<DiagnosticIssue> issues = new ArrayList<>();

    /**
     * Summary counts for resources
     */
    private ResourceSummary resourceSummary;

    /**
     * User-friendly summary message
     */
    private String summaryMessage;

    /**
     * Recommended actions to fix the issues
     */
    @Builder.Default
    private List<String> recommendedActions = new ArrayList<>();

    /**
     * Estimated time to fix (in minutes)
     */
    private Integer estimatedFixTimeMinutes;

    /**
     * Overall diagnostic status
     */
    public enum DiagnosticStatus {
        READY("Ready for Schedule Generation", "All requirements met. You can proceed with schedule generation."),
        WARNING("Ready with Warnings", "Schedule generation can proceed, but there are optimization opportunities."),
        CRITICAL("Not Ready - Critical Issues", "Schedule generation will fail. Please fix the critical issues listed below."),
        INCOMPLETE_DATA("Incomplete Data", "Essential data is missing. Please add the required information.");

        private final String displayName;
        private final String description;

        DiagnosticStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Individual diagnostic issue
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiagnosticIssue {
        private IssueSeverity severity;
        private IssueCategory category;
        private String title;
        private String description;
        private String userFriendlyExplanation;
        private String howToFix;
        private Integer affectedCount;
        private List<String> affectedItems;
    }

    /**
     * Issue severity levels
     */
    public enum IssueSeverity {
        CRITICAL("Critical", "Must be fixed before schedule generation"),
        WARNING("Warning", "Recommended to fix for better schedules"),
        INFO("Information", "Optional optimization opportunity");

        private final String displayName;
        private final String description;

        IssueSeverity(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Issue categories for grouping
     */
    public enum IssueCategory {
        TEACHERS("Teachers & Certifications"),
        COURSES("Courses & Assignments"),
        ROOMS("Rooms & Facilities"),
        STUDENTS("Students & Enrollments"),
        DATA_QUALITY("Data Quality"),
        CONSTRAINTS("Scheduling Constraints");

        private final String displayName;

        IssueCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Resource summary counts
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResourceSummary {
        private int activeTeachers;
        private int activeCourses;
        private int coursesWithTeachers;
        private int coursesWithoutTeachers;
        private int availableRooms;
        private int activeStudents;
        private int totalEnrollments;

        // Room type breakdown
        private int standardRooms;
        private int labRooms;
        private int gymRooms;
        private int auditoriumRooms;
        private int musicRooms;
        private int computerLabRooms;

        // Course type breakdown
        private int mathCourses;
        private int scienceCourses;
        private int peCourses;
        private int musicCourses;
        private int labRequiredCourses;
    }

    /**
     * Convenience method to add an issue
     */
    public void addIssue(DiagnosticIssue issue) {
        if (this.issues == null) {
            this.issues = new ArrayList<>();
        }
        this.issues.add(issue);

        if (issue.getSeverity() == IssueSeverity.CRITICAL) {
            this.criticalIssuesCount++;
        } else if (issue.getSeverity() == IssueSeverity.WARNING) {
            this.warningsCount++;
        }
    }

    /**
     * Convenience method to add a recommended action
     */
    public void addRecommendedAction(String action) {
        if (this.recommendedActions == null) {
            this.recommendedActions = new ArrayList<>();
        }
        this.recommendedActions.add(action);
    }

    /**
     * Check if ready for schedule generation
     */
    public boolean isReadyForScheduleGeneration() {
        return overallStatus == DiagnosticStatus.READY || overallStatus == DiagnosticStatus.WARNING;
    }

    /**
     * Get user-friendly status message
     */
    public String getStatusMessage() {
        if (overallStatus != null) {
            return overallStatus.getDescription();
        }
        return "Unknown status";
    }
}
