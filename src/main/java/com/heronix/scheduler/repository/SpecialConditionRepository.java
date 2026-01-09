package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.SpecialCondition;
import com.heronix.scheduler.model.domain.SpecialCondition.ConditionType;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.domain.Student;
import com.heronix.scheduler.model.domain.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface SpecialConditionRepository extends JpaRepository<SpecialCondition, Long> {

    List<SpecialCondition> findByActiveTrue();

    List<SpecialCondition> findByTeacherAndActiveTrue(Teacher teacher);

    List<SpecialCondition> findByStudentAndActiveTrue(Student student);

    List<SpecialCondition> findByCourseAndActiveTrue(Course course);

    List<SpecialCondition> findByConditionTypeAndActiveTrue(ConditionType type);

    @Query("SELECT sc FROM SpecialCondition sc WHERE sc.active = true " +
           "AND (sc.effectiveDate IS NULL OR sc.effectiveDate <= CURRENT_DATE) " +
           "AND (sc.expirationDate IS NULL OR sc.expirationDate >= CURRENT_DATE)")
    List<SpecialCondition> findAllEffectiveConditions();

    @Query("SELECT sc FROM SpecialCondition sc WHERE sc.teacher = :teacher " +
           "AND sc.dayOfWeek = :dayOfWeek AND sc.periodNumber = :periodNumber " +
           "AND sc.active = true")
    List<SpecialCondition> findTeacherConditionsForSlot(
        @Param("teacher") Teacher teacher,
        @Param("dayOfWeek") DayOfWeek dayOfWeek,
        @Param("periodNumber") Integer periodNumber);

    List<SpecialCondition> findByIsUnavailableTrueAndActiveTrue();
}
