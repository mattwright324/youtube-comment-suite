Build Documentation
=

The guide is mainly built around IntelliJ, I'm sure this could be improved to be more IDE-agnostic
with an IntelliJ artifact equivalent configured in Maven or Gradle. This way other IDEs that
support either of those frameworks could do the same.

### Building youtube-comment-suite

Get a copy of the repository

```
git clone https://github.com/mattwright324/youtube-comment-suite.git
```

Install [Java Development Kit 8](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)
if it isn't installed already.

Install [IntelliJ IDEA](https://www.jetbrains.com/idea/download/#section=windows)
if it isn't installed already.

Open the cloned project folder with IntelliJ and choose all the default things it picks up. 
This project uses Maven for dependency management
so reload/import Maven settings to get the required dependecy jars. 
Make sure the project SDK is using JDK8.

#### Continue if building to jar

Currently, this project is pre-configured to be built with an IntelliJ
artifact which is what gets zipped up in a release on the GitHub page for this project. 
What the IntelliJ artifact does:
- Compile source to youtube-comment-suite.jar
  - MANIFEST.mf settings
    - Main class `io.mattw.youtube.commentsuite.FXMLSuite.java`
    - Manual creation of class-path jars list from the `lib/` subfolder.
- Include in dir: README.md and LICENSE
- Copy all Maven dependency jars to subfolder `lib/`

To perform the artifact build, from the menu bar `Build -> Build Artifacts... -> youtube-comment-suite:jar -> Build`.
Choose `Rebuild` to overwrite with new changes if you did a `Build` already.

If updating the `pom.xml` dependency versions, class-path jars list must be updated as well as the artifact settings removing
the old versions and putting the new ones in the lib/ subfolder

When it comes to a release, I then manually rename the jar and zip to the new version before uploading. 
This version should match what is in `src/main/java/io.mattw.youtube.commentsuite.fxml/Settings.fxml`.

### Running youtube-comment-suite

There are two ways to run this application from IntelliJ

1. **From in IDE** Right click and choose `Run FXMLSuite.main()` on file `src/main/java/io.mattw.youtube.commentsuite/FXMLSuite.java`
2. **From build to jar** If compiled through the IntelliJ Artifact, double click the jar at `out/artifacts/youtube_comment_suite_jar/youtube-comment-suite.jar` 

*Note: My YouTube API key provided in the application is not restricted at all given it isn't a website.
Access should work for all local development. Please do not abuse it such that it uses up the daily quota.*
