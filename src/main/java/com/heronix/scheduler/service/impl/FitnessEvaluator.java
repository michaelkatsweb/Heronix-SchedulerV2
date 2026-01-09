package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.ConflictSeverity;
import com.heronix.scheduler.model.enums.ConflictType;
import com.heronix.scheduler.model.enums.ConstraintType;
import com.heronix.scheduler.service.ConflictDetectorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fitness Evaluator
 * Evaluates schedule quality using conflict detection
 *
 * Location: src/main/java/com/eduscheduler/service/impl/FitnessEvaluator.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
@Component
@Slf4j
public class FitnessEvaluator {

    private final ConflictDetectorService conflictDetector;

    // Base scores
    private static final double PERFECT_SCORE = 10000.0;
    private static final double CRITICAL_PENALTY = 1000.0;
    private static final double HIGH_PENALTY = 100.0;
    private static final double MEDIUM_PENALTY = 10.0;
    private static final double LOW_PENALTY = 1.0;

    public FitnessEvaluator(ConflictDetectorService conflictDetector) {
        this.conflictDetector = conflictDetector;
    }

    // ========================================================================
    // FITNESS EVALUATION
    // ========================================================================

    /**
     * Evaluate fitness of a schedule
     * Higher score = better schedule
     */
    public double evaluate(Schedule schedule, OptimizationConfig config) {
        // ✅ NULL SAFE: Validate parameters
        if (schedule == null) {
            log.warn("Cannot evaluate null schedule");
            return 0.0;
        }

        double fitness = PERFECT_SCORE;

        try {
            // Detect all conflicts
            List<Conflict> conflicts = conflictDetector.detectAllConflicts(schedule);
            // ✅ NULL SAFE: Validate conflicts list
            if (conflicts == null) {
                conflicts = java.util.Collections.emptyList();
            }

            // Calculate conflict penalties
            for (Conflict conflict : conflicts) {
                // ✅ NULL SAFE: Skip null conflicts
                if (conflict == null) continue;
                if (conflict.getIsResolved() || conflict.getIsIgnored()) {
                    continue; // Skip resolved/ignored conflicts
                }

                double penalty = calculateConflictPenalty(conflict, config);
                fitness -= penalty;
            }

            // Add bonuses for good attributes
            fitness += calculateUtilizationBonus(schedule, config);
            fitness += calculateBalanceBonus(schedule, config);

            // Ensure non-negative
            fitness = Math.max(0, fitness);

        } catch (Exception e) {
            log.error("Error evaluating fitness for schedule: {}", schedule.getId(), e);
            fitness = 0.0;
        }

        return fitness;
    }

    /**
     * Get fitness breakdown by constraint
     */
    public FitnessBreakdown getBreakdown(Schedule schedule, OptimizationConfig config) {
        FitnessBreakdown breakdown = new FitnessBreakdown();

        try {
            List<Conflict> conflicts = conflictDetector.detectAllConflicts(schedule);

            double totalFitness = PERFECT_SCORE;
            double hardScore = 0;
            double softScore = 0;

            Map<ConstraintType, Integer> violationCounts = new HashMap<>();
            Map<ConstraintType, Double> scores = new HashMap<>();

            for (Conflict conflict : conflicts) {
                if (conflict.getIsResolved() || conflict.getIsIgnored()) {
                    continue;
                }

                ConstraintType constraintType = mapConflictToConstraint(conflict.getConflictType());
                double penalty = calculateConflictPenalty(conflict, config);

                violationCounts.merge(constraintType, 1, Integer::sum);
                scores.merge(constraintType, -penalty, Double::sum);

                totalFitness -= penalty;

                if (constraintType.isHard()) {
                    hardScore -= penalty;
                } else {
                    softScore -= penalty;
                }
            }

            breakdown.setTotalFitness(Math.max(0, totalFitness));
            breakdown.setHardConstraintScore(hardScore);
            breakdown.setSoftConstraintScore(softScore);

            for (Map.Entry<ConstraintType, Integer> entry : violationCounts.entrySet()) {
                breakdown.addConstraintScore(
                    entry.getKey(),
                    scores.getOrDefault(entry.getKey(), 0.0),
                    entry.getValue()
                );
            }

        } catch (Exception e) {
            log.error("Error getting fitness breakdown", e);
        }

        return breakdown;
    }

