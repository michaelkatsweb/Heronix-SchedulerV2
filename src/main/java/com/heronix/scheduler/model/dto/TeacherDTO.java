package com.heronix.scheduler.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Teacher Data Transfer Object
 *
 * Purpose: Transfer teacher data from SIS to Scheduler via REST API
 *
 * Contains minimal teacher information needed for scheduling:
 * - ID, name, employee ID
 * - Department, certifications
 * - Availability windows
 * - Room preferences
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherDTO {

    private Long id;
    private String employeeId;
    private String firstName;
    private String lastName;
    private String email;
    private String department;

    // Certifications & Qualifications
    private List<String> certifications;
    private List<String> qualifications;

    // Scheduling Preferences
    private Long homeRoomId;
    private List<Long> preferredRoomIds;

    // Availability
    private List<TeacherAvailabilityDTO> availability;

    // Lunch Assignment
    private Long lunchWaveId;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
