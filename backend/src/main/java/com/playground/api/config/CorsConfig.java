package com.playground.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    public static final List<String> DEFAULT_ORIGINS = List.of(
            "http://localhost:[*]",
            "http://127.0.0.1:[*]",
            "http://localhost",
            "https://localhost:[*]"
    );

    private final List<String> allowedOrigins;

    public CorsConfig(
            @Value("${playground.cors.allowed-origins:http://localhost:[*],http://127.0.0.1:[*],http://localhost,https://localhost:[*]}")
            List<String> rawAllowedOrigins) {
        this.allowedOrigins = parseOrigins(rawAllowedOrigins);
    }

    private static List<String> parseOrigins(List<String> rawOrigins) {
        if (rawOrigins == null || rawOrigins.isEmpty()) {
            return DEFAULT_ORIGINS;
        }
        List<String> parsed = rawOrigins.stream()
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return parsed.isEmpty() ? DEFAULT_ORIGINS : parsed;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.toArray(new String[0]))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type", "Content-Disposition", "X-Execution-Duration", "X-Response-Size")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
