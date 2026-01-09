package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.domain.Course;

import java.util.List;

/**
 * Teacher Assignment Service
 * Handles teacher-room assignments and multi-subject teaching certification management
 *
 * Location: src/main/java/com/eduscheduler/service/TeacherAssignmentService.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-11-10
 */
public interface TeacherAssignmentService {

    /**
     * Assign home room to teacher
     * This is the primary classroom where the teacher conducts most classes
     *
     * @param teacher The teacher to assign
     * @param room The room to assign as home room
     * @return Updated teacher entity
     */
    Teacher assignHomeRoom(Teacher teacher, Room room);

    /**
     * Get teacher's home room
     *
     * @param teacher The teacher
     * @return Teacher's designated home room, or null if not assigned
     */
    Room getHomeRoom(Teacher teacher);

    /**
     * Check if teacher is certified for subject
     * Checks both legacy certifications list and SubjectCertification entities
     *
     * @param teacher The teacher to check
     * @param subject The subject to check (e.g., "Mathematics", "Science")
     * @return true if teacher has valid certification for subject
     */
    boolean isCertifiedFor(Teacher teacher, String subject);

    /**
     * Add certification to teacher
     * Adds to legacy certifications list for backward compatibility
     *
     * @param teacher The teacher
     * @param subject The subject to certify (e.g., "Mathematics", "AP Calculus")
     * @return Updated teacher entity
     */
    Teacher addCertification(Teacher teacher, String subject);

    /**
     * Remove certification from teacher
     *
     * @param teacher The teacher
     * @param subject The subject to remove
     * @return Updated teacher entity
     */
    Teacher removeCertification(Teacher teacher, String subject);

    /**
     * Get all subjects a teacher is certified to teach
     *
     * @param teacher The teacher
     * @return List of certified subject names
     */
    List<String> getCertifiedSubjects(Teacher teacher);

    /**
     * Get all courses a teacher can teach (based on certifications and department)
     *
     * @param teacher The teacher
     * @return List of eligible courses
     */
    List<Course> getEligibleCourses(Teacher teacher);

    /**
     * Validate teacher can teach course
     * Checks certification, department match, and other eligibility criteria
     *
     * @param teacher The teacher
     * @param course The course
     * @return true if teacher can teach the course
     */
    boolean canTeachCourse(Teacher teacher, Course course);

    /**
     * Get teachers certified for a specific subject
     *
     * @param subject The subject (e.g., "Mathematics")
     * @return List of qualified teachers
     */
    List<Teacher> getTeachersCertifiedFor(String subject);

    /**
     * Update teacher's maximum periods per day
     * Used for block schedule vs standard schedule configuration
     *
     * @param teacher The teacher
     * @param maxPeriods Maximum periods (standard: 7, block: 4)
     * @return Updated teacher entity
     */
    Teacher updateMaxPeriodsPerDay(Teacher teacher, Integer maxPeriods);

    /**
     * Validate teacher assignment for a time slot
     * Checks workload, conflicts, and eligibility
     *
     * @param teacher The teacher
     * @param course The course to assign
     * @param periodNumber The period number
     * @return Validation result with any issues
     */
    TeacherAssignmentValidation validateAssignment(Teacher teacher, Course course, Integer periodNumber);

    /**
     * Teacher Assignment Validation Result
     */
    class TeacherAssignmentValidation {
        private boolean valid;
        private List<String> issues;
        private List<String> warnings;

        public TeacherAssignmentValidation(boolean valid) {
            this.valid = valid;
            this.issues = new java.util.ArrayList<>();
            this.warnings = new java.util.ArrayList<>();
        }

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public List<String> getIssues() {
            return issues;
        }

        public void addIssue(String issue) {
            this.issues.add(issue);
            this.valid = false;
        }

        public List<String> getWarnings() {
            return warnings;
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        public boolean hasIssues() {
            return !issues.isEmpty();
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Valid: ").append(valid).append("\n");
            if (!issues.isEmpty()) {
                sb.append("Issues:\n");
                issues.forEach(issue -> sb.append("  - ").append(issue).append("\n"));
            }
            if (!warnings.isEmpty()) {
                sb.append("Warnings:\n");
                warnings.forEach(warning -> sb.append("  - ").append(warning).append("\n"));
            }
            return sb.toString();
        }
    }
}
