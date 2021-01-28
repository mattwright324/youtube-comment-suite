SELECT COUNT(comment_id) AS total_comments
FROM comments_moderated
WHERE video_id IN (
    SELECT video_id FROM gitem_video
    JOIN group_gitem USING (gitem_id)
    WHERE group_id = ?
);