package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.CourseRoomAssignment;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.domain.ScheduleSlot;
import com.heronix.scheduler.model.domain.TimeSlot;
import com.heronix.scheduler.model.enums.UsagePattern;
import com.heronix.scheduler.repository.CourseRoomAssignmentRepository;
import com.heronix.scheduler.repository.ScheduleSlotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Multi-Room Scheduling Service
 * Phase 6E: Multi-Room Courses
 *
 * Manages scheduling logic for courses that use multiple rooms simultaneously.
 * Handles validation, capacity calculation, proximity checks, and room availability.
 *
 * @since Phase 6E - December 3, 2025
 */
@Slf4j
@Service
@Transactional
public class MultiRoomSchedulingService {

    @Autowired
    private CourseRoomAssignmentRepository assignmentRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    // ========================================================================
    // ROOM ASSIGNMENT MANAGEMENT
    // ========================================================================

    /**
     * Assign multiple rooms to a course
     */
    public void assignRoomsToCourse(Course course, List<CourseRoomAssignment> assignments) {
        log.info("Assigning {} rooms to course {}", assignments.size(), course.getCourseCode());

        // Validate assignments
        for (CourseRoomAssignment assignment : assignments) {
            if (assignment.getCourse() == null) {
                assignment.setCourse(course);
            }
            if (assignment.getPriority() == null) {
                assignment.setPriority(1);
            }
            if (assignment.getActive() == null) {
                assignment.setActive(true);
            }
        }

        // Save assignments
        assignmentRepository.saveAll(assignments);

        // Update course flag
        if (assignments.size() > 1) {
            course.setUsesMultipleRooms(true);
        }

        log.info("Successfully assigned rooms to course {}", course.getCourseCode());
    }

    /**
     * Get all active room assignments for a course
     */
    public List<CourseRoomAssignment> getActiveAssignments(Course course) {
        return assignmentRepository.findByCourseAndActiveTrue(course);
    }

    /**
     * Get primary room for a course
     */
    public Room getPrimaryRoom(Course course) {
        Optional<CourseRoomAssignment> primary = assignmentRepository.findPrimaryRoomAssignment(course);
        return primary.map(CourseRoomAssignment::getRoom).orElse(null);
    }

    /**
     * Get effective rooms for a course on a specific day
     * Respects usage patterns (alternating, odd/even, etc.)
     */
    public List<Room> getEffectiveRooms(Course course, DayOfWeek dayOfWeek, LocalDate date) {
        List<CourseRoomAssignment> assignments = getActiveAssignments(course);

        if (assignments.isEmpty()) {
            return new ArrayList<>();
        }

        return assignments.stream()
                .filter(assignment -> isRoomUsedOnDate(assignment, dayOfWeek, date))
                .map(CourseRoomAssignment::getRoom)
                .collect(Collectors.toList());
    }

    /**
     * Check if a room is used on a specific date based on usage pattern
     */
    private boolean isRoomUsedOnDate(CourseRoomAssignment assignment, DayOfWeek dayOfWeek, LocalDate date) {
        UsagePattern pattern = assignment.getUsagePattern();

        if (pattern == null || pattern == UsagePattern.ALWAYS) {
            return true;
        }

        switch (pattern) {
            case ODD_DAYS:
                return date.getDayOfMonth() % 2 == 1;

            case EVEN_DAYS:
                return date.getDayOfMonth() % 2 == 0;

            case ALTERNATING_DAYS:
                // Simple alternating based on day of year
                return date.getDayOfYear() % 2 == 0;

            case SPECIFIC_DAYS:
                // Check if day of week matches specific days
                String specificDays = assignment.getSpecificDays();
                if (specificDays != null && !specificDays.isEmpty()) {
                    return specificDays.contains(dayOfWeek.name());
                }
                return false;

            case WEEKLY_ROTATION:
                // Check if it's the right week (simple week number mod 2)
                return date.getDayOfYear() / 7 % 2 == 0;

            case FIRST_HALF:
            case SECOND_HALF:
                // Time-based patterns always apply (checked during specific time)
                return true;

            default:
                return true;
        }
    }

    // ========================================================================
    // CAPACITY CALCULATION
    // ========================================================================

    /**
     * Calculate total capacity across all active room assignments
     */
    public int calculateTotalCapacity(List<CourseRoomAssignment> assignments) {
        return assignments.stream()
                .filter(CourseRoomAssignment::isUsable)
                .map(CourseRoomAssignment::getRoom)
                .filter(room -> room != null)
                .mapToInt(room -> room.getCapacity() != null ? room.getCapacity() : 0)
                .sum();
    }

    /**
     * Calculate capacity for a course on a specific day
     */
    public int calculateCapacityForDate(Course course, DayOfWeek dayOfWeek, LocalDate date) {
        List<Room> effectiveRooms = getEffectiveRooms(course, dayOfWeek, date);
        return effectiveRooms.stream()
                .mapToInt(room -> room.getCapacity() != null ? room.getCapacity() : 0)
                .sum();
    }

    // ========================================================================
    // ROOM AVAILABILITY
    // ========================================================================

    /**
     * Check if all required rooms are available at the same time
     */
    public boolean areRoomsAvailable(List<Room> rooms, TimeSlot timeSlot, DayOfWeek dayOfWeek) {
        for (Room room : rooms) {
            if (!isRoomAvailable(room, timeSlot, dayOfWeek)) {
                log.debug("Room {} not available at {} on {}", room.getRoomNumber(), timeSlot, dayOfWeek);
                return false;
            }
        }
        return true;
    }

