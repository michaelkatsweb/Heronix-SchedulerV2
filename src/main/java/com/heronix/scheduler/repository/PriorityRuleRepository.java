package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.PriorityRule;
import com.heronix.scheduler.model.enums.PriorityRuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PriorityRule Entity
 *
 * Provides queries for administrator-configurable priority rules:
 * - Finding active rules
 * - Filtering by rule type
 * - Weight-based ordering
 * - Rule evaluation queries
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 4 - November 20, 2025
 */
@Repository
public interface PriorityRuleRepository extends JpaRepository<PriorityRule, Long> {

    // ========================================================================
    // ACTIVE RULE QUERIES
    // ========================================================================

    /**
     * Find all active rules, ordered by weight (highest first)
     * This is the main query used by the assignment algorithm
     */
    List<PriorityRule> findByActiveTrueOrderByWeightDesc();

    /**
     * Find all active rules (unordered)
     */
    List<PriorityRule> findByActiveTrue();

    /**
     * Find all inactive rules
     */
    List<PriorityRule> findByActiveFalse();

    /**
     * Count active rules
     */
    long countByActiveTrue();

    // ========================================================================
    // RULE TYPE QUERIES
    // ========================================================================

    /**
     * Find all rules of a specific type
     */
    List<PriorityRule> findByRuleType(PriorityRuleType ruleType);

    /**
     * Find active rules of a specific type
     */
    List<PriorityRule> findByRuleTypeAndActiveTrueOrderByWeightDesc(PriorityRuleType ruleType);

    /**
     * Count rules by type
     */
    long countByRuleType(PriorityRuleType ruleType);

    // ========================================================================
    // RULE NAME QUERIES
    // ========================================================================

    /**
     * Find rule by exact name
     */
    Optional<PriorityRule> findByRuleName(String ruleName);

    /**
     * Check if rule name already exists
     */
    boolean existsByRuleName(String ruleName);

    /**
     * Find rules with name containing text (case-insensitive)
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE LOWER(pr.ruleName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<PriorityRule> searchByRuleName(@Param("searchTerm") String searchTerm);

    // ========================================================================
    // WEIGHT-BASED QUERIES
    // ========================================================================

    /**
     * Find rules with weight greater than or equal to threshold
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.weight >= :minWeight AND pr.active = true " +
           "ORDER BY pr.weight DESC")
    List<PriorityRule> findHighPriorityRules(@Param("minWeight") Integer minWeight);

    /**
     * Find rules with weight in a specific range
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.weight BETWEEN :minWeight AND :maxWeight " +
           "ORDER BY pr.weight DESC")
    List<PriorityRule> findRulesByWeightRange(
        @Param("minWeight") Integer minWeight,
        @Param("maxWeight") Integer maxWeight
    );

    // ========================================================================
    // GPA-BASED RULE QUERIES
    // ========================================================================

    /**
     * Find GPA threshold rules
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.ruleType = 'GPA_THRESHOLD' " +
           "AND pr.active = true ORDER BY pr.minGPAThreshold DESC")
    List<PriorityRule> findGPAThresholdRules();

    /**
     * Find applicable GPA rules for a specific GPA
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.ruleType = 'GPA_THRESHOLD' " +
           "AND pr.active = true " +
           "AND (pr.minGPAThreshold IS NULL OR :gpa >= pr.minGPAThreshold) " +
           "AND (pr.maxGPAThreshold IS NULL OR :gpa <= pr.maxGPAThreshold) " +
           "ORDER BY pr.weight DESC")
    List<PriorityRule> findApplicableGPARules(@Param("gpa") Double gpa);

    // ========================================================================
    // BEHAVIOR-BASED RULE QUERIES
    // ========================================================================

    /**
     * Find behavior-based rules
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.ruleType = 'BEHAVIOR_BASED' " +
           "AND pr.active = true ORDER BY pr.minBehaviorScore DESC")
    List<PriorityRule> findBehaviorBasedRules();

    /**
     * Find applicable behavior rules for a specific behavior score
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.ruleType = 'BEHAVIOR_BASED' " +
           "AND pr.active = true " +
           "AND (pr.minBehaviorScore IS NULL OR :behaviorScore >= pr.minBehaviorScore) " +
           "ORDER BY pr.weight DESC")
    List<PriorityRule> findApplicableBehaviorRules(@Param("behaviorScore") Integer behaviorScore);

    // ========================================================================
    // GRADE LEVEL QUERIES
    // ========================================================================

    /**
     * Find rules that apply to a specific grade level
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.active = true " +
           "AND (pr.gradeLevels IS NULL OR pr.gradeLevels LIKE CONCAT('%', :gradeLevel, '%')) " +
           "ORDER BY pr.weight DESC")
    List<PriorityRule> findRulesForGradeLevel(@Param("gradeLevel") String gradeLevel);

    /**
     * Find seniority rules
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.ruleType = 'SENIORITY' " +
           "AND pr.active = true ORDER BY pr.weight DESC")
    List<PriorityRule> findSeniorityRules();

    // ========================================================================
    // SPECIAL POPULATION QUERIES
    // ========================================================================

    /**
     * Find rules that apply to IEP students
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.active = true " +
           "AND pr.applyToIEP = true ORDER BY pr.weight DESC")
    List<PriorityRule> findIEPRules();

    /**
     * Find rules that apply to 504 students
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.active = true " +
           "AND pr.applyTo504 = true ORDER BY pr.weight DESC")
    List<PriorityRule> find504Rules();

    /**
     * Find rules that apply to gifted students
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.active = true " +
           "AND pr.applyToGifted = true ORDER BY pr.weight DESC")
    List<PriorityRule> findGiftedRules();

    /**
     * Find all special needs rules (IEP or 504)
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.ruleType = 'SPECIAL_NEEDS' " +
           "AND pr.active = true ORDER BY pr.weight DESC")
    List<PriorityRule> findSpecialNeedsRules();

    // ========================================================================
    // BONUS POINTS QUERIES
    // ========================================================================

    /**
     * Find rules with highest bonus points
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.active = true " +
           "ORDER BY pr.bonusPoints DESC")
    List<PriorityRule> findRulesOrderedByBonusPoints();

    /**
     * Find rules with bonus points >= threshold
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.active = true " +
           "AND pr.bonusPoints >= :minBonus ORDER BY pr.bonusPoints DESC")
    List<PriorityRule> findRulesWithMinimumBonus(@Param("minBonus") Integer minBonus);

    /**
     * Calculate total possible bonus points from all active rules
     */
    @Query("SELECT SUM(pr.bonusPoints) FROM PriorityRule pr WHERE pr.active = true")
    Integer getTotalPossibleBonusPoints();

