package com.playground.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEnvironmentDto {

    @NotBlank(message = "Environment name is required")
    @Size(max = 150, message = "Environment name must be 150 characters or less")
    private String name;

    @Builder.Default
    private Boolean isActive = false;

    @Builder.Default
    private List<EnvironmentVariableDto> variables = new ArrayList<>();
}
