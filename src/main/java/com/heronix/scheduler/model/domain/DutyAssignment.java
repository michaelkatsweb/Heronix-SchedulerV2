package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * ═══════════════════════════════════════════════════════════════
 * DUTY ASSIGNMENT ENTITY
 * Tracks teacher and staff duty schedules
 * ═══════════════════════════════════════════════════════════════
 * 
 * Location: src/main/java/com/eduscheduler/model/domain/DutyAssignment.java
 * 
 * Purpose:
 * - Track AM/PM duty assignments for teachers and staff
 * - Manage duty locations (hallways, cafeteria, parking lots, etc.)
 * - Ensure fair distribution of duty responsibilities
 * - Handle substitute duty assignments
 * 
 * Features:
 * ✓ Multiple duty types (AM, PM, Lunch, Bus, Event)
 * ✓ Location tracking
 * ✓ Recurring duty schedules
 * ✓ Substitute duty coverage
 * ✓ Automatic rotation
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-02
 */
@Entity
@Table(name = "duty_assignments", indexes = {
        @Index(name = "idx_duty_teacher", columnList = "teacher_id"),
        @Index(name = "idx_duty_date", columnList = "duty_date"),
        @Index(name = "idx_duty_type", columnList = "duty_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DutyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Teacher or staff member assigned to duty
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private Teacher teacher;

    /**
     * Duty type (AM, PM, LUNCH, BUS, EVENT, SPECIAL)
     */
    @Column(name = "duty_type", nullable = false, length = 20)
    private String dutyType;

    /**
     * Duty location (e.g., "Front Entrance", "Cafeteria", "Bus Loop", "Hallway 2A")
     */
    @Column(name = "duty_location", nullable = false, length = 100)
    private String dutyLocation;

    /**
     * Date of duty assignment
     */
    @Column(name = "duty_date", nullable = false)
    private LocalDate dutyDate;

    /**
     * Start time of duty
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * End time of duty
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Is this a recurring duty? (e.g., every Monday AM duty)
     */
    @Column(name = "is_recurring")
    private boolean isRecurring = false;

    /**
     * Recurrence pattern (e.g., "WEEKLY", "DAILY", "BIWEEKLY")
     */
    @Column(name = "recurrence_pattern", length = 20)
    private String recurrencePattern;

    /**
     * Day of week for recurring duties (1=Monday, 7=Sunday)
     */
    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    /**
     * Is this duty covered by a substitute?
     */
    @Column(name = "is_substitute")
    private boolean isSubstitute = false;

    /**
     * Original teacher if this is a substitute duty
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_teacher_id")
    private Teacher originalTeacher;

    /**
     * Duty priority (1=Required, 2=Normal, 3=Optional)
     */
    @Column(name = "priority")
    private Integer priority = 2;

    /**
     * Special instructions or notes
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Active status
     */
    @Column(name = "is_active")
    private boolean isActive = true;

    /**
     * Schedule this duty belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Get display string for duty assignment
     */
    public String getDisplayString() {
        return String.format("%s Duty - %s (%s to %s)",
                dutyType,
                dutyLocation,
                startTime.toString(),
                endTime.toString());
    }

    /**
     * Check if duty is on a specific date
     */
    public boolean isOnDate(LocalDate date) {
        if (!isRecurring) {
            return dutyDate.equals(date);
        }

        // Check recurring pattern
        if (dayOfWeek != null && date.getDayOfWeek().getValue() == dayOfWeek) {
            return date.isAfter(dutyDate) || date.isEqual(dutyDate);
        }

        return false;
    }

    /**
     * Get teacher name for display
     */
    public String getTeacherName() {
        return teacher != null ? teacher.getName() : "Unassigned";
    }

    /**
     * Check if this is an AM duty
     */
    public boolean isAMDuty() {
        return "AM".equalsIgnoreCase(dutyType);
    }

    /**
     * Check if this is a PM duty
     */
    public boolean isPMDuty() {
        return "PM".equalsIgnoreCase(dutyType);
    }

    /**
     * Check if this is a lunch duty
     */
    public boolean isLunchDuty() {
        return "LUNCH".equalsIgnoreCase(dutyType);
    }
}