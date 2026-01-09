package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.domain.Teacher;
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
 * Intelligent Teacher Assignment Service
 *
 * Automatically assigns:
 * 1. Courses to teachers based on certifications and subject expertise
 * 2. Rooms to teachers based on subject requirements
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since December 12, 2025
 */
@Service
@Slf4j
public class IntelligentTeacherAssignmentService {

    @Autowired
    private SISDataService sisDataService;
    @Autowired
    private RoomRepository roomRepository;

    /**
     * Auto-assign courses to teachers based on certifications
     *
     * @return Map with assignment statistics
     */
    @Transactional
    public Map<String, Object> autoAssignCoursesByCertifications() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("AUTO-ASSIGNING COURSES BASED ON TEACHER CERTIFICATIONS");
        log.info("═══════════════════════════════════════════════════════════");

        Map<String, Object> results = new HashMap<>();
        int totalAssigned = 0;
        int totalFailed = 0;

        // Get all active teachers and unassigned courses
        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(t -> t != null && Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());
        // ✅ NULL SAFE: Filter null courses before checking teacher/active status
        List<Course> unassignedCourses = sisDataService.getAllCourses().stream()
            .filter(c -> c != null && c.getTeacher() == null && c.isActive())
            .collect(Collectors.toList());

        log.info("Found {} active teachers", teachers.size());
        log.info("Found {} unassigned courses", unassignedCourses.size());

        // Group teachers by their certifications and subjects
        Map<String, List<Teacher>> teachersBySubject = new HashMap<>();
        // ✅ NULL SAFE: Filter null teachers before processing
        for (Teacher teacher : teachers) {
            if (teacher == null) continue;

            // Use department as primary subject
            if (teacher.getDepartment() != null) {
                teachersBySubject.computeIfAbsent(teacher.getDepartment().toLowerCase(), k -> new ArrayList<>())
                    .add(teacher);
            }

            // Add teachers to subject groups based on certifications
            if (teacher.getCertifications() != null) {
                for (String cert : teacher.getCertifications()) {
                    // ✅ NULL SAFE: Check certification string exists
                    if (cert == null) continue;
                    String certLower = cert.toLowerCase();
                    teachersBySubject.computeIfAbsent(certLower, k -> new ArrayList<>()).add(teacher);
                }
            }
        }

        log.info("\nTeacher distribution by subject:");
        teachersBySubject.forEach((subject, teacherList) ->
            log.info("  {} -> {} teachers", subject, teacherList.size()));

        // Assign courses based on subject matching
        // ✅ NULL SAFE: Filter null courses before processing
        for (Course course : unassignedCourses) {
            if (course == null) continue;

            Teacher assignedTeacher = findBestTeacherForCourse(course, teachersBySubject, teachers);

            if (assignedTeacher != null) {
                course.setTeacher(assignedTeacher);
                // Note: Course teacher assignment should be synced back to SIS via API
                totalAssigned++;
                // ✅ NULL SAFE: Safe field extraction with defaults
                log.debug("✅ Assigned '{}' to {} {}",
                    course.getCourseName() != null ? course.getCourseName() : "Unknown",
                    assignedTeacher.getFirstName() != null ? assignedTeacher.getFirstName() : "Unknown",
                    assignedTeacher.getLastName() != null ? assignedTeacher.getLastName() : "Unknown");
            } else {
                totalFailed++;
                log.warn("⚠️  No qualified teacher found for: {} ({})",
                    course.getCourseName() != null ? course.getCourseName() : "Unknown",
                    course.getSubject() != null ? course.getSubject() : "Unknown");
            }
        }

        log.info("\n═══════════════════════════════════════════════════════════");
        log.info("COURSE ASSIGNMENT COMPLETE");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("✅ Successfully assigned: {} courses", totalAssigned);
        log.info("⚠️  Failed to assign: {} courses", totalFailed);
        log.info("═══════════════════════════════════════════════════════════\n");

        results.put("totalAssigned", totalAssigned);
        results.put("totalFailed", totalFailed);
        results.put("totalProcessed", unassignedCourses.size());

