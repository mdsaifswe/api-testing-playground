package com.playground.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "playground.security")
public class PlaygroundSecurityProperties {

    /**
     * Whether loopback addresses (127.0.0.1, localhost, ::1) are permitted.
     * True by default for local development so that developers can test built-in echo endpoints.
     * Overridden to false in production profiles (e.g. application-postgres.yml).
     */
    private boolean allowLoopback = true;

    /**
     * Whether RFC 1918 private IP ranges are permitted.
     * True by default for local development.
     * Overridden to false in production profiles (e.g. application-postgres.yml).
     */
    private boolean allowPrivateIps = true;

    /**
     * Maximum number of HTTP redirect hops to follow.
     */
    private int maxRedirects = 5;

    /**
     * Outbound HTTP connect timeout in milliseconds.
     */
    private int connectTimeoutMs = 10000;

    /**
     * Outbound HTTP socket/read timeout in milliseconds.
     */
    private int readTimeoutMs = 30000;

    /**
     * Maximum response payload size allowed in bytes (default: 5MB).
     */
    private long maxResponseSizeBytes = 5242880L;
}
