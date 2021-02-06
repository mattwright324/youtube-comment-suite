DELETE FROM group_gitem WHERE group_id NOT IN (SELECT DISTINCT group_id FROM groups);
DELETE FROM gitem_list WHERE gitem_id NOT IN (SELECT DISTINCT gitem_id FROM group_gitem);
DELETE FROM gitem_video WHERE gitem_id NOT IN (SELECT DISTINCT gitem_id FROM gitem_list);
DELETE FROM videos WHERE video_id NOT IN (SELECT DISTINCT video_id FROM gitem_video);
DELETE FROM comments WHERE video_id NOT IN (SELECT DISTINCT video_id FROM videos);
DELETE FROM comments_moderated WHERE video_id NOT IN (SELECT DISTINCT video_id FROM videos);
DELETE FROM comment_tags WHERE comment_id NOT IN (
    SELECT comment_id FROM comments
    UNION SELECT comment_id FROM comments_moderated
    );
DELETE FROM channels WHERE channel_id NOT IN (
    SELECT DISTINCT channel_id FROM videos
    UNION SELECT channel_id FROM comments
    UNION SELECT channel_id FROM comments_moderated
);