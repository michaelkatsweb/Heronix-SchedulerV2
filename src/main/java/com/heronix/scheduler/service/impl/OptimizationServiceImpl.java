package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.ConstraintType;
import com.heronix.scheduler.model.enums.OptimizationAlgorithm;
import com.heronix.scheduler.repository.OptimizationConfigRepository;
import com.heronix.scheduler.repository.OptimizationResultRepository;
import com.heronix.scheduler.repository.RoomRepository;
import com.heronix.scheduler.repository.ScheduleRepository;
import com.heronix.scheduler.service.ConflictDetectorService;
import com.heronix.scheduler.service.OptimizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heronix.scheduler.service.data.SISDataService;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Optimization Service Implementation
 * Orchestrates schedule optimization using genetic algorithms
 *
 * Location: src/main/java/com/eduscheduler/service/impl/OptimizationServiceImpl.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
@Service
@Transactional
@Slf4j
public class OptimizationServiceImpl implements OptimizationService {

    @Autowired
    private SISDataService sisDataService;
    @Autowired
    private ConflictDetectorService conflictDetector;

    @Autowired
    private OptimizationConfigRepository configRepository;

    @Autowired
    private OptimizationResultRepository resultRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private FitnessEvaluator fitnessEvaluator;

    // Track running optimizations for cancellation
    private final Map<Long, GeneticAlgorithm> runningOptimizations = new ConcurrentHashMap<>();

    // ========================================================================
    // OPTIMIZATION OPERATIONS
    // ========================================================================

    @Override
    public OptimizationResult optimizeSchedule(Schedule schedule, OptimizationConfig config,
                                               Consumer<OptimizationProgress> progressCallback) {
        log.info("Starting optimization for schedule: {}", schedule.getScheduleName());

        // Create result record
        OptimizationResult result = OptimizationResult.builder()
            .schedule(schedule)
            .config(config)
            .algorithm(config.getAlgorithm())
            .status(OptimizationResult.OptimizationStatus.PENDING)
            .build();

        result = resultRepository.save(result);

        try {
            // Create and run genetic algorithm
            GeneticAlgorithm ga = new GeneticAlgorithm(config, fitnessEvaluator);
            runningOptimizations.put(result.getId(), ga);

            // Convert progress callback
            Consumer<GeneticAlgorithm.GAProgress> gaCallback = gaProgress -> {
                if (progressCallback != null) {
                    OptimizationProgress progress = new OptimizationProgress(
                        gaProgress.getGeneration(),
                        gaProgress.getMaxGenerations(),
                        gaProgress.getAvgFitness(),
                        gaProgress.getBestFitness(),
                        gaProgress.getConflicts(),
                        "Generation " + gaProgress.getGeneration(),
                        gaProgress.getElapsedSeconds()
                    );
                    progressCallback.accept(progress);
                }
            };

            // Run optimization
            OptimizationResult gaResult = ga.run(schedule, gaCallback);

            // Update result
            result.setStatus(gaResult.getStatus());
            result.setStartedAt(gaResult.getStartedAt());
            result.setCompletedAt(gaResult.getCompletedAt());
            result.setRuntimeSeconds(gaResult.getRuntimeSeconds());
            result.setInitialFitness(gaResult.getInitialFitness());
            result.setFinalFitness(gaResult.getFinalFitness());
            result.setBestFitness(gaResult.getBestFitness());
            result.setImprovementPercentage(gaResult.getImprovementPercentage());
            result.setGenerationsExecuted(gaResult.getGenerationsExecuted());
            result.setInitialConflicts(gaResult.getInitialConflicts());
            result.setFinalConflicts(gaResult.getFinalConflicts());
            result.setSuccessful(gaResult.getSuccessful());
            result.setMessage(gaResult.getMessage());

            // Calculate additional statistics
            // ✅ NULL SAFE: Filter null conflicts and null severity before counting
            List<Conflict> finalConflicts = conflictDetector.detectAllConflicts(schedule);
            result.setCriticalConflicts((int) finalConflicts.stream()
                .filter(c -> c != null && c.getSeverity() != null)
                .filter(c -> c.getSeverity() == com.heronix.scheduler.model.enums.ConflictSeverity.CRITICAL)
                .count());

            // Save updated result
            result = resultRepository.save(result);

            // Save optimized schedule
            scheduleRepository.save(schedule);

            log.info("Optimization completed. Result ID: {}, Final fitness: {}",
                    result.getId(), result.getFinalFitness());

        } catch (Exception e) {
            log.error("Optimization failed", e);
            result.setStatus(OptimizationResult.OptimizationStatus.FAILED);
            result.setSuccessful(false);
            result.setMessage("Optimization failed: " + e.getMessage());
            result.setErrorDetails(e.toString());
            result = resultRepository.save(result);
        } finally {
            runningOptimizations.remove(result.getId());
        }

        return result;
    }

