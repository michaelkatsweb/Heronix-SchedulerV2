// Location: src/main/java/com/eduscheduler/controller/LunchAssignmentController.java
package com.heronix.scheduler.controller;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.LunchAssignmentMethod;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.data.SISDataService;
import com.heronix.scheduler.service.LunchAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Lunch Assignment REST API Controller
 * Location: src/main/java/com/eduscheduler/controller/LunchAssignmentController.java
 *
 * Phase 5D: UI Integration & Testing
 *
 * Endpoints:
 * - POST /api/lunch-assignments/assign-students - Auto-assign all students
 * - POST /api/lunch-assignments/assign-teachers - Auto-assign all teachers
 * - POST /api/lunch-assignments/rebalance - Rebalance assignments
 *
 * - GET /api/lunch-assignments/student/{studentId}?scheduleId={id} - Get student assignment
 * - PUT /api/lunch-assignments/student/{studentId} - Reassign student
 * - DELETE /api/lunch-assignments/student/{studentId}?scheduleId={id} - Remove assignment
 *
 * - GET /api/lunch-assignments/teacher/{teacherId}?scheduleId={id} - Get teacher assignment
 * - PUT /api/lunch-assignments/teacher/{teacherId} - Reassign teacher
 * - PUT /api/lunch-assignments/teacher/{teacherId}/supervision - Assign supervision duty
 * - DELETE /api/lunch-assignments/teacher/{teacherId}/supervision - Remove supervision duty
 *
 * - GET /api/lunch-assignments/wave/{waveId}/roster/students - Get student roster
 * - GET /api/lunch-assignments/wave/{waveId}/roster/teachers - Get teacher roster
 * - GET /api/lunch-assignments/schedule/{scheduleId}/unassigned/students - Get unassigned students
 * - GET /api/lunch-assignments/schedule/{scheduleId}/unassigned/teachers - Get unassigned teachers
 * - GET /api/lunch-assignments/stats?scheduleId={id} - Get assignment statistics
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-01
 */
