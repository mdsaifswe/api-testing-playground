package com.playground.api.service;

import com.playground.api.repository.EnvironmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class VariableSubstitutionServiceTest {

    @Mock
    private EnvironmentRepository environmentRepository;

    private VariableSubstitutionService service;

    @BeforeEach
    void setUp() {
        service = new VariableSubstitutionService(environmentRepository);
    }

    @Test
    @DisplayName("Should replace single and multiple variables correctly")
    void testVariableReplacement() {
        Map<String, String> vars = new HashMap<>();
        vars.put("baseUrl", "https://api.example.com");
        vars.put("version", "v1");
        vars.put("userId", "12345");

        String template = "{{baseUrl}}/{{version}}/users/{{userId}}";
        String resolved = service.replaceVariables(template, vars);

        assertEquals("https://api.example.com/v1/users/12345", resolved);
    }

    @Test
    @DisplayName("Should leave undefined variables unchanged")
    void testUndefinedVariables() {
        Map<String, String> vars = new HashMap<>();
        vars.put("host", "localhost");

        String template = "http://{{host}}:8080/{{unknownPath}}";
        String resolved = service.replaceVariables(template, vars);

        assertEquals("http://localhost:8080/{{unknownPath}}", resolved);
    }

    @Test
    @DisplayName("Should handle null and empty inputs safely")
    void testNullAndEmpty() {
        assertEquals("", service.replaceVariables("", Map.of()));
        assertEquals(null, service.replaceVariables(null, Map.of()));
    }
}
