package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.ConflictType;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.ConflictDetectorService;
import com.heronix.scheduler.service.ConflictResolverService;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Conflict Resolver Service Implementation
 * Provides intelligent conflict resolution strategies
 *
 * Location: src/main/java/com/eduscheduler/service/impl/ConflictResolverServiceImpl.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7B - Conflict Resolution
 */
@Slf4j
@Service
@Transactional
public class ConflictResolverServiceImpl implements ConflictResolverService {

    @Autowired
    private ConflictRepository conflictRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private ConflictDetectorService conflictDetectorService;

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    @Autowired(required = false)
    private com.heronix.scheduler.service.ConflictResolutionSuggestionService conflictResolutionSuggestionService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ========================================================================
    // RESOLUTION SUGGESTIONS
    // ========================================================================

    @Override
    public List<ResolutionSuggestion> getSuggestions(Conflict conflict) {
        // ✅ NULL SAFE: Validate conflict parameter
        if (conflict == null || conflict.getConflictType() == null) {
            if (conflict == null) {
                log.warn("Cannot generate suggestions for null conflict");
            } else {
                log.warn("Conflict {} has no type specified", conflict.getId());
            }
            return new ArrayList<>();
        }

        // ✅ NULL SAFE: Safe extraction of conflict ID for logging
        String conflictIdStr = (conflict.getId() != null) ? conflict.getId().toString() : "Unknown";
        log.debug("Generating resolution suggestions for conflict: {}", conflictIdStr);
        List<ResolutionSuggestion> suggestions = new ArrayList<>();

        switch (conflict.getConflictType()) {
            case TEACHER_OVERLOAD:
                suggestions.addAll(suggestTeacherOverloadResolutions(conflict));
                break;

            case ROOM_DOUBLE_BOOKING:
                suggestions.addAll(suggestRoomConflictResolutions(conflict));
                break;

            case ROOM_CAPACITY_EXCEEDED:
                suggestions.addAll(suggestRoomCapacityResolutions(conflict));
                break;

            case TIME_OVERLAP:
                suggestions.addAll(suggestTimeOverlapResolutions(conflict));
                break;

            default:
                // Generic suggestions for other conflict types
                suggestions.addAll(suggestGenericResolutions(conflict));
                break;
        }

        // Sort by confidence (highest first)
        suggestions.sort((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()));

        log.debug("Generated {} suggestions", suggestions.size());
        return suggestions;
    }

    @Override
    public ResolutionSuggestion getBestSuggestion(Conflict conflict) {
        List<ResolutionSuggestion> suggestions = getSuggestions(conflict);
        return suggestions.isEmpty() ? null : suggestions.get(0);
    }

    @Override
    public boolean applyResolution(Conflict conflict, ResolutionSuggestion suggestion, User user) {
        log.info("Applying resolution suggestion for conflict {}", conflict.getId());

        if (suggestion == null || suggestion.getAction() == null) {
            log.warn("No action specified in suggestion");
            return false;
        }

        ResolutionAction action = suggestion.getAction();
        boolean success = false;

        try {
            switch (action.getActionType()) {
                case "MOVE_TIME":
                    success = moveSlot(action.getTargetSlot(), action.getNewTime(), user);
                    break;

                case "CHANGE_ROOM":
                    success = changeRoom(action.getTargetSlot(), action.getNewRoom(), user);
                    break;

                case "CHANGE_TEACHER":
                    success = changeTeacher(action.getTargetSlot(), action.getNewTeacher(), user);
                    break;

                case "SWAP":
                    success = swapSlots(action.getTargetSlot(), action.getSwapWithSlot(), user);
                    break;

                case "DELETE":
                    success = deleteSlot(action.getTargetSlot(), user, "Conflict resolution");
                    break;

                default:
                    log.warn("Unknown action type: {}", action.getActionType());
                    return false;
            }

            if (success) {
                markResolved(conflict, user, "Applied suggestion: " + suggestion.getStrategy());
                log.info("Successfully applied resolution");
            }

            return success;

        } catch (Exception e) {
            log.error("Error applying resolution", e);
            return false;
        }
    }

    // ========================================================================
    // AUTOMATIC RESOLUTION
    // ========================================================================

    @Override
    public boolean autoResolve(Conflict conflict, User user) {
        log.info("Attempting auto-resolution for conflict {}", conflict.getId());

        ResolutionSuggestion best = getBestSuggestion(conflict);

        if (best == null) {
            log.info("No suggestions available for auto-resolution");
            return false;
        }

        // Only auto-apply if confidence is high enough
        if (best.getConfidence() < 0.7) {
            log.info("Confidence too low for auto-resolution: {}", best.getConfidence());
            return false;
        }

        return applyResolution(conflict, best, user);
    }

