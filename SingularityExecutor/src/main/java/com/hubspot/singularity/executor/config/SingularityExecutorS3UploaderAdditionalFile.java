package com.hubspot.singularity.executor.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityExecutorS3UploaderAdditionalFile {
    private final String filename;
    private final Optional<String> s3UploaderBucket;
    private final Optional<String> s3UploaderKeyPattern;
    private final Optional<String> s3UploaderFilenameHint;
    private final Optional<String> directory;

    @JsonCreator
    public static SingularityExecutorS3UploaderAdditionalFile fromString(String value) {
        return new SingularityExecutorS3UploaderAdditionalFile(value, Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent());
    }

    @JsonCreator
    public SingularityExecutorS3UploaderAdditionalFile(@JsonProperty("filename") String filename,
        @JsonProperty("s3UploaderBucket") Optional<String> s3UploaderBucket,
        @JsonProperty("s3UploaderKeyPattern") Optional<String> s3UploaderKeyPattern,
        @JsonProperty("s3UploaderFilenameHint") Optional<String> s3UploaderFilenameHint,
        @JsonProperty("directory") Optional<String> directory) {
        this.filename = filename;
        this.s3UploaderBucket = s3UploaderBucket;
        this.s3UploaderKeyPattern = s3UploaderKeyPattern;
        this.s3UploaderFilenameHint = s3UploaderFilenameHint;
        this.directory = directory;
    }

    public String getFilename() {
        return filename;
    }

    public Optional<String> getS3UploaderBucket() {
        return s3UploaderBucket;
    }

    public Optional<String> getS3UploaderKeyPattern() {
        return s3UploaderKeyPattern;
    }

    public Optional<String> getS3UploaderFilenameHint() {
        return s3UploaderFilenameHint;
    }

    public Optional<String> getDirectory() {
        return directory;
    }

    @Override
    public String toString() {
        return "SingularityExecutorS3UploaderAdditionalFile[" +
            "filename='" + filename + '\'' +
            ", s3UploaderBucket=" + s3UploaderBucket +
            ", s3UploaderKeyPattern=" + s3UploaderKeyPattern +
            ", s3UploaderFilenameHint=" + s3UploaderFilenameHint +
            ", directory=" + directory +
            ']';
    }
}
