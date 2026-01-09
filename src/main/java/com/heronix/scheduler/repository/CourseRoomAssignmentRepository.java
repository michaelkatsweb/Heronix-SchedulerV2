package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.CourseRoomAssignment;
import com.heronix.scheduler.model.domain.Room;
import com.heronix.scheduler.model.enums.RoomAssignmentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Course Room Assignment Repository
 * Phase 6E: Multi-Room Courses
 *
 * Repository for managing course room assignments in multi-room scenarios.
 *
 * @since Phase 6E - December 3, 2025
 */
@Repository
public interface CourseRoomAssignmentRepository extends JpaRepository<CourseRoomAssignment, Long> {

    /**
     * Find all room assignments for a course
     */
    List<CourseRoomAssignment> findByCourse(Course course);

    /**
     * Find all active room assignments for a course
     */
    List<CourseRoomAssignment> findByCourseAndActiveTrue(Course course);

    /**
     * Find all room assignments for a specific room
     */
    List<CourseRoomAssignment> findByRoom(Room room);

    /**
     * Find all active room assignments for a specific room
     */
    List<CourseRoomAssignment> findByRoomAndActiveTrue(Room room);

    /**
     * Find room assignment by course and room
     */
    Optional<CourseRoomAssignment> findByCourseAndRoom(Course course, Room room);

    /**
     * Find active room assignment by course and room
     */
    Optional<CourseRoomAssignment> findByCourseAndRoomAndActiveTrue(Course course, Room room);

    /**
     * Find room assignments by type
     */
    List<CourseRoomAssignment> findByAssignmentType(RoomAssignmentType assignmentType);

    /**
     * Find primary room assignment for a course
     */
    @Query("SELECT cra FROM CourseRoomAssignment cra " +
           "WHERE cra.course = :course " +
           "AND cra.assignmentType = 'PRIMARY' " +
           "AND cra.active = true " +
           "ORDER BY cra.priority ASC")
    Optional<CourseRoomAssignment> findPrimaryRoomAssignment(@Param("course") Course course);

    /**
     * Find all active room assignments for a course ordered by priority
     */
    @Query("SELECT cra FROM CourseRoomAssignment cra " +
           "WHERE cra.course = :course " +
           "AND cra.active = true " +
           "ORDER BY cra.priority ASC, cra.assignmentType ASC")
    List<CourseRoomAssignment> findActiveSortedByPriority(@Param("course") Course course);

    /**
     * Count active room assignments for a course
     */
    @Query("SELECT COUNT(cra) FROM CourseRoomAssignment cra " +
           "WHERE cra.course = :course " +
           "AND cra.active = true")
    long countActiveByCourse(@Param("course") Course course);

    /**
     * Check if a room is assigned to any course
     */
    @Query("SELECT CASE WHEN COUNT(cra) > 0 THEN true ELSE false END " +
           "FROM CourseRoomAssignment cra " +
           "WHERE cra.room = :room " +
           "AND cra.active = true")
    boolean isRoomAssigned(@Param("room") Room room);

    /**
     * Find courses that use a specific room
     */
    @Query("SELECT DISTINCT cra.course FROM CourseRoomAssignment cra " +
           "WHERE cra.room = :room " +
           "AND cra.active = true")
    List<Course> findCoursesUsingRoom(@Param("room") Room room);

    /**
     * Delete all assignments for a course
     */
    void deleteByCourse(Course course);

    /**
     * Delete all assignments for a room
     */
    void deleteByRoom(Room room);

    /**
     * Deactivate all assignments for a course
     */
    @Query("UPDATE CourseRoomAssignment cra SET cra.active = false WHERE cra.course = :course")
    void deactivateByCourse(@Param("course") Course course);

    /**
     * Find all courses that use multiple rooms
     */
    @Query("SELECT DISTINCT cra.course FROM CourseRoomAssignment cra " +
           "WHERE cra.active = true " +
           "GROUP BY cra.course " +
           "HAVING COUNT(cra) > 1")
    List<Course> findCoursesWithMultipleRooms();
}
