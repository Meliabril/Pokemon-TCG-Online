alter table game_card_instances add column zone_position integer;
alter table game_card_instances add column is_face_down boolean default false;

alter table game_card_instances alter column game_id set not null;
alter table game_card_instances alter column owner_user_id set not null;
alter table game_card_instances alter column card_id set not null;
alter table game_card_instances alter column zone set not null;
alter table game_card_instances alter column is_face_down set not null;

create index ix_game_card_instances_game_owner_zone
on game_card_instances (game_id, owner_user_id, zone);

create index ix_game_card_instances_game_zone
on game_card_instances (game_id, zone);

create index ix_game_card_instances_card_id
on game_card_instances (card_id);

create unique index ux_card_zone_position
on game_card_instances (game_id, owner_user_id, zone, zone_position);
