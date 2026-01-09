package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.dto.ScheduleGenerationRequest;
import com.heronix.scheduler.model.enums.RoomType;
import com.heronix.scheduler.model.enums.ScheduleStatus;
import com.heronix.scheduler.model.enums.ScheduleType;
import com.heronix.scheduler.model.enums.SlotStatus;
import com.heronix.scheduler.model.planning.SchedulingSolution;
import com.heronix.scheduler.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.optaplanner.core.api.solver.SolverJob;
import org.optaplanner.core.api.solver.SolverManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.heronix.scheduler.model.enums.SchedulePeriod;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * Schedule Generation Service - AI-Powered Schedule Generation
 * Location:
 * src/main/java/com/eduscheduler/service/ScheduleGenerationService.java
 * 
 * Orchestrates the complete AI-powered schedule generation process using
 * OptaPlanner:
 * 1. Loads and validates resources (teachers, courses, rooms, students)
 * 2. Generates available time slots based on school hours
 * 3. Creates schedule slots for each course
 * 4. Runs OptaPlanner AI solver to optimize assignments
 * 5. Saves optimized schedule to database
 * 6. Calculates quality metrics
 * 
 * @author Heronix Scheduling System Team
 * @version 2.1.0
 * @since 2025-10-10
 */
@Slf4j
@Service
public class ScheduleGenerationService {

    // ========================================================================
    // DEPENDENCIES
    // ========================================================================

    @Autowired
    private com.heronix.scheduler.client.SISApiClient sisApiClient;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private SolverManager<SchedulingSolution, UUID> solverManager;

    @Autowired
    private com.heronix.scheduler.util.DiagnosticHelper diagnosticHelper;

    @Autowired
    private ScheduleDiagnosticService scheduleDiagnosticService;

    @Autowired
    private ConflictAnalysisService conflictAnalysisService;

    // Phase 5C: Multiple Rotating Lunch Periods
    @Autowired
    private LunchWaveService lunchWaveService;

    @Autowired
    private LunchAssignmentService lunchAssignmentService;

    @Autowired
    private LunchWaveRepository lunchWaveRepository;

    // ✅ PRIORITY 2 FIX December 15, 2025: Inject lunch assignment repositories
    // Needed for OptaPlanner constraint validation
    @Autowired
    private StudentLunchAssignmentRepository studentLunchAssignmentRepository;

    @Autowired
    private TeacherLunchAssignmentRepository teacherLunchAssignmentRepository;

    // ========================================================================
    // CONSTANTS
    // ========================================================================

    private static final int DEFAULT_START_HOUR = 7;
    private static final int DEFAULT_END_HOUR = 15;
    private static final int DEFAULT_PERIOD_DURATION = 50;
    private static final int DEFAULT_LUNCH_START_HOUR = 12;
    private static final int DEFAULT_LUNCH_DURATION = 30;
    private static final int DEFAULT_SESSIONS_PER_WEEK = 3;

    // ========================================================================
    // MAIN GENERATION METHOD
    // ========================================================================

    /**
     * Generate an optimized schedule using AI
     * 
     * @param request          Configuration parameters for generation
     * @param progressCallback Callback for progress updates (progress%, message)
     * @return Generated and optimized Schedule entity
     * @throws Exception if generation fails
     */
    @Transactional
    public Schedule generateSchedule(ScheduleGenerationRequest request,
            BiConsumer<Integer, String> progressCallback) throws Exception {
        return generateSchedule(request, progressCallback, false);
    }

