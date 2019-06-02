SELECT comment_id FROM comments
JOIN gitem_video USING (video_id)
JOIN group_gitem USING (gitem_id)
WHERE group_id = ?