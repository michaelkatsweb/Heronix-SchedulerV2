package com.heronix.scheduler.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Enrollment Data Transfer Object
 *
 * Purpose: Transfer student enrollment data from SIS to Scheduler via REST API
 *
 * Contains enrollment relationships:
 * - Student ID
 * - Course ID
 * - Enrollment status
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentDTO {

    private Long id;
    private Long studentId;
    private Long courseId;
    private String enrollmentStatus;  // ENROLLED, DROPPED, COMPLETED
    private Integer priority;  // For conflict resolution (1 = highest)

    // Preferences
    private Long preferredTeacherId;
    private String preferredPeriod;  // "Morning", "Afternoon", etc.
}
