package com.heronix.scheduler.model.domain;

import com.heronix.scheduler.model.enums.ConstraintType;
import com.heronix.scheduler.model.enums.OptimizationAlgorithm;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Optimization Configuration Entity
 *
 * Stores settings for schedule optimization
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Entity
@Table(name = "optimization_configs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OptimizationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_name", nullable = false)
    private String configName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm", nullable = false)
    @Builder.Default
    private OptimizationAlgorithm algorithm = OptimizationAlgorithm.GENETIC_ALGORITHM;

    @Column(name = "population_size")
    @Builder.Default
    private Integer populationSize = 100;

    @Column(name = "max_generations")
    @Builder.Default
    private Integer maxGenerations = 1000;

    @Column(name = "mutation_rate")
    @Builder.Default
    private Double mutationRate = 0.1;

    @Column(name = "crossover_rate")
    @Builder.Default
    private Double crossoverRate = 0.8;

    @Column(name = "elite_size")
    @Builder.Default
    private Integer eliteSize = 5;

    @Column(name = "tournament_size")
    @Builder.Default
    private Integer tournamentSize = 3;

    @Column(name = "max_runtime_seconds")
    @Builder.Default
    private Integer maxRuntimeSeconds = 300;

    @Column(name = "target_fitness_score")
    private Double targetFitnessScore;

    @Column(name = "enable_parallel_processing")
    @Builder.Default
    private Boolean enableParallelProcessing = true;

    @Column(name = "thread_pool_size")
    @Builder.Default
    private Integer threadPoolSize = 4;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "active")
    @Builder.Default
    private Boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;

    @Column(name = "modified_by")
    private String modifiedBy;

    @Column(name = "log_frequency")
    @Builder.Default
    private Integer logFrequency = 10;

    @Column(name = "stagnation_limit")
    @Builder.Default
    private Integer stagnationLimit = 50;

    @Transient
    @Builder.Default
    private Map<ConstraintType, Double> constraintWeights = new HashMap<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        modifiedAt = createdAt;
        initializeDefaultWeights();
    }

    @PreUpdate
    protected void onUpdate() {
        modifiedAt = LocalDateTime.now();
    }

    @PostLoad
    protected void onLoad() {
        initializeDefaultWeights();
    }

    /**
     * Initialize default constraint weights
     */
    private void initializeDefaultWeights() {
        if (constraintWeights == null) {
            constraintWeights = new HashMap<>();
        }
        // Set default weights if not already set
        if (constraintWeights.isEmpty()) {
            constraintWeights.put(ConstraintType.NO_TEACHER_OVERLAP, 1000.0);
            constraintWeights.put(ConstraintType.NO_ROOM_OVERLAP, 1000.0);
            constraintWeights.put(ConstraintType.NO_STUDENT_OVERLAP, 1000.0);
            constraintWeights.put(ConstraintType.TEACHER_PREFERENCES, 10.0);
            constraintWeights.put(ConstraintType.ROOM_PREFERENCES, 5.0);
            constraintWeights.put(ConstraintType.MINIMIZE_TEACHER_TRAVEL, 5.0);
        }
    }

    /**
     * Get log frequency (how often to log progress)
     */
    public Integer getLogFrequency() {
        return logFrequency != null ? logFrequency : 10;
    }

    /**
     * Get stagnation limit (generations without improvement before stopping)
     */
    public Integer getStagnationLimit() {
        return stagnationLimit != null ? stagnationLimit : 50;
    }

    /**
     * Get target fitness score
     */
    public Double getTargetFitness() {
        return targetFitnessScore;
    }

    /**
     * Get constraint weight for a specific constraint type
     */
    public Double getConstraintWeight(ConstraintType constraintType) {
        if (constraintWeights == null) {
            initializeDefaultWeights();
        }
        return constraintWeights.getOrDefault(constraintType, 1.0);
    }

    /**
     * Set constraint weight for a specific constraint type
     */
    public void setConstraintWeight(ConstraintType constraintType, Double weight) {
        if (constraintWeights == null) {
            constraintWeights = new HashMap<>();
        }
        constraintWeights.put(constraintType, weight);
    }
}
