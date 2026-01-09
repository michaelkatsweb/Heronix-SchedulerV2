package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;
import java.util.List;
import java.util.Map;

public interface MasterScheduleService {

    // ========== SINGLETON MANAGEMENT ==========

    /**
     * Identify singleton courses (single section) for priority scheduling
     */
    List<CourseSection> identifySingletons(Integer year);

    /**
     * Schedule singletons first to avoid conflicts
     */
    void scheduleSingletons(List<CourseSection> singletons);

    /**
     * Check if course is singleton based on demand
     */
    boolean isSingleton(Course course, Integer year);

    // ========== SECTION BALANCING ==========

    /**
     * Balance enrollment across sections of same course
     */
    void balanceSections(Course course, int tolerance);

    /**
     * Get section balance report for course
     */
    Map<String, Object> getSectionBalanceReport(Course course);

    /**
     * Redistribute students to balance sections
     */
    void redistributeStudents(List<CourseSection> sections);

    /**
     * Balance by demographics (gender, ethnicity)
     */
    void balanceByDemographics(List<CourseSection> sections);

    // ========== WAITLIST PROCESSING ==========

    /**
     * Process waitlist for course section when seat opens
     */
    void processWaitlist(CourseSection section);

    /**
     * Add student to waitlist with priority
     */
    Waitlist addToWaitlist(Student student, Course course, Integer priorityWeight);

    /**
     * Auto-enroll from waitlist when seat available
     */
    boolean enrollFromWaitlist(CourseSection section);

    /**
     * Check if student can be enrolled (no conflicts/holds)
     */
    boolean canEnrollStudent(Student student, CourseSection section);

    // ========== COMMON PLANNING TIME ==========

    /**
     * Assign common planning periods for departments
     */
    void assignCommonPlanningTime(String department, Integer period);

    /**
     * Get recommended planning periods for collaboration
     */
    List<Integer> recommendPlanningPeriods(List<Teacher> teachers);

    /**
     * Ensure minimum planning periods for all teachers
     */
    void ensureMinimumPlanningTime(Integer minPeriods);

    // ========== SCHEDULE VALIDATION ==========

    /**
     * Validate entire master schedule for conflicts
     */
    List<String> validateMasterSchedule(Schedule schedule);

    /**
     * Check singleton conflict resolution
     */
    boolean areSingletonsConflictFree(Integer year);

    /**
     * Verify section balance within tolerance
     */
    boolean verifySectionBalance(int tolerance);
}
