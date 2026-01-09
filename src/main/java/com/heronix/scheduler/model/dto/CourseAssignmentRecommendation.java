package com.heronix.scheduler.model.dto;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Teacher;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Recommendation for assigning a course to a teacher
 * Includes match score and reasoning
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAssignmentRecommendation {

    private Course course;
    private Teacher currentTeacher; // May be null if unassigned
    private Teacher recommendedTeacher;

    /**
     * Match score: 0-100
     * - 100: Perfect match (all certifications match, workload optimal)
     * - 80-99: Good match (most certifications match)
     * - 60-79: Acceptable match (some certifications match)
     * - 40-59: Weak match (minimal certifications)
     * - 0-39: Poor match (no certification match)
     */
    private int matchScore;

    /**
     * Priority level for this recommendation
     * CRITICAL: Current teacher has no matching certifications
     * HIGH: Better match available with proper certifications
     * MEDIUM: Workload balancing improvement
     * LOW: Minor optimization
     */
    private RecommendationPriority priority;

    /**
     * List of matching certifications between course and teacher
     */
    @Builder.Default
    private List<String> matchingCertifications = new ArrayList<>();

    /**
     * List of missing certifications
     */
    @Builder.Default
    private List<String> missingCertifications = new ArrayList<>();

    /**
     * Human-readable reasoning for this recommendation
     */
    private String reasoning;

    /**
     * Alternative teachers who could also teach this course (sorted by score)
     */
    @Builder.Default
    private List<TeacherMatchInfo> alternativeTeachers = new ArrayList<>();

    public enum RecommendationPriority {
        CRITICAL, // Must fix - no certifications
        HIGH,     // Should fix - significant mismatch
        MEDIUM,   // Could fix - minor improvements
        LOW       // Optional - optimization only
    }

    /**
     * Info about a teacher's match with a course
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TeacherMatchInfo {
        private Teacher teacher;
        private int matchScore;
        private int currentCourseLoad;
        private List<String> matchingCertifications;
        private String summary;
    }
}
