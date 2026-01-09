package com.heronix.scheduler.service.analysis;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.repository.RoomRepository;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Resource Capacity Analyzer - FIXES #6 and #7
 * Location:
 * src/main/java/com/eduscheduler/service/analysis/ResourceCapacityAnalyzer.java
 * 
 * Analyzes if there are sufficient teachers, rooms, and time slots
 * to accommodate all courses in the schedule.
 * 
 * ISSUE FIXED:
 * - System was trying to schedule 450 slots with only 86 teachers
 * - Math: 86 teachers × 5 periods/day = 430 maximum capacity (INSUFFICIENT!)
 * - This analyzer detects capacity issues BEFORE solver runs
 * 
 * @author Heronix Scheduling System Team
 * @version 1.0.0 - FIXES #6 and #7
 * @since 2025-10-28
 */
@Service
@Slf4j
public class ResourceCapacityAnalyzer {

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;

    /**
     * Result of capacity analysis
     */
    public static class CapacityAnalysisResult {
        public boolean sufficient;
        public int requiredSlots;
        public int availableTeacherSlots;
        public int availableRoomSlots;
        public int shortfallTeachers;
        public int shortfallRooms;
        public List<String> warnings;
        public List<String> recommendations;

        public CapacityAnalysisResult() {
            this.warnings = new ArrayList<>();
            this.recommendations = new ArrayList<>();
        }
    }

    /**
     * Analyze if resources are sufficient for schedule generation
     * 
     * @param periodsPerDay Number of periods per day (e.g., 7 for traditional, 4
     *                      for block)
     * @param daysPerWeek   Number of school days per week (typically 5)
     * @return Analysis result with warnings and recommendations
     */
    public CapacityAnalysisResult analyzeCapacity(int periodsPerDay, int daysPerWeek) {
        CapacityAnalysisResult result = new CapacityAnalysisResult();

        // Get current resources
        List<Teacher> teachers = sisDataService.getAllTeachers();
        List<Course> courses = sisDataService.getAllCourses().stream().filter(Course::getActive).toList();
        List<Room> rooms = roomRepository.findAll();

        int teacherCount = teachers.size();
        int roomCount = rooms.size();

        log.info("======================================");
        log.info("   RESOURCE CAPACITY ANALYSIS");
        log.info("======================================");

        // Calculate required slots
        int requiredSlots = 0;
        for (Course course : courses) {
            int sessionsPerWeek = course.getSessionsPerWeek() != null ? course.getSessionsPerWeek() : 5;
            requiredSlots += sessionsPerWeek;
        }
        result.requiredSlots = requiredSlots;

        log.info("📊 DEMAND:");
        log.info("   - Active Courses: {}", courses.size());
        log.info("   - Total Slots Required: {}", requiredSlots);

        // Calculate available teacher capacity
        int totalTeacherCapacity = 0;
        int totalTeacherHoursPerDay = 0;

        for (Teacher teacher : teachers) {
            // Default: assume teachers can teach up to 6 periods per day (1 period for
            // prep/lunch)
            Integer maxHours = teacher.getMaxHoursPerWeek();
            int dailyCapacity;

            if (maxHours != null) {
                // Convert weekly hours to daily periods
                dailyCapacity = Math.min(maxHours / daysPerWeek, periodsPerDay - 1);
            } else {
                // Default: periodsPerDay - 1 (leaving 1 for lunch/prep)
                dailyCapacity = periodsPerDay - 1;
            }

            totalTeacherHoursPerDay += dailyCapacity;
            totalTeacherCapacity += dailyCapacity * daysPerWeek; // Weekly capacity
        }

        result.availableTeacherSlots = totalTeacherCapacity;

        log.info("👨‍🏫 TEACHER CAPACITY:");
        log.info("   - Teachers Available: {}", teacherCount);
        log.info("   - Avg Periods/Day per Teacher: {}",
                teacherCount > 0 ? totalTeacherHoursPerDay / teacherCount : 0);
        log.info("   - Total Weekly Capacity: {} slots", totalTeacherCapacity);

        // Calculate available room capacity
        int roomCapacity = roomCount * periodsPerDay * daysPerWeek;
        result.availableRoomSlots = roomCapacity;

        log.info("🏫 ROOM CAPACITY:");
        log.info("   - Rooms Available: {}", roomCount);
        log.info("   - Periods per Day: {}", periodsPerDay);
        log.info("   - Total Weekly Capacity: {} slots", roomCapacity);

        // Check sufficiency
        boolean teachersSufficient = totalTeacherCapacity >= requiredSlots;
        boolean roomsSufficient = roomCapacity >= requiredSlots;
        result.sufficient = teachersSufficient && roomsSufficient;

        log.info("======================================");
        log.info("📈 ANALYSIS RESULTS:");

        if (teachersSufficient) {
            log.info("   ✅ TEACHERS: Sufficient ({} available vs {} needed)",
                    totalTeacherCapacity, requiredSlots);
        } else {
            result.shortfallTeachers = requiredSlots - totalTeacherCapacity;
            log.warn("   ❌ TEACHERS: INSUFFICIENT ({} available vs {} needed)",
                    totalTeacherCapacity, requiredSlots);
            log.warn("      SHORTFALL: {} slots", result.shortfallTeachers);
            result.warnings.add(String.format(
                    "Teacher capacity insufficient: %d slots short", result.shortfallTeachers));
        }

        if (roomsSufficient) {
            log.info("   ✅ ROOMS: Sufficient ({} available vs {} needed)",
                    roomCapacity, requiredSlots);
        } else {
            result.shortfallRooms = requiredSlots - roomCapacity;
            log.warn("   ❌ ROOMS: INSUFFICIENT ({} available vs {} needed)",
                    roomCapacity, requiredSlots);
            log.warn("      SHORTFALL: {} slots", result.shortfallRooms);
            result.warnings.add(String.format(
                    "Room capacity insufficient: %d slots short", result.shortfallRooms));
        }

        // Generate recommendations
        generateRecommendations(result, teacherCount, roomCount, courses.size(), periodsPerDay);

        log.info("======================================");

        return result;
    }

