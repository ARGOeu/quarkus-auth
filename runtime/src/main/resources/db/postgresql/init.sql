CREATE SCHEMA IF NOT EXISTS quarkus_auth;

CREATE TABLE IF NOT EXISTS quarkus_auth.resource_authorization
(
    id
    SERIAL
    PRIMARY
    KEY,
    secured_endpoint_id
    VARCHAR
(
    255
) NOT NULL,
    rule VARCHAR
(
    255
) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
(
),
    UNIQUE
(
    secured_endpoint_id,
    rule
)
    );

CREATE TABLE IF NOT EXISTS quarkus_auth.endpoint_resolver
(
    id
    SERIAL
    PRIMARY
    KEY,
    secured_endpoint_id
    VARCHAR
(
    255
) NOT NULL,
    resource VARCHAR
(
    255
) NOT NULL,
    original_field VARCHAR
(
    255
) NOT NULL,
    mapped_field VARCHAR
(
    255
) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
(
),
    UNIQUE
(
    secured_endpoint_id,
    resource,
    original_field
)
    );

CREATE TABLE IF NOT EXISTS quarkus_auth.role_endpoint
(
    id
    SERIAL
    PRIMARY
    KEY,
    role_name
    VARCHAR
(
    255
) NOT NULL,
    role_id VARCHAR
(
    255
) NOT NULL,
    secured_endpoint_id VARCHAR
(
    255
) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now
(
),
    UNIQUE
(
    secured_endpoint_id,
    role_id
)
    );

ALTER TABLE quarkus_auth.role_endpoint
    ADD COLUMN IF NOT EXISTS scope VARCHAR (50) NULL;
--
-- ALTER TABLE quarkus_auth.role_endpoint
--     ALTER COLUMN scope SET DEFAULT 'none';