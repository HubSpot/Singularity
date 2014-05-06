package com.hubspot.singularity.executor.task;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
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
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class SingularityExecutorTask {
  
  private final ExecutorDriver driver;
  private final String taskId;
  private final Protos.TaskInfo taskInfo;
  private final Logger log;
  private final ExecutorData executorData;
  private final ReentrantLock lock;
  private final AtomicBoolean killed;
  private final SingularityExecutorTaskProcessBuilder processBuilder;
  private final ObjectMapper objectMapper;
  private final SingularityExecutorConfiguration configuration;
  
  private final Path taskDirectory;
  private final Path executorBashOut;
  private final Path serviceLogOut;
  
  public SingularityExecutorTask(ExecutorDriver driver, ExecutorUtils executorUtils, SingularityExecutorConfiguration configuration, String taskId, 
      ExecutorData executorData, ArtifactManager artifactManager, Protos.TaskInfo taskInfo, TemplateManager templateManager, ObjectMapper objectMapper, Logger log) {
    this.driver = driver;
    this.taskInfo = taskInfo;
    this.executorData = executorData;
    this.taskId = taskId;
    this.log = log;
    this.configuration = configuration;
    this.objectMapper = objectMapper; 
    
    this.lock = new ReentrantLock();
    this.killed = new AtomicBoolean(false);

    this.serviceLogOut = configuration.getTaskDirectoryPath(taskId).resolve(configuration.getServiceLog());
    this.taskDirectory = configuration.getTaskDirectoryPath(taskId);
    this.executorBashOut = configuration.getExecutorBashLogPath(taskId);
    
    this.processBuilder = new SingularityExecutorTaskProcessBuilder(this, executorUtils, artifactManager, templateManager, configuration, executorData);
  }
  
  public void cleanupTaskAppDirectory() {
    final Path taskAppDirectoryPath = configuration.getTaskAppDirectoryPath(getTaskId());
    final String pathToDelete = taskAppDirectoryPath.toString();
    
    log.info("Deleting: {}", pathToDelete);
    
    try {
      Runtime.getRuntime().exec(String.format("rm -rf %s", pathToDelete)).waitFor();
    } catch (Throwable t) {
      log.error("While deleting directory {}", pathToDelete, t);
    }
  }
  
  private void ensureServiceOutExists() {
    try {
      Files.createFile(serviceLogOut);
    } catch (FileAlreadyExistsException faee) {
      log.warn("Executor out {} already existed", serviceLogOut);
    } catch (Throwable t) {
      log.error("Failed creating executor out {}", serviceLogOut, t);
    }
  }
  
  public void writeTailMetadata(boolean finished) {
    if (!executorData.getLoggingTag().isPresent()) {
      if (!finished) {
        log.warn("Not writing logging metadata because logging tag is absent");
      }
      return;
    }
    
    if (!finished) {
      ensureServiceOutExists();
    }
    
    final TailMetadata tailMetadata = new TailMetadata(serviceLogOut.toString(), executorData.getLoggingTag().get(), executorData.getLoggingExtraFields(), finished);
    final Path path = TailMetadata.getTailMetadataPath(configuration.getLogMetadataDirectory(), configuration.getLogMetadataSuffix(), tailMetadata);
    
    log.info("Writing logging metadata {} to {}", tailMetadata, path);
    
    try {
      objectMapper.writeValue(path.toFile(), tailMetadata);
    } catch (Throwable t) {
      log.error("Failed writing logging metadata", t);
    }
  }

  public Path getTaskDirectory() {
    return taskDirectory;
  }

  public Path getExecutorBashOut() {
    return executorBashOut;
  }

  public boolean isSuccessExitCode(int exitCode) {
    if (executorData.getSuccessfulExitCodes().isEmpty()) {
      return exitCode == 0;
    }
    
    return executorData.getSuccessfulExitCodes().contains(exitCode);
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
  
  public boolean wasKilled() {
    return killed.get();
  }
  
  public void markKilled() {
    this.killed.set(true);
  }
  
  public ExecutorDriver getDriver() {
    return driver;
  }

  public Protos.TaskInfo getTaskInfo() {
    return taskInfo;
  }

  public String getTaskId() {
    return taskId;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("taskId", taskId)
        .add("killed", killed.get())
        .add("taskInfo", taskInfo)
        .toString();
  }
  
}
