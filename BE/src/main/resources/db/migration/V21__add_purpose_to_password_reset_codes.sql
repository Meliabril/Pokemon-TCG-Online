alter table password_reset_codes
    add column if not exists purpose varchar(40) not null default 'PASSWORD_RESET';

create index if not exists ix_password_reset_codes_user_purpose_used
    on password_reset_codes(user_id, purpose, used_at);
