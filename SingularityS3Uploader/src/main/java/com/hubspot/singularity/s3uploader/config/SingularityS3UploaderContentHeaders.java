package com.hubspot.singularity.s3uploader.config;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityS3UploaderContentHeaders {
    @NotEmpty
    private final String filenameEndsWith;

    private final Optional<String> contentType;
    private final Optional<String> contentEncoding;

    @JsonCreator
    public SingularityS3UploaderContentHeaders(@JsonProperty("filenameEndsWith") String filenameEndsWith, @JsonProperty("contentType") Optional<String> contentType, @JsonProperty("contentEncoding") Optional<String> contentEncoding) {
        this.filenameEndsWith = filenameEndsWith;
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
    }

    public String getFilenameEndsWith() {
        return filenameEndsWith;
    }

    public Optional<String> getContentType() {
        return contentType;
    }

    public Optional<String> getContentEncoding() {
        return contentEncoding;
    }

    @Override
    public String toString() {
        return "SingularityContentTypeAndEncoding{" +
            "contentType=" + contentType +
            ", contentEncoding=" + contentEncoding +
            '}';
    }
}
