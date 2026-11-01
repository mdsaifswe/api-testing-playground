package com.playground.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "request_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequestHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private WorkspaceEntity workspace;

    @Column(nullable = false, length = 10)
    private String method;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "request_headers", columnDefinition = "TEXT")
    private String requestHeaders;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_status_text", length = 100)
    private String responseStatusText;

    @Column(name = "response_headers", columnDefinition = "TEXT")
    private String responseHeaders;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "response_size")
    private Long responseSize;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_category", length = 50)
    private String errorCategory;

    @Column(name = "executed_at", nullable = false)
    @Builder.Default
    private Instant executedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (this.executedAt == null) {
            this.executedAt = Instant.now();
        }
    }
}
