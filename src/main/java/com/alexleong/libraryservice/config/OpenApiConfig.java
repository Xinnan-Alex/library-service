package com.alexleong.libraryservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI libraryServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Library Service API")
                .version("v1")
                .description("Register borrowers and physical book copies, and manage borrowing and returns.")
                .contact(new Contact().name("Library Service maintainers")));
    }
}