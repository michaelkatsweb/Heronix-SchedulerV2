// Location: src/main/java/com/eduscheduler/service/RoomService.java
package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Room;
import java.util.List;

/**
 * Room Service Interface
 * Defines operations for managing rooms
 * 
 * @author Heronix Scheduling System Team
 * @version 1.1.0
 * @since 2025-10-25
 */
public interface RoomService {

    /**
     * Get all active rooms
     * 
     * @return List of active rooms
     */
    List<Room> getAllActiveRooms();

    /**
     * Get room by ID
     * 
     * @param id Room ID
     * @return Room object
     */
    Room getRoomById(Long id);

    /**
     * Get all rooms (including inactive)
     * 
     * @return List of all rooms
     */
    List<Room> findAll();
}