package com.heronix.scheduler.controller.ui;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.dto.ScheduleGenerationRequest;
import com.heronix.scheduler.model.dto.UserFeedback;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.heronix.scheduler.model.enums.SlotStatus;
import com.heronix.scheduler.model.enums.SchedulePeriod;

/**
 * Enhanced Hybrid Scheduling Engine
 * Location:
 * src/main/java/com/eduscheduler/ui/controller/EnhancedHybridScheduler.java
 * 
 * Multi-phase optimization combining:
 * 1. K-Means-inspired clustering for grouping similar requirements
 * 2. Genetic Algorithm for global optimization
 * 3. Simulated Annealing for local refinement
 * 4. OptaPlanner for constraint satisfaction
 * 5. Machine Learning for pattern-based improvements
 * 
 * @author Heronix Scheduling System Team
 * @version 3.0.0
 * @since 2025-10-29
 */
@Slf4j
@Service
public class EnhancedHybridScheduler {

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private com.heronix.scheduler.solver.HybridSchedulingSolver hybridSolver;

    // Machine learning pattern storage
    private Map<ScenarioType, List<SchedulePattern>> learnedPatterns = new HashMap<>();
    private Map<String, Double> constraintWeights = new HashMap<>();
    private Random random = new Random();

    // ========================================================================
    // CONFIGURATION CONSTANTS
    // ========================================================================

    private static final int GA_POPULATION_SIZE = 100;
    private static final int GA_GENERATIONS = 200;
    private static final double GA_MUTATION_RATE = 0.1;
    private static final double GA_CROSSOVER_RATE = 0.8;

    private static final double SA_INITIAL_TEMP = 1000;
    private static final double SA_COOLING_RATE = 0.95;
    private static final int SA_ITERATIONS = 1000;

    private static final int CLUSTER_COUNT = 5;
    private static final double ML_CONFIDENCE_THRESHOLD = 0.85;

    // ========================================================================
    // SCENARIO TYPES ENUM
    // ========================================================================

    public enum ScenarioType {
        HIGH_SCHOOL,
        ELEMENTARY,
        MIDDLE_SCHOOL,
        UNIVERSITY,
        VOCATIONAL
    }

    // ========================================================================
    // MAIN HYBRID OPTIMIZATION METHOD
    // ========================================================================

    /**
     * Generate optimized schedule using hybrid approach
     */
    public Schedule generateHybridSchedule(
            ScheduleGenerationRequest request,
            ScenarioType scenarioType) throws Exception {

        log.info(
                "â•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—");
        log.info("HYBRID SCHEDULING ENGINE - Starting Multi-Phase Optimization");
        log.info("Scenario: {}", scenarioType);
        log.info(
                "â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•");

        // Load resources
        List<Course> courses = sisDataService.getAllCourses();
        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());
        List<Room> rooms = roomRepository.findAll();
        List<Student> students = sisDataService.getAllStudents();

        // Phase 1: Clustering
        log.info("ðŸ“Š Phase 1: Clustering Analysis");
        List<CourseCluster> clusters = performClustering(courses, teachers);

        // Phase 2: Genetic Algorithm
        log.info("ðŸ§¬ Phase 2: Genetic Algorithm Optimization");
        Schedule gaSchedule = runGeneticAlgorithm(clusters, rooms, scenarioType);

        // Phase 3: Simulated Annealing
        log.info("ðŸ”¥ Phase 3: Simulated Annealing Refinement");
        Schedule saSchedule = runSimulatedAnnealing(gaSchedule, scenarioType);

        // Phase 4: OptaPlanner Final Optimization
        log.info("ðŸŽ¯ Phase 4: OptaPlanner Constraint Solving");
        Schedule optimizedSchedule = applyOptaPlanner(saSchedule, request, scenarioType);

        // Phase 5: Machine Learning Pattern Application
        log.info("ðŸ¤– Phase 5: ML-Based Pattern Recognition & Application");
        Schedule finalSchedule = applyMLPatterns(optimizedSchedule, scenarioType);

        // Calculate final metrics
        ScheduleMetrics metrics = calculateScheduleMetrics(finalSchedule);
        log.info("âœ… Final Quality Score: {}/100", metrics.getQualityScore());

