package com.playground.api.controller;

import com.playground.api.dto.request.CreateEnvironmentDto;
import com.playground.api.dto.request.UpdateEnvironmentDto;
import com.playground.api.dto.response.EnvironmentResponseDto;
import com.playground.api.service.EnvironmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/environments")
@RequiredArgsConstructor
@Tag(name = "Environments", description = "Create, edit, activate, and delete environments and variables")
public class EnvironmentController {

    private final EnvironmentService environmentService;

    @GetMapping
    @Operation(summary = "Get all environments", description = "Lists all environments with their variables")
    public ResponseEntity<List<EnvironmentResponseDto>> getAllEnvironments() {
        return ResponseEntity.ok(environmentService.getAllEnvironments());
    }

    @GetMapping("/active")
    @Operation(summary = "Get active environment", description = "Retrieves the currently active environment if any")
    public ResponseEntity<EnvironmentResponseDto> getActiveEnvironment() {
        EnvironmentResponseDto active = environmentService.getActiveEnvironment();
        if (active == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(active);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get environment by ID", description = "Retrieves a single environment with its variables")
    public ResponseEntity<EnvironmentResponseDto> getEnvironmentById(@PathVariable Long id) {
        return ResponseEntity.ok(environmentService.getEnvironmentById(id));
    }

    @PostMapping
    @Operation(summary = "Create environment", description = "Creates a new environment with optional variables")
    public ResponseEntity<EnvironmentResponseDto> createEnvironment(@Valid @RequestBody CreateEnvironmentDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(environmentService.createEnvironment(dto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update environment", description = "Updates an environment's name, active state, and variables")
    public ResponseEntity<EnvironmentResponseDto> updateEnvironment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEnvironmentDto dto) {
        return ResponseEntity.ok(environmentService.updateEnvironment(id, dto));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activate environment", description = "Sets specified environment as active and deactivates others")
    public ResponseEntity<EnvironmentResponseDto> activateEnvironment(@PathVariable Long id) {
        return ResponseEntity.ok(environmentService.activateEnvironment(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete environment", description = "Deletes an environment and its variables")
    public ResponseEntity<Void> deleteEnvironment(@PathVariable Long id) {
        environmentService.deleteEnvironment(id);
        return ResponseEntity.noContent().build();
    }
}
