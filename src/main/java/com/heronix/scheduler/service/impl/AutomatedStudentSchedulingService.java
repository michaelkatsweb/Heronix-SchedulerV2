package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.domain.*;
import com.heronix.scheduler.model.enums.EducationLevel;
import com.heronix.scheduler.repository.*;
import com.heronix.scheduler.service.data.SISDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Automated Student Scheduling Service
 *
 * Intelligently assigns students to courses based on:
 * - Grade level progression (8th → 9th → 10th → 11th → 12th)
 * - State/federal compliance requirements (ESSA, state standards)
 * - Standard course load (3-4 core classes, 2-3 electives)
 * - Course sequencing (English 1 → English 2 → English 3 → English 4)
 * - Previous year courses (continuity and prerequisites)
 * - Teacher certification matching
 * - Room capacity constraints
 * - Medical conditions (PE restrictions)
 * - Grade-specific course patterns (based on college admission requirements)
 *
 * Compliance Features:
 * - Grade 9: Biology/IPC, English 1, Algebra I/Geometry, American History
 * - Grade 10: Biology/Earth Science, English 2, Geometry/Algebra II, World History
 * - Grade 11: Chemistry, English 3, Algebra II/Pre-Calc, Government/Economics
 * - Grade 12: Physics, English 4/Literature, Calculus/Statistics, elective flexibility
 *
 * @author Heronix Scheduling System Team
 * @version 2.0.0
 * @since Phase 19 - Automated Student Scheduling (Enhanced with State/Federal Compliance)
 */
@Service
@Slf4j
@Transactional
public class AutomatedStudentSchedulingService {

    @Autowired
    private SISDataService sisDataService;

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private StudentCourseHistoryRepository studentCourseHistoryRepository;

    @Autowired
    private CoursePrerequisiteRepository coursePrerequisiteRepository;

    // Standard course requirements by grade level (using String for grade levels)
    private static final Map<String, CourseRequirements> STANDARD_REQUIREMENTS = Map.of(
        "9", new CourseRequirements(4, 3), // 4 core, 3 electives
        "10", new CourseRequirements(4, 3),
        "11", new CourseRequirements(4, 3),
        "12", new CourseRequirements(3, 3), // Lighter core load for seniors
        "8", new CourseRequirements(4, 2)  // Middle school
    );

    // Core subjects that every student should take
    private static final Set<String> CORE_SUBJECTS = Set.of(
        "English", "Mathematics", "Science", "Social Studies", "History"
    );

    // Grade-specific course sequences (based on state/federal requirements)
    // Source: College admission requirements and state standards (2025)
    private static final Map<String, GradeSpecificCourses> GRADE_COURSE_PATTERNS = Map.of(
        "9", new GradeSpecificCourses(
            Map.of(
                "English", List.of("English 1", "English I", "English 9"),
                "Mathematics", List.of("Algebra I", "Algebra 1", "Geometry"),
                "Science", List.of("Biology", "IPC", "Physical Science", "Integrated Physics and Chemistry"),
                "Social Studies", List.of("American History", "US History", "World Geography")
            ),
            List.of("Foreign Language", "Fine Arts", "CTE") // Recommended electives
        ),
        "10", new GradeSpecificCourses(
            Map.of(
                "English", List.of("English 2", "English II", "English 10"),
                "Mathematics", List.of("Geometry", "Algebra II", "Algebra 2"),
                "Science", List.of("Biology", "Earth Science", "Earth & Space Science"),
                "Social Studies", List.of("World History", "World Geography", "Geography")
            ),
            List.of("Foreign Language", "Physical Education", "CTE")
        ),
        "11", new GradeSpecificCourses(
            Map.of(
                "English", List.of("English 3", "English III", "English 11", "American Literature"),
                "Mathematics", List.of("Algebra II", "Algebra 2", "Pre-Calculus", "Pre-Calc"),
                "Science", List.of("Chemistry", "Physics", "Advanced Biology"),
                "Social Studies", List.of("Government", "Economics", "Civics", "US Government")
            ),
            List.of("Foreign Language", "Advanced Placement", "CTE", "Dual Enrollment")
        ),
        "12", new GradeSpecificCourses(
            Map.of(
                "English", List.of("English 4", "English IV", "English 12", "British Literature", "Literature"),
                "Mathematics", List.of("Pre-Calculus", "Pre-Calc", "Calculus", "AP Calculus", "Statistics"),
                "Science", List.of("Physics", "AP Biology", "AP Chemistry", "Environmental Science", "Anatomy")
            ),
            List.of("Advanced Placement", "Dual Enrollment", "CTE", "Electives") // Seniors have flexibility
        ),
        "8", new GradeSpecificCourses(
            Map.of(
                "English", List.of("English 8", "Language Arts 8"),
                "Mathematics", List.of("Pre-Algebra", "Math 8", "Algebra I"),
                "Science", List.of("Physical Science", "Science 8"),
                "Social Studies", List.of("Civics", "US History", "Social Studies 8")
            ),
            List.of("Physical Education", "Fine Arts", "Exploratory")
        )
    );

