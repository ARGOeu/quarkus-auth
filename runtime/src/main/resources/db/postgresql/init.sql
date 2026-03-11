CREATE TABLE IF NOT EXISTS resource_authorization (
    id SERIAL PRIMARY KEY,
    name        VARCHAR(255)        NOT NULL,
    created_at  TIMESTAMPTZ         NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS secured_endpoint (
    id          BIGSERIAL PRIMARY KEY,
    secured_endpoint_id VARCHAR(255) NOT NULL UNIQUE,
    resource    VARCHAR(255) NOT NULL,
    action      VARCHAR(255) NOT NULL,
    path        VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL
);