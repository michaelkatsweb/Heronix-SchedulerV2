package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.OptimizationAlgorithm;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Optimization Result Entity
 * Stores results of optimization runs
 *
 * Location: src/main/java/com/eduscheduler/model/domain/OptimizationResult.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
@Entity
@Table(name = "optimization_results", indexes = {
    @Index(name = "idx_optimization_schedule", columnList = "schedule_id"),
    @Index(name = "idx_optimization_status", columnList = "status"),
    @Index(name = "idx_optimization_started", columnList = "started_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ========================================================================
    // OPTIMIZATION RUN DETAILS
    // ========================================================================

    /**
     * The schedule that was optimized
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    /**
     * Configuration used for optimization
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "config_id")
    private OptimizationConfig config;

    /**
     * Algorithm used
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm", nullable = false)
    private OptimizationAlgorithm algorithm;

    /**
     * Run status
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OptimizationStatus status = OptimizationStatus.PENDING;

    // ========================================================================
    // TIMING
    // ========================================================================

    /**
     * When optimization started
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * When optimization completed
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Runtime in seconds
     */
    @Column(name = "runtime_seconds")
    private Long runtimeSeconds;

    // ========================================================================
    // RESULTS
    // ========================================================================

    /**
     * Initial fitness score (before optimization)
     */
    @Column(name = "initial_fitness")
    private Double initialFitness;

    /**
     * Final fitness score (after optimization)
     */
    @Column(name = "final_fitness")
    private Double finalFitness;

    /**
     * Best fitness achieved during run
     */
    @Column(name = "best_fitness")
    private Double bestFitness;

    /**
     * Fitness improvement percentage
     */
    @Column(name = "improvement_percentage")
    private Double improvementPercentage;

    /**
     * Number of generations/iterations executed
     */
    @Column(name = "generations_executed")
    private Integer generationsExecuted;

    /**
     * Generation where best solution was found
     */
    @Column(name = "best_generation")
    private Integer bestGeneration;

    // ========================================================================
    // CONFLICT STATISTICS
    // ========================================================================

    /**
     * Initial number of conflicts
     */
    @Column(name = "initial_conflicts")
    private Integer initialConflicts;

    /**
     * Final number of conflicts
     */
    @Column(name = "final_conflicts")
    private Integer finalConflicts;

    /**
     * Critical conflicts remaining
     */
    @Column(name = "critical_conflicts")
    private Integer criticalConflicts;

    /**
     * Hard constraint violations
     */
    @Column(name = "hard_violations")
    private Integer hardViolations;

    /**
     * Soft constraint violations
     */
    @Column(name = "soft_violations")
    private Integer softViolations;

    // ========================================================================
    // ADDITIONAL METRICS
    // ========================================================================

    /**
     * Average teacher utilization rate
     */
    @Column(name = "teacher_utilization")
    private Double teacherUtilization;

    /**
     * Average room utilization rate
     */
    @Column(name = "room_utilization")
    private Double roomUtilization;

    /**
     * Number of schedule changes made
     */
    @Column(name = "changes_made")
    private Integer changesMade;

    /**
     * Average class size
     */
    @Column(name = "average_class_size")
    private Double averageClassSize;

    // ========================================================================
    // LOGS AND MESSAGES
    // ========================================================================

    /**
     * Success/error message
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /**
     * Detailed log of optimization process
     */
    @Column(name = "optimization_log", columnDefinition = "TEXT")
    private String optimizationLog;

    /**
     * Error details (if failed)
     */
    @Column(name = "error_details", columnDefinition = "TEXT")
    private String errorDetails;

    // ========================================================================
    // METADATA
    // ========================================================================

    /**
     * User who initiated optimization
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiated_by_user_id")
    private User initiatedBy;

    /**
     * Was this optimization successful?
     */
    @Column(name = "successful")
    private Boolean successful;

    /**
     * Creation timestamp
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    // ========================================================================
    // OPTIMIZATION STATUS ENUM
    // ========================================================================

    public enum OptimizationStatus {
        PENDING("Pending", "Waiting to start"),
        RUNNING("Running", "Optimization in progress"),
        COMPLETED("Completed", "Optimization finished successfully"),
        FAILED("Failed", "Optimization failed"),
        CANCELLED("Cancelled", "Optimization was cancelled"),
        TIMEOUT("Timeout", "Optimization exceeded time limit");

        private final String displayName;
        private final String description;

        OptimizationStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }

        public boolean isTerminal() {
            return this != PENDING && this != RUNNING;
        }
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Mark optimization as started
     */
    public void markStarted() {
        this.status = OptimizationStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    /**
     * Mark optimization as completed
     */
    public void markCompleted(boolean success) {
        this.status = success ? OptimizationStatus.COMPLETED : OptimizationStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.successful = success;
        if (startedAt != null) {
            this.runtimeSeconds = java.time.Duration.between(startedAt, completedAt).getSeconds();
        }
    }

    /**
     * Calculate improvement percentage
     */
    public void calculateImprovement() {
        if (initialFitness != null && finalFitness != null && initialFitness > 0) {
            this.improvementPercentage = ((finalFitness - initialFitness) / initialFitness) * 100.0;
        }
    }

    /**
     * Add log entry
     */
    public void appendLog(String entry) {
        if (optimizationLog == null) {
            optimizationLog = "";
        }
        optimizationLog += LocalDateTime.now() + ": " + entry + "\n";
    }

    /**
     * Get summary string
     */
    public String getSummary() {
        return String.format("%s: %s -> %s (%.1f%% improvement) in %d generations",
            algorithm.getDisplayName(),
            initialFitness != null ? String.format("%.2f", initialFitness) : "N/A",
            finalFitness != null ? String.format("%.2f", finalFitness) : "N/A",
            improvementPercentage != null ? improvementPercentage : 0.0,
            generationsExecuted != null ? generationsExecuted : 0
        );
    }

    /**
     * Check if optimization was successful
     */
    public boolean wasSuccessful() {
        return successful != null && successful && status == OptimizationStatus.COMPLETED;
    }
}
