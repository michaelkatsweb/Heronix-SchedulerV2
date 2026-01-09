package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.CourseEnrollmentRequest;
import com.heronix.scheduler.model.domain.Student;
import com.heronix.scheduler.model.enums.EnrollmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Course Enrollment Request Repository
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Repository
public interface CourseEnrollmentRequestRepository extends JpaRepository<CourseEnrollmentRequest, Long> {

    /**
     * Find requests by student and status
     */
    List<CourseEnrollmentRequest> findByStudentAndRequestStatus(Student student, EnrollmentRequestStatus status);

    /**
     * Find all pending requests ordered by priority
     */
    @Query("SELECT r FROM CourseEnrollmentRequest r WHERE r.requestStatus = 'PENDING' ORDER BY r.priorityScore DESC, r.preferenceRank ASC")
    List<CourseEnrollmentRequest> findAllPendingOrderedByPriority();

    /**
     * Find pending requests by academic year ordered by priority
     */
    @Query("SELECT r FROM CourseEnrollmentRequest r WHERE r.academicYearId = :academicYearId AND r.requestStatus = 'PENDING' ORDER BY r.priorityScore DESC, r.preferenceRank ASC")
    List<CourseEnrollmentRequest> findPendingByAcademicYearOrderedByPriority(@Param("academicYearId") Long academicYearId);

    /**
     * Find pending requests by course ordered by priority
     */
    @Query("SELECT r FROM CourseEnrollmentRequest r WHERE r.courseId = :courseId AND r.requestStatus = 'PENDING' ORDER BY r.priorityScore DESC, r.preferenceRank ASC")
    List<CourseEnrollmentRequest> findPendingByCourseOrderedByPriority(@Param("courseId") Long courseId);

    /**
     * Find pending requests by student ordered by preference rank
     */
    @Query("SELECT r FROM CourseEnrollmentRequest r WHERE r.studentId = :studentId AND r.requestStatus = 'PENDING' ORDER BY r.preferenceRank ASC")
    List<CourseEnrollmentRequest> findPendingByStudentOrderedByPreference(@Param("studentId") Long studentId);

    /**
     * Find waitlist entries by course ordered by position
     */
    @Query("SELECT r FROM CourseEnrollmentRequest r WHERE r.courseId = :courseId AND r.isWaitlist = true ORDER BY r.waitlistPosition ASC")
    List<CourseEnrollmentRequest> findWaitlistByCourseId(@Param("courseId") Long courseId);

    /**
     * Find waitlist entries by course and status ordered by position
     */
    List<CourseEnrollmentRequest> findByCourseAndIsWaitlistTrueOrderByWaitlistPositionAsc(Course course);

    /**
     * Count waitlist entries for a course
     */
    Long countByCourseAndIsWaitlistTrue(Course course);

    /**
     * Get next available waitlist position for a course
     */
    @Query("SELECT COALESCE(MAX(r.waitlistPosition), 0) + 1 FROM CourseEnrollmentRequest r WHERE r.courseId = :courseId AND r.isWaitlist = true")
    Integer getNextWaitlistPosition(@Param("courseId") Long courseId);
}
