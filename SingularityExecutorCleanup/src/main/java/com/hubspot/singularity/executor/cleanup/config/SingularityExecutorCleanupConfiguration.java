package com.hubspot.singularity.executor.cleanup.config;

import org.hibernate.validator.constraints.NotEmpty;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.validation.constraints.Min;

import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.configuration.BaseRunnerConfiguration;
import com.hubspot.singularity.runner.base.configuration.Configuration;
import com.hubspot.singularity.runner.base.constraints.DirectoryExists;

@Configuration("/etc/singularity.executor.cleanup.yaml")
public class SingularityExecutorCleanupConfiguration extends BaseRunnerConfiguration {
  public static final String SAFE_MODE_WONT_RUN_WITH_NO_TASKS = "executor.cleanup.safe.mode.wont.run.with.no.tasks";
  public static final String EXECUTOR_CLEANUP_CLEANUP_APP_DIRECTORY_OF_FAILED_TASKS_AFTER_MILLIS = "executor.cleanup.cleanup.app.directory.of.failed.tasks.after.millis";
  public static final String EXECUTOR_CLEANUP_RESULTS_DIRECTORY = "executor.cleanup.results.directory";
  public static final String EXECUTOR_CLEANUP_RESULTS_SUFFIX = "executor.cleanup.results.suffix";

  private boolean safeModeWontRunWithNoTasks = true;

  @DirectoryExists
  private String executorCleanupResultsDirectory;

  @NotEmpty
  private String executorCleanupResultsSuffix = ".cleanup.json";

  @Min(1)
  private long cleanupAppDirectoryOfFailedTasksAfterMillis = TimeUnit.DAYS.toMillis(1);

  public SingularityExecutorCleanupConfiguration() {
    super(Optional.of("singularity-executor-cleanup.log"));
  }

  public boolean isSafeModeWontRunWithNoTasks() {
    return safeModeWontRunWithNoTasks;
  }

  public void setSafeModeWontRunWithNoTasks(boolean safeModeWontRunWithNoTasks) {
    this.safeModeWontRunWithNoTasks = safeModeWontRunWithNoTasks;
  }

  public Path getExecutorCleanupResultsDirectory() {
    return Paths.get(executorCleanupResultsDirectory);
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

  @Override
  public String toString() {
    return "SingularityExecutorCleanupConfiguration [safeModeWontRunWithNoTasks=" + safeModeWontRunWithNoTasks + ", executorCleanupResultsDirectory=" + executorCleanupResultsDirectory
        + ", executorCleanupResultsSuffix=" + executorCleanupResultsSuffix + ", cleanupAppDirectoryOfFailedTasksAfterMillis=" + cleanupAppDirectoryOfFailedTasksAfterMillis + "]";
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
  }
}
