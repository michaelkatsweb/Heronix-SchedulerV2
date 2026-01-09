package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.dto.ScheduleDiagnosticReport;
import com.heronix.scheduler.model.dto.ScheduleDiagnosticReport.*;
import com.heronix.scheduler.model.enums.EnrollmentStatus;
import com.heronix.scheduler.model.enums.RoomType;
import com.heronix.scheduler.service.data.SISDataService;
import com.heronix.scheduler.repository.RoomRepository;
import com.heronix.scheduler.repository.StudentEnrollmentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Schedule Diagnostic Service - ADMINISTRATOR-FRIENDLY DIAGNOSTICS
 * Location: src/main/java/com/eduscheduler/service/ScheduleDiagnosticService.java
 *
 * Generates clear, actionable reports about schedule generation readiness
 * Explains issues in plain language that administrators can understand
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since November 18, 2025
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class ScheduleDiagnosticService {

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StudentEnrollmentRepository enrollmentRepository;

    /**
     * Generate comprehensive diagnostic report
     * This is the main method administrators will use
     *
     * @return User-friendly diagnostic report
     */
    public ScheduleDiagnosticReport generateDiagnosticReport() {
        log.info("Generating schedule diagnostic report...");

        ScheduleDiagnosticReport report = ScheduleDiagnosticReport.builder()
                .diagnosticTimestamp(LocalDateTime.now())
                .criticalIssuesCount(0)
                .warningsCount(0)
                .issues(new ArrayList<>())
                .recommendedActions(new ArrayList<>())
                .build();

        // Collect resource summary
        ResourceSummary summary = collectResourceSummary();
        report.setResourceSummary(summary);

        // Run all diagnostic checks
        checkTeacherAssignments(report, summary);
        checkRoomTypes(report, summary);
        checkRoomCapacity(report, summary);
        checkStudentEnrollments(report, summary);
        checkDataQuality(report, summary);
        checkConstraintRequirements(report, summary);

        // Determine overall status
        determineOverallStatus(report);

        // Generate summary message
        generateSummaryMessage(report);

        // Estimate fix time
        estimateFixTime(report);

        log.info("Diagnostic complete: {} critical issues, {} warnings",
                report.getCriticalIssuesCount(), report.getWarningsCount());

        return report;
    }

    /**
     * Collect summary of all resources
     */
    private ResourceSummary collectResourceSummary() {
        List<Teacher> teachers = sisDataService.getAllTeachers();
        List<Course> courses = sisDataService.getAllCourses();
        List<Room> rooms = roomRepository.findByActiveTrue();
        List<Student> students = sisDataService.getAllStudents();

        // ✅ NULL SAFE: Filter null courses before accessing teacher
        long coursesWithTeachers = courses.stream()
                .filter(c -> c != null && c.getTeacher() != null)
                .count();

        long coursesWithoutTeachers = courses.stream()
                .filter(c -> c != null && c.getTeacher() == null)
                .count();

        // Count room types
        // ✅ NULL SAFE: Filter null rooms before accessing type
        long standardRooms = rooms.stream().filter(r -> r != null && r.getType() == RoomType.CLASSROOM).count();
        long labRooms = rooms.stream().filter(r -> r != null && (r.getType() == RoomType.LAB || r.getType() == RoomType.SCIENCE_LAB)).count();
        long gymRooms = rooms.stream().filter(r -> r != null && r.getType() == RoomType.GYMNASIUM).count();
        long auditoriumRooms = rooms.stream().filter(r -> r != null && r.getType() == RoomType.AUDITORIUM).count();
        long musicRooms = rooms.stream().filter(r -> r != null && r.getType() == RoomType.MUSIC_ROOM).count();
        long computerLabRooms = rooms.stream().filter(r -> r != null && r.getType() == RoomType.COMPUTER_LAB).count();

        // Count course types
        // ✅ NULL SAFE: Filter null courses and check fields exist
        long mathCourses = courses.stream()
                .filter(c -> c != null && c.getSubject() != null && c.getSubject().toLowerCase().contains("math"))
                .count();
        long scienceCourses = courses.stream()
                .filter(c -> c != null && c.getSubject() != null &&
                        (c.getSubject().toLowerCase().contains("science") ||
                         c.getSubject().toLowerCase().contains("biology") ||
                         c.getSubject().toLowerCase().contains("chemistry") ||
                         c.getSubject().toLowerCase().contains("physics")))
                .count();
        long peCourses = courses.stream()
                .filter(c -> c != null &&
                             ((c.getSubject() != null && (c.getSubject().toLowerCase().contains("physical") ||
                                                          c.getSubject().toLowerCase().contains("pe"))) ||
                              (c.getCourseName() != null && (c.getCourseName().toLowerCase().contains("pe") ||
                                                             c.getCourseName().toLowerCase().contains("gym")))))
                .count();
        long musicCourses = courses.stream()
                .filter(c -> c != null &&
                             ((c.getSubject() != null && c.getSubject().toLowerCase().contains("music")) ||
                              (c.getCourseName() != null && c.getCourseName().toLowerCase().contains("music"))))
                .count();
        long labRequiredCourses = courses.stream()
                .filter(c -> c != null && Boolean.TRUE.equals(c.getRequiresLab()))
                .count();

        long totalEnrollments = enrollmentRepository.count();

        return ResourceSummary.builder()
                .activeTeachers(teachers.size())
                .activeCourses(courses.size())
                .coursesWithTeachers((int) coursesWithTeachers)
                .coursesWithoutTeachers((int) coursesWithoutTeachers)
                .availableRooms(rooms.size())
                .activeStudents(students.size())
                .totalEnrollments((int) totalEnrollments)
                .standardRooms((int) standardRooms)
                .labRooms((int) labRooms)
                .gymRooms((int) gymRooms)
                .auditoriumRooms((int) auditoriumRooms)
                .musicRooms((int) musicRooms)
                .computerLabRooms((int) computerLabRooms)
                .mathCourses((int) mathCourses)
                .scienceCourses((int) scienceCourses)
                .peCourses((int) peCourses)
                .musicCourses((int) musicCourses)
                .labRequiredCourses((int) labRequiredCourses)
                .build();
    }

    /**
     * Check if all courses have teachers assigned
     */
    private void checkTeacherAssignments(ScheduleDiagnosticReport report, ResourceSummary summary) {
        if (summary.getCoursesWithoutTeachers() > 0) {
            // ✅ NULL SAFE: Filter null courses before checking teacher
            List<Course> coursesWithoutTeachers = sisDataService.getAllCourses().stream()
                    .filter(c -> c != null && c.getAssignedTeacher() == null)
                    .limit(10)
                    .collect(Collectors.toList());

            // ✅ NULL SAFE: Filter null courses and provide defaults for missing fields
            List<String> affectedCourses = coursesWithoutTeachers.stream()
                    .filter(c -> c != null)
                    .map(c -> String.format("%s (%s)",
                            c.getCourseName() != null ? c.getCourseName() : "Unknown",
                            c.getCourseCode() != null ? c.getCourseCode() : "Unknown"))
                    .collect(Collectors.toList());

            DiagnosticIssue issue = DiagnosticIssue.builder()
                    .severity(IssueSeverity.CRITICAL)
                    .category(IssueCategory.COURSES)
                    .title("Courses Missing Teacher Assignments")
                    .description(String.format("%d course(s) do not have a teacher assigned", summary.getCoursesWithoutTeachers()))
                    .userFriendlyExplanation("Every course needs a teacher to be scheduled. The schedule generator " +
                            "cannot create schedules for courses without teachers.")
                    .howToFix("Go to each course and assign a teacher from the dropdown. You can find these courses " +
                            "in the Courses section by filtering for courses without teachers.")
                    .affectedCount(summary.getCoursesWithoutTeachers())
                    .affectedItems(affectedCourses)
                    .build();

            report.addIssue(issue);
            report.addRecommendedAction(String.format("Assign teachers to %d course(s) that are missing teacher assignments",
                    summary.getCoursesWithoutTeachers()));
        }
    }

    /**
     * Check if required room types exist
     */
    private void checkRoomTypes(ScheduleDiagnosticReport report, ResourceSummary summary) {
        // Check for PE courses needing GYM
        if (summary.getPeCourses() > 0 && summary.getGymRooms() == 0) {
            DiagnosticIssue issue = DiagnosticIssue.builder()
                    .severity(IssueSeverity.CRITICAL)
                    .category(IssueCategory.ROOMS)
                    .title("No Gymnasium Available for PE Courses")
                    .description(String.format("You have %d Physical Education course(s) but no gymnasium rooms", summary.getPeCourses()))
                    .userFriendlyExplanation("Physical Education classes MUST be scheduled in a gymnasium. " +
                            "Without at least one gym, PE courses cannot be scheduled and schedule generation will fail.")
                    .howToFix("Add at least one room and set its Room Type to 'GYM'. Go to Rooms → Add Room, " +
                            "then select 'GYM' as the room type. You can also edit an existing room (like your gymnasium) " +
                            "and change its type to 'GYM'.")
                    .affectedCount(summary.getPeCourses())
                    .affectedItems(List.of(String.format("%d PE course(s) requiring gymnasium", summary.getPeCourses())))
                    .build();

            report.addIssue(issue);
            report.addRecommendedAction("Add at least 1 GYM room for Physical Education courses");
        }

        // Check for Music courses needing AUDITORIUM/MUSIC_ROOM
        if (summary.getMusicCourses() > 0 && summary.getAuditoriumRooms() == 0 && summary.getMusicRooms() == 0) {
            DiagnosticIssue issue = DiagnosticIssue.builder()
                    .severity(IssueSeverity.CRITICAL)
                    .category(IssueCategory.ROOMS)
                    .title("No Auditorium or Music Room for Music Courses")
                    .description(String.format("You have %d Music course(s) but no auditorium or music room", summary.getMusicCourses()))
                    .userFriendlyExplanation("Music classes should be scheduled in an auditorium or dedicated music room. " +
                            "Without these room types, music courses cannot be properly scheduled.")
                    .howToFix("Add at least one room and set its Room Type to either 'AUDITORIUM' or 'MUSIC_ROOM'. " +
                            "Go to Rooms → Add Room, or edit an existing room that you use for music classes.")
                    .affectedCount(summary.getMusicCourses())
                    .affectedItems(List.of(String.format("%d Music course(s) requiring auditorium or music room", summary.getMusicCourses())))
                    .build();

            report.addIssue(issue);
            report.addRecommendedAction("Add at least 1 AUDITORIUM or MUSIC_ROOM for Music courses");
        }

        // Check for Lab courses needing LAB rooms
        if (summary.getLabRequiredCourses() > 0 && summary.getLabRooms() < 3) {
            IssueSeverity severity = summary.getLabRooms() == 0 ? IssueSeverity.CRITICAL : IssueSeverity.WARNING;
            String howToFix = summary.getLabRooms() == 0 ?
                    "Add at least 3-5 LAB rooms for science courses. Go to Rooms → Add Room and set Room Type to 'LAB'." :
                    String.format("You have %d LAB room(s) but %d courses requiring labs. Consider adding more LAB rooms for better scheduling.",
                            summary.getLabRooms(), summary.getLabRequiredCourses());

            DiagnosticIssue issue = DiagnosticIssue.builder()
                    .severity(severity)
                    .category(IssueCategory.ROOMS)
                    .title("Insufficient Laboratory Rooms")
                    .description(String.format("You have %d course(s) requiring labs but only %d lab room(s)",
                            summary.getLabRequiredCourses(), summary.getLabRooms()))
                    .userFriendlyExplanation("Science courses that require laboratory work must be scheduled in LAB rooms. " +
                            "With insufficient lab rooms, these courses may not be scheduled optimally or at all.")
                    .howToFix(howToFix)
                    .affectedCount(summary.getLabRequiredCourses())
                    .affectedItems(List.of(String.format("%d course(s) requiring laboratory facilities", summary.getLabRequiredCourses())))
                    .build();

            report.addIssue(issue);
            if (summary.getLabRooms() == 0) {
                report.addRecommendedAction("Add at least 3-5 LAB rooms for science courses");
            } else {
                report.addRecommendedAction(String.format("Consider adding more LAB rooms (currently have %d for %d lab courses)",
                        summary.getLabRooms(), summary.getLabRequiredCourses()));
            }
        }
    }

    /**
     * Check room capacity vs enrollments
     */
    private void checkRoomCapacity(ScheduleDiagnosticReport report, ResourceSummary summary) {
        List<Course> courses = sisDataService.getAllCourses();
        List<Room> rooms = roomRepository.findByActiveTrue();

        if (rooms.isEmpty()) {
            DiagnosticIssue issue = DiagnosticIssue.builder()
                    .severity(IssueSeverity.CRITICAL)
                    .category(IssueCategory.ROOMS)
                    .title("No Rooms Available")
                    .description("There are no available rooms in the system")
                    .userFriendlyExplanation("Schedule generation requires at least a few rooms to assign courses. " +
                            "Without rooms, no schedule can be created.")
                    .howToFix("Add rooms to your facility. Go to Rooms → Add Room and create entries for your classrooms, " +
                            "labs, gymnasium, auditorium, etc.")
                    .affectedCount(0)
                    .affectedItems(List.of("No rooms available"))
                    .build();

            report.addIssue(issue);
            report.addRecommendedAction("Add at least 10-20 rooms to your system");
            return;
        }

        // Check if largest room can accommodate largest class
        // ✅ NULL SAFE: Filter null rooms before accessing capacity
        int largestRoomCapacity = rooms.stream()
                .filter(r -> r != null)
                .mapToInt(r -> r.getCapacity() != null ? r.getCapacity() : 0)
                .max()
                .orElse(0);

        // ✅ NULL SAFE: Filter null courses and check ID exists
        for (Course course : courses) {
            if (course == null || course.getId() == null) continue;

            long enrollmentCount = enrollmentRepository.countByCourseId(course.getId());

            if (enrollmentCount > largestRoomCapacity) {
                DiagnosticIssue issue = DiagnosticIssue.builder()
                        .severity(IssueSeverity.CRITICAL)
                        .category(IssueCategory.ROOMS)
                        .title("Room Capacity Too Small for Some Courses")
                        .description(String.format("Course '%s' has %d students but largest room holds %d",
                                course.getCourseName(), enrollmentCount, largestRoomCapacity))
                        .userFriendlyExplanation("Some courses have more enrolled students than any room can hold. " +
                                "This makes scheduling impossible.")
                        .howToFix("Either: 1) Increase room capacities to accommodate larger classes, " +
                                "2) Split large courses into multiple sections, or " +
                                "3) Add larger rooms to your facility.")
                        .affectedCount(1)
                        .affectedItems(List.of(String.format("%s: %d students, max room: %d seats",
                                course.getCourseName(), enrollmentCount, largestRoomCapacity)))
                        .build();

                report.addIssue(issue);
                report.addRecommendedAction(String.format("Increase room capacity or split course '%s' into sections",
                        course.getCourseName()));
                break; // Only report first occurrence
            }
        }
    }

    /**
     * Check student enrollments
     */
    private void checkStudentEnrollments(ScheduleDiagnosticReport report, ResourceSummary summary) {
        if (summary.getActiveStudents() == 0) {
            DiagnosticIssue issue = DiagnosticIssue.builder()
                    .severity(IssueSeverity.WARNING)
                    .category(IssueCategory.STUDENTS)
                    .title("No Active Students")
                    .description("There are no active students in the system")
                    .userFriendlyExplanation("While you can generate a schedule without students, it won't include " +
                            "class sizes or student assignments. Consider adding students for a complete schedule.")
                    .howToFix("Add students via Students → Add Student or import students from a CSV file.")
                    .affectedCount(0)
                    .affectedItems(List.of("No students found"))
                    .build();

            report.addIssue(issue);
        }

        if (summary.getTotalEnrollments() == 0 && summary.getActiveStudents() > 0) {
            DiagnosticIssue issue = DiagnosticIssue.builder()
                    .severity(IssueSeverity.WARNING)
                    .category(IssueCategory.STUDENTS)
                    .title("Students Not Enrolled in Courses")
                    .description(String.format("You have %d student(s) but no course enrollments", summary.getActiveStudents()))
                    .userFriendlyExplanation("Students should be enrolled in courses for the schedule to assign them to classes.")
                    .howToFix("Enroll students in courses via Students → Edit Student → Enrollments, or use bulk enrollment.")
                    .affectedCount(summary.getActiveStudents())
                    .affectedItems(List.of(String.format("%d student(s) with no enrollments", summary.getActiveStudents())))
                    .build();

            report.addIssue(issue);
        }
    }

    /**
     * Check data quality issues
     */
    private void checkDataQuality(ScheduleDiagnosticReport report, ResourceSummary summary) {
        // Check for courses without subjects
        // ✅ NULL SAFE: Filter null courses before checking subject
        long coursesWithoutSubject = sisDataService.getAllCourses().stream()
                .filter(c -> c != null && (c.getSubject() == null || c.getSubject().trim().isEmpty()))
                .count();

        if (coursesWithoutSubject > 0) {
            DiagnosticIssue issue = DiagnosticIssue.builder()
                    .severity(IssueSeverity.WARNING)
                    .category(IssueCategory.DATA_QUALITY)
                    .title("Courses Missing Subject Information")
                    .description(String.format("%d course(s) do not have a subject assigned", coursesWithoutSubject))
                    .userFriendlyExplanation("Courses should have a subject (e.g., Mathematics, Science, English) " +
                            "for better organization and room assignment.")
                    .howToFix("Edit each course and select an appropriate subject from the dropdown.")
                    .affectedCount((int) coursesWithoutSubject)
                    .build();

            report.addIssue(issue);
        }

        // Check for rooms without capacity
        // ✅ NULL SAFE: Filter null rooms before checking capacity
        long roomsWithoutCapacity = roomRepository.findByActiveTrue().stream()
                .filter(r -> r != null && (r.getCapacity() == null || r.getCapacity() == 0))
                .count();

        if (roomsWithoutCapacity > 0) {
            DiagnosticIssue issue = DiagnosticIssue.builder()
                    .severity(IssueSeverity.CRITICAL)
                    .category(IssueCategory.DATA_QUALITY)
                    .title("Rooms Missing Capacity Information")
                    .description(String.format("%d room(s) do not have capacity set", roomsWithoutCapacity))
                    .userFriendlyExplanation("Every room must have a capacity (number of seats) for the scheduler " +
                            "to assign the right number of students.")
                    .howToFix("Edit each room and enter the number of seats/desks available.")
                    .affectedCount((int) roomsWithoutCapacity)
                    .build();

            report.addIssue(issue);
            report.addRecommendedAction(String.format("Set capacity for %d room(s)", roomsWithoutCapacity));
        }
    }

    /**
     * Check constraint-specific requirements
     */
    private void checkConstraintRequirements(ScheduleDiagnosticReport report, ResourceSummary summary) {
        // Overall resource check
        if (summary.getActiveCourses() == 0) {
            DiagnosticIssue issue = DiagnosticIssue.builder()
                    .severity(IssueSeverity.CRITICAL)
                    .category(IssueCategory.COURSES)
                    .title("No Active Courses")
                    .description("There are no active courses to schedule")
                    .userFriendlyExplanation("Schedule generation requires at least one active course.")
                    .howToFix("Add courses via Courses → Add Course or activate existing courses.")
                    .affectedCount(0)
                    .build();

            report.addIssue(issue);
            report.addRecommendedAction("Add at least 5-10 active courses");
        }

        if (summary.getActiveTeachers() == 0) {
            DiagnosticIssue issue = DiagnosticIssue.builder()
                    .severity(IssueSeverity.CRITICAL)
                    .category(IssueCategory.TEACHERS)
                    .title("No Active Teachers")
                    .description("There are no active teachers to assign to courses")
                    .userFriendlyExplanation("Schedule generation requires teachers to teach the courses.")
                    .howToFix("Add teachers via Teachers → Add Teacher or activate existing teachers.")
                    .affectedCount(0)
                    .build();

            report.addIssue(issue);
            report.addRecommendedAction("Add at least 5-10 active teachers");
        }
    }

    /**
     * Determine overall status based on issues
     */
    private void determineOverallStatus(ScheduleDiagnosticReport report) {
        if (report.getCriticalIssuesCount() > 0) {
            report.setOverallStatus(DiagnosticStatus.CRITICAL);
        } else if (report.getWarningsCount() > 0) {
            report.setOverallStatus(DiagnosticStatus.WARNING);
        } else {
            report.setOverallStatus(DiagnosticStatus.READY);
        }
    }

    /**
     * Generate user-friendly summary message
     */
    private void generateSummaryMessage(ScheduleDiagnosticReport report) {
        StringBuilder message = new StringBuilder();

        if (report.getOverallStatus() == DiagnosticStatus.READY) {
            message.append("✅ Your system is ready for schedule generation! ");
            message.append("All requirements are met and you can proceed.");
        } else if (report.getOverallStatus() == DiagnosticStatus.WARNING) {
            message.append("⚠️ Schedule generation can proceed, but there are ");
            message.append(report.getWarningsCount()).append(" optimization opportunity/opportunities. ");
            message.append("Consider addressing these for better schedules.");
        } else {
            message.append("❌ Schedule generation will fail. You have ");
            message.append(report.getCriticalIssuesCount()).append(" critical issue(s) that must be fixed:\n\n");

            int count = 1;
            for (DiagnosticIssue issue : report.getIssues()) {
                if (issue.getSeverity() == IssueSeverity.CRITICAL) {
                    message.append(count++).append(". ").append(issue.getTitle()).append("\n");
                }
            }

            message.append("\nPlease review the detailed report below for how to fix each issue.");
        }

        report.setSummaryMessage(message.toString());
    }

    /**
     * Estimate time to fix issues
     */
    private void estimateFixTime(ScheduleDiagnosticReport report) {
        int estimatedMinutes = 0;

        for (DiagnosticIssue issue : report.getIssues()) {
            if (issue.getSeverity() == IssueSeverity.CRITICAL) {
                switch (issue.getCategory()) {
                    case COURSES:
                        // Assigning teachers: ~1 min per course
                        estimatedMinutes += (issue.getAffectedCount() != null ? issue.getAffectedCount() : 10);
                        break;
                    case ROOMS:
                        // Adding/editing rooms: ~2 min per room
                        estimatedMinutes += 5; // Typically need 2-3 rooms
                        break;
                    case TEACHERS:
                        // Adding teachers: ~2 min per teacher
                        estimatedMinutes += 10;
                        break;
                    case DATA_QUALITY:
                        // Fixing data: ~1 min per item
                        estimatedMinutes += (issue.getAffectedCount() != null ? issue.getAffectedCount() : 5);
                        break;
                    default:
                        estimatedMinutes += 5;
                }
            }
        }

        report.setEstimatedFixTimeMinutes(Math.max(estimatedMinutes, 5));
    }
}
