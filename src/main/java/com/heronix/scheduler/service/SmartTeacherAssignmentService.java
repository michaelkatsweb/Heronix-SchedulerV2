package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Teacher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.heronix.scheduler.service.data.SISDataService;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Smart Teacher Assignment Service - UPDATED VERSION
 * Location: src/main/java/com/eduscheduler/service/SmartTeacherAssignmentService.java
 *
 * Automatically assigns teachers to courses using intelligent matching:
 * 1. Strict subject certification matching (Science → Science ONLY)
 * 2. Workload balancing with in-memory tracking (prevents over-assignment)
 * 3. LIMITED workload: 2 courses optimal, 3 courses maximum (per administrator request)
 * 4. Course sequencing logic (English 1 → English 2 same teacher)
 * 5. Teacher shortage handling: Stops assignment, alerts administrators to hire
 *
 * UPDATED December 14, 2025:
 * - Changed workload limits from 5-6 courses to 2-3 courses per teacher
 * - Administrator must handle teacher shortages (no over-assignment)
 * - Clear error messages distinguish "no teacher" vs "teachers at capacity"
 *
 * @version 2.1.0 - UPDATED: 2-3 course limit, shortage handling
 * @since 2025-11-20 (original), 2025-12-14 (workload limits updated)
 */
@Slf4j
@Service
public class SmartTeacherAssignmentService {

    @Autowired
    private SISDataService sisDataService;
    // UPDATED December 14, 2025: Per user request - limit to 2-3 courses per teacher
    // "a teacher should not be assigned more than 2 courses in some cases 3 max"
    // "if there is a shortage of teachers then that needs to be handled by administrators"

    // Workload Limiting Mode: Choose between COURSE_COUNT, CREDIT_HOURS, or TEACHING_PERIODS
    public enum WorkloadLimitMode {
        COURSE_COUNT,      // Limit by number of courses (simpler, works for uniform schedules)
        CREDIT_HOURS,      // Limit by credit hours (more accurate for mixed semester/year courses)
        TEACHING_PERIODS   // Limit by teaching periods (MOST ACCURATE for real-world scheduling)
                          // Example: Teacher teaches 6 periods/day (excluding planning period)
    }

    // Configuration: Choose which mode to use
    // COURSE_COUNT: Simpler, recommended for basic scheduling
    // CREDIT_HOURS: More complex, better for schools with mixed semester/year schedules
    // TEACHING_PERIODS: Most accurate, matches real-world teacher workload (RECOMMENDED)
    private static final WorkloadLimitMode WORKLOAD_MODE = WorkloadLimitMode.TEACHING_PERIODS;

    // Course count limits (used when WORKLOAD_MODE = COURSE_COUNT)
    private static final int MAX_WORKLOAD_COURSES = 3;        // Hard limit: 3 courses maximum
    private static final int OPTIMAL_WORKLOAD_COURSES = 2;    // Target: 2 courses per teacher
    private static final int WARNING_WORKLOAD_COURSES = 2;    // Warn when reaching 2 courses

    // Credit hour limits (used when WORKLOAD_MODE = CREDIT_HOURS)
    // Example: 2 year-long courses (1.0 × 2 = 2.0 credits) or 4 semester courses (0.5 × 4 = 2.0 credits)
    private static final double MAX_WORKLOAD_CREDITS = 3.0;      // Hard limit: 3.0 credit hours
    private static final double OPTIMAL_WORKLOAD_CREDITS = 2.0;  // Target: 2.0 credit hours
    private static final double WARNING_WORKLOAD_CREDITS = 2.0;  // Warn at 2.0 credits

    // Teaching period limits (used when WORKLOAD_MODE = TEACHING_PERIODS) - RECOMMENDED
    // Real-world example: 7 periods/day - 1 planning period = 6 teaching periods
    // Teacher Maria: Periods 1,2,3,5,6,7 teaching, Period 4 planning
    // Geometry teacher: 6 periods teaching Geometry (6 sections), 1 planning
    private static final int MAX_WORKLOAD_PERIODS = 6;        // Hard limit: 6 teaching periods max
    private static final int OPTIMAL_WORKLOAD_PERIODS = 5;    // Target: 5 teaching periods
    private static final int WARNING_WORKLOAD_PERIODS = 5;    // Warn when reaching 5 periods

