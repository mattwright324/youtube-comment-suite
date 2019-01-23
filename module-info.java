// Java 9+ Compatibility
module mattw.youtube.commentsuite {
    requires com.google.common;
    requires gson;
    requires java.desktop;
    requires java.sql;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.apache.commons.text;
    requires org.apache.logging.log4j;
    requires org.jsoup;
    requires sqlite.jdbc;
    requires youtube.data.list.mod;
    opens mattw.youtube.commentsuite to javafx.fxml;
    opens mattw.youtube.commentsuite.fxml to javafx.fxml;
    exports mattw.youtube.commentsuite;
}