SELECT CAST(publish_date/604800000.00 AS INTEGER)*604800000 AS week, count(*) AS count FROM videos
WHERE video_id IN (
    SELECT video_id FROM gitem_video
    JOIN group_gitem USING (gitem_id)
    WHERE group_id = ?
) GROUP BY week ORDER BY week;