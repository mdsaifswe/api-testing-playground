package com.playground.api.service;

import com.playground.api.dto.request.CreateEnvironmentDto;
import com.playground.api.dto.request.EnvironmentVariableDto;
import com.playground.api.dto.request.UpdateEnvironmentDto;
import com.playground.api.dto.response.EnvironmentResponseDto;
import com.playground.api.entity.EnvironmentEntity;
import com.playground.api.entity.EnvironmentVariableEntity;
import com.playground.api.exception.ResourceNotFoundException;
import com.playground.api.repository.EnvironmentRepository;
import com.playground.api.repository.EnvironmentVariableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;
    private final EnvironmentVariableRepository variableRepository;

    @Transactional(readOnly = true)
    public List<EnvironmentResponseDto> getAllEnvironments() {
        return environmentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EnvironmentResponseDto getEnvironmentById(Long id) {
        EnvironmentEntity entity = environmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));
        return mapToDto(entity);
    }

    @Transactional(readOnly = true)
    public EnvironmentResponseDto getActiveEnvironment() {
        return environmentRepository.findByIsActiveTrue()
                .map(this::mapToDto)
                .orElse(null);
    }

    @Transactional
    public EnvironmentResponseDto createEnvironment(CreateEnvironmentDto dto) {
        if (Boolean.TRUE.equals(dto.getIsActive())) {
            environmentRepository.deactivateAll();
        }

        EnvironmentEntity entity = EnvironmentEntity.builder()
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
        EnvironmentEntity entity = environmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));

        if (Boolean.TRUE.equals(dto.getIsActive()) && !Boolean.TRUE.equals(entity.getIsActive())) {
            environmentRepository.deactivateAll();
            entity.setIsActive(true);
        } else if (Boolean.FALSE.equals(dto.getIsActive())) {
            entity.setIsActive(false);
        }

        entity.setName(dto.getName());

        // Replace variables
        variableRepository.deleteByEnvironmentId(id);
        List<EnvironmentVariableEntity> newVars = new ArrayList<>();
        if (dto.getVariables() != null) {
            for (EnvironmentVariableDto varDto : dto.getVariables()) {
                if (varDto.getKey() != null && !varDto.getKey().trim().isEmpty()) {
                    newVars.add(EnvironmentVariableEntity.builder()
                            .environment(entity)
                            .key(varDto.getKey().trim())
                            .value(varDto.getValue())
                            .isSecret(varDto.isSecret())
                            .isEnabled(varDto.isEnabled())
                            .build());
                }
            }
        }
        variableRepository.saveAll(newVars);
        entity.setVariables(newVars);

        EnvironmentEntity saved = environmentRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public EnvironmentResponseDto activateEnvironment(Long id) {
        EnvironmentEntity entity = environmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment not found with id: " + id));

        environmentRepository.deactivateAll();
        entity.setIsActive(true);
        EnvironmentEntity saved = environmentRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public void deleteEnvironment(Long id) {
        if (!environmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Environment not found with id: " + id);
        }
        environmentRepository.deleteById(id);
    }

    public EnvironmentResponseDto mapToDto(EnvironmentEntity entity) {
        List<EnvironmentVariableDto> varDtos = entity.getVariables() != null
                ? entity.getVariables().stream().map(v -> EnvironmentVariableDto.builder()
                        .id(v.getId())
                        .key(v.getKey())
                        .value(v.getValue())
                        .isSecret(Boolean.TRUE.equals(v.getIsSecret()))
                        .isEnabled(Boolean.TRUE.equals(v.getIsEnabled()))
                        .build())
                .collect(Collectors.toList())
                : Collections.emptyList();

        return EnvironmentResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .isActive(entity.getIsActive())
                .variables(varDtos)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