    // ✅ PRIORITY 3 FIX December 15, 2025: Regex pattern cache for performance
    // Prevents recompiling the same patterns repeatedly in matchesFamily()
    private static final java.util.concurrent.ConcurrentHashMap<String, java.util.regex.Pattern> PATTERN_CACHE =
        new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Smart assign teachers to all unassigned courses
     * IMPROVED: Tracks workload in-memory, strict certification, course sequencing
     *
     * @return AssignmentResult with statistics
     */
    @Transactional
    public AssignmentResult smartAssignAllTeachers() {
        log.info("Starting IMPROVED smart teacher assignment...");

        AssignmentResult result = new AssignmentResult();
        result.setStartTime(System.currentTimeMillis());

        try {
            // Get all courses that need teacher assignment
            // NULL SAFETY: Handle null course list from repository
            List<Course> allCourses = sisDataService.getAllCourses();
            if (allCourses == null) {
                log.warn("Course repository returned null");
                result.setMessage("Unable to load courses");
                result.setEndTime(System.currentTimeMillis());
                return result;
            }

            List<Course> unassignedCourses = allCourses.stream()
                .filter(c -> c.getTeacher() == null)
                .collect(Collectors.toList());

            result.setTotalCoursesProcessed(unassignedCourses.size());
            log.info("Found {} courses without teachers", unassignedCourses.size());

            if (unassignedCourses.isEmpty()) {
                result.setMessage("All courses already have teachers assigned");
                result.setEndTime(System.currentTimeMillis());
                return result;
            }

            // Get all available teachers with courses loaded
            // NULL SAFETY: Handle null teacher list from repository
            List<Teacher> allTeachers = sisDataService.getAllTeachers();
            if (allTeachers == null) {
                log.warn("Teacher repository returned null");
                result.setMessage("Unable to load teachers");
                result.setEndTime(System.currentTimeMillis());
                return result;
            }

            log.info("Found {} teachers available for assignment", allTeachers.size());

            if (allTeachers.isEmpty()) {
                result.setMessage("No teachers available for assignment");
                result.setEndTime(System.currentTimeMillis());
                return result;
            }

            // FIX #1: In-memory workload tracking to prevent over-assignment
            // Supports COURSE_COUNT, CREDIT_HOURS, and TEACHING_PERIODS modes
            // ✅ PRIORITY 2 FIX December 15, 2025: Use ConcurrentHashMap for thread safety
            // Prevents race conditions if multiple threads call smartAssignAllTeachers()
            Map<Teacher, Double> workloadTracker = new java.util.concurrent.ConcurrentHashMap<>();
            String unit = WORKLOAD_MODE == WorkloadLimitMode.CREDIT_HOURS ? "credits"
                        : WORKLOAD_MODE == WorkloadLimitMode.TEACHING_PERIODS ? "periods"
                        : "courses";
            for (Teacher teacher : allTeachers) {
                double initialWorkload = calculateTeacherWorkload(teacher);
                workloadTracker.put(teacher, initialWorkload);
                log.info("🔍 INIT: Teacher {} starts with {} {} (courses collection size: {})",
                    teacher.getName(),
                    initialWorkload,
                    unit,
                    teacher.getCourses() != null ? teacher.getCourses().size() : "NULL");
            }
            log.info("📊 Initialized workload tracker for {} teachers", workloadTracker.size());

            // FIX #4: Group courses by sequences (English 1 → English 2)
            Map<String, List<Course>> courseSequences = groupCourseSequences(unassignedCourses);

            // Sort courses: sequences first, then by priority
            List<Course> sortedCourses = new ArrayList<>();

            // Add sequence groups first (English 1, English 2 together)
            for (Map.Entry<String, List<Course>> entry : courseSequences.entrySet()) {
                if (entry.getValue().size() > 1) {  // Is a sequence
                    sortedCourses.addAll(entry.getValue());
                }
            }

            // Add non-sequence courses
            for (Map.Entry<String, List<Course>> entry : courseSequences.entrySet()) {
                if (entry.getValue().size() == 1) {  // Not a sequence
                    sortedCourses.addAll(entry.getValue());
                }
            }

            // Sort by priority within groups
            sortedCourses.sort(Comparator
                .comparing(Course::getPriorityLevel, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Course::getLevel, Comparator.nullsLast(Comparator.naturalOrder())));

            // Track which teacher was assigned to which sequence
            Map<String, Teacher> sequenceTeacherMap = new HashMap<>();

            // Assign teachers to courses
            for (Course course : sortedCourses) {
                String sequenceKey = getSequenceKey(course);

                // FIX #4: If this course is part of a sequence, check if we already assigned that sequence
                Teacher assignedTeacher = null;
                if (courseSequences.get(sequenceKey).size() > 1 && sequenceTeacherMap.containsKey(sequenceKey)) {
                    // Try to assign to same teacher as previous course in sequence
                    Teacher sequenceTeacher = sequenceTeacherMap.get(sequenceKey);
                    double maxLimit = getMaxWorkloadLimit();
                    if (workloadTracker.get(sequenceTeacher) < maxLimit) {
                        assignedTeacher = sequenceTeacher;
                        log.info("Assigning {} to {} (continuing sequence)", course.getCourseName(), sequenceTeacher.getName());
                    }
                }

                // If not part of sequence or sequence teacher unavailable, find best teacher
                if (assignedTeacher == null) {
                    assignedTeacher = findBestTeacher(course, allTeachers, workloadTracker);
                }

                if (assignedTeacher != null) {
                    course.setTeacher(assignedTeacher);
                    // Note: Course teacher assignment changes should be synced back to SIS via API
                    result.incrementAssigned();

                    // FIX #1: Update in-memory tracker (supports both modes)
                    double oldWorkload = workloadTracker.get(assignedTeacher);
                    double courseWorkload = getCourseWorkload(course);
                    double newWorkload = oldWorkload + courseWorkload;
                    workloadTracker.put(assignedTeacher, newWorkload);

                    // Track sequence assignments
                    if (courseSequences.get(sequenceKey).size() > 1) {
                        sequenceTeacherMap.put(sequenceKey, assignedTeacher);
                    }

                    // UPDATED December 14, 2025: Warn at optimal limit (mode-aware)
                    double warningLimit = getWarningWorkloadLimit();
                    double optimalLimit = getOptimalWorkloadLimit();
                    if (newWorkload >= warningLimit) {
                        String warningMsg = newWorkload <= optimalLimit
                            ? String.format("Teacher %s now has %.1f %s (at optimal limit - consider hiring more teachers)",
                                assignedTeacher.getName(), newWorkload, unit)
                            : String.format("Teacher %s now has %.1f %s (OVER optimal limit of %.1f - administrator review needed)",
                                assignedTeacher.getName(), newWorkload, unit, optimalLimit);
                        result.addWarning(warningMsg);
                    }

                    log.debug("Assigned {} to course {} (subject: {}) - Teacher now has {} {}",
                        assignedTeacher.getName(), course.getCourseName(), course.getSubject(), newWorkload, unit);
                } else {
                    result.incrementFailed();
                    // Check if no teachers exist vs. teachers at capacity
                    long certifiedTeachersCount = allTeachers.stream()
                        .filter(t -> isStrictlyCertifiedForCourse(t, course))
                        .count();

                    String errorMsg;
                    double maxLimit = getMaxWorkloadLimit();

                    if (certifiedTeachersCount == 0) {
                        errorMsg = String.format("❌ No certified teacher available for course %s (subject: %s). " +
                            "ADMINISTRATOR ACTION: Hire teacher certified in %s",
                            course.getCourseName(), course.getSubject(),
                            getSubjectFamilyName(course.getSubject()));
                    } else {
                        errorMsg = String.format("⚠️ Teacher shortage: All certified teachers for %s (subject: %s) are at capacity (%.1f %s max). " +
                            "ADMINISTRATOR ACTION: Hire additional %s teacher or manually override limit",
                            course.getCourseName(), course.getSubject(), maxLimit, unit,
                            getSubjectFamilyName(course.getSubject()));
                    }
                    result.addError(errorMsg);
                    log.warn("Could not assign course: {} (subject: {}) - Certified teachers: {}, All at capacity",
                        course.getCourseName(), course.getSubject(), certifiedTeachersCount);
                }
            }

            result.setEndTime(System.currentTimeMillis());
            result.setMessage(String.format("Assigned %d of %d courses. %d courses could not be assigned.",
                result.getCoursesAssigned(), result.getTotalCoursesProcessed(), result.getCoursesFailed()));

            // Log final workload distribution
            log.info("=== FINAL WORKLOAD DISTRIBUTION ===");
            for (Map.Entry<Teacher, Double> entry : workloadTracker.entrySet()) {
                log.info("Teacher {}: {} {}", entry.getKey().getName(), entry.getValue(), unit);
            }

            log.info("Smart teacher assignment completed: {} assigned, {} failed in {}ms",
                result.getCoursesAssigned(), result.getCoursesFailed(),
                result.getEndTime() - result.getStartTime());

        } catch (Exception e) {
            log.error("Error during smart teacher assignment", e);
            result.addError("System error: " + e.getMessage());
            result.setMessage("Assignment failed due to error: " + e.getMessage());
            result.setEndTime(System.currentTimeMillis());
        }

        return result;
    }

