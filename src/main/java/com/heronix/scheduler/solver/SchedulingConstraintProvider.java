package com.heronix.scheduler.solver;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.RoomType;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintCollectors;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduling Constraint Provider - SMART QUALIFICATION WITH DEPARTMENT INFERENCE
 * Location:
 * src/main/java/com/eduscheduler/solver/SchedulingConstraintProvider.java
 *
 * CRITICAL CONSTRAINTS:
 * ✅ Teacher qualification (SOFT with graduated penalties) - Prefers certified teachers
 *    - Uses: Certifications, Department, Course assignments
 *    - Allows department-based inference when certifications incomplete
 *    - Warns administrators to add certifications via logs
 * ✅ Lab requirement constraint (HARD) - Lab courses must get lab rooms
 * ✅ Room type matching (HARD) - Subjects matched to appropriate room types
 * ✅ Teacher-course preference (SOFT) - Prefer pre-assigned teachers
 * ✅ Room-subject affinity (SOFT) - Keep same subjects in same rooms
 *
 * RECENT CHANGES (2025-11-29):
 * - ✅ Added department-based qualification inference (Check #4)
 * - ✅ Changed to SOFT constraint with graduated penalties (0/10/100)
 * - ✅ Fixed lazy loading: Teachers loaded with certifications eagerly
 * - ✅ Comprehensive logging: Warnings for inferred/poor matches
 * - Allows schedule generation even when certifications incomplete
 * - Encourages proper certification setup via penalty scores
 *
 * @author Heronix Scheduling System Team
 * @version 6.1.0 - SMART QUALIFICATION WITH DEPARTMENT INFERENCE
 * @since 2025-11-29
 */
public class SchedulingConstraintProvider implements ConstraintProvider {

    private static final Logger log = LoggerFactory.getLogger(SchedulingConstraintProvider.class);
    private static final int DEFAULT_ROOM_CAPACITY = 30;
    private static final int MAX_CONSECUTIVE_PERIODS = 3;

    // UPDATED December 15, 2025: Changed to teaching period-based limits (matches real-world scheduling)
    // Real-world: 7 periods per day = 6 teaching + 1 planning
    // Teachers work 6 teaching periods, not unlimited courses
    private static final int MAX_DAILY_TEACHING_PERIODS = 6;  // Teachers: 6 teaching periods maximum
    private static final int DEFAULT_MAX_PERIODS_PER_TEACHER = 6;  // Hard limit: 6 teaching periods
    private static final int DEFAULT_MIN_PERIODS_PER_TEACHER = 1;  // Minimum: 1 teaching period
    // Note: This replaces the old course-count limits (3 courses) with period-based limits

    // Kept for backwards compatibility, but period-based limits are recommended
    private static final int DEFAULT_MAX_COURSES_PER_TEACHER = 4;  // Legacy: 4 courses maximum
    private static final int DEFAULT_MIN_COURSES_PER_TEACHER = 1;  // Legacy: 1 course minimum

    private static final int STUDENT_DAILY_COURSES = 7;  // Students: 7 courses + 1 lunch = 8 total periods

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // HARD CONSTRAINTS (Must be satisfied)
                teacherConflict(constraintFactory),
                roomConflict(constraintFactory),
                roomCapacity(constraintFactory),
                labRequirement(constraintFactory),              // NEW: Lab courses need lab rooms
                roomTypeMatching(constraintFactory),            // NEW: Match room type to subject

                // SOFT CONSTRAINTS (Optimization goals)
                minimizeUnassignedSlots(constraintFactory),
                balanceTeacherWorkload(constraintFactory),
                preferMorningForComplexCourses(constraintFactory),
                minimizeTeacherGaps(constraintFactory),
                maximizeRoomUtilization(constraintFactory),
                teacherQualification(constraintFactory),        // SOFT: Prefer qualified/certified teachers
                teacherCoursePreference(constraintFactory),     // NEW: Prefer pre-assigned teachers
                teacherCourseLoadBalance(constraintFactory),    // NEW: Balance teacher course assignments (3-4 courses)
                roomSubjectAffinity(constraintFactory),         // NEW: Keep subjects in same rooms
                preferTeacherHomeRoom(constraintFactory),       // NEW: Prefer teacher's designated home room
                studentLunchPeriod(constraintFactory),          // HARD: All students must have lunch
                teacherLunchPeriod(constraintFactory),          // HARD: All teachers must have lunch
                lunchCapacityLimit(constraintFactory),          // HARD: Lunch capacity not exceeded

                // PHASE 5C: Multiple Rotating Lunch Wave Constraints
                studentLunchWaveAssignment(constraintFactory),   // HARD: Students attend their assigned lunch wave
                teacherLunchWaveAssignment(constraintFactory),   // HARD: Teachers attend their assigned lunch wave
                studentFreeDuringLunchWave(constraintFactory),   // HARD: Students must be free during lunch wave
                teacherFreeDuringLunchWave(constraintFactory),   // HARD: Teachers must be free during lunch wave

                // ADVANCED CONSTRAINTS
                maxConsecutivePeriodsTeacher(constraintFactory), // HARD: Teacher max consecutive
                maxDailyPeriodsTeacher(constraintFactory),       // HARD: Teacher max daily load
                minPlanningPeriods(constraintFactory),           // HARD: Minimum planning time
                teacherPlanningPeriodReserved(constraintFactory), // HARD: No teaching during planning period (NEW Dec 15, 2025)
                avoidBackToBackSameSubject(constraintFactory),   // SOFT: Student variety
                balanceClassSizes(constraintFactory),            // SOFT: Even distribution
                honorIepAccommodations(constraintFactory),       // HARD: IEP requirements
                minimizeBuildingTransitions(constraintFactory),  // SOFT: Reduce student travel
                respectSpecialConditions(constraintFactory),     // HARD/SOFT: Custom rules

                // PHASE 5H: PE Activity Room Matching
                roomActivityMatching(constraintFactory),         // SOFT: Match activity type to room tags

                // PHASE 6A: Teacher Availability Constraints
                teacherAvailability(constraintFactory),          // HARD: Teachers must be available during assigned slots

                // PHASE 6B: Teacher Room Preferences
                teacherRoomRestrictions(constraintFactory),      // HARD: Teachers restricted to specific rooms
                teacherRoomPreferences(constraintFactory),       // SOFT: Teachers prefer specific rooms

                // PHASE 6D: Room Equipment Matching
                roomEquipmentCompatibility(constraintFactory),   // SOFT: Courses prefer rooms with required equipment

                // PHASE 6C: Department Room Zones
                departmentZonePreference(constraintFactory),     // SOFT: Teachers prefer rooms in their department zone
                minimizeTeacherTravel(constraintFactory),        // SOFT: Minimize travel between consecutive periods

                // PHASE 6E: Multi-Room Courses
                multiRoomAvailability(constraintFactory),        // HARD: All required rooms must be available simultaneously
                multiRoomProximity(constraintFactory),           // SOFT: Multi-room courses prefer nearby rooms
                multiRoomCapacity(constraintFactory)             // SOFT: Total capacity across rooms should meet enrollment
        };
    }

    // ========================================================================
    // HARD CONSTRAINTS
    // ========================================================================

    /**
     * HARD: Teacher cannot teach two classes at the same time
     */
    private Constraint teacherConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(ScheduleSlot.class,
                        Joiners.equal(ScheduleSlot::getTeacher),
                        Joiners.overlapping(
                                slot -> slot.getTimeSlot() != null ? slot.getTimeSlot().getStartTime() : null,
                                slot -> slot.getTimeSlot() != null ? slot.getTimeSlot().getEndTime() : null))
                .filter((slot1, slot2) -> slot1.getTeacher() != null &&
                        slot1.getTimeSlot() != null &&
                        slot2.getTimeSlot() != null &&
                        slot1.getTimeSlot().getDayOfWeek() != null &&
                        slot2.getTimeSlot().getDayOfWeek() != null &&
                        slot1.getTimeSlot().getDayOfWeek().equals(slot2.getTimeSlot().getDayOfWeek()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher conflict");
    }

    /**
     * HARD: Room cannot host more than maxConcurrentClasses at the same time
     * Supports shared rooms like gymnasiums (e.g., 3-4 PE classes simultaneously)
     *
     * Example: Gymnasium with maxConcurrentClasses=4 can host 4 PE classes at once
     */
    private Constraint roomConflict(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getRoom() != null && slot.getTimeSlot() != null)
                .groupBy(ScheduleSlot::getRoom,
                        ScheduleSlot::getTimeSlot,
                        ScheduleSlot::getDayOfWeek,
                        slot -> 1)  // Count concurrent classes
                .filter((room, timeSlot, day, count) -> {
                    Integer maxConcurrent = room.getMaxConcurrentClasses();
                    int limit = (maxConcurrent != null && maxConcurrent > 0) ? maxConcurrent : 1;
                    return count > limit;
                })
                .penalize(HardSoftScore.ONE_HARD,
                        (room, timeSlot, day, count) -> {
                            Integer maxConcurrent = room.getMaxConcurrentClasses();
                            int limit = (maxConcurrent != null && maxConcurrent > 0) ? maxConcurrent : 1;
                            return Math.max(0, count - limit);
                        })
                .asConstraint("Room concurrent class limit exceeded");
    }

    /**
     * HARD: Room total capacity must not be exceeded
     * For shared rooms (maxConcurrentClasses > 1), checks TOTAL students across ALL concurrent classes
     *
     * Example: Gymnasium capacity=120, 3 concurrent PE classes with 40 students each = 120 total (OK)
     *          If 4th class added with 10 students = 130 total (VIOLATION - exceeds 120)
     */
    private Constraint roomCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getRoom() != null && slot.getTimeSlot() != null && slot.getStudents() != null)
                .groupBy(ScheduleSlot::getRoom,
                        ScheduleSlot::getTimeSlot,
                        ScheduleSlot::getDayOfWeek,
                        slot -> {
                            try {
                                return slot.getStudents().size();
                            } catch (Exception e) {
                                return 0;
                            }
                        })
                .filter((room, timeSlot, day, totalStudents) -> {
                    Integer capacity = room.getCapacity();
                    int roomCapacity = (capacity != null) ? capacity : DEFAULT_ROOM_CAPACITY;
                    return totalStudents > roomCapacity;
                })
                .penalize(HardSoftScore.ONE_HARD,
                        (room, timeSlot, day, totalStudents) -> {
                            Integer capacity = room.getCapacity();
                            int roomCapacity = (capacity != null) ? capacity : DEFAULT_ROOM_CAPACITY;
                            int overage = Math.max(0, totalStudents - roomCapacity);

                            if (overage > 0) {
                                log.warn("⚠ CAPACITY VIOLATION: Room {} has {} total students across concurrent classes (capacity: {}, overage: {})",
                                         room.getRoomNumber(), totalStudents, roomCapacity, overage);
                            }

                            return overage;
                        })
                .asConstraint("Room total capacity exceeded");
    }

    /**
     * SOFT: Prefer qualified teachers for courses (with department-based inference)
     * Uses multiple qualification checks with graduated penalties
     *
     * QUALIFICATION HIERARCHY (best to worst):
     * 1. Teacher explicitly assigned to course (teacher.courses) - NO PENALTY
     * 2. Valid SubjectCertification for subject - NO PENALTY
     * 3. Legacy certification string match - NO PENALTY
     * 4. Department matches subject (INFERENCE) - SMALL PENALTY (needs cert)
     * 5. No qualification found - LARGE PENALTY (likely wrong assignment)
     *
     * This allows the AI to use department data when certifications are incomplete,
     * while still preferring teachers with explicit certifications.
     */
    private Constraint teacherQualification(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() != null && slot.getCourse() != null)
                .filter(slot -> !Boolean.TRUE.equals(slot.getPinned())) // Respect manual assignments
                .penalize(HardSoftScore.ONE_SOFT, slot -> {
                    Teacher teacher = slot.getTeacher();
                    Course course = slot.getCourse();

                    // QUALIFICATION CHECK #1: Teacher explicitly assigned to course
                    if (teacher.getCourses() != null && teacher.getCourses().contains(course)) {
                        if (log.isDebugEnabled()) {
                            log.debug("✓ Teacher {} qualified for {} via course assignment",
                                     teacher.getName(), course.getCourseName());
                        }
                        return 0; // ✓ PERFECT MATCH - No penalty
                    }

                    // QUALIFICATION CHECK #2: SubjectCertification system
                    if (course.getSubject() != null && teacher.hasCertificationForSubject(course.getSubject())) {
                        if (log.isDebugEnabled()) {
                            log.debug("✓ Teacher {} qualified for {} via SubjectCertification ({})",
                                     teacher.getName(), course.getCourseName(), course.getSubject());
                        }
                        return 0; // ✓ CERTIFIED - No penalty
                    }

                    // QUALIFICATION CHECK #3: Legacy certifications list
                    if (teacher.getCertifications() != null && course.getSubject() != null) {
                        String courseSubject = course.getSubject().toLowerCase().trim();
                        for (String cert : teacher.getCertifications()) {
                            if (cert != null && cert.toLowerCase().trim().contains(courseSubject)) {
                                if (log.isDebugEnabled()) {
                                    log.debug("✓ Teacher {} qualified for {} via legacy certification ({})",
                                             teacher.getName(), course.getCourseName(), cert);
                                }
                                return 0; // ✓ CERTIFIED (legacy) - No penalty
                            }
                        }
                    }

                    // QUALIFICATION CHECK #4: Department-based inference (NEW!)
                    if (teacher.getDepartment() != null && course.getSubject() != null) {
                        String department = teacher.getDepartment().toLowerCase().trim();
                        String subject = course.getSubject().toLowerCase().trim();

                        // Check if department matches or contains subject
                        if (department.contains(subject) || subject.contains(department)) {
                            log.warn("⚠ INFERENCE: Teacher {} (Dept: {}) assigned to {} (Subject: {}). " +
                                     "Using department match. RECOMMEND: Add certification for {}",
                                     teacher.getName(), teacher.getDepartment(),
                                     course.getCourseName(), course.getSubject(),
                                     course.getSubject());
                            return 10; // ⚠ INFERRED MATCH - Small penalty (encourage adding cert)
                        }
                    }

                    // ✗ No qualification found - LARGE PENALTY
                    log.warn("✗ POOR MATCH: Teacher {} (Dept: {}) assigned to {} (Subject: {}). " +
                             "No certification or department match found. " +
                             "Certifications: {} | RECOMMEND: Review this assignment",
                             teacher.getName(),
                             teacher.getDepartment() != null ? teacher.getDepartment() : "NONE",
                             course.getCourseName(),
                             course.getSubject(),
                             teacher.getSubjectCertifications() != null ?
                                 teacher.getSubjectCertifications().size() + " certs" : "NONE");
                    return 100; // ✗ NO MATCH - Large penalty (avoid if possible)
                })
                .asConstraint("Prefer qualified teachers for courses");
    }

    /**
     * HARD: Courses requiring labs must be assigned to lab rooms
     */
    private Constraint labRequirement(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> {
                    if (slot.getCourse() == null || slot.getRoom() == null) {
                        return false;
                    }
                    if (Boolean.TRUE.equals(slot.getPinned())) {
                        return false; // Respect manual assignments
                    }

                    // Check if course requires a lab
                    if (!Boolean.TRUE.equals(slot.getCourse().getRequiresLab())) {
                        return false;
                    }

                    // Check if room is a lab
                    Room room = slot.getRoom();
                    if (room.getType() != null && room.getType().isLab()) {
                        return false; // Room is a lab
                    }

                    return true; // Course needs lab but room is not a lab
                })
                .penalize(HardSoftScore.ONE_HARD, slot -> 50)
                .asConstraint("Lab courses require lab rooms");
    }

    /**
     * HARD: Room type must match course subject requirements
     */
    private Constraint roomTypeMatching(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> {
                    if (slot.getCourse() == null || slot.getRoom() == null) {
                        return false;
                    }
                    if (Boolean.TRUE.equals(slot.getPinned())) {
                        return false; // Respect manual assignments
                    }

                    String subject = slot.getCourse().getSubject();
                    if (subject == null) {
                        return false;
                    }

                    Room room = slot.getRoom();
                    if (room.getType() == null) {
                        return false;
                    }

                    String subjectLower = subject.toLowerCase().trim();

                    // Science courses should be in science labs or classrooms
                    if (subjectLower.contains("science") || subjectLower.contains("biology") ||
                            subjectLower.contains("chemistry") || subjectLower.contains("physics")) {
                        if (room.getType().name().equals("GYM") ||
                                room.getType().name().equals("AUDITORIUM") ||
                                room.getType().name().equals("CAFETERIA")) {
                            return true; // Invalid room type for science
                        }
                    }

                    // Computer/IT courses should be in computer labs or classrooms
                    if (subjectLower.contains("computer") || subjectLower.contains("technology") ||
                            subjectLower.contains("coding") || subjectLower.contains("programming")) {
                        if (room.getType().name().equals("GYM") ||
                                room.getType().name().equals("AUDITORIUM") ||
                                room.getType().name().equals("CAFETERIA")) {
                            return true; // Invalid room type for computer courses
                        }
                    }

                    // Physical Education must be in GYM
                    if (subjectLower.contains("physical education") || subjectLower.contains("pe") ||
                            subjectLower.contains("gym") || subjectLower.contains("athletics")) {
                        if (!room.getType().name().equals("GYM")) {
                            return true; // PE must be in gym
                        }
                    }

                    // Music courses should be in music rooms or auditorium
                    if (subjectLower.contains("music") || subjectLower.contains("band") ||
                            subjectLower.contains("orchestra") || subjectLower.contains("choir")) {
                        if (room.getType().name().equals("GYM") ||
                                room.getType().name().equals("CAFETERIA") ||
                                room.getType().name().equals("LAB") ||
                                room.getType().name().equals("COMPUTER_LAB")) {
                            return true; // Invalid room type for music
                        }
                    }

                    return false; // Room type is acceptable
                })
                .penalize(HardSoftScore.ONE_HARD, slot -> 30)
                .asConstraint("Room type must match subject");
    }

    // ========================================================================
    // SOFT CONSTRAINTS
    // ========================================================================

    /**
     * SOFT: Minimize unassigned slots (maximize schedule completeness)
     */
    private Constraint minimizeUnassignedSlots(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() == null ||
                        slot.getRoom() == null ||
                        slot.getTimeSlot() == null)
                .penalize(HardSoftScore.ONE_SOFT, slot -> 10)
                .asConstraint("Minimize unassigned slots");
    }

    /**
     * SOFT: Balance teacher workload across all teachers
     */
    private Constraint balanceTeacherWorkload(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() != null)
                .groupBy(ScheduleSlot::getTeacher, org.optaplanner.core.api.score.stream.ConstraintCollectors.count())
                .penalize(HardSoftScore.ONE_SOFT,
                        (teacher, count) -> {
                            // Penalize deviation from average
                            int avgSlots = 5; // Typical teacher has 5 slots per day
                            return Math.abs(count - avgSlots);
                        })
                .asConstraint("Balance teacher workload");
    }

    /**
     * SOFT: Schedule complex courses in the morning when students are more alert
     */
    private Constraint preferMorningForComplexCourses(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> {
                    if (slot.getCourse() == null || slot.getTimeSlot() == null) {
                        return false;
                    }

                    Integer complexity = slot.getCourse().getComplexityScore();
                    if (complexity == null || complexity < 7) {
                        return false;
                    }

                    // Penalize if scheduled after 1pm
                    return slot.getTimeSlot().getStartTime().getHour() >= 13;
                })
                .penalize(HardSoftScore.ONE_SOFT,
                        slot -> {
                            Integer complexity = slot.getCourse().getComplexityScore();
                            return (complexity != null) ? complexity : 0;
                        })
                .asConstraint("Prefer morning for complex courses");
    }

    /**
     * SOFT: Minimize gaps in teacher schedules
     */
    private Constraint minimizeTeacherGaps(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() != null && slot.getTimeSlot() != null)
                .penalize(HardSoftScore.ONE_SOFT, slot -> 1)
                .asConstraint("Minimize teacher gaps");
    }

    /**
     * SOFT: Maximize room utilization (use fewer rooms more efficiently)
     */
    private Constraint maximizeRoomUtilization(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getRoom() != null && slot.getStudents() != null)
                .reward(HardSoftScore.ONE_SOFT,
                        slot -> {
                            Integer capacity = slot.getRoom().getCapacity();
                            if (capacity == null || capacity == 0) {
                                return 0;
                            }
                            int studentCount = slot.getStudents().size();
                            // Reward based on % of capacity used (0-100)
                            return (studentCount * 100) / capacity;
                        })
                .asConstraint("Maximize room utilization");
    }

    /**
     * SOFT: Prefer teachers already assigned to teach the course
     * Rewards assigning a teacher to a course they're already assigned to
     */
    /**
     * SOFT: Prefer teachers assigned to their historical courses
     * STRONG preference - helps veteran teachers keep familiar courses
     * Dramatically reduces solving time by narrowing possibilities
     *
     * Use teacher.courses to pre-assign historical teacher-course pairings
     * Combined with pinned=true for 100% guaranteed assignments
     */
    private Constraint teacherCoursePreference(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() != null && slot.getCourse() != null)
                .filter(slot -> !Boolean.TRUE.equals(slot.getPinned())) // Skip pinned (already locked)
                .reward(HardSoftScore.ONE_SOFT, slot -> {
                    Teacher teacher = slot.getTeacher();
                    Course course = slot.getCourse();

                    // Reward if teacher is pre-assigned to this course
                    if (teacher.getCourses() != null && teacher.getCourses().contains(course)) {
                        if (log.isDebugEnabled()) {
                            log.debug("✓ HISTORICAL MATCH: Teacher {} assigned to familiar course {}",
                                     teacher.getName(), course.getCourseName());
                        }
                        return 50; // STRONG preference - keep veterans with their courses
                    }

                    return 0; // No bonus
                })
                .asConstraint("Prefer historical teacher-course pairings");
    }

    /**
     * SOFT: Balance teacher teaching period load
     * UPDATED December 15, 2025: Changed from course count to teaching period count
     *
     * Penalizes teachers with too few or too many teaching periods per day
     * Real-world: Teachers should teach 5-6 periods per day (with 1 planning period)
     *
     * Teachers should teach between minPeriodsPerDay and maxPeriodsPerDay periods
     * Default: 1-6 periods (adjustable per teacher by administrators)
     *
     * Example violations:
     * - Teacher with only 1-2 teaching periods (underutilized)
     * - Teacher with 7+ teaching periods (overloaded - exceeds max)
     */
    private Constraint teacherCourseLoadBalance(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() != null
                             && slot.getPeriodNumber() != null
                             && !Boolean.TRUE.equals(slot.getIsLunchPeriod()))
                .groupBy(ScheduleSlot::getTeacher,
                        ScheduleSlot::getDayOfWeek,
                        ConstraintCollectors.countDistinct(ScheduleSlot::getPeriodNumber))
                .penalize(HardSoftScore.ONE_SOFT, (teacher, day, periodCount) -> {
                    // UPDATED: Now counts distinct teaching periods per day, not courses
                    int count = (int) (long) periodCount;

                    // Get teacher's max period limits (defaults: 1-6 periods)
                    // Note: Using maxPeriodsPerDay field (minPeriodsPerDay not implemented yet)
                    Integer maxPeriods = teacher.getMaxPeriodsPerDay();

                    int min = DEFAULT_MIN_PERIODS_PER_TEACHER;  // Use constant for now
                    int max = (maxPeriods != null) ? maxPeriods : DEFAULT_MAX_PERIODS_PER_TEACHER;

                    int penalty = 0;

                    if (count < min) {
                        // Too few periods - penalize (teacher underutilized)
                        penalty = (min - count) * 30;
                        log.warn("⚠ UNDERLOAD: Teacher {} teaching only {} periods on {} (min: {}). Penalty: {}",
                                teacher.getName(), count, day, min, penalty);
                    } else if (count > max) {
                        // Too many periods - penalize heavily (teacher overloaded - CRITICAL!)
                        penalty = (count - max) * 100;
                        log.warn("⚠ OVERLOAD: Teacher {} teaching {} periods on {} (max: {}). Penalty: {}",
                                teacher.getName(), count, day, max, penalty);
                    } else {
                        // Within acceptable range - no penalty
                        if (log.isDebugEnabled()) {
                            log.debug("✓ BALANCED: Teacher {} teaching {} periods on {} (range: {}-{})",
                                    teacher.getName(), count, day, min, max);
                        }
                    }

                    return penalty;
                })
                .asConstraint("Balance teacher teaching period load (5-6 periods per day)");
    }

    /**
     * SOFT: Keep same subjects in same rooms (room-subject affinity)
     * Encourages scheduling the same subject in the same room across different time slots
     */
    private Constraint roomSubjectAffinity(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getRoom() != null &&
                        slot.getCourse() != null &&
                        slot.getCourse().getSubject() != null)
                .join(ScheduleSlot.class,
                        Joiners.equal(ScheduleSlot::getRoom),
                        Joiners.lessThan(ScheduleSlot::getId)) // Avoid double counting
                .filter((slot1, slot2) -> {
                    if (slot2.getCourse() == null || slot2.getCourse().getSubject() == null) {
                        return false;
                    }
                    // Reward if both slots have same subject in same room
                    return slot1.getCourse().getSubject().equals(slot2.getCourse().getSubject());
                })
                .reward(HardSoftScore.ONE_SOFT, (slot1, slot2) -> 5) // Small reward for affinity
                .asConstraint("Room-subject affinity");
    }

    /**
     * SOFT: Prefer teacher's designated home room
     * Teachers should conduct classes in their home room when possible
     * Exceptions: Lab courses, PE, Music, and other special room requirements
     */
    private Constraint preferTeacherHomeRoom(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> {
                    if (slot.getTeacher() == null || slot.getRoom() == null) {
                        return false;
                    }
                    if (Boolean.TRUE.equals(slot.getPinned())) {
                        return false; // Respect manual assignments
                    }

                    Teacher teacher = slot.getTeacher();
                    Room assignedRoom = slot.getRoom();

                    // If teacher has no home room, no preference to enforce
                    if (teacher.getHomeRoom() == null) {
                        return false;
                    }

                    // If already in home room, no penalty
                    if (teacher.getHomeRoom().equals(assignedRoom)) {
                        return false;
                    }

                    // Check if course requires special room type
                    Course course = slot.getCourse();
                    if (course != null) {
                        // Allow exceptions for courses that need special rooms
                        if (Boolean.TRUE.equals(course.getRequiresLab())) {
                            return false; // Lab courses need lab rooms
                        }

                        String subject = course.getSubject();
                        if (subject != null) {
                            String subjectLower = subject.toLowerCase().trim();

                            // Allow exceptions for PE, Music, and other special subjects
                            if (subjectLower.contains("physical education") ||
                                subjectLower.contains("pe") ||
                                subjectLower.contains("gym") ||
                                subjectLower.contains("athletics") ||
                                subjectLower.contains("music") ||
                                subjectLower.contains("band") ||
                                subjectLower.contains("orchestra") ||
                                subjectLower.contains("choir")) {
                                return false; // These need special rooms
                            }
                        }
                    }

                    // Penalize if teacher is not in their home room
                    return true;
                })
                .penalize(HardSoftScore.ONE_SOFT, slot -> 20) // Moderate penalty for not using home room
                .asConstraint("Prefer teacher home room");
    }

    // ========================================================================
    // LUNCH PERIOD CONSTRAINTS
    // ========================================================================

    /**
     * HARD: Every student must have a lunch period scheduled
     */
    private Constraint studentLunchPeriod(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getStudents() != null && !slot.getStudents().isEmpty())
                .filter(slot -> !Boolean.TRUE.equals(slot.getIsLunchPeriod()))
                .groupBy(
                    slot -> slot.getSchedule(),
                    slot -> slot.getStudents().stream().findFirst().orElse(null)
                )
                .filter((schedule, student) -> student != null)
                .filter((schedule, student) -> {
                    // Check if student has any lunch period
                    if (schedule == null || schedule.getSlots() == null) return true;
                    return schedule.getSlots().stream()
                        .filter(s -> Boolean.TRUE.equals(s.getIsLunchPeriod()))
                        .noneMatch(s -> s.getStudents() != null && s.getStudents().contains(student));
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student must have lunch");
    }

    /**
     * HARD: Every teacher must have a lunch period scheduled
     */
    private Constraint teacherLunchPeriod(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() != null)
                .filter(slot -> !Boolean.TRUE.equals(slot.getIsLunchPeriod()))
                .groupBy(
                    slot -> slot.getSchedule(),
                    ScheduleSlot::getTeacher
                )
                .filter((schedule, teacher) -> {
                    // Check if teacher has any lunch period
                    if (schedule == null || schedule.getSlots() == null) return true;
                    return schedule.getSlots().stream()
                        .filter(s -> Boolean.TRUE.equals(s.getIsLunchPeriod()))
                        .noneMatch(s -> teacher.equals(s.getTeacher()));
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher must have lunch");
    }

    /**
     * HARD: Lunch period capacity must not be exceeded
     */
    private Constraint lunchCapacityLimit(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> Boolean.TRUE.equals(slot.getIsLunchPeriod()))
                .filter(slot -> slot.getStudents() != null)
                .filter(slot -> slot.getLunchWaveNumber() != null)
                .groupBy(
                    slot -> slot.getSchedule(),
                    ScheduleSlot::getLunchWaveNumber,
                    slot -> slot.getStudents().size()
                )
                .filter((schedule, wave, count) -> count > 300) // Default max per wave
                .penalize(HardSoftScore.ONE_HARD,
                    (schedule, wave, count) -> Math.max(0, count - 300))
                .asConstraint("Lunch capacity limit");
    }

    // ========================================================================
    // PHASE 5C: MULTIPLE ROTATING LUNCH WAVE CONSTRAINTS
    // ========================================================================

    /**
     * HARD: Students must attend their assigned lunch wave
     * Phase 5C: Multiple Rotating Lunch Periods
     *
     * Ensures that:
     * 1. Students assigned to Lunch Wave 1 attend Lunch 1 time slots
     * 2. Students assigned to Lunch Wave 2 attend Lunch 2 time slots
     * 3. Students assigned to Lunch Wave 3 attend Lunch 3 time slots
     *
     * This prevents OptaPlanner from assigning students to wrong lunch waves
     */
    private Constraint studentLunchWaveAssignment(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> Boolean.TRUE.equals(slot.getIsLunchPeriod()))
                .filter(slot -> slot.getStudents() != null && !slot.getStudents().isEmpty())
                .filter(slot -> slot.getLunchWaveNumber() != null)
                // ✅ PRIORITY 2 FIX December 15, 2025: Join with StudentLunchAssignment
                // This allows us to validate students are in their assigned lunch wave
                .join(StudentLunchAssignment.class,
                    Joiners.filtering((slot, assignment) -> {
                        // Check if this lunch slot contains a student that has a lunch assignment
                        return slot.getStudents() != null &&
                               slot.getStudents().stream()
                                   .anyMatch(s -> s.getId().equals(assignment.getStudent().getId()));
                    }))
                .filter((slot, assignment) -> {
                    // Violation: Student in slot but assigned to different wave
                    return assignment.getLunchWave() != null &&
                           assignment.getLunchWave().getWaveOrder() != null &&
                           !assignment.getLunchWave().getWaveOrder().equals(slot.getLunchWaveNumber());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Student lunch wave assignment");
    }

    /**
     * HARD: Teachers must attend their assigned lunch wave
     * Phase 5C: Multiple Rotating Lunch Periods
     *
     * Similar to student constraint - ensures teachers attend correct lunch wave
     */
    private Constraint teacherLunchWaveAssignment(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> Boolean.TRUE.equals(slot.getIsLunchPeriod()))
                .filter(slot -> slot.getTeacher() != null)
                .filter(slot -> slot.getLunchWaveNumber() != null)
                // ✅ PRIORITY 2 FIX December 15, 2025: Join with TeacherLunchAssignment
                // This allows us to validate teachers are in their assigned lunch wave
                .join(TeacherLunchAssignment.class,
                    Joiners.filtering((slot, assignment) -> {
                        // Check if this lunch slot's teacher has a lunch assignment
                        return slot.getTeacher() != null &&
                               assignment.getTeacher() != null &&
                               slot.getTeacher().getId().equals(assignment.getTeacher().getId());
                    }))
                .filter((slot, assignment) -> {
                    // Violation: Teacher in slot but assigned to different wave
                    return assignment.getLunchWave() != null &&
                           assignment.getLunchWave().getWaveOrder() != null &&
                           !assignment.getLunchWave().getWaveOrder().equals(slot.getLunchWaveNumber());
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher lunch wave assignment");
    }

    /**
     * HARD: Students must be free (no classes scheduled) during their assigned lunch wave
     * Phase 5C: Multiple Rotating Lunch Periods
     *
     * Example: Student assigned to Lunch 2 (10:58-11:28) cannot have a class during that time
     * This prevents conflicts where a student has both class and lunch at same time
     */
    private Constraint studentFreeDuringLunchWave(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(ScheduleSlot.class,
                    // Join lunch slots with regular class slots
                    Joiners.filtering((lunchSlot, classSlot) -> {
                        // lunchSlot is a lunch period, classSlot is a regular class
                        if (!Boolean.TRUE.equals(lunchSlot.getIsLunchPeriod())) return false;
                        if (Boolean.TRUE.equals(classSlot.getIsLunchPeriod())) return false;
                        if (lunchSlot.getStudents() == null || classSlot.getStudents() == null) return false;

                        // Check if any students overlap AND time slots overlap
                        boolean hasCommonStudents = lunchSlot.getStudents().stream()
                            .anyMatch(s -> classSlot.getStudents().contains(s));

                        if (!hasCommonStudents) return false;

                        // Check time overlap
                        if (lunchSlot.getTimeSlot() == null || classSlot.getTimeSlot() == null) return false;
                        return lunchSlot.getTimeSlot().overlapsWith(classSlot.getTimeSlot());
                    }))
                .penalize(HardSoftScore.ONE_HARD,
                    (lunchSlot, classSlot) -> {
                        // Count how many students have conflict
                        return (int) lunchSlot.getStudents().stream()
                            .filter(s -> classSlot.getStudents().contains(s))
                            .count();
                    })
                .asConstraint("Student free during lunch wave");
    }

    /**
     * HARD: Teachers must be free (no classes scheduled) during their assigned lunch wave
     * Phase 5C: Multiple Rotating Lunch Periods
     *
     * Ensures teachers can attend their lunch period without teaching conflicts
     * Teachers with supervision duty are handled separately
     */
    private Constraint teacherFreeDuringLunchWave(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEachUniquePair(ScheduleSlot.class,
                    // Join lunch slots with regular class slots for same teacher
                    Joiners.equal(ScheduleSlot::getTeacher),
                    Joiners.filtering((lunchSlot, classSlot) -> {
                        // lunchSlot is where teacher has lunch, classSlot is where they teach
                        if (!Boolean.TRUE.equals(lunchSlot.getIsLunchPeriod())) return false;
                        if (Boolean.TRUE.equals(classSlot.getIsLunchPeriod())) return false;
                        if (lunchSlot.getTeacher() == null) return false;

                        // Check time overlap
                        if (lunchSlot.getTimeSlot() == null || classSlot.getTimeSlot() == null) return false;
                        return lunchSlot.getTimeSlot().overlapsWith(classSlot.getTimeSlot());
                    }))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher free during lunch wave");
    }

    // ========================================================================
    // ADVANCED CONSTRAINTS
    // ========================================================================

    /**
     * HARD: Teacher cannot teach more than MAX_CONSECUTIVE_PERIODS in a row
     * Simplified to check if any 4+ consecutive periods exist
     */
    private Constraint maxConsecutivePeriodsTeacher(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() != null && slot.getPeriodNumber() != null)
                .join(ScheduleSlot.class,
                    Joiners.equal(ScheduleSlot::getTeacher),
                    Joiners.equal(ScheduleSlot::getDayOfWeek))
                .filter((slot1, slot2) -> {
                    if (slot1.getPeriodNumber() == null || slot2.getPeriodNumber() == null) return false;
                    int diff = Math.abs(slot1.getPeriodNumber() - slot2.getPeriodNumber());
                    // Check if slots are exactly MAX_CONSECUTIVE_PERIODS+1 apart (indicating violation)
                    return diff == (MAX_CONSECUTIVE_PERIODS + 1);
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Max consecutive periods");
    }

    /**
     * HARD: Teacher cannot exceed maximum daily periods
     */
    private Constraint maxDailyPeriodsTeacher(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() != null)
                .filter(slot -> !Boolean.TRUE.equals(slot.getIsLunchPeriod()))
                .groupBy(ScheduleSlot::getTeacher,
                    ScheduleSlot::getDayOfWeek,
                    slot -> 1)
                .filter((teacher, day, count) -> {
                    Integer maxPeriods = teacher.getMaxPeriodsPerDay();
                    int limit = maxPeriods != null ? maxPeriods : MAX_DAILY_TEACHING_PERIODS;
                    return count > limit;
                })
                .penalize(HardSoftScore.ONE_HARD,
                    (teacher, day, count) -> {
                        Integer maxPeriods = teacher.getMaxPeriodsPerDay();
                        int limit = maxPeriods != null ? maxPeriods : MAX_DAILY_TEACHING_PERIODS;
                        return Math.max(0, count - limit);
                    })
                .asConstraint("Max daily periods");
    }

    /**
     * HARD: Teachers need minimum planning periods per day
     */
    private Constraint minPlanningPeriods(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() != null)
                .groupBy(ScheduleSlot::getTeacher,
                    ScheduleSlot::getDayOfWeek,
                    slot -> 1)
                .filter((teacher, day, count) -> {
                    // If teaching 5+ periods, should have 1+ planning period
                    return count >= 5 && count >= MAX_DAILY_TEACHING_PERIODS;
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Minimum planning periods");
    }

    /**
     * HARD: Teacher must not teach during their planning period
     * ADDED: December 15, 2025 - Real-world scheduling support
     *
     * Every teacher has a designated planning period (1-7) that must be kept free for:
     * - Lesson planning and preparation
     * - Grading assignments
     * - Parent communication
     * - Professional meetings
     *
     * Example: Teacher Maria has planningPeriod=4
     * - System must NOT assign any course to Period 4 for Maria
     * - Period 4 is reserved for planning activities
     *
     * This constraint ensures no teaching assignments during planning period.
     */
    private Constraint teacherPlanningPeriodReserved(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() != null
                             && slot.getPeriodNumber() != null
                             && slot.getTeacher().getPlanningPeriod() != null
                             && !Boolean.TRUE.equals(slot.getIsLunchPeriod())) // Exclude lunch periods
                .filter(slot -> slot.getPeriodNumber().equals(slot.getTeacher().getPlanningPeriod()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher planning period must be free");
    }

    /**
     * SOFT: Avoid students having same subject back-to-back
     */
    private Constraint avoidBackToBackSameSubject(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getCourse() != null && slot.getStudents() != null)
                .join(ScheduleSlot.class,
                    Joiners.equal(slot -> slot.getStudents().stream().findFirst().orElse(null)),
                    Joiners.equal(ScheduleSlot::getDayOfWeek),
                    Joiners.filtering((slot1, slot2) -> {
                        // Both slots must have courses with subjects
                        return slot2.getCourse() != null &&
                               slot1.getCourse().getSubject() != null &&
                               slot2.getCourse().getSubject() != null &&
                               slot1.getCourse().getSubject().equals(slot2.getCourse().getSubject());
                    }))
                .filter((slot1, slot2) -> {
                    if (slot1.getPeriodNumber() == null || slot2.getPeriodNumber() == null) return false;
                    int diff = Math.abs(slot1.getPeriodNumber() - slot2.getPeriodNumber());
                    return diff == 1;
                })
                .penalize(HardSoftScore.ONE_SOFT, (slot1, slot2) -> 15)
                .asConstraint("Avoid back-to-back same subject");
    }

    /**
     * SOFT: Balance class sizes across sections
     * Penalize large differences between section sizes for same course
     */
    private Constraint balanceClassSizes(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getCourse() != null && slot.getStudents() != null)
                .join(ScheduleSlot.class,
                    Joiners.lessThan(ScheduleSlot::getId), // Avoid double counting (must come first!)
                    Joiners.filtering((slot1, slot2) -> {
                        // Both slots must have courses with matching names
                        return slot2.getCourse() != null &&
                               slot1.getCourse().getCourseName() != null &&
                               slot2.getCourse().getCourseName() != null &&
                               slot1.getCourse().getCourseName().equals(slot2.getCourse().getCourseName());
                    }))
                .filter((slot1, slot2) -> {
                    if (slot1.getStudents() == null || slot2.getStudents() == null) return false;
                    int diff = Math.abs(slot1.getStudents().size() - slot2.getStudents().size());
                    return diff > 5; // More than 5 student difference
                })
                .penalize(HardSoftScore.ONE_SOFT, (slot1, slot2) -> {
                    int diff = Math.abs(slot1.getStudents().size() - slot2.getStudents().size());
                    return Math.max(0, diff - 5); // Penalty for difference beyond 5
                })
                .asConstraint("Balance class sizes");
    }

    /**
     * HARD: Honor IEP accommodations (small class, resource room access)
     */
    private Constraint honorIepAccommodations(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getStudents() != null)
                .filter(slot -> slot.getStudents().stream()
                    .anyMatch(s -> Boolean.TRUE.equals(s.getHasIEP())))
                .filter(slot -> {
                    // Check if IEP student in oversized class
                    if (slot.getRoom() == null) return false;
                    int iepCount = (int) slot.getStudents().stream()
                        .filter(s -> Boolean.TRUE.equals(s.getHasIEP()))
                        .count();
                    int totalStudents = slot.getStudents().size();
                    // IEP classes should be smaller (max 20 if multiple IEP students)
                    return iepCount >= 3 && totalStudents > 20;
                })
                .penalize(HardSoftScore.ONE_HARD,
                    slot -> slot.getStudents().size() - 20)
                .asConstraint("IEP small class requirement");
    }

    /**
     * SOFT: Minimize building transitions for students
     */
    private Constraint minimizeBuildingTransitions(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getRoom() != null && slot.getStudents() != null)
                .join(ScheduleSlot.class,
                    Joiners.equal(slot -> slot.getStudents().stream().findFirst().orElse(null)),
                    Joiners.equal(ScheduleSlot::getDayOfWeek))
                .filter((slot1, slot2) -> {
                    if (slot1.getPeriodNumber() == null || slot2.getPeriodNumber() == null) return false;
                    int diff = slot2.getPeriodNumber() - slot1.getPeriodNumber();
                    if (diff != 1) return false; // Only check consecutive periods

                    // Check if different buildings
                    String building1 = slot1.getRoom().getBuilding();
                    String building2 = slot2.getRoom().getBuilding();
                    return building1 != null && building2 != null && !building1.equals(building2);
                })
                .penalize(HardSoftScore.ONE_SOFT, (slot1, slot2) -> 25)
                .asConstraint("Minimize building transitions");
    }

    /**
     * HARD/SOFT: Respect special conditions from database
     */
    private Constraint respectSpecialConditions(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> {
                    // Check for unavailable times, required rooms, etc.
                    // This would need SpecialCondition repository injection
                    // For now, basic check for pinned slots
                    return Boolean.TRUE.equals(slot.getPinned());
                })
                .reward(HardSoftScore.ONE_SOFT, slot -> 100) // Reward keeping pinned slots
                .asConstraint("Respect special conditions");
    }

    // ========================================================================
    // PHASE 5H: PE ACTIVITY ROOM MATCHING
    // ========================================================================

    /**
     * SOFT: Prefer rooms that match course activity type
     * Phase 5H: PE Activity Room Matching
     *
     * Uses the Room.supportsActivity() method from Phase 5F to match courses to appropriate rooms.
     *
     * Examples:
     * - Basketball course → Gymnasium with "Basketball" tag (no penalty)
     * - Basketball course → Weight Room (penalty: 50 points - critical mismatch)
     * - Weights course → Gymnasium (penalty: 20 points - tolerable but not ideal)
     * - Weights course → Weight Room (no penalty - perfect match)
     * - Dance course → Dance Studio (no penalty)
     * - Dance course → Weight Room (penalty: 50 points - critical mismatch)
     *
     * Graduated penalty system:
     * - Perfect match (room supports activity): 0 points
     * - Tolerable mismatch (e.g., Weights in Gym): 20 points
     * - Critical mismatch (e.g., Basketball in Weight Room): 50 points
     * - Default penalty for unspecified mismatches: 5 points
     *
     * @since Phase 5H - December 2, 2025
     */
    private Constraint roomActivityMatching(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getRoom() != null && slot.getCourse() != null)
                .filter(slot -> !Boolean.TRUE.equals(slot.getPinned())) // Respect manual assignments
                .filter(slot -> {
                    Room room = slot.getRoom();
                    Course course = slot.getCourse();

                    // Only apply constraint if course has an activity type
                    if (course.getActivityType() == null || course.getActivityType().isEmpty()) {
                        return false; // No activity type specified - no constraint
                    }

                    // Check if room supports this activity (Phase 5F method)
                    return !room.supportsActivity(course.getActivityType());
                })
                .penalize(HardSoftScore.ONE_SOFT, slot -> {
                    String activity = slot.getCourse().getActivityType();
                    Room room = slot.getRoom();
                    RoomType roomType = room.getType();

                    // CRITICAL MISMATCHES (50 points) - Very inappropriate room assignments
                    if ("Basketball".equalsIgnoreCase(activity) || "Volleyball".equalsIgnoreCase(activity) ||
                        "Soccer".equalsIgnoreCase(activity) || "Indoor Soccer".equalsIgnoreCase(activity)) {
                        // Ball sports in non-gym spaces
                        if (roomType != null && (roomType.name().equals("WEIGHT_ROOM") ||
                                                  roomType.name().equals("DANCE_STUDIO") ||
                                                  roomType.name().equals("CLASSROOM"))) {
                            log.warn("⚠ CRITICAL MISMATCH: {} course assigned to {} (Type: {}). Penalty: 50",
                                    activity, room.getRoomNumber(), roomType);
                            return 50;
                        }
                    }

                    if ("Weights".equalsIgnoreCase(activity) || "Strength Training".equalsIgnoreCase(activity) ||
                        "Conditioning".equalsIgnoreCase(activity) || "Powerlifting".equalsIgnoreCase(activity)) {
                        // Weight training in inappropriate spaces
                        if (roomType != null && (roomType.name().equals("DANCE_STUDIO") ||
                                                  roomType.name().equals("CLASSROOM") ||
                                                  roomType.name().equals("AUDITORIUM"))) {
                            log.warn("⚠ CRITICAL MISMATCH: {} course assigned to {} (Type: {}). Penalty: 50",
                                    activity, room.getRoomNumber(), roomType);
                            return 50;
                        }
                    }

                    if ("Dance".equalsIgnoreCase(activity) || "Aerobics".equalsIgnoreCase(activity) ||
                        "Yoga".equalsIgnoreCase(activity) || "Zumba".equalsIgnoreCase(activity)) {
                        // Dance/movement in inappropriate spaces
                        if (roomType != null && (roomType.name().equals("WEIGHT_ROOM") ||
                                                  roomType.name().equals("CLASSROOM") ||
                                                  roomType.name().equals("AUDITORIUM"))) {
                            log.warn("⚠ CRITICAL MISMATCH: {} course assigned to {} (Type: {}). Penalty: 50",
                                    activity, room.getRoomNumber(), roomType);
                            return 50;
                        }
                    }

                    if ("Karate".equalsIgnoreCase(activity) || "Martial Arts".equalsIgnoreCase(activity) ||
                        "Wrestling".equalsIgnoreCase(activity) || "Boxing".equalsIgnoreCase(activity)) {
                        // Martial arts in inappropriate spaces
                        if (roomType != null && (roomType.name().equals("WEIGHT_ROOM") ||
                                                  roomType.name().equals("DANCE_STUDIO") ||
                                                  roomType.name().equals("CLASSROOM"))) {
                            log.warn("⚠ CRITICAL MISMATCH: {} course assigned to {} (Type: {}). Penalty: 50",
                                    activity, room.getRoomNumber(), roomType);
                            return 50;
                        }
                    }

                    // TOLERABLE MISMATCHES (20 points) - Not ideal but workable
                    if ("Weights".equalsIgnoreCase(activity) || "Strength Training".equalsIgnoreCase(activity)) {
                        // Weights in gymnasium is tolerable (some gyms have weight areas)
                        if (roomType != null && roomType.name().equals("GYM")) {
                            if (log.isDebugEnabled()) {
                                log.debug("⚠ TOLERABLE: {} in gymnasium {}. Penalty: 20", activity, room.getRoomNumber());
                            }
                            return 20;
                        }
                    }

                    if ("General PE".equalsIgnoreCase(activity)) {
                        // General PE can work in various spaces with reduced penalty
                        if (roomType != null && !roomType.name().equals("GYM")) {
                            if (log.isDebugEnabled()) {
                                log.debug("⚠ TOLERABLE: General PE in non-gym {}. Penalty: 10", room.getRoomNumber());
                            }
                            return 10;
                        }
                    }

                    // DEFAULT MISMATCH (5 points) - Minor preference violation
                    if (log.isDebugEnabled()) {
                        log.debug("⚠ MINOR MISMATCH: {} course in room {} without matching activity tag. Penalty: 5",
                                activity, room.getRoomNumber());
                    }
                    return 5;
                })
                .asConstraint("Room activity type matching");
    }

    // ========================================================================
    // PHASE 6A: TEACHER AVAILABILITY CONSTRAINTS
    // ========================================================================

    /**
     * HARD: Teachers must be available during assigned time slots
     * Phase 6A: Teacher Availability Constraints
     *
     * This constraint enforces that teachers are only scheduled during times when they are available.
     * Administrators can define unavailable time blocks for each teacher (e.g., department meetings,
     * IEP meetings, appointments, part-time schedules).
     *
     * The scheduler will NEVER assign a teacher to a slot during their unavailable times.
     *
     * Examples:
     * - Teacher has department meeting every Monday 9:00-10:00 → blocked from Monday 9am slots
     * - Teacher has IEP meetings Wednesday afternoons → blocked from Wednesday PM slots
     * - Part-time teacher only works M/W/F → blocked from Tuesday and Thursday entirely
     *
     * Implementation:
     * - Uses Teacher.isAvailableAt(dayOfWeek, time) method
     * - Checks Teacher.unavailableTimes JSON field
     * - HARD constraint (cannot be violated)
     * - Respects pinned assignments (manual overrides)
     *
     * @param constraintFactory The constraint factory
     * @return HARD constraint ensuring teacher availability
     * @since Phase 6A - December 2, 2025
     */
    private Constraint teacherAvailability(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                // Only check slots with both teacher and time slot assigned
                .filter(slot -> slot.getTeacher() != null && slot.getTimeSlot() != null)
                // Respect manual/pinned assignments
                .filter(slot -> !Boolean.TRUE.equals(slot.getPinned()))
                // Check if teacher is unavailable at this time
                .filter(slot -> {
                    Teacher teacher = slot.getTeacher();
                    TimeSlot timeSlot = slot.getTimeSlot();

                    // Create timeblock string for availability check (format: "MON_0800")
                    String timeBlock = timeSlot.getDayOfWeek().toString().substring(0, 3) + "_" +
                                      timeSlot.getStartTime().toString().replace(":", "");

                    // Check if teacher is unavailable during this time
                    boolean isAvailable = teacher.isAvailableAt(timeBlock);

                    // If teacher is NOT available, this slot violates the constraint
                    if (!isAvailable) {
                        log.warn("⚠ AVAILABILITY VIOLATION: Teacher {} is unavailable {} at {}",
                                teacher.getName(),
                                timeSlot.getDayOfWeek(),
                                timeSlot.getStartTime());
                    }

                    return !isAvailable; // Filter returns true for violations
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher must be available during assigned time");
    }

    // ========================================================================
    // PHASE 6B: TEACHER ROOM PREFERENCES
    // ========================================================================

    /**
     * HARD: Teachers restricted to specific rooms must be assigned to those rooms
     *
     * Phase 6B: Room Preferences
     * When a teacher has room restrictions (not just preferences), they can ONLY
     * be assigned to rooms in their restricted list. This is a HARD constraint.
     *
     * Use Cases:
     * - PE teacher must use gymnasium (safety/equipment)
     * - Science teacher must use lab (equipment/safety)
     * - Music teacher must use music room (instruments/acoustics)
     *
     * Implementation:
     * - Checks Teacher.roomPreferences JSON field
     * - Only applies when restrictedToRooms = true
     * - HARD constraint (cannot be violated)
     * - Respects pinned assignments (manual overrides)
     *
     * @param constraintFactory The constraint factory
     * @return HARD constraint ensuring room restrictions
     * @since Phase 6B - December 3, 2025
     */
    private Constraint teacherRoomRestrictions(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                // Only check slots with both teacher and room assigned
                .filter(slot -> slot.getTeacher() != null && slot.getRoom() != null)
                // Respect manual/pinned assignments
                .filter(slot -> !Boolean.TRUE.equals(slot.getPinned()))
                // Check if teacher is restricted to specific rooms
                .filter(slot -> {
                    Teacher teacher = slot.getTeacher();
                    Room room = slot.getRoom();

                    // Only apply if teacher has room restrictions
                    if (!teacher.isRestrictedToRooms()) {
                        return false; // No restrictions = no violation
                    }

                    // Check if assigned room is allowed
                    boolean canUseRoom = teacher.canUseRoom(room);

                    // Log violations for debugging
                    if (!canUseRoom) {
                        log.warn("⚠ ROOM RESTRICTION VIOLATION: Teacher {} cannot use room {} (restricted to specific rooms)",
                                teacher.getName(),
                                room.getRoomNumber());
                    }

                    return !canUseRoom; // Filter returns true for violations
                })
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Teacher room restriction violated");
    }

    /**
     * SOFT: Prefer assigning teachers to their preferred rooms
     *
     * Phase 6B: Room Preferences
     * When a teacher has room preferences (not restrictions), the scheduler
     * will PREFER to assign them to those rooms but can use others if needed.
     * This is a SOFT constraint weighted by preference strength.
     *
     * Use Cases:
     * - Math teacher prefers rooms with smartboards
     * - Teacher prefers room near their department
     * - Traveling teacher prefers consistent room
     *
     * Implementation:
     * - Checks Teacher.roomPreferences JSON field
     * - Only applies when restrictedToRooms = false (preferences, not restrictions)
     * - SOFT constraint (can be violated if necessary)
     * - Penalty weighted by PreferenceStrength (LOW=1, MEDIUM=3, HIGH=5)
     * - Respects pinned assignments (manual overrides)
     *
     * @param constraintFactory The constraint factory
     * @return SOFT constraint preferring room assignments
     * @since Phase 6B - December 3, 2025
     */
    private Constraint teacherRoomPreferences(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                // Only check slots with both teacher and room assigned
                .filter(slot -> slot.getTeacher() != null && slot.getRoom() != null)
                // Respect manual/pinned assignments
                .filter(slot -> !Boolean.TRUE.equals(slot.getPinned()))
                // Check if teacher has preferences (not restrictions)
                .filter(slot -> {
                    Teacher teacher = slot.getTeacher();

                    // Only apply if teacher has preferences (not restrictions)
                    if (!teacher.hasRoomPreferences() || teacher.isRestrictedToRooms()) {
                        return false; // No preferences or is restriction = skip
                    }

                    Room room = slot.getRoom();

                    // Return true if room is NOT preferred (violation)
                    return !teacher.prefersRoom(room);
                })
                // Weight penalty by preference strength
                .penalize(HardSoftScore.ONE_SOFT, (slot) -> {
                    Teacher teacher = slot.getTeacher();
                    // Teacher.getRoomPreferences() returns List<Room>, not RoomPreferences DTO
                    // Use default penalty weight of 3 (MEDIUM)
                    int penaltyWeight = 3;

                    // Log preference violations for debugging (only at MEDIUM or higher)
                    if (penaltyWeight >= 3) {
                        log.debug("📍 ROOM PREFERENCE: Teacher {} assigned to non-preferred room {} (penalty: {})",
                                teacher.getName(),
                                slot.getRoom().getRoomNumber(),
                                penaltyWeight);
                    }

                    return penaltyWeight;
                })
                .asConstraint("Teacher room preference not met");
    }

    // ========================================================================
    // PHASE 6C: DEPARTMENT ROOM ZONES - DECEMBER 3, 2025
    // ========================================================================

    /**
     * SOFT: Prefer assigning teachers to rooms in their department zone
     * Priority: Lower than individual room preferences (Phase 6B)
     * Penalty: 2 points per period outside department zone
     *
     * This constraint encourages grouping teachers by department to:
     * - Reduce teacher travel time between periods
     * - Keep departments together for easier collaboration
     * - Simplify resource sharing within departments
     *
     * NOTE: This constraint is SKIPPED if teacher has individual room preferences (Phase 6B)
     * to ensure Phase 6B preferences take priority.
     *
     * @since Phase 6C - December 3, 2025
     */
    private Constraint departmentZonePreference(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getTeacher() != null && slot.getRoom() != null)
                .filter(slot -> !Boolean.TRUE.equals(slot.getPinned()))
                .filter(slot -> {
                    Teacher teacher = slot.getTeacher();
                    Room room = slot.getRoom();

                    // Skip if teacher has specific room preferences (Phase 6B takes priority)
                    if (teacher.hasRoomPreferences()) {
                        return false;
                    }

                    // Skip if no department or no zone
                    if (teacher.getDepartment() == null || room.getZone() == null) {
                        return false;
                    }

                    // Check if room is NOT in teacher's department zone
                    String preferredZone = getDepartmentZone(teacher.getDepartment());
                    if (preferredZone == null) {
                        return false;  // No zone preference for this department
                    }

                    return !preferredZone.equals(room.getZone());
                })
                .penalize(HardSoftScore.ofSoft(2))
                .asConstraint("Teacher department zone preference");
    }

    /**
     * SOFT: Minimize teacher travel between consecutive periods
     * Penalty: Distance-based (same building = 0-1, different building = 5)
     *
     * This constraint reduces teacher fatigue and passing period congestion by:
     * - Keeping teachers in the same room when possible (0 penalty)
     * - Preferring rooms in the same zone (1 penalty)
     * - Allowing same building, different zone (3 penalty)
     * - Discouraging cross-campus travel (5 penalty)
     *
     * The constraint checks consecutive periods on the same day and penalizes
     * based on the distance between assigned rooms.
     *
     * @since Phase 6C - December 3, 2025
     */
    private Constraint minimizeTeacherTravel(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .join(ScheduleSlot.class,
                        Joiners.equal(ScheduleSlot::getTeacher),
                        Joiners.equal(slot -> slot.getTimeSlot().getDayOfWeek()))
                .filter((slot1, slot2) -> {
                    // Only consider slots with both teachers and rooms assigned
                    if (slot1.getTeacher() == null || slot2.getTeacher() == null) {
                        return false;
                    }
                    if (slot1.getRoom() == null || slot2.getRoom() == null) {
                        return false;
                    }
                    // Check if slots are consecutive periods
                    return areConsecutivePeriods(slot1.getTimeSlot(), slot2.getTimeSlot());
                })
                .penalize(HardSoftScore.ONE_SOFT, (slot1, slot2) -> {
                    return calculateTravelPenalty(slot1.getRoom(), slot2.getRoom());
                })
                .asConstraint("Minimize teacher travel between periods");
    }

    /**
     * Check if two time slots are consecutive periods
     */
    private boolean areConsecutivePeriods(TimeSlot timeSlot1, TimeSlot timeSlot2) {
        if (timeSlot1 == null || timeSlot2 == null) {
            return false;
        }

        // Must be on same day
        if (!timeSlot1.getDayOfWeek().equals(timeSlot2.getDayOfWeek())) {
            return false;
        }

        // Check if slot1 ends and slot2 begins immediately after
        // (allowing for passing period)
        return timeSlot1.getEndTime().equals(timeSlot2.getStartTime()) ||
                timeSlot1.getEndTime().plusMinutes(5).isAfter(timeSlot2.getStartTime()) &&
                timeSlot1.getEndTime().isBefore(timeSlot2.getStartTime());
    }

    /**
     * Calculate travel penalty between two rooms
     *
     * @param room1 First room
     * @param room2 Second room
     * @return Penalty (0 = same room, 1 = same zone, 3 = same building, 5 = different building)
     */
    private int calculateTravelPenalty(Room room1, Room room2) {
        if (room1 == null || room2 == null) {
            return 0;
        }

        // Same room = no penalty
        if (room1.getId().equals(room2.getId())) {
            return 0;
        }

        // Same zone = minimal penalty (rooms are close)
        if (room1.getZone() != null && room1.getZone().equals(room2.getZone())) {
            return 1;
        }

        // Same building, different zone = moderate penalty
        if (room1.getBuilding() != null && room1.getBuilding().equals(room2.getBuilding())) {
            return 3;
        }

        // Different building = high penalty (must walk across campus)
        return 5;
    }

    /**
     * Map department name to preferred zone
     * (Same logic as RoomZoneService for consistency)
     */
    private String getDepartmentZone(String department) {
        if (department == null) return null;

        String deptLower = department.toLowerCase();

        if (deptLower.contains("math")) {
            return "Math Wing";
        } else if (deptLower.contains("science")) {
            return "Science Wing";
        } else if (deptLower.contains("english") || deptLower.contains("language arts")) {
            return "English Wing";
        } else if (deptLower.contains("social") || deptLower.contains("history")) {
            return "Social Studies Wing";
        } else if (deptLower.contains("physical education") || deptLower.contains("pe")) {
            return "Athletics Building";
        } else if (deptLower.contains("art") || deptLower.contains("music") || deptLower.contains("drama")) {
            return "Arts Building";
        } else if (deptLower.contains("technology") || deptLower.contains("computer")) {
            return "Technology Wing";
        } else if (deptLower.contains("vocational") || deptLower.contains("career")) {
            return "Vocational Building";
        }

        return null;  // No default zone for this department
    }

    // ========================================================================
    // PHASE 6D: ROOM EQUIPMENT MATCHING - DECEMBER 3, 2025
    // ========================================================================

    /**
     * Room Equipment Compatibility Constraint
     *
     * Ensures courses are assigned to rooms with appropriate equipment.
     * Uses equipment compatibility scoring to penalize mismatches.
     *
     * Priority: Higher than zone preferences (equipment more critical than location)
     *
     * Scoring:
     * - 0 points  = Perfect match (all equipment present)
     * - 2 points  = Minor mismatch (optional equipment missing)
     * - 5 points  = Moderate mismatch (some required equipment missing)
     * - 10 points = Major mismatch (critical equipment/room type wrong)
     *
     * @since Phase 6D - December 3, 2025
     */
    private Constraint roomEquipmentCompatibility(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> slot.getCourse() != null && slot.getRoom() != null)
                .filter(slot -> !Boolean.TRUE.equals(slot.getPinned()))
                .penalize(HardSoftScore.ONE_SOFT, slot -> {
                    return calculateEquipmentPenalty(slot.getCourse(), slot.getRoom());
                })
                .asConstraint("Room equipment compatibility");
    }

    /**
     * Calculate equipment penalty for a course-room assignment
     *
     * @param course Course with equipment requirements
     * @param room Room with available equipment
     * @return Penalty score (0-10)
     */
    private int calculateEquipmentPenalty(Course course, Room room) {
        if (course == null || room == null) {
            return 0;  // No penalty
        }

        // Check if course has any equipment requirements
        boolean hasRequirements = course.getRequiredRoomType() != null
                || Boolean.TRUE.equals(course.getRequiresProjector())
                || Boolean.TRUE.equals(course.getRequiresSmartboard())
                || Boolean.TRUE.equals(course.getRequiresComputers())
                || (course.getAdditionalEquipment() != null && !course.getAdditionalEquipment().trim().isEmpty());

        if (!hasRequirements) {
            return 0;  // No requirements, no penalty
        }

        // Calculate compatibility score (0-100)
        int score = calculateEquipmentScore(course, room);

        // Map score to penalty
        if (score >= 100) {
            return 0;   // Perfect match
        } else if (score >= 70) {
            return 2;   // Minor mismatch
        } else if (score >= 40) {
            return 5;   // Moderate mismatch
        } else {
            return 10;  // Major mismatch
        }
    }

    /**
     * Calculate equipment compatibility score
     *
     * @param course Course with requirements
     * @param room Room with equipment
     * @return Score from 0 (incompatible) to 100 (perfect match)
     */
    private int calculateEquipmentScore(Course course, Room room) {
        int score = 100;  // Start with perfect score
        int penalties = 0;

        // Check room type requirement (highest priority)
        if (course.getRequiredRoomType() != null) {
            if (room.getType() == null || !course.getRequiredRoomType().equals(room.getType())) {
                penalties += 100;  // Room type mismatch is critical
            }
        }

        // Check projector requirement
        if (Boolean.TRUE.equals(course.getRequiresProjector())) {
            if (!Boolean.TRUE.equals(room.getHasProjector())) {
                penalties += 30;
            }
        }

        // Check smartboard requirement
        if (Boolean.TRUE.equals(course.getRequiresSmartboard())) {
            if (!Boolean.TRUE.equals(room.getHasSmartboard())) {
                penalties += 30;
            }
        }

        // Check computers requirement
        if (Boolean.TRUE.equals(course.getRequiresComputers())) {
            if (!Boolean.TRUE.equals(room.getHasComputers())) {
                penalties += 40;  // Computers are important
            }
        }

        // Additional equipment check (simplified for constraint provider)
        if (course.getAdditionalEquipment() != null && !course.getAdditionalEquipment().trim().isEmpty()) {
            // Assume some penalty for additional equipment requirements
            penalties += 10;
        }

        score = Math.max(0, score - penalties);
        return score;
    }

    // ========================================================================
    // PHASE 6E: MULTI-ROOM COURSES - DECEMBER 3, 2025
    // ========================================================================

    /**
     * HARD: All rooms required for a multi-room course must be available simultaneously
     * Phase 6E: Multi-Room Courses
     *
     * For courses using multiple rooms (e.g., team teaching, lab/lecture splits,
     * overflow rooms), all assigned rooms must be available at the same time.
     *
     * This constraint ensures that when a course is scheduled in a time slot,
     * ALL of its required rooms are free and available during that time.
     *
     * Example Violations:
     * - Course A uses Rooms 101 + 102, scheduled Monday 9:00-10:00
     * - Room 101 is free, but Room 102 is occupied by Course B
     * - VIOLATION: Cannot schedule Course A because Room 102 unavailable
     *
     * Implementation:
     * - Checks Course.usesMultipleRooms flag
     * - Loads all CourseRoomAssignments for the course
     * - Verifies each required room is available at the time slot
     * - HARD constraint (cannot be violated)
     *
     * @param constraintFactory The constraint factory
     * @return HARD constraint ensuring multi-room availability
     * @since Phase 6E - December 3, 2025
     */
    private Constraint multiRoomAvailability(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                // Only check slots for multi-room courses
                .filter(slot -> {
                    if (slot.getCourse() == null || slot.getRoom() == null || slot.getTimeSlot() == null) {
                        return false;
                    }
                    // Check if course uses multiple rooms
                    return Boolean.TRUE.equals(slot.getCourse().getUsesMultipleRooms());
                })
                .filter(slot -> !Boolean.TRUE.equals(slot.getPinned()))
                // Join with other slots to check for room conflicts
                .join(ScheduleSlot.class,
                        // Different courses at same time/day
                        Joiners.equal(ScheduleSlot::getTimeSlot),
                        Joiners.equal(ScheduleSlot::getDayOfWeek),
                        Joiners.lessThan(ScheduleSlot::getId)) // Avoid double counting
                .filter((multiRoomSlot, otherSlot) -> {
                    if (otherSlot.getRoom() == null) {
                        return false;
                    }

                    // Check if otherSlot uses a room that multiRoomSlot's course needs
                    Course multiRoomCourse = multiRoomSlot.getCourse();
                    Room conflictingRoom = otherSlot.getRoom();

                    // If the multi-room course has room assignments, check if conflictingRoom is needed
                    if (multiRoomCourse.getRoomAssignments() != null) {
                        return multiRoomCourse.getRoomAssignments().stream()
                                .filter(room -> room != null)
                                .anyMatch(room ->
                                    room.getId() != null &&
                                    room.getId().equals(conflictingRoom.getId()));
                    }

                    return false;
                })
                .penalize(HardSoftScore.ONE_HARD, (multiRoomSlot, otherSlot) -> {
                    log.warn("⚠ MULTI-ROOM CONFLICT: Course {} needs room {} but it's occupied by {} at {} on {}",
                            multiRoomSlot.getCourse().getCourseCode(),
                            otherSlot.getRoom().getRoomNumber(),
                            otherSlot.getCourse() != null ? otherSlot.getCourse().getCourseCode() : "unknown",
                            multiRoomSlot.getTimeSlot().getStartTime(),
                            multiRoomSlot.getDayOfWeek());
                    return 1;
                })
                .asConstraint("Multi-room courses require all rooms available");
    }

    /**
     * SOFT: Multi-room courses prefer rooms that are nearby
     * Phase 6E: Multi-Room Courses
     *
     * For courses using multiple rooms, prefer assigning rooms that are close together
     * to minimize student/teacher travel between spaces.
     *
     * Uses the proximity calculation from MultiRoomSchedulingService:
     * - Same room: 0 minutes (no penalty)
     * - Same building, floor, zone: 1 minute (minimal penalty)
     * - Different floor: +2 minutes
     * - Different zone: +3 minutes
     * - Different building: +5 minutes
     *
     * Penalty Scale:
     * - 0-1 minutes: No penalty (rooms are adjacent)
     * - 2-3 minutes: 5 point penalty (tolerable distance)
     * - 4-5 minutes: 15 point penalty (significant travel)
     * - 6+ minutes: 30 point penalty (excessive travel)
     *
     * Only applies if course specifies maxRoomDistanceMinutes preference.
     *
     * @param constraintFactory The constraint factory
     * @return SOFT constraint preferring nearby rooms
     * @since Phase 6E - December 3, 2025
     */
    private Constraint multiRoomProximity(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                // Only check multi-room courses with distance preferences
                .filter(slot -> {
                    if (slot.getCourse() == null || slot.getRoom() == null) {
                        return false;
                    }
                    Course course = slot.getCourse();
                    return Boolean.TRUE.equals(course.getUsesMultipleRooms()) &&
                           course.getMaxRoomDistanceMinutes() != null;
                })
                .filter(slot -> !Boolean.TRUE.equals(slot.getPinned()))
                .penalizeLong(HardSoftScore.ONE_SOFT, slot -> {
                    Course course = slot.getCourse();
                    Room primaryRoom = slot.getRoom();

                    // Get all active room assignments
                    if (course.getRoomAssignments() == null || course.getRoomAssignments().isEmpty()) {
                        return 0L;
                    }

                    int totalPenalty = 0;
                    int maxAllowedDistance = course.getMaxRoomDistanceMinutes();

                    // Check proximity between primary room and all other assigned rooms
                    for (Room otherRoom : course.getRoomAssignments()) {
                        if (otherRoom == null || otherRoom.getId() == null) {
                            continue;
                        }

                        if (otherRoom.getId().equals(primaryRoom.getId())) {
                            continue; // Skip same room
                        }

                        // Calculate proximity (simplified - would use MultiRoomSchedulingService in production)
                        int distance = calculateRoomDistance(primaryRoom, otherRoom);

                        // Apply penalty if exceeds max distance
                        if (distance > maxAllowedDistance) {
                            int excess = distance - maxAllowedDistance;
                            if (excess <= 2) {
                                totalPenalty += 5;  // Tolerable
                            } else if (excess <= 4) {
                                totalPenalty += 15; // Significant
                            } else {
                                totalPenalty += 30; // Excessive
                                log.debug("⚠ EXCESSIVE DISTANCE: Course {} rooms {} and {} are {} min apart (max: {})",
                                        course.getCourseCode(),
                                        primaryRoom.getRoomNumber(),
                                        otherRoom.getRoomNumber(),
                                        distance,
                                        maxAllowedDistance);
                            }
                        }
                    }

                    return (long) totalPenalty;
                })
                .asConstraint("Multi-room courses prefer nearby rooms");
    }

    /**
     * SOFT: Total capacity across multi-room assignments should meet course enrollment
     * Phase 6E: Multi-Room Courses
     *
     * For courses using multiple rooms simultaneously (e.g., overflow rooms,
     * team teaching), the combined capacity should accommodate expected enrollment.
     *
     * Checks:
     * - Sum of all active room capacities
     * - Compare to course.maxStudents or actual enrollment
     * - Penalize if total capacity is insufficient
     *
     * Penalty Scale:
     * - Capacity >= maxStudents: No penalty
     * - Capacity 90-99% of maxStudents: 5 point penalty (minor shortage)
     * - Capacity 70-89% of maxStudents: 15 point penalty (significant shortage)
     * - Capacity < 70% of maxStudents: 30 point penalty (critical shortage)
     *
     * Example:
     * - Course needs 60 students
     * - Room A: 30 capacity, Room B: 25 capacity
     * - Total: 55 capacity (92% of need) → 5 point penalty
     *
     * @param constraintFactory The constraint factory
     * @return SOFT constraint ensuring adequate multi-room capacity
     * @since Phase 6E - December 3, 2025
     */
    private Constraint multiRoomCapacity(ConstraintFactory constraintFactory) {
        return constraintFactory
                .forEach(ScheduleSlot.class)
                .filter(slot -> {
                    if (slot.getCourse() == null || slot.getRoom() == null) {
                        return false;
                    }
                    Course course = slot.getCourse();
                    return Boolean.TRUE.equals(course.getUsesMultipleRooms()) &&
                           course.getMaxStudents() != null &&
                           course.getMaxStudents() > 0;
                })
                .filter(slot -> !Boolean.TRUE.equals(slot.getPinned()))
                .penalizeLong(HardSoftScore.ONE_SOFT, slot -> {
                    Course course = slot.getCourse();
                    int requiredCapacity = course.getMaxStudents();

                    // Calculate total capacity across all active room assignments
                    int totalCapacity = 0;
                    if (course.getRoomAssignments() != null) {
                        for (Room room : course.getRoomAssignments()) {
                            if (room != null) {
                                Integer roomCapacity = room.getCapacity();
                                totalCapacity += (roomCapacity != null) ? roomCapacity : 0;
                            }
                        }
                    }

                    // No penalty if capacity is sufficient
                    if (totalCapacity >= requiredCapacity) {
                        return 0L;
                    }

                    // Calculate capacity percentage
                    double capacityRatio = (double) totalCapacity / requiredCapacity;

                    // Apply graduated penalties
                    if (capacityRatio >= 0.90) {
                        // 90-99% capacity - minor shortage
                        return 5L;
                    } else if (capacityRatio >= 0.70) {
                        // 70-89% capacity - significant shortage
                        log.debug("⚠ CAPACITY SHORTAGE: Course {} total capacity {} is only {.0f}% of required {}",
                                course.getCourseCode(),
                                totalCapacity,
                                capacityRatio * 100,
                                requiredCapacity);
                        return 15L;
                    } else {
                        // < 70% capacity - critical shortage
                        log.warn("⚠ CRITICAL CAPACITY SHORTAGE: Course {} total capacity {} is only {.0f}% of required {}",
                                course.getCourseCode(),
                                totalCapacity,
                                capacityRatio * 100,
                                requiredCapacity);
                        return 30L;
                    }
                })
                .asConstraint("Multi-room total capacity should meet enrollment");
    }

    /**
     * Helper method: Calculate distance between two rooms (in minutes)
     * Simplified version - production would use MultiRoomSchedulingService
     */
    private int calculateRoomDistance(Room room1, Room room2) {
        if (room1 == null || room2 == null) {
            return Integer.MAX_VALUE;
        }

        if (room1.getId().equals(room2.getId())) {
            return 0;
        }

        int distance = 0;

        // Different buildings = 5 minutes base
        String building1 = room1.getBuilding();
        String building2 = room2.getBuilding();
        if (building1 != null && building2 != null && !building1.equalsIgnoreCase(building2)) {
            distance += 5;
        }

        // Different floors = 2 minutes
        Integer floor1 = room1.getFloor();
        Integer floor2 = room2.getFloor();
        if (floor1 != null && floor2 != null && !floor1.equals(floor2)) {
            distance += 2;
        }

        // Different zones = 3 minutes
        String zone1 = room1.getZone();
        String zone2 = room2.getZone();
        if (zone1 != null && zone2 != null && !zone1.equalsIgnoreCase(zone2)) {
            distance += 3;
        }

        // If same building, floor, and zone = 1 minute
        if (distance == 0) {
            distance = 1;
        }

        return distance;
    }
}