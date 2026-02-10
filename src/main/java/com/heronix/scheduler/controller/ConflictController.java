package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.Conflict;
import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.domain.User;
import com.heronix.scheduler.repository.ConflictRepository;
import com.heronix.scheduler.repository.ScheduleRepository;
import com.heronix.scheduler.repository.UserRepository;
import com.heronix.scheduler.service.ConflictDetectionService;
import com.heronix.scheduler.service.ConflictResolverService;
import com.heronix.scheduler.service.ConflictResolverService.ResolutionSuggestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Conflict REST API Controller
 *
 * Provides endpoints for conflict detection, resolution, and management.
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/conflicts")
@CrossOrigin(origins = {"http://localhost:9580", "http://localhost:9585", "http://localhost:9590", "http://localhost:58280", "http://localhost:58180"})
@RequiredArgsConstructor
public class ConflictController {

    private final ConflictDetectionService conflictDetectionService;
    private final ConflictResolverService conflictResolverService;
    private final ConflictRepository conflictRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    // ========== Query Endpoints ==========

    /**
     * GET /api/conflicts/schedule/{scheduleId}
     * Get all conflicts for a schedule
     */
    @GetMapping("/schedule/{scheduleId}")
    public ResponseEntity<List<Conflict>> getConflictsBySchedule(@PathVariable Long scheduleId) {
        log.info("GET /api/conflicts/schedule/{}", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }

        List<Conflict> conflicts = conflictRepository.findByScheduleOrderBySeverity(schedule);
        return ResponseEntity.ok(conflicts);
    }

