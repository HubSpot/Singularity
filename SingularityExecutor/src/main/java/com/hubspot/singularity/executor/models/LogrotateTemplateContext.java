package com.hubspot.singularity.executor.models;

import java.util.ArrayList;
import java.util.List;

import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
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

  /**
   * Extra files for logrotate to rotate. If these do not exist logrotate will continue without error.
   * @return filenames to rotate.
   */
  public List<String> getExtrasFiles() {
    final List<String> original = configuration.getLogrotateAdditionalFiles();
    final List<String> transformed = new ArrayList<>(original.size());

    for (String filename : original) {
      transformed.add(taskDefinition.getTaskDirectoryPath().resolve(filename).toString());
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

  @Override
  public String toString() {
    return "LogrotateTemplateContext [taskId=" + taskDefinition.getTaskId() + "]";
  }


}
