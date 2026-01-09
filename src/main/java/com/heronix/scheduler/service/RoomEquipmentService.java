package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Room Equipment Service
 * Phase 6D: Room Equipment Matching
 *
 * Manages equipment compatibility scoring between courses and rooms.
 * Provides logic to ensure courses are assigned to rooms with appropriate equipment.
 *
 * Scoring Algorithm:
 * - Base Score: 100 points
 * - Required room type mismatch: -100 (completely incompatible)
 * - Missing required projector: -30
 * - Missing required smartboard: -30
 * - Missing required computers: -40
 * - Missing additional equipment: -10 per item
 *
 * Penalty Mapping for OptaPlanner:
 * - 0 points  = Perfect match (100 score)
 * - 2 points  = Minor mismatch (70-99 score)
 * - 5 points  = Moderate mismatch (40-69 score)
 * - 10 points = Major mismatch (0-39 score)
 *
 * @since Phase 6D - December 3, 2025
 */
@Service
@Transactional
public class RoomEquipmentService {

    private static final Logger log = LoggerFactory.getLogger(RoomEquipmentService.class);

    // Penalty weights for missing equipment
    private static final int ROOM_TYPE_MISMATCH_PENALTY = 100;
    private static final int PROJECTOR_PENALTY = 30;
    private static final int SMARTBOARD_PENALTY = 30;
    private static final int COMPUTERS_PENALTY = 40;
    private static final int ADDITIONAL_EQUIPMENT_PENALTY = 10;

    /**
     * Calculate equipment compatibility score between a course and room
     *
     * @param course Course with equipment requirements
     * @param room Room with equipment available
     * @return Compatibility score from 0 (incompatible) to 100 (perfect match)
     */
    public int calculateCompatibilityScore(Course course, Room room) {
        if (course == null || room == null) {
            return 100;  // No requirements, perfect match
        }

        int score = 100;  // Start with perfect score
        int penalties = 0;

        // Check room type requirement (highest priority)
        if (course.getRequiredRoomType() != null) {
            if (room.getType() == null || !course.getRequiredRoomType().equals(room.getType())) {
                penalties += ROOM_TYPE_MISMATCH_PENALTY;
                log.debug("Room type mismatch: Course {} requires {}, Room {} is {}",
                        course.getCourseCode(),
                        course.getRequiredRoomType(),
                        room.getRoomNumber(),
                        room.getType());
            }
        }

        // Check projector requirement
        if (Boolean.TRUE.equals(course.getRequiresProjector())) {
            if (!Boolean.TRUE.equals(room.getHasProjector())) {
                penalties += PROJECTOR_PENALTY;
                log.debug("Room {} missing required projector for course {}",
                        room.getRoomNumber(), course.getCourseCode());
            }
        }

        // Check smartboard requirement
        if (Boolean.TRUE.equals(course.getRequiresSmartboard())) {
            if (!Boolean.TRUE.equals(room.getHasSmartboard())) {
                penalties += SMARTBOARD_PENALTY;
                log.debug("Room {} missing required smartboard for course {}",
                        room.getRoomNumber(), course.getCourseCode());
            }
        }

        // Check computers requirement
        if (Boolean.TRUE.equals(course.getRequiresComputers())) {
            if (!Boolean.TRUE.equals(room.getHasComputers())) {
                penalties += COMPUTERS_PENALTY;
                log.debug("Room {} missing required computers for course {}",
                        room.getRoomNumber(), course.getCourseCode());
            }
        }

        // Check additional equipment requirements
        List<String> missingEquipment = getMissingAdditionalEquipment(course, room);
        if (!missingEquipment.isEmpty()) {
            penalties += missingEquipment.size() * ADDITIONAL_EQUIPMENT_PENALTY;
            log.debug("Room {} missing additional equipment for course {}: {}",
                    room.getRoomNumber(), course.getCourseCode(), missingEquipment);
        }

        score = Math.max(0, score - penalties);  // Ensure score doesn't go negative
        return score;
    }

    /**
     * Check if room meets ALL required equipment needs for a course
     *
     * @param course Course with equipment requirements
     * @param room Room with equipment available
     * @return true if room meets all requirements, false otherwise
     */
    public boolean meetsRequirements(Course course, Room room) {
        if (course == null || room == null) {
            return true;  // No requirements
        }

        // Check room type (most critical requirement)
        if (course.getRequiredRoomType() != null) {
            if (room.getType() == null || !course.getRequiredRoomType().equals(room.getType())) {
                return false;
            }
        }

        // Check projector
        if (Boolean.TRUE.equals(course.getRequiresProjector())) {
            if (!Boolean.TRUE.equals(room.getHasProjector())) {
                return false;
            }
        }

        // Check smartboard
        if (Boolean.TRUE.equals(course.getRequiresSmartboard())) {
            if (!Boolean.TRUE.equals(room.getHasSmartboard())) {
                return false;
            }
        }

        // Check computers
        if (Boolean.TRUE.equals(course.getRequiresComputers())) {
            if (!Boolean.TRUE.equals(room.getHasComputers())) {
                return false;
            }
        }

        // Additional equipment check (SOFT requirement, don't fail on this)
        // We return true even if additional equipment is missing
        // because additional equipment can sometimes be moved between rooms

        return true;
    }

