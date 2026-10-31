package com.playground.api.controller;

import com.playground.api.dto.request.SaveRequestDto;
import com.playground.api.dto.response.SavedRequestResponseDto;
import com.playground.api.service.SavedRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/requests")
@RequiredArgsConstructor
@Tag(name = "Saved Requests", description = "Create, edit, duplicate, and delete saved requests")
public class SavedRequestController {

    private final SavedRequestService savedRequestService;

    @GetMapping
    @Operation(summary = "Get all saved requests", description = "Returns all saved requests or filters by collection ID")
    public ResponseEntity<List<SavedRequestResponseDto>> getRequests(
            @RequestParam(required = false) Long collectionId) {
        if (collectionId != null) {
            return ResponseEntity.ok(savedRequestService.getRequestsByCollection(collectionId));
        }
        return ResponseEntity.ok(savedRequestService.getAllRequests());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get request by ID", description = "Retrieves a single saved request")
    public ResponseEntity<SavedRequestResponseDto> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(savedRequestService.getRequestById(id));
    }

    @PostMapping
    @Operation(summary = "Save new request", description = "Saves a configured HTTP request")
    public ResponseEntity<SavedRequestResponseDto> saveRequest(@Valid @RequestBody SaveRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRequestService.saveRequest(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update saved request", description = "Updates an existing saved request")
    public ResponseEntity<SavedRequestResponseDto> updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody SaveRequestDto dto) {
        return ResponseEntity.ok(savedRequestService.updateRequest(id, dto));
    }

    @PostMapping("/{id}/duplicate")
    @Operation(summary = "Duplicate request", description = "Creates a duplicate copy of a saved request")
    public ResponseEntity<SavedRequestResponseDto> duplicateRequest(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(savedRequestService.duplicateRequest(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete saved request", description = "Deletes a saved request by ID")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        savedRequestService.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }
}
