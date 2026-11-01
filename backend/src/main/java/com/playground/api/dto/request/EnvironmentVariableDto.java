package com.playground.api.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentVariableDto {

    private Long id;

    @NotBlank(message = "Variable key is required")
    private String key;

    private String value;

    @JsonProperty("isSecret")
    @JsonAlias({"secret", "isSecret"})
    @Builder.Default
    private boolean isSecret = false;

    @JsonProperty("isEnabled")
    @JsonAlias({"enabled", "isEnabled"})
    @Builder.Default
    private boolean isEnabled = true;
}
