package com.heronix.scheduler.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalTime;
import java.time.DayOfWeek;

/**
 * Conflict DTO - Represents a scheduling conflict
 * Location: src/main/java/com/eduscheduler/model/dto/Conflict.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Conflict {

    private String conflictType; // TEACHER_CONFLICT, ROOM_CONFLICT, etc.
    private String severity; // HIGH, MEDIUM, LOW
    private String description; // Human-readable description

    private Long teacherId;
    private String teacherName;

    private Long roomId;
    private String roomNumber;

    private Long courseId;
    private String courseName;

    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;

    private String resolution; // Suggested fix
    private boolean canAutoResolve; // Can system fix automatically?
}