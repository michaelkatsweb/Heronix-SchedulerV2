package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Waitlist Entity
 *
 * Tracks students waiting for course enrollment
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Entity
@Table(name = "waitlists")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Waitlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "schedule_slot_id")
    private Long scheduleSlotId;

    @Transient
    private Student student;

    @Transient
    private Course course;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "priority_weight")
    private Integer priorityWeight = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WaitlistStatus status = WaitlistStatus.ACTIVE;

    @Column(name = "added_at")
    private java.time.LocalDateTime addedAt = java.time.LocalDateTime.now();

    @Column(name = "enrolled_at")
    private java.time.LocalDateTime enrolledAt;

    @Column(name = "bypass_reason")
    private String bypassReason;

    @Column(name = "notification_sent")
    private Boolean notificationSent = false;

    /**
     * Get student (transient field)
     */
    public Student getStudent() {
        return student;
    }

    /**
     * Set student (transient field)
     */
    public void setStudent(Student student) {
        this.student = student;
        if (student != null) {
            this.studentId = student.getId();
        }
    }

    /**
     * Set course (transient field)
     */
    public void setCourse(Course course) {
        this.course = course;
        if (course != null) {
            this.courseId = course.getId();
        }
    }

    public enum WaitlistStatus {
        ACTIVE,
        ENROLLED,
        BYPASSED,
        EXPIRED,
        CANCELLED
    }
}
