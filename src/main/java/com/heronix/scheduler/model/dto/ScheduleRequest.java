package com.heronix.scheduler.model.dto;

import com.heronix.scheduler.model.enums.SchedulePeriod;
import com.heronix.scheduler.model.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Request object for schedule generation/optimization
 * Location: src/main/java/com/eduscheduler/model/dto/ScheduleRequest.java
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

    // Basic schedule info
    private String scheduleName;
    private ScheduleType scheduleType;
    private SchedulePeriod period;
    private LocalDate startDate;
    private LocalDate endDate;

    // Scheduling constraints
    private LocalTime schoolStartTime;
    private LocalTime schoolEndTime;
    private LocalTime lunchStartTime;
    private Integer lunchDurationMinutes = 30;
    private Integer breakDurationMinutes = 10;
    private Integer maxConsecutiveHours = 4;

    // Optimization settings
    private boolean useAIOptimization = true;
    private boolean minimizeGaps = true;
    private boolean balanceWorkload = true;
    private boolean optimizeRoomUsage = true;
    private boolean respectTeacherPreferences = true;

    // Management principles
    private boolean applyLeanPrinciples = true;
    private boolean applyKanban = false;

    // Entity selection for scheduling
    private List<Long> teacherIds = new ArrayList<>();
    private List<Long> courseIds = new ArrayList<>();
    private List<Long> roomIds = new ArrayList<>();

    // Additional constraints
    private List<Long> excludedTeacherIds = new ArrayList<>();
    private List<Long> excludedRoomIds = new ArrayList<>();
    private List<Long> priorityCourseIds = new ArrayList<>();

    // Timeout settings
    private Integer optimizationTimeoutSeconds = 300; // 5 minutes default
}