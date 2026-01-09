// Location: src/main/java/com/eduscheduler/service/impl/TimeSlotServiceImpl.java
package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.TimeSlot;
import com.heronix.scheduler.service.TimeSlotService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * TimeSlot Service Implementation
 * Manages time slots for scheduling
 *
 * Note: TimeSlot is not a JPA entity, so this service generates them dynamically
 * based on standard school schedule parameters
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
@Slf4j
@Service
public class TimeSlotServiceImpl implements TimeSlotService {

    // In-memory storage for time slots (since TimeSlot is not a JPA entity)
    private final List<TimeSlot> timeSlots = new ArrayList<>();
    private Long nextId = 1L;

    public TimeSlotServiceImpl() {
        log.info("Initializing TimeSlotService with default time slots");
        initializeDefaultTimeSlots();
    }

    /**
     * Initialize default time slots for a standard school week
     * Monday - Friday, 8:00 AM to 4:00 PM, 1-hour periods
     */
    private void initializeDefaultTimeSlots() {
        List<DayOfWeek> schoolDays = Arrays.asList(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY
        );

        // Define periods: 8:00-9:00, 9:00-10:00, 10:00-11:00, 11:00-12:00,
        // 12:00-1:00 (lunch), 1:00-2:00, 2:00-3:00, 3:00-4:00
        List<LocalTime> startTimes = Arrays.asList(
            LocalTime.of(8, 0),
            LocalTime.of(9, 0),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            LocalTime.of(12, 0),  // Lunch period
            LocalTime.of(13, 0),
            LocalTime.of(14, 0),
            LocalTime.of(15, 0)
        );

        int periodNumber = 1;
        for (DayOfWeek day : schoolDays) {
            for (LocalTime startTime : startTimes) {
                LocalTime endTime = startTime.plusHours(1);
                TimeSlot slot = new TimeSlot(day, startTime, endTime, periodNumber);
                timeSlots.add(slot);
                periodNumber++;
            }
        }

        log.info("Initialized {} default time slots", timeSlots.size());
    }

    @Override
    public List<TimeSlot> getAllTimeSlots() {
        log.debug("Fetching all time slots");
        return new ArrayList<>(timeSlots);
    }

    @Override
    public List<TimeSlot> getTimeSlotsByDay(DayOfWeek dayOfWeek) {
        // ✅ NULL SAFE: Validate dayOfWeek parameter
        if (dayOfWeek == null) {
            log.warn("Cannot fetch time slots for null dayOfWeek");
            return Collections.emptyList();
        }

        log.debug("Fetching time slots for day: {}", dayOfWeek);
        // ✅ NULL SAFE: Filter null slots and null dayOfWeek in slots
        return timeSlots.stream()
            .filter(slot -> slot != null && slot.getDayOfWeek() != null && slot.getDayOfWeek().equals(dayOfWeek))
            .collect(Collectors.toList());
    }

    @Override
    public TimeSlot createTimeSlot(TimeSlot timeSlot) {
        // ✅ NULL SAFE: Validate timeSlot parameter
        if (timeSlot == null) {
            throw new IllegalArgumentException("TimeSlot cannot be null");
        }

        // ✅ NULL SAFE: Safe extraction for logging
        String displayString = (timeSlot.getDayOfWeek() != null && timeSlot.getStartTime() != null)
            ? timeSlot.toDisplayString() : "Unknown";
        log.info("Creating new time slot: {}", displayString);

        // Validate
        if (timeSlot.getDayOfWeek() == null || timeSlot.getStartTime() == null ||
            timeSlot.getEndTime() == null) {
            throw new IllegalArgumentException("TimeSlot must have day, start time, and end time");
        }

        // Check for overlaps
        // ✅ NULL SAFE: Filter null slots before checking overlaps
        boolean hasOverlap = timeSlots.stream()
            .filter(existing -> existing != null)
            .anyMatch(existing -> existing.overlapsWith(timeSlot));

        if (hasOverlap) {
            log.warn("Time slot overlaps with existing slot: {}", timeSlot.toDisplayString());
            throw new IllegalArgumentException("Time slot overlaps with existing slot");
        }

        // Assign period number if not set
        if (timeSlot.getPeriodNumber() == null) {
            timeSlot.setPeriodNumber(timeSlots.size() + 1);
        }

        timeSlots.add(timeSlot);
        log.info("Time slot created successfully: {}", timeSlot.toDisplayString());
        return timeSlot;
    }

    @Override
    public void deleteTimeSlot(Long id) {
        // ✅ NULL SAFE: Validate id parameter
        if (id == null) {
            throw new IllegalArgumentException("ID cannot be null");
        }

        log.info("Deleting time slot with ID: {}", id);

        // Since TimeSlot doesn't have an ID field, we'll use the period number as pseudo-ID
        // ✅ NULL SAFE: Check slot is not null before accessing period number
        boolean removed = timeSlots.removeIf(slot ->
            slot != null && slot.getPeriodNumber() != null && slot.getPeriodNumber().equals(id.intValue()));

        if (removed) {
            log.info("Time slot deleted successfully");
        } else {
            log.warn("Time slot not found with ID: {}", id);
            throw new IllegalArgumentException("Time slot not found with ID: " + id);
        }
    }

    @Override
    public Optional<TimeSlot> findByDayAndTime(com.heronix.scheduler.model.enums.DayOfWeek day,
                                                 java.time.LocalTime time) {
        // ✅ NULL SAFE: Validate parameters
        if (day == null || time == null) {
            log.warn("Cannot find time slot with null day or time");
            return Optional.empty();
        }

        // Convert enum DayOfWeek to java.time.DayOfWeek
        DayOfWeek javaDayOfWeek = convertToJavaDayOfWeek(day);

        // ✅ NULL SAFE: Check conversion result
        if (javaDayOfWeek == null) {
            log.warn("Failed to convert day enum to java.time.DayOfWeek: {}", day);
            return Optional.empty();
        }

        // ✅ NULL SAFE: Filter null slots and check dayOfWeek and startTime exist before equals
        return timeSlots.stream()
            .filter(slot -> slot != null && slot.getDayOfWeek() != null && slot.getDayOfWeek().equals(javaDayOfWeek) &&
                          slot.getStartTime() != null && slot.getStartTime().equals(time))
            .findFirst();
    }

    /**
     * Convert custom DayOfWeek enum to java.time.DayOfWeek
     */
    private DayOfWeek convertToJavaDayOfWeek(com.heronix.scheduler.model.enums.DayOfWeek day) {
        if (day == null) {
            return null;
        }

        switch (day) {
            case MONDAY: return DayOfWeek.MONDAY;
            case TUESDAY: return DayOfWeek.TUESDAY;
            case WEDNESDAY: return DayOfWeek.WEDNESDAY;
            case THURSDAY: return DayOfWeek.THURSDAY;
            case FRIDAY: return DayOfWeek.FRIDAY;
            case SATURDAY: return DayOfWeek.SATURDAY;
            case SUNDAY: return DayOfWeek.SUNDAY;
            default: return null;
        }
    }
}
