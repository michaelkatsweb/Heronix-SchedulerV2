package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Schedule;
import com.heronix.scheduler.model.enums.ScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    /**
     * Find schedules by status
     */
    List<Schedule> findByStatus(ScheduleStatus status);

    /**
     * Find schedules by status and end date before
     */
    List<Schedule> findByStatusAndEndDateBefore(ScheduleStatus status, LocalDate date);

    /**
     * Find all schedules with slots (avoids lazy loading issue)
     */
    @Query("SELECT DISTINCT s FROM Schedule s " +
           "LEFT JOIN FETCH s.slots slot " +
           "LEFT JOIN FETCH slot.course " +
           "LEFT JOIN FETCH slot.teacher " +
           "LEFT JOIN FETCH slot.room")
    List<Schedule> findAllWithSlots();

    /**
     * Find schedule by ID with slots eagerly loaded (avoids lazy loading issue)
     */
    @Query("SELECT DISTINCT s FROM Schedule s " +
           "LEFT JOIN FETCH s.slots slot " +
           "LEFT JOIN FETCH slot.course " +
           "LEFT JOIN FETCH slot.teacher " +
           "LEFT JOIN FETCH slot.room " +
           "WHERE s.id = :id")
    java.util.Optional<Schedule> findByIdWithSlots(Long id);

    /**
     * Count schedules that fall within a date range (for academic year association)
     */
    @Query("SELECT COUNT(s) FROM Schedule s WHERE " +
           "s.startDate >= :startDate AND s.endDate <= :endDate")
    long countByDateRange(LocalDate startDate, LocalDate endDate);
}
