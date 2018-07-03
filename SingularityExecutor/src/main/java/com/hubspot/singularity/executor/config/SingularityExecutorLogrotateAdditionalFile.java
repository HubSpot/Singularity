package com.hubspot.singularity.executor.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;

public class SingularityExecutorLogrotateAdditionalFile {
    private final String filename;
    private final Optional<String> extension;
    private final Optional<String> dateformat;
    private final Optional<SingularityExecutorLogrotateFrequency> logrotateFrequencyOverride;

    @JsonCreator
    public static SingularityExecutorLogrotateAdditionalFile fromString(String value) {
        return new SingularityExecutorLogrotateAdditionalFile(value, Optional.absent(), Optional.absent(), null);
    }

    @JsonCreator
    public SingularityExecutorLogrotateAdditionalFile(@JsonProperty("filename") String filename,
            @JsonProperty("extension") Optional<String> extension,
            @JsonProperty("dateformat") Optional<String> dateformat,
            @JsonProperty("logrotateFrequencyOverride") SingularityExecutorLogrotateFrequency logrotateFrequencyOverride) {
        this.filename = filename;
        this.extension = extension;
        this.dateformat = dateformat;
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
