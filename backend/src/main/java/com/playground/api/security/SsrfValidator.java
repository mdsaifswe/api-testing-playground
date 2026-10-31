package com.playground.api.security;

import com.playground.api.config.PlaygroundSecurityProperties;
import com.playground.api.exception.SsrfSecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class SsrfValidator {

    private final PlaygroundSecurityProperties properties;

    // Disallowed ports to prevent port scanning or protocol smuggling attacks against local infrastructure
    private static final Set<Integer> BLOCKED_PORTS = new HashSet<>(Arrays.asList(
            21,    // FTP
            22,    // SSH
            23,    // Telnet
            25,    // SMTP
            69,    // TFTP
            110,   // POP3
            135,   // RPC
            139,   // NetBIOS
            445,   // SMB
            1433,  // MSSQL
            1521,  // Oracle
            2375,  // Docker unencrypted
            2376,  // Docker TLS
            3306,  // MySQL
            5432,  // PostgreSQL
            6379,  // Redis
            9200,  // Elasticsearch
            11211, // Memcached
            27017  // MongoDB
    ));

    private static final String CLOUD_METADATA_IP = "169.254.169.254";

    /**
     * Validates a URL against SSRF vulnerabilities, DNS rebinding, and dangerous targets.
     *
     * @param urlString target URL
     * @return validated URI
     * @throws SsrfSecurityException if URL violates security constraints
     */
    public URI validateUrl(String urlString) {
        if (urlString == null || urlString.trim().isEmpty()) {
            throw new SsrfSecurityException("Target URL cannot be empty.");
        }

        URI uri;
        try {
            uri = new URI(urlString.trim());
        } catch (URISyntaxException e) {
            throw new SsrfSecurityException("Malformed URL syntax: " + e.getMessage());
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))) {
            throw new SsrfSecurityException("Invalid scheme '" + scheme + "'. Only 'http' and 'https' protocols are permitted.");
        }

        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) {
            throw new SsrfSecurityException("URL must specify a valid host name or IP.");
        }

        int port = uri.getPort();
        if (port != -1 && BLOCKED_PORTS.contains(port)) {
            throw new SsrfSecurityException("Access to target port " + port + " is blocked for security.");
        }

        // Validate DNS resolution & destination IP addresses
        validateHostResolution(host);

        return uri;
    }

    private void validateHostResolution(String host) {
        // Strip IPv6 brackets if present
        String cleanHost = host.replaceAll("[\\[\\]]", "");

        // 1. Check direct Cloud Metadata IP or hostname
        if (CLOUD_METADATA_IP.equalsIgnoreCase(cleanHost) || "metadata.google.internal".equalsIgnoreCase(cleanHost)) {
            throw new SsrfSecurityException("Requests to cloud instance metadata service are prohibited.");
        }

        // 2. Perform DNS resolution
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(cleanHost);
        } catch (UnknownHostException e) {
            throw new SsrfSecurityException("DNS resolution failed for host: " + host);
        }

        if (addresses == null || addresses.length == 0) {
            throw new SsrfSecurityException("No IP address could be resolved for host: " + host);
        }

        for (InetAddress address : addresses) {
            validateAddress(address);
        }
    }

    private void validateAddress(InetAddress address) {
        String ip = address.getHostAddress();

        // 1. Cloud metadata
        if (CLOUD_METADATA_IP.equals(ip)) {
            throw new SsrfSecurityException("SSRF Blocked: Destination resolves to cloud metadata IP: " + ip);
        }

        // 2. Link-local
        if (address.isLinkLocalAddress()) {
            throw new SsrfSecurityException("SSRF Blocked: Destination is a link-local address: " + ip);
        }

        // 3. Multicast / AnyLocal / Wildcard
        if (address.isMulticastAddress() || address.isAnyLocalAddress() || "0.0.0.0".equals(ip)) {
            throw new SsrfSecurityException("SSRF Blocked: Multicast or wildcard IP address prohibited: " + ip);
        }

        // 4. Loopback (127.0.0.0/8 or ::1)
        if (address.isLoopbackAddress()) {
            if (!properties.isAllowLoopback()) {
                throw new SsrfSecurityException("SSRF Blocked: Loopback requests (localhost / 127.0.0.1) are prohibited in current security mode.");
            }
            return; // allowed when allowLoopback is true
        }

        // 5. Site-local / Private network (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
        if (address.isSiteLocalAddress()) {
            if (!properties.isAllowPrivateIps()) {
                throw new SsrfSecurityException("SSRF Blocked: Private network destination (" + ip + ") is prohibited in current security mode.");
            }
        }
    }
}
