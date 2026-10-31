package com.playground.api.controller;

import com.playground.api.dto.request.ExecuteRequestDto;
import com.playground.api.dto.response.ExecutionResponseDto;
import com.playground.api.service.RequestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/execute")
@RequiredArgsConstructor
@Tag(name = "Execution", description = "Execute outbound HTTP requests through SSRF-guarded proxy engine")
public class ExecutionController {

    private final RequestExecutionService executionService;

    @PostMapping
    @Operation(summary = "Execute HTTP Request", description = "Executes the configured request, enforces SSRF protections, measures duration, normalizes response, and logs to history.")
    public ResponseEntity<ExecutionResponseDto> executeRequest(@Valid @RequestBody ExecuteRequestDto requestDto) {
        ExecutionResponseDto response = executionService.executeRequest(requestDto);
        return ResponseEntity.ok(response);
    }
}
