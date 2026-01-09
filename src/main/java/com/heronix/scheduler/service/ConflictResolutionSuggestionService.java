package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Conflict;
import com.heronix.scheduler.model.dto.ConflictResolutionSuggestion;
import com.heronix.scheduler.model.dto.ConflictPriorityScore;

import java.util.List;

/**
 * Conflict Resolution Suggestion Service Interface
 * Location: src/main/java/com/eduscheduler/service/ConflictResolutionSuggestionService.java
 *
 * AI-powered service for generating, analyzing, and applying conflict resolution suggestions.
 * Uses machine learning and historical data to provide optimal resolution recommendations.
 *
 * @author Heronix Scheduling System Team
 * @since Beta 1 Polish - November 26, 2025
 */
public interface ConflictResolutionSuggestionService {

    /**
     * Generate AI-powered resolution suggestions for a conflict
     *
     * Analyzes the conflict and generates multiple resolution options ranked by:
     * - Impact score (lower is better)
     * - Success probability (higher is better)
     * - Historical success rates
     * - AI confidence level
     *
     * @param conflict The conflict to analyze
     * @return List of suggestions ranked by priority (best first)
     */
    List<ConflictResolutionSuggestion> generateSuggestions(Conflict conflict);

    /**
     * Calculate priority score for a conflict
     *
     * Uses ML-based scoring to prioritize conflicts:
     * - Hard constraint violations
     * - Number of affected entities
     * - Cascade impact potential
     * - Historical difficulty
     * - Time sensitivity
     *
     * @param conflict The conflict to score
     * @return Priority score with detailed breakdown
     */
    ConflictPriorityScore calculatePriorityScore(Conflict conflict);

    /**
     * Apply a suggested resolution
     *
     * Executes the resolution actions and updates the database.
     * Records success/failure for ML learning.
     *
     * @param conflict The conflict to resolve
     * @param suggestion The suggestion to apply
     * @return True if successfully applied, false otherwise
     */
    boolean applySuggestion(Conflict conflict, ConflictResolutionSuggestion suggestion);

    /**
     * Get historical success rate for a resolution type
     *
     * Analyzes past resolutions to determine success probability.
     * Used for ML-based suggestion ranking.
     *
     * @param type The resolution type
     * @return Success rate percentage (0-100)
     */
    int getHistoricalSuccessRate(ConflictResolutionSuggestion.ResolutionType type);

    /**
     * Get all conflicts sorted by priority
     *
     * Returns conflicts ordered by their priority scores.
     * Critical conflicts appear first.
     *
     * @return List of conflicts sorted by priority (highest first)
     */
    List<Conflict> getConflictsByPriority();

    /**
     * Estimate cascade impact of a conflict
     *
     * Analyzes how many other entities could be affected if this
     * conflict cascades.
     *
     * @param conflict The conflict to analyze
     * @return Estimated number of potentially affected entities
     */
    int estimateCascadeImpact(Conflict conflict);

    /**
     * Check if suggestion can be auto-applied
     *
     * Determines if a suggestion is safe enough to apply automatically
     * without user confirmation.
     *
     * @param suggestion The suggestion to check
     * @return True if can be auto-applied, false if needs confirmation
     */
    boolean canAutoApply(ConflictResolutionSuggestion suggestion);
}
