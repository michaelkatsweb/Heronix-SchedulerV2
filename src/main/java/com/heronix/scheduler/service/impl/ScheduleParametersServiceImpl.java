package com.heronix.scheduler.service.impl;

import com.heronix.scheduler.model.dto.ScheduleParameters;
import com.heronix.scheduler.model.enums.ScheduleType;
import com.heronix.scheduler.service.ScheduleParametersService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalTime;

/**
 * Schedule Parameters Service Implementation
 * Location: src/main/java/com/eduscheduler/service/impl/ScheduleParametersServiceImpl.java
 * 
 * @version 4.0.0
 * @since 2025-10-11
 */
@Slf4j
@Service
public class ScheduleParametersServiceImpl implements ScheduleParametersService {
    
    private static final String PARAMS_FILE = "schedule_parameters.json";
    private final ObjectMapper objectMapper;
    private final File paramsFile;
    
    public ScheduleParametersServiceImpl() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());

        // ✅ NULL SAFE: Validate user.home property with fallback
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            userHome = System.getProperty("java.io.tmpdir");
            if (userHome == null) {
                userHome = ".";
            }
            log.warn("user.home property is null, using fallback: {}", userHome);
        }

        File configDir = new File(userHome, ".eduscheduler");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        this.paramsFile = new File(configDir, PARAMS_FILE);

        // ✅ NULL SAFE: Safe extraction of file path with null check
        String filePath = (paramsFile != null && paramsFile.getAbsolutePath() != null)
            ? paramsFile.getAbsolutePath() : "Unknown";
        log.info("Parameters file: {}", filePath);
    }
    
    @Override
    public void saveParameters(ScheduleParameters parameters) {
        // ✅ NULL SAFE: Validate parameters parameter
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(paramsFile, parameters);

            // ✅ NULL SAFE: Safe extraction of file path with null check
            String filePath = (paramsFile != null && paramsFile.getAbsolutePath() != null)
                ? paramsFile.getAbsolutePath() : "Unknown";
            log.info("✓ Parameters saved to {}", filePath);

        } catch (IOException e) {
            log.error("Failed to save parameters", e);
            throw new RuntimeException("Failed to save parameters: " + e.getMessage(), e);
        }
    }
    
    @Override
    public ScheduleParameters loadParameters() {
        // ✅ NULL SAFE: Check paramsFile exists
        if (paramsFile == null || !paramsFile.exists()) {
            log.info("No saved parameters found, using defaults");
            return getDefaultParameters();
        }

        try {
            ScheduleParameters params = objectMapper.readValue(paramsFile, ScheduleParameters.class);

            // ✅ NULL SAFE: Validate loaded parameters
            if (params == null) {
                log.warn("Loaded parameters are null, using defaults");
                return getDefaultParameters();
            }

            // ✅ NULL SAFE: Safe extraction of file path with null check
            String filePath = (paramsFile.getAbsolutePath() != null)
                ? paramsFile.getAbsolutePath() : "Unknown";
            log.info("✓ Parameters loaded from {}", filePath);
            return params;

        } catch (IOException e) {
            log.error("Failed to load parameters", e);
            return getDefaultParameters();
        }
    }
    
    @Override
    public ScheduleParameters getDefaultParameters() {
        ScheduleParameters params = new ScheduleParameters();
        
        // School day
        params.setSchoolStartTime(LocalTime.of(8, 0));
        params.setSchoolEndTime(LocalTime.of(15, 0));
        
        // Schedule type
        params.setScheduleType(ScheduleType.TRADITIONAL);
        
        // Periods
        params.setPeriodDuration(50);
        params.setPeriodsPerDay(7);
        
        // Lunch
        params.setLunchEnabled(true);
        params.setLunchStartTime(LocalTime.of(12, 0));
        params.setLunchDuration(30);
        params.setLunchWaves(1);
        
        // Breaks
        params.setPassingPeriodDuration(5);
        
        // Teacher constraints
        params.setMaxConsecutiveHours(3);
        params.setMaxClassesPerDay(6);
        params.setMaxDailyHours(8);
        params.setMinPrepPeriods(1);
        params.setRequireLunchBreak(true);
        
        // Room settings
        params.setDefaultRoomCapacity(30);
        params.setCapacityBufferPercent(10.0);
        params.setAllowRoomSharing(false);
        
        log.info("Using default parameters");
        return params;
    }
}