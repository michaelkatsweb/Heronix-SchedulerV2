package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Room Zone Service
 * Phase 6C: Department Room Zones
 *
 * Manages room zones and provides zone-based assignment logic to:
 * - Group rooms into department zones (Math Wing, Science Wing, etc.)
 * - Automatically suggest zones based on room type and number patterns
 * - Calculate zone preference scores for teacher-room assignments
 * - Minimize teacher travel between periods
 *
 * @since Phase 6C - December 3, 2025
 */
@Service
@Transactional
public class RoomZoneService {

    private static final Logger log = LoggerFactory.getLogger(RoomZoneService.class);

    @Autowired
    private RoomRepository roomRepository;

    /**
     * Automatically assign zones based on room type and number patterns
     * This provides intelligent defaults for new room data
     */
    public void autoAssignZones() {
        log.info("Auto-assigning zones to rooms based on type and number patterns");

        List<Room> rooms = roomRepository.findAll();
        int assignedCount = 0;

        for (Room room : rooms) {
            if (room.getZone() == null || room.getZone().trim().isEmpty()) {
                String suggestedZone = getSuggestedZone(room);
                if (suggestedZone != null) {
                    room.setZone(suggestedZone);
                    assignedCount++;
                }
            }
        }

        roomRepository.saveAll(rooms);
        log.info("Auto-assigned zones to {} rooms", assignedCount);
    }

    /**
     * Get suggested zone for a room based on type and room number
     *
     * @param room Room to analyze
     * @return Suggested zone name, or null if cannot determine
     */
    public String getSuggestedZone(Room room) {
        if (room == null) return null;

        // Priority 1: Assign by room type
        String typeBasedZone = getZoneByRoomType(room);
        if (typeBasedZone != null) {
            return typeBasedZone;
        }

        // Priority 2: Assign by room number pattern (floor-based)
        String roomNumber = room.getRoomNumber();
        if (roomNumber != null && !roomNumber.isEmpty()) {
            return getZoneByRoomNumber(roomNumber);
        }

        return null;
    }

    /**
     * Map room type to default zone
     */
    private String getZoneByRoomType(Room room) {
        if (room.getType() == null) return null;

        return switch (room.getType()) {
            // Science facilities
            case SCIENCE_LAB, LAB -> "Science Wing";

            // Physical Education
            case GYMNASIUM, WEIGHT_ROOM -> "Athletics Building";

            // Arts
            case ART_STUDIO, MUSIC_ROOM, BAND_ROOM, CHORUS_ROOM, THEATER -> "Arts Building";

            // Technology
            case COMPUTER_LAB, STEM_LAB -> "Technology Wing";

            // Vocational
            case CULINARY_LAB, WORKSHOP -> "Vocational Building";

            // Library/Commons
            case LIBRARY, MEDIA_CENTER, RESOURCE_ROOM -> "Academic Commons";

            // Large spaces
            case AUDITORIUM, CAFETERIA, CONFERENCE_ROOM, MULTIPURPOSE -> "Main Building";

            // Classrooms - determined by room number
            case CLASSROOM -> null;

            default -> null;
        };
    }

    /**
     * Assign zone based on room number patterns
     * Typical school layout: 100s = 1st floor, 200s = 2nd floor, etc.
     */
    private String getZoneByRoomNumber(String roomNumber) {
        // Try to extract first digit(s) to determine floor
        if (roomNumber.matches("^1[0-9]+.*")) {
            return "English Wing";  // 100-level rooms
        } else if (roomNumber.matches("^2[0-9]+.*")) {
            return "Math Wing";     // 200-level rooms
        } else if (roomNumber.matches("^3[0-9]+.*")) {
            return "Social Studies Wing";  // 300-level rooms
        } else if (roomNumber.matches("^4[0-9]+.*")) {
            return "Science Wing";  // 400-level rooms
        }

        return null;  // Cannot determine
    }

