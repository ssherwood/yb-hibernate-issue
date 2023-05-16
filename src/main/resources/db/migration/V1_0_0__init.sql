CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE account
(
    id             UUID PRIMARY KEY      DEFAULT uuid_generate_v4(),
    email          VARCHAR(255) NOT NULL UNIQUE,
    user_name      VARCHAR(30)  NOT NULL,
    details        JSONB,
    is_active      BOOLEAN      DEFAULT true,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_access_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);