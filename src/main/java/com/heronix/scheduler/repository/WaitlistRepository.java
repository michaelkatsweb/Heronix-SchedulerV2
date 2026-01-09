package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Student;
import com.heronix.scheduler.model.domain.Waitlist;
import com.heronix.scheduler.model.domain.Waitlist.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Waitlist Repository
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    /**
     * Find waitlist entries by course and status ordered by position
     */
    List<Waitlist> findByCourseAndStatusOrderByPositionAsc(Course course, WaitlistStatus status);

    /**
     * Find waitlist entry by student, course, and status
     */
    Optional<Waitlist> findByStudentAndCourseAndStatus(Student student, Course course, WaitlistStatus status);

    /**
     * Count active waitlist entries for a course
     */
    @Query("SELECT COUNT(w) FROM Waitlist w WHERE w.courseId = :courseId AND w.status = 'ACTIVE'")
    Long countActiveWaitlistForCourse(@Param("courseId") Long courseId);
}
