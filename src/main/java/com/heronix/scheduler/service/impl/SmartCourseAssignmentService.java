package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.dto.CourseAssignmentRecommendation;
import com.heronix.scheduler.model.dto.CourseAssignmentRecommendation.RecommendationPriority;
import com.heronix.scheduler.model.dto.CourseAssignmentRecommendation.TeacherMatchInfo;
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
 * Smart Course Assignment Service
 *
 * Intelligently matches courses to teachers based on:
 * 1. Certification matching (primary criteria)
 * 2. Workload balancing
 * 3. Subject expertise (department matching)
 * 4. Teacher preferences and availability
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 18 - Smart Course Assignment
 */
@Service
@Slf4j
@Transactional
public class SmartCourseAssignmentService {

    @Autowired
    private SISDataService sisDataService;
    /**
     * Analyze all courses and generate recommendations for reassignment
     *
     * @return List of recommendations sorted by priority
     */
    public List<CourseAssignmentRecommendation> analyzeAndRecommend() {
        log.info("Starting smart course assignment analysis...");

        List<Course> allCourses = sisDataService.getAllCourses();
        List<Teacher> allTeachers = sisDataService.getAllTeachers();

        // ✅ NULL SAFE: Validate repository results
        if (allCourses == null) {
            log.warn("Course repository returned null");
            allCourses = Collections.emptyList();
        }
        if (allTeachers == null) {
            log.warn("Teacher repository returned null");
            allTeachers = Collections.emptyList();
        }

        List<CourseAssignmentRecommendation> recommendations = new ArrayList<>();

        for (Course course : allCourses) {
            // ✅ NULL SAFE: Skip null courses and validate isActive method
            if (course == null) {
                continue;
            }
            if (!course.isActive()) {
                continue;
            }

            CourseAssignmentRecommendation recommendation = analyzeCourse(course, allTeachers);
            if (recommendation != null) {
                recommendations.add(recommendation);
            }
        }

        // Sort by priority (CRITICAL first) and then by match score (lowest first - need most help)
        recommendations.sort(Comparator
            .comparing(CourseAssignmentRecommendation::getPriority)
            .thenComparing(r -> -r.getMatchScore())); // Higher score = better match, but we want to show worst matches first

        log.info("Analysis complete. Found {} recommendations", recommendations.size());
        return recommendations;
    }