    @Override
    public int autoResolveAll(Schedule schedule, User user) {
        // ✅ NULL SAFE: Validate schedule parameter
        if (schedule == null) {
            log.warn("Cannot auto-resolve conflicts for null schedule");
            return 0;
        }

        // ✅ NULL SAFE: Safe extraction of schedule name for logging
        String scheduleName = (schedule.getScheduleName() != null) ?
            schedule.getScheduleName() : "Unknown";
        log.info("Attempting auto-resolution for all conflicts in schedule: {}", scheduleName);

        List<Conflict> activeConflicts = conflictRepository.findActiveBySchedule(schedule);
        int resolvedCount = 0;

        for (Conflict conflict : activeConflicts) {
            // ✅ NULL SAFE: Skip null conflicts
            if (conflict == null) continue;

            if (autoResolve(conflict, user)) {
                resolvedCount++;
            }
        }

        log.info("Auto-resolved {} out of {} conflicts", resolvedCount, activeConflicts.size());
        return resolvedCount;
    }

    @Override
    public int autoResolveByType(Schedule schedule, String conflictTypeName, User user) {
        log.info("Attempting auto-resolution for {} conflicts", conflictTypeName);

        ConflictType type = ConflictType.valueOf(conflictTypeName);
        List<Conflict> conflicts = conflictRepository.findByScheduleAndConflictType(schedule, type);
        int resolvedCount = 0;

        for (Conflict conflict : conflicts) {
            // ✅ NULL SAFE: Skip null conflicts and check active status safely
            if (conflict == null || !conflict.isActive()) continue;

            if (autoResolve(conflict, user)) {
                resolvedCount++;
            }
        }

        log.info("Auto-resolved {} conflicts of type {}", resolvedCount, conflictTypeName);
        return resolvedCount;
    }

    // ========================================================================
    // MANUAL RESOLUTION TOOLS
    // ========================================================================

    @Override
    public List<TimeSlotOption> findAlternativeTimeSlots(ScheduleSlot slot) {
        // ✅ NULL SAFE: Validate slot parameter
        if (slot == null) {
            log.warn("Cannot find alternative time slots for null slot");
            return new ArrayList<>();
        }

        // ✅ NULL SAFE: Safe extraction of slot ID for logging
        String slotIdStr = (slot.getId() != null) ? slot.getId().toString() : "Unknown";
        log.debug("Finding alternative time slots for slot {}", slotIdStr);
        List<TimeSlotOption> options = new ArrayList<>();

        // ✅ NULL SAFE: Check schedule exists
        if (slot.getSchedule() == null || slot.getSchedule().getId() == null) {
            return options;
        }

        // Get all slots in the schedule
        // ✅ NULL SAFE: Filter null slots and check schedule before accessing ID
        List<ScheduleSlot> allSlots = scheduleSlotRepository.findAll().stream()
            .filter(s -> s != null && s.getSchedule() != null &&
                        s.getSchedule().getId() != null &&
                        s.getSchedule().getId().equals(slot.getSchedule().getId()))
            .collect(Collectors.toList());

        // Generate time slot options for each day of week
        for (DayOfWeek day : DayOfWeek.values()) {
            // Generate typical school day time slots (8:00 AM to 3:00 PM)
            LocalTime currentTime = LocalTime.of(8, 0);
            LocalTime endOfDay = LocalTime.of(15, 0);

            while (currentTime.isBefore(endOfDay)) {
                LocalTime slotEnd = currentTime.plusMinutes(50); // 50-minute periods

                TimeSlotOption option = new TimeSlotOption(
                    day.toString(),
                    currentTime.format(TIME_FORMATTER),
                    slotEnd.format(TIME_FORMATTER)
                );

                // Check if this time is available for the teacher and room
                boolean available = isTimeSlotAvailable(slot, day, currentTime, slotEnd, allSlots);
                option.setAvailable(available);

                if (!available) {
                    option.setAvailabilityReason(getUnavailabilityReason(slot, day, currentTime, slotEnd, allSlots));
                }

                options.add(option);

                currentTime = slotEnd.plusMinutes(10); // 10-minute passing period
            }
        }

        return options.stream()
            .filter(TimeSlotOption::isAvailable)
            .collect(Collectors.toList());
    }

