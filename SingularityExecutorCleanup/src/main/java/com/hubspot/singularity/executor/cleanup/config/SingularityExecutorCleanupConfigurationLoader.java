package com.hubspot.singularity.executor.cleanup.config;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;
import com.hubspot.singularity.runner.base.config.SingularityConfigurationLoader;

public class SingularityExecutorCleanupConfigurationLoader extends SingularityConfigurationLoader {

  public static final String SAFE_MODE_WONT_RUN_WITH_NO_TASKS = "executor.cleanup.safe.mode.wont.run.with.no.tasks";
  public static final String EXECUTOR_CLEANUP_CLEANUP_APP_DIRECTORY_OF_FAILED_TASKS_AFTER_MILLIS = "executor.cleanup.cleanup.app.directory.of.failed.tasks.after.millis";
  public static final String EXECUTOR_CLEANUP_RESULTS_DIRECTORY = "executor.cleanup.results.directory";
  public static final String EXECUTOR_CLEANUP_RESULTS_SUFFIX = "executor.cleanup.results.suffix";

  public SingularityExecutorCleanupConfigurationLoader() {
    super("/etc/singularity.executor.cleanup.properties", Optional.of("singularity-executor-cleanup.log"));
  }

  @Override
  protected void bindDefaults(Properties properties) {
    properties.put(SAFE_MODE_WONT_RUN_WITH_NO_TASKS, Boolean.toString(true));

    properties.put(EXECUTOR_CLEANUP_RESULTS_SUFFIX, ".cleanup.json");
    properties.put(EXECUTOR_CLEANUP_CLEANUP_APP_DIRECTORY_OF_FAILED_TASKS_AFTER_MILLIS, Long.toString(TimeUnit.DAYS.toMillis(1)));
  }

}
