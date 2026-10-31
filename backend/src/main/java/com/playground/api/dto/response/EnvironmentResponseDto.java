package com.playground.api.dto.response;

import com.playground.api.dto.request.EnvironmentVariableDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvironmentResponseDto {
    private Long id;
    private String name;
    private Boolean isActive;
    @Builder.Default
    private List<EnvironmentVariableDto> variables = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
