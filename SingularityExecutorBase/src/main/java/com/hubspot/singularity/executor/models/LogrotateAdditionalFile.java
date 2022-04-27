package com.hubspot.singularity.executor.models;

import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import java.util.Optional;

public class LogrotateAdditionalFile {

  private final String filename;
  private final String extension;
  private final String dateformat;
  private final Optional<SingularityExecutorLogrotateFrequency> logrotateFrequencyOverride;
  private final Optional<String> logrotateSizeOverride;

  public LogrotateAdditionalFile(
    String filename,
    String extension,
    String dateformat,
    Optional<SingularityExecutorLogrotateFrequency> logrotateFrequencyOverride,
    Optional<String> logrotateSizeOverride
  ) {
    this.filename = filename;
    this.extension = extension;
    this.dateformat = dateformat;
    this.logrotateFrequencyOverride = logrotateFrequencyOverride;
    this.logrotateSizeOverride = logrotateSizeOverride;
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

  public Optional<SingularityExecutorLogrotateFrequency> getLogrotateFrequencyOverride() {
    return logrotateFrequencyOverride;
  }

  public String getLogrotateFrequencyOverrideValue() {
    return logrotateFrequencyOverride
      .map(SingularityExecutorLogrotateFrequency::getLogrotateValue)
      .orElse("");
  }

  public String getLogrotateSizeOverride() {
    return logrotateSizeOverride.orElse("");
  }

  @Override
  public String toString() {
    return (
      "LogrotateAdditionalFile{" +
      "filename='" +
      filename +
      '\'' +
      ", extension='" +
      extension +
      '\'' +
      ", dateformat='" +
      dateformat +
      '\'' +
      ", frequency='" +
      logrotateFrequencyOverride +
      '\'' +
      ", size='" +
      logrotateSizeOverride +
      '\'' +
      '}'
    );
  }
}