    @Override
    public ScheduleGenerationResult generateSchedule(String scheduleName, List<Course> courses,
                                                     OptimizationConfig config,
                                                     Consumer<OptimizationProgress> progressCallback) {
        log.info("Generating new schedule: {}", scheduleName);

        try {
            // Create blank schedule
            Schedule schedule = new Schedule();
            schedule.setScheduleName(scheduleName);
            schedule.setPeriod(com.heronix.scheduler.model.enums.SchedulePeriod.SEMESTER);
            schedule.setScheduleType(com.heronix.scheduler.model.enums.ScheduleType.TRADITIONAL);
            schedule.setStatus(com.heronix.scheduler.model.enums.ScheduleStatus.DRAFT);
            schedule.setStartDate(LocalDateTime.now().toLocalDate());
            schedule.setEndDate(LocalDateTime.now().plusMonths(4).toLocalDate());

            // Generate initial slot assignments for courses
            generateInitialSlotAssignments(schedule);

            schedule = scheduleRepository.save(schedule);

            // Optimize the schedule
            OptimizationResult result = optimizeSchedule(schedule, config, progressCallback);

            return new ScheduleGenerationResult(
                schedule,
                result,
                result.getSuccessful(),
                "Schedule generated successfully"
            );

        } catch (Exception e) {
            log.error("Schedule generation failed", e);
            return new ScheduleGenerationResult(
                null,
                null,
                false,
                "Generation failed: " + e.getMessage()
            );
        }
    }

    @Override
    public OptimizationResult improveScheduleForConstraint(Schedule schedule,
                                                          ConstraintType constraintType,
                                                          int maxIterations) {
        log.info("Improving schedule for constraint: {}", constraintType);

        // Create custom config focusing on specific constraint
        OptimizationConfig config = getDefaultConfig();
        config.setMaxGenerations(maxIterations);

        // Increase weight for target constraint
        config.setConstraintWeight(constraintType, (double)(constraintType.getDefaultWeight() * 2));

        return optimizeSchedule(schedule, config, null);
    }

    @Override
    public OptimizationResult quickOptimize(Schedule schedule) {
        log.info("Quick optimization for schedule: {}", schedule.getScheduleName());

        // Create quick config with fewer iterations
        OptimizationConfig config = getDefaultConfig();
        config.setPopulationSize(50);
        config.setMaxGenerations(100);
        config.setMaxRuntimeSeconds(60); // 1 minute max

        return optimizeSchedule(schedule, config, null);
    }

    @Override
    public void cancelOptimization(Long resultId) {
        log.info("Cancelling optimization: {}", resultId);

        GeneticAlgorithm ga = runningOptimizations.get(resultId);
        if (ga != null) {
            ga.cancel();
            runningOptimizations.remove(resultId);

            // Update result status
            OptimizationResult result = resultRepository.findById(resultId).orElse(null);
            if (result != null) {
                result.setStatus(OptimizationResult.OptimizationStatus.CANCELLED);
                result.setMessage("Cancelled by user");
                resultRepository.save(result);
            }
        }
    }

    // ========================================================================
    // FITNESS EVALUATION
    // ========================================================================

    @Override
    public double evaluateFitness(Schedule schedule, OptimizationConfig config) {
        return fitnessEvaluator.evaluate(schedule, config);
    }

    @Override
    public FitnessBreakdown getFitnessBreakdown(Schedule schedule, OptimizationConfig config) {
        FitnessEvaluator.FitnessBreakdown evaluatorBreakdown =
            fitnessEvaluator.getBreakdown(schedule, config);

        // Convert to service interface type
        FitnessBreakdown breakdown = new FitnessBreakdown();
        breakdown.setTotalFitness(evaluatorBreakdown.getTotalFitness());
        breakdown.setHardConstraintScore(evaluatorBreakdown.getHardConstraintScore());
        breakdown.setSoftConstraintScore(evaluatorBreakdown.getSoftConstraintScore());

        for (Map.Entry<ConstraintType, Double> entry :
             evaluatorBreakdown.getConstraintScores().entrySet()) {
            int violations = evaluatorBreakdown.getViolationCounts().getOrDefault(entry.getKey(), 0);
            breakdown.addConstraintScore(entry.getKey(), entry.getValue(), violations);
        }

        return breakdown;
    }

