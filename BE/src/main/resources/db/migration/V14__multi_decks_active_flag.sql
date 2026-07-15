alter table decks add column is_active boolean default false;

update decks
set is_active = true
where is_active = false;

alter table decks alter column is_active set not null;

alter table decks drop constraint if exists fk_decks_owner;
alter table decks drop constraint if exists ux_one_deck_per_user;
alter table decks add constraint fk_decks_owner foreign key (owner_user_id) references users (id);

create index if not exists ix_decks_owner_active on decks (owner_user_id, is_active);
alter table decks add column active_owner_user_id uuid
    generated always as (case when is_active then owner_user_id else null end) ${generatedColumnStoredClause};
create unique index if not exists ux_decks_owner_active_true on decks (active_owner_user_id);
