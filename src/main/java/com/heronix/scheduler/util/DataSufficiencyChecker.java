package com.heronix.scheduler.util;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Sufficiency Checker
 * Analyzes database to determine if sufficient data exists for schedule generation
 *
 * Location: src/main/java/com/eduscheduler/util/DataSufficiencyChecker.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-10
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSufficiencyChecker {

    private final SISDataService sisDataService;
    
    private final RoomRepository roomRepository;
    

    /**
     * Comprehensive data sufficiency check
     *
     * @return DataSufficiencyReport with detailed findings
     */
    public DataSufficiencyReport checkDataSufficiency() {
        log.info("═══════════════════════════════════════════════════════");
        log.info("  DATA SUFFICIENCY CHECK - SCHEDULE GENERATION");
        log.info("═══════════════════════════════════════════════════════");

        DataSufficiencyReport report = new DataSufficiencyReport();

        // Load all data
        List<Teacher> teachers = sisDataService.getAllTeachers();
        List<Course> courses = sisDataService.getAllCourses();
        List<Room> rooms = roomRepository.findAll();
        List<Student> students = sisDataService.getAllStudents();

        // Check basic counts
        report.teacherCount = teachers.size();
        report.courseCount = courses.size();
        report.roomCount = rooms.size();
        report.studentCount = students.size();

        log.info("\n📊 DATABASE COUNTS:");
        log.info("  Teachers: {}", report.teacherCount);
        log.info("  Courses:  {}", report.courseCount);
        log.info("  Rooms:    {}", report.roomCount);
        log.info("  Students: {}", report.studentCount);

        // Validate minimum requirements
        checkMinimumRequirements(report);

        // Check teacher qualifications
        checkTeacherData(teachers, report);

        // Check course data
        checkCourseData(courses, report);

        // Check room data
        checkRoomData(rooms, report);

        // Check student enrollments
        checkStudentData(students, report);

        // Check relationships
        checkRelationships(teachers, courses, rooms, students, report);

        // Calculate overall sufficiency
        report.calculateSufficiency();

        // Print report
        printReport(report);

        return report;
    }

    private void checkMinimumRequirements(DataSufficiencyReport report) {
        log.info("\n✅ MINIMUM REQUIREMENTS CHECK:");

        if (report.teacherCount == 0) {
            report.addCriticalIssue("No teachers in database - schedule generation impossible");
            log.error("  ❌ NO TEACHERS FOUND");
        } else if (report.teacherCount < 5) {
            report.addWarning("Very few teachers (" + report.teacherCount + ") - may cause conflicts");
            log.warn("  ⚠️  Only {} teachers (recommend 10+)", report.teacherCount);
        } else {
            log.info("  ✓ Teachers: {} (sufficient)", report.teacherCount);
        }

        if (report.courseCount == 0) {
            report.addCriticalIssue("No courses in database - nothing to schedule");
            log.error("  ❌ NO COURSES FOUND");
        } else if (report.courseCount < 10) {
            report.addWarning("Very few courses (" + report.courseCount + ") - limited schedule");
            log.warn("  ⚠️  Only {} courses (recommend 20+)", report.courseCount);
        } else {
            log.info("  ✓ Courses: {} (sufficient)", report.courseCount);
        }

        if (report.roomCount == 0) {
            report.addCriticalIssue("No rooms in database - nowhere to hold classes");
            log.error("  ❌ NO ROOMS FOUND");
        } else if (report.roomCount < 5) {
            report.addWarning("Very few rooms (" + report.roomCount + ") - may cause conflicts");
            log.warn("  ⚠️  Only {} rooms (recommend 10+)", report.roomCount);
        } else {
            log.info("  ✓ Rooms: {} (sufficient)", report.roomCount);
        }

        if (report.studentCount == 0) {
            report.addWarning("No students in database - classes will be empty");
            log.warn("  ⚠️  No students (optional but recommended)");
        } else {
            log.info("  ✓ Students: {} (good)", report.studentCount);
        }
    }

    private void checkTeacherData(List<Teacher> teachers, DataSufficiencyReport report) {
        log.info("\n👨‍🏫 TEACHER DATA QUALITY:");

        int activeTeachers = 0;
        int teachersWithDepartment = 0;
        int teachersWithEmail = 0;
        int teachersWithMaxHours = 0;

        for (Teacher teacher : teachers) {
            if (Boolean.TRUE.equals(teacher.getActive())) activeTeachers++;
            if (teacher.getDepartment() != null && !teacher.getDepartment().isEmpty()) teachersWithDepartment++;
            if (teacher.getEmail() != null && !teacher.getEmail().isEmpty()) teachersWithEmail++;
            if (teacher.getMaxHoursPerWeek() != null && teacher.getMaxHoursPerWeek() > 0) teachersWithMaxHours++;
        }

        report.activeTeachers = activeTeachers;
        log.info("  Active teachers: {} / {}", activeTeachers, teachers.size());
        log.info("  With department: {} / {}", teachersWithDepartment, teachers.size());
        log.info("  With email: {} / {}", teachersWithEmail, teachers.size());
        log.info("  With max hours: {} / {}", teachersWithMaxHours, teachers.size());

        if (activeTeachers == 0) {
            report.addCriticalIssue("No active teachers - all teachers are inactive");
        }
        if (teachersWithDepartment < teachers.size() / 2) {
            report.addWarning("Many teachers missing department assignment");
        }
    }

    private void checkCourseData(List<Course> courses, DataSufficiencyReport report) {
        log.info("\n📚 COURSE DATA QUALITY:");

        int activeCourses = 0;
        int coursesWithDuration = 0;
        int coursesWithSubject = 0;
        int coursesWithMaxStudents = 0;

        for (Course course : courses) {
            if (Boolean.TRUE.equals(course.getActive())) activeCourses++;
            if (course.getDurationMinutes() != null && course.getDurationMinutes() > 0) coursesWithDuration++;
            if (course.getSubject() != null && !course.getSubject().isEmpty()) coursesWithSubject++;
            if (course.getMaxStudents() != null && course.getMaxStudents() > 0) coursesWithMaxStudents++;
        }

        report.activeCourses = activeCourses;
        log.info("  Active courses: {} / {}", activeCourses, courses.size());
        log.info("  With duration: {} / {}", coursesWithDuration, courses.size());
        log.info("  With subject: {} / {}", coursesWithSubject, courses.size());
        log.info("  With max students: {} / {}", coursesWithMaxStudents, courses.size());

        if (activeCourses == 0) {
            report.addCriticalIssue("No active courses - nothing to schedule");
        }
        if (coursesWithDuration < activeCourses) {
            report.addWarning((activeCourses - coursesWithDuration) + " active courses missing duration");
        }
    }

    private void checkRoomData(List<Room> rooms, DataSufficiencyReport report) {
        log.info("\n🏫 ROOM DATA QUALITY:");

        int activeRooms = 0;
        int roomsWithCapacity = 0;
        int roomsWithType = 0;
        int roomsWithBuilding = 0;

        for (Room room : rooms) {
            if (Boolean.TRUE.equals(room.getActive())) activeRooms++;
            if (room.getCapacity() != null && room.getCapacity() > 0) roomsWithCapacity++;
            if (room.getRoomType() != null) roomsWithType++;
            if (room.getBuilding() != null && !room.getBuilding().isEmpty()) roomsWithBuilding++;
        }

        report.activeRooms = activeRooms;
        log.info("  Active rooms: {} / {}", activeRooms, rooms.size());
        log.info("  With capacity: {} / {}", roomsWithCapacity, rooms.size());
        log.info("  With type: {} / {}", roomsWithType, rooms.size());
        log.info("  With building: {} / {}", roomsWithBuilding, rooms.size());

        if (activeRooms == 0) {
            report.addCriticalIssue("No active rooms - nowhere to hold classes");
        }
        if (roomsWithCapacity < activeRooms) {
            report.addWarning((activeRooms - roomsWithCapacity) + " active rooms missing capacity");
        }
    }

    private void checkStudentData(List<Student> students, DataSufficiencyReport report) {
        log.info("\n🎓 STUDENT DATA QUALITY:");

        if (students.isEmpty()) {
            log.warn("  ⚠️  No students in database");
            report.addWarning("No students - classes will have zero enrollment");
            return;
        }

        int activeStudents = 0;
        int studentsWithEnrollments = 0;
        int totalEnrollments = 0;

        for (Student student : students) {
            if (Boolean.TRUE.equals(student.getActive())) activeStudents++;
            if (student.getEnrolledCourses() != null && !student.getEnrolledCourses().isEmpty()) {
                studentsWithEnrollments++;
                totalEnrollments += student.getEnrolledCourses().size();
            }
        }

        report.activeStudents = activeStudents;
        report.studentsWithEnrollments = studentsWithEnrollments;
        report.totalEnrollments = totalEnrollments;

        log.info("  Active students: {} / {}", activeStudents, students.size());
        log.info("  With enrollments: {} / {}", studentsWithEnrollments, students.size());
        log.info("  Total enrollments: {}", totalEnrollments);

        if (studentsWithEnrollments == 0) {
            report.addCriticalIssue("No student-course enrollments - cannot assign students to classes");
        } else if (studentsWithEnrollments < activeStudents / 2) {
            report.addWarning("Less than half of students have course enrollments");
        }

        if (totalEnrollments > 0 && activeStudents > 0) {
            double avgEnrollments = (double) totalEnrollments / activeStudents;
            log.info("  Avg enrollments per student: {}", String.format("%.1f", avgEnrollments));

            if (avgEnrollments < 4) {
                report.addWarning("Students have very few course enrollments (avg " + String.format("%.1f", avgEnrollments) + ")");
            }
        }
    }

    private void checkRelationships(List<Teacher> teachers, List<Course> courses,
                                    List<Room> rooms, List<Student> students,
                                    DataSufficiencyReport report) {
        log.info("\n🔗 RELATIONSHIP CHECKS:");

        // Check if courses have qualified teachers
        int coursesWithQualifiedTeachers = 0;
        for (Course course : courses) {
            if (!Boolean.TRUE.equals(course.getActive())) continue;

            String subject = course.getSubject();
            if (subject != null) {
                long qualifiedTeachers = teachers.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getActive()))
                    .filter(t -> subject.equalsIgnoreCase(t.getDepartment()))
                    .count();

                if (qualifiedTeachers > 0) {
                    coursesWithQualifiedTeachers++;
                }
            }
        }

        log.info("  Courses with qualified teachers: {} / {}", coursesWithQualifiedTeachers, report.activeCourses);

        if (coursesWithQualifiedTeachers < report.activeCourses / 2) {
            report.addWarning("Many courses lack teachers in matching department");
        }

        // Check room-course compatibility
        int labCourses = (int) courses.stream()
            .filter(c -> Boolean.TRUE.equals(c.getActive()))
            .filter(c -> c.isRequiresLab())
            .count();

        int labRooms = (int) rooms.stream()
            .filter(r -> Boolean.TRUE.equals(r.getActive()))
            .filter(r -> r.getRoomType() != null && r.getRoomType().toString().contains("LAB"))
            .count();

        log.info("  Lab courses: {}", labCourses);
        log.info("  Lab rooms: {}", labRooms);

        if (labCourses > 0 && labRooms == 0) {
            report.addWarning(labCourses + " courses require labs but no lab rooms available");
        }
    }

    private void printReport(DataSufficiencyReport report) {
        log.info("\n═══════════════════════════════════════════════════════");
        log.info("  SUFFICIENCY REPORT");
        log.info("═══════════════════════════════════════════════════════");

        if (report.isSufficient) {
            log.info("✅ STATUS: SUFFICIENT FOR SCHEDULE GENERATION");
        } else {
            log.error("❌ STATUS: INSUFFICIENT DATA");
        }

        log.info("\n📊 SUMMARY:");
        log.info("  Overall Score: {}%", report.sufficiencyScore);
        log.info("  Critical Issues: {}", report.criticalIssues.size());
        log.info("  Warnings: {}", report.warnings.size());

        if (!report.criticalIssues.isEmpty()) {
            log.error("\n🚨 CRITICAL ISSUES:");
            for (String issue : report.criticalIssues) {
                log.error("  • {}", issue);
            }
        }

        if (!report.warnings.isEmpty()) {
            log.warn("\n⚠️  WARNINGS:");
            for (String warning : report.warnings) {
                log.warn("  • {}", warning);
            }
        }

        log.info("\n💡 RECOMMENDATIONS:");
        if (report.teacherCount < 10) {
            log.info("  • Import more teachers (current: {}, recommend: 10+)", report.teacherCount);
        }
        if (report.courseCount < 20) {
            log.info("  • Import more courses (current: {}, recommend: 20+)", report.courseCount);
        }
        if (report.roomCount < 10) {
            log.info("  • Import more rooms (current: {}, recommend: 10+)", report.roomCount);
        }
        if (report.totalEnrollments == 0) {
            log.info("  • Create student-course enrollments");
        }
        if (report.studentCount == 0) {
            log.info("  • Import student data");
        }

        log.info("═══════════════════════════════════════════════════════\n");
    }

    /**
     * Data Sufficiency Report
     */
    public static class DataSufficiencyReport {
        public int teacherCount = 0;
        public int courseCount = 0;
        public int roomCount = 0;
        public int studentCount = 0;

        public int activeTeachers = 0;
        public int activeCourses = 0;
        public int activeRooms = 0;
        public int activeStudents = 0;

        public int studentsWithEnrollments = 0;
        public int totalEnrollments = 0;

        public List<String> criticalIssues = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();

        public boolean isSufficient = false;
        public int sufficiencyScore = 0;

        public void addCriticalIssue(String issue) {
            criticalIssues.add(issue);
        }

        public void addWarning(String warning) {
            warnings.add(warning);
        }

        public void calculateSufficiency() {
            // Calculate score out of 100
            int score = 0;

            // Teachers (25 points)
            if (activeTeachers >= 10) score += 25;
            else if (activeTeachers >= 5) score += 15;
            else if (activeTeachers >= 1) score += 5;

            // Courses (25 points)
            if (activeCourses >= 20) score += 25;
            else if (activeCourses >= 10) score += 15;
            else if (activeCourses >= 1) score += 5;

            // Rooms (25 points)
            if (activeRooms >= 10) score += 25;
            else if (activeRooms >= 5) score += 15;
            else if (activeRooms >= 1) score += 5;

            // Enrollments (25 points)
            if (totalEnrollments >= 100) score += 25;
            else if (totalEnrollments >= 50) score += 15;
            else if (totalEnrollments >= 10) score += 5;

            sufficiencyScore = score;

            // Minimum requirements: at least 1 active teacher, course, and room
            isSufficient = (activeTeachers > 0 && activeCourses > 0 && activeRooms > 0 && criticalIssues.isEmpty());
        }

        public String getSummary() {
            return String.format("Teachers: %d/%d active | Courses: %d/%d active | Rooms: %d/%d active | Enrollments: %d | Score: %d%% | Sufficient: %s",
                activeTeachers, teacherCount, activeCourses, courseCount, activeRooms, roomCount,
                totalEnrollments, sufficiencyScore, isSufficient ? "YES" : "NO");
        }
    }
}
