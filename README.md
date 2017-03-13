# youtube-comment-suite
Keep track of comments and replies from videos, channels, and playlists. Search for videos, playlists, and channels. Search through found comments by name, text, type. 

#### Requirements
1. Java 8
2. JavaFX (included with Java 8)

![Search your comments](http://i.imgur.com/orTeqX8.png)

### How-To
#### How do I retrieve the comments from my channel, playlist, or videos?
1. Starting off in the `Search Youtube` section, type keywords into the search-bar just like you would on the website. Click on the channel, playlist, and/or videos and then click `Add To Group` at the bottom. Add your selection to the Default group, another existing group, or create a new group with a unique name. 
2. Go to the `Manage Groups` section and select the group you chose from above. Here you can view the last time you checked all the items in your group. Click `Refresh` at the top to begin checking the group items for new videos (channels or playlists), comments, and replies. 

    First-time refreshing takes the longest because everything is new and saving to the `commentsuite.db` database file. Future refreshes will take significantly less time as only new content has to be saved. For example, a larger channel with anywhere from 100 to 1000 videos and approximately 600 to 3000 comments per videos may take approximately 25 minutes the first time and only 3 to 8 minutes the next time.
    
3. You may now search through all the comments on your channel, playlist, and or videos and even reply to them if you are signed in.

#### How do I sign in?
You may sign in by clicking `Setup` at the top right corner and then `Sign in`. You may sign out at any time.


## Gallery

![Search for your channels.](http://i.imgur.com/WwjkmIz.png)
Search Youtube for your channels just as you would on the standard website.

![See interesting statistics](http://i.imgur.com/OG07CKM.png)
See interesting statistics: most active and popular viewers, most comment comments, comment counts per week, video counts per week, most popular, disliked, and commented videos, and a list of disabled videos. 

![See progress as you refresh a group](http://i.imgur.com/lJyS1ms.png)
See how much is new as you refresh a group. 

![Manage several channels](http://i.imgur.com/68bReJ2.png)
Check what you've already found and what exists in your group.

