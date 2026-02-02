package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.AssignmentDuration;
import com.heronix.scheduler.model.enums.AssignmentStatus;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.data.SISDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating substitute schedules from assignments
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Service
@Transactional
public class SubstituteScheduleGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(SubstituteScheduleGeneratorService.class);

    @Autowired
    private SubstituteAssignmentRepository assignmentRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private SISDataService sisDataService;

    // ARCHITECTURAL NOTE: This service creates Teacher records (substitute teachers)
    // This violates microservice boundaries. Consider:
    // 1. Creating scheduler-specific SubstituteProfile entity
    // 2. Implementing SIS write API for substitute teacher management
    // See: SESSION_3_FINAL_REPORT.md - Architectural Challenges

    /**
     * Generate schedules for all assignments on a specific date
     */
    public GenerationResult generateSchedulesForDate(LocalDate date) {
        logger.info("Generating schedules for date: {}", date);

        GenerationResult result = new GenerationResult();
        result.setDate(date);

        // Get all confirmed assignments for this date
        List<SubstituteAssignment> assignments = assignmentRepository.findByAssignmentDate(date).stream()
                .filter(a -> a.getStatus() == AssignmentStatus.CONFIRMED ||
                           a.getStatus() == AssignmentStatus.IN_PROGRESS)
                .collect(Collectors.toList());

        result.setTotalAssignments(assignments.size());
        logger.info("Found {} assignments for date {}", assignments.size(), date);

        // Process each assignment
        for (SubstituteAssignment assignment : assignments) {
            try {
                generateScheduleForAssignment(assignment, result);
            } catch (Exception e) {
                logger.error("Error generating schedule for assignment {}: {}",
                        assignment.getId(), e.getMessage(), e);
                result.addError(assignment, e.getMessage());
            }
        }

        logger.info("Schedule generation completed: {} generated, {} skipped, {} errors",
                result.getGeneratedCount(), result.getSkippedCount(), result.getErrorCount());

        return result;
    }

    /**
     * Generate schedules for a date range
     */
    public List<GenerationResult> generateSchedulesForDateRange(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating schedules from {} to {}", startDate, endDate);

        List<GenerationResult> results = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            // Skip weekends (unless there are assignments)
            if (current.getDayOfWeek() != DayOfWeek.SATURDAY &&
                current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                GenerationResult result = generateSchedulesForDate(current);
                results.add(result);
            }
            current = current.plusDays(1);
        }

        return results;
    }

    /**
     * Generate schedule for a single assignment
     */
    private void generateScheduleForAssignment(SubstituteAssignment assignment, GenerationResult result) {
        // Check if schedules already exist
        if (!assignment.getScheduleSlots().isEmpty()) {
            logger.debug("Assignment {} already has schedule slots, skipping", assignment.getId());
            result.incrementSkipped();
            return;
        }

        // Get the teacher being replaced
        Teacher replacedTeacher = assignment.getReplacedTeacher();
        if (replacedTeacher == null) {
            logger.warn("Assignment {} has no replaced teacher, cannot generate schedule", assignment.getId());
            result.addError(assignment, "No teacher to replace");
            return;
        }

        // Get the teacher's schedule for this day
        DayOfWeek dayOfWeek = assignment.getAssignmentDate().getDayOfWeek();
        List<ScheduleSlot> teacherSlots = scheduleSlotRepository.findTeacherScheduleByDay(
                replacedTeacher.getId(), dayOfWeek);

        if (teacherSlots.isEmpty()) {
            logger.warn("Teacher {} has no schedule for {}, creating placeholder slots",
                    replacedTeacher.getName(), dayOfWeek);
            createPlaceholderSlots(assignment, result);
            return;
        }

        // Filter slots by time range
        List<ScheduleSlot> slotsToReplace = filterSlotsByTimeRange(
                teacherSlots,
                assignment.getStartTime(),
                assignment.getEndTime());

        if (slotsToReplace.isEmpty()) {
            logger.warn("No schedule slots found in time range {} - {} for assignment {}",
                    assignment.getStartTime(), assignment.getEndTime(), assignment.getId());
            createPlaceholderSlots(assignment, result);
            return;
        }

        // Create substitute slots
        List<ScheduleSlot> substituteSlots = new ArrayList<>();
        for (ScheduleSlot originalSlot : slotsToReplace) {
            ScheduleSlot substituteSlot = createSubstituteSlot(originalSlot, assignment);
            scheduleSlotRepository.save(substituteSlot);
            substituteSlots.add(substituteSlot);

            logger.debug("Created substitute slot for period {}, course {}",
                    originalSlot.getTimeSlot() != null ? originalSlot.getTimeSlot().getPeriodNumber() : "?",
                    originalSlot.getCourse() != null ? originalSlot.getCourse().getCourseName() : "?");
        }

        // Link slots to assignment
        assignment.setScheduleSlots(substituteSlots);
        assignmentRepository.save(assignment);

        result.incrementGenerated();
        result.addGeneratedSlots(assignment, substituteSlots);

        logger.info("Generated {} schedule slots for assignment {}", substituteSlots.size(), assignment.getId());
    }

    /**
     * Filter schedule slots by time range
     */
    private List<ScheduleSlot> filterSlotsByTimeRange(List<ScheduleSlot> slots,
                                                      LocalTime startTime,
                                                      LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return slots; // Return all if no time constraint
        }

        return slots.stream()
                .filter(slot -> {
                    if (slot.getTimeSlot() == null) return false;

                    LocalTime slotStart = slot.getStartTime();
                    LocalTime slotEnd = slot.getEndTime();

                    if (slotStart == null || slotEnd == null) return false;

                    // Check if slot overlaps with assignment time range
                    return !slotEnd.isBefore(startTime) && !slotStart.isAfter(endTime);
                })
                .collect(Collectors.toList());
    }

    /**
     * Create a substitute schedule slot from an original slot
     */
    private ScheduleSlot createSubstituteSlot(ScheduleSlot originalSlot, SubstituteAssignment assignment) {
        ScheduleSlot substituteSlot = new ScheduleSlot();

        // Copy basic info from original
        substituteSlot.setDayOfWeek(originalSlot.getDayOfWeek());
        substituteSlot.setTimeSlot(originalSlot.getTimeSlot());
        substituteSlot.setStartTime(originalSlot.getStartTime());
        substituteSlot.setEndTime(originalSlot.getEndTime());

        // Use substitute's info as Teacher entity
        // Note: The substitute is stored in SubstituteAssignment,
        // but we need a Teacher entity for the schedule slot
        // This is a design decision - we'll create a temporary reference
        substituteSlot.setSubstituteTeacher(convertSubstituteToTeacher(assignment.getSubstitute()));

        // Set original teacher
        substituteSlot.setTeacher(originalSlot.getTeacher());

        // Copy course and room
        substituteSlot.setCourse(originalSlot.getCourse());
        substituteSlot.setRoom(originalSlot.getRoom());

        // Set status
        substituteSlot.setStatus(com.heronix.scheduler.model.enums.SlotStatus.SUBSTITUTE_ASSIGNED);

        // Add notes
        String notes = "Substitute assignment for " + originalSlot.getTeacher().getName();
        if (assignment.getAbsenceReason() != null) {
            notes += " (Reason: " + assignment.getAbsenceReason().getDisplayName() + ")";
        }
        substituteSlot.setNotes(notes);

        return substituteSlot;
    }

    /**
     * Create placeholder slots when teacher schedule not found
     */
    private void createPlaceholderSlots(SubstituteAssignment assignment, GenerationResult result) {
        logger.info("Creating placeholder slots for assignment {}", assignment.getId());

        DayOfWeek dayOfWeek = assignment.getAssignmentDate().getDayOfWeek();

        // Since TimeSlot is not a JPA entity with repository, we'll create a generic slot
        ScheduleSlot genericSlot = createGenericSubstituteSlot(assignment);
        scheduleSlotRepository.save(genericSlot);

        assignment.getScheduleSlots().add(genericSlot);
        assignmentRepository.save(assignment);

        result.incrementGenerated();
        result.addGeneratedSlots(assignment, List.of(genericSlot));

        logger.info("Created generic placeholder slot for assignment {}", assignment.getId());
    }

    /**
     * Create a generic substitute slot when no time slots available
     */
    private ScheduleSlot createGenericSubstituteSlot(SubstituteAssignment assignment) {
        ScheduleSlot slot = new ScheduleSlot();

        slot.setDayOfWeek(assignment.getAssignmentDate().getDayOfWeek());
        slot.setStartTime(assignment.getStartTime());
        slot.setEndTime(assignment.getEndTime());

        slot.setSubstituteTeacher(convertSubstituteToTeacher(assignment.getSubstitute()));
        slot.setTeacher(assignment.getReplacedTeacher());
        slot.setCourse(assignment.getCourse());
        slot.setRoom(assignment.getRoom());
        slot.setStatus(com.heronix.scheduler.model.enums.SlotStatus.SUBSTITUTE_ASSIGNED);
        slot.setNotes("Generic substitute assignment slot");

        return slot;
    }

    /**
     * Convert Substitute to Teacher entity for schedule slot
     * This is a workaround since ScheduleSlot expects Teacher entity
     */
    private Teacher convertSubstituteToTeacher(Substitute substitute) {
        if (substitute == null) return null;

        // Check if this substitute already has a Teacher entity in SIS
        Optional<Teacher> existingTeacher = sisDataService.getAllTeachers().stream()
                .filter(t -> t.getEmployeeId() != null &&
                           t.getEmployeeId().equals(substitute.getEmployeeId()))
                .findFirst();

        if (existingTeacher.isPresent()) {
            return existingTeacher.get();
        }

        // Note: Cannot create Teacher entities in SchedulerV2
        // Substitutes must be registered as Teachers in the SIS first
        // For now, return null and log a warning

        System.err.println("WARNING: Substitute " + substitute.getFirstName() + " " +
                          substitute.getLastName() + " (ID: " + substitute.getEmployeeId() +
                          ") is not registered as a Teacher in SIS. " +
                          "Please register substitute teachers in SIS before assigning schedules.");

        // SIS API call to register substitute as teacher not available — requires SIS-side registration
        // sisApiClient.registerSubstituteAsTeacher(substitute);

        return null;
    }

    /**
     * Check for schedule conflicts
     */
    public List<ScheduleConflict> checkConflicts(SubstituteAssignment assignment) {
        List<ScheduleConflict> conflicts = new ArrayList<>();

        // Get substitute's other assignments on same date
        List<SubstituteAssignment> sameDay = assignmentRepository
                .findBySubstituteAndAssignmentDate(
                        assignment.getSubstitute(),
                        assignment.getAssignmentDate());

        for (SubstituteAssignment other : sameDay) {
            if (other.getId().equals(assignment.getId())) continue;

            // Check time overlap
            if (timesOverlap(assignment.getStartTime(), assignment.getEndTime(),
                           other.getStartTime(), other.getEndTime())) {
                ScheduleConflict conflict = new ScheduleConflict();
                conflict.setAssignment1(assignment);
                conflict.setAssignment2(other);
                conflict.setConflictType("TIME_OVERLAP");
                conflict.setDescription(String.format(
                        "Substitute %s has overlapping assignments: %s-%s and %s-%s",
                        assignment.getSubstitute().getFullName(),
                        assignment.getStartTime(), assignment.getEndTime(),
                        other.getStartTime(), other.getEndTime()));
                conflicts.add(conflict);
            }
        }

        return conflicts;
    }

    /**
     * Check if two time ranges overlap
     */
    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return !end1.isBefore(start2) && !start1.isAfter(end2);
    }

    // ==================== RESULT CLASSES ====================

    /**
     * Result of schedule generation
     */
    public static class GenerationResult {
        private LocalDate date;
        private int totalAssignments;
        private int generatedCount;
        private int skippedCount;
        private List<GenerationError> errors = new ArrayList<>();
        private Map<SubstituteAssignment, List<ScheduleSlot>> generatedSlots = new HashMap<>();

        public LocalDate getDate() {
            return date;
        }

        public void setDate(LocalDate date) {
            this.date = date;
        }

        public int getTotalAssignments() {
            return totalAssignments;
        }

        public void setTotalAssignments(int totalAssignments) {
            this.totalAssignments = totalAssignments;
        }

        public int getGeneratedCount() {
            return generatedCount;
        }

        public void incrementGenerated() {
            this.generatedCount++;
        }

        public int getSkippedCount() {
            return skippedCount;
        }

        public void incrementSkipped() {
            this.skippedCount++;
        }

        public List<GenerationError> getErrors() {
            return errors;
        }

        public void addError(SubstituteAssignment assignment, String message) {
            errors.add(new GenerationError(assignment, message));
        }

        public int getErrorCount() {
            return errors.size();
        }

        public Map<SubstituteAssignment, List<ScheduleSlot>> getGeneratedSlots() {
            return generatedSlots;
        }

        public void addGeneratedSlots(SubstituteAssignment assignment, List<ScheduleSlot> slots) {
            generatedSlots.put(assignment, slots);
        }

        public String getSummary() {
            return String.format("Date: %s | Total: %d | Generated: %d | Skipped: %d | Errors: %d",
                    date, totalAssignments, generatedCount, skippedCount, getErrorCount());
        }
    }

    /**
     * Generation error details
     */
    public static class GenerationError {
        private SubstituteAssignment assignment;
        private String message;

        public GenerationError(SubstituteAssignment assignment, String message) {
            this.assignment = assignment;
            this.message = message;
        }

        public SubstituteAssignment getAssignment() {
            return assignment;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "Assignment " + assignment.getId() + ": " + message;
        }
    }

    /**
     * Schedule conflict details
     */
    public static class ScheduleConflict {
        private SubstituteAssignment assignment1;
        private SubstituteAssignment assignment2;
        private String conflictType;
        private String description;

        public SubstituteAssignment getAssignment1() {
            return assignment1;
        }

        public void setAssignment1(SubstituteAssignment assignment1) {
            this.assignment1 = assignment1;
        }

        public SubstituteAssignment getAssignment2() {
            return assignment2;
        }

        public void setAssignment2(SubstituteAssignment assignment2) {
            this.assignment2 = assignment2;
        }

        public String getConflictType() {
            return conflictType;
        }

        public void setConflictType(String conflictType) {
            this.conflictType = conflictType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            return conflictType + ": " + description;
        }
    }
}
