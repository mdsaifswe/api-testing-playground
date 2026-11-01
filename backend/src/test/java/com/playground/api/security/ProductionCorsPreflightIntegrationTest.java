package com.playground.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.api.dto.request.CreateWorkspaceDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "playground.cors.allowed-origins=https://api-testing-playground-a36.pages.dev,http://localhost:5173"
})
@AutoConfigureMockMvc
class ProductionCorsPreflightIntegrationTest {

    private static final String PROD_FRONTEND_ORIGIN = "https://api-testing-playground-a36.pages.dev";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Production CORS: Preflight OPTIONS request to /api/v1/workspaces from Cloudflare Pages origin returns 200 OK")
    void testPreflightWorkspacesFromProductionOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/workspaces")
                        .header("Origin", PROD_FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization,X-Workspace-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", PROD_FRONTEND_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("OPTIONS")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("X-Workspace-Key")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Content-Type")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("Authorization")));
    }

    @Test
    @DisplayName("Production CORS: Preflight OPTIONS request to /api/v1/execute from Cloudflare Pages origin returns 200 OK")
    void testPreflightExecuteFromProductionOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/execute")
                        .header("Origin", PROD_FRONTEND_ORIGIN)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,Authorization,X-Workspace-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", PROD_FRONTEND_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("OPTIONS")))
                .andExpect(header().string("Access-Control-Allow-Headers", containsString("X-Workspace-Key")));
    }

    @Test
    @DisplayName("Production CORS: Preflight OPTIONS to /api/v1/collections supports GET, POST, PUT, PATCH, DELETE")
    void testPreflightCollectionsAllMethods() throws Exception {
        for (String method : new String[]{"GET", "POST", "PUT", "PATCH", "DELETE"}) {
            mockMvc.perform(options("/api/v1/collections")
                            .header("Origin", PROD_FRONTEND_ORIGIN)
                            .header("Access-Control-Request-Method", method)
                            .header("Access-Control-Request-Headers", "Content-Type,Authorization,X-Workspace-Key"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", PROD_FRONTEND_ORIGIN))
                    .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
        }
    }

    @Test
    @DisplayName("Production CORS: Preflight OPTIONS from unauthorized domain returns 403 Forbidden")
    void testPreflightFromUnauthorizedDomainRejected() throws Exception {
        mockMvc.perform(options("/api/v1/workspaces")
                        .header("Origin", "https://malicious-untrusted-site.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,X-Workspace-Key"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));

        mockMvc.perform(options("/api/v1/execute")
                        .header("Origin", "https://malicious-untrusted-site.com")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type,X-Workspace-Key"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("Security Preservation: POST /api/v1/execute without X-Workspace-Key still returns 401 Unauthorized with CORS headers")
    void testProtectedEndpointStillEnforcesSecurity() throws Exception {
        mockMvc.perform(post("/api/v1/execute")
                        .header("Origin", PROD_FRONTEND_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"GET\",\"url\":\"https://example.com\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value(containsString("Missing required header: X-Workspace-Key")))
                .andExpect(header().string("Access-Control-Allow-Origin", PROD_FRONTEND_ORIGIN));
    }

    @Test
    @DisplayName("Public Workspace Creation: POST /api/v1/workspaces succeeds with 201 Created and CORS headers")
    void testPublicWorkspaceCreationWithCorsHeaders() throws Exception {
        CreateWorkspaceDto dto = new CreateWorkspaceDto("Prod Cloudflare WS");
        mockMvc.perform(post("/api/v1/workspaces")
                        .header("Origin", PROD_FRONTEND_ORIGIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Access-Control-Allow-Origin", PROD_FRONTEND_ORIGIN))
                .andExpect(jsonPath("$.name").value("Prod Cloudflare WS"))
                .andExpect(jsonPath("$.workspaceKey").value(startsWith("wpk_")));
    }
}