    // ========================================================================
    // CONSTRAINT CHECKING
    // ========================================================================

    @Override
    public int countConstraintViolations(Schedule schedule, ConstraintType constraintType) {
        List<Conflict> conflicts = conflictDetector.detectAllConflicts(schedule);

        // Count conflicts that map to this constraint
        return (int) conflicts.stream()
            .filter(c -> !c.getIsResolved() && !c.getIsIgnored())
            .filter(c -> mapsToConstraint(c, constraintType))
            .count();
    }

    @Override
    public List<ConstraintViolation> getAllViolations(Schedule schedule) {
        List<Conflict> conflicts = conflictDetector.detectAllConflicts(schedule);
        List<ConstraintViolation> violations = new ArrayList<>();

        for (Conflict conflict : conflicts) {
            if (conflict.getIsResolved() || conflict.getIsIgnored()) {
                continue;
            }

            ConstraintType type = mapConflictToConstraint(conflict.getConflictType());
            ConstraintViolation violation = new ConstraintViolation(
                type,
                conflict.getDescription(),
                conflict.getAffectedSlots(),
                conflict.getSeverity().getPriorityScore()
            );
            violations.add(violation);
        }

        return violations;
    }

    @Override
    public boolean satisfiesHardConstraints(Schedule schedule) {
        List<Conflict> conflicts = conflictDetector.detectAllConflicts(schedule);

        // Check if any critical conflicts exist
        return conflicts.stream()
            .filter(c -> !c.getIsResolved() && !c.getIsIgnored())
            .noneMatch(c -> c.getSeverity() ==
                com.heronix.scheduler.model.enums.ConflictSeverity.CRITICAL);
    }

    // ========================================================================
    // CONFIGURATION MANAGEMENT
    // ========================================================================

    @Override
    public OptimizationConfig getDefaultConfig() {
        // Check if default config exists in database
        List<OptimizationConfig> configs = configRepository.findAll();
        Optional<OptimizationConfig> defaultConfig = configs.stream()
            .filter(OptimizationConfig::getIsDefault)
            .findFirst();

        if (defaultConfig.isPresent()) {
            return defaultConfig.get();
        }

        // Create default configuration
        OptimizationConfig config = OptimizationConfig.builder()
            .configName("Default Configuration")
            .description("Default optimization settings")
            .algorithm(OptimizationAlgorithm.GENETIC_ALGORITHM)
            .populationSize(100)
            .maxGenerations(1000)
            .mutationRate(0.1)
            .crossoverRate(0.8)
            .eliteSize(5)
            .tournamentSize(5)
            .maxRuntimeSeconds(300)
            .stagnationLimit(100)
            // .useParallelProcessing(true) // Field not present on OptimizationConfig
            // .parallelThreadCount(4) // Method does not exist
            .logFrequency(10)
            .isDefault(true)
            .build();

        return configRepository.save(config);
    }

    @Override
    public OptimizationConfig saveConfig(OptimizationConfig config) {
        // If this is being set as default, unset other defaults
        if (config.getIsDefault()) {
            List<OptimizationConfig> allConfigs = configRepository.findAll();
            for (OptimizationConfig c : allConfigs) {
                if (c.getIsDefault() && !c.getId().equals(config.getId())) {
                    c.setIsDefault(false);
                    configRepository.save(c);
                }
            }
        }

        return configRepository.save(config);
    }

    @Override
    public List<OptimizationConfig> getAllConfigs() {
        return configRepository.findAll();
    }

    @Override
    public void deleteConfig(Long configId) {
        OptimizationConfig config = configRepository.findById(configId).orElse(null);
        if (config != null && !config.getIsDefault()) {
            configRepository.deleteById(configId);
        } else {
            log.warn("Cannot delete default configuration");
        }
    }

    // ========================================================================
    // RESULT MANAGEMENT
    // ========================================================================

    @Override
    public OptimizationResult getResult(Long resultId) {
        return resultRepository.findById(resultId).orElse(null);
    }

    @Override
    public List<OptimizationResult> getResultsForSchedule(Schedule schedule) {
        return resultRepository.findByScheduleOrderByStartedAtDesc(schedule);
    }

    @Override
    public List<OptimizationResult> getRecentResults(int limit) {
        return resultRepository.findTopNByOrderByStartedAtDesc(limit);
    }

