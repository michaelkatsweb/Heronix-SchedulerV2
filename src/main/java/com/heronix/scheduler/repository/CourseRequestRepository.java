package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.CourseRequest;
import com.heronix.scheduler.model.domain.Student;
import com.heronix.scheduler.model.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRequestRepository extends JpaRepository<CourseRequest, Long> {

    List<CourseRequest> findByStudent(Student student);

    List<CourseRequest> findByCourse(Course course);

    @Query("SELECT cr FROM CourseRequest cr WHERE cr.course.id = :courseId")
    List<CourseRequest> findByCourseId(@Param("courseId") Long courseId);

    List<CourseRequest> findByRequestStatus(CourseRequest.RequestStatus status);

    List<CourseRequest> findByStudentAndRequestStatus(Student student, CourseRequest.RequestStatus status);

    @Query("SELECT cr FROM CourseRequest cr WHERE " +
           "cr.requestYear = :year " +
           "ORDER BY cr.isRequiredForGraduation DESC, cr.studentWeight DESC, cr.priorityRank ASC")
    List<CourseRequest> findPendingRequestsForYear(@Param("year") Integer year);

    @Query("SELECT cr.course, COUNT(cr) FROM CourseRequest cr WHERE " +
           "cr.requestYear = :year " +
           "GROUP BY cr.course " +
           "ORDER BY COUNT(cr) DESC")
    List<Object[]> getCourseDemandForYear(@Param("year") Integer year);

    @Query("SELECT cr FROM CourseRequest cr WHERE " +
           "cr.student = :student AND cr.requestYear = :year " +
           "ORDER BY cr.priorityRank ASC")
    List<CourseRequest> findByStudentAndYear(@Param("student") Student student, @Param("year") Integer year);
}
