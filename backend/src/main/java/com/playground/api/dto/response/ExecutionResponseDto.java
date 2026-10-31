package com.playground.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResponseDto {
    private Integer statusCode;
    private String statusText;
    private long durationMs;
    private long sizeBytes;
    private Map<String, String> headers;
    private String body;
    private String contentType;
    private String errorCategory; // SUCCESS, HTTP_ERROR, NETWORK_ERROR, TIMEOUT, SECURITY_VIOLATION, CANCELLED
    private String errorMessage;
    private Long historyId;
}
