package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.SpecialEventBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * Special Event Block Repository
 * Location: src/main/java/com/eduscheduler/repository/SpecialEventBlockRepository.java
 */
@Repository
public interface SpecialEventBlockRepository extends JpaRepository<SpecialEventBlock, Long> {

    /**
     * Find all active event blocks ordered by event date
     */
    List<SpecialEventBlock> findByActiveTrueOrderByEventDateAsc();

    /**
     * Find event blocks by type (uses inner enum)
     */
    List<SpecialEventBlock> findByBlockType(SpecialEventBlock.EventBlockType blockType);

    /**
     * Find event blocks by day of week
     */
    List<SpecialEventBlock> findByDayOfWeek(DayOfWeek dayOfWeek);

    /**
     * Find event blocks by specific date
     */
    List<SpecialEventBlock> findByEventDate(LocalDate date);

    /**
     * Find all active event blocks
     */
    List<SpecialEventBlock> findByActiveTrue();

    /**
     * Find blocks for a specific date (includes recurring weekly blocks)
     */
    @Query("SELECT e FROM SpecialEventBlock e WHERE e.active = true AND " +
            "(e.eventDate = :date OR (e.dayOfWeek = :day AND e.eventDate IS NULL))")
    List<SpecialEventBlock> findBlocksForDate(@Param("date") LocalDate date, @Param("day") DayOfWeek day);

    /**
     * Count active blocks
     */
    @Query("SELECT COUNT(e) FROM SpecialEventBlock e WHERE e.active = true")
    long countActiveBlocks();
}