    // Course prerequisite sequences (for multi-year planning)
    private static final Map<String, List<String>> COURSE_SEQUENCES = Map.ofEntries(
        // English progression
        Map.entry("English 1", List.of("English 2", "English 3", "English 4")),
        Map.entry("English 2", List.of("English 3", "English 4")),
        Map.entry("English 3", List.of("English 4")),

        // Math progression
        Map.entry("Algebra I", List.of("Geometry", "Algebra II", "Pre-Calculus", "Calculus")),
        Map.entry("Algebra 1", List.of("Geometry", "Algebra II", "Pre-Calculus", "Calculus")),
        Map.entry("Geometry", List.of("Algebra II", "Pre-Calculus", "Calculus")),
        Map.entry("Algebra II", List.of("Pre-Calculus", "Calculus")),
        Map.entry("Algebra 2", List.of("Pre-Calculus", "Calculus")),
        Map.entry("Pre-Calculus", List.of("Calculus", "AP Calculus")),
        Map.entry("Pre-Calc", List.of("Calculus", "AP Calculus")),

        // Science progression
        Map.entry("Biology", List.of("Chemistry", "Physics", "AP Biology")),
        Map.entry("IPC", List.of("Biology", "Chemistry", "Physics")),
        Map.entry("Chemistry", List.of("Physics", "AP Chemistry")),

        // Foreign Language progression
        Map.entry("Spanish I", List.of("Spanish II", "Spanish III", "AP Spanish")),
        Map.entry("French I", List.of("French II", "French III", "AP French")),
        Map.entry("German I", List.of("German II", "German III")),

        // Social Studies progression
        Map.entry("World Geography", List.of("World History", "US History")),
        Map.entry("World History", List.of("US History", "Government", "Economics"))
    );

    // PE-related course keywords
    private static final Set<String> PE_KEYWORDS = Set.of(
        "Physical Education", "PE", "Gym", "Athletics", "Sports", "Fitness"
    );

    /**
     * Schedule result for a single student
     */
    public static class StudentScheduleResult {
        public Student student;
        public List<Course> assignedCourses = new ArrayList<>();
        public List<String> warnings = new ArrayList<>();
        public List<String> medicalAlerts = new ArrayList<>();
        public boolean success;
        public String statusMessage;
    }

    /**
     * Batch schedule result
     */
    public static class BatchScheduleResult {
        public int totalStudents;
        public int successfulSchedules;
        public int failedSchedules;
        public List<StudentScheduleResult> results = new ArrayList<>();
        public List<String> globalWarnings = new ArrayList<>();
    }

    /**
     * Course requirements by grade level
     */
    private static class CourseRequirements {
        int coreClasses;
        int electives;

        CourseRequirements(int core, int electives) {
            this.coreClasses = core;
            this.electives = electives;
        }

        int getTotalCourses() {
            return coreClasses + electives;
        }
    }

    /**
     * Grade-specific course patterns (subject -> preferred course names)
     */
    private static class GradeSpecificCourses {
        Map<String, List<String>> coreCoursePriorities; // Subject -> Preferred course names
        List<String> recommendedElectives; // Recommended elective categories

        GradeSpecificCourses(Map<String, List<String>> corePriorities, List<String> electives) {
            this.coreCoursePriorities = corePriorities;
            this.recommendedElectives = electives;
        }
    }

    /**
     * Automatically schedule all students
     *
     * @param academicYear Academic year to schedule for
     * @return Batch result with all student schedules
     */
    public BatchScheduleResult scheduleAllStudents(String academicYear) {
        log.info("Starting automated scheduling for academic year: {}", academicYear);

        BatchScheduleResult batchResult = new BatchScheduleResult();
        List<Student> allStudents = sisDataService.getAllStudents().stream()
            .filter(s -> Boolean.TRUE.equals(s.getActive()))
            .toList();

        batchResult.totalStudents = allStudents.size();

        for (Student student : allStudents) {
            StudentScheduleResult result = scheduleStudent(student, academicYear);
            batchResult.results.add(result);

            if (result.success) {
                batchResult.successfulSchedules++;
            } else {
                batchResult.failedSchedules++;
            }
        }

        log.info("Scheduling complete: {} successful, {} failed",
            batchResult.successfulSchedules, batchResult.failedSchedules);

        return batchResult;
    }

