package com.playground.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCollectionDto {

    @NotBlank(message = "Collection name is required")
    @Size(max = 150, message = "Collection name must be 150 characters or less")
    private String name;

    private String description;
}
