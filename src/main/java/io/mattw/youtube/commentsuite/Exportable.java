package io.mattw.youtube.commentsuite;

/**
 * To be used on classes that may be exported to JSON files as part of the export process.
 */
public interface Exportable {

    /**
     * Prepares the object for export.
     *
     * This may be achieved by formatting values into more readable formats (epoch millis to RFC3339 timestamp format)
     * and removing the unformatted values to null.
     *
     * In the case that fields should not be exported yet no readability processing needs to happen,
     * use the *transient* modifier to exclude them instead of setting it to null.
     */
    void prepForExport();

}