    /**
     * GET /api/conflicts/{id}
     * Get a conflict by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Conflict> getConflictById(@PathVariable Long id) {
        log.info("GET /api/conflicts/{}", id);

        return conflictRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ========== Detection ==========

    /**
     * POST /api/conflicts/detect/{scheduleId}
     * Run conflict detection on a schedule
     */
    @PostMapping("/detect/{scheduleId}")
    public ResponseEntity<List<com.heronix.scheduler.model.dto.Conflict>> detectConflicts(@PathVariable Long scheduleId) {
        log.info("POST /api/conflicts/detect/{}", scheduleId);

        if (!scheduleRepository.existsById(scheduleId)) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<com.heronix.scheduler.model.dto.Conflict> conflicts = conflictDetectionService.detectConflicts(scheduleId);
            return ResponseEntity.ok(conflicts);
        } catch (Exception e) {
            log.error("Conflict detection failed for schedule {}", scheduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/conflicts/slot/{slotId}
     * Check conflicts for a specific slot
     */
    @GetMapping("/slot/{slotId}")
    public ResponseEntity<List<com.heronix.scheduler.model.dto.Conflict>> checkSlotConflicts(@PathVariable Long slotId) {
        log.info("GET /api/conflicts/slot/{}", slotId);

        try {
            List<com.heronix.scheduler.model.dto.Conflict> conflicts = conflictDetectionService.checkSlotConflicts(slotId);
            return ResponseEntity.ok(conflicts);
        } catch (Exception e) {
            log.error("Slot conflict check failed for slot {}", slotId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Resolution ==========

    /**
     * POST /api/conflicts/{id}/resolve
     * Get suggestions and apply the best resolution for a conflict
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveConflict(
            @PathVariable Long id,
            @RequestBody(required = false) ResolveConflictRequest request) {
        log.info("POST /api/conflicts/{}/resolve", id);

        Conflict conflict = conflictRepository.findById(id).orElse(null);
        if (conflict == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<ResolutionSuggestion> suggestions = conflictResolverService.getSuggestions(conflict);
            if (suggestions.isEmpty()) {
                return ResponseEntity.ok(Map.of("resolved", false, "message", "No resolution suggestions available"));
            }

            User user = null;
            if (request != null && request.getUserId() != null) {
                user = userRepository.findById(request.getUserId()).orElse(null);
            }

            ResolutionSuggestion bestSuggestion = suggestions.get(0);
            boolean resolved = conflictResolverService.applyResolution(conflict, bestSuggestion, user);

            return ResponseEntity.ok(Map.of(
                "resolved", resolved,
                "suggestion", bestSuggestion.toString(),
                "totalSuggestions", suggestions.size()
            ));
        } catch (Exception e) {
            log.error("Failed to resolve conflict {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/conflicts/schedule/{scheduleId}/auto-resolve
     * Auto-resolve all conflicts for a schedule
     */
    @PostMapping("/schedule/{scheduleId}/auto-resolve")
    public ResponseEntity<Map<String, Object>> autoResolveAll(@PathVariable Long scheduleId) {
        log.info("POST /api/conflicts/schedule/{}/auto-resolve", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            // Use a system user for auto-resolution
            User systemUser = userRepository.findByUsername("system").orElse(null);
            int resolvedCount = conflictResolverService.autoResolveAll(schedule, systemUser);

            return ResponseEntity.ok(Map.of(
                "resolvedCount", resolvedCount,
                "scheduleId", scheduleId
            ));
        } catch (Exception e) {
            log.error("Auto-resolve failed for schedule {}", scheduleId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/conflicts/{id}/suggestions
     * Get resolution suggestions for a conflict
     */
    @GetMapping("/{id}/suggestions")
    public ResponseEntity<List<ResolutionSuggestion>> getSuggestions(@PathVariable Long id) {
        log.info("GET /api/conflicts/{}/suggestions", id);

        Conflict conflict = conflictRepository.findById(id).orElse(null);
        if (conflict == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            List<ResolutionSuggestion> suggestions = conflictResolverService.getSuggestions(conflict);
            return ResponseEntity.ok(suggestions);
        } catch (Exception e) {
            log.error("Failed to get suggestions for conflict {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Status Management ==========

    /**
     * POST /api/conflicts/{id}/ignore
     * Mark a conflict as ignored
     */
    @PostMapping("/{id}/ignore")
    public ResponseEntity<Map<String, String>> ignoreConflict(
            @PathVariable Long id,
            @RequestBody(required = false) IgnoreConflictRequest request) {
        log.info("POST /api/conflicts/{}/ignore", id);

        Conflict conflict = conflictRepository.findById(id).orElse(null);
        if (conflict == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String reason = (request != null && request.getReason() != null)
                    ? request.getReason() : "Ignored by user";
            conflictResolverService.markIgnored(conflict, reason);
            return ResponseEntity.ok(Map.of("status", "IGNORED", "conflictId", id.toString()));
        } catch (Exception e) {
            log.error("Failed to ignore conflict {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/conflicts/{id}/mark-resolved
     * Manually mark a conflict as resolved
     */
    @PostMapping("/{id}/mark-resolved")
    public ResponseEntity<Map<String, String>> markResolved(
            @PathVariable Long id,
            @RequestBody(required = false) MarkResolvedRequest request) {
        log.info("POST /api/conflicts/{}/mark-resolved", id);

        Conflict conflict = conflictRepository.findById(id).orElse(null);
        if (conflict == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            User user = null;
            if (request != null && request.getUserId() != null) {
                user = userRepository.findById(request.getUserId()).orElse(null);
            }
            String notes = (request != null && request.getNotes() != null)
                    ? request.getNotes() : "Manually marked as resolved";

            conflictResolverService.markResolved(conflict, user, notes);
            return ResponseEntity.ok(Map.of("status", "RESOLVED", "conflictId", id.toString()));
        } catch (Exception e) {
            log.error("Failed to mark conflict {} as resolved", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ========== Request DTOs ==========

    public static class ResolveConflictRequest {
        private Long userId;
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
    }

    public static class IgnoreConflictRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class MarkResolvedRequest {
        private Long userId;
        private String notes;
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }
}
