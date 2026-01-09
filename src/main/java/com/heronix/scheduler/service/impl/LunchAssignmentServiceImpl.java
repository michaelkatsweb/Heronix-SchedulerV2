package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.LunchAssignmentMethod;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.LunchAssignmentService;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of LunchAssignmentService
 *
 * Provides student and teacher lunch assignment functionality
 * with multiple assignment strategies
 *
 * Phase 5B: Multiple Rotating Lunch Periods - Service Layer
 * Date: December 1, 2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LunchAssignmentServiceImpl implements LunchAssignmentService {

    private final StudentLunchAssignmentRepository studentLunchAssignmentRepository;
    private final TeacherLunchAssignmentRepository teacherLunchAssignmentRepository;
    private final LunchWaveRepository lunchWaveRepository;
    private final SISDataService sisDataService;
    private final ScheduleRepository scheduleRepository;

    // ========== Student Assignment Methods ==========

    @Override
    public int assignStudentsToLunchWaves(Long scheduleId, LunchAssignmentMethod method) {
        // Load entity at start
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        log.info("Assigning students to lunch waves using method: {}", method);

        switch (method) {
            case BY_GRADE_LEVEL:
                return assignStudentsByGradeLevel(scheduleId);
            case ALPHABETICAL:
                return assignStudentsAlphabetically(scheduleId);
            case RANDOM:
                return assignStudentsRandomly(scheduleId);
            case BALANCED:
                return assignStudentsBalanced(scheduleId);
            case BY_STUDENT_ID:
                return assignStudentsByStudentId(scheduleId);
            case MANUAL:
                log.info("MANUAL assignment method - skipping auto-assignment");
                return 0;
            default:
                log.warn("Unsupported assignment method: {}, falling back to BALANCED", method);
                return assignStudentsBalanced(scheduleId);
        }
    }

    @Override
    public int assignStudentsToLunchWaves(Long scheduleId, Long campusId, LunchAssignmentMethod method) {
        // Load entity at start
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        log.info("Assigning students from campus {} using method {} for schedule {}", campusId, method, schedule.getId());

        List<LunchWave> waves = lunchWaveRepository.findActiveByScheduleId(schedule.getId());
        if (waves.isEmpty()) {
            log.error("No active lunch waves found for schedule {}", scheduleId);
            throw new IllegalStateException("No active lunch waves found for schedule " + scheduleId);
        }

        // Get unassigned students filtered by campus
        List<Student> students = studentLunchAssignmentRepository.findUnassignedStudentsByCampus(scheduleId, campusId);
        if (students.isEmpty()) {
            log.info("No unassigned students in campus {}", campusId);
            return 0;
        }

        log.debug("Found {} unassigned students in campus {}", students.size(), campusId);

        // Assign students based on method - use the filtered list
        int assignedCount = 0;
        switch (method) {
            case BY_GRADE_LEVEL:
                assignedCount = assignStudentsByGradeLevelFromList(students, waves, schedule, method);
                break;
            case ALPHABETICAL:
                assignedCount = assignStudentsAlphabeticallyFromList(students, waves, schedule, method);
                break;
            case BALANCED:
                assignedCount = assignStudentsBalancedFromList(students, waves, schedule, method);
                break;
            case RANDOM:
                assignedCount = assignStudentsRandomlyFromList(students, waves, schedule, method);
                break;
            case BY_STUDENT_ID:
                assignedCount = assignStudentsByStudentIdFromList(students, waves, schedule, method);
                break;
            default:
                log.warn("Unknown assignment method: {}", method);
        }

        log.info("Assigned {} students from campus {} to lunch waves", assignedCount, campusId);
        return assignedCount;
    }

    @Override
    public StudentLunchAssignment assignStudentToWave(Long studentId, Long lunchWaveId, String username) {
        // Load entities at start
        Student student = sisDataService.getStudentById(studentId)
            .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        LunchWave lunchWave = lunchWaveRepository.findById(lunchWaveId)
            .orElseThrow(() -> new IllegalArgumentException("LunchWave not found: " + lunchWaveId));

        log.debug("Assigning student {} to lunch wave {}", student.getStudentId(), lunchWave.getWaveName());

        // ✅ NULL SAFE: Check if lunchWave has schedule before querying
        if (lunchWave.getSchedule() == null) {
            throw new IllegalStateException("LunchWave must have an associated schedule");
        }

        // Check if student already has assignment
        Optional<StudentLunchAssignment> existing = studentLunchAssignmentRepository
            .findByStudentAndSchedule(student, lunchWave.getSchedule());

        // If student already has an assignment, reassign them
        if (existing.isPresent()) {
            log.warn("Student {} already assigned to lunch wave, reassigning", student.getStudentId());
            Long existingId = existing.get().getId();
            return reassignStudent(existingId, lunchWaveId, username);
        }

        // Check grade level eligibility
        Integer studentGrade = parseGradeLevel(student.getGradeLevel());
        if (!lunchWave.isGradeLevelEligible(studentGrade)) {
            throw new IllegalArgumentException(String.format(
                "Student grade level %s not eligible for wave %s (restriction: %d)",
                student.getGradeLevel(), lunchWave.getWaveName(), lunchWave.getGradeLevelRestriction()
            ));
        }

        // Check capacity
        if (!lunchWave.canAcceptStudent()) {
            throw new IllegalStateException(String.format(
                "Lunch wave %s is at capacity (%d/%d)",
                lunchWave.getWaveName(), lunchWave.getCurrentAssignments(), lunchWave.getMaxCapacity()
            ));
        }

        // Create assignment
        StudentLunchAssignment assignment = StudentLunchAssignment.builder()
            .student(student)
            .schedule(lunchWave.getSchedule())
            .lunchWave(lunchWave)
            .assignmentMethod(LunchAssignmentMethod.MANUAL.name())
            .assignedAt(LocalDateTime.now())
            .assignedBy(username)
            .manualOverride(true)
            .priority(5)
            .isLocked(false)
            .build();

        assignment = studentLunchAssignmentRepository.save(assignment);

        // Update wave capacity counter in memory
        // Note: currentAssignments is not persisted here - it will be recalculated when needed
        lunchWave.addAssignment();

        log.info("Assigned student {} to {}", student.getStudentId(), lunchWave.getWaveName());
        return assignment;
    }

    @Override
    public StudentLunchAssignment reassignStudent(Long assignmentId, Long newWaveId, String username) {
        // Load entities at start
        StudentLunchAssignment assignment = studentLunchAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("StudentLunchAssignment not found: " + assignmentId));
        LunchWave newWave = lunchWaveRepository.findById(newWaveId)
            .orElseThrow(() -> new IllegalArgumentException("LunchWave not found: " + newWaveId));

        // ✅ NULL SAFE: Check for null student and lunchWave references
        if (assignment.getStudent() == null || assignment.getLunchWave() == null) {
            throw new IllegalStateException("Assignment must have student and lunchWave");
        }

        log.info("Reassigning student {} from {} to {}",
            assignment.getStudent().getStudentId(),
            assignment.getLunchWave().getWaveName(),
            newWave.getWaveName());

        // Update wave capacity counters in memory
        // Note: currentAssignments is not persisted here - it will be recalculated when needed
        LunchWave oldWave = assignment.getLunchWave();
        oldWave.removeAssignment();

        newWave.addAssignment();

        // Update assignment
        assignment.setLunchWave(newWave);
        assignment.setLastModifiedAt(LocalDateTime.now());
        assignment.setLastModifiedBy(username);
        assignment.setManualOverride(true);

        return studentLunchAssignmentRepository.save(assignment);
    }

    @Override
    public void removeStudentAssignment(Long assignmentId) {
        // Load entity at start
        StudentLunchAssignment assignment = studentLunchAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("StudentLunchAssignment not found: " + assignmentId));

        // ✅ NULL SAFE: Check for null student and lunchWave references
        if (assignment.getStudent() == null || assignment.getLunchWave() == null) {
            throw new IllegalStateException("Assignment must have student and lunchWave");
        }

        log.info("Removing lunch assignment for student {}", assignment.getStudent().getStudentId());

        // Update wave capacity counter in memory
        // Note: currentAssignments is not persisted here - it will be recalculated when needed
        LunchWave wave = assignment.getLunchWave();
        wave.removeAssignment();

        studentLunchAssignmentRepository.delete(assignment);
    }

    @Override
    public int removeAllStudentAssignments(Long scheduleId) {
        log.info("Removing all student lunch assignments for schedule {}", scheduleId);

        List<StudentLunchAssignment> assignments = studentLunchAssignmentRepository.findByScheduleId(scheduleId);
        int count = assignments.size();

        // Reset wave capacity counters in memory
        // Note: currentAssignments is not persisted here - it will be recalculated when needed
        List<LunchWave> waves = lunchWaveRepository.findByScheduleIdOrderByWaveOrderAsc(scheduleId);
        for (LunchWave wave : waves) {
            wave.setCurrentAssignments(0);
        }

        studentLunchAssignmentRepository.deleteAll(assignments);

        log.info("Removed {} student lunch assignments", count);
        return count;
    }

    // ========== Assignment Algorithm Methods ==========

    @Override
    public int assignStudentsByGradeLevel(Long scheduleId) {
        // Load entity at start
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        log.info("Assigning students by grade level for schedule {}", schedule.getId());

        List<LunchWave> waves = lunchWaveRepository.findActiveByScheduleId(schedule.getId());
        if (waves.isEmpty()) {
            log.warn("No active lunch waves found");
            return 0;
        }

        List<Student> students = getUnassignedStudents(schedule.getId());
        if (students.isEmpty()) {
            log.info("No unassigned students");
            return 0;
        }

        // Group students by grade level
        Map<Integer, List<Student>> byGrade = students.stream()
            .filter(s -> s.getGradeLevel() != null)
            .collect(Collectors.groupingBy(s -> parseGradeLevel(s.getGradeLevel())))
            .entrySet().stream()
            .filter(e -> e.getKey() != null)  // Remove null grades
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.debug("Grouped {} students into {} grade levels", students.size(), byGrade.size());

        int assignedCount = 0;

        // Assign each grade level to waves
        List<Integer> gradeLevels = new ArrayList<>(byGrade.keySet());
        Collections.sort(gradeLevels);

        for (int i = 0; i < gradeLevels.size(); i++) {
            Integer gradeLevel = gradeLevels.get(i);
            List<Student> gradeStudents = byGrade.get(gradeLevel);

            // Find wave for this grade (use wave order or grade restriction)
            LunchWave targetWave = null;

            // First, try to find wave with matching grade restriction
            for (LunchWave wave : waves) {
                if (wave.getGradeLevelRestriction() != null &&
                    wave.getGradeLevelRestriction().equals(gradeLevel) &&
                    wave.canAcceptStudent()) {
                    targetWave = wave;
                    break;
                }
            }

            // If no grade-specific wave, use round-robin
            if (targetWave == null) {
                targetWave = waves.get(i % waves.size());
            }

            // Assign all students in this grade to the target wave
            for (Student student : gradeStudents) {
                if (!targetWave.canAcceptStudent()) {
                    // Wave full, try next wave
                    targetWave = findNextAvailableWave(waves, student.getGradeLevel());
                    if (targetWave == null) {
                        log.warn("No available lunch wave for grade {}", gradeLevel);
                        break;
                    }
                }

                StudentLunchAssignment assignment = StudentLunchAssignment.builder()
                    .student(student)
                    .schedule(schedule)
                    .lunchWave(targetWave)
                    .assignmentMethod(LunchAssignmentMethod.BY_GRADE_LEVEL.name())
                    .assignedAt(LocalDateTime.now())
                    .assignedBy("SYSTEM")
                    .priority(5)
                    .build();

                studentLunchAssignmentRepository.save(assignment);
                targetWave.addAssignment();
                assignedCount++;
            }
            // Note: targetWave.currentAssignments is not persisted here - it will be recalculated when needed
        }

        log.info("Assigned {} students by grade level", assignedCount);
        return assignedCount;
    }

    @Override
    public int assignStudentsAlphabetically(Long scheduleId) {
        // Load entity at start
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        log.info("Assigning students alphabetically for schedule {}", schedule.getId());

        List<LunchWave> waves = lunchWaveRepository.findActiveByScheduleId(schedule.getId());
        if (waves.isEmpty()) {
            log.warn("No active lunch waves found");
            return 0;
        }

        List<Student> students = getUnassignedStudents(schedule.getId());
        if (students.isEmpty()) {
            log.info("No unassigned students");
            return 0;
        }

        // Sort students alphabetically by last name
        students.sort(Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName));

        log.debug("Sorted {} students alphabetically", students.size());

        // Distribute evenly across waves
        int studentsPerWave = (int) Math.ceil((double) students.size() / waves.size());
        int assignedCount = 0;

        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            int waveIndex = Math.min(i / studentsPerWave, waves.size() - 1);
            LunchWave wave = waves.get(waveIndex);

            // Check capacity
            if (!wave.canAcceptStudent()) {
                // Try to find another wave with capacity
                wave = findNextAvailableWave(waves, student.getGradeLevel());
                if (wave == null) {
                    log.warn("No available lunch wave for student {}", student.getStudentId());
                    continue;
                }
            }

            StudentLunchAssignment assignment = StudentLunchAssignment.builder()
                .student(student)
                .schedule(schedule)
                .lunchWave(wave)
                .assignmentMethod(LunchAssignmentMethod.ALPHABETICAL.name())
                .assignedAt(LocalDateTime.now())
                .assignedBy("SYSTEM")
                .priority(5)
                .build();

            studentLunchAssignmentRepository.save(assignment);
            wave.addAssignment();
            assignedCount++;
        }

        // Note: wave.currentAssignments is not persisted here - it will be recalculated when needed

        log.info("Assigned {} students alphabetically", assignedCount);
        return assignedCount;
    }

    @Override
    public int assignStudentsRandomly(Long scheduleId) {
        // Load entity at start
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        log.info("Assigning students randomly for schedule {}", schedule.getId());

        List<LunchWave> waves = lunchWaveRepository.findActiveByScheduleId(schedule.getId());
        if (waves.isEmpty()) {
            log.warn("No active lunch waves found");
            return 0;
        }

        List<Student> students = getUnassignedStudents(schedule.getId());
        if (students.isEmpty()) {
            log.info("No unassigned students");
            return 0;
        }

        // Shuffle students randomly
        Collections.shuffle(students);
        log.debug("Shuffled {} students for random assignment", students.size());

        int assignedCount = 0;
        int currentWaveIndex = 0;

        for (Student student : students) {
            // Try current wave
            LunchWave wave = waves.get(currentWaveIndex);

            // If wave full or grade restricted, find available wave
            Integer studentGrade = parseGradeLevel(student.getGradeLevel());
            if (!wave.canAcceptStudent() || !wave.isGradeLevelEligible(studentGrade)) {
                wave = findNextAvailableWave(waves, student.getGradeLevel());
                if (wave == null) {
                    log.warn("No available lunch wave for student {}", student.getStudentId());
                    continue;
                }
            }

            StudentLunchAssignment assignment = StudentLunchAssignment.builder()
                .student(student)
                .schedule(schedule)
                .lunchWave(wave)
                .assignmentMethod(LunchAssignmentMethod.RANDOM.name())
                .assignedAt(LocalDateTime.now())
                .assignedBy("SYSTEM")
                .priority(5)
                .build();

            studentLunchAssignmentRepository.save(assignment);
            wave.addAssignment();
            assignedCount++;

            // Move to next wave (round-robin)
            currentWaveIndex = (currentWaveIndex + 1) % waves.size();
        }

        // Note: wave.currentAssignments is not persisted here - it will be recalculated when needed

        log.info("Assigned {} students randomly", assignedCount);
        return assignedCount;
    }

    @Override
    public int assignStudentsBalanced(Long scheduleId) {
        // Load entity at start
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        log.info("Assigning students with balanced capacity for schedule {}", schedule.getId());

        List<LunchWave> waves = lunchWaveRepository.findActiveByScheduleId(schedule.getId());
        if (waves.isEmpty()) {
            log.warn("No active lunch waves found");
            return 0;
        }

        List<Student> students = getUnassignedStudents(schedule.getId());
        if (students.isEmpty()) {
            log.info("No unassigned students");
            return 0;
        }

        log.debug("Balancing {} students across {} waves", students.size(), waves.size());

        int assignedCount = 0;

        for (Student student : students) {
            // ✅ NULL SAFE: Skip students with null gradeLevel
            if (student.getGradeLevel() == null) {
                log.warn("Student {} has null grade level, skipping", student.getStudentId());
                continue;
            }

            // Find wave with most available capacity for this student's grade
            LunchWave bestWave = findWaveWithMostCapacity(waves, student.getGradeLevel());

            if (bestWave == null || !bestWave.canAcceptStudent()) {
                log.warn("No available lunch wave for student {}", student.getStudentId());
                continue;
            }

            StudentLunchAssignment assignment = StudentLunchAssignment.builder()
                .student(student)
                .schedule(schedule)
                .lunchWave(bestWave)
                .assignmentMethod(LunchAssignmentMethod.BALANCED.name())
                .assignedAt(LocalDateTime.now())
                .assignedBy("SYSTEM")
                .priority(5)
                .build();

            studentLunchAssignmentRepository.save(assignment);
            bestWave.addAssignment();
            assignedCount++;
        }

        // Note: wave.currentAssignments is not persisted here - it will be recalculated when needed

        log.info("Assigned {} students with balanced capacity", assignedCount);
        return assignedCount;
    }

    @Override
    public int assignStudentsByStudentId(Long scheduleId) {
        // Load entity at start
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        log.info("Assigning students by student ID for schedule {}", schedule.getId());

        List<LunchWave> waves = lunchWaveRepository.findActiveByScheduleId(schedule.getId());
        if (waves.isEmpty()) {
            log.warn("No active lunch waves found");
            return 0;
        }

        List<Student> students = getUnassignedStudents(schedule.getId());
        if (students.isEmpty()) {
            log.info("No unassigned students");
            return 0;
        }

        // Sort by student ID
        students.sort(Comparator.comparing(Student::getStudentId));

        // Calculate ID ranges
        int totalStudents = students.size();
        int studentsPerWave = (int) Math.ceil((double) totalStudents / waves.size());

        int assignedCount = 0;

        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            int waveIndex = Math.min(i / studentsPerWave, waves.size() - 1);
            LunchWave wave = waves.get(waveIndex);

            if (!wave.canAcceptStudent()) {
                wave = findNextAvailableWave(waves, student.getGradeLevel());
                if (wave == null) {
                    log.warn("No available lunch wave for student {}", student.getStudentId());
                    continue;
                }
            }

            StudentLunchAssignment assignment = StudentLunchAssignment.builder()
                .student(student)
                .schedule(schedule)
                .lunchWave(wave)
                .assignmentMethod(LunchAssignmentMethod.BY_STUDENT_ID.name())
                .assignedAt(LocalDateTime.now())
                .assignedBy("SYSTEM")
                .priority(5)
                .build();

            studentLunchAssignmentRepository.save(assignment);
            wave.addAssignment();
            assignedCount++;
        }

        // Note: wave.currentAssignments is not persisted here - it will be recalculated when needed

        log.info("Assigned {} students by student ID", assignedCount);
        return assignedCount;
    }

    @Override
    public int rebalanceLunchWaves(Long scheduleId) {
        // Load entity at start
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        log.info("Rebalancing lunch waves for schedule {}", schedule.getId());

        // Get reassignable assignments (not locked)
        List<StudentLunchAssignment> reassignable = studentLunchAssignmentRepository
            .findReassignableAssignments(schedule.getId());

        if (reassignable.isEmpty()) {
            log.info("No reassignable students");
            return 0;
        }

        // Remove all reassignable assignments temporarily
        for (StudentLunchAssignment assignment : reassignable) {
            removeStudentAssignment(assignment.getId());
        }

        // Reassign using BALANCED method
        List<LunchWave> waves = lunchWaveRepository.findActiveByScheduleId(schedule.getId());
        int reassignedCount = 0;

        for (StudentLunchAssignment old : reassignable) {
            // ✅ NULL SAFE: Skip assignments with null student or gradeLevel
            if (old.getStudent() == null || old.getStudent().getGradeLevel() == null) {
                continue;
            }

            LunchWave bestWave = findWaveWithMostCapacity(waves, old.getStudent().getGradeLevel());

            if (bestWave != null && bestWave.canAcceptStudent()) {
                StudentLunchAssignment assignment = StudentLunchAssignment.builder()
                    .student(old.getStudent())
                    .schedule(schedule)
                    .lunchWave(bestWave)
                    .assignmentMethod(LunchAssignmentMethod.BALANCED.name())
                    .assignedAt(LocalDateTime.now())
                    .assignedBy("SYSTEM")
                    .priority(old.getPriority())
                    .build();

                studentLunchAssignmentRepository.save(assignment);
                bestWave.addAssignment();
                reassignedCount++;
            }
        }

        // Note: wave.currentAssignments is not persisted here - it will be recalculated when needed

        log.info("Rebalanced {} student assignments", reassignedCount);
        return reassignedCount;
    }

    // ========== Helper Methods ==========

    /**
     * Helper method to assign students by grade level from a given list
     * Used for campus-specific assignments
     */
    private int assignStudentsByGradeLevelFromList(List<Student> students, List<LunchWave> waves,
                                                     Schedule schedule, LunchAssignmentMethod method) {
        // Group students by grade level
        Map<Integer, List<Student>> byGrade = students.stream()
            .filter(s -> s.getGradeLevel() != null)
            .collect(Collectors.groupingBy(s -> parseGradeLevel(s.getGradeLevel())))
            .entrySet().stream()
            .filter(e -> e.getKey() != null)  // Remove null grades
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        log.debug("Grouped {} students into {} grade levels", students.size(), byGrade.size());

        int assignedCount = 0;

        // Assign each grade level to waves
        List<Integer> gradeLevels = new ArrayList<>(byGrade.keySet());
        Collections.sort(gradeLevels);

        for (int i = 0; i < gradeLevels.size(); i++) {
            Integer gradeLevel = gradeLevels.get(i);
            List<Student> gradeStudents = byGrade.get(gradeLevel);

            // Find wave for this grade (use wave order or grade restriction)
            LunchWave targetWave = null;

            // First, try to find wave with matching grade restriction
            for (LunchWave wave : waves) {
                if (wave.getGradeLevelRestriction() != null &&
                    wave.getGradeLevelRestriction().equals(gradeLevel) &&
                    wave.canAcceptStudent()) {
                    targetWave = wave;
                    break;
                }
            }

            // If no grade-specific wave, use round-robin
            if (targetWave == null) {
                targetWave = waves.get(i % waves.size());
            }

            // Assign all students in this grade to the target wave
            for (Student student : gradeStudents) {
                if (!targetWave.canAcceptStudent()) {
                    // Wave full, try next wave
                    targetWave = findNextAvailableWave(waves, student.getGradeLevel());
                    if (targetWave == null) {
                        log.warn("No available lunch wave for grade {}", gradeLevel);
                        break;
                    }
                }

                StudentLunchAssignment assignment = StudentLunchAssignment.builder()
                    .student(student)
                    .schedule(schedule)
                    .lunchWave(targetWave)
                    .assignmentMethod(method.name())
                    .assignedAt(LocalDateTime.now())
                    .assignedBy("SYSTEM")
                    .priority(5)
                    .build();

                studentLunchAssignmentRepository.save(assignment);
                targetWave.addAssignment();
                assignedCount++;
            }
        }

        return assignedCount;
    }

    /**
     * Helper method to assign students alphabetically from a given list
     * Used for campus-specific assignments
     */
    private int assignStudentsAlphabeticallyFromList(List<Student> students, List<LunchWave> waves,
                                                       Schedule schedule, LunchAssignmentMethod method) {
        // Sort students alphabetically by last name
        students.sort(Comparator.comparing(Student::getLastName)
            .thenComparing(Student::getFirstName));

        log.debug("Sorted {} students alphabetically", students.size());

        // Distribute evenly across waves
        int studentsPerWave = (int) Math.ceil((double) students.size() / waves.size());
        int assignedCount = 0;

        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            int waveIndex = Math.min(i / studentsPerWave, waves.size() - 1);
            LunchWave wave = waves.get(waveIndex);

            // Check capacity
            if (!wave.canAcceptStudent()) {
                // Try to find another wave with capacity
                wave = findNextAvailableWave(waves, student.getGradeLevel());
                if (wave == null) {
                    log.warn("No available lunch wave for student {}", student.getStudentId());
                    continue;
                }
            }

            StudentLunchAssignment assignment = StudentLunchAssignment.builder()
                .student(student)
                .schedule(schedule)
                .lunchWave(wave)
                .assignmentMethod(method.name())
                .assignedAt(LocalDateTime.now())
                .assignedBy("SYSTEM")
                .priority(5)
                .build();

            studentLunchAssignmentRepository.save(assignment);
            wave.addAssignment();
            assignedCount++;
        }

        return assignedCount;
    }

    /**
     * Helper method to assign students randomly from a given list
     * Used for campus-specific assignments
     */
    private int assignStudentsRandomlyFromList(List<Student> students, List<LunchWave> waves,
                                                 Schedule schedule, LunchAssignmentMethod method) {
        // Shuffle students randomly
        Collections.shuffle(students);
        log.debug("Shuffled {} students for random assignment", students.size());

        int assignedCount = 0;
        int currentWaveIndex = 0;

        for (Student student : students) {
            // Try current wave
            LunchWave wave = waves.get(currentWaveIndex);

            // If wave full or grade restricted, find available wave
            Integer studentGrade = parseGradeLevel(student.getGradeLevel());
            if (!wave.canAcceptStudent() || !wave.isGradeLevelEligible(studentGrade)) {
                wave = findNextAvailableWave(waves, student.getGradeLevel());
                if (wave == null) {
                    log.warn("No available lunch wave for student {}", student.getStudentId());
                    continue;
                }
            }

            StudentLunchAssignment assignment = StudentLunchAssignment.builder()
                .student(student)
                .schedule(schedule)
                .lunchWave(wave)
                .assignmentMethod(method.name())
                .assignedAt(LocalDateTime.now())
                .assignedBy("SYSTEM")
                .priority(5)
                .build();

            studentLunchAssignmentRepository.save(assignment);
            wave.addAssignment();
            assignedCount++;

            // Move to next wave (round-robin)
            currentWaveIndex = (currentWaveIndex + 1) % waves.size();
        }

        return assignedCount;
    }

    /**
     * Helper method to assign students balanced from a given list
     * Used for campus-specific assignments
     */
    private int assignStudentsBalancedFromList(List<Student> students, List<LunchWave> waves,
                                                 Schedule schedule, LunchAssignmentMethod method) {
        log.debug("Balancing {} students across {} waves", students.size(), waves.size());

        int assignedCount = 0;

        for (Student student : students) {
            // ✅ NULL SAFE: Skip students with null gradeLevel
            if (student.getGradeLevel() == null) {
                log.warn("Student {} has null grade level, skipping", student.getStudentId());
                continue;
            }

            // Find wave with most available capacity for this student's grade
            LunchWave bestWave = findWaveWithMostCapacity(waves, student.getGradeLevel());

            if (bestWave == null || !bestWave.canAcceptStudent()) {
                log.warn("No available lunch wave for student {}", student.getStudentId());
                continue;
            }

            StudentLunchAssignment assignment = StudentLunchAssignment.builder()
                .student(student)
                .schedule(schedule)
                .lunchWave(bestWave)
                .assignmentMethod(method.name())
                .assignedAt(LocalDateTime.now())
                .assignedBy("SYSTEM")
                .priority(5)
                .build();

            studentLunchAssignmentRepository.save(assignment);
            bestWave.addAssignment();
            assignedCount++;
        }

        return assignedCount;
    }

    /**
     * Helper method to assign students by student ID from a given list
     * Used for campus-specific assignments
     */
    private int assignStudentsByStudentIdFromList(List<Student> students, List<LunchWave> waves,
                                                    Schedule schedule, LunchAssignmentMethod method) {
        // Sort by student ID
        students.sort(Comparator.comparing(Student::getStudentId));

        // Calculate ID ranges
        int totalStudents = students.size();
        int studentsPerWave = (int) Math.ceil((double) totalStudents / waves.size());

        int assignedCount = 0;

        for (int i = 0; i < students.size(); i++) {
            Student student = students.get(i);
            int waveIndex = Math.min(i / studentsPerWave, waves.size() - 1);
            LunchWave wave = waves.get(waveIndex);

            if (!wave.canAcceptStudent()) {
                wave = findNextAvailableWave(waves, student.getGradeLevel());
                if (wave == null) {
                    log.warn("No available lunch wave for student {}", student.getStudentId());
                    continue;
                }
            }

            StudentLunchAssignment assignment = StudentLunchAssignment.builder()
                .student(student)
                .schedule(schedule)
                .lunchWave(wave)
                .assignmentMethod(method.name())
                .assignedAt(LocalDateTime.now())
                .assignedBy("SYSTEM")
                .priority(5)
                .build();

            studentLunchAssignmentRepository.save(assignment);
            wave.addAssignment();
            assignedCount++;
        }

        return assignedCount;
    }

    /**
     * Parse grade level from String to Integer
     * Handles: "9", "10", "11", "12", "9th", "10th", etc.
     */
    private Integer parseGradeLevel(String gradeLevel) {
        if (gradeLevel == null) {
            return null;
        }
        try {
            // Remove non-numeric characters
            String numeric = gradeLevel.replaceAll("[^0-9]", "");
            return numeric.isEmpty() ? null : Integer.parseInt(numeric);
        } catch (Exception e) {
            log.warn("Could not parse grade level: {}", gradeLevel);
            return null;
        }
    }

    private LunchWave findNextAvailableWave(List<LunchWave> waves, String gradeLevel) {
        Integer grade = parseGradeLevel(gradeLevel);
        for (LunchWave wave : waves) {
            if (wave.canAcceptStudent() && wave.isGradeLevelEligible(grade)) {
                return wave;
            }
        }
        return null;
    }

    private LunchWave findWaveWithMostCapacity(List<LunchWave> waves, String gradeLevel) {
        Integer grade = parseGradeLevel(gradeLevel);
        return waves.stream()
            .filter(w -> w.canAcceptStudent() && w.isGradeLevelEligible(grade))
            .max(Comparator.comparingInt(LunchWave::getAvailableSeats))
            .orElse(null);
    }

    // ========== Student Query Methods ==========

    @Override
    @Transactional(readOnly = true)
    public List<Student> getUnassignedStudents(Long scheduleId) {
        return studentLunchAssignmentRepository.findUnassignedStudents(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnassignedStudents(Long scheduleId) {
        return studentLunchAssignmentRepository.countUnassignedStudents(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StudentLunchAssignment> getStudentAssignment(Long studentId, Long scheduleId) {
        return studentLunchAssignmentRepository.findByStudentIdAndScheduleId(studentId, scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentLunchAssignment> getWaveRoster(Long lunchWaveId) {
        return studentLunchAssignmentRepository.findByLunchWaveIdOrderByStudentName(lunchWaveId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentLunchAssignment> getAllStudentAssignments(Long scheduleId) {
        return studentLunchAssignmentRepository.findByScheduleId(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentLunchAssignment> getManualAssignments(Long scheduleId) {
        return studentLunchAssignmentRepository.findManualAssignments(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentLunchAssignment> getLockedAssignments(Long scheduleId) {
        return studentLunchAssignmentRepository.findLockedAssignments(scheduleId);
    }

    // ========== Assignment Management ==========

    @Override
    public void lockAssignment(Long assignmentId, String username) {
        StudentLunchAssignment assignment = studentLunchAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        assignment.lock(username);
        studentLunchAssignmentRepository.save(assignment);
        log.info("Locked assignment {} by {}", assignmentId, username);
    }

    @Override
    public void unlockAssignment(Long assignmentId, String username) {
        StudentLunchAssignment assignment = studentLunchAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        assignment.unlock(username);
        studentLunchAssignmentRepository.save(assignment);
        log.info("Unlocked assignment {} by {}", assignmentId, username);
    }

    @Override
    public void setAssignmentPriority(Long assignmentId, int priority, String username) {
        if (priority < 1 || priority > 10) {
            throw new IllegalArgumentException("Priority must be between 1 and 10");
        }

        StudentLunchAssignment assignment = studentLunchAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        assignment.setPriority(priority);
        assignment.updateModification(username);
        studentLunchAssignmentRepository.save(assignment);
        log.info("Set assignment {} priority to {} by {}", assignmentId, priority, username);
    }

    @Override
    public void markAsManualOverride(Long assignmentId, String username) {
        StudentLunchAssignment assignment = studentLunchAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        assignment.markAsManualOverride();
        assignment.setLastModifiedBy(username);
        assignment.setLastModifiedAt(LocalDateTime.now());
        studentLunchAssignmentRepository.save(assignment);
        log.info("Marked assignment {} as manual override by {}", assignmentId, username);
    }

    // ========== Teacher Assignment Methods ==========

    @Override
    public int assignTeachersToLunchWaves(Long scheduleId) {
        // Load entity at start
        Schedule schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        log.info("Assigning teachers to lunch waves for schedule {}", schedule.getId());

        List<LunchWave> waves = lunchWaveRepository.findActiveByScheduleId(schedule.getId());
        if (waves.isEmpty()) {
            log.warn("No active lunch waves found");
            return 0;
        }

        List<Teacher> teachers = getUnassignedTeachers(schedule.getId());
        if (teachers.isEmpty()) {
            log.info("No unassigned teachers");
            return 0;
        }

        // Distribute teachers evenly across waves
        int assignedCount = 0;
        int currentWaveIndex = 0;

        for (Teacher teacher : teachers) {
            LunchWave wave = waves.get(currentWaveIndex);

            TeacherLunchAssignment assignment = TeacherLunchAssignment.builder()
                .teacher(teacher)
                .schedule(schedule)
                .lunchWave(wave)
                .assignmentMethod(LunchAssignmentMethod.BALANCED.name())
                .isDutyFree(true)
                .hasSupervisionDuty(false)
                .hasDutyDuringOtherWaves(false)
                .assignedAt(LocalDateTime.now())
                .assignedBy("SYSTEM")
                .priority(5)
                .build();

            teacherLunchAssignmentRepository.save(assignment);
            assignedCount++;

            currentWaveIndex = (currentWaveIndex + 1) % waves.size();
        }

        log.info("Assigned {} teachers to lunch waves", assignedCount);
        return assignedCount;
    }

    @Override
    public TeacherLunchAssignment assignTeacherToWave(Long teacherId, Long lunchWaveId, String username) {
        // Load entities at start
        Teacher teacher = sisDataService.getTeacherById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("Teacher not found: " + teacherId));
        LunchWave lunchWave = lunchWaveRepository.findById(lunchWaveId)
            .orElseThrow(() -> new IllegalArgumentException("LunchWave not found: " + lunchWaveId));

        log.debug("Assigning teacher {} to lunch wave {}", teacher.getEmployeeId(), lunchWave.getWaveName());

        TeacherLunchAssignment assignment = TeacherLunchAssignment.builder()
            .teacher(teacher)
            .schedule(lunchWave.getSchedule())
            .lunchWave(lunchWave)
            .assignmentMethod(LunchAssignmentMethod.MANUAL.name())
            .isDutyFree(true)
            .hasSupervisionDuty(false)
            .assignedAt(LocalDateTime.now())
            .assignedBy(username)
            .manualOverride(true)
            .priority(5)
            .build();

        assignment = teacherLunchAssignmentRepository.save(assignment);
        log.info("Assigned teacher {} to {}", teacher.getEmployeeId(), lunchWave.getWaveName());

        return assignment;
    }

    @Override
    public TeacherLunchAssignment reassignTeacher(Long assignmentId, Long newWaveId, String username) {
        // Load entities at start
        TeacherLunchAssignment assignment = teacherLunchAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("TeacherLunchAssignment not found: " + assignmentId));
        LunchWave newWave = lunchWaveRepository.findById(newWaveId)
            .orElseThrow(() -> new IllegalArgumentException("LunchWave not found: " + newWaveId));

        // ✅ NULL SAFE: Check for null teacher and lunchWave references
        if (assignment.getTeacher() == null || assignment.getLunchWave() == null) {
            throw new IllegalStateException("Assignment must have teacher and lunchWave");
        }

        log.info("Reassigning teacher {} from {} to {}",
            assignment.getTeacher().getEmployeeId(),
            assignment.getLunchWave().getWaveName(),
            newWave.getWaveName());

        assignment.setLunchWave(newWave);
        assignment.setLastModifiedAt(LocalDateTime.now());
        assignment.setLastModifiedBy(username);
        assignment.setManualOverride(true);

        return teacherLunchAssignmentRepository.save(assignment);
    }

    @Override
    public TeacherLunchAssignment assignSupervisionDuty(Long assignmentId, String location, String username) {
        TeacherLunchAssignment assignment = teacherLunchAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        assignment.assignSupervisionDuty();
        assignment.setLastModifiedBy(username);
        assignment.setLastModifiedAt(LocalDateTime.now());
        teacherLunchAssignmentRepository.save(assignment);
        log.info("Assigned supervision duty at {} to teacher {}", location, assignment.getTeacher().getEmployeeId());

        return assignment;
    }

    @Override
    public TeacherLunchAssignment removeSupervisionDuty(Long assignmentId, String username) {
        TeacherLunchAssignment assignment = teacherLunchAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        assignment.removeSupervisionDuty();
        assignment.setLastModifiedBy(username);
        assignment.setLastModifiedAt(LocalDateTime.now());
        teacherLunchAssignmentRepository.save(assignment);
        log.info("Removed supervision duty from teacher {}", assignment.getTeacher().getEmployeeId());

        return assignment;
    }

    @Override
    public TeacherLunchAssignment markDutyDuringOtherWaves(Long assignmentId, String username) {
        TeacherLunchAssignment assignment = teacherLunchAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new IllegalArgumentException("Assignment not found: " + assignmentId));

        assignment.setHasDutyDuringOtherWaves(true);
        assignment.updateModification(username);
        teacherLunchAssignmentRepository.save(assignment);
        log.info("Marked teacher {} as having duty during other waves", assignment.getTeacher().getEmployeeId());

        return assignment;
    }

    // ========== Teacher Query Methods ==========

    @Override
    @Transactional(readOnly = true)
    public List<Teacher> getUnassignedTeachers(Long scheduleId) {
        return teacherLunchAssignmentRepository.findUnassignedTeachers(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TeacherLunchAssignment> getTeacherAssignment(Long teacherId, Long scheduleId) {
        return teacherLunchAssignmentRepository.findByTeacherIdAndScheduleId(teacherId, scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherLunchAssignment> getTeachersInWave(Long lunchWaveId) {
        return teacherLunchAssignmentRepository.findByLunchWaveIdOrderByTeacherName(lunchWaveId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherLunchAssignment> getDutyFreeTeachers(Long scheduleId) {
        return teacherLunchAssignmentRepository.findDutyFreeAssignments(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherLunchAssignment> getTeachersWithSupervisionDuty(Long scheduleId) {
        return teacherLunchAssignmentRepository.findWithSupervisionDuty(scheduleId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherLunchAssignment> getTeachersAvailableForSupervision(Long scheduleId) {
        return teacherLunchAssignmentRepository.findAvailableForSupervision(scheduleId);
    }

    // ========== Validation Methods ==========

    @Override
    @Transactional(readOnly = true)
    public boolean areAllStudentsAssigned(Long scheduleId) {
        return countUnassignedStudents(scheduleId) == 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areCapacitiesRespected(Long scheduleId) {
        List<LunchWave> fullWaves = lunchWaveRepository.findFullByScheduleId(scheduleId);
        return fullWaves.isEmpty();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areGradeLevelsRespected(Long scheduleId) {
        List<StudentLunchAssignment> assignments = studentLunchAssignmentRepository.findByScheduleId(scheduleId);

        for (StudentLunchAssignment assignment : assignments) {
            LunchWave wave = assignment.getLunchWave();
            Student student = assignment.getStudent();

            // ✅ NULL SAFE: Skip assignments with null wave or student
            if (wave == null || student == null) {
                log.warn("Assignment has null wave or student, skipping validation");
                continue;
            }

            Integer studentGrade = parseGradeLevel(student.getGradeLevel());
            if (!wave.isGradeLevelEligible(studentGrade)) {
                log.warn("Student {} grade {} not eligible for wave {} (restriction: {})",
                    student.getStudentId(), student.getGradeLevel(),
                    wave.getWaveName(), wave.getGradeLevelRestriction());
                return false;
            }
        }

        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean areAssignmentsValid(Long scheduleId) {
        // Check all students assigned
        if (!areAllStudentsAssigned(scheduleId)) {
            log.warn("Not all students are assigned to lunch waves");
            return false;
        }

        // Check capacities
        if (!areCapacitiesRespected(scheduleId)) {
            log.warn("Some lunch waves are over capacity");
            return false;
        }

        // Check grade levels
        if (!areGradeLevelsRespected(scheduleId)) {
            log.warn("Some students are in wrong grade-level waves");
            return false;
        }

        // Check at least one teacher per wave
        List<LunchWave> waves = lunchWaveRepository.findActiveByScheduleId(scheduleId);
        for (LunchWave wave : waves) {
            long teacherCount = teacherLunchAssignmentRepository.countByLunchWave(wave);
            if (teacherCount == 0) {
                log.warn("Lunch wave {} has no teachers assigned", wave.getWaveName());
                return false;
            }
        }

        log.debug("All lunch assignments are valid for schedule {}", scheduleId);
        return true;
    }

    // ========== Statistics Methods ==========

    @Override
    @Transactional(readOnly = true)
    public LunchAssignmentStatistics getAssignmentStatistics(Long scheduleId) {
        LunchAssignmentStatistics stats = new LunchAssignmentStatistics();

        // Student statistics
        List<StudentLunchAssignment> studentAssignments = studentLunchAssignmentRepository.findByScheduleId(scheduleId);
        stats.setAssignedStudents(studentAssignments.size());
        stats.setUnassignedStudents(countUnassignedStudents(scheduleId));
        stats.setTotalStudents(stats.getAssignedStudents() + stats.getUnassignedStudents());
        stats.setLockedAssignments(studentAssignments.stream().filter(a -> a.getIsLocked() != null && a.getIsLocked()).count());
        stats.setManualOverrides(studentAssignments.stream().filter(a -> a.getManualOverride() != null && a.getManualOverride()).count());

        // Teacher statistics
        List<TeacherLunchAssignment> teacherAssignments = teacherLunchAssignmentRepository.findByScheduleId(scheduleId);
        stats.setAssignedTeachers(teacherAssignments.size());
        stats.setTotalTeachers(stats.getAssignedTeachers() + teacherLunchAssignmentRepository.countUnassignedTeachers(scheduleId));
        stats.setDutyFreeTeachers(teacherAssignments.stream().filter(a -> a.getIsDutyFree() != null && a.getIsDutyFree()).count());
        stats.setTeachersWithDuty(teacherAssignments.stream().filter(a -> a.getHasSupervisionDuty() != null && a.getHasSupervisionDuty()).count());

        return stats;
    }
}
