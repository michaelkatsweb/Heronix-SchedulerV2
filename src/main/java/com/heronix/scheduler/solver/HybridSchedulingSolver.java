package com.heronix.scheduler.solver;

// ============================================================================
// FILE: HybridSchedulingSolver.java - AI-Powered Hybrid Scheduling Solver
// Location: src/main/java/com/eduscheduler/ai/solver/HybridSchedulingSolver.java
// ============================================================================

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.planning.SchedulingSolution;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.data.SISDataService;
import com.heronix.scheduler.solver.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Hybrid AI-Powered Scheduling Solver using OptaPlanner
 * 
 * This class provides AI-powered schedule optimization using OptaPlanner's
 * constraint solver. It combines multiple optimization strategies:
 * 
 * 1. Constraint Satisfaction - Hard constraints (no conflicts)
 * 2. Heuristic Optimization - Soft constraints (balanced workloads)
 * 3. Local Search - Fine-tuning of solutions
 * 4. Simulated Annealing - Escape local optima
 * 
 * Location:
 * src/main/java/com/eduscheduler/ai/solver/HybridSchedulingSolver.java
 * 
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since 2025-10-30
 */
@Slf4j
@Component
public class HybridSchedulingSolver {

    private final SolverFactory<SchedulingSolution> solverFactory;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * Constructor - Initialize OptaPlanner solver factory
     */
    public HybridSchedulingSolver() {
        log.info("Initializing HybridSchedulingSolver");

        // Initialize OptaPlanner solver from XML configuration
        this.solverFactory = SolverFactory.createFromXmlResource("solverConfig.xml");

        log.info("HybridSchedulingSolver initialized successfully");
    }

    /**
     * Solve complete scheduling problem with given constraints
     *
     * @param problem The scheduling problem to solve
     * @return Optimized scheduling solution
     * @throws InvalidScheduleProblemException if input problem is invalid
     * @throws SolverExecutionException if solver execution fails
     */
    public SchedulingSolution solve(SchedulingSolution problem) {
        log.info("Starting hybrid solver for scheduling problem");

        // Validate input
        validateSchedulingProblem(problem);

        log.info("Problem size: {} schedule slots, {} teachers, {} rooms, {} time slots",
                problem.getScheduleSlots().size(),
                problem.getTeachers().size(),
                problem.getRooms().size(),
                problem.getTimeSlots().size());

        try {
            // Build and execute solver
            Solver<SchedulingSolution> solver = solverFactory.buildSolver();

            if (solver == null) {
                throw new SolverExecutionException("Failed to build solver from configuration");
            }

            SchedulingSolution solution = solver.solve(problem);

            if (solution == null) {
                throw new SolverExecutionException("Solver returned null solution");
            }

            log.info("Solver completed. Score: {}, Assigned: {}/{}",
                    solution.getScore(),
                    solution.getAssignedCount(),
                    solution.getTotalSlots());

            return solution;

        } catch (InvalidScheduleProblemException | SolverExecutionException e) {
            // Re-throw custom exceptions
            throw e;
        } catch (Exception e) {
            log.error("Unexpected solver failure", e);
            throw new SolverExecutionException("Failed to solve scheduling problem: " + e.getMessage(), e);
        }
    }