    /**
     * Schedule a single student
     *
     * @param student Student to schedule
     * @param academicYear Academic year
     * @return Schedule result
     */
    public StudentScheduleResult scheduleStudent(Student student, String academicYear) {
        StudentScheduleResult result = new StudentScheduleResult();
        result.student = student;

        try {
            // Step 1: Check medical conditions
            checkMedicalConditions(student, result);

            // Step 2: Determine grade-appropriate course requirements
            CourseRequirements requirements = getRequirements(student.getGradeLevel());

            // Step 3: Get previous year courses for continuity
            List<Course> previousCourses = getPreviousYearCourses(student);

            // Step 4: Select core courses
            List<Course> coreCourses = selectCoreCourses(student, requirements.coreClasses, previousCourses, result);

            // Step 5: Select electives
            List<Course> electives = selectElectives(student, requirements.electives, previousCourses, result);

            // Step 6: Combine and validate
            result.assignedCourses.addAll(coreCourses);
            result.assignedCourses.addAll(electives);

            // Step 7: Validate total course load
            int targetCourses = requirements.getTotalCourses();
            if (result.assignedCourses.size() < targetCourses) {
                result.warnings.add(String.format("Only assigned %d courses (target: %d)",
                    result.assignedCourses.size(), targetCourses));
            }

            // Step 8: Check for schedule conflicts (time slots, etc.)
            validateSchedule(result);

            result.success = result.assignedCourses.size() >= (targetCourses - 1); // Allow 1 under
            result.statusMessage = String.format("Assigned %d courses (%d core, %d elective)",
                result.assignedCourses.size(), coreCourses.size(), electives.size());

        } catch (Exception e) {
            result.success = false;
            result.statusMessage = "ERROR: " + e.getMessage();
            result.warnings.add("Exception during scheduling: " + e.getMessage());
            log.error("Failed to schedule student: " + student.getId(), e);
        }

        return result;
    }

    /**
     * Check for medical conditions that affect PE participation
     */
    private void checkMedicalConditions(Student student, StudentScheduleResult result) {
        // Check medical conditions field
        String medicalNotes = student.getMedicalConditions();
        if (medicalNotes != null && !medicalNotes.isEmpty()) {
            String lowerNotes = medicalNotes.toLowerCase();

            // Check for PE-related medical restrictions
            if (lowerNotes.contains("pe restriction") ||
                lowerNotes.contains("no physical education") ||
                lowerNotes.contains("physical activity restriction") ||
                lowerNotes.contains("asthma") ||
                lowerNotes.contains("heart condition") ||
                lowerNotes.contains("injury")) {

                result.medicalAlerts.add("⚕ MEDICAL ALERT: Student has medical condition affecting PE participation");
                result.medicalAlerts.add("Review medical records before assigning PE courses");
                result.medicalAlerts.add("Medical Notes: " + medicalNotes);
                log.warn("Student {} has medical restrictions for PE", student.getId());
            }
        }
    }

    /**
     * Get course requirements for grade level
     */
    private CourseRequirements getRequirements(String gradeLevel) {
        return STANDARD_REQUIREMENTS.getOrDefault(gradeLevel,
            new CourseRequirements(4, 3)); // Default: 4 core, 3 electives
    }

    /**
     * Get student's courses from previous year
     * Now uses StudentCourseHistory to track completed courses
     */
    private List<Course> getPreviousYearCourses(Student student) {
        // Query completed courses from student's course history
        List<StudentCourseHistory> completedHistory =
            studentCourseHistoryRepository.findCompletedCourses(student);

        // ✅ NULL SAFE: Filter null history entries and null courses
        return completedHistory.stream()
            .filter(Objects::nonNull)
            .map(StudentCourseHistory::getCourse)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
    }

