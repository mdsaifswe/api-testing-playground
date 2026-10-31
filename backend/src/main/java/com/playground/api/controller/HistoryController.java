package com.playground.api.controller;

import com.playground.api.dto.response.RequestHistoryResponseDto;
import com.playground.api.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
@Tag(name = "History", description = "Query, paginate, and manage persisted request execution history")
public class HistoryController {

    private final HistoryService historyService;

    @GetMapping
    @Operation(summary = "Get request execution history", description = "Returns paginated history or recent list")
    public ResponseEntity<?> getHistory(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {

        if (page != null) {
            Page<RequestHistoryResponseDto> paged = historyService.getHistoryPaged(page, size);
            return ResponseEntity.ok(paged);
        }
        List<RequestHistoryResponseDto> recent = historyService.getRecentHistory();
        return ResponseEntity.ok(recent);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get history item by ID", description = "Retrieves execution details for a specific history record")
    public ResponseEntity<RequestHistoryResponseDto> getHistoryById(@PathVariable Long id) {
        return ResponseEntity.ok(historyService.getHistoryById(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete history item", description = "Deletes a specific history record by ID")
    public ResponseEntity<Void> deleteHistoryById(@PathVariable Long id) {
        historyService.deleteHistoryById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    @Operation(summary = "Clear all history", description = "Deletes all request execution history records")
    public ResponseEntity<Void> clearHistory() {
        historyService.clearHistory();
        return ResponseEntity.noContent().build();
    }
}
