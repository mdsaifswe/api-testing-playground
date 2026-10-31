package com.playground.api.security;

import com.playground.api.config.PlaygroundSecurityProperties;
import com.playground.api.exception.SsrfSecurityException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class RedirectSsrfAndDnsSecurityTest {

    private SsrfValidator validator;
    private PlaygroundSecurityProperties properties;
    private SafeDnsResolver safeDnsResolver;

    @BeforeEach
    void setUp() {
        properties = new PlaygroundSecurityProperties();
        properties.setAllowLoopback(false); // Strict mode for testing security violations
        properties.setAllowPrivateIps(false);
        validator = new SsrfValidator(properties);
        safeDnsResolver = new SafeDnsResolver(validator);
    }

    @Test
    @DisplayName("Redirect: Should block redirect to localhost / loopback")
    void testRedirectToLocalhostBlocked() {
        URI initialUri = URI.create("https://httpbin.org/redirect");
        assertThrows(SsrfSecurityException.class, () ->
                validator.resolveAndValidateRedirect(initialUri, "http://127.0.0.1:8080/admin"));
        assertThrows(SsrfSecurityException.class, () ->
                validator.resolveAndValidateRedirect(initialUri, "http://localhost:8080/internal"));
    }

    @Test
    @DisplayName("Redirect: Should block redirect to private RFC 1918 IPs")
    void testRedirectToPrivateIpBlocked() {
        URI initialUri = URI.create("https://httpbin.org/redirect");
        assertThrows(SsrfSecurityException.class, () ->
                validator.resolveAndValidateRedirect(initialUri, "http://10.0.0.1/config"));
        assertThrows(SsrfSecurityException.class, () ->
                validator.resolveAndValidateRedirect(initialUri, "http://192.168.1.1/router"));
        assertThrows(SsrfSecurityException.class, () ->
                validator.resolveAndValidateRedirect(initialUri, "http://172.16.0.5/api"));
    }

    @Test
    @DisplayName("Redirect: Should block redirect to AWS/GCP cloud metadata IP")
    void testRedirectToCloudMetadataBlocked() {
        URI initialUri = URI.create("https://httpbin.org/redirect");
        assertThrows(SsrfSecurityException.class, () ->
                validator.resolveAndValidateRedirect(initialUri, "http://169.254.169.254/latest/meta-data"));
        assertThrows(SsrfSecurityException.class, () ->
                validator.resolveAndValidateRedirect(initialUri, "http://metadata.google.internal/computeMetadata"));
    }

    @Test
    @DisplayName("Redirect: Should permit valid redirect to public target")
    void testRedirectToPermittedPublicTarget() {
        URI initialUri = URI.create("https://httpbin.org/redirect");
        URI nextUri = validator.resolveAndValidateRedirect(initialUri, "https://httpbin.org/get");
        assertNotNull(nextUri);
        assertEquals("httpbin.org", nextUri.getHost());
        assertEquals("/get", nextUri.getPath());
    }

    @Test
    @DisplayName("Redirect: Should correctly resolve relative redirect destinations")
    void testRelativeRedirect() {
        URI initialUri = URI.create("https://httpbin.org/api/v1/old-endpoint");
        URI nextUri = validator.resolveAndValidateRedirect(initialUri, "/api/v2/new-endpoint");
        assertNotNull(nextUri);
        assertEquals("https://httpbin.org/api/v2/new-endpoint", nextUri.toString());

        URI relativeNext = validator.resolveAndValidateRedirect(initialUri, "../v2/target");
        assertEquals("https://httpbin.org/api/v2/target", relativeNext.toString());
    }

    @Test
    @DisplayName("Redirect: Should reject dangerous schemes in redirect location")
    void testDangerousRedirectSchemes() {
        URI initialUri = URI.create("https://httpbin.org/redirect");
        assertThrows(SsrfSecurityException.class, () ->
                validator.resolveAndValidateRedirect(initialUri, "file:///etc/shadow"));
        assertThrows(SsrfSecurityException.class, () ->
                validator.resolveAndValidateRedirect(initialUri, "gopher://127.0.0.1:6379/"));
    }

    @Test
    @DisplayName("DNS Rebinding / SafeDnsResolver: Should reject resolution to cloud metadata IP")
    void testDnsResolutionCloudMetadataBlocked() {
        assertThrows(UnknownHostException.class, () ->
                safeDnsResolver.resolve("169.254.169.254"));
    }

    @Test
    @DisplayName("DNS Rebinding / SafeDnsResolver: Should reject resolution to loopback when disallowed")
    void testDnsResolutionLoopbackBlocked() {
        assertThrows(UnknownHostException.class, () ->
                safeDnsResolver.resolve("127.0.0.1"));
    }

    @Test
    @DisplayName("DNS Rebinding / SafeDnsResolver: Should reject IPv6 loopback (::1)")
    void testDnsResolutionIpv6LoopbackBlocked() {
        assertThrows(UnknownHostException.class, () ->
                safeDnsResolver.resolve("::1"));
    }

    @Test
    @DisplayName("DNS Rebinding / SafeDnsResolver: Should validate address directly")
    void testDirectAddressValidation() throws UnknownHostException {
        // Cloud metadata
        InetAddress metadataAddr = InetAddress.getByName("169.254.169.254");
        assertThrows(SsrfSecurityException.class, () -> validator.validateAddress(metadataAddr));

        // Loopback
        InetAddress loopbackAddr = InetAddress.getByName("127.0.0.1");
        assertThrows(SsrfSecurityException.class, () -> validator.validateAddress(loopbackAddr));

        // Private IP
        InetAddress privateAddr = InetAddress.getByName("10.0.0.1");
        assertThrows(SsrfSecurityException.class, () -> validator.validateAddress(privateAddr));
    }

    @Test
    @DisplayName("IPv6 SSRF: Should reject IPv6 Unique Local Address range fc00::/7 (fc00::/8 and fd00::/8)")
    void testIpv6UniqueLocalAddressesBlocked() throws UnknownHostException {
        // fc00::/8
        InetAddress fc00Addr = InetAddress.getByName("fc00::1");
        assertThrows(SsrfSecurityException.class, () -> validator.validateAddress(fc00Addr));

        // fd00::/8
        InetAddress fd00Addr = InetAddress.getByName("fd12:3456:789a:1::1");
        assertThrows(SsrfSecurityException.class, () -> validator.validateAddress(fd00Addr));
    }

    @Test
    @DisplayName("IPv6 SSRF: Should reject IPv4-mapped IPv6 addresses targeting loopback, cloud metadata, and private IPs")
    void testIpv4MappedIpv6Blocked() throws UnknownHostException {
        // IPv4-mapped loopback ::ffff:127.0.0.1
        InetAddress mappedLoopback = InetAddress.getByName("::ffff:127.0.0.1");
        assertThrows(SsrfSecurityException.class, () -> validator.validateAddress(mappedLoopback));

        // IPv4-mapped cloud metadata ::ffff:169.254.169.254
        InetAddress mappedMetadata = InetAddress.getByName("::ffff:169.254.169.254");
        assertThrows(SsrfSecurityException.class, () -> validator.validateAddress(mappedMetadata));

        // IPv4-mapped private IP ::ffff:10.0.0.1
        InetAddress mappedPrivate = InetAddress.getByName("::ffff:10.0.0.1");
        assertThrows(SsrfSecurityException.class, () -> validator.validateAddress(mappedPrivate));
    }

    @Test
    @DisplayName("IPv6 SSRF: Should permit global public IPv6 address when safe")
    void testPermittedPublicIpv6() throws UnknownHostException {
        // Google Public DNS IPv6
        InetAddress globalIpv6 = InetAddress.getByName("2001:4860:4860::8888");
        assertDoesNotThrow(() -> validator.validateAddress(globalIpv6));
    }
}
