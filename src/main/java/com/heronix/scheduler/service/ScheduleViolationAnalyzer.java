package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.RoomType;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Schedule Violation Analyzer Service
 *
 * Provides detailed analysis of schedule generation failures, identifying:
 * - Courses without teachers
 * - Courses without appropriate rooms
 * - Teacher availability issues
 * - Room capacity violations
 * - Suggested fixes with actionable recommendations
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 8D - November 21, 2025
 */
@Slf4j
@Service
public class ScheduleViolationAnalyzer {

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * Violation types for categorization
     */
    public enum ViolationType {
        NO_TEACHER("Course has no assigned teacher"),
        NO_ROOM("No suitable room available"),
        ROOM_CAPACITY("Room capacity insufficient"),
        TEACHER_OVERLOAD("Teacher workload exceeds limit"),
        ROOM_TYPE_MISMATCH("Course requires specific room type not available"),
        SCHEDULING_CONFLICT("Time slot conflicts exist"),
        INSUFFICIENT_ROOMS("Not enough rooms of required type");

        private final String description;

        ViolationType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Represents a single violation with details and suggested fix
     */
    @Data
    @Builder
    public static class Violation {
        private ViolationType type;
        private String entityName;           // Course name, Teacher name, Room number
        private Long entityId;
        private String description;
        private String suggestedFix;
        private List<SuggestedAction> actions; // Interactive fix options
        private int severity;                 // 1-3, 3 being critical
    }

    /**
     * Suggested action that can be taken to fix a violation
     */
    @Data
    @Builder
    public static class SuggestedAction {
        private String actionType;    // ASSIGN_TEACHER, ASSIGN_ROOM, CREATE_ROOM, etc.
        private String label;         // Display label for UI
        private Long targetId;        // ID of entity to act on
        private Map<String, Object> parameters;  // Additional parameters
    }

    /**
     * Complete analysis result
     */
    @Data
    @Builder
    public static class AnalysisResult {
        private int totalViolations;
        private int criticalCount;
        private int warningCount;
        private int infoCount;
        private List<Violation> violations;
        private Map<ViolationType, Integer> violationsByType;
        private List<String> summaryLines;
        private boolean canAutoFix;
    }

    /**
     * Analyze the current schedule data and identify all potential violations
     * before attempting to generate a schedule
     */
    @Transactional(readOnly = true)
    public AnalysisResult analyzePreSchedule() {
        log.info("Starting pre-schedule violation analysis...");

        List<Violation> violations = new ArrayList<>();

        // 1. Check courses without teachers
        violations.addAll(analyzeCourseTeacherAssignments());

        // 2. Check room availability by type
        violations.addAll(analyzeRoomAvailability());

        // 3. Check room capacity vs enrollment
        violations.addAll(analyzeRoomCapacity());

        // 4. Check teacher workload
        violations.addAll(analyzeTeacherWorkload());

        // Build result
        Map<ViolationType, Integer> byType = violations.stream()
            .collect(Collectors.groupingBy(
                Violation::getType,
                Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
            ));

        int critical = (int) violations.stream().filter(v -> v.getSeverity() == 3).count();
        int warning = (int) violations.stream().filter(v -> v.getSeverity() == 2).count();
        int info = (int) violations.stream().filter(v -> v.getSeverity() == 1).count();

        List<String> summary = buildSummary(violations, byType);

        boolean canAutoFix = violations.stream()
            .allMatch(v -> v.getActions() != null && !v.getActions().isEmpty());

        log.info("Analysis complete: {} violations ({} critical, {} warning, {} info)",
            violations.size(), critical, warning, info);

        return AnalysisResult.builder()
            .totalViolations(violations.size())
            .criticalCount(critical)
            .warningCount(warning)
            .infoCount(info)
            .violations(violations)
            .violationsByType(byType)
            .summaryLines(summary)
            .canAutoFix(canAutoFix)
            .build();
    }

    /**
     * Analyze courses that don't have teachers assigned
     */
    private List<Violation> analyzeCourseTeacherAssignments() {
        List<Violation> violations = new ArrayList<>();

        List<Course> allCourses = sisDataService.getAllCourses();
        List<Teacher> availableTeachers = sisDataService.getAllTeachers().stream().filter(t -> Boolean.TRUE.equals(t.getActive())).toList();

        for (Course course : allCourses) {
            if (!Boolean.TRUE.equals(course.getActive())) continue;

            if (course.getTeacher() == null) {
                // Find potential teachers for this course
                List<SuggestedAction> actions = findTeachersForCourse(course, availableTeachers);

                violations.add(Violation.builder()
                    .type(ViolationType.NO_TEACHER)
                    .entityName(course.getCourseName())
                    .entityId(course.getId())
                    .description(String.format("Course '%s' (%s) has no assigned teacher",
                        course.getCourseName(), course.getSubject()))
                    .suggestedFix(actions.isEmpty()
                        ? "No qualified teachers available. Consider hiring for " + course.getSubject()
                        : "Assign one of " + actions.size() + " qualified teacher(s)")
                    .actions(actions)
                    .severity(3) // Critical
                    .build());
            }
        }

        return violations;
    }

