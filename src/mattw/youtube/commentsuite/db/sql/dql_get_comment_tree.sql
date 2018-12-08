SELECT * FROM comments
JOIN channels USING (channel_id)
WHERE comment_id = ? OR parent_id = ?
ORDER BY is_reply ASC, comment_date ASC;