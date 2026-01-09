package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.DayOfWeek;
import com.heronix.scheduler.model.enums.DutyType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Special Duty Assignment Entity
 * Tracks special duty assignments for teachers, staff, and administrators
 * Supports both daily routine duties and special event duties
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-06
 */
@Entity
@Table(name = "special_duty_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecialDutyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // DUTY INFORMATION
    // ========================================================================

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, name = "duty_type")
    private DutyType dutyType;

    @Column(name = "custom_duty_name")
    private String customDutyName; // For CUSTOM duty type

    @Column(name = "duty_location")
    private String dutyLocation; // e.g., "Main Entrance", "Cafeteria A", "Gymnasium"

    @Column(columnDefinition = "TEXT")
    private String notes;

    // ========================================================================
    // STAFF ASSIGNMENT (Polymorphic - can be Teacher or other staff)
    // ========================================================================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @Column(name = "staff_name")
    private String staffName; // For non-teacher staff (administrators, support staff)

    @Column(name = "staff_type")
    private String staffType; // "TEACHER", "ADMINISTRATOR", "SUPPORT_STAFF", "SECURITY"

    // ========================================================================
    // SCHEDULE INFORMATION
    // ========================================================================

    /**
     * For daily duties: recurring day of week
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    /**
     * For special events: specific date
     */
    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    /**
     * For recurring duties: effective date range
     */
    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_until")
    private LocalDate effectiveUntil;

    // ========================================================================
    // DUTY ATTRIBUTES
    // ========================================================================

    @Column(name = "is_recurring")
    private Boolean isRecurring = false; // true for daily duties, false for one-time events

    @Column(name = "is_mandatory")
    private Boolean isMandatory = true;

    @Column(name = "requires_training")
    private Boolean requiresTraining = false;

    @Column(name = "compensation_hours")
    private Double compensationHours = 0.0; // Extra hours credit

    @Column(name = "max_capacity")
    private Integer maxCapacity = 1; // Number of staff needed

    @Column(name = "priority")
    private Integer priority = 3; // 1=High, 2=Medium, 3=Low

    // ========================================================================
    // STATUS
    // ========================================================================

    @Column(nullable = false)
    private Boolean active = true;

    @Column(name = "completed")
    private Boolean completed = false;

    @Column(name = "confirmed_by_staff")
    private Boolean confirmedByStaff = false;

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Get the display name for this assignment
     */
    public String getDisplayName() {
        if (dutyType == DutyType.CUSTOM && customDutyName != null && !customDutyName.isEmpty()) {
            return customDutyName;
        }
        return dutyType != null ? dutyType.getDisplayName() : "Unknown Duty";
    }

    /**
     * Get assigned person's name
     * Safe for lazy-loaded entities
     */
    public String getAssignedPersonName() {
        if (teacher != null) {
            try {
                return teacher.getName();
            } catch (Exception e) {
                // Lazy initialization failed, return fallback
                return "Teacher (ID: " + teacher.getId() + ")";
            }
        }
        return staffName != null ? staffName : "Unassigned";
    }

    /**
     * Get staff type display
     * Safe for lazy-loaded entities
     */
    public String getStaffTypeDisplay() {
        if (teacher != null) {
            try {
                // Try to access teacher to check if it's initialized
                teacher.getName();
                return "Teacher";
            } catch (Exception e) {
                // Lazy initialization failed
                return "Teacher";
            }
        }
        return staffType != null ? staffType.replace("_", " ") : "Staff";
    }

    /**
     * Get time range display
     */
    public String getTimeRange() {
        if (startTime != null && endTime != null) {
            return startTime.toString() + " - " + endTime.toString();
        }
        return "Not scheduled";
    }

    /**
     * Get date display
     */
    public String getDateDisplay() {
        if (eventDate != null) {
            return eventDate.toString();
        }
        if (dayOfWeek != null) {
            return dayOfWeek.getDisplayName() + " (Recurring)";
        }
        return "Not scheduled";
    }

    /**
     * Check if this is a daily duty
     */
    public boolean isDaily() {
        return Boolean.TRUE.equals(isRecurring) && dayOfWeek != null;
    }

    /**
     * Check if this is a special event
     */
    public boolean isSpecialEvent() {
        return !Boolean.TRUE.equals(isRecurring) && eventDate != null;
    }

    /**
     * Check if duty is currently in effect
     */
    public boolean isCurrentlyEffective() {
        LocalDate now = LocalDate.now();
        if (effectiveFrom != null && now.isBefore(effectiveFrom)) {
            return false;
        }
        if (effectiveUntil != null && now.isAfter(effectiveUntil)) {
            return false;
        }
        return Boolean.TRUE.equals(active);
    }

    /**
     * Get priority display
     */
    public String getPriorityDisplay() {
        if (priority == null) return "Medium";
        switch (priority) {
            case 1: return "High";
            case 2: return "Medium";
            case 3: return "Low";
            default: return "Medium";
        }
    }
}
