alter table refresh_tokens add column token_hash varchar(255);
alter table refresh_tokens add column expires_at timestamp;
alter table refresh_tokens add column revoked_at timestamp;
alter table refresh_tokens add column replaced_by_token_id uuid;

update refresh_tokens
set token_hash = id,
    expires_at = created_at
where token_hash is null;

alter table refresh_tokens alter column user_id set not null;
alter table refresh_tokens alter column token_hash set not null;
alter table refresh_tokens alter column expires_at set not null;

create unique index ux_refresh_tokens_hash on refresh_tokens (token_hash);
create index ix_refresh_tokens_user on refresh_tokens(user_id);
create index ix_refresh_tokens_expires on refresh_tokens(expires_at);
create index ix_refresh_tokens_revoked on refresh_tokens(revoked_at);
