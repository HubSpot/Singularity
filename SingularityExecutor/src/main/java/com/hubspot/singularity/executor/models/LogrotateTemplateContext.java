package com.hubspot.singularity.executor.models;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.io.Files;
import com.hubspot.singularity.executor.config.LogrotateCompressionSettings;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.config.SingularityExecutorLogrotateAdditionalFile;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;

/**
 * Handlebars context for generating logrotate.conf files.
 * Check `man logrotate` for more information.
 */
public class LogrotateTemplateContext {

  private final SingularityExecutorTaskDefinition taskDefinition;
  private final SingularityExecutorConfiguration configuration;

  public LogrotateTemplateContext(SingularityExecutorConfiguration configuration, SingularityExecutorTaskDefinition taskDefinition) {
    this.configuration = configuration;
    this.taskDefinition = taskDefinition;
  }

  public String getRotateDateformat() {
    return configuration.getLogrotateDateformat();
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

  public String getLogrotateFrequency() {
    return taskDefinition.getExecutorData().getLogrotateFrequency().or(configuration.getLogrotateFrequency()).getLogrotateValue();
  }

  public String getCompressCmd() {
    return configuration.getLogrotateCompressionSettings().getCompressCmd().orNull();
  }

  public String getUncompressCmd() {
    return configuration.getLogrotateCompressionSettings().getUncompressCmd().orNull();
  }

  public String getCompressOptions() {
    return configuration.getLogrotateCompressionSettings().getCompressOptions().orNull();
  }

  public String getCompressExt() {
    return configuration.getLogrotateCompressionSettings().getCompressExt().orNull();
  }

  /**
   * Extra files for logrotate to rotate. If these do not exist logrotate will continue without error.
   * @return filenames to rotate.
   */
  public List<LogrotateAdditionalFile> getExtrasFiles() {
    final List<SingularityExecutorLogrotateAdditionalFile> original = configuration.getLogrotateAdditionalFiles();
    final List<LogrotateAdditionalFile> transformed = new ArrayList<>(original.size());

    for (SingularityExecutorLogrotateAdditionalFile additionalFile : original) {
      transformed.add(new LogrotateAdditionalFile(taskDefinition.getTaskDirectoryPath().resolve(additionalFile.getFilename()).toString(), additionalFile.getExtension().or(Strings.emptyToNull(Files.getFileExtension(additionalFile.getFilename()))), additionalFile.getDateformat().or(configuration.getLogrotateExtrasDateformat())));
    }

    return transformed;
  }

  private Optional<String> parseFilenameExtension(String filename) {
    final int lastPeriodIndex = filename.lastIndexOf('.');

    if ((lastPeriodIndex > -1) && !filename.substring(lastPeriodIndex + 1).contains("*")) {
      return Optional.of(filename.substring(lastPeriodIndex + 1));
    } else {
      return Optional.absent();
    }
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

  @Override
  public String toString() {
    return "LogrotateTemplateContext [taskId=" + taskDefinition.getTaskId() + "]";
  }


}
