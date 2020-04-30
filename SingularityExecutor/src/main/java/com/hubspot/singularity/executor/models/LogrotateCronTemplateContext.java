package com.hubspot.singularity.executor.models;

import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;
import java.nio.file.Paths;

public class LogrotateCronTemplateContext {
  private final String cronSchedule;
  private final String logrotateCommand;
  private final String logrotateStateFile;
  private final String logrotateConfig;
  private final String outputRedirect;

  public LogrotateCronTemplateContext(
    SingularityExecutorConfiguration configuration,
    SingularityExecutorTaskDefinition taskDefinition,
    SingularityExecutorLogrotateFrequency logrotateFrequency
  ) {
    this.logrotateCommand = configuration.getLogrotateCommand();
    this.logrotateStateFile = taskDefinition.getLogrotateStateFilePath().toString();
    this.logrotateConfig =
      Paths
        .get(configuration.getLogrotateHourlyConfDirectory())
        .resolve(taskDefinition.getTaskId())
        .toString();
    this.cronSchedule = logrotateFrequency.getCronSchedule().get();
    this.outputRedirect =
      configuration.isIgnoreLogrotateOutput() ? "> /dev/null 2>&1" : "";
  }

  public String getLogrotateCommand() {
    return logrotateCommand;
  }

  public String getLogrotateStateFile() {
    return logrotateStateFile;
  }

  public String getLogrotateConfig() {
    return logrotateConfig;
  }

  public String getCronSchedule() {
    return cronSchedule;
  }

  public String getOutputRedirect() {
    return outputRedirect;
  }
}
