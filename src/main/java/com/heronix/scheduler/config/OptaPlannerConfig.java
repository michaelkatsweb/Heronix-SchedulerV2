package com.heronix.scheduler.config;

import com.heronix.scheduler.model.planning.SchedulingSolution;
import org.optaplanner.core.api.solver.SolverManager;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.SolverManagerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * OptaPlanner Configuration - PRODUCTION READY
 * Location: src/main/java/com/eduscheduler/config/OptaPlannerConfig.java
 * 
 * Configures OptaPlanner AI solver beans for dependency injection
 * 
 * @author Heronix Scheduling System Team
 * @version 4.0.1 - SYNTAX FIXED
 * @since 2025-11-01
 */
@Slf4j
@Configuration
public class OptaPlannerConfig {

    /**
     * Create SolverManager bean for managing OptaPlanner solving jobs
     * 
     * @return Configured SolverManager instance
     */
    @Bean
    public SolverManager<SchedulingSolution, UUID> solverManager() {
        log.info("========================================");
        log.info("INITIALIZING OPTAPLANNER SOLVER MANAGER");
        log.info("========================================");
        
        try {
            // Load solver configuration from XML
            log.info("Loading solver configuration from: solverConfig.xml");
            SolverConfig solverConfig = SolverConfig.createFromXmlResource("solverConfig.xml");
            
            // Validate configuration
            if (solverConfig == null) {
                throw new IllegalStateException("Failed to load solverConfig.xml");
            }
            
            log.info("SUCCESS: Solver configuration loaded successfully");
            log.info("   - Solution class: {}", solverConfig.getSolutionClass());
            log.info("   - Entity classes: {}", solverConfig.getEntityClassList());
            
            // Create SolverManagerConfig for parallel solving
            SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
            
            // Set parallel solver count based on available processors
            int parallelSolverCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);
            solverManagerConfig.setParallelSolverCount(String.valueOf(parallelSolverCount));
            
            log.info("   - Parallel solver count: {}", parallelSolverCount);
            
            // Create and return SolverManager
            SolverManager<SchedulingSolution, UUID> solverManager = 
                SolverManager.create(solverConfig, solverManagerConfig);
            
            log.info("SUCCESS: OptaPlanner SolverManager initialized successfully");
            log.info("========================================");
            
            return solverManager;
            
        } catch (Exception e) {
            log.error("FAILED TO INITIALIZE OPTAPLANNER", e);
            log.error("========================================");
            throw new RuntimeException("OptaPlanner initialization failed", e);
        }
    }

    /**
     * Create SolverFactory bean for creating solver instances
     * 
     * @return Configured SolverFactory instance
     */
    @Bean
    public SolverFactory<SchedulingSolution> solverFactory() {
        log.info("Creating OptaPlanner SolverFactory");
        
        try {
            SolverConfig solverConfig = SolverConfig.createFromXmlResource("solverConfig.xml");
            SolverFactory<SchedulingSolution> solverFactory = SolverFactory.create(solverConfig);
            
            log.info("SUCCESS: SolverFactory created successfully");
            return solverFactory;
            
        } catch (Exception e) {
            log.error("FAILED to create SolverFactory", e);
            throw new RuntimeException("SolverFactory creation failed", e);
        }
    }
}