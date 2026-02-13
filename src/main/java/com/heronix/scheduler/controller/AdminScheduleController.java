package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.domain.ScheduleSlot;
import com.heronix.scheduler.model.dto.ScheduleGenerationRequest;
import com.heronix.scheduler.model.dto.ScheduleGenerationResult;
import com.heronix.scheduler.repository.ScheduleSlotRepository;
import com.heronix.scheduler.service.EnhancedScheduleGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Admin Schedule Controller — SIS-facing API for delegated schedule generation.
 *
 * SIS calls these endpoints to trigger generation, poll for progress, and
 * export the finished schedule. SchedulerV2 pulls all data it needs from SIS
 * via {@link com.heronix.scheduler.client.SISApiClient}.
 */
@Slf4j
@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = {"http://localhost:9590", "http://localhost:8090"})
@RequiredArgsConstructor
public class AdminScheduleController {

    private final EnhancedScheduleGenerationService enhancedGenerationService;
    private final ScheduleSlotRepository scheduleSlotRepository;

    // In-memory job tracker (keyed by jobId)
    private final ConcurrentHashMap<String, JobState> jobs = new ConcurrentHashMap<>();

    // ========================================================================
    // POST /api/schedule/generate-for-sis
    // ========================================================================

    @PostMapping("/generate-for-sis")
    public ResponseEntity<Map<String, Object>> generateForSis(
            @RequestBody ScheduleGenerationRequest request) {

        String jobId = UUID.randomUUID().toString();
        log.info("POST /api/schedule/generate-for-sis — jobId={}, scheduleName={}",
                jobId, request.getScheduleName());

        JobState state = new JobState(jobId);
        jobs.put(jobId, state);

        // Run generation asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                state.status = "RUNNING";
                state.message = "Schedule generation in progress";

                ScheduleGenerationResult result = enhancedGenerationService.generateWithFallback(
                        request, (progress, message) -> {
                            state.progress = progress;
                            state.message = message;
                        });

                state.result = result;
                state.scheduleId = result.getSchedule() != null ? result.getSchedule().getId() : null;
                state.status = "COMPLETED";
                state.message = result.getSummaryMessage();
                state.progress = 100;
                state.optimizationScore = result.getOptimizationScore();
                state.elapsedSeconds = result.getGenerationTimeSeconds();

                log.info("Job {} completed — scheduleId={}", jobId, state.scheduleId);

            } catch (Exception e) {
                log.error("Job {} failed", jobId, e);
                state.status = "FAILED";
                state.message = e.getMessage();
            }
        });

        return ResponseEntity.accepted().body(Map.of(
                "jobId", jobId,
                "status", "QUEUED"
        ));
    }

    // ========================================================================
    // GET /api/schedule/job/{jobId}/status
    // ========================================================================

    @GetMapping("/job/{jobId}/status")
    public ResponseEntity<Map<String, Object>> getJobStatus(@PathVariable String jobId) {
        JobState state = jobs.get(jobId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("jobId", state.jobId);
        body.put("status", state.status);
        body.put("progress", state.progress);
        body.put("message", state.message);
        body.put("elapsedSeconds", state.elapsedSeconds);
        body.put("optimizationScore", state.optimizationScore);
        body.put("scheduleId", state.scheduleId);

        return ResponseEntity.ok(body);
    }

    // ========================================================================
    // GET /api/schedule/job/{jobId}/export
    // ========================================================================

    @GetMapping("/job/{jobId}/export")
    public ResponseEntity<?> exportJobResult(@PathVariable String jobId) {
        JobState state = jobs.get(jobId);
        if (state == null) {
            return ResponseEntity.notFound().build();
        }

        if (!"COMPLETED".equals(state.status)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Job not completed yet", "status", state.status));
        }

        if (state.scheduleId == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Generation completed but no schedule was produced"));
        }

        // Return the full generation result (includes schedule + conflicts + metrics)
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleIdWithDetails(state.scheduleId);

        Map<String, Object> export = new java.util.LinkedHashMap<>();
        export.put("jobId", jobId);
        export.put("scheduleId", state.scheduleId);
        export.put("status", state.status);
        export.put("optimizationScore", state.optimizationScore);
        export.put("slots", slots);

        if (state.result != null) {
            export.put("completionPercentage", state.result.getCompletionPercentage());
            export.put("totalCourses", state.result.getTotalCourses());
            export.put("scheduledCourses", state.result.getScheduledCourses());
            export.put("conflicts", state.result.getConflicts());
            export.put("summaryMessage", state.result.getSummaryMessage());
        }

        return ResponseEntity.ok(export);
    }

    // ========================================================================
    // Internal job state holder
    // ========================================================================

    private static class JobState {
        final String jobId;
        volatile String status = "QUEUED";
        volatile int progress = 0;
        volatile String message = "Waiting to start";
        volatile Long scheduleId;
        volatile String optimizationScore;
        volatile Long elapsedSeconds;
        volatile ScheduleGenerationResult result;

        JobState(String jobId) {
            this.jobId = jobId;
        }
    }
}
