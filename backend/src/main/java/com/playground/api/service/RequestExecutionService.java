package com.playground.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.playground.api.config.PlaygroundSecurityProperties;
import com.playground.api.dto.common.AuthDto;
import com.playground.api.dto.common.KeyValueDto;
import com.playground.api.dto.request.ExecuteRequestDto;
import com.playground.api.dto.response.ExecutionResponseDto;
import com.playground.api.entity.EnvironmentEntity;
import com.playground.api.entity.EnvironmentVariableEntity;
import com.playground.api.entity.RequestHistoryEntity;
import com.playground.api.entity.WorkspaceEntity;
import com.playground.api.exception.InvalidRequestException;
import com.playground.api.exception.ResourceNotFoundException;
import com.playground.api.exception.SsrfSecurityException;
import com.playground.api.repository.EnvironmentRepository;
import com.playground.api.repository.EnvironmentVariableRepository;
import com.playground.api.repository.RequestHistoryRepository;
import com.playground.api.security.SafeDnsResolver;
import com.playground.api.security.SsrfValidator;
import com.playground.api.security.WorkspaceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestExecutionService {

    public static final int HARD_MAX_TIMEOUT_MS = 15_000; // Hard ceiling of 15 seconds
    public static final int DEFAULT_TIMEOUT_MS = 10_000;

    private final SsrfValidator ssrfValidator;
    private final SafeDnsResolver safeDnsResolver;
    private final VariableSubstitutionService substitutionService;
    private final RequestHistoryRepository historyRepository;
    private final PlaygroundSecurityProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private EnvironmentRepository environmentRepository;

    @Autowired(required = false)
    private EnvironmentVariableRepository variableRepository;

    public static final Set<String> SENSITIVE_PARAM_NAMES = Set.of(
            "apikey", "api_key", "api-key",
            "token", "access_token", "refresh_token", "auth_token",
            "secret", "client_secret",
            "password", "passwd", "pass", "pwd",
            "authorization", "credential", "credentials",
            "key", "auth"
    );

    public static final Set<String> SENSITIVE_HEADER_NAMES = Set.of(
            "authorization",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "api-key",
            "apikey",
            "x-auth-token",
            "token",
            "secret"
    );

    public static final Set<String> SENSITIVE_JSON_FIELDS = Set.of(
            "password", "passwd", "pwd",
            "token", "access_token", "refresh_token", "auth_token",
            "apikey", "api_key", "api-key",
            "secret", "client_secret",
            "authorization", "credential", "credentials", "auth"
    );

    /**
     * Executes the HTTP request configured by the client, normalizes response, and persists sanitized history.
     */
    public ExecutionResponseDto executeRequest(ExecuteRequestDto requestDto) {
        if (requestDto == null) {
            throw new InvalidRequestException("Execution request payload cannot be null.");
        }

        long startNano = System.nanoTime();
        long durationMs = 0;

        // 0. Verify workspace authorization for environment if specified
        Long envId = requestDto.getEnvironmentId();
        WorkspaceEntity currentWorkspace = WorkspaceContextHolder.get();
        if (envId != null && currentWorkspace != null && environmentRepository != null) {
            if (!environmentRepository.existsByIdAndWorkspaceId(envId, currentWorkspace.getId())) {
                throw new ResourceNotFoundException("Environment not found with id: " + envId);
            }
        }

        // 1. Resolve environment variables if environment is specified or active
        String resolvedUrl = substitutionService.substitute(requestDto.getUrl(), envId);
        String resolvedBody = substitutionService.substitute(requestDto.getBody(), envId);

        // 2. Build complete URL with query parameters and potential query-based Auth
        String fullUrl = buildFullUrl(resolvedUrl, requestDto.getQueryParams(), requestDto.getAuth(), envId);

        // 3. Validate target URL through SSRF guard
        URI targetUri;
        try {
            targetUri = ssrfValidator.validateUrl(fullUrl);
        } catch (SsrfSecurityException e) {
            durationMs = (System.nanoTime() - startNano) / 1_000_000L;
            log.warn("SSRF security violation blocked for URL {}: {}", redactSensitiveUrl(fullUrl), e.getMessage());
            RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                    resolvedBody, 403, "Forbidden", Collections.emptyMap(), e.getMessage(), 0, durationMs, "SECURITY_VIOLATION", envId);

            return ExecutionResponseDto.builder()
                    .statusCode(403)
                    .statusText("Forbidden")
                    .durationMs(durationMs)
                    .sizeBytes(0)
                    .headers(Collections.emptyMap())
                    .body(e.getMessage())
                    .errorCategory("SECURITY_VIOLATION")
                    .errorMessage(e.getMessage())
                    .historyId(history.getId())
                    .build();
        }

        // 4. Configure timeouts with hard 15-second cap and disable automatic redirect following
        int timeoutMs = resolveTimeout(requestDto.getTimeoutMs());
        int connectTimeoutMs = resolveConnectTimeout();
        long deadlineNano = startNano + (timeoutMs * 1_000_000L);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(timeoutMs))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setRedirectsEnabled(false) // Strict hop-by-hop manual validation
                .build();

        try {
            PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                    .setDnsResolver(safeDnsResolver)
                    .build();

            try (CloseableHttpClient httpClient = HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .build()) {

                URI currentUri = targetUri;
                String currentMethod = requestDto.getMethod();
                String currentBody = resolvedBody;
                int redirectCount = 0;
                Set<URI> visitedUris = new HashSet<>();
                visitedUris.add(currentUri);

                CloseableHttpResponse response = null;
                try {
                    while (true) {
                        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000L;
                        long remainingMs = timeoutMs - elapsedMs;
                        if (remainingMs <= 0) {
                            throw new SocketTimeoutException("Overall request deadline of " + timeoutMs + "ms exceeded.");
                        }

                        int hopConnectTimeout = Math.min(connectTimeoutMs, (int) Math.max(1, remainingMs));
                        int hopResponseTimeout = (int) Math.max(1, remainingMs);
                        RequestConfig hopConfig = RequestConfig.custom()
                                .setConnectTimeout(Timeout.ofMilliseconds(hopConnectTimeout))
                                .setResponseTimeout(Timeout.ofMilliseconds(hopResponseTimeout))
                                .setConnectionRequestTimeout(Timeout.ofMilliseconds(hopConnectTimeout))
                                .setRedirectsEnabled(false)
                                .build();

                        HttpUriRequestBase httpRequest = createHttpRequest(currentMethod, currentUri);
                        httpRequest.setConfig(hopConfig);

                        // Cross-origin check: strip sensitive headers and credentials if redirect destination has a different origin
                        boolean isCrossOrigin = !isSameOrigin(targetUri, currentUri);
                        applyHeaders(httpRequest, requestDto.getHeaders(), requestDto.getAuth(), envId, isCrossOrigin);

                        if (currentBody != null && !currentBody.trim().isEmpty() && httpRequest.getEntity() == null) {
                            if (isEntityEnclosingMethod(currentMethod)) {
                                httpRequest.setEntity(new StringEntity(currentBody, StandardCharsets.UTF_8));
                            }
                        }

                        response = httpClient.execute(httpRequest);
                        int statusCode = response.getCode();

                        // Check if response is an HTTP redirect (301, 302, 303, 307, 308)
                        if (isRedirectStatus(statusCode)) {
                            Header locationHeader = response.getFirstHeader("Location");
                            if (locationHeader != null && locationHeader.getValue() != null && !locationHeader.getValue().trim().isEmpty()) {
                                redirectCount++;
                                if (redirectCount > properties.getMaxRedirects()) {
                                    response.close();
                                    throw new SsrfSecurityException("Maximum redirect limit of " + properties.getMaxRedirects() + " exceeded.");
                                }

                                String locationValue = locationHeader.getValue().trim();
                                URI nextUri = ssrfValidator.resolveAndValidateRedirect(currentUri, locationValue);

                                // Block insecure protocol downgrade (e.g. HTTPS to HTTP)
                                if ("https".equalsIgnoreCase(currentUri.getScheme()) && "http".equalsIgnoreCase(nextUri.getScheme())) {
                                    response.close();
                                    throw new SsrfSecurityException("Protocol downgrade from HTTPS to HTTP is prohibited during redirect: " + nextUri);
                                }

                                // Prevent redirect loops
                                if (!visitedUris.add(nextUri)) {
                                    response.close();
                                    throw new SsrfSecurityException("Redirect loop detected for URI: " + nextUri);
                                }

                                // Close response before following the redirect hop
                                response.close();

                                // Adjust HTTP method according to HTTP specifications
                                if (statusCode == 303) {
                                    currentMethod = "GET";
                                    currentBody = null;
                                } else if ((statusCode == 301 || statusCode == 302) && isEntityEnclosingMethod(currentMethod)) {
                                    currentMethod = "GET";
                                    currentBody = null;
                                }
                                // For 307 and 308, currentMethod and currentBody are strictly preserved

                                currentUri = nextUri;
                                continue;
                            }
                        }

                        // Final non-redirect response received
                        break;
                    }

                    durationMs = (System.nanoTime() - startNano) / 1_000_000L;
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
                        BoundedReadResult readResult = readBoundedBody(entity.getContent(), properties.getMaxResponseSizeBytes(), deadlineNano);
                        responseBody = readResult.getBody();
                        responseSize = readResult.getSizeBytes();
                    }

                    String errorCategory = (statusCode >= 200 && statusCode < 400) ? "SUCCESS" : "HTTP_ERROR";

                    RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                            resolvedBody, statusCode, statusText, responseHeaders, responseBody, responseSize, durationMs, errorCategory, envId);

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

                } finally {
                    if (response != null) {
                        try {
                            response.close();
                        } catch (Exception ignored) {}
                    }
                }
            }

        } catch (SsrfSecurityException e) {
            durationMs = (System.nanoTime() - startNano) / 1_000_000L;
            log.warn("SSRF security policy blocked redirect/request: {}", e.getMessage());
            RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                    resolvedBody, 403, "Forbidden", Collections.emptyMap(), e.getMessage(), 0, durationMs, "SECURITY_VIOLATION", envId);

            return ExecutionResponseDto.builder()
                    .statusCode(403)
                    .statusText("Forbidden")
                    .durationMs(durationMs)
                    .sizeBytes(0)
                    .headers(Collections.emptyMap())
                    .body(e.getMessage())
                    .errorCategory("SECURITY_VIOLATION")
                    .errorMessage(e.getMessage())
                    .historyId(history.getId())
                    .build();

        } catch (SocketTimeoutException e) {
            durationMs = (System.nanoTime() - startNano) / 1_000_000L;
            log.warn("Request timed out after {}ms: {}", durationMs, redactSensitiveUrl(fullUrl));

            RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                    resolvedBody, 504, "Gateway Timeout", Collections.emptyMap(), "Request timed out after " + timeoutMs + "ms", 0, durationMs, "TIMEOUT", envId);

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

        } catch (UnknownHostException e) {
            durationMs = (System.nanoTime() - startNano) / 1_000_000L;
            log.warn("DNS resolution / Connection blocked: {}", e.getMessage());
            boolean isSsrf = e.getMessage() != null && e.getMessage().contains("SSRF");
            int statusCode = isSsrf ? 403 : 502;
            String statusText = isSsrf ? "Forbidden" : "Bad Gateway";
            String category = isSsrf ? "SECURITY_VIOLATION" : "NETWORK_ERROR";
            String errorMsg = isSsrf ? e.getMessage() : "Could not resolve or connect to target host: " + e.getMessage();

            RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                    resolvedBody, statusCode, statusText, Collections.emptyMap(), errorMsg, 0, durationMs, category, envId);

            return ExecutionResponseDto.builder()
                    .statusCode(statusCode)
                    .statusText(statusText)
                    .durationMs(durationMs)
                    .sizeBytes(0)
                    .headers(Collections.emptyMap())
                    .body(errorMsg)
                    .errorCategory(category)
                    .errorMessage(errorMsg)
                    .historyId(history.getId())
                    .build();

        } catch (IOException e) {
            durationMs = (System.nanoTime() - startNano) / 1_000_000L;
            log.warn("Network error executing request: {} - {}", redactSensitiveUrl(fullUrl), e.getMessage());

            Throwable cause = e.getCause();
            boolean isSsrf = (cause != null && cause.getMessage() != null && cause.getMessage().contains("SSRF"))
                    || (e.getMessage() != null && e.getMessage().contains("SSRF"));
            int statusCode = isSsrf ? 403 : 502;
            String statusText = isSsrf ? "Forbidden" : "Bad Gateway / Network Error";
            String category = isSsrf ? "SECURITY_VIOLATION" : "NETWORK_ERROR";
            String errorMsg = isSsrf ? (cause != null ? cause.getMessage() : e.getMessage()) : "Could not connect to target host: " + e.getMessage();

            RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                    resolvedBody, statusCode, statusText, Collections.emptyMap(), errorMsg, 0, durationMs, category);

            return ExecutionResponseDto.builder()
                    .statusCode(statusCode)
                    .statusText(statusText)
                    .durationMs(durationMs)
                    .sizeBytes(0)
                    .headers(Collections.emptyMap())
                    .body("Network error: " + errorMsg)
                    .errorCategory(category)
                    .errorMessage(errorMsg)
                    .historyId(history.getId())
                    .build();

        } catch (Exception e) {
            durationMs = (System.nanoTime() - startNano) / 1_000_000L;
            log.error("Execution error: {}", e.getMessage(), e);

            RequestHistoryEntity history = recordHistory(requestDto.getMethod(), fullUrl, requestDto.getHeaders(),
                    resolvedBody, 500, "Execution Error", Collections.emptyMap(), e.getMessage(), 0, durationMs, "NETWORK_ERROR");

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

    /**
     * Determines whether two URIs share the exact same origin (Scheme, Host, and effective Port).
     */
    public boolean isSameOrigin(URI uri1, URI uri2) {
        if (uri1 == null || uri2 == null) {
            return false;
        }
        String scheme1 = uri1.getScheme() != null ? uri1.getScheme().toLowerCase(Locale.ROOT) : "";
        String scheme2 = uri2.getScheme() != null ? uri2.getScheme().toLowerCase(Locale.ROOT) : "";
        if (!scheme1.equals(scheme2)) {
            return false;
        }

        String host1 = uri1.getHost() != null ? uri1.getHost().toLowerCase(Locale.ROOT) : "";
        String host2 = uri2.getHost() != null ? uri2.getHost().toLowerCase(Locale.ROOT) : "";
        if (!host1.equals(host2)) {
            return false;
        }

        int port1 = uri1.getPort() != -1 ? uri1.getPort() : ("https".equals(scheme1) ? 443 : 80);
        int port2 = uri2.getPort() != -1 ? uri2.getPort() : ("https".equals(scheme2) ? 443 : 80);
        return port1 == port2;
    }

    public String redactSensitiveUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return url;
        }
        int queryIndex = url.indexOf('?');
        if (queryIndex < 0) {
            return url;
        }

        String base = url.substring(0, queryIndex);
        String queryAndFragment = url.substring(queryIndex + 1);

        String fragment = "";
        int fragmentIndex = queryAndFragment.indexOf('#');
        if (fragmentIndex >= 0) {
            fragment = queryAndFragment.substring(fragmentIndex);
            queryAndFragment = queryAndFragment.substring(0, fragmentIndex);
        }

        if (queryAndFragment.isEmpty()) {
            return base + fragment;
        }

        String[] pairs = queryAndFragment.split("&");
        List<String> redactedPairs = new ArrayList<>();
        for (String pair : pairs) {
            if (pair.isEmpty()) {
                continue;
            }
            int eqIndex = pair.indexOf('=');
            String key = eqIndex >= 0 ? pair.substring(0, eqIndex) : pair;
            String cleanKey = key.trim().toLowerCase(Locale.ROOT);
            String decodedKey = cleanKey;
            try {
                decodedKey = URLDecoder.decode(cleanKey, StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
            } catch (Exception ignored) {}

            if (isSensitiveParam(cleanKey) || isSensitiveParam(decodedKey)) {
                redactedPairs.add(key + "=[REDACTED]");
            } else {
                redactedPairs.add(pair);
            }
        }

        return base + "?" + String.join("&", redactedPairs) + fragment;
    }

    private boolean isSensitiveParam(String cleanKey) {
        if (cleanKey == null) return false;
        if (SENSITIVE_PARAM_NAMES.contains(cleanKey)) {
            return true;
        }
        return cleanKey.contains("secret") || cleanKey.contains("password") || cleanKey.contains("passwd")
                || cleanKey.contains("token") || cleanKey.contains("apikey");
    }

    public List<KeyValueDto> redactSensitiveHeaders(List<KeyValueDto> headers) {
        if (headers == null) {
            return Collections.emptyList();
        }
        List<KeyValueDto> redacted = new ArrayList<>();
        for (KeyValueDto h : headers) {
            if (h == null) continue;
            String key = h.getKey();
            String val = h.getValue();
            if (key != null) {
                String cleanKey = key.trim().toLowerCase(Locale.ROOT);
                if (isSensitiveHeaderName(cleanKey)) {
                    val = "[REDACTED]";
                }
            }
            redacted.add(new KeyValueDto(key, val, h.isEnabled()));
        }
        return redacted;
    }

    public boolean isSensitiveHeaderName(String cleanKey) {
        if (cleanKey == null) return false;
        String lower = cleanKey.trim().toLowerCase(Locale.ROOT);
        if (SENSITIVE_HEADER_NAMES.contains(lower)) {
            return true;
        }
        return lower.contains("token") || lower.contains("secret") || lower.contains("key")
                || lower.contains("auth") || lower.contains("cookie");
    }

    /**
     * Recursively redacts sensitive properties in JSON request/response bodies.
     * Safely returns original content unmodified if payload is not valid JSON.
     */
    public String redactSensitiveJson(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return jsonString;
        }
        try {
            JsonNode rootNode = objectMapper.readTree(jsonString);
            if (rootNode == null || (!rootNode.isObject() && !rootNode.isArray())) {
                return jsonString;
            }
            JsonNode redactedNode = redactJsonNode(rootNode);
            return objectMapper.writeValueAsString(redactedNode);
        } catch (Exception e) {
            // Malformed JSON or non-JSON text; return safely without error
            return jsonString;
        }
    }

    private JsonNode redactJsonNode(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node.deepCopy();
            List<String> fieldNames = new ArrayList<>();
            objectNode.fieldNames().forEachRemaining(fieldNames::add);

            for (String fieldName : fieldNames) {
                JsonNode child = objectNode.get(fieldName);
                String cleanField = fieldName.trim().toLowerCase(Locale.ROOT);
                if (isSensitiveJsonField(cleanField)) {
                    if (child.isValueNode()) {
                        objectNode.put(fieldName, "[REDACTED]");
                    } else if (child.isObject() || child.isArray()) {
                        objectNode.set(fieldName, redactJsonNode(child));
                    }
                } else if (child.isObject() || child.isArray()) {
                    objectNode.set(fieldName, redactJsonNode(child));
                }
            }
            return objectNode;
        } else if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node.deepCopy();
            for (int i = 0; i < arrayNode.size(); i++) {
                JsonNode item = arrayNode.get(i);
                if (item.isObject() || item.isArray()) {
                    arrayNode.set(i, redactJsonNode(item));
                }
            }
            return arrayNode;
        }
        return node;
    }

    public boolean isSensitiveJsonField(String cleanField) {
        if (cleanField == null) return false;
        String lower = cleanField.trim().toLowerCase(Locale.ROOT);
        if (SENSITIVE_JSON_FIELDS.contains(lower)) {
            return true;
        }
        return lower.contains("secret") || lower.contains("password") || lower.contains("passwd")
                || lower.contains("token") || lower.contains("apikey") || lower.contains("auth")
                || lower.contains("credential");
    }

    private boolean isRedirectStatus(int statusCode) {
        return statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308;
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

    public void applyHeaders(HttpUriRequestBase request, List<KeyValueDto> headers, AuthDto auth, Long envId, boolean isCrossOrigin) {
        boolean hasContentType = false;

        if (headers != null) {
            for (KeyValueDto header : headers) {
                if (header.isEnabled() && header.getKey() != null && !header.getKey().trim().isEmpty()) {
                    String cleanKey = header.getKey().trim().toLowerCase(Locale.ROOT);
                    // On cross-origin redirects, strip sensitive credential-bearing headers
                    if (isCrossOrigin && isSensitiveHeaderName(cleanKey)) {
                        log.debug("Stripping sensitive header '{}' during cross-origin redirect", header.getKey());
                        continue;
                    }

                    String rawKey = header.getKey().trim();
                    String rawVal = header.getValue() != null ? header.getValue().trim() : "";
                    String resolvedKey = substitutionService.substitute(rawKey, envId);
                    if (resolvedKey == null) resolvedKey = rawKey;
                    String resolvedVal = substitutionService.substitute(rawVal, envId);
                    if (resolvedVal == null) resolvedVal = rawVal;

                    request.addHeader(resolvedKey, resolvedVal);
                    if ("content-type".equalsIgnoreCase(resolvedKey)) {
                        hasContentType = true;
                    }
                }
            }
        }

        // Apply API Key via Header if configured - ONLY if same origin!
        if (!isCrossOrigin && auth != null && "API_KEY".equalsIgnoreCase(auth.getType()) && "HEADER".equalsIgnoreCase(auth.getPlacement())) {
            if (auth.getKeyName() != null && !auth.getKeyName().trim().isEmpty()) {
                String rawKey = auth.getKeyName().trim();
                String rawVal = auth.getKeyValue() != null ? auth.getKeyValue().trim() : "";
                String resolvedKey = substitutionService.substitute(rawKey, envId);
                if (resolvedKey == null) resolvedKey = rawKey;
                String resolvedVal = substitutionService.substitute(rawVal, envId);
                if (resolvedVal == null) resolvedVal = rawVal;
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
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 303 -> "See Other";
            case 307 -> "Temporary Redirect";
            case 308 -> "Permanent Redirect";
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
        return recordHistory(method, url, headers, body, status, statusText, responseHeaders, responseBody, responseSize, durationMs, errorCategory, null);
    }

    private RequestHistoryEntity recordHistory(String method, String url, List<KeyValueDto> headers,
                                              String body, Integer status, String statusText,
                                              Map<String, String> responseHeaders, String responseBody,
                                              long responseSize, long durationMs, String errorCategory, Long envId) {
        try {
            WorkspaceEntity workspace = WorkspaceContextHolder.get();
            String safeUrl = redactEnvironmentSecrets(redactSensitiveUrl(url), envId);
            List<KeyValueDto> safeHeaders = redactSensitiveHeaders(headers);
            String safeBody = redactEnvironmentSecrets(redactSensitiveJson(body), envId);
            String headersJson = safeHeaders != null ? objectMapper.writeValueAsString(safeHeaders) : "[]";
            String respHeadersJson = responseHeaders != null ? objectMapper.writeValueAsString(responseHeaders) : "{}";

            RequestHistoryEntity entity = RequestHistoryEntity.builder()
                    .workspace(workspace)
                    .method(method)
                    .url(safeUrl)
                    .requestHeaders(headersJson)
                    .requestBody(safeBody)
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
            WorkspaceEntity workspace = WorkspaceContextHolder.get();
            RequestHistoryEntity fallback = RequestHistoryEntity.builder()
                    .workspace(workspace)
                    .method(method)
                    .url(redactEnvironmentSecrets(redactSensitiveUrl(url), envId))
                    .requestBody(redactEnvironmentSecrets(redactSensitiveJson(body), envId))
                    .responseStatus(status)
                    .responseStatusText(statusText)
                    .durationMs(durationMs)
                    .errorCategory(errorCategory)
                    .executedAt(Instant.now())
                    .build();
            return historyRepository.save(fallback);
        }
    }

    private String redactEnvironmentSecrets(String text, Long envId) {
        if (text == null || text.isEmpty() || variableRepository == null) {
            return text;
        }
        try {
            Long targetEnvId = envId;
            if (targetEnvId == null) {
                WorkspaceEntity ws = WorkspaceContextHolder.get();
                if (ws != null && environmentRepository != null) {
                    targetEnvId = environmentRepository.findByWorkspaceIdAndIsActiveTrue(ws.getId())
                            .map(EnvironmentEntity::getId).orElse(null);
                }
            }
            if (targetEnvId != null) {
                List<EnvironmentVariableEntity> vars = variableRepository.findByEnvironmentId(targetEnvId);
                for (EnvironmentVariableEntity v : vars) {
                    if (Boolean.TRUE.equals(v.getIsSecret()) && v.getValue() != null && v.getValue().length() >= 3) {
                        text = text.replace(v.getValue(), "[REDACTED]");
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to redact environment secrets from history: {}", e.getMessage());
        }
        return text;
    }

    /**
     * Resolves and bounds outbound timeout to ensure it never exceeds HARD_MAX_TIMEOUT_MS (15,000ms).
     * Non-positive, zero, or null timeouts safely fallback to configured read timeout or default, capped at 15,000ms.
     */
    public int resolveTimeout(Integer requestedTimeoutMs) {
        if (requestedTimeoutMs == null || requestedTimeoutMs <= 0) {
            int defaultTimeout = properties.getReadTimeoutMs() > 0 ? properties.getReadTimeoutMs() : DEFAULT_TIMEOUT_MS;
            return Math.min(defaultTimeout, HARD_MAX_TIMEOUT_MS);
        }
        return Math.min(requestedTimeoutMs, HARD_MAX_TIMEOUT_MS);
    }

    public int resolveConnectTimeout() {
        int defaultTimeout = properties.getConnectTimeoutMs() > 0 ? properties.getConnectTimeoutMs() : DEFAULT_TIMEOUT_MS;
        return Math.min(defaultTimeout, HARD_MAX_TIMEOUT_MS);
    }

    public static class BoundedReadResult {
        private final String body;
        private final long sizeBytes;
        private final boolean exceeded;

        public BoundedReadResult(String body, long sizeBytes, boolean exceeded) {
            this.body = body;
            this.sizeBytes = sizeBytes;
            this.exceeded = exceeded;
        }

        public String getBody() {
            return body;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public boolean isExceeded() {
            return exceeded;
        }
    }

    /**
     * Reads up to maxBytes from the response input stream without an overall deadline.
     */
    public BoundedReadResult readBoundedBody(InputStream in, long maxBytes) throws IOException {
        return readBoundedBody(in, maxBytes, -1L);
    }

    /**
     * Reads up to maxBytes from the response input stream while strictly enforcing the overall deadline.
     * If the response exceeds maxBytes or exceeds the deadline, reading terminates immediately and the stream is closed.
     */
    public BoundedReadResult readBoundedBody(InputStream in, long maxBytes, long deadlineNano) throws IOException {
        if (in == null) {
            return new BoundedReadResult("", 0, false);
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long totalRead = 0;
        boolean exceeded = false;

        try (in) {
            int bytesRead;
            while ((bytesRead = in.read(chunk)) != -1) {
                if (deadlineNano > 0 && System.nanoTime() > deadlineNano) {
                    throw new SocketTimeoutException("Overall request deadline exceeded while reading response stream.");
                }
                totalRead += bytesRead;
                if (totalRead > maxBytes) {
                    exceeded = true;
                    break;
                }
                buffer.write(chunk, 0, bytesRead);
            }
        }

        if (exceeded) {
            return new BoundedReadResult("[Response payload exceeded limit of " + maxBytes + " bytes]", totalRead, true);
        } else {
            byte[] bytes = buffer.toByteArray();
            return new BoundedReadResult(new String(bytes, StandardCharsets.UTF_8), bytes.length, false);
        }
    }
}
