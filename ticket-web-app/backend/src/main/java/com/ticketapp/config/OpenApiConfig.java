package com.ticketapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketAppOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ticket Web App API")
                        .description("APIs for authentication, ticket and card purchases, passenger data, and administration")
                        .version("1.0.0"));
    }
}
