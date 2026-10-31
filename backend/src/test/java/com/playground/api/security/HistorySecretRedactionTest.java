package com.playground.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.api.config.PlaygroundSecurityProperties;
import com.playground.api.dto.common.KeyValueDto;
import com.playground.api.repository.RequestHistoryRepository;
import com.playground.api.service.RequestExecutionService;
import com.playground.api.service.VariableSubstitutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class HistorySecretRedactionTest {

    @Mock
    private SsrfValidator ssrfValidator;

    @Mock
    private SafeDnsResolver safeDnsResolver;

    @Mock
    private VariableSubstitutionService substitutionService;

    @Mock
    private RequestHistoryRepository historyRepository;

    private PlaygroundSecurityProperties properties;
    private ObjectMapper objectMapper;
    private RequestExecutionService executionService;

    @BeforeEach
    void setUp() {
        properties = new PlaygroundSecurityProperties();
        objectMapper = new ObjectMapper();
        executionService = new RequestExecutionService(
                ssrfValidator,
                safeDnsResolver,
                substitutionService,
                historyRepository,
                properties,
                objectMapper
        );
    }

    @Test
    @DisplayName("Redaction: Should redact apiKey and api_key in query parameters")
    void testRedactApiKeyQueryParam() {
        String url1 = "https://api.example.com/data?apiKey=secret-key-12345&page=1";
        String redacted1 = executionService.redactSensitiveUrl(url1);
        assertEquals("https://api.example.com/data?apiKey=[REDACTED]&page=1", redacted1);

        String url2 = "https://api.example.com/data?api_key=secret-key-67890&sort=desc";
        String redacted2 = executionService.redactSensitiveUrl(url2);
        assertEquals("https://api.example.com/data?api_key=[REDACTED]&sort=desc", redacted2);

        String url3 = "https://api.example.com/data?api-key=secret-key-abc";
        String redacted3 = executionService.redactSensitiveUrl(url3);
        assertEquals("https://api.example.com/data?api-key=[REDACTED]", redacted3);
    }

    @Test
    @DisplayName("Redaction: Should redact access_token, refresh_token, and token in query parameters")
    void testRedactTokensQueryParam() {
        String url = "https://auth.example.com/oauth/token?token=jwt123&access_token=acc456&refresh_token=ref789&client_id=myApp";
        String redacted = executionService.redactSensitiveUrl(url);
        assertEquals("https://auth.example.com/oauth/token?token=[REDACTED]&access_token=[REDACTED]&refresh_token=[REDACTED]&client_id=myApp", redacted);
    }

    @Test
    @DisplayName("Redaction: Should redact password, client_secret, and secret in query parameters")
    void testRedactPasswordAndSecrets() {
        String url = "https://api.example.com/login?username=saif&password=supersecretpassword&client_secret=topsecretkey";
        String redacted = executionService.redactSensitiveUrl(url);
        assertEquals("https://api.example.com/login?username=saif&password=[REDACTED]&client_secret=[REDACTED]", redacted);
    }

    @Test
    @DisplayName("Redaction: Should perform case-insensitive parameter matching")
    void testCaseInsensitiveRedaction() {
        String url = "https://api.example.com/test?APIKEY=secret1&Token=secret2&SECRET=secret3&PASSWORD=secret4";
        String redacted = executionService.redactSensitiveUrl(url);
        assertEquals("https://api.example.com/test?APIKEY=[REDACTED]&Token=[REDACTED]&SECRET=[REDACTED]&PASSWORD=[REDACTED]", redacted);
    }

    @Test
    @DisplayName("Redaction: Should preserve non-sensitive query parameters unchanged")
    void testPreserveNormalQueryParams() {
        String url = "https://api.example.com/products?category=electronics&page=2&limit=50&inStock=true";
        String redacted = executionService.redactSensitiveUrl(url);
        assertEquals("https://api.example.com/products?category=electronics&page=2&limit=50&inStock=true", redacted);
    }

    @Test
    @DisplayName("Redaction: Should handle URLs without query parameters and with fragments")
    void testUrlsWithoutParamsAndWithFragments() {
        String urlWithoutParams = "https://api.example.com/products";
        assertEquals(urlWithoutParams, executionService.redactSensitiveUrl(urlWithoutParams));

        String urlWithFragment = "https://api.example.com/auth?token=mysecret#heading-2";
        assertEquals("https://api.example.com/auth?token=[REDACTED]#heading-2", executionService.redactSensitiveUrl(urlWithFragment));
    }

    @Test
    @DisplayName("Redaction: Should redact sensitive headers like Authorization and X-API-Key")
    void testRedactSensitiveHeaders() {
        List<KeyValueDto> headers = List.of(
                new KeyValueDto("Authorization", "Bearer eyJhbGciOi...", true),
                new KeyValueDto("X-API-Key", "secret-header-key", true),
                new KeyValueDto("Content-Type", "application/json", true),
                new KeyValueDto("Accept", "*/*", true)
        );

        List<KeyValueDto> redacted = executionService.redactSensitiveHeaders(headers);
        assertEquals(4, redacted.size());

        assertEquals("[REDACTED]", redacted.get(0).getValue());
        assertEquals("[REDACTED]", redacted.get(1).getValue());
        assertEquals("application/json", redacted.get(2).getValue());
        assertEquals("*/*", redacted.get(3).getValue());
    }

    @Test
    @DisplayName("Redaction: Should redact sensitive keys in top-level JSON body")
    void testRedactTopLevelJsonSecrets() {
        String json = "{\"username\":\"saif\",\"password\":\"supersecret123\",\"token\":\"jwt.token.here\"}";
        String redacted = executionService.redactSensitiveJson(json);
        assertTrue(redacted.contains("\"username\":\"saif\""));
        assertTrue(redacted.contains("\"password\":\"[REDACTED]\""));
        assertTrue(redacted.contains("\"token\":\"[REDACTED]\""));
        assertFalse(redacted.contains("supersecret123"));
        assertFalse(redacted.contains("jwt.token.here"));
    }

    @Test
    @DisplayName("Redaction: Should recursively redact sensitive keys in nested JSON objects and arrays")
    void testRedactNestedJsonSecrets() {
        String json = "{\"auth\":{\"client_secret\":\"topsecret\",\"credentials\":[{\"apiKey\":\"key1\"},{\"apiKey\":\"key2\"}]},\"status\":\"active\"}";
        String redacted = executionService.redactSensitiveJson(json);
        assertTrue(redacted.contains("\"client_secret\":\"[REDACTED]\""));
        assertTrue(redacted.contains("\"apiKey\":\"[REDACTED]\""));
        assertTrue(redacted.contains("\"status\":\"active\""));
        assertFalse(redacted.contains("topsecret"));
        assertFalse(redacted.contains("key1"));
        assertFalse(redacted.contains("key2"));
    }

    @Test
    @DisplayName("Redaction: Should perform case-insensitive matching for JSON field keys")
    void testCaseInsensitiveJsonFieldRedaction() {
        String json = "{\"PASSWORD\":\"myPassword\",\"Access_Token\":\"tok123\",\"ApiKey\":\"key456\"}";
        String redacted = executionService.redactSensitiveJson(json);
        assertTrue(redacted.contains("\"PASSWORD\":\"[REDACTED]\""));
        assertTrue(redacted.contains("\"Access_Token\":\"[REDACTED]\""));
        assertTrue(redacted.contains("\"ApiKey\":\"[REDACTED]\""));
        assertFalse(redacted.contains("myPassword"));
        assertFalse(redacted.contains("tok123"));
    }

    @Test
    @DisplayName("Redaction: Should return non-JSON or malformed payloads without modification or exceptions")
    void testNonJsonPayloadRedactionSafeFallback() {
        String nonJson = "user=admin&password=secretpassword";
        assertEquals(nonJson, executionService.redactSensitiveJson(nonJson));

        String malformedJson = "{not-valid-json";
        assertEquals(malformedJson, executionService.redactSensitiveJson(malformedJson));

        assertNull(executionService.redactSensitiveJson(null));
        assertEquals("", executionService.redactSensitiveJson(""));
    }

    @Test
    @DisplayName("Redaction: Should redact URL-encoded sensitive parameter names")
    void testUrlEncodedSensitiveParamRedaction() {
        String url = "https://api.example.com/data?api%5Fkey=secret123&page=1";
        String redacted = executionService.redactSensitiveUrl(url);
        assertEquals("https://api.example.com/data?api%5Fkey=[REDACTED]&page=1", redacted);
    }
}

