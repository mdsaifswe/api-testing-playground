package com.playground.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkspaceDto {

    @Size(max = 150, message = "Workspace name cannot exceed 150 characters")
    private String name;
}
