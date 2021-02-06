package io.mattw.youtube.commentsuite.fxml;

import io.mattw.youtube.commentsuite.util.BrowserUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static javafx.application.Platform.runLater;

public class MainQuotaModal extends VBox {

    private static final Logger logger = LogManager.getLogger();
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mma");
    private static final BrowserUtil browserUtil = new BrowserUtil();

    @FXML
    private Label timeZone, timeUntil;
    @FXML
    private Hyperlink issueLink;

    @FXML
    private Button btnClose;

    public MainQuotaModal() {
        logger.debug("Initialize MainQuotaModal");

        FXMLLoader loader = new FXMLLoader(getClass().getResource("MainQuotaModal.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();

            updateTime();

            ScheduledExecutorService schedule = Executors.newSingleThreadScheduledExecutor();
            schedule.scheduleAtFixedRate(this::updateTime, 0, 15, TimeUnit.SECONDS);

            issueLink.setOnAction(ae -> browserUtil.open("https://github.com/mattwright324/youtube-comment-suite/issues/5"));
        } catch (IOException e) {
            logger.error(e);
            e.printStackTrace();
        }
    }

    private void updateTime() {
        final ZonedDateTime ptNextMidnight = ZonedDateTime.of(
                LocalDate.now(),
                LocalTime.MIDNIGHT,
                TimeZone.getTimeZone("America/Los_Angeles").toZoneId()
        ).plusDays(1);
        final String ptZone = ptNextMidnight.getZone().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        final ZonedDateTime systemNextMidnight = ptNextMidnight.withZoneSameInstant(ZoneId.systemDefault());
        final ZonedDateTime systemNow = ZonedDateTime.now(ZoneId.systemDefault());
        final String systemZone = systemNow.getZone().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        final Duration difference = Duration.between(systemNow, systemNextMidnight);

        runLater(() -> {
            timeZone.setText(String.format("%s %s or %s %s (your time)",
                    formatter.format(ptNextMidnight),
                    ptZone,
                    formatter.format(systemNextMidnight),
                    systemZone)
            );
            timeUntil.setText(String.format("%s hours %s minutes from now",
                    difference.toHours(),
                    difference.minusHours(difference.toHours()).toMinutes()));
        });
    }


    public Button getBtnClose() {
        return btnClose;
    }

}
