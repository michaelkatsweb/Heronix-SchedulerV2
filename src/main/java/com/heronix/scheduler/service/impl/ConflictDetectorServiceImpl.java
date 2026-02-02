package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.ConflictSeverity;
import com.heronix.scheduler.model.enums.ConflictType;
import com.heronix.scheduler.model.enums.RoomType;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.ConflictDetectorService;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Conflict Detector Service Implementation
 * Implements comprehensive conflict detection algorithms
 *
 * Location: src/main/java/com/eduscheduler/service/impl/ConflictDetectorServiceImpl.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7B - Conflict Detection
 */
@Slf4j
@Service
@Transactional
public class ConflictDetectorServiceImpl implements ConflictDetectorService {

    @Autowired
    private ConflictRepository conflictRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private CourseSectionRepository courseSectionRepository;

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;

    // ========================================================================
    // COMPREHENSIVE DETECTION
    // ========================================================================

    @Override
    public List<Conflict> detectAllConflicts(Schedule schedule) {
        log.info("Detecting all conflicts for schedule: {}", schedule.getScheduleName());

        List<Conflict> allConflicts = new ArrayList<>();

        // Time-based conflicts
        allConflicts.addAll(detectTimeOverlaps(schedule));
        allConflicts.addAll(detectBackToBackViolations(schedule));
        allConflicts.addAll(detectMissingLunchBreaks(schedule));
        allConflicts.addAll(detectExcessiveConsecutiveClasses(schedule));

        // Room-based conflicts
        allConflicts.addAll(detectRoomDoubleBookings(schedule));
        allConflicts.addAll(detectRoomCapacityViolations(schedule));
        allConflicts.addAll(detectRoomTypeMismatches(schedule));

        // Teacher-based conflicts
        allConflicts.addAll(detectTeacherOverloads(schedule));
        allConflicts.addAll(detectExcessiveTeachingHours(schedule));
        allConflicts.addAll(detectMissingPreparationPeriods(schedule));
        allConflicts.addAll(detectSubjectMismatches(schedule));

        // Student-based conflicts
        allConflicts.addAll(detectStudentScheduleConflicts(schedule));

        // Course-based conflicts
        allConflicts.addAll(detectSectionOverEnrollment(schedule));
        allConflicts.addAll(detectSectionUnderEnrollment(schedule));

        log.info("Total conflicts detected: {}", allConflicts.size());
        return allConflicts;
    }

    @Override
    public List<Conflict> detectConflictsForSlot(ScheduleSlot slot) {
        log.debug("Detecting conflicts for slot: {}", slot.getId());

        List<Conflict> conflicts = new ArrayList<>();

        // Check time overlaps with other slots
        List<ScheduleSlot> overlappingSlots = findOverlappingSlots(slot);

        for (ScheduleSlot overlapping : overlappingSlots) {
            // Check for teacher conflicts
            if (slot.getTeacher() != null && overlapping.getTeacher() != null &&
                slot.getTeacher().getId().equals(overlapping.getTeacher().getId())) {

                Conflict conflict = createConflict(
                    ConflictType.TEACHER_OVERLOAD,
                    ConflictSeverity.CRITICAL,
                    "Teacher Double-Booked",
                    String.format("Teacher %s is assigned to multiple classes at the same time",
                        slot.getTeacher().getName()),
                    slot.getSchedule()
                );
                conflict.getAffectedSlots().add(slot);
                conflict.getAffectedSlots().add(overlapping);
                conflict.getAffectedTeachers().add(slot.getTeacher());
                conflicts.add(conflict);
            }

            // Check for room conflicts
            if (slot.getRoom() != null && overlapping.getRoom() != null &&
                slot.getRoom().getId().equals(overlapping.getRoom().getId())) {

                Conflict conflict = createConflict(
                    ConflictType.ROOM_DOUBLE_BOOKING,
                    ConflictSeverity.CRITICAL,
                    "Room Double-Booked",
                    String.format("Room %s is assigned to multiple classes at the same time",
                        slot.getRoom().getRoomNumber()),
                    slot.getSchedule()
                );
                conflict.getAffectedSlots().add(slot);
                conflict.getAffectedSlots().add(overlapping);
                conflict.getAffectedRooms().add(slot.getRoom());
                conflicts.add(conflict);
            }
        }

        // Check room capacity
        if (slot.getRoom() != null && slot.getCourse() != null) {
            int enrollmentCount = getEnrollmentCount(slot);
            if (enrollmentCount > slot.getRoom().getCapacity()) {
                Conflict conflict = createConflict(
                    ConflictType.ROOM_CAPACITY_EXCEEDED,
                    ConflictSeverity.HIGH,
                    "Room Capacity Exceeded",
                    String.format("Room %s (capacity %d) has %d students enrolled",
                        slot.getRoom().getRoomNumber(), slot.getRoom().getCapacity(), enrollmentCount),
                    slot.getSchedule()
                );
                conflict.getAffectedSlots().add(slot);
                conflict.getAffectedRooms().add(slot.getRoom());
                conflicts.add(conflict);
            }
        }

        return conflicts;
    }

    @Override
    public boolean hasConflicts(Schedule schedule) {
        long count = conflictRepository.countActiveBySchedule(schedule);
        return count > 0;
    }

    // ========================================================================
    // TIME-BASED DETECTION
    // ========================================================================

