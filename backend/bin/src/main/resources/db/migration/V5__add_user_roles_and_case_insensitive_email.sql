ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

ALTER TABLE users
    ADD COLUMN email_normalized VARCHAR(254);

UPDATE users
SET email_normalized = LOWER(email)
WHERE email_normalized IS NULL;

CREATE UNIQUE INDEX uk_user_email_normalized
    ON users (email_normalized);