    @Override
    public List<Room> findAlternativeRooms(ScheduleSlot slot) {
        // ✅ NULL SAFE: Validate slot parameter
        if (slot == null) {
            log.warn("Cannot find alternative rooms for null slot");
            return new ArrayList<>();
        }

        // ✅ NULL SAFE: Safe extraction of slot ID for logging
        String slotIdStr = (slot.getId() != null) ? slot.getId().toString() : "Unknown";
        log.debug("Finding alternative rooms for slot {}", slotIdStr);

        if (slot.getCourse() == null) {
            return new ArrayList<>();
        }

        // Get all rooms
        List<Room> allRooms = roomRepository.findAll();

        // ✅ NULL SAFE: Filter to rooms with sufficient capacity and matching room type
        return allRooms.stream()
            .filter(room -> room != null)
            .filter(room -> isRoomAvailableForSlot(slot, room))
            .filter(room -> room.getCapacity() != null && room.getCapacity() >= getRequiredCapacity(slot))
            .filter(room -> isRoomTypeCompatible(slot.getCourse(), room))
            .collect(Collectors.toList());
    }

    @Override
    public List<Teacher> findAlternativeTeachers(ScheduleSlot slot) {
        // ✅ NULL SAFE: Validate slot parameter
        if (slot == null) {
            log.warn("Cannot find alternative teachers for null slot");
            return new ArrayList<>();
        }

        // ✅ NULL SAFE: Safe extraction of slot ID for logging
        String slotIdStr = (slot.getId() != null) ? slot.getId().toString() : "Unknown";
        log.debug("Finding alternative teachers for slot {}", slotIdStr);

        if (slot.getCourse() == null) {
            return new ArrayList<>();
        }

        // Get all active teachers
        // ✅ NULL SAFE: Filter null teachers before checking active status
        List<Teacher> allTeachers = sisDataService.getAllTeachers().stream()
            .filter(t -> t != null && Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());

        // Filter to teachers available at this time and qualified for the subject
        return allTeachers.stream()
            .filter(teacher -> isTeacherAvailableForSlot(slot, teacher))
            .filter(teacher -> isTeacherQualifiedForCourse(teacher, slot.getCourse()))
            .collect(Collectors.toList());
    }

    @Override
    public List<SlotSwapSuggestion> suggestSlotSwaps(Conflict conflict) {
        // ✅ NULL SAFE: Validate conflict parameter
        if (conflict == null) {
            log.warn("Cannot suggest slot swaps for null conflict");
            return new ArrayList<>();
        }

        // ✅ NULL SAFE: Safe extraction of conflict ID for logging
        String conflictIdStr = (conflict.getId() != null) ? conflict.getId().toString() : "Unknown";
        log.debug("Suggesting slot swaps for conflict {}", conflictIdStr);
        List<SlotSwapSuggestion> suggestions = new ArrayList<>();

        // ✅ NULL SAFE: Check affected slots exist and have at least 2 entries
        if (conflict.getAffectedSlots() == null || conflict.getAffectedSlots().size() < 2) {
            return suggestions;
        }

        // For now, suggest swapping the two affected slots
        ScheduleSlot slot1 = conflict.getAffectedSlots().get(0);
        ScheduleSlot slot2 = conflict.getAffectedSlots().get(1);

        // ✅ NULL SAFE: Skip if either slot is null
        if (slot1 == null || slot2 == null) {
            return suggestions;
        }

        SlotSwapSuggestion swap = new SlotSwapSuggestion(
            slot1, slot2,
            "Swap these two slots to resolve the conflict"
        );
        swap.setBenefit(0.8);
        suggestions.add(swap);

        return suggestions;
    }

    // ========================================================================
    // CONFLICT RESOLUTION ACTIONS
    // ========================================================================

    @Override
    public boolean moveSlot(ScheduleSlot slot, TimeSlotOption newTime, User user) {
        log.info("Moving slot {} to new time: {}", slot.getId(), newTime);

        try {
            // Parse new time
            DayOfWeek newDay = DayOfWeek.valueOf(newTime.getDayOfWeek());
            LocalTime startTime = LocalTime.parse(newTime.getStartTime(), TIME_FORMATTER);
            LocalTime endTime = LocalTime.parse(newTime.getEndTime(), TIME_FORMATTER);

            // Update slot
            slot.setDayOfWeek(newDay);
            slot.setStartTime(startTime);
            slot.setEndTime(endTime);

            scheduleSlotRepository.save(slot);
            log.info("Slot moved successfully");
            return true;

        } catch (Exception e) {
            log.error("Error moving slot", e);
            return false;
        }
    }

