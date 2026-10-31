package com.playground.api.controller;

import com.playground.api.dto.request.CreateCollectionDto;
import com.playground.api.dto.request.UpdateCollectionDto;
import com.playground.api.dto.response.CollectionResponseDto;
import com.playground.api.service.CollectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/collections")
@RequiredArgsConstructor
@Tag(name = "Collections", description = "Organize API requests into collections")
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    @Operation(summary = "Get all collections", description = "Lists all collections with their saved requests")
    public ResponseEntity<List<CollectionResponseDto>> getAllCollections() {
        return ResponseEntity.ok(collectionService.getAllCollections());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get collection by ID", description = "Retrieves a single collection with its saved requests")
    public ResponseEntity<CollectionResponseDto> getCollectionById(@PathVariable Long id) {
        return ResponseEntity.ok(collectionService.getCollectionById(id));
    }

    @PostMapping
    @Operation(summary = "Create collection", description = "Creates a new collection")
    public ResponseEntity<CollectionResponseDto> createCollection(@Valid @RequestBody CreateCollectionDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(collectionService.createCollection(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update collection", description = "Updates a collection's name or description")
    public ResponseEntity<CollectionResponseDto> updateCollection(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCollectionDto dto) {
        return ResponseEntity.ok(collectionService.updateCollection(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete collection", description = "Deletes a collection and cascades to its saved requests")
    public ResponseEntity<Void> deleteCollection(@PathVariable Long id) {
        collectionService.deleteCollection(id);
        return ResponseEntity.noContent().build();
    }
}
