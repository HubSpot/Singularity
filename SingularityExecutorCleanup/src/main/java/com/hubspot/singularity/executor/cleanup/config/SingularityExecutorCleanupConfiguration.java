package com.hubspot.singularity.executor.cleanup.config;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityClientCredentials;
import com.hubspot.singularity.client.SingularityClientModule;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;
import com.hubspot.singularity.runner.base.constraints.DirectoryExists;

@Configuration(filename = "/etc/singularity.executor.cleanup.yaml", consolidatedField = "executorCleanup")
public class SingularityExecutorCleanupConfiguration extends BaseRunnerConfiguration {
  public static final String SAFE_MODE_WONT_RUN_WITH_NO_TASKS = "executor.cleanup.safe.mode.wont.run.with.no.tasks";
  public static final String EXECUTOR_CLEANUP_CLEANUP_APP_DIRECTORY_OF_FAILED_TASKS_AFTER_MILLIS = "executor.cleanup.cleanup.app.directory.of.failed.tasks.after.millis";
  public static final String EXECUTOR_CLEANUP_RESULTS_DIRECTORY = "executor.cleanup.results.directory";
  public static final String EXECUTOR_CLEANUP_RESULTS_SUFFIX = "executor.cleanup.results.suffix";
  public static final String EXECUTOR_CLEANUP_RUN_DOCKER = "executor.cleanup.docker";

  @JsonProperty
  private boolean safeModeWontRunWithNoTasks = true;

  @DirectoryExists
  @JsonProperty
  private String executorCleanupResultsDirectory;

  @NotEmpty
  @JsonProperty
  private String executorCleanupResultsSuffix = ".cleanup.json";

  @Min(1)
  @JsonProperty
  private long cleanupAppDirectoryOfFailedTasksAfterMillis = TimeUnit.DAYS.toMillis(1);

  @NotEmpty
  @JsonProperty
  private List<String> singularityHosts = Collections.emptyList();

  @NotEmpty
  @JsonProperty
  private String singularityContextPath = "";

  @JsonProperty
  private boolean runDockerCleanup = false;

  @NotNull
  @JsonProperty
  private Optional<SingularityClientCredentials> singularityClientCredentials = Optional.absent();

  public SingularityExecutorCleanupConfiguration() {
    super(Optional.of("singularity-executor-cleanup.log"));
  }

  public boolean isSafeModeWontRunWithNoTasks() {
    return safeModeWontRunWithNoTasks;
  }

  public void setSafeModeWontRunWithNoTasks(boolean safeModeWontRunWithNoTasks) {
    this.safeModeWontRunWithNoTasks = safeModeWontRunWithNoTasks;
  }

  public String getExecutorCleanupResultsDirectory() {
    return executorCleanupResultsDirectory;
  }

  public void setExecutorCleanupResultsDirectory(String executorCleanupResultsDirectory) {
    this.executorCleanupResultsDirectory = executorCleanupResultsDirectory;
  }

  public String getExecutorCleanupResultsSuffix() {
    return executorCleanupResultsSuffix;
  }

  public void setExecutorCleanupResultsSuffix(String executorCleanupResultsSuffix) {
    this.executorCleanupResultsSuffix = executorCleanupResultsSuffix;
  }

  public long getCleanupAppDirectoryOfFailedTasksAfterMillis() {
    return cleanupAppDirectoryOfFailedTasksAfterMillis;
  }

  public void setCleanupAppDirectoryOfFailedTasksAfterMillis(long cleanupAppDirectoryOfFailedTasksAfterMillis) {
    this.cleanupAppDirectoryOfFailedTasksAfterMillis = cleanupAppDirectoryOfFailedTasksAfterMillis;
  }

  public List<String> getSingularityHosts() {
    return singularityHosts;
  }

  public void setSingularityHosts(List<String> singularityHosts) {
    this.singularityHosts = singularityHosts;
  }

  public String getSingularityContextPath() {
    return singularityContextPath;
  }

  public void setSingularityContextPath(String singularityContextPath) {
    this.singularityContextPath = singularityContextPath;
  }

  public boolean isRunDockerCleanup() {
    return runDockerCleanup;
  }

  public void setRunDockerCleanup(boolean runDockerCleanup) {
    this.runDockerCleanup = runDockerCleanup;
  }

  public Optional<SingularityClientCredentials> getSingularityClientCredentials() {
    return singularityClientCredentials;
  }

  public void setSingularityClientCredentials(Optional<SingularityClientCredentials> singularityClientCredentials) {
    this.singularityClientCredentials = singularityClientCredentials;
  }

  @Override
  public String toString() {
    return "SingularityExecutorCleanupConfiguration[" +
            "safeModeWontRunWithNoTasks=" + safeModeWontRunWithNoTasks +
            ", executorCleanupResultsDirectory='" + executorCleanupResultsDirectory + '\'' +
            ", executorCleanupResultsSuffix='" + executorCleanupResultsSuffix + '\'' +
            ", cleanupAppDirectoryOfFailedTasksAfterMillis=" + cleanupAppDirectoryOfFailedTasksAfterMillis +
            ", singularityHosts=" + singularityHosts +
            ", singularityContextPath='" + singularityContextPath + '\'' +
            ", runDockerCleanup=" + runDockerCleanup +
            ", singularityClientCredentials=" + singularityClientCredentials +
            ']';
  }

  @Override
  public void updateFromProperties(Properties properties) {
    if (properties.containsKey(SAFE_MODE_WONT_RUN_WITH_NO_TASKS)) {
      setSafeModeWontRunWithNoTasks(Boolean.parseBoolean(properties.getProperty(SAFE_MODE_WONT_RUN_WITH_NO_TASKS)));
    }

    if (properties.containsKey(EXECUTOR_CLEANUP_CLEANUP_APP_DIRECTORY_OF_FAILED_TASKS_AFTER_MILLIS)) {
      setCleanupAppDirectoryOfFailedTasksAfterMillis(Long.parseLong(properties.getProperty(EXECUTOR_CLEANUP_CLEANUP_APP_DIRECTORY_OF_FAILED_TASKS_AFTER_MILLIS)));
    }

    if (properties.containsKey(EXECUTOR_CLEANUP_RESULTS_DIRECTORY)) {
      setExecutorCleanupResultsDirectory(properties.getProperty(EXECUTOR_CLEANUP_RESULTS_DIRECTORY));
    }

    if (properties.containsKey(EXECUTOR_CLEANUP_RESULTS_SUFFIX)) {
      setExecutorCleanupResultsSuffix(properties.getProperty(EXECUTOR_CLEANUP_RESULTS_SUFFIX));
    }

    if (properties.containsKey(SingularityClientModule.HOSTS_PROPERTY_NAME)) {
      setSingularityHosts(JavaUtils.COMMA_SPLITTER.splitToList(properties.getProperty(SingularityClientModule.HOSTS_PROPERTY_NAME)));
    }

    if (properties.containsKey(SingularityClientModule.CONTEXT_PATH)) {
      setSingularityContextPath(properties.getProperty(SingularityClientModule.CONTEXT_PATH));
    }

    if (properties.containsKey(EXECUTOR_CLEANUP_RUN_DOCKER)) {
      setRunDockerCleanup(Boolean.parseBoolean(properties.getProperty(EXECUTOR_CLEANUP_RUN_DOCKER)));
    }
  }
}
