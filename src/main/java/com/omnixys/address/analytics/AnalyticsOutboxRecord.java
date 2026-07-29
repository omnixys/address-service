package com.omnixys.address.analytics;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_outbox")
@Getter
@Setter
@NoArgsConstructor
public class AnalyticsOutboxRecord {
    @Id
    private UUID id;
    @Column(nullable = false)
    private String topic;
    @Column(nullable = false)
    private String tenantId;
    private String actorId;
    private String correlationId;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(nullable = false)
    private Instant occurredAt;
    @Column(nullable = false)
    private Instant nextAttemptAt;
    @Column(nullable = false)
    private int attempts;
    private Instant publishedAt;
    private Instant deadLetteredAt;
    @Column(columnDefinition = "text")
    private String lastError;
}
