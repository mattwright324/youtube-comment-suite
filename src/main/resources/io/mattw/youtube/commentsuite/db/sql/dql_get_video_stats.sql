SELECT COUNT(video_id) AS total_videos,
       SUM(total_views) AS total_views,
       SUM(total_likes) AS total_likes,
       SUM(total_dislikes) AS total_dislikes,
       SUM(total_comments) AS total_comments
FROM videos
WHERE video_id IN (
    SELECT video_id FROM gitem_video
    JOIN group_gitem USING (gitem_id)
    WHERE group_id = ?
);
