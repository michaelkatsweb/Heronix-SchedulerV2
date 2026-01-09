package com.heronix.scheduler.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Cache Configuration
 *
 * Configures Spring Cache for dashboard metrics and other frequently accessed data.
 * Uses simple in-memory caching with ConcurrentHashMap backend.
 *
 * Location: src/main/java/com/eduscheduler/config/CacheConfig.java
 *
 * @author Heronix Scheduling System Team
 * @version 1.0.0
 * @since 2025-12-12
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure cache manager with named caches.
     *
     * Available caches:
     * - dashboardMetrics: Dashboard statistics and metrics (DashboardMetricsService)
     *
     * @return configured CacheManager
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(Arrays.asList(
            new ConcurrentMapCache("dashboardMetrics")
        ));
        return cacheManager;
    }
}
