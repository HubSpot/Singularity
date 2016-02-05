package com.hubspot.singularity.executor.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;

public class SingularityExecutorLogrotateAdditionalFile {
    private final String filename;
    private final Optional<String> extension;
    private final Optional<String> dateformat;

    @JsonCreator
    public static SingularityExecutorLogrotateAdditionalFile fromString(String value) {
        return new SingularityExecutorLogrotateAdditionalFile(value, Optional.<String>absent(), Optional.<String>absent());
    }

    @JsonCreator
    public SingularityExecutorLogrotateAdditionalFile(@JsonProperty("filename") String filename,
        @JsonProperty("extension") Optional<String> extension,
        @JsonProperty("dateformat") Optional<String> dateformat) {
        this.filename = filename;
        this.extension = extension;
        this.dateformat = dateformat;
    }

    public String getFilename() {
        return filename;
    }

    public Optional<String> getExtension() {
        return extension;
    }

    public Optional<String> getDateformat() {
        return dateformat;
    }
}