    @Override
    public boolean changeRoom(ScheduleSlot slot, Room newRoom, User user) {
        log.info("Changing room for slot {} to {}", slot.getId(), newRoom.getRoomNumber());

        try {
            slot.setRoom(newRoom);
            scheduleSlotRepository.save(slot);
            log.info("Room changed successfully");
            return true;

        } catch (Exception e) {
            log.error("Error changing room", e);
            return false;
        }
    }

    @Override
    public boolean changeTeacher(ScheduleSlot slot, Teacher newTeacher, User user) {
        log.info("Changing teacher for slot {} to {}", slot.getId(), newTeacher.getName());

        try {
            slot.setTeacher(newTeacher);
            scheduleSlotRepository.save(slot);
            log.info("Teacher changed successfully");
            return true;

        } catch (Exception e) {
            log.error("Error changing teacher", e);
            return false;
        }
    }

    @Override
    public boolean swapSlots(ScheduleSlot slot1, ScheduleSlot slot2, User user) {
        log.info("Swapping slots {} and {}", slot1.getId(), slot2.getId());

        try {
            // Save original values
            DayOfWeek day1 = slot1.getDayOfWeek();
            LocalTime start1 = slot1.getStartTime();
            LocalTime end1 = slot1.getEndTime();
            Room room1 = slot1.getRoom();

            // Swap
            slot1.setDayOfWeek(slot2.getDayOfWeek());
            slot1.setStartTime(slot2.getStartTime());
            slot1.setEndTime(slot2.getEndTime());
            slot1.setRoom(slot2.getRoom());

            slot2.setDayOfWeek(day1);
            slot2.setStartTime(start1);
            slot2.setEndTime(end1);
            slot2.setRoom(room1);

            scheduleSlotRepository.save(slot1);
            scheduleSlotRepository.save(slot2);

            log.info("Slots swapped successfully");
            return true;

        } catch (Exception e) {
            log.error("Error swapping slots", e);
            return false;
        }
    }

    @Override
    public boolean deleteSlot(ScheduleSlot slot, User user, String reason) {
        log.info("Deleting slot {} - Reason: {}", slot.getId(), reason);

        try {
            scheduleSlotRepository.delete(slot);
            log.info("Slot deleted successfully");
            return true;

        } catch (Exception e) {
            log.error("Error deleting slot", e);
            return false;
        }
    }

    // ========================================================================
    // CONFLICT MARKING
    // ========================================================================

    @Override
    public void markResolved(Conflict conflict, User user, String notes) {
        log.info("Marking conflict {} as resolved", conflict.getId());
        conflict.resolve(user, notes);
        conflictRepository.save(conflict);
    }

    @Override
    public void markIgnored(Conflict conflict, String reason) {
        log.info("Marking conflict {} as ignored", conflict.getId());
        conflict.ignore(reason);
        conflictRepository.save(conflict);
    }

    @Override
    public void unignore(Conflict conflict) {
        log.info("Unignoring conflict {}", conflict.getId());
        conflict.unignore();
        conflictRepository.save(conflict);
    }

    // ========================================================================
    // RESOLUTION VALIDATION
    // ========================================================================

    @Override
    public List<Conflict> validateResolution(ScheduleSlot slot, TimeSlotOption newTime,
                                            Room newRoom, Teacher newTeacher) {
        log.debug("Validating resolution for slot {}", slot.getId());

        // Create a temporary copy of the slot with proposed changes
        ScheduleSlot tempSlot = new ScheduleSlot();
        tempSlot.setId(slot.getId());
        tempSlot.setSchedule(slot.getSchedule());
        tempSlot.setCourse(slot.getCourse());

        // Apply proposed changes
        if (newTime != null) {
            tempSlot.setDayOfWeek(DayOfWeek.valueOf(newTime.getDayOfWeek()));
            tempSlot.setStartTime(LocalTime.parse(newTime.getStartTime(), TIME_FORMATTER));
            tempSlot.setEndTime(LocalTime.parse(newTime.getEndTime(), TIME_FORMATTER));
        } else {
            tempSlot.setDayOfWeek(slot.getDayOfWeek());
            tempSlot.setStartTime(slot.getStartTime());
            tempSlot.setEndTime(slot.getEndTime());
        }

        tempSlot.setRoom(newRoom != null ? newRoom : slot.getRoom());
        tempSlot.setTeacher(newTeacher != null ? newTeacher : slot.getTeacher());

        // Detect potential conflicts
        return conflictDetectorService.detectConflictsForSlot(tempSlot);
    }

