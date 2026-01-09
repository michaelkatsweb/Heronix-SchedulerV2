package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.DutyAssignment;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.repository.DutyAssignmentRepository;
import com.heronix.scheduler.service.DutyRosterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heronix.scheduler.service.data.SISDataService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ═══════════════════════════════════════════════════════════════
 * DUTY ROSTER SERVICE IMPLEMENTATION
 * Complete duty management with auto-generation and fair distribution
 * ═══════════════════════════════════════════════════════════════
 *
 * Location:
 * src/main/java/com/eduscheduler/service/impl/DutyRosterServiceImpl.java
 *
 * Features:
 * ✓ CRUD operations for duties
 * ✓ Auto-generation with fair distribution
 * ✓ Conflict detection
 * ✓ Substitute management
 * ✓ Recurring duty handling
 * ✓ Statistics and reporting
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-02
 */
@Slf4j
@Service
public class DutyRosterServiceImpl implements DutyRosterService {

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private DutyAssignmentRepository dutyRepository;

    // ═══════════════════════════════════════════════════════════════
    // CRUD OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public DutyAssignment createDuty(DutyAssignment duty) {
        // ✅ NULL SAFE: Validate duty parameter
        if (duty == null) {
            throw new IllegalArgumentException("Duty cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction of duty properties for logging
        String dutyType = (duty.getDutyType() != null) ? duty.getDutyType() : "Unknown";
        String dutyLocation = (duty.getDutyLocation() != null) ? duty.getDutyLocation() : "Unknown";
        log.info("✅ Creating duty assignment: {} - {}", dutyType, dutyLocation);

        // Validate no conflicts
        if (hasConflicts(duty)) {
            log.warn("⚠️ Duty assignment has conflicts");
            throw new IllegalStateException("Duty assignment conflicts with existing duties");
        }

        return dutyRepository.save(duty);
    }

    @Override
    @Transactional
    public DutyAssignment updateDuty(Long id, DutyAssignment updatedDuty) {
        log.info("🔄 Updating duty assignment {}", id);

        DutyAssignment existing = getDutyById(id);

        existing.setTeacher(updatedDuty.getTeacher());
        existing.setDutyType(updatedDuty.getDutyType());
        existing.setDutyLocation(updatedDuty.getDutyLocation());
        existing.setDutyDate(updatedDuty.getDutyDate());
        existing.setStartTime(updatedDuty.getStartTime());
        existing.setEndTime(updatedDuty.getEndTime());
        existing.setNotes(updatedDuty.getNotes());
        existing.setPriority(updatedDuty.getPriority());

        return dutyRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteDuty(Long id) {
        log.info("🗑️ Deleting duty assignment {}", id);

        DutyAssignment duty = getDutyById(id);
        duty.setActive(false); // Soft delete
        dutyRepository.save(duty);
    }

    @Override
    public DutyAssignment getDutyById(Long id) {
        return dutyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Duty not found: " + id));
    }

    @Override
    public List<DutyAssignment> getAllActiveDuties() {
        return dutyRepository.findAllActiveDutiesOrdered();
    }

    // ═══════════════════════════════════════════════════════════════
    // QUERY OPERATIONS
    // ═══════════════════════════════════════════════════════════════

    @Override
    public List<DutyAssignment> getDutiesForTeacher(Long teacherId) {
        return dutyRepository.findByTeacherId(teacherId);
    }

    @Override
    public List<DutyAssignment> getDutiesForDate(LocalDate date) {
        List<DutyAssignment> specificDuties = dutyRepository.findByDutyDate(date);
        List<DutyAssignment> recurringDuties = getRecurringDutiesForDate(date);

        List<DutyAssignment> allDuties = new ArrayList<>();
        allDuties.addAll(specificDuties);
        allDuties.addAll(recurringDuties);

        return allDuties;
    }

    @Override
    public List<DutyAssignment> getDutiesInDateRange(LocalDate startDate, LocalDate endDate) {
        return dutyRepository.findByDateRange(startDate, endDate);
    }

    @Override
    public List<DutyAssignment> getDutiesByType(String dutyType) {
        return dutyRepository.findByDutyTypeAndIsActiveTrue(dutyType);
    }

    @Override
    public List<DutyAssignment> getDutiesByLocation(String location) {
        return dutyRepository.findByDutyLocationAndIsActiveTrue(location);
    }

    // ═══════════════════════════════════════════════════════════════
    // AUTOMATIC GENERATION
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public List<DutyAssignment> generateDutiesForDateRange(
            LocalDate startDate,
            LocalDate endDate,
            Long scheduleId) {

        log.info("🔄 Generating duties from {} to {}", startDate, endDate);

        List<Teacher> availableTeachers = sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());
        List<DutyAssignment> generatedDuties = new ArrayList<>();

        // Define duty locations
        String[] locations = {
                "Front Entrance",
                "Cafeteria",
                "Hallway 2A",
                "Bus Loop",
                "Student Parking",
                "Gym Entrance"
        };

        LocalDate currentDate = startDate;
        int teacherIndex = 0;

        while (!currentDate.isAfter(endDate)) {
            // Skip weekends
            if (currentDate.getDayOfWeek() != DayOfWeek.SATURDAY &&
                    currentDate.getDayOfWeek() != DayOfWeek.SUNDAY) {

                // Generate AM duties
                for (String location : locations) {
                    // ✅ NULL SAFE: Skip null locations
                    if (location == null) continue;

                    DutyAssignment amDuty = new DutyAssignment();
                    amDuty.setTeacher(availableTeachers.get(teacherIndex % availableTeachers.size()));
                    amDuty.setDutyType("AM");
                    amDuty.setDutyLocation(location);
                    amDuty.setDutyDate(currentDate);
                    amDuty.setStartTime(LocalTime.of(7, 30));
                    amDuty.setEndTime(LocalTime.of(8, 0));
                    amDuty.setPriority(1);

                    generatedDuties.add(dutyRepository.save(amDuty));
                    teacherIndex++;
                }

                // Generate PM duties
                for (String location : locations) {
                    DutyAssignment pmDuty = new DutyAssignment();
                    pmDuty.setTeacher(availableTeachers.get(teacherIndex % availableTeachers.size()));
                    pmDuty.setDutyType("PM");
                    pmDuty.setDutyLocation(location);
                    pmDuty.setDutyDate(currentDate);
                    pmDuty.setStartTime(LocalTime.of(15, 30));
                    pmDuty.setEndTime(LocalTime.of(16, 0));
                    pmDuty.setPriority(1);

                    generatedDuties.add(dutyRepository.save(pmDuty));
                    teacherIndex++;
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info("✅ Generated {} duty assignments", generatedDuties.size());
        return generatedDuties;
    }

    @Override
    @Transactional
    public List<DutyAssignment> generateRotatingSchedule(
            List<Teacher> teachers,
            LocalDate startDate,
            int weeks,
            String dutyType,
            String location) {

        log.info("🔄 Generating rotating {} duty schedule for {} weeks", dutyType, weeks);

        List<DutyAssignment> duties = new ArrayList<>();
        LocalDate endDate = startDate.plusWeeks(weeks);
        LocalDate currentDate = startDate;
        int teacherIndex = 0;

        LocalTime startTime = dutyType.equals("AM") ? LocalTime.of(7, 30) : LocalTime.of(15, 30);
        LocalTime endTime = dutyType.equals("AM") ? LocalTime.of(8, 0) : LocalTime.of(16, 0);

        while (!currentDate.isAfter(endDate)) {
            if (currentDate.getDayOfWeek() != DayOfWeek.SATURDAY &&
                    currentDate.getDayOfWeek() != DayOfWeek.SUNDAY) {

                DutyAssignment duty = new DutyAssignment();
                duty.setTeacher(teachers.get(teacherIndex % teachers.size()));
                duty.setDutyType(dutyType);
                duty.setDutyLocation(location);
                duty.setDutyDate(currentDate);
                duty.setStartTime(startTime);
                duty.setEndTime(endTime);
                duty.setPriority(2);

                duties.add(dutyRepository.save(duty));
                teacherIndex++;
            }

            currentDate = currentDate.plusDays(1);
        }

        log.info("✅ Generated {} rotating duty assignments", duties.size());
        return duties;
    }

    // ═══════════════════════════════════════════════════════════════
    // CONFLICT DETECTION
    // ═══════════════════════════════════════════════════════════════

    @Override
    public boolean hasConflicts(DutyAssignment duty) {
        if (duty.getTeacher() == null || duty.getDutyDate() == null) {
            return false;
        }

        List<DutyAssignment> conflicts = dutyRepository.findConflictingDuties(
                duty.getTeacher().getId(),
                duty.getDutyDate(),
                duty.getStartTime(),
                duty.getEndTime());

        // Exclude self if updating
        if (duty.getId() != null) {
            conflicts = conflicts.stream()
                    .filter(d -> !d.getId().equals(duty.getId()))
                    .collect(Collectors.toList());
        }

        return !conflicts.isEmpty();
    }

    @Override
    public List<DutyAssignment> getConflicts(Long teacherId, LocalDate date,
            LocalTime startTime, LocalTime endTime) {
        return dutyRepository.findConflictingDuties(teacherId, date, startTime, endTime);
    }

    // ═══════════════════════════════════════════════════════════════
    // STATISTICS & REPORTING
    // ═══════════════════════════════════════════════════════════════

    @Override
    public Map<Teacher, Long> getDutyCountByTeacher(LocalDate startDate, LocalDate endDate) {
        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());
        Map<Teacher, Long> counts = new HashMap<>();

        for (Teacher teacher : teachers) {
            // ✅ NULL SAFE: Skip null teachers or teachers with null ID
            if (teacher == null || teacher.getId() == null) continue;

            long count = dutyRepository.countByTeacherAndDateRange(
                    teacher.getId(), startDate, endDate);
            counts.put(teacher, count);
        }

        return counts;
    }

    @Override
    public Map<String, Object> getDutyStatistics(LocalDate startDate, LocalDate endDate) {
        Map<String, Object> stats = new HashMap<>();

        List<DutyAssignment> duties = getDutiesInDateRange(startDate, endDate);

        stats.put("totalDuties", duties.size());
        // ✅ NULL SAFE: Filter null duties before checking properties
        stats.put("amDuties", duties.stream().filter(d -> d != null && d.isAMDuty()).count());
        stats.put("pmDuties", duties.stream().filter(d -> d != null && d.isPMDuty()).count());
        stats.put("lunchDuties", duties.stream().filter(d -> d != null && d.isLunchDuty()).count());
        stats.put("substituteDuties", duties.stream().filter(d -> d != null && d.isSubstitute()).count());

        stats.put("dutyCountByTeacher", getDutyCountByTeacher(startDate, endDate));
        stats.put("balanceScore", getDutyBalanceScore(startDate, endDate));

        return stats;
    }

    @Override
    public double getDutyBalanceScore(LocalDate startDate, LocalDate endDate) {
        Map<Teacher, Long> counts = getDutyCountByTeacher(startDate, endDate);

        if (counts.isEmpty()) {
            return 100.0;
        }

        List<Long> countValues = new ArrayList<>(counts.values());
        double average = countValues.stream().mapToLong(Long::longValue).average().orElse(0);
        double variance = countValues.stream()
                .mapToDouble(c -> Math.pow(c - average, 2))
                .average()
                .orElse(0);

        double stdDev = Math.sqrt(variance);

        // Score: 100 - (stdDev * 10), capped at 0-100
        double score = 100 - (stdDev * 10);
        return Math.max(0, Math.min(100, score));
    }

    // ═══════════════════════════════════════════════════════════════
    // SUBSTITUTE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public DutyAssignment assignSubstitute(Long dutyId, Long substituteTeacherId) {
        log.info("🔄 Assigning substitute {} to duty {}", substituteTeacherId, dutyId);

        DutyAssignment duty = getDutyById(dutyId);
        Teacher substitute = sisDataService.getTeacherById(substituteTeacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        duty.setOriginalTeacher(duty.getTeacher());
        duty.setTeacher(substitute);
        duty.setSubstitute(true);

        return dutyRepository.save(duty);
    }

    @Override
    @Transactional
    public DutyAssignment removeSubstitute(Long dutyId) {
        log.info("🔄 Removing substitute from duty {}", dutyId);

        DutyAssignment duty = getDutyById(dutyId);

        if (duty.getOriginalTeacher() != null) {
            duty.setTeacher(duty.getOriginalTeacher());
            duty.setOriginalTeacher(null);
            duty.setSubstitute(false);
        }

        return dutyRepository.save(duty);
    }

    @Override
    public List<DutyAssignment> getSubstituteDuties() {
        return dutyRepository.findByIsSubstituteTrueAndIsActiveTrue();
    }

    // ═══════════════════════════════════════════════════════════════
    // RECURRING DUTIES
    // ═══════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public DutyAssignment createRecurringDuty(
            Teacher teacher,
            String dutyType,
            String location,
            Integer dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            String recurrencePattern) {

        log.info("✅ Creating recurring duty: {} {} on day {}", dutyType, location, dayOfWeek);

        DutyAssignment duty = new DutyAssignment();
        duty.setTeacher(teacher);
        duty.setDutyType(dutyType);
        duty.setDutyLocation(location);
        duty.setDayOfWeek(dayOfWeek);
        duty.setStartTime(startTime);
        duty.setEndTime(endTime);
        duty.setRecurring(true);
        duty.setRecurrencePattern(recurrencePattern);
        duty.setDutyDate(LocalDate.now()); // Starting reference date

        return dutyRepository.save(duty);
    }

    @Override
    public List<DutyAssignment> getRecurringDuties() {
        return dutyRepository.findByIsRecurringTrueAndIsActiveTrue();
    }

    @Override
    @Transactional
    public List<DutyAssignment> generateRecurringInstances(LocalDate startDate, LocalDate endDate) {
        log.info("🔄 Generating recurring duty instances from {} to {}", startDate, endDate);

        List<DutyAssignment> recurringDuties = getRecurringDuties();
        List<DutyAssignment> instances = new ArrayList<>();

        for (DutyAssignment recurring : recurringDuties) {
            LocalDate currentDate = startDate;

            while (!currentDate.isAfter(endDate)) {
                if (currentDate.getDayOfWeek().getValue() == recurring.getDayOfWeek()) {
                    DutyAssignment instance = new DutyAssignment();
                    instance.setTeacher(recurring.getTeacher());
                    instance.setDutyType(recurring.getDutyType());
                    instance.setDutyLocation(recurring.getDutyLocation());
                    instance.setDutyDate(currentDate);
                    instance.setStartTime(recurring.getStartTime());
                    instance.setEndTime(recurring.getEndTime());
                    instance.setPriority(recurring.getPriority());
                    instance.setNotes("Generated from recurring duty #" + recurring.getId());

                    instances.add(dutyRepository.save(instance));
                }

                currentDate = currentDate.plusDays(1);
            }
        }

        log.info("✅ Generated {} recurring duty instances", instances.size());
        return instances;
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    private List<DutyAssignment> getRecurringDutiesForDate(LocalDate date) {
        int dayOfWeek = date.getDayOfWeek().getValue();
        List<DutyAssignment> recurringDuties = dutyRepository.findRecurringDutiesByDayOfWeek(dayOfWeek);

        // Filter duties that should be active on this date
        // ✅ NULL SAFE: Filter null duties before checking date
        return recurringDuties.stream()
                .filter(duty -> duty != null && duty.getDutyDate() != null &&
                                !duty.getDutyDate().isAfter(date))
                .collect(Collectors.toList());
    }
}