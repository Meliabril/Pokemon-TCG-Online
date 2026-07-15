alter table password_reset_codes
    add column if not exists verified_at timestamp null;