    @Override
    public ResolutionImpact analyzeImpact(Conflict conflict, ResolutionSuggestion suggestion) {
        log.debug("Analyzing impact for resolution suggestion");

        ResolutionImpact impact = new ResolutionImpact();

        if (suggestion.getAction() == null) {
            return impact;
        }

        ResolutionAction action = suggestion.getAction();

        // Count affected entities based on action type
        if (action.getTargetSlot() != null) {
            impact.setSlotsAffected(1);

            if (action.getTargetSlot().getTeacher() != null) {
                impact.setTeachersAffected(1);
            }

            if (action.getTargetSlot().getRoom() != null) {
                impact.setRoomsAffected(1);
            }

            // Count students from course section enrollment
            if (action.getTargetSlot().getCourse() != null) {
                List<CourseSection> sections = courseSectionRepository.findByCourse(action.getTargetSlot().getCourse());
                int studentCount = sections.stream()
                    .filter(s -> s.getCurrentEnrollment() != null)
                    .mapToInt(CourseSection::getCurrentEnrollment)
                    .sum();
                // Student count available for reporting: studentCount
                log.debug("Resolution affects approximately {} students", studentCount);
            }
        }

        // Check for potential new conflicts
        if ("MOVE_TIME".equals(action.getActionType()) && action.getNewTime() != null) {
            List<Conflict> newConflicts = validateResolution(
                action.getTargetSlot(),
                action.getNewTime(),
                null,
                null
            );
            impact.getPotentialNewConflicts().addAll(newConflicts);
        }

        impact.getResolvedConflicts().add(conflict);

        impact.setSummary(String.format(
            "Affects %d slot(s), %d teacher(s), %d room(s). " +
            "Resolves %d conflict(s). May create %d new conflict(s).",
            impact.getSlotsAffected(),
            impact.getTeachersAffected(),
            impact.getRoomsAffected(),
            impact.getResolvedConflicts().size(),
            impact.getPotentialNewConflicts().size()
        ));

        return impact;
    }

    // ========================================================================
    // SUGGESTION GENERATORS
    // ========================================================================

    private List<ResolutionSuggestion> suggestTeacherOverloadResolutions(Conflict conflict) {
        List<ResolutionSuggestion> suggestions = new ArrayList<>();

        // ✅ NULL SAFE: Check affected slots exist and are not empty
        if (conflict.getAffectedSlots() == null || conflict.getAffectedSlots().isEmpty()) {
            return suggestions;
        }

        ScheduleSlot slot = conflict.getAffectedSlots().get(0);

        // ✅ NULL SAFE: Skip if slot is null
        if (slot == null) {
            return suggestions;
        }

        // Suggestion 1: Find alternative time slots
        List<TimeSlotOption> altTimes = findAlternativeTimeSlots(slot);
        if (!altTimes.isEmpty()) {
            ResolutionAction action = new ResolutionAction();
            action.setActionType("MOVE_TIME");
            action.setTargetSlot(slot);
            action.setNewTime(altTimes.get(0));

            suggestions.add(new ResolutionSuggestion(
                "Move to Different Time",
                "Move one class to " + altTimes.get(0),
                0.8,
                action
            ));
        }

        // Suggestion 2: Find alternative teachers
        List<Teacher> altTeachers = findAlternativeTeachers(slot);
        if (!altTeachers.isEmpty()) {
            ResolutionAction action = new ResolutionAction();
            action.setActionType("CHANGE_TEACHER");
            action.setTargetSlot(slot);
            action.setNewTeacher(altTeachers.get(0));

            suggestions.add(new ResolutionSuggestion(
                "Assign Different Teacher",
                "Assign teacher " + altTeachers.get(0).getName(),
                0.7,
                action
            ));
        }

        return suggestions;
    }

    private List<ResolutionSuggestion> suggestRoomConflictResolutions(Conflict conflict) {
        List<ResolutionSuggestion> suggestions = new ArrayList<>();

        // ✅ NULL SAFE: Check affected slots exist and are not empty
        if (conflict.getAffectedSlots() == null || conflict.getAffectedSlots().isEmpty()) {
            return suggestions;
        }

        ScheduleSlot slot = conflict.getAffectedSlots().get(0);

        // ✅ NULL SAFE: Skip if slot is null
        if (slot == null) {
            return suggestions;
        }

        // Suggestion 1: Find alternative rooms
        List<Room> altRooms = findAlternativeRooms(slot);
        if (!altRooms.isEmpty()) {
            ResolutionAction action = new ResolutionAction();
            action.setActionType("CHANGE_ROOM");
            action.setTargetSlot(slot);
            action.setNewRoom(altRooms.get(0));

            suggestions.add(new ResolutionSuggestion(
                "Assign Different Room",
                "Move to room " + altRooms.get(0).getRoomNumber(),
                0.9,
                action
            ));
        }

        // Suggestion 2: Find alternative time slots
        List<TimeSlotOption> altTimes = findAlternativeTimeSlots(slot);
        if (!altTimes.isEmpty()) {
            ResolutionAction action = new ResolutionAction();
            action.setActionType("MOVE_TIME");
            action.setTargetSlot(slot);
            action.setNewTime(altTimes.get(0));

            suggestions.add(new ResolutionSuggestion(
                "Move to Different Time",
                "Move to " + altTimes.get(0),
                0.7,
                action
            ));
        }

        return suggestions;
    }

