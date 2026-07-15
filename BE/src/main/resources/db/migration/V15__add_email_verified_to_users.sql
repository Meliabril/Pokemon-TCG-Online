alter table users add column if not exists email_verified boolean not null default false;

-- Existing local users predate the email-verification flow, so keep them able to log in.
update users set email_verified = true where email_verified = false;
