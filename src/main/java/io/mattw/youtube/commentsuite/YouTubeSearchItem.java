package io.mattw.youtube.commentsuite;

import com.google.api.services.youtube.model.SearchResult;
import io.mattw.youtube.commentsuite.db.YouTubeObject;

/**
 * @author mattwright324
 */
public class YouTubeSearchItem extends YouTubeObject {

    public YouTubeSearchItem(SearchResult searchResult) {
        setId(getIdFromResource(searchResult.getId()));
        setTitle(searchResult.getSnippet().getTitle());
        setThumbUrl(searchResult.getSnippet().getThumbnails().getMedium().getUrl());
    }
}
