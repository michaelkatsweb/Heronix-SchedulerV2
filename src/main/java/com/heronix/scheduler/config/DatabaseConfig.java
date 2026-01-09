package com.heronix.scheduler.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * Database Configuration
 * Location: src/main/java/com/heronix/config/DatabaseConfig.java
 *
 * Configures HikariCP connection pools for different environments:
 * - dev: H2 embedded database
 * - prod: PostgreSQL or H2 file-based
 */
@Configuration
public class DatabaseConfig {

    /**
     * Development profile: H2 embedded database
     * Auto-starts database, stores in ./data folder
     * Uses environment variables or defaults for credentials
     */
    @Bean
    @Profile("dev")
    public DataSource devDataSource(
            @Value("${spring.datasource.username:sa}") String username,
            @Value("${spring.datasource.password:}") String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:file:./data/heronix_dev;AUTO_SERVER=TRUE");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.h2.Driver");

        // Dev pool settings - smaller pool
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);

        return new HikariDataSource(config);
    }

    /**
     * Production profile: Uses environment variables for security
     * Supports PostgreSQL or H2 file-based
     */
    @Bean
    @Profile("prod")
    public DataSource prodDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password,
            @Value("${spring.datasource.driver-class-name}") String driverClassName) {

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driverClassName);

        // Production pool settings - larger pool, longer timeouts
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes

        // Performance tuning
        // Set autoCommit=false to match application.properties and enable proper transaction management
        config.setAutoCommit(false);
        config.setConnectionTestQuery("SELECT 1");

        return new HikariDataSource(config);
    }

    /**
     * Test profile: In-memory H2 database
     * Fast, clean database for each test run
     */
    @Bean
    @Profile("test")
    public DataSource testDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        config.setDriverClassName("org.h2.Driver");

        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);

        return new HikariDataSource(config);
    }
}