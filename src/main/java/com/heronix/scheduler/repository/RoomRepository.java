package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.enums.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Room Repository - WITH LENIENT IMPORT SUPPORT
 * Location: src/main/java/com/eduscheduler/repository/RoomRepository.java
 * 
 * @version 2.0.0 - Added needsReview queries
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // Find room by number
    Optional<Room> findByRoomNumber(String roomNumber);

    // Find all active rooms
    List<Room> findByActiveTrue();

    // Find rooms by building
    List<Room> findByBuilding(String building);

    // Find rooms by type
    List<Room> findByType(RoomType type);

    // Find rooms with minimum capacity
    @Query("SELECT r FROM Room r WHERE r.capacity >= :minCapacity AND r.active = true")
    List<Room> findByMinimumCapacity(int minCapacity);

    // Find rooms with specific equipment
    @Query("SELECT r FROM Room r JOIN r.equipment e WHERE e = :equipment AND r.active = true")
    List<Room> findByEquipment(String equipment);

    // Find rooms with projector
    List<Room> findByHasProjectorTrueAndActiveTrue();

    // Find wheelchair accessible rooms
    List<Room> findByWheelchairAccessibleTrueAndActiveTrue();

    // Find rooms with computers
    List<Room> findByHasComputersTrueAndActiveTrue();

    // ========================================================================
    // ✅ NEW METHODS FOR LENIENT IMPORT
    // ========================================================================

    /**
     * Find all rooms that need review (incomplete data)
     */
    List<Room> findByNeedsReviewTrue();

    /**
     * Count rooms needing review
     */
    long countByNeedsReviewTrue();

    /**
     * Find rooms needing review by building
     */
    List<Room> findByNeedsReviewTrueAndBuilding(String building);

    // ========================================================================
    // ✅ PHASE 6C: DEPARTMENT ROOM ZONES - DECEMBER 3, 2025
    // ========================================================================

    /**
     * Find all rooms in a specific zone
     * @param zone Zone name (e.g., "Math Wing", "Science Wing")
     * @return List of rooms in that zone
     */
    List<Room> findByZone(String zone);

    /**
     * Find all rooms in a specific building and floor
     * @param building Building name
     * @param floor Floor number
     * @return List of rooms matching criteria
     */
    List<Room> findByBuildingAndFloor(String building, Integer floor);

    /**
     * Get all distinct zone names in the database
     * @return List of zone names, sorted alphabetically
     */
    @Query("SELECT DISTINCT r.zone FROM Room r WHERE r.zone IS NOT NULL ORDER BY r.zone")
    List<String> findAllZones();

    /**
     * Get all distinct building names in the database
     * @return List of building names, sorted alphabetically
     */
    @Query("SELECT DISTINCT r.building FROM Room r WHERE r.building IS NOT NULL ORDER BY r.building")
    List<String> findAllBuildings();

    // ========================================================================
    // ✅ EAGER LOADING FOR UI DISPLAY - DECEMBER 14, 2025
    // ========================================================================

    /**
     * Find all rooms with currentTeacher eagerly loaded for UI display
     * Prevents LazyInitializationException when displaying teacher names in RoomsController table
     * @return List of all rooms with teacher relationship initialized
     */
    @Query("SELECT DISTINCT r FROM Room r LEFT JOIN FETCH r.currentTeacher")
    List<Room> findAllWithTeacher();

    // ========================================================================
    // ✅ PRIORITY 2 FIX - QUERY-LEVEL FILTERING - DECEMBER 15, 2025
    // ========================================================================

    /**
     * Find all schedulable rooms (active and with schedulable room types)
     * Performance optimization: Filters at database level instead of in Java
     *
     * Used by SmartRoomAssignmentService to avoid fetching 500+ rooms then filtering
     *
     * @return List of rooms that can be used for scheduling (excludes storage, server rooms, etc.)
     */
    @Query("SELECT r FROM Room r WHERE r.active = true AND r.type IS NOT NULL " +
           "AND r.type IN ('CLASSROOM', 'SCIENCE_LAB', 'COMPUTER_LAB', 'ART_ROOM', " +
           "'MUSIC_ROOM', 'GYMNASIUM', 'LIBRARY', 'AUDITORIUM', 'CAFETERIA')")
    List<Room> findAllSchedulableRooms();
}