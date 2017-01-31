package com.hubspot.singularity.executor.task;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.singularity.SingularityTaskExecutorData;

public class SingularityExecutorTaskDefinition {

  private final SingularityTaskExecutorData executorData;
  private final String taskId;
  private final Path taskDirectoryPath;
  private final String executorBashOut;
  private final String serviceLogOut;
  private final String serviceLogOutExtension;
  private final String serviceFinishedTailLog;
  private final String taskAppDirectory;
  private final String logrotateStateFile;
  private final String executorPid;
  private final String signatureVerifyOut;

  @JsonCreator
  public SingularityExecutorTaskDefinition(@JsonProperty("taskId") String taskId, @JsonProperty("executorData") SingularityTaskExecutorData executorData, @JsonProperty("taskDirectory") String taskDirectory, @JsonProperty("executorPid") String executorPid,
      @JsonProperty("serviceLogOut") String serviceLogOut, @JsonProperty("serviceLogOutExtension") String serviceLogOutExtension, @JsonProperty("serviceFinishedTailLog") String serviceFinishedTailLog, @JsonProperty("taskAppDirectory") String taskAppDirectory, @JsonProperty("executorBashOut") String executorBashOut, @JsonProperty("logrotateStateFilePath") String logrotateStateFile, @JsonProperty("signatureVerifyOut") String signatureVerifyOut) {
    this.executorData = executorData;
    this.taskId = taskId;
    this.taskDirectoryPath = Paths.get(taskDirectory);
    this.executorPid = executorPid;
    this.executorBashOut = executorBashOut;
    this.serviceLogOut = serviceLogOut;
    this.serviceLogOutExtension = serviceLogOutExtension;
    this.serviceFinishedTailLog = serviceFinishedTailLog;
    this.taskAppDirectory = taskAppDirectory;
    this.logrotateStateFile = logrotateStateFile;
    this.signatureVerifyOut = signatureVerifyOut;
  }

  @JsonIgnore
  public Path getTaskDirectoryPath() {
    return taskDirectoryPath;
  }

  @JsonIgnore
  public Path getExecutorBashOutPath() {
    return taskDirectoryPath.resolve(executorBashOut);
  }

  @JsonIgnore
  public Path getServiceLogOutPath() {
    return taskDirectoryPath.resolve(serviceLogOut);
  }

  @JsonIgnore
  public Path getTaskAppDirectoryPath() {
    return taskDirectoryPath.resolve(taskAppDirectory);
  }

  @JsonIgnore
  public Path getLogrotateStateFilePath() {
    return taskDirectoryPath.resolve(logrotateStateFile);
  }

  @JsonIgnore
  /**
   * Convenience method for handling skipLogrotateAndCompress
   */
  public boolean shouldLogrotateLogFile() {
    return !executorData.getSkipLogrotateAndCompress().or(Boolean.FALSE).booleanValue();
  }

  @JsonIgnore
  public Path getSignatureVerifyOutPath() { return taskDirectoryPath.resolve(signatureVerifyOut); }

  public String getTaskDirectory() {
    return taskDirectoryPath.toString();
  }

  public String getExecutorBashOut() {
    return getExecutorBashOutPath().toString();
  }

  public String getServiceLogOut() {
    return getServiceLogOutPath().toString();
  }

  @JsonIgnore
  public String getServiceLogFileName() {
    return serviceLogOut;
  }

  @JsonIgnore
  public String getServiceFinishedTailLogFileName() {
    return serviceFinishedTailLog;
  }

  public String getServiceLogOutExtension() {
    return serviceLogOutExtension;
  }

  public String getServiceFinishedTailLog() {
    return getServiceFinishedTailLogPath().toString();
  }

  @JsonIgnore
  public Path getServiceFinishedTailLogPath() {
    return taskDirectoryPath.resolve(serviceFinishedTailLog);
  }

  public String getTaskAppDirectory() {
    return getTaskAppDirectoryPath().toString();
  }

  public String getLogrotateStateFile() {
    return getLogrotateStateFilePath().toString();
  }

  public SingularityTaskExecutorData getExecutorData() {
    return executorData;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getExecutorPid() {
    return executorPid;
  }

  public String getSignatureVerifyOut() {
    return signatureVerifyOut;
  }

  @JsonIgnore
  public Optional<Integer> getExecutorPidSafe() {
    try {
      return Optional.of(Integer.parseInt(executorPid));
    } catch (NumberFormatException nfe) {
      return Optional.<Integer> absent();
    }
  }

  @Override
  public String toString() {
    return "SingularityExecutorTaskDefinition{" +
        "executorData=" + executorData +
        ", taskId='" + taskId + '\'' +
        ", taskDirectoryPath=" + taskDirectoryPath +
        ", executorBashOut='" + executorBashOut + '\'' +
        ", serviceLogOut='" + serviceLogOut + '\'' +
        ", serviceLogOutExtension='" + serviceLogOutExtension + '\'' +
        ", serviceFinishedTailLog='" + serviceFinishedTailLog + '\'' +
        ", taskAppDirectory='" + taskAppDirectory + '\'' +
        ", logrotateStateFile='" + logrotateStateFile + '\'' +
        ", executorPid='" + executorPid + '\'' +
        ", signatureVerifyOut='" + signatureVerifyOut + '\'' +
        '}';
  }
}
