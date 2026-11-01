package com.playground.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.api.dto.common.AuthDto;
import com.playground.api.dto.common.KeyValueDto;
import com.playground.api.dto.request.CreateCollectionDto;
import com.playground.api.dto.request.CreateEnvironmentDto;
import com.playground.api.dto.request.EnvironmentVariableDto;
import com.playground.api.dto.request.ExecuteRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import com.playground.api.dto.request.CreateWorkspaceDto;
import com.playground.api.dto.response.WorkspaceResponseDto;
import com.playground.api.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExecutionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkspaceService workspaceService;

    private String testWorkspaceKey;

    @BeforeEach
    void setUp() {
        WorkspaceResponseDto ws = workspaceService.createWorkspace(new CreateWorkspaceDto("Test WS"));
        testWorkspaceKey = ws.getWorkspaceKey();
    }

    @Test
    @DisplayName("Health endpoint should report UP status and sanitized details without internal database vendor or error exposure")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("api-testing-playground-backend"))
                .andExpect(jsonPath("$.details.databaseConnected").value(true))
                .andExpect(jsonPath("$.details.databaseType").doesNotExist())
                .andExpect(jsonPath("$.details.dbError").doesNotExist());
    }

    @Test
    @DisplayName("Echo GET endpoint should echo query parameters and client metadata")
    void testEchoGetEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/echo/get")
                        .param("testKey", "testVal")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.queryParams.testKey").value("testVal"))
                .andExpect(jsonPath("$.queryParams.page").value("1"));
    }

    @Test
    @DisplayName("Echo POST endpoint should echo JSON payload")
    void testEchoPostEndpoint() throws Exception {
        String payload = "{\"username\":\"saif\",\"action\":\"test\"}";

        mockMvc.perform(post("/api/v1/echo/post")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.body").value(payload));
    }

    @Test
    @DisplayName("Echo Auth Check should reject request without API key with 401")
    void testEchoAuthCheckUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/echo/auth-check"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.authenticated").value(false));
    }

    @Test
    @DisplayName("Echo Auth Check should accept valid header API key with 200")
    void testEchoAuthCheckHeaderSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/echo/auth-check")
                        .header("X-API-Key", "my-secret-key-123456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.placement").value("HEADER (X-API-Key)"));
    }

    @Test
    @DisplayName("Echo Auth Check should accept valid query parameter API key with 200")
    void testEchoAuthCheckQuerySuccess() throws Exception {
        mockMvc.perform(get("/api/v1/echo/auth-check")
                        .param("apiKey", "query-key-abcdef"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.placement").value("QUERY (apiKey)"));
    }

    @Test
    @DisplayName("Collection lifecycle: create, list, and retrieve")
    void testCollectionLifecycle() throws Exception {
        CreateCollectionDto createDto = CreateCollectionDto.builder()
                .name("Integration Test Collection")
                .description("Created during test")
                .build();

        mockMvc.perform(post("/api/v1/collections")
                        .header("X-Workspace-Key", testWorkspaceKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Integration Test Collection"));

        mockMvc.perform(get("/api/v1/collections")
                        .header("X-Workspace-Key", testWorkspaceKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Environment lifecycle: create, activate, and list")
    void testEnvironmentLifecycle() throws Exception {
        CreateEnvironmentDto envDto = CreateEnvironmentDto.builder()
                .name("QA Test Env")
                .isActive(true)
                .variables(List.of(
                        EnvironmentVariableDto.builder().key("apiUrl").value("http://localhost:8080").isSecret(false).isEnabled(true).build(),
                        EnvironmentVariableDto.builder().key("token").value("qa-secret-token").isSecret(true).isEnabled(true).build()
                ))
                .build();

        mockMvc.perform(post("/api/v1/environments")
                        .header("X-Workspace-Key", testWorkspaceKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("QA Test Env"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.variables[0].value").value("http://localhost:8080"))
                .andExpect(jsonPath("$.variables[1].value").value("********"));

        mockMvc.perform(get("/api/v1/environments/active")
                        .header("X-Workspace-Key", testWorkspaceKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("QA Test Env"))
                .andExpect(jsonPath("$.variables[1].value").value("********"));
    }

    @Test
    @DisplayName("CORS: Preflight request from allowed localhost origin should be accepted with credentials")
    void testCorsAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("CORS: Request from unapproved arbitrary origin must be rejected without allow header")
    void testCorsDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header("Origin", "https://untrusted-third-party.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("CORS: Arbitrary pages.dev domains must NOT be accepted by default")
    void testCorsArbitraryPagesDevBlockedByDefault() throws Exception {
        mockMvc.perform(options("/api/v1/health")
                        .header("Origin", "https://unauthorized-deployment.pages.dev")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
