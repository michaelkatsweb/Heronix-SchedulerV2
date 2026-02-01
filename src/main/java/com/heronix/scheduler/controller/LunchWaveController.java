// Location: src/main/java/com/eduscheduler/controller/LunchWaveController.java
package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.LunchWave;
import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.dto.ScheduleGenerationRequest;
import com.heronix.scheduler.repository.LunchWaveRepository;
import com.heronix.scheduler.repository.ScheduleRepository;
import com.heronix.scheduler.service.LunchWaveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * Lunch Wave REST API Controller
 * Location: src/main/java/com/eduscheduler/controller/LunchWaveController.java
 *
 * Phase 5D: UI Integration & Testing
 *
 * Endpoints:
 * - GET /api/lunch-waves?scheduleId={id} - Get all lunch waves for schedule
 * - GET /api/lunch-waves/{id} - Get lunch wave by ID
 * - POST /api/lunch-waves - Create new lunch wave
 * - POST /api/lunch-waves/bulk - Create multiple lunch waves
 * - PUT /api/lunch-waves/{id} - Update lunch wave
 * - PUT /api/lunch-waves/{id}/capacity - Update capacity
 * - PUT /api/lunch-waves/{id}/times - Update times
 * - PUT /api/lunch-waves/{id}/activate - Activate wave
 * - PUT /api/lunch-waves/{id}/deactivate - Deactivate wave
 * - DELETE /api/lunch-waves/{id} - Delete lunch wave
 * - DELETE /api/lunch-waves/schedule/{scheduleId} - Delete all waves for schedule
 * - GET /api/lunch-waves/active?scheduleId={id} - Get active lunch waves
 * - GET /api/lunch-waves/available?scheduleId={id} - Get available waves (not full)
 * - GET /api/lunch-waves/stats?scheduleId={id} - Get wave statistics
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-01
 */
@Slf4j
@RestController
@RequestMapping("/api/lunch-waves")
@CrossOrigin(origins = {"http://localhost:9580", "http://localhost:9585", "http://localhost:9590", "http://localhost:58280", "http://localhost:58180"})
@RequiredArgsConstructor
public class LunchWaveController {

    private final LunchWaveService lunchWaveService;
    private final LunchWaveRepository lunchWaveRepository;
    private final ScheduleRepository scheduleRepository;

    // ========== Query Endpoints ==========

    /**
     * GET /api/lunch-waves?scheduleId={id}
     * Get all lunch waves for a schedule
     */
    @GetMapping
    public ResponseEntity<List<LunchWave>> getLunchWaves(@RequestParam Long scheduleId) {
        log.info("GET /api/lunch-waves?scheduleId={}", scheduleId);

        List<LunchWave> waves = lunchWaveService.getAllLunchWaves(scheduleId);
        return ResponseEntity.ok(waves);
    }

    /**
     * GET /api/lunch-waves/{id}
     * Get lunch wave by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LunchWave> getLunchWaveById(@PathVariable Long id) {
        log.info("GET /api/lunch-waves/{}", id);

        return lunchWaveService.getLunchWaveById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/lunch-waves/active?scheduleId={id}
     * Get only active lunch waves for a schedule
     */
    @GetMapping("/active")
    public ResponseEntity<List<LunchWave>> getActiveLunchWaves(@RequestParam Long scheduleId) {
        log.info("GET /api/lunch-waves/active?scheduleId={}", scheduleId);

        List<LunchWave> waves = lunchWaveService.getActiveLunchWaves(scheduleId);
        return ResponseEntity.ok(waves);
    }

    /**
     * GET /api/lunch-waves/available?scheduleId={id}
     * Get lunch waves with available capacity
     */
    @GetMapping("/available")
    public ResponseEntity<List<LunchWave>> getAvailableLunchWaves(@RequestParam Long scheduleId) {
        log.info("GET /api/lunch-waves/available?scheduleId={}", scheduleId);

        List<LunchWave> waves = lunchWaveService.getAvailableLunchWaves(scheduleId);
        return ResponseEntity.ok(waves);
    }

