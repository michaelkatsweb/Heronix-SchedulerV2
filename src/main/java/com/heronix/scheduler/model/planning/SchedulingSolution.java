package com.heronix.scheduler.model.planning;

import com.heronix.scheduler.model.domain.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;

import java.util.ArrayList;
import java.util.List;

/**
 * OptaPlanner Planning Solution - CONSOLIDATED VERSION
 * Location: src/main/java/com/eduscheduler/model/planning/SchedulingSolution.java
 *
 * This class represents the complete scheduling problem and solution for OptaPlanner.
 * It contains both the problem facts (inputs) and planning entities (what OptaPlanner modifies).
 *
 * PROBLEM FACTS (Fixed inputs):
 * - Available time slots (when classes can be scheduled)
 * - Available teachers (who can teach)
 * - Available rooms (where classes can be held)
 * - Courses to be scheduled (what needs to be scheduled)
 * - Students enrolled in courses
 *
 * PLANNING ENTITIES (OptaPlanner modifies these):
 * - ScheduleSlots: OptaPlanner assigns teacher, room, and time to each slot
 *
 * SCORE:
 * - Hard score: constraint violations (must be 0 for valid solution)
 * - Soft score: optimization quality (higher is better)
 *
 * Example score: 0hard/-50soft means valid solution with room for optimization
 *                -5hard/-20soft means invalid (5 hard constraint violations)
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0 - CONSOLIDATED from solver.ScheduleSolution
 * @since 2025-11-03
 */
