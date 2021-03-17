package io.mattw.youtube.commentsuite.fxml;

import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.SearchResultSnippet;
import com.google.api.services.youtube.model.Thumbnail;
import com.google.api.services.youtube.model.ThumbnailDetails;
import io.mattw.youtube.commentsuite.db.GroupItemResolver;
import io.mattw.youtube.commentsuite.db.HasImage;

import java.util.Optional;

public class YouTubeSearchItem implements HasImage {

    private String id;
    private String title;
    private String thumbUrl;
    private SearchResult searchResult;

    public YouTubeSearchItem(final SearchResult searchResult) {
        this.searchResult = searchResult;

        this.id = Optional.ofNullable(searchResult)
                .map(SearchResult::getId)
                .map(GroupItemResolver::getIdFromResource)
                .orElse(null);
        this.title = Optional.ofNullable(searchResult)
                .map(SearchResult::getSnippet)
                .map(SearchResultSnippet::getTitle)
                .orElse(null);
        this.thumbUrl = Optional.ofNullable(searchResult)
                .map(SearchResult::getSnippet)
                .map(SearchResultSnippet::getThumbnails)
                .map(ThumbnailDetails::getMedium)
                .map(Thumbnail::getUrl)
                .orElse("https://placehold.it/64x64");
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public SearchResult getSearchResult() {
        return searchResult;
    }

}