    /**
     * Get all rooms in the same zone as the given room
     *
     * @param room Reference room
     * @return List of rooms in same zone (excluding the given room)
     */
    public List<Room> getRoomsInSameZone(Room room) {
        if (room == null || room.getZone() == null || room.getZone().trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<Room> sameZoneRooms = roomRepository.findByZone(room.getZone());

        // Remove the input room from results
        sameZoneRooms.removeIf(r -> r.getId().equals(room.getId()));

        return sameZoneRooms;
    }

    /**
     * Calculate zone preference score for a teacher-room assignment
     * Higher score = better match
     *
     * Used by OptaPlanner constraints to prefer rooms in teacher's department zone
     *
     * @param teacher Teacher being assigned
     * @param room Room being considered
     * @return Preference score (0 = no preference, 2 = department zone match)
     */
    public int getZonePreferenceScore(Teacher teacher, Room room) {
        if (teacher == null || room == null) {
            return 0;
        }

        if (teacher.getDepartment() == null || room.getZone() == null) {
            return 0;  // No zone preference
        }

        // Map teacher's department to preferred zone
        String preferredZone = getDepartmentZone(teacher.getDepartment());

        if (preferredZone != null && preferredZone.equals(room.getZone())) {
            return 2;  // Moderate preference for department zone
        }

        return 0;  // No match
    }

    /**
     * Map department name to default zone
     * This provides the standard department-to-zone mapping
     *
     * @param department Department name
     * @return Preferred zone name, or null if no mapping exists
     */
    public String getDepartmentZone(String department) {
        if (department == null) return null;

        String deptLower = department.toLowerCase();

        if (deptLower.contains("math")) {
            return "Math Wing";
        } else if (deptLower.contains("science")) {
            return "Science Wing";
        } else if (deptLower.contains("english") || deptLower.contains("language arts")) {
            return "English Wing";
        } else if (deptLower.contains("social") || deptLower.contains("history")) {
            return "Social Studies Wing";
        } else if (deptLower.contains("physical education") || deptLower.contains("pe")) {
            return "Athletics Building";
        } else if (deptLower.contains("art") || deptLower.contains("music") || deptLower.contains("drama")) {
            return "Arts Building";
        } else if (deptLower.contains("technology") || deptLower.contains("computer")) {
            return "Technology Wing";
        } else if (deptLower.contains("vocational") || deptLower.contains("career")) {
            return "Vocational Building";
        }

        return null;  // No default zone for this department
    }

    /**
     * Calculate travel penalty between two rooms
     * Used by OptaPlanner to minimize teacher travel between consecutive periods
     *
     * @param room1 First room
     * @param room2 Second room
     * @return Penalty score (0 = same room, 1 = same zone, 3 = same building, 5 = different building)
     */
    public int calculateTravelPenalty(Room room1, Room room2) {
        if (room1 == null || room2 == null) {
            return 0;  // No penalty if rooms not assigned
        }

        // Same room = no penalty
        if (room1.getId().equals(room2.getId())) {
            return 0;
        }

        // Same zone = minimal penalty (rooms are close)
        if (room1.getZone() != null && room1.getZone().equals(room2.getZone())) {
            return 1;
        }

        // Same building, different zone = moderate penalty
        if (room1.getBuilding() != null && room1.getBuilding().equals(room2.getBuilding())) {
            return 3;
        }

        // Different building = high penalty (teacher must walk across campus)
        return 5;
    }

    /**
     * Get all distinct zone names in the database
     *
     * @return List of zone names, sorted alphabetically
     */
    public List<String> getAllZones() {
        return roomRepository.findAllZones();
    }

    /**
     * Get all distinct building names in the database
     *
     * @return List of building names, sorted alphabetically
     */
    public List<String> getAllBuildings() {
        return roomRepository.findAllBuildings();
    }

    /**
     * Get all rooms in a specific zone
     *
     * @param zone Zone name
     * @return List of rooms in that zone
     */
    public List<Room> getRoomsByZone(String zone) {
        if (zone == null || zone.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return roomRepository.findByZone(zone);
    }

    /**
     * Get all rooms in a specific building and floor
     *
     * @param building Building name
     * @param floor Floor number
     * @return List of rooms matching criteria
     */
    public List<Room> getRoomsByBuildingAndFloor(String building, Integer floor) {
        if (building == null || floor == null) {
            return new ArrayList<>();
        }
        return roomRepository.findByBuildingAndFloor(building, floor);
    }
}
