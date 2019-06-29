SELECT video_id FROM videos
WHERE video_id IN (
    SELECT video_id FROM gitem_video
    JOIN group_gitem USING (gitem_id)
    WHERE gitem_id = ?
) ORDER BY publish_date DESC;