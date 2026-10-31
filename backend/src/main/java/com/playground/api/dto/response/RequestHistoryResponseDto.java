package com.playground.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestHistoryResponseDto {
    private Long id;
    private String method;
    private String url;
    private String requestHeaders;
    private String requestBody;
    private Integer responseStatus;
    private String responseStatusText;
    private String responseHeaders;
    private String responseBody;
    private Long responseSize;
    private Long durationMs;
    private String errorCategory;
    private Instant executedAt;
}
