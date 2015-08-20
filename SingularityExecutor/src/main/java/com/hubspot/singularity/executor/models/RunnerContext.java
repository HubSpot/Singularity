package com.hubspot.singularity.executor.models;

import com.google.common.base.Optional;

/**
 * Handlebars context for generating the runner.sh file.
 */
public class RunnerContext {

  private final String cmd;
  private final String taskAppDirectory;
  private final String logDir;
  private final String logFile;
  private final String taskId;

  private final Optional<Integer> maxTaskThreads;

  public RunnerContext(String cmd, String taskAppDirectory, String logDir, String logFile, String taskId, Optional<Integer> maxTaskThreads) {
    this.cmd = cmd;
    this.taskAppDirectory = taskAppDirectory;
    this.logDir = logDir;
    this.logFile = logFile;
    this.taskId = taskId;

    this.maxTaskThreads = maxTaskThreads;
  }

  public String getCmd() {
    return cmd;
  }

  public String getTaskAppDirectory() {
    return taskAppDirectory;
  }

  public String getLogDir() {
    return logDir;
  }

  public String getLogFile() {
    return logFile;
  }

  public String getTaskId() {
    return taskId;
  }

  public Optional<Integer> getMaxTaskThreads() {
    return maxTaskThreads;
  }

  @Override
  public String toString() {
    return "RunnerContext[" +
            "cmd='" + cmd + '\'' +
            ", taskAppDirectory='" + taskAppDirectory + '\'' +
            ", logDir='" + logDir + '\'' +
            ", logFile='" + logFile + '\'' +
            ", taskId='" + taskId + '\'' +
            ", maxTaskThreads=" + maxTaskThreads +
            ']';
  }
}
