package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.dto.AIIssue;
import com.heronix.scheduler.service.data.SISDataService;
import com.heronix.scheduler.repository.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Rule-Based Schedule Issue Detector
 *
 * Detects common scheduling issues using predefined rules.
 * Complements AI analysis with fast, deterministic checks.
 *
 * Issue Types Detected:
 * - Certification mismatches
 * - Teacher overload/underload
 * - Room capacity problems
 * - Room type mismatches
 * - Course sequence inconsistencies
 * - Resource underutilization
 *
 * @since Phase 2 - Background Analysis
 * @version 1.0.0
 */
@Slf4j
@Service
public class ScheduleIssueDetector {

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;

    // Thresholds
    private static final int OPTIMAL_WORKLOAD = 5;
    private static final int MAX_WORKLOAD = 6;
    private static final int HIGH_WORKLOAD_WARNING = 5;
    private static final int UNDERUTILIZED_THRESHOLD = 2;
    private static final double CAPACITY_BUFFER = 1.1; // 10% buffer

    /**
     * Detect all issues in current schedule
     */
    public List<AIIssue> detectAllIssues() {
        List<AIIssue> issues = new ArrayList<>();

        log.debug("Starting comprehensive issue detection");

        // Run all detection methods
        issues.addAll(detectCertificationMismatches());
        issues.addAll(detectTeacherWorkloadIssues());
        issues.addAll(detectRoomCapacityIssues());
        issues.addAll(detectRoomTypeMismatches());
        issues.addAll(detectCourseSequenceIssues());
        issues.addAll(detectResourceUnderutilization());
        issues.addAll(detectUnassignedCourses());

        // Sort by priority (highest first)
        issues.sort(Comparator.comparingInt(AIIssue::getPriority).reversed());

        log.info("Issue detection complete: {} issues found", issues.size());
        log.info("  - Critical: {}", issues.stream().filter(i -> i.getSeverity() == AIIssue.Severity.CRITICAL).count());
        log.info("  - Warning: {}", issues.stream().filter(i -> i.getSeverity() == AIIssue.Severity.WARNING).count());
        log.info("  - Info: {}", issues.stream().filter(i -> i.getSeverity() == AIIssue.Severity.INFO).count());

        return issues;
    }

    /**
     * Detect certification mismatches
     */
    private List<AIIssue> detectCertificationMismatches() {
        List<AIIssue> issues = new ArrayList<>();

        List<Course> assignedCourses = sisDataService.getAllCourses().stream()
            .filter(c -> c.getTeacher() != null)
            .collect(Collectors.toList());

        for (Course course : assignedCourses) {
            Teacher teacher = course.getTeacher();

            if (!isCertified(teacher, course)) {
                issues.add(AIIssue.builder()
                    .severity(AIIssue.Severity.CRITICAL)
                    .type(AIIssue.Type.CERTIFICATION_MISMATCH)
                    .title("Certification Mismatch")
                    .description(String.format("%s is not certified to teach %s (%s)",
                        teacher.getName(), course.getCourseName(), course.getSubject()))
                    .affectedEntity(String.format("Course: %s, Teacher: %s",
                        course.getCourseCode(), teacher.getName()))
                    .suggestedAction(String.format("Assign a teacher certified in %s, or verify %s's certifications",
                        course.getSubject(), teacher.getName()))
                    .priority(10)
                    .build());
            }
        }

        return issues;
    }

