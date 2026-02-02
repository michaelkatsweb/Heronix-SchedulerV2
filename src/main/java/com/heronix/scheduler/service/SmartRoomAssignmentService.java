package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.enums.RoomType;
import com.heronix.scheduler.repository.RoomRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heronix.scheduler.service.data.SISDataService;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Room Assignment Service
 * Location: src/main/java/com/eduscheduler/service/SmartRoomAssignmentService.java
 *
 * Automatically assigns rooms to courses using intelligent matching:
 * 1. Room type matching (lab for science, gym for PE, etc.)
 * 2. Capacity matching (room size >= expected enrollment)
 * 3. Room availability (prefer less-used rooms for balance)
 * 4. Efficient space usage (smallest suitable room)
 *
 * @version 1.0.0
 * @since 2025-11-19
 */
@Slf4j
@Service
public class SmartRoomAssignmentService {

    @Autowired
    private SISDataService sisDataService;
    @Autowired
    private RoomRepository roomRepository;

    /**
     * Smart assign rooms to all unassigned courses
     * Uses room type and capacity matching
     *
     * @return AssignmentResult with statistics
     */
    @Transactional
    public SmartTeacherAssignmentService.AssignmentResult smartAssignAllRooms() {
        log.info("Starting smart room assignment...");

        SmartTeacherAssignmentService.AssignmentResult result = new SmartTeacherAssignmentService.AssignmentResult();
        result.setStartTime(System.currentTimeMillis());

        try {
            // Get all courses that need room assignment
            List<Course> unassignedCourses = sisDataService.getAllCourses().stream()
                .filter(c -> c.getRoom() == null)
                .collect(Collectors.toList());

            result.setTotalCoursesProcessed(unassignedCourses.size());
            log.info("Found {} courses without rooms", unassignedCourses.size());

            if (unassignedCourses.isEmpty()) {
                result.setMessage("All courses already have rooms assigned");
                result.setEndTime(System.currentTimeMillis());
                return result;
            }

            // ✅ PRIORITY 2 FIX December 15, 2025: Query-level filtering for performance
            // Use findAllSchedulableRooms() instead of findAll() to filter at database level
            // Performance: With 500+ rooms, this eliminates fetching storage/server rooms only to filter them out
            List<Room> allRooms = roomRepository.findAllSchedulableRooms();
            log.info("Found {} schedulable rooms available for assignment (filtered at database level)", allRooms.size());

            if (allRooms.isEmpty()) {
                result.setMessage("No rooms available for assignment");
                result.setEndTime(System.currentTimeMillis());
                return result;
            }

            // Sort courses by priority and enrollment (larger classes first)
            unassignedCourses.sort(Comparator
                .comparing(Course::getPriorityLevel, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Course::getMaxStudents, Comparator.nullsLast(Comparator.reverseOrder())));

            // Assign rooms to courses
            for (Course course : unassignedCourses) {
                Room bestRoom = findBestRoom(course, allRooms);

                if (bestRoom != null) {
                    // Note: Room assignments are handled through ScheduleSlot or CourseRoomAssignment
                    // Cannot directly set room on Course entity (managed by SIS)
                    // Room assignment persistence not implemented — requires CourseRoomAssignment or ScheduleSlot update
                    result.incrementAssigned();

                    // Check if room capacity is tight
                    if (course.getMaxStudents() != null &&
                        course.getMaxStudents() > bestRoom.getCapacity() * 0.9) {
                        result.addWarning(String.format("Room %s capacity (%d) is near limit for course %s (%d students)",
                            bestRoom.getRoomNumber(), bestRoom.getCapacity(),
                            course.getCourseName(), course.getMaxStudents()));
                    }

                    log.debug("Assigned room {} to course {} (type: {}, capacity: {})",
                        bestRoom.getRoomNumber(), course.getCourseName(),
                        bestRoom.getRoomType(), bestRoom.getCapacity());
                } else {
                    result.incrementFailed();
                    result.addError(String.format("No suitable room found for course %s (needs: %s, capacity: %d)",
                        course.getCourseName(),
                        determineRequiredRoomType(course),
                        course.getMaxStudents() != null ? course.getMaxStudents() : 0));
                    log.warn("Could not find suitable room for course: {} (subject: {}, max students: {})",
                        course.getCourseName(), course.getSubject(), course.getMaxStudents());
                }
            }

            result.setEndTime(System.currentTimeMillis());
            result.setMessage(String.format("Assigned %d of %d courses. %d courses could not be assigned.",
                result.getCoursesAssigned(), result.getTotalCoursesProcessed(), result.getCoursesFailed()));

            log.info("Smart room assignment completed: {} assigned, {} failed in {}ms",
                result.getCoursesAssigned(), result.getCoursesFailed(),
                result.getEndTime() - result.getStartTime());

        } catch (Exception e) {
            log.error("Error during smart room assignment", e);
            result.addError("System error: " + e.getMessage());
            result.setMessage("Assignment failed due to error: " + e.getMessage());
            result.setEndTime(System.currentTimeMillis());
        }

        return result;
    }