    /**
     * Generate an optimized schedule using AI with option for partial schedules
     *
     * @param request          Configuration parameters for generation
     * @param progressCallback Callback for progress updates (progress%, message)
     * @param allowPartial     If true, allows saving partial schedules with constraint violations
     * @return Optimized schedule
     * @throws Exception if generation fails completely
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public Schedule generateSchedule(ScheduleGenerationRequest request,
            BiConsumer<Integer, String> progressCallback,
            boolean allowPartial) throws Exception {

        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║   STARTING AI SCHEDULE GENERATION (allowPartial={})            ║", allowPartial);
        log.info("╚════════════════════════════════════════════════════════════════╝");

        // Print diagnostics before generation
        diagnosticHelper.printSchedulingDiagnostics();

        UUID problemId = UUID.randomUUID();
        long startTime = System.currentTimeMillis();

        try {
            // ================================================================
            // PHASE 1: LOAD RESOURCES (5% - 10%)
            // ================================================================
            updateProgress(progressCallback, 5, "Loading resources...");

            List<Teacher> teachers = loadTeachers();
            List<Course> courses = loadCourses();
            List<Room> rooms = loadRooms();
            List<Student> students = loadStudents();

            log.info("✓ Loaded {} teachers, {} courses, {} rooms, {} students",
                    teachers.size(), courses.size(), rooms.size(), students.size());

            validateResources(teachers, courses, rooms);

            updateProgress(progressCallback, 10,
                    String.format("Resources loaded: %d teachers, %d courses, %d rooms",
                            teachers.size(), courses.size(), rooms.size()));

            // ================================================================
            // PHASE 2: GENERATE TIME SLOTS (10% - 20%)
            // ================================================================
            updateProgress(progressCallback, 15, "Generating time slots...");

            // Note: Time slots are generated BEFORE lunch waves are created
            // If multiple lunches are enabled, we'll generate split period placeholders
            // that will be populated with actual lunch wave assignments later
            List<TimeSlot> timeSlots = generateTimeSlots(request);

            log.info("✓ Generated {} time slots", timeSlots.size());
            updateProgress(progressCallback, 20,
                    String.format("Generated %d time slots", timeSlots.size()));

            // ================================================================
            // PHASE 3: CREATE SCHEDULE ENTITY (20% - 25%)
            // ================================================================
            updateProgress(progressCallback, 22, "Creating schedule entity...");

            Schedule schedule = createScheduleEntity(request);
            schedule = scheduleRepository.save(schedule);

            log.info("✓ Created schedule: {} (ID: {})", schedule.getName(), schedule.getId());
            updateProgress(progressCallback, 25, "Schedule entity created");

            // ================================================================
            // PHASE 3.5: CREATE LUNCH WAVES (NEW - Phase 5C)
            // ================================================================
            // If multiple lunch periods are enabled, create lunch waves and assign students/teachers
            if (request.getEnableMultipleLunches() != null && request.getEnableMultipleLunches()) {
                updateProgress(progressCallback, 26, "Creating lunch waves...");

                int waveCount = request.getLunchWaveCount() != null ? request.getLunchWaveCount() : 1;
                log.info("🍽️ Creating {} lunch waves for schedule", waveCount);

                // Create lunch waves using service
                List<LunchWave> waves = lunchWaveService.createLunchWavesForSchedule(schedule, request);
                log.info("✓ Created {} lunch waves", waves.size());

                // Assign students to lunch waves
                updateProgress(progressCallback, 27, "Assigning students to lunch waves...");
                int assignedStudents = lunchAssignmentService.assignStudentsToLunchWaves(
                    schedule.getId(),
                    request.getLunchAssignmentMethod()
                );
                log.info("✓ Assigned {} students to lunch waves using {} method",
                    assignedStudents, request.getLunchAssignmentMethod());

                // Assign teachers to lunch waves
                int assignedTeachers = lunchAssignmentService.assignTeachersToLunchWaves(schedule.getId());
                log.info("✓ Assigned {} teachers to lunch waves", assignedTeachers);

                // Validate assignments
                if (!lunchAssignmentService.areAssignmentsValid(schedule.getId())) {
                    log.warn("⚠️ Lunch wave assignments have validation warnings - check logs");
                }

                // Log statistics
                var stats = lunchAssignmentService.getAssignmentStatistics(schedule.getId());
                log.info("📊 Lunch Wave Statistics:");
                log.info("   Students: {}/{} assigned", stats.getAssignedStudents(), stats.getTotalStudents());
                log.info("   Teachers: {}/{} assigned", stats.getAssignedTeachers(), stats.getTotalTeachers());
                log.info("   Teachers with supervision: {}", stats.getTeachersWithDuty());
            }

            // ================================================================
            // PHASE 4: CREATE SCHEDULE SLOTS (25% - 35%)
            // ================================================================
            updateProgress(progressCallback, 28, "Creating schedule slots for courses...");

            List<ScheduleSlot> scheduleSlots = createScheduleSlots(schedule, courses, students);

            log.info("✓ Created {} schedule slots, saving to database...", scheduleSlots.size());
            scheduleSlots = scheduleSlotRepository.saveAll(scheduleSlots);
            log.info("✓ Schedule slots saved with IDs");

            // ================================================================
            // PHASE 4.5: CREATE LUNCH PERIOD SLOTS (NEW - FIX FOR CONSTRAINT VIOLATIONS)
            // ================================================================
            // CRITICAL: Create lunch ScheduleSlots to satisfy lunch period constraints
            // Without these, OptaPlanner will report hard constraint violations
            if (isLunchEnabled(request)) {
                updateProgress(progressCallback, 32, "Creating lunch period slots...");

                // Find lunch TimeSlots (those created during lunch period)
                List<TimeSlot> lunchTimeSlots = timeSlots.stream()
                        .filter(ts -> isLunchPeriod(ts.getStartTime(), request))
                        .collect(Collectors.toList());

                if (!lunchTimeSlots.isEmpty()) {
                    // Find cafeteria rooms
                    List<Room> cafeterias = rooms.stream()
                            .filter(r -> r.getType() == RoomType.CAFETERIA)
                            .collect(Collectors.toList());

                    // Create lunch schedule slots
                    List<ScheduleSlot> lunchSlots = createLunchPeriodSlots(
                            schedule, request, lunchTimeSlots, students, teachers, cafeterias);

                    // Save lunch slots to database
                    lunchSlots = scheduleSlotRepository.saveAll(lunchSlots);

                    // Add lunch slots to the main schedule slots list
                    scheduleSlots.addAll(lunchSlots);

                    log.info("✅ Lunch periods configured: {} time slots, {} schedule slots",
                             lunchTimeSlots.size(), lunchSlots.size());
                } else {
                    log.warn("⚠️ Lunch enabled but no lunch time slots found!");
                }
            }

            updateProgress(progressCallback, 35,
                    String.format("Created %d schedule slots", scheduleSlots.size()));

            // ================================================================
            // PHASE 5: BUILD OPTAPLANNER SOLUTION (35% - 45%)
            // ================================================================
            updateProgress(progressCallback, 38, "Building optimization problem...");

            // ✅ PRIORITY 2 FIX December 15, 2025: Load lunch wave assignments for constraint validation
            // These provide OptaPlanner with the student/teacher lunch wave mappings
            // so constraints can validate that students/teachers are in correct lunch waves
            List<StudentLunchAssignment> studentLunchAssignments = new ArrayList<>();
            List<TeacherLunchAssignment> teacherLunchAssignments = new ArrayList<>();

            if (isLunchEnabled(request)) {
                studentLunchAssignments = studentLunchAssignmentRepository.findByScheduleId(schedule.getId());
                teacherLunchAssignments = teacherLunchAssignmentRepository.findByScheduleId(schedule.getId());
                log.info("✓ Loaded {} student lunch assignments and {} teacher lunch assignments for constraint validation",
                        studentLunchAssignments.size(), teacherLunchAssignments.size());
            }

            SchedulingSolution problem = new SchedulingSolution(
                    scheduleSlots, teachers, rooms, timeSlots, courses, students);

            // Set lunch assignments (new fields added to SchedulingSolution)
            problem.setStudentLunchAssignments(studentLunchAssignments);
            problem.setTeacherLunchAssignments(teacherLunchAssignments);

            log.info("✓ OptaPlanner solution built with {} planning variables",
                    scheduleSlots.size());
            updateProgress(progressCallback, 45, "Optimization problem ready");

            // ================================================================
            // PHASE 6: RUN AI OPTIMIZATION (45% - 90%)
            // ================================================================
            updateProgress(progressCallback, 45, "Starting AI optimization...");

            log.info("═══════════════════════════════════════════════════════════════");
            log.info("RUNNING OPTAPLANNER SOLVER");
            log.info("═══════════════════════════════════════════════════════════════");

            SolverJob<SchedulingSolution, UUID> solverJob = solverManager.solve(problemId, problem);
            monitorSolverProgress(solverJob, progressCallback, 45, 90);
            SchedulingSolution solution = solverJob.getFinalBestSolution();

            validateSolution(solution, allowPartial);

            log.info("═══════════════════════════════════════════════════════════════");
            log.info("SOLVER COMPLETED - Score: {}", solution.getScore());
            log.info("═══════════════════════════════════════════════════════════════");
            updateProgress(progressCallback, 90, "Optimization complete!");

            // ================================================================
            // PHASE 7: SAVE RESULTS (90% - 95%)
            // ================================================================
            updateProgress(progressCallback, 92, "Saving optimized schedule...");

            saveOptimizedSchedule(schedule, solution);

            log.info("✓ Schedule saved to database");
            updateProgress(progressCallback, 95, "Schedule saved");

            // ================================================================
            // PHASE 8: CALCULATE METRICS (95% - 100%)
            // ================================================================
            updateProgress(progressCallback, 97, "Calculating metrics...");

            calculateScheduleMetrics(schedule, solution);
            schedule = scheduleRepository.save(schedule);

            // Analyze the generated schedule for issues
            diagnosticHelper.analyzeSchedule(schedule.getId());

            long duration = (System.currentTimeMillis() - startTime) / 1000;

            log.info("✓ Metrics calculated");
            updateProgress(progressCallback, 100, "Generation complete!");

            log.info("╔════════════════════════════════════════════════════════════════╗");
            log.info("║   SCHEDULE GENERATION COMPLETE                                 ║");
            log.info("║   Schedule: {:50} ║", schedule.getName());
            log.info("║   Score: {:55} ║", solution.getScore());
            log.info("║   Duration: {} seconds {:40} ║", duration);
            log.info("╚════════════════════════════════════════════════════════════════╝");

            return schedule;

        } catch (Exception e) {
            log.error("❌ SCHEDULE GENERATION FAILED", e);

            // Generate diagnostic report to help user understand what went wrong
            try {
                log.info("Generating diagnostic report to identify issues...");
                com.heronix.scheduler.model.dto.ScheduleDiagnosticReport diagnosticReport =
                    scheduleDiagnosticService.generateDiagnosticReport();

                // Build user-friendly error message
                String userMessage = buildUserFriendlyErrorMessage(e, diagnosticReport);
                updateProgress(progressCallback, 0, userMessage);
                throw new RuntimeException(userMessage, e);
            } catch (Exception diagEx) {
                // If diagnostic fails, fall back to original error
                log.warn("Failed to generate diagnostic report", diagEx);
                updateProgress(progressCallback, 0, "Generation failed: " + e.getMessage());
                throw new RuntimeException("Schedule generation failed: " + e.getMessage(), e);
            }
        }
    }

    // ========================================================================
    // RESOURCE LOADING
    // ========================================================================

    /**
     * Load active teachers from SIS via REST API
     * Converts TeacherDTOs to Teacher entities for scheduling
     */
    private List<Teacher> loadTeachers() {
        log.debug("Fetching teachers from SIS API...");
        List<com.heronix.scheduler.model.dto.TeacherDTO> teacherDTOs = sisApiClient.getAllTeachers();
        List<Teacher> teachers = com.heronix.scheduler.util.DTOConverter.toTeachers(teacherDTOs);
        log.debug("Loaded {} active teachers from SIS", teachers.size());
        return teachers;
    }

    /**
     * Load active courses from SIS via REST API
     * Converts CourseDTOs to Course entities for scheduling
     */
    private List<Course> loadCourses() {
        log.debug("Fetching courses from SIS API...");
        List<com.heronix.scheduler.model.dto.CourseDTO> courseDTOs = sisApiClient.getAllCourses();
        List<Course> courses = com.heronix.scheduler.util.DTOConverter.toCourses(courseDTOs);
        log.debug("Loaded {} active courses from SIS", courses.size());
        return courses;
    }

    /**
     * Load all rooms from local database
     * Rooms are scheduler-specific and managed locally
     */
    private List<Room> loadRooms() {
        List<Room> rooms = roomRepository.findAll();
        log.debug("Loaded {} rooms from local database", rooms.size());
        return rooms;
    }