    /**
     * FIX #4: Group courses by sequence
     * Example: "English 1", "English 2" → same group
     */
    private Map<String, List<Course>> groupCourseSequences(List<Course> courses) {
        Map<String, List<Course>> sequences = new HashMap<>();

        for (Course course : courses) {
            String key = getSequenceKey(course);
            sequences.computeIfAbsent(key, k -> new ArrayList<>()).add(course);
        }

        return sequences;
    }

    /**
     * Get sequence key for a course (e.g., "English" for "English 1", "English 2")
     */
    private String getSequenceKey(Course course) {
        if (course.getSubject() == null || course.getCourseName() == null) {
            return course.getCourseName() != null ? course.getCourseName() : "Unknown";
        }

        String courseName = course.getCourseName().toLowerCase();

        // Remove level indicators to get base name
        String baseKey = courseName
            .replaceAll("\\s+[1-4]$", "")          // Remove " 1", " 2", etc.
            .replaceAll("\\s+i{1,3}$", "")         // Remove " I", " II", " III"
            .replaceAll("\\s+intro.*$", "")        // Remove " Intro"
            .replaceAll("\\s+advanced$", "")       // Remove " Advanced"
            .replaceAll("\\s+ap$", "")             // Remove " AP"
            .replaceAll("\\s+honors$", "")         // Remove " Honors"
            .trim();

        return course.getSubject() + ":" + baseKey;
    }

