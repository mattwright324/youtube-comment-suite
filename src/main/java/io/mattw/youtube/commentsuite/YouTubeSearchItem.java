package io.mattw.youtube.commentsuite;

import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchResult;
import io.mattw.youtube.commentsuite.db.YouTubeObject;

/**
 * @since 2018-12-30
 * @author mattwright324
 */
public class YouTubeSearchItem extends YouTubeObject {

    public YouTubeSearchItem(SearchResult searchResult) {
        setYoutubeId(getId(searchResult.getId()));
        setTitle(searchResult.getSnippet().getTitle());
        setThumbUrl(searchResult.getSnippet().getThumbnails().getMedium().getUrl());
        setFetchThumb(false);
    }
}
