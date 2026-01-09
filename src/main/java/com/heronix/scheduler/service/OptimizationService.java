package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.ConstraintType;

import java.util.List;
import java.util.function.Consumer;

/**
 * Optimization Service Interface
 * Provides schedule optimization capabilities using various algorithms
 *
 * Location: src/main/java/com/eduscheduler/service/OptimizationService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
public interface OptimizationService {

    // ========================================================================
    // OPTIMIZATION OPERATIONS
    // ========================================================================

    /**
     * Optimize an existing schedule
     * @param schedule The schedule to optimize
     * @param config Optimization configuration
     * @param progressCallback Callback for progress updates (0.0 to 1.0)
     * @return Optimization result
     */
    OptimizationResult optimizeSchedule(Schedule schedule, OptimizationConfig config,
                                       Consumer<OptimizationProgress> progressCallback);

    /**
     * Generate a new schedule from scratch
     * @param scheduleName Name for the new schedule
     * @param courses Courses to include
     * @param config Optimization configuration
     * @param progressCallback Callback for progress updates
     * @return Generated schedule and optimization result
     */
    ScheduleGenerationResult generateSchedule(String scheduleName, List<Course> courses,
                                             OptimizationConfig config,
                                             Consumer<OptimizationProgress> progressCallback);

    /**
     * Improve schedule focusing on specific constraint
     * @param schedule The schedule to improve
     * @param constraintType The constraint to focus on
     * @param maxIterations Maximum number of iterations
     * @return Optimization result
     */
    OptimizationResult improveScheduleForConstraint(Schedule schedule, ConstraintType constraintType,
                                                   int maxIterations);

    /**
     * Quick optimization (fast, fewer iterations)
     * @param schedule The schedule to optimize
     * @return Optimization result
     */
    OptimizationResult quickOptimize(Schedule schedule);

    /**
     * Cancel running optimization
     * @param resultId The optimization result ID
     */
    void cancelOptimization(Long resultId);

    // ========================================================================
    // FITNESS EVALUATION
    // ========================================================================

    /**
     * Evaluate fitness of a schedule
     * @param schedule The schedule to evaluate
     * @param config Configuration with constraint weights
     * @return Fitness score (higher is better)
     */
    double evaluateFitness(Schedule schedule, OptimizationConfig config);

    /**
     * Get detailed fitness breakdown by constraint
     * @param schedule The schedule to evaluate
     * @param config Configuration with constraint weights
     * @return Fitness breakdown
     */
    FitnessBreakdown getFitnessBreakdown(Schedule schedule, OptimizationConfig config);

    // ========================================================================
    // CONSTRAINT CHECKING
    // ========================================================================

    /**
     * Check how many times a constraint is violated
     * @param schedule The schedule to check
     * @param constraintType The constraint type
     * @return Number of violations
     */
    int countConstraintViolations(Schedule schedule, ConstraintType constraintType);

    /**
     * Get all constraint violations
     * @param schedule The schedule to check
     * @return List of all violations
     */
    List<ConstraintViolation> getAllViolations(Schedule schedule);

    /**
     * Check if schedule satisfies all hard constraints
     * @param schedule The schedule to check
     * @return true if all hard constraints are satisfied
     */
    boolean satisfiesHardConstraints(Schedule schedule);

    // ========================================================================
    // CONFIGURATION MANAGEMENT
    // ========================================================================

    /**
     * Get default optimization configuration
     * @return Default configuration
     */
    OptimizationConfig getDefaultConfig();

    /**
     * Save optimization configuration
     * @param config The configuration to save
     * @return Saved configuration
     */
    OptimizationConfig saveConfig(OptimizationConfig config);

    /**
     * Get all saved configurations
     * @return List of configurations
     */
    List<OptimizationConfig> getAllConfigs();

    /**
     * Delete configuration
     * @param configId Configuration ID
     */
    void deleteConfig(Long configId);

    // ========================================================================
    // RESULT MANAGEMENT
    // ========================================================================

    /**
     * Get optimization result by ID
     * @param resultId Result ID
     * @return Optimization result
     */
    OptimizationResult getResult(Long resultId);

    /**
     * Get all optimization results for a schedule
     * @param schedule The schedule
     * @return List of results
     */
    List<OptimizationResult> getResultsForSchedule(Schedule schedule);

    /**
     * Get recent optimization results
     * @param limit Maximum number of results
     * @return List of recent results
     */
    List<OptimizationResult> getRecentResults(int limit);

    /**
     * Delete old optimization results
     * @param daysOld Delete results older than this many days
     * @return Number of results deleted
     */
    int deleteOldResults(int daysOld);

    // ========================================================================
    // SUPPORTING CLASSES
    // ========================================================================

    /**
     * Progress update for optimization
     */
    class OptimizationProgress {
        private int currentGeneration;
        private int totalGenerations;
        private double currentFitness;
        private double bestFitness;
        private int conflictCount;
        private String statusMessage;
        private long elapsedSeconds;

        public OptimizationProgress(int currentGeneration, int totalGenerations,
                                   double currentFitness, double bestFitness,
                                   int conflictCount, String statusMessage,
                                   long elapsedSeconds) {
            this.currentGeneration = currentGeneration;
            this.totalGenerations = totalGenerations;
            this.currentFitness = currentFitness;
            this.bestFitness = bestFitness;
            this.conflictCount = conflictCount;
            this.statusMessage = statusMessage;
            this.elapsedSeconds = elapsedSeconds;
        }

        // Getters
        public int getCurrentGeneration() { return currentGeneration; }
        public int getTotalGenerations() { return totalGenerations; }
        public double getCurrentFitness() { return currentFitness; }
        public double getBestFitness() { return bestFitness; }
        public int getConflictCount() { return conflictCount; }
        public String getStatusMessage() { return statusMessage; }
        public long getElapsedSeconds() { return elapsedSeconds; }

        public double getProgress() {
            return totalGenerations > 0 ? (double) currentGeneration / totalGenerations : 0.0;
        }

        public String getFormattedProgress() {
            return String.format("%d/%d (%.1f%%)", currentGeneration, totalGenerations,
                               getProgress() * 100);
        }
    }

    /**
     * Result of schedule generation
     */
    class ScheduleGenerationResult {
        private Schedule schedule;
        private OptimizationResult optimizationResult;
        private boolean success;
        private String message;

        public ScheduleGenerationResult(Schedule schedule, OptimizationResult result,
                                       boolean success, String message) {
            this.schedule = schedule;
            this.optimizationResult = result;
            this.success = success;
            this.message = message;
        }

        // Getters
        public Schedule getSchedule() { return schedule; }
        public OptimizationResult getOptimizationResult() { return optimizationResult; }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    /**
     * Fitness breakdown by constraint
     */
    class FitnessBreakdown {
        private double totalFitness;
        private double hardConstraintScore;
        private double softConstraintScore;
        private java.util.Map<ConstraintType, Double> constraintScores;
        private java.util.Map<ConstraintType, Integer> violationCounts;

        public FitnessBreakdown() {
            this.constraintScores = new java.util.HashMap<>();
            this.violationCounts = new java.util.HashMap<>();
        }

        // Getters and setters
        public double getTotalFitness() { return totalFitness; }
        public void setTotalFitness(double totalFitness) { this.totalFitness = totalFitness; }

        public double getHardConstraintScore() { return hardConstraintScore; }
        public void setHardConstraintScore(double score) { this.hardConstraintScore = score; }

        public double getSoftConstraintScore() { return softConstraintScore; }
        public void setSoftConstraintScore(double score) { this.softConstraintScore = score; }

        public java.util.Map<ConstraintType, Double> getConstraintScores() { return constraintScores; }
        public java.util.Map<ConstraintType, Integer> getViolationCounts() { return violationCounts; }

        public void addConstraintScore(ConstraintType type, double score, int violations) {
            constraintScores.put(type, score);
            violationCounts.put(type, violations);
        }

        public String getSummary() {
            return String.format("Total: %.2f | Hard: %.2f | Soft: %.2f",
                totalFitness, hardConstraintScore, softConstraintScore);
        }
    }

    /**
     * Constraint violation details
     */
    class ConstraintViolation {
        private ConstraintType constraintType;
        private String description;
        private List<ScheduleSlot> affectedSlots;
        private int severity; // Weight/penalty

        public ConstraintViolation(ConstraintType type, String description,
                                  List<ScheduleSlot> slots, int severity) {
            this.constraintType = type;
            this.description = description;
            this.affectedSlots = slots;
            this.severity = severity;
        }

        // Getters
        public ConstraintType getConstraintType() { return constraintType; }
        public String getDescription() { return description; }
        public List<ScheduleSlot> getAffectedSlots() { return affectedSlots; }
        public int getSeverity() { return severity; }

        public String getSummary() {
            return String.format("%s: %s (affects %d slots)",
                constraintType.getDisplayName(), description,
                affectedSlots != null ? affectedSlots.size() : 0);
        }
    }
}