    /**
     * Find teachers who could teach a given course
     */
    private List<SuggestedAction> findTeachersForCourse(Course course, List<Teacher> teachers) {
        List<SuggestedAction> actions = new ArrayList<>();

        for (Teacher teacher : teachers) {
            boolean isQualified = false;
            String qualificationReason = "";

            // Check certifications
            if (course.getSubject() != null) {
                if (teacher.hasCertificationForSubject(course.getSubject())) {
                    isQualified = true;
                    qualificationReason = "Has certification for " + course.getSubject();
                } else if (teacher.getCertifications() != null) {
                    for (String cert : teacher.getCertifications()) {
                        if (cert != null && cert.toLowerCase().contains(course.getSubject().toLowerCase())) {
                            isQualified = true;
                            qualificationReason = "Has certification: " + cert;
                            break;
                        }
                    }
                }
            }

            // Check workload
            int currentCourses = teacher.getCourseCount();
            int maxCourses = 6; // Default max courses per teacher
            boolean hasCapacity = currentCourses < maxCourses;

            if (isQualified && hasCapacity) {
                actions.add(SuggestedAction.builder()
                    .actionType("ASSIGN_TEACHER")
                    .label(String.format("%s (%d/%d courses) - %s",
                        teacher.getName(), currentCourses, maxCourses, qualificationReason))
                    .targetId(teacher.getId())
                    .parameters(Map.of(
                        "courseId", course.getId(),
                        "teacherId", teacher.getId(),
                        "teacherName", teacher.getName()
                    ))
                    .build());
            }
        }

        return actions;
    }

