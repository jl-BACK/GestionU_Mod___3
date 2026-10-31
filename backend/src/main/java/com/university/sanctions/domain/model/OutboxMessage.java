package com.university.sanctions.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "outbox_message")
@Getter @Setter @NoArgsConstructor
public class OutboxMessage {
    @Id
    @Column(length = 36)
    private String id;
    @Column(nullable = false, length = 200)
    private String topic;
    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;
    @Column(name = "message_key", nullable = false, length = 200)
    private String messageKey;
    @Lob
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String payload;
    @Lob
    @Column(name = "headers_json", columnDefinition = "TEXT")
    private String headersJson;
    @Column(nullable = false, length = 20)
    private String status = "PENDIENTE";
    @Column(nullable = false)
    private int attempts;
    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;
    @Column(name = "last_error", length = 1000)
    private String lastError;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;
}