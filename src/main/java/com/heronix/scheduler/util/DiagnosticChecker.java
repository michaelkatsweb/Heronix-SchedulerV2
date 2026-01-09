package com.heronix.scheduler.util;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Diagnostic Checker - Database State Analysis
 * Location: src/main/java/com/eduscheduler/util/DiagnosticChecker.java
 * 
 * Purpose: Verify data exists and identify issues before generation
 * FIXED: Added null safety checks to prevent NPE during diagnostics
 */
@Slf4j
@Component
public class DiagnosticChecker {

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @PostConstruct
    public void runDiagnostics() {
        log.info("================================================================");
        log.info("📊 DATABASE DIAGNOSTICS");
        log.info("================================================================");

        // Check Teachers
        List<Teacher> teachers = sisDataService.getAllTeachers().stream()
            .filter(t -> Boolean.TRUE.equals(t.getActive()))
            .collect(java.util.stream.Collectors.toList());
        log.info("👨‍🏫 TEACHERS: {}", teachers.size());
        if (!teachers.isEmpty()) {
            // Null-safe teacher access
            Teacher firstTeacher = teachers.stream()
                    .filter(t -> t != null && t.getName() != null)
                    .findFirst()
                    .orElse(null);
            
            if (firstTeacher != null) {
                log.info("   Sample: {}", firstTeacher.getName());
                log.info("   Departments: {}", teachers.stream()
                        .filter(t -> t != null && t.getDepartment() != null)
                        .map(Teacher::getDepartment)
                        .distinct()
                        .count());
            }
        }

        // Check Courses
        List<Course> courses = sisDataService.getAllCourses();
        log.info("📚 COURSES: {}", courses.size());
        if (!courses.isEmpty()) {
            // Null-safe course access
            Course firstCourse = courses.stream()
                    .filter(c -> c != null && c.getCourseName() != null)
                    .findFirst()
                    .orElse(null);
            
            if (firstCourse != null) {
                log.info("   Sample: {}", firstCourse.getCourseName());
                log.info("   Subjects: {}", courses.stream()
                        .filter(c -> c != null && c.getSubject() != null)
                        .map(Course::getSubject)
                        .distinct()
                        .count());
            }
        }

        // Check Rooms
        List<Room> rooms = roomRepository.findAll();
        log.info("🏫 ROOMS: {}", rooms.size());
        if (!rooms.isEmpty()) {
            // Null-safe room access
            Room firstRoom = rooms.stream()
                    .filter(r -> r != null && r.getRoomNumber() != null)
                    .findFirst()
                    .orElse(null);
            
            if (firstRoom != null) {
                log.info("   Sample: {}", firstRoom.getRoomNumber());
                log.info("   Buildings: {}", rooms.stream()
                        .filter(r -> r != null && r.getBuilding() != null)
                        .map(Room::getBuilding)
                        .distinct()
                        .count());
            }
        }

        // Check Students - FIXED: Added comprehensive null safety
        List<Student> students = sisDataService.getAllStudents();
        log.info("👨‍🎓 STUDENTS: {}", students.size());
        if (!students.isEmpty()) {
            // Null-safe student access
            Student firstStudent = students.stream()
                    .filter(s -> s != null && s.getFirstName() != null && s.getLastName() != null)
                    .findFirst()
                    .orElse(null);
            
            if (firstStudent != null) {
                log.info("   Sample: {} {}", firstStudent.getFirstName(), firstStudent.getLastName());
                
                // Count grade levels safely
                long gradeLevels = students.stream()
                        .filter(s -> s != null && s.getGradeLevel() != null)
                        .map(Student::getGradeLevel)
                        .distinct()
                        .count();
                log.info("   Grade Levels: {}", gradeLevels);
            } else {
                log.warn("   ⚠️ Students exist but contain null or incomplete data");
            }
        }

        // Check Existing Schedules
        List<Schedule> schedules = scheduleRepository.findAll();
        log.info("📅 SCHEDULES: {}", schedules.size());
        for (Schedule schedule : schedules) {
            if (schedule != null) {
                log.info("   - {} (Status: {})", 
                    schedule.getName() != null ? schedule.getName() : "Unnamed", 
                    schedule.getStatus() != null ? schedule.getStatus() : "Unknown");

                // Check slots with comprehensive error handling
                try {
                    List<ScheduleSlot> slots = scheduleSlotRepository.findByScheduleId(schedule.getId());
                    log.info("      Slots: {}", slots != null ? slots.size() : 0);

                    if (slots != null && !slots.isEmpty()) {
                        try {
                            long uniqueTeachers = slots.stream()
                                    .filter(s -> s != null && s.getTeacher() != null && s.getTeacher().getName() != null)
                                    .map(s -> s.getTeacher().getName())
                                    .distinct()
                                    .count();

                            log.info("      Unique Teachers Assigned: {}", uniqueTeachers);
                        } catch (Exception e) {
                            log.warn("      Could not count unique teachers: {}", e.getMessage());
                        }

                        // Sample assignments - with null safety
                        try {
                            ScheduleSlot sample = slots.stream()
                                    .filter(s -> s != null)
                                    .findFirst()
                                    .orElse(null);
                            
                            if (sample != null) {
                                log.info("      Sample Slot:");
                                try {
                                    log.info("         Course: {}",
                                            sample.getCourse() != null && sample.getCourse().getCourseName() != null 
                                                ? sample.getCourse().getCourseName() : "NULL");
                                } catch (Exception e) {
                                    log.info("         Course: ERROR - {}", e.getMessage());
                                }
                                
                                try {
                                    log.info("         Teacher: {}", 
                                            sample.getTeacher() != null && sample.getTeacher().getName() != null 
                                                ? sample.getTeacher().getName() : "NULL");
                                } catch (Exception e) {
                                    log.info("         Teacher: ERROR - {}", e.getMessage());
                                }
                                
                                try {
                                    log.info("         Room: {}", 
                                            sample.getRoom() != null && sample.getRoom().getRoomNumber() != null 
                                                ? sample.getRoom().getRoomNumber() : "NULL");
                                } catch (Exception e) {
                                    log.info("         Room: ERROR - {}", e.getMessage());
                                }
                                
                                try {
                                    log.info("         Time: {} {} - {}",
                                            sample.getDayOfWeek() != null ? sample.getDayOfWeek() : "NULL",
                                            sample.getStartTime() != null ? sample.getStartTime() : "NULL",
                                            sample.getEndTime() != null ? sample.getEndTime() : "NULL");
                                } catch (Exception e) {
                                    log.info("         Time: ERROR - {}", e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            log.warn("      Could not retrieve sample slot: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.error("      Error processing schedule slots: {}", e.getMessage(), e);
                }
            }
        }

        log.info("================================================================");
        log.info("✅ DIAGNOSTIC SUMMARY");
        log.info("================================================================");

        // Analysis
        if (teachers.isEmpty()) {
            log.warn("⚠️ NO TEACHERS FOUND - Import teachers first!");
        }
        if (courses.isEmpty()) {
            log.warn("⚠️ NO COURSES FOUND - Import courses first!");
        }
        if (rooms.isEmpty()) {
            log.warn("⚠️ NO ROOMS FOUND - Import rooms first!");
        }
        if (students.isEmpty()) {
            log.warn("⚠️ NO STUDENTS FOUND - Import students first!");
        }

        if (!teachers.isEmpty() && !courses.isEmpty() && !rooms.isEmpty()) {
            if (schedules.isEmpty()) {
                log.info("✅ Ready for FIRST schedule generation");
            } else {
                log.info("✅ Data loaded, existing schedules found");
                log.info("   Run new generation to test OptaPlanner AI");
            }
        }

        log.info("================================================================\n");
    }
}