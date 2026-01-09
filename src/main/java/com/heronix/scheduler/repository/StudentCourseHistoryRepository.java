package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Student;
import com.heronix.scheduler.model.domain.StudentCourseHistory;
import com.heronix.scheduler.model.enums.CompletionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for StudentCourseHistory entity
 *
 * Provides database access for student course history and prerequisite validation
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7E - November 21, 2025
 */
@Repository
public interface StudentCourseHistoryRepository extends JpaRepository<StudentCourseHistory, Long> {

    /**
     * Find all course history for a specific student
     *
     * @param student The student
     * @return List of course history ordered by year, semester
     */
    @Query("SELECT h FROM StudentCourseHistory h " +
           "WHERE h.student = :student " +
           "ORDER BY h.academicYear DESC, h.semester, h.course.courseCode")
    List<StudentCourseHistory> findByStudent(@Param("student") Student student);

    /**
     * Find all course history for a student by student ID
     *
     * @param studentId The student ID
     * @return List of course history
     */
    @Query("SELECT h FROM StudentCourseHistory h " +
           "WHERE h.student.id = :studentId " +
           "ORDER BY h.academicYear DESC, h.semester")
    List<StudentCourseHistory> findByStudentId(@Param("studentId") Long studentId);

    /**
     * Find all completed courses for a student
     * (COMPLETED, TRANSFERRED, or WAIVED status)
     *
     * @param student The student
     * @return List of successfully completed courses
     */
    @Query("SELECT h FROM StudentCourseHistory h " +
           "WHERE h.student = :student " +
           "AND h.completionStatus IN ('COMPLETED', 'TRANSFERRED', 'WAIVED') " +
           "ORDER BY h.completionDate DESC")
    List<StudentCourseHistory> findCompletedCourses(@Param("student") Student student);

    /**
     * Find if student has completed a specific course
     *
     * @param student The student
     * @param course The course
     * @return Optional history entry if completed
     */
    @Query("SELECT h FROM StudentCourseHistory h " +
           "WHERE h.student = :student AND h.course = :course " +
           "AND h.completionStatus IN ('COMPLETED', 'TRANSFERRED', 'WAIVED') " +
           "ORDER BY h.completionDate DESC")
    Optional<StudentCourseHistory> findCompletedCourse(
        @Param("student") Student student,
        @Param("course") Course course
    );

    /**
     * Find all courses student is currently taking
     *
     * @param student The student
     * @return List of in-progress courses
     */
    @Query("SELECT h FROM StudentCourseHistory h " +
           "WHERE h.student = :student " +
           "AND h.completionStatus IN ('IN_PROGRESS', 'INCOMPLETE') " +
           "ORDER BY h.academicYear DESC, h.semester")
    List<StudentCourseHistory> findInProgressCourses(@Param("student") Student student);

    /**
     * Find course history for specific academic year
     *
     * @param student The student
     * @param academicYear The academic year (e.g., "2023-2024")
     * @return List of courses for that year
     */
    @Query("SELECT h FROM StudentCourseHistory h " +
           "WHERE h.student = :student AND h.academicYear = :academicYear " +
           "ORDER BY h.semester, h.course.courseCode")
    List<StudentCourseHistory> findByStudentAndYear(
        @Param("student") Student student,
        @Param("academicYear") String academicYear
    );

    /**
     * Find course history for specific semester
     *
     * @param student The student
     * @param academicYear The academic year
     * @param semester The semester
     * @return List of courses for that semester
     */
    @Query("SELECT h FROM StudentCourseHistory h " +
           "WHERE h.student = :student " +
           "AND h.academicYear = :academicYear " +
           "AND h.semester = :semester " +
           "ORDER BY h.course.courseCode")
    List<StudentCourseHistory> findByStudentYearAndSemester(
        @Param("student") Student student,
        @Param("academicYear") String academicYear,
        @Param("semester") String semester
    );

