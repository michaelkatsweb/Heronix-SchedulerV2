package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.dto.ConflictDetail;
import com.heronix.scheduler.model.dto.ScheduleGenerationRequest;
import com.heronix.scheduler.model.dto.ScheduleGenerationResult;
import com.heronix.scheduler.model.enums.*;
import com.heronix.scheduler.model.planning.SchedulingSolution;
import com.heronix.scheduler.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Enhanced Schedule Generation Service
 *
 * Wraps the existing ScheduleGenerationService to add support for:
 * - Partial schedule generation when hard constraints can't be satisfied
 * - Detailed conflict analysis and reporting
 * - Graceful degradation instead of complete failure
 *
 * This service enables the system to ALWAYS provide a useful result,
 * even when an optimal solution cannot be found.
 *
 * Location: src/main/java/com/eduscheduler/service/EnhancedScheduleGenerationService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-18
 */
@Slf4j
@Service
public class EnhancedScheduleGenerationService {

    @Autowired
    private ScheduleGenerationService scheduleGenerationService;

    @Autowired
    private ConflictAnalysisService conflictAnalysisService;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private com.heronix.scheduler.client.SISApiClient sisApiClient;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private SolverManager<SchedulingSolution, UUID> solverManager;

    /**
     * Generate schedule with fallback to partial scheduling
     *
     * This method attempts full schedule generation, and if hard constraints
     * cannot be satisfied, it gracefully falls back to partial scheduling
     * with detailed conflict reporting.
     *
     * @param request          Schedule generation parameters
     * @param progressCallback Progress update callback (optional)
     * @return Complete result with schedule and conflict information
     */
    public ScheduleGenerationResult generateWithFallback(
            ScheduleGenerationRequest request,
            BiConsumer<Integer, String> progressCallback) {

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("ENHANCED SCHEDULE GENERATION STARTING");
        log.info("═══════════════════════════════════════════════════════════════");

        long startTime = System.currentTimeMillis();
        Schedule schedule = null;
        ScheduleStatus status = ScheduleStatus.DRAFT;
        List<ConflictDetail> conflicts = Collections.emptyList();

        try {
            // Attempt full schedule generation using existing service
            log.info("Attempting full schedule generation...");
            schedule = scheduleGenerationService.generateSchedule(request, progressCallback);

            // If we got here, generation was successful
            status = ScheduleStatus.PUBLISHED;
            conflicts = Collections.emptyList();

            log.info("✅ Full schedule generation successful!");

        } catch (RuntimeException e) {
            // Check if this is a hard constraint violation (could be wrapped)
            Throwable cause = e.getCause();
            boolean isConstraintViolation = e.getMessage().contains("hard constraint violations") ||
                                           (cause != null && cause.getMessage() != null &&
                                            cause.getMessage().contains("hard constraint violations"));

            if (isConstraintViolation) {
                // Hard constraint violations detected
                log.warn("Hard constraint violations detected, attempting partial schedule generation");

                try {
                    // Re-run generation with allowPartial=true
                    log.info("Re-running schedule generation with partial scheduling enabled...");
                    schedule = scheduleGenerationService.generateSchedule(request, progressCallback, true);

                // Schedule was saved with partial results
                status = ScheduleStatus.DRAFT;

                // Analyze conflicts
                log.info("Analyzing constraint violations...");
                conflicts = analyzeScheduleConflicts(schedule);

                log.warn("⚠️  Partial schedule generated with {} conflicts", conflicts.size());

                } catch (Exception partialEx) {
                    log.error("Failed to generate even partial schedule", partialEx);
                    throw new RuntimeException(
                        "Schedule generation completely failed. Original error: " +
                        e.getMessage() + ". Partial generation error: " + partialEx.getMessage(),
                        partialEx
                    );
                }
            } else {
                // Not a constraint violation - re-throw
                log.error("Unexpected error during schedule generation", e);
                throw e;
            }

        } catch (Exception e) {
            // Other errors (database, network, etc.)
            log.error("Unexpected error during schedule generation", e);
            throw new RuntimeException("Schedule generation failed: " + e.getMessage(), e);
        }

        // Calculate statistics
        long generationTime = (System.currentTimeMillis() - startTime) / 1000;

        // Build comprehensive result
        ScheduleGenerationResult result = buildGenerationResult(
            schedule, status, conflicts, generationTime, request);

        // Log summary
        logGenerationSummary(result);

        return result;
    }

