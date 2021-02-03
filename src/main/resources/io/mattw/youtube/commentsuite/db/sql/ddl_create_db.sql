CREATE TABLE IF NOT EXISTS gitem_type
(
    type_id      INTEGER PRIMARY KEY,
    nameProperty STRING
);

INSERT OR IGNORE INTO gitem_type
VALUES (0, 'video'),
       (1, 'channel'),
       (2, 'playlist');

CREATE TABLE IF NOT EXISTS gitem_list
(
    gitem_id      STRING PRIMARY KEY,
    type_id       INTEGER,
    title         STRING,
    channel_title STRING,
    published     DATE,
    last_checked  DATE,
    thumb_url     STRING,
    FOREIGN KEY (type_id) REFERENCES gitem_type (type_id)
);

CREATE TABLE IF NOT EXISTS groups
(
    group_id   STRING PRIMARY KEY,
    group_name STRING UNIQUE
);

CREATE TABLE IF NOT EXISTS group_gitem
(
    group_id STRING,
    gitem_id STRING,
    PRIMARY KEY (group_id, gitem_id),
    FOREIGN KEY (group_id) REFERENCES groups (group_id),
    FOREIGN KEY (gitem_id) REFERENCES gitem_list (gitem_id)
);

CREATE TABLE IF NOT EXISTS gitem_video
(
    gitem_id STRING,
    video_id STRING,
    PRIMARY KEY (gitem_id, video_id),
    FOREIGN KEY (gitem_id) REFERENCES gitem_list (gitem_id),
    FOREIGN KEY (video_id) REFERENCES videos (video_id)
);

CREATE TABLE IF NOT EXISTS videos
(
    video_id       STRING PRIMARY KEY,
    channel_id     STRING,
    grab_date      INTEGER,
    publish_date   INTEGER,
    video_title    STRING,
    total_comments INTEGER,
    total_views    INTEGER,
    total_likes    INTGEGER,
    total_dislikes INTEGER,
    video_desc     STRING,
    thumb_url      STRING,
    http_code      int,
    FOREIGN KEY (channel_id) REFERENCES channels (channel_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_videos_id ON videos (video_id);

CREATE TABLE IF NOT EXISTS comments
(
    comment_id    STRING PRIMARY KEY,
    channel_id    STRING,
    video_id      STRING,
    comment_date  INTEGER,
    comment_likes INTEGER,
    reply_count   INTEGER,
    is_reply      BOOLEAN,
    parent_id     STRING,
    comment_text  TEXT,
    FOREIGN KEY (channel_id) REFERENCES channels (channel_id),
    FOREIGN KEY (video_id) REFERENCES videos (video_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_comments_id ON comments (comment_id);

CREATE TABLE IF NOT EXISTS comments_moderated
(
    comment_id        STRING PRIMARY KEY,
    channel_id        STRING,
    video_id          STRING,
    comment_date      INTEGER,
    comment_likes     INTEGER,
    reply_count       INTEGER,
    is_reply          BOOLEAN,
    parent_id         STRING,
    comment_text      TEXT,
    moderation_status STRING,
    FOREIGN KEY (channel_id) REFERENCES channels (channel_id),
    FOREIGN KEY (video_id) REFERENCES videos (video_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_comments_id ON comments (comment_id);

CREATE TABLE IF NOT EXISTS comment_tags
(
    comment_id    STRING,
    tag           STRING,
    PRIMARY KEY (comment_id, tag),
    FOREIGN KEY (comment_id) REFERENCES comments (comment_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_comments_id ON comments (comment_id);

CREATE TABLE IF NOT EXISTS channels
(
    channel_id          STRING PRIMARY KEY,
    channel_name        STRING,
    channel_profile_url STRING,
    download_profile    BOOLEAN
);
CREATE UNIQUE INDEX IF NOT EXISTS idx_channels_id ON channels (channel_id);