package com.hubspot.singularity.executor.models;

import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;

/**
 * Handlebars context for generating logrotate.conf files.
 * Check `man logrotate` for more information.
 */
public class LogrotateTemplateContext {

  private final SingularityExecutorTaskDefinition taskDefinition;
  private final SingularityExecutorConfiguration configuration;

  /**
   * @param configuration configuration to pull from.
   * @param taskDefinition information about the task we're writing the logrotate configs for.
   */
  public LogrotateTemplateContext(SingularityExecutorConfiguration configuration, SingularityExecutorTaskDefinition taskDefinition) {
    this.configuration = configuration;
    this.taskDefinition = taskDefinition;
  }

  public String getRotateDateformat() {
    return configuration.getLogrotateDateformat();
  }

  /**
   * Log files are rotated count times before being removed.
   * @return count.
   */
  public String getRotateCount() {
    return configuration.getLogrotateCount();
  }

  /**
   * Remove rotated logs older than $count days.
   * The age is only checked if the logfile is to be rotated.
   * @return days.
   */
  public String getMaxageDays() {
    return configuration.getLogrotateMaxageDays();
  }

  /**
   * Logs are moved into $directory for rotation.
   * @return directory.
   */
  public String getRotateDirectory() {
    return configuration.getLogrotateToDirectory();
  }

  /**
   * Extra files for logrotate to rotate.
   * @return filenames to rotate.
   */
  public String[] getExtrasFiles() {
    final String[] original = configuration.getLogrotateExtrasFiles();
    final String[] transformed = new String[original.length];

    for (int i = 0; i < original.length; i++) {
      transformed[i] = taskDefinition.getTaskDirectoryPath().resolve(original[i]).toString();
    }

    return transformed;
  }

  /**
   * dateformat for extra files.
   * Only %Y %m %d and %s specifiers are allowed.
   * @return dateformat (e.g. "-%Y%m%d%s").
   */
  public String getExtrasDateformat() {
    return configuration.getLogrotateExtrasDateformat();
  }

  /**
   * Default log to logrotate, defaults to service.log.
   * This if this log doesn't exist, logrotate will return an error message.
   * @return filename to rotate.
   */
  public String getLogfile() {
    return taskDefinition.getServiceLogOut();
  }

  @Override
  public String toString() {
    return "LogrotateTemplateContext [taskId=" + taskDefinition.getTaskId() + "]";
  }


}
