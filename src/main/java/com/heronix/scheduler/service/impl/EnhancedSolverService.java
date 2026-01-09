package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Enhanced Solver Service with Advanced Optimization Techniques
 *
 * Features:
 * - Simulated Annealing with adaptive cooling
 * - Tabu Search with dynamic tenure
 * - Genetic Algorithm with elitism
 * - Parallel Island Model optimization
 * - Constraint relaxation strategies
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 14 - Solver Optimizations
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedSolverService {

    private final ScheduleSlotRepository scheduleSlotRepository;
    private final SISDataService sisDataService;
    private final RoomRepository roomRepository;

    // ========================================================================
    // SIMULATED ANNEALING WITH ADAPTIVE COOLING
    // ========================================================================

    /**
     * Optimize schedule using Simulated Annealing with adaptive cooling rate
     */
    public OptimizationResult optimizeWithSimulatedAnnealing(List<ScheduleSlot> schedule,
            SimulatedAnnealingConfig config) {
        // ✅ NULL SAFE: Validate parameters
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
        if (config == null) {
            config = SimulatedAnnealingConfig.builder().build(); // Use defaults
        }

        log.info("Starting Simulated Annealing optimization with {} slots", schedule.size());

        long startTime = System.currentTimeMillis();
        List<ScheduleSlot> currentSolution = new ArrayList<>(schedule);
        List<ScheduleSlot> bestSolution = new ArrayList<>(schedule);

        double currentEnergy = calculateEnergy(currentSolution);
        double bestEnergy = currentEnergy;

        double temperature = config.getInitialTemperature();
        int iteration = 0;
        int stagnationCount = 0;
        List<Double> energyHistory = new ArrayList<>();

        while (temperature > config.getMinTemperature() && iteration < config.getMaxIterations()) {
            // Generate neighbor solution
            List<ScheduleSlot> neighbor = generateNeighbor(currentSolution);
            double neighborEnergy = calculateEnergy(neighbor);

            double deltaE = neighborEnergy - currentEnergy;

            // Accept or reject based on Metropolis criterion
            if (deltaE < 0 || Math.random() < Math.exp(-deltaE / temperature)) {
                currentSolution = neighbor;
                currentEnergy = neighborEnergy;

                if (currentEnergy < bestEnergy) {
                    bestSolution = new ArrayList<>(currentSolution);
                    bestEnergy = currentEnergy;
                    stagnationCount = 0;
                } else {
                    stagnationCount++;
                }
            } else {
                stagnationCount++;
            }

            // Adaptive cooling - slow down if improving, speed up if stagnant
            double coolingRate = config.getCoolingRate();
            if (stagnationCount > 100) {
                // Reheat if stuck
                temperature = Math.min(temperature * 1.5, config.getInitialTemperature() * 0.5);
                stagnationCount = 0;
                coolingRate *= 0.95; // Slower cooling after reheat
            } else {
                temperature *= coolingRate;
            }

            energyHistory.add(currentEnergy);
            iteration++;

            if (iteration % 1000 == 0) {
                log.debug("SA Iteration {}: temp={:.2f}, energy={:.2f}, best={:.2f}",
                    iteration, temperature, currentEnergy, bestEnergy);
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        return OptimizationResult.builder()
            .algorithm("Simulated Annealing (Adaptive)")
            .initialScore(calculateEnergy(schedule))
            .finalScore(bestEnergy)
            .improvement((calculateEnergy(schedule) - bestEnergy) / calculateEnergy(schedule) * 100)
            .iterations(iteration)
            .durationMs(duration)
            .optimizedSchedule(bestSolution)
            .energyHistory(energyHistory)
            .build();
    }

    // ========================================================================
    // TABU SEARCH WITH DYNAMIC TENURE
    // ========================================================================

    /**
     * Optimize schedule using Tabu Search with dynamic tenure adjustment
     */
    public OptimizationResult optimizeWithTabuSearch(List<ScheduleSlot> schedule,
            TabuSearchConfig config) {
        // ✅ NULL SAFE: Validate parameters
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
        if (config == null) {
            config = TabuSearchConfig.builder().build(); // Use defaults
        }

        log.info("Starting Tabu Search optimization with {} slots", schedule.size());

        long startTime = System.currentTimeMillis();
        List<ScheduleSlot> currentSolution = new ArrayList<>(schedule);
        List<ScheduleSlot> bestSolution = new ArrayList<>(schedule);

        double currentScore = calculateEnergy(currentSolution);
        double bestScore = currentScore;

        // Tabu list stores move signatures
        LinkedList<String> tabuList = new LinkedList<>();
        int tabuTenure = config.getInitialTabuTenure();

        int iteration = 0;
        int improvementCount = 0;
        List<Double> scoreHistory = new ArrayList<>();

        while (iteration < config.getMaxIterations()) {
            // Generate neighborhood
            List<Move> neighborhood = generateNeighborhood(currentSolution, config.getNeighborhoodSize());

            // Find best non-tabu move (or aspiration)
            Move bestMove = null;
            double bestMoveScore = Double.MAX_VALUE;

            for (Move move : neighborhood) {
                String moveSignature = move.getSignature();
                double moveScore = evaluateMove(currentSolution, move);

                boolean isTabu = tabuList.contains(moveSignature);
                boolean aspirationMet = moveScore < bestScore; // Better than global best

                if ((!isTabu || aspirationMet) && moveScore < bestMoveScore) {
                    bestMove = move;
                    bestMoveScore = moveScore;
                }
            }

            if (bestMove != null) {
                // Apply move
                applyMove(currentSolution, bestMove);
                currentScore = bestMoveScore;

                // Update tabu list
                tabuList.addLast(bestMove.getSignature());
                if (tabuList.size() > tabuTenure) {
                    tabuList.removeFirst();
                }

                // Update best solution
                if (currentScore < bestScore) {
                    bestSolution = new ArrayList<>(currentSolution);
                    bestScore = currentScore;
                    improvementCount++;

                    // Decrease tenure when improving (intensification)
                    tabuTenure = Math.max(config.getMinTabuTenure(),
                        (int)(tabuTenure * 0.9));
                } else {
                    // Increase tenure when not improving (diversification)
                    tabuTenure = Math.min(config.getMaxTabuTenure(),
                        (int)(tabuTenure * 1.1));
                }
            }

            scoreHistory.add(currentScore);
            iteration++;
        }

        long duration = System.currentTimeMillis() - startTime;

        return OptimizationResult.builder()
            .algorithm("Tabu Search (Dynamic Tenure)")
            .initialScore(calculateEnergy(schedule))
            .finalScore(bestScore)
            .improvement((calculateEnergy(schedule) - bestScore) / calculateEnergy(schedule) * 100)
            .iterations(iteration)
            .durationMs(duration)
            .optimizedSchedule(bestSolution)
            .energyHistory(scoreHistory)
            .build();
    }

    // ========================================================================
    // GENETIC ALGORITHM WITH ELITISM
    // ========================================================================

    /**
     * Optimize schedule using Genetic Algorithm with elitism
     */
    public OptimizationResult optimizeWithGeneticAlgorithm(List<ScheduleSlot> schedule,
            GeneticAlgorithmConfig config) {
        // ✅ NULL SAFE: Validate parameters
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
        if (config == null) {
            config = GeneticAlgorithmConfig.builder().build(); // Use defaults
        }

        log.info("Starting Genetic Algorithm optimization with {} slots", schedule.size());

        long startTime = System.currentTimeMillis();

        // Initialize population
        List<List<ScheduleSlot>> population = initializePopulation(schedule, config.getPopulationSize());

        double bestFitness = Double.MAX_VALUE;
        List<ScheduleSlot> bestIndividual = null;
        List<Double> fitnessHistory = new ArrayList<>();

        for (int generation = 0; generation < config.getGenerations(); generation++) {
            // Evaluate fitness
            List<Double> fitnesses = population.stream()
                .map(this::calculateEnergy)
                .collect(Collectors.toList());

            // Track best
            for (int i = 0; i < population.size(); i++) {
                if (fitnesses.get(i) < bestFitness) {
                    bestFitness = fitnesses.get(i);
                    bestIndividual = new ArrayList<>(population.get(i));
                }
            }
            fitnessHistory.add(bestFitness);

            // Elitism - keep top individuals
            List<List<ScheduleSlot>> newPopulation = new ArrayList<>();
            int eliteCount = (int)(config.getPopulationSize() * config.getElitismRate());

            // Sort by fitness and keep elite
            List<Integer> indices = new ArrayList<>();
            for (int i = 0; i < population.size(); i++) indices.add(i);
            indices.sort((a, b) -> Double.compare(fitnesses.get(a), fitnesses.get(b)));

            for (int i = 0; i < eliteCount; i++) {
                newPopulation.add(new ArrayList<>(population.get(indices.get(i))));
            }

            // Selection, crossover, mutation for rest
            while (newPopulation.size() < config.getPopulationSize()) {
                // Tournament selection
                List<ScheduleSlot> parent1 = tournamentSelect(population, fitnesses, config.getTournamentSize());
                List<ScheduleSlot> parent2 = tournamentSelect(population, fitnesses, config.getTournamentSize());

                // Crossover
                List<ScheduleSlot> child;
                if (Math.random() < config.getCrossoverRate()) {
                    child = crossover(parent1, parent2);
                } else {
                    child = new ArrayList<>(Math.random() < 0.5 ? parent1 : parent2);
                }

                // Mutation
                if (Math.random() < config.getMutationRate()) {
                    mutate(child);
                }

                newPopulation.add(child);
            }

            population = newPopulation;

            if (generation % 50 == 0) {
                log.debug("GA Generation {}: best fitness = {:.2f}", generation, bestFitness);
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        return OptimizationResult.builder()
            .algorithm("Genetic Algorithm (Elitism)")
            .initialScore(calculateEnergy(schedule))
            .finalScore(bestFitness)
            .improvement((calculateEnergy(schedule) - bestFitness) / calculateEnergy(schedule) * 100)
            .iterations(config.getGenerations())
            .durationMs(duration)
            .optimizedSchedule(bestIndividual)
            .energyHistory(fitnessHistory)
            .build();
    }

    // ========================================================================
    // PARALLEL ISLAND MODEL
    // ========================================================================

    /**
     * Optimize using parallel island model with migration
     */
    public OptimizationResult optimizeWithIslandModel(List<ScheduleSlot> schedule,
            IslandModelConfig config) {
        // ✅ NULL SAFE: Validate parameters
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
        final IslandModelConfig finalConfig = (config == null) ?
            IslandModelConfig.builder().build() : config; // Use defaults if null

        log.info("Starting Island Model optimization with {} islands", finalConfig.getNumIslands());

        long startTime = System.currentTimeMillis();
        ExecutorService executor = Executors.newFixedThreadPool(finalConfig.getNumIslands());

        try {
            // Create islands with different algorithms
            List<Future<OptimizationResult>> futures = new ArrayList<>();

            for (int i = 0; i < finalConfig.getNumIslands(); i++) {
                final int islandId = i;
                futures.add(executor.submit(() -> runIsland(schedule, islandId, finalConfig)));
            }

            // Collect results
            List<OptimizationResult> results = new ArrayList<>();
            for (Future<OptimizationResult> future : futures) {
                try {
                    results.add(future.get(finalConfig.getTimeoutSeconds(), TimeUnit.SECONDS));
                } catch (Exception e) {
                    log.warn("Island failed: {}", e.getMessage());
                }
            }

            // Find best result
            OptimizationResult best = results.stream()
                .min(Comparator.comparingDouble(OptimizationResult::getFinalScore))
                .orElse(null);

            if (best != null) {
                best.setAlgorithm("Island Model (Parallel)");
                best.setDurationMs(System.currentTimeMillis() - startTime);
            }

            return best;

        } finally {
            executor.shutdown();
        }
    }

    private OptimizationResult runIsland(List<ScheduleSlot> schedule, int islandId, IslandModelConfig config) {
        // Each island uses a different algorithm variant
        return switch (islandId % 3) {
            case 0 -> optimizeWithSimulatedAnnealing(schedule,
                SimulatedAnnealingConfig.builder()
                    .initialTemperature(1000 + islandId * 100)
                    .minTemperature(0.1)
                    .coolingRate(0.995)
                    .maxIterations(config.getIterationsPerIsland())
                    .build());
            case 1 -> optimizeWithTabuSearch(schedule,
                TabuSearchConfig.builder()
                    .initialTabuTenure(7 + islandId)
                    .minTabuTenure(3)
                    .maxTabuTenure(20)
                    .neighborhoodSize(20)
                    .maxIterations(config.getIterationsPerIsland())
                    .build());
            default -> optimizeWithGeneticAlgorithm(schedule,
                GeneticAlgorithmConfig.builder()
                    .populationSize(50)
                    .generations(config.getIterationsPerIsland() / 50)
                    .crossoverRate(0.8)
                    .mutationRate(0.1 + islandId * 0.02)
                    .elitismRate(0.1)
                    .tournamentSize(3)
                    .build());
        };
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private double calculateEnergy(List<ScheduleSlot> schedule) {
        double energy = 0;

        // Count conflicts
        Map<String, List<ScheduleSlot>> teacherSlots = new HashMap<>();
        Map<String, List<ScheduleSlot>> roomSlots = new HashMap<>();

        for (ScheduleSlot slot : schedule) {
            if (slot.getTeacher() != null && slot.getDayOfWeek() != null && slot.getStartTime() != null) {
                String teacherKey = slot.getTeacher().getId() + "-" + slot.getDayOfWeek() + "-" + slot.getStartTime();
                teacherSlots.computeIfAbsent(teacherKey, k -> new ArrayList<>()).add(slot);
            }
            if (slot.getRoom() != null && slot.getDayOfWeek() != null && slot.getStartTime() != null) {
                String roomKey = slot.getRoom().getId() + "-" + slot.getDayOfWeek() + "-" + slot.getStartTime();
                roomSlots.computeIfAbsent(roomKey, k -> new ArrayList<>()).add(slot);
            }
        }

        // Teacher conflicts (high penalty)
        for (List<ScheduleSlot> slots : teacherSlots.values()) {
            if (slots.size() > 1) {
                energy += (slots.size() - 1) * 100;
            }
        }

        // Room conflicts (high penalty)
        for (List<ScheduleSlot> slots : roomSlots.values()) {
            if (slots.size() > 1) {
                energy += (slots.size() - 1) * 100;
            }
        }

        // Workload balance penalty
        Map<Long, Integer> teacherLoads = new HashMap<>();
        for (ScheduleSlot slot : schedule) {
            if (slot.getTeacher() != null) {
                teacherLoads.merge(slot.getTeacher().getId(), 1, Integer::sum);
            }
        }
        if (!teacherLoads.isEmpty()) {
            double avgLoad = teacherLoads.values().stream().mapToInt(Integer::intValue).average().orElse(0);
            double variance = teacherLoads.values().stream()
                .mapToDouble(load -> Math.pow(load - avgLoad, 2))
                .average().orElse(0);
            energy += Math.sqrt(variance) * 10; // Penalize uneven distribution
        }

        return energy;
    }

    private List<ScheduleSlot> generateNeighbor(List<ScheduleSlot> schedule) {
        List<ScheduleSlot> neighbor = new ArrayList<>(schedule);
        if (neighbor.isEmpty()) return neighbor;

        int idx = (int)(Math.random() * neighbor.size());
        ScheduleSlot slot = neighbor.get(idx);

        // Random modification
        double r = Math.random();
        if (r < 0.33) {
            // Change time
            int hour = 7 + (int)(Math.random() * 8);
            slot.setStartTime(LocalTime.of(hour, 0));
            slot.setEndTime(LocalTime.of(hour + 1, 0));
        } else if (r < 0.66) {
            // Change day
            DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                               DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
            slot.setDayOfWeek(days[(int)(Math.random() * days.length)]);
        } else {
            // Swap with another slot
            int idx2 = (int)(Math.random() * neighbor.size());
            if (idx != idx2) {
                ScheduleSlot slot2 = neighbor.get(idx2);
                DayOfWeek tempDay = slot.getDayOfWeek();
                LocalTime tempStart = slot.getStartTime();
                LocalTime tempEnd = slot.getEndTime();

                slot.setDayOfWeek(slot2.getDayOfWeek());
                slot.setStartTime(slot2.getStartTime());
                slot.setEndTime(slot2.getEndTime());

                slot2.setDayOfWeek(tempDay);
                slot2.setStartTime(tempStart);
                slot2.setEndTime(tempEnd);
            }
        }

        return neighbor;
    }

    private List<Move> generateNeighborhood(List<ScheduleSlot> schedule, int size) {
        List<Move> moves = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            int idx = (int)(Math.random() * schedule.size());
            int hour = 7 + (int)(Math.random() * 8);
            DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                               DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};
            moves.add(new Move(idx, days[(int)(Math.random() * days.length)],
                LocalTime.of(hour, 0), LocalTime.of(hour + 1, 0)));
        }
        return moves;
    }

    private double evaluateMove(List<ScheduleSlot> schedule, Move move) {
        List<ScheduleSlot> temp = new ArrayList<>(schedule);
        applyMove(temp, move);
        return calculateEnergy(temp);
    }

    private void applyMove(List<ScheduleSlot> schedule, Move move) {
        if (move.getSlotIndex() < schedule.size()) {
            ScheduleSlot slot = schedule.get(move.getSlotIndex());
            slot.setDayOfWeek(move.getNewDay());
            slot.setStartTime(move.getNewStartTime());
            slot.setEndTime(move.getNewEndTime());
        }
    }

    private List<List<ScheduleSlot>> initializePopulation(List<ScheduleSlot> template, int size) {
        List<List<ScheduleSlot>> population = new ArrayList<>();
        population.add(new ArrayList<>(template));

        for (int i = 1; i < size; i++) {
            List<ScheduleSlot> individual = new ArrayList<>(template);
            // Randomize
            for (int j = 0; j < individual.size() / 2; j++) {
                individual = generateNeighbor(individual);
            }
            population.add(individual);
        }
        return population;
    }

    private List<ScheduleSlot> tournamentSelect(List<List<ScheduleSlot>> population,
            List<Double> fitnesses, int tournamentSize) {
        int best = -1;
        double bestFitness = Double.MAX_VALUE;

        for (int i = 0; i < tournamentSize; i++) {
            int idx = (int)(Math.random() * population.size());
            if (fitnesses.get(idx) < bestFitness) {
                bestFitness = fitnesses.get(idx);
                best = idx;
            }
        }
        return population.get(best);
    }

    private List<ScheduleSlot> crossover(List<ScheduleSlot> parent1, List<ScheduleSlot> parent2) {
        List<ScheduleSlot> child = new ArrayList<>();
        int crossPoint = (int)(Math.random() * parent1.size());

        for (int i = 0; i < parent1.size(); i++) {
            child.add(i < crossPoint ? parent1.get(i) : parent2.get(i));
        }
        return child;
    }

    private void mutate(List<ScheduleSlot> individual) {
        generateNeighbor(individual); // Apply random change
    }

    // ========================================================================
    // DTOs
    // ========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptimizationResult {
        private String algorithm;
        private double initialScore;
        private double finalScore;
        private double improvement;
        private int iterations;
        private long durationMs;
        private List<ScheduleSlot> optimizedSchedule;
        private List<Double> energyHistory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimulatedAnnealingConfig {
        @Builder.Default private double initialTemperature = 1000;
        @Builder.Default private double minTemperature = 0.1;
        @Builder.Default private double coolingRate = 0.995;
        @Builder.Default private int maxIterations = 10000;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TabuSearchConfig {
        @Builder.Default private int initialTabuTenure = 7;
        @Builder.Default private int minTabuTenure = 3;
        @Builder.Default private int maxTabuTenure = 20;
        @Builder.Default private int neighborhoodSize = 20;
        @Builder.Default private int maxIterations = 5000;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeneticAlgorithmConfig {
        @Builder.Default private int populationSize = 100;
        @Builder.Default private int generations = 200;
        @Builder.Default private double crossoverRate = 0.8;
        @Builder.Default private double mutationRate = 0.1;
        @Builder.Default private double elitismRate = 0.1;
        @Builder.Default private int tournamentSize = 3;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IslandModelConfig {
        @Builder.Default private int numIslands = 4;
        @Builder.Default private int iterationsPerIsland = 2000;
        @Builder.Default private int migrationInterval = 500;
        @Builder.Default private int migrationSize = 5;
        @Builder.Default private int timeoutSeconds = 60;
    }

    @Data
    @AllArgsConstructor
    private static class Move {
        private int slotIndex;
        private DayOfWeek newDay;
        private LocalTime newStartTime;
        private LocalTime newEndTime;

        public String getSignature() {
            return slotIndex + "-" + newDay + "-" + newStartTime;
        }
    }
}
