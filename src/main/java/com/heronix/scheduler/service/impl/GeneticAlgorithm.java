package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.ConstraintType;
import lombok.extern.slf4j.Slf4j;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Genetic Algorithm Implementation
 * Evolution-based schedule optimization
 *
 * Location: src/main/java/com/eduscheduler/service/impl/GeneticAlgorithm.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
@Slf4j
public class GeneticAlgorithm {

    private final OptimizationConfig config;
    private final FitnessEvaluator fitnessEvaluator;
    private final Random random;

    private List<Individual> population;
    private Individual bestSolution;
    private double bestFitness = Double.NEGATIVE_INFINITY;
    private int generationsSinceImprovement = 0;
    private boolean cancelled = false;

    // ========================================================================
    // CONSTRUCTOR
    // ========================================================================

    public GeneticAlgorithm(OptimizationConfig config, FitnessEvaluator fitnessEvaluator) {
        this.config = config;
        this.fitnessEvaluator = fitnessEvaluator;
        this.random = new Random();
        this.population = new ArrayList<>();
    }

    // ========================================================================
    // MAIN ALGORITHM
    // ========================================================================

    /**
     * Run the genetic algorithm
     */
    public OptimizationResult run(Schedule schedule,
                                  java.util.function.Consumer<GAProgress> progressCallback) {
        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }

        log.info("Starting Genetic Algorithm optimization");
        long startTime = System.currentTimeMillis();

        OptimizationResult result = new OptimizationResult();
        result.setAlgorithm(config.getAlgorithm());
        result.setSchedule(schedule);
        result.setConfig(config);
        result.markStarted();

        try {
            // Initialize population
            initializePopulation(schedule);

            // Evaluate initial population
            evaluatePopulation();
            result.setInitialFitness(bestFitness);
            result.setInitialConflicts(fitnessEvaluator.getConflictCount(bestSolution.getSchedule()));

            // Evolution loop
            for (int generation = 0; generation < config.getMaxGenerations(); generation++) {

                // Check termination conditions
                if (shouldTerminate(generation, startTime)) {
                    log.info("Termination condition met at generation {}", generation);
                    break;
                }

                if (cancelled) {
                    result.setStatus(OptimizationResult.OptimizationStatus.CANCELLED);
                    result.setMessage("Optimization cancelled by user");
                    break;
                }

                // Evolve population
                evolveGeneration();

                // Report progress
                if (generation % config.getLogFrequency() == 0) {
                    reportProgress(generation, progressCallback, startTime);
                }
            }

            // Finalize result
            result.setFinalFitness(bestFitness);
            result.setBestFitness(bestFitness);
            result.setFinalConflicts(fitnessEvaluator.getConflictCount(bestSolution.getSchedule()));
            result.setGenerationsExecuted(Math.min(population.size(), config.getMaxGenerations()));
            result.calculateImprovement();

            // Apply best solution to schedule
            applyBestSolution(schedule);

            result.markCompleted(true);
            result.setMessage("Optimization completed successfully");

            log.info("Genetic Algorithm completed. Best fitness: {}", bestFitness);

        } catch (Exception e) {
            log.error("Genetic Algorithm failed", e);
            result.markCompleted(false);
            result.setMessage("Optimization failed: " + e.getMessage());
            result.setErrorDetails(e.toString());
        }

