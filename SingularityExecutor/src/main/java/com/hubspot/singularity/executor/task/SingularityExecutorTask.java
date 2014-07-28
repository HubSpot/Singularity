package com.hubspot.singularity.executor.task;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;

import ch.qos.logback.classic.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Objects;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.singularity.executor.ArtifactManager;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;

public class SingularityExecutorTask {
  
  private final ExecutorDriver driver;
  private final Protos.TaskInfo taskInfo;
  private final Logger log;
  private final ReentrantLock lock;
  private final AtomicBoolean killed;
  private final AtomicBoolean destroyed;
  private final SingularityExecutorTaskProcessBuilder processBuilder;
  private final SingularityExecutorTaskLogManager taskLogManager;
  private final SingularityExecutorTaskCleanup taskCleanup;
  private final SingularityExecutorTaskDefinition taskDefinition;
  
  public SingularityExecutorTask(ExecutorDriver driver, ExecutorUtils executorUtils, SingularityExecutorConfiguration configuration, SingularityExecutorTaskDefinition taskDefinition, String executorPid,
      ArtifactManager artifactManager, Protos.TaskInfo taskInfo, TemplateManager templateManager, ObjectMapper objectMapper, Logger log, JsonObjectFileHelper jsonObjectFileHelper) {
    this.driver = driver;
    this.taskInfo = taskInfo;
    this.log = log;
    
    this.lock = new ReentrantLock();
    this.killed = new AtomicBoolean(false);
    this.destroyed = new AtomicBoolean(false);
    
    this.taskDefinition = taskDefinition;
    
    this.taskLogManager = new SingularityExecutorTaskLogManager(taskDefinition, templateManager, configuration, log, jsonObjectFileHelper);
    this.taskCleanup = new SingularityExecutorTaskCleanup(taskLogManager, configuration, taskDefinition, log);
    this.processBuilder = new SingularityExecutorTaskProcessBuilder(this, executorUtils, artifactManager, templateManager, configuration, taskDefinition.getExecutorData(), executorPid);
  }
    
  public void cleanup() {
    taskCleanup.cleanup();
  }
  
  public SingularityExecutorTaskLogManager getTaskLogManager() {
    return taskLogManager;
  }

  public boolean isSuccessExitCode(int exitCode) {
    if (getExecutorData().getSuccessfulExitCodes().isEmpty()) {
      return exitCode == 0;
    }
    
    return getExecutorData().getSuccessfulExitCodes().contains(exitCode);
  }
  
  public ReentrantLock getLock() {
    return lock;
  }
  
  public Logger getLog() {
    return log;
  }
  
  public SingularityExecutorTaskProcessBuilder getProcessBuilder() {
    return processBuilder;
  }
  
  public boolean wasDestroyed() {
    return destroyed.get();
  }
  
  public boolean wasKilled() {
    return killed.get();
  }
  
  public void markKilled() {
    this.killed.set(true);
  }
  
  public void markDestroyed() {
    this.destroyed.set(true);
  }
  
  public ExecutorDriver getDriver() {
    return driver;
  }

  public Protos.TaskInfo getTaskInfo() {
    return taskInfo;
  }

  public String getTaskId() {
    return taskDefinition.getTaskId();
  }

  public ExecutorData getExecutorData() {
    return taskDefinition.getExecutorData();
  }
  
  public SingularityExecutorTaskDefinition getTaskDefinition() {
    return taskDefinition;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("taskId", getTaskId())
        .add("killed", killed.get())
        .add("taskInfo", taskInfo)
        .toString();
  }
  
}
