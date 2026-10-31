package com.playground.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.api.dto.common.AuthDto;
import com.playground.api.dto.common.KeyValueDto;
import com.playground.api.dto.request.CreateCollectionDto;
import com.playground.api.dto.request.UpdateCollectionDto;
import com.playground.api.dto.response.CollectionResponseDto;
import com.playground.api.dto.response.SavedRequestResponseDto;
import com.playground.api.entity.CollectionEntity;
import com.playground.api.entity.SavedRequestEntity;
import com.playground.api.exception.ResourceNotFoundException;
import com.playground.api.repository.CollectionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<CollectionResponseDto> getAllCollections() {
        return collectionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CollectionResponseDto getCollectionById(Long id) {
        CollectionEntity entity = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + id));
        return mapToDto(entity);
    }

    @Transactional
    public CollectionResponseDto createCollection(CreateCollectionDto dto) {
        CollectionEntity entity = CollectionEntity.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        CollectionEntity saved = collectionRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public CollectionResponseDto updateCollection(Long id, UpdateCollectionDto dto) {
        CollectionEntity entity = collectionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + id));

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        CollectionEntity updated = collectionRepository.save(entity);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteCollection(Long id) {
        if (!collectionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Collection not found with id: " + id);
        }
        collectionRepository.deleteById(id);
    }

    public CollectionResponseDto mapToDto(CollectionEntity entity) {
        List<SavedRequestResponseDto> requestDtos = entity.getRequests() != null
                ? entity.getRequests().stream().map(this::mapRequestToDto).collect(Collectors.toList())
                : Collections.emptyList();

        return CollectionResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .requests(requestDtos)
                .requestCount(requestDtos.size())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    public SavedRequestResponseDto mapRequestToDto(SavedRequestEntity req) {
        List<KeyValueDto> queryParams = deserializeList(req.getQueryParams(), new TypeReference<List<KeyValueDto>>() {});
        List<KeyValueDto> headers = deserializeList(req.getHeaders(), new TypeReference<List<KeyValueDto>>() {});
        AuthDto authConfig = deserializeObject(req.getAuthConfig(), AuthDto.class);

        return SavedRequestResponseDto.builder()
                .id(req.getId())
                .collectionId(req.getCollection() != null ? req.getCollection().getId() : null)
                .name(req.getName())
                .method(req.getMethod())
                .url(req.getUrl())
                .queryParams(queryParams)
                .headers(headers)
                .authType(req.getAuthType())
                .authConfig(authConfig)
                .body(req.getBody())
                .createdAt(req.getCreatedAt())
                .updatedAt(req.getUpdatedAt())
                .build();
    }

    private <T> List<T> deserializeList(String json, TypeReference<List<T>> typeRef) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private <T> T deserializeObject(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }
}
