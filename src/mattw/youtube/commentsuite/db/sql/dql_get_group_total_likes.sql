SELECT SUM(comment_likes) AS total_likes FROM comments
WHERE video_id IN (
    SELECT video_id FROM gitem_video
    JOIN group_gitem USING (gitem_id)
    WHERE group_id = ?
)