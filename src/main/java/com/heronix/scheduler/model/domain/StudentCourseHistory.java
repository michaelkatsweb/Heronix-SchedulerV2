package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.CompletionStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Student Course History Entity
 *
 * Tracks all courses a student has taken or is currently taking.
 * Used for:
 * - Prerequisite validation
 * - Transcript generation
 * - Academic progress tracking
 * - Transfer credit recording
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7E - November 21, 2025
 */
@Entity
@Table(name = "student_course_history",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"student_id", "course_id", "academic_year", "semester"}
       ),
       indexes = {
           @Index(name = "idx_history_student", columnList = "student_id"),
           @Index(name = "idx_history_course", columnList = "course_id"),
           @Index(name = "idx_history_status", columnList = "completion_status"),
           @Index(name = "idx_history_year", columnList = "academic_year")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentCourseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The student who took this course
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * The course that was taken
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Academic year when course was taken
     * Format: "2023-2024", "2024-2025"
     */
    @Column(name = "academic_year", nullable = false, length = 20)
    private String academicYear;

    /**
     * Semester when course was taken
     * Values: "Fall", "Spring", "Summer", "Full Year", "Quarter 1", etc.
     */
    @Column(name = "semester", nullable = false, length = 20)
    private String semester;

    /**
     * Grade received in the course
     * Supports multiple formats:
     * - Letter: A+, A, A-, B+, B, B-, C+, C, C-, D+, D, D-, F
     * - Numeric: 95, 87.5, etc. (as string)
     * - Special: P (Pass), F (Fail), I (Incomplete), W (Withdrawn)
     */
    @Column(name = "grade_received", length = 10)
    private String gradeReceived;

    /**
     * Completion status of this course
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "completion_status", nullable = false, length = 20)
    private CompletionStatus completionStatus;

    /**
     * Credits earned for this course
     * Typically 0.5, 1.0, 1.5, 2.0, etc.
     */
    @Column(name = "credits_earned")
    private Double creditsEarned;

    /**
     * Date when course was completed
     */
    @Column(name = "completion_date")
    private LocalDate completionDate;

    /**
     * Additional notes about this history entry
     * Examples:
     * - "Transfer credit from Previous High School"
     * - "Waived by department head"
     * - "Summer enrichment program"
     * - "Completed online through district partnership"
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * When this history record was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When this history record was last updated
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Who created this record (for audit purposes)
     */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if this history entry counts toward prerequisites
     * (must be successfully completed)
     */
    public boolean countsForPrerequisites() {
        return completionStatus != null && completionStatus.countsAsCompleted();
    }

    /**
     * Check if grade meets minimum requirement
     *
     * @param minimumGrade Required minimum grade (e.g., "B", "C+")
     * @return true if this grade meets or exceeds the minimum
     */
    public boolean meetsMinimumGrade(String minimumGrade) {
        if (minimumGrade == null || minimumGrade.isEmpty()) {
            // No minimum specified = any passing grade OK
            return countsForPrerequisites();
        }

        if (gradeReceived == null || gradeReceived.isEmpty()) {
            return false;
        }

        // Special case: P (Pass) always counts if no specific grade required
        if (gradeReceived.equalsIgnoreCase("P")) {
            return true;
        }

        // Compare grades
        return compareGrades(gradeReceived, minimumGrade) >= 0;
    }

    /**
     * Compare two letter grades
     *
     * @param grade1 First grade
     * @param grade2 Second grade
     * @return positive if grade1 > grade2, negative if grade1 < grade2, 0 if equal
     */
    private int compareGrades(String grade1, String grade2) {
        int score1 = gradeToScore(grade1);
        int score2 = gradeToScore(grade2);
        return Integer.compare(score1, score2);
    }

    /**
     * Convert letter grade to numeric score for comparison
     */
    private int gradeToScore(String grade) {
        if (grade == null || grade.isEmpty()) return 0;

        grade = grade.toUpperCase().trim();

        // Handle letter grades with +/-
        return switch (grade) {
            case "A+", "A" -> 13;
            case "A-" -> 12;
            case "B+" -> 11;
            case "B" -> 10;
            case "B-" -> 9;
            case "C+" -> 8;
            case "C" -> 7;
            case "C-" -> 6;
            case "D+" -> 5;
            case "D" -> 4;
            case "D-" -> 3;
            case "F" -> 0;
            case "P" -> 7;  // Pass = C equivalent
            default -> {
                // Try to parse as numeric
                try {
                    double numeric = Double.parseDouble(grade);
                    if (numeric >= 97) yield 13;  // A+
                    if (numeric >= 93) yield 13;  // A
                    if (numeric >= 90) yield 12;  // A-
                    if (numeric >= 87) yield 11;  // B+
                    if (numeric >= 83) yield 10;  // B
                    if (numeric >= 80) yield 9;   // B-
                    if (numeric >= 77) yield 8;   // C+
                    if (numeric >= 73) yield 7;   // C
                    if (numeric >= 70) yield 6;   // C-
                    if (numeric >= 67) yield 5;   // D+
                    if (numeric >= 63) yield 4;   // D
                    if (numeric >= 60) yield 3;   // D-
                    yield 0;  // F
                } catch (NumberFormatException e) {
                    yield 0;  // Unknown = fail
                }
            }
        };
    }

    /**
     * Get display string for this history entry
     */
    public String getDisplayString() {
        return String.format("%s - %s (%s %s): Grade %s [%s]",
            academicYear,
            course != null ? course.getCourseCode() : "Unknown",
            semester,
            course != null ? course.getCourseName() : "",
            gradeReceived != null ? gradeReceived : "N/A",
            completionStatus != null ? completionStatus.getDisplayName() : "Unknown");
    }

    @Override
    public String toString() {
        return String.format("StudentCourseHistory{student=%s, course=%s, year=%s, semester=%s, grade=%s, status=%s}",
            student != null ? student.getStudentId() : "null",
            course != null ? course.getCourseCode() : "null",
            academicYear,
            semester,
            gradeReceived,
            completionStatus);
    }
}
