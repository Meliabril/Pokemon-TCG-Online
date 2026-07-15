create table users (
    id uuid primary key,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table refresh_tokens (
    id uuid primary key,
    user_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_refresh_tokens_user
        foreign key (user_id) references users (id)
);

create table revoked_access_tokens (
    id uuid primary key,
    user_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_revoked_access_tokens_user
        foreign key (user_id) references users (id)
);

create table cards (
    id uuid primary key,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table attacks (
    id uuid primary key,
    card_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_attacks_card
        foreign key (card_id) references cards (id)
);

create table attack_costs (
    id uuid primary key,
    attack_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_attack_costs_attack
        foreign key (attack_id) references attacks (id)
);

create table card_weaknesses (
    id uuid primary key,
    card_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_card_weaknesses_card
        foreign key (card_id) references cards (id)
);

create table card_resistances (
    id uuid primary key,
    card_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_card_resistances_card
        foreign key (card_id) references cards (id)
);

create table decks (
    id uuid primary key,
    owner_user_id uuid,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_decks_owner
        foreign key (owner_user_id) references users (id)
);

create table deck_cards (
    id uuid primary key,
    deck_id uuid,
    card_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_deck_cards_deck
        foreign key (deck_id) references decks (id),
    constraint fk_deck_cards_card
        foreign key (card_id) references cards (id)
);

create table matchmaking_queue_entries (
    id uuid primary key,
    user_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_matchmaking_queue_entries_user
        foreign key (user_id) references users (id)
);

create table games (
    id uuid primary key,
    status varchar(30),
    current_phase varchar(30),
    turn_number integer not null default 0,
    state_version integer not null default 0,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table game_participants (
    id uuid primary key,
    game_id uuid,
    user_id uuid,
    deck_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_game_participants_game
        foreign key (game_id) references games (id),
    constraint fk_game_participants_user
        foreign key (user_id) references users (id),
    constraint fk_game_participants_deck
        foreign key (deck_id) references decks (id)
);

create table game_card_instances (
    id uuid primary key,
    game_id uuid,
    owner_user_id uuid,
    card_id uuid,
    zone varchar(40),
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint fk_game_card_instances_game
        foreign key (game_id) references games (id),
    constraint fk_game_card_instances_owner
        foreign key (owner_user_id) references users (id),
    constraint fk_game_card_instances_card
        foreign key (card_id) references cards (id)
);

create table pokemon_in_play (
    id uuid primary key,
    game_id uuid,
    active_card_instance_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_pokemon_in_play_game
        foreign key (game_id) references games (id),
    constraint fk_pokemon_in_play_card_instance
        foreign key (active_card_instance_id) references game_card_instances (id)
);

create table pokemon_evolution_stack (
    id uuid primary key,
    pokemon_in_play_id uuid,
    game_card_instance_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_pokemon_evolution_stack_pokemon
        foreign key (pokemon_in_play_id) references pokemon_in_play (id),
    constraint fk_pokemon_evolution_stack_instance
        foreign key (game_card_instance_id) references game_card_instances (id)
);

create table pokemon_attached_cards (
    id uuid primary key,
    pokemon_in_play_id uuid,
    game_card_instance_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_pokemon_attached_cards_pokemon
        foreign key (pokemon_in_play_id) references pokemon_in_play (id),
    constraint fk_pokemon_attached_cards_instance
        foreign key (game_card_instance_id) references game_card_instances (id)
);

create table special_conditions (
    id uuid primary key,
    pokemon_in_play_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_special_conditions_pokemon
        foreign key (pokemon_in_play_id) references pokemon_in_play (id)
);

create table game_action_logs (
    id uuid primary key,
    game_id uuid,
    created_at timestamp not null default current_timestamp,
    constraint fk_game_action_logs_game
        foreign key (game_id) references games (id)
);

create table game_state_snapshots (
    id uuid primary key,
    game_id uuid,
    state_version integer not null default 0,
    created_at timestamp not null default current_timestamp,
    constraint fk_game_state_snapshots_game
        foreign key (game_id) references games (id)
);

create table game_events (
    id uuid primary key,
    game_id uuid,
    event_type varchar(40),
    created_at timestamp not null default current_timestamp,
    constraint fk_game_events_game
        foreign key (game_id) references games (id)
);