        return result;
    }

    /**
     * Cancel the optimization
     */
    public void cancel() {
        this.cancelled = true;
    }

    // ========================================================================
    // POPULATION INITIALIZATION
    // ========================================================================

    private void initializePopulation(Schedule schedule) {
        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null) {
            throw new IllegalArgumentException("Cannot initialize population with null schedule");
        }

        log.debug("Initializing population of size {}", config.getPopulationSize());

        population.clear();

        // Add current schedule as first individual
        population.add(new Individual(cloneSchedule(schedule)));

        // Generate random variations
        for (int i = 1; i < config.getPopulationSize(); i++) {
            Schedule variant = cloneSchedule(schedule);
            randomizeSchedule(variant, 0.3); // 30% random changes
            population.add(new Individual(variant));
        }
    }

    private Schedule cloneSchedule(Schedule original) {
        // ✅ NULL SAFE: Validate original schedule
        if (original == null) {
            throw new IllegalArgumentException("Cannot clone null schedule");
        }

        // Create a new schedule with same settings but independent slots
        Schedule clone = new Schedule();
        clone.setScheduleName(original.getScheduleName() + " (Optimized)");
        clone.setPeriod(original.getPeriod());
        clone.setScheduleType(original.getScheduleType());
        clone.setStatus(original.getStatus());
        clone.setStartDate(original.getStartDate());
        clone.setEndDate(original.getEndDate());

        // Clone all schedule slots
        // ✅ NULL SAFE: Validate slots list and filter nulls
        List<ScheduleSlot> originalSlots = original.getSlots();
        if (originalSlots == null) {
            originalSlots = Collections.emptyList();
        }

        List<ScheduleSlot> clonedSlots = originalSlots.stream()
            .filter(slot -> slot != null) // ✅ NULL SAFE: Filter null slots
            .map(this::cloneSlot)
            .collect(Collectors.toList());

        clonedSlots.forEach(slot -> slot.setSchedule(clone));
        clone.setSlots(clonedSlots);

        return clone;
    }

    private ScheduleSlot cloneSlot(ScheduleSlot original) {
        // ✅ NULL SAFE: Validate original slot
        if (original == null) {
            return null; // Return null for null input, will be filtered out
        }

        ScheduleSlot clone = new ScheduleSlot();
        clone.setCourse(original.getCourse());
        clone.setTeacher(original.getTeacher());
        clone.setRoom(original.getRoom());
        clone.setDayOfWeek(original.getDayOfWeek());
        clone.setStartTime(original.getStartTime());
        clone.setEndTime(original.getEndTime());
        clone.setPeriodNumber(original.getPeriodNumber());
        return clone;
    }

    private void randomizeSchedule(Schedule schedule, double changeRate) {
        // ✅ NULL SAFE: Validate schedule and get slots
        if (schedule == null) {
            return;
        }

        List<ScheduleSlot> slots = schedule.getSlots();
        // ✅ NULL SAFE: Validate slots list
        if (slots == null || slots.isEmpty()) {
            return;
        }

        for (ScheduleSlot slot : slots) {
            // ✅ NULL SAFE: Skip null slots
            if (slot == null) {
                continue;
            }
            if (random.nextDouble() < changeRate) {
                // Randomly change time
                if (random.nextBoolean()) {
                    randomizeSlotTime(slot);
                }
                // Or randomly change room
                else {
                    // Room randomization would require room list
                    // For now, just change time
                    randomizeSlotTime(slot);
                }
            }
        }
    }

    private void randomizeSlotTime(ScheduleSlot slot) {
        // Random day
        DayOfWeek[] days = DayOfWeek.values();
        slot.setDayOfWeek(days[random.nextInt(5)]); // Mon-Fri

        // Random period (8:00 AM to 3:00 PM)
        int hour = 8 + random.nextInt(7); // 8-14
        int minute = random.nextInt(2) * 30; // 0 or 30
        slot.setStartTime(LocalTime.of(hour, minute));
        slot.setEndTime(slot.getStartTime().plusMinutes(50)); // 50-minute periods
    }

    // ========================================================================
    // POPULATION EVALUATION
    // ========================================================================

    private void evaluatePopulation() {
        // ✅ NULL SAFE: Validate population list
        if (population == null) {
            population = new ArrayList<>();
            return;
        }

        for (Individual individual : population) {
            // ✅ NULL SAFE: Skip null individuals
            if (individual == null || individual.getSchedule() == null) {
                continue;
            }

            if (individual.getFitness() == null) {
                double fitness = fitnessEvaluator.evaluate(individual.getSchedule(), config);
                individual.setFitness(fitness);

                // Track best solution
                if (fitness > bestFitness) {
                    bestFitness = fitness;
                    bestSolution = individual;
                    generationsSinceImprovement = 0;
                }
            }
        }
    }

    // ========================================================================
    // EVOLUTION OPERATIONS
    // ========================================================================

    private void evolveGeneration() {
        List<Individual> newPopulation = new ArrayList<>();

        // Elitism - keep best individuals
        List<Individual> elite = selectElite();
        newPopulation.addAll(elite);

        // Generate offspring
        while (newPopulation.size() < config.getPopulationSize()) {
            // Selection
            Individual parent1 = tournamentSelection();
            Individual parent2 = tournamentSelection();

            // Crossover
            Individual offspring = crossover(parent1, parent2);

            // Mutation
            mutate(offspring);

            // Evaluate offspring
            offspring.setFitness(fitnessEvaluator.evaluate(offspring.getSchedule(), config));

            newPopulation.add(offspring);
        }

        population = newPopulation;
        generationsSinceImprovement++;
    }

    private List<Individual> selectElite() {
        return population.stream()
            .sorted((a, b) -> Double.compare(b.getFitness(), a.getFitness()))
            .limit(config.getEliteSize())
            .collect(Collectors.toList());
    }

    private Individual tournamentSelection() {
        List<Individual> tournament = new ArrayList<>();

        for (int i = 0; i < config.getTournamentSize(); i++) {
            int index = random.nextInt(population.size());
            tournament.add(population.get(index));
        }

        return tournament.stream()
            .max(Comparator.comparingDouble(Individual::getFitness))
            .orElse(tournament.get(0));
    }

    private Individual crossover(Individual parent1, Individual parent2) {
        if (random.nextDouble() > config.getCrossoverRate()) {
            // No crossover, return copy of parent1
            return new Individual(cloneSchedule(parent1.getSchedule()));
        }

        // Single-point crossover
        Schedule offspring = cloneSchedule(parent1.getSchedule());
        List<ScheduleSlot> slots1 = parent1.getSchedule().getSlots();
        List<ScheduleSlot> slots2 = parent2.getSchedule().getSlots();
        List<ScheduleSlot> offspringSlots = offspring.getSlots();

        if (slots1.size() == slots2.size() && slots1.size() == offspringSlots.size()) {
            int crossoverPoint = random.nextInt(slots1.size());

            for (int i = crossoverPoint; i < offspringSlots.size(); i++) {
                ScheduleSlot slot = offspringSlots.get(i);
                ScheduleSlot parent2Slot = slots2.get(i);

                // Copy time from parent2
                slot.setDayOfWeek(parent2Slot.getDayOfWeek());
                slot.setStartTime(parent2Slot.getStartTime());
                slot.setEndTime(parent2Slot.getEndTime());
                slot.setRoom(parent2Slot.getRoom());
            }
        }

        return new Individual(offspring);
    }

    private void mutate(Individual individual) {
        Schedule schedule = individual.getSchedule();
        List<ScheduleSlot> slots = schedule.getSlots();

        for (ScheduleSlot slot : slots) {
            if (random.nextDouble() < config.getMutationRate()) {
                // Mutate this slot
                int mutationType = random.nextInt(3);

                switch (mutationType) {
                    case 0: // Change day
                        DayOfWeek[] days = DayOfWeek.values();
                        slot.setDayOfWeek(days[random.nextInt(5)]);
                        break;
                    case 1: // Change time
                        randomizeSlotTime(slot);
                        break;
                    case 2: // Swap with another slot
                        if (slots.size() > 1) {
                            ScheduleSlot other = slots.get(random.nextInt(slots.size()));
                            swapSlotTimes(slot, other);
                        }
                        break;
                }
            }
        }
    }

    private void swapSlotTimes(ScheduleSlot slot1, ScheduleSlot slot2) {
        DayOfWeek tempDay = slot1.getDayOfWeek();
        LocalTime tempStart = slot1.getStartTime();
        LocalTime tempEnd = slot1.getEndTime();

        slot1.setDayOfWeek(slot2.getDayOfWeek());
        slot1.setStartTime(slot2.getStartTime());
        slot1.setEndTime(slot2.getEndTime());

        slot2.setDayOfWeek(tempDay);
        slot2.setStartTime(tempStart);
        slot2.setEndTime(tempEnd);
    }

    // ========================================================================
    // TERMINATION CONDITIONS
    // ========================================================================

    private boolean shouldTerminate(int generation, long startTime) {
        // Check max generations
        if (generation >= config.getMaxGenerations()) {
            return true;
        }

        // Check timeout
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        if (elapsed >= config.getMaxRuntimeSeconds()) {
            log.info("Timeout reached: {} seconds", elapsed);
            return true;
        }

        // Check stagnation
        if (generationsSinceImprovement >= config.getStagnationLimit()) {
            log.info("Stagnation detected: no improvement for {} generations",
                    generationsSinceImprovement);
            return true;
        }

        // Check target fitness
        if (config.getTargetFitness() != null && bestFitness >= config.getTargetFitness()) {
            log.info("Target fitness reached: {}", bestFitness);
            return true;
        }

        return false;
    }

    // ========================================================================
    // PROGRESS REPORTING
    // ========================================================================

    private void reportProgress(int generation,
                                java.util.function.Consumer<GAProgress> callback,
                                long startTime) {
        if (callback == null) return;

        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        double avgFitness = population.stream()
            .mapToDouble(Individual::getFitness)
            .average()
            .orElse(0.0);

        int conflicts = fitnessEvaluator.getConflictCount(bestSolution.getSchedule());

        GAProgress progress = new GAProgress(
            generation,
            config.getMaxGenerations(),
            avgFitness,
            bestFitness,
            conflicts,
            elapsed,
            generationsSinceImprovement
        );

        callback.accept(progress);

        log.debug("Generation {}: Best={}, Avg={}, Conflicts={}",
                 generation, bestFitness, avgFitness, conflicts);
    }

    // ========================================================================
    // RESULT APPLICATION
    // ========================================================================

    private void applyBestSolution(Schedule schedule) {
        if (bestSolution == null) return;

        Schedule bestSchedule = bestSolution.getSchedule();
        List<ScheduleSlot> originalSlots = schedule.getSlots();
        List<ScheduleSlot> bestSlots = bestSchedule.getSlots();

        // Apply best slot assignments to original schedule
        for (int i = 0; i < Math.min(originalSlots.size(), bestSlots.size()); i++) {
            ScheduleSlot original = originalSlots.get(i);
            ScheduleSlot best = bestSlots.get(i);

            original.setDayOfWeek(best.getDayOfWeek());
            original.setStartTime(best.getStartTime());
            original.setEndTime(best.getEndTime());
            original.setRoom(best.getRoom());
        }
    }

    // ========================================================================
    // SUPPORTING CLASSES
    // ========================================================================

    /**
     * Individual in the population (candidate solution)
     */
    public static class Individual {
        private Schedule schedule;
        private Double fitness;

        public Individual(Schedule schedule) {
            this.schedule = schedule;
        }

        public Schedule getSchedule() { return schedule; }
        public Double getFitness() { return fitness; }
        public void setFitness(Double fitness) { this.fitness = fitness; }
    }

    /**
     * Progress information
     */
    public static class GAProgress {
        private int generation;
        private int maxGenerations;
        private double avgFitness;
        private double bestFitness;
        private int conflicts;
        private long elapsedSeconds;
        private int stagnationCount;

        public GAProgress(int generation, int maxGenerations, double avgFitness,
                         double bestFitness, int conflicts, long elapsedSeconds,
                         int stagnationCount) {
            this.generation = generation;
            this.maxGenerations = maxGenerations;
            this.avgFitness = avgFitness;
            this.bestFitness = bestFitness;
            this.conflicts = conflicts;
            this.elapsedSeconds = elapsedSeconds;
            this.stagnationCount = stagnationCount;
        }

        // Getters
        public int getGeneration() { return generation; }
        public int getMaxGenerations() { return maxGenerations; }
        public double getAvgFitness() { return avgFitness; }
        public double getBestFitness() { return bestFitness; }
        public int getConflicts() { return conflicts; }
        public long getElapsedSeconds() { return elapsedSeconds; }
        public int getStagnationCount() { return stagnationCount; }

        public double getProgress() {
            return maxGenerations > 0 ? (double) generation / maxGenerations : 0.0;
        }
    }
}
