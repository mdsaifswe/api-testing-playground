package com.playground.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ApiTestingPlaygroundApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiTestingPlaygroundApplication.class, args);
    }
}
