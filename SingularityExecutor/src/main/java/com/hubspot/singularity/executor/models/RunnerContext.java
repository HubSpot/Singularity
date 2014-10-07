package com.hubspot.singularity.executor.models;

import com.google.common.base.Optional;

public class RunnerContext {

  private final String cmd;
  private final String user;
  private final String logfile;
  private final String taskId;
  private final String taskAppDirectory;
  private final Optional<Integer> maxTaskThreads;

  public RunnerContext(String cmd, String taskAppDirectory, String user, String logfile, String taskId, Optional<Integer> maxTaskThreads) {
    this.cmd = cmd;
    this.user = user;
    this.logfile = logfile;
    this.taskId = taskId;
    this.taskAppDirectory = taskAppDirectory;
    this.maxTaskThreads = maxTaskThreads;
  }

  public String getTaskId() {
    return taskId;
  }

  public String getCmd() {
    return cmd;
  }

  public String getTaskAppDirectory() {
    return taskAppDirectory;
  }

  public String getUser() {
    return user;
  }

  public String getLogfile() {
    return logfile;
  }

  public Optional<Integer> getMaxTaskThreads() {
    return maxTaskThreads;
  }

  @Override
  public String toString() {
    return "RunnerContext [" +
        "cmd='" + cmd + '\'' +
        ", user='" + user + '\'' +
        ", logfile='" + logfile + '\'' +
        ", taskId='" + taskId + '\'' +
        ", taskAppDirectory='" + taskAppDirectory + '\'' +
        ", maxTaskThreads=" + maxTaskThreads +
        ']';
  }
}
