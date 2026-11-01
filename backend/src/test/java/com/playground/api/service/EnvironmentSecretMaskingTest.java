package com.playground.api.service;

import com.playground.api.dto.request.CreateEnvironmentDto;
import com.playground.api.dto.request.EnvironmentVariableDto;
import com.playground.api.dto.request.UpdateEnvironmentDto;
import com.playground.api.dto.response.EnvironmentResponseDto;
import com.playground.api.entity.EnvironmentEntity;
import com.playground.api.entity.EnvironmentVariableEntity;
import com.playground.api.repository.EnvironmentRepository;
import com.playground.api.repository.EnvironmentVariableRepository;
import com.playground.api.entity.WorkspaceEntity;
import com.playground.api.security.WorkspaceContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvironmentSecretMaskingTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    @Mock
    private EnvironmentVariableRepository variableRepository;

    @InjectMocks
    private EnvironmentService environmentService;

    private WorkspaceEntity testWorkspace;
    private EnvironmentEntity sampleEnv;
    private EnvironmentVariableEntity secretVar;
    private EnvironmentVariableEntity normalVar;

    private static final String REAL_SECRET_TOKEN = "super-secret-token-xyz-987654321";
    private static final String PUBLIC_BASE_URL = "https://api.example.com";

    @BeforeEach
    void setUp() {
        testWorkspace = WorkspaceEntity.builder()
                .id(1L)
                .name("Test Workspace")
                .keyHash("hash123")
                .build();
        WorkspaceContextHolder.set(testWorkspace);

        sampleEnv = EnvironmentEntity.builder()
                .id(1L)
                .workspace(testWorkspace)
                .name("Production Environment")
                .isActive(true)
                .build();

        secretVar = EnvironmentVariableEntity.builder()
                .id(101L)
                .environment(sampleEnv)
                .key("apiKey")
                .value(REAL_SECRET_TOKEN)
                .isSecret(true)
                .isEnabled(true)
                .build();

        normalVar = EnvironmentVariableEntity.builder()
                .id(102L)
                .environment(sampleEnv)
                .key("baseUrl")
                .value(PUBLIC_BASE_URL)
                .isSecret(false)
                .isEnabled(true)
                .build();

        sampleEnv.setVariables(new ArrayList<>(List.of(secretVar, normalVar)));
    }

    @AfterEach
    void tearDown() {
        WorkspaceContextHolder.clear();
    }

    @Test
    @DisplayName("Secret masking: mapToDto must never disclose secret values and return ******** placeholder")
    void testMapToDtoMasksSecretValues() {
        EnvironmentResponseDto dto = environmentService.mapToDto(sampleEnv);

        assertNotNull(dto);
        assertEquals(2, dto.getVariables().size());

        EnvironmentVariableDto secretDto = dto.getVariables().stream()
                .filter(EnvironmentVariableDto::isSecret)
                .findFirst()
                .orElseThrow();

        // Must NOT contain real secret
        assertNotEquals(REAL_SECRET_TOKEN, secretDto.getValue(), "Real secret value must NEVER be returned in DTO");
        assertEquals(EnvironmentService.SECRET_MASK, secretDto.getValue(), "Masked placeholder ******** must be returned for secret");
        assertTrue(secretDto.isSecret());

        EnvironmentVariableDto publicDto = dto.getVariables().stream()
                .filter(v -> !v.isSecret())
                .findFirst()
                .orElseThrow();

        assertEquals(PUBLIC_BASE_URL, publicDto.getValue(), "Non-secret value must be preserved as-is");
    }

    @Test
    @DisplayName("Create Environment: Secret value must be masked in response while real value is saved to database")
    void testCreateEnvironmentMasksSecretInResponse() {
        CreateEnvironmentDto createDto = CreateEnvironmentDto.builder()
                .name("New Secret Env")
                .isActive(false)
                .variables(List.of(
                        EnvironmentVariableDto.builder().key("secretKey").value("super-private-key-456").isSecret(true).isEnabled(true).build(),
                        EnvironmentVariableDto.builder().key("endpoint").value("https://example.com/v1").isSecret(false).isEnabled(true).build()
                ))
                .build();

        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> {
            EnvironmentEntity e = invocation.getArgument(0);
            e.setId(2L);
            return e;
        });

        EnvironmentResponseDto response = environmentService.createEnvironment(createDto);

        assertNotNull(response);
        EnvironmentVariableDto secretDto = response.getVariables().stream()
                .filter(EnvironmentVariableDto::isSecret)
                .findFirst()
                .orElseThrow();

        assertEquals(EnvironmentService.SECRET_MASK, secretDto.getValue(), "Secret must be masked in create response");

        // Verify that variableRepository received the real unmasked secret for persistence
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EnvironmentVariableEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(variableRepository).saveAll(captor.capture());

        List<EnvironmentVariableEntity> savedEntities = captor.getValue();
        EnvironmentVariableEntity savedSecret = savedEntities.stream()
                .filter(EnvironmentVariableEntity::getIsSecret)
                .findFirst()
                .orElseThrow();

        assertEquals("super-private-key-456", savedSecret.getValue(), "Database must store the real secret value unmasked");
    }

    @Test
    @DisplayName("Update Environment: Submitting the mask placeholder ******** must preserve existing stored secret")
    void testUpdateWithMaskPlaceholderPreservesExistingSecret() {
        when(environmentRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(sampleEnv));
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEnvironmentDto updateDto = UpdateEnvironmentDto.builder()
                .name("Updated Env")
                .isActive(true)
                .variables(List.of(
                        // Client sends back the mask placeholder
                        EnvironmentVariableDto.builder().id(101L).key("apiKey").value(EnvironmentService.SECRET_MASK).isSecret(true).isEnabled(true).build(),
                        EnvironmentVariableDto.builder().id(102L).key("baseUrl").value("https://updated.example.com").isSecret(false).isEnabled(true).build()
                ))
                .build();

        environmentService.updateEnvironment(1L, updateDto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EnvironmentVariableEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(variableRepository).saveAll(captor.capture());

        EnvironmentVariableEntity preservedSecret = captor.getValue().stream()
                .filter(EnvironmentVariableEntity::getIsSecret)
                .findFirst()
                .orElseThrow();

        assertEquals(REAL_SECRET_TOKEN, preservedSecret.getValue(),
                "Submitting mask placeholder must preserve the original stored secret in the database");
    }

    @Test
    @DisplayName("Update Environment: Submitting empty or blank value for secret must preserve existing stored secret")
    void testUpdateWithEmptyOrBlankPreservesExistingSecret() {
        when(environmentRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(sampleEnv));
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEnvironmentDto updateDto = UpdateEnvironmentDto.builder()
                .name("Updated Env")
                .isActive(true)
                .variables(List.of(
                        // Client submits blank value for secret
                        EnvironmentVariableDto.builder().id(101L).key("apiKey").value("   ").isSecret(true).isEnabled(true).build()
                ))
                .build();

        environmentService.updateEnvironment(1L, updateDto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EnvironmentVariableEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(variableRepository).saveAll(captor.capture());

        EnvironmentVariableEntity preservedSecret = captor.getValue().get(0);
        assertEquals(REAL_SECRET_TOKEN, preservedSecret.getValue(),
                "Submitting blank value must preserve the original stored secret in the database");
    }

    @Test
    @DisplayName("Update Environment: Submitting a new non-empty value must replace the old secret")
    void testUpdateWithNewSecretReplacesOldSecret() {
        when(environmentRepository.findByIdAndWorkspaceId(1L, 1L)).thenReturn(Optional.of(sampleEnv));
        when(environmentRepository.save(any(EnvironmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String newSecret = "brand-new-secret-token-777";
        UpdateEnvironmentDto updateDto = UpdateEnvironmentDto.builder()
                .name("Updated Env")
                .isActive(true)
                .variables(List.of(
                        EnvironmentVariableDto.builder().id(101L).key("apiKey").value(newSecret).isSecret(true).isEnabled(true).build()
                ))
                .build();

        environmentService.updateEnvironment(1L, updateDto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<EnvironmentVariableEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(variableRepository).saveAll(captor.capture());

        EnvironmentVariableEntity updatedSecret = captor.getValue().get(0);
        assertEquals(newSecret, updatedSecret.getValue(), "Submitting a new secret value must replace the old secret");
    }

    @Test
    @DisplayName("Variable Substitution: Substitution service must resolve real unmasked secret, not the placeholder")
    void testVariableSubstitutionUsesRealSecret() {
        when(environmentRepository.findByWorkspaceIdAndIsActiveTrue(1L)).thenReturn(Optional.of(sampleEnv));
        VariableSubstitutionService substitutionService = new VariableSubstitutionService(environmentRepository);

        Map<String, String> varMap = substitutionService.getVariablesMap(null);
        assertEquals(REAL_SECRET_TOKEN, varMap.get("apiKey"), "Variable map must contain real secret for execution");
        assertNotEquals(EnvironmentService.SECRET_MASK, varMap.get("apiKey"));

        String input = "Bearer {{apiKey}}";
        String resolved = substitutionService.substitute(input, null);
        assertEquals("Bearer " + REAL_SECRET_TOKEN, resolved, "Substituted output must use the real stored secret");
        assertFalse(resolved.contains(EnvironmentService.SECRET_MASK));
    }
}