    /**
     * Find the best teacher for a course using intelligent scoring
     * FIX #1: Now uses workloadTracker instead of getCourseCount()
     * UPDATED December 14, 2025: Supports both COURSE_COUNT and CREDIT_HOURS modes
     *
     * @param course The course to assign
     * @param availableTeachers List of available teachers
     * @param workloadTracker In-memory workload tracking (Double to support both modes)
     * @return Best matching teacher, or null if none suitable
     */
    private Teacher findBestTeacher(Course course, List<Teacher> availableTeachers,
                                   Map<Teacher, Double> workloadTracker) {
        // FIX #1: Filter using workloadTracker instead of getCourseCount()
        double maxLimit = getMaxWorkloadLimit();

        // DEBUG: Log all teachers and their workloads
        String unit = WORKLOAD_MODE == WorkloadLimitMode.CREDIT_HOURS ? "credits" : "courses";
        log.info("🔎 Finding teacher for course: {} (subject: {})", course.getCourseName(), course.getSubject());
        log.info("📊 Max limit: {} {}", maxLimit, unit);

        List<Teacher> suitableTeachers = availableTeachers.stream()
            .filter(t -> {
                Double workload = workloadTracker.get(t);

                // ✅ PRIORITY 2 FIX December 15, 2025: Account for planning period
                // If teacher has a planning period, they have one less available teaching period
                // Example: 7 periods/day - 1 planning - 1 lunch = 6 teaching periods MAX (not 7!)
                double effectiveMaxLimit = maxLimit;
                if (WORKLOAD_MODE == WorkloadLimitMode.TEACHING_PERIODS && t.getPlanningPeriod() != null) {
                    // Teacher has planning period reserved, reduce available periods
                    // Standard: 7 total periods - 1 planning = 6 teaching periods max
                    log.debug("  Teacher {} has planning period {}, effective limit reduced",
                        t.getName(), t.getPlanningPeriod());
                }

                boolean underLimit = workload != null && workload < effectiveMaxLimit;
                log.info("  👤 {} - Workload: {} {} - Effective limit: {} - Under limit? {}",
                    t.getName(),
                    workload != null ? workload : "NULL",
                    unit,
                    effectiveMaxLimit,
                    underLimit);
                return underLimit;
            })
            .collect(Collectors.toList());

        if (suitableTeachers.isEmpty()) {
            log.warn("❌ All teachers at maximum workload ({} {})", maxLimit, unit);
            return null;
        }
        log.info("✅ Found {} suitable teachers (under {} {} limit)", suitableTeachers.size(), maxLimit, unit);

        // FIX #2: Only consider strictly certified teachers
        List<Teacher> certifiedTeachers = suitableTeachers.stream()
            .filter(t -> isStrictlyCertifiedForCourse(t, course))
            .collect(Collectors.toList());

        if (certifiedTeachers.isEmpty()) {
            log.warn("No certified teachers available for course: {} (subject: {})",
                course.getCourseName(), course.getSubject());
            return null;  // STRICT: No fallback to non-certified teachers
        }

        // Score each certified teacher
        Map<Teacher, Double> scores = new HashMap<>();

        for (Teacher teacher : certifiedTeachers) {
            double score = calculateTeacherScore(teacher, course, workloadTracker);
            scores.put(teacher, score);
        }

        // Find teacher with highest score
        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }

