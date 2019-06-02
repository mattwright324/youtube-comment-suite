INSERT OR REPLACE INTO videos (
    video_id, channel_id, grab_date, publish_date, video_title, total_comments, total_views,
    total_likes, total_dislikes, video_desc, thumb_url, http_code
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);