SELECT *, count(channel_id) AS count, MAX(comment_date) AS last_comment_on FROM channels
JOIN comments USING (channel_id)
WHERE video_id IN (
    SELECT video_id FROM gitem_video
    JOIN group_gitem USING (gitem_id)
    WHERE group_id = ?
) GROUP BY channel_id
ORDER BY count DESC LIMIT ?;