    /**
     * Load all students from SIS via REST API
     * Converts StudentDTOs to Student entities for scheduling
     */
    private List<Student> loadStudents() {
        log.debug("Fetching students from SIS API...");
        List<com.heronix.scheduler.model.dto.StudentDTO> studentDTOs = sisApiClient.getAllStudents();
        List<Student> students = com.heronix.scheduler.util.DTOConverter.toStudents(studentDTOs);
        log.debug("Loaded {} students from SIS", students.size());
        return students;
    }

    // ========================================================================
    // RESOURCE VALIDATION
    // ========================================================================

    /**
     * Validate that we have sufficient resources to generate a schedule
     * 
     * @throws IllegalStateException if resources are insufficient
     */
    private void validateResources(List<Teacher> teachers, List<Course> courses, List<Room> rooms) {
        // ✅ NULL SAFE: Validate all resource lists exist before checking size
        if (teachers == null || teachers.isEmpty()) {
            throw new IllegalStateException("Cannot generate schedule: No teachers available. " +
                    "Please add teachers before generating a schedule.");
        }
        if (courses == null || courses.isEmpty()) {
            throw new IllegalStateException("Cannot generate schedule: No courses available. " +
                    "Please add courses before generating a schedule.");
        }
        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalStateException("Cannot generate schedule: No rooms available. " +
                    "Please add rooms before generating a schedule.");
        }

