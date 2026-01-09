package com.heronix.scheduler.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Course Data Transfer Object
 *
 * Purpose: Transfer course data from SIS to Scheduler via REST API
 *
 * Contains course information needed for scheduling:
 * - ID, code, name
 * - Subject, department
 * - Credits, capacity
 * - Lab requirement
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseDTO {

    private Long id;
    private String courseCode;
    private String courseName;
    private String subject;
    private String department;

    // Course Details
    private Double credits;
    private Integer capacity;  // Max students per section
    private String courseType;  // AP, HONORS, REGULAR, etc.

    // Scheduling Requirements
    private boolean requiresLab;
    private String requiredRoomType;  // LAB, GYM, COMPUTER, etc.
    private Integer periodsPerWeek;  // How many periods per week

    // Teacher Assignment
    private Long preferredTeacherId;
}
