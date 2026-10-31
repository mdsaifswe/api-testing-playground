package com.playground.api.security;

import com.playground.api.config.PlaygroundSecurityProperties;
import com.playground.api.exception.SsrfSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

class SsrfValidatorTest {

    private SsrfValidator validator;
    private PlaygroundSecurityProperties properties;

    @BeforeEach
    void setUp() {
        properties = new PlaygroundSecurityProperties();
        properties.setAllowLoopback(true); // Loopback allowed for local dev
        properties.setAllowPrivateIps(true);
        validator = new SsrfValidator(properties);
    }

    @Test
    @DisplayName("Should permit valid HTTP and HTTPS URLs")
    void testValidUrls() {
        URI uri1 = validator.validateUrl("http://localhost:8080/api/v1/echo/get");
        assertNotNull(uri1);
        assertEquals("localhost", uri1.getHost());

        URI uri2 = validator.validateUrl("https://httpbin.org/get");
        assertNotNull(uri2);
        assertEquals("httpbin.org", uri2.getHost());
    }

    @Test
    @DisplayName("Should block non-HTTP schemes like file, ftp, gopher")
    void testDisallowedSchemes() {
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl("file:///etc/passwd"));
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl("ftp://192.168.1.1/secret"));
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl("gopher://127.0.0.1:6379"));
    }

    @Test
    @DisplayName("Should block AWS/GCP cloud metadata IP 169.254.169.254 unconditionally")
    void testBlockCloudMetadata() {
        assertThrows(SsrfSecurityException.class, () ->
                validator.validateUrl("http://169.254.169.254/latest/meta-data/iam/security-credentials/"));
        assertThrows(SsrfSecurityException.class, () ->
                validator.validateUrl("http://metadata.google.internal/computeMetadata/v1/"));
    }

    @Test
    @DisplayName("Should block dangerous database and management ports like 22, 3306, 6379, 2375")
    void testBlockDangerousPorts() {
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl("http://localhost:22/"));
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl("http://localhost:3306/"));
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl("http://localhost:6379/"));
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl("http://localhost:2375/version"));
    }

    @Test
    @DisplayName("Should block loopback addresses when allowLoopback is false")
    void testStrictLoopbackBlocking() {
        properties.setAllowLoopback(false);
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl("http://127.0.0.1:8080/api"));
        assertThrows(SsrfSecurityException.class, () -> validator.validateUrl("http://localhost:8080/api"));
    }
}
