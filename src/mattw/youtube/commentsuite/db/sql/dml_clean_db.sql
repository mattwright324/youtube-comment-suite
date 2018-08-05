DELETE FROM group_gitem WHERE group_id NOT IN (SELECT DISTINCT group_id FROM groups);
DELETE FROM gitem_list WHERE gitem_id NOT IN (SELECT DISTINCT gitem_id FROM group_gitem);
DELETE FROM gitem_video WHERE gitem_id NOT IN (SELECT DISTINCT gitem_id FROM gitem_list);
DELETE FROM videos WHERE video_id NOT IN (SELECT DISTINCT video_id FROM gitem_video);
DELETE FROM comments WHERE video_id NOT IN (SELECT DISTINCT video_id FROM videos);
WITH clist AS (SELECT DISTINCT channel_id FROM videos UNION SELECT channel_id FROM comments);
DELETE FROM channels WHERE channel_id NOT IN clist;