    /**
     * Detect teacher workload issues
     */
    private List<AIIssue> detectTeacherWorkloadIssues() {
        List<AIIssue> issues = new ArrayList<>();

        List<Teacher> allTeachers = sisDataService.getAllTeachers();

        for (Teacher teacher : allTeachers) {
            if (!teacher.getActive()) continue;

            int courseCount = teacher.getCourseCount();

            // Overloaded (CRITICAL)
            if (courseCount > MAX_WORKLOAD) {
                issues.add(AIIssue.builder()
                    .severity(AIIssue.Severity.CRITICAL)
                    .type(AIIssue.Type.TEACHER_OVERLOAD)
                    .title("Teacher Overload")
                    .description(String.format("%s has %d courses (limit: %d)",
                        teacher.getName(), courseCount, MAX_WORKLOAD))
                    .affectedEntity("Teacher: " + teacher.getName())
                    .suggestedAction(String.format("Reassign %d course(s) to other teachers",
                        courseCount - MAX_WORKLOAD))
                    .priority(9)
                    .build());
            }
            // High workload (WARNING)
            else if (courseCount == HIGH_WORKLOAD_WARNING) {
                issues.add(AIIssue.builder()
                    .severity(AIIssue.Severity.WARNING)
                    .type(AIIssue.Type.TEACHER_OVERLOAD)
                    .title("High Workload")
                    .description(String.format("%s has %d courses (approaching limit of %d)",
                        teacher.getName(), courseCount, MAX_WORKLOAD))
                    .affectedEntity("Teacher: " + teacher.getName())
                    .suggestedAction("Consider this when assigning additional courses")
                    .priority(6)
                    .build());
            }
            // Underutilized (INFO)
            else if (courseCount <= UNDERUTILIZED_THRESHOLD) {
                issues.add(AIIssue.builder()
                    .severity(AIIssue.Severity.INFO)
                    .type(AIIssue.Type.RESOURCE_UNDERUTILIZATION)
                    .title("Teacher Underutilized")
                    .description(String.format("%s has only %d course(s) (capacity: %d)",
                        teacher.getName(), courseCount, OPTIMAL_WORKLOAD))
                    .affectedEntity("Teacher: " + teacher.getName())
                    .suggestedAction(String.format("Consider assigning %d more course(s)",
                        OPTIMAL_WORKLOAD - courseCount))
                    .priority(3)
                    .build());
            }
        }

        return issues;
    }

    /**
     * Detect room capacity issues
     */
    private List<AIIssue> detectRoomCapacityIssues() {
        List<AIIssue> issues = new ArrayList<>();

        List<Course> assignedCourses = sisDataService.getAllCourses().stream()
            .filter(c -> c.getRoom() != null && c.getMaxStudents() != null)
            .collect(Collectors.toList());

        for (Course course : assignedCourses) {
            Room room = course.getRoom();
            int required = course.getMaxStudents();
            int available = room.getCapacity();

            // Insufficient capacity (CRITICAL)
            if (available < required) {
                issues.add(AIIssue.builder()
                    .severity(AIIssue.Severity.CRITICAL)
                    .type(AIIssue.Type.ROOM_CAPACITY)
                    .title("Insufficient Room Capacity")
                    .description(String.format("Room %s has capacity %d but %s needs %d students",
                        room.getRoomNumber(), available, course.getCourseName(), required))
                    .affectedEntity(String.format("Course: %s, Room: %s",
                        course.getCourseCode(), room.getRoomNumber()))
                    .suggestedAction(String.format("Assign a room with capacity %d or more",
                        required))
                    .priority(8)
                    .build());
            }
            // Tight fit (WARNING)
            else if (available < required * CAPACITY_BUFFER) {
                issues.add(AIIssue.builder()
                    .severity(AIIssue.Severity.WARNING)
                    .type(AIIssue.Type.ROOM_CAPACITY)
                    .title("Tight Room Capacity")
                    .description(String.format("Room %s has capacity %d for %s with %d students (no buffer)",
                        room.getRoomNumber(), available, course.getCourseName(), required))
                    .affectedEntity(String.format("Course: %s, Room: %s",
                        course.getCourseCode(), room.getRoomNumber()))
                    .suggestedAction("Consider a larger room for comfort and flexibility")
                    .priority(4)
                    .build());
            }
        }

        return issues;
    }

