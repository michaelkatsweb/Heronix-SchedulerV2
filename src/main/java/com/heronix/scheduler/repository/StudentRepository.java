package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Student Repository for local persistence of student data.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByStudentId(String studentId);

    List<Student> findByGradeLevel(String gradeLevel);

    List<Student> findByEnrollmentStatus(String enrollmentStatus);

    List<Student> findByHasIEPTrue();

    List<Student> findByHas504PlanTrue();
}