    /**
     * Check if a single room is available
     */
    private boolean isRoomAvailable(Room room, TimeSlot timeSlot, DayOfWeek dayOfWeek) {
        // Check if room is already assigned at this time
        List<ScheduleSlot> existingSlots = scheduleSlotRepository.findRoomTimeConflicts(
                room.getId(), dayOfWeek, timeSlot.getStartTime(), timeSlot.getEndTime());

        return existingSlots.isEmpty();
    }

    // ========================================================================
    // ROOM PROXIMITY
    // ========================================================================

    /**
     * Calculate walking distance/time between rooms in minutes
     * Based on building, floor, and zone proximity
     */
    public int calculateRoomProximity(Room room1, Room room2) {
        if (room1 == null || room2 == null) {
            return Integer.MAX_VALUE;
        }

        // Same room = 0 minutes
        if (room1.getId().equals(room2.getId())) {
            return 0;
        }

        int distance = 0;

        // Different buildings = 5 minutes base
        if (!isSameBuilding(room1, room2)) {
            distance += 5;
        }

        // Different floors = 2 minutes
        if (!isSameFloor(room1, room2)) {
            distance += 2;
        }

        // Different zones = 3 minutes
        if (!isSameZone(room1, room2)) {
            distance += 3;
        }

        // If same building, floor, and zone = 1 minute
        if (distance == 0) {
            distance = 1;
        }

        return distance;
    }

    /**
     * Check if rooms are within acceptable proximity
     */
    public boolean areRoomsNearby(List<Room> rooms, int maxDistanceMinutes) {
        if (rooms.size() <= 1) {
            return true;
        }

        // Check all pairs of rooms
        for (int i = 0; i < rooms.size() - 1; i++) {
            for (int j = i + 1; j < rooms.size(); j++) {
                int distance = calculateRoomProximity(rooms.get(i), rooms.get(j));
                if (distance > maxDistanceMinutes) {
                    log.debug("Rooms {} and {} are {} minutes apart (max: {})",
                            rooms.get(i).getRoomNumber(),
                            rooms.get(j).getRoomNumber(),
                            distance,
                            maxDistanceMinutes);
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isSameBuilding(Room room1, Room room2) {
        String building1 = room1.getBuilding();
        String building2 = room2.getBuilding();

        if (building1 == null && building2 == null) return true;
        if (building1 == null || building2 == null) return false;

        return building1.equalsIgnoreCase(building2);
    }

    private boolean isSameFloor(Room room1, Room room2) {
        Integer floor1 = room1.getFloor();
        Integer floor2 = room2.getFloor();

        if (floor1 == null && floor2 == null) return true;
        if (floor1 == null || floor2 == null) return false;

        return floor1.equals(floor2);
    }

    private boolean isSameZone(Room room1, Room room2) {
        String zone1 = room1.getZone();
        String zone2 = room2.getZone();

        if (zone1 == null && zone2 == null) return true;
        if (zone1 == null || zone2 == null) return false;

        return zone1.equalsIgnoreCase(zone2);
    }

    // ========================================================================
    // VALIDATION
    // ========================================================================

    /**
     * Validate multi-room assignment for a course
     * Checks: availability, capacity, proximity
     */
    public ValidationResult validateMultiRoomAssignment(
            Course course,
            List<Room> rooms,
            TimeSlot timeSlot,
            DayOfWeek dayOfWeek) {

        ValidationResult result = new ValidationResult();

        // Check if rooms are available
        if (!areRoomsAvailable(rooms, timeSlot, dayOfWeek)) {
            result.addError("One or more rooms are not available at the specified time");
        }

        // Check capacity
        int totalCapacity = rooms.stream()
                .mapToInt(room -> room.getCapacity() != null ? room.getCapacity() : 0)
                .sum();

        if (course.getMaxStudents() != null && totalCapacity < course.getMaxStudents()) {
            result.addWarning(String.format(
                    "Total capacity (%d) may be insufficient for max students (%d)",
                    totalCapacity, course.getMaxStudents()));
        }

        // Check proximity
        if (course.getMaxRoomDistanceMinutes() != null) {
            if (!areRoomsNearby(rooms, course.getMaxRoomDistanceMinutes())) {
                result.addWarning(String.format(
                        "Rooms exceed maximum distance of %d minutes",
                        course.getMaxRoomDistanceMinutes()));
            }
        }

        // Check for at least one primary room
        boolean hasPrimary = assignmentRepository.findPrimaryRoomAssignment(course).isPresent();
        if (!hasPrimary) {
            result.addError("Course must have at least one primary room assignment");
        }

        return result;
    }

    /**
     * Check if a course uses multiple rooms
     */
    public boolean usesMultipleRooms(Course course) {
        return Boolean.TRUE.equals(course.getUsesMultipleRooms())
                && assignmentRepository.countActiveByCourse(course) > 1;
    }

    // ========================================================================
    // VALIDATION RESULT HELPER CLASS
    // ========================================================================

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (!errors.isEmpty()) {
                sb.append("Errors: ").append(String.join(", ", errors));
            }
            if (!warnings.isEmpty()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append("Warnings: ").append(String.join(", ", warnings));
            }
            return sb.toString();
        }
    }
}
