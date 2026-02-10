package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.OptimizationConfig;
import com.heronix.scheduler.model.domain.OptimizationResult;
import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.repository.ScheduleRepository;
import com.heronix.scheduler.service.OptimizationService;
import com.heronix.scheduler.service.OptimizationService.ConstraintViolation;
import com.heronix.scheduler.service.OptimizationService.FitnessBreakdown;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Optimization REST API Controller
 *
 * Provides endpoints for schedule optimization, fitness evaluation,
 * constraint checking, and configuration management.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/optimization")
@CrossOrigin(origins = {"http://localhost:9580", "http://localhost:9585", "http://localhost:9590", "http://localhost:58280", "http://localhost:58180"})
@RequiredArgsConstructor
public class OptimizationController {

    private final OptimizationService optimizationService;
    private final ScheduleRepository scheduleRepository;

    // ========== Optimization Operations ==========

    /**
     * POST /api/optimization/schedule/{scheduleId}/start
     * Start full optimization for a schedule
     */
    @PostMapping("/schedule/{scheduleId}/start")
    public ResponseEntity<OptimizationResult> startOptimization(
            @PathVariable Long scheduleId,
            @RequestBody(required = false) OptimizationConfig config) {
        log.info("POST /api/optimization/schedule/{}/start", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            OptimizationConfig effectiveConfig = (config != null)
                    ? config : optimizationService.getDefaultConfig();

            OptimizationResult result = optimizationService.optimizeSchedule(
                    schedule, effectiveConfig,
                    progress -> log.info("Optimization progress: {} - {}",
                            progress.getFormattedProgress(), progress.getStatusMessage())
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Optimization failed for schedule {}", scheduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/optimization/schedule/{scheduleId}/quick
     * Run quick optimization (fewer iterations, faster)
     */
    @PostMapping("/schedule/{scheduleId}/quick")
    public ResponseEntity<OptimizationResult> quickOptimize(@PathVariable Long scheduleId) {
        log.info("POST /api/optimization/schedule/{}/quick", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            OptimizationResult result = optimizationService.quickOptimize(schedule);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Quick optimization failed for schedule {}", scheduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/optimization/cancel/{resultId}
     * Cancel a running optimization
     */
    @PostMapping("/cancel/{resultId}")
    public ResponseEntity<Map<String, String>> cancelOptimization(@PathVariable Long resultId) {
        log.info("POST /api/optimization/cancel/{}", resultId);

        try {
            optimizationService.cancelOptimization(resultId);
            return ResponseEntity.ok(Map.of("status", "CANCELLED", "resultId", resultId.toString()));
        } catch (Exception e) {
            log.error("Failed to cancel optimization {}", resultId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Fitness & Validation ==========

    /**
     * GET /api/optimization/schedule/{scheduleId}/fitness
     * Get fitness evaluation and breakdown for a schedule
     */
    @GetMapping("/schedule/{scheduleId}/fitness")
    public ResponseEntity<Map<String, Object>> getFitness(@PathVariable Long scheduleId) {
        log.info("GET /api/optimization/schedule/{}/fitness", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            OptimizationConfig config = optimizationService.getDefaultConfig();
            double fitness = optimizationService.evaluateFitness(schedule, config);
            FitnessBreakdown breakdown = optimizationService.getFitnessBreakdown(schedule, config);

            return ResponseEntity.ok(Map.of(
                "fitness", fitness,
                "breakdown", breakdown
            ));
        } catch (Exception e) {
            log.error("Fitness evaluation failed for schedule {}", scheduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/optimization/schedule/{scheduleId}/violations
     * Get all constraint violations for a schedule
     */
    @GetMapping("/schedule/{scheduleId}/violations")
    public ResponseEntity<List<ConstraintViolation>> getViolations(@PathVariable Long scheduleId) {
        log.info("GET /api/optimization/schedule/{}/violations", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<ConstraintViolation> violations = optimizationService.getAllViolations(schedule);
            return ResponseEntity.ok(violations);
        } catch (Exception e) {
            log.error("Failed to get violations for schedule {}", scheduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/optimization/schedule/{scheduleId}/validate
     * Validate whether a schedule satisfies all hard constraints
     */
    @GetMapping("/schedule/{scheduleId}/validate")
    public ResponseEntity<Map<String, Object>> validateSchedule(@PathVariable Long scheduleId) {
        log.info("GET /api/optimization/schedule/{}/validate", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            boolean valid = optimizationService.satisfiesHardConstraints(schedule);
            return ResponseEntity.ok(Map.of(
                "scheduleId", scheduleId,
                "satisfiesHardConstraints", valid
            ));
        } catch (Exception e) {
            log.error("Validation failed for schedule {}", scheduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Configuration Management ==========

    /**
     * GET /api/optimization/config
     * Get all saved optimization configurations
     */
    @GetMapping("/config")
    public ResponseEntity<List<OptimizationConfig>> getAllConfigs() {
        log.info("GET /api/optimization/config");
        List<OptimizationConfig> configs = optimizationService.getAllConfigs();
        return ResponseEntity.ok(configs);
    }

    /**
     * GET /api/optimization/config/default
     * Get the default optimization configuration
     */
    @GetMapping("/config/default")
    public ResponseEntity<OptimizationConfig> getDefaultConfig() {
        log.info("GET /api/optimization/config/default");
        OptimizationConfig config = optimizationService.getDefaultConfig();
        return ResponseEntity.ok(config);
    }

    /**
     * POST /api/optimization/config
     * Save a new optimization configuration
     */
    @PostMapping("/config")
    public ResponseEntity<OptimizationConfig> saveConfig(@RequestBody OptimizationConfig config) {
        log.info("POST /api/optimization/config - Saving config: {}", config.getConfigName());

        try {
            OptimizationConfig saved = optimizationService.saveConfig(config);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            log.error("Failed to save optimization config", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/optimization/config/{configId}
     * Delete an optimization configuration
     */
    @DeleteMapping("/config/{configId}")
    public ResponseEntity<Void> deleteConfig(@PathVariable Long configId) {
        log.info("DELETE /api/optimization/config/{}", configId);

        try {
            optimizationService.deleteConfig(configId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Failed to delete optimization config {}", configId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Result Management ==========

    /**
     * GET /api/optimization/result/{resultId}
     * Get an optimization result by ID
     */
    @GetMapping("/result/{resultId}")
    public ResponseEntity<OptimizationResult> getResult(@PathVariable Long resultId) {
        log.info("GET /api/optimization/result/{}", resultId);

        try {
            OptimizationResult result = optimizationService.getResult(resultId);
            if (result == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to get optimization result {}", resultId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/optimization/schedule/{scheduleId}/results
     * Get all optimization results for a schedule
     */
    @GetMapping("/schedule/{scheduleId}/results")
    public ResponseEntity<List<OptimizationResult>> getResultsForSchedule(@PathVariable Long scheduleId) {
        log.info("GET /api/optimization/schedule/{}/results", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<OptimizationResult> results = optimizationService.getResultsForSchedule(schedule);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Failed to get results for schedule {}", scheduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/optimization/results/recent
     * Get recent optimization results
     */
    @GetMapping("/results/recent")
    public ResponseEntity<List<OptimizationResult>> getRecentResults(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("GET /api/optimization/results/recent?limit={}", limit);

        try {
            List<OptimizationResult> results = optimizationService.getRecentResults(limit);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Failed to get recent optimization results", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
