package com.hubspot.singularity.executor.task;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.hubspot.deploy.ExecutorData;

public class SingularityExecutorTaskDefinition {

  private final ExecutorData executorData;
  private final String taskId;
  private final Path taskDirectoryPath;
  private final String executorBashOut;
  private final String serviceLogOut;
  private final String taskAppDirectory;
  private final String logrotateStateFile;
  private final String executorPid;

  @JsonCreator
  public SingularityExecutorTaskDefinition(@JsonProperty("taskId") String taskId, @JsonProperty("executorData") ExecutorData executorData, @JsonProperty("taskDirectory") String taskDirectory, @JsonProperty("executorPid") String executorPid,
      @JsonProperty("serviceLogOut") String serviceLogOut, @JsonProperty("taskAppDirectory") String taskAppDirectory, @JsonProperty("executorBashOut") String executorBashOut, @JsonProperty("logrotateStateFilePath") String logrotateStateFile) {
    this.executorData = executorData;
    this.taskId = taskId;
    this.taskDirectoryPath = Paths.get(taskDirectory);

    this.executorPid = executorPid;
    this.executorBashOut = executorBashOut;
    this.serviceLogOut = serviceLogOut;
    this.taskAppDirectory = taskAppDirectory;
    this.logrotateStateFile = logrotateStateFile;
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

  public String getTaskDirectory() {
    return taskDirectoryPath.toString();
  }

  public String getExecutorBashOut() {
    return getExecutorBashOutPath().toString();
  }

  public String getServiceLogOut() {
    return getServiceLogOutPath().toString();
  }

  public String getTaskAppDirectory() {
    return getTaskAppDirectoryPath().toString();
  }

  public String getLogrotateStateFile() {
    return getLogrotateStateFilePath().toString();
  }

  public ExecutorData getExecutorData() {
    return executorData;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getExecutorPid() {
    return executorPid;
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
    return "SingularityExecutorTaskDefinition [executorData=" + executorData + ", taskId=" + taskId + ", taskDirectoryPath=" + taskDirectoryPath + ", executorBashOut=" + executorBashOut
        + ", serviceLogOut=" + serviceLogOut + ", taskAppDirectory=" + taskAppDirectory + ", logrotateStateFile=" + logrotateStateFile + ", executorPid=" + executorPid + "]";
  }

}
