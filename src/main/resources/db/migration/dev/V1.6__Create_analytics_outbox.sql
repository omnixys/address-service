CREATE TABLE analytics_outbox (
    id UUID PRIMARY KEY,
    topic VARCHAR(200) NOT NULL,
    tenant_id VARCHAR(100) NOT NULL,
    actor_id VARCHAR(256),
    correlation_id VARCHAR(256),
    payload TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    published_at TIMESTAMPTZ,
    dead_lettered_at TIMESTAMPTZ,
    last_error TEXT
);
CREATE INDEX analytics_outbox_ready_idx
    ON analytics_outbox (next_attempt_at, occurred_at)
    WHERE published_at IS NULL AND dead_lettered_at IS NULL;
