package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Student Enrollment Repository - SchedulerV2 Version
 * Location: src/main/java/com/heronix/scheduler/repository/StudentEnrollmentRepository.java
 */
@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, Long> {

    /**
     * Find all enrollments for a student
     */
    List<StudentEnrollment> findByStudentId(Long studentId);

    /**
     * Find enrollments for a student in a specific schedule
     */
    List<StudentEnrollment> findByStudentIdAndScheduleId(Long studentId, Long scheduleId);

    /**
     * Find all enrollments for a course
     */
    List<StudentEnrollment> findByCourseId(Long courseId);

    /**
     * Find all enrollments for a schedule
     */
    List<StudentEnrollment> findByScheduleId(Long scheduleId);

    /**
     * Find active enrollments for a student
     */
    @Query("SELECT e FROM StudentEnrollment e WHERE e.student.id = :studentId AND e.status = 'ACTIVE'")
    List<StudentEnrollment> findActiveEnrollmentsByStudentId(@Param("studentId") Long studentId);

    /**
     * Count enrollments for a course
     */
    long countByCourseId(Long courseId);

    /**
     * Find enrollments for a specific schedule slot
     */
    List<StudentEnrollment> findByScheduleSlotId(Long scheduleSlotId);

    /**
     * Check if student is enrolled in a course
     */
    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);
}
