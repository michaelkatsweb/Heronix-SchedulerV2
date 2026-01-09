package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.ScheduleSlot;
import com.heronix.scheduler.repository.ScheduleSlotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Schedule Slot Service Implementation
 * Location: src/main/java/com/eduscheduler/service/ScheduleSlotService.java
 * 
 * ✅ FIXED: Added save() method to resolve compilation errors
 * 
 * @author Heronix Scheduling System Team
 * @version 3.1.0 - COMPILATION FIX
 * @since 2025-10-31
 */
@Slf4j
@Service
@Transactional
public class ScheduleSlotService {

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    /**
     * Save a schedule slot (create or update)
     * 
     * ✅ NEW METHOD - Fixes compilation errors at lines 538, 920, 921, 944
     * 
     * @param slot The slot to save
     * @return The saved slot
     */
    @Transactional
    public ScheduleSlot save(ScheduleSlot slot) {
        log.debug("Saving schedule slot: {}", slot.getId());
        
        try {
            ScheduleSlot saved = scheduleSlotRepository.save(slot);
            log.info("✓ Saved schedule slot: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("✗ Failed to save schedule slot", e);
            throw new RuntimeException("Failed to save schedule slot: " + e.getMessage(), e);
        }
    }

    /**
     * Get all schedule slots for a specific schedule
     * 
     * ✅ PHASE 1 FIX: Uses JOIN FETCH query to eagerly load all associations
     * This prevents LazyInitializationException when accessing students, teacher,
     * etc.
     * 
     * @param scheduleId The schedule ID
     * @return List of fully loaded schedule slots
     */
    @Transactional(readOnly = true)
    public List<ScheduleSlot> getSlotsBySchedule(Long scheduleId) {
        log.debug("Fetching slots for schedule ID: {}", scheduleId);

        try {
            List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleIdWithDetails(scheduleId);

            log.info("✓ Loaded {} schedule slots (with eager associations)", slots.size());

            if (!slots.isEmpty()) {
                long slotsWithTeacher = slots.stream().filter(s -> s.getTeacher() != null).count();
                long slotsWithCourse = slots.stream().filter(s -> s.getCourse() != null).count();
                long slotsWithRoom = slots.stream().filter(s -> s.getRoom() != null).count();

                log.debug("  - {} slots have teachers", slotsWithTeacher);
                log.debug("  - {} slots have courses", slotsWithCourse);
                log.debug("  - {} slots have rooms", slotsWithRoom);
            }

            return slots;

        } catch (Exception e) {
            log.error("✗ Failed to load schedule slots for schedule {}", scheduleId, e);
            return new ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlot> findBySchedule(com.heronix.scheduler.model.domain.Schedule schedule) {
        if (schedule == null) {
            log.warn("⚠️ findBySchedule called with null schedule");
            return new ArrayList<>();
        }

        if (schedule.getId() == null) {
            log.warn("⚠️ findBySchedule called with schedule that has no ID");
            return new ArrayList<>();
        }

        log.debug("Finding slots for schedule: {} (ID: {})", schedule.getName(), schedule.getId());
        return getSlotsBySchedule(schedule.getId());
    }

    @Transactional(readOnly = true)
    public Optional<ScheduleSlot> getScheduleSlotById(Long id) {
        log.debug("Fetching schedule slot: {}", id);
        return scheduleSlotRepository.findById(id);
    }

    @Transactional
    public ScheduleSlot createScheduleSlot(ScheduleSlot slot) {
        log.info("Creating new schedule slot");

        try {
            ScheduleSlot saved = scheduleSlotRepository.save(slot);
            log.info("✓ Created schedule slot: {}", saved.getId());
            return saved;
        } catch (Exception e) {
            log.error("✗ Failed to create schedule slot", e);
            throw new RuntimeException("Failed to create schedule slot: " + e.getMessage(), e);
        }
    }

    @Transactional
    public ScheduleSlot updateScheduleSlot(Long id, ScheduleSlot slotDetails) {
        log.info("Updating schedule slot: {}", id);

        ScheduleSlot slot = scheduleSlotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Schedule slot not found: " + id));

        if (slotDetails.getTeacher() != null) {
            slot.setTeacher(slotDetails.getTeacher());
        }
        if (slotDetails.getCourse() != null) {
            slot.setCourse(slotDetails.getCourse());
        }
        if (slotDetails.getRoom() != null) {
            slot.setRoom(slotDetails.getRoom());
        }
        if (slotDetails.getDayOfWeek() != null) {
            slot.setDayOfWeek(slotDetails.getDayOfWeek());
        }
        if (slotDetails.getStartTime() != null) {
            slot.setStartTime(slotDetails.getStartTime());
        }
        if (slotDetails.getEndTime() != null) {
            slot.setEndTime(slotDetails.getEndTime());
        }
        if (slotDetails.getStudents() != null) {
            slot.setStudents(slotDetails.getStudents());
        }

        ScheduleSlot updated = scheduleSlotRepository.save(slot);
        log.info("✓ Updated schedule slot: {}", id);
        return updated;
    }

    @Transactional
    public void deleteScheduleSlot(Long id) {
        log.info("Deleting schedule slot: {}", id);

        if (!scheduleSlotRepository.existsById(id)) {
            throw new RuntimeException("Schedule slot not found: " + id);
        }

        scheduleSlotRepository.deleteById(id);
        log.info("✓ Deleted schedule slot: {}", id);
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlot> getAllScheduleSlots() {
        log.debug("Fetching all schedule slots");
        return scheduleSlotRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlot> getSlotsByDay(Long scheduleId, DayOfWeek day) {
        log.debug("Fetching slots for schedule {} on {}", scheduleId, day);

        return getSlotsBySchedule(scheduleId).stream()
                .filter(slot -> slot.getDayOfWeek() == day)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlot> getSlotsByTimeRange(Long scheduleId, LocalTime startTime, LocalTime endTime) {
        log.debug("Fetching slots for schedule {} between {} and {}", scheduleId, startTime, endTime);

        return getSlotsBySchedule(scheduleId).stream()
                .filter(slot -> slot.getStartTime() != null)
                .filter(slot -> !slot.getStartTime().isBefore(startTime))
                .filter(slot -> !slot.getStartTime().isAfter(endTime))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlot> getSlotsByTeacher(Long scheduleId, Long teacherId) {
        log.debug("Fetching slots for schedule {} and teacher {}", scheduleId, teacherId);

        return getSlotsBySchedule(scheduleId).stream()
                .filter(slot -> slot.getTeacher() != null)
                .filter(slot -> slot.getTeacher().getId().equals(teacherId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlot> getSlotsByRoom(Long scheduleId, Long roomId) {
        log.debug("Fetching slots for schedule {} and room {}", scheduleId, roomId);

        return getSlotsBySchedule(scheduleId).stream()
                .filter(slot -> slot.getRoom() != null)
                .filter(slot -> slot.getRoom().getId().equals(roomId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ScheduleSlot> getSlotsByCourse(Long scheduleId, Long courseId) {
        log.debug("Fetching slots for schedule {} and course {}", scheduleId, courseId);

        return getSlotsBySchedule(scheduleId).stream()
                .filter(slot -> slot.getCourse() != null)
                .filter(slot -> slot.getCourse().getId().equals(courseId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> detectConflicts(Long scheduleId) {
        log.debug("Detecting conflicts for schedule: {}", scheduleId);

        List<String> conflicts = new ArrayList<>();
        List<ScheduleSlot> slots = getSlotsBySchedule(scheduleId);

        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                ScheduleSlot slot1 = slots.get(i);
                ScheduleSlot slot2 = slots.get(j);

                if (hasConflictBetween(slot1, slot2)) {
                    String conflictDesc = String.format(
                            "Conflict between slot %d and %d: %s",
                            slot1.getId(),
                            slot2.getId(),
                            getConflictReason(slot1, slot2));
                    conflicts.add(conflictDesc);
                }
            }
        }

        log.info("Found {} conflicts in schedule {}", conflicts.size(), scheduleId);
        return conflicts;
    }

    @Transactional(readOnly = true)
    public boolean hasConflicts(ScheduleSlot slot) {
        if (slot.getSchedule() == null) {
            return false;
        }

        List<ScheduleSlot> allSlots = getSlotsBySchedule(slot.getSchedule().getId());

        return allSlots.stream()
                .filter(s -> !s.getId().equals(slot.getId()))
                .anyMatch(s -> hasConflictBetween(slot, s));
    }

    private boolean hasConflictBetween(ScheduleSlot slot1, ScheduleSlot slot2) {
        if (slot1.getDayOfWeek() == null || slot2.getDayOfWeek() == null) {
            return false;
        }

        if (slot1.getDayOfWeek() != slot2.getDayOfWeek()) {
            return false;
        }

        if (slot1.getStartTime() == null || slot1.getEndTime() == null ||
                slot2.getStartTime() == null || slot2.getEndTime() == null) {
            return false;
        }

        boolean timeOverlap = slot1.getStartTime().isBefore(slot2.getEndTime()) &&
                slot2.getStartTime().isBefore(slot1.getEndTime());

        if (!timeOverlap) {
            return false;
        }

        if (slot1.getTeacher() != null && slot2.getTeacher() != null &&
                slot1.getTeacher().getId().equals(slot2.getTeacher().getId())) {
            return true;
        }

        if (slot1.getRoom() != null && slot2.getRoom() != null &&
                slot1.getRoom().getId().equals(slot2.getRoom().getId())) {
            return true;
        }

        return false;
    }

    private String getConflictReason(ScheduleSlot slot1, ScheduleSlot slot2) {
        List<String> reasons = new ArrayList<>();

        if (slot1.getTeacher() != null && slot2.getTeacher() != null &&
                slot1.getTeacher().getId().equals(slot2.getTeacher().getId())) {
            reasons.add("Teacher " + slot1.getTeacher().getName());
        }

        if (slot1.getRoom() != null && slot2.getRoom() != null &&
                slot1.getRoom().getId().equals(slot2.getRoom().getId())) {
            reasons.add("Room " + slot1.getRoom().getRoomNumber());
        }

        return String.join(" and ", reasons);
    }

    @Transactional(readOnly = true)
    public int getConflictCount(Long scheduleId) {
        return detectConflicts(scheduleId).size();
    }

    @Transactional
    public int markConflictingSlots(Long scheduleId) {
        log.info("Marking conflicting slots for schedule {}", scheduleId);

        List<ScheduleSlot> slots = getSlotsBySchedule(scheduleId);
        int conflictCount = 0;

        for (ScheduleSlot slot : slots) {
            boolean hasConflict = slots.stream()
                    .filter(s -> !s.getId().equals(slot.getId()))
                    .anyMatch(s -> hasConflictBetween(slot, s));

            if (slot.getHasConflict() != hasConflict) {
                slot.setHasConflict(hasConflict);
                scheduleSlotRepository.save(slot);

                if (hasConflict) {
                    conflictCount++;
                }
            }
        }

        log.info("✓ Marked {} conflicting slots", conflictCount);
        return conflictCount;
    }
}