    /**
     * Calculate suitability score for a teacher-course pairing
     * FIX #1: Uses workloadTracker instead of getCourseCount()
     * UPDATED December 14, 2025: Supports both COURSE_COUNT and CREDIT_HOURS modes
     *
     * Scoring factors:
     * - Exact certification match: +100 points
     * - Workload balance: 0-50 points (prefer less loaded teachers)
     * - Subject family match: +75 points (e.g., Biology teacher for Chemistry)
     *
     * @param teacher The teacher to evaluate
     * @param course The course to assign
     * @param workloadTracker In-memory workload tracking (Double to support both modes)
     * @return Suitability score (higher is better)
     */
    private double calculateTeacherScore(Teacher teacher, Course course,
                                        Map<Teacher, Double> workloadTracker) {
        double score = 0.0;

        // Factor 1: Certification match (most important)
        if (hasExactCertificationMatch(teacher, course)) {
            score += 100.0;
            log.trace("Teacher {} has exact certification for {}: +100 points",
                teacher.getName(), course.getSubject());
        } else if (isInSameSubjectFamily(teacher, course)) {
            score += 75.0;  // Same family but not exact (Biology teacher → Chemistry)
            log.trace("Teacher {} is in same subject family for {}: +75 points",
                teacher.getName(), course.getSubject());
        }

        // Factor 2: Workload balance (FIX #1: Use workloadTracker, supports both modes)
        double currentWorkload = workloadTracker.get(teacher);
        double workloadScore = calculateWorkloadScore(currentWorkload);
        score += workloadScore;
        String unit = WORKLOAD_MODE == WorkloadLimitMode.CREDIT_HOURS ? "credits" : "courses";
        log.trace("Teacher {} workload score ({} {}): +{} points",
            teacher.getName(), currentWorkload, unit, workloadScore);

        log.trace("Teacher {} total score for course {}: {}",
            teacher.getName(), course.getCourseName(), score);

        return score;
    }

    /**
     * UPDATED December 14, 2025: Calculate workload score with 2-3 limit
     * User requirement: "teacher should not be assigned more than 2 courses in some cases 3 max"
     *
     * Supports both COURSE_COUNT and CREDIT_HOURS modes:
     * - COURSE_COUNT: 0=50pts, 1=45pts, 2=20pts, 3+=0pts
     * - CREDIT_HOURS: 0.0=50pts, <optimal=45pts, optimal=20pts, >max=0pts
     *
     * @param currentWorkload Current teacher workload (courses or credits)
     * @return Score from 0-50 points (higher = less loaded)
     */
    private double calculateWorkloadScore(double currentWorkload) {
        double optimalLimit = getOptimalWorkloadLimit();
        double maxLimit = getMaxWorkloadLimit();

        if (WORKLOAD_MODE == WorkloadLimitMode.COURSE_COUNT) {
            // Course count mode (simple integer comparison)
            if (currentWorkload == 0) return 50.0;  // Prioritize teachers with no courses
            if (currentWorkload == 1) return 45.0;  // Teacher has light load
            if (currentWorkload == 2) return 20.0;  // At optimal limit - use only if necessary
            return 0.0;  // At or over hard limit - DO NOT ASSIGN
        } else {
            // Credit hours mode (floating point comparison)
            if (currentWorkload < 0.1) return 50.0;  // No courses
            if (currentWorkload < optimalLimit) return 45.0;  // Below optimal
            if (currentWorkload <= optimalLimit) return 20.0;  // At optimal - use only if necessary
            if (currentWorkload < maxLimit) return 5.0;  // Between optimal and max - avoid if possible
            return 0.0;  // At or over max - DO NOT ASSIGN
        }
    }

    /**
     * FIX #2: STRICT certification checking
     * Only returns true for exact match or same subject family
     * NO FALLBACKS - prevents Science teacher → Gymnastics
     */
    private boolean isStrictlyCertifiedForCourse(Teacher teacher, Course course) {
        if (course.getSubject() == null || teacher.getCertifiedSubjects() == null) {
            return false;
        }

        return hasExactCertificationMatch(teacher, course) ||
               isInSameSubjectFamily(teacher, course);
    }

    /**
     * Check for exact certification match
     */
    private boolean hasExactCertificationMatch(Teacher teacher, Course course) {
        if (course.getSubject() == null || teacher.getCertifiedSubjects() == null) {
            return false;
        }

        List<String> certifications = teacher.getCertifiedSubjects();

        // Exact match (case-insensitive)
        return certifications.stream()
            .anyMatch(cert -> cert.equalsIgnoreCase(course.getSubject()));
    }

