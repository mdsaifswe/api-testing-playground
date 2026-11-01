package com.playground.api.service;

import com.playground.api.dto.response.RequestHistoryResponseDto;
import com.playground.api.entity.RequestHistoryEntity;
import com.playground.api.entity.WorkspaceEntity;
import com.playground.api.exception.ResourceNotFoundException;
import com.playground.api.repository.RequestHistoryRepository;
import com.playground.api.security.WorkspaceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryService {

    private final RequestHistoryRepository historyRepository;

    @Transactional(readOnly = true)
    public Page<RequestHistoryResponseDto> getHistoryPaged(int page, int size) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        return historyRepository.findByWorkspaceIdOrderByExecutedAtDesc(workspace.getId(), PageRequest.of(page, size))
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<RequestHistoryResponseDto> getRecentHistory() {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        return historyRepository.findTop50ByWorkspaceIdOrderByExecutedAtDesc(workspace.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RequestHistoryResponseDto getHistoryById(Long id) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        RequestHistoryEntity entity = historyRepository.findByIdAndWorkspaceId(id, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("History record not found with id: " + id));
        return mapToDto(entity);
    }

    @Transactional
    public void deleteHistoryById(Long id) {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        RequestHistoryEntity entity = historyRepository.findByIdAndWorkspaceId(id, workspace.getId())
                .orElseThrow(() -> new ResourceNotFoundException("History record not found with id: " + id));
        historyRepository.delete(entity);
    }

    @Transactional
    public void clearHistory() {
        WorkspaceEntity workspace = WorkspaceContextHolder.requireCurrent();
        historyRepository.deleteByWorkspaceId(workspace.getId());
    }

    public RequestHistoryResponseDto mapToDto(RequestHistoryEntity entity) {
        return RequestHistoryResponseDto.builder()
                .id(entity.getId())
                .method(entity.getMethod())
                .url(entity.getUrl())
                .requestHeaders(entity.getRequestHeaders())
                .requestBody(entity.getRequestBody())
                .responseStatus(entity.getResponseStatus())
                .responseStatusText(entity.getResponseStatusText())
                .responseHeaders(entity.getResponseHeaders())
                .responseBody(entity.getResponseBody())
                .responseSize(entity.getResponseSize())
                .durationMs(entity.getDurationMs())
                .errorCategory(entity.getErrorCategory())
                .executedAt(entity.getExecutedAt())
                .build();
    }
}
