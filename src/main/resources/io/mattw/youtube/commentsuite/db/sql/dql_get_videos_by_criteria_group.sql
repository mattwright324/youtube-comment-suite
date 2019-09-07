SELECT * FROM videos
WHERE video_id IN (
    SELECT video_id FROM gitem_video
    LEFT JOIN group_gitem USING (gitem_id)
    WHERE group_id = ?
)
AND video_title LIKE ?
OR video_id = ?
ORDER BY :order LIMIT ?;