    /**
     * Get conflict count for a schedule
     */
    public int getConflictCount(Schedule schedule) {
        try {
            List<Conflict> conflicts = conflictDetector.detectAllConflicts(schedule);
            return (int) conflicts.stream()
                .filter(c -> !c.getIsResolved() && !c.getIsIgnored())
                .count();
        } catch (Exception e) {
            log.error("Error counting conflicts", e);
            return Integer.MAX_VALUE; // Worst case
        }
    }

    // ========================================================================
    // PENALTY CALCULATION
    // ========================================================================

    private double calculateConflictPenalty(Conflict conflict, OptimizationConfig config) {
        // Base penalty by severity
        double basePenalty = switch (conflict.getSeverity()) {
            case CRITICAL -> CRITICAL_PENALTY;
            case HIGH -> HIGH_PENALTY;
            case MEDIUM -> MEDIUM_PENALTY;
            case LOW -> LOW_PENALTY;
            case INFO -> 0.0;
        };

        // Apply constraint weight
        ConstraintType constraintType = mapConflictToConstraint(conflict.getConflictType());
        Double weight = config.getConstraintWeight(constraintType);
        double weightValue = (weight != null) ? weight : 100.0;

        // Calculate final penalty
        double penalty = basePenalty * (weightValue / 100.0);

        // Multiply by affected entities count (more impact = higher penalty)
        int affectedCount = conflict.getAffectedEntitiesCount();
        if (affectedCount > 1) {
            penalty *= Math.log(affectedCount + 1);
        }

        return penalty;
    }

    // ========================================================================
    // BONUS CALCULATION
    // ========================================================================

    private double calculateUtilizationBonus(Schedule schedule, OptimizationConfig config) {
        double bonus = 0.0;

        try {
            List<ScheduleSlot> slots = schedule.getSlots();
            if (slots == null || slots.isEmpty()) {
                return bonus;
            }

            // Calculate room utilization: count unique rooms used vs total slots
            Map<Long, Integer> roomUsage = new HashMap<>();
            for (ScheduleSlot slot : slots) {
                if (slot.getRoom() != null) {
                    roomUsage.merge(slot.getRoom().getId(), 1, Integer::sum);
                }
            }

            // Reward balanced room usage (not overusing single rooms)
            if (!roomUsage.isEmpty()) {
                double avgUsage = slots.size() / (double) roomUsage.size();
                double variance = roomUsage.values().stream()
                    .mapToDouble(usage -> Math.pow(usage - avgUsage, 2))
                    .average()
                    .orElse(0.0);

                // Lower variance = better distribution = higher bonus
                double utilizationScore = 1.0 / (1.0 + Math.sqrt(variance));
                bonus += utilizationScore * 50; // Max 50 points bonus
                log.debug("Room utilization bonus: {} (variance: {})", bonus, variance);
            }
        } catch (Exception e) {
            log.debug("Error calculating utilization bonus", e);
        }

        return bonus;
    }

    private double calculateBalanceBonus(Schedule schedule, OptimizationConfig config) {
        double bonus = 0.0;

        try {
            List<ScheduleSlot> slots = schedule.getSlots();
            if (slots == null || slots.isEmpty()) {
                return bonus;
            }

            // Calculate teacher workload balance
            Map<Long, Integer> teacherSlotCount = new HashMap<>();
            for (ScheduleSlot slot : slots) {
                if (slot.getTeacher() != null) {
                    teacherSlotCount.merge(slot.getTeacher().getId(), 1, Integer::sum);
                }
            }

            // Reward balanced teacher workloads
            if (!teacherSlotCount.isEmpty()) {
                double avgSlots = slots.size() / (double) teacherSlotCount.size();
                double variance = teacherSlotCount.values().stream()
                    .mapToDouble(count -> Math.pow(count - avgSlots, 2))
                    .average()
                    .orElse(0.0);

                // Lower variance = better balance = higher bonus
                double balanceScore = 1.0 / (1.0 + Math.sqrt(variance));
                bonus += balanceScore * 50; // Max 50 points bonus
                log.debug("Teacher balance bonus: {} (variance: {})", bonus, variance);
            }
        } catch (Exception e) {
            log.debug("Error calculating balance bonus", e);
        }

        return bonus;
    }

