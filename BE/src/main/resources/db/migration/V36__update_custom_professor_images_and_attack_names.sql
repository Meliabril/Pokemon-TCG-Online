update cards
set image_small_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/147-151-angularquin.png',
    image_large_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/147-151-angularquin.png',
    updated_at = current_timestamp
where external_id = 'custom-professors-pr-001';

update cards
set image_small_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/148-151-ramaven.png',
    image_large_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/148-151-ramaven.png',
    updated_at = current_timestamp
where external_id = 'custom-professors-pr-002';

update cards
set image_small_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/149-151-belgranode.png',
    image_large_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/149-151-belgranode.png',
    updated_at = current_timestamp
where external_id = 'custom-professors-pr-003';

update cards
set image_small_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/150-151-santormento.png',
    image_large_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/150-151-santormento.png',
    updated_at = current_timestamp
where external_id = 'custom-professors-pr-004';

update cards
set image_small_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/151-151-hernullpointer.png',
    image_large_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/151-151-hernullpointer.png',
    updated_at = current_timestamp
where external_id = 'custom-professors-pr-005';

update attacks
set name = 'RelampagoLM'
where id = '00000000-0000-0000-0000-000000025101';

update attacks
set name = 'Dos del Toro'
where name = 'Dos del Cornudo'
  and exists (
      select 1
      from cards
      where cards.id = attacks.card_id
        and cards.external_id = 'custom-professors-pr-004'
  );

update attacks
set name = 'Idempotencia Inmutable'
where name = 'Singleton Supremo'
  and exists (
      select 1
      from cards
      where cards.id = attacks.card_id
        and cards.external_id = 'custom-professors-pr-005'
  );
