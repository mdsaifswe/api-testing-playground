package com.playground.api.service;

import com.playground.api.entity.EnvironmentEntity;
import com.playground.api.entity.EnvironmentVariableEntity;
import com.playground.api.repository.EnvironmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VariableSubstitutionService {

    private final EnvironmentRepository environmentRepository;
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{([a-zA-Z0-9_-]+)\\}\\}");

    /**
     * Resolves variables in a target string using variables from the active environment or given environment.
     */
    @Transactional(readOnly = true)
    public String substitute(String input, Long environmentId) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Map<String, String> varMap = getVariablesMap(environmentId);
        if (varMap.isEmpty()) {
            return input;
        }

        return replaceVariables(input, varMap);
    }

    /**
     * Substitutes using an explicitly provided variable map.
     */
    public String replaceVariables(String input, Map<String, String> variables) {
        if (input == null || variables == null || variables.isEmpty()) {
            return input;
        }

        Matcher matcher = VAR_PATTERN.matcher(input);
        StringBuilder sb = new StringBuilder();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variables.get(varName);
            if (replacement != null) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                // Keep original {{varName}} if not found
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public Map<String, String> getVariablesMap(Long environmentId) {
        EnvironmentEntity env = null;
        if (environmentId != null) {
            env = environmentRepository.findById(environmentId).orElse(null);
        }
        if (env == null) {
            env = environmentRepository.findByIsActiveTrue().orElse(null);
        }

        if (env == null || env.getVariables() == null) {
            return Collections.emptyMap();
        }

        Map<String, String> map = new HashMap<>();
        for (EnvironmentVariableEntity variable : env.getVariables()) {
            if (Boolean.TRUE.equals(variable.getIsEnabled()) && variable.getKey() != null) {
                map.put(variable.getKey(), variable.getValue() != null ? variable.getValue() : "");
            }
        }
        return map;
    }
}
