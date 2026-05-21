CREATE TABLE IF NOT EXISTS users (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    keycloak_id  VARCHAR(255) NOT NULL UNIQUE,
    email        VARCHAR(255) NOT NULL UNIQUE,
    username     VARCHAR(255) NOT NULL UNIQUE,
    full_name    VARCHAR(255) NOT NULL,
    phone        VARCHAR(50),
    status       VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    last_login   TIMESTAMPTZ,
    date_created TIMESTAMPTZ  NOT NULL,
    created_by   VARCHAR(255) NOT NULL,
    last_updated TIMESTAMPTZ  NOT NULL,
    updated_by   VARCHAR(255) NOT NULL
);
