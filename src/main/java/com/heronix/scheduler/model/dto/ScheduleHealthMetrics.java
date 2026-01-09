package com.heronix.scheduler.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Schedule Health Metrics
 * Provides a comprehensive health score and detailed metrics for schedule quality
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleHealthMetrics {

    // ========== Overall Health Score ==========

    /**
     * Overall health score (0-100)
     * - 90-100: Excellent (Green)
     * - 70-89: Good (Yellow)
     * - 50-69: Fair (Orange)
     * - 0-49: Poor (Red)
     */
    private Double overallScore;

    private String healthStatus; // EXCELLENT, GOOD, FAIR, POOR

    // ========== Component Scores ==========

    /**
     * Conflict score (0-100)
     * Measures absence of conflicts
     */
    private Double conflictScore;

    /**
     * Balance score (0-100)
     * Measures section enrollment balance
     */
    private Double balanceScore;

    /**
     * Utilization score (0-100)
     * Measures teacher and room utilization
     */
    private Double utilizationScore;

    /**
     * Compliance score (0-100)
     * Measures prep time, IEP, and other compliance
     */
    private Double complianceScore;

    /**
     * Coverage score (0-100)
     * Measures how many students are properly scheduled
     */
    private Double coverageScore;

    // ========== Detailed Metrics ==========

    private Integer totalConflicts;
    private Integer criticalConflicts; // Singleton conflicts
    private Integer warningConflicts; // High-count conflicts

    private Integer unbalancedSections;
    private Integer overEnrolledSections;
    private Integer underEnrolledSections;

    private Integer teachersOverloaded; // > 90% utilization
    private Integer teachersUnderutilized; // < 60% utilization
    private Double averageTeacherUtilization;

    private Integer roomsOverused; // > 90% utilization
    private Integer roomsUnderused; // < 40% utilization
    private Double averageRoomUtilization;

    private Integer studentsFullyScheduled;
    private Integer studentsPartiallyScheduled;
    private Integer studentsUnscheduled;
    private Double studentCoveragePercentage;

    private Integer teachersMissingPrep;
    private Integer iepViolations; // IEP minutes not met

    // ========== Issues & Recommendations ==========

    @Builder.Default
    private List<String> criticalIssues = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    @Builder.Default
    private List<String> recommendations = new ArrayList<>();

    // ========== Calculated Properties ==========

    /**
     * Get health status based on overall score
     */
    public String getHealthStatus() {
        if (overallScore == null) {
            return "UNKNOWN";
        }
        if (overallScore >= 90) {
            return "EXCELLENT";
        } else if (overallScore >= 70) {
            return "GOOD";
        } else if (overallScore >= 50) {
            return "FAIR";
        } else {
            return "POOR";
        }
    }

    /**
     * Get CSS color class for health status
     */
    public String getHealthColor() {
        String status = getHealthStatus();
        switch (status) {
            case "EXCELLENT":
                return "health-excellent"; // Green
            case "GOOD":
                return "health-good"; // Yellow
            case "FAIR":
                return "health-fair"; // Orange
            case "POOR":
                return "health-poor"; // Red
            default:
                return "health-unknown"; // Gray
        }
    }

    /**
     * Check if schedule is acceptable for production
     */
    public boolean isAcceptable() {
        return overallScore != null && overallScore >= 70;
    }

    /**
     * Get summary description
     */
    public String getSummary() {
        if (overallScore == null) {
            return "Health metrics not calculated";
        }

        return String.format("Schedule Health: %s (%.1f/100) - %d critical issues, %d warnings",
            getHealthStatus(), overallScore,
            criticalIssues.size(), warnings.size());
    }
}
