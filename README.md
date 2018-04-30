# <img src="https://i.imgur.com/edLrUKt.png" width="466" height="64" />

![Github All Releases](https://img.shields.io/github/downloads/mattwright324/youtube-comment-suite/total.svg)
![GitHub release](https://img.shields.io/github/release/mattwright324/youtube-comment-suite.svg)
![Github Releases](https://img.shields.io/github/downloads/mattwright324/youtube-comment-suite/latest/total.svg)

YouTube Comment Suite lets you aggregate YouTube comments from numerous videos, playlists, and channels for archiving, general search, and showing activity. 
Achieve the functionality of the *Community > Comments* tool that is provided to YouTube creators and more.

![Example](https://i.imgur.com/pE57Cql.png)

* Want to see how often a keyword/topic comes up?
* Have a question that may have been answered in the thousands of comments and videos?
* Want to know who the most active and popular fans are?
* Want to see a user's comment history over an entire channel or channels?

## Features
* Cross-platform using Java 8 and JavaFX
* Include multiple channels, playlists, and videos in a single group.
* Search for comments by video, type, username, keyword, length, and date.
* Display stats about videos: publishes per week, most popular, most disliked, most commented, and disabled.
* Display stats about comments: posts per week, most active posters, most popular posters.
* Option to save thumbnails and profiles for archival and offline viewing.
* Sign into multiple YouTube accounts and choose which to reply with.
* View video context when selecting a comment.

## Getting Started
[![GitHub Releases](https://img.shields.io/badge/downloads-releases-brightgreen.svg?maxAge=60&style=flat-square)](https://github.com/mattwright324/youtube-comment-suite/releases)

### Windows
Extract the latest release zip file and run `youtube-comment-suite.jar`. 

### Linux / Ubuntu
Extract the latest release zip file into its own folder. Before you can run the jar file, the version of Java installed may cause issue. Run `java -version` in the command line.

* If your version of Java is `OpenJDK` you will have to install JavaFX which is not included. Use the command `sudo apt-get install openjfx`. 
* If your version of Java is `OracleJDK`, JavaFX is included and you are good to go!

## Using a Group
Begin managing a group by starting in the `Manage Groups` tab and selecting the Group that you want. `Default` is the group provided by default and can be renamed using the manager.

Adding channels, playlists, or videos to the group can be done one of two ways:
* **A:** While managing the group, click the `Add Item` button and paste in the full URL to the video, playlist, or channel.
* **B:** Search for videos, playlists, or channels using the `Search YouTube` tab at the top. Select any of the results and click the `Add to Group` button at the bottom.

Download all of the comments by clicking the `Refresh` button. * *Errors may appear while refreshing. Most commonly 404 errors, they can be caused by a slowed or interrupted internet. **Excessive 404 errors** may be a result of reaching the YouTube API Quota limit. If this happens, wait until the next day when the Quota resets at midnight Pacific Time (PT).*

Stats are viewable in the group manager after every refresh. This can be turned off in the settings.

Search all of the downloaded comments using the `Search Comments` tab. Clicking on a comment will load their profile picture(s) and related video context. Right clicking selected comments provide options to open a direct link to the comment, comment's author, and comment's video of origin. 
