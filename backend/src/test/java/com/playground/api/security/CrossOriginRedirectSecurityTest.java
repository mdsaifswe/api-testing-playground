package com.playground.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.api.config.PlaygroundSecurityProperties;
import com.playground.api.dto.common.AuthDto;
import com.playground.api.dto.common.KeyValueDto;
import com.playground.api.repository.RequestHistoryRepository;
import com.playground.api.service.RequestExecutionService;
import com.playground.api.service.VariableSubstitutionService;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.core5.http.Header;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CrossOriginRedirectSecurityTest {

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

        // By default substitution passes through strings regardless of whether envId is provided or null
        lenient().when(substitutionService.substitute(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Origin Comparison: Identifies matching same-origin URIs correctly")
    void testSameOriginDetection() {
        URI base = URI.create("https://api.example.com/v1/users");
        URI sameOriginPath = URI.create("https://api.example.com/v2/posts");
        URI sameOriginDefaultPort = URI.create("https://api.example.com:443/v1/auth");

        assertTrue(executionService.isSameOrigin(base, sameOriginPath));
        assertTrue(executionService.isSameOrigin(base, sameOriginDefaultPort));

        URI httpBase = URI.create("http://internal.service:80/data");
        URI httpSame = URI.create("http://internal.service/info");
        assertTrue(executionService.isSameOrigin(httpBase, httpSame));
    }

    @Test
    @DisplayName("Origin Comparison: Detects cross-origin transitions (different host, scheme, or port)")
    void testCrossOriginDetection() {
        URI base = URI.create("https://api.example.com/data");

        // Different host
        URI diffHost = URI.create("https://attacker.com/data");
        assertFalse(executionService.isSameOrigin(base, diffHost));

        // Subdomain (not same origin per RFC 6454)
        URI subDomain = URI.create("https://sub.api.example.com/data");
        assertFalse(executionService.isSameOrigin(base, subDomain));

        // Different scheme (HTTPS to HTTP)
        URI diffScheme = URI.create("http://api.example.com/data");
        assertFalse(executionService.isSameOrigin(base, diffScheme));

        // Different port
        URI diffPort = URI.create("https://api.example.com:8443/data");
        assertFalse(executionService.isSameOrigin(base, diffPort));
    }

    @Test
    @DisplayName("Cross-Origin Redirect: Strips sensitive headers and AuthDto credentials when redirecting cross-origin")
    void testStripSensitiveCredentialsOnCrossOriginRedirect() {
        HttpGet request = new HttpGet("https://other-domain.com/landing");

        List<KeyValueDto> headers = Arrays.asList(
                new KeyValueDto("Authorization", "Bearer sensitive-user-token", true),
                new KeyValueDto("Proxy-Authorization", "Basic credentials", true),
                new KeyValueDto("Cookie", "session=secret-session-id", true),
                new KeyValueDto("Set-Cookie", "auth_token=123", true),
                new KeyValueDto("X-API-Key", "my-secret-key", true),
                new KeyValueDto("X-Auth-Token", "token456", true),
                new KeyValueDto("Accept", "application/json", true),
                new KeyValueDto("X-Correlation-Id", "corr-9999", true)
        );

        AuthDto auth = AuthDto.builder()
                .type("API_KEY")
                .placement("HEADER")
                .keyName("X-Custom-Auth")
                .keyValue("confidential-api-key")
                .build();

        // Cross-origin redirect execution
        executionService.applyHeaders(request, headers, auth, null, true);

        // Verify sensitive headers are stripped
        assertNull(request.getFirstHeader("Authorization"), "Authorization must be stripped on cross-origin redirect");
        assertNull(request.getFirstHeader("Proxy-Authorization"), "Proxy-Authorization must be stripped on cross-origin redirect");
        assertNull(request.getFirstHeader("Cookie"), "Cookie must be stripped on cross-origin redirect");
        assertNull(request.getFirstHeader("Set-Cookie"), "Set-Cookie must be stripped on cross-origin redirect");
        assertNull(request.getFirstHeader("X-API-Key"), "X-API-Key must be stripped on cross-origin redirect");
        assertNull(request.getFirstHeader("X-Auth-Token"), "X-Auth-Token must be stripped on cross-origin redirect");
        assertNull(request.getFirstHeader("X-Custom-Auth"), "AuthDto credentials must not attach to cross-origin redirect");

        // Verify non-sensitive headers are preserved
        assertNotNull(request.getFirstHeader("Accept"));
        assertEquals("application/json", request.getFirstHeader("Accept").getValue());
        assertNotNull(request.getFirstHeader("X-Correlation-Id"));
        assertEquals("corr-9999", request.getFirstHeader("X-Correlation-Id").getValue());
    }

    @Test
    @DisplayName("Same-Origin Redirect: Preserves credentials and sensitive headers on same-origin redirects")
    void testPreserveCredentialsOnSameOriginRedirect() {
        HttpGet request = new HttpGet("https://api.example.com/v2/new-target");

        List<KeyValueDto> headers = Arrays.asList(
                new KeyValueDto("Authorization", "Bearer sensitive-user-token", true),
                new KeyValueDto("Cookie", "session=secret-session-id", true),
                new KeyValueDto("Accept", "application/json", true)
        );

        AuthDto auth = AuthDto.builder()
                .type("API_KEY")
                .placement("HEADER")
                .keyName("X-API-Key")
                .keyValue("my-api-key-value")
                .build();

        // Same-origin redirect execution (isCrossOrigin = false)
        executionService.applyHeaders(request, headers, auth, null, false);

        // Verify credentials and sensitive headers are preserved
        assertNotNull(request.getFirstHeader("Authorization"));
        assertEquals("Bearer sensitive-user-token", request.getFirstHeader("Authorization").getValue());
        assertNotNull(request.getFirstHeader("Cookie"));
        assertEquals("session=secret-session-id", request.getFirstHeader("Cookie").getValue());
        assertNotNull(request.getFirstHeader("Accept"));
        assertEquals("application/json", request.getFirstHeader("Accept").getValue());

        // Verify AuthDto is applied
        Header[] authHeaders = request.getHeaders("X-API-Key");
        assertTrue(authHeaders.length > 0);
        assertEquals("my-api-key-value", authHeaders[0].getValue());
    }
}
