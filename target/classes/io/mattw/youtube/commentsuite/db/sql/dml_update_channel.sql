UPDATE channels
SET channel_name = ?,
    channel_profile_url = ?,
    download_profile = ?
WHERE channel_id = ?;