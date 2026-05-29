CREATE TABLE IF NOT EXISTS roles (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE,
    description  VARCHAR(255),
    date_created TIMESTAMPTZ  NOT NULL,
    created_by   VARCHAR(255) NOT NULL,
    last_updated TIMESTAMPTZ  NOT NULL,
    updated_by   VARCHAR(255) NOT NULL
);
