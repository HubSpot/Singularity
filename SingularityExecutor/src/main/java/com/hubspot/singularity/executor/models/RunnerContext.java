package com.hubspot.singularity.executor.models;

import com.google.common.base.Optional;

/**
 * Handlebars context for generating the runner.sh file.
 */
public class RunnerContext {

  private final String cmd;
  private final String taskAppDirectory;
  private final String logDir;
  private final String user;
  private final String logFile;
  private final String taskId;

  private final Optional<Integer> maxTaskThreads;
  private final boolean shouldChangeUser;

  public RunnerContext(String cmd, String taskAppDirectory, String logDir, String user, String logFile, String taskId, Optional<Integer> maxTaskThreads, boolean shouldChangeUser) {
    this.cmd = cmd;
    this.taskAppDirectory = taskAppDirectory;
    this.logDir = logDir;
    this.user = user;
    this.logFile = logFile;
    this.taskId = taskId;

    this.maxTaskThreads = maxTaskThreads;
    this.shouldChangeUser = shouldChangeUser;
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

  public String getUser() {
    return user;
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

  public boolean isShouldChangeUser() {
    return shouldChangeUser;
  }

  @Override
  public String toString() {
    return "RunnerContext[" +
            "cmd='" + cmd + '\'' +
            ", taskAppDirectory='" + taskAppDirectory + '\'' +
            ", logDir='" + logDir + '\'' +
            ", user='" + user + '\'' +
            ", logFile='" + logFile + '\'' +
            ", taskId='" + taskId + '\'' +
            ", maxTaskThreads=" + maxTaskThreads +
            ", shouldChangeUser=" + shouldChangeUser +
            ']';
  }
}
