package mattw.youtube.commentsuite.db;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class GroupStats {

    private long totalViews = 0;
    private long totalVideos = 0;
    private long totalLikes = 0;
    private long totalDislikes = 0;
    private long totalComments = 0;
    private List<YouTubeVideo> mostCommented = new ArrayList<>();
    private List<YouTubeVideo> mostDisliked = new ArrayList<>();
    private List<YouTubeVideo> mostViewed = new ArrayList<>();
    private List<YouTubeVideo> commentsDisabled = new ArrayList<>();
    private Map<Long, Long> weeklyUploadHistogram = new LinkedHashMap<>();

    private long totalCommentLikes = 0;
    private long totalGrabbedComments = 0;
    private Map<YouTubeChannel, Long> mostLikedViewers = new LinkedHashMap<>();
    private Map<YouTubeChannel, Long> mostActiveViewers = new LinkedHashMap<>();
    private Map<Long, Long> weeklyCommentHistogram = new LinkedHashMap<>();

    public GroupStats() {}

    public long getTotalViews() {
        return totalViews;
    }

    public void setTotalViews(long totalViews) {
        this.totalViews = totalViews;
    }

    public long getTotalVideos() {
        return totalVideos;
    }

    public void setTotalVideos(long totalVideos) {
        this.totalVideos = totalVideos;
    }

    public long getTotalLikes() {
        return totalLikes;
    }

    public void setTotalLikes(long totalLikes) {
        this.totalLikes = totalLikes;
    }

    public long getTotalDislikes() {
        return totalDislikes;
    }

    public void setTotalDislikes(long totalDislikes) {
        this.totalDislikes = totalDislikes;
    }

    public long getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(long totalComments) {
        this.totalComments = totalComments;
    }

    public List<YouTubeVideo> getMostCommented() {
        return mostCommented;
    }

    public void setMostCommented(List<YouTubeVideo> mostCommented) {
        this.mostCommented = mostCommented;
    }

    public List<YouTubeVideo> getMostDisliked() {
        return mostDisliked;
    }

    public void setMostDisliked(List<YouTubeVideo> mostDisliked) {
        this.mostDisliked = mostDisliked;
    }

    public List<YouTubeVideo> getMostViewed() {
        return mostViewed;
    }

    public void setMostViewed(List<YouTubeVideo> mostViewed) {
        this.mostViewed = mostViewed;
    }

    public List<YouTubeVideo> getCommentsDisabled() {
        return commentsDisabled;
    }

    public void setCommentsDisabled(List<YouTubeVideo> commentsDisabled) {
        this.commentsDisabled = commentsDisabled;
    }

    public Map<Long, Long> getWeeklyUploadHistogram() {
        return weeklyUploadHistogram;
    }

    public void setWeeklyUploadHistogram(Map<Long, Long> weeklyUploadHistogram) {
        this.weeklyUploadHistogram = weeklyUploadHistogram;
    }

    public long getTotalCommentLikes() {
        return totalCommentLikes;
    }

    public void setTotalCommentLikes(long totalCommentLikes) {
        this.totalCommentLikes = totalCommentLikes;
    }

    public long getTotalGrabbedComments() {
        return totalGrabbedComments;
    }

    public void setTotalGrabbedComments(long totalGrabbedComments) {
        this.totalGrabbedComments = totalGrabbedComments;
    }

    public Map<YouTubeChannel, Long> getMostLikedViewers() {
        return mostLikedViewers;
    }

    public void setMostLikedViewers(Map<YouTubeChannel, Long> mostLikedViewers) {
        this.mostLikedViewers = mostLikedViewers;
    }

    public Map<YouTubeChannel, Long> getMostActiveViewers() {
        return mostActiveViewers;
    }

    public void setMostActiveViewers(Map<YouTubeChannel, Long> mostActiveViewers) {
        this.mostActiveViewers = mostActiveViewers;
    }

    public Map<Long, Long> getWeeklyCommentHistogram() {
        return weeklyCommentHistogram;
    }

    public void setWeeklyCommentHistogram(Map<Long, Long> weeklyCommentHistogram) {
        this.weeklyCommentHistogram = weeklyCommentHistogram;
    }

    @Override
    public String toString() {
        return "GroupStats{" +
                "totalViews=" + totalViews +
                ", totalVideos=" + totalVideos +
                ", totalLikes=" + totalLikes +
                ", totalDislikes=" + totalDislikes +
                ", totalComments=" + totalComments +
                ", mostCommented=" + mostCommented +
                ", mostDisliked=" + mostDisliked +
                ", mostViewed=" + mostViewed +
                ", commentsDisabled=" + commentsDisabled +
                ", weeklyUploadHistogram=" + weeklyUploadHistogram +
                ", totalCommentLikes=" + totalCommentLikes +
                ", totalGrabbedComments=" + totalGrabbedComments +
                ", mostLikedViewers=" + mostLikedViewers +
                ", mostActiveViewers=" + mostActiveViewers +
                ", weeklyCommentHistogram=" + weeklyCommentHistogram +
                '}';
    }
}
