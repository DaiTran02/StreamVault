CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    username VARCHAR(128) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE videos ADD COLUMN IF NOT EXISTS user_id UUID REFERENCES users (id);
ALTER TABLE videos ADD COLUMN IF NOT EXISTS content_sha256 VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_videos_user_id ON videos (user_id);

CREATE TABLE IF NOT EXISTS custody_events (
    id UUID PRIMARY KEY,
    video_id UUID NOT NULL REFERENCES videos (id) ON DELETE CASCADE,
    user_id UUID REFERENCES users (id),
    action VARCHAR(64) NOT NULL,
    content_sha256 VARCHAR(64) NOT NULL,
    previous_chain_hash VARCHAR(64) NOT NULL,
    chain_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_custody_events_video_id ON custody_events (video_id, created_at, id);
