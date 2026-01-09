package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.ConflictMatrix;
import com.heronix.scheduler.model.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConflictMatrixRepository extends JpaRepository<ConflictMatrix, Long> {

    Optional<ConflictMatrix> findByCourse1AndCourse2(Course course1, Course course2);

    List<ConflictMatrix> findByCourse1(Course course1);

    List<ConflictMatrix> findByCourse2(Course course2);

    @Query("SELECT cm FROM ConflictMatrix cm WHERE " +
           "(cm.course1 = :course OR cm.course2 = :course) " +
           "ORDER BY cm.conflictCount DESC")
    List<ConflictMatrix> findAllConflictsForCourse(@Param("course") Course course);

    @Query("SELECT cm FROM ConflictMatrix cm WHERE " +
           "cm.isSingletonConflict = true " +
           "ORDER BY cm.conflictCount DESC")
    List<ConflictMatrix> findSingletonConflicts();

    @Query("SELECT cm FROM ConflictMatrix cm WHERE " +
           "cm.scheduleYear = :year " +
           "ORDER BY cm.conflictCount DESC")
    List<ConflictMatrix> findByScheduleYear(@Param("year") Integer year);

    @Query("SELECT cm FROM ConflictMatrix cm WHERE " +
           "cm.conflictCount >= :minCount " +
           "ORDER BY cm.conflictCount DESC")
    List<ConflictMatrix> findHighConflicts(@Param("minCount") Integer minCount);
}
