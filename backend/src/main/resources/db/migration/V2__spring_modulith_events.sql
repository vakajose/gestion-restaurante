-- ============================================================================
-- V2__spring_modulith_events.sql
-- Spring Modulith Transactional Event Publication Registry (PostgreSQL)
-- Supports asynchronous modular events (e.g. POS OrderPaidEvent -> Kardex)
-- ============================================================================

CREATE TABLE IF NOT EXISTS event_publication (
    id UUID NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP WITH TIME ZONE,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    completion_attempts INT NOT NULL DEFAULT 0,
    status VARCHAR(20),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_event_publication_date ON event_publication(publication_date);
CREATE INDEX IF NOT EXISTS idx_event_publication_status ON event_publication(status);

CREATE TABLE IF NOT EXISTS event_publication_archive (
    id UUID NOT NULL,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event TEXT NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP WITH TIME ZONE,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    completion_attempts INT NOT NULL DEFAULT 0,
    status VARCHAR(20),
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS idx_event_publication_archive_date ON event_publication_archive(publication_date);
