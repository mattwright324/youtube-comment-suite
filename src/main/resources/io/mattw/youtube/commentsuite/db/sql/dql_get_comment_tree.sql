SELECT * FROM comments
JOIN channels USING (channel_id)
WHERE comment_id = ? OR parent_id = ?
ORDER BY is_reply, comment_date;