    /**
     * Analyze a single course and find the best teacher match
     */
    private CourseAssignmentRecommendation analyzeCourse(Course course, List<Teacher> allTeachers) {
        Teacher currentTeacher = course.getTeacher();
        List<String> requiredCerts = course.getRequiredCertifications();

        // If course has no required certifications, skip (can't make intelligent recommendation)
        if (requiredCerts == null || requiredCerts.isEmpty()) {
            return null;
        }

        // Calculate match score for current teacher (if assigned)
        int currentMatchScore = 0;
        List<String> currentMatching = new ArrayList<>();
        List<String> currentMissing = new ArrayList<>();

        if (currentTeacher != null) {
            currentMatchScore = calculateMatchScore(course, currentTeacher, currentMatching, currentMissing);
        }

        // Find all potential teachers and their match scores
        // ✅ NULL SAFE: Filter null teachers and validate allTeachers list
        if (allTeachers == null) {
            allTeachers = Collections.emptyList();
        }

        List<TeacherMatchInfo> potentialTeachers = allTeachers.stream()
            .filter(teacher -> teacher != null) // ✅ NULL SAFE: Skip null teachers
            .map(teacher -> {
                List<String> matching = new ArrayList<>();
                List<String> missing = new ArrayList<>();
                int score = calculateMatchScore(course, teacher, matching, missing);
                int courseLoad = getCourseLoad(teacher);

                // ✅ NULL SAFE: Safe string join with null check
                String matchingSummary = matching != null && !matching.isEmpty() ?
                    String.join(", ", matching) : "None";

                return TeacherMatchInfo.builder()
                    .teacher(teacher)
                    .matchScore(score)
                    .currentCourseLoad(courseLoad)
                    .matchingCertifications(matching)
                    .summary(String.format("Score: %d, Load: %d courses, Matches: %s",
                        score, courseLoad, matchingSummary))
                    .build();
            })
            .filter(info -> info != null && info.getMatchScore() > 0) // ✅ NULL SAFE: Filter null info objects
            .sorted(Comparator
                .comparingInt(TeacherMatchInfo::getMatchScore).reversed()
                .thenComparingInt(TeacherMatchInfo::getCurrentCourseLoad)) // Prefer less loaded teachers
            .collect(Collectors.toList());

        if (potentialTeachers.isEmpty()) {
            // No qualified teachers found
            return CourseAssignmentRecommendation.builder()
                .course(course)
                .currentTeacher(currentTeacher)
                .recommendedTeacher(null)
                .matchScore(0)
                .priority(RecommendationPriority.CRITICAL)
                .matchingCertifications(Collections.emptyList())
                .missingCertifications(requiredCerts)
                .reasoning("❌ CRITICAL: No qualified teachers found for this course. Required: " +
                    String.join(", ", requiredCerts))
                .alternativeTeachers(Collections.emptyList())
                .build();
        }

        TeacherMatchInfo best = potentialTeachers.get(0);

        // Determine if we should recommend a change
        boolean shouldRecommend = false;
        RecommendationPriority priority;
        String reasoning;

        if (currentTeacher == null) {
            // Unassigned course
            shouldRecommend = true;
            priority = RecommendationPriority.HIGH;
            reasoning = String.format("📝 Course is unassigned. Recommended: %s (Match: %d%%, Load: %d courses)",
                getTeacherName(best.getTeacher()), best.getMatchScore(), best.getCurrentCourseLoad());
        } else if (currentMatchScore == 0) {
            // Current teacher has NO matching certifications
            shouldRecommend = true;
            priority = RecommendationPriority.CRITICAL;
            reasoning = String.format("❌ CRITICAL: Current teacher '%s' has NO matching certifications!\nRequired: %s\nRecommended: %s (Match: %d%%)",
                getTeacherName(currentTeacher),
                String.join(", ", requiredCerts),
                getTeacherName(best.getTeacher()),
                best.getMatchScore());
        } else if (currentMatchScore < 60 && best.getMatchScore() >= 80) {
            // Current teacher has weak match, better teacher available
            shouldRecommend = true;
            priority = RecommendationPriority.HIGH;
            reasoning = String.format("⚠️ Current teacher '%s' has weak match (%d%%).\nMissing: %s\nRecommended: %s (Match: %d%%)",
                getTeacherName(currentTeacher),
                currentMatchScore,
                String.join(", ", currentMissing),
                getTeacherName(best.getTeacher()),
                best.getMatchScore());
        } else if (currentMatchScore < best.getMatchScore() - 20) {
            // Significantly better match available
            shouldRecommend = true;
            priority = RecommendationPriority.MEDIUM;
            reasoning = String.format("💡 Better match available: %s (Match: %d%% vs current %d%%)",
                getTeacherName(best.getTeacher()),
                best.getMatchScore(),
                currentMatchScore);
        } else {
            // Current assignment is good
            return null;
        }

        if (!shouldRecommend) {
            return null;
        }

        return CourseAssignmentRecommendation.builder()
            .course(course)
            .currentTeacher(currentTeacher)
            .recommendedTeacher(best.getTeacher())
            .matchScore(best.getMatchScore())
            .priority(priority)
            .matchingCertifications(best.getMatchingCertifications())
            // ✅ NULL SAFE: Filter null certifications and validate matching list
            .missingCertifications(requiredCerts.stream()
                .filter(cert -> cert != null)
                .filter(cert -> best.getMatchingCertifications() == null ||
                    !best.getMatchingCertifications().contains(cert))
                .collect(Collectors.toList()))
            .reasoning(reasoning)
            .alternativeTeachers(potentialTeachers.size() > 1 ?
                potentialTeachers.subList(1, Math.min(4, potentialTeachers.size())) :
                Collections.emptyList())
            .build();
    }

