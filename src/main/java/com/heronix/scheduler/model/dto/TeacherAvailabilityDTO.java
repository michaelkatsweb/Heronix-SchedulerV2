package com.heronix.scheduler.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * Teacher Availability Data Transfer Object
 *
 * Purpose: Transfer teacher availability windows from SIS to Scheduler
 *
 * Contains time windows when teacher is available:
 * - Day of week
 * - Start/end time
 * - Availability type
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0
 * @since 2025-12-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAvailabilityDTO {

    private Long id;
    private Long teacherId;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String availabilityType;  // AVAILABLE, UNAVAILABLE, PLANNING_PERIOD

    public boolean isAvailable() {
        return "AVAILABLE".equalsIgnoreCase(availabilityType);
    }
}
