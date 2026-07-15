delete from matchmaking_queue_entries;

alter table matchmaking_queue_entries
    add column deck_id uuid;

alter table matchmaking_queue_entries
    add constraint fk_matchmaking_queue_entries_deck
        foreign key (deck_id) references decks (id);

alter table matchmaking_queue_entries
    alter column deck_id set not null;

create index ix_matchmaking_queue_entries_deck
    on matchmaking_queue_entries (deck_id);
