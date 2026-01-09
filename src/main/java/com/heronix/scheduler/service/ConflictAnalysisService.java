package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.dto.ConflictDetail;
import com.heronix.scheduler.model.dto.ConflictDetail.ManualOverrideOption;
import com.heronix.scheduler.model.enums.ConflictSeverity;
import com.heronix.scheduler.model.enums.ConflictType;
import com.heronix.scheduler.model.planning.SchedulingSolution;
import com.heronix.scheduler.repository.RoomRepository;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Conflict Analysis Service
 *
 * Analyzes scheduling solutions to identify and categorize conflicts,
 * providing detailed diagnostic information and suggested solutions.
 *
 * This service is critical for the partial scheduling feature - it enables
 * the system to generate schedules even when hard constraints can't be fully
 * satisfied, by providing users with actionable conflict reports.
 *
 * Location: src/main/java/com/eduscheduler/service/ConflictAnalysisService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-18
 */
@Slf4j
@Service
public class ConflictAnalysisService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SISDataService sisDataService;

    /**
     * Analyze a scheduling solution to identify all conflicts
     *
     * @param solution The scheduling solution to analyze
     * @return List of detailed conflict information
     */
    public List<ConflictDetail> analyzeConstraintViolations(SchedulingSolution solution) {
        log.info("Starting conflict analysis for scheduling solution");

        List<ConflictDetail> conflicts = new ArrayList<>();

        if (solution == null || solution.getScheduleSlots() == null) {
            log.warn("Cannot analyze null solution or solution with no slots");
            return conflicts;
        }

        List<ScheduleSlot> slots = solution.getScheduleSlots();
        log.info("Analyzing {} schedule slots for conflicts", slots.size());

        // Analyze each slot for assignment issues
        for (ScheduleSlot slot : slots) {
            if (!isSlotFullyAssigned(slot)) {
                ConflictDetail conflict = analyzeUnassignedSlot(slot);
                if (conflict != null) {
                    conflicts.add(conflict);
                }
            } else {
                // Check for constraint violations even in assigned slots
                List<ConflictDetail> slotConflicts = analyzeAssignedSlot(slot);
                conflicts.addAll(slotConflicts);
            }
        }

        // Analyze cross-slot conflicts (teacher/room double-booking)
        List<ConflictDetail> crossSlotConflicts = analyzeCrossSlotConflicts(slots);
        conflicts.addAll(crossSlotConflicts);

        // Sort by severity (most severe first)
        conflicts.sort((c1, c2) -> {
            if (c1.getSeverity() == null) return 1;
            if (c2.getSeverity() == null) return -1;
            return Integer.compare(
                c2.getSeverity().getPriorityScore(),
                c1.getSeverity().getPriorityScore()
            );
        });

        log.info("Conflict analysis complete: {} conflicts identified", conflicts.size());
        return conflicts;
    }

    /**
     * Check if a slot is fully assigned (has teacher, room, and time)
     */
    private boolean isSlotFullyAssigned(ScheduleSlot slot) {
        return slot.getTeacher() != null &&
               slot.getRoom() != null &&
               slot.getTimeSlot() != null;
    }

    /**
     * Analyze an unassigned slot to determine why it couldn't be scheduled
     */
    private ConflictDetail analyzeUnassignedSlot(ScheduleSlot slot) {
        if (slot.getCourse() == null) {
            log.warn("Slot {} has no course assigned", slot.getId());
            return null;
        }

        Course course = slot.getCourse();
        ConflictDetail.ConflictDetailBuilder builder = ConflictDetail.builder()
            .slotId(slot.getId())
            .courseId(course.getId())
            .courseName(course.getCourseName())
            .courseCode(course.getCourseCode())
            .studentsAffected(slot.getStudents() != null ? slot.getStudents().size() : 0)
            .blocking(true);

        // Determine specific reason for non-assignment
        if (slot.getTeacher() == null) {
            return analyzeNoTeacherConflict(slot, course, builder);
        } else if (slot.getRoom() == null) {
            return analyzeNoRoomConflict(slot, course, builder);
        } else if (slot.getTimeSlot() == null) {
            return analyzeNoTimeSlotConflict(slot, course, builder);
        }

        return null;
    }

    /**
     * Analyze why no teacher could be assigned
     */
    private ConflictDetail analyzeNoTeacherConflict(
            ScheduleSlot slot,
            Course course,
            ConflictDetail.ConflictDetailBuilder builder) {

        // Check if course has a teacher assigned in database
        if (course.getTeacher() == null || course.getTeacher().getId() == null) {
            return builder
                .type(ConflictType.TEACHER_OVERLOAD) // Using existing enum value
                .severity(ConflictSeverity.CRITICAL)
                .description("This course has no teacher assigned")
                .violatedConstraint("Course requires assigned teacher")
                .estimatedFixTimeMinutes(5)
                .possibleSolutions(List.of(
                    "Assign a qualified teacher to " + course.getCourseCode() + " in the Courses section",
                    "Import teachers with course assignments using CSV import",
                    "Run the automated course assignment utility (ASSIGN_COURSES_NOW.bat)"
                ))
                .overrideOptions(List.of(
                    ManualOverrideOption.builder()
                        .label("Assign any available teacher")
                        .action("ASSIGN_ANY_TEACHER")
                        .requiresConfirmation(true)
                        .warningMessage("Teacher may not be qualified for this subject")
                        .build()
                ))
                .build();
        }

        // Teacher exists but is overloaded
        List<Teacher> availableTeachers = sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());

        return builder
            .type(ConflictType.TEACHER_OVERLOAD)
            .severity(ConflictSeverity.HIGH)
            .description("All qualified teachers are at maximum capacity")
            .violatedConstraint("Teacher maximum periods per day exceeded")
            .estimatedFixTimeMinutes(10)
            .possibleSolutions(List.of(
                "Hire additional " + course.getSubject() + " teachers",
                "Increase max periods per day for existing teachers",
                "Reduce number of course sections",
                availableTeachers.isEmpty() ?
                    "Add teachers to the system" :
                    "Consider assigning to: " + availableTeachers.stream()
                        .limit(3)
                        .map(t -> t.getFirstName() + " " + t.getLastName())
                        .collect(Collectors.joining(", "))
            ))
            .build();
    }

    /**
     * Analyze why no room could be assigned
     */
    private ConflictDetail analyzeNoRoomConflict(
            ScheduleSlot slot,
            Course course,
            ConflictDetail.ConflictDetailBuilder builder) {

        // Check for special room requirements
        if (Boolean.TRUE.equals(course.getRequiresLab())) {
            long labCount = roomRepository.findAll().stream()
                .filter(r -> r.getType() != null && r.getType().isLab())
                .count();

            if (labCount == 0) {
                return builder
                    .type(ConflictType.ROOM_TYPE_MISMATCH)
                    .severity(ConflictSeverity.CRITICAL)
                    .description("Course requires a lab but no lab rooms exist in the system")
                    .violatedConstraint("Lab course requires lab room")
                    .estimatedFixTimeMinutes(15)
                    .possibleSolutions(List.of(
                        "Add a lab room in the Rooms section (Type: LAB or SCIENCE_LAB)",
                        "Convert an existing room to a lab",
                        "Mark course as not requiring a lab (if incorrect)"
                    ))
                    .overrideOptions(List.of(
                        ManualOverrideOption.builder()
                            .label("Schedule in regular classroom anyway")
                            .action("OVERRIDE_LAB_REQUIREMENT")
                            .requiresConfirmation(true)
                            .warningMessage("This course requires lab equipment which may not be available in a regular classroom")
                            .build()
                    ))
                    .build();
            }
        }

        // Check for PE/Gym requirement
        String subject = course.getSubject();
        if (subject != null && (subject.toLowerCase().contains("physical education") ||
                                subject.toLowerCase().contains("pe") ||
                                subject.toLowerCase().contains("gym"))) {
            long gymCount = roomRepository.findAll().stream()
                .filter(r -> r.getType() != null &&
                           (r.getType().name().equals("GYMNASIUM") || r.getType().name().equals("GYM")))
                .count();

            if (gymCount == 0) {
                return builder
                    .type(ConflictType.ROOM_TYPE_MISMATCH)
                    .severity(ConflictSeverity.CRITICAL)
                    .description("PE course requires a gymnasium but none exist in the system")
                    .violatedConstraint("PE courses require gymnasium")
                    .estimatedFixTimeMinutes(10)
                    .possibleSolutions(List.of(
                        "Add a gymnasium in the Rooms section (Type: GYMNASIUM)",
                        "Convert an existing room to gymnasium type",
                        "Schedule PE classes off-campus (if applicable)"
                    ))
                    .overrideOptions(List.of(
                        ManualOverrideOption.builder()
                            .label("Schedule in regular classroom")
                            .action("OVERRIDE_GYM_REQUIREMENT")
                            .requiresConfirmation(true)
                            .warningMessage("PE classes typically require a gymnasium for proper instruction")
                            .build()
                    ))
                    .build();
            }
        }

        // General room capacity issue
        int studentCount = slot.getStudents() != null ? slot.getStudents().size() : 0;
        List<Room> availableRooms = roomRepository.findAll().stream()
            .filter(r -> r.getCapacity() != null && r.getCapacity() >= studentCount)
            .collect(Collectors.toList());

        if (availableRooms.isEmpty()) {
            return builder
                .type(ConflictType.ROOM_CAPACITY_EXCEEDED)
                .severity(ConflictSeverity.HIGH)
                .description("No room large enough for " + studentCount + " students")
                .violatedConstraint("Room capacity must accommodate all enrolled students")
                .estimatedFixTimeMinutes(10)
                .possibleSolutions(List.of(
                    "Add a larger room (capacity: " + studentCount + "+)",
                    "Split course into multiple sections",
                    "Reduce course enrollment",
                    "Increase capacity of existing rooms"
                ))
                .build();
        }

        // All rooms at capacity at this time
        return builder
            .type(ConflictType.ROOM_DOUBLE_BOOKING)
            .severity(ConflictSeverity.HIGH)
            .description("All suitable rooms are occupied at available time slots")
            .violatedConstraint("Room availability exhausted")
            .estimatedFixTimeMinutes(5)
            .possibleSolutions(List.of(
                "Add more rooms to the system",
                "Extend school hours to create more time slots",
                availableRooms.isEmpty() ? "No suggestions available" :
                    "Consider using: " + availableRooms.stream()
                        .limit(3)
                        .map(Room::getRoomNumber)
                        .collect(Collectors.joining(", "))
            ))
            .build();
    }

    /**
     * Analyze why no time slot could be assigned
     */
    private ConflictDetail analyzeNoTimeSlotConflict(
            ScheduleSlot slot,
            Course course,
            ConflictDetail.ConflictDetailBuilder builder) {

        return builder
            .type(ConflictType.TIME_OVERLAP)
            .severity(ConflictSeverity.HIGH)
            .description("All available time slots have been exhausted")
            .violatedConstraint("No available time slots remain")
            .estimatedFixTimeMinutes(5)
            .possibleSolutions(List.of(
                "Extend school hours (add periods to start/end of day)",
                "Reduce number of course sections",
                "Enable lunch period scheduling",
                "Add more rooms to increase capacity"
            ))
            .build();
    }

    /**
     * Analyze assigned slot for constraint violations
     */
    private List<ConflictDetail> analyzeAssignedSlot(ScheduleSlot slot) {
        List<ConflictDetail> conflicts = new ArrayList<>();

        if (slot.getCourse() == null || slot.getRoom() == null) {
            return conflicts;
        }

        // Check room capacity
        int studentCount = slot.getStudents() != null ? slot.getStudents().size() : 0;
        Integer roomCapacity = slot.getRoom().getCapacity();

        if (roomCapacity != null && studentCount > roomCapacity) {
            int overflow = studentCount - roomCapacity;
            conflicts.add(ConflictDetail.builder()
                .slotId(slot.getId())
                .courseId(slot.getCourse().getId())
                .courseName(slot.getCourse().getCourseName())
                .courseCode(slot.getCourse().getCourseCode())
                .type(ConflictType.ROOM_CAPACITY_EXCEEDED)
                .severity(ConflictSeverity.MEDIUM)
                .description("Room " + slot.getRoom().getRoomNumber() + " capacity exceeded by " + overflow + " students")
                .violatedConstraint("Room capacity: " + roomCapacity + ", Students: " + studentCount)
                .studentsAffected(overflow)
                .blocking(false)
                .estimatedFixTimeMinutes(5)
                .possibleSolutions(List.of(
                    "Move to larger room",
                    "Split into 2 sections",
                    "Reduce enrollment by " + overflow + " students",
                    "Temporarily allow overflow (accept the risk)"
                ))
                .relatedEntityIds(List.of(slot.getRoom().getId()))
                .build()
            );
        }

        return conflicts;
    }

    /**
     * Analyze cross-slot conflicts (teacher/room double-booking)
     */
    private List<ConflictDetail> analyzeCrossSlotConflicts(List<ScheduleSlot> slots) {
        List<ConflictDetail> conflicts = new ArrayList<>();

        // Group slots by day and period
        Map<String, List<ScheduleSlot>> slotsByTime = new HashMap<>();

        for (ScheduleSlot slot : slots) {
            if (slot.getTimeSlot() == null) continue;

            String timeKey = slot.getDayOfWeek() + "_" + slot.getPeriodNumber();
            slotsByTime.computeIfAbsent(timeKey, k -> new ArrayList<>()).add(slot);
        }

        // Check each time slot for conflicts
        for (Map.Entry<String, List<ScheduleSlot>> entry : slotsByTime.entrySet()) {
            List<ScheduleSlot> slotsAtTime = entry.getValue();
            if (slotsAtTime.size() <= 1) continue;

            // Check for teacher conflicts
            Map<Long, List<ScheduleSlot>> slotsByTeacher = slotsAtTime.stream()
                .filter(s -> s.getTeacher() != null && s.getTeacher().getId() != null)
                .collect(Collectors.groupingBy(s -> s.getTeacher().getId()));

            for (Map.Entry<Long, List<ScheduleSlot>> teacherEntry : slotsByTeacher.entrySet()) {
                if (teacherEntry.getValue().size() > 1) {
                    conflicts.add(createTeacherConflict(teacherEntry.getValue()));
                }
            }

            // Check for room conflicts
            Map<Long, List<ScheduleSlot>> slotsByRoom = slotsAtTime.stream()
                .filter(s -> s.getRoom() != null && s.getRoom().getId() != null)
                .collect(Collectors.groupingBy(s -> s.getRoom().getId()));

            for (Map.Entry<Long, List<ScheduleSlot>> roomEntry : slotsByRoom.entrySet()) {
                if (roomEntry.getValue().size() > 1) {
                    conflicts.add(createRoomConflict(roomEntry.getValue()));
                }
            }
        }

        return conflicts;
    }

    /**
     * Create conflict detail for teacher double-booking
     */
    private ConflictDetail createTeacherConflict(List<ScheduleSlot> conflictingSlots) {
        ScheduleSlot firstSlot = conflictingSlots.get(0);
        Teacher teacher = firstSlot.getTeacher();

        String coursesList = conflictingSlots.stream()
            .map(s -> s.getCourse() != null ? s.getCourse().getCourseCode() : "Unknown")
            .collect(Collectors.joining(", "));

        int totalStudents = conflictingSlots.stream()
            .mapToInt(s -> s.getStudents() != null ? s.getStudents().size() : 0)
            .sum();

        return ConflictDetail.builder()
            .slotId(firstSlot.getId())
            .courseId(firstSlot.getCourse() != null ? firstSlot.getCourse().getId() : null)
            .courseName(coursesList)
            .type(ConflictType.TEACHER_OVERLOAD)
            .severity(ConflictSeverity.CRITICAL)
            .description("Teacher " + teacher.getFirstName() + " " + teacher.getLastName() +
                        " is assigned to " + conflictingSlots.size() + " classes at the same time")
            .violatedConstraint("Teacher can only teach one class at a time")
            .studentsAffected(totalStudents)
            .blocking(true)
            .estimatedFixTimeMinutes(10)
            .possibleSolutions(List.of(
                "Reassign one of these courses to a different teacher",
                "Move one course to a different time slot",
                "Cancel or combine duplicate sections"
            ))
            .relatedEntityIds(conflictingSlots.stream()
                .map(ScheduleSlot::getId)
                .collect(Collectors.toList()))
            .build();
    }

    /**
     * Create conflict detail for room double-booking
     */
    private ConflictDetail createRoomConflict(List<ScheduleSlot> conflictingSlots) {
        ScheduleSlot firstSlot = conflictingSlots.get(0);
        Room room = firstSlot.getRoom();

        String coursesList = conflictingSlots.stream()
            .map(s -> s.getCourse() != null ? s.getCourse().getCourseCode() : "Unknown")
            .collect(Collectors.joining(", "));

        int totalStudents = conflictingSlots.stream()
            .mapToInt(s -> s.getStudents() != null ? s.getStudents().size() : 0)
            .sum();

        return ConflictDetail.builder()
            .slotId(firstSlot.getId())
            .courseId(firstSlot.getCourse() != null ? firstSlot.getCourse().getId() : null)
            .courseName(coursesList)
            .type(ConflictType.ROOM_DOUBLE_BOOKING)
            .severity(ConflictSeverity.CRITICAL)
            .description("Room " + room.getRoomNumber() + " is assigned to " +
                        conflictingSlots.size() + " classes at the same time")
            .violatedConstraint("Room can only host one class at a time")
            .studentsAffected(totalStudents)
            .blocking(true)
            .estimatedFixTimeMinutes(5)
            .possibleSolutions(List.of(
                "Move one course to a different room",
                "Move one course to a different time slot",
                "Add more rooms to the system"
            ))
            .relatedEntityIds(conflictingSlots.stream()
                .map(ScheduleSlot::getId)
                .collect(Collectors.toList()))
            .build();
    }

    /**
     * Calculate completion percentage for a solution
     *
     * @param solution Scheduling solution
     * @return Percentage of slots fully assigned (0-100)
     */
    public double calculateCompletionPercentage(SchedulingSolution solution) {
        if (solution == null || solution.getScheduleSlots() == null || solution.getScheduleSlots().isEmpty()) {
            return 0.0;
        }

        long assignedCount = solution.getScheduleSlots().stream()
            .filter(this::isSlotFullyAssigned)
            .count();

        return (assignedCount * 100.0) / solution.getScheduleSlots().size();
    }
}
