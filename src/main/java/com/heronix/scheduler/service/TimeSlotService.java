package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.TimeSlot;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

/**
 * TimeSlot Service Interface
 * Provides operations for managing time slots in the schedule
 * Location: src/main/java/com/eduscheduler/service/TimeSlotService.java
 */
public interface TimeSlotService {

    /**
     * Retrieves all time slots
     * 
     * @return List of all time slots
     */
    List<TimeSlot> getAllTimeSlots();

    /**
     * Retrieves time slots for a specific day of the week
     * 
     * @param dayOfWeek The day to filter by
     * @return List of time slots for the specified day
     */
    List<TimeSlot> getTimeSlotsByDay(DayOfWeek dayOfWeek);

    /**
     * Creates a new time slot
     * 
     * @param timeSlot The time slot to create
     * @return The saved time slot with generated ID
     */
    TimeSlot createTimeSlot(TimeSlot timeSlot);

    /**
     * Deletes a time slot by ID
     * 
     * @param id The ID of the time slot to delete
     */
    void deleteTimeSlot(Long id);

    Optional<TimeSlot> findByDayAndTime(com.heronix.scheduler.model.enums.DayOfWeek day, java.time.LocalTime time);
}