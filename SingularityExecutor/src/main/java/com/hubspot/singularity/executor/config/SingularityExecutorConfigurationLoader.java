package com.hubspot.singularity.executor.config;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import com.hubspot.singularity.runner.base.config.SingularityRunnerBaseConfigurationLoader;

public class SingularityExecutorConfigurationLoader extends SingularityRunnerBaseConfigurationLoader {
  
  public static final String SHUTDOWN_TIMEOUT_MILLIS = "executor.shutdown.timeout.millis";
  
  public static final String HARD_KILL_AFTER_MILLIS = "executor.hard.kill.after.millis";
  public static final String NUM_CORE_KILL_THREADS = "executor.num.core.kill.threads";
  
  public static final String MAX_TASK_MESSAGE_LENGTH = "executor.status.update.max.task.message.length";
  
  public static final String IDLE_EXECUTOR_SHUTDOWN_AFTER_MILLIS = "executor.idle.shutdown.after.millis";
  public static final String SHUTDOWN_STOP_DRIVER_AFTER_MILLIS = "executor.shutdown.stop.driver.after.millis";
  
  public static final String TASK_APP_DIRECTORY = "task.executor.app.directory";

  public static final String TASK_EXECUTOR_JAVA_LOG_PATH = "task.executor.java.log.path";
  public static final String TASK_EXECUTOR_BASH_LOG_PATH = "task.executor.bash.log.path";
  public static final String TASK_SERVICE_LOG_PATH = "task.service.log.path";
  
  public static final String DEFAULT_USER = "default.user";
  
  public static final String ARTIFACT_CACHE_DIRECTORY = "executor.artifact.cache.directory";

  @Override
  protected void bindDefaults(Properties properties) {
    super.bindDefaults(properties);

    properties.put(TASK_APP_DIRECTORY, "app");
    properties.put(TASK_EXECUTOR_BASH_LOG_PATH, "executor.bash.log");
    properties.put(TASK_EXECUTOR_JAVA_LOG_PATH, "executor.java.log");
    properties.put(TASK_SERVICE_LOG_PATH, "service.log");
    properties.put(HARD_KILL_AFTER_MILLIS, Long.toString(TimeUnit.MINUTES.toMillis(3)));
    properties.put(NUM_CORE_KILL_THREADS, "1");
    properties.put(MAX_TASK_MESSAGE_LENGTH, "80");
    properties.put(SHUTDOWN_TIMEOUT_MILLIS, Long.toString(TimeUnit.MINUTES.toMillis(5)));
    properties.put(IDLE_EXECUTOR_SHUTDOWN_AFTER_MILLIS, Long.toString(TimeUnit.SECONDS.toMillis(30)));
    properties.put(SHUTDOWN_STOP_DRIVER_AFTER_MILLIS, Long.toString(TimeUnit.SECONDS.toMillis(5)));
  }
  
}
