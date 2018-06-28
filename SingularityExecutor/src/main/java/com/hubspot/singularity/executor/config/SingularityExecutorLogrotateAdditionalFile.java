package com.hubspot.singularity.executor.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;

public class SingularityExecutorLogrotateAdditionalFile {
    private final String filename;
    private final String extension;
    private final String dateformat;
    private final SingularityExecutorLogrotateFrequency frequencyOverride;

    @JsonCreator
    public static SingularityExecutorLogrotateAdditionalFile fromString(String value) {
        return new SingularityExecutorLogrotateAdditionalFile(value, null, null, null);
    }

    @JsonCreator
    public SingularityExecutorLogrotateAdditionalFile(@JsonProperty("filename") String filename,
        @JsonProperty("extension") String extension,
        @JsonProperty("dateformat") String dateformat,
        @JsonProperty("logrotateFrequencyOverride") SingularityExecutorLogrotateFrequency frequencyOverride) {
        this.filename = filename;
        this.extension = extension;
        this.dateformat = dateformat;
        this.frequencyOverride = frequencyOverride;
    }

    public String getFilename() {
        return filename;
    }

    public Optional<String> getExtension() {
        return Optional.fromNullable(extension);
    }

    public Optional<String> getDateformat() {
        return Optional.fromNullable(dateformat);
    }

    public Optional<SingularityExecutorLogrotateFrequency> getFrequencyOverride() {
        return Optional.fromNullable(frequencyOverride);
    }
}
