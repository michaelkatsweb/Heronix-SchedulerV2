package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.domain.ScheduleSlot;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.domain.TimeSlot;
import com.heronix.scheduler.model.dto.Conflict;
import com.heronix.scheduler.repository.ScheduleRepository;
import com.heronix.scheduler.repository.ScheduleSlotRepository;
import com.heronix.scheduler.service.ConflictDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enhanced Conflict Detection Service Implementation
 * 
 * ✅ FIXED: All type mismatches resolved
 * ✅ FIXED: DayOfWeek enum conversions corrected
 * ✅ FIXED: Conflict DTO uses java.time.DayOfWeek
 * 
 * Location:
 * src/main/java/com/eduscheduler/service/impl/ConflictDetectionServiceImpl.java
 * 
 * @author Heronix Scheduling System Team
 * @version 2.3.0 - ALL COMPILATION ERRORS FIXED
 * @since 2025-10-31
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ConflictDetectionServiceImpl implements ConflictDetectionService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;

    // ========================================================================
    // PUBLIC API METHODS - Interface Implementation
    // ========================================================================

    @Override
    public List<Conflict> detectConflicts(Long scheduleId) {
        log.debug("Detecting conflicts for schedule ID: {}", scheduleId);

        // ✅ NULL SAFETY: Handle null schedule ID
        if (scheduleId == null) {
            log.warn("Cannot detect conflicts for null schedule ID");
            return new ArrayList<>();
        }

        // ✅ NULL SAFETY: Return empty list for non-existent schedule instead of throwing exception
        Schedule schedule = scheduleRepository.findById(scheduleId).orElse(null);
        if (schedule == null) {
            log.debug("Schedule not found: {}", scheduleId);
            return new ArrayList<>();
        }

        return detectConflicts(schedule);
    }

    @Override
    public List<Conflict> detectConflicts(Schedule schedule) {
        // ✅ NULL SAFETY: Handle null schedule
        if (schedule == null) {
            log.warn("Cannot detect conflicts for null schedule");
            return new ArrayList<>();
        }

        // ✅ NULL SAFETY: Handle null schedule ID
        if (schedule.getId() == null) {
            log.warn("Cannot detect conflicts for schedule with null ID");
            return new ArrayList<>();
        }

        log.debug("Detecting conflicts for schedule: {}", schedule.getId());

        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());
        List<Conflict> allConflicts = new ArrayList<>();

        allConflicts.addAll(detectTeacherConflicts(slots));
        allConflicts.addAll(detectRoomConflicts(slots));
        allConflicts.addAll(detectCapacityViolations(slots));

        log.info("Found {} total conflicts", allConflicts.size());
        return allConflicts;
    }

    @Override
    public List<String> detectAllConflicts(Schedule schedule) {
        // ✅ NULL SAFETY: Handle null schedule
        if (schedule == null) {
            log.warn("Cannot detect all conflicts for null schedule");
            return new ArrayList<>();
        }

        // ✅ NULL SAFETY: Handle null schedule ID
        if (schedule.getId() == null) {
            log.warn("Cannot detect all conflicts for schedule with null ID");
            return new ArrayList<>();
        }

        log.debug("Detecting all conflicts (string format) for schedule: {}", schedule.getId());

        List<Conflict> conflicts = detectConflicts(schedule);

        return conflicts.stream()
                .map(this::formatConflictAsString)
                .collect(Collectors.toList());
    }

    @Override
    public List<Conflict> checkSlotConflicts(Long slotId) {
        log.debug("Checking conflicts for slot ID: {}", slotId);

        // ✅ NULL SAFETY: Handle null slot ID
        if (slotId == null) {
            log.warn("Cannot check conflicts for null slot ID");
            return new ArrayList<>();
        }

        // ✅ NULL SAFETY: Return empty list for non-existent slot instead of throwing exception
        ScheduleSlot slot = scheduleSlotRepository.findById(slotId).orElse(null);
        if (slot == null) {
            log.debug("Slot not found: {}", slotId);
            return new ArrayList<>();
        }

        List<Conflict> conflicts = new ArrayList<>();

        if (hasTeacherConflict(slot)) {
            conflicts.add(createTeacherConflict(slot));
        }

        if (hasRoomConflict(slot)) {
            conflicts.add(createRoomConflict(slot));
        }

        return conflicts;
    }

    @Override
    public List<Conflict> checkMoveConflicts(ScheduleSlot slot, TimeSlot newTime) {
        // ✅ NULL SAFETY: Handle null slot
        if (slot == null) {
            log.warn("Cannot check move conflicts for null slot");
            return new ArrayList<>();
        }

        // ✅ NULL SAFETY: Handle null newTime
        if (newTime == null) {
            log.warn("Cannot check move conflicts with null time slot");
            return new ArrayList<>();
        }

        log.debug("Checking move conflicts for slot {} to time {}", slot.getId(), newTime);

        List<Conflict> conflicts = new ArrayList<>();

        // Check teacher availability at new time
        // ✅ NULL SAFE: Check teacher and schedule exist
        if (slot.getTeacher() != null && slot.getSchedule() != null) {
            boolean teacherConflict = hasTeacherConflictAtTime(
                    slot.getSchedule().getId(),
                    slot.getTeacher().getId(),
                    newTime.getDayOfWeek(),
                    newTime.getStartTime(),
                    newTime.getEndTime(),
                    slot.getId());

            if (teacherConflict) {
                Conflict conflict = new Conflict();
                conflict.setConflictType("TEACHER_CONFLICT");
                conflict.setSeverity("HIGH");
                conflict.setDescription(String.format(
                        "Teacher %s is already scheduled at %s %s-%s",
                        slot.getTeacher().getName(),
                        newTime.getDayOfWeek(),
                        newTime.getStartTime(),
                        newTime.getEndTime()));
                conflict.setTeacherId(slot.getTeacher().getId());
                conflict.setTeacherName(slot.getTeacher().getName());
                conflict.setDayOfWeek(newTime.getDayOfWeek()); // java.time.DayOfWeek
                conflict.setStartTime(newTime.getStartTime());
                conflict.setEndTime(newTime.getEndTime());
                conflicts.add(conflict);
            }
        }

        // Check room availability at new time
        // ✅ NULL SAFE: Check room and schedule exist
        if (slot.getRoom() != null && slot.getSchedule() != null) {
            boolean roomConflict = hasRoomConflictAtTime(
                    slot.getSchedule().getId(),
                    slot.getRoom().getId(),
                    newTime.getDayOfWeek(),
                    newTime.getStartTime(),
                    newTime.getEndTime(),
                    slot.getId());

            if (roomConflict) {
                Conflict conflict = new Conflict();
                conflict.setConflictType("ROOM_CONFLICT");
                conflict.setSeverity("HIGH");
                conflict.setDescription(String.format(
                        "Room %s is already booked at %s %s-%s",
                        slot.getRoom().getRoomNumber(),
                        newTime.getDayOfWeek(),
                        newTime.getStartTime(),
                        newTime.getEndTime()));
                conflict.setRoomId(slot.getRoom().getId());
                conflict.setRoomNumber(slot.getRoom().getRoomNumber());
                conflict.setDayOfWeek(newTime.getDayOfWeek()); // java.time.DayOfWeek
                conflict.setStartTime(newTime.getStartTime());
                conflict.setEndTime(newTime.getEndTime());
                conflicts.add(conflict);
            }
        }

        return conflicts;
    }

    @Override
    public List<String> getResolutionSuggestions(Conflict conflict) {
        List<String> suggestions = new ArrayList<>();

        switch (conflict.getConflictType()) {
            case "TEACHER_CONFLICT":
                suggestions.add("Find another available teacher for this course");
                suggestions.add("Move the class to a different time slot");
                suggestions.add("Swap with another class that has no conflicts");
                break;

            case "ROOM_CONFLICT":
                suggestions.add("Find another available room with similar capacity");
                suggestions.add("Move the class to a different time slot");
                suggestions.add("Swap rooms with another class");
                break;

            case "CAPACITY_CONFLICT":
                suggestions.add("Move to a larger room");
                suggestions.add("Split the class into multiple sections");
                suggestions.add("Reduce enrollment");
                break;

            default:
                suggestions.add("Manually review and adjust the schedule");
        }

        return suggestions;
    }

    @Override
    public boolean autoResolveConflicts(Long scheduleId) {
        log.info("Attempting to auto-resolve conflicts for schedule: {}", scheduleId);

        List<Conflict> conflicts = detectConflicts(scheduleId);

        if (conflicts.isEmpty()) {
            log.info("No conflicts to resolve");
            return true;
        }

        log.info("Auto-resolution requires manual implementation for {} conflicts", conflicts.size());
        return false;
    }

    /**
     * ✅ FIXED: ScheduleSlot stores java.time.DayOfWeek directly
     */
    @Override
    public boolean hasTeacherConflict(Long teacherId, DayOfWeek day,
            LocalTime start, LocalTime end) {

        List<ScheduleSlot> allSlots = scheduleSlotRepository.findAll();

        List<ScheduleSlot> teacherSlots = allSlots.stream()
                .filter(slot -> slot.getTeacher() != null)
                .filter(slot -> slot.getTeacher().getId().equals(teacherId))
                .filter(slot -> slot.getDayOfWeek() != null)
                .filter(slot -> slot.getDayOfWeek().equals(day))
                .collect(Collectors.toList());

        return teacherSlots.stream()
                .anyMatch(slot -> timeSlotsOverlap(
                        slot.getStartTime(), slot.getEndTime(),
                        start, end));
    }

    /**
     * ✅ FIXED: ScheduleSlot stores java.time.DayOfWeek directly
     */
    @Override
    public boolean hasRoomConflict(Long roomId, DayOfWeek day,
            LocalTime start, LocalTime end) {

        List<ScheduleSlot> allSlots = scheduleSlotRepository.findAll();

        List<ScheduleSlot> roomSlots = allSlots.stream()
                .filter(slot -> slot.getRoom() != null)
                .filter(slot -> slot.getRoom().getId().equals(roomId))
                .filter(slot -> slot.getDayOfWeek() != null)
                .filter(slot -> slot.getDayOfWeek().equals(day))
                .collect(Collectors.toList());

        return roomSlots.stream()
                .anyMatch(slot -> timeSlotsOverlap(
                        slot.getStartTime(), slot.getEndTime(),
                        start, end));
    }

    // ========================================================================
    // PRIVATE HELPER METHODS
    // ========================================================================

    /**
     * ✅ FIXED: ScheduleSlot stores java.time.DayOfWeek directly
     */
    private boolean hasTeacherConflictAtTime(Long scheduleId, Long teacherId,
            DayOfWeek day, LocalTime start, LocalTime end, Long excludeSlotId) {

        List<ScheduleSlot> scheduleSlots = scheduleSlotRepository.findByScheduleId(scheduleId);

        List<ScheduleSlot> teacherSlots = scheduleSlots.stream()
                .filter(slot -> slot.getTeacher() != null)
                .filter(slot -> slot.getTeacher().getId().equals(teacherId))
                .filter(slot -> slot.getDayOfWeek() != null)
                .filter(slot -> slot.getDayOfWeek().equals(day))
                .filter(slot -> !slot.getId().equals(excludeSlotId))
                .collect(Collectors.toList());

        return teacherSlots.stream()
                .anyMatch(slot -> timeSlotsOverlap(
                        slot.getStartTime(), slot.getEndTime(),
                        start, end));
    }

    /**
     * ✅ FIXED: ScheduleSlot stores java.time.DayOfWeek directly
     */
    private boolean hasRoomConflictAtTime(Long scheduleId, Long roomId,
            DayOfWeek day, LocalTime start, LocalTime end, Long excludeSlotId) {

        List<ScheduleSlot> scheduleSlots = scheduleSlotRepository.findByScheduleId(scheduleId);

        List<ScheduleSlot> roomSlots = scheduleSlots.stream()
                .filter(slot -> slot.getRoom() != null)
                .filter(slot -> slot.getRoom().getId().equals(roomId))
                .filter(slot -> slot.getDayOfWeek() != null)
                .filter(slot -> slot.getDayOfWeek().equals(day))
                .filter(slot -> !slot.getId().equals(excludeSlotId))
                .collect(Collectors.toList());

        return roomSlots.stream()
                .anyMatch(slot -> timeSlotsOverlap(
                        slot.getStartTime(), slot.getEndTime(),
                        start, end));
    }

    /**
     * ✅ FIXED: Map type uses java.time.DayOfWeek (ScheduleSlot stores java.time.DayOfWeek)
     */
    private List<Conflict> detectTeacherConflicts(List<ScheduleSlot> slots) {
        List<Conflict> conflicts = new ArrayList<>();

        // Group by teacher and day (ScheduleSlot uses java.time.DayOfWeek)
        // ✅ NULL SAFE: Filter null teachers and teacher IDs
        Map<Long, Map<DayOfWeek, List<ScheduleSlot>>> teacherSchedule = slots.stream()
                .filter(s -> s.getTeacher() != null && s.getTeacher().getId() != null)
                .filter(s -> s.getDayOfWeek() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getTeacher().getId(),
                        Collectors.groupingBy(ScheduleSlot::getDayOfWeek)));

        // Check for time overlaps within each day
        for (Map.Entry<Long, Map<DayOfWeek, List<ScheduleSlot>>> teacherEntry : teacherSchedule
                .entrySet()) {
            for (Map.Entry<DayOfWeek, List<ScheduleSlot>> dayEntry : teacherEntry
                    .getValue().entrySet()) {
                List<ScheduleSlot> daySlots = dayEntry.getValue();

                for (int i = 0; i < daySlots.size(); i++) {
                    for (int j = i + 1; j < daySlots.size(); j++) {
                        ScheduleSlot slot1 = daySlots.get(i);
                        ScheduleSlot slot2 = daySlots.get(j);

                        if (timeSlotsOverlap(
                                slot1.getStartTime(), slot1.getEndTime(),
                                slot2.getStartTime(), slot2.getEndTime())) {
                            conflicts.add(createTeacherConflict(slot1, slot2));
                        }
                    }
                }
            }
        }

        log.debug("Found {} teacher conflicts", conflicts.size());
        return conflicts;
    }

    /**
     * ✅ FIXED: Map type uses java.time.DayOfWeek (ScheduleSlot stores java.time.DayOfWeek)
     */
    private List<Conflict> detectRoomConflicts(List<ScheduleSlot> slots) {
        List<Conflict> conflicts = new ArrayList<>();

        // Group by room and day (ScheduleSlot uses java.time.DayOfWeek)
        // ✅ NULL SAFE: Filter null rooms and room IDs
        Map<Long, Map<DayOfWeek, List<ScheduleSlot>>> roomSchedule = slots.stream()
                .filter(s -> s.getRoom() != null && s.getRoom().getId() != null)
                .filter(s -> s.getDayOfWeek() != null)
                .collect(Collectors.groupingBy(
                        s -> s.getRoom().getId(),
                        Collectors.groupingBy(ScheduleSlot::getDayOfWeek)));

        // Check for time overlaps within each day
        for (Map.Entry<Long, Map<DayOfWeek, List<ScheduleSlot>>> roomEntry : roomSchedule
                .entrySet()) {
            for (Map.Entry<DayOfWeek, List<ScheduleSlot>> dayEntry : roomEntry.getValue()
                    .entrySet()) {
                List<ScheduleSlot> daySlots = dayEntry.getValue();

                for (int i = 0; i < daySlots.size(); i++) {
                    for (int j = i + 1; j < daySlots.size(); j++) {
                        ScheduleSlot slot1 = daySlots.get(i);
                        ScheduleSlot slot2 = daySlots.get(j);

                        if (timeSlotsOverlap(
                                slot1.getStartTime(), slot1.getEndTime(),
                                slot2.getStartTime(), slot2.getEndTime())) {
                            conflicts.add(createRoomConflict(slot1, slot2));
                        }
                    }
                }
            }
        }

        log.debug("Found {} room conflicts", conflicts.size());
        return conflicts;
    }

    private List<Conflict> detectCapacityViolations(List<ScheduleSlot> slots) {
        List<Conflict> conflicts = new ArrayList<>();

        for (ScheduleSlot slot : slots) {
            if (slot.getRoom() != null && slot.getEnrolledStudents() != null) {
                Integer capacity = slot.getRoom().getCapacity();
                Integer enrolled = slot.getEnrolledStudents();

                if (capacity != null && enrolled > capacity) {
                    conflicts.add(createCapacityConflict(slot));
                }
            }
        }

        log.debug("Found {} capacity violations", conflicts.size());
        return conflicts;
    }

    private boolean timeSlotsOverlap(LocalTime start1, LocalTime end1,
            LocalTime start2, LocalTime end2) {
        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    /**
     * ✅ FIXED: ScheduleSlot stores java.time.DayOfWeek directly
     */
    private boolean hasTeacherConflict(ScheduleSlot slot) {
        if (slot.getTeacher() == null || slot.getDayOfWeek() == null)
            return false;

        return hasTeacherConflict(
                slot.getTeacher().getId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime());
    }

    /**
     * ✅ FIXED: ScheduleSlot stores java.time.DayOfWeek directly
     */
    private boolean hasRoomConflict(ScheduleSlot slot) {
        if (slot.getRoom() == null || slot.getDayOfWeek() == null)
            return false;

        return hasRoomConflict(
                slot.getRoom().getId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime());
    }

    // ========================================================================
    // DAY OF WEEK CONVERSION HELPERS
    // ========================================================================

    private com.heronix.scheduler.model.enums.DayOfWeek convertJavaDayToCustomDay(DayOfWeek javaDay) {
        if (javaDay == null)
            return null;

        switch (javaDay) {
            case MONDAY:
                return com.heronix.scheduler.model.enums.DayOfWeek.MONDAY;
            case TUESDAY:
                return com.heronix.scheduler.model.enums.DayOfWeek.TUESDAY;
            case WEDNESDAY:
                return com.heronix.scheduler.model.enums.DayOfWeek.WEDNESDAY;
            case THURSDAY:
                return com.heronix.scheduler.model.enums.DayOfWeek.THURSDAY;
            case FRIDAY:
                return com.heronix.scheduler.model.enums.DayOfWeek.FRIDAY;
            case SATURDAY:
                return com.heronix.scheduler.model.enums.DayOfWeek.SATURDAY;
            case SUNDAY:
                return com.heronix.scheduler.model.enums.DayOfWeek.SUNDAY;
            default:
                return com.heronix.scheduler.model.enums.DayOfWeek.MONDAY;
        }
    }

    private DayOfWeek convertCustomDayToJavaDay(com.heronix.scheduler.model.enums.DayOfWeek customDay) {
        if (customDay == null)
            return null;

        switch (customDay) {
            case MONDAY:
                return DayOfWeek.MONDAY;
            case TUESDAY:
                return DayOfWeek.TUESDAY;
            case WEDNESDAY:
                return DayOfWeek.WEDNESDAY;
            case THURSDAY:
                return DayOfWeek.THURSDAY;
            case FRIDAY:
                return DayOfWeek.FRIDAY;
            case SATURDAY:
                return DayOfWeek.SATURDAY;
            case SUNDAY:
                return DayOfWeek.SUNDAY;
            default:
                return DayOfWeek.MONDAY;
        }
    }

    // ========================================================================
    // CONFLICT DTO CREATION
    // ========================================================================

    private Conflict createTeacherConflict(ScheduleSlot slot) {
        Conflict conflict = new Conflict();
        conflict.setConflictType("TEACHER_CONFLICT");
        conflict.setSeverity("HIGH");
        conflict.setDescription(String.format(
                "Teacher %s is double-booked on %s from %s to %s",
                slot.getTeacher().getName(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime()));

        populateTeacherConflict(conflict, slot);
        conflict.setCanAutoResolve(false);

        return conflict;
    }

    private Conflict createTeacherConflict(ScheduleSlot slot1, ScheduleSlot slot2) {
        Conflict conflict = new Conflict();
        conflict.setConflictType("TEACHER_CONFLICT");
        conflict.setSeverity("HIGH");

        // ✅ NULL SAFE: Check course and course name exist
        String course1 = (slot1.getCourse() != null && slot1.getCourse().getCourseName() != null)
                ? slot1.getCourse().getCourseName() : "Unknown";
        String course2 = (slot2.getCourse() != null && slot2.getCourse().getCourseName() != null)
                ? slot2.getCourse().getCourseName() : "Unknown";

        // ✅ NULL SAFE: Check teacher name exists
        String teacherName = (slot1.getTeacher() != null && slot1.getTeacher().getName() != null)
                ? slot1.getTeacher().getName() : "Unknown Teacher";

        conflict.setDescription(String.format(
                "Teacher %s is scheduled for both '%s' and '%s' on %s (%s-%s)",
                teacherName,
                course1,
                course2,
                slot1.getDayOfWeek(),
                slot1.getStartTime(),
                slot1.getEndTime()));

        populateTeacherConflict(conflict, slot1);
        conflict.setCanAutoResolve(false);

        return conflict;
    }

    private Conflict createRoomConflict(ScheduleSlot slot) {
        Conflict conflict = new Conflict();
        conflict.setConflictType("ROOM_CONFLICT");
        conflict.setSeverity("HIGH");
        conflict.setDescription(String.format(
                "Room %s is double-booked on %s from %s to %s",
                slot.getRoom().getRoomNumber(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime()));

        populateRoomConflict(conflict, slot);
        conflict.setCanAutoResolve(false);

        return conflict;
    }

    private Conflict createRoomConflict(ScheduleSlot slot1, ScheduleSlot slot2) {
        Conflict conflict = new Conflict();
        conflict.setConflictType("ROOM_CONFLICT");
        conflict.setSeverity("HIGH");

        // ✅ NULL SAFE: Check course and course name exist
        String course1 = (slot1.getCourse() != null && slot1.getCourse().getCourseName() != null)
                ? slot1.getCourse().getCourseName() : "Unknown";
        String course2 = (slot2.getCourse() != null && slot2.getCourse().getCourseName() != null)
                ? slot2.getCourse().getCourseName() : "Unknown";

        // ✅ NULL SAFE: Check room number exists
        String roomNumber = (slot1.getRoom() != null && slot1.getRoom().getRoomNumber() != null)
                ? slot1.getRoom().getRoomNumber() : "Unknown Room";

        conflict.setDescription(String.format(
                "Room %s is scheduled for both '%s' and '%s' on %s (%s-%s)",
                roomNumber,
                course1,
                course2,
                slot1.getDayOfWeek(),
                slot1.getStartTime(),
                slot1.getEndTime()));

        populateRoomConflict(conflict, slot1);
        conflict.setCanAutoResolve(true);

        return conflict;
    }

    private Conflict createCapacityConflict(ScheduleSlot slot) {
        Conflict conflict = new Conflict();
        conflict.setConflictType("CAPACITY_CONFLICT");
        conflict.setSeverity("MEDIUM");

        Room room = slot.getRoom();
        Integer enrolled = slot.getEnrolledStudents();
        Integer capacity = room.getCapacity();
        int overflow = enrolled - capacity;

        conflict.setDescription(String.format(
                "Room %s capacity exceeded: %d students enrolled (capacity: %d, overflow: %d)",
                room.getRoomNumber(),
                enrolled,
                capacity,
                overflow));

        populateRoomConflict(conflict, slot);
        conflict.setCanAutoResolve(true);

        return conflict;
    }

    /**
     * ✅ FIXED: ScheduleSlot already stores java.time.DayOfWeek
     */
    private void populateTeacherConflict(Conflict conflict, ScheduleSlot slot) {
        Teacher teacher = slot.getTeacher();
        if (teacher != null) {
            conflict.setTeacherId(teacher.getId());
            conflict.setTeacherName(teacher.getName());
        }

        conflict.setDayOfWeek(slot.getDayOfWeek());
        conflict.setStartTime(slot.getStartTime());
        conflict.setEndTime(slot.getEndTime());
    }

    /**
     * ✅ FIXED: ScheduleSlot already stores java.time.DayOfWeek
     */
    private void populateRoomConflict(Conflict conflict, ScheduleSlot slot) {
        Room room = slot.getRoom();
        if (room != null) {
            conflict.setRoomId(room.getId());
            conflict.setRoomNumber(room.getRoomNumber());
        }

        conflict.setDayOfWeek(slot.getDayOfWeek());
        conflict.setStartTime(slot.getStartTime());
        conflict.setEndTime(slot.getEndTime());
    }

    private String formatConflictAsString(Conflict conflict) {
        StringBuilder sb = new StringBuilder();

        switch (conflict.getSeverity().toUpperCase()) {
            case "HIGH":
                sb.append("🔴 ");
                break;
            case "MEDIUM":
                sb.append("🟡 ");
                break;
            case "LOW":
                sb.append("🟢 ");
                break;
            default:
                sb.append("⚠️ ");
        }

        sb.append(conflict.getDescription());

        if (conflict.getDayOfWeek() != null) {
            sb.append(" [").append(conflict.getDayOfWeek()).append("]");
        }

        if (conflict.getStartTime() != null && conflict.getEndTime() != null) {
            sb.append(" [").append(conflict.getStartTime())
                    .append("-").append(conflict.getEndTime()).append("]");
        }

        return sb.toString();
    }

    // ========================================================================
    // MANUAL OVERRIDE CONFLICT DETECTION
    // ========================================================================

    @Override
    public List<String> detectConflicts(ScheduleSlot slot, Teacher newTeacher, Room newRoom) {
        List<String> conflicts = new ArrayList<>();

        if (slot == null || slot.getSchedule() == null) {
            return conflicts;
        }

        // Check teacher conflict
        if (newTeacher != null && hasTeacherConflict(
                newTeacher,
                slot.getPeriodNumber(),
                slot.getDayType() != null ? slot.getDayType().name() : "DAILY",
                slot.getSchedule().getId())) {
            conflicts.add(String.format(
                "Teacher %s is already scheduled during Period %d",
                newTeacher.getName(), slot.getPeriodNumber()));
        }

        // Check room conflict
        if (newRoom != null && hasRoomConflict(
                newRoom,
                slot.getPeriodNumber(),
                slot.getDayType() != null ? slot.getDayType().name() : "DAILY",
                slot.getSchedule().getId())) {
            conflicts.add(String.format(
                "Room %s is already booked during Period %d",
                newRoom.getRoomNumber(), slot.getPeriodNumber()));
        }

        // Check capacity
        if (newRoom != null && slot.getEnrolledStudents() != null
                && hasCapacityConflict(newRoom, slot.getEnrolledStudents())) {
            conflicts.add(String.format(
                "Room %s capacity (%d) exceeded by enrollment (%d)",
                newRoom.getRoomNumber(), newRoom.getCapacity(), slot.getEnrolledStudents()));
        }

        return conflicts;
    }

    @Override
    public boolean hasTeacherConflict(Teacher teacher, Integer periodNumber,
            String dayType, Long scheduleId) {
        if (teacher == null || periodNumber == null) return false;

        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(scheduleId);

        return slots.stream()
            .filter(s -> s.getTeacher() != null)
            .filter(s -> s.getTeacher().getId().equals(teacher.getId()))
            .filter(s -> s.getPeriodNumber() != null)
            .filter(s -> s.getPeriodNumber().equals(periodNumber))
            .filter(s -> {
                String slotDayType = s.getDayType() != null ? s.getDayType().name() : "DAILY";
                return slotDayType.equals(dayType) || slotDayType.equals("DAILY") || dayType.equals("DAILY");
            })
            .findAny()
            .isPresent();
    }

    @Override
    public boolean hasRoomConflict(Room room, Integer periodNumber,
            String dayType, Long scheduleId) {
        if (room == null || periodNumber == null) return false;

        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(scheduleId);

        return slots.stream()
            .filter(s -> s.getRoom() != null)
            .filter(s -> s.getRoom().getId().equals(room.getId()))
            .filter(s -> s.getPeriodNumber() != null)
            .filter(s -> s.getPeriodNumber().equals(periodNumber))
            .filter(s -> {
                String slotDayType = s.getDayType() != null ? s.getDayType().name() : "DAILY";
                return slotDayType.equals(dayType) || slotDayType.equals("DAILY") || dayType.equals("DAILY");
            })
            .findAny()
            .isPresent();
    }

    @Override
    public boolean hasCapacityConflict(Room room, Integer currentEnrollment) {
        if (room == null || room.getCapacity() == null || currentEnrollment == null) {
            return false;
        }
        return currentEnrollment > room.getCapacity();
    }
}
