package com.playground.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playground.api.config.PlaygroundSecurityProperties;
import com.playground.api.repository.RequestHistoryRepository;
import com.playground.api.security.SafeDnsResolver;
import com.playground.api.security.SsrfValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class BoundedResponseAndTimeoutTest {

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
        properties.setReadTimeoutMs(10000);
        properties.setConnectTimeoutMs(5000);
        properties.setMaxResponseSizeBytes(1000); // Set small 1000 bytes limit for test predictability
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
    @DisplayName("Bounded Streaming: Response below limit should be fully read and preserved")
    void testResponseBelowLimit() throws IOException {
        byte[] payload = "Hello, Secure World!".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream in = new ByteArrayInputStream(payload);

        RequestExecutionService.BoundedReadResult result = executionService.readBoundedBody(in, 1000);

        assertFalse(result.isExceeded(), "Payload below limit should not be marked as exceeded");
        assertEquals("Hello, Secure World!", result.getBody());
        assertEquals(payload.length, result.getSizeBytes());
    }

    @Test
    @DisplayName("Bounded Streaming: Response exactly at limit should be fully read")
    void testResponseExactlyAtLimit() throws IOException {
        byte[] payload = new byte[1000];
        for (int i = 0; i < 1000; i++) {
            payload[i] = 'A';
        }
        ByteArrayInputStream in = new ByteArrayInputStream(payload);

        RequestExecutionService.BoundedReadResult result = executionService.readBoundedBody(in, 1000);

        assertFalse(result.isExceeded(), "Payload exactly at limit should not exceed");
        assertEquals(1000, result.getSizeBytes());
        assertEquals(1000, result.getBody().length());
    }

    @Test
    @DisplayName("Bounded Streaming: Response exceeding limit must be truncated and marked exceeded")
    void testResponseExceedingLimit() throws IOException {
        byte[] payload = new byte[2500];
        for (int i = 0; i < 2500; i++) {
            payload[i] = 'B';
        }
        ByteArrayInputStream in = new ByteArrayInputStream(payload);

        RequestExecutionService.BoundedReadResult result = executionService.readBoundedBody(in, 1000);

        assertTrue(result.isExceeded(), "Payload above limit must be marked exceeded");
        assertEquals("[Response payload exceeded limit of 1000 bytes]", result.getBody());
        assertTrue(result.getSizeBytes() > 1000, "Reported size should reflect bytes consumed up to cutoff");
    }

    @Test
    @DisplayName("Bounded Streaming: Unbounded / infinite stream must stop reading immediately once max bytes is reached")
    void testUnboundedStreamDoesNotConsumeUnlimitedMemory() throws IOException {
        AtomicInteger bytesServed = new AtomicInteger(0);
        AtomicBoolean closed = new AtomicBoolean(false);

        // Infinite stream that would cause OutOfMemoryError if EntityUtils.toByteArray was used
        InputStream infiniteStream = new InputStream() {
            @Override
            public int read() {
                bytesServed.incrementAndGet();
                return 'X';
            }

            @Override
            public int read(byte[] b, int off, int len) {
                int bytesToProvide = Math.min(len, 4096);
                for (int i = 0; i < bytesToProvide; i++) {
                    b[off + i] = 'X';
                }
                bytesServed.addAndGet(bytesToProvide);
                return bytesToProvide;
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };

        long maxLimit = 1000;
        RequestExecutionService.BoundedReadResult result = executionService.readBoundedBody(infiniteStream, maxLimit);

        assertTrue(result.isExceeded());
        assertEquals("[Response payload exceeded limit of 1000 bytes]", result.getBody());
        // Verify bounded reading: bytes served must NOT run away indefinitely (stopped right after exceeding 1000)
        assertTrue(bytesServed.get() < 20_000, "Reading must stop immediately upon reaching max limit; served: " + bytesServed.get());
        assertTrue(closed.get(), "Underlying stream must be closed when reading completes or terminates");
    }

    @Test
    @DisplayName("Bounded Streaming: Null or empty input stream safely returns empty body")
    void testNullOrEmptyStreamHandling() throws IOException {
        RequestExecutionService.BoundedReadResult nullResult = executionService.readBoundedBody(null, 1000);
        assertNotNull(nullResult);
        assertEquals("", nullResult.getBody());
        assertEquals(0, nullResult.getSizeBytes());
        assertFalse(nullResult.isExceeded());

        ByteArrayInputStream emptyIn = new ByteArrayInputStream(new byte[0]);
        RequestExecutionService.BoundedReadResult emptyResult = executionService.readBoundedBody(emptyIn, 1000);
        assertEquals("", emptyResult.getBody());
        assertEquals(0, emptyResult.getSizeBytes());
        assertFalse(emptyResult.isExceeded());
    }

    @Test
    @DisplayName("Timeout Capping: Normal timeout under 15s should be preserved")
    void testNormalTimeoutPreserved() {
        assertEquals(5000, executionService.resolveTimeout(5000));
        assertEquals(12000, executionService.resolveTimeout(12000));
    }

    @Test
    @DisplayName("Timeout Capping: Timeout equal to 15s should be preserved")
    void testFifteenSecondTimeoutPreserved() {
        assertEquals(15000, executionService.resolveTimeout(15000));
    }

    @Test
    @DisplayName("Timeout Capping: Timeout exceeding 15s must be capped at 15,000ms")
    void testExcessiveTimeoutCappedAtFifteenSeconds() {
        assertEquals(RequestExecutionService.HARD_MAX_TIMEOUT_MS, executionService.resolveTimeout(30000));
        assertEquals(RequestExecutionService.HARD_MAX_TIMEOUT_MS, executionService.resolveTimeout(60000));
        assertEquals(RequestExecutionService.HARD_MAX_TIMEOUT_MS, executionService.resolveTimeout(1000000));
    }

    @Test
    @DisplayName("Timeout Capping: Null or non-positive timeout must safely default within 15s")
    void testNullOrNegativeTimeoutSafeDefault() {
        // Null should use configured read timeout (10000ms)
        assertEquals(10000, executionService.resolveTimeout(null));

        // Negative should fallback to safe default
        assertEquals(10000, executionService.resolveTimeout(-500));
        assertEquals(10000, executionService.resolveTimeout(0));

        // If configured read timeout is set higher than 15s, it must still be capped
        properties.setReadTimeoutMs(99999);
        assertEquals(RequestExecutionService.HARD_MAX_TIMEOUT_MS, executionService.resolveTimeout(null));
        assertEquals(RequestExecutionService.HARD_MAX_TIMEOUT_MS, executionService.resolveTimeout(-1));
    }

    @Test
    @DisplayName("Timeout Capping: Connect timeout must never exceed 15,000ms")
    void testConnectTimeoutCapped() {
        properties.setConnectTimeoutMs(5000);
        assertEquals(5000, executionService.resolveConnectTimeout());

        properties.setConnectTimeoutMs(30000);
        assertEquals(RequestExecutionService.HARD_MAX_TIMEOUT_MS, executionService.resolveConnectTimeout());
    }
}
