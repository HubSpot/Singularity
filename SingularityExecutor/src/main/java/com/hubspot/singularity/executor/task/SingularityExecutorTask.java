package com.hubspot.singularity.executor.task;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mesos.ExecutorDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.TaskState;

import ch.qos.logback.classic.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.deploy.ExecutorData;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.executor.TemplateManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.utils.ExecutorUtils;
import com.hubspot.singularity.runner.base.configuration.SingularityRunnerBaseConfiguration;
import com.hubspot.singularity.runner.base.shared.JsonObjectFileHelper;

public class SingularityExecutorTask {

  private final ExecutorDriver driver;
  private final Protos.TaskInfo taskInfo;
  private final Logger log;
  private final ReentrantLock lock;
  private final AtomicBoolean killed;
  private final AtomicInteger threadCountAtOverage;
  private final AtomicBoolean killedAfterThreadOverage;
  private final AtomicBoolean destroyedAfterWaiting;
  private final AtomicBoolean forceDestroyed;
  private final SingularityExecutorTaskProcessBuilder processBuilder;
  private final SingularityExecutorTaskLogManager taskLogManager;
  private final SingularityExecutorTaskCleanup taskCleanup;
  private final SingularityExecutorTaskDefinition taskDefinition;

  public SingularityExecutorTask(ExecutorDriver driver, ExecutorUtils executorUtils, SingularityRunnerBaseConfiguration baseConfiguration, SingularityExecutorConfiguration executorConfiguration, SingularityExecutorTaskDefinition taskDefinition, String executorPid,
      SingularityExecutorArtifactFetcher artifactFetcher, Protos.TaskInfo taskInfo, TemplateManager templateManager, ObjectMapper objectMapper, Logger log, JsonObjectFileHelper jsonObjectFileHelper) {
    this.driver = driver;
    this.taskInfo = taskInfo;
    this.log = log;

    this.lock = new ReentrantLock();
    this.killed = new AtomicBoolean(false);
    this.destroyedAfterWaiting = new AtomicBoolean(false);
    this.forceDestroyed = new AtomicBoolean(false);
    this.killedAfterThreadOverage = new AtomicBoolean(false);
    this.threadCountAtOverage = new AtomicInteger(0);

    this.taskDefinition = taskDefinition;

    this.taskLogManager = new SingularityExecutorTaskLogManager(taskDefinition, templateManager, baseConfiguration, executorConfiguration, log, jsonObjectFileHelper);
    this.taskCleanup = new SingularityExecutorTaskCleanup(taskLogManager, executorConfiguration, taskDefinition, log);
    this.processBuilder = new SingularityExecutorTaskProcessBuilder(this, executorUtils, artifactFetcher, templateManager, executorConfiguration, taskDefinition.getExecutorData(), executorPid);
  }

  public void cleanup(TaskState state) {
    ExtendedTaskState extendedTaskState = ExtendedTaskState.fromTaskState(state);

    boolean cleanupAppTaskDirectory = !extendedTaskState.isFailed();

    taskCleanup.cleanup(cleanupAppTaskDirectory);
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

  public Logger getLogbackLog() {
    return log;
  }

  public org.slf4j.Logger getLog() {
    return log;
  }

  public SingularityExecutorTaskProcessBuilder getProcessBuilder() {
    return processBuilder;
  }

  public boolean wasForceDestroyed() {
    return forceDestroyed.get();
  }

  public boolean wasDestroyedAfterWaiting() {
    return destroyedAfterWaiting.get();
  }

  public boolean wasKilled() {
    return killed.get();
  }

  public void markKilled() {
    this.killed.set(true);
  }

  public void markKilledDueToThreads(int currentThreads) {
    this.killedAfterThreadOverage.set(true);
    this.threadCountAtOverage.set(currentThreads);
  }

  public boolean wasKilledDueToThreads() {
    return killedAfterThreadOverage.get();
  }

  public int getThreadCountAtOverageTime() {
    return threadCountAtOverage.get();
  }

  public void markForceDestroyed() {
    this.forceDestroyed.set(true);
  }

  public void markDestroyedAfterWaiting() {
    this.destroyedAfterWaiting.set(true);
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
    return "SingularityExecutorTask [taskInfo=" + taskInfo + ", killed=" + killed + ", getTaskId()=" + getTaskId() + "]";
  }

}
