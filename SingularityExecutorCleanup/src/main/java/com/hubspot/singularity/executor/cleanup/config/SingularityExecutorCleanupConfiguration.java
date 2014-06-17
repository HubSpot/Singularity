package com.hubspot.singularity.executor.cleanup.config;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityExecutorCleanupConfiguration {

  private final boolean safeModeWontRunWithNoTasks;
  
  @Inject
  public SingularityExecutorCleanupConfiguration(
      @Named(SingularityExecutorCleanupConfigurationLoader.SAFE_MODE_WONT_RUN_WITH_NO_TASKS) String safeModeWontRunWithNoTasks
      ) {
    this.safeModeWontRunWithNoTasks = Boolean.parseBoolean(safeModeWontRunWithNoTasks);
  }

  public boolean isSafeModeWontRunWithNoTasks() {
    return safeModeWontRunWithNoTasks;
  }

  @Override
  public String toString() {
    return "SingularityExecutorCleanupConfiguration [safeModeWontRunWithNoTasks=" + safeModeWontRunWithNoTasks + "]";
  }

}
