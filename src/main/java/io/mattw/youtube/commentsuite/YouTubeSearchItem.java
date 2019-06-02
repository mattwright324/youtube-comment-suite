package io.mattw.youtube.commentsuite;

import io.mattw.youtube.commentsuite.db.YouTubeObject;
import io.mattw.youtube.datav3.entrypoints.SearchList;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class YouTubeSearchItem extends YouTubeObject {

    public YouTubeSearchItem(SearchList.Item searchListItem) {
        setYoutubeId(searchListItem.getId().getId());
        setTitle(searchListItem.getSnippet().getTitle());
        setThumbUrl(searchListItem.getSnippet().getThumbnails().getMedium().getURL().toString());
        setFetchThumb(false);
    }
}
