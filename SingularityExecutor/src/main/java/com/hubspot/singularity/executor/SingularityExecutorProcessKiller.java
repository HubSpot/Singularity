package com.hubspot.singularity.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.executor.config.SingularityExecutorConfiguration;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskProcessCallable;

public class SingularityExecutorProcessKiller {

  private final SingularityExecutorConfiguration configuration;
  private final ScheduledExecutorService scheduledExecutorService;

  @Inject
  public SingularityExecutorProcessKiller(SingularityExecutorConfiguration configuration) {
    this.configuration = configuration;
    
    this.scheduledExecutorService = Executors.newScheduledThreadPool(configuration.getKillThreads(), new ThreadFactoryBuilder().setNameFormat("SingularityExecutorKillThread-%d").build());
  }
  
  public void submitKillRequest(final SingularityExecutorTaskProcessCallable processCallable) {
    // make it so that the task can not make progress
    processCallable.markKilled();
    processCallable.signalProcessIfActive();
    
    scheduledExecutorService.schedule(new Runnable() {
      
      @Override
      public void run() {
        processCallable.destroyProcessIfActive();
      }
    }, processCallable.getTask().getExecutorData().getSigKillProcessesAfterMillis().or(configuration.getHardKillAfterMillis()), TimeUnit.MILLISECONDS);
  }
  
  public ExecutorService getExecutorService() {
    return scheduledExecutorService;
  }
  
}
