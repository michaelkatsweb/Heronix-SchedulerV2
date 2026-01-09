package com.heronix.scheduler.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Lunch Assignment Data Transfer Object
 *
 * Purpose: Transfer lunch wave assignments from SIS to Scheduler
 *
 * Contains lunch assignments for students and teachers:
 * - Student/Teacher ID
 * - Lunch wave ID
 * - Assignment type
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LunchAssignmentDTO {

    private Long id;
    private Long studentId;  // Null if teacher assignment
    private Long teacherId;  // Null if student assignment
    private Long lunchWaveId;
    private String assignmentType;  // STUDENT, TEACHER_DUTY

    public boolean isStudentAssignment() {
        return studentId != null;
    }

    public boolean isTeacherAssignment() {
        return teacherId != null;
    }
}
