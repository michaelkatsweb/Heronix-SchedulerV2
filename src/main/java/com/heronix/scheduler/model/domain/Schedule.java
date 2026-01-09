package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.SchedulePeriod;
import com.heronix.scheduler.model.enums.ScheduleStatus;
import com.heronix.scheduler.model.enums.ScheduleType;
import com.heronix.scheduler.model.enums.SlotStatus;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║ SCHEDULE ENTITY ║
 * ║ Master Schedule Domain Model ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 * 
 * Location: src/main/java/com/eduscheduler/model/domain/Schedule.java
 * 
 * Purpose:
 * - Represents a complete schedule (master, weekly, or daily)
 * - Contains collection of schedule slots
 * - Tracks optimization metrics and status
 * - Supports multiple schedule types and periods
 * 
 * Updates:
 * ✓ Added getStartTime() and getEndTime() methods
 * ✓ Enhanced with time range support
 * 
 * @author Heronix Scheduling System Team
 * @version 3.1.0 - ENHANCED
 * @since 2025-10-21
 */
@Entity
@Table(name = "schedules")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchedulePeriod period;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleType scheduleType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleStatus status = ScheduleStatus.DRAFT;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private LocalDate createdDate;
    private String createdBy;
    private LocalDate lastModifiedDate;
    private String lastModifiedBy;

    @Lob
    private String notes;

    /**
     * Schedule slots - Using LAZY fetch by default
     * Use repository methods with JOIN FETCH when accessing slots outside transactions
     * to prevent LazyInitializationException
     */
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @ToString.Exclude
    private List<ScheduleSlot> slots = new ArrayList<>();

    private Double optimizationScore;
    private Double efficiencyRate;
    private Double teacherUtilization;
    private Double roomUtilization;

    private Integer totalConflicts = 0;
    private Integer resolvedConflicts = 0;

    @Version
    private Long version;

    @Column(name = "is_active")
    private Boolean active = true;

    @Column(name = "quality_score")
    private Double qualityScore = 0.0;

    // ════════════════════════════════════════════════════════════════════════
    // TIME RANGE CONFIGURATION (Added in v3.1.0)
    // ════════════════════════════════════════════════════════════════════════
    @Transient
    private Schedule previousVersion;

    public Schedule getPreviousVersion() {
        return previousVersion;
    }

    public void setPreviousVersion(Schedule previousVersion) {
        this.previousVersion = previousVersion;
    }

    /**
     * Optional: Store custom start time in database
     * If not set, defaults to 7:00 AM
     */
    @Column(name = "day_start_time")
    private LocalTime dayStartTime;

    /**
     * Optional: Store custom end time in database
     * If not set, defaults to 4:00 PM
     */
    @Column(name = "day_end_time")
    private LocalTime dayEndTime;

    /**
     * Optional: Store slot duration in minutes
     * If not set, defaults to 45 minutes
     */
    @Column(name = "slot_duration_minutes")
    private Integer slotDurationMinutes;

    // ════════════════════════════════════════════════════════════════════════
    // EXISTING METHODS (Preserved from original)
    // ════════════════════════════════════════════════════════════════════════

    public String getScheduleName() {
        return name;
    }

    public void setScheduleName(String scheduleName) {
        this.name = scheduleName;
    }

    public int getUnresolvedConflicts() {
        return totalConflicts - resolvedConflicts;
    }

    public boolean isPublished() {
        return ScheduleStatus.PUBLISHED.equals(status);
    }

    public boolean isActiveOn(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    public void addSlot(ScheduleSlot slot) {
        slots.add(slot);
        slot.setSchedule(this);
    }

    public void removeSlot(ScheduleSlot slot) {
        slots.remove(slot);
        slot.setSchedule(null);
    }

    /**
     * Get the total number of slots in this schedule
     * 
     * @return int number of slots
     */
    public int getSlotCount() {
        return slots != null ? slots.size() : 0;
    }

    // ========================================================================
    // ADD TO SCHEDULE.JAVA - NEW METHODS
    // ========================================================================

    /**
     * Count total conflicts in the schedule
     * 
     * @return number of slots with CONFLICT status
     */
    public long getConflictCount() {
        if (slots == null || slots.isEmpty()) {
            return 0;
        }

        return slots.stream()
                .filter(slot -> slot.getStatus() == SlotStatus.CONFLICT)
                .count();
    }

    /**
     * Create a deep copy of this schedule
     * Used by optimization algorithms that need to test modifications
     * 
     * @return a new Schedule object with copied data
     */
    public Schedule copy() {
        Schedule copy = new Schedule();

        // Copy basic fields
        copy.setName(this.name);
        copy.setScheduleType(this.scheduleType);
        copy.setStartDate(this.startDate);
        copy.setEndDate(this.endDate);
        copy.setStatus(this.status);
        copy.setActive(this.active);
        copy.setQualityScore(this.qualityScore);
        copy.setEfficiencyRate(this.efficiencyRate);

        // Deep copy slots
        if (this.slots != null) {
            List<ScheduleSlot> copiedSlots = new ArrayList<>();
            for (ScheduleSlot slot : this.slots) {
                ScheduleSlot newSlot = new ScheduleSlot();
                newSlot.setDayOfWeek(slot.getDayOfWeek());
                newSlot.setStartTime(slot.getStartTime());
                newSlot.setEndTime(slot.getEndTime());
                newSlot.setTeacher(slot.getTeacher());
                newSlot.setCourse(slot.getCourse());
                newSlot.setRoom(slot.getRoom());
                newSlot.setStatus(slot.getStatus());
                newSlot.setSchedule(copy); // Link back to new schedule
                copiedSlots.add(newSlot);
            }
            copy.setSlots(copiedSlots);
        }

        return copy;
    }

    /**
     * Validate schedule has no major constraint violations
     * 
     * @return true if schedule is valid, false if there are violations
     */
    public boolean isValid() {
        if (slots == null || slots.isEmpty()) {
            return false;
        }

        // Check for teacher conflicts
        Map<String, List<ScheduleSlot>> byTeacherAndTime = new HashMap<>();
        for (ScheduleSlot slot : slots) {
            if (slot.getTeacher() != null) {
                String key = slot.getTeacher().getId() + "_" +
                        slot.getDayOfWeek() + "_" +
                        slot.getStartTime();
                byTeacherAndTime.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
            }
        }

        // If any teacher has multiple slots at same time = conflict
        boolean hasTeacherConflicts = byTeacherAndTime.values().stream()
                .anyMatch(slotList -> slotList.size() > 1);

        if (hasTeacherConflicts) {
            return false;
        }

        // Check for room conflicts
        Map<String, List<ScheduleSlot>> byRoomAndTime = new HashMap<>();
        for (ScheduleSlot slot : slots) {
            if (slot.getRoom() != null) {
                String key = slot.getRoom().getId() + "_" +
                        slot.getDayOfWeek() + "_" +
                        slot.getStartTime();
                byRoomAndTime.computeIfAbsent(key, k -> new ArrayList<>()).add(slot);
            }
        }

        boolean hasRoomConflicts = byRoomAndTime.values().stream()
                .anyMatch(slotList -> slotList.size() > 1);

        return !hasRoomConflicts;
    }

    // ════════════════════════════════════════════════════════════════════════
    // NEW TIME RANGE METHODS (Added in v3.1.0) ✨
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Get the start time for this schedule's school day
     * 
     * @return LocalTime - defaults to 7:00 AM if not configured
     */
    public LocalTime getStartTime() {
        if (dayStartTime != null) {
            return dayStartTime;
        }

        // Default school start time: 7:00 AM
        return LocalTime.of(7, 0);
    }

    /**
     * Set a custom start time for this schedule
     * 
     * @param startTime LocalTime for school day start
     */
    public void setStartTime(LocalTime startTime) {
        this.dayStartTime = startTime;
    }

    /**
     * Get the end time for this schedule's school day
     * 
     * @return LocalTime - defaults to 4:00 PM if not configured
     */
    public LocalTime getEndTime() {
        if (dayEndTime != null) {
            return dayEndTime;
        }

        // Default school end time: 4:00 PM (16:00)
        return LocalTime.of(16, 0);
    }

    /**
     * Set a custom end time for this schedule
     * 
     * @param endTime LocalTime for school day end
     */
    public void setEndTime(LocalTime endTime) {
        this.dayEndTime = endTime;
    }

    /**
     * Get the slot duration in minutes
     * 
     * @return int - defaults to 45 minutes if not configured
     */
    public int getSlotDuration() {
        if (slotDurationMinutes != null && slotDurationMinutes > 0) {
            return slotDurationMinutes;
        }

        // Default period length: 45 minutes (typical class period)
        return 45;
    }

    /**
     * Set a custom slot duration
     * 
     * @param durationMinutes int slot duration in minutes
     */
    public void setSlotDuration(int durationMinutes) {
        this.slotDurationMinutes = durationMinutes;
    }

    /**
     * Calculate total number of time slots in a school day
     * Based on start time, end time, and slot duration
     * 
     * @return int number of slots per day
     */
    public int getTotalSlotsPerDay() {
        LocalTime start = getStartTime();
        LocalTime end = getEndTime();
        int duration = getSlotDuration();

        int totalMinutes = (end.getHour() * 60 + end.getMinute()) -
                (start.getHour() * 60 + start.getMinute());

        return totalMinutes / duration;
    }

    /**
     * Check if a given time falls within this schedule's time range
     * 
     * @param time LocalTime to check
     * @return boolean true if time is within schedule range
     */
    public boolean isTimeInRange(LocalTime time) {
        if (time == null) {
            return false;
        }

        LocalTime start = getStartTime();
        LocalTime end = getEndTime();

        return !time.isBefore(start) && !time.isAfter(end);
    }

    /**
     * Get all time slots for this schedule as a list
     * Useful for generating schedule grids
     * 
     * @return List<LocalTime> all time slot start times
     */
    public List<LocalTime> getAllTimeSlots() {
        List<LocalTime> timeSlots = new ArrayList<>();
        LocalTime current = getStartTime();
        LocalTime end = getEndTime();
        int duration = getSlotDuration();

        while (current.isBefore(end)) {
            timeSlots.add(current);
            current = current.plusMinutes(duration);
        }

        return timeSlots;
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILITY METHODS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Get a human-readable summary of this schedule
     * Note: This method safely handles lazy-loaded slots
     *
     * @return String schedule summary
     */
    public String getSummary() {
        try {
            int slotCount = (slots != null && org.hibernate.Hibernate.isInitialized(slots)) ? slots.size() : 0;
            return String.format("%s | %s to %s | %d slots | Status: %s",
                    name,
                    startDate,
                    endDate,
                    slotCount,
                    status);
        } catch (Exception e) {
            // If slots collection cannot be accessed, return summary without slot count
            return String.format("%s | %s to %s | Status: %s",
                    name,
                    startDate,
                    endDate,
                    status);
        }
    }

    /**
     * Get optimization quality as percentage
     * 
     * @return String formatted percentage
     */
    public String getOptimizationScoreFormatted() {
        if (optimizationScore == null) {
            return "N/A";
        }
        return String.format("%.1f%%", optimizationScore * 100);
    }

    /**
     * Get conflict rate as percentage
     * 
     * @return String formatted percentage
     */
    public String getConflictRate() {
        if (totalConflicts == null || totalConflicts == 0) {
            return "0%";
        }
        double rate = (getUnresolvedConflicts() * 100.0) / totalConflicts;
        return String.format("%.1f%%", rate);
    }

    /**
     * Check if schedule needs attention (has unresolved conflicts or low score)
     * 
     * @return boolean true if schedule needs review
     */
    public boolean needsAttention() {
        if (getUnresolvedConflicts() > 0) {
            return true;
        }

        if (optimizationScore != null && optimizationScore < 0.85) {
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        // Avoid lazy loading exception by not accessing slots collection
        // toString() is often called outside of Hibernate session context
        return String.format("Schedule[id=%d, name='%s', type=%s, status=%s]",
                id, name, scheduleType, status);
    }
}