    // ========================================================================
    // CUSTOM RULE QUERIES
    // ========================================================================

    /**
     * Find all custom rules
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.ruleType = 'CUSTOM' " +
           "AND pr.active = true ORDER BY pr.weight DESC")
    List<PriorityRule> findCustomRules();

    /**
     * Find rules with custom conditions
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.customCondition IS NOT NULL " +
           "AND pr.active = true ORDER BY pr.weight DESC")
    List<PriorityRule> findRulesWithCustomConditions();

    // ========================================================================
    // AUDIT QUERIES
    // ========================================================================

    /**
     * Find rules created by a specific user
     */
    List<PriorityRule> findByCreatedBy(String createdBy);

    /**
     * Find rules modified by a specific user
     */
    List<PriorityRule> findByModifiedBy(String modifiedBy);

    /**
     * Find recently created rules (last N)
     */
    @Query("SELECT pr FROM PriorityRule pr ORDER BY pr.createdAt DESC LIMIT :limit")
    List<PriorityRule> findRecentlyCreatedRules(@Param("limit") int limit);

    /**
     * Find recently modified rules (last N)
     */
    @Query("SELECT pr FROM PriorityRule pr WHERE pr.modifiedAt IS NOT NULL " +
           "ORDER BY pr.modifiedAt DESC LIMIT :limit")
    List<PriorityRule> findRecentlyModifiedRules(@Param("limit") int limit);

    // ========================================================================
    // STATISTICS QUERIES
    // ========================================================================

    /**
     * Count rules by type
     */
    @Query("SELECT pr.ruleType, COUNT(pr) FROM PriorityRule pr GROUP BY pr.ruleType")
    List<Object[]> countRulesByType();

    /**
     * Get average weight by rule type
     */
    @Query("SELECT pr.ruleType, AVG(pr.weight) FROM PriorityRule pr " +
           "WHERE pr.active = true GROUP BY pr.ruleType")
    List<Object[]> getAverageWeightByType();

    /**
     * Get average bonus points by rule type
     */
    @Query("SELECT pr.ruleType, AVG(pr.bonusPoints) FROM PriorityRule pr " +
           "WHERE pr.active = true GROUP BY pr.ruleType")
    List<Object[]> getAverageBonusByType();

    /**
     * Count active vs inactive rules
     */
    @Query("SELECT pr.active, COUNT(pr) FROM PriorityRule pr GROUP BY pr.active")
    List<Object[]> countByActiveStatus();

    // ========================================================================
    // BULK OPERATIONS
    // ========================================================================

    /**
     * Activate/deactivate multiple rules by type
     */
    @Query("UPDATE PriorityRule pr SET pr.active = :active WHERE pr.ruleType = :ruleType")
    void setActiveByRuleType(@Param("ruleType") PriorityRuleType ruleType, @Param("active") Boolean active);

    /**
     * Delete inactive rules
     */
    void deleteByActiveFalse();

    // ========================================================================
    // VALIDATION QUERIES
    // ========================================================================

    /**
     * Check if there are any active rules
     */
    boolean existsByActiveTrue();

    /**
     * Check if there are any GPA-based rules
     */
    @Query("SELECT CASE WHEN COUNT(pr) > 0 THEN true ELSE false END " +
           "FROM PriorityRule pr WHERE pr.ruleType = 'GPA_THRESHOLD' AND pr.active = true")
    boolean existsActiveGPARules();

    /**
     * Check if there are conflicting GPA rules (overlapping ranges)
     */
    @Query("SELECT pr1.id, pr2.id FROM PriorityRule pr1, PriorityRule pr2 " +
           "WHERE pr1.id < pr2.id " +
           "AND pr1.ruleType = 'GPA_THRESHOLD' AND pr2.ruleType = 'GPA_THRESHOLD' " +
           "AND pr1.active = true AND pr2.active = true " +
           "AND pr1.minGPAThreshold IS NOT NULL AND pr2.minGPAThreshold IS NOT NULL " +
           "AND pr1.maxGPAThreshold IS NOT NULL AND pr2.maxGPAThreshold IS NOT NULL " +
           "AND NOT (pr1.maxGPAThreshold < pr2.minGPAThreshold OR pr2.maxGPAThreshold < pr1.minGPAThreshold)")
    List<Object[]> findConflictingGPARules();
}