@Slf4j
@RestController
@RequestMapping("/api/lunch-assignments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LunchAssignmentController {

    private final LunchAssignmentService lunchAssignmentService;
    private final ScheduleRepository scheduleRepository;
    private final SISDataService sisDataService;
    
    private final LunchWaveRepository lunchWaveRepository;

    // ========== Assignment Endpoints ==========

    /**
     * POST /api/lunch-assignments/assign-students
     * Auto-assign all students to lunch waves
     *
     * Request body:
     * {
     *   "scheduleId": 1,
     *   "method": "ALPHABETICAL"
     * }
     */
    @PostMapping("/assign-students")
    public ResponseEntity<Map<String, Object>> assignStudents(@RequestBody AssignStudentsRequest request) {
        log.info("POST /api/lunch-assignments/assign-students - Method: {}", request.getMethod());

        Schedule schedule = scheduleRepository.findById(request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + request.getScheduleId()));

        int assignedCount = lunchAssignmentService.assignStudentsToLunchWaves(schedule.getId(), request.getMethod());

        log.info("Assigned {} students using {} method", assignedCount, request.getMethod());
        return ResponseEntity.ok(Map.of(
            "assignedCount", assignedCount,
            "method", request.getMethod().toString()
        ));
    }

    /**
     * POST /api/lunch-assignments/assign-teachers
     * Auto-assign all teachers to lunch waves
     *
     * Request body:
     * {
     *   "scheduleId": 1
     * }
     */
    @PostMapping("/assign-teachers")
    public ResponseEntity<Map<String, Object>> assignTeachers(@RequestBody Map<String, Long> body) {
        Long scheduleId = body.get("scheduleId");
        log.info("POST /api/lunch-assignments/assign-teachers - Schedule: {}", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        int assignedCount = lunchAssignmentService.assignTeachersToLunchWaves(schedule.getId());

        log.info("Assigned {} teachers", assignedCount);
        return ResponseEntity.ok(Map.of("assignedCount", assignedCount));
    }

    /**
     * POST /api/lunch-assignments/rebalance
     * Rebalance existing lunch wave assignments
     *
     * Request body:
     * {
     *   "scheduleId": 1
     * }
     */
    @PostMapping("/rebalance")
    public ResponseEntity<Map<String, Object>> rebalanceAssignments(@RequestBody Map<String, Long> body) {
        Long scheduleId = body.get("scheduleId");
        log.info("POST /api/lunch-assignments/rebalance - Schedule: {}", scheduleId);

        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        int reassignedCount = lunchAssignmentService.rebalanceLunchWaves(schedule.getId());

        log.info("Rebalanced {} student assignments", reassignedCount);
        return ResponseEntity.ok(Map.of("reassignedCount", reassignedCount));
    }

    // ========== Student Assignment Endpoints ==========

    /**
     * GET /api/lunch-assignments/student/{studentId}?scheduleId={id}
     * Get student's lunch assignment
     */
    @GetMapping("/student/{studentId}")
    public ResponseEntity<StudentLunchAssignment> getStudentAssignment(
            @PathVariable Long studentId,
            @RequestParam Long scheduleId) {
        log.info("GET /api/lunch-assignments/student/{}?scheduleId={}", studentId, scheduleId);

        return lunchAssignmentService.getStudentAssignment(studentId, scheduleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/lunch-assignments/student/{studentId}
     * Reassign student to different lunch wave
     *
     * Request body:
     * {
     *   "lunchWaveId": 2,
     *   "username": "admin"
     * }
     */
    @PutMapping("/student/{studentId}")
    public ResponseEntity<StudentLunchAssignment> reassignStudent(
            @PathVariable Long studentId,
            @RequestBody ReassignStudentRequest request) {
        log.info("PUT /api/lunch-assignments/student/{} - New wave: {}", studentId, request.getLunchWaveId());

        // Get current assignment
        var assignment = lunchAssignmentService.getStudentAssignment(studentId, request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Student assignment not found"));

        // Reassign
        StudentLunchAssignment updated = lunchAssignmentService.reassignStudent(
            assignment.getId(),
            request.getLunchWaveId(),
            request.getUsername() != null ? request.getUsername() : "system"
        );

        log.info("Reassigned student {} to lunch wave {}", studentId, updated.getLunchWave().getWaveName());
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/lunch-assignments/student/{studentId}?scheduleId={id}
     * Remove student's lunch assignment
     */
    @DeleteMapping("/student/{studentId}")
    public ResponseEntity<Void> removeStudentAssignment(
            @PathVariable Long studentId,
            @RequestParam Long scheduleId) {
        log.info("DELETE /api/lunch-assignments/student/{}?scheduleId={}", studentId, scheduleId);

        var assignment = lunchAssignmentService.getStudentAssignment(studentId, scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Student assignment not found"));

        lunchAssignmentService.removeStudentAssignment(assignment.getId());
        log.info("Removed student assignment for student {}", studentId);
        return ResponseEntity.noContent().build();
    }

    // ========== Teacher Assignment Endpoints ==========

    /**
     * GET /api/lunch-assignments/teacher/{teacherId}?scheduleId={id}
     * Get teacher's lunch assignment
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<TeacherLunchAssignment> getTeacherAssignment(
            @PathVariable Long teacherId,
            @RequestParam Long scheduleId) {
        log.info("GET /api/lunch-assignments/teacher/{}?scheduleId={}", teacherId, scheduleId);

        return lunchAssignmentService.getTeacherAssignment(teacherId, scheduleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * PUT /api/lunch-assignments/teacher/{teacherId}
     * Reassign teacher to different lunch wave
     *
     * Request body:
     * {
     *   "scheduleId": 1,
     *   "lunchWaveId": 2,
     *   "username": "admin"
     * }
     */
    @PutMapping("/teacher/{teacherId}")
    public ResponseEntity<TeacherLunchAssignment> reassignTeacher(
            @PathVariable Long teacherId,
            @RequestBody ReassignTeacherRequest request) {
        log.info("PUT /api/lunch-assignments/teacher/{} - New wave: {}", teacherId, request.getLunchWaveId());

        // Get current assignment
        var assignment = lunchAssignmentService.getTeacherAssignment(teacherId, request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher assignment not found"));

        // Reassign
        TeacherLunchAssignment updated = lunchAssignmentService.reassignTeacher(
            assignment.getId(),
            request.getLunchWaveId(),
            request.getUsername() != null ? request.getUsername() : "system"
        );

        log.info("Reassigned teacher {} to lunch wave {}", teacherId, updated.getLunchWave().getWaveName());
        return ResponseEntity.ok(updated);
    }

    /**
     * PUT /api/lunch-assignments/teacher/{teacherId}/supervision
     * Assign cafeteria supervision duty to teacher
     *
     * Request body:
     * {
     *   "scheduleId": 1,
     *   "location": "Main Cafeteria",
     *   "username": "admin"
     * }
     */
    @PutMapping("/teacher/{teacherId}/supervision")
    public ResponseEntity<TeacherLunchAssignment> assignSupervisionDuty(
            @PathVariable Long teacherId,
            @RequestBody SupervisionDutyRequest request) {
        log.info("PUT /api/lunch-assignments/teacher/{}/supervision - Location: {}",
            teacherId, request.getLocation());

        // Get teacher's assignment
        var assignment = lunchAssignmentService.getTeacherAssignment(teacherId, request.getScheduleId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher assignment not found"));

        // Assign duty
        TeacherLunchAssignment updated = lunchAssignmentService.assignSupervisionDuty(
            assignment.getId(),
            request.getLocation(),
            request.getUsername() != null ? request.getUsername() : "system"
        );

        log.info("Assigned supervision duty to teacher {} at {}", teacherId, request.getLocation());
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/lunch-assignments/teacher/{teacherId}/supervision?scheduleId={id}
     * Remove supervision duty from teacher
     */
    @DeleteMapping("/teacher/{teacherId}/supervision")
    public ResponseEntity<TeacherLunchAssignment> removeSupervisionDuty(
            @PathVariable Long teacherId,
            @RequestParam Long scheduleId,
            @RequestParam(required = false) String username) {
        log.info("DELETE /api/lunch-assignments/teacher/{}/supervision", teacherId);

        // Get teacher's assignment
        var assignment = lunchAssignmentService.getTeacherAssignment(teacherId, scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher assignment not found"));

        // Remove duty
        TeacherLunchAssignment updated = lunchAssignmentService.removeSupervisionDuty(
            assignment.getId(),
            username != null ? username : "system"
        );

        log.info("Removed supervision duty from teacher {}", teacherId);
        return ResponseEntity.ok(updated);
    }

    // ========== Roster Endpoints ==========

    /**
     * GET /api/lunch-assignments/wave/{waveId}/roster/students
     * Get student roster for a lunch wave
     */
    @GetMapping("/wave/{waveId}/roster/students")
    public ResponseEntity<List<StudentLunchAssignment>> getStudentRoster(@PathVariable Long waveId) {
        log.info("GET /api/lunch-assignments/wave/{}/roster/students", waveId);

        List<StudentLunchAssignment> roster = lunchAssignmentService.getWaveRoster(waveId);
        return ResponseEntity.ok(roster);
    }

    /**
     * GET /api/lunch-assignments/wave/{waveId}/roster/teachers
     * Get teacher roster for a lunch wave
     */
    @GetMapping("/wave/{waveId}/roster/teachers")
    public ResponseEntity<List<TeacherLunchAssignment>> getTeacherRoster(@PathVariable Long waveId) {
        log.info("GET /api/lunch-assignments/wave/{}/roster/teachers", waveId);

        List<TeacherLunchAssignment> roster = lunchAssignmentService.getTeachersInWave(waveId);
        return ResponseEntity.ok(roster);
    }

    /**
     * GET /api/lunch-assignments/schedule/{scheduleId}/unassigned/students
     * Get students without lunch assignments
     */
    @GetMapping("/schedule/{scheduleId}/unassigned/students")
    public ResponseEntity<List<Student>> getUnassignedStudents(@PathVariable Long scheduleId) {
        log.info("GET /api/lunch-assignments/schedule/{}/unassigned/students", scheduleId);

        List<Student> unassigned = lunchAssignmentService.getUnassignedStudents(scheduleId);
        return ResponseEntity.ok(unassigned);
    }

    /**
     * GET /api/lunch-assignments/schedule/{scheduleId}/unassigned/teachers
     * Get teachers without lunch assignments
     */
    @GetMapping("/schedule/{scheduleId}/unassigned/teachers")
    public ResponseEntity<List<Teacher>> getUnassignedTeachers(@PathVariable Long scheduleId) {
        log.info("GET /api/lunch-assignments/schedule/{}/unassigned/teachers", scheduleId);

        List<Teacher> unassigned = lunchAssignmentService.getUnassignedTeachers(scheduleId);
        return ResponseEntity.ok(unassigned);
    }

    // ========== Statistics Endpoints ==========

    /**
     * GET /api/lunch-assignments/stats?scheduleId={id}
     * Get assignment statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<LunchAssignmentService.LunchAssignmentStatistics> getStatistics(
            @RequestParam Long scheduleId) {
        log.info("GET /api/lunch-assignments/stats?scheduleId={}", scheduleId);

        var stats = lunchAssignmentService.getAssignmentStatistics(scheduleId);
        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/lunch-assignments/validate?scheduleId={id}
     * Validate assignments are ready for schedule generation
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateAssignments(@RequestParam Long scheduleId) {
        log.info("GET /api/lunch-assignments/validate?scheduleId={}", scheduleId);

        boolean allStudentsAssigned = lunchAssignmentService.areAllStudentsAssigned(scheduleId);
        boolean capacitiesRespected = lunchAssignmentService.areCapacitiesRespected(scheduleId);
        boolean gradeLevelsRespected = lunchAssignmentService.areGradeLevelsRespected(scheduleId);
        boolean assignmentsValid = lunchAssignmentService.areAssignmentsValid(scheduleId);

        Map<String, Object> validation = Map.of(
            "allStudentsAssigned", allStudentsAssigned,
            "capacitiesRespected", capacitiesRespected,
            "gradeLevelsRespected", gradeLevelsRespected,
            "isValid", assignmentsValid
        );

        return ResponseEntity.ok(validation);
    }

    // ========== Request/Response DTOs ==========

    /**
     * Request body for assigning students
     */
    public static class AssignStudentsRequest {
        private Long scheduleId;
        private LunchAssignmentMethod method;

        // Getters and setters
        public Long getScheduleId() { return scheduleId; }
        public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

        public LunchAssignmentMethod getMethod() { return method; }
        public void setMethod(LunchAssignmentMethod method) { this.method = method; }
    }

    /**
     * Request body for reassigning a student
     */
    public static class ReassignStudentRequest {
        private Long scheduleId;
        private Long lunchWaveId;
        private String username;

        // Getters and setters
        public Long getScheduleId() { return scheduleId; }
        public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

        public Long getLunchWaveId() { return lunchWaveId; }
        public void setLunchWaveId(Long lunchWaveId) { this.lunchWaveId = lunchWaveId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    /**
     * Request body for reassigning a teacher
     */
    public static class ReassignTeacherRequest {
        private Long scheduleId;
        private Long lunchWaveId;
        private String username;

        // Getters and setters
        public Long getScheduleId() { return scheduleId; }
        public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

        public Long getLunchWaveId() { return lunchWaveId; }
        public void setLunchWaveId(Long lunchWaveId) { this.lunchWaveId = lunchWaveId; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }

    /**
     * Request body for assigning supervision duty
     */
    public static class SupervisionDutyRequest {
        private Long scheduleId;
        private String location;
        private String username;

        // Getters and setters
        public Long getScheduleId() { return scheduleId; }
        public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}
