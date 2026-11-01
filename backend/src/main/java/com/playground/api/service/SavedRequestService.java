package com.playground.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.api.dto.request.SaveRequestDto;
import com.playground.api.dto.response.SavedRequestResponseDto;
import com.playground.api.entity.CollectionEntity;
import com.playground.api.entity.SavedRequestEntity;
import com.playground.api.entity.WorkspaceEntity;
import com.playground.api.exception.ResourceNotFoundException;
import com.playground.api.repository.CollectionRepository;
import com.playground.api.repository.SavedRequestRepository;
import com.playground.api.security.WorkspaceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SavedRequestService {

    private final SavedRequestRepository savedRequestRepository;
    private final CollectionRepository collectionRepository;
    private final CollectionService collectionService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<SavedRequestResponseDto> getAllRequests() {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        return savedRequestRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspace.getId())
                .stream()
                .map(collectionService::mapRequestToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SavedRequestResponseDto> getRequestsByCollection(Long collectionId) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        return savedRequestRepository.findByCollectionIdAndWorkspaceIdOrderByCreatedAtDesc(collectionId, workspace.getId())
                .stream()
                .map(collectionService::mapRequestToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SavedRequestResponseDto getRequestById(Long id) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        SavedRequestEntity entity = savedRequestRepository.findByIdAndWorkspaceId(id, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Saved request not found with id: " + id));
        return collectionService.mapRequestToDto(entity);
    }

    @Transactional
    public SavedRequestResponseDto saveRequest(SaveRequestDto dto) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        CollectionEntity collection = null;
        if (dto.getCollectionId() != null) {
            collection = collectionRepository.findByIdAndWorkspaceId(dto.getCollectionId(), workspace.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + dto.getCollectionId()));
        }

        String queryParamsJson = serialize(dto.getQueryParams());
        String headersJson = serialize(dto.getHeaders());
        String authConfigJson = serialize(dto.getAuth());
        String authType = dto.getAuth() != null && dto.getAuth().getType() != null
                ? dto.getAuth().getType()
                : "NONE";

        SavedRequestEntity entity = SavedRequestEntity.builder()
                .workspace(workspace)
                .collection(collection)
                .name(dto.getName())
                .method(dto.getMethod().toUpperCase())
                .url(dto.getUrl())
                .queryParams(queryParamsJson)
                .headers(headersJson)
                .authType(authType)
                .authConfig(authConfigJson)
                .body(dto.getBody())
                .build();

        SavedRequestEntity saved = savedRequestRepository.save(entity);
        return collectionService.mapRequestToDto(saved);
    }

    @Transactional
    public SavedRequestResponseDto updateRequest(Long id, SaveRequestDto dto) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        SavedRequestEntity entity = savedRequestRepository.findByIdAndWorkspaceId(id, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Saved request not found with id: " + id));

        if (dto.getCollectionId() != null) {
            CollectionEntity collection = collectionRepository.findByIdAndWorkspaceId(dto.getCollectionId(), workspace.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + dto.getCollectionId()));
            entity.setCollection(collection);
        } else {
            entity.setCollection(null);
        }

        entity.setName(dto.getName());
        entity.setMethod(dto.getMethod().toUpperCase());
        entity.setUrl(dto.getUrl());
        entity.setQueryParams(serialize(dto.getQueryParams()));
        entity.setHeaders(serialize(dto.getHeaders()));
        if (dto.getAuth() != null) {
            entity.setAuthType(dto.getAuth().getType() != null ? dto.getAuth().getType() : "NONE");
            entity.setAuthConfig(serialize(dto.getAuth()));
        }
        entity.setBody(dto.getBody());

        SavedRequestEntity updated = savedRequestRepository.save(entity);
        return collectionService.mapRequestToDto(updated);
    }

    @Transactional
    public SavedRequestResponseDto duplicateRequest(Long id) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        SavedRequestEntity original = savedRequestRepository.findByIdAndWorkspaceId(id, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Saved request not found with id: " + id));

        SavedRequestEntity clone = SavedRequestEntity.builder()
                .workspace(workspace)
                .collection(original.getCollection())
                .name(original.getName() + " (Copy)")
                .method(original.getMethod())
                .url(original.getUrl())
                .queryParams(original.getQueryParams())
                .headers(original.getHeaders())
                .authType(original.getAuthType())
                .authConfig(original.getAuthConfig())
                .body(original.getBody())
                .build();

        SavedRequestEntity saved = savedRequestRepository.save(clone);
        return collectionService.mapRequestToDto(saved);
    }

    @Transactional
    public void deleteRequest(Long id) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        SavedRequestEntity entity = savedRequestRepository.findByIdAndWorkspaceId(id, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Saved request not found with id: " + id));
        savedRequestRepository.delete(entity);
    }

    private String serialize(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Serialization error: {}", e.getMessage());
            return null;
        }
    }
}
