package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Period Timer Entity
 * Defines period start/end times and attendance window rules from the master schedule.
 *
 * Features:
 * - Period-based scheduling
 * - Configurable attendance windows
 * - Auto mark absent after window closes
 * - Day-of-week filtering
 * - Academic year association
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025 - QR Attendance System Phase 1
 */
@Entity
@Table(name = "period_timers", indexes = {
    @Index(name = "idx_period_timer_period", columnList = "period_number"),
    @Index(name = "idx_period_timer_active", columnList = "active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodTimer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Associated academic year (null = applies to all years)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    @ToString.Exclude
    private AcademicYear academicYear;

    /**
     * Period number (1, 2, 3, etc.)
     * Special values:
     * - 0: Before school/arrival
     * - 99: After school/dismissal
     * - -1: Lunch period
     */
    @Column(name = "period_number", nullable = false)
    private Integer periodNumber;

    /**
     * Display name for this period
     * Examples: "Period 1", "Lunch A", "Advisory"
     */
    @Column(name = "period_name", length = 100)
    private String periodName;

    /**
     * Period start time
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * Period end time
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Attendance window in minutes from period start
     * Example: 15 minutes means attendance can be taken from 8:00 to 8:15 for an 8:00 period
     * Default: 15 minutes
     */
    @Column(name = "attendance_window_minutes")
    @Builder.Default
    private Integer attendanceWindowMinutes = 15;

    /**
     * Automatically mark students absent if they don't scan within attendance window
     * Default: true
     */
    @Column(name = "auto_mark_absent")
    @Builder.Default
    private Boolean autoMarkAbsent = true;

    /**
     * Days of week this period applies to (comma-separated)
     * Examples: "MON,TUE,WED,THU,FRI" or "MON,WED,FRI"
     * Default: All weekdays
     */
    @Column(name = "days_of_week", length = 50)
    @Builder.Default
    private String daysOfWeek = "MON,TUE,WED,THU,FRI";

    /**
     * Is this period timer currently active?
     * Set to false to disable without deleting
     */
    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    /**
     * Record creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Record last update timestamp
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * JPA lifecycle callback - set creation timestamp
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * JPA lifecycle callback - update timestamp on modification
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Get attendance window end time
     */
    public LocalTime getAttendanceWindowEndTime() {
        return startTime.plusMinutes(attendanceWindowMinutes != null ? attendanceWindowMinutes : 15);
    }

    /**
     * Check if current time is within attendance window
     */
    public boolean isWithinAttendanceWindow(LocalTime currentTime) {
        LocalTime windowEnd = getAttendanceWindowEndTime();
        return !currentTime.isBefore(startTime) && !currentTime.isAfter(windowEnd);
    }

    /**
     * Check if current time is within the actual period
     */
    public boolean isWithinPeriod(LocalTime currentTime) {
        return !currentTime.isBefore(startTime) && !currentTime.isAfter(endTime);
    }

    /**
     * Check if this period applies to a specific date
     */
    public boolean appliesTo(LocalDate date) {
        if (!active) {
            return false;
        }

        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            return true; // No day restriction
        }

        String dayOfWeek = date.getDayOfWeek().toString().substring(0, 3).toUpperCase();
        return daysOfWeek.contains(dayOfWeek);
    }

    /**
     * Get period duration in minutes
     */
    public int getDurationMinutes() {
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Get formatted time range
     */
    public String getFormattedTimeRange() {
        return String.format("%s - %s", formatTime(startTime), formatTime(endTime));
    }

    /**
     * Format time in 12-hour format
     */
    private String formatTime(LocalTime time) {
        int hour = time.getHour();
        int minute = time.getMinute();
        String amPm = hour >= 12 ? "PM" : "AM";
        if (hour > 12) hour -= 12;
        if (hour == 0) hour = 12;
        return String.format("%d:%02d %s", hour, minute, amPm);
    }

    /**
     * Get display label for this period
     */
    public String getDisplayLabel() {
        if (periodName != null && !periodName.isEmpty()) {
            return periodName + " (" + getFormattedTimeRange() + ")";
        }
        return "Period " + periodNumber + " (" + getFormattedTimeRange() + ")";
    }
}
