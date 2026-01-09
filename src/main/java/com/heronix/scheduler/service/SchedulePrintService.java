package com.heronix.scheduler.service;

import com.heronix.scheduler.model.domain.Schedule;

/**
 * Schedule Print Service
 * Location: src/main/java/com/eduscheduler/service/SchedulePrintService.java
 */
public interface SchedulePrintService {

    void printSchedule(Schedule schedule);

    void printSchedulePreview(Schedule schedule);
}