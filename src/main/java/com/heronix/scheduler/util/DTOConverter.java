package com.heronix.scheduler.util;

import com.heronix.scheduler.model.domain.Course;
import com.heronix.scheduler.model.domain.Student;
import com.heronix.scheduler.model.domain.Teacher;
import com.heronix.scheduler.model.dto.CourseDTO;
import com.heronix.scheduler.model.dto.StudentDTO;
import com.heronix.scheduler.model.dto.TeacherDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO Converter Utility
 *
 * Converts DTOs fetched from SIS REST API into Scheduler domain entities.
 * These are "shadow" entities used only for scheduling operations.
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Slf4j
public class DTOConverter {

    /**
     * Convert StudentDTO to Student entity
     */
    public static Student toStudent(StudentDTO dto) {
        if (dto == null) {
            return null;
        }

        Student student = new Student();
        student.setId(dto.getId());
        student.setStudentId(dto.getStudentId());
        student.setFirstName(dto.getFirstName());
        student.setLastName(dto.getLastName());
        student.setGradeLevel(dto.getGrade());
        student.setDateOfBirth(dto.getDateOfBirth());
        student.setEnrollmentStatus(dto.getEnrollmentStatus());
        student.setHasIEP(dto.isHasIEP());
        student.setHas504Plan(dto.isHas504Plan());
        student.setLunchWaveId(dto.getLunchWaveId());
        student.setEnrolledCourses(new ArrayList<>());

        return student;
    }

    /**
     * Convert list of StudentDTOs to Student entities
     */
    public static List<Student> toStudents(List<StudentDTO> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(DTOConverter::toStudent)
                .collect(Collectors.toList());
    }

    /**
     * Convert TeacherDTO to Teacher entity
     */
    public static Teacher toTeacher(TeacherDTO dto) {
        if (dto == null) {
            return null;
        }

        Teacher teacher = new Teacher();
        teacher.setId(dto.getId());
        teacher.setEmployeeId(dto.getEmployeeId());
        teacher.setFirstName(dto.getFirstName());
        teacher.setLastName(dto.getLastName());
        teacher.setEmail(dto.getEmail());
        teacher.setDepartment(dto.getDepartment());
        teacher.setCertifications(dto.getCertifications() != null ? dto.getCertifications() : new ArrayList<>());
        teacher.setQualifications(dto.getQualifications() != null ? dto.getQualifications() : new ArrayList<>());
        teacher.setHomeRoomId(dto.getHomeRoomId());
        teacher.setPreferredRoomIds(dto.getPreferredRoomIds() != null ? dto.getPreferredRoomIds() : new ArrayList<>());
        teacher.setLunchWaveId(dto.getLunchWaveId());
        teacher.setCourses(new ArrayList<>());

        return teacher;
    }

    /**
     * Convert list of TeacherDTOs to Teacher entities
     */
    public static List<Teacher> toTeachers(List<TeacherDTO> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(DTOConverter::toTeacher)
                .collect(Collectors.toList());
    }

    /**
     * Convert CourseDTO to Course entity
     */
    public static Course toCourse(CourseDTO dto) {
        if (dto == null) {
            return null;
        }

        Course course = new Course();
        course.setId(dto.getId());
        course.setCourseCode(dto.getCourseCode());
        course.setCourseName(dto.getCourseName());
        course.setSubject(dto.getSubject());
        course.setDepartment(dto.getDepartment());
        course.setCredits(dto.getCredits());
        course.setCapacity(dto.getCapacity());
        course.setCourseType(dto.getCourseType());
        course.setRequiresLab(dto.isRequiresLab());
        course.setRequiredRoomType(dto.getRequiredRoomType());
        course.setPeriodsPerWeek(dto.getPeriodsPerWeek());
        course.setEnrolledStudents(new ArrayList<>());

        return course;
    }

    /**
     * Convert list of CourseDTOs to Course entities
     */
    public static List<Course> toCourses(List<CourseDTO> dtos) {
        if (dtos == null) {
            return new ArrayList<>();
        }
        return dtos.stream()
                .map(DTOConverter::toCourse)
                .collect(Collectors.toList());
    }
}
