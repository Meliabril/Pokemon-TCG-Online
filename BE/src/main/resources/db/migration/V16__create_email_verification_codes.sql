create table if not exists email_verification_codes (
    id uuid primary key,
    user_id uuid not null,
    code_hash varchar(255) not null,
    expires_at timestamp not null,
    used_at timestamp null,
    created_at timestamp not null,
    attempts int not null default 0,
    constraint fk_email_verification_codes_user
        foreign key (user_id) references users(id)
);

create index if not exists ix_email_verification_codes_user_id
    on email_verification_codes(user_id);

create index if not exists ix_email_verification_codes_expires_at
    on email_verification_codes(expires_at);
