package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.LunchWave;
import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.dto.ScheduleGenerationRequest;
import com.heronix.scheduler.repository.LunchWaveRepository;
import com.heronix.scheduler.service.LunchWaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of LunchWaveService
 *
 * Manages creation, update, and querying of lunch waves for schedules
 *
 * Phase 5B: Multiple Rotating Lunch Periods - Service Layer
 * Date: December 1, 2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LunchWaveServiceImpl implements LunchWaveService {

    private final LunchWaveRepository lunchWaveRepository;

    // ========== Creation Methods ==========

    @Override
    public List<LunchWave> createLunchWavesForSchedule(Schedule schedule, ScheduleGenerationRequest request) {
        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }

        log.info("Creating lunch waves for schedule ID: {}", schedule.getId());

        // ✅ NULL SAFE: Check request and its configs
        if (request != null && request.getLunchWaveConfigs() != null && !request.getLunchWaveConfigs().isEmpty()) {
            log.debug("Using custom lunch wave configurations ({} waves)", request.getLunchWaveConfigs().size());
            return createFromConfigs(schedule, request.getLunchWaveConfigs());
        }

        // Use default template based on wave count
        // ✅ NULL SAFE: Safe extraction with defaults when request is null
        int waveCount = (request != null && request.getLunchWaveCount() != null)
            ? request.getLunchWaveCount() : 1;
        LocalTime firstLunchStart = (request != null && request.getLunchStartTime() != null)
            ? request.getLunchStartTime() : LocalTime.of(11, 0);
        int lunchDuration = (request != null && request.getLunchDuration() != null)
            ? request.getLunchDuration() : 30;

        log.debug("Creating {} lunch waves starting at {} for {} minutes each",
            waveCount, firstLunchStart, lunchDuration);

        return createCustomLunchWaves(schedule, waveCount, firstLunchStart, lunchDuration, 30, 250);
    }

    private List<LunchWave> createFromConfigs(Schedule schedule, List<ScheduleGenerationRequest.LunchWaveConfig> configs) {
        List<LunchWave> waves = new ArrayList<>();

        for (ScheduleGenerationRequest.LunchWaveConfig config : configs) {
            // ✅ NULL SAFE: Skip null configs
            if (config == null) continue;

            LunchWave wave = createLunchWave(
                schedule,
                config.getWaveName(),
                config.getWaveOrder(),
                config.getStartTime(),
                config.getEndTime(),
                config.getMaxCapacity() != null ? config.getMaxCapacity() : 250,
                config.getGradeLevelRestriction()
            );
            waves.add(wave);
        }

        log.info("Created {} lunch waves from custom configurations", waves.size());
        return waves;
    }

    @Override
    public LunchWave createLunchWave(
        Schedule schedule,
        String name,
        int order,
        LocalTime startTime,
        LocalTime endTime,
        int maxCapacity
    ) {
        return createLunchWave(schedule, name, order, startTime, endTime, maxCapacity, null);
    }

    @Override
    public LunchWave createLunchWave(
        Schedule schedule,
        String name,
        int order,
        LocalTime startTime,
        LocalTime endTime,
        int maxCapacity,
        Integer gradeLevel
    ) {
        log.debug("Creating lunch wave: {} (order {}) from {} to {}, capacity {}",
            name, order, startTime, endTime, maxCapacity);

        LunchWave wave = LunchWave.builder()
            .schedule(schedule)
            .waveName(name)
            .waveOrder(order)
            .startTime(startTime)
            .endTime(endTime)
            .maxCapacity(maxCapacity)
            .currentAssignments(0)
            .gradeLevelRestriction(gradeLevel)
            .isActive(true)
            .build();

        wave = lunchWaveRepository.save(wave);
        // ✅ NULL SAFE: Safe extraction of wave summary
        String summary = (wave != null && wave.getSummary() != null)
            ? wave.getSummary() : "Unknown";
        log.info("Created lunch wave: {}", summary);

        return wave;
    }

    // ========== Template Methods ==========

    @Override
    public List<LunchWave> createWeekiWacheeTemplate(Schedule schedule) {
        log.info("Creating Weeki Wachee HS template (3 waves, 250 capacity)");

        List<LunchWave> waves = new ArrayList<>();

        waves.add(createLunchWave(schedule, "Lunch 1", 1,
            LocalTime.of(10, 4), LocalTime.of(10, 34), 250));

        waves.add(createLunchWave(schedule, "Lunch 2", 2,
            LocalTime.of(10, 58), LocalTime.of(11, 28), 250));

        waves.add(createLunchWave(schedule, "Lunch 3", 3,
            LocalTime.of(11, 52), LocalTime.of(12, 22), 250));

        log.info("Created Weeki Wachee template: 3 lunch waves");
        return waves;
    }

    @Override
    public List<LunchWave> createParrottMSTemplate(Schedule schedule) {
        log.info("Creating Parrott MS template (3 grade-level lunches, 300 capacity)");

        List<LunchWave> waves = new ArrayList<>();

        waves.add(createLunchWave(schedule, "6th Grade Lunch", 1,
            LocalTime.of(11, 0), LocalTime.of(11, 30), 300, 6));

        waves.add(createLunchWave(schedule, "7th Grade Lunch", 2,
            LocalTime.of(11, 35), LocalTime.of(12, 5), 300, 7));

        waves.add(createLunchWave(schedule, "8th Grade Lunch", 3,
            LocalTime.of(12, 10), LocalTime.of(12, 40), 300, 8));

        log.info("Created Parrott MS template: 3 grade-level lunch waves");
        return waves;
    }

    @Override
    public List<LunchWave> createCustomLunchWaves(
        Schedule schedule,
        int count,
        LocalTime firstLunchStart,
        int lunchDuration,
        int gapBetweenLunches,
        int capacity
    ) {
        log.info("Creating {} custom lunch waves, starting at {}, {} minutes each, {} minute gaps, capacity {}",
            count, firstLunchStart, lunchDuration, gapBetweenLunches, capacity);

        List<LunchWave> waves = new ArrayList<>();
        LocalTime currentStart = firstLunchStart;

        for (int i = 1; i <= count; i++) {
            LocalTime currentEnd = currentStart.plusMinutes(lunchDuration);

            LunchWave wave = createLunchWave(
                schedule,
                "Lunch " + i,
                i,
                currentStart,
                currentEnd,
                capacity
            );
            waves.add(wave);

            // Next lunch starts after current lunch + gap
            currentStart = currentEnd.plusMinutes(gapBetweenLunches);
        }

        log.info("Created {} custom lunch waves", waves.size());
        return waves;
    }

    // ========== Update Methods ==========

    @Override
    public LunchWave updateLunchWave(LunchWave wave) {
        // ✅ NULL SAFE: Validate wave parameter
        if (wave == null) {
            throw new IllegalArgumentException("Lunch wave cannot be null");
        }
        log.debug("Updating lunch wave ID: {}", wave.getId());
        return lunchWaveRepository.save(wave);
    }

    @Override
    public LunchWave updateCapacity(Long waveId, int newCapacity) {
        log.info("Updating lunch wave {} capacity to {}", waveId, newCapacity);

        LunchWave wave = lunchWaveRepository.findById(waveId)
            .orElseThrow(() -> new IllegalArgumentException("Lunch wave not found: " + waveId));

        wave.setMaxCapacity(newCapacity);
        return lunchWaveRepository.save(wave);
    }

    @Override
    public LunchWave updateTimes(Long waveId, LocalTime newStartTime, LocalTime newEndTime) {
        log.info("Updating lunch wave {} times to {} - {}", waveId, newStartTime, newEndTime);

        LunchWave wave = lunchWaveRepository.findById(waveId)
            .orElseThrow(() -> new IllegalArgumentException("Lunch wave not found: " + waveId));

        wave.setStartTime(newStartTime);
        wave.setEndTime(newEndTime);
        return lunchWaveRepository.save(wave);
    }

    @Override
    public LunchWave activateWave(Long waveId) {
        log.info("Activating lunch wave {}", waveId);

        LunchWave wave = lunchWaveRepository.findById(waveId)
            .orElseThrow(() -> new IllegalArgumentException("Lunch wave not found: " + waveId));

        wave.setIsActive(true);
        return lunchWaveRepository.save(wave);
    }

    @Override
    public LunchWave deactivateWave(Long waveId) {
        log.info("Deactivating lunch wave {}", waveId);

        LunchWave wave = lunchWaveRepository.findById(waveId)
            .orElseThrow(() -> new IllegalArgumentException("Lunch wave not found: " + waveId));

        wave.setIsActive(false);
        return lunchWaveRepository.save(wave);
    }

    // ========== Delete Methods ==========

    @Override
    public void deleteLunchWave(Long waveId) {
        log.info("Deleting lunch wave {}", waveId);
        lunchWaveRepository.deleteById(waveId);
    }

    @Override
    public void deleteAllLunchWaves(Long scheduleId) {
        log.info("Deleting all lunch waves for schedule {}", scheduleId);
        List<LunchWave> waves = lunchWaveRepository.findByScheduleIdOrderByWaveOrderAsc(scheduleId);
        lunchWaveRepository.deleteAll(waves);
        log.info("Deleted {} lunch waves", waves.size());
    }

    // ========== Query Methods ==========

    @Override
    @Transactional(readOnly = true)
    public List<LunchWave> getAllLunchWaves(Long scheduleId) {
        return lunchWaveRepository.findByScheduleIdOrderByWaveOrderAsc(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchWave> getActiveLunchWaves(Long scheduleId) {
        return lunchWaveRepository.findActiveByScheduleId(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LunchWave> getLunchWaveById(Long waveId) {
        return lunchWaveRepository.findById(waveId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LunchWave> findLunchWaveByName(Long scheduleId, String waveName) {
        List<LunchWave> waves = lunchWaveRepository.findByScheduleIdOrderByWaveOrderAsc(scheduleId);
        // ✅ NULL SAFE: Filter null waves and check wave name before comparing
        return waves.stream()
            .filter(w -> w != null && w.getWaveName() != null && w.getWaveName().equals(waveName))
            .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchWave> getAvailableLunchWaves(Long scheduleId) {
        return lunchWaveRepository.findAvailableByScheduleId(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchWave> getFullLunchWaves(Long scheduleId) {
        return lunchWaveRepository.findFullByScheduleId(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LunchWave> findWaveWithMostCapacity(Long scheduleId) {
        return lunchWaveRepository.findWaveWithMostCapacity(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchWave> getAvailableWavesForGradeLevel(Long scheduleId, Integer gradeLevel) {
        return lunchWaveRepository.findAvailableForGradeLevel(scheduleId, gradeLevel);
    }

    // ========== Statistics Methods ==========

    @Override
    @Transactional(readOnly = true)
    public int getTotalCapacity(Long scheduleId) {
        Integer total = lunchWaveRepository.getTotalCapacityByScheduleId(scheduleId);
        return total != null ? total : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public int getTotalAssignments(Long scheduleId) {
        Integer total = lunchWaveRepository.getTotalAssignmentsByScheduleId(scheduleId);
        return total != null ? total : 0;
    }

    @Override
    @Transactional(readOnly = true)
    public double getOverallUtilization(Long scheduleId) {
        int capacity = getTotalCapacity(scheduleId);
        if (capacity == 0) {
            return 0.0;
        }
        int assignments = getTotalAssignments(scheduleId);
        return (assignments * 100.0) / capacity;
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveLunchWaves(Long scheduleId) {
        return lunchWaveRepository.countActiveByScheduleId(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasLunchWaves(Long scheduleId) {
        return countActiveLunchWaves(scheduleId) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLunchWaveConfigurationValid(Long scheduleId) {
        List<LunchWave> waves = getActiveLunchWaves(scheduleId);

        if (waves.isEmpty()) {
            log.warn("No active lunch waves found for schedule {}", scheduleId);
            return false;
        }

        // Check for sequential wave orders
        for (int i = 0; i < waves.size(); i++) {
            // ✅ NULL SAFE: Check wave exists before accessing properties
            LunchWave wave = waves.get(i);
            if (wave == null || wave.getWaveOrder() != i + 1) {
                log.warn("Lunch wave order is not sequential for schedule {}", scheduleId);
                return false;
            }
        }

        // Check for time overlaps
        for (int i = 0; i < waves.size() - 1; i++) {
            LunchWave current = waves.get(i);
            LunchWave next = waves.get(i + 1);

            // ✅ NULL SAFE: Check waves and their times exist before comparing
            if (current == null || next == null ||
                current.getEndTime() == null || next.getStartTime() == null) {
                log.warn("Null wave or time found in lunch wave configuration for schedule {}", scheduleId);
                return false;
            }

            if (current.getEndTime().isAfter(next.getStartTime())) {
                // ✅ NULL SAFE: Safe extraction of wave names
                String currentName = current.getWaveName() != null ? current.getWaveName() : "Unknown";
                String nextName = next.getWaveName() != null ? next.getWaveName() : "Unknown";
                log.warn("Lunch waves {} and {} have overlapping times for schedule {}",
                    currentName, nextName, scheduleId);
                return false;
            }
        }

        log.debug("Lunch wave configuration is valid for schedule {}", scheduleId);
        return true;
    }
}
