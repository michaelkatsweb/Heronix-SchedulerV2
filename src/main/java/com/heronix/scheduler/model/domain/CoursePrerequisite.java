package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Course Prerequisite Entity
 *
 * Defines prerequisite relationships between courses.
 * Supports multiple prerequisite options using groups (OR logic).
 *
 * Example:
 * AP Calculus AB requires:
 *   - Group 1: Precalculus (Grade C+)
 *   - Group 2: Algebra II (Grade A)
 * Logic: Student needs (Group 1) OR (Group 2)
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7E - November 21, 2025
 */
@Entity
@Table(name = "course_prerequisites",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"course_id", "prerequisite_course_id", "prerequisite_group"}
       ))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CoursePrerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The course that HAS prerequisites
     * Example: AP Calculus AB
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * The prerequisite course (ONE of potentially many options)
     * Example: Precalculus
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_course_id", nullable = false)
    private Course prerequisiteCourse;

    /**
     * Minimum grade required in the prerequisite course
     * Values: "A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "P" (Pass)
     * null = any passing grade acceptable
     */
    @Column(name = "minimum_grade", length = 5)
    private String minimumGrade;

    /**
     * Is this prerequisite required (true) or just recommended (false)?
     * Required prerequisites block enrollment if not met
     * Recommended prerequisites show warnings but allow enrollment
     */
    @Column(name = "is_required", nullable = false)
    private Boolean isRequired = true;

    /**
     * Prerequisite group for OR logic
     * Prerequisites in the same group are alternatives (OR)
     * Prerequisites in different groups are all required (AND)
     *
     * Example:
     * Course X requires:
     *   - (Algebra II OR Geometry) - Both in Group 1
     *   - Chemistry I - Group 2
     * Logic: (Group 1) AND (Group 2)
     */
    @Column(name = "prerequisite_group", nullable = false)
    private Integer prerequisiteGroup = 1;

    /**
     * Description or notes about this prerequisite
     * Example: "Or placement test score of 85+"
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Is this prerequisite definition active?
     * Inactive prerequisites are not enforced but kept for historical records
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * When this prerequisite was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private java.time.LocalDateTime createdAt;

    /**
     * When this prerequisite was last modified
     */
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }

    /**
     * Get display string for this prerequisite
     */
    public String getDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append(prerequisiteCourse.getCourseCode());
        sb.append(" - ").append(prerequisiteCourse.getCourseName());

        if (minimumGrade != null && !minimumGrade.isEmpty()) {
            sb.append(" (Min Grade: ").append(minimumGrade).append(")");
        }

        if (!isRequired) {
            sb.append(" [Recommended]");
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return String.format("CoursePrerequisite{course=%s, prerequisite=%s, minGrade=%s, group=%d}",
            course != null ? course.getCourseCode() : "null",
            prerequisiteCourse != null ? prerequisiteCourse.getCourseCode() : "null",
            minimumGrade,
            prerequisiteGroup);
    }
}
