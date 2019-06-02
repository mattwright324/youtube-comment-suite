UPDATE videos
SET grab_date = ?,
    video_title = ?,
    total_comments = ?,
    total_views = ?,
    total_likes = ?,
    total_dislikes = ?,
    video_desc = ?,
    thumb_url = ?
WHERE video_id = ?