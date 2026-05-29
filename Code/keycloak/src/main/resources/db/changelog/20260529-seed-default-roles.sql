INSERT INTO roles (id, name, description, date_created, created_by, last_updated, updated_by)
VALUES
    (gen_random_uuid(), 'ROLE_USER',  'Default role for all registered users', now(), 'system', now(), 'system'),
    (gen_random_uuid(), 'ROLE_ADMIN', 'Administrator role with full access',    now(), 'system', now(), 'system')
ON CONFLICT (name) DO NOTHING;
