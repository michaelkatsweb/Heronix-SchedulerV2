package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.CoursePrerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for CoursePrerequisite entity
 *
 * Provides database access for prerequisite definitions and validation
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7E - November 21, 2025
 */
@Repository
public interface CoursePrerequisiteRepository extends JpaRepository<CoursePrerequisite, Long> {

    /**
     * Find all prerequisites for a specific course
     *
     * @param course The course to get prerequisites for
     * @return List of prerequisites ordered by group
     */
    @Query("SELECT cp FROM CoursePrerequisite cp " +
           "WHERE cp.course = :course AND cp.active = true " +
           "ORDER BY cp.prerequisiteGroup, cp.prerequisiteCourse.courseCode")
    List<CoursePrerequisite> findByCourse(@Param("course") Course course);

    /**
     * Find all active prerequisites for a specific course
     *
     * @param courseId The course ID
     * @return List of active prerequisites
     */
    @Query("SELECT cp FROM CoursePrerequisite cp " +
           "WHERE cp.course.id = :courseId AND cp.active = true " +
           "ORDER BY cp.prerequisiteGroup")
    List<CoursePrerequisite> findActiveByCourseId(@Param("courseId") Long courseId);

    /**
     * Find all courses that have a specific course as a prerequisite
     * (What courses require this course?)
     *
     * @param prerequisiteCourse The prerequisite course
     * @return List of courses that require this prerequisite
     */
    @Query("SELECT cp FROM CoursePrerequisite cp " +
           "WHERE cp.prerequisiteCourse = :prerequisiteCourse AND cp.active = true " +
           "ORDER BY cp.course.courseCode")
    List<CoursePrerequisite> findCoursesRequiringPrerequisite(
        @Param("prerequisiteCourse") Course prerequisiteCourse
    );

    /**
     * Find all prerequisites in a specific group for a course
     *
     * @param course The course
     * @param group The prerequisite group number
     * @return List of prerequisites in that group
     */
    @Query("SELECT cp FROM CoursePrerequisite cp " +
           "WHERE cp.course = :course AND cp.prerequisiteGroup = :group AND cp.active = true " +
           "ORDER BY cp.prerequisiteCourse.courseCode")
    List<CoursePrerequisite> findByCourseAndGroup(
        @Param("course") Course course,
        @Param("group") Integer group
    );

    /**
     * Find all required prerequisites for a course
     *
     * @param course The course
     * @return List of required prerequisites
     */
    @Query("SELECT cp FROM CoursePrerequisite cp " +
           "WHERE cp.course = :course AND cp.isRequired = true AND cp.active = true " +
           "ORDER BY cp.prerequisiteGroup")
    List<CoursePrerequisite> findRequiredPrerequisites(@Param("course") Course course);

    /**
     * Find all recommended (non-required) prerequisites for a course
     *
     * @param course The course
     * @return List of recommended prerequisites
     */
    @Query("SELECT cp FROM CoursePrerequisite cp " +
           "WHERE cp.course = :course AND cp.isRequired = false AND cp.active = true " +
           "ORDER BY cp.prerequisiteGroup")
    List<CoursePrerequisite> findRecommendedPrerequisites(@Param("course") Course course);

    /**
     * Count how many prerequisite groups a course has
     *
     * @param course The course
     * @return Number of distinct prerequisite groups
     */
    @Query("SELECT COUNT(DISTINCT cp.prerequisiteGroup) FROM CoursePrerequisite cp " +
           "WHERE cp.course = :course AND cp.active = true")
    Integer countPrerequisiteGroups(@Param("course") Course course);

    /**
     * Check if a specific prerequisite relationship exists
     *
     * @param courseId The course ID
     * @param prerequisiteCourseId The prerequisite course ID
     * @return true if relationship exists
     */
    @Query("SELECT COUNT(cp) > 0 FROM CoursePrerequisite cp " +
           "WHERE cp.course.id = :courseId AND cp.prerequisiteCourse.id = :prerequisiteCourseId " +
           "AND cp.active = true")
    boolean existsPrerequisite(
        @Param("courseId") Long courseId,
        @Param("prerequisiteCourseId") Long prerequisiteCourseId
    );

    /**
     * Find all courses that have any prerequisites
     *
     * @return List of courses with prerequisites
     */
    @Query("SELECT DISTINCT cp.course FROM CoursePrerequisite cp " +
           "WHERE cp.active = true " +
           "ORDER BY cp.course.courseCode")
    List<Course> findAllCoursesWithPrerequisites();

    /**
     * Delete all prerequisites for a course
     *
     * @param course The course
     */
    void deleteByCourse(Course course);

    /**
     * Deactivate (soft delete) all prerequisites for a course
     *
     * @param courseId The course ID
     */
    @Query("UPDATE CoursePrerequisite cp SET cp.active = false " +
           "WHERE cp.course.id = :courseId")
    void deactivatePrerequisitesByCourseId(@Param("courseId") Long courseId);
}
