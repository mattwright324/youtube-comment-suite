INSERT OR IGNORE INTO comments (
    comment_id, channel_id, video_id, comment_date, comment_text, comment_likes, reply_count, is_reply, parent_id
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)