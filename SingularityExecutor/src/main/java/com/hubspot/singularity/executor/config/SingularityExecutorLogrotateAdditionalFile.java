package com.hubspot.singularity.executor.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;

public class SingularityExecutorLogrotateAdditionalFile {
  private final String filename;
  private final Optional<String> extension;
  private final Optional<String> dateformat;
  private final Optional<SingularityExecutorLogrotateFrequency> logrotateFrequencyOverride;
  private final Optional<String> logrotateSizeOverride;
  private final boolean deleteInExecutorCleanup;

  @JsonCreator
  @SuppressFBWarnings("NP_NULL_PARAM_DEREF_NONVIRTUAL")
  public static SingularityExecutorLogrotateAdditionalFile fromString(String value) {
    return new SingularityExecutorLogrotateAdditionalFile(
      value,
      Optional.empty(),
      Optional.empty(),
      null,
      null,
      false
    );
  }

  @JsonCreator
  public SingularityExecutorLogrotateAdditionalFile(
    @JsonProperty("filename") String filename,
    @JsonProperty("extension") Optional<String> extension,
    @JsonProperty("dateformat") Optional<String> dateformat,
    @JsonProperty(
      "logrotateFrequencyOverride"
    ) SingularityExecutorLogrotateFrequency logrotateFrequencyOverride,
    @JsonProperty("logrotateSizeOverride") String logrotateSizeOverride,
    @JsonProperty("deleteInExecutorCleanup") boolean deleteInExecutorCleanup
  ) {
    this.filename = filename;
    this.extension = extension;
    this.dateformat = dateformat;
    this.logrotateFrequencyOverride = Optional.ofNullable(logrotateFrequencyOverride);
    this.logrotateSizeOverride = Optional.ofNullable(logrotateSizeOverride);
    this.deleteInExecutorCleanup = deleteInExecutorCleanup;
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

  public Optional<String> getLogrotateSizeOverride() {
    return logrotateSizeOverride;
  }

  public boolean isDeleteInExecutorCleanup() {
    return deleteInExecutorCleanup;
  }
}
