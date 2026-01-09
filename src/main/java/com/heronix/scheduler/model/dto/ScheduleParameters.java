package com.heronix.scheduler.model.dto;

import com.heronix.scheduler.model.enums.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

/**
 * Schedule Parameters DTO
 * Location: src/main/java/com/eduscheduler/model/dto/ScheduleParameters.java
 * 
 * @version 4.0.0
 * @since 2025-10-11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleParameters {

    // School day timing
    private LocalTime schoolStartTime = LocalTime.of(8, 0);
    private LocalTime schoolEndTime = LocalTime.of(15, 0);

    // Schedule type
    private ScheduleType scheduleType = ScheduleType.TRADITIONAL;

    // Period configuration
    private int periodDuration = 50; // minutes
    private int periodsPerDay = 7;
    private boolean blockSchedule = false;
    private int blockDuration = 90; // minutes

    // Lunch configuration
    private boolean lunchEnabled = true;
    private LocalTime lunchStartTime = LocalTime.of(12, 0);
    private int lunchDuration = 30; // minutes
    private int lunchWaves = 1;

    // Break configuration
    private boolean morningBreakEnabled = false;
    private LocalTime morningBreakTime = LocalTime.of(10, 15);
    private int morningBreakDuration = 10; // minutes

    private boolean afternoonBreakEnabled = false;
    private LocalTime afternoonBreakTime = LocalTime.of(14, 15);
    private int afternoonBreakDuration = 10; // minutes

    private int passingPeriodDuration = 5; // minutes

    // Teacher constraints
    private int maxConsecutiveHours = 3;
    private int maxClassesPerDay = 6;
    private int maxDailyHours = 8;
    private int minPrepPeriods = 1;
    private boolean requireLunchBreak = true;

    // Room settings
    private int defaultRoomCapacity = 30;
    private double capacityBufferPercent = 10.0;
    private boolean allowRoomSharing = false;

    // Advanced options
    private boolean rotatingSchedule = false;
    private String rotatingDays = "A,B";
    private boolean flexModEnabled = false;
    private int flexModDuration = 20; // minutes
}