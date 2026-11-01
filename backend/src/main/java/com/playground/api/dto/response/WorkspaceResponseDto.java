package com.playground.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkspaceResponseDto {

    private Long id;
    private String name;
    private String workspaceKey;
    private Instant createdAt;
}