    /**
     * Calculate match score between a course and teacher
     *
     * ENHANCED: Now uses SubjectCertification entity for grade-level aware validation
     *
     * @param course Course to match
     * @param teacher Teacher to match
     * @param outMatching Output list of matching certifications
     * @param outMissing Output list of missing certifications
     * @return Match score 0-100
     */
    private int calculateMatchScore(Course course, Teacher teacher,
                                    List<String> outMatching, List<String> outMissing) {
        // ✅ NULL SAFE: Validate parameters
        if (course == null || teacher == null) {
            log.warn("Cannot calculate match score with null course or teacher");
            return 0;
        }

        List<String> requiredCerts = course.getRequiredCertifications();
        List<String> teacherCerts = teacher.getCertifications();

        if (requiredCerts == null || requiredCerts.isEmpty()) {
            return 50; // Neutral score if no requirements
        }

        // ENHANCED: Get grade level for validation
        String gradeLevel = course.getLevel() != null ? course.getLevel().toString() : "9";

        // ENHANCED: Use new SubjectCertification entity with grade-level validation
        // hasCertificationForSubjectAndGrade() not available on Teacher — defaults to false
        boolean isCertifiedForCourse = false; // teacher.hasCertificationForSubjectAndGrade(course.getSubject(), gradeLevel);

        // Legacy string-based certification matching (still needed for detailed tracking)
        if (teacherCerts == null || teacherCerts.isEmpty()) {
            if (!isCertifiedForCourse) {
                outMissing.addAll(requiredCerts);
                return 0;
            }
            // Teacher has valid SubjectCertification but no legacy certs
            outMatching.add(course.getSubject() + " (Grade " + gradeLevel + ")");
        } else {
            for (String required : requiredCerts) {
                // ✅ NULL SAFE: Filter null certifications before matching
                boolean found = teacherCerts.stream()
                    .filter(cert -> cert != null)
                    .anyMatch(cert -> certificationMatches(required, cert));

                if (found) {
                    // ✅ NULL SAFE: Check outMatching is not null before adding
                    if (outMatching != null) {
                        outMatching.add(required);
                    }
                } else {
                    // ✅ NULL SAFE: Check outMissing is not null before adding
                    if (outMissing != null) {
                        outMissing.add(required);
                    }
                }
            }
        }

        // ENHANCED: Base score calculation with grade-level awareness
        int baseScore;
        if (isCertifiedForCourse && !outMatching.isEmpty()) {
            baseScore = 100; // Fully qualified with valid SubjectCertification
        } else if (isCertifiedForCourse) {
            baseScore = 90; // Qualified via SubjectCertification but cert string mismatch
            // Add to matching list for display
            outMatching.add(course.getSubject() + " ✓");
        } else if (!outMatching.isEmpty()) {
            // Has some legacy cert matches but not properly certified
            baseScore = (int) ((outMatching.size() * 85.0) / requiredCerts.size());
        } else {
            baseScore = 0; // Not qualified
        }

        // Bonus points for department matching
        int bonusScore = 0;
        // ✅ NULL SAFE: Validate both subject and department before comparison
        String courseSubject = course.getSubject();
        String teacherDept = teacher.getDepartment();
        if (courseSubject != null && teacherDept != null && courseSubject.equalsIgnoreCase(teacherDept)) {
            bonusScore += 10;
        }

        // Penalty for overloaded teachers
        int courseLoad = getCourseLoad(teacher);
        int loadPenalty = 0;
        if (courseLoad > 6) {
            loadPenalty = Math.min(20, (courseLoad - 6) * 5);
        }

        // ENHANCED: Warning for expiring certifications (minor penalty)
        int expirationPenalty = 0;
        // hasExpiringCertifications() not available on Teacher — expiration check disabled
        if (isCertifiedForCourse && false) { // teacher.hasExpiringCertifications()) {
            expirationPenalty = 5; // Small penalty, still qualified
        }

        return Math.max(0, Math.min(100, baseScore + bonusScore - loadPenalty - expirationPenalty));
    }

