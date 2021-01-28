SELECT *, null as moderation_status FROM comments
JOIN channels USING (channel_id)
WHERE comment_id = ? OR parent_id = ?

UNION ALL

SELECT * FROM comments_moderated
JOIN channels USING (channel_id)
WHERE ? = true AND (comment_id = ? OR parent_id = ?)
ORDER BY is_reply, comment_date