    /**
     * Detect room type mismatches
     */
    private List<AIIssue> detectRoomTypeMismatches() {
        List<AIIssue> issues = new ArrayList<>();

        List<Course> assignedCourses = sisDataService.getAllCourses().stream()
            .filter(c -> c.getRoom() != null)
            .collect(Collectors.toList());

        for (Course course : assignedCourses) {
            Room room = course.getRoom();
            String subject = course.getSubject() != null ? course.getSubject().toLowerCase() : "";
            String courseName = course.getCourseName() != null ? course.getCourseName().toLowerCase() : "";

            // Science courses need labs
            if ((subject.contains("science") || subject.contains("chemistry") ||
                 subject.contains("biology") || subject.contains("physics") || course.isRequiresLab()) &&
                (!room.getRoomType().name().contains("LAB"))) {

                issues.add(AIIssue.builder()
                    .severity(AIIssue.Severity.WARNING)
                    .type(AIIssue.Type.ROOM_TYPE_MISMATCH)
                    .title("Room Type Mismatch")
                    .description(String.format("%s (%s) assigned to %s (%s) - should be in a lab",
                        course.getCourseName(), subject, room.getRoomNumber(), room.getRoomType()))
                    .affectedEntity(String.format("Course: %s, Room: %s",
                        course.getCourseCode(), room.getRoomNumber()))
                    .suggestedAction("Assign to a Science Lab room")
                    .priority(5)
                    .build());
            }

            // PE courses need gymnasium
            if ((subject.contains("physical education") || subject.contains("pe") ||
                 courseName.contains("gym")) &&
                (!room.getRoomType().name().contains("GYMNASIUM"))) {

                issues.add(AIIssue.builder()
                    .severity(AIIssue.Severity.WARNING)
                    .type(AIIssue.Type.ROOM_TYPE_MISMATCH)
                    .title("Room Type Mismatch")
                    .description(String.format("%s assigned to %s (%s) - should be in gymnasium",
                        course.getCourseName(), room.getRoomNumber(), room.getRoomType()))
                    .affectedEntity(String.format("Course: %s, Room: %s",
                        course.getCourseCode(), room.getRoomNumber()))
                    .suggestedAction("Assign to a Gymnasium")
                    .priority(5)
                    .build());
            }
        }

        return issues;
    }

    /**
     * Detect course sequence inconsistencies
     */
    private List<AIIssue> detectCourseSequenceIssues() {
        List<AIIssue> issues = new ArrayList<>();

        // Group courses by sequence (e.g., English 1, English 2)
        Map<String, List<Course>> sequences = sisDataService.getAllCourses().stream()
            .filter(c -> c.getTeacher() != null)
            .collect(Collectors.groupingBy(this::getSequenceKey));

        for (Map.Entry<String, List<Course>> entry : sequences.entrySet()) {
            List<Course> sequence = entry.getValue();

            // Only check sequences with 2+ courses
            if (sequence.size() < 2) continue;

            // Check if same teacher teaches all courses in sequence
            Set<Teacher> teachers = sequence.stream()
                .map(Course::getTeacher)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            if (teachers.size() > 1) {
                String courseList = sequence.stream()
                    .map(Course::getCourseName)
                    .collect(Collectors.joining(", "));

                String teacherList = teachers.stream()
                    .map(Teacher::getName)
                    .collect(Collectors.joining(", "));

                issues.add(AIIssue.builder()
                    .severity(AIIssue.Severity.INFO)
                    .type(AIIssue.Type.SEQUENCE_INCONSISTENCY)
                    .title("Course Sequence Inconsistency")
                    .description(String.format("Course sequence [%s] taught by multiple teachers: %s",
                        courseList, teacherList))
                    .affectedEntity("Sequence: " + entry.getKey())
                    .suggestedAction("Consider assigning all courses in sequence to one teacher for continuity")
                    .priority(2)
                    .build());
            }
        }

        return issues;
    }

    /**
     * Detect resource underutilization
     */
    private List<AIIssue> detectResourceUnderutilization() {
        List<AIIssue> issues = new ArrayList<>();

        // Check for unused rooms
        List<Room> allRooms = roomRepository.findAll();
        List<Course> allCourses = sisDataService.getAllCourses();

        Set<Room> usedRooms = allCourses.stream()
            .map(Course::getRoom)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        long unusedRooms = allRooms.stream()
            .filter(Room::getActive)
            .filter(r -> !usedRooms.contains(r))
            .count();

        if (unusedRooms > 0) {
            issues.add(AIIssue.builder()
                .severity(AIIssue.Severity.INFO)
                .type(AIIssue.Type.RESOURCE_UNDERUTILIZATION)
                .title("Unused Rooms")
                .description(String.format("%d room(s) are not assigned to any courses", unusedRooms))
                .affectedEntity("Facility Resources")
                .suggestedAction("Consider utilizing these rooms or marking them as inactive")
                .priority(1)
                .build());
        }

        return issues;
    }

