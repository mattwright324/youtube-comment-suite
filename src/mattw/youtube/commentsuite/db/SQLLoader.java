package mattw.youtube.commentsuite.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Loads various resource SQL scripts.
 * Scripts are a mixture of DDL, DML, and DQL.
 *
 * Content of the resource scripts accessed by calling toString().
 *
 * Refactoring hard-coded strings into SQL files makes
 * maintaining and creating new functionality easier.
 *
 * @author mattwright324
 */
public enum SQLLoader {
    /* DDL SQL Scripts */
    CREATE_DB("ddl_create_db.sql", SQLType.DDL),
    RESET_DB("ddl_reset_db.sql", SQLType.DDL),

    /* DML SQL Scripts */
    CLEAN_DB("dml_clean_db.sql", SQLType.DML),
    VACUUM_DB("dml_vacuum_db.sql", SQLType.DML),
    GET_CHANNEL_BY_ID("dql_get_channel_by_id.sql", SQLType.DML),
    GROUP_CREATE("dml_create_group.sql", SQLType.DML),
    GROUP_CREATE_DEFAULT("dml_create_default_group.sql", SQLType.DML),
    GROUP_RENAME("dml_rename_group.sql", SQLType.DML),

    /* DQL SQL Scripts */
    GET_ALL_GROUPS("dql_get_all_groups.sql", SQLType.DQL),
    GET_COMMENT_TREE("dql_get_comment_tree.sql", SQLType.DQL)
    ;

    /**
     * Currently unused but defines whether the SQL script is definition (DDL),
     * manipulation (DML), or query (DQL) in nature.
     */
    enum SQLType {
        DDL,
        DQL,
        DML
    }

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
                }
                this.fileData = sb.toString();
            }
        } catch (IOException e) {
            logger.error(String.format("Failed to load resource sql file [fileKey=%s]", fileName));
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
