package com.playground.api.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyValueDto {
    private String key;
    private String value;
    @Builder.Default
    private boolean enabled = true;
}