    /**
     * Detect unassigned courses
     */
    private List<AIIssue> detectUnassignedCourses() {
        List<AIIssue> issues = new ArrayList<>();

        // Note: Course doesn't have isUnassigned() method - filter differently
        // Unassigned course detection not implemented — Course lacks isUnassigned() method
        List<Course> unassignedCourses = new ArrayList<>();
        // List<Course> unassignedCourses = sisDataService.getAllCourses().stream()
        //     .filter(course -> course.isUnassigned())
        //     .collect(Collectors.toList());

        if (!unassignedCourses.isEmpty()) {
            String courseList = unassignedCourses.stream()
                .limit(5)
                .map(Course::getCourseCode)
                .collect(Collectors.joining(", "));

            if (unassignedCourses.size() > 5) {
                courseList += String.format(" and %d more", unassignedCourses.size() - 5);
            }

            issues.add(AIIssue.builder()
                .severity(AIIssue.Severity.WARNING)
                .type(AIIssue.Type.OTHER)
                .title("Unassigned Courses")
                .description(String.format("%d course(s) are not fully assigned: %s",
                    unassignedCourses.size(), courseList))
                .affectedEntity("Schedule Completion")
                .suggestedAction("Assign teachers and rooms to complete the schedule")
                .priority(7)
                .build());
        }

        return issues;
    }

    /**
     * Check if teacher is certified for course
     */
    private boolean isCertified(Teacher teacher, Course course) {
        if (teacher.getCertifiedSubjects() == null || teacher.getCertifiedSubjects().isEmpty() ||
            course.getSubject() == null) {
            return false;
        }

        String subject = course.getSubject().toLowerCase();
        return teacher.getCertifiedSubjects().stream()
            .anyMatch(cert -> {
                String certLower = cert.toLowerCase();
                return certLower.equals(subject) ||
                       certLower.contains(subject) ||
                       subject.contains(certLower) ||
                       isInSameSubjectFamily(certLower, subject);
            });
    }

    /**
     * Check if subjects are in same family
     */
    private boolean isInSameSubjectFamily(String cert, String subject) {
        // Science family
        Set<String> scienceFamily = Set.of("science", "biology", "chemistry", "physics",
            "earth science", "life science", "physical science");
        if (scienceFamily.stream().anyMatch(s -> cert.contains(s)) &&
            scienceFamily.stream().anyMatch(s -> subject.contains(s))) {
            return true;
        }

        // Math family
        Set<String> mathFamily = Set.of("math", "algebra", "geometry", "calculus",
            "trigonometry", "pre-calculus", "statistics");
        if (mathFamily.stream().anyMatch(s -> cert.contains(s)) &&
            mathFamily.stream().anyMatch(s -> subject.contains(s))) {
            return true;
        }

        // English family
        Set<String> englishFamily = Set.of("english", "literature", "language arts", "writing");
        if (englishFamily.stream().anyMatch(s -> cert.contains(s)) &&
            englishFamily.stream().anyMatch(s -> subject.contains(s))) {
            return true;
        }

        return false;
    }

    /**
     * Get sequence key for course (removes numbers and levels)
     */
    private String getSequenceKey(Course course) {
        String courseName = course.getCourseName().toLowerCase();
        String baseKey = courseName
            .replaceAll("\\s+[1-4]$", "")
            .replaceAll("\\s+i{1,3}$", "")
            .replaceAll("\\s+intro.*$", "")
            .replaceAll("\\s+advanced$", "")
            .replaceAll("\\s+ap$", "")
            .trim();
        return course.getSubject() + ":" + baseKey;
    }
}