        log.debug("✓ Resource validation passed: {} teachers, {} courses, {} rooms",
                teachers.size(), courses.size(), rooms.size());
    }

    // ========================================================================
    // TIME SLOT GENERATION
    // ========================================================================

    /**
     * Generate time slots based on request parameters
     * Creates all possible scheduling windows for the school week
     * 
     * @param request Configuration containing start/end times, period duration
     * @return List of available time slots
     */
    private List<TimeSlot> generateTimeSlots(ScheduleGenerationRequest request) {
        List<TimeSlot> timeSlots = new ArrayList<>();

        // Extract parameters with backward compatibility
        // Priority: Use new LocalTime fields if present, otherwise fall back to legacy hour fields
        LocalTime startTime;
        LocalTime schoolEndTime;

        if (request.getFirstPeriodStartTime() != null) {
            // Use new minute-precise start time (e.g., 07:20)
            startTime = request.getFirstPeriodStartTime();
            log.debug("Using minute-precise start time: {}", startTime);
        } else {
            // Fall back to legacy hour-based start time (e.g., 07:00)
            int startHour = getValueOrDefault(request.getStartHour(), DEFAULT_START_HOUR);
            startTime = LocalTime.of(startHour, 0);
            log.debug("Using legacy hour-based start time: {}:00", startHour);
        }

        if (request.getSchoolEndTime() != null) {
            // Use new minute-precise end time (e.g., 14:10)
            schoolEndTime = request.getSchoolEndTime();
            log.debug("Using minute-precise end time: {}", schoolEndTime);
        } else {
            // Fall back to legacy hour-based end time (e.g., 14:00)
            int endHour = getValueOrDefault(request.getEndHour(), DEFAULT_END_HOUR);
            schoolEndTime = LocalTime.of(endHour, 0);
            log.debug("Using legacy hour-based end time: {}:00", endHour);
        }

        int periodDuration = getValueOrDefault(request.getPeriodDuration(), DEFAULT_PERIOD_DURATION);
        int passingPeriodDuration = getValueOrDefault(request.getPassingPeriodDuration(), 0); // Default: no passing periods

        // School days (Monday - Friday)
        List<DayOfWeek> daysOfWeek = List.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY);

        // Check if multiple lunch periods are enabled (Phase 5C)
        boolean multipleLunchesEnabled = request.getEnableMultipleLunches() != null
            && request.getEnableMultipleLunches();

        // Generate time slots for each day
        for (DayOfWeek day : daysOfWeek) {
            int periodNumber = 1;
            LocalTime currentTime = startTime;
            LocalTime endTime = schoolEndTime;

            while (currentTime.plusMinutes(periodDuration).isBefore(endTime) ||
                    currentTime.plusMinutes(periodDuration).equals(endTime)) {

                LocalTime slotEnd = currentTime.plusMinutes(periodDuration);

                // PHASE 5C: Handle multiple rotating lunch periods with split periods
                if (multipleLunchesEnabled && isLunchEnabled(request) && isLunchPeriod(currentTime, request)) {
                    log.debug("Creating split periods for multiple lunch waves at {} on {}", currentTime, day);

                    // Generate split periods (e.g., 4A/Lunch1/4B/Lunch2/4C/Lunch3)
                    List<TimeSlot> splitPeriods = generateSplitPeriodsForLunchWaves(
                        day, periodNumber, currentTime, slotEnd, request);

                    timeSlots.addAll(splitPeriods);

                    // Move past the entire lunch period block
                    // The split period generation handles all lunch waves and split segments
                    int lunchBlockDuration = calculateLunchBlockDuration(request);
                    currentTime = currentTime.plusMinutes(lunchBlockDuration);
                    periodNumber++; // All splits share same period number, so only increment once

                    log.debug("Added {} split period time slots for period {} lunch block",
                        splitPeriods.size(), periodNumber - 1);
                    continue;
                }

                // LEGACY: Single lunch period handling (backward compatible)
                if (isLunchEnabled(request) && isLunchPeriod(currentTime, request)) {
                    int lunchDuration = getValueOrDefault(request.getLunchDuration(), DEFAULT_LUNCH_DURATION);
                    LocalTime lunchEnd = currentTime.plusMinutes(lunchDuration);

                    // Create TimeSlot for lunch period (will be populated with lunch ScheduleSlots later)
                    TimeSlot lunchTimeSlot = new TimeSlot(day, currentTime, lunchEnd, periodNumber);
                    timeSlots.add(lunchTimeSlot);

                    log.debug("Created single lunch period TimeSlot for {} at {} ({}min)", day, currentTime, lunchDuration);

                    currentTime = lunchEnd;
                    periodNumber++;
                    continue;
                }

                // Regular period (not lunch)
                TimeSlot slot = new TimeSlot(day, currentTime, slotEnd, periodNumber);
                timeSlots.add(slot);

                // Add passing period after this slot (if configured)
                if (passingPeriodDuration > 0) {
                    currentTime = slotEnd.plusMinutes(passingPeriodDuration);
                    log.debug("Added {}min passing period after period {} on {}", passingPeriodDuration, periodNumber, day);
                } else {
                    currentTime = slotEnd;
                }
                periodNumber++;
            }
        }

        log.debug("Generated {} time slots from {} to {} ({}min periods, {}min passing)",
                timeSlots.size(), startTime, schoolEndTime, periodDuration, passingPeriodDuration);

        return timeSlots;
    }

    /**
     * Check if lunch is enabled in request
     */
    private boolean isLunchEnabled(ScheduleGenerationRequest request) {
        return request.getEnableLunch() != null && request.getEnableLunch();
    }

    /**
     * Check if given time falls within lunch period
     * Supports both new LocalTime fields and legacy hour-based fields
     */
    private boolean isLunchPeriod(LocalTime time, ScheduleGenerationRequest request) {
        LocalTime lunchStart;

        if (request.getLunchStartTime() != null) {
            // Use new minute-precise lunch start time (e.g., 10:50)
            lunchStart = request.getLunchStartTime();
        } else {
            // Fall back to legacy hour-based lunch start time (e.g., 12:00)
            int lunchStartHour = getValueOrDefault(request.getLunchStartHour(), DEFAULT_LUNCH_START_HOUR);
            lunchStart = LocalTime.of(lunchStartHour, 0);
        }

        int lunchDuration = getValueOrDefault(request.getLunchDuration(), DEFAULT_LUNCH_DURATION);
        LocalTime lunchEnd = lunchStart.plusMinutes(lunchDuration);

        return !time.isBefore(lunchStart) && time.isBefore(lunchEnd);
    }

    /**
     * Calculate total duration of lunch block including all lunch waves
     * Phase 5C: Multiple Rotating Lunch Periods
     *
     * Example for 3 lunch waves @ 30 minutes each with 54-minute periods:
     * - Split A: 18 min (1/3 of period)
     * - Lunch 1: 30 min
     * - Split B: 18 min
     * - Lunch 2: 30 min
     * - Split C: 18 min
     * - Lunch 3: 30 min
     * Total: 144 minutes
     *
     * @param request Generation request containing lunch configuration
     * @return Total duration of lunch block in minutes
     */
    private int calculateLunchBlockDuration(ScheduleGenerationRequest request) {
        int periodDuration = getValueOrDefault(request.getPeriodDuration(), DEFAULT_PERIOD_DURATION);
        int lunchDuration = getValueOrDefault(request.getLunchDuration(), DEFAULT_LUNCH_DURATION);
        int waveCount = request.getLunchWaveCount() != null ? request.getLunchWaveCount() : 1;

        // Each lunch wave takes lunchDuration minutes
        int totalLunchTime = waveCount * lunchDuration;

        // Split period segments fill the gaps between lunches
        // For N waves, we have N+1 split segments (A, B, C for 2 waves, etc.)
        int splitCount = waveCount + 1;

        // ✅ PRIORITY 2 FIX December 15, 2025: Use proper rounding to avoid losing remainder
        // Example: 54 min ÷ 5 splits = 10.8 min → round UP to 11 min (not 10!)
        // Integer division loses precision: 54/5=10 (loses 4 minutes!)
        // Using Math.ceil ensures we don't lose time
        int splitSegmentDuration = (int) Math.ceil((double) periodDuration / splitCount);
        int totalSplitTime = splitCount * splitSegmentDuration;

        int total = totalLunchTime + totalSplitTime;

        log.debug("Lunch block duration: {} waves × {}min lunch + {} splits × {}min = {}min",
            waveCount, lunchDuration, splitCount, splitSegmentDuration, total);

        return total;
    }

    /**
     * Generate split periods for multiple rotating lunch waves
     * Phase 5C: Multiple Rotating Lunch Periods
     *
     * Creates time slots with splitPeriodLabel for period segments that are divided by lunch waves.
     *
     * Example: Period 4 with 3 lunch waves:
     * - 4A: 10:04-10:22 (18 min)
     * - Lunch 1: 10:22-10:52 (30 min)
     * - 4B: 10:52-11:10 (18 min)
     * - Lunch 2: 11:10-11:40 (30 min)
     * - 4C: 11:40-11:58 (18 min)
     * - Lunch 3: 11:58-12:28 (30 min)
     *
     * @param day Day of week
     * @param periodNumber Period number (all splits share this number)
     * @param periodStart Start time of the period
     * @param periodEnd Planned end time (may extend due to lunches)
     * @param request Generation request
     * @return List of TimeSlot objects for split periods and lunch waves
     */
    private List<TimeSlot> generateSplitPeriodsForLunchWaves(
            DayOfWeek day,
            int periodNumber,
            LocalTime periodStart,
            LocalTime periodEnd,
            ScheduleGenerationRequest request) {

        List<TimeSlot> slots = new ArrayList<>();

        int periodDuration = getValueOrDefault(request.getPeriodDuration(), DEFAULT_PERIOD_DURATION);
        int lunchDuration = getValueOrDefault(request.getLunchDuration(), DEFAULT_LUNCH_DURATION);
        int waveCount = request.getLunchWaveCount() != null ? request.getLunchWaveCount() : 1;

        // Calculate split segment duration
        // For N lunch waves, we have N+1 split segments
        int splitCount = waveCount + 1;
        int splitSegmentDuration = periodDuration / splitCount;

        LocalTime currentTime = periodStart;
        char splitLabel = 'A';

        // Interleave split periods and lunch waves
        for (int wave = 0; wave < waveCount; wave++) {
            // Create split period segment (e.g., "4A", "4B", "4C")
            LocalTime splitEnd = currentTime.plusMinutes(splitSegmentDuration);
            TimeSlot splitSlot = new TimeSlot(day, currentTime, splitEnd, periodNumber);
            splitSlot.setSplitPeriodLabel(String.valueOf(splitLabel));
            slots.add(splitSlot);

            log.debug("  Split period {}{}: {} - {} ({}min)",
                periodNumber, splitLabel, currentTime, splitEnd, splitSegmentDuration);

            currentTime = splitEnd;
            splitLabel++;

            // Create lunch wave time slot (e.g., "Lunch 1", "Lunch 2")
            LocalTime lunchEnd = currentTime.plusMinutes(lunchDuration);
            TimeSlot lunchSlot = new TimeSlot(day, currentTime, lunchEnd, periodNumber);
            lunchSlot.setLunchWaveLabel("Lunch " + (wave + 1));
            slots.add(lunchSlot);

            log.debug("  Lunch {}: {} - {} ({}min)",
                wave + 1, currentTime, lunchEnd, lunchDuration);

            currentTime = lunchEnd;
        }

        // Final split period segment (after last lunch)
        LocalTime finalSplitEnd = currentTime.plusMinutes(splitSegmentDuration);
        TimeSlot finalSplitSlot = new TimeSlot(day, currentTime, finalSplitEnd, periodNumber);
        finalSplitSlot.setSplitPeriodLabel(String.valueOf(splitLabel));
        slots.add(finalSplitSlot);

        log.debug("  Split period {}{}: {} - {} ({}min)",
            periodNumber, splitLabel, currentTime, finalSplitEnd, splitSegmentDuration);

        log.info("Created {} time slots for period {} lunch block ({} waves)",
            slots.size(), periodNumber, waveCount);

        return slots;
    }

    // ========================================================================
    // SCHEDULE ENTITY CREATION
    // ========================================================================

    /**
     * Create Schedule entity from request parameters
     * 
     * @param request Generation request with schedule details
     * @return Newly created Schedule entity (not yet saved)
     */
    private Schedule createScheduleEntity(ScheduleGenerationRequest request) {
        Schedule schedule = new Schedule();

        // Basic information
        schedule.setName(request.getScheduleName());
        schedule.setScheduleType(request.getScheduleType());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());

        // Initial status and metrics
        schedule.setStatus(ScheduleStatus.DRAFT);
        schedule.setOptimizationScore(0.0);
        schedule.setTotalConflicts(0);

        // Set period based on schedule type (FIXED: Returns enum now)
        schedule.setPeriod(determinePeriod(request.getScheduleType()));

        log.debug("Created schedule entity: {} ({} to {})",
                schedule.getName(), schedule.getStartDate(), schedule.getEndDate());

        return schedule;
    }

    /**
     * Determine schedule period enum from schedule type
     * All generated schedules are MASTER schedules (full term)
     * 
     * @param type Schedule type enum (not used currently)
     * @return Period enum (always MASTER for generated schedules)
     */
    private SchedulePeriod determinePeriod(ScheduleType type) {
        // Generated schedules are always MASTER schedules (full term/semester)
        // WEEKLY and DAILY are for viewing/display purposes only
        return SchedulePeriod.MASTER;
    }

    // ========================================================================
    // SCHEDULE SLOT CREATION
    // ========================================================================

    /**
     * Create schedule slots for each course
     * Each course needs multiple slots per week based on sessionsPerWeek
     * 
     * @param schedule Parent schedule entity
     * @param courses  List of courses to schedule
     * @param students List of students for enrollment
     * @return List of created schedule slots (unassigned)
     */
    private List<ScheduleSlot> createScheduleSlots(Schedule schedule,
            List<Course> courses,
            List<Student> students) {
        List<ScheduleSlot> slots = new ArrayList<>();

        for (Course course : courses) {
            // Determine how many times per week this course meets
            int sessionsPerWeek = getValueOrDefault(course.getSessionsPerWeek(), DEFAULT_SESSIONS_PER_WEEK);

            // Get all students enrolled in this course
            List<Student> allEnrolledStudents = assignStudentsToCourse(students, course);

            // UPDATED December 15, 2025: Divide students among sections instead of duplicating
            // CRITICAL FIX: Use proper division with remainder distribution
            // Real-world example: English 1 has 107 students enrolled across 6 sections
            //   - Sections 1-5: 18 students each (5 × 18 = 90)
            //   - Section 6: 17 students (remainder)
            // Each student attends ONE section (not all 6!)

            // Calculate base students per section and remainder
            int baseStudentsPerSection = allEnrolledStudents.isEmpty() ? 0
                : allEnrolledStudents.size() / sessionsPerWeek;
            int remainder = allEnrolledStudents.isEmpty() ? 0
                : allEnrolledStudents.size() % sessionsPerWeek;

            log.debug("Course {} has {} enrolled students, creating {} sections: {} get {} students, {} get {} students",
                course.getCourseCode(), allEnrolledStudents.size(), sessionsPerWeek,
                remainder, baseStudentsPerSection + 1, sessionsPerWeek - remainder, baseStudentsPerSection);

            // Track current position in student list
            int currentIndex = 0;

            // Create that many slots for this course, dividing students among them
            for (int sessionNumber = 1; sessionNumber <= sessionsPerWeek; sessionNumber++) {
                ScheduleSlot slot = new ScheduleSlot();

                // Link to parent schedule and course
                slot.setSchedule(schedule);
                slot.setCourse(course);

                // Initial status (OptaPlanner will assign teacher/room/time)
                slot.setStatus(SlotStatus.DRAFT);
                slot.setHasConflict(false);

                // UPDATED December 15, 2025: Pre-assign teacher and room from course
                // This ensures ALL sections of a course have the SAME teacher and room
                // Example: English 1 Period 1, 2, 3 all taught by Maria in Room 300
                if (course.getTeacher() != null) {
                    slot.setTeacher(course.getTeacher());
                    log.debug("  Pre-assigned teacher {} to section {}",
                        course.getTeacher().getName(), sessionNumber);
                }

                if (course.getRoom() != null) {
                    slot.setRoom(course.getRoom());
                    log.debug("  Pre-assigned room {} to section {}",
                        course.getRoom().getRoomNumber(), sessionNumber);
                }

                // DIVIDE students among sections (not duplicate!)
                // First 'remainder' sections get one extra student
                int sectionSize = baseStudentsPerSection + (sessionNumber <= remainder ? 1 : 0);
                int endIdx = Math.min(currentIndex + sectionSize, allEnrolledStudents.size());

                // Extract sublist of students for THIS section only
                List<Student> sectionStudents = (currentIndex < allEnrolledStudents.size())
                    ? new ArrayList<>(allEnrolledStudents.subList(currentIndex, endIdx))
                    : new ArrayList<>();

                slot.setStudents(sectionStudents);

                log.debug("  Section {}: {} students (indices {}-{})",
                    sessionNumber, sectionStudents.size(), currentIndex, endIdx - 1);

                currentIndex = endIdx;  // Move to next group of students
                slots.add(slot);
            }
        }

        log.info("Created {} schedule slots from {} courses (avg {:.1f} sessions/course)",
                slots.size(), courses.size(), (double) slots.size() / courses.size());

        return slots;
    }

    /**
     * Assign students to course based on enrollment
     * Uses actual enrollment data from Student.enrolledCourses relationship
     *
     * @param students All available students
     * @param course   Course to enroll students in
     * @return List of enrolled students
     */
    private List<Student> assignStudentsToCourse(List<Student> students, Course course) {
        // ✅ NULL SAFE: Check students list and course exist
        if (students == null || course == null) {
            return new ArrayList<>();
        }

        // Filter students who are actually enrolled in this course
        // Note: enrolledCourses is now eagerly loaded via JOIN FETCH, so no lazy loading exception
        // ✅ NULL SAFE: Filter null students and check enrolledCourses exists
        List<Student> enrolledStudents = students.stream()
                .filter(student -> student != null && student.getEnrolledCourses() != null
                                && student.getEnrolledCourses().contains(course))
                .collect(Collectors.toList());

        // Log enrollment statistics
        log.debug("Course {} has {} enrolled students out of {} total students",
                course.getCourseCode(), enrolledStudents.size(), students.size());

        return enrolledStudents;
    }

    /**
     * Create lunch period schedule slots for all students and teachers
     * This satisfies the HARD constraints: studentLunchPeriod and teacherLunchPeriod
     *
     * CRITICAL: OptaPlanner requires lunch ScheduleSlots with isLunchPeriod=true
     * to satisfy lunch period constraints. Without these, schedule generation fails.
     *
     * @param schedule       The schedule being generated
     * @param lunchTimeSlots List of time slots designated for lunch
     * @param students       All students who need lunch
     * @param teachers       All teachers who need lunch
     * @param cafeterias     List of cafeteria rooms (can be empty/null)
     * @return List of created lunch ScheduleSlots
     */
    private List<ScheduleSlot> createLunchPeriodSlots(
            Schedule schedule,
            ScheduleGenerationRequest request,
            List<TimeSlot> lunchTimeSlots,
            List<Student> students,
            List<Teacher> teachers,
            List<Room> cafeterias) {

        List<ScheduleSlot> lunchSlots = new ArrayList<>();

        log.info("🍽️ Creating lunch period slots for {} students and {} teachers",
                 students.size(), teachers.size());

        if (lunchTimeSlots.isEmpty()) {
            log.warn("⚠️ No lunch time slots found! Lunch constraints may fail.");
            return lunchSlots;
        }

        // Get cafeteria room if available (nullable - some schools don't have dedicated cafeterias)
        Room cafeteria = (cafeterias != null && !cafeterias.isEmpty()) ? cafeterias.get(0) : null;

        // ===================================================================
        // FLEXIBLE LUNCH WAVE CONFIGURATION (Phase 5: Administrator-Configurable)
        // ===================================================================
        // Determine number of waves and capacity based on administrator settings
        int lunchWaveCount = (request.getLunchWaveCount() != null && request.getLunchWaveCount() > 0)
                           ? request.getLunchWaveCount()
                           : calculateOptimalWaveCount(students.size(), cafeterias);

        // Calculate students per wave based on total students and wave count
        int studentsPerWave = (int) Math.ceil((double) students.size() / lunchWaveCount);

        log.info("📊 Lunch Configuration: {} waves, ~{} students per wave (Total students: {})",
                 lunchWaveCount, studentsPerWave, students.size());

        // ===================================================================
        // PHASE 5C: COHORT-BASED WAVE ASSIGNMENT
        // ===================================================================
        // Assign students to lunch waves based on grade level cohorts
        // (Course schedule not available yet - will be determined by OptaPlanner)
        List<com.heronix.scheduler.model.domain.LunchWave> waveAssignments =
                assignLunchWavesByGradeLevel(students, lunchWaveCount);

        // ===================================================================
        // CREATE LUNCH SLOTS FOR ALL DAYS (FIXED: was only creating for first day)
        // ===================================================================
        // CRITICAL FIX: Loop through ALL lunch TimeSlots (one per day M-F)
        // Previously only used lunchTimeSlots.get(0) which created lunch for one day only
        int totalStudentWaves = 0;
        int totalTeacherSlots = 0;

        for (TimeSlot lunchTimeSlot : lunchTimeSlots) {
            log.debug("Creating lunch slots for {} at {}-{}",
                     lunchTimeSlot.getDayOfWeek(),
                     lunchTimeSlot.getStartTime(),
                     lunchTimeSlot.getEndTime());

            // ===================================================================
            // CREATE STUDENT LUNCH SLOTS FOR THIS DAY (Using cohort-based assignments)
            // ===================================================================

            for (com.heronix.scheduler.model.domain.LunchWave wave : waveAssignments) {
                List<Student> waveStudents = wave.getAllStudents();

                ScheduleSlot studentLunchSlot = new ScheduleSlot();
                studentLunchSlot.setSchedule(schedule);
                studentLunchSlot.setTimeSlot(lunchTimeSlot);
                studentLunchSlot.setDayOfWeek(lunchTimeSlot.getDayOfWeek());
                studentLunchSlot.setStartTime(lunchTimeSlot.getStartTime());
                studentLunchSlot.setEndTime(lunchTimeSlot.getEndTime());
                studentLunchSlot.setPeriodNumber(lunchTimeSlot.getPeriodNumber());
                studentLunchSlot.setRoom(cafeteria);  // Nullable
                studentLunchSlot.setIsLunchPeriod(true);  // CRITICAL: Mark as lunch for constraints
                studentLunchSlot.setLunchWaveNumber(wave.getNumber());
                studentLunchSlot.setStudents(new ArrayList<>(waveStudents));
                studentLunchSlot.setPinned(true);  // Lock so OptaPlanner doesn't try to move it

                lunchSlots.add(studentLunchSlot);
                totalStudentWaves++;
            }

            // ===================================================================
            // CREATE TEACHER LUNCH SLOTS FOR THIS DAY
            // ===================================================================

            for (Teacher teacher : teachers) {
                ScheduleSlot teacherLunchSlot = new ScheduleSlot();
                teacherLunchSlot.setSchedule(schedule);
                teacherLunchSlot.setTimeSlot(lunchTimeSlot);
                teacherLunchSlot.setDayOfWeek(lunchTimeSlot.getDayOfWeek());
                teacherLunchSlot.setStartTime(lunchTimeSlot.getStartTime());
                teacherLunchSlot.setEndTime(lunchTimeSlot.getEndTime());
                teacherLunchSlot.setPeriodNumber(lunchTimeSlot.getPeriodNumber());
                teacherLunchSlot.setTeacher(teacher);
                teacherLunchSlot.setRoom(cafeteria);  // Nullable
                teacherLunchSlot.setIsLunchPeriod(true);  // CRITICAL: Mark as lunch for constraints
                teacherLunchSlot.setPinned(true);  // Lock so OptaPlanner doesn't move it

                lunchSlots.add(teacherLunchSlot);
                totalTeacherSlots++;
            }
        }

        log.info("✅ Created {} student lunch waves and {} teacher lunch slots across {} days (total: {} lunch slots)",
                 totalStudentWaves, totalTeacherSlots, lunchTimeSlots.size(), lunchSlots.size());

        return lunchSlots;
    }

    /**
     * Calculate optimal number of lunch waves based on student population and cafeteria capacity
     * Phase 5: Smart defaults for administrator flexibility
     *
     * Strategy:
     * - Small schools (< 200 students): 1 wave
     * - Medium schools (200-500): 2 waves
     * - Large schools (500-1000): 3 waves
     * - Very large schools (1000-2000): 4 waves
     * - Mega schools (2000-4000): 5 waves
     * - Massive schools (4000+): 6 waves
     *
     * Also considers cafeteria capacity:
     * - Multiple cafeterias can support more students per wave
     * - Typical cafeteria seats 250-350 students
     *
     * @param studentCount  Total number of students
     * @param cafeterias    Available cafeteria rooms
     * @return Optimal number of lunch waves (1-6)
     */
    private int calculateOptimalWaveCount(int studentCount, List<Room> cafeterias) {
        // Determine base cafeteria capacity
        int totalCafeteriaCapacity = 0;
        if (cafeterias != null && !cafeterias.isEmpty()) {
            for (Room cafeteria : cafeterias) {
                // Use room capacity if set, otherwise default to 250
                int capacity = (cafeteria.getCapacity() != null && cafeteria.getCapacity() > 0)
                             ? cafeteria.getCapacity()
                             : 250;
                totalCafeteriaCapacity += capacity;
            }
        } else {
            // No cafeterias defined, use default capacity of 250
            totalCafeteriaCapacity = 250;
        }

        // Calculate minimum waves needed based on capacity
        int minWavesForCapacity = (int) Math.ceil((double) studentCount / totalCafeteriaCapacity);

        // Apply school size guidelines
        int recommendedWaves;
        if (studentCount < 200) {
            recommendedWaves = 1;
        } else if (studentCount < 500) {
            recommendedWaves = 2;
        } else if (studentCount < 1000) {
            recommendedWaves = 3;
        } else if (studentCount < 2000) {
            recommendedWaves = 4;
        } else if (studentCount < 4000) {
            recommendedWaves = 5;
        } else {
            recommendedWaves = 6;
        }

        // Use the maximum of capacity-based and size-based recommendations
        int optimalWaves = Math.max(minWavesForCapacity, recommendedWaves);

        // Cap at 6 waves (practical maximum for K-12 schools)
        optimalWaves = Math.min(optimalWaves, 6);

        log.info("📊 Calculated optimal lunch waves: {} (students: {}, cafeteria capacity: {}, cafeterias: {})",
                 optimalWaves, studentCount, totalCafeteriaCapacity,
                 cafeterias != null ? cafeterias.size() : 0);

        return optimalWaves;
    }

    // ========================================================================
    // COHORT-BASED LUNCH WAVE ASSIGNMENT (Phase 5C)
    // ========================================================================

    /**
     * Assign students to lunch waves based on grade level cohorts
     * Phase 5C: Grade-level cohort assignment (used when course schedule not yet available)
     *
     * Strategy:
     * 1. Group students by grade level
     * 2. Use bin packing to assign grade-level cohorts to waves
     * 3. Balance wave sizes while keeping grades together when possible
     *
     * @param students  All students who need lunch
     * @param waveCount Number of lunch waves
     * @return List of lunch waves with assigned cohorts
     */
    private List<com.heronix.scheduler.model.domain.LunchWave> assignLunchWavesByGradeLevel(
            List<Student> students,
            int waveCount) {

        log.info("🎯 Assigning lunch waves by grade level for {} students into {} waves",
                 students.size(), waveCount);

        // Group students by grade level
        Map<String, List<Student>> gradeCohorts = new HashMap<>();
        for (Student student : students) {
            String gradeLevel = student.getGradeLevel();
            String cohortKey = gradeLevel != null
                    ? String.format("Grade %s", gradeLevel)
                    : "Unassigned Students";
            gradeCohorts.computeIfAbsent(cohortKey, k -> new ArrayList<>()).add(student);
        }

        log.info("📋 Identified {} grade-level cohorts", gradeCohorts.size());

        // Convert to LunchCohort objects and sort by size (largest first)
        List<com.heronix.scheduler.model.dto.LunchCohort> cohorts = gradeCohorts.entrySet().stream()
                .map(e -> new com.heronix.scheduler.model.dto.LunchCohort(e.getKey(), e.getValue()))
                .sorted((c1, c2) -> Integer.compare(c2.getSize(), c1.getSize()))
                .collect(Collectors.toList());

        // Use bin packing to assign cohorts to waves
        List<com.heronix.scheduler.model.domain.LunchWave> waves =
                assignCohortsToWaves(cohorts, waveCount, students.size());

        // Log results
        logWaveAssignmentResults(waves);

        return waves;
    }

    /**
     * Assign students to lunch waves based on their class cohorts
     * Phase 5C: Course-based cohort assignment (used when course schedule IS available)
     *
     * Strategy:
     * 1. Group students by their scheduled class during lunch period
     * 2. Use bin packing algorithm to assign cohorts to waves
     * 3. Balance wave sizes while keeping cohorts intact
     * 4. Prefer spatial grouping (adjacent rooms in same wave)
     *
     * @param students    All students who need lunch
     * @param waveCount   Number of lunch waves
     * @param courseSlots All course schedule slots (to determine what class students have)
     * @param lunchTime   Start time of lunch period
     * @return List of lunch waves with assigned cohorts
     */
    private List<com.heronix.scheduler.model.domain.LunchWave> assignLunchWavesByCohort(
            List<Student> students,
            int waveCount,
            List<ScheduleSlot> courseSlots,
            LocalTime lunchTime) {

        log.info("🎯 Starting cohort-based lunch wave assignment for {} students into {} waves",
                 students.size(), waveCount);

        // Step 1: Group students by their course cohorts
        Map<String, List<Student>> cohortMap = groupStudentsByCourse(
                students, courseSlots, lunchTime);

        log.info("📋 Identified {} cohorts from course schedules", cohortMap.size());

        // Step 2: Convert to LunchCohort objects and sort by size (largest first)
        List<com.heronix.scheduler.model.dto.LunchCohort> cohorts = cohortMap.entrySet().stream()
                .map(e -> new com.heronix.scheduler.model.dto.LunchCohort(e.getKey(), e.getValue()))
                .sorted((c1, c2) -> Integer.compare(c2.getSize(), c1.getSize()))
                .collect(Collectors.toList());

        // Step 3: Use bin packing to assign cohorts to waves
        List<com.heronix.scheduler.model.domain.LunchWave> waves = assignCohortsToWaves(cohorts, waveCount, students.size());

        // Step 4: Log results
        logWaveAssignmentResults(waves);

        return waves;
    }

    /**
     * Group students by their scheduled course during lunch period
     *
     * For students who have a class during lunch time:
     * - Cohort = "Room [room] - [course name]"
     *
     * For students with no class (free period, study hall):
     * - Cohort = "Grade [X] - Free Period"
     */
    private Map<String, List<Student>> groupStudentsByCourse(
            List<Student> students,
            List<ScheduleSlot> courseSlots,
            LocalTime lunchTime) {

        Map<String, List<Student>> cohorts = new HashMap<>();

        // ✅ NULL SAFE: Filter null students before processing
        for (Student student : students) {
            if (student == null) continue;

            // Find what class (if any) this student is scheduled for during lunch
            ScheduleSlot studentClass = findStudentClassNearTime(
                    student, courseSlots, lunchTime);

            String cohortKey;
            if (studentClass != null &&
                studentClass.getCourse() != null &&
                studentClass.getRoom() != null) {
                // Student has a class - cohort by room and course
                cohortKey = String.format("Room %s - %s",
                        studentClass.getRoom().getRoomNumber(),
                        studentClass.getCourse().getCourseName());
            } else {
                // No class during lunch - cohort by grade level
                String gradeLevel = student.getGradeLevel();
                if (gradeLevel != null) {
                    cohortKey = String.format("Grade %s - Free Period", gradeLevel);
                } else {
                    cohortKey = "Unassigned Students";
                }
            }

            cohorts.computeIfAbsent(cohortKey, k -> new ArrayList<>()).add(student);
        }

        return cohorts;
    }

    /**
     * Find the class a student is scheduled for near the lunch time
     * Looks for classes scheduled within 2 hours of lunch start
     */
    private ScheduleSlot findStudentClassNearTime(
            Student student,
            List<ScheduleSlot> courseSlots,
            LocalTime targetTime) {

        if (courseSlots == null || student == null) {
            return null;
        }

        // Find slots that contain this student and are near the target time
        for (ScheduleSlot slot : courseSlots) {
            if (slot.getStudents() != null &&
                slot.getStudents().contains(student) &&
                slot.getStartTime() != null) {

                // Check if this slot is within 2 hours of lunch time
                long hoursDiff = Math.abs(
                        java.time.Duration.between(slot.getStartTime(), targetTime).toHours());

                if (hoursDiff <= 2) {
                    return slot;
                }
            }
        }

        return null;
    }

    /**
     * Assign cohorts to waves using First Fit Decreasing bin packing algorithm
     *
     * Goal: Balance wave sizes while keeping cohorts together
     */
    private List<com.heronix.scheduler.model.domain.LunchWave> assignCohortsToWaves(
            List<com.heronix.scheduler.model.dto.LunchCohort> sortedCohorts,
            int waveCount,
            int totalStudents) {

        // Initialize empty waves
        List<com.heronix.scheduler.model.domain.LunchWave> waves = new ArrayList<>();
        for (int i = 0; i < waveCount; i++) {
            com.heronix.scheduler.model.domain.LunchWave wave = com.heronix.scheduler.model.domain.LunchWave.builder()
                .waveOrder(i + 1)
                .waveName("Wave " + (i + 1))
                .build();
            waves.add(wave);
        }

        // Target size per wave (for balance checking)
        int targetPerWave = (int) Math.ceil((double) totalStudents / waveCount);

        log.info("🎯 Target students per wave: {} (Total: {} students in {} waves)",
                 targetPerWave, totalStudents, waveCount);

        // Assign each cohort to the wave with most available space
        // ✅ NULL SAFE: Filter null cohorts before processing
        for (com.heronix.scheduler.model.dto.LunchCohort cohort : sortedCohorts) {
            if (cohort == null) continue;

            // Find wave with smallest current size (best fit)
            com.heronix.scheduler.model.domain.LunchWave bestWave = waves.stream()
                    .filter(w -> w != null)
                    .min(Comparator.comparing(com.heronix.scheduler.model.domain.LunchWave::getCurrentSize))
                    .orElse(waves.get(0));

            bestWave.addCohort(cohort);

            log.debug("  ├─ Assigned '{}' ({} students) to Wave {} (now {} students)",
                    cohort.getName(), cohort.getSize(),
                    bestWave.getNumber(), bestWave.getCurrentSize());
        }

        return waves;
    }

    /**
     * Log detailed wave assignment results
     */
    private void logWaveAssignmentResults(List<com.heronix.scheduler.model.domain.LunchWave> waves) {
        log.info("📊 ========== LUNCH WAVE ASSIGNMENT RESULTS ==========");

        // ✅ NULL SAFE: Filter null waves before processing
        for (com.heronix.scheduler.model.domain.LunchWave wave : waves) {
            if (wave == null) continue;

            log.info("📍 {}", wave.toString());
            log.info("   Cohorts: {}", wave.getCohortCount());
            log.info("   Spatial Cohesion: {:.1f}%", wave.getSpatialCohesionScore());

            // Log top 3 cohorts in this wave
            // ✅ NULL SAFE: Check cohorts list exists before streaming
            if (wave.getCohorts() == null) continue;

            List<com.heronix.scheduler.model.dto.LunchCohort> topCohorts = wave.getCohorts().stream()
                    .filter(c -> c != null)
                    .limit(3)
                    .collect(Collectors.toList());

            for (com.heronix.scheduler.model.dto.LunchCohort cohort : topCohorts) {
                log.info("      ├─ {} ({} students)", cohort.getName(), cohort.getSize());
            }

            if (wave.getCohortCount() > 3) {
                log.info("      └─ ... and {} more cohorts",
                        wave.getCohortCount() - 3);
            }
        }

        // Calculate balance score
        double avgSize = waves.stream()
                .mapToInt(com.heronix.scheduler.model.domain.LunchWave::getCurrentSize)
                .average()
                .orElse(0);

        double maxDeviation = waves.stream()
                .mapToDouble(w -> Math.abs(w.getCurrentSize() - avgSize))
                .max()
                .orElse(0);

        double balanceScore = avgSize > 0 ? (1 - (maxDeviation / avgSize)) * 100 : 0;

        log.info("✅ Balance Score: {:.1f}% (avg: {:.0f} students, max deviation: {:.0f})",
                 balanceScore, avgSize, maxDeviation);

        log.info("====================================================");
    }

    // ========================================================================
    // SOLVER MONITORING
    // ========================================================================

    /**
     * Monitor solver progress and report updates via callback
     * 
     * @param solverJob        Active solver job
     * @param progressCallback Progress reporting callback
     * @param startProgress    Starting progress percentage
     * @param endProgress      Ending progress percentage
     * @throws InterruptedException if thread is interrupted
     */
    private void monitorSolverProgress(SolverJob<SchedulingSolution, UUID> solverJob,
            BiConsumer<Integer, String> progressCallback,
            int startProgress,
            int endProgress) throws InterruptedException {

        long startTime = System.currentTimeMillis();
        int lastProgress = startProgress;

        while (solverJob.getSolverStatus() == org.optaplanner.core.api.solver.SolverStatus.SOLVING_ACTIVE) {
            Thread.sleep(1000); // Check every second

            long elapsed = (System.currentTimeMillis() - startTime) / 1000;

            // Estimate progress based on elapsed time (assumes 120 second max)
            int currentProgress = startProgress +
                    (int) ((elapsed / 120.0) * (endProgress - startProgress));
            currentProgress = Math.min(currentProgress, endProgress);

            if (currentProgress > lastProgress) {
                updateProgress(progressCallback, currentProgress,
                        String.format("Optimizing... (%d seconds elapsed)", elapsed));
                lastProgress = currentProgress;

                // Log every 10 seconds
                if (elapsed % 10 == 0) {
                    log.info("Solver running: {} seconds elapsed", elapsed);
                }
            }
        }
    }

    // ========================================================================
    // SOLUTION VALIDATION
    // ========================================================================

    /**
     * Validate that solver produced a valid solution
     *
     * @param solution Solver output
     * @throws IllegalStateException if solution is invalid
     */
    private void validateSolution(SchedulingSolution solution) {
        validateSolution(solution, false);
    }

    /**
     * Validate solution with option to allow partial schedules
     *
     * @param solution       Solver output
     * @param allowPartial   If true, allows solutions with hard constraint violations
     * @throws IllegalStateException if solution is invalid
     */
    private void validateSolution(SchedulingSolution solution, boolean allowPartial) {
        if (solution == null) {
            throw new IllegalStateException("Solver returned null solution");
        }

        if (solution.getScheduleSlots() == null || solution.getScheduleSlots().isEmpty()) {
            throw new IllegalStateException("Solution contains no schedule slots");
        }

        if (solution.getScore() == null) {
            log.warn("Solution has null score - using unscored solution");
        } else if (solution.getScore().hardScore() < 0) {
            int violations = Math.abs(solution.getScore().hardScore());

            if (allowPartial) {
                // Log warning but continue with partial schedule
                log.warn("Solution has {} hard constraint violations - proceeding with partial schedule", violations);
                log.warn("Partial schedule will require manual review and conflict resolution");
            } else {
                // Original strict behavior
                log.error("Solution has {} hard constraint violations - cannot save invalid schedule", violations);
                throw new IllegalStateException(
                    String.format("Schedule generation failed: %d hard constraint violations detected. " +
                                 "This indicates conflicts that cannot be resolved (e.g., teacher double-booked, " +
                                 "room capacity exceeded). Please review your constraints or add more resources.",
                                 violations));
            }
        }

        log.debug("Solution validated: {} slots, score: {}",
                solution.getScheduleSlots().size(), solution.getScore());
    }

    // ========================================================================
    // SAVE OPTIMIZED RESULTS
    // ========================================================================

    /**
     * Save the optimized schedule slots to database
     * Syncs OptaPlanner planning variables to database fields
     * 
     * @param schedule Parent schedule entity
     * @param solution Optimized solution from solver
     */
    @Transactional
    private void saveOptimizedSchedule(Schedule schedule, SchedulingSolution solution) {
        List<ScheduleSlot> optimizedSlots = solution.getScheduleSlots();
        int assignedCount = 0;

        for (ScheduleSlot slot : optimizedSlots) {
            // Sync TimeSlot planning variable to database fields
            if (slot.getTimeSlot() != null) {
                slot.syncWithTimeSlot();
            }

            // Update status based on assignment
            if (slot.isAssigned()) {
                slot.setStatus(SlotStatus.ACTIVE);
                assignedCount++;
            } else {
                slot.setStatus(SlotStatus.BLOCKED);
            }

            // Save to database
            scheduleSlotRepository.save(slot);
        }

        double assignmentRate = (double) assignedCount / optimizedSlots.size() * 100;
        log.info("✓ Saved {} schedule slots to database ({} assigned, {:.1f}% assignment rate)",
                optimizedSlots.size(), assignedCount, assignmentRate);
    }

    // ========================================================================
    // METRICS CALCULATION
    // ========================================================================

    /**
     * Calculate schedule quality metrics
     * Updates schedule entity with optimization score and conflict count
     * 
     * @param schedule Schedule to update
     * @param solution Optimized solution
     */
    private void calculateScheduleMetrics(Schedule schedule, SchedulingSolution solution) {
        // Calculate optimization score from hard/soft scores
        if (solution.getScore() != null) {
            double hardScore = solution.getScore().hardScore();
            double softScore = solution.getScore().softScore();

            // If hard score is 0 (no violations), base quality on soft score
            // Otherwise, quality is 0% (has hard constraint violations)
            double optimizationScore;
            if (hardScore == 0) {
                // Map soft score to 0-100% range
                // Soft score is typically negative, so we add 100 and clamp
                optimizationScore = Math.max(0, Math.min(100, 50 + (softScore / 10.0)));
            } else {
                optimizationScore = 0.0;
            }

            schedule.setOptimizationScore(optimizationScore / 100.0); // Store as 0.0-1.0

            log.debug("Optimization score calculated: {:.1f}% (hard: {}, soft: {})",
                    optimizationScore, hardScore, softScore);
        }

        // Count conflicts
        List<ScheduleSlot> slots = solution.getScheduleSlots();
        int conflictCount = (int) slots.stream()
                .filter(slot -> Boolean.TRUE.equals(slot.getHasConflict()))
                .count();
        schedule.setTotalConflicts(conflictCount);

        log.info("Schedule metrics: Score={:.1f}%, Conflicts={}",
                schedule.getOptimizationScore() * 100, conflictCount);
    }

    // ========================================================================
    // UTILITY METHODS
    // ========================================================================

    /**
     * Update progress via callback
     * 
     * @param callback Progress callback (can be null)
     * @param progress Progress percentage (0-100)
     * @param message  Progress message
     */
    private void updateProgress(BiConsumer<Integer, String> callback,
            int progress,
            String message) {
        if (callback != null) {
            callback.accept(progress, message);
        }
        log.debug("Progress: {}% - {}", progress, message);
    }

    /**
     * Get value or default if null
     *
     * @param value        Potentially null value
     * @param defaultValue Default to use if null
     * @return value if not null, otherwise defaultValue
     */
    private int getValueOrDefault(Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    // ========================================================================
    // USER-FRIENDLY ERROR MESSAGES
    // ========================================================================

    /**
     * Build user-friendly error message from exception and diagnostic report
     * Translates technical errors into actionable guidance for administrators
     *
     * @param exception Original exception
     * @param report Diagnostic report
     * @return User-friendly error message
     */
    private String buildUserFriendlyErrorMessage(Exception exception,
                                                  com.heronix.scheduler.model.dto.ScheduleDiagnosticReport report) {
        StringBuilder message = new StringBuilder();

        // Check if it's a constraint violation error
        if (exception.getMessage() != null &&
            exception.getMessage().contains("hard constraint violations")) {

            message.append("Schedule Generation Failed - Data Issues Detected\n\n");

            if (report.getCriticalIssuesCount() > 0) {
                message.append("We found ").append(report.getCriticalIssuesCount())
                       .append(" critical issue(s) that prevent schedule generation:\n\n");

                int count = 1;
                for (com.heronix.scheduler.model.dto.ScheduleDiagnosticReport.DiagnosticIssue issue : report.getIssues()) {
                    if (issue.getSeverity() ==
                        com.heronix.scheduler.model.dto.ScheduleDiagnosticReport.IssueSeverity.CRITICAL) {
                        message.append(count++).append(". ").append(issue.getTitle()).append("\n");
                        message.append("   Problem: ").append(issue.getDescription()).append("\n");
                        message.append("   How to fix: ").append(issue.getHowToFix()).append("\n\n");
                    }
                }

                message.append("Estimated time to fix: ")
                       .append(report.getEstimatedFixTimeMinutes())
                       .append(" minutes\n\n");

                message.append("Click the 'View Diagnostic Report' button for detailed information.");

            } else {
                // Constraint violations but diagnostic didn't find obvious issues
                message.append("The schedule generator encountered constraints that couldn't be satisfied.\n\n");
                message.append("Common causes:\n");
                message.append("• Courses without teacher assignments\n");
                message.append("• PE courses without gymnasium rooms\n");
                message.append("• Music courses without auditorium/music rooms\n");
                message.append("• Lab courses without enough lab rooms\n");
                message.append("• Rooms too small for enrolled students\n\n");
                message.append("Please click 'View Diagnostic Report' for a detailed analysis.");
            }

        } else {
            // Other types of errors
            message.append("Schedule Generation Failed\n\n");
            message.append("Error: ").append(exception.getMessage()).append("\n\n");

            if (report.getCriticalIssuesCount() > 0) {
                message.append("However, we did find some data issues that may be related:\n\n");
                for (com.heronix.scheduler.model.dto.ScheduleDiagnosticReport.DiagnosticIssue issue : report.getIssues()) {
                    if (issue.getSeverity() ==
                        com.heronix.scheduler.model.dto.ScheduleDiagnosticReport.IssueSeverity.CRITICAL) {
                        message.append("• ").append(issue.getTitle()).append("\n");
                    }
                }
                message.append("\nClick 'View Diagnostic Report' for more details.");
            }
        }

        return message.toString();
    }
}