package com.hubspot.singularity.executor.models;

import com.hubspot.singularity.executor.SingularityExecutorLogrotateFrequency;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskDefinition;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LogrotateCronTemplateContext {

  private final String logrotateStateFile;
  private final String logrotateSizeBasedConfig;
  Map<SingularityExecutorLogrotateFrequency, String> logrotateConfPathsByLogrotateFrequency;
  private final SingularityExecutorConfiguration configuration;

  public LogrotateCronTemplateContext(
    SingularityExecutorConfiguration configuration,
    SingularityExecutorTaskDefinition taskDefinition,
    Map<SingularityExecutorLogrotateFrequency, String> logrotateConfPathsByLogrotateFrequency,
    String logrotateSizeBasedConfig
  ) {
    this.configuration = configuration;
    this.logrotateStateFile = taskDefinition.getLogrotateStateFilePath().toString();
    this.logrotateConfPathsByLogrotateFrequency = logrotateConfPathsByLogrotateFrequency;
    this.logrotateSizeBasedConfig = logrotateSizeBasedConfig;
  }

  public List<LogrotateForceConfig> getLogrotateForceConfigs() {
    return logrotateConfPathsByLogrotateFrequency
      .entrySet()
      .stream()
      .map(frequencyWithLogrotateConfPath -> {
        SingularityExecutorLogrotateFrequency frequency = frequencyWithLogrotateConfPath.getKey();
        String frequencySpecificLogrotateConfPath = frequencyWithLogrotateConfPath.getValue();

        return new LogrotateForceConfig(
          frequencySpecificLogrotateConfPath,
          frequency.getCronSchedule().get()
        );
      })
      .collect(Collectors.toList());
  }

  public static class LogrotateForceConfig {

    private final String logrotateForceConfigPath;
    private final String cronSchedule;

    public LogrotateForceConfig(String logrotateForceConfigPath, String cronSchedule) {
      this.logrotateForceConfigPath = logrotateForceConfigPath;
      this.cronSchedule = cronSchedule;
    }

    public String getLogrotateForceConfigPath() {
      return logrotateForceConfigPath;
    }

    public String getCronSchedule() {
      return cronSchedule;
    }
  }

  public String getLogrotateStateFile() {
    return logrotateStateFile;
  }

  public String getLogrotateSizeBasedConfig() {
    return logrotateSizeBasedConfig;
  }

  public String getOutputRedirect() {
    return configuration.isIgnoreLogrotateOutput() ? "> /dev/null 2>&1" : "";
  }

  public String getLogrotateCommand() {
    return configuration.getLogrotateCommand();
  }
}
