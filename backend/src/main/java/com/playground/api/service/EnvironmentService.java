package com.playground.api.service;

import com.playground.api.dto.request.CreateEnvironmentDto;
import com.playground.api.dto.request.EnvironmentVariableDto;
import com.playground.api.dto.request.UpdateEnvironmentDto;
import com.playground.api.dto.response.EnvironmentResponseDto;
import com.playground.api.entity.EnvironmentEntity;
import com.playground.api.entity.EnvironmentVariableEntity;
import com.playground.api.entity.WorkspaceEntity;
import com.playground.api.exception.ResourceNotFoundException;
import com.playground.api.repository.EnvironmentRepository;
import com.playground.api.repository.EnvironmentVariableRepository;
import com.playground.api.security.WorkspaceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentService {

    public static final String SECRET_MASK = "********";

    private final EnvironmentRepository environmentRepository;
    private final EnvironmentVariableRepository variableRepository;

    @Transactional(readOnly = true)
    public List<EnvironmentResponseDto> getAllEnvironments() {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        return environmentRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspace.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EnvironmentResponseDto getEnvironmentById(Long id) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        EnvironmentEntity entity = environmentRepository.findByIdAndWorkspaceId(id, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));
        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public EnvironmentResponseDto getActiveEnvironment() {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        return environmentRepository.findByWorkspaceIdAndIsActiveTrue(workspace.getId())
                .map(this::mapToDto)
                .orElse(null);
    }

    @Transactional
    public EnvironmentResponseDto createEnvironment(CreateEnvironmentDto dto) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        if (Boolean.TRUE.equals(dto.getIsActive())) {
            environmentRepository.deactivateAllByWorkspaceId(workspace.getId());
        }

        EnvironmentEntity entity = EnvironmentEntity.builder()
                .workspace(workspace)
                .name(dto.getName())
                .isActive(Boolean.TRUE.equals(dto.getIsActive()))
                .build();

        EnvironmentEntity saved = environmentRepository.save(entity);

        if (dto.getVariables() != null && !dto.getVariables().isEmpty()) {
            List<EnvironmentVariableEntity> vars = new ArrayList<>();
            for (EnvironmentVariableDto varDto : dto.getVariables()) {
                if (varDto.getKey() != null && !varDto.getKey().trim().isEmpty()) {
                    vars.add(EnvironmentVariableEntity.builder()
                            .environment(saved)
                            .key(varDto.getKey().trim())
                            .value(varDto.getValue())
                            .isSecret(varDto.isSecret())
                            .isEnabled(varDto.isEnabled())
                            .build());
                }
            }
            variableRepository.saveAll(vars);
            saved.setVariables(vars);
        }

        return mapToDto(saved);
    }

    @Transactional
    public EnvironmentResponseDto updateEnvironment(Long id, UpdateEnvironmentDto dto) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        EnvironmentEntity entity = environmentRepository.findByIdAndWorkspaceId(id, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));

        if (Boolean.TRUE.equals(dto.getIsActive()) && !Boolean.TRUE.equals(entity.getIsActive())) {
            environmentRepository.deactivateAllByWorkspaceId(workspace.getId());
            entity.setIsActive(true);
        } else if (Boolean.FALSE.equals(dto.getIsActive())) {
            entity.setIsActive(false);
        }

        entity.setName(dto.getName());

        // Map existing variables by ID and Key for secret preservation
        Map<Long, EnvironmentVariableEntity> existingById = new HashMap<>();
        Map<String, EnvironmentVariableEntity> existingByKey = new HashMap<>();
        if (entity.getVariables() != null) {
            for (EnvironmentVariableEntity existing : entity.getVariables()) {
                if (existing.getId() != null) {
                    existingById.put(existing.getId(), existing);
                }
                if (existing.getKey() != null) {
                    existingByKey.put(existing.getKey().trim(), existing);
                }
            }
        }

        // Replace variables - update collection in-place to preserve Hibernate PersistentBag reference with orphanRemoval
        List<EnvironmentVariableEntity> newVars = new ArrayList<>();
        if (dto.getVariables() != null) {
            for (EnvironmentVariableDto varDto : dto.getVariables()) {
                if (varDto.getKey() != null && !varDto.getKey().trim().isEmpty()) {
                    String targetKey = varDto.getKey().trim();
                    boolean isSecret = varDto.isSecret();
                    String submittedValue = varDto.getValue();

                    EnvironmentVariableEntity existingVar = null;
                    if (varDto.getId() != null) {
                        existingVar = existingById.get(varDto.getId());
                    }
                    if (existingVar == null) {
                        existingVar = existingByKey.get(targetKey);
                    }

                    String finalValue = submittedValue;
                    if (isSecret && existingVar != null) {
                        // Preserve previously stored secret if masked placeholder or blank is submitted
                        if (SECRET_MASK.equals(submittedValue) || submittedValue == null || submittedValue.trim().isEmpty()) {
                            finalValue = existingVar.getValue();
                        }
                    }

                    newVars.add(EnvironmentVariableEntity.builder()
                            .environment(entity)
                            .key(targetKey)
                            .value(finalValue)
                            .isSecret(isSecret)
                            .isEnabled(varDto.isEnabled())
                            .build());
                }
            }
        }

        if (entity.getVariables() != null) {
            entity.getVariables().clear();
            entity.getVariables().addAll(newVars);
        } else {
            entity.setVariables(new ArrayList<>(newVars));
        }

        variableRepository.saveAll(newVars);
        EnvironmentEntity saved = environmentRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public EnvironmentResponseDto activateEnvironment(Long id) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        EnvironmentEntity entity = environmentRepository.findByIdAndWorkspaceId(id, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));

        environmentRepository.deactivateAllByWorkspaceId(workspace.getId());
        entity.setIsActive(true);
        EnvironmentEntity saved = environmentRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteEnvironment(Long id) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        EnvironmentEntity entity = environmentRepository.findByIdAndWorkspaceId(id, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));
        environmentRepository.delete(entity);
    }

    public EnvironmentResponseDto mapToDto(EnvironmentEntity entity) {
        List<EnvironmentVariableDto> varDtos = entity.getVariables() != null
                ? entity.getVariables().stream().map(v -> {
                    boolean isSecret = Boolean.TRUE.equals(v.getIsSecret());
                    String displayVal = isSecret ? SECRET_MASK : v.getValue();
                    return EnvironmentVariableDto.builder()
                            .id(v.getId())
                            .key(v.getKey())
                            .value(displayVal)
                            .isSecret(isSecret)
                            .isEnabled(Boolean.TRUE.equals(v.getIsEnabled()))
                            .build();
                })
                .collect(Collectors.toList())
                : Collections.emptyList();

        return EnvironmentResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .isActive(Boolean.TRUE.equals(entity.getIsActive()))
                .variables(varDtos)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
