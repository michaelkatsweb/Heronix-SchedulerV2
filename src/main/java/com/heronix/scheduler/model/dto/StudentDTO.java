package com.heronix.scheduler.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;

/**
 * Student Data Transfer Object
 *
 * Purpose: Transfer student data from SIS to Scheduler via REST API
 *
 * Contains minimal student information needed for scheduling:
 * - ID, name, grade level
 * - Enrollment status
 * - Special education flags
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StudentDTO {

    private Long id;
    private String studentId;  // Student ID number
    private String firstName;
    private String lastName;
    private String grade;  // Grade level (9, 10, 11, 12)
    private LocalDate dateOfBirth;
    private String enrollmentStatus;  // ENROLLED, WITHDRAWN, etc.

    // Special Education Flags
    private boolean hasIEP;
    private boolean has504Plan;

    // Lunch Assignment
    private Long lunchWaveId;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
