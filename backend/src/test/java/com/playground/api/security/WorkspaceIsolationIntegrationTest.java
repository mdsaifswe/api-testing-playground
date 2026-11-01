package com.playground.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.api.dto.request.CreateCollectionDto;
import com.playground.api.dto.request.CreateEnvironmentDto;
import com.playground.api.dto.request.CreateWorkspaceDto;
import com.playground.api.dto.request.EnvironmentVariableDto;
import com.playground.api.dto.response.CollectionResponseDto;
import com.playground.api.dto.response.EnvironmentResponseDto;
import com.playground.api.dto.response.WorkspaceResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class WorkspaceIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService.reset();
    }

    private WorkspaceResponseDto createNewWorkspace(String name) throws Exception {
        CreateWorkspaceDto dto = new CreateWorkspaceDto(name);
        MvcResult result = mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), WorkspaceResponseDto.class);
    }

    @Test
    @DisplayName("Creating a workspace returns a 256-bit cryptographically secure credential starting with wpk_")
    void testCreateWorkspaceReturnsValidCredential() throws Exception {
        WorkspaceResponseDto ws = createNewWorkspace("Security Test WS");

        assertThat(ws.getId()).isNotNull();
        assertThat(ws.getName()).isEqualTo("Security Test WS");
        assertThat(ws.getWorkspaceKey()).isNotNull();
        assertThat(ws.getWorkspaceKey()).startsWith("wpk_");
        // "wpk_" (4 chars) + 64 hex characters = 68 chars
        assertThat(ws.getWorkspaceKey().length()).isEqualTo(68);
    }

    @Test
    @DisplayName("Accessing protected endpoints without X-Workspace-Key returns 401 Unauthorized")
    void testMissingWorkspaceKeyReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/collections"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(containsString("Missing required header: X-Workspace-Key")));

        mockMvc.perform(get("/api/v1/environments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/history"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Accessing protected endpoints with invalid or malformed X-Workspace-Key returns 401 Unauthorized")
    void testInvalidWorkspaceKeyReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/collections")
                        .header("X-Workspace-Key", "invalid-key-format-1234"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        mockMvc.perform(get("/api/v1/collections")
                        .header("X-Workspace-Key", "wpk_0000000000000000000000000000000000000000000000000000000000000000"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Workspace Isolation: Workspace A cannot read or mutate collections owned by Workspace B")
    void testCrossWorkspaceCollectionIsolation() throws Exception {
        WorkspaceResponseDto wsA = createNewWorkspace("Workspace A");
        WorkspaceResponseDto wsB = createNewWorkspace("Workspace B");

        // 1. Workspace A creates a collection
        CreateCollectionDto createDto = CreateCollectionDto.builder()
                .name("Workspace A Secret Collection")
                .description("Private to WS A")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/collections")
                        .header("X-Workspace-Key", wsA.getWorkspaceKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn();

        CollectionResponseDto createdCollection = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), CollectionResponseDto.class);

        // 2. Workspace A can see it
        mockMvc.perform(get("/api/v1/collections/" + createdCollection.getId())
                        .header("X-Workspace-Key", wsA.getWorkspaceKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Workspace A Secret Collection"));

        // 3. Workspace B CANNOT see it (returns 404 to avoid leaking existence)
        mockMvc.perform(get("/api/v1/collections/" + createdCollection.getId())
                        .header("X-Workspace-Key", wsB.getWorkspaceKey()))
                .andExpect(status().isNotFound());

        // 4. Workspace B's collection list does NOT contain Workspace A's collection
        mockMvc.perform(get("/api/v1/collections")
                        .header("X-Workspace-Key", wsB.getWorkspaceKey()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", not(hasItem("Workspace A Secret Collection"))));

        // 5. Workspace B CANNOT delete Workspace A's collection
        mockMvc.perform(delete("/api/v1/collections/" + createdCollection.getId())
                        .header("X-Workspace-Key", wsB.getWorkspaceKey()))
                .andExpect(status().isNotFound());

        // Verify Workspace A's collection still exists
        mockMvc.perform(get("/api/v1/collections/" + createdCollection.getId())
                        .header("X-Workspace-Key", wsA.getWorkspaceKey()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Workspace Isolation: Workspace A cannot view or activate environments owned by Workspace B")
    void testCrossWorkspaceEnvironmentIsolation() throws Exception {
        WorkspaceResponseDto wsA = createNewWorkspace("Workspace A");
        WorkspaceResponseDto wsB = createNewWorkspace("Workspace B");

        CreateEnvironmentDto envDto = CreateEnvironmentDto.builder()
                .name("Workspace A Isolated Env")
                .isActive(true)
                .variables(List.of(
                        EnvironmentVariableDto.builder().key("secretKey").value("ws-a-secret").isSecret(true).isEnabled(true).build()
                ))
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/v1/environments")
                        .header("X-Workspace-Key", wsA.getWorkspaceKey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(envDto)))
                .andExpect(status().isCreated())
                .andReturn();

        EnvironmentResponseDto createdEnv = objectMapper.readValue(
                createResult.getResponse().getContentAsString(), EnvironmentResponseDto.class);

        // Workspace B cannot view Workspace A's environment
        mockMvc.perform(get("/api/v1/environments/" + createdEnv.getId())
                        .header("X-Workspace-Key", wsB.getWorkspaceKey()))
                .andExpect(status().isNotFound());

        // Workspace B cannot activate Workspace A's environment
        mockMvc.perform(patch("/api/v1/environments/" + createdEnv.getId() + "/activate")
                        .header("X-Workspace-Key", wsB.getWorkspaceKey()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Rate limiting: Exceeding workspace creation threshold triggers HTTP 429 Too Many Requests")
    void testWorkspaceCreationRateLimit() throws Exception {
        // Workspace creation is limited to 10/min per IP
        for (int i = 0; i < 10; i++) {
            createNewWorkspace("Bulk WS " + i);
        }

        // 11th request from same IP should be blocked with 429
        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateWorkspaceDto("Overlimit WS"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.error").value("Too Many Requests"));
    }
}
