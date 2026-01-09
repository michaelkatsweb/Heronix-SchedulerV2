package com.heronix.scheduler.model.enums;

/**
 * Methods for assigning students to lunch waves
 *
 * Real-world examples:
 * - Weeki Wachee HS: Uses MANUAL (students assigned by name to Lunch 1, 2, or 3)
 * - Parrott MS: Uses BY_GRADE_LEVEL (6th, 7th, 8th grades get separate lunches)
 * - Many schools: Use ALPHABETICAL (A-H → Lunch 1, I-P → Lunch 2, Q-Z → Lunch 3)
 *
 * Phase 5A: Multiple Rotating Lunch Periods
 * Date: December 1, 2025
 */
public enum LunchAssignmentMethod {

    /**
     * Assign by grade level
     * Example: All 9th graders → Lunch 1, All 10th graders → Lunch 2, etc.
     * Used by: Parrott Middle School (Grade 6, 7, 8 lunches)
     */
    BY_GRADE_LEVEL("By Grade Level", "Assign students based on their grade level"),

    /**
     * Assign alphabetically by last name
     * Example: A-H → Lunch 1, I-P → Lunch 2, Q-Z → Lunch 3
     * Common in large schools to evenly distribute students
     */
    ALPHABETICAL("Alphabetical", "Distribute students alphabetically by last name"),

    /**
     * Assign by student ID ranges
     * Example: IDs 1000-1999 → Lunch 1, 2000-2999 → Lunch 2, etc.
     * Useful when student IDs are assigned by grade or enrollment year
     */
    BY_STUDENT_ID("By Student ID", "Assign based on student ID number ranges"),

    /**
     * Random assignment
     * Distributes students randomly across lunch waves
     * Good for mixing grade levels and social groups
     */
    RANDOM("Random", "Randomly distribute students across lunch waves"),

    /**
     * Manual assignment
     * Administrator manually assigns each student to a specific lunch wave
     * Example: Weeki Wachee High School (assigns students by name)
     * Most flexible but labor-intensive
     */
    MANUAL("Manual", "Administrator manually assigns each student"),

    /**
     * AI-optimized assignment
     * Uses OptaPlanner to optimize lunch assignments based on:
     * - Balancing lunch wave sizes
     * - Keeping friend groups together
     * - Minimizing conflicts with preferred courses
     * - Teacher availability for supervision
     */
    OPTIMIZED("AI-Optimized", "Use AI to optimize lunch wave assignments"),

    /**
     * Assign by homeroom/advisory
     * All students in the same homeroom go to the same lunch
     * Simplifies supervision and maintains class cohesion
     */
    BY_HOMEROOM("By Homeroom", "Assign students based on their homeroom/advisory"),

    /**
     * Assign by cohort (Academy, House, Program)
     * Example: STEM Academy → Lunch 1, Arts Academy → Lunch 2
     * Used in schools with specialized programs
     */
    BY_COHORT("By Cohort", "Assign students based on their academy/program"),

    /**
     * Assign to balance cafeteria capacity
     * Automatically distributes students to balance wave sizes
     * Considers max capacity constraints
     */
    BALANCED("Balanced", "Automatically balance students across lunch waves");

    private final String displayName;
    private final String description;

    LunchAssignmentMethod(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this method requires manual intervention
     */
    public boolean isManual() {
        return this == MANUAL;
    }

    /**
     * Check if this method can be fully automated
     */
    public boolean isAutomated() {
        return this != MANUAL;
    }

    /**
     * Check if this method uses AI optimization
     */
    public boolean usesAI() {
        return this == OPTIMIZED;
    }

    /**
     * Get user-friendly description for UI tooltips
     */
    public String getTooltip() {
        return String.format("%s: %s", displayName, description);
    }

    @Override
    public String toString() {
        return displayName;
    }
}
