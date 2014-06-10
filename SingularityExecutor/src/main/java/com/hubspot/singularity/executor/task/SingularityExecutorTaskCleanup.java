package com.hubspot.singularity.executor.task;

import java.nio.file.Path;
import java.util.Collections;

import org.slf4j.Logger;

import com.hubspot.singularity.executor.SimpleProcessManager;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;

public class SingularityExecutorTaskCleanup extends SimpleProcessManager {

  private final SingularityExecutorTaskDefinition taskDefinition;
  private final SingularityExecutorTaskLogManager taskLogManager;
  private final SingularityExecutorConfiguration configuration;
  private final Logger log;
  
  public SingularityExecutorTaskCleanup(SingularityExecutorTaskLogManager taskLogManager, SingularityExecutorConfiguration configuration, SingularityExecutorTaskDefinition taskDefinition, Logger log) {
    super(log);
    this.configuration = configuration;
    this.taskLogManager = taskLogManager;
    this.taskDefinition = taskDefinition;
    this.log = log;
  }
  
  public boolean cleanup() {
    boolean logTearDownSuccess = taskLogManager.teardown();
    boolean cleanupTaskAppDirectorySuccess = cleanupTaskAppDirectory();
  
    return logTearDownSuccess && cleanupTaskAppDirectorySuccess;
  }
  
  private boolean cleanupTaskAppDirectory() {
    final Path taskAppDirectoryPath = configuration.getTaskAppDirectoryPath(taskDefinition.getTaskId());
    final String pathToDelete = taskAppDirectoryPath.toString();
    
    log.info("Deleting: {}", pathToDelete);
    
    try {
      super.runCommand(Collections.singletonList(String.format("rm -rf %s", pathToDelete)));
      return true;
    } catch (Throwable t) {
      log.error("While deleting directory {}", pathToDelete, t);
    }
    
    return false;
  }
  
}