    private List<ResolutionSuggestion> suggestRoomCapacityResolutions(Conflict conflict) {
        List<ResolutionSuggestion> suggestions = new ArrayList<>();

        // ✅ NULL SAFE: Check affected slots exist and are not empty
        if (conflict.getAffectedSlots() == null || conflict.getAffectedSlots().isEmpty()) {
            return suggestions;
        }

        ScheduleSlot slot = conflict.getAffectedSlots().get(0);

        // ✅ NULL SAFE: Skip if slot is null
        if (slot == null) {
            return suggestions;
        }

        // Find larger rooms
        // ✅ NULL SAFE: Safe extraction of current room capacity
        int currentCapacity = (slot.getRoom() != null && slot.getRoom().getCapacity() != null) ?
            slot.getRoom().getCapacity() : 0;

        List<Room> largerRooms = findAlternativeRooms(slot).stream()
            .filter(room -> room != null && room.getCapacity() != null &&
                           room.getCapacity() > currentCapacity)
            .collect(Collectors.toList());

        if (!largerRooms.isEmpty()) {
            ResolutionAction action = new ResolutionAction();
            action.setActionType("CHANGE_ROOM");
            action.setTargetSlot(slot);
            action.setNewRoom(largerRooms.get(0));

            suggestions.add(new ResolutionSuggestion(
                "Move to Larger Room",
                "Move to room " + largerRooms.get(0).getRoomNumber() +
                " (capacity: " + largerRooms.get(0).getCapacity() + ")",
                0.9,
                action
            ));
        }

        return suggestions;
    }

    private List<ResolutionSuggestion> suggestTimeOverlapResolutions(Conflict conflict) {
        return suggestTeacherOverloadResolutions(conflict);
    }

