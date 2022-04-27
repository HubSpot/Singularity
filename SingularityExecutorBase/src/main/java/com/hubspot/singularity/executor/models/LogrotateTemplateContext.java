package com.hubspot.singularity.executor.models;

import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.config.SingularityExecutorLogrotateAdditionalFile;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Handlebars context for generating logrotate.conf files.
 * Check `man logrotate` for more information.
 */
public class LogrotateTemplateContext {

  private static final Predicate<LogrotateAdditionalFile> BELONGS_IN_HOURLY_OR_MORE_FREQUENT_CRON_FORCED_LOGROTATE_CONF = p ->
    p.getLogrotateFrequencyOverride().isPresent() &&
    SingularityExecutorLogrotateFrequency.HOURLY_OR_MORE_FREQUENT_LOGROTATE_VALUES.contains(
      p.getLogrotateFrequencyOverride().get()
    );

  private static final Predicate<LogrotateAdditionalFile> BELONGS_IN_SIZE_BASED_LOGROTATE_CONF = p ->
    p.getLogrotateSizeOverride() != null && !p.getLogrotateSizeOverride().isEmpty();

  private final SingularityExecutorTaskDefinition taskDefinition;
  private final SingularityExecutorConfiguration configuration;

  private Optional<SingularityExecutorLogrotateFrequency> extrasFilesFrequencyFilter = Optional.empty();

  public LogrotateTemplateContext(
    SingularityExecutorConfiguration configuration,
    SingularityExecutorTaskDefinition taskDefinition
  ) {
    this.configuration = configuration;
    this.taskDefinition = taskDefinition;
  }

  public String getRotateDateformat() {
    return configuration.getLogrotateDateformat().startsWith("-")
      ? configuration.getLogrotateDateformat().substring(1)
      : configuration.getLogrotateDateformat();
  }

  public int getRotateCount() {
    return configuration.getLogrotateCount();
  }

  public int getMaxageDays() {
    return configuration.getLogrotateMaxageDays();
  }

  public String getRotateDirectory() {
    return configuration.getLogrotateToDirectory();
  }

  public boolean getShouldLogRotateLogFile() {
    return taskDefinition.shouldLogrotateLogFile();
  }

  public String getTaskDirectory() {
    return taskDefinition.getTaskDirectoryPath().toString();
  }

  public String getLogrotateFrequency() {
    return taskDefinition
      .getExecutorData()
      .getLogrotateFrequency()
      .orElse(configuration.getLogrotateFrequency())
      .getLogrotateValue();
  }

  public String getCompressCmd() {
    return configuration.getLogrotateCompressionSettings().getCompressCmd().orElse(null);
  }

  public String getUncompressCmd() {
    return configuration
      .getLogrotateCompressionSettings()
      .getUncompressCmd()
      .orElse(null);
  }

  public String getCompressOptions() {
    return configuration
      .getLogrotateCompressionSettings()
      .getCompressOptions()
      .orElse(null);
  }

  public String getCompressExt() {
    return configuration.getLogrotateCompressionSettings().getCompressExt().orElse(null);
  }

  /**
   * Extra files for logrotate to rotate (less frequent than hourly). If these do not exist logrotate will continue without error.
   * @return filenames to rotate.
   */
  public List<LogrotateAdditionalFile> getExtrasFiles() {
    return getAllExtraFiles()
      .stream()
      .filter(
        BELONGS_IN_HOURLY_OR_MORE_FREQUENT_CRON_FORCED_LOGROTATE_CONF
          .negate()
          .and(BELONGS_IN_SIZE_BASED_LOGROTATE_CONF.negate())
      )
      .collect(Collectors.toList());
  }

