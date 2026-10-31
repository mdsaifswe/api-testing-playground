package com.playground.api.dto.request;

import com.playground.api.dto.common.AuthDto;
import com.playground.api.dto.common.KeyValueDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class ExecuteRequestDto {

    @NotBlank(message = "HTTP method is required")
    @Pattern(regexp = "^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)$", message = "Invalid HTTP method")
    private String method;

    @NotBlank(message = "URL is required")
    private String url;

    @Builder.Default
    private List<KeyValueDto> queryParams = new ArrayList<>();

    @Builder.Default
    private List<KeyValueDto> headers = new ArrayList<>();

    private AuthDto auth;

    private String body;

    private Integer timeoutMs;

    private Long environmentId; // Optional environment to resolve variables from
}