    // ========================================================================
    // CONSTRAINT MAPPING
    // ========================================================================

    /**
     * Map ConflictType to ConstraintType
     */
    private ConstraintType mapConflictToConstraint(ConflictType conflictType) {
        return switch (conflictType) {
            // Hard constraints
            case TEACHER_OVERLOAD -> ConstraintType.NO_TEACHER_OVERLAP;
            case ROOM_DOUBLE_BOOKING -> ConstraintType.NO_ROOM_OVERLAP;
            case STUDENT_SCHEDULE_CONFLICT -> ConstraintType.NO_STUDENT_OVERLAP;
            case ROOM_CAPACITY_EXCEEDED -> ConstraintType.ROOM_CAPACITY;
            case SUBJECT_MISMATCH -> ConstraintType.TEACHER_QUALIFICATION;
            case EQUIPMENT_UNAVAILABLE -> ConstraintType.EQUIPMENT_AVAILABLE;

            // Soft constraints
            case NO_LUNCH_BREAK -> ConstraintType.LUNCH_BREAK;
            case TEACHER_TRAVEL_TIME -> ConstraintType.MINIMIZE_TEACHER_TRAVEL;
            case STUDENT_TRAVEL_TIME -> ConstraintType.MINIMIZE_STUDENT_TRAVEL;
            case NO_PREPARATION_PERIOD -> ConstraintType.TEACHER_PREP_PERIODS;
            case EXCESSIVE_TEACHING_HOURS -> ConstraintType.BALANCE_TEACHER_LOAD;
            case ROOM_TYPE_MISMATCH -> ConstraintType.ROOM_PREFERENCES;
            case SECTION_OVER_ENROLLED, SECTION_UNDER_ENROLLED -> ConstraintType.BALANCE_CLASS_SIZES;

            // Default to soft constraint
            default -> ConstraintType.MINIMIZE_STUDENT_GAPS;
        };
    }

    // ========================================================================
    // SUPPORTING CLASSES
    // ========================================================================

    /**
     * Fitness breakdown by constraint
     */
    public static class FitnessBreakdown {
        private double totalFitness;
        private double hardConstraintScore;
        private double softConstraintScore;
        private Map<ConstraintType, Double> constraintScores = new HashMap<>();
        private Map<ConstraintType, Integer> violationCounts = new HashMap<>();

        public double getTotalFitness() { return totalFitness; }
        public void setTotalFitness(double fitness) { this.totalFitness = fitness; }

        public double getHardConstraintScore() { return hardConstraintScore; }
        public void setHardConstraintScore(double score) { this.hardConstraintScore = score; }

        public double getSoftConstraintScore() { return softConstraintScore; }
        public void setSoftConstraintScore(double score) { this.softConstraintScore = score; }

        public Map<ConstraintType, Double> getConstraintScores() { return constraintScores; }
        public Map<ConstraintType, Integer> getViolationCounts() { return violationCounts; }

        public void addConstraintScore(ConstraintType type, double score, int violations) {
            constraintScores.put(type, score);
            violationCounts.put(type, violations);
        }

        public String getSummary() {
            return String.format("Total: %.2f | Hard: %.2f | Soft: %.2f | Violations: %d",
                totalFitness, hardConstraintScore, softConstraintScore,
                violationCounts.values().stream().mapToInt(Integer::intValue).sum());
        }
    }
}
