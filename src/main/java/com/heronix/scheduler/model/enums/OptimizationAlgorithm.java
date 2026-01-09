package com.heronix.scheduler.model.enums;

/**
 * Optimization Algorithm Enum
 *
 * Defines available optimization algorithms
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
public enum OptimizationAlgorithm {

    GENETIC_ALGORITHM(
        "Genetic Algorithm",
        "Evolution-based optimization using crossover and mutation",
        AlgorithmType.EVOLUTIONARY
    ),

    SIMULATED_ANNEALING(
        "Simulated Annealing",
        "Probabilistic technique inspired by metallurgy",
        AlgorithmType.LOCAL_SEARCH
    ),

    TABU_SEARCH(
        "Tabu Search",
        "Local search with memory to avoid revisiting solutions",
        AlgorithmType.LOCAL_SEARCH
    ),

    HILL_CLIMBING(
        "Hill Climbing",
        "Simple greedy algorithm that always improves",
        AlgorithmType.LOCAL_SEARCH
    ),

    CONSTRAINT_PROGRAMMING(
        "Constraint Programming",
        "Logic programming approach to constraint satisfaction",
        AlgorithmType.EXACT
    ),

    HYBRID(
        "Hybrid Algorithm",
        "Combines genetic algorithm with local search",
        AlgorithmType.HYBRID
    );

    private final String displayName;
    private final String description;
    private final AlgorithmType type;

    public enum AlgorithmType {
        EVOLUTIONARY("Evolutionary", "Population-based algorithms"),
        LOCAL_SEARCH("Local Search", "Neighborhood-based algorithms"),
        EXACT("Exact", "Guarantees optimal solution"),
        HYBRID("Hybrid", "Combines multiple approaches");

        private final String displayName;
        private final String description;

        AlgorithmType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }

    OptimizationAlgorithm(String displayName, String description, AlgorithmType type) {
        this.displayName = displayName;
        this.description = description;
        this.type = type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public AlgorithmType getType() {
        return type;
    }

    public int getRecommendedPopulationSize() {
        return switch (this) {
            case GENETIC_ALGORITHM -> 100;
            case HYBRID -> 50;
            default -> 1;
        };
    }

    public int getRecommendedIterations() {
        return switch (this) {
            case GENETIC_ALGORITHM -> 1000;
            case SIMULATED_ANNEALING -> 10000;
            case TABU_SEARCH -> 5000;
            case HILL_CLIMBING -> 1000;
            case CONSTRAINT_PROGRAMMING -> 100;
            case HYBRID -> 500;
        };
    }

    public boolean usesPopulation() {
        return type == AlgorithmType.EVOLUTIONARY || type == AlgorithmType.HYBRID;
    }

    public boolean supportsParallelExecution() {
        return this == GENETIC_ALGORITHM || this == HYBRID;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
