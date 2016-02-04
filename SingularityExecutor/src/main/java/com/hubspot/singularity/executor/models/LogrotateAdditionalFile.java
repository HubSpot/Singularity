package com.hubspot.singularity.executor.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class LogrotateAdditionalFile {
    private final String filename;
    private final String extension;

    @JsonCreator
    public static LogrotateAdditionalFile fromString(String value) {
        return new LogrotateAdditionalFile(value, null);
    }

    @JsonCreator
    public LogrotateAdditionalFile(@JsonProperty("filename") String filename, @JsonProperty("extension") String extension) {
        this.filename = filename;
        this.extension = extension;
    }

    public String getFilename() {
        return filename;
    }

    public String getExtension() {
        return extension;
    }

    @Override public String toString() {
        return "LogrotateAdditionalFile[" +
            "filename='" + filename + '\'' +
            ", extension='" + extension + '\'' +
            ']';
    }
}
