package io.mattw.youtube.commentsuite.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads various resource SQL scripts for definition, query, and manipulation.
 * <p>
 * Content of the resource scripts accessed by calling toString().
 * Content of SQL Scripts are a mixture of single statements, multiple statements, and statements that require:
 * - Use in PreparedStatement (? chars)
 * - Use in NamedParameterStatement (:params)
 * - String replacement before use in one of above (:order)
 *
 */
public enum SQLLoader {
    CLEAN_DB("dml_clean_db.sql"),
    CREATE_DB("ddl_create_db.sql"),
    CREATE_GITEM("dml_create_gitem.sql"),
    CREATE_GROUP_GITEM("dml_create_group_gitem.sql"),
    DELETE_GROUP("dml_delete_group.sql"),
    DELETE_GROUP_GITEM("dml_delete_group_gitem.sql"),
    DOES_CHANNEL_EXIST("dql_does_channel_exist.sql"),
    GET_ALL_VIDEO_IDS_BY_GROUP("dql_get_video_ids_by_group.sql"),
    GET_ALL_VIDEO_IDS_BY_GROUPITEM("dql_get_video_ids_by_groupitem.sql"),
    GET_COMMENT_STATS("dql_get_comment_stats.sql"),
    GET_COMMENT_TREE("dql_get_comment_tree.sql"),
    GET_COMMENT_WEEK_HISTOGRAM("dql_get_comment_week_histogram.sql"),
    GET_GROUPITEMS("dql_get_groupitems.sql"),
    GET_GROUP_ACTIVE_VIEWERS("dql_get_group_active_viewers.sql"),
    GET_GROUP_COMMENTED_VIDEOS("dql_get_group_commented_videos.sql"),
    GET_GROUP_DISABLED_VIDEOS("dql_get_group_disabled_videos.sql"),
    GET_GROUP_LAST_CHECKED("dql_get_group_last_checked.sql"),
    GET_GROUP_POPULAR_VIDEOS("dql_get_group_popular_videos.sql"),
    GET_GROUP_POPULAR_VIEWERS("dql_get_group_popular_viewers.sql"),
    GET_MODERATED_COMMENT_STATS("dql_get_moderated_comment_stats.sql"),
    GET_UNIQUE_VIEWERS_BY_GROUP("dql_get_unique_viewers_by_group.sql"),
    GET_VIDEOS_BY_CRITERIA_GITEM("dql_get_videos_by_criteria_gitem.sql"),
    GET_VIDEOS_BY_CRITERIA_GROUP("dql_get_videos_by_criteria_group.sql"),
    GET_VIDEO_STATS("dql_get_video_stats.sql"),
    GET_VIDEO_WEEK_HISTOGRAM("dql_get_video_week_histogram.sql"),
    GROUP_CREATE("dml_create_group.sql"),
    GROUP_RENAME("dml_rename_group.sql"),
    INSERT_OR_CHANNELS("dml_insert_or_channels.sql"),
    INSERT_OR_COMMENTS("dml_insert_or_comments.sql"),
    INSERT_IGNORE_GITEM_VIDEO("dml_insert_ignore_gitem_video.sql"),
    INSERT_OR_MODERATED_COMMENTS("dml_insert_or_moderated_comments.sql"),
    INSERT_REPLACE_VIDEOS("dml_insert_replace_videos.sql"),
    RESET_DB("ddl_reset_db.sql"),
    UPDATE_GITEM("dml_update_gitem.sql"),
    UPDATE_VIDEO_HTTPCODE("dml_update_video_httpcode.sql")
    ;

    private final Logger logger = LogManager.getLogger();

    private final String fileName;
    private final String fileData;

    SQLLoader(final String fileName) {
        this.fileName = fileName;

        final List<String> lines = new ArrayList<>();
        try {
            String line;
            try (final InputStreamReader isr = new InputStreamReader(getClass().getResourceAsStream("/io/mattw/youtube/commentsuite/db/sql/" + fileName));
                 final BufferedReader br = new BufferedReader(isr)) {
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load resource sql file [fileKey={}]", getFileName());
        } finally {
            this.fileData = String.join(" ", lines);
        }
    }

    public String getFileName() {
        return fileName;
    }

    public String toString() {
        return fileData;
    }

}
