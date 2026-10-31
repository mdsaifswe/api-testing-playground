package com.playground.api.dto.request;

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

    @Builder.Default
    private boolean isSecret = false;

    @Builder.Default
    private boolean isEnabled = true;
}
