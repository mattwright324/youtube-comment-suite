SELECT comment_id, db_replies FROM comments
JOIN (
    SELECT parent_id, COUNT(parent_id) AS db_replies FROM comments
    WHERE parent_id NOT NULL AND is_reply = 1
    GROUP BY parent_id
) AS cc ON cc.parent_id = comment_id
JOIN gitem_video USING (video_id)
JOIN group_gitem USING (gitem_id)
WHERE is_reply = ? AND group_id = ?;