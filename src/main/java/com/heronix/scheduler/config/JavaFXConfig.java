package com.heronix.scheduler.config;

import javafx.stage.Stage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

/**
 * JavaFX Configuration - FIXED threading issue
 * Location: src/main/java/com/eduscheduler/config/JavaFXConfig.java
 */
@Configuration
public class JavaFXConfig {

    /**
     * Primary Stage bean
     * 
     * @Lazy prevents immediate instantiation
     *       @Scope("prototype") creates new instance when needed
     */
    @Bean
    @Lazy
    @Scope("prototype")
    public Stage primaryStage() {
        // Don't create Stage here - let JavaFX app create it
        return null;
    }
}