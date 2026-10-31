package com.playground.api.service;

import com.playground.api.dto.response.RequestHistoryResponseDto;
import com.playground.api.entity.RequestHistoryEntity;
import com.playground.api.exception.ResourceNotFoundException;
import com.playground.api.repository.RequestHistoryRepository;
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
        return historyRepository.findAllByOrderByExecutedAtDesc(PageRequest.of(page, size))
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<RequestHistoryResponseDto> getRecentHistory() {
        return historyRepository.findTop50ByOrderByExecutedAtDesc()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RequestHistoryResponseDto getHistoryById(Long id) {
        RequestHistoryEntity entity = historyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("History record not found with id: " + id));
        return mapToDto(entity);
    }

    @Transactional
    public void deleteHistoryById(Long id) {
        if (!historyRepository.existsById(id)) {
            throw new ResourceNotFoundException("History record not found with id: " + id);
        }
        historyRepository.deleteById(id);
    }

    @Transactional
    public void clearHistory() {
        historyRepository.deleteAll();
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
