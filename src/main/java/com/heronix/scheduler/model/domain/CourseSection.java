package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Course Section Entity (Scheduler Shadow Copy)
 *
 * Minimal representation for scheduling purposes only.
 * Authoritative data lives in SIS microservice.
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Entity
@Table(name = "course_sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "section_number", length = 20)
    private String sectionNumber;

    @Column(name = "is_singleton")
    private Boolean isSingleton = false;

    @Column(name = "is_doubleton")
    private Boolean isDoubleton = false;

    @Column(name = "requires_consecutive_periods")
    private Boolean requiresConsecutivePeriods = false;

    @Column(name = "current_enrollment")
    private Integer currentEnrollment = 0;

    @Column(name = "max_enrollment")
    private Integer maxEnrollment = 30;

    @Column(name = "min_enrollment")
    private Integer minEnrollment = 10;

    @Column(name = "target_enrollment")
    private Integer targetEnrollment = 25;

    @Column(name = "waitlist_count")
    private Integer waitlistCount = 0;

    // Additional fields for scheduling
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_teacher_id")
    private Teacher assignedTeacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_room_id")
    private Room assignedRoom;

    @Column(name = "assigned_period")
    private Integer assignedPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "section_status")
    private SectionStatus sectionStatus = SectionStatus.PLANNED;

    @Column(name = "is_honors")
    private Boolean isHonors = false;

    @Column(name = "is_ap")
    private Boolean isAp = false;

    @Column(name = "gender_distribution_male")
    private Integer genderDistributionMale = 0;

    @Column(name = "gender_distribution_female")
    private Integer genderDistributionFemale = 0;

    @Column(name = "avg_gpa")
    private Double avgGpa;

    @Column(name = "schedule_year")
    private Integer scheduleYear;

    @Column(name = "semester")
    private Integer semester;

    public enum SectionStatus {
        PLANNED,        // Section planned but not yet scheduled
        SCHEDULED,      // Scheduled with teacher/room/period
        OPEN,           // Open for enrollment
        FULL,           // At capacity
        CLOSED,         // Closed for enrollment
        CANCELLED       // Section cancelled
    }

    public boolean isBalanced(CourseSection other, int tolerance) {
        if (other == null) return true;
        int diff = Math.abs(this.currentEnrollment - other.currentEnrollment);
        return diff <= tolerance;
    }

    public int getAvailableSeats() {
        return Math.max(0, maxEnrollment - currentEnrollment);
    }

    public boolean canEnroll() {
        return sectionStatus == SectionStatus.OPEN && currentEnrollment < maxEnrollment;
    }
}