    /**
     * Find all students who have taken a specific course
     *
     * @param course The course
     * @return List of history entries
     */
    @Query("SELECT h FROM StudentCourseHistory h " +
           "WHERE h.course = :course " +
           "ORDER BY h.student.lastName, h.student.firstName")
    List<StudentCourseHistory> findByCourse(@Param("course") Course course);

    /**
     * Find all history entries with specific completion status
     *
     * @param status The completion status
     * @return List of history entries
     */
    @Query("SELECT h FROM StudentCourseHistory h " +
           "WHERE h.completionStatus = :status " +
           "ORDER BY h.student.lastName, h.academicYear DESC")
    List<StudentCourseHistory> findByCompletionStatus(@Param("status") CompletionStatus status);

    /**
     * Calculate total credits earned by student
     *
     * @param student The student
     * @return Total credits earned
     */
    @Query("SELECT COALESCE(SUM(h.creditsEarned), 0.0) FROM StudentCourseHistory h " +
           "WHERE h.student = :student " +
           "AND h.completionStatus IN ('COMPLETED', 'TRANSFERRED', 'WAIVED') " +
           "AND h.creditsEarned IS NOT NULL")
    Double calculateTotalCredits(@Param("student") Student student);

    /**
     * Calculate GPA for student
     * (Only counts courses with letter/numeric grades)
     *
     * @param studentId The student ID
     * @return Average grade (0.0 to 4.0 scale)
     */
    @Query(value = "SELECT AVG(CASE " +
           "WHEN grade_received = 'A+' OR grade_received = 'A' THEN 4.0 " +
           "WHEN grade_received = 'A-' THEN 3.7 " +
           "WHEN grade_received = 'B+' THEN 3.3 " +
           "WHEN grade_received = 'B' THEN 3.0 " +
           "WHEN grade_received = 'B-' THEN 2.7 " +
           "WHEN grade_received = 'C+' THEN 2.3 " +
           "WHEN grade_received = 'C' THEN 2.0 " +
           "WHEN grade_received = 'C-' THEN 1.7 " +
           "WHEN grade_received = 'D+' THEN 1.3 " +
           "WHEN grade_received = 'D' THEN 1.0 " +
           "WHEN grade_received = 'D-' THEN 0.7 " +
           "WHEN grade_received = 'F' THEN 0.0 " +
           "ELSE NULL END) " +
           "FROM student_course_history " +
           "WHERE student_id = :studentId " +
           "AND completion_status IN ('COMPLETED', 'TRANSFERRED') " +
           "AND grade_received IS NOT NULL",
           nativeQuery = true)
    Double calculateGPA(@Param("studentId") Long studentId);

    /**
     * Check if duplicate entry exists
     *
     * @param studentId Student ID
     * @param courseId Course ID
     * @param academicYear Academic year
     * @param semester Semester
     * @return true if entry exists
     */
    @Query("SELECT COUNT(h) > 0 FROM StudentCourseHistory h " +
           "WHERE h.student.id = :studentId " +
           "AND h.course.id = :courseId " +
           "AND h.academicYear = :academicYear " +
           "AND h.semester = :semester")
    boolean existsDuplicate(
        @Param("studentId") Long studentId,
        @Param("courseId") Long courseId,
        @Param("academicYear") String academicYear,
        @Param("semester") String semester
    );

    /**
     * Find all history entries for a specific academic year
     *
     * @param academicYear The academic year
     * @return List of history entries
     */
    @Query("SELECT h FROM StudentCourseHistory h " +
           "WHERE h.academicYear = :academicYear " +
           "ORDER BY h.student.lastName, h.semester")
    List<StudentCourseHistory> findByAcademicYear(@Param("academicYear") String academicYear);

    /**
     * Count courses completed by student
     *
     * @param student The student
     * @return Number of completed courses
     */
    @Query("SELECT COUNT(h) FROM StudentCourseHistory h " +
           "WHERE h.student = :student " +
           "AND h.completionStatus IN ('COMPLETED', 'TRANSFERRED', 'WAIVED')")
    Integer countCompletedCourses(@Param("student") Student student);

    /**
     * Delete all history for a student
     *
     * @param student The student
     */
    void deleteByStudent(Student student);
}