    @Override
    public int deleteOldResults(int daysOld) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
        List<OptimizationResult> oldResults = resultRepository.findByCompletedAtBefore(cutoffDate);
        resultRepository.deleteAll(oldResults);
        return oldResults.size();
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private boolean mapsToConstraint(Conflict conflict, ConstraintType constraintType) {
        ConstraintType mapped = mapConflictToConstraint(conflict.getConflictType());
        return mapped == constraintType;
    }

    private ConstraintType mapConflictToConstraint(
        com.heronix.scheduler.model.enums.ConflictType conflictType) {

        return switch (conflictType) {
            case TEACHER_OVERLOAD -> ConstraintType.NO_TEACHER_OVERLAP;
            case ROOM_DOUBLE_BOOKING -> ConstraintType.NO_ROOM_OVERLAP;
            case STUDENT_SCHEDULE_CONFLICT -> ConstraintType.NO_STUDENT_OVERLAP;
            case ROOM_CAPACITY_EXCEEDED -> ConstraintType.ROOM_CAPACITY;
            case SUBJECT_MISMATCH -> ConstraintType.TEACHER_QUALIFICATION;
            case EQUIPMENT_UNAVAILABLE -> ConstraintType.EQUIPMENT_AVAILABLE;
            case NO_LUNCH_BREAK -> ConstraintType.LUNCH_BREAK;
            case TEACHER_TRAVEL_TIME -> ConstraintType.MINIMIZE_TEACHER_TRAVEL;
            case STUDENT_TRAVEL_TIME -> ConstraintType.MINIMIZE_STUDENT_TRAVEL;
            case NO_PREPARATION_PERIOD -> ConstraintType.TEACHER_PREP_PERIODS;
            case EXCESSIVE_TEACHING_HOURS -> ConstraintType.BALANCE_TEACHER_LOAD;
            case ROOM_TYPE_MISMATCH -> ConstraintType.ROOM_PREFERENCES;
            case SECTION_OVER_ENROLLED, SECTION_UNDER_ENROLLED -> ConstraintType.BALANCE_CLASS_SIZES;
            default -> ConstraintType.MINIMIZE_STUDENT_GAPS;
        };
    }

    // ========================================================================
    // INITIAL SLOT GENERATION
    // ========================================================================

    /**
     * Generate initial slot assignments for all courses.
     * Creates a starting point for optimization algorithms.
     */
    private void generateInitialSlotAssignments(Schedule schedule) {
        log.info("Generating initial slot assignments for schedule");

        List<Course> courses = sisDataService.getAllCourses();
        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(Teacher::getActive)
            .collect(Collectors.toList());
        List<Room> rooms = roomRepository.findAll().stream()
            .filter(Room::isAvailable)
            .collect(Collectors.toList());

        if (courses.isEmpty() || teachers.isEmpty() || rooms.isEmpty()) {
            log.warn("Not enough data to generate slots: {} courses, {} teachers, {} rooms",
                courses.size(), teachers.size(), rooms.size());
            return;
        }

        // Define time slots (8 periods per day)
        LocalTime[] periodStarts = {
            LocalTime.of(8, 0), LocalTime.of(8, 50), LocalTime.of(9, 40),
            LocalTime.of(10, 30), LocalTime.of(11, 20), LocalTime.of(12, 10),
            LocalTime.of(13, 0), LocalTime.of(13, 50)
        };
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                           DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};

        int slotIndex = 0;
        int teacherIndex = 0;
        int roomIndex = 0;

        for (Course course : courses) {
            // Default to 5 periods per week for each course (1 per day)
            int periodsPerWeek = 5;

            for (int i = 0; i < periodsPerWeek && slotIndex < days.length * periodStarts.length; i++) {
                DayOfWeek day = days[slotIndex % days.length];
                LocalTime startTime = periodStarts[(slotIndex / days.length) % periodStarts.length];
                LocalTime endTime = startTime.plusMinutes(45);

                // Round-robin teacher and room assignment
                Teacher teacher = teachers.get(teacherIndex % teachers.size());
                Room room = rooms.get(roomIndex % rooms.size());

                ScheduleSlot slot = new ScheduleSlot();
                slot.setSchedule(schedule);
                slot.setCourse(course);
                slot.setTeacher(teacher);
                slot.setRoom(room);
                slot.setDayOfWeek(day);
                slot.setStartTime(startTime);
                slot.setEndTime(endTime);
                slot.setStatus(com.heronix.scheduler.model.enums.SlotStatus.DRAFT);

                schedule.addSlot(slot);

                slotIndex++;
                teacherIndex++;
                roomIndex++;
            }
        }

        log.info("Generated {} initial slots for {} courses", schedule.getSlots().size(), courses.size());
    }
}
