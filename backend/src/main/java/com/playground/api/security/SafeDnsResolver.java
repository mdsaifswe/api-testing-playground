package com.playground.api.security;

import com.playground.api.exception.SsrfSecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.SystemDefaultDnsResolver;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Custom Apache HttpClient 5 DNS Resolver that intercepts DNS resolution at the
 * socket-connection layer to defend against Time-of-Check / Time-of-Use (TOCTOU)
 * DNS rebinding attacks.
 *
 * It validates every resolved IP address against SSRF policies right before
 * the connection is established, while preserving the target hostname for
 * HTTP Host headers, TLS SNI, and certificate validation.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeDnsResolver implements DnsResolver {

    private final SsrfValidator ssrfValidator;

    @Override
    public InetAddress[] resolve(String host) throws UnknownHostException {
        if (host == null || host.trim().isEmpty()) {
            throw new UnknownHostException("Host is null or empty");
        }

        String cleanHost = host.replaceAll("[\\[\\]]", "").trim();

        // Perform DNS resolution
        InetAddress[] addresses = SystemDefaultDnsResolver.INSTANCE.resolve(cleanHost);
        if (addresses == null || addresses.length == 0) {
            throw new UnknownHostException("No IP address could be resolved for host: " + host);
        }

        // Validate each resolved address at connection time
        for (InetAddress address : addresses) {
            try {
                ssrfValidator.validateAddress(address);
            } catch (SsrfSecurityException e) {
                log.warn("DNS Rebinding / SSRF blocked at connection time for host '{}' -> {}: {}", host, address.getHostAddress(), e.getMessage());
                throw new UnknownHostException("Connection blocked by SSRF policy: " + e.getMessage());
            }
        }

        return addresses;
    }

    @Override
    public String resolveCanonicalHostname(String host) throws UnknownHostException {
        return SystemDefaultDnsResolver.INSTANCE.resolveCanonicalHostname(host);
    }
}