    private List<ResolutionSuggestion> suggestGenericResolutions(Conflict conflict) {
        List<ResolutionSuggestion> suggestions = new ArrayList<>();

        // Generic suggestion: manual review
        suggestions.add(new ResolutionSuggestion(
            "Manual Review Required",
            "This conflict requires manual review and resolution",
            0.3,
            new ResolutionAction()
        ));

        return suggestions;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private boolean isTimeSlotAvailable(ScheduleSlot slot, DayOfWeek day,
                                       LocalTime startTime, LocalTime endTime,
                                       List<ScheduleSlot> allSlots) {
        // Check if teacher is available
        // ✅ NULL SAFE: Check teacher and IDs exist before comparison
        if (slot.getTeacher() != null && slot.getTeacher().getId() != null) {
            boolean teacherBusy = allSlots.stream()
                .filter(s -> s != null && s.getId() != null &&
                           !s.getId().equals(slot.getId()))
                .filter(s -> s.getTeacher() != null && s.getTeacher().getId() != null &&
                           s.getTeacher().getId().equals(slot.getTeacher().getId()))
                .filter(s -> s.getDayOfWeek() == day)
                .anyMatch(s -> timesOverlap(s.getStartTime(), s.getEndTime(), startTime, endTime));

            if (teacherBusy) {
                return false;
            }
        }

        // Check if room is available
        // ✅ NULL SAFE: Check room and IDs exist before comparison
        if (slot.getRoom() != null && slot.getRoom().getId() != null) {
            boolean roomBusy = allSlots.stream()
                .filter(s -> s != null && s.getId() != null &&
                           !s.getId().equals(slot.getId()))
                .filter(s -> s.getRoom() != null && s.getRoom().getId() != null &&
                           s.getRoom().getId().equals(slot.getRoom().getId()))
                .filter(s -> s.getDayOfWeek() == day)
                .anyMatch(s -> timesOverlap(s.getStartTime(), s.getEndTime(), startTime, endTime));

            if (roomBusy) {
                return false;
            }
        }

        return true;
    }

    private String getUnavailabilityReason(ScheduleSlot slot, DayOfWeek day,
                                          LocalTime startTime, LocalTime endTime,
                                          List<ScheduleSlot> allSlots) {
        if (slot.getTeacher() != null) {
            boolean teacherBusy = allSlots.stream()
                .filter(s -> s.getTeacher() != null &&
                           s.getTeacher().getId().equals(slot.getTeacher().getId()))
                .filter(s -> s.getDayOfWeek() == day)
                .anyMatch(s -> timesOverlap(s.getStartTime(), s.getEndTime(), startTime, endTime));

            if (teacherBusy) {
                return "Teacher not available";
            }
        }

        if (slot.getRoom() != null) {
            boolean roomBusy = allSlots.stream()
                .filter(s -> s.getRoom() != null &&
                           s.getRoom().getId().equals(slot.getRoom().getId()))
                .filter(s -> s.getDayOfWeek() == day)
                .anyMatch(s -> timesOverlap(s.getStartTime(), s.getEndTime(), startTime, endTime));

            if (roomBusy) {
                return "Room not available";
            }
        }

        return "Not available";
    }

    private boolean isRoomAvailableForSlot(ScheduleSlot slot, Room room) {
        if (slot.getDayOfWeek() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
            return true; // Can't check without time info
        }

        // Check if room is free at this time
        List<ScheduleSlot> allSlots = scheduleSlotRepository.findAll();

        return allSlots.stream()
            .filter(s -> !s.getId().equals(slot.getId()))
            .filter(s -> s.getRoom() != null && s.getRoom().getId().equals(room.getId()))
            .filter(s -> s.getDayOfWeek() == slot.getDayOfWeek())
            .noneMatch(s -> timesOverlap(s.getStartTime(), s.getEndTime(),
                                        slot.getStartTime(), slot.getEndTime()));
    }

    private boolean isTeacherAvailableForSlot(ScheduleSlot slot, Teacher teacher) {
        if (slot.getDayOfWeek() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
            return true; // Can't check without time info
        }

        // Check if teacher is free at this time
        List<ScheduleSlot> allSlots = scheduleSlotRepository.findAll();

        return allSlots.stream()
            .filter(s -> !s.getId().equals(slot.getId()))
            .filter(s -> s.getTeacher() != null && s.getTeacher().getId().equals(teacher.getId()))
            .filter(s -> s.getDayOfWeek() == slot.getDayOfWeek())
            .noneMatch(s -> timesOverlap(s.getStartTime(), s.getEndTime(),
                                        slot.getStartTime(), slot.getEndTime()));
    }

    private boolean timesOverlap(LocalTime start1, LocalTime end1, LocalTime start2, LocalTime end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private int getRequiredCapacity(ScheduleSlot slot) {
        // Get actual enrollment count from course section if available
        if (slot.getCourse() != null) {
            // Check if there's a course section with enrollment data
            List<CourseSection> sections = courseSectionRepository.findByCourse(slot.getCourse());
            if (!sections.isEmpty()) {
                // Return the max current enrollment across sections, or target enrollment
                int maxEnrollment = sections.stream()
                    .filter(s -> s.getCurrentEnrollment() != null)
                    .mapToInt(CourseSection::getCurrentEnrollment)
                    .max()
                    .orElse(25);
                return Math.max(maxEnrollment, 15); // At least 15 students
            }
        }
        return 25; // Default assumption
    }

    /**
     * Check if room type is compatible with course requirements
     */
    private boolean isRoomTypeCompatible(Course course, Room room) {
        if (course == null || room == null) {
            return true; // No restriction if data missing
        }

        // If room has no type specified, it's a general purpose room
        if (room.getRoomType() == null) {
            return true;
        }

        // Check course subject to determine required room type
        String subject = course.getSubject();
        if (subject == null) {
            return true; // No subject, any room works
        }

        // Match subject to appropriate room types
        String subjectLower = subject.toLowerCase();
        com.heronix.scheduler.model.enums.RoomType roomType = room.getRoomType();

        // Science courses need labs
        if (subjectLower.contains("science") || subjectLower.contains("chemistry") ||
            subjectLower.contains("physics") || subjectLower.contains("biology")) {
            return roomType == com.heronix.scheduler.model.enums.RoomType.LAB ||
                   roomType == com.heronix.scheduler.model.enums.RoomType.SCIENCE_LAB ||
                   roomType == com.heronix.scheduler.model.enums.RoomType.CLASSROOM;
        }

        // Computer courses need computer labs
        if (subjectLower.contains("computer") || subjectLower.contains("technology") ||
            subjectLower.contains("programming") || subjectLower.contains("coding")) {
            return roomType == com.heronix.scheduler.model.enums.RoomType.COMPUTER_LAB ||
                   roomType == com.heronix.scheduler.model.enums.RoomType.LAB;
        }

        // Art courses need art rooms
        if (subjectLower.contains("art") || subjectLower.contains("drawing") ||
            subjectLower.contains("painting")) {
            return roomType == com.heronix.scheduler.model.enums.RoomType.ART_STUDIO ||
                   roomType == com.heronix.scheduler.model.enums.RoomType.CLASSROOM;
        }

        // Music courses need music rooms
        if (subjectLower.contains("music") || subjectLower.contains("band") ||
            subjectLower.contains("choir") || subjectLower.contains("orchestra")) {
            return roomType == com.heronix.scheduler.model.enums.RoomType.MUSIC_ROOM ||
                   roomType == com.heronix.scheduler.model.enums.RoomType.BAND_ROOM ||
                   roomType == com.heronix.scheduler.model.enums.RoomType.CHORUS_ROOM ||
                   roomType == com.heronix.scheduler.model.enums.RoomType.AUDITORIUM;
        }

        // PE courses need gym
        if (subjectLower.contains("physical education") || subjectLower.contains("pe") ||
            subjectLower.contains("gym") || subjectLower.contains("sports")) {
            return roomType == com.heronix.scheduler.model.enums.RoomType.GYMNASIUM;
        }

        // Default: regular classroom works for most subjects
        return true;
    }

    /**
     * Check if teacher is qualified to teach the course subject
     */
    private boolean isTeacherQualifiedForCourse(Teacher teacher, Course course) {
        if (teacher == null || course == null) {
            return false;
        }

        // Check subject qualification
        String subject = course.getSubject();
        if (subject != null && !subject.isEmpty()) {
            return teacher.hasCertificationForSubject(subject);
        }

        // If no subject specified, assume teacher is qualified
        return true;
    }

    // ========================================================================
    // AI-POWERED CONFLICT RESOLUTION METHODS
    // ========================================================================

    /**
     * Get AI-powered resolution suggestions for a conflict
     *
     * @param conflict The conflict to resolve
     * @return List of AI-generated suggestions with priority ranking
     */
    public List<com.heronix.scheduler.model.dto.ConflictResolutionSuggestion> getSuggestedFixes(Conflict conflict) {
        if (conflictResolutionSuggestionService == null) {
            log.warn("ConflictResolutionSuggestionService not available, returning empty list");
            return new ArrayList<>();
        }

        return conflictResolutionSuggestionService.generateSuggestions(conflict);
    }

    /**
     * Get priority score for a conflict using ML-based scoring
     *
     * @param conflict The conflict to score
     * @return Priority score with breakdown
     */
    public com.heronix.scheduler.model.dto.ConflictPriorityScore getPriorityScore(Conflict conflict) {
        if (conflictResolutionSuggestionService == null) {
            log.warn("ConflictResolutionSuggestionService not available, returning null");
            return null;
        }

        return conflictResolutionSuggestionService.calculatePriorityScore(conflict);
    }

    /**
     * Apply an AI-suggested fix automatically
     *
     * @param conflict The conflict to resolve
     * @param suggestion The AI suggestion to apply
     * @return true if successfully applied
     */
    public boolean applyAutoFix(Conflict conflict, com.heronix.scheduler.model.dto.ConflictResolutionSuggestion suggestion) {
        if (conflictResolutionSuggestionService == null) {
            log.warn("ConflictResolutionSuggestionService not available");
            return false;
        }

        return conflictResolutionSuggestionService.applySuggestion(conflict, suggestion);
    }

    /**
     * Get all active conflicts sorted by AI priority
     *
     * @return List of conflicts sorted by priority (highest first)
     */
    public List<Conflict> getConflictsByAIPriority() {
        if (conflictResolutionSuggestionService == null) {
            log.warn("ConflictResolutionSuggestionService not available, returning unsorted list");
            return conflictRepository.findAllActive();
        }

        return conflictResolutionSuggestionService.getConflictsByPriority();
    }

    /**
     * Check if a suggestion can be auto-applied without user confirmation
     *
     * @param suggestion The suggestion to check
     * @return true if safe to auto-apply
     */
    public boolean canAutoApplySuggestion(com.heronix.scheduler.model.dto.ConflictResolutionSuggestion suggestion) {
        if (conflictResolutionSuggestionService == null) {
            return false;
        }

        return conflictResolutionSuggestionService.canAutoApply(suggestion);
    }
}
