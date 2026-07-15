alter table users add column email varchar(320) not null;
alter table users add column username varchar(100) not null;
alter table users add column first_name varchar(100) not null;
alter table users add column last_name varchar(100) not null;
alter table users add column password_hash varchar(255) not null;
alter table users add column role varchar(30) not null;
alter table users add column status varchar(30) not null;

create unique index ux_users_email on users (email);
create unique index ux_users_username on users (username);
create index ix_users_status on users (status);
