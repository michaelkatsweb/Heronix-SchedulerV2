package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Teacher Repository for local persistence of teacher data.
 */
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmployeeId(String employeeId);

    List<Teacher> findByDepartment(String department);

    List<Teacher> findByActiveTrue();

    Optional<Teacher> findByEmail(String email);
}
