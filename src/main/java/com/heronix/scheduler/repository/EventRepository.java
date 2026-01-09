package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Event;
import com.heronix.scheduler.model.enums.EventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Event Repository - FIXED with correct field names
 * Location: src/main/java/com/eduscheduler/repository/EventRepository.java
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // ✅ FIXED: Changed from findByType to findByEventType
    List<Event> findByEventType(EventType eventType);

    // ✅ FIXED: Changed from findByBlockSchedulingTrue to findByBlocksSchedulingTrue
    List<Event> findByBlocksSchedulingTrue();

    // Find recurring events
    List<Event> findByRecurringTrue();

    // Find all-day events
    List<Event> findByAllDayTrue();

    // Find events within date range
    @Query("SELECT e FROM Event e WHERE e.startDateTime <= :endDateTime AND e.endDateTime >= :startDateTime")
    List<Event> findEventsInDateRange(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime);

    // Find upcoming events
    @Query("SELECT e FROM Event e WHERE e.startDateTime >= :now ORDER BY e.startDateTime")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now);

    // Find IEP/504 meetings
    @Query("SELECT e FROM Event e WHERE e.eventType IN ('IEP_MEETING', 'IEP_ANNUAL_REVIEW', 'SECTION_504_MEETING', 'SECTION_504_REVIEW')")
    List<Event> findComplianceMeetings();

    // Find events affecting a specific teacher
    @Query("SELECT e FROM Event e JOIN e.affectedTeachers t WHERE t.id = :teacherId")
    List<Event> findByAffectedTeacherId(@Param("teacherId") Long teacherId);

    // Find events affecting a specific room
    @Query("SELECT e FROM Event e JOIN e.affectedRooms r WHERE r.id = :roomId")
    List<Event> findByAffectedRoomId(@Param("roomId") Long roomId);
}