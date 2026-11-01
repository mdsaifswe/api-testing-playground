package com.playground.api.service;

import com.playground.api.dto.request.CreateWorkspaceDto;
import com.playground.api.dto.response.WorkspaceResponseDto;
import com.playground.api.entity.*;
import com.playground.api.exception.UnauthorizedException;
import com.playground.api.repository.*;
import com.playground.api.security.WorkspaceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final CollectionRepository collectionRepository;
    private final SavedRequestRepository savedRequestRepository;
    private final EnvironmentRepository environmentRepository;
    private final EnvironmentVariableRepository environmentVariableRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public WorkspaceResponseDto createWorkspace(CreateWorkspaceDto dto) {
        String name = (dto != null && dto.getName() != null && !dto.getName().trim().isEmpty())
                ? dto.getName().trim()
                : "My Workspace";

        String rawKey = generateRawKey();
        String keyHash = hashKey(rawKey);

        WorkspaceEntity workspace = WorkspaceEntity.builder()
                .name(name)
                .keyHash(keyHash)
                .build();

        WorkspaceEntity saved = workspaceRepository.save(workspace);
        seedStarterData(saved);

        log.info("Created new isolated workspace id: {}", saved.getId());

        return WorkspaceResponseDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .workspaceKey(rawKey) // Returned only once on initial creation
                .createdAt(saved.getCreatedAt())
                .build();
    }

    public WorkspaceEntity resolveWorkspace(String rawKey) {
        if (rawKey == null || rawKey.trim().isEmpty()) {
            throw new UnauthorizedException("Missing required workspace credential.");
        }

        String trimmed = rawKey.trim();
        if (!trimmed.startsWith("wpk_") || trimmed.length() < 36) {
            throw new UnauthorizedException("Invalid workspace credential format.");
        }

        String keyHash = hashKey(trimmed);
        return workspaceRepository.findByKeyHash(keyHash)
                .orElseThrow(() -> new UnauthorizedException("Workspace credential not recognized or expired."));
    }

    public WorkspaceResponseDto getCurrentWorkspaceDto() {
        WorkspaceEntity current = WorkspaceContextHolder.requireCurrent();
        return WorkspaceResponseDto.builder()
                .id(current.getId())
                .name(current.getName())
                .createdAt(current.getCreatedAt())
                .build();
    }

    public String generateRawKey() {
        byte[] randomBytes = new byte[32]; // 256 bits of cryptographic entropy
        secureRandom.nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder("wpk_");
        for (byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String hashKey(String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawKey.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private void seedStarterData(WorkspaceEntity workspace) {
        // 1. Starter Collection & Saved Requests
        CollectionEntity collection = CollectionEntity.builder()
                .workspace(workspace)
                .name("Starter API Collection")
                .description("Sample collection containing common REST API requests to get started quickly.")
                .build();
        CollectionEntity savedCollection = collectionRepository.save(collection);

        List<SavedRequestEntity> requests = List.of(
                SavedRequestEntity.builder()
                        .collection(savedCollection)
                        .workspace(workspace)
                        .name("Echo GET with Params")
                        .method("GET")
                        .url("{{baseUrl}}/get?test=true&platform=playground")
                        .queryParams("[{\"key\":\"test\",\"value\":\"true\",\"enabled\":true},{\"key\":\"platform\",\"value\":\"playground\",\"enabled\":true}]")
                        .headers("[{\"key\":\"Accept\",\"value\":\"application/json\",\"enabled\":true}]")
                        .authType("NONE")
                        .authConfig("{}")
                        .build(),
                SavedRequestEntity.builder()
                        .collection(savedCollection)
                        .workspace(workspace)
                        .name("Echo Authenticated Endpoint")
                        .method("GET")
                        .url("{{baseUrl}}/auth-check")
                        .queryParams("[]")
                        .headers("[]")
                        .authType("API_KEY")
                        .authConfig("{\"keyName\":\"X-API-Key\",\"keyValue\":\"{{apiKey}}\",\"placement\":\"HEADER\"}")
                        .build(),
                SavedRequestEntity.builder()
                        .collection(savedCollection)
                        .workspace(workspace)
                        .name("Echo POST JSON Body")
                        .method("POST")
                        .url("{{baseUrl}}/post")
                        .queryParams("[]")
                        .headers("[{\"key\":\"Content-Type\",\"value\":\"application/json\",\"enabled\":true}]")
                        .authType("NONE")
                        .authConfig("{}")
                        .body("{\n  \"message\": \"Hello from API Testing Playground!\",\n  \"userId\": \"{{demoUserId}}\",\n  \"timestamp\": 1730000000000\n}")
                        .build()
        );
        savedRequestRepository.saveAll(requests);

        // 2. Starter Environments & Variables
        EnvironmentEntity localEcho = EnvironmentEntity.builder()
                .workspace(workspace)
                .name("Local Echo Server")
                .isActive(true)
                .build();
        EnvironmentEntity savedEcho = environmentRepository.save(localEcho);

        List<EnvironmentVariableEntity> echoVars = List.of(
                EnvironmentVariableEntity.builder()
                        .environment(savedEcho)
                        .key("baseUrl")
                        .value("http://localhost:8080/api/v1/echo")
                        .isSecret(false)
                        .isEnabled(true)
                        .build(),
                EnvironmentVariableEntity.builder()
                        .environment(savedEcho)
                        .key("apiKey")
                        .value("playground-secret-token-xyz123")
                        .isSecret(true)
                        .isEnabled(true)
                        .build(),
                EnvironmentVariableEntity.builder()
                        .environment(savedEcho)
                        .key("demoUserId")
                        .value("usr_998877")
                        .isSecret(false)
                        .isEnabled(true)
                        .build()
        );
        environmentVariableRepository.saveAll(echoVars);

        EnvironmentEntity prodDemo = EnvironmentEntity.builder()
                .workspace(workspace)
                .name("Production Demo")
                .isActive(false)
                .build();
        EnvironmentEntity savedProd = environmentRepository.save(prodDemo);

        List<EnvironmentVariableEntity> prodVars = List.of(
                EnvironmentVariableEntity.builder()
                        .environment(savedProd)
                        .key("baseUrl")
                        .value("https://httpbin.org")
                        .isSecret(false)
                        .isEnabled(true)
                        .build(),
                EnvironmentVariableEntity.builder()
                        .environment(savedProd)
                        .key("apiKey")
                        .value("prod-vault-key-abc987")
                        .isSecret(true)
                        .isEnabled(true)
                        .build()
        );
        environmentVariableRepository.saveAll(prodVars);
    }
}
