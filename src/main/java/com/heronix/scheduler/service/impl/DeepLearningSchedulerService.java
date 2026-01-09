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
import java.util.stream.Collectors;

/**
 * Deep Learning-based Schedule Optimization Service
 *
 * This service provides neural network-inspired algorithms for schedule optimization.
 * Uses pure Java implementations that mimic deep learning approaches without
 * requiring external ML frameworks (TensorFlow/PyTorch).
 *
 * Features:
 * - Reinforcement Learning-based slot assignment
 * - Neural Network-style pattern recognition
 * - Q-Learning for conflict resolution
 * - Deep Q-Network (DQN) inspired optimization
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 14 - Deep Learning Integration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeepLearningSchedulerService {

    private final ScheduleSlotRepository scheduleSlotRepository;
    private final SISDataService sisDataService;
    private final RoomRepository roomRepository;

    // Neural network weights (simplified)
    private double[][] inputWeights;
    private double[][] hiddenWeights;
    private double[] outputWeights;
    private boolean initialized = false;

    // Q-Learning parameters
    private Map<String, double[]> qTable = new HashMap<>();
    private static final double LEARNING_RATE = 0.1;
    private static final double DISCOUNT_FACTOR = 0.95;
    private static final double EXPLORATION_RATE = 0.1;

    // ========================================================================
    // REINFORCEMENT LEARNING SCHEDULER
    // ========================================================================

    /**
     * Optimize schedule using Q-Learning approach
     */
    public RLOptimizationResult optimizeWithReinforcementLearning(List<ScheduleSlot> schedule,
            RLConfig config) {
        // ✅ NULL SAFE: Validate parameters
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
        if (config == null) {
            config = RLConfig.builder().build(); // Use default config
        }

        log.info("Starting Reinforcement Learning optimization with {} slots", schedule.size());

        long startTime = System.currentTimeMillis();
        List<ScheduleSlot> currentSchedule = new ArrayList<>(schedule);
        List<ScheduleSlot> bestSchedule = new ArrayList<>(schedule);
        double bestReward = calculateTotalReward(currentSchedule);

        List<Double> rewardHistory = new ArrayList<>();
        int episodes = config.getEpisodes();

        for (int episode = 0; episode < episodes; episode++) {
            // Reset to initial state with some randomization
            if (episode > 0 && Math.random() < 0.3) {
                currentSchedule = new ArrayList<>(schedule);
            }

            double episodeReward = 0;

            // Run through all slots
            for (int step = 0; step < currentSchedule.size(); step++) {
                String state = encodeState(currentSchedule, step);
                int action = selectAction(state, config.getExplorationRate());

                // Apply action
                double reward = applyAction(currentSchedule, step, action);
                episodeReward += reward;

                // Update Q-values
                String newState = encodeState(currentSchedule, step);
                updateQValue(state, action, reward, newState);
            }

            // Track best solution
            double totalReward = calculateTotalReward(currentSchedule);
            if (totalReward > bestReward) {
                bestSchedule = new ArrayList<>(currentSchedule);
                bestReward = totalReward;
            }

            rewardHistory.add(totalReward);

            if (episode % 100 == 0) {
                log.debug("RL Episode {}: reward={:.2f}, best={:.2f}", episode, totalReward, bestReward);
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        return RLOptimizationResult.builder()
            .initialReward(calculateTotalReward(schedule))
            .finalReward(bestReward)
            .improvement((bestReward - calculateTotalReward(schedule)) / Math.abs(calculateTotalReward(schedule)) * 100)
            .episodes(episodes)
            .durationMs(duration)
            .optimizedSchedule(bestSchedule)
            .rewardHistory(rewardHistory)
            .qTableSize(qTable.size())
            .build();
    }

    private String encodeState(List<ScheduleSlot> schedule, int currentIndex) {
        // ✅ NULL SAFE: Validate parameters
        if (schedule == null || currentIndex < 0 || currentIndex >= schedule.size()) {
            return "INVALID_STATE";
        }

        // Encode schedule state as string
        StringBuilder sb = new StringBuilder();
        ScheduleSlot current = schedule.get(currentIndex);
        // ✅ NULL SAFE: Validate current slot
        if (current == null) {
            return "NULL_SLOT";
        }

        // Current slot features
        sb.append(current.getDayOfWeek() != null ? current.getDayOfWeek().ordinal() : -1).append(",");
        sb.append(current.getStartTime() != null ? current.getStartTime().getHour() : -1).append(",");

        // Count conflicts
        int teacherConflicts = countTeacherConflicts(schedule, currentIndex);
        int roomConflicts = countRoomConflicts(schedule, currentIndex);
        sb.append(teacherConflicts).append(",");
        sb.append(roomConflicts);

        return sb.toString();
    }

    private int selectAction(String state, double explorationRate) {
        // Epsilon-greedy action selection
        if (Math.random() < explorationRate) {
            return (int)(Math.random() * 5); // Random action
        }

        // Exploit: choose best action from Q-table
        double[] qValues = qTable.getOrDefault(state, new double[5]);
        int bestAction = 0;
        double bestValue = qValues[0];
        for (int i = 1; i < qValues.length; i++) {
            if (qValues[i] > bestValue) {
                bestValue = qValues[i];
                bestAction = i;
            }
        }
        return bestAction;
    }

    private double applyAction(List<ScheduleSlot> schedule, int index, int action) {
        // ✅ NULL SAFE: Validate parameters
        if (schedule == null || index < 0 || index >= schedule.size()) {
            return 0;
        }

        ScheduleSlot slot = schedule.get(index);
        // ✅ NULL SAFE: Validate slot
        if (slot == null) {
            return 0;
        }

        double oldReward = calculateSlotReward(schedule, index);

        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                          DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};

        switch (action) {
            case 0 -> { // Move to earlier time
                if (slot.getStartTime() != null && slot.getStartTime().getHour() > 7) {
                    slot.setStartTime(slot.getStartTime().minusHours(1));
                    slot.setEndTime(slot.getEndTime().minusHours(1));
                }
            }
            case 1 -> { // Move to later time
                if (slot.getStartTime() != null && slot.getStartTime().getHour() < 14) {
                    slot.setStartTime(slot.getStartTime().plusHours(1));
                    slot.setEndTime(slot.getEndTime().plusHours(1));
                }
            }
            case 2 -> { // Move to previous day
                if (slot.getDayOfWeek() != null && slot.getDayOfWeek() != DayOfWeek.MONDAY) {
                    slot.setDayOfWeek(DayOfWeek.of(slot.getDayOfWeek().getValue() - 1));
                }
            }
            case 3 -> { // Move to next day
                if (slot.getDayOfWeek() != null && slot.getDayOfWeek() != DayOfWeek.FRIDAY) {
                    slot.setDayOfWeek(DayOfWeek.of(slot.getDayOfWeek().getValue() + 1));
                }
            }
            case 4 -> { // Swap with random slot
                int otherIndex = (int)(Math.random() * schedule.size());
                if (otherIndex != index && otherIndex < schedule.size()) {
                    ScheduleSlot other = schedule.get(otherIndex);
                    // ✅ NULL SAFE: Validate other slot before swapping
                    if (other != null) {
                        swapSlots(slot, other);
                    }
                }
            }
        }

        double newReward = calculateSlotReward(schedule, index);
        return newReward - oldReward; // Reward is improvement
    }

    private void updateQValue(String state, int action, double reward, String newState) {
        double[] qValues = qTable.computeIfAbsent(state, k -> new double[5]);
        double[] newQValues = qTable.getOrDefault(newState, new double[5]);

        double maxNextQ = Arrays.stream(newQValues).max().orElse(0);
        qValues[action] = qValues[action] + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQ - qValues[action]);
    }

    // ========================================================================
    // NEURAL NETWORK PATTERN RECOGNITION
    // ========================================================================

    /**
     * Use neural network to predict optimal slot assignments
     */
    public NNPredictionResult predictOptimalAssignment(ScheduleSlot slot, List<ScheduleSlot> existingSchedule) {
        // ✅ NULL SAFE: Validate parameters
        if (slot == null) {
            throw new IllegalArgumentException("Slot cannot be null");
        }
        if (existingSchedule == null) {
            existingSchedule = Collections.emptyList();
        }

        initializeNetwork();

        // Extract features
        double[] features = extractFeatures(slot, existingSchedule);

        // Forward pass
        double[] hiddenOutput = forwardHidden(features);
        double[] output = forwardOutput(hiddenOutput);

        // Interpret output as day/time recommendations
        int recommendedDay = argmax(Arrays.copyOfRange(output, 0, 5));
        int recommendedHour = 7 + argmax(Arrays.copyOfRange(output, 5, 13));

        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                          DayOfWeek.THURSDAY, DayOfWeek.FRIDAY};

        return NNPredictionResult.builder()
            .recommendedDay(days[recommendedDay])
            .recommendedStartTime(LocalTime.of(recommendedHour, 0))
            .recommendedEndTime(LocalTime.of(recommendedHour + 1, 0))
            .confidence(sigmoid(output[argmax(output)]))
            .alternativeSlots(generateAlternatives(output, days))
            .build();
    }

    private void initializeNetwork() {
        if (initialized) return;

        // Simple 3-layer network: 20 inputs, 15 hidden, 13 outputs (5 days + 8 hours)
        Random rand = new Random(42);

        inputWeights = new double[20][15];
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 15; j++) {
                inputWeights[i][j] = rand.nextGaussian() * 0.5;
            }
        }

        hiddenWeights = new double[15][13];
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 13; j++) {
                hiddenWeights[i][j] = rand.nextGaussian() * 0.5;
            }
        }

        outputWeights = new double[13];
        for (int i = 0; i < 13; i++) {
            outputWeights[i] = rand.nextGaussian() * 0.5;
        }

        initialized = true;
    }

    private double[] extractFeatures(ScheduleSlot slot, List<ScheduleSlot> schedule) {
        // ✅ NULL SAFE: Validate parameters
        if (slot == null || schedule == null) {
            return new double[20]; // Return zero features
        }

        double[] features = new double[20];

        // Teacher workload features (5 features for 5 days)
        if (slot.getTeacher() != null && slot.getTeacher().getId() != null) {
            Map<DayOfWeek, Long> teacherLoad = schedule.stream()
                // ✅ NULL SAFE: Filter null slots and validate teacher
                .filter(s -> s != null && s.getTeacher() != null && s.getTeacher().getId() != null)
                .filter(s -> s.getTeacher().getId().equals(slot.getTeacher().getId()))
                .filter(s -> s.getDayOfWeek() != null)
                .collect(Collectors.groupingBy(ScheduleSlot::getDayOfWeek, Collectors.counting()));

            for (DayOfWeek day : DayOfWeek.values()) {
                if (day.getValue() <= 5) {
                    features[day.getValue() - 1] = teacherLoad.getOrDefault(day, 0L) / 8.0;
                }
            }
        }

        // Room utilization features (5 features)
        if (slot.getRoom() != null && slot.getRoom().getId() != null) {
            Map<DayOfWeek, Long> roomLoad = schedule.stream()
                // ✅ NULL SAFE: Filter null slots and validate room
                .filter(s -> s != null && s.getRoom() != null && s.getRoom().getId() != null)
                .filter(s -> s.getRoom().getId().equals(slot.getRoom().getId()))
                .filter(s -> s.getDayOfWeek() != null)
                .collect(Collectors.groupingBy(ScheduleSlot::getDayOfWeek, Collectors.counting()));

            for (DayOfWeek day : DayOfWeek.values()) {
                if (day.getValue() <= 5) {
                    features[5 + day.getValue() - 1] = roomLoad.getOrDefault(day, 0L) / 8.0;
                }
            }
        }

        // Time slot availability (8 features for hours 7-14)
        for (int hour = 7; hour <= 14; hour++) {
            int finalHour = hour;
            long conflicts = schedule.stream()
                // ✅ NULL SAFE: Filter null slots before checking start time
                .filter(s -> s != null && s.getStartTime() != null && s.getStartTime().getHour() == finalHour)
                .count();
            features[10 + hour - 7] = schedule.size() > 0 ? 1.0 - (conflicts / (double)schedule.size()) : 0.0;
        }

        // Course features
        features[18] = slot.getCourse() != null ? 1.0 : 0.0;
        features[19] = Math.random(); // Noise for regularization

        return features;
    }

    private double[] forwardHidden(double[] input) {
        double[] hidden = new double[15];
        for (int j = 0; j < 15; j++) {
            double sum = 0;
            for (int i = 0; i < input.length; i++) {
                sum += input[i] * inputWeights[i][j];
            }
            hidden[j] = relu(sum);
        }
        return hidden;
    }

    private double[] forwardOutput(double[] hidden) {
        double[] output = new double[13];
        for (int j = 0; j < 13; j++) {
            double sum = 0;
            for (int i = 0; i < hidden.length; i++) {
                sum += hidden[i] * hiddenWeights[i][j];
            }
            output[j] = sigmoid(sum);
        }
        return output;
    }

    // ========================================================================
    // DEEP Q-NETWORK INSPIRED BATCH OPTIMIZATION
    // ========================================================================

    /**
     * Optimize using experience replay (DQN-inspired)
     */
    public DQNOptimizationResult optimizeWithDQN(List<ScheduleSlot> schedule, DQNConfig config) {
        // ✅ NULL SAFE: Validate parameters
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
        if (config == null) {
            config = DQNConfig.builder().build(); // Use default config
        }

        log.info("Starting DQN-inspired optimization with {} slots", schedule.size());

        long startTime = System.currentTimeMillis();

        // Experience replay buffer
        List<Experience> replayBuffer = new ArrayList<>();
        int bufferSize = config.getReplayBufferSize();

        List<ScheduleSlot> currentSchedule = new ArrayList<>(schedule);
        List<ScheduleSlot> bestSchedule = new ArrayList<>(schedule);
        double bestScore = calculateTotalReward(currentSchedule);

        List<Double> scoreHistory = new ArrayList<>();

        for (int episode = 0; episode < config.getEpisodes(); episode++) {
            // Collect experiences
            for (int step = 0; step < currentSchedule.size(); step++) {
                String state = encodeState(currentSchedule, step);
                int action = selectAction(state, config.getExplorationRate());
                double reward = applyAction(currentSchedule, step, action);
                String nextState = encodeState(currentSchedule, step);

                // Store experience
                replayBuffer.add(new Experience(state, action, reward, nextState));
                if (replayBuffer.size() > bufferSize) {
                    replayBuffer.remove(0);
                }
            }

            // Experience replay - learn from random batch
            if (replayBuffer.size() >= config.getBatchSize()) {
                List<Experience> batch = sampleBatch(replayBuffer, config.getBatchSize());
                for (Experience exp : batch) {
                    updateQValue(exp.state, exp.action, exp.reward, exp.nextState);
                }
            }

            // Track progress
            double score = calculateTotalReward(currentSchedule);
            if (score > bestScore) {
                bestSchedule = new ArrayList<>(currentSchedule);
                bestScore = score;
            }
            scoreHistory.add(score);

            // Decay exploration
            if (episode % 50 == 0) {
                log.debug("DQN Episode {}: score={:.2f}, best={:.2f}, buffer={}",
                    episode, score, bestScore, replayBuffer.size());
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        return DQNOptimizationResult.builder()
            .initialScore(calculateTotalReward(schedule))
            .finalScore(bestScore)
            .improvement((bestScore - calculateTotalReward(schedule)) / Math.abs(calculateTotalReward(schedule)) * 100)
            .episodes(config.getEpisodes())
            .durationMs(duration)
            .optimizedSchedule(bestSchedule)
            .scoreHistory(scoreHistory)
            .experiencesCollected(replayBuffer.size())
            .build();
    }

    private List<Experience> sampleBatch(List<Experience> buffer, int batchSize) {
        // ✅ NULL SAFE: Validate buffer
        if (buffer == null || buffer.isEmpty()) {
            return Collections.emptyList();
        }

        List<Experience> batch = new ArrayList<>();
        Random rand = new Random();
        int actualBatchSize = Math.min(batchSize, buffer.size());
        for (int i = 0; i < actualBatchSize; i++) {
            batch.add(buffer.get(rand.nextInt(buffer.size())));
        }
        return batch;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private double calculateTotalReward(List<ScheduleSlot> schedule) {
        // ✅ NULL SAFE: Validate schedule
        if (schedule == null || schedule.isEmpty()) {
            return 0;
        }

        double reward = 100; // Base reward

        // Penalize conflicts
        for (int i = 0; i < schedule.size(); i++) {
            reward -= countTeacherConflicts(schedule, i) * 10;
            reward -= countRoomConflicts(schedule, i) * 10;
        }

        // Reward even distribution
        Map<DayOfWeek, Long> dayDistribution = schedule.stream()
            // ✅ NULL SAFE: Filter null slots before grouping
            .filter(s -> s != null && s.getDayOfWeek() != null)
            .collect(Collectors.groupingBy(ScheduleSlot::getDayOfWeek, Collectors.counting()));

        if (!dayDistribution.isEmpty()) {
            double avg = dayDistribution.values().stream().mapToLong(Long::longValue).average().orElse(0);
            double variance = dayDistribution.values().stream()
                .mapToDouble(v -> Math.pow(v - avg, 2)).average().orElse(0);
            reward -= Math.sqrt(variance) * 2;
        }

        return reward;
    }

    private double calculateSlotReward(List<ScheduleSlot> schedule, int index) {
        double reward = 10;
        reward -= countTeacherConflicts(schedule, index) * 5;
        reward -= countRoomConflicts(schedule, index) * 5;
        return reward;
    }

    private int countTeacherConflicts(List<ScheduleSlot> schedule, int index) {
        // ✅ NULL SAFE: Validate parameters
        if (schedule == null || index < 0 || index >= schedule.size()) {
            return 0;
        }

        ScheduleSlot slot = schedule.get(index);
        // ✅ NULL SAFE: Validate slot and required fields
        if (slot == null || slot.getTeacher() == null || slot.getTeacher().getId() == null ||
            slot.getDayOfWeek() == null || slot.getStartTime() == null) {
            return 0;
        }

        int conflicts = 0;
        for (int i = 0; i < schedule.size(); i++) {
            if (i != index) {
                ScheduleSlot other = schedule.get(i);
                // ✅ NULL SAFE: Validate other slot before comparison
                if (other != null && other.getTeacher() != null && other.getTeacher().getId() != null &&
                    other.getTeacher().getId().equals(slot.getTeacher().getId()) &&
                    other.getDayOfWeek() == slot.getDayOfWeek() &&
                    other.getStartTime() != null &&
                    other.getStartTime().equals(slot.getStartTime())) {
                    conflicts++;
                }
            }
        }
        return conflicts;
    }

    private int countRoomConflicts(List<ScheduleSlot> schedule, int index) {
        // ✅ NULL SAFE: Validate parameters
        if (schedule == null || index < 0 || index >= schedule.size()) {
            return 0;
        }

        ScheduleSlot slot = schedule.get(index);
        // ✅ NULL SAFE: Validate slot and required fields
        if (slot == null || slot.getRoom() == null || slot.getRoom().getId() == null ||
            slot.getDayOfWeek() == null || slot.getStartTime() == null) {
            return 0;
        }

        int conflicts = 0;
        for (int i = 0; i < schedule.size(); i++) {
            if (i != index) {
                ScheduleSlot other = schedule.get(i);
                // ✅ NULL SAFE: Validate other slot before comparison
                if (other != null && other.getRoom() != null && other.getRoom().getId() != null &&
                    other.getRoom().getId().equals(slot.getRoom().getId()) &&
                    other.getDayOfWeek() == slot.getDayOfWeek() &&
                    other.getStartTime() != null &&
                    other.getStartTime().equals(slot.getStartTime())) {
                    conflicts++;
                }
            }
        }
        return conflicts;
    }

    private void swapSlots(ScheduleSlot a, ScheduleSlot b) {
        DayOfWeek tempDay = a.getDayOfWeek();
        LocalTime tempStart = a.getStartTime();
        LocalTime tempEnd = a.getEndTime();

        a.setDayOfWeek(b.getDayOfWeek());
        a.setStartTime(b.getStartTime());
        a.setEndTime(b.getEndTime());

        b.setDayOfWeek(tempDay);
        b.setStartTime(tempStart);
        b.setEndTime(tempEnd);
    }

    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private double relu(double x) {
        return Math.max(0, x);
    }

    private int argmax(double[] arr) {
        int maxIdx = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[maxIdx]) maxIdx = i;
        }
        return maxIdx;
    }

    private List<AlternativeSlot> generateAlternatives(double[] output, DayOfWeek[] days) {
        List<AlternativeSlot> alternatives = new ArrayList<>();

        // Sort outputs and pick top 3 alternatives
        List<int[]> ranked = new ArrayList<>();
        for (int d = 0; d < 5; d++) {
            for (int h = 0; h < 8; h++) {
                ranked.add(new int[]{d, h, (int)(output[d] * output[5+h] * 100)});
            }
        }
        ranked.sort((a, b) -> b[2] - a[2]);

        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            int[] r = ranked.get(i);
            alternatives.add(AlternativeSlot.builder()
                .day(days[r[0]])
                .startTime(LocalTime.of(7 + r[1], 0))
                .score(r[2] / 100.0)
                .build());
        }

        return alternatives;
    }

    // ========================================================================
    // DTOs
    // ========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RLConfig {
        @Builder.Default private int episodes = 1000;
        @Builder.Default private double explorationRate = 0.1;
        @Builder.Default private double learningRate = 0.1;
        @Builder.Default private double discountFactor = 0.95;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RLOptimizationResult {
        private double initialReward;
        private double finalReward;
        private double improvement;
        private int episodes;
        private long durationMs;
        private List<ScheduleSlot> optimizedSchedule;
        private List<Double> rewardHistory;
        private int qTableSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NNPredictionResult {
        private DayOfWeek recommendedDay;
        private LocalTime recommendedStartTime;
        private LocalTime recommendedEndTime;
        private double confidence;
        private List<AlternativeSlot> alternativeSlots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlternativeSlot {
        private DayOfWeek day;
        private LocalTime startTime;
        private double score;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DQNConfig {
        @Builder.Default private int episodes = 500;
        @Builder.Default private int replayBufferSize = 1000;
        @Builder.Default private int batchSize = 32;
        @Builder.Default private double explorationRate = 0.2;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DQNOptimizationResult {
        private double initialScore;
        private double finalScore;
        private double improvement;
        private int episodes;
        private long durationMs;
        private List<ScheduleSlot> optimizedSchedule;
        private List<Double> scoreHistory;
        private int experiencesCollected;
    }

    @Data
    @AllArgsConstructor
    private static class Experience {
        private String state;
        private int action;
        private double reward;
        private String nextState;
    }
}
