package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "special_conditions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecialCondition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition_type", nullable = false)
    private ConditionType conditionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope")
    private ConditionScope scope = ConditionScope.GLOBAL;

    // ========== TARGET ENTITIES ==========

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private Student student;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    // ========== TIME RESTRICTIONS ==========

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek dayOfWeek;

    @Column(name = "period_number")
    private Integer periodNumber;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    // ========== CONDITION DETAILS ==========

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity")
    private ConditionSeverity severity = ConditionSeverity.HARD;

    @Column(name = "penalty_weight")
    private Integer penaltyWeight = 1000;

    // ========== PAIRING/GROUPING ==========

    @ManyToOne
    @JoinColumn(name = "paired_with_teacher_id")
    private Teacher pairedWithTeacher;

    @ManyToOne
    @JoinColumn(name = "paired_with_course_id")
    private Course pairedWithCourse;

    @Column(name = "must_be_consecutive")
    private Boolean mustBeConsecutive = false;

    @Column(name = "max_separation_periods")
    private Integer maxSeparationPeriods;

    // ========== AVAILABILITY ==========

    @Column(name = "is_unavailable")
    private Boolean isUnavailable = false;

    @Column(name = "is_preferred")
    private Boolean isPreferred = false;

    @Column(name = "is_required")
    private Boolean isRequired = false;

    // ========== METADATA ==========

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "priority")
    private Integer priority = 1;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    @Column(name = "effective_date")
    private java.time.LocalDate effectiveDate;

    @Column(name = "expiration_date")
    private java.time.LocalDate expirationDate;

    // ========== NESTED ENUMS ==========

    public enum ConditionType {
        UNAVAILABLE_TIME,          // Teacher/student not available
        REQUIRED_TIME,             // Must schedule at specific time
        PREFERRED_TIME,            // Prefer this time slot
        AVOID_TIME,                // Soft constraint to avoid time
        ROOM_REQUIRED,             // Must use specific room
        ROOM_PREFERRED,            // Prefer specific room
        CONSECUTIVE_PERIODS,       // Classes must be back-to-back
        SAME_DAY,                  // Classes must be on same day
        DIFFERENT_DAY,             // Classes must be on different days
        MAX_DAILY_LOAD,            // Max periods per day override
        PAIRED_TEACHING,           // Co-teaching requirement
        SEPARATED_FROM,            // Keep apart from another class/teacher
        BEFORE,                    // Must schedule before another
        AFTER,                     // Must schedule after another
        NO_FIRST_PERIOD,           // Cannot be first period
        NO_LAST_PERIOD,            // Cannot be last period
        BUILDING_RESTRICTION,      // Must stay in specific building
        ACCESSIBILITY_REQUIREMENT, // ADA/accessibility needs
        SMALL_GROUP,               // Reduced class size required
        EXTENDED_TIME              // Needs extra time (IEP/504)
    }

    public enum ConditionScope {
        GLOBAL,                    // Applies to all schedules
        SCHEDULE_SPECIFIC,         // Applies to one schedule
        TEMPORARY,                 // Time-limited condition
        RECURRING                  // Repeating pattern
    }

    public enum ConditionSeverity {
        HARD,                      // Must be satisfied (constraint)
        MEDIUM,                    // Should be satisfied (high penalty)
        SOFT,                      // Nice to have (low penalty)
        PREFERENCE                 // Optimization goal only
    }
}