    /**
     * Analyze room availability by type
     */
    private List<Violation> analyzeRoomAvailability() {
        List<Violation> violations = new ArrayList<>();

        List<Course> allCourses = sisDataService.getAllCourses();
        List<Room> allRooms = roomRepository.findByActiveTrue();

        // Count rooms by type
        Map<RoomType, Long> roomsByType = allRooms.stream()
            .filter(r -> r.getType() != null)
            .collect(Collectors.groupingBy(Room::getType, Collectors.counting()));

        // Count course requirements by room type
        Map<String, List<Course>> coursesByRequiredRoom = new HashMap<>();

        for (Course course : allCourses) {
            if (!Boolean.TRUE.equals(course.getActive())) continue;

            String subject = course.getSubject();
            if (subject == null) continue;

            String subjectLower = subject.toLowerCase();
            String requiredType = null;

            if (subjectLower.contains("physical education") || subjectLower.contains("pe") ||
                subjectLower.contains("gym") || subjectLower.contains("athletics")) {
                requiredType = "GYMNASIUM";
            } else if (subjectLower.contains("music") || subjectLower.contains("band") ||
                       subjectLower.contains("orchestra") || subjectLower.contains("choir")) {
                requiredType = "MUSIC_ROOM";
            } else if (Boolean.TRUE.equals(course.getRequiresLab()) ||
                       subjectLower.contains("science") || subjectLower.contains("chemistry") ||
                       subjectLower.contains("biology") || subjectLower.contains("physics")) {
                requiredType = "SCIENCE_LAB";
            } else if (subjectLower.contains("computer") || subjectLower.contains("technology") ||
                       subjectLower.contains("coding")) {
                requiredType = "COMPUTER_LAB";
            } else if (subjectLower.contains("art")) {
                requiredType = "ART_STUDIO";
            }

            if (requiredType != null) {
                coursesByRequiredRoom.computeIfAbsent(requiredType, k -> new ArrayList<>()).add(course);
            }
        }

        // Check for insufficient rooms
        for (Map.Entry<String, List<Course>> entry : coursesByRequiredRoom.entrySet()) {
            String requiredType = entry.getKey();
            List<Course> courses = entry.getValue();

            try {
                RoomType roomType = RoomType.valueOf(requiredType);
                long availableRooms = roomsByType.getOrDefault(roomType, 0L);

                // Check if rooms can support concurrent usage
                List<Room> roomsOfType = allRooms.stream()
                    .filter(r -> r.getType() == roomType)
                    .collect(Collectors.toList());

                // Calculate max concurrent capacity
                int maxConcurrentSlots = 0;
                for (Room room : roomsOfType) {
                    maxConcurrentSlots += room.getMaxConcurrentClasses();
                }

                // Estimate periods needed (assuming 6 periods per day, 5 days)
                int periodsAvailable = maxConcurrentSlots * 30; // 30 periods per week
                int periodsNeeded = courses.size() * 5; // Each course meets ~5 times per week

                if (periodsNeeded > periodsAvailable) {
                    List<SuggestedAction> actions = new ArrayList<>();

                    // Suggest enabling room sharing
                    for (Room room : roomsOfType) {
                        if (!room.isAllowSharing()) {
                            actions.add(SuggestedAction.builder()
                                .actionType("ENABLE_SHARING")
                                .label(String.format("Enable room sharing for %s (Room %s)",
                                    roomType.getDisplayName(), room.getRoomNumber()))
                                .targetId(room.getId())
                                .parameters(Map.of(
                                    "roomId", room.getId(),
                                    "roomNumber", room.getRoomNumber(),
                                    "maxConcurrent", 3 // Suggest 3 concurrent classes
                                ))
                                .build());
                        }
                    }

                    // Calculate detailed capacity analysis
                    int shortfall = periodsNeeded - periodsAvailable;
                    int additionalRoomsNeeded = (int) Math.ceil((double) shortfall / 30);
                    int concurrentClassesNeeded = (int) Math.ceil((double) periodsNeeded / (availableRooms * 30));

                    // Build enhanced description with capacity analysis
                    String enhancedDescription = String.format(
                        "%d courses need %s but only %d room(s) available. " +
                        "Need %d periods/week but only %d available.\n\n" +
                        "📊 CAPACITY ANALYSIS:\n" +
                        "  • Current: %d room(s) × 30 periods/week = %d period capacity\n" +
                        "  • Required: %d periods/week\n" +
                        "  • Shortfall: %d periods/week\n\n" +
                        "💡 SOLUTIONS:\n" +
                        "  Option A: Add %d more %s room(s) (%d total needed)\n" +
                        "  Option B: Enable room sharing (maxConcurrentClasses = %d) to reach %d capacity\n" +
                        "  Option C: Reduce course sections or extend room availability",
                        courses.size(), roomType.getDisplayName(), availableRooms,
                        periodsNeeded, periodsAvailable,
                        availableRooms, periodsAvailable,
                        periodsNeeded,
                        shortfall,
                        additionalRoomsNeeded, roomType.getDisplayName(), (int)(availableRooms + additionalRoomsNeeded),
                        concurrentClassesNeeded, (int)(availableRooms * concurrentClassesNeeded * 30)
                    );

                    violations.add(Violation.builder()
                        .type(ViolationType.INSUFFICIENT_ROOMS)
                        .entityName(roomType.getDisplayName())
                        .entityId(null)
                        .description(enhancedDescription)
                        .suggestedFix(actions.isEmpty()
                            ? "Consider adding more " + roomType.getDisplayName() + " rooms"
                            : "Enable room sharing to increase capacity")
                        .actions(actions)
                        .severity(courses.size() > 3 ? 3 : 2)
                        .build());
                }
            } catch (IllegalArgumentException e) {
                log.warn("Unknown room type: {}", requiredType);
            }
        }

        return violations;
    }

    /**
     * Analyze room capacity vs course enrollment
     */
    private List<Violation> analyzeRoomCapacity() {
        List<Violation> violations = new ArrayList<>();

        List<Course> allCourses = sisDataService.getAllCourses();
        List<Room> allRooms = roomRepository.findByActiveTrue();

        // Find maximum room capacity for standard classrooms
        int maxStandardCapacity = allRooms.stream()
            .filter(r -> r.getType() == RoomType.CLASSROOM || r.getType() == null)
            .mapToInt(Room::getEffectiveMaxCapacity)
            .max()
            .orElse(35);

        for (Course course : allCourses) {
            if (!Boolean.TRUE.equals(course.getActive())) continue;

            int enrolled = course.getCurrentEnrollment() != null ? course.getCurrentEnrollment() : 0;

            if (enrolled > maxStandardCapacity) {
                // Find larger rooms that could work
                List<SuggestedAction> actions = allRooms.stream()
                    .filter(r -> r.getEffectiveMaxCapacity() >= enrolled)
                    .map(r -> SuggestedAction.builder()
                        .actionType("ASSIGN_ROOM")
                        .label(String.format("Room %s (%s, capacity: %d)",
                            r.getRoomNumber(),
                            r.getType() != null ? r.getType().getDisplayName() : "Classroom",
                            r.getEffectiveMaxCapacity()))
                        .targetId(r.getId())
                        .parameters(Map.of(
                            "courseId", course.getId(),
                            "roomId", r.getId(),
                            "roomCapacity", r.getEffectiveMaxCapacity()
                        ))
                        .build())
                    .collect(Collectors.toList());

                violations.add(Violation.builder()
                    .type(ViolationType.ROOM_CAPACITY)
                    .entityName(course.getCourseName())
                    .entityId(course.getId())
                    .description(String.format(
                        "Course '%s' has %d students but max classroom capacity is %d",
                        course.getCourseName(), enrolled, maxStandardCapacity))
                    .suggestedFix(actions.isEmpty()
                        ? "Consider splitting into sections or using auditorium/gymnasium"
                        : "Use one of " + actions.size() + " larger room(s)")
                    .actions(actions)
                    .severity(enrolled - maxStandardCapacity > 10 ? 3 : 2)
                    .build());
            }
        }

        return violations;
    }

