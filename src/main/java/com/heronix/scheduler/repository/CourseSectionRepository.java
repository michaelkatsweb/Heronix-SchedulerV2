package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.CourseSection;
import com.heronix.scheduler.model.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseSectionRepository extends JpaRepository<CourseSection, Long> {

    List<CourseSection> findByCourse(Course course);

    List<CourseSection> findByIsSingletonTrue();

    List<CourseSection> findByIsDoubletonTrue();

    List<CourseSection> findBySectionStatus(CourseSection.SectionStatus status);

    @Query("SELECT cs FROM CourseSection cs WHERE " +
           "cs.scheduleYear = :year AND cs.semester = :semester")
    List<CourseSection> findByYearAndSemester(@Param("year") Integer year, @Param("semester") Integer semester);

    @Query("SELECT cs FROM CourseSection cs WHERE " +
           "cs.course = :course AND cs.sectionStatus IN ('OPEN', 'SCHEDULED') " +
           "ORDER BY cs.currentEnrollment ASC")
    List<CourseSection> findAvailableSectionsForCourse(@Param("course") Course course);

    @Query("SELECT cs FROM CourseSection cs WHERE " +
           "cs.course.isSingleton = true AND cs.scheduleYear = :year")
    List<CourseSection> findSingletonsForYear(@Param("year") Integer year);

    @Query("SELECT AVG(cs.currentEnrollment) FROM CourseSection cs WHERE " +
           "cs.course = :course AND cs.sectionStatus != 'CANCELLED'")
    Double getAverageEnrollmentForCourse(@Param("course") Course course);

    /**
     * Count course sections by schedule year
     */
    long countByScheduleYear(Integer scheduleYear);

    // ========================================================================
    // SECTION MANAGEMENT QUERIES (Added Dec 21, 2025)
    // ========================================================================

    /**
     * Find all sections for a specific course with teacher and room loaded
     */
    @Query("SELECT cs FROM CourseSection cs " +
           "LEFT JOIN FETCH cs.assignedTeacher " +
           "LEFT JOIN FETCH cs.assignedRoom " +
           "WHERE cs.course.id = :courseId " +
           "ORDER BY cs.sectionNumber")
    List<CourseSection> findByCourseIdWithTeacherAndRoom(@Param("courseId") Long courseId);

    /**
     * Find all sections assigned to a specific teacher
     */
    @Query("SELECT cs FROM CourseSection cs " +
           "LEFT JOIN FETCH cs.course " +
           "WHERE cs.assignedTeacher.id = :teacherId " +
           "ORDER BY cs.assignedPeriod, cs.course.courseCode")
    List<CourseSection> findByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * Check if teacher is assigned to a section at a specific period
     * Used to prevent double-booking
     */
    @Query("SELECT cs FROM CourseSection cs " +
           "WHERE cs.assignedTeacher.id = :teacherId AND cs.assignedPeriod = :period")
    List<CourseSection> findByTeacherIdAndPeriod(
            @Param("teacherId") Long teacherId,
            @Param("period") Integer period);

    /**
     * Check if room is assigned to a section at a specific period
     * Used to prevent double-booking
     */
    @Query("SELECT cs FROM CourseSection cs " +
           "WHERE cs.assignedRoom.id = :roomId AND cs.assignedPeriod = :period")
    List<CourseSection> findByRoomIdAndPeriod(
            @Param("roomId") Long roomId,
            @Param("period") Integer period);

    /**
     * Get total enrollment across all sections of a course
     */
    @Query("SELECT COALESCE(SUM(cs.currentEnrollment), 0) FROM CourseSection cs WHERE cs.course.id = :courseId")
    int getTotalEnrollmentByCourseId(@Param("courseId") Long courseId);

    /**
     * Get total capacity across all sections of a course
     */
    @Query("SELECT COALESCE(SUM(cs.maxEnrollment), 0) FROM CourseSection cs WHERE cs.course.id = :courseId")
    int getTotalCapacityByCourseId(@Param("courseId") Long courseId);
}
