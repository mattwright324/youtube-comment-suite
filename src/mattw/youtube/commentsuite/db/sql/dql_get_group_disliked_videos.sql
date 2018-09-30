SELECT * FROM videos
WHERE video_id IN (
    SELECT video_id FROM gitem_video
    JOIN group_gitem USING (gitem_id)
    WHERE group_id = ?
) ORDER BY total_dislikes DESC LIMIT ?;