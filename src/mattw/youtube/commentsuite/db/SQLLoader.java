package mattw.youtube.commentsuite.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Loads various resource SQL scripts for definition, query, and manipulation.
 *
 * Content of the resource scripts accessed by calling toString().
 * Content of SQL Scripts are a mixture of single statements, multiple statements, and statements that require:
 *  - Use in PreparedStatement (? chars)
 *  - Use in NamedParameterStatement (:params)
 *  - String replacement before use in one of above (:order)
 *
 * @since 2018-12-30
 * @author mattwright324
 */
public enum SQLLoader {
    /* DDL SQL Scripts */
    CREATE_DB("ddl_create_db.sql", SQLType.DDL),
    RESET_DB("ddl_reset_db.sql", SQLType.DDL),

    /* DML SQL Scripts */
    CLEAN_DB("dml_clean_db.sql", SQLType.DML),
    VACUUM_DB("dml_vacuum_db.sql", SQLType.DML),
    GROUP_CREATE("dml_create_group.sql", SQLType.DML),
    GROUP_CREATE_DEFAULT("dml_create_default_group.sql", SQLType.DML),
    GROUP_RENAME("dml_rename_group.sql", SQLType.DML),
    INSERT_REPLACE_VIDEOS("dml_insert_replace_videos.sql", SQLType.DML),
    INSERT_IGNORE_COMMENTS("dml_insert_ignore_comments.sql", SQLType.DML),
    INSERT_IGNORE_CHANNELS("dml_insert_ignore_channels.sql", SQLType.DML),
    INSERT_IGNORE_GITEM_VIDEO("dml_insert_ignore_gitem_video.sql", SQLType.DML),
    DELETE_GROUP("dml_delete_group.sql", SQLType.DML),
    CREATE_GITEM("dml_create_gitem.sql", SQLType.DML),
    CREATE_GROUP_GITEM("dml_create_group_gitem.sql", SQLType.DML),
    UPDATE_GITEM_LAST_CHECKED("dml_update_gitem_last_checked.sql", SQLType.DML),
    DELETE_GROUP_GITEM("dml_delete_group_gitem.sql", SQLType.DML),
    UPDATE_VIDEO("dml_update_video.sql", SQLType.DML),
    UPDATE_VIDEO_HTTPCODE("dml_update_video_httpcode.sql", SQLType.DML),
    UPDATE_CHANNEL("dml_update_channel.sql", SQLType.DML),

    /* DQL SQL Scripts */
    GET_ALL_GROUPS("dql_get_all_groups.sql", SQLType.DQL),
    GET_COMMENT_TREE("dql_get_comment_tree.sql", SQLType.DQL),
    GET_GROUPITEMS("dql_get_groupitems.sql", SQLType.DQL),
    GET_ALL_COMMENT_IDS_BY_GROUP("dql_get_all_comment_ids_by_group.sql", SQLType.DQL),
    DOES_COMMENT_EXIST("dql_does_comment_exist.sql", SQLType.DQL),
    DOES_VIDEO_EXIST("dql_does_video_exist.sql", SQLType.DQL),
    DOES_CHANNEL_EXIST("dql_does_channel_exist.sql", SQLType.DQL),
    GET_ALL_VIDEO_IDS_BY_GROUP("dql_get_video_ids_by_group.sql", SQLType.DQL),
    GET_ALL_UNIQUE_VIDEO_IDS("dql_get_all_video_ids.sql", SQLType.DQL),
    GET_VIDEO_BY_ID("dql_get_video_by_id.sql", SQLType.DQL),
    GET_VIDEOS_BY_CRITERIA_GITEM("dql_get_videos_by_criteria_gitem.sql", SQLType.DQL),
    GET_VIDEOS_BY_CRITERIA_GROUP("dql_get_videos_by_criteria_group.sql", SQLType.DQL),
    GET_COMMENTTHREAD_REPLY_COUNT_BY_GROUP("dql_get_commentthread_reply_count_by_group.sql", SQLType.DQL),
    GET_CHANNEL_BY_ID("dql_get_channel_by_id.sql", SQLType.DQL),
    GET_CHANNEL_EXISTS("dql_get_channel_exists.sql", SQLType.DQL),
    GET_ALL_UNIQUE_CHANNEL_IDS("dql_get_all_unique_channel_ids.sql", SQLType.DQL),
    GET_ALL_GITEM_VIDEO("dql_get_all_gitem_video.sql", SQLType.DQL),
    GET_GROUP_LAST_CHECKED("dql_get_group_last_checked.sql", SQLType.DQL),
    GET_COMMENT_WEEK_HISTOGRAM("dql_get_comment_week_histogram.sql", SQLType.DQL),
    GET_VIDEO_WEEK_HISTOGRAM("dql_get_video_week_histogram.sql", SQLType.DQL),
    GET_GROUP_ACTIVE_VIEWERS("dql_get_group_active_viewers.sql", SQLType.DQL),
    GET_GROUP_POPULAR_VIEWERS("dql_get_group_popular_viewers.sql", SQLType.DQL),
    GET_GROUP_POPULAR_VIDEOS("dql_get_group_popular_videos.sql", SQLType.DQL),
    GET_GROUP_DISLIKED_VIDEOS("dql_get_group_disliked_videos.sql", SQLType.DQL),
    GET_GROUP_COMMENTED_VIDEOS("dql_get_group_commented_videos.sql", SQLType.DQL),
    GET_GROUP_DISABLED_VIDEOS("dql_get_group_disabled_videos.sql", SQLType.DQL),
    GET_VIDEO_STATS("dql_get_video_stats.sql", SQLType.DQL),
    GET_COMMENT_STATS("dql_get_comment_stats.sql", SQLType.DQL),
    ;

    /**
     * Defines whether the SQL script is definition (DDL),
     * manipulation (DML), or query (DQL) in nature.
     */
    enum SQLType { DDL, DQL, DML }

    private Logger logger = LogManager.getLogger(SQLLoader.class.getSimpleName());

    private String basePath = "/mattw/youtube/commentsuite/db/sql/";
    private String fileName;
    private SQLType scriptType;
    private String fileData = "";

    SQLLoader(String fileName, SQLType scriptType) {
        this.fileName = fileName;
        this.scriptType = scriptType;
        try {
            String line;
            StringBuilder sb = new StringBuilder();
            try(InputStreamReader isr = new InputStreamReader(getClass().getResource(basePath+fileName).openStream());
                BufferedReader br = new BufferedReader(isr)) {
                while((line = br.readLine()) != null) {
                    sb.append(line);
                    sb.append(' ');
                }
                this.fileData = sb.toString();
            }
            if((scriptType == SQLType.DML && this.fileData.startsWith("SELECT")) ||
                    (scriptType == SQLType.DQL && !this.fileData.startsWith("SELECT"))) {
                logger.warn(String.format("Incorrect SQLType? [fileKey=%s,fileType=%s]", getFileName(), getScriptType()));
            }
        } catch (IOException e) {
            logger.error(String.format("Failed to load resource sql file [fileKey=%s,fileType%s]", getFileName(), getScriptType()));
        }
    }

    public String getFileName() {
        return fileName;
    }

    public SQLType getScriptType() {
        return scriptType;
    }

    public String toString() {
        return fileData;
    }

}
