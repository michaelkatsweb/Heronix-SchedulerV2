package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.domain.ScheduleSlot;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.domain.Student;
import com.heronix.scheduler.model.enums.ScheduleStatus;
import com.heronix.scheduler.model.enums.SlotStatus;
import com.heronix.scheduler.model.enums.RoomType;
import com.heronix.scheduler.repository.ScheduleRepository;
import com.heronix.scheduler.repository.ScheduleSlotRepository;
import com.heronix.scheduler.repository.RoomRepository;
import com.heronix.scheduler.service.ScheduleService;
import com.heronix.scheduler.service.ScheduleGenerationService;
import com.heronix.scheduler.service.data.SISDataService;
import com.heronix.scheduler.model.dto.ScheduleGenerationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Schedule Service Implementation
 * Location:
 * src/main/java/com/eduscheduler/service/impl/ScheduleServiceImpl.java
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final SISDataService sisDataService;
    private final RoomRepository roomRepository;
    private final ScheduleGenerationService scheduleGenerationService;

    @Override
    public List<Schedule> getAllSchedules() {
        log.debug("Getting all schedules");
        return scheduleRepository.findAll();
    }

    @Override
    public Optional<Schedule> getScheduleById(Long id) {
        log.debug("Getting schedule by id: {}", id);
        return scheduleRepository.findById(id);
    }

    @Override
    @Transactional
    public Schedule saveSchedule(Schedule schedule) {
        log.info("Saving schedule: {}", schedule.getName());
        return scheduleRepository.saveAndFlush(schedule);
    }

    @Override
    @Transactional
    public void deleteSchedule(Long id) {
        log.info("Deleting schedule: {}", id);
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(id);
        scheduleSlotRepository.deleteAll(slots);
        scheduleRepository.deleteById(id);
    }

    @Override
    public List<Schedule> getActiveSchedules() {
        log.debug("Getting active schedules");
        return scheduleRepository.findAll().stream()
                .filter(s -> s.getStatus() != null && s.getStatus() != ScheduleStatus.ARCHIVED)
                .toList();
    }

    /**
     * Generate schedule with AI optimization
     */
    @Transactional
    public Schedule generateSchedule(Schedule schedule) {
        log.info("Generating schedule: {}", schedule.getName());

        schedule.setStatus(ScheduleStatus.IN_PROGRESS);
        schedule = scheduleRepository.save(schedule);

        try {
            // Load resources
            List<Teacher> teachers = sisDataService.getAllTeachers();
            List<Course> courses = sisDataService.getAllCourses();
            List<Room> rooms = roomRepository.findAll();
            List<Student> students = sisDataService.getAllStudents();

            log.info("Loaded resources: {} teachers, {} courses, {} rooms, {} students",
                    teachers.size(), courses.size(), rooms.size(), students.size());

            // ✅ FIX: Create request using setters, not builder()
            ScheduleGenerationRequest request = new ScheduleGenerationRequest();
            request.setScheduleName(schedule.getName());
            request.setScheduleType(schedule.getScheduleType());
            request.setStartDate(schedule.getStartDate());
            request.setEndDate(schedule.getEndDate());

            // ✅ NULL SAFE: Check for null before accessing getHour()
            request.setStartHour(schedule.getStartTime() != null ? schedule.getStartTime().getHour() : 8);
            request.setEndHour(schedule.getEndTime() != null ? schedule.getEndTime().getHour() : 15);

            request.setPeriodDuration(schedule.getSlotDuration());
            request.setOptimizationTimeSeconds(120);

            // Call generation service
            Schedule generatedSchedule = scheduleGenerationService.generateSchedule(
                    request,
                    (progress, message) -> log.debug("Progress: {}% - {}", progress, message));

            // Set active status on slots
            List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(generatedSchedule.getId());
            slots.forEach(slot -> {
                if (slot.getStatus() == null) {
                    slot.setStatus(SlotStatus.ACTIVE);
                }
            });
            scheduleSlotRepository.saveAll(slots);

            return generatedSchedule;

        } catch (Exception e) {
            log.error("Schedule generation failed", e);
            schedule.setStatus(ScheduleStatus.DRAFT);
            scheduleRepository.save(schedule);
            throw new RuntimeException("Schedule generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get teacher utilization
     */
    public double getTeacherUtilization(Long scheduleId) {
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(scheduleId);

        long totalSlots = slots.size();
        long filledSlots = slots.stream()
                .filter(s -> s.getTeacher() != null && s.getStatus() == SlotStatus.ACTIVE)
                .count();

        return totalSlots > 0 ? (double) filledSlots / totalSlots : 0.0;
    }

    /**
     * Get room utilization
     */
    public double getRoomUtilization(Long scheduleId) {
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(scheduleId);

        long totalSlots = slots.size();
        long filledSlots = slots.stream()
                .filter(s -> s.getRoom() != null)
                .count();

        return totalSlots > 0 ? (double) filledSlots / totalSlots : 0.0;
    }

    /**
     * Get slots by teacher
     */
    public List<ScheduleSlot> getSlotsByTeacher(Long scheduleId, Long teacherId) {
        return scheduleSlotRepository.findByScheduleId(scheduleId).stream()
                .filter(s -> s.getTeacher() != null && s.getTeacher().getId().equals(teacherId))
                .collect(Collectors.toList());
    }

    /**
     * Get slots by room
     */
    public List<ScheduleSlot> getSlotsByRoom(Long scheduleId, Long roomId) {
        return scheduleSlotRepository.findByScheduleId(scheduleId).stream()
                .filter(s -> s.getRoom() != null && s.getRoom().getId().equals(roomId))
                .collect(Collectors.toList());
    }

    /**
     * Get lab rooms
     */
    public List<Room> getLabRooms() {
        return roomRepository.findAll().stream()
                .filter(r -> r.getType() == RoomType.SCIENCE_LAB ||
                        r.getType() == RoomType.COMPUTER_LAB)
                .collect(Collectors.toList());
    }

    /**
     * Get slots needing labs
     */
    public List<ScheduleSlot> getSlotsNeedingLabs(Long scheduleId) {
        return scheduleSlotRepository.findByScheduleId(scheduleId).stream()
                .filter(s -> s.getCourse() != null && s.getCourse().isRequiresLab())
                .collect(Collectors.toList());
    }

    /**
     * Get unassigned slots
     */
    public List<ScheduleSlot> getUnassignedSlots(Long scheduleId) {
        return scheduleSlotRepository.findByScheduleId(scheduleId).stream()
                .filter(s -> s.getTeacher() == null || s.getRoom() == null)
                .collect(Collectors.toList());
    }

    /**
     * Publish schedule
     */
    @Transactional
    public void publishSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        schedule.setStatus(ScheduleStatus.PUBLISHED);
        scheduleRepository.save(schedule);
        log.info("Schedule {} published", scheduleId);
    }

    /**
     * Clone schedule
     */
    @Transactional
    public Schedule cloneSchedule(Long scheduleId) {
        Schedule original = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        Schedule clone = new Schedule();
        clone.setName(original.getName() + " (Copy)");
        clone.setScheduleType(original.getScheduleType());
        clone.setPeriod(original.getPeriod());
        clone.setStartDate(original.getStartDate());
        clone.setEndDate(original.getEndDate());
        clone.setStatus(ScheduleStatus.DRAFT);

        clone = scheduleRepository.save(clone);

        // Clone slots
        List<ScheduleSlot> originalSlots = scheduleSlotRepository.findByScheduleId(scheduleId);

        for (ScheduleSlot originalSlot : originalSlots) {
            ScheduleSlot newSlot = new ScheduleSlot();
            newSlot.setSchedule(clone);
            newSlot.setDayOfWeek(originalSlot.getDayOfWeek());
            newSlot.setStartTime(originalSlot.getStartTime());
            newSlot.setEndTime(originalSlot.getEndTime());
            newSlot.setTeacher(originalSlot.getTeacher());
            newSlot.setCourse(originalSlot.getCourse());
            newSlot.setRoom(originalSlot.getRoom());
            newSlot.setStatus(originalSlot.getStatus());

            scheduleSlotRepository.save(newSlot);
        }

        log.info("Cloned schedule {} to {}", scheduleId, clone.getId());
        return clone;
    }

    /**
     * Archive schedule
     */
    @Transactional
    public void archiveSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));

        schedule.setStatus(ScheduleStatus.ARCHIVED);
        scheduleRepository.save(schedule);
        log.info("Schedule {} archived", scheduleId);
    }

    /**
     * Get schedule statistics
     * ✅ FIX: Use getHasConflict() instead of isHasConflict()
     */
    public String getScheduleStatistics(Long scheduleId) {
        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(scheduleId);

        long totalSlots = slots.size();
        long assignedSlots = slots.stream()
                .filter(s -> s.getTeacher() != null && s.getRoom() != null)
                .count();
        // ✅ FIX: Changed from isHasConflict() to getHasConflict()
        long conflictSlots = slots.stream()
                .filter(s -> s.getHasConflict() != null && s.getHasConflict())
                .count();

        return String.format("Total: %d, Assigned: %d, Conflicts: %d",
                totalSlots, assignedSlots, conflictSlots);
    }
}