    /**
     * Generate recommendations for capacity issues
     */
    private void generateRecommendations(CapacityAnalysisResult result, int teacherCount,
            int roomCount, int courseCount, int periodsPerDay) {
        if (!result.sufficient) {
            log.info("💡 RECOMMENDATIONS:");

            if (result.shortfallTeachers > 0) {
                int additionalTeachersNeeded = (int) Math.ceil(result.shortfallTeachers / (double) (periodsPerDay - 1));
                result.recommendations.add(String.format(
                        "Option 1: Add %d more teachers", additionalTeachersNeeded));
                log.info("   1. Add {} more teachers", additionalTeachersNeeded);

                result.recommendations.add(
                        "Option 2: Increase max hours per week for existing teachers");
                log.info("   2. Increase max hours/week for existing teachers");

                int coursesToReduce = (int) Math.ceil(result.shortfallTeachers / 5.0);
                result.recommendations.add(String.format(
                        "Option 3: Reduce course count by %d courses", coursesToReduce));
                log.info("   3. Reduce course count by {} courses", coursesToReduce);

                result.recommendations.add(
                        "Option 4: Reduce sessions per week for some courses");
                log.info("   4. Reduce sessions/week for some courses");
            }

            if (result.shortfallRooms > 0) {
                int additionalRoomsNeeded = (int) Math.ceil(
                        result.shortfallRooms / (double) (periodsPerDay * 5));
                result.recommendations.add(String.format(
                        "Room Solution: Add %d more rooms", additionalRoomsNeeded));
                log.info("   Room Solution: Add {} more rooms", additionalRoomsNeeded);
            }
        } else {
            result.recommendations.add("Resources are sufficient for schedule generation");
            log.info("   ✅ Resources are sufficient for schedule generation");
        }
    }

    /**
     * Quick check before solver runs
     * Throws exception if capacity is insufficient
     */
    public void validateCapacityOrThrow(int periodsPerDay, int daysPerWeek) {
        CapacityAnalysisResult result = analyzeCapacity(periodsPerDay, daysPerWeek);

        if (!result.sufficient) {
            StringBuilder error = new StringBuilder();
            error.append("❌ INSUFFICIENT RESOURCES FOR SCHEDULE GENERATION\n\n");

            for (String warning : result.warnings) {
                error.append("⚠️  ").append(warning).append("\n");
            }

            error.append("\n💡 Recommendations:\n");
            for (String recommendation : result.recommendations) {
                error.append("   • ").append(recommendation).append("\n");
            }

            throw new IllegalStateException(error.toString());
        }
    }
}