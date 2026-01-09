package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.dto.LunchCohort;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a single lunch wave/period in a multiple-lunch schedule.
 *
 * Real-world examples:
 * - Weeki Wachee HS: 3 lunch waves (Lunch 1, 2, 3) for 1,600 students
 * - Parrott MS: Grade-level lunches (6th, 7th, 8th grade)
 *
 * Key features:
 * - Capacity tracking (typical: 250 students per cafeteria wave)
 * - Time slot definition (e.g., 10:04-10:34)
 * - Optional grade-level restrictions
 * - Wave ordering for split periods (Period 4A/Lunch 1/4B)
 *
 * Phase 5A: Multiple Rotating Lunch Periods
 * Date: December 1, 2025
 */
@Entity
@Table(name = "lunch_waves")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LunchWave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The schedule this lunch wave belongs to
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    /**
     * Display name for this lunch wave
     * Examples: "Lunch 1", "Lunch 2", "Lunch 3", "6th Grade Lunch"
     */
    @Column(name = "wave_name", nullable = false, length = 50)
    private String waveName;

    /**
     * Ordinal position of this wave (1, 2, 3)
     * Used for split period sequencing (4A → Lunch 1 → 4B → Lunch 2 → 4C)
     */
    @Column(name = "wave_order", nullable = false)
    private Integer waveOrder;

    /**
     * Start time of this lunch period
     * Example: 10:04 for Weeki Wachee's Lunch 1
     */
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /**
     * End time of this lunch period
     * Example: 10:34 for Weeki Wachee's Lunch 1 (30-minute duration)
     */
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    /**
     * Maximum number of students that can be assigned to this wave
     * Typically limited by cafeteria seating capacity (250-300)
     *
     * Real-world example:
     * Weeki Wachee: 1,600 students ÷ 3 waves ≈ 533 students per wave
     * But cafeteria capacity may limit to 250, requiring more efficient assignment
     */
    @Column(name = "max_capacity")
    @Builder.Default
    private Integer maxCapacity = 250;

    /**
     * Current number of students assigned to this wave
     * Calculated by counting StudentLunchAssignment records
     */
    @Column(name = "current_assignments")
    @Builder.Default
    private Integer currentAssignments = 0;

    /**
     * Optional: Restrict this lunch wave to specific grade level
     * Example: Parrott MS uses 6, 7, 8 for each wave
     * null = all grades allowed (like Weeki Wachee)
     */
    @Column(name = "grade_level_restriction")
    private Integer gradeLevelRestriction;

    /**
     * Whether this lunch wave is currently active/enabled
     */
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Optional: Room where lunch is served (for split lunch locations)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lunch_room_id")
    private Room lunchRoom;

    /**
     * Notes about this lunch wave
     * Examples: "Athletes priority", "Special dietary needs", etc.
     */
    @Column(name = "notes", length = 500)
    private String notes;

    /**
     * Cohorts for spatial cohesion tracking
     */
    @Transient
    @Builder.Default
    private List<LunchCohort> cohorts = new ArrayList<>();

    /**
     * Spatial cohesion score for this wave
     */
    @Transient
    @Builder.Default
    private Double spatialCohesionScore = 0.0;

    // ========== Helper Methods ==========

    /**
     * Check if this lunch wave is at full capacity
     */
    public boolean isAtCapacity() {
        if (maxCapacity == null || currentAssignments == null) {
            return false;
        }
        return currentAssignments >= maxCapacity;
    }

    /**
     * Get number of available seats remaining
     */
    public int getAvailableSeats() {
        if (maxCapacity == null) {
            return Integer.MAX_VALUE; // No limit
        }
        if (currentAssignments == null) {
            return maxCapacity;
        }
        return Math.max(0, maxCapacity - currentAssignments);
    }

    /**
     * Calculate utilization percentage
     * Returns 0-100
     */
    public double getUtilizationPercentage() {
        if (maxCapacity == null || maxCapacity == 0 || currentAssignments == null) {
            return 0.0;
        }
        return (currentAssignments * 100.0) / maxCapacity;
    }

    /**
     * Get duration of this lunch period in minutes
     */
    public int getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Check if a student's grade level is eligible for this wave
     * @param studentGradeLevel Grade level (9-12 for high school, 6-8 for middle school)
     * @return true if eligible or no restriction, false if restricted
     */
    public boolean isGradeLevelEligible(Integer studentGradeLevel) {
        if (gradeLevelRestriction == null) {
            return true; // No restriction, all grades allowed
        }
        if (studentGradeLevel == null) {
            return true; // Unknown grade, allow by default
        }
        return studentGradeLevel.equals(gradeLevelRestriction);
    }

    /**
     * Check if this wave can accept another student
     */
    public boolean canAcceptStudent() {
        return isActive && !isAtCapacity();
    }

    /**
     * Increment the current assignment count
     * Called when a student is assigned to this wave
     */
    public void addAssignment() {
        if (currentAssignments == null) {
            currentAssignments = 0;
        }
        currentAssignments++;
    }

    /**
     * Decrement the current assignment count
     * Called when a student is removed from this wave
     */
    public void removeAssignment() {
        if (currentAssignments != null && currentAssignments > 0) {
            currentAssignments--;
        }
    }

    /**
     * Get a formatted time range string
     * Example: "10:04 - 10:34"
     */
    public String getTimeRangeFormatted() {
        if (startTime == null || endTime == null) {
            return "Not set";
        }
        return String.format("%s - %s", startTime, endTime);
    }

    /**
     * Get a display string with capacity info
     * Example: "Lunch 1 (250/250 students, 100% full)"
     */
    public String getDisplayStringWithCapacity() {
        return String.format("%s (%d/%d students, %.0f%% full)",
            waveName != null ? waveName : "Unnamed",
            currentAssignments != null ? currentAssignments : 0,
            maxCapacity != null ? maxCapacity : 0,
            getUtilizationPercentage()
        );
    }

    /**
     * Get a short summary for UI display
     * Example: "Lunch 1: 10:04-10:34 (250 seats)"
     */
    public String getSummary() {
        return String.format("%s: %s (%d seats)",
            waveName != null ? waveName : "Unnamed",
            getTimeRangeFormatted(),
            maxCapacity != null ? maxCapacity : 0
        );
    }

    /**
     * Get wave number (alias for waveOrder)
     */
    public Integer getNumber() {
        return waveOrder;
    }

    /**
     * Get current size (alias for currentAssignments)
     */
    public Integer getCurrentSize() {
        return currentAssignments != null ? currentAssignments : 0;
    }

    /**
     * Get all students assigned to this wave
     */
    public List<Student> getAllStudents() {
        if (cohorts == null || cohorts.isEmpty()) {
            return new ArrayList<>();
        }
        return cohorts.stream()
            .flatMap(cohort -> cohort.getStudentIds().stream())
            .map(studentId -> {
                Student student = new Student();
                student.setId(studentId);
                return student;
            })
            .collect(Collectors.toList());
    }

    /**
     * Get count of cohorts
     */
    public int getCohortCount() {
        return cohorts != null ? cohorts.size() : 0;
    }

    /**
     * Get cohorts list
     */
    public List<LunchCohort> getCohorts() {
        return cohorts != null ? cohorts : new ArrayList<>();
    }

    /**
     * Add a cohort to this wave
     */
    public void addCohort(LunchCohort cohort) {
        if (this.cohorts == null) {
            this.cohorts = new ArrayList<>();
        }
        this.cohorts.add(cohort);
        // Update current assignments based on cohort size
        if (cohort.getStudentIds() != null) {
            this.currentAssignments = (this.currentAssignments != null ? this.currentAssignments : 0)
                + cohort.getStudentIds().size();
        }
    }

    /**
     * Get spatial cohesion score
     */
    public Double getSpatialCohesionScore() {
        return spatialCohesionScore != null ? spatialCohesionScore : 0.0;
    }

    /**
     * Set spatial cohesion score
     */
    public void setSpatialCohesionScore(Double score) {
        this.spatialCohesionScore = score;
    }

    @Override
    public String toString() {
        return String.format("LunchWave{id=%d, name='%s', order=%d, time=%s, capacity=%d/%d, gradeRestriction=%s}",
            id,
            waveName,
            waveOrder,
            getTimeRangeFormatted(),
            currentAssignments != null ? currentAssignments : 0,
            maxCapacity != null ? maxCapacity : 0,
            gradeLevelRestriction != null ? gradeLevelRestriction + "th grade" : "All grades"
        );
    }
}