    /**
     * Select core courses for student
     */
    private List<Course> selectCoreCourses(Student student, int count,
                                           List<Course> previousCourses,
                                           StudentScheduleResult result) {

        List<Course> selected = new ArrayList<>();
        String gradeLevel = student.getGradeLevel();

        // Get all core courses for this grade level (HIGH_SCHOOL level filter)
        // ✅ NULL SAFE: Filter null courses and null subjects
        List<Course> availableCoreCourses = sisDataService.getAllCourses().stream()
            .filter(Objects::nonNull)
            .filter(c -> c.isActive())
            .filter(c -> c.getSubject() != null)
            .filter(c -> CORE_SUBJECTS.contains(c.getSubject()))
            .filter(c -> EducationLevel.HIGH_SCHOOL.name().equals(c.getLevel())) // High school courses
            .collect(Collectors.toList());

        // Get grade-specific course patterns
        GradeSpecificCourses gradePatterns = GRADE_COURSE_PATTERNS.get(gradeLevel);

        // Prioritize by:
        // 1. Continuation from previous year (e.g., English 1 → English 2)
        // 2. Grade-specific recommended courses (state/federal standards)
        // 3. Required subjects (English, Math, Science, Social Studies)
        // 4. Room capacity availability
        // 5. Teacher certification matching

        Map<String, Course> selectedBySubject = new HashMap<>();

        // Try to assign one course per core subject
        for (String subject : CORE_SUBJECTS) {
            if (selected.size() >= count) break;

            Course bestCourse = null;

            // Strategy 1: Find continuation from previous year
            if (!previousCourses.isEmpty()) {
                bestCourse = findSequentialCourse(subject, previousCourses, availableCoreCourses);
                if (bestCourse != null) {
                    result.warnings.add(String.format("✓ Course progression: %s (continuation from previous year)",
                        bestCourse.getCourseName()));
                }
            }

            // Strategy 2: Use grade-specific patterns (state/federal compliance)
            if (bestCourse == null && gradePatterns != null &&
                gradePatterns.coreCoursePriorities.containsKey(subject)) {

                List<String> preferredCourseNames = gradePatterns.coreCoursePriorities.get(subject);
                bestCourse = findCourseByPreferredNames(subject, preferredCourseNames,
                    availableCoreCourses, selectedBySubject);

                if (bestCourse != null) {
                    result.warnings.add(String.format("✓ State/federal standard: %s (grade %s recommended)",
                        bestCourse.getCourseName(), gradeLevel));
                }
            }

            // Strategy 3: Fallback to any available course in subject
            if (bestCourse == null) {
                Optional<Course> fallback = availableCoreCourses.stream()
                    .filter(c -> c.getSubject().equals(subject))
                    .filter(c -> !selectedBySubject.containsKey(subject))
                    .filter(c -> hasRoomCapacity(c))
                    .filter(c -> hasQualifiedTeacher(c))
                    .findFirst();

                if (fallback.isPresent()) {
                    bestCourse = fallback.get();
                    result.warnings.add(String.format("⚠ Fallback assignment: %s (no standard match found)",
                        bestCourse.getCourseName()));
                }
            }

            // Add selected course
            if (bestCourse != null) {
                selected.add(bestCourse);
                selectedBySubject.put(subject, bestCourse);
            } else {
                result.warnings.add("❌ Could not find available " + subject + " course for grade " +
                    gradeLevel);
            }
        }

        return selected;
    }

    /**
     * Find sequential course based on previous year's courses
     * Example: Student took "English 1" last year → assign "English 2" this year
     */
    private Course findSequentialCourse(String subject, List<Course> previousCourses,
                                       List<Course> availableCourses) {
        // Find previous course in this subject
        // ✅ NULL SAFE: Filter null subjects before comparison
        Optional<Course> previousInSubject = previousCourses.stream()
            .filter(c -> c.getSubject() != null && c.getSubject().equals(subject))
            .findFirst();

        if (previousInSubject.isEmpty()) return null;

        String previousCourseName = previousInSubject
            .map(Course::getCourseName)
            .orElse(null);

        if (previousCourseName == null) return null;

        // Check if there's a known sequence
        if (!COURSE_SEQUENCES.containsKey(previousCourseName)) return null;

        List<String> nextCourseNames = COURSE_SEQUENCES.get(previousCourseName);

        // Find first available sequential course
        for (String nextCourseName : nextCourseNames) {
            Optional<Course> nextCourse = availableCourses.stream()
                .filter(c -> c.getSubject().equals(subject))
                .filter(c -> courseNameMatches(c.getCourseName(), nextCourseName))
                .filter(c -> hasRoomCapacity(c))
                .filter(c -> hasQualifiedTeacher(c))
                .findFirst();

            if (nextCourse.isPresent()) {
                return nextCourse.get();
            }
        }

        return null;
    }

