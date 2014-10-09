package com.hubspot.singularity.executor.cleanup.config;

import java.nio.file.Path;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;

@Singleton
public class SingularityExecutorCleanupConfiguration {

  private final boolean safeModeWontRunWithNoTasks;
  private final Path executorCleanupResultsDirectory;
  private final String executorCleanupResultsSuffix;
  private final long cleanupAppDirectoryOfFailedTasksAfterMillis;

  @Inject
  public SingularityExecutorCleanupConfiguration(
      @Named(SingularityExecutorCleanupConfigurationLoader.EXECUTOR_CLEANUP_CLEANUP_APP_DIRECTORY_OF_FAILED_TASKS_AFTER_MILLIS) String cleanupAppDirectoryOfFailedTasksAfterMillis,
      @Named(SingularityExecutorCleanupConfigurationLoader.SAFE_MODE_WONT_RUN_WITH_NO_TASKS) String safeModeWontRunWithNoTasks,
      @Named(SingularityExecutorCleanupConfigurationLoader.EXECUTOR_CLEANUP_RESULTS_DIRECTORY) String executorCleanupResultsDirectory,
      @Named(SingularityExecutorCleanupConfigurationLoader.EXECUTOR_CLEANUP_RESULTS_SUFFIX) String executorCleanupResultsSuffix) {
    this.safeModeWontRunWithNoTasks = Boolean.parseBoolean(safeModeWontRunWithNoTasks);
    this.executorCleanupResultsDirectory = JavaUtils.getValidDirectory(executorCleanupResultsDirectory, SingularityExecutorCleanupConfigurationLoader.EXECUTOR_CLEANUP_RESULTS_DIRECTORY);
    this.executorCleanupResultsSuffix = executorCleanupResultsSuffix;
    this.cleanupAppDirectoryOfFailedTasksAfterMillis = Long.parseLong(cleanupAppDirectoryOfFailedTasksAfterMillis);
  }

  public long getCleanupAppDirectoryOfFailedTasksAfterMillis() {
    return cleanupAppDirectoryOfFailedTasksAfterMillis;
  }

  public boolean isSafeModeWontRunWithNoTasks() {
    return safeModeWontRunWithNoTasks;
  }

  public Path getExecutorCleanupResultsDirectory() {
    return executorCleanupResultsDirectory;
  }

  public String getExecutorCleanupResultsSuffix() {
    return executorCleanupResultsSuffix;
  }

  @Override
  public String toString() {
    return "SingularityExecutorCleanupConfiguration [safeModeWontRunWithNoTasks=" + safeModeWontRunWithNoTasks + ", executorCleanupResultsDirectory=" + executorCleanupResultsDirectory
        + ", executorCleanupResultsSuffix=" + executorCleanupResultsSuffix + ", cleanupAppDirectoryOfFailedTasksAfterMillis=" + cleanupAppDirectoryOfFailedTasksAfterMillis + "]";
  }


}
