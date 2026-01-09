package com.heronix.scheduler.model.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Assignment Result DTO
 *
 * Contains results from course assignment algorithm
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentResult {

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String initiatedBy;
    private Long academicYearId;
    private Boolean isSimulation = false;

    private int totalRequestsProcessed = 0;
    private int requestsApproved = 0;
    private int requestsWaitlisted = 0;
    private int requestsDenied = 0;
    private int requestsAlternateAssigned = 0;

    private int studentsGotFirstChoice = 0;
    private int studentsGotSecondChoice = 0;
    private int studentsGotThirdChoice = 0;
    private int studentsGotFourthChoice = 0;

    private int totalStudentsProcessed = 0;
    private int studentsWithCompleteSchedules = 0;
    private int studentsWithPartialSchedules = 0;
    private int studentsWithNoAssignments = 0;
    private double averageCoursesPerStudent = 0.0;

    private int totalCoursesProcessed = 0;
    private int coursesNowFull = 0;
    private int coursesAtOptimal = 0;
    private int coursesBelowMinimum = 0;
    private int coursesWithWaitlists = 0;
    private double averageEnrollmentPerCourse = 0.0;

    private double successRate = 0.0;
    private double firstChoiceSatisfactionRate = 0.0;
    private double fairnessScore = 0.0;
    private double balanceScore = 0.0;

    private List<String> studentsWithCompleteSchedulesList = new ArrayList<>();
    private List<String> studentsNeedingReview = new ArrayList<>();
    private List<String> coursesBelowMinimumList = new ArrayList<>();
    private List<String> coursesWithLongWaitlists = new ArrayList<>();

    private List<String> issues = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private int conflictsDetected = 0;
    private int conflictsResolved = 0;
    private List<String> unresolvedConflicts = new ArrayList<>();

    public void calculateDerivedMetrics() {
        if (totalRequestsProcessed > 0) {
            successRate = (double) requestsApproved / totalRequestsProcessed * 100;
        }

        if (requestsApproved > 0) {
            firstChoiceSatisfactionRate = (double) studentsGotFirstChoice / requestsApproved * 100;
        }

        if (totalStudentsProcessed > 0) {
            averageCoursesPerStudent = (double) requestsApproved / totalStudentsProcessed;
        }

        if (totalCoursesProcessed > 0) {
            averageEnrollmentPerCourse = (double) requestsApproved / totalCoursesProcessed;
        }

        if (startTime != null && endTime != null) {
            durationMs = java.time.Duration.between(startTime, endTime).toMillis();
        }
    }

    public void addIssue(String issue) {
        this.issues.add(issue);
    }

    public void addWarning(String warning) {
        this.warnings.add(warning);
    }

    public void addStudentNeedingReview(String studentInfo) {
        this.studentsNeedingReview.add(studentInfo);
    }

    public void addCourseBelowMinimum(String courseInfo) {
        this.coursesBelowMinimumList.add(courseInfo);
    }

    public boolean isSuccessful() {
        return successRate >= 80.0 && issues.isEmpty();
    }

    public boolean hasCriticalIssues() {
        return !issues.isEmpty() || unresolvedConflicts.size() > 5;
    }

    public String getSummary() {
        return String.format(
            "Assignment %s: %d/%d requests approved (%.1f%%), %d waitlisted, %d denied. " +
            "Duration: %dms. Issues: %d, Warnings: %d",
            isSuccessful() ? "SUCCESSFUL" : "COMPLETED WITH ISSUES",
            requestsApproved,
            totalRequestsProcessed,
            successRate,
            requestsWaitlisted,
            requestsDenied,
            durationMs != null ? durationMs : 0,
            issues.size(),
            warnings.size()
        );
    }
}
