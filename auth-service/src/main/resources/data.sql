INSERT INTO users (id, created_at, email, password_hash, role, status)
VALUES (
    '41de4b06-35ec-42d0-958a-def1831d3a06',
        NOW(),
        'admin@gmail.com',
        '$2a$10$C1HWR9nBDdSO2zMmAq9ieujnF9Iab0xNL0eFihQaxXaG6xbpbgF8i',
        'ADMIN',
        'ACTIVE'
    )
ON CONFLICT (email) DO NOTHING;