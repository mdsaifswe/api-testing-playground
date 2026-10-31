package com.playground.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/echo")
@Tag(name = "Echo Test Endpoints", description = "Controlled endpoints for testing GET, POST, Auth verification, status codes, and latency")
public class EchoController {

    @GetMapping("/get")
    @Operation(summary = "Echo GET request", description = "Echoes back received query parameters, headers, and request metadata")
    public ResponseEntity<Map<String, Object>> echoGet(
            @RequestParam Map<String, String> queryParams,
            HttpServletRequest request) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Echo GET request successful");
        response.put("timestamp", Instant.now().toString());
        response.put("method", "GET");
        response.put("clientIp", request.getRemoteAddr());
        response.put("queryParams", queryParams);
        response.put("headers", extractHeaders(request));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/post")
    @Operation(summary = "Echo POST request", description = "Echoes back received body, content type, and headers")
    public ResponseEntity<Map<String, Object>> echoPost(
            @RequestBody(required = false) String body,
            HttpServletRequest request) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Echo POST request successful");
        response.put("timestamp", Instant.now().toString());
        response.put("method", "POST");
        response.put("contentType", request.getContentType());
        response.put("body", body != null ? body : "");
        response.put("headers", extractHeaders(request));

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/put")
    @Operation(summary = "Echo PUT request", description = "Echoes back received body for PUT operations")
    public ResponseEntity<Map<String, Object>> echoPut(
            @RequestBody(required = false) String body,
            HttpServletRequest request) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Echo PUT request successful");
        response.put("timestamp", Instant.now().toString());
        response.put("method", "PUT");
        response.put("body", body != null ? body : "");
        response.put("headers", extractHeaders(request));

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/patch")
    @Operation(summary = "Echo PATCH request", description = "Echoes back received body for PATCH operations")
    public ResponseEntity<Map<String, Object>> echoPatch(
            @RequestBody(required = false) String body,
            HttpServletRequest request) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Echo PATCH request successful");
        response.put("timestamp", Instant.now().toString());
        response.put("method", "PATCH");
        response.put("body", body != null ? body : "");
        response.put("headers", extractHeaders(request));

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Echo DELETE request", description = "Confirms DELETE execution")
    public ResponseEntity<Map<String, Object>> echoDelete(
            @RequestParam Map<String, String> queryParams,
            HttpServletRequest request) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Resource successfully deleted");
        response.put("timestamp", Instant.now().toString());
        response.put("method", "DELETE");
        response.put("queryParams", queryParams);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/auth-check")
    @Operation(summary = "Verify API Key", description = "Verifies presence of API key in X-API-Key header or apiKey query parameter")
    public ResponseEntity<Map<String, Object>> checkAuth(
            @RequestHeader(value = "X-API-Key", required = false) String headerKey,
            @RequestParam(value = "apiKey", required = false) String queryKey,
            HttpServletRequest request) {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("timestamp", Instant.now().toString());

        String key = headerKey != null ? headerKey : queryKey;
        String placement = headerKey != null ? "HEADER (X-API-Key)" : (queryKey != null ? "QUERY (apiKey)" : "NONE");

        if (key == null || key.trim().isEmpty()) {
            response.put("authenticated", false);
            response.put("error", "Unauthorized: Missing API Key in header 'X-API-Key' or query parameter 'apiKey'");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        response.put("authenticated", true);
        response.put("message", "API Key successfully validated");
        response.put("placement", placement);
        // Mask the key in the response for security (show only first 4 chars)
        String maskedKey = key.length() > 6 ? key.substring(0, 4) + "..." + key.substring(key.length() - 2) : "****";
        response.put("receivedKeyMasked", maskedKey);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{code}")
    @Operation(summary = "Return specific HTTP status", description = "Returns the requested HTTP status code for testing error handling")
    public ResponseEntity<Map<String, Object>> returnStatus(@PathVariable int code) {
        HttpStatus status;
        try {
            status = HttpStatus.valueOf(code);
        } catch (IllegalArgumentException e) {
            status = HttpStatus.BAD_REQUEST;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestedStatusCode", code);
        response.put("statusPhrase", status.getReasonPhrase());
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/delay/{seconds}")
    @Operation(summary = "Simulate delayed response", description = "Waits for specified seconds before returning response to test timeout handling")
    public ResponseEntity<Map<String, Object>> simulateDelay(@PathVariable int seconds) throws InterruptedException {
        int cappedSeconds = Math.min(seconds, 30); // Max 30s to prevent unbounded sleep
        Thread.sleep(cappedSeconds * 1000L);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Response completed after delay");
        response.put("delayedSeconds", cappedSeconds);
        response.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(response);
    }

    private Map<String, String> extractHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name, request.getHeader(name));
            }
        }
        return headers;
    }
}
