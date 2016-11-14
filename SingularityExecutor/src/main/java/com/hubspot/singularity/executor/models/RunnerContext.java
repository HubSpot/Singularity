package com.hubspot.singularity.executor.models;

import com.google.common.base.Objects;
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
  private final String logFilePath;
  private final String taskId;

  private final Optional<Integer> maxTaskThreads;
  private final boolean shouldChangeUser;
  private final Integer maxOpenFiles;
  private final String switchUserCommand;
  private final boolean useFileAttributes;

  public RunnerContext(String cmd, String taskAppDirectory, String logDir, String user, String logFile, String logFilePath, String taskId, Optional<Integer> maxTaskThreads, boolean shouldChangeUser, Integer maxOpenFiles, String switchUserCommand, boolean useFileAttributes) {
    this.cmd = cmd;
    this.taskAppDirectory = taskAppDirectory;
    this.logDir = logDir;
    this.user = user;
    this.logFile = logFile;
    this.logFilePath = logFilePath;
    this.taskId = taskId;

    this.maxTaskThreads = maxTaskThreads;
    this.shouldChangeUser = shouldChangeUser;
    this.maxOpenFiles = maxOpenFiles;
    this.switchUserCommand = switchUserCommand;
    this.useFileAttributes = useFileAttributes;
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

  public String getLogFilePath() {
    return logFilePath;
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

  public Integer getMaxOpenFiles() {
    return maxOpenFiles;
  }

  public String getSwitchUserCommand() {
    return switchUserCommand;
  }

  public boolean isUseFileAttributes() {
    return useFileAttributes;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
      .add("cmd", cmd)
      .add("taskAppDirectory", taskAppDirectory)
      .add("logDir", logDir)
      .add("user", user)
      .add("logFile", logFile)
      .add("logFilePath", logFilePath)
      .add("taskId", taskId)
      .add("maxTaskThreads", maxTaskThreads)
      .add("shouldChangeUser", shouldChangeUser)
      .add("maxOpenFiles", maxOpenFiles)
      .add("switchUserCommand", switchUserCommand)
      .add("useFileAttributes", useFileAttributes)
      .toString();
  }
}
