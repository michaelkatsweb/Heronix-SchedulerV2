package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.domain.ScheduleSlot;
import com.heronix.scheduler.model.dto.ScheduleGenerationRequest;
import com.heronix.scheduler.model.dto.ScheduleGenerationResult;
import com.heronix.scheduler.repository.ScheduleRepository;
import com.heronix.scheduler.repository.ScheduleSlotRepository;
import com.heronix.scheduler.service.EnhancedScheduleGenerationService;
import com.heronix.scheduler.service.ExportService;
import com.heronix.scheduler.service.impl.ScheduleServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Schedule REST API Controller
 *
 * Provides endpoints for schedule CRUD, generation, publishing, cloning,
 * archiving, statistics, analysis, and export.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/schedule")
@CrossOrigin(origins = {"http://localhost:9580", "http://localhost:9585", "http://localhost:9590", "http://localhost:58280", "http://localhost:58180"})
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleServiceImpl scheduleService;
    private final EnhancedScheduleGenerationService enhancedGenerationService;
    private final ExportService exportService;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final ScheduleRepository scheduleRepository;

    // ========== Generation ==========

    /**
     * POST /api/schedule/generate
     * Generate a new schedule using the enhanced AI engine with fallback strategies
     */
    @PostMapping("/generate")
    public ResponseEntity<ScheduleGenerationResult> generateSchedule(
            @RequestBody ScheduleGenerationRequest request) {
        log.info("POST /api/schedule/generate - Generating schedule: {}", request.getScheduleName());

        try {
            ScheduleGenerationResult result = enhancedGenerationService.generateWithFallback(request, (progress, message) ->
                log.info("Generation progress: {}% - {}", progress, message)
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Schedule generation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== CRUD ==========

    /**
     * GET /api/schedule
     * Get all schedules
     */
    @GetMapping
    public ResponseEntity<List<Schedule>> getAllSchedules() {
        log.info("GET /api/schedule");
        List<Schedule> schedules = scheduleService.getAllSchedules();
        return ResponseEntity.ok(schedules);
    }

    /**
     * GET /api/schedule/active
     * Get all active (non-archived) schedules
     */
    @GetMapping("/active")
    public ResponseEntity<List<Schedule>> getActiveSchedules() {
        log.info("GET /api/schedule/active");
        List<Schedule> schedules = scheduleService.getActiveSchedules();
        return ResponseEntity.ok(schedules);
    }

    /**
     * GET /api/schedule/{id}
     * Get a schedule by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Schedule> getScheduleById(@PathVariable Long id) {
        log.info("GET /api/schedule/{}", id);
        return scheduleService.getScheduleById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/schedule/{id}/slots
     * Get all schedule slots for a schedule (with details)
     */
    @GetMapping("/{id}/slots")
    public ResponseEntity<List<ScheduleSlot>> getScheduleSlots(@PathVariable Long id) {
        log.info("GET /api/schedule/{}/slots", id);

        if (!scheduleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleIdWithDetails(id);
        return ResponseEntity.ok(slots);
    }

    /**
     * GET /api/schedule/latest
     * Get the most recently created schedule
     */
    @GetMapping("/latest")
    public ResponseEntity<Schedule> getLatestSchedule() {
        log.info("GET /api/schedule/latest");

        List<Schedule> schedules = scheduleRepository.findAll(Sort.by(Sort.Direction.DESC, "id"));
        if (schedules.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(schedules.get(0));
    }

    /**
     * DELETE /api/schedule/{id}
     * Delete a schedule
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long id) {
        log.info("DELETE /api/schedule/{}", id);

        if (!scheduleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }

    // ========== Workflow Actions ==========

    /**
     * POST /api/schedule/{id}/publish
     * Publish a schedule (changes status to PUBLISHED)
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Map<String, String>> publishSchedule(@PathVariable Long id) {
        log.info("POST /api/schedule/{}/publish", id);

        try {
            scheduleService.publishSchedule(id);
            return ResponseEntity.ok(Map.of("status", "PUBLISHED", "scheduleId", id.toString()));
        } catch (IllegalArgumentException e) {
            log.error("Schedule not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to publish schedule {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/schedule/{id}/clone
     * Clone a schedule (creates a copy with all slots)
     */
    @PostMapping("/{id}/clone")
    public ResponseEntity<Schedule> cloneSchedule(@PathVariable Long id) {
        log.info("POST /api/schedule/{}/clone", id);

        try {
            Schedule cloned = scheduleService.cloneSchedule(id);
            return ResponseEntity.status(HttpStatus.CREATED).body(cloned);
        } catch (IllegalArgumentException e) {
            log.error("Schedule not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to clone schedule {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/schedule/{id}/archive
     * Archive a schedule (changes status to ARCHIVED)
     */
    @PostMapping("/{id}/archive")
    public ResponseEntity<Map<String, String>> archiveSchedule(@PathVariable Long id) {
        log.info("POST /api/schedule/{}/archive", id);

        try {
            scheduleService.archiveSchedule(id);
            return ResponseEntity.ok(Map.of("status", "ARCHIVED", "scheduleId", id.toString()));
        } catch (IllegalArgumentException e) {
            log.error("Schedule not found: {}", id);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Failed to archive schedule {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Statistics & Analysis ==========

    /**
     * GET /api/schedule/{id}/statistics
     * Get schedule statistics including utilization metrics
     */
    @GetMapping("/{id}/statistics")
    public ResponseEntity<Map<String, Object>> getScheduleStatistics(@PathVariable Long id) {
        log.info("GET /api/schedule/{}/statistics", id);

        if (!scheduleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        try {
            String stats = scheduleService.getScheduleStatistics(id);
            double teacherUtilization = scheduleService.getTeacherUtilization(id);
            double roomUtilization = scheduleService.getRoomUtilization(id);

            Map<String, Object> result = Map.of(
                "summary", stats,
                "teacherUtilization", teacherUtilization,
                "roomUtilization", roomUtilization
            );
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to get statistics for schedule {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/schedule/{id}/analyze
     * Analyze an existing schedule for conflicts and quality metrics
     */
    @GetMapping("/{id}/analyze")
    public ResponseEntity<ScheduleGenerationResult> analyzeSchedule(@PathVariable Long id) {
        log.info("GET /api/schedule/{}/analyze", id);

        if (!scheduleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        try {
            ScheduleGenerationResult result = enhancedGenerationService.analyzeExistingSchedule(id);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to analyze schedule {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Export ==========

    /**
     * GET /api/schedule/{id}/export/pdf
     * Export schedule as PDF
     */
    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        log.info("GET /api/schedule/{}/export/pdf", id);

        if (!scheduleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] pdf = exportService.exportScheduleToPDF(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "schedule-" + id + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to export PDF for schedule {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/schedule/{id}/export/csv
     * Export schedule as CSV
     */
    @GetMapping("/{id}/export/csv")
    public ResponseEntity<byte[]> exportCsv(@PathVariable Long id) {
        log.info("GET /api/schedule/{}/export/csv", id);

        if (!scheduleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] csv = exportService.exportScheduleToCSV(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "schedule-" + id + ".csv");
            return new ResponseEntity<>(csv, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to export CSV for schedule {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/schedule/{id}/export/excel
     * Export schedule as Excel
     */
    @GetMapping("/{id}/export/excel")
    public ResponseEntity<byte[]> exportExcel(@PathVariable Long id) {
        log.info("GET /api/schedule/{}/export/excel", id);

        if (!scheduleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] excel = exportService.exportScheduleToExcel(id);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "schedule-" + id + ".xlsx");
            return new ResponseEntity<>(excel, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to export Excel for schedule {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
