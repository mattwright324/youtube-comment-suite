package io.mattw.youtube.commentsuite.fxml;

public enum ExportFormat {
    JSON("json"),
    CSV("csv");

    private final String extension;

    ExportFormat(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
