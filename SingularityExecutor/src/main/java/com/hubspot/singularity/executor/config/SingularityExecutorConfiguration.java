package com.hubspot.singularity.executor.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;

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
  
  private final long hardKillAfterMillis;
  private final int killThreads;

  private final int maxTaskMessageLength;
  
  private final String logrotateCommand;
  private final Path logrotateConfDirectory;
  private final String logrotateToDirectory;
  private final String logrotateMaxageDays;
  private final String logrotateCount; 
  private final String logrotateDateformat;

  private final Path logMetadataDirectory;
  private final String logMetadataSuffix;
  
  private final int tailLogLinesToSave;
  private final String serviceFinishedTailLog;
  
  private final String s3MetadataSuffix;
  private final Path s3MetadataDirectory;
  
  private final String s3KeyPattern;
  private final String s3Bucket;
  
  @Inject
  public SingularityExecutorConfiguration(
      @Named(SingularityExecutorConfigurationLoader.ARTIFACT_CACHE_DIRECTORY) String cacheDirectory, 
      @Named(SingularityExecutorConfigurationLoader.TASK_APP_DIRECTORY) String taskAppDirectory,
      @Named(SingularityExecutorConfigurationLoader.TASK_EXECUTOR_BASH_LOG_PATH) String executorBashLog, 
      @Named(SingularityExecutorConfigurationLoader.TASK_EXECUTOR_JAVA_LOG_PATH) String executorJavaLog, 
      @Named(SingularityExecutorConfigurationLoader.TASK_SERVICE_LOG_PATH) String serviceLog, 
      @Named(SingularityExecutorConfigurationLoader.DEFAULT_USER) String defaultRunAsUser, 
      @Named(SingularityExecutorConfigurationLoader.SHUTDOWN_STOP_DRIVER_AFTER_MILLIS) String stopDriverAfterMillis, 
      @Named(SingularityExecutorConfigurationLoader.SHUTDOWN_TIMEOUT_MILLIS) String shutdownTimeoutWaitMillis, 
      @Named(SingularityExecutorConfigurationLoader.IDLE_EXECUTOR_SHUTDOWN_AFTER_MILLIS) String idleExecutorShutdownWaitMillis, 
      @Named(SingularityExecutorConfigurationLoader.HARD_KILL_AFTER_MILLIS) String hardKillAfterMillis,
      @Named(SingularityExecutorConfigurationLoader.NUM_CORE_KILL_THREADS) String killThreads, 
      @Named(SingularityExecutorConfigurationLoader.MAX_TASK_MESSAGE_LENGTH) String maxTaskMessageLength,
      @Named(SingularityExecutorConfigurationLoader.LOG_METADATA_DIRECTORY) String logMetadataDirectory,
      @Named(SingularityExecutorConfigurationLoader.LOG_METADATA_SUFFIX) String logMetadataSuffix,
      @Named(SingularityExecutorConfigurationLoader.S3_UPLOADER_BUCKET) String s3Bucket,
      @Named(SingularityExecutorConfigurationLoader.S3_UPLOADER_PATTERN) String s3KeyPattern,
      @Named(SingularityExecutorConfigurationLoader.S3_METADATA_DIRECTORY) String s3MetadataDirectory,
      @Named(SingularityExecutorConfigurationLoader.S3_METADATA_SUFFIX) String s3MetadataSuffix,
      @Named(SingularityExecutorConfigurationLoader.LOGROTATE_COMMAND) String logrotateCommand, 
      @Named(SingularityExecutorConfigurationLoader.LOGROTATE_COUNT) String logrotateCount, 
      @Named(SingularityExecutorConfigurationLoader.LOGROTATE_MAXAGE_DAYS) String logrotateMaxageDays, 
      @Named(SingularityExecutorConfigurationLoader.LOGROTATE_DATEFORMAT) String logrotateDateformat, 
      @Named(SingularityExecutorConfigurationLoader.LOGROTATE_DIRECTORY) String logrotateToDirectory,
      @Named(SingularityExecutorConfigurationLoader.LOGROTATE_CONFIG_DIRECTORY) String logrotateConfDirectory,
      @Named(SingularityExecutorConfigurationLoader.TAIL_LOG_LINES_TO_SAVE) String tailLogLinesToSave,
      @Named(SingularityExecutorConfigurationLoader.TAIL_LOG_FILENAME) String serviceFinishedTailLog
      ) {
    this.executorBashLog = executorBashLog;
    this.taskAppDirectory = taskAppDirectory;
    this.executorJavaLog = executorJavaLog;
    this.cacheDirectory = cacheDirectory;
    this.serviceLog = serviceLog;
    this.defaultRunAsUser = defaultRunAsUser;
    this.shutdownTimeoutWaitMillis = Long.parseLong(shutdownTimeoutWaitMillis);
    this.idleExecutorShutdownWaitMillis = Long.parseLong(idleExecutorShutdownWaitMillis);
    this.stopDriverAfterMillis = Long.parseLong(stopDriverAfterMillis);
    this.hardKillAfterMillis = Long.parseLong(hardKillAfterMillis);
    this.killThreads = Integer.parseInt(killThreads);
    this.maxTaskMessageLength = Integer.parseInt(maxTaskMessageLength); 
    this.logMetadataDirectory = JavaUtils.getValidDirectory(logMetadataDirectory, SingularityExecutorConfigurationLoader.LOG_METADATA_DIRECTORY);
    this.logMetadataSuffix = logMetadataSuffix;
    this.logrotateCommand = logrotateCommand;
    this.logrotateConfDirectory = JavaUtils.getValidDirectory(logrotateConfDirectory, SingularityExecutorConfigurationLoader.LOGROTATE_CONFIG_DIRECTORY);
    this.logrotateToDirectory = logrotateToDirectory;
    this.logrotateCount = logrotateCount;
    this.logrotateMaxageDays = logrotateMaxageDays;
    this.logrotateDateformat = logrotateDateformat;
    this.s3Bucket = s3Bucket;
    this.s3KeyPattern = s3KeyPattern;
    this.s3MetadataSuffix = s3MetadataSuffix;
    this.s3MetadataDirectory = JavaUtils.getValidDirectory(s3MetadataDirectory, SingularityExecutorConfigurationLoader.S3_METADATA_DIRECTORY);
    this.tailLogLinesToSave = Integer.parseInt(tailLogLinesToSave);
    this.serviceFinishedTailLog = serviceFinishedTailLog;
  }
  
  public int getTailLogLinesToSave() {
    return tailLogLinesToSave;
  }

  public Path getLogMetadataDirectory() {
    return logMetadataDirectory;
  }

  public String getLogMetadataSuffix() {
    return logMetadataSuffix;
  }

  public long getHardKillAfterMillis() {
    return hardKillAfterMillis;
  }

  public int getKillThreads() {
    return killThreads;
  }

  public int getMaxTaskMessageLength() {
    return maxTaskMessageLength;
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

  public String getServiceFinishedTailLog() {
    return serviceFinishedTailLog;
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
    return Paths.get(getSafeTaskIdForDirectory(taskId)).toAbsolutePath();
  }
  
  private String getSafeTaskIdForDirectory(String taskId) {
    return taskId.replace(":", "_");
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

  public String getLogrotateCommand() {
    return logrotateCommand;
  }

  public String getLogrotateToDirectory() {
    return logrotateToDirectory;
  }

  public String getLogrotateMaxageDays() {
    return logrotateMaxageDays;
  }

  public String getLogrotateCount() {
    return logrotateCount;
  }

  public String getLogrotateDateformat() {
    return logrotateDateformat;
  }

  public String getS3MetadataSuffix() {
    return s3MetadataSuffix;
  }

  public Path getS3MetadataDirectory() {
    return s3MetadataDirectory;
  }

  public String getS3KeyPattern() {
    return s3KeyPattern;
  }

  public String getS3Bucket() {
    return s3Bucket;
  }

  public Path getLogrotateConfDirectory() {
    return logrotateConfDirectory;
  }

  @Override
  public String toString() {
    return "SingularityExecutorConfiguration [executorJavaLog=" + executorJavaLog + ", executorBashLog=" + executorBashLog + ", serviceLog=" + serviceLog + ", defaultRunAsUser=" + defaultRunAsUser + ", cacheDirectory=" + cacheDirectory
        + ", taskAppDirectory=" + taskAppDirectory + ", shutdownTimeoutWaitMillis=" + shutdownTimeoutWaitMillis + ", idleExecutorShutdownWaitMillis=" + idleExecutorShutdownWaitMillis + ", stopDriverAfterMillis=" + stopDriverAfterMillis
        + ", hardKillAfterMillis=" + hardKillAfterMillis + ", killThreads=" + killThreads + ", maxTaskMessageLength=" + maxTaskMessageLength + ", logrotateCommand=" + logrotateCommand + ", logrotateConfDirectory=" + logrotateConfDirectory
        + ", logrotateToDirectory=" + logrotateToDirectory + ", logrotateMaxageDays=" + logrotateMaxageDays + ", logrotateCount=" + logrotateCount + ", logrotateDateformat=" + logrotateDateformat + ", logMetadataDirectory="
        + logMetadataDirectory + ", logMetadataSuffix=" + logMetadataSuffix + ", tailLogLinesToSave=" + tailLogLinesToSave + ", s3MetadataSuffix=" + s3MetadataSuffix + ", s3MetadataDirectory=" + s3MetadataDirectory + ", s3KeyPattern="
        + s3KeyPattern + ", s3Bucket=" + s3Bucket + "]";
  }

}