    /**
     * Analyze teacher workload for potential overloads
     */
    private List<Violation> analyzeTeacherWorkload() {
        List<Violation> violations = new ArrayList<>();

        List<Teacher> allTeachers = sisDataService.getAllTeachers();

        for (Teacher teacher : allTeachers) {
            if (!Boolean.TRUE.equals(teacher.getActive())) continue;

            int courseCount = teacher.getCourseCount();
            int maxCourses = 6; // Default max courses per teacher

            if (courseCount > maxCourses) {
                // Find courses that could be reassigned
                List<SuggestedAction> actions = new ArrayList<>();

                if (teacher.getCourses() != null) {
                    for (Course course : teacher.getCourses()) {
                        // Find alternative teachers
                        List<Teacher> alternates = allTeachers.stream()
                            .filter(t -> !t.equals(teacher))
                            .filter(t -> t.getCourseCount() < 6) // Filter teachers under max load
                            .filter(t -> course.getSubject() == null ||
                                         t.hasCertificationForSubject(course.getSubject()))
                            .limit(3)
                            .collect(Collectors.toList());

                        for (Teacher alt : alternates) {
                            actions.add(SuggestedAction.builder()
                                .actionType("REASSIGN_COURSE")
                                .label(String.format("Move '%s' to %s (%d courses)",
                                    course.getCourseName(), alt.getName(), alt.getCourseCount()))
                                .targetId(course.getId())
                                .parameters(Map.of(
                                    "courseId", course.getId(),
                                    "fromTeacherId", teacher.getId(),
                                    "toTeacherId", alt.getId()
                                ))
                                .build());
                        }
                    }
                }

                violations.add(Violation.builder()
                    .type(ViolationType.TEACHER_OVERLOAD)
                    .entityName(teacher.getName())
                    .entityId(teacher.getId())
                    .description(String.format(
                        "Teacher '%s' has %d courses (max: %d)",
                        teacher.getName(), courseCount, maxCourses))
                    .suggestedFix(actions.isEmpty()
                        ? "Consider hiring additional staff for " + teacher.getDepartment()
                        : "Reassign " + (courseCount - maxCourses) + " course(s) to other teachers")
                    .actions(actions)
                    .severity(courseCount - maxCourses > 2 ? 3 : 2)
                    .build());
            }
        }

        return violations;
    }

    /**
     * Build summary lines for display
     */
    private List<String> buildSummary(List<Violation> violations, Map<ViolationType, Integer> byType) {
        List<String> summary = new ArrayList<>();

        if (violations.isEmpty()) {
            summary.add("No violations detected. Schedule generation should proceed without issues.");
            return summary;
        }

        summary.add(String.format("Found %d potential issue(s) that may prevent schedule generation:", violations.size()));
        summary.add("");

        for (Map.Entry<ViolationType, Integer> entry : byType.entrySet()) {
            summary.add(String.format("• %s: %d issue(s)", entry.getKey().getDescription(), entry.getValue()));
        }

        summary.add("");

        // Top priority items
        long criticalCount = violations.stream().filter(v -> v.getSeverity() == 3).count();
        if (criticalCount > 0) {
            summary.add(String.format("⚠️ %d CRITICAL issue(s) must be resolved before scheduling.", criticalCount));
        }

        return summary;
    }

    /**
     * Get a quick summary string suitable for error dialogs
     */
    @Transactional(readOnly = true)
    public String getQuickSummary() {
        AnalysisResult result = analyzePreSchedule();

        if (result.getTotalViolations() == 0) {
            return "No issues detected.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d issue(s) found:\n\n", result.getTotalViolations()));

        // Group by type for cleaner display
        for (Map.Entry<ViolationType, Integer> entry : result.getViolationsByType().entrySet()) {
            sb.append(String.format("• %s: %d\n", entry.getKey().getDescription(), entry.getValue()));
        }

        if (result.getCriticalCount() > 0) {
            sb.append(String.format("\n⚠️ %d critical issue(s) must be fixed.", result.getCriticalCount()));
        }

        return sb.toString();
    }
}
