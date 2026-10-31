package com.playground.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Testing Playground REST API")
                        .version("1.0.0")
                        .description("Production-grade backend service for HTTP request execution, history persistence, collections, and environment management.")
                        .contact(new Contact().name("API Testing Playground Team"))
                        .license(new License().name("Apache 2.0")));
    }
}