    /**
     * Analyze conflicts in a partially generated schedule
     */
    private List<ConflictDetail> analyzeScheduleConflicts(Schedule schedule) {
        log.info("Analyzing conflicts in schedule: {}", schedule.getName());

        // Load schedule slots
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());

        if (slots.isEmpty()) {
            log.warn("No schedule slots found for analysis");
            return Collections.emptyList();
        }

        // Build a SchedulingSolution for analysis
        // ✅ FIX: Load teachers from SIS API
        SchedulingSolution solution = new SchedulingSolution();
        solution.setScheduleSlots(slots);
        List<com.heronix.scheduler.model.dto.TeacherDTO> teacherDTOs = sisApiClient.getAllTeachers();
        solution.setTeachers(com.heronix.scheduler.util.DTOConverter.toTeachers(teacherDTOs));
        solution.setRooms(roomRepository.findAll());
        solution.setTimeSlots(timeSlotService.getAllTimeSlots());

        // Use ConflictAnalysisService to analyze
        List<ConflictDetail> conflicts = conflictAnalysisService.analyzeConstraintViolations(solution);

        log.info("Identified {} conflicts", conflicts.size());
        return conflicts;
    }

    /**
     * Generate partial schedule when full generation fails
     *
     * @deprecated This method is no longer used. We now call generateSchedule(request, progressCallback, true)
     *             directly to enable partial scheduling in a single pass.
     *
     * This method attempts to extract whatever schedule was generated before
     * the hard constraint failure, and provides detailed conflict analysis.
     */
    @Deprecated
    private ScheduleGenerationResult generatePartialSchedule(
            ScheduleGenerationRequest request,
            BiConsumer<Integer, String> progressCallback,
            IllegalStateException originalException) throws Exception {

        log.info("Starting partial schedule generation...");

        // Parse the original exception to understand constraint violations
        String errorMessage = originalException.getMessage();
        int violationCount = parseViolationCount(errorMessage);

        log.info("Detected {} hard constraint violations", violationCount);

        // STEP 1: Re-run solver to get partial solution
        updateProgress(progressCallback, 50, "Attempting partial schedule generation...");
        SchedulingSolution partialSolution = runSolverForPartialSchedule(request, progressCallback);

        log.info("Partial solution obtained with score: {}", partialSolution.getScore());

        // STEP 2: Analyze conflicts
        updateProgress(progressCallback, 92, "Analyzing conflicts...");
        List<ConflictDetail> conflicts = conflictAnalysisService.analyzeConstraintViolations(partialSolution);

        log.info("Identified {} conflicts", conflicts.size());

        // STEP 3: Save partial schedule
        updateProgress(progressCallback, 95, "Saving partial schedule...");
        Schedule partialSchedule = savePartialScheduleFromSolution(partialSolution, request);

        // STEP 4: Build and return result
        updateProgress(progressCallback, 98, "Building result...");
        return buildPartialResult(partialSchedule, conflicts, partialSolution, request);
    }

    /**
     * Parse violation count from exception message
     */
    private int parseViolationCount(String errorMessage) {
        try {
            // Extract number from message like "12 hard constraint violations"
            String[] parts = errorMessage.split("\\s+");
            for (int i = 0; i < parts.length - 2; i++) {
                if (parts[i + 1].equals("hard") && parts[i + 2].equals("constraint")) {
                    return Integer.parseInt(parts[i]);
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse violation count from error message", e);
        }
        return 0;
    }

    /**
     * Build comprehensive generation result
     */
    private ScheduleGenerationResult buildGenerationResult(
            Schedule schedule,
            ScheduleStatus status,
            List<ConflictDetail> conflicts,
            long generationTime,
            ScheduleGenerationRequest request) {

        // Count courses
        int totalCourses = schedule.getSlots() != null ? schedule.getSlots().size() : 0;
        int unscheduledCourses = conflicts.size();
        int scheduledCourses = totalCourses - unscheduledCourses;

        // Calculate completion percentage
        double completionPct = totalCourses > 0 ?
            (scheduledCourses * 100.0) / totalCourses : 0.0;

        // Build result
        ScheduleGenerationResult result = ScheduleGenerationResult.builder()
            .schedule(schedule)
            .status(status)
            .conflicts(conflicts)
            .completionPercentage(completionPct)
            .totalCourses(totalCourses)
            .scheduledCourses(scheduledCourses)
            .unscheduledCourses(unscheduledCourses)
            .generationTimeSeconds(generationTime)
            .optimizationScore(schedule.getOptimizationScore() != null ?
                schedule.getOptimizationScore().toString() : "N/A")
            .build();

        // Add recommendations
        result.addRecommendation(generateRecommendations(result));

        // Calculate statistics
        result.calculateStatistics();

        return result;
    }

    /**
     * Generate recommendations based on result
     */
    private String generateRecommendations(ScheduleGenerationResult result) {
        StringBuilder recommendations = new StringBuilder();

        if (result.getCompletionPercentage() >= 100.0) {
            recommendations.append("✅ Perfect! Schedule is complete and ready to publish.");
        } else if (result.getCompletionPercentage() >= 90.0) {
            recommendations.append("⚠️  Schedule is mostly complete (")
                          .append(String.format("%.1f", result.getCompletionPercentage()))
                          .append("%). Review conflicts and consider accepting as-is or fixing critical issues.");
        } else if (result.getCompletionPercentage() >= 70.0) {
            recommendations.append("⚠️  Partial schedule generated (")
                          .append(String.format("%.1f", result.getCompletionPercentage()))
                          .append("%). Fix high-priority conflicts and regenerate for better results.");
        } else {
            recommendations.append("❌ Schedule generation needs attention. Only ")
                          .append(String.format("%.1f", result.getCompletionPercentage()))
                          .append("% complete. Address critical data issues before regenerating.");
        }

        return recommendations.toString();
    }

    /**
     * Log generation summary
     */
    private void logGenerationSummary(ScheduleGenerationResult result) {
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("SCHEDULE GENERATION RESULT SUMMARY");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("Status: {}", result.getStatus());
        log.info("Completion: {}/{} courses ({}%)",
            result.getScheduledCourses(),
            result.getTotalCourses(),
            String.format("%.1f", result.getCompletionPercentage()));
        log.info("Conflicts: {}", result.getConflicts().size());
        log.info("  - Blocking: {}", result.getBlockingConflictCount());
        log.info("  - Major: {}", result.getMajorConflictCount());
        log.info("Generation Time: {} seconds", result.getGenerationTimeSeconds());
        log.info("Acceptable: {}", result.isSuccess() ? "Yes" : "No");
        log.info("═══════════════════════════════════════════════════════════════");
    }

    /**
     * Analyze existing schedule for conflicts
     *
     * This method can be called on already-generated schedules to
     * produce conflict reports without regenerating.
     *
     * @param scheduleId ID of schedule to analyze
     * @return Conflict analysis result
     */
    public ScheduleGenerationResult analyzeExistingSchedule(Long scheduleId) {
        log.info("Analyzing existing schedule {}", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Schedule not found: " + scheduleId));

        // Get all schedule slots for this schedule
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());

        // Create a SchedulingSolution from existing schedule for analysis
        SchedulingSolution solution = new SchedulingSolution();
        solution.setScheduleSlots(slots);

        // Analyze conflicts using the existing service
        List<ConflictDetail> conflicts = conflictAnalysisService.analyzeConstraintViolations(solution);

        // Calculate completion percentage based on assigned slots
        int totalSlots = slots.size();
        int assignedSlots = (int) slots.stream()
            .filter(s -> s.getTeacher() != null && s.getRoom() != null)
            .count();
        double completionPercentage = totalSlots > 0 ? (assignedSlots * 100.0 / totalSlots) : 100.0;

        // Separate hard vs soft conflicts
        List<ConflictDetail> hardConflicts = conflicts.stream()
            .filter(c -> c.getSeverity() != null && c.getSeverity() == ConflictSeverity.CRITICAL)
            .collect(Collectors.toList());

        log.info("Analysis complete for schedule {}: {} total conflicts ({} critical), {}% assigned",
            scheduleId, conflicts.size(), hardConflicts.size(), String.format("%.1f", completionPercentage));

        return ScheduleGenerationResult.builder()
            .schedule(schedule)
            .status(schedule.getStatus())
            .conflicts(conflicts)
            .completionPercentage(completionPercentage)
            .totalCourses(totalSlots)
            .scheduledCourses(assignedSlots)
            .unscheduledCourses(totalSlots - assignedSlots)
            .build();
    }

    // ========================================================================
    // PARTIAL SCHEDULE GENERATION - NEW METHODS
    // ========================================================================

    /**
     * Re-run solver to capture partial solution without validation
     */
    private SchedulingSolution runSolverForPartialSchedule(
            ScheduleGenerationRequest request,
            BiConsumer<Integer, String> progressCallback) throws Exception {

        log.info("Attempting to retrieve partial solution from failed generation...");

        // Strategy: The first generation attempt already ran the solver and created
        // a schedule entity and slots in the database. The solver produced a solution
        // but validation failed. We need to re-run the solver to get that solution again
        // WITHOUT creating duplicate database entries.

        // Find the most recently created schedule (from the failed attempt)
        List<Schedule> recentSchedules = scheduleRepository.findAll();
        Schedule existingSchedule = recentSchedules.stream()
            .filter(s -> s.getName().equals(request.getScheduleName()))
            .max((a, b) -> a.getId().compareTo(b.getId()))
            .orElse(null);

        Schedule schedule;
        if (existingSchedule != null) {
            log.info("Found existing schedule from failed attempt: {} (ID: {})",
                existingSchedule.getName(), existingSchedule.getId());
            schedule = existingSchedule;
        } else {
            // Fallback: Create new schedule if none found
            log.warn("No existing schedule found, creating new one...");
            schedule = createScheduleEntity(request);
            schedule = scheduleRepository.save(schedule);
        }

        // PHASE 1: Load Resources
        // ✅ CRITICAL FIX: Load resources from SIS API
        updateProgress(progressCallback, 52, "Loading resources from SIS...");
        List<com.heronix.scheduler.model.dto.TeacherDTO> teacherDTOs = sisApiClient.getAllTeachers();
        List<Teacher> teachers = com.heronix.scheduler.util.DTOConverter.toTeachers(teacherDTOs);

        List<com.heronix.scheduler.model.dto.CourseDTO> courseDTOs = sisApiClient.getAllCourses();
        List<Course> courses = com.heronix.scheduler.util.DTOConverter.toCourses(courseDTOs);

        List<com.heronix.scheduler.model.dto.StudentDTO> studentDTOs = sisApiClient.getAllStudents();
        List<Student> students = com.heronix.scheduler.util.DTOConverter.toStudents(studentDTOs);

        List<Room> rooms = roomRepository.findAll();

        log.info("Loaded {} teachers, {} courses, {} students from SIS", teachers.size(), courses.size(), students.size());

        // PHASE 2: Generate Time Slots
        updateProgress(progressCallback, 60, "Generating time slots...");
        List<TimeSlot> timeSlots = generateOrReuseTimeSlots(request);

        // PHASE 3: Create Schedule Slots (reuse or create)
        updateProgress(progressCallback, 65, "Preparing schedule slots...");
        List<ScheduleSlot> existingSlots = scheduleSlotRepository.findByScheduleId(schedule.getId());

        List<ScheduleSlot> slots;
        if (!existingSlots.isEmpty()) {
            log.info("Reusing {} existing schedule slots", existingSlots.size());
            slots = existingSlots;
        } else {
            log.info("Creating new schedule slots...");
            slots = createScheduleSlots(schedule, courses, students);
            // Don't save yet - solver will work with transient objects
        }

        // PHASE 4: Build Optimization Problem
        updateProgress(progressCallback, 70, "Building optimization problem...");
        SchedulingSolution problem = new SchedulingSolution();
        problem.setScheduleSlots(slots);
        problem.setTeachers(teachers);
        problem.setRooms(rooms);
        problem.setTimeSlots(timeSlots);

        // PHASE 5: Run Solver WITHOUT validation
        updateProgress(progressCallback, 75, "Running AI optimization...");
        UUID problemId = UUID.randomUUID();
        SolverJob<SchedulingSolution, UUID> solverJob = solverManager.solve(problemId, problem);

        // Wait for solution
        SchedulingSolution solution = solverJob.getFinalBestSolution();

        log.info("Solver completed with score: {}", solution.getScore());

        // IMPORTANT: Return solution WITHOUT validation
        return solution;
    }

    /**
     * Generate or reuse existing time slots
     */
    private List<TimeSlot> generateOrReuseTimeSlots(ScheduleGenerationRequest request) {
        // Check if time slots already exist
        List<TimeSlot> existing = timeSlotService.getAllTimeSlots();
        if (!existing.isEmpty()) {
            log.info("Reusing {} existing time slots", existing.size());
            return existing;
        }

        // Generate new time slots
        log.info("Generating new time slots...");
        List<TimeSlot> timeSlots = new ArrayList<>();

        int startHour = request.getStartHour() != null ? request.getStartHour() : 8;
        int endHour = request.getEndHour() != null ? request.getEndHour() : 15;
        int duration = request.getPeriodDuration() != null ? request.getPeriodDuration() : 50;
        boolean enableLunch = request.getEnableLunch() != null ? request.getEnableLunch() : true;
        int lunchStartHour = request.getLunchStartHour() != null ? request.getLunchStartHour() : 12;
        int lunchDuration = request.getLunchDuration() != null ? request.getLunchDuration() : 30;

        // Generate for each day of week
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};

        for (DayOfWeek day : days) {
            LocalTime currentTime = LocalTime.of(startHour, 0);
            LocalTime endTime = LocalTime.of(endHour, 0);
            int slotNumber = 1;

            while (currentTime.isBefore(endTime)) {
                // Skip lunch period
                if (enableLunch && isLunchTime(currentTime, lunchStartHour, lunchDuration)) {
                    currentTime = currentTime.plusMinutes(lunchDuration);
                    continue;
                }

                TimeSlot slot = new TimeSlot();
                slot.setDayOfWeek(day);
                slot.setStartTime(currentTime);
                slot.setEndTime(currentTime.plusMinutes(duration));
                // slotNumber++ - TimeSlot doesn't have setSlotNumber method

                timeSlots.add(slot);

                // Move to next slot (add break time)
                currentTime = currentTime.plusMinutes(duration + 10);
            }
        }

        // TimeSlots will be passed to OptaPlanner directly, no need to persist
        // They're transient objects used only for schedule generation

        log.info("Generated {} time slots for planning", timeSlots.size());
        return timeSlots;
    }

    /**
     * Check if time falls within lunch period
     */
    private boolean isLunchTime(LocalTime time, int lunchStartHour, int lunchDuration) {
        LocalTime lunchStart = LocalTime.of(lunchStartHour, 0);
        LocalTime lunchEnd = lunchStart.plusMinutes(lunchDuration);
        return !time.isBefore(lunchStart) && time.isBefore(lunchEnd);
    }

    /**
     * Create Schedule entity from request
     */
    private Schedule createScheduleEntity(ScheduleGenerationRequest request) {
        Schedule schedule = new Schedule();

        schedule.setName(request.getScheduleName());
        schedule.setScheduleType(request.getScheduleType());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setStatus(ScheduleStatus.DRAFT);
        schedule.setOptimizationScore(0.0);
        schedule.setTotalConflicts(0);
        schedule.setPeriod(SchedulePeriod.MASTER);  // Generated schedules are always MASTER

        return schedule;
    }

    /**
     * Create schedule slots for each course
     */
    private List<ScheduleSlot> createScheduleSlots(
            Schedule schedule,
            List<Course> courses,
            List<Student> students) {

        List<ScheduleSlot> slots = new ArrayList<>();

        for (Course course : courses) {
            // Determine sessions per week for this course
            int sessionsPerWeek = course.getSessionsPerWeek() != null ?
                course.getSessionsPerWeek() : 5;

            // Create slots for each session
            for (int sessionNum = 1; sessionNum <= sessionsPerWeek; sessionNum++) {
                ScheduleSlot slot = new ScheduleSlot();

                // Link to schedule and course
                slot.setSchedule(schedule);
                slot.setCourse(course);

                // Initial status (OptaPlanner will assign teacher/room/time)
                slot.setStatus(SlotStatus.DRAFT);
                slot.setHasConflict(false);

                // Assign enrolled students
                // Note: Student enrollment linking happens through course relationships
                // For now, leave students empty - OptaPlanner will handle assignments
                slot.setStudents(new ArrayList<>());

                slots.add(slot);
            }
        }

        log.info("Created {} slots for {} courses", slots.size(), courses.size());
        return slots;
    }

    /**
     * Save partial schedule from solution
     */
    private Schedule savePartialScheduleFromSolution(
            SchedulingSolution solution,
            ScheduleGenerationRequest request) {

        log.info("Saving partial schedule to database...");

        // Get the schedule entity (already created during solver run)
        List<ScheduleSlot> slots = solution.getScheduleSlots();

        if (slots == null || slots.isEmpty()) {
            throw new IllegalStateException("Solution has no schedule slots");
        }

        Schedule schedule = slots.get(0).getSchedule();

        // Update schedule properties
        schedule.setStatus(ScheduleStatus.DRAFT);  // Partial schedules stay in DRAFT

        if (solution.getScore() != null) {
            // setOptimizationScore expects Double, convert score to numeric value
            schedule.setOptimizationScore((double) solution.getScore().hardScore());
        }

        // Count assigned vs unassigned
        int assignedCount = 0;
        int unassignedCount = 0;

        // Update slot statuses
        for (ScheduleSlot slot : slots) {
            boolean fullyAssigned = slot.getTeacher() != null &&
                                   slot.getRoom() != null &&
                                   slot.getTimeSlot() != null;

            if (fullyAssigned) {
                slot.setStatus(SlotStatus.SCHEDULED);
                assignedCount++;
            } else {
                slot.setStatus(SlotStatus.CONFLICT);
                unassignedCount++;
            }
        }

        schedule.setTotalConflicts(unassignedCount);

        log.info("Partial schedule: {} assigned, {} unassigned", assignedCount, unassignedCount);

        // Save and return
        return scheduleRepository.save(schedule);
    }

    /**
     * Build partial result from partial schedule and conflicts
     */
    private ScheduleGenerationResult buildPartialResult(
            Schedule schedule,
            List<ConflictDetail> conflicts,
            SchedulingSolution solution,
            ScheduleGenerationRequest request) {

        // Count slots
        int totalSlots = solution.getScheduleSlots().size();
        int assignedSlots = (int) solution.getScheduleSlots().stream()
            .filter(slot -> slot.getTeacher() != null &&
                           slot.getRoom() != null &&
                           slot.getTimeSlot() != null)
            .count();

        int unassignedSlots = totalSlots - assignedSlots;

        // Calculate completion percentage
        double completionPct = totalSlots > 0 ? (assignedSlots * 100.0) / totalSlots : 0.0;

        // Build result
        ScheduleGenerationResult result = ScheduleGenerationResult.builder()
            .schedule(schedule)
            .status(ScheduleStatus.DRAFT)
            .conflicts(conflicts)
            .completionPercentage(completionPct)
            .totalCourses(totalSlots)
            .scheduledCourses(assignedSlots)
            .unscheduledCourses(unassignedSlots)
            .optimizationScore(solution.getScore() != null ?
                solution.getScore().toString() : "N/A")
            .generationTimeSeconds(0L)  // Could track if needed
            .build();

        // Calculate statistics and add recommendations
        result.calculateStatistics();
        result.addRecommendation(generateRecommendations(result));

        log.info("Partial result built: {}% complete, {} conflicts",
            String.format("%.1f", completionPct), conflicts.size());

        return result;
    }

    /**
     * Update progress callback
     */
    private void updateProgress(BiConsumer<Integer, String> callback, int percent, String message) {
        if (callback != null) {
            callback.accept(percent, message);
        }
        log.info("{}% - {}", percent, message);
    }
}
