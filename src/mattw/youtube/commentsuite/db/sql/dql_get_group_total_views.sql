SELECT SUM(total_views) AS total_views FROM videos
WHERE video_id IN (
    SELECT video_id FROM gitem_video
    JOIN group_gitem USING (gitem_id)
    WHERE group_id = ?
)