    /**
     * Get list of missing equipment
     *
     * @param course Course with equipment requirements
     * @param room Room with equipment available
     * @return List of missing equipment names
     */
    public List<String> getMissingEquipment(Course course, Room room) {
        List<String> missing = new ArrayList<>();

        if (course == null || room == null) {
            return missing;
        }

        // Check room type
        if (course.getRequiredRoomType() != null) {
            if (room.getType() == null || !course.getRequiredRoomType().equals(room.getType())) {
                missing.add("Room Type: " + course.getRequiredRoomType());
            }
        }

        // Check projector
        if (Boolean.TRUE.equals(course.getRequiresProjector())) {
            if (!Boolean.TRUE.equals(room.getHasProjector())) {
                missing.add("Projector");
            }
        }

        // Check smartboard
        if (Boolean.TRUE.equals(course.getRequiresSmartboard())) {
            if (!Boolean.TRUE.equals(room.getHasSmartboard())) {
                missing.add("Smartboard");
            }
        }

        // Check computers
        if (Boolean.TRUE.equals(course.getRequiresComputers())) {
            if (!Boolean.TRUE.equals(room.getHasComputers())) {
                missing.add("Computers");
            }
        }

        // Check additional equipment
        List<String> missingAdditional = getMissingAdditionalEquipment(course, room);
        missing.addAll(missingAdditional);

        return missing;
    }

    /**
     * Get equipment mismatch penalty for OptaPlanner constraint
     * Maps compatibility score to penalty points
     *
     * @param course Course with equipment requirements
     * @param room Room with equipment available
     * @return Penalty score (0 = perfect, 2 = minor, 5 = moderate, 10 = major)
     */
    public int getEquipmentPenalty(Course course, Room room) {
        int score = calculateCompatibilityScore(course, room);

        // Map score to penalty
        if (score >= 100) {
            return 0;   // Perfect match
        } else if (score >= 70) {
            return 2;   // Minor mismatch (missing non-critical equipment)
        } else if (score >= 40) {
            return 5;   // Moderate mismatch (missing some important equipment)
        } else {
            return 10;  // Major mismatch (missing critical equipment like room type)
        }
    }

    /**
     * Get list of missing additional equipment
     *
     * @param course Course with additional equipment requirements
     * @param room Room with equipment available
     * @return List of missing additional equipment items
     */
    private List<String> getMissingAdditionalEquipment(Course course, Room room) {
        List<String> missing = new ArrayList<>();

        // Parse course additional equipment requirements
        List<String> requiredItems = parseEquipmentList(course.getAdditionalEquipment());
        if (requiredItems.isEmpty()) {
            return missing;  // No additional requirements
        }

        // Parse room equipment
        List<String> availableItems = parseEquipmentList(room.getEquipment());

        // Find missing items
        for (String required : requiredItems) {
            boolean found = availableItems.stream()
                    .anyMatch(available -> available.equalsIgnoreCase(required.trim()));
            if (!found) {
                missing.add(required.trim());
            }
        }

        return missing;
    }

    /**
     * Parse comma-separated equipment list
     *
     * @param equipmentString Comma-separated equipment string
     * @return List of equipment items (trimmed, non-empty)
     */
    private List<String> parseEquipmentList(String equipmentString) {
        if (equipmentString == null || equipmentString.trim().isEmpty()) {
            return new ArrayList<>();
        }

        return Arrays.stream(equipmentString.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Get a human-readable summary of equipment requirements
     *
     * @param course Course to summarize
     * @return Summary string of equipment requirements
     */
    public String getEquipmentSummary(Course course) {
        if (course == null) {
            return "No requirements";
        }

        List<String> requirements = new ArrayList<>();

        if (course.getRequiredRoomType() != null) {
            requirements.add("Room: " + course.getRequiredRoomType());
        }

        if (Boolean.TRUE.equals(course.getRequiresProjector())) {
            requirements.add("Projector");
        }

        if (Boolean.TRUE.equals(course.getRequiresSmartboard())) {
            requirements.add("Smartboard");
        }

        if (Boolean.TRUE.equals(course.getRequiresComputers())) {
            requirements.add("Computers");
        }

        if (course.getAdditionalEquipment() != null && !course.getAdditionalEquipment().trim().isEmpty()) {
            requirements.add(course.getAdditionalEquipment());
        }

        if (requirements.isEmpty()) {
            return "No requirements";
        }

        return String.join(", ", requirements);
    }

    /**
     * Check if a course has any equipment requirements
     *
     * @param course Course to check
     * @return true if course has equipment requirements, false otherwise
     */
    public boolean hasEquipmentRequirements(Course course) {
        if (course == null) {
            return false;
        }

        return course.getRequiredRoomType() != null
                || Boolean.TRUE.equals(course.getRequiresProjector())
                || Boolean.TRUE.equals(course.getRequiresSmartboard())
                || Boolean.TRUE.equals(course.getRequiresComputers())
                || (course.getAdditionalEquipment() != null && !course.getAdditionalEquipment().trim().isEmpty());
    }
}
