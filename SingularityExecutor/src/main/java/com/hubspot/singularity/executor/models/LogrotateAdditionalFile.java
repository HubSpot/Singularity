package com.hubspot.singularity.executor.models;

import com.google.common.base.Optional;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;

public class LogrotateAdditionalFile {
    private final String filename;
    private final String extension;
    private final String dateformat;
    private final SingularityExecutorLogrotateFrequency logrotateFrequencyOverride;

    public LogrotateAdditionalFile(String filename, String extension, String dateformat, SingularityExecutorLogrotateFrequency logrotateFrequencyOverride) {
        this.filename = filename;
        this.extension = extension;
        this.dateformat = dateformat;
        this.logrotateFrequencyOverride = logrotateFrequencyOverride;
    }

    public String getFilename() {
        return filename;
    }

    public String getExtension() {
        return extension;
    }

    public String getDateformat() {
        return dateformat;
    }

    public Optional<String> getLogrotateFrequencyOverride() {
        return Optional.of(logrotateFrequencyOverride).isPresent() ?
            Optional.of(logrotateFrequencyOverride.getLogrotateValue()) : Optional.absent();
    }

    @Override
    public String toString() {
        return "LogrotateAdditionalFile{" +
            "filename='" + filename + '\'' +
            ", extension='" + extension + '\'' +
            ", dateformat='" + dateformat + '\'' +
            ", frequency='" + logrotateFrequencyOverride + '\'' +
            '}';
    }
}
