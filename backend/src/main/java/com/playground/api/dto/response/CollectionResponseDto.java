package com.playground.api.dto.response;

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
public class CollectionResponseDto {
    private Long id;
    private String name;
    private String description;
    @Builder.Default
    private List<SavedRequestResponseDto> requests = new ArrayList<>();
    private int requestCount;
    private Instant createdAt;
    private Instant updatedAt;
}
