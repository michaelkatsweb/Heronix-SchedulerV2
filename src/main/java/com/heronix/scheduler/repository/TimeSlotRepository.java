package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * TimeSlot Repository for local persistence of time slot data.
 */
@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    Optional<TimeSlot> findByDayOfWeekAndStartTimeAndEndTime(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime);

    List<TimeSlot> findByDayOfWeek(DayOfWeek dayOfWeek);

    List<TimeSlot> findByPeriodNumber(Integer periodNumber);
}
