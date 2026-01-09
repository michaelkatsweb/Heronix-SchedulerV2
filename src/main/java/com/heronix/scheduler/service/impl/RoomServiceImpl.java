// Location: src/main/java/com/eduscheduler/service/impl/RoomServiceImpl.java
package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.repository.RoomRepository;
import com.heronix.scheduler.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Room Service Implementation
 * Location: src/main/java/com/eduscheduler/service/impl/RoomServiceImpl.java
 * 
 * @author Heronix Scheduling System Team
 * @version 1.1.0
 * @since 2025-10-25
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    /**
     * Get all active rooms
     * 
     * @return List of active rooms
     */
    @Override
    @Transactional(readOnly = true)
    public List<Room> getAllActiveRooms() {
        log.debug("🏫 Fetching all active rooms");
        List<Room> rooms = roomRepository.findByActiveTrue();
        log.info("✅ Found {} active rooms", rooms.size());
        return rooms;
    }

    /**
     * Get room by ID
     * 
     * @param id Room ID
     * @return Room object or null if not found
     */
    @Override
    @Transactional(readOnly = true)
    public Room getRoomById(Long id) {
        log.debug("🔍 Fetching room with ID: {}", id);
        Room room = roomRepository.findById(id).orElse(null);
        // ✅ NULL SAFE: Check room exists before accessing roomNumber
        if (room != null) {
            log.debug("✅ Found room: {}", room.getRoomNumber() != null ? room.getRoomNumber() : "Unknown");
        } else {
            log.warn("⚠️ Room not found with ID: {}", id);
        }
        return room;
    }

    /**
     * Get all rooms (including inactive)
     * 
     * @return List of all rooms
     */
    @Override
    @Transactional(readOnly = true)
    public List<Room> findAll() {
        log.debug("🏫 Fetching all rooms (including inactive)");
        List<Room> rooms = roomRepository.findAll();
        log.info("✅ Found {} total rooms", rooms.size());
        return rooms;
    }
}