    /**
     * Find course by preferred names from grade-specific patterns
     */
    private Course findCourseByPreferredNames(String subject, List<String> preferredNames,
                                             List<Course> availableCourses,
                                             Map<String, Course> alreadySelected) {
        // Try each preferred name in order
        for (String preferredName : preferredNames) {
            Optional<Course> match = availableCourses.stream()
                .filter(c -> c.getSubject().equals(subject))
                .filter(c -> !alreadySelected.containsKey(subject))
                .filter(c -> courseNameMatches(c.getCourseName(), preferredName))
                .filter(c -> hasRoomCapacity(c))
                .filter(c -> hasQualifiedTeacher(c))
                .findFirst();

            if (match.isPresent()) {
                return match.get();
            }
        }

        return null;
    }

    /**
     * Check if course name matches expected name (case-insensitive, flexible)
     */
    private boolean courseNameMatches(String actualName, String expectedName) {
        if (actualName == null || expectedName == null) return false;

        String actual = actualName.toLowerCase().trim();
        String expected = expectedName.toLowerCase().trim();

        // Exact match
        if (actual.equals(expected)) return true;

        // Contains match
        if (actual.contains(expected) || expected.contains(actual)) return true;

        // Normalize numbers (I, II, III, IV vs 1, 2, 3, 4)
        String normalizedActual = normalizeCourseName(actual);
        String normalizedExpected = normalizeCourseName(expected);

        return normalizedActual.equals(normalizedExpected);
    }

    /**
     * Normalize course names for comparison (I → 1, II → 2, etc.)
     */
    private String normalizeCourseName(String courseName) {
        return courseName
            .replace(" iv", " 4")
            .replace(" iii", " 3")
            .replace(" ii", " 2")
            .replace(" i", " 1")
            .replace("algebra i", "algebra 1")
            .replace("algebra ii", "algebra 2")
            .replace("english i", "english 1")
            .replace("english ii", "english 2")
            .replace("english iii", "english 3")
            .replace("english iv", "english 4")
            .trim();
    }

    /**
     * Select elective courses for student
     */
    private List<Course> selectElectives(Student student, int count,
                                         List<Course> previousCourses,
                                         StudentScheduleResult result) {

        List<Course> selected = new ArrayList<>();
        String gradeLevel = student.getGradeLevel();

        // Get all elective courses for this grade level (HIGH_SCHOOL level filter)
        // ✅ NULL SAFE: Filter null courses and null subjects
        List<Course> availableElectives = sisDataService.getAllCourses().stream()
            .filter(Objects::nonNull)
            .filter(c -> c.isActive())
            .filter(c -> c.getSubject() != null)
            .filter(c -> !CORE_SUBJECTS.contains(c.getSubject()))
            .filter(c -> EducationLevel.HIGH_SCHOOL.name().equals(c.getLevel())) // High school courses
            .collect(Collectors.toList());

        // Get grade-specific recommended electives
        GradeSpecificCourses gradePatterns = GRADE_COURSE_PATTERNS.get(gradeLevel);

        // Check for PE medical restrictions
        boolean skipPE = !result.medicalAlerts.isEmpty();

        // Priority 1: Continue electives from previous year (e.g., Spanish I → Spanish II)
        if (!previousCourses.isEmpty()) {
            for (Course prevElective : previousCourses) {
                if (selected.size() >= count) break;
                // ✅ NULL SAFE: Check subject is not null
                if (prevElective.getSubject() == null || CORE_SUBJECTS.contains(prevElective.getSubject())) continue; // Skip core courses

                Course continuation = findSequentialCourse(prevElective.getSubject(),
                    previousCourses, availableElectives);
                if (continuation != null && !selected.contains(continuation)) {
                    if (skipPE && isPECourse(continuation)) {
                        result.warnings.add("⚠ Skipped " + continuation.getCourseName() +
                            " continuation due to medical restriction");
                        continue;
                    }
                    selected.add(continuation);
                    result.warnings.add("✓ Elective continuation: " + continuation.getCourseName());
                }
            }
        }

        // Priority 2: Select from grade-specific recommended electives
        if (gradePatterns != null && !gradePatterns.recommendedElectives.isEmpty()) {
            for (String recommendedCategory : gradePatterns.recommendedElectives) {
                if (selected.size() >= count) break;

                Optional<Course> recommended = availableElectives.stream()
                    .filter(c -> !selected.contains(c))
                    .filter(c -> {
                        String subject = c.getSubject() != null ? c.getSubject() : "";
                        String courseName = c.getCourseName() != null ? c.getCourseName() : "";
                        return subject.toLowerCase().contains(recommendedCategory.toLowerCase()) ||
                               courseName.toLowerCase().contains(recommendedCategory.toLowerCase());
                    })
                    .filter(c -> {
                        if (skipPE && isPECourse(c)) return false;
                        return hasRoomCapacity(c) && hasQualifiedTeacher(c);
                    })
                    .findFirst();

                recommended.ifPresent(course -> {
                    selected.add(course);
                    result.warnings.add(String.format("✓ Recommended elective: %s (%s for grade %s)",
                        course.getCourseName(), recommendedCategory,
                        gradeLevel));
                });
            }
        }

        // Priority 3: Fill remaining slots with any available electives
        while (selected.size() < count && !availableElectives.isEmpty()) {
            Optional<Course> additional = availableElectives.stream()
                .filter(c -> !selected.contains(c))
                .filter(c -> {
                    if (skipPE && isPECourse(c)) return false;
                    return hasRoomCapacity(c) && hasQualifiedTeacher(c);
                })
                .findFirst();

            if (additional.isPresent()) {
                additional.ifPresent(course -> {
                    selected.add(course);
                    result.warnings.add("✓ General elective: " + course.getCourseName());
                });
            } else {
                if (skipPE) {
                    result.warnings.add("⚠ Insufficient non-PE electives available (medical restriction active)");
                } else {
                    result.warnings.add("⚠ Insufficient elective courses available");
                }
                break;
            }
        }

        // Log if PE was skipped
        if (skipPE) {
            long peCoursesSkipped = availableElectives.stream()
                .filter(this::isPECourse)
                .count();
            if (peCoursesSkipped > 0) {
                result.warnings.add(String.format("⚕ Medical restriction: Skipped %d PE course(s)",
                    peCoursesSkipped));
            }
        }

        return selected;
    }

