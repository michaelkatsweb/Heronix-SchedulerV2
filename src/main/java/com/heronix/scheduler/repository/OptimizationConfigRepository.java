package com.heronix.scheduler.repository;

import com.heronix.scheduler.model.domain.OptimizationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Optimization Config Repository
 * Data access for optimization configurations
 *
 * Location: src/main/java/com/eduscheduler/repository/OptimizationConfigRepository.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since Phase 7C - Schedule Optimization
 */
@Repository
public interface OptimizationConfigRepository extends JpaRepository<OptimizationConfig, Long> {

    /**
     * Find default configuration
     */
    Optional<OptimizationConfig> findByIsDefaultTrue();

    /**
     * Find configuration by name
     */
    Optional<OptimizationConfig> findByConfigName(String configName);
}
