# youtube-comment-suite
Keep track of comments and replies from videos, channels, and playlists. Search for videos, playlists, and channels. Search through found comments by name, text, type. 

#### Requirements
1. Youtube Data API Key - [Guide to creating a key](https://developers.google.com/youtube/v3/getting-started).
2. Paste and save your key into the `Google API Key` field found in the `View Settings` tab.

### How-To
#### Finding Videos / Creating a Group
1. Search for a video or channel like you would on the Youtube website.
2. Make your selection in the table by clicking the check-boxes or `Select All` then `Add To Group`
3. Either name a new group or choose and existing one to add to.

#### Manage a Group
4. Select a group listed in the table. It will automatically load all connected Group Items and Videos (if found yet).
5. `Refresh Group` will take all Group Items and search for new videos, comments, replies, and update changed information.
6. You may also Delete or Edit the name of any group other than Default.

#### Searching Comments
7. Select the group to search from or even choose specific items from that Group.
8. Name and text searches check if the string contains what you type. Sqlite allows for the `%` wildcard which matches any character and length of characters between.
9. Sort comments by date, likes, reply count, alphabet. Can be filtered for just comments and or replies.
10. A comment can be viewed and scrolled. The html-comment is used on double-click or by right clicking for `View Full Comment`
11. Right clicking a comment can let you view the full comment, all the comments in the reply tree, open the video, or open the commenter's profile in browser.
12. Clicking on a comment will also load the thumbnail, title, description, views, likes/dislikes, and more for added context.
13. The video-context thumbnail is clickable to open the video.
