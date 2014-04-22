package com.hubspot.singularity.executor.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class SingularityExecutorConfiguration {

  private final String executorJavaLog;
  private final String executorBashLog;
  private final String serviceLog;
  private final String defaultRunAsUser;
  private final String cacheDirectory;
  private final String taskAppDirectory;
  private final long shutdownTimeoutWaitMillis;
  private final long idleExecutorShutdownWaitMillis;
  private final long stopDriverAfterMillis;
  
  @Inject
  public SingularityExecutorConfiguration(@Named(SingularityExecutorModule.ARTIFACT_CACHE_DIRECTORY) String cacheDirectory, @Named(SingularityExecutorModule.TASK_APP_DIRECTORY) String taskAppDirectory,
      @Named(SingularityExecutorModule.TASK_EXECUTOR_BASH_LOG_PATH) String executorBashLog, @Named(SingularityExecutorModule.TASK_EXECUTOR_JAVA_LOG_PATH) String executorJavaLog, 
      @Named(SingularityExecutorModule.TASK_SERVICE_LOG_PATH) String serviceLog, @Named(SingularityExecutorModule.DEFAULT_USER) String defaultRunAsUser, @Named(SingularityExecutorModule.SHUTDOWN_STOP_DRIVER_AFTER_MILLIS) String stopDriverAfterMillis,
      @Named(SingularityExecutorModule.SHUTDOWN_TIMEOUT_MILLIS) String shutdownTimeoutWaitMillis, @Named(SingularityExecutorModule.IDLE_EXECUTOR_SHUTDOWN_AFTER_MILLIS) String idleExecutorShutdownWaitMillis) {
    this.executorBashLog = executorBashLog;
    this.taskAppDirectory = taskAppDirectory;
    this.executorJavaLog = executorJavaLog;
    this.cacheDirectory = cacheDirectory;
    this.serviceLog = serviceLog;
    this.defaultRunAsUser = defaultRunAsUser;
    this.shutdownTimeoutWaitMillis = Long.parseLong(shutdownTimeoutWaitMillis);
    this.idleExecutorShutdownWaitMillis = Long.parseLong(idleExecutorShutdownWaitMillis);
    this.stopDriverAfterMillis = Long.parseLong(stopDriverAfterMillis);
  }

  public long getStopDriverAfterMillis() {
    return stopDriverAfterMillis;
  }

  public long getIdleExecutorShutdownWaitMillis() {
    return idleExecutorShutdownWaitMillis;
  }

  public long getShutdownTimeoutWaitMillis() {
    return shutdownTimeoutWaitMillis;
  }

  public String getExecutorJavaLog() {
    return executorJavaLog;
  }

  public String getExecutorBashLog() {
    return executorBashLog;
  }

  public String getServiceLog() {
    return serviceLog;
  }

  public String getDefaultRunAsUser() {
    return defaultRunAsUser;
  }
  
  public String getTaskAppDirectory() {
    return taskAppDirectory;
  }

  public String getCacheDirectory() {
    return cacheDirectory;
  }
  
  public Path getTaskDirectoryPath(String taskId) {
    return Paths.get(taskId);
  }
  
  public Path getTaskAppDirectoryPath(String taskId) {
    return getTaskDirectoryPath(taskId).resolve(taskAppDirectory);
  }
  
  public Path getExecutorBashLogPath(String taskId) { 
    return getTaskDirectoryPath(taskId).resolve(getExecutorBashLog());
  }
  
  public Path getExecutorJavaLogPath(String taskId) { 
    return getTaskDirectoryPath(taskId).resolve(getExecutorJavaLog());
  }

  @Override
  public String toString() {
    return "SingularityExecutorConfiguration [executorJavaLog=" + executorJavaLog + ", executorBashLog=" + executorBashLog + ", serviceLog=" + serviceLog + ", defaultRunAsUser=" + defaultRunAsUser + ", cacheDirectory=" + cacheDirectory
        + ", taskAppDirectory=" + taskAppDirectory + ", shutdownTimeoutWaitMillis=" + shutdownTimeoutWaitMillis + ", idleExecutorShutdownWaitMillis=" + idleExecutorShutdownWaitMillis + ", stopDriverAfterMillis=" + stopDriverAfterMillis
        + "]";
  }
 
}
