package com.hubspot.singularity.executor.task;

import java.util.concurrent.Callable;

import org.slf4j.Logger;

import com.hubspot.singularity.executor.SafeProcessManager;

public class SingularityExecutorTaskProcessCallable extends SafeProcessManager implements Callable<Integer> {

  private final ProcessBuilder processBuilder;
  
  public SingularityExecutorTaskProcessCallable(Logger log, ProcessBuilder processBuilder) {
    super(log);
    
    this.processBuilder = processBuilder;
  }

  @Override
  public Integer call() throws Exception {
    Process process = super.startProcess(processBuilder);
    
    return process.waitFor();
  }
  
}