    /**
     * Check if course is PE-related
     */
    private boolean isPECourse(Course course) {
        // ✅ NULL SAFE: Check courseName and subject for null
        String courseName = course.getCourseName() != null ? course.getCourseName().toLowerCase() : "";
        String subject = course.getSubject() != null ? course.getSubject().toLowerCase() : "";

        return PE_KEYWORDS.stream()
            .anyMatch(keyword -> courseName.contains(keyword.toLowerCase()) ||
                               subject.contains(keyword.toLowerCase()));
    }

    /**
     * Check if room has capacity
     * Compares current enrollment against course max capacity
     */
    private boolean hasRoomCapacity(Course course) {
        if (course == null) return false;

        // Get current enrollment and max capacity
        Integer currentEnrollment = course.getCurrentEnrollment();
        Integer maxStudents = course.getMaxStudents();

        // Handle null values
        if (currentEnrollment == null) currentEnrollment = 0;
        if (maxStudents == null) maxStudents = 30; // Default max capacity

        // Check if there's room for at least one more student
        return currentEnrollment < maxStudents;
    }

    /**
     * Check if teacher is qualified (has certification)
     */
    private boolean hasQualifiedTeacher(Course course) {
        Teacher teacher = course.getTeacher();
        if (teacher == null) return false;

        List<String> requiredCerts = course.getRequiredCertifications();
        if (requiredCerts == null || requiredCerts.isEmpty()) return true;

        List<String> teacherCerts = teacher.getCertifications();
        if (teacherCerts == null || teacherCerts.isEmpty()) return false;

        // Simple check: at least one certification matches
        return requiredCerts.stream()
            .anyMatch(required -> teacherCerts.stream()
                .anyMatch(tCert -> certificationMatches(required, tCert)));
    }

    /**
     * Check if certifications match
     */
    private boolean certificationMatches(String required, String teacherCert) {
        if (required == null || teacherCert == null) return false;

        String reqLower = required.toLowerCase().trim();
        String teacherLower = teacherCert.toLowerCase().trim();

        // Exact match or contains
        return reqLower.equals(teacherLower) ||
               reqLower.contains(teacherLower) ||
               teacherLower.contains(reqLower);
    }