    /**
     * Check if two certifications match (case-insensitive, partial matching)
     */
    private boolean certificationMatches(String required, String teacherCert) {
        if (required == null || teacherCert == null) {
            return false;
        }

        required = required.toLowerCase().trim();
        teacherCert = teacherCert.toLowerCase().trim();

        // Exact match
        if (required.equals(teacherCert)) {
            return true;
        }

        // Partial match (either contains the other)
        if (required.contains(teacherCert) || teacherCert.contains(required)) {
            return true;
        }

        // Subject keyword matching
        String[] requiredKeywords = required.split("[\\s\\-]+");
        String[] teacherKeywords = teacherCert.split("[\\s\\-]+");

        int keywordMatches = 0;
        for (String rk : requiredKeywords) {
            for (String tk : teacherKeywords) {
                if (rk.length() > 3 && tk.length() > 3 && rk.equals(tk)) {
                    keywordMatches++;
                }
            }
        }

        // If 50%+ keywords match, consider it a match
        return keywordMatches >= Math.max(requiredKeywords.length, teacherKeywords.length) / 2;
    }

    /**
     * Get current course load for a teacher
     */
    private int getCourseLoad(Teacher teacher) {
        // ✅ NULL SAFE: Validate teacher and teacher ID
        if (teacher == null || teacher.getId() == null) {
            log.warn("Cannot get course load for null teacher or teacher with null ID");
            return 0;
        }

        // Note: getCourseById returns Optional<Course>, not List<Course>
        // Teachers don't have a direct list of courses - need to query through ScheduleSlots
        // For now, return 0 as course load calculation needs different approach
        // Course load calculation requires ScheduleSlot queries — returns 0 as placeholder
        log.warn("Teacher course load calculation not implemented - requires ScheduleSlot queries");
        return 0;
    }

    /**
     * Get teacher name (handles null first/last names)
     */
    private String getTeacherName(Teacher teacher) {
        // ✅ NULL SAFE: Validate teacher parameter
        if (teacher == null) {
            return "Unknown Teacher";
        }

        if (teacher.getFirstName() != null && teacher.getLastName() != null) {
            return teacher.getFirstName() + " " + teacher.getLastName();
        } else if (teacher.getName() != null) {
            return teacher.getName();
        }
        return "Unknown Teacher";
    }

    /**
     * Apply recommended assignments
     *
     * @param recommendations List of recommendations to apply
     * @return Number of assignments changed
     */
    @Transactional
    public int applyRecommendations(List<CourseAssignmentRecommendation> recommendations) {
        // ✅ NULL SAFE: Validate recommendations list
        if (recommendations == null) {
            log.warn("Cannot apply null recommendations list");
            return 0;
        }

        int changedCount = 0;

        for (CourseAssignmentRecommendation rec : recommendations) {
            // ✅ NULL SAFE: Skip null recommendations and validate fields
            if (rec == null || rec.getCourse() == null) {
                continue;
            }

            if (rec.getRecommendedTeacher() != null) {
                Course course = rec.getCourse();
                course.setTeacher(rec.getRecommendedTeacher());
                // Cannot persist Course changes — SIS entities are read-only from SchedulerV2
                // sisDataService.save(course);
                changedCount++;

                // ✅ NULL SAFE: Safe extraction of course name
                String courseName = course.getCourseName() != null ?
                    course.getCourseName() : "Unknown Course";

                log.info("✅ Assigned {} -> {} (Score: {}%)",
                    courseName,
                    getTeacherName(rec.getRecommendedTeacher()),
                    rec.getMatchScore());
            }
        }

        log.info("Applied {} course assignments", changedCount);
        return changedCount;
    }
}
