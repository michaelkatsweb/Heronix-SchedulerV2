package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import com.heronix.scheduler.model.enums.EnrollmentStatus;

/**
 * Student Enrollment Entity - SchedulerV2 Version
 * Location: src/main/java/com/heronix/scheduler/model/domain/StudentEnrollment.java
 *
 * Represents a student's enrollment in a specific course.
 * Links students to courses and tracks which schedule slots they attend.
 *
 * SIMPLIFIED VERSION: Uses shadow Student/Course entities from SIS
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-10-18
 */
@Entity
@Table(name = "student_enrollments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Student enrolled in the course
     * SIMPLIFIED: Uses shadow Student entity (populated from SIS API)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /**
     * Course the student is enrolled in
     * SIMPLIFIED: Uses shadow Course entity (populated from SIS API)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Specific schedule slot assigned to this student
     * This determines when/where the student attends this course
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_slot_id")
    private ScheduleSlot scheduleSlot;

    /**
     * Schedule this enrollment is part of
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    /**
     * Enrollment status
     * ACTIVE: Currently enrolled
     * DROPPED: Student dropped the course
     * COMPLETED: Course completed
     * WAITLISTED: Waiting for open slot
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    /**
     * When the student was enrolled
     */
    @Column(nullable = false)
    private LocalDateTime enrolledDate = LocalDateTime.now();

    /**
     * Student's current grade in this course
     * Optional - may not be set initially
     */
    private Double currentGrade;

    /**
     * Attendance rate for this course
     * Percentage (0-100)
     */
    private Double attendanceRate;

    /**
     * Priority for scheduling
     * Higher priority students get better time slots
     * Example: Seniors = high, Freshmen = lower
     */
    @Column(nullable = false)
    private Integer priority = 5;

    /**
     * Notes about this enrollment
     * Example: "IEP accommodation needed", "Transfer from another section"
     */
    @Column(length = 500)
    private String notes;

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Check if student is actively enrolled
     */
    public boolean isActive() {
        return status == EnrollmentStatus.ACTIVE;
    }

    /**
     * Check if schedule slot is assigned
     */
    public boolean hasScheduleSlot() {
        return scheduleSlot != null;
    }

    /**
     * Get course name
     */
    public String getCourseName() {
        return course != null ? course.getCourseName() : "Unknown";
    }

    /**
     * Get student name
     */
    public String getStudentName() {
        if (student == null)
            return "Unknown";
        return student.getFirstName() + " " + student.getLastName();
    }

    /**
     * Get time slot description
     */
    public String getTimeSlotDescription() {
        if (scheduleSlot == null || scheduleSlot.getTimeSlot() == null) {
            return "Not Scheduled";
        }

        TimeSlot timeSlot = scheduleSlot.getTimeSlot();
        return String.format("%s %s-%s",
                timeSlot.getDayOfWeek(),
                timeSlot.getStartTime(),
                timeSlot.getEndTime());
    }

    /**
     * Get room assignment
     */
    public String getRoomAssignment() {
        if (scheduleSlot == null || scheduleSlot.getRoom() == null) {
            return "TBA";
        }
        return scheduleSlot.getRoom().getRoomNumber();
    }

    /**
     * Get teacher name
     */
    public String getTeacherName() {
        if (scheduleSlot == null || scheduleSlot.getTeacher() == null) {
            return "TBA";
        }

        Teacher teacher = scheduleSlot.getTeacher();
        return teacher.getFirstName() + " " + teacher.getLastName();
    }

    @Override
    public String toString() {
        return String.format("Enrollment: %s in %s (%s)",
                getStudentName(), getCourseName(), status);
    }
}