    @Override
    public List<Conflict> detectTimeOverlaps(Schedule schedule) {
        log.debug("Detecting time overlaps for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .collect(Collectors.toList());

        // Group by day and check for overlaps
        Map<String, List<ScheduleSlot>> slotsByDay = slots.stream()
            .collect(Collectors.groupingBy(slot ->
                slot.getDayOfWeek() != null ? slot.getDayOfWeek().toString() : "UNKNOWN"));

        for (Map.Entry<String, List<ScheduleSlot>> entry : slotsByDay.entrySet()) {
            List<ScheduleSlot> daySlots = entry.getValue();

            for (int i = 0; i < daySlots.size(); i++) {
                for (int j = i + 1; j < daySlots.size(); j++) {
                    ScheduleSlot slot1 = daySlots.get(i);
                    ScheduleSlot slot2 = daySlots.get(j);

                    if (timesOverlap(slot1, slot2)) {
                        // Check if same teacher or room
                        boolean sameTeacher = slot1.getTeacher() != null && slot2.getTeacher() != null &&
                            slot1.getTeacher().getId().equals(slot2.getTeacher().getId());

                        boolean sameRoom = slot1.getRoom() != null && slot2.getRoom() != null &&
                            slot1.getRoom().getId().equals(slot2.getRoom().getId());

                        if (sameTeacher || sameRoom) {
                            String entity = sameTeacher ? "Teacher" : "Room";
                            String name = sameTeacher ? slot1.getTeacher().getName() : slot1.getRoom().getRoomNumber();

                            Conflict conflict = createConflict(
                                sameTeacher ? ConflictType.TEACHER_OVERLOAD : ConflictType.ROOM_DOUBLE_BOOKING,
                                ConflictSeverity.CRITICAL,
                                entity + " Time Overlap",
                                String.format("%s %s has overlapping assignments on %s", entity, name, entry.getKey()),
                                schedule
                            );
                            conflict.getAffectedSlots().add(slot1);
                            conflict.getAffectedSlots().add(slot2);
                            if (sameTeacher) {
                                conflict.getAffectedTeachers().add(slot1.getTeacher());
                            } else {
                                conflict.getAffectedRooms().add(slot1.getRoom());
                            }
                            conflicts.add(conflict);
                        }
                    }
                }
            }
        }

        return conflicts;
    }

    @Override
    public List<Conflict> detectBackToBackViolations(Schedule schedule) {
        log.debug("Detecting back-to-back violations for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all schedule slots for this schedule
        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .collect(Collectors.toList());

        // Group by teacher
        Map<Long, List<ScheduleSlot>> slotsByTeacher = slots.stream()
            .filter(slot -> slot.getTeacher() != null)
            .collect(Collectors.groupingBy(slot -> slot.getTeacher().getId()));

        for (Map.Entry<Long, List<ScheduleSlot>> entry : slotsByTeacher.entrySet()) {
            Teacher teacher = entry.getValue().get(0).getTeacher();
            List<ScheduleSlot> teacherSlots = entry.getValue();

            // Sort by day and time
            teacherSlots.sort((s1, s2) -> {
                int dayCompare = s1.getDayOfWeek().compareTo(s2.getDayOfWeek());
                if (dayCompare != 0) return dayCompare;
                return s1.getStartTime().compareTo(s2.getStartTime());
            });

            // Check for excessive consecutive classes (more than 4 in a row)
            int consecutiveCount = 1;
            for (int i = 0; i < teacherSlots.size() - 1; i++) {
                ScheduleSlot current = teacherSlots.get(i);
                ScheduleSlot next = teacherSlots.get(i + 1);

                if (current.getDayOfWeek().equals(next.getDayOfWeek()) &&
                    current.getEndTime() != null && next.getStartTime() != null &&
                    current.getEndTime().equals(next.getStartTime())) {
                    consecutiveCount++;

                    // Field not available on SIS Teacher entity — uses default 15-minute break
                    // For now, use default of 15 minutes preferred break
                    Integer preferredBreakMinutes = 15;
                    if (preferredBreakMinutes != null && preferredBreakMinutes > 0) {
                        Conflict conflict = createConflict(
                            ConflictType.TEACHER_OVERLOAD,
                            ConflictSeverity.LOW,
                            "Back-to-Back Classes Without Break",
                            String.format("Teacher %s has back-to-back classes on %s without preferred %d minute break",
                                teacher.getName(),
                                current.getDayOfWeek(),
                                preferredBreakMinutes),
                            schedule
                        );
                        conflict.getAffectedSlots().add(current);
                        conflict.getAffectedSlots().add(next);
                        conflict.getAffectedTeachers().add(teacher);
                        conflicts.add(conflict);
                    }
                } else {
                    consecutiveCount = 1;
                }
            }
        }

        log.debug("Found {} back-to-back violation conflicts", conflicts.size());
        return conflicts;
    }

    @Override
    public List<Conflict> detectMissingLunchBreaks(Schedule schedule) {
        log.debug("Detecting missing lunch breaks for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all schedule slots for this schedule
        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .collect(Collectors.toList());

        // Group by teacher and day
        Map<Long, Map<DayOfWeek, List<ScheduleSlot>>> slotsByTeacherAndDay = slots.stream()
            .filter(slot -> slot.getTeacher() != null && slot.getDayOfWeek() != null)
            .collect(Collectors.groupingBy(
                slot -> slot.getTeacher().getId(),
                Collectors.groupingBy(ScheduleSlot::getDayOfWeek)
            ));

        for (Map.Entry<Long, Map<DayOfWeek, List<ScheduleSlot>>> teacherEntry : slotsByTeacherAndDay.entrySet()) {
            Teacher teacher = sisDataService.getTeacherById(teacherEntry.getKey()).orElse(null);
            if (teacher == null) continue;

            for (Map.Entry<DayOfWeek, List<ScheduleSlot>> dayEntry : teacherEntry.getValue().entrySet()) {
                List<ScheduleSlot> daySlots = dayEntry.getValue();

                // Sort by time
                daySlots.sort((s1, s2) -> s1.getStartTime().compareTo(s2.getStartTime()));

                // Check if there's a lunch break (typically 11:00 AM - 1:00 PM)
                boolean hasLunchBreak = false;
                LocalTime lunchStart = LocalTime.of(11, 0);
                LocalTime lunchEnd = LocalTime.of(13, 0);

                for (ScheduleSlot slot : daySlots) {
                    // Check if any slot overlaps with lunch time window
                    if (slot.getStartTime() != null && slot.getEndTime() != null) {
                        if (slot.getStartTime().isBefore(lunchEnd) && slot.getEndTime().isAfter(lunchStart)) {
                            // Slot is during lunch window - check if it's a full lunch period
                            continue;
                        }
                    }
                }

                // Check if teacher has at least 30-minute gap during lunch time
                for (int i = 0; i < daySlots.size() - 1; i++) {
                    ScheduleSlot current = daySlots.get(i);
                    ScheduleSlot next = daySlots.get(i + 1);

                    if (current.getEndTime() != null && next.getStartTime() != null) {
                        // Check if gap is during lunch time and at least 30 minutes
                        if (current.getEndTime().isBefore(lunchEnd) && next.getStartTime().isAfter(lunchStart)) {
                            long minutes = java.time.Duration.between(current.getEndTime(), next.getStartTime()).toMinutes();
                            if (minutes >= 30) {
                                hasLunchBreak = true;
                                break;
                            }
                        }
                    }
                }

                // If no lunch break found and teacher has 5+ periods that day, flag it
                if (!hasLunchBreak && daySlots.size() >= 5) {
                    Conflict conflict = createConflict(
                        ConflictType.TEACHER_OVERLOAD,
                        ConflictSeverity.MEDIUM,
                        "Missing Lunch Break",
                        String.format("Teacher %s has no lunch break on %s",
                            teacher.getName(),
                            dayEntry.getKey()),
                        schedule
                    );
                    conflict.getAffectedTeachers().add(teacher);
                    conflicts.add(conflict);
                }
            }
        }

        log.debug("Found {} missing lunch break conflicts", conflicts.size());
        return conflicts;
    }

    @Override
    public List<Conflict> detectExcessiveConsecutiveClasses(Schedule schedule) {
        log.debug("Detecting excessive consecutive classes for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all schedule slots for this schedule
        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .collect(Collectors.toList());

        // Group by teacher
        Map<Long, List<ScheduleSlot>> slotsByTeacher = slots.stream()
            .filter(slot -> slot.getTeacher() != null)
            .collect(Collectors.groupingBy(slot -> slot.getTeacher().getId()));

        for (Map.Entry<Long, List<ScheduleSlot>> entry : slotsByTeacher.entrySet()) {
            Teacher teacher = entry.getValue().get(0).getTeacher();
            List<ScheduleSlot> teacherSlots = entry.getValue();
            // Field not available on SIS Teacher entity — uses default max of 4 consecutive hours
            Integer maxConsecutive = 4;

            // Sort by day and time
            teacherSlots.sort((s1, s2) -> {
                int dayCompare = s1.getDayOfWeek().compareTo(s2.getDayOfWeek());
                if (dayCompare != 0) return dayCompare;
                return s1.getStartTime().compareTo(s2.getStartTime());
            });

            // Track consecutive classes
            int consecutiveCount = 1;
            List<ScheduleSlot> consecutiveSlots = new ArrayList<>();
            consecutiveSlots.add(teacherSlots.get(0));

            for (int i = 0; i < teacherSlots.size() - 1; i++) {
                ScheduleSlot current = teacherSlots.get(i);
                ScheduleSlot next = teacherSlots.get(i + 1);

                // Check if on same day and back-to-back
                if (current.getDayOfWeek().equals(next.getDayOfWeek()) &&
                    current.getEndTime() != null && next.getStartTime() != null &&
                    current.getEndTime().equals(next.getStartTime())) {

                    consecutiveCount++;
                    consecutiveSlots.add(next);

                    // Check if exceeded max consecutive
                    if (consecutiveCount > maxConsecutive) {
                        Conflict conflict = createConflict(
                            ConflictType.TEACHER_OVERLOAD,
                            ConflictSeverity.MEDIUM,
                            "Excessive Consecutive Classes",
                            String.format("Teacher %s has %d consecutive classes on %s (max: %d)",
                                teacher.getName(),
                                consecutiveCount,
                                current.getDayOfWeek(),
                                maxConsecutive),
                            schedule
                        );
                        consecutiveSlots.forEach(conflict.getAffectedSlots()::add);
                        conflict.getAffectedTeachers().add(teacher);
                        conflicts.add(conflict);
                        break; // Only report once per sequence
                    }
                } else {
                    // Reset counter
                    consecutiveCount = 1;
                    consecutiveSlots.clear();
                    consecutiveSlots.add(next);
                }
            }
        }

        log.debug("Found {} excessive consecutive class conflicts", conflicts.size());
        return conflicts;
    }

    // ========================================================================
    // ROOM-BASED DETECTION
    // ========================================================================

    @Override
    public List<Conflict> detectRoomDoubleBookings(Schedule schedule) {
        log.debug("Detecting room double bookings for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .collect(Collectors.toList());

        // Group by room
        Map<Long, List<ScheduleSlot>> slotsByRoom = slots.stream()
            .filter(slot -> slot.getRoom() != null)
            .collect(Collectors.groupingBy(slot -> slot.getRoom().getId()));

        for (Map.Entry<Long, List<ScheduleSlot>> entry : slotsByRoom.entrySet()) {
            List<ScheduleSlot> roomSlots = entry.getValue();

            // Check for overlapping times
            for (int i = 0; i < roomSlots.size(); i++) {
                for (int j = i + 1; j < roomSlots.size(); j++) {
                    ScheduleSlot slot1 = roomSlots.get(i);
                    ScheduleSlot slot2 = roomSlots.get(j);

                    if (sameDayAndTimeOverlap(slot1, slot2)) {
                        Conflict conflict = createConflict(
                            ConflictType.ROOM_DOUBLE_BOOKING,
                            ConflictSeverity.CRITICAL,
                            "Room Double-Booked",
                            String.format("Room %s is assigned to multiple classes at the same time",
                                slot1.getRoom().getRoomNumber()),
                            schedule
                        );
                        conflict.getAffectedSlots().add(slot1);
                        conflict.getAffectedSlots().add(slot2);
                        conflict.getAffectedRooms().add(slot1.getRoom());
                        conflicts.add(conflict);
                    }
                }
            }
        }

        return conflicts;
    }

    @Override
    public List<Conflict> detectRoomCapacityViolations(Schedule schedule) {
        log.debug("Detecting room capacity violations for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .collect(Collectors.toList());

        for (ScheduleSlot slot : slots) {
            if (slot.getRoom() != null && slot.getCourse() != null) {
                int enrollmentCount = getEnrollmentCount(slot);

                if (enrollmentCount > slot.getRoom().getCapacity()) {
                    Conflict conflict = createConflict(
                        ConflictType.ROOM_CAPACITY_EXCEEDED,
                        ConflictSeverity.HIGH,
                        "Room Capacity Exceeded",
                        String.format("Room %s (capacity %d) has %d students enrolled in %s",
                            slot.getRoom().getRoomNumber(),
                            slot.getRoom().getCapacity(),
                            enrollmentCount,
                            slot.getCourse().getCourseName()),
                        schedule
                    );
                    conflict.getAffectedSlots().add(slot);
                    conflict.getAffectedRooms().add(slot.getRoom());
                    conflict.getAffectedCourses().add(slot.getCourse());
                    conflicts.add(conflict);
                }
            }
        }

        return conflicts;
    }

    @Override
    public List<Conflict> detectRoomTypeMismatches(Schedule schedule) {
        log.debug("Detecting room type mismatches for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all schedule slots for this schedule
        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .filter(slot -> slot.getRoom() != null && slot.getCourse() != null)
            .collect(Collectors.toList());

        for (ScheduleSlot slot : slots) {
            Room room = slot.getRoom();
            Course course = slot.getCourse();

            // Check for lab requirements
            if (course.isRequiresLab()) {
                RoomType roomType = room.getRoomType();

                // If course requires lab but room is not a lab
                if (roomType != null && roomType != RoomType.LAB &&
                    roomType != RoomType.COMPUTER_LAB && roomType != RoomType.SCIENCE_LAB) {

                    Conflict conflict = createConflict(
                        ConflictType.ROOM_DOUBLE_BOOKING, // Using closest available type
                        ConflictSeverity.MEDIUM,
                        "Room Type Mismatch",
                        String.format("Course %s requires a lab but assigned to %s (Type: %s)",
                            course.getCourseName(),
                            room.getRoomNumber(),
                            roomType),
                        schedule
                    );
                    conflict.getAffectedSlots().add(slot);
                    conflict.getAffectedRooms().add(room);
                    conflict.getAffectedCourses().add(course);
                    conflicts.add(conflict);
                }
            }

            // Check for subject-specific room requirements
            if (course.getSubject() != null) {
                RoomType roomType = room.getRoomType();
                String subject = course.getSubject().toLowerCase();

                // Science courses should be in science labs
                if ((subject.contains("science") || subject.contains("chemistry") ||
                     subject.contains("physics") || subject.contains("biology")) &&
                    roomType != null && roomType != RoomType.SCIENCE_LAB && roomType != RoomType.LAB) {

                    Conflict conflict = createConflict(
                        ConflictType.ROOM_DOUBLE_BOOKING,
                        ConflictSeverity.LOW,
                        "Subject Room Mismatch",
                        String.format("%s course should be in a science lab (currently: %s)",
                            course.getCourseName(),
                            room.getRoomNumber()),
                        schedule
                    );
                    conflict.getAffectedSlots().add(slot);
                    conflict.getAffectedRooms().add(room);
                    conflict.getAffectedCourses().add(course);
                    conflicts.add(conflict);
                }

                // Computer courses should be in computer labs
                if ((subject.contains("computer") || subject.contains("programming") ||
                     subject.contains("technology")) &&
                    roomType != null && roomType != RoomType.COMPUTER_LAB) {

                    Conflict conflict = createConflict(
                        ConflictType.ROOM_DOUBLE_BOOKING,
                        ConflictSeverity.LOW,
                        "Subject Room Mismatch",
                        String.format("%s course should be in a computer lab (currently: %s)",
                            course.getCourseName(),
                            room.getRoomNumber()),
                        schedule
                    );
                    conflict.getAffectedSlots().add(slot);
                    conflict.getAffectedRooms().add(room);
                    conflict.getAffectedCourses().add(course);
                    conflicts.add(conflict);
                }
            }
        }

        log.debug("Found {} room type mismatch conflicts", conflicts.size());
        return conflicts;
    }

    @Override
    public List<Conflict> detectEquipmentUnavailability(Schedule schedule) {
        log.debug("Detecting equipment unavailability for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all schedule slots for this schedule
        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .filter(slot -> slot.getRoom() != null && slot.getCourse() != null)
            .collect(Collectors.toList());

        for (ScheduleSlot slot : slots) {
            Room room = slot.getRoom();
            Course course = slot.getCourse();

            // Field not available on SIS Course entity — resource/equipment check disabled
            // Would need to fetch from SIS API or store separately
            // Commenting out equipment check for now
            /*
            // Check for required resources
            if (course.getRequiredResources() != null && !course.getRequiredResources().isEmpty()) {
                String resources = course.getRequiredResources().toLowerCase();

                // Check for projector
                if (resources.contains("projector") && !room.getHasProjector()) {
                    Conflict conflict = createConflict(
                        ConflictType.ROOM_DOUBLE_BOOKING, // Using closest available type
                        ConflictSeverity.LOW,
                        "Equipment Unavailable",
                        String.format("Course %s requires projector but room %s doesn't have one",
                            course.getCourseName(),
                            room.getRoomNumber()),
                        schedule
                    );
                    conflict.getAffectedSlots().add(slot);
                    conflict.getAffectedRooms().add(room);
                    conflict.getAffectedCourses().add(course);
                    conflicts.add(conflict);
                }

                // Check for smartboard
                if (resources.contains("smartboard") && !room.getHasSmartboard()) {
                    Conflict conflict = createConflict(
                        ConflictType.ROOM_DOUBLE_BOOKING,
                        ConflictSeverity.LOW,
                        "Equipment Unavailable",
                        String.format("Course %s requires smartboard but room %s doesn't have one",
                            course.getCourseName(),
                            room.getRoomNumber()),
                        schedule
                    );
                    conflict.getAffectedSlots().add(slot);
                    conflict.getAffectedRooms().add(room);
                    conflict.getAffectedCourses().add(course);
                    conflicts.add(conflict);
                }

                // Check for computers
                if (resources.contains("computer") && !room.getHasComputers()) {
                    Conflict conflict = createConflict(
                        ConflictType.ROOM_DOUBLE_BOOKING,
                        ConflictSeverity.MEDIUM,
                        "Equipment Unavailable",
                        String.format("Course %s requires computers but room %s doesn't have them",
                            course.getCourseName(),
                            room.getRoomNumber()),
                        schedule
                    );
                    conflict.getAffectedSlots().add(slot);
                    conflict.getAffectedRooms().add(room);
                    conflict.getAffectedCourses().add(course);
                    conflicts.add(conflict);
                }
            }
            */
        }

        log.debug("Found {} equipment unavailability conflicts", conflicts.size());
        return conflicts;
    }

    // ========================================================================
    // TEACHER-BASED DETECTION
    // ========================================================================

    @Override
    public List<Conflict> detectTeacherOverloads(Schedule schedule) {
        log.debug("Detecting teacher overloads for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .collect(Collectors.toList());

        // Group by teacher
        Map<Long, List<ScheduleSlot>> slotsByTeacher = slots.stream()
            .filter(slot -> slot.getTeacher() != null)
            .collect(Collectors.groupingBy(slot -> slot.getTeacher().getId()));

        for (Map.Entry<Long, List<ScheduleSlot>> entry : slotsByTeacher.entrySet()) {
            List<ScheduleSlot> teacherSlots = entry.getValue();

            // Check for overlapping times
            for (int i = 0; i < teacherSlots.size(); i++) {
                for (int j = i + 1; j < teacherSlots.size(); j++) {
                    ScheduleSlot slot1 = teacherSlots.get(i);
                    ScheduleSlot slot2 = teacherSlots.get(j);

                    if (sameDayAndTimeOverlap(slot1, slot2)) {
                        Conflict conflict = createConflict(
                            ConflictType.TEACHER_OVERLOAD,
                            ConflictSeverity.CRITICAL,
                            "Teacher Double-Booked",
                            String.format("Teacher %s is assigned to multiple classes at the same time",
                                slot1.getTeacher().getName()),
                            schedule
                        );
                        conflict.getAffectedSlots().add(slot1);
                        conflict.getAffectedSlots().add(slot2);
                        conflict.getAffectedTeachers().add(slot1.getTeacher());
                        conflicts.add(conflict);
                    }
                }
            }
        }

        return conflicts;
    }

    @Override
    public List<Conflict> detectExcessiveTeachingHours(Schedule schedule) {
        log.debug("Detecting excessive teaching hours for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all schedule slots for this schedule
        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .collect(Collectors.toList());

        // Group by teacher
        Map<Long, List<ScheduleSlot>> slotsByTeacher = slots.stream()
            .filter(slot -> slot.getTeacher() != null)
            .collect(Collectors.groupingBy(slot -> slot.getTeacher().getId()));

        for (Map.Entry<Long, List<ScheduleSlot>> entry : slotsByTeacher.entrySet()) {
            Teacher teacher = entry.getValue().get(0).getTeacher();
            List<ScheduleSlot> teacherSlots = entry.getValue();

            // Calculate total teaching periods per day
            Map<DayOfWeek, Long> periodsPerDay = teacherSlots.stream()
                .filter(slot -> slot.getDayOfWeek() != null)
                .collect(Collectors.groupingBy(
                    ScheduleSlot::getDayOfWeek,
                    Collectors.counting()
                ));

            // Check against max periods per day
            Integer maxPeriodsPerDay = teacher.getMaxPeriodsPerDay() != null ? teacher.getMaxPeriodsPerDay() : 7;

            for (Map.Entry<DayOfWeek, Long> dayEntry : periodsPerDay.entrySet()) {
                if (dayEntry.getValue() > maxPeriodsPerDay) {
                    Conflict conflict = createConflict(
                        ConflictType.TEACHER_OVERLOAD,
                        ConflictSeverity.HIGH,
                        "Excessive Teaching Hours",
                        String.format("Teacher %s has %d periods on %s (max: %d)",
                            teacher.getName(),
                            dayEntry.getValue(),
                            dayEntry.getKey(),
                            maxPeriodsPerDay),
                        schedule
                    );
                    conflict.getAffectedTeachers().add(teacher);
                    conflicts.add(conflict);
                }
            }
        }

        log.debug("Found {} excessive teaching hours conflicts", conflicts.size());
        return conflicts;
    }

    @Override
    public List<Conflict> detectMissingPreparationPeriods(Schedule schedule) {
        log.debug("Detecting missing preparation periods for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all schedule slots for this schedule
        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .collect(Collectors.toList());

        // Group by teacher and day
        Map<Long, Map<DayOfWeek, List<ScheduleSlot>>> slotsByTeacherAndDay = slots.stream()
            .filter(slot -> slot.getTeacher() != null && slot.getDayOfWeek() != null)
            .collect(Collectors.groupingBy(
                slot -> slot.getTeacher().getId(),
                Collectors.groupingBy(ScheduleSlot::getDayOfWeek)
            ));

        for (Map.Entry<Long, Map<DayOfWeek, List<ScheduleSlot>>> teacherEntry : slotsByTeacherAndDay.entrySet()) {
            Teacher teacher = sisDataService.getTeacherById(teacherEntry.getKey()).orElse(null);
            if (teacher == null) continue;

            for (Map.Entry<DayOfWeek, List<ScheduleSlot>> dayEntry : teacherEntry.getValue().entrySet()) {
                List<ScheduleSlot> daySlots = dayEntry.getValue();

                // Check if teacher has at least one prep period per day (usually 1 out of 7-8 periods)
                // A full day is typically 7-8 periods, so if teaching 7+ periods, no prep time
                if (daySlots.size() >= 7) {
                    Conflict conflict = createConflict(
                        ConflictType.TEACHER_OVERLOAD,
                        ConflictSeverity.MEDIUM,
                        "Missing Preparation Period",
                        String.format("Teacher %s has %d consecutive teaching periods on %s with no prep time",
                            teacher.getName(),
                            daySlots.size(),
                            dayEntry.getKey()),
                        schedule
                    );
                    conflict.getAffectedTeachers().add(teacher);
                    conflicts.add(conflict);
                }
            }
        }

        log.debug("Found {} missing preparation period conflicts", conflicts.size());
        return conflicts;
    }

    @Override
    public List<Conflict> detectSubjectMismatches(Schedule schedule) {
        log.debug("Detecting subject mismatches for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all schedule slots for this schedule
        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .filter(slot -> slot.getTeacher() != null && slot.getCourse() != null)
            .collect(Collectors.toList());

        for (ScheduleSlot slot : slots) {
            Teacher teacher = slot.getTeacher();
            Course course = slot.getCourse();

            // Check if teacher's department matches course subject
            if (teacher.getDepartment() != null && course.getSubject() != null) {
                String teacherDept = teacher.getDepartment().toLowerCase().trim();
                String courseSubject = course.getSubject().toLowerCase().trim();

                // Simple matching - more sophisticated matching would use certification data
                if (!teacherDept.contains(courseSubject) && !courseSubject.contains(teacherDept)) {
                    Conflict conflict = createConflict(
                        ConflictType.TEACHER_OVERLOAD, // Using closest available type
                        ConflictSeverity.LOW,
                        "Subject Mismatch",
                        String.format("Teacher %s (Dept: %s) assigned to %s (Subject: %s)",
                            teacher.getName(),
                            teacher.getDepartment(),
                            course.getCourseName(),
                            course.getSubject()),
                        schedule
                    );
                    conflict.getAffectedSlots().add(slot);
                    conflict.getAffectedTeachers().add(teacher);
                    conflict.getAffectedCourses().add(course);
                    conflicts.add(conflict);
                }
            }
        }

        log.debug("Found {} subject mismatch conflicts", conflicts.size());
        return conflicts;
    }

    @Override
    public List<Conflict> detectTeacherTravelTimeIssues(Schedule schedule) {
        log.debug("Detecting teacher travel time issues for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all schedule slots for this schedule
        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .collect(Collectors.toList());

        // Group by teacher
        Map<Long, List<ScheduleSlot>> slotsByTeacher = slots.stream()
            .filter(slot -> slot.getTeacher() != null)
            .collect(Collectors.groupingBy(slot -> slot.getTeacher().getId()));

        for (Map.Entry<Long, List<ScheduleSlot>> entry : slotsByTeacher.entrySet()) {
            List<ScheduleSlot> teacherSlots = entry.getValue();

            // Sort by day and time
            teacherSlots.sort((s1, s2) -> {
                int dayCompare = s1.getDayOfWeek().compareTo(s2.getDayOfWeek());
                if (dayCompare != 0) return dayCompare;
                return s1.getStartTime().compareTo(s2.getStartTime());
            });

            // Check for back-to-back classes in different buildings
            for (int i = 0; i < teacherSlots.size() - 1; i++) {
                ScheduleSlot current = teacherSlots.get(i);
                ScheduleSlot next = teacherSlots.get(i + 1);

                // Check if classes are on same day and consecutive
                if (current.getDayOfWeek().equals(next.getDayOfWeek()) &&
                    current.getRoom() != null && next.getRoom() != null &&
                    current.getRoom().getBuilding() != null && next.getRoom().getBuilding() != null) {

                    // Check if in different buildings
                    if (!current.getRoom().getBuilding().equals(next.getRoom().getBuilding())) {
                        // Check if end time of current is same as start time of next (back-to-back)
                        if (current.getEndTime() != null && next.getStartTime() != null &&
                            current.getEndTime().equals(next.getStartTime())) {

                            Conflict conflict = createConflict(
                                ConflictType.TEACHER_OVERLOAD, // Using closest available type
                                ConflictSeverity.LOW,
                                "Building Travel Time Issue",
                                String.format("Teacher %s has back-to-back classes in %s and %s on %s",
                                    current.getTeacher().getName(),
                                    current.getRoom().getBuilding(),
                                    next.getRoom().getBuilding(),
                                    current.getDayOfWeek()),
                                schedule
                            );
                            conflict.getAffectedSlots().add(current);
                            conflict.getAffectedSlots().add(next);
                            conflict.getAffectedTeachers().add(current.getTeacher());
                            conflicts.add(conflict);
                        }
                    }
                }
            }
        }

        log.debug("Found {} teacher travel time issues", conflicts.size());
        return conflicts;
    }

    // ========================================================================
    // STUDENT-BASED DETECTION
    // ========================================================================

    @Override
    public List<Conflict> detectStudentScheduleConflicts(Schedule schedule) {
        log.debug("Detecting student schedule conflicts for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all enrollments for this schedule
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByScheduleId(schedule.getId());

        // Group by student
        Map<Long, List<StudentEnrollment>> enrollmentsByStudent = enrollments.stream()
            .filter(e -> e.getStudent() != null && e.getScheduleSlot() != null && e.isActive())
            .collect(Collectors.groupingBy(e -> e.getStudent().getId()));

        // Check for time conflicts for each student
        for (Map.Entry<Long, List<StudentEnrollment>> entry : enrollmentsByStudent.entrySet()) {
            List<StudentEnrollment> studentEnrollments = entry.getValue();

            // Check each pair of enrollments for this student
            for (int i = 0; i < studentEnrollments.size(); i++) {
                for (int j = i + 1; j < studentEnrollments.size(); j++) {
                    StudentEnrollment enrollment1 = studentEnrollments.get(i);
                    StudentEnrollment enrollment2 = studentEnrollments.get(j);

                    ScheduleSlot slot1 = enrollment1.getScheduleSlot();
                    ScheduleSlot slot2 = enrollment2.getScheduleSlot();

                    if (sameDayAndTimeOverlap(slot1, slot2)) {
                        Conflict conflict = createConflict(
                            ConflictType.TEACHER_OVERLOAD, // Using closest available type
                            ConflictSeverity.CRITICAL,
                            "Student Schedule Conflict",
                            String.format("Student %s is enrolled in %s and %s at the same time",
                                enrollment1.getStudentName(),
                                enrollment1.getCourseName(),
                                enrollment2.getCourseName()),
                            schedule
                        );

                        conflict.getAffectedSlots().add(slot1);
                        conflict.getAffectedSlots().add(slot2);
                        if (enrollment1.getCourse() != null) {
                            conflict.getAffectedCourses().add(enrollment1.getCourse());
                        }
                        if (enrollment2.getCourse() != null) {
                            conflict.getAffectedCourses().add(enrollment2.getCourse());
                        }
                        conflicts.add(conflict);
                    }
                }
            }
        }

        log.debug("Found {} student schedule conflicts", conflicts.size());
        return conflicts;
    }

    @Override
    public List<Conflict> detectPrerequisiteViolations(Schedule schedule) {
        log.debug("Detecting prerequisite violations for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all enrollments for this schedule
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByScheduleId(schedule.getId());

        // Field not available on SIS Course entity — prerequisite check disabled
        // Would need to fetch from SIS API
        // Commenting out prerequisite check for now
        /*
        for (StudentEnrollment enrollment : enrollments) {
            if (enrollment.getCourse() != null && enrollment.getCourse().getPrerequisites() != null &&
                !enrollment.getCourse().getPrerequisites().isEmpty()) {

                // Check if student has completed prerequisites
                // Note: This is a simplified check - full implementation would need
                // student transcript data to verify prerequisite completion

                String prerequisites = enrollment.getCourse().getPrerequisites();

                Conflict conflict = createConflict(
                    ConflictType.TEACHER_OVERLOAD, // Using closest available type
                    ConflictSeverity.MEDIUM,
                    "Potential Prerequisite Violation",
                    String.format("Student %s enrolled in %s which requires: %s",
                        enrollment.getStudentName(),
                        enrollment.getCourseName(),
                        prerequisites),
                    schedule
                );

                if (enrollment.getCourse() != null) {
                    conflict.getAffectedCourses().add(enrollment.getCourse());
                }
                // Note: This creates a warning for all courses with prerequisites
                // A full implementation would verify actual completion
                // conflicts.add(conflict);
            }
        }
        */

        log.debug("Found {} prerequisite violation warnings", conflicts.size());
        return conflicts;
    }

    @Override
    public List<Conflict> detectCreditHourViolations(Schedule schedule) {
        log.debug("Detecting credit hour violations for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Note: This would require credit hour tracking per course and student max/min credit rules
        // Future enhancement: Add creditHours field to Course entity
        // Add minCredits/maxCredits fields to Student entity

        return conflicts;
    }

    @Override
    public List<Conflict> detectGraduationRequirementIssues(Schedule schedule) {
        log.debug("Detecting graduation requirement issues for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Note: This would require graduation requirement tracking and student progress data
        // Future enhancement: Add GraduationRequirement entity
        // Add StudentProgress entity to track requirement completion

        return conflicts;
    }

    @Override
    public List<Conflict> detectCourseSequenceViolations(Schedule schedule) {
        log.debug("Detecting course sequence violations for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Note: This would require course sequence rules (e.g., Algebra I before Algebra II)
        // Future enhancement: Add CourseSequence entity to define proper ordering

        return conflicts;
    }

    // ========================================================================
    // COURSE-BASED DETECTION
    // ========================================================================

    @Override
    public List<Conflict> detectSectionOverEnrollment(Schedule schedule) {
        log.debug("Detecting section over-enrollment for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all schedule slots for this schedule
        List<ScheduleSlot> slots = scheduleSlotRepository.findAll().stream()
            .filter(slot -> slot.getSchedule() != null && slot.getSchedule().getId().equals(schedule.getId()))
            .collect(Collectors.toList());

        for (ScheduleSlot slot : slots) {
            int enrollmentCount = getEnrollmentCount(slot);

            // Check against room capacity
            if (slot.getRoom() != null && enrollmentCount > slot.getRoom().getCapacity()) {
                Conflict conflict = createConflict(
                    ConflictType.ROOM_CAPACITY_EXCEEDED,
                    ConflictSeverity.HIGH,
                    "Section Over-Enrolled (Room Capacity)",
                    String.format("Section has %d students but room %s only holds %d",
                        enrollmentCount, slot.getRoom().getRoomNumber(), slot.getRoom().getCapacity()),
                    schedule
                );
                conflict.getAffectedSlots().add(slot);
                conflict.getAffectedRooms().add(slot.getRoom());
                if (slot.getCourse() != null) {
                    conflict.getAffectedCourses().add(slot.getCourse());
                }
                conflicts.add(conflict);
            }

            // Check against course max students
            if (slot.getCourse() != null && slot.getCourse().getMaxStudents() != null &&
                enrollmentCount > slot.getCourse().getMaxStudents()) {

                Conflict conflict = createConflict(
                    ConflictType.ROOM_CAPACITY_EXCEEDED,
                    ConflictSeverity.HIGH,
                    "Section Over-Enrolled (Course Limit)",
                    String.format("Course %s has %d students enrolled but max is %d",
                        slot.getCourse().getCourseName(), enrollmentCount, slot.getCourse().getMaxStudents()),
                    schedule
                );
                conflict.getAffectedSlots().add(slot);
                conflict.getAffectedCourses().add(slot.getCourse());
                conflicts.add(conflict);
            }
        }

        log.debug("Found {} over-enrollment conflicts", conflicts.size());
        return conflicts;
    }

    @Override
    public List<Conflict> detectSectionUnderEnrollment(Schedule schedule) {
        log.debug("Detecting section under-enrollment for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all course sections for courses in this schedule
        List<CourseSection> sections = courseSectionRepository.findAll().stream()
            .filter(section -> section.getScheduleYear() != null)
            .collect(Collectors.toList());

        for (CourseSection section : sections) {
            // Check if current enrollment is below minimum
            if (section.getMinEnrollment() != null && section.getCurrentEnrollment() != null &&
                section.getCurrentEnrollment() < section.getMinEnrollment()) {

                Conflict conflict = createConflict(
                    ConflictType.ROOM_CAPACITY_EXCEEDED, // Using closest available type
                    ConflictSeverity.MEDIUM,
                    "Section Under-Enrolled",
                    String.format("Section %s of %s has only %d students (minimum: %d)",
                        section.getSectionNumber(),
                        section.getCourse() != null ? section.getCourse().getCourseName() : "Unknown",
                        section.getCurrentEnrollment(),
                        section.getMinEnrollment()),
                    schedule
                );

                if (section.getCourse() != null) {
                    conflict.getAffectedCourses().add(section.getCourse());
                }
                conflicts.add(conflict);
            }
        }

        log.debug("Found {} under-enrollment conflicts", conflicts.size());
        return conflicts;
    }

    @Override
    public List<Conflict> detectDuplicateEnrollments(Schedule schedule) {
        log.debug("Detecting duplicate enrollments for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Get all enrollments for this schedule
        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findByScheduleId(schedule.getId());

        // Group by student and course to find duplicates
        Map<String, List<StudentEnrollment>> enrollmentMap = new HashMap<>();

        for (StudentEnrollment enrollment : enrollments) {
            if (enrollment.getStudent() != null && enrollment.getCourse() != null && enrollment.isActive()) {
                String key = enrollment.getStudent().getId() + "_" + enrollment.getCourse().getId();
                enrollmentMap.computeIfAbsent(key, k -> new ArrayList<>()).add(enrollment);
            }
        }

        // Check for duplicates
        for (Map.Entry<String, List<StudentEnrollment>> entry : enrollmentMap.entrySet()) {
            List<StudentEnrollment> studentEnrollments = entry.getValue();

            if (studentEnrollments.size() > 1) {
                StudentEnrollment first = studentEnrollments.get(0);

                Conflict conflict = createConflict(
                    ConflictType.TEACHER_OVERLOAD, // Using closest available type
                    ConflictSeverity.HIGH,
                    "Duplicate Student Enrollment",
                    String.format("Student %s is enrolled %d times in %s",
                        first.getStudentName(),
                        studentEnrollments.size(),
                        first.getCourseName()),
                    schedule
                );

                if (first.getCourse() != null) {
                    conflict.getAffectedCourses().add(first.getCourse());
                }
                conflicts.add(conflict);
            }
        }

        log.debug("Found {} duplicate enrollment conflicts", conflicts.size());
        return conflicts;
    }

    @Override
    public List<Conflict> detectCoRequisiteViolations(Schedule schedule) {
        log.debug("Detecting co-requisite violations for schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = new ArrayList<>();

        // Note: This would require co-requisite tracking in the Course entity
        // For now, return empty list as co-requisites are not yet tracked
        // Future enhancement: Add co-requisites field to Course entity

        return conflicts;
    }

    // ========================================================================
    // REAL-TIME DETECTION
    // ========================================================================

    @Override
    public List<Conflict> detectPotentialConflicts(Schedule schedule, ScheduleSlot potentialSlot) {
        log.debug("Detecting potential conflicts for new slot");
        return detectConflictsForSlot(potentialSlot);
    }

    @Override
    public ValidationResult validateSchedule(Schedule schedule) {
        log.info("Validating schedule: {}", schedule.getScheduleName());
        List<Conflict> conflicts = detectAllConflicts(schedule);
        return new ValidationResult(conflicts);
    }

    // ========================================================================
    // CONFLICT PERSISTENCE
    // ========================================================================

    @Override
    public List<Conflict> saveConflicts(List<Conflict> conflicts) {
        log.info("Saving {} conflicts to database", conflicts.size());
        return conflictRepository.saveAll(conflicts);
    }

    @Override
    public void clearConflicts(Schedule schedule) {
        log.info("Clearing conflicts for schedule: {}", schedule.getScheduleName());
        conflictRepository.deleteBySchedule(schedule);
    }

    @Override
    public List<Conflict> refreshConflicts(Schedule schedule) {
        log.info("Refreshing conflicts for schedule: {}", schedule.getScheduleName());
        clearConflicts(schedule);
        List<Conflict> newConflicts = detectAllConflicts(schedule);
        return saveConflicts(newConflicts);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private Conflict createConflict(ConflictType type, ConflictSeverity severity,
                                    String title, String description, Schedule schedule) {
        return Conflict.builder()
            .conflictType(type)
            .severity(severity)
            .category(type.getCategory())
            .title(title)
            .description(description)
            .schedule(schedule)
            .detectionMethod("auto")
            .build();
    }

    private boolean timesOverlap(ScheduleSlot slot1, ScheduleSlot slot2) {
        if (slot1.getStartTime() == null || slot1.getEndTime() == null ||
            slot2.getStartTime() == null || slot2.getEndTime() == null) {
            return false;
        }

        LocalTime start1 = slot1.getStartTime();
        LocalTime end1 = slot1.getEndTime();
        LocalTime start2 = slot2.getStartTime();
        LocalTime end2 = slot2.getEndTime();

        return start1.isBefore(end2) && start2.isBefore(end1);
    }

    private boolean sameDayAndTimeOverlap(ScheduleSlot slot1, ScheduleSlot slot2) {
        if (slot1.getDayOfWeek() == null || slot2.getDayOfWeek() == null ||
            !slot1.getDayOfWeek().equals(slot2.getDayOfWeek())) {
            return false;
        }

        return timesOverlap(slot1, slot2);
    }

    private List<ScheduleSlot> findOverlappingSlots(ScheduleSlot slot) {
        if (slot.getSchedule() == null) {
            return new ArrayList<>();
        }

        List<ScheduleSlot> allSlots = scheduleSlotRepository.findAll().stream()
            .filter(s -> s.getSchedule() != null && s.getSchedule().getId().equals(slot.getSchedule().getId()))
            .collect(Collectors.toList());

        return allSlots.stream()
            .filter(s -> !s.getId().equals(slot.getId()))
            .filter(s -> sameDayAndTimeOverlap(slot, s))
            .collect(Collectors.toList());
    }

    /**
     * Get actual enrollment count for a schedule slot
     * @param slot the schedule slot to check
     * @return number of students enrolled in this slot
     */
    private int getEnrollmentCount(ScheduleSlot slot) {
        if (slot == null || slot.getId() == null) {
            return 0;
        }

        // Count active enrollments for this schedule slot
        long count = studentEnrollmentRepository.findByScheduleSlotId(slot.getId()).stream()
            .filter(StudentEnrollment::isActive)
            .count();

        return (int) count;
    }
}
