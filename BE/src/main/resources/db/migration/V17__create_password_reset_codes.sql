create table if not exists password_reset_codes (
    id uuid primary key,
    user_id uuid not null,
    code_hash varchar(255) not null,
    expires_at timestamp not null,
    used_at timestamp null,
    created_at timestamp not null,
    constraint fk_password_reset_codes_user
        foreign key (user_id) references users(id)
);

create index if not exists ix_password_reset_codes_user_id
    on password_reset_codes(user_id);

create index if not exists ix_password_reset_codes_expires_at
    on password_reset_codes(expires_at);

create index if not exists ix_password_reset_codes_used_at
    on password_reset_codes(used_at);
