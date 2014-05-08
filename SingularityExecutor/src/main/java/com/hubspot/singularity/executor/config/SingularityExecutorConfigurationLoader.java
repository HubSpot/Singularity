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
  
  public static final String TASK_APP_DIRECTORY = "executor.task.app.directory";

  public static final String TASK_EXECUTOR_JAVA_LOG_PATH = "executor.task.java.log.path";
  public static final String TASK_EXECUTOR_BASH_LOG_PATH = "executor.task.bash.log.path";
  public static final String TASK_SERVICE_LOG_PATH = "executor.task.service.log.path";
  
  public static final String DEFAULT_USER = "executor.default.user";
  
  public static final String ARTIFACT_CACHE_DIRECTORY = "executor.artifact.cache.directory";

  public static final String LOGROTATE_COMMAND = "executor.logrotate.command";
  public static final String LOGROTATE_CONFIG_DIRECTORY = "executor.logrotate.config.folder";
  public static final String LOGROTATE_AFTER_BYTES = "executor.logrotate.after.bytes";
  public static final String LOGROTATE_DIRECTORY = "executor.logrotate.to.directory";
  public static final String LOGROTATE_MAXAGE_DAYS = "executor.logrotate.maxage.days";
  public static final String LOGROTATE_COUNT = "executor.logrotate.count";
  public static final String LOGROTATE_DATEFORMAT = "executor.logrotate.dateformat";
  
  public static final String S3_UPLOADER_PATTERN = "executor.s3.uploader.pattern";
  public static final String S3_UPLOADER_BUCKET = "executor.s3.uploader.bucket";
  
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
  
    properties.put(LOGROTATE_COMMAND, "logrotate");
    properties.put(LOGROTATE_DIRECTORY, "logs");
    properties.put(LOGROTATE_MAXAGE_DAYS, "7");
    properties.put(LOGROTATE_COUNT, "20");
    properties.put(LOGROTATE_DATEFORMAT, "-%s");
    properties.put(LOGROTATE_CONFIG_DIRECTORY, "/etc/logrotate.d");
  }
  
}