    /**
     * Validate scheduling problem input
     *
     * Ensures that the problem has all required data and no obvious inconsistencies.
     *
     * @param problem The problem to validate
     * @throws InvalidScheduleProblemException if problem is invalid
     */
    private void validateSchedulingProblem(SchedulingSolution problem) {
        if (problem == null) {
            throw new InvalidScheduleProblemException("Scheduling problem cannot be null");
        }

        // Validate schedule slots
        if (problem.getScheduleSlots() == null) {
            throw new InvalidScheduleProblemException("Schedule slots list cannot be null");
        }
        if (problem.getScheduleSlots().isEmpty()) {
            throw new InvalidScheduleProblemException("Schedule slots list cannot be empty - nothing to schedule");
        }

        // Validate teachers
        if (problem.getTeachers() == null) {
            throw new InvalidScheduleProblemException("Teachers list cannot be null");
        }
        if (problem.getTeachers().isEmpty()) {
            throw new InvalidScheduleProblemException("Teachers list cannot be empty - no teachers available");
        }

        // Validate rooms
        if (problem.getRooms() == null) {
            throw new InvalidScheduleProblemException("Rooms list cannot be null");
        }
        if (problem.getRooms().isEmpty()) {
            throw new InvalidScheduleProblemException("Rooms list cannot be empty - no rooms available");
        }

        // Validate time slots
        if (problem.getTimeSlots() == null) {
            throw new InvalidScheduleProblemException("Time slots list cannot be null");
        }
        if (problem.getTimeSlots().isEmpty()) {
            throw new InvalidScheduleProblemException("Time slots list cannot be empty - no time slots available");
        }

        // Validate courses
        if (problem.getCourses() == null) {
            throw new InvalidScheduleProblemException("Courses list cannot be null");
        }
        if (problem.getCourses().isEmpty()) {
            throw new InvalidScheduleProblemException("Courses list cannot be empty - nothing to schedule");
        }

        // Check for impossible constraints
        int totalSlots = problem.getScheduleSlots().size();
        int availableSlots = problem.getTimeSlots().size() * problem.getRooms().size();

        if (totalSlots > availableSlots) {
            log.warn("Schedule slots ({}) exceed available capacity ({})", totalSlots, availableSlots);
            // Don't throw - OptaPlanner might still find a solution with overlaps
        }

        log.debug("Problem validation passed: {} slots, {} teachers, {} rooms, {} time slots, {} courses",
                totalSlots,
                problem.getTeachers().size(),
                problem.getRooms().size(),
                problem.getTimeSlots().size(),
                problem.getCourses().size());
    }

