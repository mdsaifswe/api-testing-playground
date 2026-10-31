package com.playground.api.dto.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthDto {
    @Builder.Default
    private String type = "NONE"; // NONE, API_KEY

    private String keyName;

    private String keyValue;

    @Builder.Default
    private String placement = "HEADER"; // HEADER, QUERY
}
