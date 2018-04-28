# YouTube Comment Suite

![Github All Releases](https://img.shields.io/github/downloads/mattwright324/youtube-comment-suite/total.svg)
![GitHub release](https://img.shields.io/github/release/mattwright324/youtube-comment-suite.svg)
![Github Releases](https://img.shields.io/github/downloads/mattwright324/youtube-comment-suite/latest/total.svg)

Aggregate YouTube comments from numerous videos, playlists, and channels for archiving, general search, and showing activity. 
Achieve the functionality of the *Community > Comments* tool that is provided to YouTube creators and more.

Want to see how often a keyword/topic comes up?
Have a question that may have been answered in the thousands of comments and videos?
Want to know who the most active and popular fans are?
Want to see a user's comment history over an entire channel or channels?

![Example](https://i.imgur.com/uF1Fqfg.png)

### Features
* Cross-platform using Java 8 and JavaFX.
* Manually add or search for videos, playlists, and channels.
* Display stats about videos and comments: per-week graphs, most popular, most disliked, most active, comments disabled.
* Search YouTube normally and by location. Geolocation button provided.
* Search for comments by type, display name, keyword, length, date.
* Option to download video thumbnails for further archival or offline viewing.
* Sign in with multiple YouTube accounts and reply to comments.

### Walkthrough
A **Group** is a collection of **GroupItems** which are videos, channels, or playlists. A group of these items check channels and playlists for new videos and then for new comments on all the videos under them. 

**Creating and Managing Group** is very easy and can be done in a couple ways. 
*Default* is the standard group created when no other groups have been. 

**Choices:** 
* Select the *Default* group while in the *Manage Groups* tab
* Click **Create New Group** button and type in the group name
* Search for **GroupItems** in the *Search YouTube* tab and click **Add to Group** at the bottom from which to add to an existing or new group.

Click **Add Item** while managing a group and paste in the direct YouTube link to a video, playlist, or channel to quickly add what you want to download comments to. Otherwise, search for videos, playlists, and channels in the *Search YouTube* tab.

**Refresh**ing a selected group is the most important feature -- this button downloads new video and comment data to store in the database. 
Of course, the larger the channel and more interactive the fans, the longer this will take. Although, after the first refresh the process will be quicker as most comments would have been stored.

**Searching for Comments** is also very easy and may only be done when a group has been **Refresh**ed since there must be data to search through. 
Provide the desired constraints (date, name, text, type) and click **Search** to find those comments. 
Clicking on a comment will load the video context to the left of the comment list. Comments may only be replied to while signed into YouTube with at least one account.

Signing into YouTube may be done by clicking the settings button to the top right. Click **Add Account** and go through the sign-in process.

### Settings

The settings are opened with the icon in the top right. 
Options include prefixing names before replying to comments, auto-loading stats when managing a group (this can make things quicker - pressing **Reload** will do this action), and downloading thumbs for archival purposes.
Download thumbs is only recommended for archiving and can take up disk space. YouTube accounts can be signed in and out in the **YouTube Accounts** section. 
**Maintenance** lets you clean and reset the database. **Clean** performs a `vacuum` operation to shrink the database to its smallest possible size. **Reset** will delete everything and start over -- data is not recoverable. The **About** section will display the release version and link back to the GitHub repo.
