package com.playground.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
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

    public static final List<String> ALLOWED_METHODS = List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
    );

    public static final List<String> ALLOWED_HEADERS = List.of(
            "Content-Type",
            "Authorization",
            "X-Workspace-Key",
            "Accept",
            "Origin",
            "X-Requested-With",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers",
            "*"
    );

    public static final List<String> EXPOSED_HEADERS = List.of(
            "Authorization",
            "Content-Type",
            "Content-Disposition",
            "X-Execution-Duration",
            "X-Response-Size"
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
                .map(s -> s.replaceAll("^[\"']+|[\"']+$", ""))
                .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return parsed.isEmpty() ? DEFAULT_ORIGINS : parsed;
    }

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistrationBean() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = createCorsConfiguration();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }

    public CorsConfiguration createCorsConfiguration() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(allowedOrigins);
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(ALLOWED_HEADERS);
        config.setExposedHeaders(EXPOSED_HEADERS);
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        return config;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns(allowedOrigins.toArray(new String[0]))
                .allowedMethods(ALLOWED_METHODS.toArray(new String[0]))
                .allowedHeaders(ALLOWED_HEADERS.toArray(new String[0]))
                .exposedHeaders(EXPOSED_HEADERS.toArray(new String[0]))
                .allowCredentials(true)
                .maxAge(3600);
    }
}

