package com.omnixys.address.analytics;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AnalyticsOutboxRepository extends JpaRepository<AnalyticsOutboxRecord, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<AnalyticsOutboxRecord>
    findTop50ByPublishedAtIsNullAndDeadLetteredAtIsNullAndNextAttemptAtLessThanEqualOrderByOccurredAtAsc(
            Instant now);
}