@PlanningSolution
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SchedulingSolution {

    // ========================================================================
    // PLANNING ENTITIES (Modified by OptaPlanner to find solution)
    // ========================================================================

    /**
     * Schedule slots to be assigned
     *
     * These are the planning entities that OptaPlanner modifies.
     * Each slot has a course assigned, and OptaPlanner will assign:
     * - A time slot (when)
     * - A teacher (who)
     * - A room (where)
     */
    @PlanningEntityCollectionProperty
    private List<ScheduleSlot> scheduleSlots;

    // ========================================================================
    // PROBLEM FACTS (Input data - not modified by OptaPlanner)
    // ========================================================================

    /**
     * Available teachers
     * These define WHO can teach classes
     */
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "teacherRange")
    private List<Teacher> teachers;

    /**
     * Available rooms
     * These define WHERE classes can be held
     */
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "roomRange")
    private List<Room> rooms;

    /**
     * Available time slots for scheduling
     * These define WHEN classes can be scheduled (days and times)
     */
    @ProblemFactCollectionProperty
    @ValueRangeProvider(id = "timeSlotRange")
    private List<TimeSlot> timeSlots;

    /**
     * Courses that need to be scheduled
     * These define WHAT needs to be scheduled
     */
    @ProblemFactCollectionProperty
    private List<Course> courses;

    /**
     * Students enrolled in courses
     * Used for student conflict detection
     */
    @ProblemFactCollectionProperty
    private List<Student> students;

    /**
     * ✅ PRIORITY 2 FIX December 15, 2025: Student lunch wave assignments
     * Used for validating lunch wave constraints
     * Provides OptaPlanner with student-to-lunch-wave mappings
     */
    @ProblemFactCollectionProperty
    private List<StudentLunchAssignment> studentLunchAssignments;

    /**
     * ✅ PRIORITY 2 FIX December 15, 2025: Teacher lunch wave assignments
     * Used for validating teacher lunch wave constraints
     * Provides OptaPlanner with teacher-to-lunch-wave mappings
     */
    @ProblemFactCollectionProperty
    private List<TeacherLunchAssignment> teacherLunchAssignments;

    // ========================================================================
    // SCORE (Solution quality metric)
    // ========================================================================

    /**
     * Score representing the quality of this solution
     *
     * Hard score: Number of hard constraint violations (must be 0)
     * Soft score: Optimization quality (higher is better)
     *
     * Examples:
     * - 0hard/0soft: Perfect solution (all constraints satisfied, fully optimized)
     * - 0hard/-100soft: Valid solution but room for optimization
     * - -3hard/-50soft: Invalid (3 hard constraint violations)
     */
    @PlanningScore
    private HardSoftScore score;

    // ========================================================================
    // CONSTRUCTORS (Backward compatibility)
    // ========================================================================

    /**
     * Constructor without score parameter (backward compatibility)
     *
     * This constructor exists for backward compatibility with existing code
     * that creates SchedulingSolution instances without specifying a score.
     * The score will be calculated by OptaPlanner during solving.
     *
     * @param scheduleSlots The schedule slots to be assigned
     * @param teachers Available teachers
     * @param rooms Available rooms
     * @param timeSlots Available time slots
     * @param courses Courses that need to be scheduled
     * @param students Students enrolled in courses
     */
    public SchedulingSolution(List<ScheduleSlot> scheduleSlots,
                              List<Teacher> teachers,
                              List<Room> rooms,
                              List<TimeSlot> timeSlots,
                              List<Course> courses,
                              List<Student> students) {
        this(scheduleSlots, teachers, rooms, timeSlots, courses, students,
             new ArrayList<>(), new ArrayList<>(), null);
    }

    // ========================================================================
    // HELPER METHODS (For analyzing solution quality)
    // ========================================================================

    /**
     * Get count of unassigned slots
     *
     * A slot is unassigned if it doesn't have both a teacher and room assigned.
     *
     * @return Number of slots that still need assignment
     */
    public int getUnassignedCount() {
        if (scheduleSlots == null || scheduleSlots.isEmpty()) {
            return 0;
        }

        return (int) scheduleSlots.stream()
            .filter(slot -> slot.getTeacher() == null ||
                           slot.getRoom() == null ||
                           slot.getTimeSlot() == null)
            .count();
    }

    /**
     * Get count of fully assigned slots
     *
     * A slot is fully assigned if it has teacher, room, AND time slot assigned.
     *
     * @return Number of completely scheduled slots
     */
    public int getAssignedCount() {
        if (scheduleSlots == null || scheduleSlots.isEmpty()) {
            return 0;
        }

        return (int) scheduleSlots.stream()
            .filter(slot -> slot.getTeacher() != null &&
                           slot.getRoom() != null &&
                           slot.getTimeSlot() != null)
            .count();
    }

    /**
     * Get total number of schedule slots
     *
     * @return Total slots (assigned + unassigned)
     */
    public int getTotalSlots() {
        return scheduleSlots != null ? scheduleSlots.size() : 0;
    }

    /**
     * Check if solution is complete
     *
     * A solution is complete when all slots are fully assigned.
     * Note: A complete solution may still have a negative score if
     * there are constraint violations.
     *
     * @return true if all slots are assigned, false otherwise
     */
    public boolean isComplete() {
        return getTotalSlots() > 0 && getUnassignedCount() == 0;
    }

    /**
     * Check if solution is valid
     *
     * A solution is valid when there are no hard constraint violations.
     * This is indicated by a hard score of 0.
     *
     * @return true if solution has no hard constraint violations
     */
    public boolean isValid() {
        return score != null && score.hardScore() == 0;
    }

    /**
     * Check if solution is feasible
     *
     * A solution is feasible if it's both complete and valid.
     * This is the minimum requirement for a usable schedule.
     *
     * @return true if solution is complete and valid
     */
    public boolean isFeasible() {
        return isComplete() && isValid();
    }

    /**
     * Get percentage of slots assigned
     *
     * @return Percentage (0-100) of slots that are assigned
     */
    public double getAssignmentPercentage() {
        int total = getTotalSlots();
        if (total == 0) return 0.0;

        return (getAssignedCount() * 100.0) / total;
    }

    /**
     * Get hard score
     *
     * @return Number of hard constraint violations (negative number, 0 is good)
     */
    public int getHardScore() {
        return score != null ? score.hardScore() : 0;
    }

    /**
     * Get soft score
     *
     * @return Optimization quality score (higher is better)
     */
    public int getSoftScore() {
        return score != null ? score.softScore() : 0;
    }

    /**
     * Get summary of solution
     *
     * Provides a quick overview of the solution quality.
     * Useful for logging and debugging.
     *
     * @return Human-readable summary string
     */
    public String getSummary() {
        return String.format(
            "Score: %s | Assigned: %d/%d (%.1f%%) | Teachers: %d | Rooms: %d | TimeSlots: %d | %s",
            score != null ? score.toString() : "Not scored",
            getAssignedCount(),
            getTotalSlots(),
            getAssignmentPercentage(),
            teachers != null ? teachers.size() : 0,
            rooms != null ? rooms.size() : 0,
            timeSlots != null ? timeSlots.size() : 0,
            isFeasible() ? "✓ FEASIBLE" : (isValid() ? "⚠ INCOMPLETE" : "✗ INVALID")
        );
    }

    /**
     * Get detailed summary with constraint breakdown
     *
     * @return Detailed multi-line summary
     */
    public String getDetailedSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(70)).append("\n");
        sb.append("SCHEDULE SOLUTION SUMMARY\n");
        sb.append("=".repeat(70)).append("\n");

        // Score information
        sb.append(String.format("Score:           %s\n",
            score != null ? score.toString() : "Not scored"));
        sb.append(String.format("  Hard Score:    %d (constraint violations)\n", getHardScore()));
        sb.append(String.format("  Soft Score:    %d (optimization quality)\n", getSoftScore()));
        sb.append("\n");

        // Assignment information
        sb.append("Assignment Status:\n");
        sb.append(String.format("  Total Slots:   %d\n", getTotalSlots()));
        sb.append(String.format("  Assigned:      %d (%.1f%%)\n",
            getAssignedCount(), getAssignmentPercentage()));
        sb.append(String.format("  Unassigned:    %d\n", getUnassignedCount()));
        sb.append("\n");

        // Resource information
        sb.append("Available Resources:\n");
        sb.append(String.format("  Time Slots:    %d\n", timeSlots != null ? timeSlots.size() : 0));
        sb.append(String.format("  Teachers:      %d\n", teachers != null ? teachers.size() : 0));
        sb.append(String.format("  Rooms:         %d\n", rooms != null ? rooms.size() : 0));
        sb.append(String.format("  Courses:       %d\n", courses != null ? courses.size() : 0));
        sb.append(String.format("  Students:      %d\n", students != null ? students.size() : 0));
        sb.append("\n");

        // Feasibility
        sb.append("Solution Status:\n");
        sb.append(String.format("  Complete:      %s\n", isComplete() ? "✓ Yes" : "✗ No"));
        sb.append(String.format("  Valid:         %s\n", isValid() ? "✓ Yes" : "✗ No"));
        sb.append(String.format("  Feasible:      %s\n", isFeasible() ? "✓ Yes" : "✗ No"));

        sb.append("=".repeat(70)).append("\n");

        return sb.toString();
    }

    /**
     * Initialize empty lists if null (safety check)
     */
    public void initializeCollections() {
        if (timeSlots == null) timeSlots = new ArrayList<>();
        if (teachers == null) teachers = new ArrayList<>();
        if (rooms == null) rooms = new ArrayList<>();
        if (courses == null) courses = new ArrayList<>();
        if (students == null) students = new ArrayList<>();
        if (scheduleSlots == null) scheduleSlots = new ArrayList<>();
    }

    /**
     * Validate that all required data is present
     *
     * @return true if solution has minimum required data
     */
    public boolean hasRequiredData() {
        return timeSlots != null && !timeSlots.isEmpty()
            && teachers != null && !teachers.isEmpty()
            && rooms != null && !rooms.isEmpty()
            && courses != null && !courses.isEmpty()
            && scheduleSlots != null && !scheduleSlots.isEmpty();
    }
}