    /**
     * GET /api/lunch-waves/stats?scheduleId={id}
     * Get statistics for lunch waves
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getLunchWaveStats(@RequestParam Long scheduleId) {
        log.info("GET /api/lunch-waves/stats?scheduleId={}", scheduleId);

        int totalCapacity = lunchWaveService.getTotalCapacity(scheduleId);
        int totalAssignments = lunchWaveService.getTotalAssignments(scheduleId);
        double utilization = lunchWaveService.getOverallUtilization(scheduleId);
        long activeWaves = lunchWaveService.countActiveLunchWaves(scheduleId);
        boolean isValid = lunchWaveService.isLunchWaveConfigurationValid(scheduleId);

        Map<String, Object> stats = Map.of(
            "totalCapacity", totalCapacity,
            "totalAssignments", totalAssignments,
            "utilizationPercent", utilization,
            "activeWaveCount", activeWaves,
            "isConfigurationValid", isValid
        );

        return ResponseEntity.ok(stats);
    }

    // ========== Create Endpoints ==========

    /**
     * POST /api/lunch-waves
     * Create a single lunch wave
     *
     * Request body:
     * {
     *   "scheduleId": 1,
     *   "waveName": "Lunch 1",
     *   "waveOrder": 1,
     *   "startTime": "10:04:00",
     *   "endTime": "10:34:00",
     *   "maxCapacity": 250,
     *   "gradeLevelRestriction": null
     * }
     */
    @PostMapping
    public ResponseEntity<LunchWave> createLunchWave(@RequestBody CreateLunchWaveRequest request) {
        log.info("POST /api/lunch-waves - Creating lunch wave: {}", request.getWaveName());

        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + request.getScheduleId()));

        LunchWave wave = lunchWaveService.createLunchWave(
            schedule,
            request.getWaveName(),
            request.getWaveOrder(),
            request.getStartTime(),
            request.getEndTime(),
            request.getMaxCapacity(),
            request.getGradeLevelRestriction()
        );

        log.info("Created lunch wave: ID={}, Name={}", wave.getId(), wave.getWaveName());
        return ResponseEntity.status(HttpStatus.CREATED).body(wave);
    }

    /**
     * POST /api/lunch-waves/bulk
     * Create multiple lunch waves from template or custom config
     *
     * Request body:
     * {
     *   "scheduleId": 1,
     *   "template": "WEEKI_WACHEE",  // or "PARROTT_MS", "CUSTOM"
     *   "customConfig": {
     *     "waveCount": 3,
     *     "firstLunchStart": "10:04:00",
     *     "lunchDuration": 30,
     *     "gapBetweenLunches": 24,
     *     "capacity": 250
     *   }
     * }
     */
    @PostMapping("/bulk")
    public ResponseEntity<List<LunchWave>> createLunchWavesBulk(@RequestBody CreateBulkLunchWavesRequest request) {
        log.info("POST /api/lunch-waves/bulk - Template: {}", request.getTemplate());

        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + request.getScheduleId()));

        List<LunchWave> waves;

        switch (request.getTemplate().toUpperCase()) {
            case "WEEKI_WACHEE":
                waves = lunchWaveService.createWeekiWacheeTemplate(schedule);
                break;

            case "PARROTT_MS":
                waves = lunchWaveService.createParrottMSTemplate(schedule);
                break;

            case "CUSTOM":
                if (request.getCustomConfig() == null) {
                    return ResponseEntity.badRequest().build();
                }
                var config = request.getCustomConfig();
                waves = lunchWaveService.createCustomLunchWaves(
                    schedule,
                    config.getWaveCount(),
                    config.getFirstLunchStart(),
                    config.getLunchDuration(),
                    config.getGapBetweenLunches(),
                    config.getCapacity()
                );
                break;

            default:
                return ResponseEntity.badRequest().build();
        }

        log.info("Created {} lunch waves using template: {}", waves.size(), request.getTemplate());
        return ResponseEntity.status(HttpStatus.CREATED).body(waves);
    }

    // ========== Update Endpoints ==========

    /**
     * PUT /api/lunch-waves/{id}
     * Update lunch wave details
     */
    @PutMapping("/{id}")
    public ResponseEntity<LunchWave> updateLunchWave(
            @PathVariable Long id,
            @RequestBody UpdateLunchWaveRequest request) {
        log.info("PUT /api/lunch-waves/{}", id);

        return lunchWaveService.getLunchWaveById(id)
                .map(wave -> {
                    if (request.getWaveName() != null) {
                        wave.setWaveName(request.getWaveName());
                    }
                    if (request.getStartTime() != null) {
                        wave.setStartTime(request.getStartTime());
                    }
                    if (request.getEndTime() != null) {
                        wave.setEndTime(request.getEndTime());
                    }
                    if (request.getMaxCapacity() != null) {
                        wave.setMaxCapacity(request.getMaxCapacity());
                    }
                    if (request.getGradeLevelRestriction() != null) {
                        wave.setGradeLevelRestriction(request.getGradeLevelRestriction());
                    }

                    LunchWave updated = lunchWaveService.updateLunchWave(wave);
                    log.info("Updated lunch wave: ID={}", updated.getId());
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/lunch-waves/{id}/capacity
     * Update only the capacity
     */
    @PutMapping("/{id}/capacity")
    public ResponseEntity<LunchWave> updateCapacity(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {
        log.info("PUT /api/lunch-waves/{}/capacity - New capacity: {}", id, body.get("capacity"));

        Integer newCapacity = body.get("capacity");
        if (newCapacity == null || newCapacity < 0) {
            return ResponseEntity.badRequest().build();
        }

        try {
            LunchWave updated = lunchWaveService.updateCapacity(id, newCapacity);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Lunch wave not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PUT /api/lunch-waves/{id}/times
     * Update start and end times
     */
    @PutMapping("/{id}/times")
    public ResponseEntity<LunchWave> updateTimes(
            @PathVariable Long id,
            @RequestBody UpdateTimesRequest request) {
        log.info("PUT /api/lunch-waves/{}/times - New times: {} - {}",
            id, request.getStartTime(), request.getEndTime());

        try {
            LunchWave updated = lunchWaveService.updateTimes(
                id,
                request.getStartTime(),
                request.getEndTime()
            );
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            log.error("Lunch wave not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PUT /api/lunch-waves/{id}/activate
     * Activate a lunch wave
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<LunchWave> activateWave(@PathVariable Long id) {
        log.info("PUT /api/lunch-waves/{}/activate", id);

        try {
            LunchWave updated = lunchWaveService.activateWave(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * PUT /api/lunch-waves/{id}/deactivate
     * Deactivate a lunch wave
     */
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<LunchWave> deactivateWave(@PathVariable Long id) {
        log.info("PUT /api/lunch-waves/{}/deactivate", id);

        try {
            LunchWave updated = lunchWaveService.deactivateWave(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========== Delete Endpoints ==========

    /**
     * DELETE /api/lunch-waves/{id}
     * Delete a single lunch wave
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLunchWave(@PathVariable Long id) {
        log.info("DELETE /api/lunch-waves/{}", id);

        if (!lunchWaveRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        lunchWaveService.deleteLunchWave(id);
        log.info("Deleted lunch wave: ID={}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * DELETE /api/lunch-waves/schedule/{scheduleId}
     * Delete all lunch waves for a schedule
     */
    @DeleteMapping("/schedule/{scheduleId}")
    public ResponseEntity<Map<String, Object>> deleteAllLunchWaves(@PathVariable Long scheduleId) {
        log.info("DELETE /api/lunch-waves/schedule/{}", scheduleId);

        List<LunchWave> waves = lunchWaveService.getAllLunchWaves(scheduleId);
        int count = waves.size();

        lunchWaveService.deleteAllLunchWaves(scheduleId);

        log.info("Deleted {} lunch waves for schedule {}", count, scheduleId);
        return ResponseEntity.ok(Map.of("deletedCount", count));
    }

    // ========== Request/Response DTOs ==========

    /**
     * Request body for creating a lunch wave
     */
    public static class CreateLunchWaveRequest {
        private Long scheduleId;
        private String waveName;
        private Integer waveOrder;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer maxCapacity;
        private Integer gradeLevelRestriction;

        // Getters and setters
        public Long getScheduleId() { return scheduleId; }
        public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

        public String getWaveName() { return waveName; }
        public void setWaveName(String waveName) { this.waveName = waveName; }

        public Integer getWaveOrder() { return waveOrder; }
        public void setWaveOrder(Integer waveOrder) { this.waveOrder = waveOrder; }

        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

        public Integer getMaxCapacity() { return maxCapacity; }
        public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

        public Integer getGradeLevelRestriction() { return gradeLevelRestriction; }
        public void setGradeLevelRestriction(Integer gradeLevelRestriction) {
            this.gradeLevelRestriction = gradeLevelRestriction;
        }
    }

    /**
     * Request body for bulk creation
     */
    public static class CreateBulkLunchWavesRequest {
        private Long scheduleId;
        private String template;  // "WEEKI_WACHEE", "PARROTT_MS", "CUSTOM"
        private CustomConfig customConfig;

        // Getters and setters
        public Long getScheduleId() { return scheduleId; }
        public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

        public String getTemplate() { return template; }
        public void setTemplate(String template) { this.template = template; }

        public CustomConfig getCustomConfig() { return customConfig; }
        public void setCustomConfig(CustomConfig customConfig) { this.customConfig = customConfig; }
    }

    /**
     * Custom configuration for bulk creation
     */
    public static class CustomConfig {
        private Integer waveCount;
        private LocalTime firstLunchStart;
        private Integer lunchDuration;
        private Integer gapBetweenLunches;
        private Integer capacity;

        // Getters and setters
        public Integer getWaveCount() { return waveCount; }
        public void setWaveCount(Integer waveCount) { this.waveCount = waveCount; }

        public LocalTime getFirstLunchStart() { return firstLunchStart; }
        public void setFirstLunchStart(LocalTime firstLunchStart) { this.firstLunchStart = firstLunchStart; }

        public Integer getLunchDuration() { return lunchDuration; }
        public void setLunchDuration(Integer lunchDuration) { this.lunchDuration = lunchDuration; }

        public Integer getGapBetweenLunches() { return gapBetweenLunches; }
        public void setGapBetweenLunches(Integer gapBetweenLunches) { this.gapBetweenLunches = gapBetweenLunches; }

        public Integer getCapacity() { return capacity; }
        public void setCapacity(Integer capacity) { this.capacity = capacity; }
    }

    /**
     * Request body for updating a lunch wave
     */
    public static class UpdateLunchWaveRequest {
        private String waveName;
        private LocalTime startTime;
        private LocalTime endTime;
        private Integer maxCapacity;
        private Integer gradeLevelRestriction;

        // Getters and setters
        public String getWaveName() { return waveName; }
        public void setWaveName(String waveName) { this.waveName = waveName; }

        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

        public Integer getMaxCapacity() { return maxCapacity; }
        public void setMaxCapacity(Integer maxCapacity) { this.maxCapacity = maxCapacity; }

        public Integer getGradeLevelRestriction() { return gradeLevelRestriction; }
        public void setGradeLevelRestriction(Integer gradeLevelRestriction) {
            this.gradeLevelRestriction = gradeLevelRestriction;
        }
    }

    /**
     * Request body for updating times
     */
    public static class UpdateTimesRequest {
        private LocalTime startTime;
        private LocalTime endTime;

        // Getters and setters
        public LocalTime getStartTime() { return startTime; }
        public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

        public LocalTime getEndTime() { return endTime; }
        public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    }
}
