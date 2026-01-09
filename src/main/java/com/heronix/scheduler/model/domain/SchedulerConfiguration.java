package com.heronix.scheduler.model.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalTime;

@Entity
@Table(name = "scheduler_configurations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    // ========== SOLVER SETTINGS ==========

    @Column(name = "max_solver_minutes")
    private Integer maxSolverMinutes = 5;

    @Column(name = "unimproved_seconds_termination")
    private Integer unimprovedSecondsTermination = 30;

    @Column(name = "enable_multithreading")
    private Boolean enableMultithreading = true;

    // ========== WORKLOAD CONSTRAINTS ==========

    @Column(name = "min_periods_per_teacher")
    private Integer minPeriodsPerTeacher = 4;

    @Column(name = "max_periods_per_teacher")
    private Integer maxPeriodsPerTeacher = 7;

    @Column(name = "max_consecutive_periods")
    private Integer maxConsecutivePeriods = 3;

    @Column(name = "min_planning_periods")
    private Integer minPlanningPeriods = 1;

    @Column(name = "allow_back_to_back_classes")
    private Boolean allowBackToBackClasses = true;

    // ========== STUDENT CONSTRAINTS ==========

    @Column(name = "max_students_per_class")
    private Integer maxStudentsPerClass = 30;

    @Column(name = "min_students_per_class")
    private Integer minStudentsPerClass = 10;

    @Column(name = "enforce_prerequisite_order")
    private Boolean enforcePrerequisiteOrder = true;

    @Column(name = "balance_class_sizes")
    private Boolean balanceClassSizes = true;

    @Column(name = "honor_student_requests")
    private Boolean honorStudentRequests = true;

    // ========== TIME PREFERENCES ==========

    @Column(name = "prefer_morning_core_subjects")
    private Boolean preferMorningCoreSubjects = true;

    @Column(name = "prefer_afternoon_electives")
    private Boolean preferAfternoonElectives = false;

    @Column(name = "earliest_start_time")
    private LocalTime earliestStartTime = LocalTime.of(7, 30);

    @Column(name = "latest_end_time")
    private LocalTime latestEndTime = LocalTime.of(15, 30);

    @Column(name = "min_passing_time_minutes")
    private Integer minPassingTimeMinutes = 5;

    // ========== SPECIAL ACCOMMODATIONS ==========

    @Column(name = "honor_iep_accommodations")
    private Boolean honorIepAccommodations = true;

    @Column(name = "honor_504_accommodations")
    private Boolean honor504Accommodations = true;

    @Column(name = "small_class_for_special_needs")
    private Boolean smallClassForSpecialNeeds = true;

    @Column(name = "resource_room_proximity")
    private Boolean resourceRoomProximity = true;

    // ========== ROOM ASSIGNMENT ==========

    @Column(name = "prefer_dedicated_classrooms")
    private Boolean preferDedicatedClassrooms = true;

    @Column(name = "minimize_teacher_moves")
    private Boolean minimizeTeacherMoves = true;

    @Column(name = "minimize_student_moves")
    private Boolean minimizeStudentMoves = false;

    @Column(name = "max_building_transitions")
    private Integer maxBuildingTransitions = 2;

    // ========== COURSE SPECIFIC ==========

    @Column(name = "lab_courses_need_double_periods")
    private Boolean labCoursesNeedDoublePeriods = false;

    @Column(name = "pe_same_gender_grouping")
    private Boolean peSameGenderGrouping = false;

    @Column(name = "group_course_sections")
    private Boolean groupCourseSections = true;

    // ========== TEACHER PREFERENCES ==========

    @Column(name = "respect_teacher_availability")
    private Boolean respectTeacherAvailability = true;

    @Column(name = "respect_teacher_preferences")
    private Boolean respectTeacherPreferences = true;

    @Column(name = "allow_teacher_period_requests")
    private Boolean allowTeacherPeriodRequests = true;

    @Column(name = "max_preps_per_teacher")
    private Integer maxPrepsPerTeacher = 3;

    // ========== OPTIMIZATION WEIGHTS ==========

    @Column(name = "weight_teacher_conflict")
    private Integer weightTeacherConflict = 1000;

    @Column(name = "weight_room_conflict")
    private Integer weightRoomConflict = 1000;

    @Column(name = "weight_capacity")
    private Integer weightCapacity = 800;

    @Column(name = "weight_workload_balance")
    private Integer weightWorkloadBalance = 50;

    @Column(name = "weight_teacher_qualification")
    private Integer weightTeacherQualification = 100;

    @Column(name = "weight_student_preference")
    private Integer weightStudentPreference = 75;

    @Column(name = "weight_minimize_gaps")
    private Integer weightMinimizeGaps = 30;

    @Column(name = "weight_room_utilization")
    private Integer weightRoomUtilization = 20;

    // ========== METADATA ==========

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt = java.time.LocalDateTime.now();

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
}
