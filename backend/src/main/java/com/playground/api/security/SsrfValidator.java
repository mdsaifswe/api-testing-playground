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

    /**
     * Resolves a redirect location against the current URI (supporting relative and absolute locations)
     * and strictly validates the resulting destination through SSRF policies.
     *
     * @param currentUri current request URI
     * @param location target location from redirect response
     * @return validated target URI
     */
    public URI resolveAndValidateRedirect(URI currentUri, String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new SsrfSecurityException("Redirect Location header is empty.");
        }
        URI resolved;
        try {
            resolved = currentUri.resolve(location.trim());
        } catch (IllegalArgumentException e) {
            throw new SsrfSecurityException("Malformed redirect location: " + location);
        }
        return validateUrl(resolved.toString());
    }

    public void validateAddress(InetAddress address) {
        if (address == null) {
            throw new SsrfSecurityException("SSRF Blocked: Destination address is null.");
        }

        byte[] rawBytes = address.getAddress();
        String ip = address.getHostAddress();

        // 1. Handle IPv4-mapped IPv6 addresses (::ffff:x.x.x.x)
        if (rawBytes.length == 16 && isIpv4MappedIpv6(rawBytes)) {
            try {
                byte[] ipv4Bytes = Arrays.copyOfRange(rawBytes, 12, 16);
                InetAddress unmapped = InetAddress.getByAddress(ipv4Bytes);
                validateAddress(unmapped);
                return;
            } catch (UnknownHostException ignored) {}
        }

        // 2. Cloud metadata IP (169.254.169.254)
        if (CLOUD_METADATA_IP.equals(ip)) {
            throw new SsrfSecurityException("SSRF Blocked: Destination resolves to cloud metadata IP: " + ip);
        }

        // 3. Link-local (IPv4 169.254.0.0/16 or IPv6 fe80::/10)
        if (address.isLinkLocalAddress()) {
            throw new SsrfSecurityException("SSRF Blocked: Destination is a link-local address: " + ip);
        }

        // 4. Multicast / AnyLocal / Wildcard (0.0.0.0, ::)
        if (address.isMulticastAddress() || address.isAnyLocalAddress() || "0.0.0.0".equals(ip) || "::".equals(ip) || "0:0:0:0:0:0:0:0".equals(ip)) {
            throw new SsrfSecurityException("SSRF Blocked: Multicast or wildcard IP address prohibited: " + ip);
        }

        // 5. Loopback (IPv4 127.0.0.0/8 or IPv6 ::1)
        if (address.isLoopbackAddress()) {
            if (!properties.isAllowLoopback()) {
                throw new SsrfSecurityException("SSRF Blocked: Loopback requests (localhost / 127.0.0.1 / ::1) are prohibited in current security mode.");
            }
            return; // allowed when allowLoopback is true
        }

        // 6. IPv6 Unique Local Addresses (fc00::/7 covering fc00::/8 and fd00::/8)
        if (rawBytes.length == 16 && (rawBytes[0] & (byte) 0xFE) == (byte) 0xFC) {
            if (!properties.isAllowPrivateIps()) {
                throw new SsrfSecurityException("SSRF Blocked: IPv6 Unique Local Address (" + ip + ") is prohibited in current security mode.");
            }
        }

        // 7. Site-local / Private network (IPv4 RFC 1918: 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16; IPv6 deprecated fec0::/10)
        if (address.isSiteLocalAddress()) {
            if (!properties.isAllowPrivateIps()) {
                throw new SsrfSecurityException("SSRF Blocked: Private network destination (" + ip + ") is prohibited in current security mode.");
            }
        }
    }

    private boolean isIpv4MappedIpv6(byte[] bytes) {
        if (bytes == null || bytes.length != 16) return false;
        for (int i = 0; i < 10; i++) {
            if (bytes[i] != 0) return false;
        }
        return bytes[10] == (byte) 0xFF && bytes[11] == (byte) 0xFF;
    }
}
