package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.DayType;
import com.heronix.scheduler.model.enums.SlotStatus;
import com.heronix.scheduler.model.enums.SpecialEventType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.entity.PlanningPin;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Schedule Slot - Planning Entity for OptaPlanner
 * Location: src/main/java/com/eduscheduler/model/domain/ScheduleSlot.java
 * 
 * ✅ PHASE 1 FIX: Changed students to EAGER fetch to prevent
 * LazyInitializationException
 * ✅ IMPORTS SlotStatus enum
 * 
 * @author Heronix Scheduling System Team
 * @version 3.0.0 - PHASE 1 FIXED
 * @since 2025-10-11
 */
@Entity
@Table(name = "schedule_slots", indexes = {
        @Index(name = "idx_schedule_slot_day", columnList = "day_of_week"),
        @Index(name = "idx_schedule_slot_time", columnList = "start_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@PlanningEntity
public class ScheduleSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PlanningId
    private Long id;

    // ========== PLANNING VARIABLES (OptaPlanner will assign these) ==========

    /**
     * PLANNING VARIABLE: Teacher assigned to this slot
     * OptaPlanner will choose from teacherRange value range
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id")
    @PlanningVariable(valueRangeProviderRefs = "teacherRange")
    private Teacher teacher;

    /**
     * PLANNING VARIABLE: Room assigned to this slot
     * OptaPlanner will choose from roomRange value range
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    @PlanningVariable(valueRangeProviderRefs = "roomRange")
    private Room room;

    /**
     * PLANNING VARIABLE: Time slot assigned
     * OptaPlanner will choose from timeSlotRange value range
     */
    @Transient // Not persisted directly, we store dayOfWeek, startTime, endTime instead
    @PlanningVariable(valueRangeProviderRefs = "timeSlotRange")
    private TimeSlot timeSlot;

    // ========== PROBLEM FACTS (Fixed during solving) ==========

    /**
     * The course being taught in this slot (FIXED - not changed by solver)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    /**
     * The schedule this slot belongs to (FIXED)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    // Transient field - calculated from students list, not persisted to DB
    @Transient
    private Integer enrolledStudents;

    /**
     * Get enrolled student count from the students relationship
     * This is the ACTUAL count from the database relationship
     */
    public Integer getEnrolledStudents() {
        return students != null ? students.size() : 0;
    }

    /**
     * Setter for compatibility (does nothing - count is derived from students list)
     */
    public void setEnrolledStudents(Integer count) {
        // Intentionally empty - this is a derived field
    }

    // ========== TIME INFORMATION (Synced with TimeSlot) ==========

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "period_number")
    private Integer periodNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_type")
    private DayType dayType = DayType.DAILY;

    // ========== ADDITIONAL FIELDS ==========

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substitute_teacher_id")
    private Teacher substituteTeacher;

    /**
     * Students enrolled in this schedule slot
     * Use @Transactional or JOIN FETCH queries to load students when needed
     */
    @ManyToMany
    @JoinTable(name = "schedule_slot_students", joinColumns = @JoinColumn(name = "schedule_slot_id"), inverseJoinColumns = @JoinColumn(name = "student_id"))
    private List<Student> students = new ArrayList<>();

    /**
     * Co-teachers assigned to this schedule slot
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "schedule_slot_co_teachers",
        joinColumns = @JoinColumn(name = "schedule_slot_id"),
        inverseJoinColumns = @JoinColumn(name = "co_teacher_id")
    )
    private List<CoTeacher> coTeachers = new ArrayList<>();

    @Column(name = "has_conflict")
    private Boolean hasConflict = false;

    @Column(name = "conflict_reason", columnDefinition = "TEXT")
    private String conflictReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SlotStatus status = SlotStatus.ACTIVE;

    /**
     * ✅ INCREMENTAL SOLVING: Pin flag to lock this slot during optimization
     * When true, OptaPlanner will not change this slot's planning variables
     * (teacher, room, timeSlot). Useful for preserving user's manual changes.
     */
    @Column(name = "pinned")
    @PlanningPin
    private Boolean pinned = false;

    @Column(name = "pinned_by")
    private String pinnedBy;

    @Column(name = "pinned_at")
    private java.time.LocalDateTime pinnedAt;

    /**
     * Special event flag - indicates this slot is for a special event
     * (assembly, testing, field trip, etc.) rather than a regular class
     */
    @Column(name = "is_special_event")
    private Boolean isSpecialEvent = false;

    /**
     * Type of special event (if isSpecialEvent is true)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "special_event_type")
    private SpecialEventType specialEventType;

    /**
     * Description of the special event
     */
    @Column(name = "special_event_description", columnDefinition = "TEXT")
    private String specialEventDescription;

    @Lob
    private String notes;

    @Column(name = "is_lunch_period")
    private Boolean isLunchPeriod = false;

    @Column(name = "lunch_wave_number")
    private Integer lunchWaveNumber;

    // ========== HELPER METHODS ==========

    /**
     * Check if this slot is assigned (has teacher, course, and room)
     */
    public boolean isAssigned() {
        return teacher != null && course != null && room != null;
    }

    /**
     * Check if this slot is complete (assigned + has time info)
     */
    public boolean isComplete() {
        return isAssigned() && dayOfWeek != null && startTime != null && endTime != null;
    }

    /**
     * Get student count - SAFE even with eager loading
     */
    public int getStudentCount() {
        try {
            return students != null ? students.size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Check if overcapacity
     */
    public boolean isOvercapacity() {
        if (room == null || students == null) {
            return false;
        }
        Integer roomCapacity = room.getCapacity();
        return roomCapacity != null && students.size() > roomCapacity;
    }

    /**
     * Sync with TimeSlot object (when OptaPlanner assigns a TimeSlot)
     */
    public void syncWithTimeSlot() {
        if (timeSlot != null) {
            this.dayOfWeek = timeSlot.getDayOfWeek();
            this.startTime = timeSlot.getStartTime();
            this.endTime = timeSlot.getEndTime();
            if (timeSlot.getPeriodNumber() != null) {
                this.periodNumber = timeSlot.getPeriodNumber();
            }
        }
    }

    /**
     * JPA callback: Automatically sync TimeSlot before persisting
     */
    @PrePersist
    @PreUpdate
    protected void onSave() {
        syncWithTimeSlot();
    }

    /**
     * Create TimeSlot from current time information
     */
    public TimeSlot toTimeSlot() {
        if (dayOfWeek != null && startTime != null && endTime != null) {
            if (periodNumber != null) {
                return new TimeSlot(dayOfWeek, startTime, endTime, periodNumber);
            }
            return new TimeSlot(dayOfWeek, startTime, endTime);
        }
        return null;
    }

    public DayOfWeek getDayOfWeek() {
        return this.dayOfWeek;
    }

    public LocalTime getStartTime() {
        return this.startTime;
    }

    public LocalTime getEndTime() {
        return this.endTime;
    }

    public Integer getPeriodNumber() {
        return this.periodNumber;
    }
}