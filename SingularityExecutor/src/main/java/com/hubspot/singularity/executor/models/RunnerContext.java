package com.hubspot.singularity.executor.models;

public class RunnerContext {

  private final String cmd;
  private final String user;
  private final String logfile;
  private final String taskId;
  private final String taskAppDirectory;
  
  public RunnerContext(String cmd, String taskAppDirectory, String user, String logfile, String taskId) {
    this.cmd = cmd;
    this.user = user;
    this.logfile = logfile;
    this.taskId = taskId;
    this.taskAppDirectory = taskAppDirectory;
  }
  
  public String getTaskId() {
    return taskId;
  }

  public String getCmd() {
    return cmd;
  }
  
  public String gettaskAppDirectory() {
    return taskAppDirectory;
  }
  
  public String getUser() {
    return user;
  }
  
  public String getLogfile() {
    return logfile;
  }

  @Override
  public String toString() {
    return "RunnerContext [cmd=" + cmd + ", user=" + user + ", logfile=" + logfile + ", taskId=" + taskId + ", taskAppDirectory=" + taskAppDirectory + "]";
  }
  
}
