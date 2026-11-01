package com.playground.api.config;

import com.playground.api.security.SsrfValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RenderDeploymentConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    @DisplayName("Dynamic Port: application.yml must configure server.port using ${PORT:8080}")
    void testApplicationYmlPortConfiguration() throws Exception {
        ClassPathResource resource = new ClassPathResource("application.yml");
        assertTrue(resource.exists(), "application.yml must exist");

        Yaml yaml = new Yaml();
        try (InputStream in = resource.getInputStream()) {
            Map<String, Object> root = yaml.load(in);
            assertNotNull(root);
            @SuppressWarnings("unchecked")
            Map<String, Object> server = (Map<String, Object>) root.get("server");
            assertNotNull(server, "server block must be defined in application.yml");

            Object portValue = server.get("port");
            assertNotNull(portValue);
            assertEquals("${PORT:8080}", portValue.toString(),
                    "server.port must use ${PORT:8080} to support Render's dynamic port assignment with an 8080 fallback");
        }
    }

    @Test
    @DisplayName("Dynamic Port: Spring Environment should resolve PORT env variable and fallback to 8080")
    void testDynamicPortResolution() {
        // Without PORT property -> resolves to 8080
        contextRunner
                .withPropertyValues("server.port=${PORT:8080}")
                .run(context -> {
                    String port = context.getEnvironment().getProperty("server.port");
                    assertEquals("8080", port, "Port should fallback to 8080 when PORT is not set");
                });

        // With PORT property (simulating Render injection) -> resolves to Render port
        contextRunner
                .withPropertyValues("PORT=10000", "server.port=${PORT:8080}")
                .run(context -> {
                    String port = context.getEnvironment().getProperty("server.port");
                    assertEquals("10000", port, "Port should bind to Render PORT environment variable when present");
                });
    }

    @Test
    @DisplayName("CORS Configuration: application.yml must define playground.cors.allowed-origins")
    void testApplicationYmlCorsConfiguration() throws Exception {
        ClassPathResource resource = new ClassPathResource("application.yml");
        Yaml yaml = new Yaml();
        try (InputStream in = resource.getInputStream()) {
            Map<String, Object> root = yaml.load(in);
            @SuppressWarnings("unchecked")
            Map<String, Object> playground = (Map<String, Object>) root.get("playground");
            assertNotNull(playground, "playground block must be defined");

            @SuppressWarnings("unchecked")
            Map<String, Object> cors = (Map<String, Object>) playground.get("cors");
            assertNotNull(cors, "playground.cors block must be defined");
            assertTrue(cors.containsKey("allowed-origins"), "playground.cors.allowed-origins must be defined");

            String allowedOriginsValue = cors.get("allowed-origins").toString();
            assertTrue(allowedOriginsValue.contains("PLAYGROUND_CORS_ALLOWED_ORIGINS"),
                    "allowed-origins must support PLAYGROUND_CORS_ALLOWED_ORIGINS environment variable");
            assertTrue(allowedOriginsValue.contains("localhost"),
                    "Default allowed-origins must contain localhost for local dev");
        }
    }

    @Test
    @DisplayName("CORS Defaults: Must preserve localhost and MUST NOT allow arbitrary pages.dev domains by default")
    void testCorsDefaultsSafety() {
        CorsConfig config = new CorsConfig(null);
        List<String> origins = config.getAllowedOrigins();

        assertNotNull(origins);
        assertFalse(origins.isEmpty());

        // Must contain localhost patterns
        assertTrue(origins.stream().anyMatch(o -> o.contains("localhost")), "Default CORS must allow localhost");

        // MUST NOT allow arbitrary pages.dev by default (security requirement)
        assertFalse(origins.stream().anyMatch(o -> o.contains("pages.dev")),
                "Default CORS must NOT allow arbitrary pages.dev domains; exact domain must be explicitly configured");
    }

    @Test
    @DisplayName("CORS Custom Configuration: Comma-separated list with whitespace must be properly trimmed and parsed")
    void testCorsCustomConfigurationParsing() {
        List<String> rawInputs = List.of(
                "https://my-custom-project.pages.dev, http://localhost:5173",
                "https://api-testing-app.pages.dev"
        );

        CorsConfig config = new CorsConfig(rawInputs);
        List<String> origins = config.getAllowedOrigins();

        assertEquals(3, origins.size());
        assertTrue(origins.contains("https://my-custom-project.pages.dev"));
        assertTrue(origins.contains("http://localhost:5173"));
        assertTrue(origins.contains("https://api-testing-app.pages.dev"));
        // Ensure no untrimmed whitespace
        assertFalse(origins.contains(" http://localhost:5173"));
    }

    @Test
    @DisplayName("CORS Custom Configuration: Quotes and trailing slashes must be sanitized")
    void testCorsSanitization() {
        List<String> rawInputs = List.of(
                "\"https://api-testing-playground-a36.pages.dev/\"",
                "'http://localhost:5173/'"
        );

        CorsConfig config = new CorsConfig(rawInputs);
        List<String> origins = config.getAllowedOrigins();

        assertEquals(2, origins.size());
        assertTrue(origins.contains("https://api-testing-playground-a36.pages.dev"));
        assertTrue(origins.contains("http://localhost:5173"));
        assertFalse(origins.contains("https://api-testing-playground-a36.pages.dev/"));

        org.springframework.web.cors.CorsConfiguration corsConfig = config.createCorsConfiguration();
        assertTrue(corsConfig.getAllowedMethods().containsAll(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")));
        assertTrue(corsConfig.getAllowedHeaders().containsAll(List.of("Content-Type", "Authorization", "X-Workspace-Key")));
    }

    @Test
    @DisplayName("SSRF Defaults: application.yml must default allow-loopback and allow-private-ips to true for local development")
    void testApplicationYmlSsrfDefaults() throws Exception {
        ClassPathResource resource = new ClassPathResource("application.yml");
        Yaml yaml = new Yaml();
        try (InputStream in = resource.getInputStream()) {
            Map<String, Object> root = yaml.load(in);
            @SuppressWarnings("unchecked")
            Map<String, Object> playground = (Map<String, Object>) root.get("playground");
            @SuppressWarnings("unchecked")
            Map<String, Object> security = (Map<String, Object>) playground.get("security");
            assertNotNull(security, "playground.security block must be defined");

            assertEquals("${PLAYGROUND_ALLOW_LOOPBACK:true}", security.get("allow-loopback").toString(),
                    "allow-loopback must default to true for local development and local echo server");
            assertEquals("${PLAYGROUND_ALLOW_PRIVATE_IPS:true}", security.get("allow-private-ips").toString(),
                    "allow-private-ips must default to true for local development");
        }
    }

    @Test
    @DisplayName("SSRF Defaults: application-postgres.yml must strictly default allow-loopback and allow-private-ips to false")
    void testApplicationPostgresYmlSsrfDefaults() throws Exception {
        ClassPathResource resource = new ClassPathResource("application-postgres.yml");
        Yaml yaml = new Yaml();
        try (InputStream in = resource.getInputStream()) {
            Map<String, Object> root = yaml.load(in);
            @SuppressWarnings("unchecked")
            Map<String, Object> playground = (Map<String, Object>) root.get("playground");
            assertNotNull(playground, "playground block must be defined in application-postgres.yml");
            @SuppressWarnings("unchecked")
            Map<String, Object> security = (Map<String, Object>) playground.get("security");
            assertNotNull(security, "playground.security block must be defined in application-postgres.yml");

            assertEquals("${PLAYGROUND_ALLOW_LOOPBACK:false}", security.get("allow-loopback").toString(),
                    "Production PostgreSQL profile must strictly default allow-loopback to false");
            assertEquals("${PLAYGROUND_ALLOW_PRIVATE_IPS:false}", security.get("allow-private-ips").toString(),
                    "Production PostgreSQL profile must strictly default allow-private-ips to false");
        }
    }

    @Test
    @DisplayName("SSRF Defaults: Render environment can explicitly enforce false or true")
    void testSsrfEnvironmentEnforcement() {
        contextRunner
                .withPropertyValues(
                        "playground.security.allow-loopback=${PLAYGROUND_ALLOW_LOOPBACK:false}",
                        "playground.security.allow-private-ips=${PLAYGROUND_ALLOW_PRIVATE_IPS:false}"
                )
                .run(context -> {
                    assertEquals("false", context.getEnvironment().getProperty("playground.security.allow-loopback"));
                    assertEquals("false", context.getEnvironment().getProperty("playground.security.allow-private-ips"));
                });

        contextRunner
                .withPropertyValues(
                        "PLAYGROUND_ALLOW_LOOPBACK=true",
                        "PLAYGROUND_ALLOW_PRIVATE_IPS=true",
                        "playground.security.allow-loopback=${PLAYGROUND_ALLOW_LOOPBACK:false}",
                        "playground.security.allow-private-ips=${PLAYGROUND_ALLOW_PRIVATE_IPS:false}"
                )
                .run(context -> {
                    assertEquals("true", context.getEnvironment().getProperty("playground.security.allow-loopback"));
                    assertEquals("true", context.getEnvironment().getProperty("playground.security.allow-private-ips"));
                });
    }

    @Test
    @DisplayName("Dockerfile: Runtime stage must configure container-aware memory and exec entrypoint")
    void testDockerfileConfiguration() throws Exception {
        Path dockerfilePath = Path.of("Dockerfile");
        if (!Files.exists(dockerfilePath)) {
            dockerfilePath = Path.of("backend", "Dockerfile");
        }
        assertTrue(Files.exists(dockerfilePath), "Dockerfile must exist");

        String dockerfileContent = Files.readString(dockerfilePath, StandardCharsets.UTF_8);

        assertTrue(dockerfileContent.contains("JAVA_OPTS"), "Dockerfile must declare JAVA_OPTS");
        assertTrue(dockerfileContent.contains("UseContainerSupport"), "Dockerfile must use container support");
        assertTrue(dockerfileContent.contains("MaxRAMPercentage"), "Dockerfile must set container MaxRAMPercentage");
        assertTrue(dockerfileContent.contains("exec java"), "Dockerfile entrypoint must use exec to handle signals as PID 1");
    }
}
