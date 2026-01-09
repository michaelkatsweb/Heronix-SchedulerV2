package com.heronix.scheduler.service;

import com.heronix.scheduler.model.dto.ScheduleParameters;

/**
 * Schedule Parameters Service Interface
 * Location:
 * src/main/java/com/eduscheduler/service/ScheduleParametersService.java
 */
public interface ScheduleParametersService {

    /**
     * Save schedule parameters
     */
    void saveParameters(ScheduleParameters parameters);

    /**
     * Load schedule parameters
     */
    ScheduleParameters loadParameters();

    /**
     * Get default parameters
     */
    ScheduleParameters getDefaultParameters();
}