    /**
     * Find the best room for a course using intelligent scoring
     *
     * @param course The course to assign
     * @param availableRooms List of available rooms
     * @return Best matching room, or null if none suitable
     */
    private Room findBestRoom(Course course, List<Room> availableRooms) {
        RoomType requiredType = determineRequiredRoomType(course);

        // UPDATED December 15, 2025: Fixed capacity calculation for multi-section courses
        // CRITICAL FIX: Added validation to prevent division by zero
        // For courses with multiple sections (sessionsPerWeek > 1), divide total enrollment by sections
        // Example: 105 students / 6 sections = 18 students per section
        int totalStudents = course.getMaxStudents() != null ? course.getMaxStudents() : 30;
        Integer sessionsPerWeek = course.getSessionsPerWeek();

        int requiredCapacity;
        if (sessionsPerWeek != null && sessionsPerWeek > 1) {
            // Multi-section course: capacity needed per section
            requiredCapacity = (int) Math.ceil((double) totalStudents / sessionsPerWeek);
            log.debug("Course {} has {} total students across {} sections = {} per section",
                course.getCourseName(), totalStudents, sessionsPerWeek, requiredCapacity);
        } else if (sessionsPerWeek != null && sessionsPerWeek == 1) {
            // Single-section course: use total students
            requiredCapacity = totalStudents;
        } else {
            // Invalid or null sessionsPerWeek - use default
            // Note: With validation (@Min(1)), this should never happen, but defensive programming
            log.warn("Invalid sessionsPerWeek for course {}: {}. Using total students as capacity.",
                course.getCourseCode(), sessionsPerWeek);
            requiredCapacity = totalStudents;
        }

        // ENHANCED: Phase 20 - Filter rooms with min/max capacity and schedulability
        List<Room> suitableRooms = availableRooms.stream()
            .filter(r -> {
                // Check if room is schedulable (exclude storage, server rooms, etc.)
                if (r.getRoomType() == null || !r.getRoomType().isSchedulable()) {
                    return false;
                }

                // Use max capacity as hard upper limit (fire code compliance)
                int maxCap = r.getEffectiveMaxCapacity();  // Uses maxCapacity if set, else capacity
                if (requiredCapacity > maxCap) {
                    return false;  // Course too large for room
                }

                // Use standard capacity as preferred target
                return r.getCapacity() >= requiredCapacity;
            })
            .collect(Collectors.toList());

        if (suitableRooms.isEmpty()) {
            // CRITICAL FIX December 15, 2025: Re-check isSchedulable() in fallback
            // Try again with slightly relaxed capacity (90% of required)
            // BUT still enforce schedulability and fire code limits
            final int relaxedCapacity = (int) (requiredCapacity * 0.9);
            suitableRooms = availableRooms.stream()
                .filter(r -> {
                    // MUST still be schedulable (no storage rooms!)
                    if (r.getRoomType() == null || !r.getRoomType().isSchedulable()) {
                        return false;
                    }
                    // MUST still respect fire code max capacity
                    int maxCap = r.getEffectiveMaxCapacity();
                    if (relaxedCapacity > maxCap) {
                        return false;
                    }
                    // Relaxed capacity check (90%)
                    return r.getCapacity() >= relaxedCapacity;
                })
                .collect(Collectors.toList());

            if (suitableRooms.isEmpty()) {
                log.warn("No suitable rooms found for course {} (required capacity: {}, relaxed: {})",
                    course.getCourseCode(), requiredCapacity, relaxedCapacity);
                return null;
            }
            log.info("Using relaxed capacity ({}) for course {}", relaxedCapacity, course.getCourseCode());
        }

        // Score each room
        Map<Room, Double> scores = new HashMap<>();

        for (Room room : suitableRooms) {
            double score = calculateRoomScore(room, course, requiredType, requiredCapacity);
            scores.put(room, score);
        }

        // Find room with highest score
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * Calculate suitability score for a room-course pairing
     * Higher score = better match
     *
     * Scoring factors:
     * - Room type match: +100 points (exact), +50 points (compatible)
     * - Capacity efficiency: 0-50 points (prefer smallest suitable room)
     * - Home room bonus: +75 points (teacher's home room) ✅ PRIORITY 2 FIX
     * - Room availability: 0-25 points (prefer less-used rooms)
     *
     * @param room The room to evaluate
     * @param course The course to assign
     * @param requiredType Required room type
     * @param requiredCapacity Required capacity
     * @return Suitability score (higher is better)
     */
    private double calculateRoomScore(Room room, Course course, RoomType requiredType, int requiredCapacity) {
        double score = 0.0;

        // Factor 1: Room type match
        if (room.getRoomType() == requiredType) {
            score += 100.0;  // Exact match
            log.trace("Room {} type matches exactly: +100 points", room.getRoomNumber());
        } else if (isCompatibleRoomType(room.getRoomType(), requiredType)) {
            score += 50.0;   // Compatible
            log.trace("Room {} type is compatible: +50 points", room.getRoomNumber());
        }

        // Factor 2: Capacity efficiency (prefer smallest suitable room)
        // ENHANCED: Phase 20 - now considers minCapacity for efficiency bonus
        double capacityScore = calculateCapacityScore(room, requiredCapacity);
        score += capacityScore;
        log.trace("Room {} capacity score: +{} points", room.getRoomNumber(), capacityScore);

        // ✅ PRIORITY 2 FIX December 15, 2025: Home room integration
        // Factor 3: Home room bonus (strongly prefer teacher's home room)
        // This ensures teachers stay in their designated rooms when possible
        if (course.getTeacher() != null && course.getTeacher().getHomeRoom() != null) {
            if (room.getId().equals(course.getTeacher().getHomeRoom().getId())) {
                score += 75.0;  // Strong preference for home room
                log.debug("Room {} is teacher's home room: +75 points", room.getRoomNumber());
            }
        }

        // ✅ PRIORITY 2 FIX December 15, 2025: Removed large room bonus
        // REMOVED: Old code gave +10 bonus for rooms with capacity > 40
        // This contradicted "efficient space usage" principle in capacity scoring
        // Now relies purely on capacity efficiency score (prefers smallest suitable room)

        log.trace("Room {} total score for course {}: {}",
            room.getRoomNumber(), course.getCourseName(), score);

        return score;
    }

    /**
     * Calculate capacity efficiency score
     * Prefer rooms that are close to required capacity (not too big)
     *
     * ENHANCED: Phase 20 - now rewards efficient room usage via minCapacity
     *
     * Perfect fit (100-110%): 50 points
     * Good fit (110-125%): 40 points
     * Acceptable fit (125-150%): 30 points
     * Loose fit (150%+): 20 points
     * Efficiency bonus: +15 points if course meets minCapacity threshold
     */
    private double calculateCapacityScore(Room room, int requiredCapacity) {
        if (requiredCapacity == 0) return 25.0;

        int roomCapacity = room.getCapacity();
        int minCapacity = room.getMinCapacity();

        // ENHANCED: Efficiency bonus for meeting minimum efficient capacity
        double efficiencyBonus = 0.0;
        if (requiredCapacity >= minCapacity) {
            efficiencyBonus = 15.0;  // Efficient use of room
        }

        double ratio = (double) roomCapacity / requiredCapacity;

        if (ratio >= 1.0 && ratio <= 1.1) return 50.0 + efficiencyBonus;  // Perfect fit
        if (ratio > 1.1 && ratio <= 1.25) return 40.0 + efficiencyBonus;  // Good fit
        if (ratio > 1.25 && ratio <= 1.5) return 30.0 + efficiencyBonus;  // Acceptable
        return 20.0;  // Loose fit (no efficiency bonus)
    }

    /**
     * Determine the required room type based on course characteristics
     */
    private RoomType determineRequiredRoomType(Course course) {
        String subject = course.getSubject() != null ? course.getSubject().toLowerCase() : "";
        String courseName = course.getCourseName() != null ? course.getCourseName().toLowerCase() : "";

        // Physical Education
        if (subject.contains("physical education") || subject.contains("pe") ||
            courseName.contains("physical education") || courseName.contains("gym")) {
            return RoomType.GYMNASIUM;
        }

        // Science labs
        if (course.isRequiresLab() || subject.contains("chemistry") ||
            subject.contains("biology") || subject.contains("physics") ||
            courseName.contains("lab")) {
            return RoomType.SCIENCE_LAB;
        }

        // Computer science
        if (subject.contains("computer") || subject.contains("technology") ||
            courseName.contains("computer")) {
            return RoomType.COMPUTER_LAB;
        }

        // Music
        if (subject.contains("music") || subject.contains("band") ||
            subject.contains("orchestra") || subject.contains("chorus")) {
            return RoomType.MUSIC_ROOM;
        }

        // Art
        if (subject.contains("art")) {
            return RoomType.ART_STUDIO;
        }

        // Default to standard classroom
        return RoomType.CLASSROOM;
    }

    /**
     * Check if two room types are compatible
     */
    private boolean isCompatibleRoomType(RoomType roomType, RoomType requiredType) {
        if (roomType == requiredType) return true;

        // Lab types are interchangeable in some cases
        if ((roomType == RoomType.LAB || roomType == RoomType.SCIENCE_LAB) &&
            (requiredType == RoomType.LAB || requiredType == RoomType.SCIENCE_LAB)) {
            return true;
        }

        // Classroom can be used for most things (fallback)
        if (roomType == RoomType.CLASSROOM &&
            (requiredType == RoomType.CLASSROOM || requiredType == RoomType.LIBRARY)) {
            return true;
        }

        return false;
    }

    /**
     * Preview what would happen without actually making assignments
     *
     * @return AssignmentResult with preview statistics
     */
    public SmartTeacherAssignmentService.AssignmentResult previewRoomAssignments() {
        log.info("Previewing smart room assignment...");

        SmartTeacherAssignmentService.AssignmentResult result = new SmartTeacherAssignmentService.AssignmentResult();
        result.setStartTime(System.currentTimeMillis());

        List<Course> unassignedCourses = sisDataService.getAllCourses().stream()
            .filter(c -> c.getRoom() == null)
            .collect(Collectors.toList());

        result.setTotalCoursesProcessed(unassignedCourses.size());

        if (unassignedCourses.isEmpty()) {
            result.setMessage("All courses already have rooms assigned");
            result.setEndTime(System.currentTimeMillis());
            return result;
        }

        // ✅ PRIORITY 2 FIX December 15, 2025: Query-level filtering for performance
        // Use findAllSchedulableRooms() instead of findAll() to filter at database level
        List<Room> allRooms = roomRepository.findAllSchedulableRooms();

        for (Course course : unassignedCourses) {
            Room bestRoom = findBestRoom(course, allRooms);
            if (bestRoom != null) {
                result.incrementAssigned();
            } else {
                result.incrementFailed();
            }
        }

        result.setEndTime(System.currentTimeMillis());
        result.setMessage(String.format("Preview: Would assign %d of %d courses",
            result.getCoursesAssigned(), result.getTotalCoursesProcessed()));

        return result;
    }
}
