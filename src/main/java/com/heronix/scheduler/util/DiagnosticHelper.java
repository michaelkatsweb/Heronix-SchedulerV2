package com.heronix.scheduler.util;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.repository.RoomRepository;
import com.heronix.scheduler.repository.ScheduleSlotRepository;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Diagnostic Helper - Helps diagnose scheduling issues
 * Location: src/main/java/com/eduscheduler/util/DiagnosticHelper.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-04
 */
@Slf4j
@Component
public class DiagnosticHelper {

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    /**
     * Print comprehensive diagnostics for schedule generation
     */
    public void printSchedulingDiagnostics() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║   SCHEDULING DIAGNOSTICS                                       ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");

        // Teacher diagnostics
        List<Teacher> allTeachers = sisDataService.getAllTeachers();
        log.info("\n📊 TEACHERS:");
        log.info("   Total teachers: {}", allTeachers.size());

        if (allTeachers.size() <= 20) {
            log.info("   Teacher list:");
            for (Teacher t : allTeachers) {
                log.info("      - {} (ID: {})", t.getName(), t.getId());
            }
        } else {
            log.info("   First 10 teachers:");
            allTeachers.stream().limit(10).forEach(t ->
                log.info("      - {} (ID: {})", t.getName(), t.getId()));
            log.info("   ... and {} more", allTeachers.size() - 10);
        }

        // Room diagnostics
        List<Room> allRooms = roomRepository.findAll();
        log.info("\n🏫 ROOMS:");
        log.info("   Total rooms: {}", allRooms.size());

        if (allRooms.size() <= 20) {
            log.info("   Room list:");
            for (Room r : allRooms) {
                log.info("      - {} (Capacity: {})", r.getRoomNumber(), r.getCapacity());
            }
        } else {
            log.info("   First 10 rooms:");
            allRooms.stream().limit(10).forEach(r ->
                log.info("      - {} (Capacity: {})", r.getRoomNumber(), r.getCapacity()));
            log.info("   ... and {} more", allRooms.size() - 10);
        }

        // Course diagnostics
        List<Course> activeCourses = sisDataService.getAllCourses().stream().filter(Course::getActive).toList();
        log.info("\n📚 COURSES:");
        log.info("   Total active courses: {}", activeCourses.size());

        long coursesWithTeacher = activeCourses.stream()
            .filter(c -> c.getTeacher() != null)
            .count();
        long coursesWithRoom = activeCourses.stream()
            .filter(c -> c.getRoom() != null)
            .count();

        log.info("   Courses with pre-assigned teacher: {}", coursesWithTeacher);
        log.info("   Courses with pre-assigned room: {}", coursesWithRoom);

        if (coursesWithTeacher > 0) {
            log.warn("\n⚠️ WARNING: {} courses have pre-assigned teachers!", coursesWithTeacher);
            log.warn("   OptaPlanner may respect these assignments.");
            log.warn("   Pre-assigned teachers:");
            Map<String, Long> teacherCounts = activeCourses.stream()
                .filter(c -> c.getTeacher() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getTeacher().getName(),
                    Collectors.counting()
                ));
            teacherCounts.forEach((name, count) ->
                log.warn("      - {}: {} courses", name, count));
        }

        if (coursesWithRoom > 0) {
            log.warn("\n⚠️ WARNING: {} courses have pre-assigned rooms!", coursesWithRoom);
            log.warn("   OptaPlanner may respect these assignments.");
            log.warn("   Pre-assigned rooms:");
            Map<String, Long> roomCounts = activeCourses.stream()
                .filter(c -> c.getRoom() != null)
                .collect(Collectors.groupingBy(
                    c -> c.getRoom().getRoomNumber(),
                    Collectors.counting()
                ));
            roomCounts.forEach((room, count) ->
                log.warn("      - {}: {} courses", room, count));
        }

        log.info("\n════════════════════════════════════════════════════════════════\n");
    }

    /**
     * Analyze a generated schedule for issues
     */
    public void analyzeSchedule(Long scheduleId) {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║   SCHEDULE ANALYSIS (ID: {})                              ║", scheduleId);
        log.info("╚════════════════════════════════════════════════════════════════╝");

        List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(scheduleId);

        log.info("\n📊 SCHEDULE STATISTICS:");
        log.info("   Total slots: {}", slots.size());

        // Teacher distribution
        Map<String, Long> teacherDistribution = slots.stream()
            .filter(s -> s.getTeacher() != null)
            .collect(Collectors.groupingBy(
                s -> s.getTeacher().getName(),
                Collectors.counting()
            ));

        log.info("\n👨‍🏫 TEACHER DISTRIBUTION:");
        if (teacherDistribution.isEmpty()) {
            log.warn("   ⚠️ No teachers assigned to any slots!");
        } else {
            teacherDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry ->
                    log.info("      - {}: {} slots", entry.getKey(), entry.getValue()));
        }

        // Room distribution
        Map<String, Long> roomDistribution = slots.stream()
            .filter(s -> s.getRoom() != null)
            .collect(Collectors.groupingBy(
                s -> s.getRoom().getRoomNumber(),
                Collectors.counting()
            ));

        log.info("\n🏫 ROOM DISTRIBUTION:");
        if (roomDistribution.isEmpty()) {
            log.warn("   ⚠️ No rooms assigned to any slots!");
        } else {
            roomDistribution.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry ->
                    log.info("      - Room {}: {} slots", entry.getKey(), entry.getValue()));
        }

        // Unassigned slots
        long unassignedTeacher = slots.stream().filter(s -> s.getTeacher() == null).count();
        long unassignedRoom = slots.stream().filter(s -> s.getRoom() == null).count();
        long unassignedTime = slots.stream().filter(s -> s.getTimeSlot() == null).count();

        log.info("\n⚠️ UNASSIGNED SLOTS:");
        log.info("   Missing teacher: {}", unassignedTeacher);
        log.info("   Missing room: {}", unassignedRoom);
        log.info("   Missing time: {}", unassignedTime);

        log.info("\n════════════════════════════════════════════════════════════════\n");
    }
}
