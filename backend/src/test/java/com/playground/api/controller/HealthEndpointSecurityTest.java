package com.playground.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.api.dto.response.HealthResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthEndpointSecurityTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @InjectMocks
    private HealthController healthController;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("Health endpoint: Healthy database returns status UP and databaseConnected true")
    void testHealthyDatabaseResponse() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);

        ResponseEntity<HealthResponseDto> responseEntity = healthController.checkHealth();
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());

        HealthResponseDto body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals("UP", body.getStatus());
        assertEquals("api-testing-playground-backend", body.getService());
        assertEquals(true, body.getDetails().get("databaseConnected"));

        // Must NOT expose database vendor banner
        assertNull(body.getDetails().get("databaseType"), "databaseType must not be exposed to anonymous callers");
        assertNull(body.getDetails().get("dbError"));

        String json = objectMapper.writeValueAsString(body);
        assertFalse(json.contains("PostgreSQL"), "JSON response must not contain database vendor names");
        assertFalse(json.contains("MySQL"), "JSON response must not contain database vendor names");
        assertFalse(json.contains("H2"), "JSON response must not contain database vendor names");
    }

    @Test
    @DisplayName("Health endpoint: Database connection failure returns status DEGRADED without leaking exception details")
    void testDatabaseFailureDoesNotLeakSensitiveExceptionDetails() throws Exception {
        String sensitiveErrorMessage = "FATAL: password authentication failed for user 'neon_admin' at ep-cool-moon-12345.us-east-2.aws.neon.tech:5432";
        when(dataSource.getConnection()).thenThrow(new SQLException(sensitiveErrorMessage));

        ResponseEntity<HealthResponseDto> responseEntity = healthController.checkHealth();
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());

        HealthResponseDto body = responseEntity.getBody();
        assertNotNull(body);
        assertEquals("DEGRADED", body.getStatus(), "Status should be DEGRADED when database check fails");
        assertEquals(false, body.getDetails().get("databaseConnected"));

        // Must NOT leak the exception message, hostname, username, or credentials
        assertNull(body.getDetails().get("dbError"), "Exception message must NEVER be serialized into the public response");
        assertNull(body.getDetails().get("databaseType"));

        String serializedJson = objectMapper.writeValueAsString(body);
        assertFalse(serializedJson.contains(sensitiveErrorMessage), "Response must not contain sensitive exception message");
        assertFalse(serializedJson.contains("neon.tech"), "Response must not contain database hostnames");
        assertFalse(serializedJson.contains("neon_admin"), "Response must not contain database usernames");
        assertFalse(serializedJson.contains(":5432"), "Response must not contain database port numbers");
    }
}
