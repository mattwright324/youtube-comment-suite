Build Documentation
=

This project is configured to be built with Gradle.

### Building youtube-comment-suite

Get a copy of the repository

```
git clone https://github.com/mattwright324/youtube-comment-suite.git
```

Run the Gradle `jpackage` command

The build files will appear in folder `build/jpackage/`


### Running youtube-comment-suite

There are two ways to run this application from your IDE

1. **With IDE** Right click on file and run `src/main/java/io.mattw.youtube.commentsuite/CommentSuite.java`
2. **With Gradle** In terminal run `gradlew clean build run`
3. **From jpackage** In `build/jpackage/youtube-comment-suite/` double click `youtube-comment-suite.exe`

*Note: My YouTube API key provided in the application is not restricted at all given it isn't a website.
Access should work for all local development. Please do not abuse it such that it uses up the daily quota.*
