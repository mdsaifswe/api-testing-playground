package com.playground.api.controller;

import com.playground.api.dto.response.HealthResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health", description = "Application and database health monitoring")
public class HealthController {

    private final DataSource dataSource;

    @GetMapping
    @Operation(summary = "Check application health", description = "Returns service status, database connectivity, and JVM runtime metrics.")
    public ResponseEntity<HealthResponseDto> checkHealth() {
        Map<String, Object> details = new LinkedHashMap<>();

        // Check database connection
        boolean dbHealthy = false;
        String dbProduct = "Unknown";
        try (Connection conn = dataSource.getConnection()) {
            dbHealthy = conn.isValid(2);
            dbProduct = conn.getMetaData().getDatabaseProductName() + " " + conn.getMetaData().getDatabaseProductVersion();
        } catch (Exception e) {
            details.put("dbError", e.getMessage());
        }

        details.put("databaseConnected", dbHealthy);
        details.put("databaseType", dbProduct);

        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024);
        long freeMemory = runtime.freeMemory() / (1024 * 1024);
        long maxMemory = runtime.maxMemory() / (1024 * 1024);

        details.put("memoryTotalMb", totalMemory);
        details.put("memoryUsedMb", (totalMemory - freeMemory));
        details.put("memoryMaxMb", maxMemory);

        HealthResponseDto response = HealthResponseDto.builder()
                .status(dbHealthy ? "UP" : "DEGRADED")
                .service("api-testing-playground-backend")
                .version("1.0.0")
                .timestamp(Instant.now())
                .details(details)
                .build();

        return ResponseEntity.ok(response);
    }
}