    /**
     * INCREMENTAL SOLVING: Quick partial optimization for real-time edits
     *
     * This method performs incremental solving when only a few slots have changed,
     * providing faster results for interactive editing by pinning unchanged slots.
     *
     * OptaPlanner PlanningPin annotation ensures pinned slots are not modified
     * during optimization, dramatically reducing the search space and solving time.
     *
     * @param current      Current schedule solution
     * @param changedSlots List of slots that have been modified
     * @return Optimized solution with changes applied
     * @throws InvalidScheduleProblemException if input is invalid
     */
    public SchedulingSolution optimizePartial(
            SchedulingSolution current,
            List<ScheduleSlot> changedSlots) {

        // Validate inputs
        if (current == null) {
            throw new InvalidScheduleProblemException("Current solution cannot be null");
        }
        if (changedSlots == null) {
            throw new InvalidScheduleProblemException("Changed slots list cannot be null");
        }
        if (changedSlots.isEmpty()) {
            log.warn("No changed slots provided, returning current solution unchanged");
            return current;
        }

        log.info("Performing incremental optimization for {} changed slots", changedSlots.size());

        try {
            // ✅ INCREMENTAL SOLVING IMPLEMENTATION
            // Step 1: Pin all slots that were NOT changed
            List<ScheduleSlot> allSlots = current.getScheduleSlots();

            if (allSlots == null || allSlots.isEmpty()) {
                throw new InvalidScheduleProblemException("Current solution has no slots");
            }

            Set<Long> changedSlotIds = changedSlots.stream()
                    .map(ScheduleSlot::getId)
                    .filter(id -> id != null)
                    .collect(Collectors.toSet());

            int pinnedCount = 0;
            int unpinnedCount = 0;

            for (ScheduleSlot slot : allSlots) {
                if (slot.getId() != null && !changedSlotIds.contains(slot.getId())) {
                    // Pin this slot to preserve its current assignment
                    slot.setPinned(true);
                    pinnedCount++;
                } else {
                    // Unpin changed slots so solver can optimize them
                    slot.setPinned(false);
                    unpinnedCount++;
                }
            }

            log.info("Solving with {} pinned slots and {} unpinned slots", pinnedCount, unpinnedCount);

            // Step 2: Solve with pinned entities (OptaPlanner will only modify unpinned slots)
            SchedulingSolution solution = solve(current);

            // Step 3: Unpin all slots after solving (reset for future operations)
            for (ScheduleSlot slot : solution.getScheduleSlots()) {
                slot.setPinned(false);
            }

            log.info("Incremental optimization complete. Score: {}", solution.getScore());
            return solution;

        } catch (InvalidScheduleProblemException | SolverExecutionException e) {
            // Unpin all slots on error and re-throw
            log.error("Incremental optimization failed", e);
            if (current.getScheduleSlots() != null) {
                for (ScheduleSlot slot : current.getScheduleSlots()) {
                    slot.setPinned(false);
                }
            }
            throw e;
        } catch (Exception e) {
            // Unpin all slots on unexpected error
            log.error("Unexpected error during incremental optimization", e);
            if (current.getScheduleSlots() != null) {
                for (ScheduleSlot slot : current.getScheduleSlots()) {
                    slot.setPinned(false);
                }
            }
            throw new SolverExecutionException("Incremental optimization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Resolve specific conflict by rearranging schedule
     *
     * @param schedule       The current schedule
     * @param conflictedSlot The slot causing the conflict
     * @param targetTime     The desired time for the slot
     * @return Updated schedule with conflict resolved
     * @throws InvalidScheduleProblemException if input is invalid
     * @throws SolverExecutionException if conflict resolution fails
     */
    public Schedule resolveConflict(
            Schedule schedule,
            ScheduleSlot conflictedSlot,
            TimeSlot targetTime) {

        log.info("Resolving conflict for slot {} to time {}",
                conflictedSlot != null ? conflictedSlot.getId() : "null", targetTime);

        // Validate inputs
        if (schedule == null) {
            throw new InvalidScheduleProblemException("Schedule cannot be null");
        }
        if (schedule.getId() == null) {
            throw new InvalidScheduleProblemException("Schedule ID cannot be null");
        }
        if (conflictedSlot == null) {
            throw new InvalidScheduleProblemException("Conflicted slot cannot be null");
        }
        if (targetTime == null) {
            throw new InvalidScheduleProblemException("Target time cannot be null");
        }

        try {
            // Load all slots for this schedule
            List<ScheduleSlot> allSlots = scheduleSlotRepository.findByScheduleId(schedule.getId());

            if (allSlots == null || allSlots.isEmpty()) {
                throw new InvalidScheduleProblemException(
                        "Schedule " + schedule.getId() + " has no slots to rearrange");
            }

            log.info("Loaded {} slots for conflict resolution", allSlots.size());

            // Create problem with conflict resolution constraints
            SchedulingSolution problem = buildConflictResolutionProblem(
                    allSlots,
                    conflictedSlot,
                    targetTime);

            // Solve
            SchedulingSolution solution = solve(problem);

            // Update schedule with solution
            updateScheduleFromSolution(schedule, solution);

            // Save changes
            Schedule savedSchedule = scheduleRepository.save(schedule);
            log.info("Conflict resolved successfully. Score: {}", solution.getScore());

            return savedSchedule;

        } catch (InvalidScheduleProblemException | SolverExecutionException e) {
            // Re-throw custom exceptions
            throw e;
        } catch (Exception e) {
            log.error("Failed to resolve conflict for slot {}", conflictedSlot.getId(), e);
            throw new SolverExecutionException(
                    "Conflict resolution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Optimize existing schedule for better resource utilization
     *
     * @param scheduleId ID of schedule to optimize
     * @return Optimized schedule
     * @throws ScheduleNotFoundException if schedule does not exist
     * @throws InvalidScheduleProblemException if schedule data is invalid
     * @throws SolverExecutionException if optimization fails
     */
    public Schedule optimizeSchedule(Long scheduleId) {
        log.info("Optimizing schedule {}", scheduleId);

        // Validate input
        if (scheduleId == null) {
            throw new InvalidScheduleProblemException("Schedule ID cannot be null");
        }

        // Load schedule with error handling
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ScheduleNotFoundException(scheduleId));

        try {
            // Load schedule slots
            List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(scheduleId);

            if (slots == null || slots.isEmpty()) {
                throw new InvalidScheduleProblemException(
                        "Schedule " + scheduleId + " has no slots to optimize");
            }

            log.info("Loaded {} slots for optimization", slots.size());

            // Build optimization problem
            SchedulingSolution problem = buildOptimizationProblem(slots);

            // Solve
            SchedulingSolution solution = solve(problem);

            // Update schedule
            updateScheduleFromSolution(schedule, solution);

            // Save with error handling
            Schedule savedSchedule = scheduleRepository.save(schedule);
            log.info("Schedule {} optimized successfully. New score: {}",
                    scheduleId, solution.getScore());

            return savedSchedule;

        } catch (InvalidScheduleProblemException | SolverExecutionException e) {
            // Re-throw custom exceptions
            throw e;
        } catch (Exception e) {
            log.error("Failed to optimize schedule {}", scheduleId, e);
            throw new SolverExecutionException(
                    "Failed to optimize schedule " + scheduleId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Solve scheduling conflicts between multiple entities
     *
     * @param schedule The schedule to resolve
     * @return Schedule with conflicts resolved
     * @throws InvalidScheduleProblemException if schedule is invalid
     * @throws SolverExecutionException if conflict resolution fails
     */
    public Schedule solveSchedulingConflicts(Schedule schedule) {
        log.info("Solving scheduling conflicts for schedule {}", schedule.getId());

        // Validate input
        if (schedule == null) {
            throw new InvalidScheduleProblemException("Schedule cannot be null");
        }
        if (schedule.getId() == null) {
            throw new InvalidScheduleProblemException("Schedule ID cannot be null");
        }

        try {
            // Load schedule slots
            List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());

            if (slots == null || slots.isEmpty()) {
                throw new InvalidScheduleProblemException(
                        "Schedule " + schedule.getId() + " has no slots to resolve");
            }

            log.info("Resolving conflicts in {} slots", slots.size());

            // Build problem
            SchedulingSolution problem = buildOptimizationProblem(slots);

            // Solve with focus on conflict resolution
            SchedulingSolution solution = solve(problem);

            // Update schedule
            updateScheduleFromSolution(schedule, solution);

            // Save changes
            Schedule savedSchedule = scheduleRepository.save(schedule);
            log.info("Conflicts resolved for schedule {}. Score: {}",
                    schedule.getId(), solution.getScore());

            return savedSchedule;

        } catch (InvalidScheduleProblemException | SolverExecutionException e) {
            // Re-throw custom exceptions
            throw e;
        } catch (Exception e) {
            log.error("Failed to solve scheduling conflicts for schedule {}", schedule.getId(), e);
            throw new SolverExecutionException(
                    "Conflict resolution failed for schedule " + schedule.getId() + ": " + e.getMessage(), e);
        }
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    /**
     * Build conflict resolution problem
     */
    private SchedulingSolution buildConflictResolutionProblem(
            List<ScheduleSlot> allSlots,
            ScheduleSlot conflictedSlot,
            TimeSlot targetTime) {

        // Load all necessary entities - FIXED: Use only active teachers
        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());  // Only active teachers
        List<Room> rooms = roomRepository.findAll();
        List<Course> courses = sisDataService.getAllCourses();
        List<TimeSlot> timeSlots = loadAvailableTimeSlots();

        // Set the target time for conflicted slot
        conflictedSlot.setTimeSlot(targetTime);

        // Build and return problem
        return new SchedulingSolution(
                allSlots,
                teachers,
                rooms,
                timeSlots,
                courses,
                null // Students not needed for this operation
        );
    }

    /**
     * Build general optimization problem
     *
     * @param slots The schedule slots to optimize
     * @return A complete SchedulingSolution ready for solving
     * @throws InvalidScheduleProblemException if required data cannot be loaded
     */
    private SchedulingSolution buildOptimizationProblem(List<ScheduleSlot> slots) {
        try {
            // Load all required entities from database - FIXED: Use only active teachers
            List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());  // Only active teachers
            List<Room> rooms = roomRepository.findAll();
            List<Course> courses = sisDataService.getAllCourses();
            List<TimeSlot> timeSlots = loadAvailableTimeSlots();

            // Validate we have required data
            if (teachers == null || teachers.isEmpty()) {
                throw new InvalidScheduleProblemException("No teachers available in database");
            }
            if (rooms == null || rooms.isEmpty()) {
                throw new InvalidScheduleProblemException("No rooms available in database");
            }
            if (courses == null || courses.isEmpty()) {
                throw new InvalidScheduleProblemException("No courses available in database");
            }
            if (timeSlots == null || timeSlots.isEmpty()) {
                throw new InvalidScheduleProblemException("No time slots available");
            }

            log.debug("Building optimization problem: {} slots, {} teachers, {} rooms, {} time slots, {} courses",
                    slots.size(), teachers.size(), rooms.size(), timeSlots.size(), courses.size());

            return new SchedulingSolution(
                    slots,
                    teachers,
                    rooms,
                    timeSlots,
                    courses,
                    null);

        } catch (InvalidScheduleProblemException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to build optimization problem", e);
            throw new InvalidScheduleProblemException("Failed to load required data: " + e.getMessage(), e);
        }
    }

    /**
     * Update schedule from solved solution
     *
     * Takes the solved solution and persists the assignments back to the database.
     *
     * @param schedule The schedule to update
     * @param solution The solved solution with assignments
     * @throws SolverExecutionException if database update fails
     */
    private void updateScheduleFromSolution(Schedule schedule, SchedulingSolution solution) {
        if (solution == null || solution.getScheduleSlots() == null) {
            log.warn("Cannot update schedule - solution or slots are null");
            return;
        }

        int updatedCount = 0;
        int skippedCount = 0;

        try {
            for (ScheduleSlot solvedSlot : solution.getScheduleSlots()) {
                if (solvedSlot.getTimeSlot() != null && solvedSlot.getId() != null) {
                    // Update slot with solved values
                    ScheduleSlot dbSlot = scheduleSlotRepository.findById(solvedSlot.getId())
                            .orElse(null);

                    if (dbSlot != null) {
                        dbSlot.setTimeSlot(solvedSlot.getTimeSlot());
                        dbSlot.setTeacher(solvedSlot.getTeacher());
                        dbSlot.setRoom(solvedSlot.getRoom());
                        dbSlot.setDayOfWeek(solvedSlot.getTimeSlot().getDayOfWeek());
                        dbSlot.setStartTime(solvedSlot.getTimeSlot().getStartTime());
                        dbSlot.setEndTime(solvedSlot.getTimeSlot().getEndTime());

                        scheduleSlotRepository.save(dbSlot);
                        updatedCount++;
                    } else {
                        log.warn("Could not find slot {} in database", solvedSlot.getId());
                        skippedCount++;
                    }
                } else {
                    skippedCount++;
                }
            }

            log.info("Updated {} slots, skipped {} slots", updatedCount, skippedCount);

        } catch (Exception e) {
            log.error("Failed to update schedule from solution", e);
            throw new SolverExecutionException("Failed to save solution to database: " + e.getMessage(), e);
        }
    }

    /**
     * Load available time slots from existing schedule slots in database
     *
     * This method extracts unique time slots from all schedule slots,
     * or generates standard school time slots if none exist.
     */
    private List<TimeSlot> loadAvailableTimeSlots() {
        log.debug("Loading available time slots");

        try {
            // Try to load unique time slots from existing schedule slots
            List<ScheduleSlot> existingSlots = scheduleSlotRepository.findAll();

            if (!existingSlots.isEmpty()) {
                // Extract unique time slots from existing schedule slots
                Set<TimeSlot> uniqueTimeSlots = existingSlots.stream()
                    .filter(slot -> slot.getDayOfWeek() != null &&
                                   slot.getStartTime() != null &&
                                   slot.getEndTime() != null)
                    .map(TimeSlot::fromScheduleSlot)
                    .collect(Collectors.toSet());

                if (!uniqueTimeSlots.isEmpty()) {
                    log.info("Loaded {} unique time slots from database", uniqueTimeSlots.size());
                    return List.copyOf(uniqueTimeSlots);
                }
            }

            // Fallback: Generate standard school time slots
            log.info("No time slots in database, generating standard time slots");
            return generateStandardTimeSlots();

        } catch (Exception e) {
            log.error("Error loading time slots from database, using standard slots", e);
            return generateStandardTimeSlots();
        }
    }

    /**
     * Generate standard school time slots
     *
     * Creates a standard 8-period day schedule:
     * - Monday through Friday
     * - 8:00 AM to 3:30 PM
     * - 8 periods of 45 minutes each
     * - 5 minute passing time between periods
     * - 30 minute lunch period (11:30 AM - 12:00 PM)
     */
    private List<TimeSlot> generateStandardTimeSlots() {
        List<TimeSlot> timeSlots = new ArrayList<>();

        // School days
        List<java.time.DayOfWeek> schoolDays = List.of(
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        );

        // Period schedule with times
        int[][] periodTimes = {
            {8, 0, 8, 45},      // Period 1: 8:00 - 8:45
            {8, 50, 9, 35},     // Period 2: 8:50 - 9:35
            {9, 40, 10, 25},    // Period 3: 9:40 - 10:25
            {10, 30, 11, 15},   // Period 4: 10:30 - 11:15
            {11, 20, 12, 5},    // Period 5: 11:20 - 12:05
            {12, 35, 13, 20},   // Period 6: 12:35 - 1:20 (after lunch)
            {13, 25, 14, 10},   // Period 7: 1:25 - 2:10
            {14, 15, 15, 0}     // Period 8: 2:15 - 3:00
        };

        // Generate time slots for each day and period
        for (java.time.DayOfWeek day : schoolDays) {
            for (int periodNum = 0; periodNum < periodTimes.length; periodNum++) {
                int[] times = periodTimes[periodNum];
                java.time.LocalTime startTime = java.time.LocalTime.of(times[0], times[1]);
                java.time.LocalTime endTime = java.time.LocalTime.of(times[2], times[3]);

                timeSlots.add(new TimeSlot(day, startTime, endTime, periodNum + 1));
            }
        }

        log.info("Generated {} standard time slots ({} days × {} periods)",
                 timeSlots.size(), schoolDays.size(), periodTimes.length);

        return timeSlots;
    }

    /**
     * Calculate quality score for a solution
     *
     * Quality is calculated based on:
     * - Hard constraints: Must be 0 (100% satisfaction required)
     * - Soft constraints: Higher is better, normalized to 0-100 scale
     *
     * Soft score interpretation:
     * - 0 soft = 100% quality (perfect solution)
     * - Negative soft scores reduce quality proportionally
     * - Quality = max(0, 100 - abs(softScore) / expectedMaxViolations * 100)
     *
     * @param solution The solution to evaluate
     * @return Quality score (0-100)
     */
    public double calculateQualityScore(SchedulingSolution solution) {
        if (solution == null || solution.getScore() == null) {
            return 0.0;
        }

        // Extract hard and soft scores from format: "0hard/-123soft"
        String scoreStr = solution.getScore().toString();

        try {
            // Parse hard score
            int hardScore = 0;
            int softScore = 0;

            if (scoreStr.contains("hard")) {
                String[] parts = scoreStr.split("hard");
                hardScore = Integer.parseInt(parts[0].trim());

                // Parse soft score if present
                if (parts.length > 1 && parts[1].contains("soft")) {
                    String softPart = parts[1].replace("soft", "").replace("/", "").trim();
                    softScore = Integer.parseInt(softPart);
                }
            } else if (scoreStr.contains("soft")) {
                // Only soft score present
                softScore = Integer.parseInt(scoreStr.replace("soft", "").trim());
            }

            // If there are hard constraint violations, quality is 0
            if (hardScore < 0) {
                log.debug("Hard constraint violations detected ({}), quality = 0", hardScore);
                return 0.0;
            }

            // Calculate quality based on soft score
            // Assume worst case scenario: 1000 possible soft violations
            // (average school: 100 slots, 10 constraints each)
            final int EXPECTED_MAX_VIOLATIONS = 1000;

            if (softScore >= 0) {
                // Perfect or positive score = 100% quality
                return 100.0;
            } else {
                // Negative soft score: reduce quality proportionally
                double violationRatio = Math.abs(softScore) / (double) EXPECTED_MAX_VIOLATIONS;
                double quality = Math.max(0, 100.0 * (1.0 - violationRatio));

                // Apply non-linear scaling for better user perception
                // Small violations shouldn't drastically reduce quality
                quality = 100.0 * Math.pow(quality / 100.0, 0.7);

                log.debug("Soft score: {}, violation ratio: {:.2f}, quality: {:.1f}%",
                        softScore, violationRatio, quality);

                return quality;
            }

        } catch (NumberFormatException e) {
            log.warn("Failed to parse score string: {}", scoreStr, e);
            // Return default quality if parsing fails
            return 50.0;
        }
    }

    /**
     * Get solver statistics
     */
    public SolverStats getStatistics() {
        return new SolverStats();
    }

    /**
     * Statistics holder class
     */
    public static class SolverStats {
        private int problemsolved = 0;
        private long averageSolveTime = 0;
        private double averageQualityScore = 0.0;

        // Getters and setters
        public int getProblemsSolved() {
            return problemsolved;
        }

        public void setProblemsSolved(int count) {
            this.problemsolved = count;
        }

        public long getAverageSolveTime() {
            return averageSolveTime;
        }

        public void setAverageSolveTime(long time) {
            this.averageSolveTime = time;
        }

        public double getAverageQualityScore() {
            return averageQualityScore;
        }

        public void setAverageQualityScore(double score) {
            this.averageQualityScore = score;
        }
    }
}