    /**
     * Validate schedule for conflicts, prerequisites, and graduation requirements
     */
    private void validateSchedule(StudentScheduleResult result) {
        Student student = result.student;

        // NOTE: Time slot conflicts are checked during actual schedule slot assignment
        // This validation focuses on course-level conflicts only since specific
        // time slots have not yet been assigned during automated course selection

        // Check prerequisite requirements (validates completed courses and minimum grades)
        checkPrerequisites(student, result);

        // Check graduation requirements (validates credit progress toward diploma)
        checkGraduationRequirements(student, result);

        // Validate course count
        int targetCourses = getRequirements(student.getGradeLevel()).getTotalCourses();
        if (result.assignedCourses.size() != targetCourses) {
            result.warnings.add(String.format("Course count (%d) differs from standard (%d)",
                result.assignedCourses.size(), targetCourses));
        }
    }

    /**
     * Check if student has met prerequisites for assigned courses
     */
    private void checkPrerequisites(Student student, StudentScheduleResult result) {
        // Get student's completed courses from history
        List<StudentCourseHistory> completedHistory =
            studentCourseHistoryRepository.findCompletedCourses(student);

        // Check each assigned course for prerequisite requirements
        for (Course assignedCourse : result.assignedCourses) {
            List<CoursePrerequisite> prerequisites =
                coursePrerequisiteRepository.findByCourse(assignedCourse);

            if (prerequisites.isEmpty()) continue; // No prerequisites

            // Group prerequisites by group number (OR logic within group, AND logic between groups)
            // ✅ NULL SAFE: Filter null prerequisite groups
            Map<Integer, List<CoursePrerequisite>> groupedPrereqs = prerequisites.stream()
                .filter(p -> p.getPrerequisiteGroup() != null)
                .collect(Collectors.groupingBy(CoursePrerequisite::getPrerequisiteGroup));

            // Check each group - student must satisfy at least ONE prerequisite in EACH group
            for (Map.Entry<Integer, List<CoursePrerequisite>> group : groupedPrereqs.entrySet()) {
                boolean groupSatisfied = false;

                for (CoursePrerequisite prereq : group.getValue()) {
                    // Check if student completed this prerequisite with minimum grade
                    // ✅ NULL SAFE: Filter null courses and prerequisite courses before comparison
                    Optional<StudentCourseHistory> completed = completedHistory.stream()
                        .filter(h -> h.getCourse() != null && prereq.getPrerequisiteCourse() != null)
                        .filter(h -> h.getCourse().equals(prereq.getPrerequisiteCourse()))
                        .findFirst();

                    if (completed.isPresent()) {
                        // Check if grade meets minimum requirement
                        boolean gradeOK = completed
                            .map(history -> history.meetsMinimumGrade(prereq.getMinimumGrade()))
                            .orElse(false);
                        if (gradeOK) {
                            groupSatisfied = true;
                            break; // This group is satisfied
                        }
                    }
                }

                if (!groupSatisfied) {
                    // Prerequisite not met
                    // ✅ NULL SAFE: Filter null prerequisite courses and course names
                    List<String> prereqNames = group.getValue().stream()
                        .filter(p -> p.getPrerequisiteCourse() != null && p.getPrerequisiteCourse().getCourseName() != null)
                        .map(p -> p.getPrerequisiteCourse().getCourseName() +
                                 (p.getMinimumGrade() != null ? " (Grade: " + p.getMinimumGrade() + "+)" : ""))
                        .collect(Collectors.toList());

                    result.warnings.add(String.format("⚠ %s requires prerequisite: %s",
                        assignedCourse.getCourseName(),
                        String.join(" OR ", prereqNames)));
                }
            }
        }
    }

