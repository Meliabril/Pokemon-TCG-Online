update cards
set image_small_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/pr-001-angularquin-large-test.png.png',
    image_large_url = 'https://ldkdohnoqmupfbysrkdz.supabase.co/storage/v1/object/public/pokemon-card-images/custom-proffesors/pr-001-angularquin-large-test.png.png',
    updated_at = current_timestamp
where external_id = 'custom-professors-pr-001';