        return results;
    }

    /**
     * Find the best teacher for a course based on:
     * 1. Matching certifications
     * 2. Department/subject expertise
     * 3. Current workload (prefer teachers with fewer courses)
     */
    private Teacher findBestTeacherForCourse(Course course,
                                             Map<String, List<Teacher>> teachersBySubject,
                                             List<Teacher> allTeachers) {
        String courseSubject = course.getSubject();
        if (courseSubject == null || courseSubject.trim().isEmpty()) {
            return null;
        }

        String subjectLower = courseSubject.toLowerCase();
        List<Teacher> candidates = new ArrayList<>();

        // 1. Try exact subject match
        if (teachersBySubject.containsKey(subjectLower)) {
            candidates.addAll(teachersBySubject.get(subjectLower));
        }

        // 2. Try partial subject matches
        if (candidates.isEmpty()) {
            for (Map.Entry<String, List<Teacher>> entry : teachersBySubject.entrySet()) {
                String key = entry.getKey();
                if (key.contains(subjectLower) || subjectLower.contains(key)) {
                    candidates.addAll(entry.getValue());
                }
            }
        }

        // 3. Try matching by department
        if (candidates.isEmpty()) {
            candidates = allTeachers.stream()
                .filter(t -> t.getDepartment() != null &&
                            matchesSubject(t.getDepartment(), courseSubject))
                .collect(Collectors.toList());
        }

        // If we have candidates, select the one with the least workload
        if (!candidates.isEmpty()) {
            return candidates.stream()
                .min(Comparator.comparingInt(t ->
                    (t.getCourses() != null ? t.getCourses().size() : 0)))
                .orElse(null);
        }

        return null;
    }

    /**
     * Check if a teacher's department/certification matches a course subject
     */
    private boolean matchesSubject(String teacherSubject, String courseSubject) {
        if (teacherSubject == null || courseSubject == null) return false;

        String teacher = teacherSubject.toLowerCase();
        String course = courseSubject.toLowerCase();

        // Mathematics variations
        if ((teacher.contains("math") || teacher.contains("algebra") || teacher.contains("geometry") || teacher.contains("calculus")) &&
            (course.contains("math") || course.contains("algebra") || course.contains("geometry") || course.contains("calculus"))) {
            return true;
        }

        // Science variations
        if ((teacher.contains("science") || teacher.contains("biology") || teacher.contains("chemistry") || teacher.contains("physics")) &&
            (course.contains("science") || course.contains("biology") || course.contains("chemistry") || course.contains("physics"))) {
            return true;
        }

        // English/Language Arts variations
        if ((teacher.contains("english") || teacher.contains("language") || teacher.contains("literature") || teacher.contains("reading")) &&
            (course.contains("english") || course.contains("language") || course.contains("literature") || course.contains("reading"))) {
            return true;
        }

        // Social Studies variations
        if ((teacher.contains("social") || teacher.contains("history") || teacher.contains("government") || teacher.contains("geography")) &&
            (course.contains("social") || course.contains("history") || course.contains("government") || course.contains("geography"))) {
            return true;
        }

        // PE/Physical Education variations
        if ((teacher.contains("physical") || teacher.contains("pe") || teacher.contains("health") || teacher.contains("athletics")) &&
            (course.contains("physical") || course.contains("pe") || course.contains("health") || course.contains("athletics"))) {
            return true;
        }

        // Arts variations
        if ((teacher.contains("art") || teacher.contains("music") || teacher.contains("drama") || teacher.contains("theater")) &&
            (course.contains("art") || course.contains("music") || course.contains("drama") || course.contains("theater"))) {
            return true;
        }

        // World Languages
        if ((teacher.contains("spanish") || teacher.contains("french") || teacher.contains("language")) &&
            (course.contains("spanish") || course.contains("french") || course.contains("language"))) {
            return true;
        }

        // Technology/Computer Science
        if ((teacher.contains("technology") || teacher.contains("computer") || teacher.contains("coding") || teacher.contains("stem")) &&
            (course.contains("technology") || teacher.contains("computer") || course.contains("coding") || course.contains("stem"))) {
            return true;
        }

        // Direct match
        return teacher.equals(course) || teacher.contains(course) || course.contains(teacher);
    }

    /**
     * Auto-assign appropriate rooms to teachers based on their subject/department
     *
     * @return Map with assignment statistics
     */
    @Transactional
    public Map<String, Object> autoAssignRoomsToTeachers() {
        log.info("═══════════════════════════════════════════════════════════");
        log.info("AUTO-ASSIGNING ROOMS TO TEACHERS BASED ON SUBJECT");
        log.info("═══════════════════════════════════════════════════════════");

        Map<String, Object> results = new HashMap<>();
        int totalAssigned = 0;
        int totalFailed = 0;

        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(t -> t != null && Boolean.TRUE.equals(t.getActive()))
            .collect(Collectors.toList());
        // ✅ NULL SAFE: Filter null rooms before checking active/available status
        List<Room> availableRooms = roomRepository.findAll().stream()
            .filter(r -> r != null && r.getActive() && r.getAvailable())
            .collect(Collectors.toList());

        log.info("Found {} active teachers", teachers.size());
        log.info("Found {} available rooms", availableRooms.size());

        // Track assigned rooms to avoid duplicates
        Set<Long> assignedRoomIds = new HashSet<>();

        // ✅ NULL SAFE: Filter null teachers before processing
        for (Teacher teacher : teachers) {
            if (teacher == null) continue;

            // Skip if teacher already has a room
            if (teacher.getHomeRoom() != null) {
                continue;
            }

            Room bestRoom = findBestRoomForTeacher(teacher, availableRooms, assignedRoomIds);

            if (bestRoom != null) {
                teacher.setHomeRoom(bestRoom);
                // Note: Teacher home room assignment should be synced back to SIS via API
                assignedRoomIds.add(bestRoom.getId());
                totalAssigned++;

                // ✅ NULL SAFE: Safe field extraction with defaults
                log.info("✅ Assigned room {} ({}) to {} {} ({})",
                    bestRoom.getRoomNumber() != null ? bestRoom.getRoomNumber() : "Unknown",
                    bestRoom.getType() != null ? bestRoom.getType().getDisplayName() : "Unknown",
                    teacher.getFirstName() != null ? teacher.getFirstName() : "Unknown",
                    teacher.getLastName() != null ? teacher.getLastName() : "Unknown",
                    teacher.getDepartment() != null ? teacher.getDepartment() : "Unknown");
            } else {
                totalFailed++;
                log.warn("⚠️  No suitable room found for: {} {} ({})",
                    teacher.getFirstName() != null ? teacher.getFirstName() : "Unknown",
                    teacher.getLastName() != null ? teacher.getLastName() : "Unknown",
                    teacher.getDepartment() != null ? teacher.getDepartment() : "Unknown");
            }
        }

        log.info("\n═══════════════════════════════════════════════════════════");
        log.info("ROOM ASSIGNMENT COMPLETE");
        log.info("═══════════════════════════════════════════════════════════");
        log.info("✅ Successfully assigned: {} rooms", totalAssigned);
        log.info("⚠️  Failed to assign: {} rooms", totalFailed);
        log.info("═══════════════════════════════════════════════════════════\n");

        results.put("totalAssigned", totalAssigned);
        results.put("totalFailed", totalFailed);
        results.put("totalProcessed", teachers.size());

        return results;
    }

    /**
     * Find the best room for a teacher based on their department/subject
     */
    private Room findBestRoomForTeacher(Teacher teacher, List<Room> availableRooms, Set<Long> assignedRoomIds) {
        String department = teacher.getDepartment();
        if (department == null || department.trim().isEmpty()) {
            // Default to standard classroom
            return availableRooms.stream()
                .filter(r -> !assignedRoomIds.contains(r.getId()) && r.getType() == RoomType.CLASSROOM)
                .findFirst()
                .orElse(null);
        }

        String deptLower = department.toLowerCase();

        // Find rooms that match the teacher's subject
        List<Room> suitableRooms = availableRooms.stream()
            .filter(r -> !assignedRoomIds.contains(r.getId()))
            .filter(r -> r.getType() != null && r.getType().isSuitableForSubject(department))
            .collect(Collectors.toList());

        if (!suitableRooms.isEmpty()) {
            return suitableRooms.get(0);
        }

        // Fallback: Try to match room type to department
        RoomType preferredType = getPreferredRoomTypeForDepartment(department);
        if (preferredType != null) {
            return availableRooms.stream()
                .filter(r -> !assignedRoomIds.contains(r.getId()) && r.getType() == preferredType)
                .findFirst()
                .orElse(null);
        }

        // Last resort: Any available classroom
        return availableRooms.stream()
            .filter(r -> !assignedRoomIds.contains(r.getId()) && r.getType() == RoomType.CLASSROOM)
            .findFirst()
            .orElse(null);
    }

    /**
     * Get the preferred room type for a given department
     */
    private RoomType getPreferredRoomTypeForDepartment(String department) {
        if (department == null) return null;

        String dept = department.toLowerCase();

        // Science teachers -> Science Lab
        if (dept.contains("science") || dept.contains("biology") ||
            dept.contains("chemistry") || dept.contains("physics")) {
            return RoomType.SCIENCE_LAB;
        }

        // Computer Science teachers -> Computer Lab
        if (dept.contains("computer") || dept.contains("technology") || dept.contains("coding")) {
            return RoomType.COMPUTER_LAB;
        }

        // PE teachers -> Gymnasium
        if (dept.contains("physical") || dept.contains("pe") ||
            dept.contains("athletics") || dept.contains("fitness")) {
            return RoomType.GYMNASIUM;
        }

        // Art teachers -> Art Studio
        if (dept.contains("art") && !dept.contains("language")) {
            return RoomType.ART_STUDIO;
        }

        // Music teachers -> Music Room
        if (dept.contains("music") || dept.contains("band") || dept.contains("chorus")) {
            return RoomType.MUSIC_ROOM;
        }

        // Drama/Theater teachers -> Theater
        if (dept.contains("drama") || dept.contains("theater")) {
            return RoomType.THEATER;
        }

        // Culinary Arts teachers -> Culinary Lab
        if (dept.contains("culinary") || dept.contains("cooking")) {
            return RoomType.CULINARY_LAB;
        }

        // Shop/Industrial Arts -> Workshop
        if (dept.contains("shop") || dept.contains("industrial")) {
            return RoomType.WORKSHOP;
        }

        // Default: Standard classroom for Math, English, Social Studies, etc.
        return RoomType.CLASSROOM;
    }

    /**
     * Perform both auto-assignments (courses and rooms)
     *
     * @return Combined results
     */
    @Transactional
    public Map<String, Object> performCompleteAutoAssignment() {
        log.info("\n╔═══════════════════════════════════════════════════════════╗");
        log.info("║  INTELLIGENT TEACHER AUTO-ASSIGNMENT                     ║");
        log.info("╚═══════════════════════════════════════════════════════════╝\n");

        Map<String, Object> results = new HashMap<>();

        // Step 1: Assign courses based on certifications
        Map<String, Object> courseResults = autoAssignCoursesByCertifications();
        results.put("courseAssignment", courseResults);

        // Step 2: Assign rooms based on subject
        Map<String, Object> roomResults = autoAssignRoomsToTeachers();
        results.put("roomAssignment", roomResults);

        // Combined totals
        int totalCoursesAssigned = (int) courseResults.get("totalAssigned");
        int totalRoomsAssigned = (int) roomResults.get("totalAssigned");

        log.info("\n╔═══════════════════════════════════════════════════════════╗");
        log.info("║  AUTO-ASSIGNMENT SUMMARY                                 ║");
        log.info("╠═══════════════════════════════════════════════════════════╣");
        log.info("║  Courses Assigned: {}", String.format("%-40d║", totalCoursesAssigned));
        log.info("║  Rooms Assigned: {}", String.format("%-42d║", totalRoomsAssigned));
        log.info("╚═══════════════════════════════════════════════════════════╝\n");

        results.put("success", true);
        results.put("totalOperations", totalCoursesAssigned + totalRoomsAssigned);

        return results;
    }
}