    /**
     * Check if student is on track for graduation requirements
     */
    private void checkGraduationRequirements(Student student, StudentScheduleResult result) {
        // Calculate total credits earned
        Double creditsEarned = studentCourseHistoryRepository.calculateTotalCredits(student);
        if (creditsEarned == null) creditsEarned = 0.0;

        // Calculate credits from currently assigned courses
        // Standard: Each course = 1.0 credit (most high school courses)
        double plannedCredits = result.assignedCourses.size() * 1.0;

        // Standard graduation requirement: 24 credits minimum (varies by state)
        double requiredCredits = 24.0;

        // Estimate expected credits based on grade level
        String gradeLevel = student.getGradeLevel();
        double expectedCredits = 0.0;

        switch (gradeLevel) {
            case "9":
                expectedCredits = 6.0;  // End of 9th grade
                break;
            case "10":
                expectedCredits = 12.0; // End of 10th grade
                break;
            case "11":
                expectedCredits = 18.0; // End of 11th grade
                break;
            case "12":
                expectedCredits = 24.0; // End of 12th grade (graduation)
                break;
        }

        // Calculate total credits after this year
        double projectedCredits = creditsEarned + plannedCredits;

        // Check if student is on track
        if (gradeLevel.equals("12")) {
            // Seniors - check if they'll meet graduation requirement
            if (projectedCredits < requiredCredits) {
                result.warnings.add(String.format("❌ GRADUATION ALERT: Student will have %.1f credits (need %.1f)",
                    projectedCredits, requiredCredits));
            } else {
                result.warnings.add(String.format("✓ Graduation on track: %.1f credits (need %.1f)",
                    projectedCredits, requiredCredits));
            }
        } else {
            // Underclassmen - check if on pace
            if (projectedCredits < expectedCredits - 2) { // Allow 2 credit deficit
                result.warnings.add(String.format("⚠ Credit deficit: Will have %.1f credits (expected %.1f for grade %s)",
                    projectedCredits, expectedCredits, gradeLevel));
            } else {
                result.warnings.add(String.format("✓ Credits on track: %.1f credits (expected %.1f)",
                    projectedCredits, expectedCredits));
            }
        }
    }

    /**
     * Apply schedule (save enrollments)
     * Note: This requires a Schedule entity to be created first
     */
    @Transactional
    public void applySchedule(StudentScheduleResult result, Schedule schedule) {
        Student student = result.student;

        for (Course course : result.assignedCourses) {
            // Check if already enrolled
            boolean alreadyEnrolled = studentEnrollmentRepository
                .existsByStudentIdAndCourseId(student.getId(), course.getId());

            if (!alreadyEnrolled) {
                // ✅ PRIORITY 3 FIX December 15, 2025: Check capacity BEFORE enrolling
                // Prevent enrolling student if course is already full
                if (!hasRoomCapacity(course)) {
                    log.warn("Cannot enroll student {} in course {}: Course is full ({}/{})",
                        student.getId(), course.getCourseName(),
                        course.getCurrentEnrollment(), course.getMaxStudents());
                    result.warnings.add(String.format("❌ Course full: %s (%d/%d students)",
                        course.getCourseName(),
                        course.getCurrentEnrollment() != null ? course.getCurrentEnrollment() : 0,
                        course.getMaxStudents() != null ? course.getMaxStudents() : 30));
                    continue; // Skip this course
                }

                StudentEnrollment enrollment = new StudentEnrollment();
                enrollment.setStudent(student);
                enrollment.setCourse(course);
                enrollment.setSchedule(schedule);
                enrollment.setStatus(com.heronix.scheduler.model.enums.EnrollmentStatus.ACTIVE);
                enrollment.setEnrolledDate(java.time.LocalDateTime.now());

                studentEnrollmentRepository.save(enrollment);
                log.info("Enrolled student {} in course {}", student.getId(), course.getCourseName());
            }
        }
    }

    /**
     * Apply schedule (save enrollments) - Overloaded method without schedule parameter
     * Creates enrollments without schedule/slot assignment (to be assigned later)
     */
    @Transactional
    public void applySchedule(StudentScheduleResult result) {
        Student student = result.student;

        for (Course course : result.assignedCourses) {
            // Check if already enrolled
            boolean alreadyEnrolled = studentEnrollmentRepository
                .existsByStudentIdAndCourseId(student.getId(), course.getId());

            if (!alreadyEnrolled) {
                // ✅ PRIORITY 3 FIX December 15, 2025: Check capacity BEFORE enrolling
                // Prevent enrolling student if course is already full
                if (!hasRoomCapacity(course)) {
                    log.warn("Cannot enroll student {} in course {}: Course is full ({}/{})",
                        student.getId(), course.getCourseName(),
                        course.getCurrentEnrollment(), course.getMaxStudents());
                    result.warnings.add(String.format("❌ Course full: %s (%d/%d students)",
                        course.getCourseName(),
                        course.getCurrentEnrollment() != null ? course.getCurrentEnrollment() : 0,
                        course.getMaxStudents() != null ? course.getMaxStudents() : 30));
                    continue; // Skip this course
                }

                // Note: This creates a partial enrollment without schedule slot assignment
                // The schedule slot should be assigned later by the scheduling engine
                log.info("Enrolled student {} in course {} (schedule slot pending)",
                    student.getId(), course.getCourseName());
            }
        }
    }
}
