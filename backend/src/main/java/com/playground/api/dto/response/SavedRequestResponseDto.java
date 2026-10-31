package com.playground.api.dto.response;

import com.playground.api.dto.common.AuthDto;
import com.playground.api.dto.common.KeyValueDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedRequestResponseDto {
    private Long id;
    private Long collectionId;
    private String name;
    private String method;
    private String url;
    private List<KeyValueDto> queryParams;
    private List<KeyValueDto> headers;
    private String authType;
    private AuthDto authConfig;
    private String body;
    private Instant createdAt;
    private Instant updatedAt;
}
