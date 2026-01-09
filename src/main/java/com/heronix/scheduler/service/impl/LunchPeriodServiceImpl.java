package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.LunchPeriod;
import com.heronix.scheduler.repository.LunchPeriodRepository;
import com.heronix.scheduler.service.LunchPeriodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.AbstractMap;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of LunchPeriodService
 */
@Service
@Transactional
public class LunchPeriodServiceImpl implements LunchPeriodService {

    private static final Logger logger = LoggerFactory.getLogger(LunchPeriodServiceImpl.class);

    @Autowired
    private LunchPeriodRepository lunchPeriodRepository;

    @Autowired
    private com.heronix.scheduler.repository.ScheduleSlotRepository scheduleSlotRepository;

    @Override
    public LunchPeriod createLunchPeriod(LunchPeriod lunchPeriod) {
        logger.info("Creating new lunch period: {}", lunchPeriod.getName());

        // Calculate duration before saving
        lunchPeriod.calculateDuration();

        // Check for duplicate name
        if (lunchPeriodRepository.existsByName(lunchPeriod.getName())) {
            throw new IllegalArgumentException("Lunch period with name '" +
                    lunchPeriod.getName() + "' already exists");
        }

        return lunchPeriodRepository.save(lunchPeriod);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LunchPeriod> getLunchPeriodById(Long id) {
        logger.debug("Fetching lunch period with ID: {}", id);
        return lunchPeriodRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchPeriod> getAllLunchPeriods() {
        logger.debug("Fetching all lunch periods");
        return lunchPeriodRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchPeriod> getActiveLunchPeriods() {
        logger.debug("Fetching active lunch periods");
        return lunchPeriodRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchPeriod> getLunchPeriodsByGroup(String lunchGroup) {
        logger.debug("Fetching lunch periods for group: {}", lunchGroup);
        return lunchPeriodRepository.findByLunchGroup(lunchGroup);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchPeriod> getLunchPeriodsForDay(Integer dayOfWeek) {
        logger.debug("Fetching lunch periods for day: {}", dayOfWeek);
        return lunchPeriodRepository.findByDayOfWeek(dayOfWeek);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LunchPeriod> getLunchPeriodsForGradeLevel(String gradeLevel) {
        logger.debug("Fetching lunch periods for grade level: {}", gradeLevel);
        return lunchPeriodRepository.findByGradeLevel(gradeLevel);
    }

    @Override
    public LunchPeriod updateLunchPeriod(Long id, LunchPeriod updatedLunchPeriod) {
        logger.info("Updating lunch period with ID: {}", id);

        LunchPeriod existingLunchPeriod = lunchPeriodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lunch period not found with ID: " + id));

        // Update fields
        existingLunchPeriod.setName(updatedLunchPeriod.getName());
        existingLunchPeriod.setLunchGroup(updatedLunchPeriod.getLunchGroup());
        existingLunchPeriod.setStartTime(updatedLunchPeriod.getStartTime());
        existingLunchPeriod.setEndTime(updatedLunchPeriod.getEndTime());
        existingLunchPeriod.setMaxStudents(updatedLunchPeriod.getMaxStudents());
        existingLunchPeriod.setLocation(updatedLunchPeriod.getLocation());
        existingLunchPeriod.setGradeLevels(updatedLunchPeriod.getGradeLevels());
        existingLunchPeriod.setDayOfWeek(updatedLunchPeriod.getDayOfWeek());
        existingLunchPeriod.setNotes(updatedLunchPeriod.getNotes());
        existingLunchPeriod.setPriority(updatedLunchPeriod.getPriority());

        // Recalculate duration
        existingLunchPeriod.calculateDuration();

        return lunchPeriodRepository.save(existingLunchPeriod);
    }

    @Override
    public void deleteLunchPeriod(Long id) {
        logger.info("Soft deleting lunch period with ID: {}", id);

        LunchPeriod lunchPeriod = lunchPeriodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lunch period not found with ID: " + id));

        lunchPeriod.setActive(false);
        lunchPeriodRepository.save(lunchPeriod);
    }

    @Override
    public void activateLunchPeriod(Long id) {
        logger.info("Activating lunch period with ID: {}", id);

        LunchPeriod lunchPeriod = lunchPeriodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lunch period not found with ID: " + id));

        lunchPeriod.setActive(true);
        lunchPeriodRepository.save(lunchPeriod);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return lunchPeriodRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasTimeConflict(LocalTime startTime, LocalTime endTime, String location) {
        logger.debug("Checking for time conflicts: {} - {} at {}",
                startTime, endTime, location);

        List<LunchPeriod> existingPeriods = lunchPeriodRepository.findByLocation(location);

        for (LunchPeriod period : existingPeriods) {
            if (!period.isActive())
                continue;

            // Check for overlap
            boolean overlaps = (startTime.isBefore(period.getEndTime()) && endTime.isAfter(period.getStartTime())) ||
                    (startTime.equals(period.getStartTime()) || endTime.equals(period.getEndTime()));

            if (overlaps) {
                logger.warn("Time conflict detected with lunch period: {}", period.getName());
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional
    public void assignLunchPeriods(com.heronix.scheduler.model.domain.Schedule schedule,
            com.heronix.scheduler.model.domain.LunchConfiguration config) {
        logger.info("Assigning lunch periods for schedule: {}", schedule.getId());

        if (schedule.getSlots() == null) return;

        int totalStudents = schedule.getSlots().stream()
            .flatMap(slot -> slot.getStudents() != null ? slot.getStudents().stream() : java.util.stream.Stream.empty())
            .distinct()
            .toList().size();

        int studentsPerWave = config.getMaxStudentsPerPeriod();
        int wavesNeeded = Math.min(
            (int) Math.ceil((double) totalStudents / studentsPerWave),
            config.getNumberOfLunchPeriods());

        logger.info("Distributing {} students across {} lunch waves", totalStudents, wavesNeeded);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.heronix.scheduler.model.domain.ScheduleSlot> getLunchSlotsForStudent(
            com.heronix.scheduler.model.domain.Student student) {
        if (student.getScheduleSlots() == null) return List.of();

        return student.getScheduleSlots().stream()
            .filter(slot -> Boolean.TRUE.equals(slot.getIsLunchPeriod()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.heronix.scheduler.model.domain.ScheduleSlot> getLunchSlotsForTeacher(
            com.heronix.scheduler.model.domain.Teacher teacher) {
        if (// TODO: Method getScheduleSlots() does not exist on Teacher - use scheduleSlotRepository instead
                    scheduleSlotRepository.findByTeacherIdWithDetails(teacher.getId()) == null) return List.of();

        return // TODO: Method getScheduleSlots() does not exist on Teacher - use scheduleSlotRepository instead
                    scheduleSlotRepository.findByTeacherIdWithDetails(teacher.getId()).stream()
            .filter(slot -> Boolean.TRUE.equals(slot.getIsLunchPeriod()))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Integer, List<com.heronix.scheduler.model.domain.Student>> getLunchDistribution(
            com.heronix.scheduler.model.domain.Schedule schedule) {
        if (schedule.getSlots() == null) return java.util.Map.of();

        return schedule.getSlots().stream()
            .filter(slot -> Boolean.TRUE.equals(slot.getIsLunchPeriod()))
            .filter(slot -> slot.getLunchWaveNumber() != null)
            .flatMap(slot -> slot.getStudents() != null ?
                slot.getStudents().stream().map(s -> new AbstractMap.SimpleEntry<>(slot.getLunchWaveNumber(), s)) :
                java.util.stream.Stream.empty())
            .collect(java.util.stream.Collectors.groupingBy(
                java.util.Map.Entry::getKey,
                java.util.stream.Collectors.mapping(java.util.Map.Entry::getValue, java.util.stream.Collectors.toList())
            ));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateLunchCapacity(com.heronix.scheduler.model.domain.Schedule schedule,
            com.heronix.scheduler.model.domain.LunchConfiguration config) {
        java.util.Map<Integer, List<com.heronix.scheduler.model.domain.Student>> distribution =
            getLunchDistribution(schedule);

        return distribution.values().stream()
            .allMatch(students -> students.size() <= config.getMaxStudentsPerPeriod());
    }

    @Override
    @Transactional
    public void staggerLunchByGrade(com.heronix.scheduler.model.domain.Schedule schedule,
            com.heronix.scheduler.model.domain.LunchConfiguration config) {
        logger.info("Staggering lunch by grade for schedule: {}", schedule.getId());
        // Implementation would group students by grade and assign waves
    }

    @Override
    public Integer determineLunchWave(com.heronix.scheduler.model.domain.Student student,
            com.heronix.scheduler.model.domain.LunchConfiguration config) {
        if (!Boolean.TRUE.equals(config.getStaggerByGrade())) {
            return student.getId().hashCode() % config.getNumberOfLunchPeriods() + 1;
        }

        if (student.getGradeLevel() != null) {
            // Parse grade level (e.g., "9", "10", "11", "12")
            try {
                int grade = Integer.parseInt(student.getGradeLevel());
                return (grade % config.getNumberOfLunchPeriods()) + 1;
            } catch (NumberFormatException e) {
                logger.warn("Could not parse grade level: {}", student.getGradeLevel());
            }
        }

        return 1;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasLunchConflict(com.heronix.scheduler.model.domain.ScheduleSlot slot) {
        return Boolean.TRUE.equals(slot.getIsLunchPeriod());
    }
}