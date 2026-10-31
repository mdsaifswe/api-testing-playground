package com.playground.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.api.config.PlaygroundSecurityProperties;
import com.playground.api.dto.common.AuthDto;
import com.playground.api.dto.common.KeyValueDto;
import com.playground.api.dto.request.ExecuteRequestDto;
import com.playground.api.dto.response.ExecutionResponseDto;
import com.playground.api.entity.RequestHistoryEntity;
import com.playground.api.exception.InvalidRequestException;
import com.playground.api.exception.SsrfSecurityException;
import com.playground.api.repository.RequestHistoryRepository;
import com.playground.api.security.SsrfValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestExecutionService {

    private final SsrfValidator ssrfValidator;
    private final VariableSubstitutionService substitutionService;
    private final RequestHistoryRepository historyRepository;
    private final PlaygroundSecurityProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * Executes the HTTP request configured by the client, normalizes response, and persists history.
     */
    public ExecutionResponseDto executeRequest(ExecuteRequestDto requestDto) {
        if (requestDto == null) {
            throw new InvalidRequestException("Execution request payload cannot be null.");
        }

        // 1. Resolve environment variables if environment is specified or active
        Long envId = requestDto.getEnvironmentId();
        String resolvedUrl = substitutionService.substitute(requestDto.getUrl(), envId);
        String resolvedBody = substitutionService.substitute(requestDto.getBody(), envId);

        // 2. Build complete URL with query parameters and potential query-based Auth
        String fullUrl = buildFullUrl(resolvedUrl, requestDto.getQueryParams(), requestDto.getAuth(), envId);

        // 3. Validate target URL through SSRF guard
        URI targetUri;
        try {
            targetUri = ssrfValidator.validateUrl(fullUrl);
        } catch (SsrfSecurityException e) {
            // Persist security violation to history
            RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                    resolvedBody, 403, "Forbidden", Collections.emptyMap(), e.getMessage(), 0, 0, "SECURITY_VIOLATION");

            return ExecutionResponseDto.builder()
                    .statusCode(403)
                    .statusText("Forbidden")
                    .errorCategory("SECURITY_VIOLATION")
                    .errorMessage(e.getMessage())
                    .historyId(history.getId())
                    .build();
        }

        // 4. Configure timeouts
        int timeoutMs = requestDto.getTimeoutMs() != null && requestDto.getTimeoutMs() > 0
                ? requestDto.getTimeoutMs()
                : properties.getReadTimeoutMs();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(properties.getConnectTimeoutMs()))
                .setResponseTimeout(Timeout.ofMilliseconds(timeoutMs))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(properties.getConnectTimeoutMs()))
                .setRedirectsEnabled(true)
                .setMaxRedirects(properties.getMaxRedirects())
                .build();

        long startTime = System.currentTimeMillis();
        long durationMs = 0;

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {

            HttpUriRequestBase httpRequest = createHttpRequest(requestDto.getMethod(), targetUri);

            // Add headers with variable substitution
            applyHeaders(httpRequest, requestDto.getHeaders(), requestDto.getAuth(), envId);

            // Apply body for POST, PUT, PATCH, DELETE
            if (resolvedBody != null && !resolvedBody.trim().isEmpty() && httpRequest.getEntity() == null) {
                if (isEntityEnclosingMethod(requestDto.getMethod())) {
                    httpRequest.setEntity(new StringEntity(resolvedBody, StandardCharsets.UTF_8));
                }
            }

            try (CloseableHttpResponse response = httpClient.execute(httpRequest)) {
                durationMs = System.currentTimeMillis() - startTime;
                int statusCode = response.getCode();
                String statusText = response.getReasonPhrase() != null ? response.getReasonPhrase() : getStatusText(statusCode);

                Map<String, String> responseHeaders = new LinkedHashMap<>();
                for (Header h : response.getHeaders()) {
                    responseHeaders.put(h.getName(), h.getValue());
                }

                String contentType = responseHeaders.getOrDefault("Content-Type", "text/plain");

                String responseBody = "";
                long responseSize = 0;

                HttpEntity entity = response.getEntity();
                if (entity != null) {
                    byte[] bytes = EntityUtils.toByteArray(entity);
                    responseSize = bytes.length;

                    // Enforce size limit
                    if (responseSize > properties.getMaxResponseSizeBytes()) {
                        responseBody = "[Response payload exceeded limit of " + properties.getMaxResponseSizeBytes() + " bytes]";
                    } else {
                        responseBody = new String(bytes, StandardCharsets.UTF_8);
                    }
                }

                String errorCategory = (statusCode >= 200 && statusCode < 400) ? "SUCCESS" : "HTTP_ERROR";

                RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                        resolvedBody, statusCode, statusText, responseHeaders, responseBody, responseSize, durationMs, errorCategory);

                return ExecutionResponseDto.builder()
                        .statusCode(statusCode)
                        .statusText(statusText)
                        .durationMs(durationMs)
                        .sizeBytes(responseSize)
                        .headers(responseHeaders)
                        .body(responseBody)
                        .contentType(contentType)
                        .errorCategory(errorCategory)
                        .historyId(history.getId())
                        .build();

            } catch (SocketTimeoutException e) {
                durationMs = System.currentTimeMillis() - startTime;
                log.warn("Request timed out after {}ms: {}", durationMs, fullUrl);

                RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                        resolvedBody, 0, "Timeout", Collections.emptyMap(), "Request timed out after " + timeoutMs + "ms", 0, durationMs, "TIMEOUT");

                return ExecutionResponseDto.builder()
                        .statusCode(504)
                        .statusText("Gateway Timeout")
                        .durationMs(durationMs)
                        .sizeBytes(0)
                        .headers(Collections.emptyMap())
                        .body("Request timed out after " + timeoutMs + "ms: " + e.getMessage())
                        .errorCategory("TIMEOUT")
                        .errorMessage("Request timed out after " + timeoutMs + "ms")
                        .historyId(history.getId())
                        .build();

            } catch (IOException e) {
                durationMs = System.currentTimeMillis() - startTime;
                log.warn("Network error executing request: {} - {}", fullUrl, e.getMessage());

                RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                        resolvedBody, 0, "Network Error", Collections.emptyMap(), e.getMessage(), 0, durationMs, "NETWORK_ERROR");

                return ExecutionResponseDto.builder()
                        .statusCode(502)
                        .statusText("Bad Gateway / Network Error")
                        .durationMs(durationMs)
                        .sizeBytes(0)
                        .headers(Collections.emptyMap())
                        .body("Network error: " + e.getMessage())
                        .errorCategory("NETWORK_ERROR")
                        .errorMessage("Could not connect to target host: " + e.getMessage())
                        .historyId(history.getId())
                        .build();
            }

        } catch (Exception e) {
            durationMs = System.currentTimeMillis() - startTime;
            log.error("Execution error: {}", e.getMessage(), e);

            RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                    resolvedBody, 0, "Execution Error", Collections.emptyMap(), e.getMessage(), 0, durationMs, "NETWORK_ERROR");

            return ExecutionResponseDto.builder()
                    .statusCode(500)
                    .statusText("Execution Error")
                    .durationMs(durationMs)
                    .sizeBytes(0)
                    .headers(Collections.emptyMap())
                    .body("Execution failed: " + e.getMessage())
                    .errorCategory("NETWORK_ERROR")
                    .errorMessage(e.getMessage())
                    .historyId(history.getId())
                    .build();
        }
    }

    private String buildFullUrl(String baseUrl, List<KeyValueDto> queryParams, AuthDto auth, Long envId) {
        StringBuilder urlBuilder = new StringBuilder(baseUrl != null ? baseUrl.trim() : "");
        List<String> params = new ArrayList<>();

        if (queryParams != null) {
            for (KeyValueDto param : queryParams) {
                if (param.isEnabled() && param.getKey() != null && !param.getKey().trim().isEmpty()) {
                    String resolvedKey = substitutionService.substitute(param.getKey().trim(), envId);
                    String resolvedVal = substitutionService.substitute(param.getValue() != null ? param.getValue().trim() : "", envId);
                    params.add(resolvedKey + "=" + resolvedVal);
                }
            }
        }

        // Apply API Key via Query Parameter if configured
        if (auth != null && "API_KEY".equalsIgnoreCase(auth.getType()) && "QUERY".equalsIgnoreCase(auth.getPlacement())) {
            if (auth.getKeyName() != null && !auth.getKeyName().trim().isEmpty()) {
                String resolvedKey = substitutionService.substitute(auth.getKeyName().trim(), envId);
                String resolvedVal = substitutionService.substitute(auth.getKeyValue() != null ? auth.getKeyValue().trim() : "", envId);
                params.add(resolvedKey + "=" + resolvedVal);
            }
        }

        if (!params.isEmpty()) {
            char separator = urlBuilder.indexOf("?") >= 0 ? '&' : '?';
            urlBuilder.append(separator).append(String.join("&", params));
        }

        return urlBuilder.toString();
    }

    private void applyHeaders(HttpUriRequestBase request, List<KeyValueDto> headers, AuthDto auth, Long envId) {
        boolean hasContentType = false;

        if (headers != null) {
            for (KeyValueDto header : headers) {
                if (header.isEnabled() && header.getKey() != null && !header.getKey().trim().isEmpty()) {
                    String resolvedKey = substitutionService.substitute(header.getKey().trim(), envId);
                    String resolvedVal = substitutionService.substitute(header.getValue() != null ? header.getValue().trim() : "", envId);
                    request.addHeader(resolvedKey, resolvedVal);
                    if ("content-type".equalsIgnoreCase(resolvedKey)) {
                        hasContentType = true;
                    }
                }
            }
        }

        // Apply API Key via Header if configured
        if (auth != null && "API_KEY".equalsIgnoreCase(auth.getType()) && "HEADER".equalsIgnoreCase(auth.getPlacement())) {
            if (auth.getKeyName() != null && !auth.getKeyName().trim().isEmpty()) {
                String resolvedKey = substitutionService.substitute(auth.getKeyName().trim(), envId);
                String resolvedVal = substitutionService.substitute(auth.getKeyValue() != null ? auth.getKeyValue().trim() : "", envId);
                request.addHeader(resolvedKey, resolvedVal);
            }
        }

        // Default Content-Type to application/json for methods with body if not provided
        if (!hasContentType && isEntityEnclosingMethod(request.getMethod())) {
            request.addHeader("Content-Type", "application/json");
        }
    }

    private HttpUriRequestBase createHttpRequest(String method, URI uri) {
        return switch (method.toUpperCase()) {
            case "GET" -> new HttpGet(uri);
            case "POST" -> new HttpPost(uri);
            case "PUT" -> new HttpPut(uri);
            case "PATCH" -> new HttpPatch(uri);
            case "DELETE" -> new HttpDelete(uri);
            case "HEAD" -> new HttpHead(uri);
            case "OPTIONS" -> new HttpOptions(uri);
            default -> throw new InvalidRequestException("Unsupported HTTP method: " + method);
        };
    }

    private boolean isEntityEnclosingMethod(String method) {
        String m = method.toUpperCase();
        return m.equals("POST") || m.equals("PUT") || m.equals("PATCH") || m.equals("DELETE");
    }

    private String getStatusText(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 500 -> "Internal Server Error";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            default -> "HTTP " + code;
        };
    }

    private RequestHistoryEntity recordHistory(String method, String url, List<KeyValueDto> headers,
                                              String body, Integer status, String statusText,
                                              Map<String, String> responseHeaders, String responseBody,
                                              long responseSize, long durationMs, String errorCategory) {
        try {
            String headersJson = headers != null ? objectMapper.writeValueAsString(headers) : "[]";
            String respHeadersJson = responseHeaders != null ? objectMapper.writeValueAsString(responseHeaders) : "{}";

            RequestHistoryEntity entity = RequestHistoryEntity.builder()
                    .method(method)
                    .url(url)
                    .requestHeaders(headersJson)
                    .requestBody(body)
                    .responseStatus(status)
                    .responseStatusText(statusText)
                    .responseHeaders(respHeadersJson)
                    .responseBody(responseBody)
                    .responseSize(responseSize)
                    .durationMs(durationMs)
                    .errorCategory(errorCategory)
                    .executedAt(Instant.now())
                    .build();

            return historyRepository.save(entity);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize history metadata: {}", e.getMessage());
            RequestHistoryEntity fallback = RequestHistoryEntity.builder()
                    .method(method)
                    .url(url)
                    .responseStatus(status)
                    .responseStatusText(statusText)
                    .durationMs(durationMs)
                    .errorCategory(errorCategory)
                    .executedAt(Instant.now())
                    .build();
            return historyRepository.save(fallback);
        }
    }
}