  /**
   * Extra files for logrotate to rotate hourly or more frequently than hourly.
   * Since we don't want to rely on native `hourly` (or more frequent) support in logrotate(8),
   *   we fake it by running an hourly cron with a force `-f` flag.
   * If these do not exist logrotate will continue without error.
   * If `setExtrasFilesFrequencyFilter()` has been called on this instance,
   *   then we only return matching logrotateAdditionalFiles configs.
   * @return filenames to rotate.
   */
  public List<LogrotateAdditionalFile> getExtrasFilesHourlyOrMoreFrequent() {
    Stream<LogrotateAdditionalFile> hourlyOrMoreFrequentLogrotateAdditionalFiles = getAllExtraFiles()
      .stream()
      .filter(BELONGS_IN_HOURLY_OR_MORE_FREQUENT_CRON_FORCED_LOGROTATE_CONF);

    return extrasFilesFrequencyFilter
      .map(singularityExecutorLogrotateFrequency ->
        hourlyOrMoreFrequentLogrotateAdditionalFiles
          .filter(file ->
            file
              .getLogrotateFrequencyOverride()
              .map(someFrequencyOverride ->
                someFrequencyOverride == singularityExecutorLogrotateFrequency
              )
              .orElse(false)
          )
          .collect(Collectors.toList())
      )
      .orElseGet(() ->
        hourlyOrMoreFrequentLogrotateAdditionalFiles.collect(Collectors.toList())
      );
  }

  /**
   * Extra files for logrotate to rotate based on size.
   * We implement this via an hourly cron (without a force `-f` flag, so that the rotate only happens if the size threshold is exceeded).
   * If these do not exist logrotate will continue without error.
   * @return filenames to rotate.
   */
  public List<LogrotateAdditionalFile> getExtrasFilesSizeBased() {
    return getAllExtraFiles()
      .stream()
      .filter(BELONGS_IN_SIZE_BASED_LOGROTATE_CONF)
      .collect(Collectors.toList());
  }

  public boolean isGlobalLogrotateHourly() {
    return configuration
      .getLogrotateFrequency()
      .getLogrotateValue()
      .equals(SingularityExecutorLogrotateFrequency.HOURLY.getLogrotateValue());
  }

  private List<LogrotateAdditionalFile> getAllExtraFiles() {
    final List<SingularityExecutorLogrotateAdditionalFile> original = configuration.getLogrotateAdditionalFiles();
    final List<LogrotateAdditionalFile> transformed = new ArrayList<>(original.size());

    for (SingularityExecutorLogrotateAdditionalFile additionalFile : original) {
      String dateformat;
      if (additionalFile.getDateformat().isPresent()) {
        dateformat =
          additionalFile.getDateformat().get().startsWith("-")
            ? additionalFile.getDateformat().get().substring(1)
            : additionalFile.getDateformat().get();
      } else {
        dateformat =
          configuration.getLogrotateExtrasDateformat().startsWith("-")
            ? configuration.getLogrotateExtrasDateformat().substring(1)
            : configuration.getLogrotateExtrasDateformat();
      }

      transformed.add(
        new LogrotateAdditionalFile(
          taskDefinition
            .getTaskDirectoryPath()
            .resolve(additionalFile.getFilename())
            .toString(),
          additionalFile.getExtension().isPresent()
            ? additionalFile.getExtension().get()
            : Strings.emptyToNull(Files.getFileExtension(additionalFile.getFilename())), // Can't have possible null in .orElse()
          dateformat,
          additionalFile.getLogrotateFrequencyOverride(),
          additionalFile.getLogrotateSizeOverride()
        )
      );
    }

    return transformed;
  }

  public String getExtrasDateformat() {
    return configuration.getLogrotateExtrasDateformat();
  }

  /**
   * Default log to logrotate, defaults to service.log.
   * This if this log doesn't exist, logrotate will return an error message.
   */
  public String getLogfile() {
    return taskDefinition.getServiceLogOut();
  }

  public String getLogfileExtension() {
    return taskDefinition.getServiceLogOutExtension();
  }

  public String getLogfileName() {
    return taskDefinition.getServiceLogFileName();
  }

  public boolean isUseFileAttributes() {
    return configuration.isUseFileAttributes();
  }

  public void setExtrasFilesFrequencyFilter(
    SingularityExecutorLogrotateFrequency frequencyFilter
  ) {
    this.extrasFilesFrequencyFilter = Optional.of(frequencyFilter);
  }

  @Override
  public String toString() {
    return (
      "LogrotateTemplateContext{" +
      "taskDefinition=" +
      taskDefinition +
      ", configuration=" +
      configuration +
      '}'
    );
  }
}