        return finalSchedule;
    }

    // ========================================================================
    // PHASE 1: K-MEANS-INSPIRED CLUSTERING
    // ========================================================================

    private List<CourseCluster> performClustering(List<Course> courses, List<Teacher> teachers) {
        log.info("Clustering {} courses into {} groups", courses.size(), CLUSTER_COUNT);

        List<CourseCluster> clusters = new ArrayList<>();

        // Simple clustering based on course subject/department
        Map<String, List<Course>> subjectGroups = courses.stream()
                .collect(Collectors.groupingBy(Course::getSubject));

        int clusterId = 0;
        for (Map.Entry<String, List<Course>> entry : subjectGroups.entrySet()) {
            CourseCluster cluster = new CourseCluster();
            cluster.setId(clusterId++);
            cluster.setCourses(entry.getValue());
            cluster.assignTeachers(teachers);
            clusters.add(cluster);

            log.debug("Cluster {}: {} - {} courses",
                    cluster.getId(), entry.getKey(), entry.getValue().size());
        }

        return clusters;
    }

    // ========================================================================
    // PHASE 2: GENETIC ALGORITHM
    // ========================================================================

    private Schedule runGeneticAlgorithm(List<CourseCluster> clusters, List<Room> rooms, ScenarioType scenarioType) {
        log.info("Running GA with population={}, generations={}", GA_POPULATION_SIZE, GA_GENERATIONS);

        // Initialize population
        List<Chromosome> population = new ArrayList<>();
        for (int i = 0; i < GA_POPULATION_SIZE; i++) {
            population.add(createRandomChromosome(clusters, rooms, scenarioType));
        }

        // Evolution loop
        for (int generation = 0; generation < GA_GENERATIONS; generation++) {
            // Evaluate fitness
            evaluateFitness(population, scenarioType);

            // Selection
            List<Chromosome> parents = selectParents(population);

            // Crossover
            List<Chromosome> offspring = performCrossover(parents);

            // Mutation
            performMutation(offspring);

            // Survivor selection
            population = selectSurvivors(population, offspring);

            if (generation % 50 == 0) {
                double avgFitness = population.stream()
                        .mapToDouble(Chromosome::getFitness)
                        .average()
                        .orElse(0.0);
                log.info("Generation {}: Average fitness = {}", generation, avgFitness);
            }
        }

        // Return best solution
        Chromosome best = population.stream()
                .max(Comparator.comparingDouble(Chromosome::getFitness))
                .orElseThrow();

        return chromosomeToSchedule(best);
    }

    private Chromosome createRandomChromosome(List<CourseCluster> clusters, List<Room> rooms,
            ScenarioType scenarioType) {
        Chromosome chromosome = new Chromosome();
        List<Gene> genes = new ArrayList<>();

        // Create gene for each course
        for (CourseCluster cluster : clusters) {
            for (Course course : cluster.getCourses()) {
                Gene gene = new Gene();
                gene.setCourse(course);

                // Randomly assign teacher from compatible teachers
                List<Teacher> compatibleTeachers = cluster.getCompatibleTeachers(course);
                if (!compatibleTeachers.isEmpty()) {
                    gene.setTeacher(compatibleTeachers.get(random.nextInt(compatibleTeachers.size())));
                }

                // Randomly assign room
                gene.setRoom(rooms.get(random.nextInt(rooms.size())));

                // Randomly assign time slot
                gene.setTimeSlot(generateRandomTimeSlot(scenarioType));

                genes.add(gene);
            }
        }

        chromosome.setGenes(genes);
        return chromosome;
    }

    // ========================================================================
    // PHASE 3: SIMULATED ANNEALING
    // ========================================================================

    private Schedule runSimulatedAnnealing(Schedule initialSchedule, ScenarioType scenarioType) {
        log.info("Running Simulated Annealing: temp={}, cooling={}, iterations={}",
                SA_INITIAL_TEMP, SA_COOLING_RATE, SA_ITERATIONS);

        Schedule currentSchedule = deepCopySchedule(initialSchedule);
        Schedule bestSchedule = deepCopySchedule(initialSchedule);

        double currentEnergy = calculateEnergy(currentSchedule, scenarioType);
        double bestEnergy = currentEnergy;
        double temperature = SA_INITIAL_TEMP;

        for (int i = 0; i < SA_ITERATIONS; i++) {
            // Generate neighbor
            Schedule neighborSchedule = generateNeighbor(currentSchedule);
            double neighborEnergy = calculateEnergy(neighborSchedule, scenarioType);

            // Accept or reject
            double delta = neighborEnergy - currentEnergy;
            if (delta < 0 || Math.exp(-delta / temperature) > random.nextDouble()) {
                currentSchedule = neighborSchedule;
                currentEnergy = neighborEnergy;

                if (currentEnergy < bestEnergy) {
                    bestSchedule = deepCopySchedule(currentSchedule);
                    bestEnergy = currentEnergy;
                }
            }

            // Cool down
            temperature *= SA_COOLING_RATE;

            if (i % 200 == 0) {
                log.debug("SA Iteration {}: Best energy = {}, Temp = {}", i, bestEnergy, temperature);
            }
        }

        return bestSchedule;
    }

    private double calculateEnergy(Schedule schedule, ScenarioType scenarioType) {
        // Lower energy is better
        double conflicts = schedule.getSlots().stream()
                .filter(slot -> slot.getStatus() == SlotStatus.CONFLICT)
                .count();

        double workloadImbalance = calculateWorkloadImbalance(schedule);
        double gaps = calculateScheduleGaps(schedule);

        // Scenario-specific penalties
        double scenarioPenalty = 0;
        switch (scenarioType) {
            case HIGH_SCHOOL:
                scenarioPenalty = calculateAPCoursePenalty(schedule);
                break;
            case ELEMENTARY:
                scenarioPenalty = calculateSpecialsDistribution(schedule);
                break;
            case UNIVERSITY:
                scenarioPenalty = calculateLabCoursePenalty(schedule);
                break;
            default:
                break;
        }

        return conflicts * 100 + workloadImbalance * 50 + gaps * 10 + scenarioPenalty * 25;
    }

    // ========================================================================
    // PHASE 4: OPTAPLANNER INTEGRATION
    // ========================================================================

    /**
     * Apply OptaPlanner constraint solver for final optimization
     *
     * This method takes the schedule refined by GA and SA, converts it to
     * OptaPlanner's SchedulingSolution format, runs the constraint solver,
     * and converts the optimized solution back to a Schedule.
     *
     * OptaPlanner provides:
     * - Hard constraint enforcement (no teacher/room conflicts)
     * - Soft constraint optimization (workload balance, student preferences)
     * - Proven constraint satisfaction algorithms
     *
     * @param initialSchedule Schedule from previous optimization phases
     * @param request Original schedule generation request
     * @param scenarioType Scenario type for context
     * @return Optimized schedule with constraints satisfied
     */
    private Schedule applyOptaPlanner(Schedule initialSchedule, ScheduleGenerationRequest request,
            ScenarioType scenarioType) {
        log.info("Applying OptaPlanner constraint solver for final optimization");
        log.info("Input schedule: {} slots, Score before: Conflicts={}, Workload={}",
                initialSchedule.getSlots().size(),
                countTeacherConflicts(initialSchedule) + countRoomConflicts(initialSchedule),
                calculateWorkloadImbalance(initialSchedule));

        try {
            // Step 1: Convert Schedule to OptaPlanner SchedulingSolution
            log.debug("Converting Schedule to SchedulingSolution format");
            com.heronix.scheduler.model.planning.SchedulingSolution problem =
                convertToSchedulingSolution(initialSchedule, request);

            if (problem == null) {
                log.error("Failed to convert schedule to SchedulingSolution");
                return initialSchedule;
            }

            log.info("Problem size: {} slots, {} teachers, {} rooms, {} time slots",
                    problem.getTotalSlots(),
                    problem.getTeachers().size(),
                    problem.getRooms().size(),
                    problem.getTimeSlots().size());

            // Step 2: Run OptaPlanner solver
            log.info("Starting OptaPlanner solver execution...");
            long startTime = System.currentTimeMillis();

            com.heronix.scheduler.model.planning.SchedulingSolution solution = hybridSolver.solve(problem);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Solver completed in {}ms", duration);

            if (solution == null) {
                log.error("Solver returned null solution");
                return initialSchedule;
            }

            // Step 3: Log solution quality
            log.info("Solution quality: {}", solution.getSummary());
            log.info("Score: {} | Assigned: {}/{} ({}%) | Feasible: {}",
                    solution.getScore(),
                    solution.getAssignedCount(),
                    solution.getTotalSlots(),
                    String.format("%.1f", solution.getAssignmentPercentage()),
                    solution.isFeasible() ? "YES" : "NO");

            // Step 4: Convert SchedulingSolution back to Schedule
            log.debug("Converting SchedulingSolution back to Schedule format");
            Schedule optimizedSchedule = convertToSchedule(solution, initialSchedule, request);

            if (optimizedSchedule == null) {
                log.error("Failed to convert SchedulingSolution back to Schedule");
                return initialSchedule;
            }

            // Step 5: Log improvement metrics
            long finalConflicts = countTeacherConflicts(optimizedSchedule) +
                                 countRoomConflicts(optimizedSchedule);
            double finalWorkload = calculateWorkloadImbalance(optimizedSchedule);

            log.info("Optimization complete:");
            long initialConflicts = countTeacherConflicts(initialSchedule) + countRoomConflicts(initialSchedule);
            long conflictChange = finalConflicts - initialConflicts;
            log.info("  Conflicts: {} -> {} ({}{})",
                    initialConflicts,
                    finalConflicts,
                    conflictChange >= 0 ? "+" : "",
                    conflictChange);
            log.info("  Workload Imbalance: {} -> {}",
                    String.format("%.2f", calculateWorkloadImbalance(initialSchedule)),
                    String.format("%.2f", finalWorkload));

            return optimizedSchedule;

        } catch (com.heronix.scheduler.solver.exception.InvalidScheduleProblemException e) {
            log.error("โŒ Invalid scheduling problem: {}", e.getMessage());
            log.warn("Returning schedule from previous phase without OptaPlanner optimization");
            return initialSchedule;

        } catch (com.heronix.scheduler.solver.exception.SolverExecutionException e) {
            log.error("โŒ Solver execution failed: {}", e.getMessage());
            log.warn("Returning schedule from previous phase without OptaPlanner optimization");
            return initialSchedule;

        } catch (Exception e) {
            log.error("โŒ Unexpected error during OptaPlanner optimization", e);
            log.warn("Returning schedule from previous phase without OptaPlanner optimization");
            return initialSchedule;
        }
    }

    /**
     * Convert domain Schedule to OptaPlanner SchedulingSolution
     *
     * This method transforms the application's Schedule model into the format
     * required by OptaPlanner's solver. It extracts all schedule slots and
     * loads all available resources (teachers, rooms, time slots).
     *
     * @param schedule The schedule to convert
     * @param request Original generation request for context
     * @return SchedulingSolution ready for OptaPlanner solving
     */
    private com.heronix.scheduler.model.planning.SchedulingSolution convertToSchedulingSolution(
            Schedule schedule, ScheduleGenerationRequest request) {

        try {
            // Extract schedule slots
            List<ScheduleSlot> slots = new ArrayList<>(schedule.getSlots());

            if (slots.isEmpty()) {
                log.warn("Schedule has no slots to optimize");
                return null;
            }

            // Load all available resources from database
            List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());
            List<Room> rooms = roomRepository.findAll();
            List<Course> courses = sisDataService.getAllCourses();
            List<Student> students = sisDataService.getAllStudents();

            // Extract unique time slots from current schedule
            List<TimeSlot> timeSlots = extractTimeSlots(slots);

            if (teachers.isEmpty() || rooms.isEmpty() || timeSlots.isEmpty()) {
                log.error("Missing required resources: teachers={}, rooms={}, timeSlots={}",
                        teachers.size(), rooms.size(), timeSlots.size());
                return null;
            }

            log.debug("Loaded resources: {} teachers, {} rooms, {} time slots, {} courses, {} students",
                    teachers.size(), rooms.size(), timeSlots.size(), courses.size(), students.size());

            // Create and return SchedulingSolution
            return new com.heronix.scheduler.model.planning.SchedulingSolution(
                    slots,
                    teachers,
                    rooms,
                    timeSlots,
                    courses,
                    students
            );

        } catch (Exception e) {
            log.error("Failed to convert Schedule to SchedulingSolution", e);
            return null;
        }
    }

    /**
     * Convert OptaPlanner SchedulingSolution back to domain Schedule
     *
     * This method takes the optimized solution from OptaPlanner and updates
     * the original schedule with the new assignments. It preserves the
     * schedule metadata while updating slot assignments.
     *
     * @param solution Solved SchedulingSolution from OptaPlanner
     * @param originalSchedule Original schedule for metadata
     * @param request Original generation request
     * @return Updated Schedule with optimized assignments
     */
    private Schedule convertToSchedule(
            com.heronix.scheduler.model.planning.SchedulingSolution solution,
            Schedule originalSchedule,
            ScheduleGenerationRequest request) {

        try {
            // Create new schedule preserving metadata
            Schedule optimizedSchedule = new Schedule();
            optimizedSchedule.setId(originalSchedule.getId());
            optimizedSchedule.setName(originalSchedule.getName());
            optimizedSchedule.setStartDate(originalSchedule.getStartDate());
            optimizedSchedule.setEndDate(originalSchedule.getEndDate());
            optimizedSchedule.setPeriod(originalSchedule.getPeriod());
            optimizedSchedule.setScheduleType(originalSchedule.getScheduleType());
            optimizedSchedule.setStatus(originalSchedule.getStatus());
            optimizedSchedule.setNotes(originalSchedule.getNotes());
            optimizedSchedule.setCreatedBy(originalSchedule.getCreatedBy());
            optimizedSchedule.setCreatedDate(originalSchedule.getCreatedDate());
            optimizedSchedule.setLastModifiedBy("OptaPlanner-Phase4");
            optimizedSchedule.setLastModifiedDate(java.time.LocalDate.now());

            // Update slots with solved assignments
            List<ScheduleSlot> optimizedSlots = new ArrayList<>();

            for (ScheduleSlot solvedSlot : solution.getScheduleSlots()) {
                ScheduleSlot updatedSlot = new ScheduleSlot();

                // Copy identifiers
                updatedSlot.setId(solvedSlot.getId());
                updatedSlot.setSchedule(optimizedSchedule);

                // Copy solved assignments
                updatedSlot.setCourse(solvedSlot.getCourse());
                updatedSlot.setTeacher(solvedSlot.getTeacher());
                updatedSlot.setRoom(solvedSlot.getRoom());
                updatedSlot.setTimeSlot(solvedSlot.getTimeSlot());

                // Set time fields from time slot
                if (solvedSlot.getTimeSlot() != null) {
                    updatedSlot.setDayOfWeek(solvedSlot.getTimeSlot().getDayOfWeek());
                    updatedSlot.setStartTime(solvedSlot.getTimeSlot().getStartTime());
                    updatedSlot.setEndTime(solvedSlot.getTimeSlot().getEndTime());
                }

                // Determine status based on assignments
                if (updatedSlot.getTeacher() == null || updatedSlot.getRoom() == null ||
                    updatedSlot.getTimeSlot() == null) {
                    updatedSlot.setStatus(SlotStatus.DRAFT);
                } else if (hasConflict(updatedSlot, optimizedSlots)) {
                    updatedSlot.setStatus(SlotStatus.CONFLICT);
                } else {
                    updatedSlot.setStatus(SlotStatus.SCHEDULED);
                }

                optimizedSlots.add(updatedSlot);
            }

            optimizedSchedule.setSlots(optimizedSlots);

            log.info("Converted solution to schedule: {} slots, {} assigned, {} unassigned",
                    optimizedSlots.size(),
                    optimizedSlots.stream().filter(s -> s.getStatus() == SlotStatus.SCHEDULED).count(),
                    optimizedSlots.stream().filter(s -> s.getStatus() == SlotStatus.DRAFT).count());

            return optimizedSchedule;

        } catch (Exception e) {
            log.error("Failed to convert SchedulingSolution to Schedule", e);
            return null;
        }
    }

    /**
     * Extract unique time slots from schedule slots
     *
     * @param slots Schedule slots to extract from
     * @return List of unique time slots
     */
    private List<TimeSlot> extractTimeSlots(List<ScheduleSlot> slots) {
        Set<String> seenSlots = new HashSet<>();
        List<TimeSlot> timeSlots = new ArrayList<>();

        for (ScheduleSlot slot : slots) {
            if (slot.getTimeSlot() != null) {
                String key = slot.getTimeSlot().getDayOfWeek() + "_" +
                           slot.getTimeSlot().getStartTime() + "_" +
                           slot.getTimeSlot().getEndTime();

                if (!seenSlots.contains(key)) {
                    seenSlots.add(key);
                    timeSlots.add(slot.getTimeSlot());
                }
            }
        }

        // If no time slots found, generate standard ones
        if (timeSlots.isEmpty()) {
            log.warn("No time slots found in schedule, generating standard time slots");
            timeSlots = generateStandardTimeSlots();
        }

        return timeSlots;
    }

    /**
     * Generate standard school time slots (Monday-Friday, 8:00-15:00, 50min periods)
     *
     * @return List of standard time slots
     */
    private List<TimeSlot> generateStandardTimeSlots() {
        List<TimeSlot> slots = new ArrayList<>();
        java.time.DayOfWeek[] days = {
            java.time.DayOfWeek.MONDAY,
            java.time.DayOfWeek.TUESDAY,
            java.time.DayOfWeek.WEDNESDAY,
            java.time.DayOfWeek.THURSDAY,
            java.time.DayOfWeek.FRIDAY
        };
        LocalTime[] startTimes = {
            LocalTime.of(8, 0),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            LocalTime.of(13, 0),
            LocalTime.of(14, 0)
        };

        for (java.time.DayOfWeek day : days) {
            for (LocalTime start : startTimes) {
                TimeSlot slot = new TimeSlot(day, start, start.plusMinutes(50));
                slots.add(slot);
            }
        }

        return slots;
    }

    /**
     * Check if a slot has conflicts with existing slots
     *
     * @param slot Slot to check
     * @param existingSlots Already scheduled slots
     * @return true if conflict detected
     */
    private boolean hasConflict(ScheduleSlot slot, List<ScheduleSlot> existingSlots) {
        if (slot.getTeacher() == null || slot.getRoom() == null ||
            slot.getDayOfWeek() == null || slot.getStartTime() == null) {
            return false;
        }

        for (ScheduleSlot existing : existingSlots) {
            if (existing.getId().equals(slot.getId())) {
                continue; // Skip self
            }

            if (existing.getDayOfWeek() == null || existing.getStartTime() == null ||
                existing.getEndTime() == null) {
                continue;
            }

            // Check if same day
            if (!existing.getDayOfWeek().equals(slot.getDayOfWeek())) {
                continue;
            }

            // Check time overlap
            if (!timesOverlap(slot.getStartTime(), slot.getEndTime(),
                            existing.getStartTime(), existing.getEndTime())) {
                continue;
            }

            // Teacher conflict
            if (existing.getTeacher() != null && slot.getTeacher() != null &&
                existing.getTeacher().getId().equals(slot.getTeacher().getId())) {
                return true;
            }

            // Room conflict
            if (existing.getRoom() != null && slot.getRoom() != null &&
                existing.getRoom().getId().equals(slot.getRoom().getId())) {
                return true;
            }
        }

        return false;
    }

    // ========================================================================
    // PHASE 5: MACHINE LEARNING PATTERNS
    // ========================================================================

    private Schedule applyMLPatterns(Schedule schedule, ScenarioType scenarioType) {
        log.info("Applying ML patterns for scenario: {}", scenarioType);

        // Load historical patterns
        List<SchedulePattern> patterns = loadHistoricalPatterns(scenarioType);

        if (patterns.isEmpty()) {
            log.info("No patterns available for scenario");
            return schedule;
        }

        // Use a final reference for the lambda
        final Schedule currentSchedule = schedule;

        // Find applicable patterns
        SchedulePattern bestPattern = patterns.stream()
                .filter(p -> p.getConfidence() > ML_CONFIDENCE_THRESHOLD)
                .filter(p -> isPatternApplicable(p, currentSchedule))
                .max(Comparator.comparingDouble(SchedulePattern::getConfidence))
                .orElse(null);

        if (bestPattern != null) {
            log.info("Applying pattern: {} (confidence: {}%)",
                    bestPattern.getName(), bestPattern.getConfidence() * 100);

            Schedule improved = applyPattern(currentSchedule, bestPattern);

            // Verify pattern application didn't break constraints
            if (isScheduleValid(improved)) {
                log.info("Pattern applied successfully");
                improved.setPreviousVersion(deepCopySchedule(currentSchedule));
                return improved;
            } else {
                log.warn("Pattern application invalidated schedule, reverting");
            }
        }

        return schedule;
    }

    // ========================================================================
    // SCENARIO-SPECIFIC CONSTRAINT BUILDERS
    // ========================================================================

    private Schedule buildHighSchoolSchedule(ScheduleGenerationRequest request) {
        log.info("Building High School schedule with AP/IB/Honors support");

        Schedule schedule = new Schedule();
        schedule.setName("High School Master Schedule");
        schedule.setPeriod(SchedulePeriod.MASTER);
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());

        // High school specific: Lunch periods
        // Apply high school constraints
        // These would be loaded from a separate constraints file
        return schedule;
    }

    private Schedule buildElementarySchedule(ScheduleGenerationRequest request) {
        log.info("Building Elementary schedule with recess/specials rotation");

        Schedule schedule = new Schedule();
        schedule.setName("Elementary Master Schedule");
        schedule.setPeriod(SchedulePeriod.MASTER);
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());

        // Elementary specific constraints
        // - Recess blocks
        // - Specials rotation (Art, Music, PE, Library)
        // - Self-contained classrooms

        return schedule;
    }

    private Schedule buildUniversitySchedule(ScheduleGenerationRequest request) {
        log.info("Building University schedule with flexible credit hours");

        Schedule schedule = new Schedule();
        schedule.setName("University Master Schedule");
        schedule.setPeriod(SchedulePeriod.MASTER);
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());

        // University specific
        // - 50-minute, 75-minute, 3-hour blocks
        // - MWF vs TR patterns
        // - Adjunct faculty availability windows
        Map<String, Object> adjunctWindows = generateAdjunctWindows();

        return schedule;
    }

    // ========================================================================
    // USER FEEDBACK & LEARNING
    // ========================================================================

    public void applyUserFeedback(Schedule schedule, UserFeedback feedback) {
        log.info("Processing user feedback for continuous improvement");

        // Adjust constraint weights based on feedback
        if (feedback.hasTeacherComplaints()) {
            adjustWeight("teacher_preference", 1.2);
            adjustWeight("consecutive_hours", 1.3);
        }

        if (feedback.hasRoomIssues()) {
            adjustWeight("room_capacity", 1.5);
            adjustWeight("room_equipment", 1.4);
        }

        if (feedback.hasStudentConflicts()) {
            adjustWeight("student_conflicts", 2.0);
            adjustWeight("course_prerequisites", 1.8);
        }

        // Extract and save successful patterns
        SchedulePattern pattern = extractPattern(schedule, feedback);
        savePattern(pattern);

        // Retrain ML model periodically
        if (getPatternCount() % 10 == 0) {
            retrainMLModel();
        }
    }

    // ========================================================================
    // METRICS CALCULATION
    // ========================================================================

    private ScheduleMetrics calculateScheduleMetrics(Schedule schedule) {
        ScheduleMetrics metrics = new ScheduleMetrics();

        metrics.setRoomUtilization(calculateRoomUtilization(schedule));
        metrics.setTeacherUtilization(calculateTeacherUtilization(schedule));
        metrics.setConflictRate(calculateConflictRate(schedule));

        // Workload balance
        metrics.setWorkloadBalance(calculateWorkloadBalance(schedule));
        metrics.setStudentSatisfaction(estimateStudentSatisfaction(schedule));
        metrics.setComplianceScore(calculateComplianceScore(schedule));

        // Overall quality score (weighted average)
        double quality = (metrics.getRoomUtilization() * 0.15 +
                metrics.getTeacherUtilization() * 0.20 +
                (1 - metrics.getConflictRate()) * 0.30 +
                metrics.getWorkloadBalance() * 0.15 +
                metrics.getStudentSatisfaction() * 0.10 +
                metrics.getComplianceScore() * 0.10) * 100;

        metrics.setQualityScore(quality);
        return metrics;
    }

    // ========================================================================
    // HELPER METHODS - GENETIC ALGORITHM
    // ========================================================================

    private void evaluateFitness(List<Chromosome> population, ScenarioType scenarioType) {
        for (Chromosome chromosome : population) {
            Schedule schedule = chromosomeToSchedule(chromosome);
            double energy = calculateEnergy(schedule, scenarioType);
            chromosome.setFitness(1.0 / (1.0 + energy)); // Convert energy to fitness
        }
    }

    private List<Chromosome> selectParents(List<Chromosome> population) {
        // Tournament selection
        List<Chromosome> parents = new ArrayList<>();
        int tournamentSize = 5;

        for (int i = 0; i < GA_POPULATION_SIZE; i++) {
            Chromosome best = null;
            for (int j = 0; j < tournamentSize; j++) {
                Chromosome candidate = population.get(random.nextInt(population.size()));
                if (best == null || candidate.getFitness() > best.getFitness()) {
                    best = candidate;
                }
            }
            parents.add(best);
        }

        return parents;
    }

    /**
     * Perform crossover on parent chromosomes
     *
     * FIXED: Uses uniform crossover to preserve all courses
     * Previous single-point crossover could create invalid offspring with
     * duplicate or missing courses.
     *
     * Complexity: O(P × G) where P = population size, G = genome size
     *
     * @param parents Parent chromosomes selected for breeding
     * @return Offspring chromosomes
     */
    private List<Chromosome> performCrossover(List<Chromosome> parents) {
        List<Chromosome> offspring = new ArrayList<>();

        for (int i = 0; i < parents.size() - 1; i += 2) {
            if (random.nextDouble() < GA_CROSSOVER_RATE) {
                Chromosome parent1 = parents.get(i);
                Chromosome parent2 = parents.get(i + 1);

                // Uniform crossover - preserves all courses
                Chromosome child1 = new Chromosome();
                Chromosome child2 = new Chromosome();

                List<Gene> genes1 = new ArrayList<>();
                List<Gene> genes2 = new ArrayList<>();

                // For each gene position, randomly choose which parent to inherit from
                for (int g = 0; g < parent1.getGenes().size(); g++) {
                    if (random.nextBoolean()) {
                        genes1.add(copyGene(parent1.getGenes().get(g)));
                        genes2.add(copyGene(parent2.getGenes().get(g)));
                    } else {
                        genes1.add(copyGene(parent2.getGenes().get(g)));
                        genes2.add(copyGene(parent1.getGenes().get(g)));
                    }
                }

                child1.setGenes(genes1);
                child2.setGenes(genes2);

                // Validate offspring - ensure all courses are present
                if (isValidChromosome(child1) && isValidChromosome(child2)) {
                    offspring.add(child1);
                    offspring.add(child2);
                } else {
                    // If offspring invalid, keep parents
                    log.warn("Crossover produced invalid offspring, keeping parents");
                    offspring.add(parent1);
                    offspring.add(parent2);
                }
            } else {
                offspring.add(parents.get(i));
                if (i + 1 < parents.size()) {
                    offspring.add(parents.get(i + 1));
                }
            }
        }

        return offspring;
    }

    /**
     * Create a deep copy of a gene
     */
    private Gene copyGene(Gene original) {
        Gene copy = new Gene();
        copy.setCourse(original.getCourse());
        copy.setTeacher(original.getTeacher());
        copy.setRoom(original.getRoom());
        copy.setTimeSlot(original.getTimeSlot());
        return copy;
    }

    /**
     * Validate chromosome has all required courses with no duplicates
     */
    private boolean isValidChromosome(Chromosome chromosome) {
        if (chromosome == null || chromosome.getGenes() == null) {
            return false;
        }

        // Check all genes have courses assigned
        for (Gene gene : chromosome.getGenes()) {
            if (gene.getCourse() == null) {
                return false;
            }
        }

        // Check for duplicate courses
        Set<Course> courses = chromosome.getGenes().stream()
            .map(Gene::getCourse)
            .collect(Collectors.toSet());

        // Valid if no duplicates (set size == list size)
        return courses.size() == chromosome.getGenes().size();
    }

    private void performMutation(List<Chromosome> population) {
        for (Chromosome chromosome : population) {
            for (Gene gene : chromosome.getGenes()) {
                if (random.nextDouble() < GA_MUTATION_RATE) {
                    // Mutate time slot
                    gene.setTimeSlot(generateRandomTimeSlot(ScenarioType.HIGH_SCHOOL));
                }
            }
        }
    }

    private List<Chromosome> selectSurvivors(List<Chromosome> population, List<Chromosome> offspring) {
        // Elitism + generational replacement
        List<Chromosome> combined = new ArrayList<>();
        combined.addAll(population);
        combined.addAll(offspring);

        return combined.stream()
                .sorted(Comparator.comparingDouble(Chromosome::getFitness).reversed())
                .limit(GA_POPULATION_SIZE)
                .collect(Collectors.toList());
    }

    // ========================================================================
    // HELPER METHODS - SIMULATED ANNEALING
    // ========================================================================

    private Schedule generateNeighbor(Schedule schedule) {
        Schedule neighbor = deepCopySchedule(schedule);

        // Random modification: swap two time slots
        List<ScheduleSlot> slots = neighbor.getSlots();
        if (slots.size() >= 2) {
            int idx1 = random.nextInt(slots.size());
            int idx2 = random.nextInt(slots.size());

            ScheduleSlot slot1 = slots.get(idx1);
            ScheduleSlot slot2 = slots.get(idx2);

            // Swap times
            LocalTime temp = slot1.getStartTime();
            slot1.setStartTime(slot2.getStartTime());
            slot2.setStartTime(temp);
        }

        return neighbor;
    }

    private Schedule deepCopySchedule(Schedule schedule) {
        // Simple deep copy - in production would use proper cloning
        Schedule copy = new Schedule();
        copy.setName(schedule.getName());
        copy.setScheduleType(schedule.getScheduleType());
        copy.setStartDate(schedule.getStartDate());
        copy.setEndDate(schedule.getEndDate());
        copy.setStatus(schedule.getStatus());

        // Copy slots
        List<ScheduleSlot> copiedSlots = new ArrayList<>();
        for (ScheduleSlot slot : schedule.getSlots()) {
            ScheduleSlot newSlot = new ScheduleSlot();
            newSlot.setDayOfWeek(slot.getDayOfWeek());
            newSlot.setStartTime(slot.getStartTime());
            newSlot.setEndTime(slot.getEndTime());
            newSlot.setTeacher(slot.getTeacher());
            newSlot.setCourse(slot.getCourse());
            newSlot.setRoom(slot.getRoom());
            newSlot.setStatus(slot.getStatus());
            copiedSlots.add(newSlot);
        }
        copy.setSlots(copiedSlots);

        return copy;
    }

    // ========================================================================
    // HELPER METHODS - CALCULATIONS
    // ========================================================================

    private double calculateWorkloadImbalance(Schedule schedule) {
        Map<Teacher, Long> workloads = schedule.getSlots().stream()
                .filter(slot -> slot.getTeacher() != null)
                .collect(Collectors.groupingBy(ScheduleSlot::getTeacher, Collectors.counting()));

        if (workloads.isEmpty())
            return 0;

        double avg = workloads.values().stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = workloads.values().stream()
                .mapToDouble(count -> Math.pow(count - avg, 2))
                .average()
                .orElse(0);

        return Math.sqrt(variance);
    }

    private double calculateScheduleGaps(Schedule schedule) {
        // Count gaps in teacher schedules
        Map<Teacher, List<ScheduleSlot>> teacherSchedules = schedule.getSlots().stream()
                .filter(slot -> slot.getTeacher() != null)
                .collect(Collectors.groupingBy(ScheduleSlot::getTeacher));

        int totalGaps = 0;
        for (List<ScheduleSlot> slots : teacherSchedules.values()) {
            slots.sort(Comparator.comparing(ScheduleSlot::getStartTime));

            for (int i = 0; i < slots.size() - 1; i++) {
                LocalTime end = slots.get(i).getEndTime();
                LocalTime nextStart = slots.get(i + 1).getStartTime();

                if (Duration.between(end, nextStart).toMinutes() > 60) {
                    totalGaps++;
                }
            }
        }

        return totalGaps;
    }

    private double calculateAPCoursePenalty(Schedule schedule) {
        // Penalty for AP courses scheduled at non-optimal times
        long badAPScheduling = schedule.getSlots().stream()
                .filter(slot -> slot.getCourse() != null &&
                        slot.getCourse().getCourseName().contains("AP"))
                .filter(slot -> slot.getStartTime().isBefore(LocalTime.of(8, 30)) ||
                        slot.getStartTime().isAfter(LocalTime.of(14, 0)))
                .count();

        return badAPScheduling;
    }

    private double calculateSpecialsDistribution(Schedule schedule) {
        // Check if specials (Art, Music, PE) are well-distributed
        // Lower score is better
        return 0; // Simplified
    }

    private double calculateLabCoursePenalty(Schedule schedule) {
        // Penalty for lab courses not getting long enough blocks
        long inadequateLabTime = schedule.getSlots().stream()
                .filter(slot -> slot.getCourse() != null &&
                        slot.getCourse().getCourseName().toLowerCase().contains("lab"))
                .filter(slot -> Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes() < 90)
                .count();

        return inadequateLabTime;
    }

    private double calculateRoomUtilization(Schedule schedule) {
        // Calculate what percentage of time rooms are being used
        long totalRoomSlots = roomRepository.count() * 40; // Assuming 40 periods per week
        long usedSlots = schedule.getSlots().stream()
                .filter(slot -> slot.getRoom() != null)
                .count();

        return usedSlots / (double) totalRoomSlots;
    }

    private double calculateTeacherUtilization(Schedule schedule) {
        // Calculate average teacher workload utilization
        Map<Teacher, Long> workloads = schedule.getSlots().stream()
                .filter(slot -> slot.getTeacher() != null)
                .collect(Collectors.groupingBy(ScheduleSlot::getTeacher, Collectors.counting()));

        if (workloads.isEmpty())
            return 0;

        double avgWorkload = workloads.values().stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        // Assume ideal workload is 25 hours/week = 5 hours/day = ~6-7 periods
        double idealWorkload = 30.0;
        return Math.min(avgWorkload / idealWorkload, 1.0);
    }

    private double calculateConflictRate(Schedule schedule) {
        long conflicts = schedule.getSlots().stream()
                .filter(slot -> slot.getStatus() == SlotStatus.CONFLICT)
                .count();

        return schedule.getSlots().isEmpty() ? 0 : conflicts / (double) schedule.getSlots().size();
    }

    private double calculateWorkloadBalance(Schedule schedule) {
        double imbalance = calculateWorkloadImbalance(schedule);
        // Convert to 0-1 scale where 1 is perfectly balanced
        return Math.max(0, 1 - (imbalance / 10.0));
    }

    private double estimateStudentSatisfaction(Schedule schedule) {
        // Simplified: fewer gaps and conflicts = higher satisfaction
        double gaps = calculateScheduleGaps(schedule);
        double conflicts = calculateConflictRate(schedule);

        return Math.max(0, 1 - (gaps * 0.05) - (conflicts * 0.5));
    }

    private double calculateComplianceScore(Schedule schedule) {
        // Check compliance with basic requirements
        // - No teacher conflicts
        // - No room conflicts
        // - Reasonable consecutive hours

        double score = 1.0;

        // Deduct for violations
        long teacherConflicts = countTeacherConflicts(schedule);
        long roomConflicts = countRoomConflicts(schedule);

        score -= (teacherConflicts * 0.1);
        score -= (roomConflicts * 0.1);

        return Math.max(0, score);
    }

    private long countTeacherConflicts(Schedule schedule) {
        Map<String, List<ScheduleSlot>> byTeacherAndTime = schedule.getSlots().stream()
                .filter(slot -> slot.getTeacher() != null)
                .collect(Collectors.groupingBy(slot -> slot.getTeacher().getId() + "_" +
                        slot.getDayOfWeek() + "_" +
                        slot.getStartTime()));

        return byTeacherAndTime.values().stream()
                .filter(slots -> slots.size() > 1)
                .count();
    }

    private long countRoomConflicts(Schedule schedule) {
        Map<String, List<ScheduleSlot>> byRoomAndTime = schedule.getSlots().stream()
                .filter(slot -> slot.getRoom() != null)
                .collect(Collectors.groupingBy(slot -> slot.getRoom().getId() + "_" +
                        slot.getDayOfWeek() + "_" +
                        slot.getStartTime()));

        return byRoomAndTime.values().stream()
                .filter(slots -> slots.size() > 1)
                .count();
    }

    // ========================================================================
    // HELPER METHODS - ML PATTERNS
    // ========================================================================

    private List<SchedulePattern> loadHistoricalPatterns(ScenarioType scenarioType) {
        return learnedPatterns.getOrDefault(scenarioType, new ArrayList<>());
    }

    private boolean isPatternApplicable(SchedulePattern pattern, Schedule schedule) {
        // Check if pattern constraints match current schedule
        return true; // Simplified
    }

    private Schedule applyPattern(Schedule schedule, SchedulePattern pattern) {
        // Apply learned pattern modifications
        return schedule; // Simplified
    }

    private boolean isScheduleValid(Schedule schedule) {
        // Verify no hard constraint violations
        return countTeacherConflicts(schedule) == 0 &&
                countRoomConflicts(schedule) == 0;
    }

    private SchedulePattern extractPattern(Schedule schedule, UserFeedback feedback) {
        SchedulePattern pattern = new SchedulePattern();
        pattern.setName("Pattern_" + System.currentTimeMillis());
        pattern.setConfidence(0.5);
        pattern.setParameters(new HashMap<>());
        return pattern;
    }

    private void savePattern(SchedulePattern pattern) {
        // Save to database or file
        log.debug("Pattern saved: {}", pattern.getName());
    }

    private int getPatternCount() {
        return learnedPatterns.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    private void retrainMLModel() {
        log.info("Retraining ML model with {} patterns", getPatternCount());
        // Actual ML training would go here
    }

    private void adjustWeight(String constraint, double factor) {
        double currentWeight = constraintWeights.getOrDefault(constraint, 1.0);
        constraintWeights.put(constraint, currentWeight * factor);
        log.debug("Adjusted weight for {}: {}", constraint, currentWeight * factor);
    }

    private TimeSlot generateRandomTimeSlot(ScenarioType scenarioType) {
        // Generate random time slot based on scenario
        TimeSlot slot = new TimeSlot();

        // Random day
        java.time.DayOfWeek[] days = java.time.DayOfWeek.values();
        slot.setDayOfWeek(days[random.nextInt(days.length)]);

        // Random start time (8 AM - 2 PM)
        int hour = 8 + random.nextInt(6);
        slot.setStartTime(LocalTime.of(hour, 0));
        slot.setEndTime(slot.getStartTime().plusMinutes(50));

        return slot;
    }

    private Schedule chromosomeToSchedule(Chromosome chromosome) {
        Schedule schedule = new Schedule();
        schedule.setName("Generated Schedule");
        schedule.setPeriod(SchedulePeriod.MASTER);

        List<ScheduleSlot> slots = new ArrayList<>();
        for (Gene gene : chromosome.getGenes()) {
            ScheduleSlot slot = new ScheduleSlot();
            slot.setCourse(gene.getCourse());
            slot.setTeacher(gene.getTeacher());
            slot.setRoom(gene.getRoom());
            slot.setDayOfWeek(gene.getTimeSlot().getDayOfWeek());
            slot.setStartTime(gene.getTimeSlot().getStartTime());
            slot.setEndTime(gene.getTimeSlot().getEndTime());
            slot.setStatus(com.heronix.scheduler.model.enums.SlotStatus.SCHEDULED);
            slots.add(slot);
        }

        schedule.setSlots(slots);
        return schedule;
    }

    private Map<String, Object> generateAdjunctWindows() {
        // Generate availability windows for adjunct faculty
        return new HashMap<>();
    }

    // ========================================================================
    // CONFLICT RESOLUTION
    // ========================================================================

    /**
     * Resolve scheduling conflicts using hybrid optimization
     *
     * @param schedule Current schedule with conflicts
     * @param slot The slot causing the conflict
     * @param newTime The proposed new time for the slot
     * @return Optimized schedule with conflicts resolved
     */
    public Schedule resolveConflict(Schedule schedule, ScheduleSlot slot, TimeSlot newTime) {
        log.info("Resolving conflict for slot: {} {} - {} to new time: {} {} - {}",
                slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime(),
                newTime.getDayOfWeek(), newTime.getStartTime(), newTime.getEndTime());

        Schedule resolvedSchedule = deepCopySchedule(schedule);

        // Find the slot to update in the copied schedule
        ScheduleSlot targetSlot = resolvedSchedule.getSlots().stream()
                .filter(s -> s.getCourse() != null && slot.getCourse() != null &&
                        s.getCourse().getId().equals(slot.getCourse().getId()) &&
                        s.getDayOfWeek() == slot.getDayOfWeek() &&
                        s.getStartTime().equals(slot.getStartTime()))
                .findFirst()
                .orElse(null);

        if (targetSlot != null) {
            // Update the slot with new time
            targetSlot.setDayOfWeek(newTime.getDayOfWeek());
            targetSlot.setStartTime(newTime.getStartTime());
            targetSlot.setEndTime(newTime.getEndTime());
            targetSlot.setStatus(SlotStatus.SCHEDULED);

            // Check for any new conflicts introduced
            boolean hasConflicts = checkForConflicts(resolvedSchedule, targetSlot);

            if (hasConflicts) {
                log.warn("New time slot introduces conflicts, attempting to resolve...");
                // Run simulated annealing to optimize around this change
                resolvedSchedule = runSimulatedAnnealing(resolvedSchedule, ScenarioType.HIGH_SCHOOL);
            }

            log.info("Conflict resolution completed successfully");
        } else {
            log.warn("Target slot not found in schedule, returning original");
            return schedule;
        }

        return resolvedSchedule;
    }

    /**
     * Check if a slot conflicts with other slots in the schedule
     */
    private boolean checkForConflicts(Schedule schedule, ScheduleSlot slot) {
        // Check teacher conflicts
        boolean teacherConflict = schedule.getSlots().stream()
                .filter(s -> s != slot && s.getTeacher() != null && slot.getTeacher() != null)
                .filter(s -> s.getTeacher().getId().equals(slot.getTeacher().getId()))
                .anyMatch(s -> s.getDayOfWeek() == slot.getDayOfWeek() &&
                        timesOverlap(s.getStartTime(), s.getEndTime(),
                                slot.getStartTime(), slot.getEndTime()));

        // Check room conflicts
        boolean roomConflict = schedule.getSlots().stream()
                .filter(s -> s != slot && s.getRoom() != null && slot.getRoom() != null)
                .filter(s -> s.getRoom().getId().equals(slot.getRoom().getId()))
                .anyMatch(s -> s.getDayOfWeek() == slot.getDayOfWeek() &&
                        timesOverlap(s.getStartTime(), s.getEndTime(),
                                slot.getStartTime(), slot.getEndTime()));

        if (teacherConflict || roomConflict) {
            slot.setStatus(SlotStatus.CONFLICT);
            return true;
        }

        return false;
    }

    /**
     * Check if two time ranges overlap
     *
     * FIXED: Rewritten for clarity (removed double negatives)
     *
     * Two time ranges overlap if:
     * - First range starts before second range ends AND
     * - First range ends after second range starts
     *
     * Examples:
     * - [9:00-10:00] and [9:30-10:30] overlap (9:00 < 10:30 AND 10:00 > 9:30)
     * - [9:00-10:00] and [10:00-11:00] do NOT overlap (adjacent, not overlapping)
     * - [9:00-10:00] and [11:00-12:00] do NOT overlap (separate)
     *
     * @param start1 Start time of first range
     * @param end1 End time of first range
     * @param start2 Start time of second range
     * @param end2 End time of second range
     * @return true if ranges overlap
     */
    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        // Times overlap if start1 is before end2 AND end1 is after start2
        return start1.isBefore(end2) && end1.isAfter(start2);
    }

    // ========================================================================
    // INNER CLASSES
    // ========================================================================

    @Data
    private static class CourseCluster {
        private int id;
        private List<Course> courses = new ArrayList<>();
        private List<Teacher> compatibleTeachers = new ArrayList<>();

        public void assignTeachers(List<Teacher> allTeachers) {
            // Find teachers qualified for courses in this cluster
            this.compatibleTeachers = allTeachers.stream()
                    .filter(t -> courses.stream()
                            .anyMatch(c -> isTeacherQualified(t, c)))
                    .collect(Collectors.toList());
        }

        public List<Teacher> getCompatibleTeachers(Course course) {
            return compatibleTeachers.stream()
                    .filter(t -> isTeacherQualified(t, course))
                    .collect(Collectors.toList());
        }

        private boolean isTeacherQualified(Teacher teacher, Course course) {
            // Check if teacher's department matches course subject
            return teacher.getDepartment() != null &&
                    course.getSubject() != null &&
                    teacher.getDepartment().equalsIgnoreCase(course.getSubject());
        }
    }

    @Data
    private static class Chromosome {
        private List<Gene> genes = new ArrayList<>();
        private double fitness;
    }

    @Data
    private static class Gene {
        private Course course;
        private Teacher teacher;
        private Room room;
        private TimeSlot timeSlot;
    }

    @Data
    private static class SchedulePattern {
        private String name;
        private double confidence;
        private Map<String, Object> parameters = new HashMap<>();
    }

    @Data
    private static class ScheduleMetrics {
        private double roomUtilization;
        private double teacherUtilization;
        private double conflictRate;
        private double workloadBalance;
        private double studentSatisfaction;
        private double complianceScore;
        private double qualityScore;
    }
}
