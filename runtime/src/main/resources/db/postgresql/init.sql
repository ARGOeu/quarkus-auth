CREATE TABLE IF NOT EXISTS resource_authorization (
    id                  SERIAL PRIMARY KEY,
    secured_endpoint_id VARCHAR(255) NOT NULL,
    rule                VARCHAR(255) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (secured_endpoint_id, rule)
    );


CREATE TABLE IF NOT EXISTS endpoint_resolver (
    id                  SERIAL PRIMARY KEY,
    secured_endpoint_id VARCHAR(255) NOT NULL,
    resource                VARCHAR(255) NOT NULL,
    original_field         VARCHAR(255) NOT NULL,
    mapped_field         VARCHAR(255) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (secured_endpoint_id, resource,original_field)
    );


CREATE TABLE IF NOT EXISTS role_endpoint (
    id SERIAL PRIMARY KEY,
    role_name VARCHAR(255) NOT NULL,
    role_id VARCHAR(255) NOT NULL,
    secured_endpoint_id VARCHAR(255) NOT NULL,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (secured_endpoint_id, role_id)
    );