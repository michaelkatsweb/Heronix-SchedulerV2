package com.heronix.scheduler.solver;

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
 * Schedule Solution - OptaPlanner Planning Solution
 * Location: src/main/java/com/eduscheduler/solver/ScheduleSolution.java
 * 
 * This class represents the complete scheduling problem and solution for OptaPlanner.
 * It contains both the problem facts (inputs) and planning entities (what OptaPlanner modifies).
 * 
 * PROBLEM FACTS (Fixed inputs):
 * - Available time slots (when classes can be scheduled)
 * - Available teachers (who can teach)
 * - Available rooms (where classes can be held)
 * - Courses to be scheduled (what needs to be scheduled)
 * - Events that may block scheduling
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
 */
@PlanningSolution
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSolution {

    // ========================================================================
    // PROBLEM FACTS (Input data - not modified by OptaPlanner)
    // ========================================================================
    
    /**
     * Available time slots for scheduling
     * These define WHEN classes can be scheduled (days and times)
     * 
     * Example: Monday 8:00-8:50, Monday 9:00-9:50, etc.
     */
    @ValueRangeProvider(id = "timeSlotRange")
    @ProblemFactCollectionProperty
    private List<TimeSlot> timeSlots;
    
    /**
     * Available teachers
     * These define WHO can teach classes
     * 
     * OptaPlanner assigns these to schedule slots
     */
    @ValueRangeProvider(id = "teacherRange")
    @ProblemFactCollectionProperty
    private List<Teacher> teachers;
    
    /**
     * Available rooms
     * These define WHERE classes can be held
     * 
     * OptaPlanner assigns these to schedule slots
     */
    @ValueRangeProvider(id = "roomRange")
    @ProblemFactCollectionProperty
    private List<Room> rooms;
    
    /**
     * Courses that need to be scheduled
     * These define WHAT needs to be scheduled
     * 
     * Each course may need multiple schedule slots (sessions per week)
     */
    @ProblemFactCollectionProperty
    private List<Course> courses;
    
    /**
     * Events that may block scheduling
     * These define when normal scheduling is blocked (assemblies, holidays, etc.)
     * 
     * OptaPlanner must avoid scheduling classes during blocking events
     */
    @ProblemFactCollectionProperty
    private List<Event> events;

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
     * 
     * OptaPlanner tries different combinations to find the best assignment
     * that satisfies all hard constraints and optimizes soft constraints.
     */
    @PlanningEntityCollectionProperty
    private List<ScheduleSlot> scheduleSlots;

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
     * 
     * OptaPlanner will try to maximize this score.
     */
    @PlanningScore
    private HardSoftScore score;

    // ========================================================================
    // CONSTRUCTORS
    // ========================================================================

    /**
     * Constructor with events
     * Used when events need to be considered in scheduling
     */
    public ScheduleSolution(List<TimeSlot> timeSlots,
                           List<Teacher> teachers,
                           List<Room> rooms,
                           List<Course> courses,
                           List<Event> events,
                           List<ScheduleSlot> scheduleSlots) {
        this.timeSlots = timeSlots != null ? timeSlots : new ArrayList<>();
        this.teachers = teachers != null ? teachers : new ArrayList<>();
        this.rooms = rooms != null ? rooms : new ArrayList<>();
        this.courses = courses != null ? courses : new ArrayList<>();
        this.events = events != null ? events : new ArrayList<>();
        this.scheduleSlots = scheduleSlots != null ? scheduleSlots : new ArrayList<>();
    }

    /**
     * Constructor without events
     * Used when events don't need to be considered
     */
    public ScheduleSolution(List<TimeSlot> timeSlots,
                           List<Teacher> teachers,
                           List<Room> rooms,
                           List<Course> courses,
                           List<ScheduleSlot> scheduleSlots) {
        this(timeSlots, teachers, rooms, courses, new ArrayList<>(), scheduleSlots);
    }

    // ========================================================================
    // ALIAS METHODS (For backward compatibility with different naming styles)
    // ========================================================================

    /**
     * Alias methods for "List" suffix naming convention
     * Some services use timeSlotList, others use timeSlots
     * These methods ensure compatibility with both styles
     */
    
    public List<TimeSlot> getTimeSlotList() {
        return timeSlots;
    }

    public void setTimeSlotList(List<TimeSlot> timeSlotList) {
        this.timeSlots = timeSlotList;
    }

    public List<Teacher> getTeacherList() {
        return teachers;
    }

    public void setTeacherList(List<Teacher> teacherList) {
        this.teachers = teacherList;
    }

    public List<Room> getRoomList() {
        return rooms;
    }

    public void setRoomList(List<Room> roomList) {
        this.rooms = roomList;
    }

    public List<Course> getCourseList() {
        return courses;
    }

    public void setCourseList(List<Course> courseList) {
        this.courses = courseList;
    }

    public List<Event> getEventList() {
        return events;
    }

    public void setEventList(List<Event> eventList) {
        this.events = eventList;
    }

    public List<ScheduleSlot> getScheduleSlotList() {
        return scheduleSlots;
    }

    public void setScheduleSlotList(List<ScheduleSlot> scheduleSlotList) {
        this.scheduleSlots = scheduleSlotList;
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
        sb.append(String.format("  Events:        %d\n", events != null ? events.size() : 0));
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
        if (events == null) events = new ArrayList<>();
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