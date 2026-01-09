package com.heronix.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Heronix AI Scheduling Engine - Main Application
 *
 * Purpose: AI-powered school schedule optimization using OptaPlanner
 * Port: 8090
 *
 * Domains:
 * - Schedules, ScheduleSlots, TimeSlots
 * - SchedulingSolution (OptaPlanner)
 * - Rooms, Equipment, Zones
 * - Conflicts, Resolutions, Overrides
 * - Optimization Parameters, Results
 * - Lunch Waves, Periods
 *
 * Features:
 * - OptaPlanner AI Constraint Solver (40+ constraints)
 * - REST API Server (for SIS integration)
 * - JavaFX Schedule Generation UI
 * - Conflict Detection and Resolution
 * - Schedule Export (PDF, CSV, Excel, iCal)
 * - Real-time Optimization Metrics
 *
 * Dependencies:
 * - Heronix-SIS (Port 8080) - Fetches Student, Teacher, Course data via REST API
 *
 * @author Heronix Educational Systems LLC
 * @version 2.0.0 - Extracted from Monolithic Scheduler
 * @since 2025-12-21
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties
@EnableCaching
@EnableAsync
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.heronix.scheduler.repository")
@EntityScan(basePackages = "com.heronix.scheduler.model.domain")
public class HeronixSchedulerApplication {

    public static void main(String[] args) {
        log.info("========================================");
        log.info("Heronix AI Scheduling Engine");
        log.info("Version: 2.0.0");
        log.info("Port: 8090");
        log.info("AI Engine: OptaPlanner 9.40.0");
        log.info("========================================");

        SpringApplication.run(HeronixSchedulerApplication.class, args);

        log.info("========================================");
        log.info("Heronix Scheduler Started Successfully!");
        log.info("REST API: http://localhost:8090/api");
        log.info("Swagger UI: http://localhost:8090/swagger-ui.html");
        log.info("H2 Console: http://localhost:8090/h2-console");
        log.info("SIS Integration: http://localhost:8080/api");
        log.info("========================================");
    }

    /**
     * REST Template for SIS API Communication
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
