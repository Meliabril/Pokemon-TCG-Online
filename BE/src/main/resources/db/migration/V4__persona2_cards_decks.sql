alter table cards add column external_id varchar(80);
alter table cards add column set_code varchar(20);
alter table cards add column set_name varchar(120);
alter table cards add column number varchar(20);
alter table cards add column name varchar(180);
alter table cards add column supertype varchar(40);
alter table cards add column category varchar(40);
alter table cards add column subtype varchar(80);
alter table cards add column evolves_from varchar(180);
alter table cards add column hp integer;
alter table cards add column pokemon_type varchar(40);
alter table cards add column retreat_cost integer;
alter table cards add column image_small_url text;
alter table cards add column image_large_url text;
alter table cards add column raw_json text;

update cards
set external_id = id,
    set_code = 'xy1',
    set_name = 'XY',
    number = '0',
    name = 'placeholder',
    supertype = 'POKEMON',
    category = 'BASIC_POKEMON',
    raw_json = '{}'
where external_id is null;

alter table cards alter column external_id set not null;
alter table cards alter column set_code set not null;
alter table cards alter column set_name set not null;
alter table cards alter column number set not null;
alter table cards alter column name set not null;
alter table cards alter column supertype set not null;
alter table cards alter column category set not null;
alter table cards alter column raw_json set not null;
alter table cards add constraint ck_cards_set_xy1 check (set_code = 'xy1');

create unique index ux_cards_external_id on cards (external_id);
create index ix_cards_name on cards (name);
create index ix_cards_set_code on cards (set_code);
create index ix_cards_category on cards (category);
create index ix_cards_set_category on cards (set_code, category);
create index ix_cards_set_name on cards (set_code, name);
create index ix_cards_supertype on cards (supertype);

alter table attacks add column name varchar(120);
alter table attacks add column damage_text varchar(40);
alter table attacks add column base_damage integer;
alter table attacks add column effect_text text;
alter table attacks add column attack_order integer default 0;
update attacks set name = 'placeholder' where name is null;
alter table attacks alter column card_id set not null;
alter table attacks alter column name set not null;
alter table attacks alter column attack_order set not null;
create index ix_attacks_card on attacks (card_id);

alter table attack_costs add column energy_type varchar(40);
alter table attack_costs add column quantity integer default 1;
update attack_costs set energy_type = 'Colorless' where energy_type is null;
alter table attack_costs alter column attack_id set not null;
alter table attack_costs alter column energy_type set not null;
alter table attack_costs alter column quantity set not null;
alter table attack_costs add constraint ck_attack_cost_quantity_positive check (quantity > 0);
create index ix_attack_costs_attack on attack_costs (attack_id);

alter table card_weaknesses add column energy_type varchar(40);
alter table card_weaknesses add column multiplier varchar(20);
update card_weaknesses set energy_type = 'Colorless', multiplier = 'x2' where energy_type is null;
alter table card_weaknesses alter column card_id set not null;
alter table card_weaknesses alter column energy_type set not null;
alter table card_weaknesses alter column multiplier set not null;
create index ix_card_weaknesses_card on card_weaknesses (card_id);

alter table card_resistances add column energy_type varchar(40);
alter table card_resistances add column resistance_value varchar(20);
update card_resistances set energy_type = 'Colorless', resistance_value = '-20' where energy_type is null;
alter table card_resistances alter column card_id set not null;
alter table card_resistances alter column energy_type set not null;
alter table card_resistances alter column resistance_value set not null;
create index ix_card_resistances_card on card_resistances (card_id);

alter table decks add column name varchar(100);
alter table decks add column format varchar(40);
alter table decks add column is_valid boolean default false;
alter table decks add column validation_errors text;
update decks set name = 'Deck', format = 'XY1_UNLIMITED' where name is null;
alter table decks alter column owner_user_id set not null;
alter table decks alter column name set not null;
alter table decks alter column format set not null;
alter table decks alter column is_valid set not null;
alter table decks add constraint ck_decks_format_xy1 check (format = 'XY1_UNLIMITED');
alter table decks add constraint ux_one_deck_per_user unique (owner_user_id);
create index ix_decks_owner on decks (owner_user_id);

alter table deck_cards add column quantity integer default 1;
alter table deck_cards alter column deck_id set not null;
alter table deck_cards alter column card_id set not null;
alter table deck_cards alter column quantity set not null;
alter table deck_cards add constraint ck_deck_card_quantity_positive check (quantity > 0);
create unique index ux_deck_card on deck_cards (deck_id, card_id);
create index ix_deck_cards_deck on deck_cards (deck_id);
create index ix_deck_cards_card on deck_cards (card_id);
