Build Documentation
=

This project is configured to be built with Maven.

### Building youtube-comment-suite

Get a copy of the repository

```
git clone https://github.com/mattwright324/youtube-comment-suite.git
```

Install [Java Development Kit 8](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)
if it isn't installed already.

Open the cloned project folder with your IDE of choice that supports Maven. 
Make sure the workspace and/or project is configured to use JDK8. 
Depending on the IDE, you may need to wait for dependencies to download and things to index.

To perform a project build, run the `mvn package` command. 

- **IntelliJ** In the Maven tab, double click the `package` option under Lifecycle.
- **Eclipse** Right click `pom.xml` -> `Run as...` -> Choose `Maven build...` -> Type `package` in the Goals textfield -> Run

The build files will appear in folder `target/package/` and contains the following:

- `lib/` folder containing dependency jars
- `LICENSE`
- `README.md`
- `youtube-comment-suite-#.#.#.jar`

This is what gets zipped up for a release.

The project version and year is configured in `pom.xml` with properties `project.version` and `current.year`.
Maven will use the version in the built jar name. 
Maven will also insert these values into `Settings.fxml` during the build.

### Running youtube-comment-suite

There are two ways to run this application from your IDE

1. **From in IDE** Right click on file and run `src/main/java/io.mattw.youtube.commentsuite/CommentSuite.java`
2. **From package** Double click the jar at `target/package/youtube-comment-suite-#.#.#.jar` 

*Note: My YouTube API key provided in the application is not restricted at all given it isn't a website.
Access should work for all local development. Please do not abuse it such that it uses up the daily quota.*