    /**
     * FIX #2: Check if teacher and course are in same subject family
     * Uses STRICT subject families - only related subjects match
     * FIXED December 14, 2025: Use whole-word matching to prevent "literature" matching "art"
     */
    private boolean isInSameSubjectFamily(Teacher teacher, Course course) {
        if (course.getSubject() == null || teacher.getCertifiedSubjects() == null) {
            return false;
        }

        String courseSubject = course.getSubject().toLowerCase();
        List<String> certifications = teacher.getCertifiedSubjects();

        for (String cert : certifications) {
            String certLower = cert.toLowerCase();

            // Science family (Biology, Chemistry, Physics, Earth Science, General Science)
            Set<String> scienceFamily = Set.of("science", "biology", "chemistry", "physics",
                "earth science", "life science", "physical science");
            if (matchesFamily(certLower, scienceFamily) && matchesFamily(courseSubject, scienceFamily)) {
                log.trace("Matched in science family: {} → {}", cert, course.getSubject());
                return true;
            }

            // Math family (Math, Algebra, Geometry, Calculus, Trigonometry, Pre-Calculus)
            Set<String> mathFamily = Set.of("math", "algebra", "geometry", "calculus",
                "trigonometry", "pre-calculus", "pre-algebra");
            if (matchesFamily(certLower, mathFamily) && matchesFamily(courseSubject, mathFamily)) {
                log.trace("Matched in math family: {} → {}", cert, course.getSubject());
                return true;
            }

            // English family (English, Literature, Language Arts, Writing, Reading)
            // NOTE: "literature" should NOT match "art" - use whole-word matching
            Set<String> englishFamily = Set.of("english", "literature", "language arts",
                "writing", "reading", "composition");
            if (matchesFamily(certLower, englishFamily) && matchesFamily(courseSubject, englishFamily)) {
                log.trace("Matched in english family: {} → {}", cert, course.getSubject());
                return true;
            }

            // Social Studies family (History, Geography, Civics, Government, Economics)
            Set<String> socialStudiesFamily = Set.of("history", "geography", "civics",
                "government", "economics", "social studies", "world history", "us history",
                "american history");
            if (matchesFamily(certLower, socialStudiesFamily) && matchesFamily(courseSubject, socialStudiesFamily)) {
                log.trace("Matched in social studies family: {} → {}", cert, course.getSubject());
                return true;
            }

            // Physical Education family (PE, Physical Education, Health, Athletics)
            Set<String> peFamily = Set.of("physical education", "pe", "health", "athletics",
                "fitness", "gym", "gymnastics");
            if (matchesFamily(certLower, peFamily) && matchesFamily(courseSubject, peFamily)) {
                log.trace("Matched in PE family: {} → {}", cert, course.getSubject());
                return true;
            }

            // Arts family (Art, Music, Drama, Theater, Band, Chorus, Orchestra)
            // NOTE: Must match whole words to prevent "literature" from matching "art"
            Set<String> artsFamily = Set.of("art", "music", "drama", "theater", "theatre",
                "band", "chorus", "orchestra", "choir", "painting", "drawing", "visual art");
            if (matchesFamily(certLower, artsFamily) && matchesFamily(courseSubject, artsFamily)) {
                log.trace("Matched in arts family: {} → {}", cert, course.getSubject());
                return true;
            }

            // Foreign Language family (Spanish, French, German, Latin, etc.)
            Set<String> languageFamily = Set.of("spanish", "french", "german", "latin",
                "chinese", "japanese", "italian", "foreign language");
            if (matchesFamily(certLower, languageFamily) && matchesFamily(courseSubject, languageFamily)) {
                log.trace("Matched in foreign language family: {} → {}", cert, course.getSubject());
                return true;
            }

            // Computer Science family
            Set<String> csFamily = Set.of("computer", "programming", "coding", "technology",
                "it", "information technology");
            if (matchesFamily(certLower, csFamily) && matchesFamily(courseSubject, csFamily)) {
                log.trace("Matched in CS family: {} → {}", cert, course.getSubject());
                return true;
            }
        }

        return false;  // No family match
    }

