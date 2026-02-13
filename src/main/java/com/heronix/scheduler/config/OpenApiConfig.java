package com.heronix.scheduler.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI heronixSchedulerOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Heronix AI Scheduling Engine")
                        .description("AI-Powered Schedule Optimization API using OptaPlanner for K-12 schools")
                        .version("2.0.0")
                        .contact(new Contact()
                                .name("Heronix Scheduling System Team")));
    }
}
