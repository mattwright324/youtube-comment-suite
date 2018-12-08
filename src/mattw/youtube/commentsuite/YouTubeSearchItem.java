package mattw.youtube.commentsuite;

import mattw.youtube.commentsuite.db.YouTubeObject;
import mattw.youtube.datav3.resources.SearchList;

/**
 * @author mattwright324
 */
public class YouTubeSearchItem extends YouTubeObject {

    public YouTubeSearchItem(SearchList.Item searchListItem) {
        setYoutubeId(searchListItem.id.getId());
        setTitle(searchListItem.snippet.title);
        setThumbUrl(searchListItem.snippet.thumbnails.medium.url.toString());
        setFetchThumb(false);
    }
}
