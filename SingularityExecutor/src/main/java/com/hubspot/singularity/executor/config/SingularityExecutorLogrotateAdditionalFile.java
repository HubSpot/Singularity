package com.hubspot.singularity.executor.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;

public class SingularityExecutorLogrotateAdditionalFile {
    private final String filename;
    private Optional<String> extension;
    private Optional<String> dateformat;
    private Optional<SingularityExecutorLogrotateFrequency> logrotateFrequencyOverride;

    @JsonCreator
    public static SingularityExecutorLogrotateAdditionalFile fromString(String value) {
        return new SingularityExecutorLogrotateAdditionalFile(value, null, null, null);
    }

    @JsonCreator
    public SingularityExecutorLogrotateAdditionalFile(@JsonProperty("filename") String filename,
        @JsonProperty("extension") String extension,
        @JsonProperty("dateformat") String dateformat,
        @JsonProperty("logrotateFrequencyOverride") SingularityExecutorLogrotateFrequency logrotateFrequencyOverride) {
        this.filename = filename;
        this.extension = Optional.fromNullable(extension);
        this.dateformat = Optional.fromNullable(dateformat);
        this.logrotateFrequencyOverride = Optional.fromNullable(logrotateFrequencyOverride);
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

    public Optional<SingularityExecutorLogrotateFrequency> getLogrotateFrequencyOverride() {
        return logrotateFrequencyOverride;
    }

}