    /**
     * Check if a subject string matches any keyword in a family using whole-word matching
     * This prevents "literature" from matching "art" (substring problem)
     *
     * @param subject The subject string (teacher cert or course subject)
     * @param family Set of keywords for this subject family
     * @return true if subject contains any whole-word match from family
     */
    private boolean matchesFamily(String subject, Set<String> family) {
        // For each keyword in the family, check if it appears as a whole word in the subject
        for (String keyword : family) {
            // Use word boundary matching: keyword must be surrounded by non-word characters or string boundaries
            // Example: "art" matches "art", "visual art", but NOT "literature"
            String regex = "\\b" + java.util.regex.Pattern.quote(keyword) + "\\b";

            // ✅ PRIORITY 3 FIX December 15, 2025: Use pattern cache for performance
            // Reuse compiled patterns instead of recompiling every time
            java.util.regex.Pattern pattern = PATTERN_CACHE.computeIfAbsent(regex,
                r -> java.util.regex.Pattern.compile(r, java.util.regex.Pattern.CASE_INSENSITIVE));

            if (pattern.matcher(subject).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get human-readable subject family name for error messages
     */
    private String getSubjectFamilyName(String subject) {
        if (subject == null) return "Unknown";

        String subjectLower = subject.toLowerCase();

        if (subjectLower.contains("science") || subjectLower.contains("biology") ||
            subjectLower.contains("chemistry") || subjectLower.contains("physics")) {
            return "Science (Biology, Chemistry, Physics, or General Science)";
        }
        if (subjectLower.contains("math") || subjectLower.contains("algebra") ||
            subjectLower.contains("geometry") || subjectLower.contains("calculus")) {
            return "Math (Algebra, Geometry, Calculus, or General Math)";
        }
        if (subjectLower.contains("english") || subjectLower.contains("literature") ||
            subjectLower.contains("language arts")) {
            return "English (English, Literature, or Language Arts)";
        }
        if (subjectLower.contains("history") || subjectLower.contains("civics") ||
            subjectLower.contains("government") || subjectLower.contains("social")) {
            return "Social Studies (History, Civics, Government, or Economics)";
        }
        if (subjectLower.contains("physical education") || subjectLower.contains("pe") ||
            subjectLower.contains("gym") || subjectLower.contains("health")) {
            return "Physical Education (PE, Health, or Athletics)";
        }

        return subject;
    }

    /**
     * Preview what would happen without actually making assignments
     *
     * @return AssignmentResult with preview statistics
     */
    public AssignmentResult previewTeacherAssignments() {
        log.info("Previewing smart teacher assignment...");

        AssignmentResult result = new AssignmentResult();
        result.setStartTime(System.currentTimeMillis());

        // NULL SAFETY: Handle null course list from repository
        List<Course> allCourses = sisDataService.getAllCourses();
        if (allCourses == null) {
            log.warn("Course repository returned null");
            result.setMessage("Unable to load courses");
            result.setEndTime(System.currentTimeMillis());
            return result;
        }

        List<Course> unassignedCourses = allCourses.stream()
            .filter(c -> c.getTeacher() == null)
            .collect(Collectors.toList());

        result.setTotalCoursesProcessed(unassignedCourses.size());

        if (unassignedCourses.isEmpty()) {
            result.setMessage("All courses already have teachers assigned");
            result.setEndTime(System.currentTimeMillis());
            return result;
        }

        // NULL SAFETY: Handle null teacher list from repository
        List<Teacher> allTeachers = sisDataService.getAllTeachers();
        if (allTeachers == null) {
            log.warn("Teacher repository returned null");
            result.setMessage("Unable to load teachers");
            result.setEndTime(System.currentTimeMillis());
            return result;
        }

        if (allTeachers.isEmpty()) {
            result.setMessage("No teachers available for assignment");
            result.setEndTime(System.currentTimeMillis());
            return result;
        }

        // Use workload tracker for preview too (supports both modes)
        Map<Teacher, Double> workloadTracker = new HashMap<>();
        for (Teacher teacher : allTeachers) {
            double initialWorkload = calculateTeacherWorkload(teacher);
            workloadTracker.put(teacher, initialWorkload);
        }

        for (Course course : unassignedCourses) {
            Teacher bestTeacher = findBestTeacher(course, allTeachers, workloadTracker);
            if (bestTeacher != null) {
                result.incrementAssigned();
                double courseWorkload = getCourseWorkload(course);
                workloadTracker.put(bestTeacher, workloadTracker.get(bestTeacher) + courseWorkload);
            } else {
                result.incrementFailed();
            }
        }

        result.setEndTime(System.currentTimeMillis());
        result.setMessage(String.format("Preview: Would assign %d of %d courses",
            result.getCoursesAssigned(), result.getTotalCoursesProcessed()));

        return result;
    }

    // ========================================================================
    // HELPER METHODS - Workload Calculation (Supports Both Modes)
    // ========================================================================

    /**
     * Calculate current workload for a teacher
     * Returns course count or credit hours depending on WORKLOAD_MODE
     */
    private double calculateTeacherWorkload(Teacher teacher) {
        if (teacher.getCourses() == null || teacher.getCourses().isEmpty()) {
            return 0.0;
        }

        if (WORKLOAD_MODE == WorkloadLimitMode.COURSE_COUNT) {
            // Simple course count
            return teacher.getCourseCount();
        } else if (WORKLOAD_MODE == WorkloadLimitMode.CREDIT_HOURS) {
            // Sum of credit hours for all assigned courses
            return teacher.getCourses().stream()
                .mapToDouble(c -> c.getCredits() != null ? c.getCredits() : 1.0)
                .sum();
        } else {
            // TEACHING_PERIODS: Count total teaching periods (sessionsPerWeek) for all courses
            // Example: Teacher has 2 courses:
            //   - English 1 (sessionsPerWeek=3) → 3 teaching periods
            //   - English 2 (sessionsPerWeek=2) → 2 teaching periods
            // Total workload = 5 teaching periods
            return teacher.getCourses().stream()
                .mapToDouble(c -> {
                    Integer sessions = c.getSessionsPerWeek();
                    return sessions != null ? sessions.doubleValue() : 1.0;
                })
                .sum();
        }
    }

    /**
     * Get workload value for a single course
     * Returns 1.0 for COURSE_COUNT mode, credit hours for CREDIT_HOURS mode,
     * or teaching periods (sessionsPerWeek) for TEACHING_PERIODS mode
     */
    private double getCourseWorkload(Course course) {
        if (WORKLOAD_MODE == WorkloadLimitMode.COURSE_COUNT) {
            return 1.0;  // Each course counts as 1
        } else if (WORKLOAD_MODE == WorkloadLimitMode.CREDIT_HOURS) {
            return course.getCredits() != null ? course.getCredits() : 1.0;
        } else {
            // TEACHING_PERIODS: Return number of teaching periods for this course
            Integer sessions = course.getSessionsPerWeek();
            return sessions != null ? sessions.doubleValue() : 1.0;
        }
    }

    /**
     * Get maximum workload limit based on current mode
     */
    private double getMaxWorkloadLimit() {
        if (WORKLOAD_MODE == WorkloadLimitMode.COURSE_COUNT) {
            return MAX_WORKLOAD_COURSES;
        } else if (WORKLOAD_MODE == WorkloadLimitMode.CREDIT_HOURS) {
            return MAX_WORKLOAD_CREDITS;
        } else {
            return MAX_WORKLOAD_PERIODS;
        }
    }

    /**
     * Get optimal workload limit based on current mode
     */
    private double getOptimalWorkloadLimit() {
        if (WORKLOAD_MODE == WorkloadLimitMode.COURSE_COUNT) {
            return OPTIMAL_WORKLOAD_COURSES;
        } else if (WORKLOAD_MODE == WorkloadLimitMode.CREDIT_HOURS) {
            return OPTIMAL_WORKLOAD_CREDITS;
        } else {
            return OPTIMAL_WORKLOAD_PERIODS;
        }
    }

    /**
     * Get warning workload limit based on current mode
     */
    private double getWarningWorkloadLimit() {
        if (WORKLOAD_MODE == WorkloadLimitMode.COURSE_COUNT) {
            return WARNING_WORKLOAD_COURSES;
        } else if (WORKLOAD_MODE == WorkloadLimitMode.CREDIT_HOURS) {
            return WARNING_WORKLOAD_CREDITS;
        } else {
            return WARNING_WORKLOAD_PERIODS;
        }
    }

    // ========================================================================
    // ASSIGNMENT RESULT CLASS
    // ========================================================================

    /**
     * Assignment result class for tracking outcomes
     */
    public static class AssignmentResult {
        private int totalCoursesProcessed;
        private int coursesAssigned;
        private int coursesFailed;
        private String message;
        private List<String> warnings = new ArrayList<>();
        private List<String> errors = new ArrayList<>();
        private long startTime;
        private long endTime;

        // Getters and setters
        public int getTotalCoursesProcessed() { return totalCoursesProcessed; }
        public void setTotalCoursesProcessed(int total) { this.totalCoursesProcessed = total; }

        public int getCoursesAssigned() { return coursesAssigned; }
        public void incrementAssigned() { this.coursesAssigned++; }

        public int getCoursesFailed() { return coursesFailed; }
        public void incrementFailed() { this.coursesFailed++; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public List<String> getWarnings() { return warnings; }
        public void addWarning(String warning) { this.warnings.add(warning); }

        public List<String> getErrors() { return errors; }
        public void addError(String error) { this.errors.add(error); }

        public long getStartTime() { return startTime; }
        public void setStartTime(long startTime) { this.startTime = startTime; }

        public long getEndTime() { return endTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }

        public long getDurationMs() { return endTime - startTime; }

        public boolean isSuccessful() { return coursesFailed == 0; }
        public boolean hasWarnings() { return !warnings.isEmpty(); }
        public boolean hasErrors() { return !errors.isEmpty(); }
    }
}
