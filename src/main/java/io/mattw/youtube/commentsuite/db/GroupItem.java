package io.mattw.youtube.commentsuite.db;


import io.mattw.youtube.commentsuite.FXMLSuite;
import mattw.youtube.datav3.Parts;
import mattw.youtube.datav3.YouTubeData3;
import mattw.youtube.datav3.entrypoints.ChannelsList;
import mattw.youtube.datav3.entrypoints.PlaylistItemsList;
import mattw.youtube.datav3.entrypoints.SearchList;
import mattw.youtube.datav3.entrypoints.VideosList;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Database entry for searched YouTube entries (Video, Channel, Playlist).
 * getYouTubeId() from YouTubeObject represents the GroupItem ID.
 *
 * @since 2018-12-30
 * @author mattwright324
 */
public class GroupItem extends YouTubeObject {

    public static String NO_ITEMS = "GI000";
    public static String ALL_ITEMS = "GI001";

    private String channelTitle;
    private long published;
    private long lastChecked;

    /**
     * Used for converting selected search items for inserting into database.
     */
    public GroupItem(SearchList.Item item) {
        super(item.getId().getId(), item.getSnippet().getTitle(),
                item.getSnippet().getThumbnails().getMedium().getURL().toString(), true);
        this.published = item.getSnippet().getPublishedAt().getTime();
        this.channelTitle = item.getSnippet().getChannelTitle();
        this.lastChecked = 0;
        if(item.getId().getVideoId() != null) setTypeId(YType.VIDEO);
        if(item.getId().getChannelId() != null) setTypeId(YType.CHANNEL);
        if(item.getId().getPlaylistId() != null) setTypeId(YType.PLAYLIST);
    }

    /**
     * Used by ManageGroupsManager when user submits link directly.
     *
     * @param item VideosList.Item
     */
    public GroupItem(VideosList.Item item) {
        this(item.getId(), YType.VIDEO, item.getSnippet().getTitle(), item.getSnippet().getChannelTitle(),
                item.getSnippet().getThumbnails().getMedium().getURL().toString(), item.getSnippet().getPublishedAt().getTime(), 0);
    }

    /**
     * Used by ManageGroupsManager when user submits link directly.
     *
     * @param item ChannelsList.Item
     */
    public GroupItem(ChannelsList.Item item) {
        this(item.getId(), YType.CHANNEL, item.getSnippet().getTitle(), item.getSnippet().getTitle(),
                item.getSnippet().getThumbnails().getMedium().getURL().toString(), item.getSnippet().getPublishedAt().getTime(), 0);
    }

    /**
     * Used by ManageGroupsManager when user submits link directly.
     *
     * @param item PlaylistItemsList.Item
     */
    public GroupItem(PlaylistItemsList.Item item) {
        this(item.getId(), YType.PLAYLIST, item.getSnippet().getTitle(), item.getSnippet().getChannelTitle(),
                item.getSnippet().getThumbnails().getMedium().getURL().toString(), item.getSnippet().getPublishedAt().getTime(), 0);
    }

    /**
     * Used for "All Items (#)" and "No items" display in the "Comment Search" and "Group Manager" ComboBoxes.
     */
    public GroupItem(String gitemId, String title) {
        super(gitemId, title, null, false);
    }

    /**
     * Used by the database when querying for group items.
     */
    public GroupItem(String gitemId, YType typeId, String title, String channelTitle, String thumbUrl, long published, long lastChecked) {
        super(gitemId, title, thumbUrl, true);
        setTypeId(typeId);
        this.channelTitle = channelTitle;
        this.published = published;
        this.lastChecked = lastChecked;
    }

    /**
     * Parses video, playlist, and channel links to create a GroupItem.
     *
     * @param link a video, playlist, or channel link
     * @throws IOException there was a problem parsing the link
     */
    public GroupItem(String link) throws IOException {
        ofLink(link);
    }

    private void ofLink(String fullLink) throws IOException {
        logger.debug("Matching link to type [fullLink={}]", fullLink);

        Pattern video1 = Pattern.compile("(?:http[s]?://youtu.be/)([\\w_\\-]+)");
        Pattern video2 = Pattern.compile("(?:http[s]?://www.youtube.com/watch\\?v=)([\\w_\\-]+)");
        Pattern playlist = Pattern.compile("(?:http[s]?://www.youtube.com/playlist\\?list=)([\\w_\\-]+)");
        Pattern channel1 = Pattern.compile("(?:http[s]?://www.youtube.com/channel/)([\\w_\\-]+)");
        Pattern channel2 = Pattern.compile("(?:http[s]?://www.youtube.com/user/)([\\w_\\-]+)");

        Matcher m;
        YType type = YType.UNKNOWN;
        boolean channelUsername = false;
        String result = "";
        if((m = video1.matcher(fullLink)).matches()) {
            result = m.group(1);
            type = YType.VIDEO;
        } else if((m = video2.matcher(fullLink)).matches()) {
            result = m.group(1);
            type = YType.VIDEO;
        } else if((m = playlist.matcher(fullLink)).matches()) {
            result = m.group(1);
            type = YType.PLAYLIST;
        } else if((m = channel1.matcher(fullLink)).matches()) {
            result = m.group(1);
            type = YType.CHANNEL;
        } else if((m = channel2.matcher(fullLink)).matches()) {
            result = m.group(1);
            type = YType.CHANNEL;
            channelUsername = true;
        }

        YouTubeData3 youtube = FXMLSuite.getYoutubeApi();

        if(result.isEmpty()) {
            throw new IOException(String.format("Input did not match expected formats [fullLink=%s]", fullLink));
        } else {
            if(type == YType.VIDEO) {
                VideosList vl = ((VideosList) youtube.videosList().part(Parts.SNIPPET))
                        .getByIds(result, "");
                if(vl.hasItems()) {
                    VideosList.Item item = vl.getItems()[0];

                    duplicate(new GroupItem(item));
                }
            } else if(type == YType.CHANNEL) {
                ChannelsList cl = youtube.channelsList().part(Parts.SNIPPET);
                if(!channelUsername) {
                    cl = cl.getByChannel(result, "");
                } else {
                    cl = cl.getByUsername(result, "");
                }

                if(cl.hasItems()) {
                    ChannelsList.Item item = cl.getItems()[0];

                    duplicate(new GroupItem(item));
                }
            } else if(type == YType.PLAYLIST) {
                PlaylistItemsList pl = ((PlaylistItemsList) youtube.playlistItemsList().part(Parts.SNIPPET))
                        .get(result, "");
                if(pl.hasItems()) {
                    PlaylistItemsList.Item item = pl.getItems()[0];

                    duplicate(new GroupItem(item));
                }
            } else {
                throw new IOException("Unexpected result, link was not of type ");
            }
        }
    }

    /**
     * Copies fields of submitted GroupItem to this instance's fields.
     *
     * @param groupItem item to copy fields from
     */
    private void duplicate(GroupItem groupItem) {
        setTypeId(groupItem.getTypeId());
        setYoutubeId(groupItem.getYoutubeId());
        setTitle(groupItem.getTitle());
        setThumbUrl(groupItem.getThumbUrl());
        setFetchThumb(groupItem.isFetchThumb());
        setYouTubeLink(groupItem.getYouTubeLink());

        setChannelTitle(groupItem.getChannelTitle());
        setLastChecked(groupItem.getLastChecked());
        setPublished(groupItem.getPublished());

    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public long getPublished() {
        return published;
    }

    public void setPublished(long published) {
        this.published = published;
    }

    public long getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(long timestamp) {
        this.lastChecked = timestamp;
    }

    public String toString() {
        return getTitle();
    }
}
