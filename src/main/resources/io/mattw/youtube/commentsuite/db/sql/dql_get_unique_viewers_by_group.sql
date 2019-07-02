SELECT COUNT(DISTINCT channel_id) AS unique_viewers
FROM comments
WHERE video_id IN (
    SELECT video_id FROM gitem_video
                             JOIN group_gitem USING (gitem_id)
    WHERE group_id = ?
)