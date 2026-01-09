package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Lunch Period Entity - ENHANCED VERSION
 * Location: src/main/java/com/eduscheduler/model/domain/LunchPeriod.java
 * 
 * Represents a lunch rotation period with full compatibility for existing service layer.
 * 
 * @author Heronix Scheduling System Team
 * @version 2.0.0 - Compatible with LunchPeriodServiceImpl
 * @since 2025-10-18
 */
@Entity
@Table(name = "lunch_periods")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LunchPeriod {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Lunch period identifier
     * Examples: "Lunch 1", "Lunch 2", "Lunch 3", "A Lunch", "B Lunch"
     */
    @Column(nullable = false, unique = true)
    private String name;
    
    /**
     * Display order for UI
     */
    @Column(nullable = false)
    private Integer displayOrder;
    
    /**
     * Start time of lunch period
     */
    @Column(nullable = false)
    private LocalTime startTime;
    
    /**
     * End time of lunch period
     */
    @Column(nullable = false)
    private LocalTime endTime;
    
    /**
     * Duration in minutes (calculated from start/end time)
     */
    @Column(nullable = false)
    private Integer durationMinutes;
    
    /**
     * Maximum capacity for this lunch period
     * Also accessible as maxStudents for compatibility
     */
    @Column(nullable = false)
    private Integer maxCapacity;
    
    /**
     * Current number of students assigned
     */
    @Column(nullable = false)
    private Integer currentCount = 0;

    /**
     * Version field for optimistic locking to prevent race conditions
     * on concurrent lunch period assignments
     */
    @Version
    private Long version;

    /**
     * Is this lunch period active?
     */
    @Column(nullable = false)
    private Boolean active = true;
    
    /**
     * Lunch group identifier (for grouping related lunches)
     * Example: "Period 4/5" (all Period 4/5 lunches share this group)
     */
    @Column
    private String lunchGroup;
    
    /**
     * Location/venue for this lunch
     * Example: "Cafeteria", "Outdoor Area", "Commons"
     */
    @Column
    private String location;
    
    /**
     * Grade levels assigned to this lunch (comma-separated)
     * Example: "9,10" or "11,12"
     */
    @Column
    private String gradeLevels;
    
    /**
     * Day of week (1=Monday, 5=Friday, null=all days)
     * For rotating schedules where lunch times vary by day
     */
    @Column
    private Integer dayOfWeek;
    
    /**
     * Priority for assignment (higher = assign first)
     * Used when balancing lunch assignments
     */
    @Column
    private Integer priority = 5;
    
    /**
     * Notes or special instructions
     */
    @Column(length = 500)
    private String notes;
    
    /**
     * Students assigned to this lunch period
     * Unidirectional relationship - Student entities store lunch_wave_id instead
     */
    @ManyToMany
    @JoinTable(
        name = "lunch_period_students",
        joinColumns = @JoinColumn(name = "lunch_period_id"),
        inverseJoinColumns = @JoinColumn(name = "student_id")
    )
    private Set<Student> students = new HashSet<>();
    
    /**
     * Teachers supervising this lunch period
     */
    @ManyToMany
    @JoinTable(
        name = "lunch_supervision",
        joinColumns = @JoinColumn(name = "lunch_period_id"),
        inverseJoinColumns = @JoinColumn(name = "teacher_id")
    )
    private Set<Teacher> supervisingTeachers = new HashSet<>();
    
    // ========================================================================
    // COMPATIBILITY METHODS (for existing LunchPeriodServiceImpl)
    // ========================================================================
    
    /**
     * Calculate duration from start and end times
     * @return Duration in minutes
     */
    public int calculateDuration() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return (int) Duration.between(startTime, endTime).toMinutes();
    }
    
    /**
     * Get max students (alias for maxCapacity)
     * @return Maximum student capacity
     */
    public Integer getMaxStudents() {
        return maxCapacity;
    }
    
    /**
     * Set max students (alias for maxCapacity)
     */
    public void setMaxStudents(Integer maxStudents) {
        this.maxCapacity = maxStudents;
    }
    
    /**
     * Check if lunch period is active
     * @return true if active
     */
    public boolean isActive() {
        return active != null && active;
    }
    
    // ========================================================================
    // HELPER METHODS
    // ========================================================================
    
    /**
     * Calculate utilization percentage
     * @return Percentage of capacity used (0-100)
     */
    public double getUtilizationRate() {
        if (maxCapacity == null || maxCapacity == 0) {
            return 0.0;
        }
        return (currentCount * 100.0) / maxCapacity;
    }
    
    /**
     * Check if lunch period is full
     * @return true if at or over capacity
     */
    public boolean isFull() {
        return currentCount != null && maxCapacity != null && currentCount >= maxCapacity;
    }
    
    /**
     * Get remaining capacity
     * @return Number of students that can still be assigned
     */
    public int getRemainingCapacity() {
        if (maxCapacity == null || currentCount == null) {
            return 0;
        }
        return Math.max(0, maxCapacity - currentCount);
    }
    
    /**
     * Add a student to this lunch period
     * @param student Student to add
     * @return true if added successfully, false if full
     */
    public boolean addStudent(Student student) {
        if (isFull()) {
            return false;
        }
        
        if (students.add(student)) {
            currentCount++;
            return true;
        }
        
        return false;
    }
    
    /**
     * Remove a student from this lunch period
     * @param student Student to remove
     */
    public void removeStudent(Student student) {
        if (students.remove(student)) {
            currentCount = Math.max(0, currentCount - 1);
        }
    }
    
    /**
     * Get duration as formatted string
     * @return Duration like "30 minutes"
     */
    public String getDurationFormatted() {
        if (durationMinutes == null) {
            durationMinutes = calculateDuration();
        }
        return durationMinutes + " minutes";
    }
    
    /**
     * Get time range as formatted string
     * @return Time range like "10:04 - 10:34"
     */
    public String getTimeRangeFormatted() {
        return startTime.toString() + " - " + endTime.toString();
    }
    
    /**
     * Parse grade levels string into array
     * @return Array of grade levels
     */
    public String[] getGradeLevelsArray() {
        if (gradeLevels == null || gradeLevels.isEmpty()) {
            return new String[0];
        }
        return gradeLevels.split(",");
    }
    
    /**
     * Check if this lunch is for a specific grade level
     */
    public boolean isForGradeLevel(String gradeLevel) {
        if (gradeLevels == null || gradeLevels.isEmpty()) {
            return true; // Available to all if not specified
        }
        
        String[] levels = getGradeLevelsArray();
        for (String level : levels) {
            if (level.trim().equals(gradeLevel)) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - %d/%d students", 
            name, getTimeRangeFormatted(), currentCount, maxCapacity);
    }
    
    // ========================================================================
    // LIFECYCLE CALLBACKS
    // ========================================================================
    
    /**
     * Called before persisting - ensure duration is calculated
     */
    @PrePersist
    @PreUpdate
    public void prePersist() {
        if (durationMinutes == null || durationMinutes == 0) {
            durationMinutes = calculateDuration();
        }
        if (priority == null) {
            priority = 5;
        }
        if (currentCount == null) {
            currentCount = 0;
        }
        if (active == null) {
            active = true;
        }
    }
}