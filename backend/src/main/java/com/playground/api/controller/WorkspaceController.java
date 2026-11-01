package com.playground.api.controller;

import com.playground.api.dto.request.CreateWorkspaceDto;
import com.playground.api.dto.response.WorkspaceResponseDto;
import com.playground.api.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
@Tag(name = "Workspaces", description = "Anonymous cryptographic workspace isolation management")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @PostMapping
    @Operation(summary = "Create isolated workspace", description = "Generates a 256-bit cryptographically secure workspace credential and seeds starter collection and environments")
    public ResponseEntity<WorkspaceResponseDto> createWorkspace(@Valid @RequestBody(required = false) CreateWorkspaceDto dto) {
        WorkspaceResponseDto created = workspaceService.createWorkspace(dto != null ? dto : new CreateWorkspaceDto());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/current")
    @Operation(summary = "Get current workspace", description = "Retrieves metadata for the authenticated workspace")
    public ResponseEntity<WorkspaceResponseDto> getCurrentWorkspace() {
        return ResponseEntity.ok(workspaceService.getCurrentWorkspaceDto());
    }
}
