package com.hubspot.singularity.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;
import com.hubspot.singularity.executor.task.SingularityExecutorTaskProcessCallable;

public class SingularityExecutorProcessKiller {

  private final long hardKillAfterMillis;
  
  private final ScheduledExecutorService scheduledExecutorService;

  @Inject
  public SingularityExecutorProcessKiller(@Named(SingularityExecutorModule.HARD_KILL_AFTER_MILLIS) String hardKillAfterMillis, @Named(SingularityExecutorModule.NUM_CORE_KILL_THREADS) String killThreads) {
    this.hardKillAfterMillis = Long.parseLong(hardKillAfterMillis);
    
    this.scheduledExecutorService = Executors.newScheduledThreadPool(Integer.parseInt(killThreads), new ThreadFactoryBuilder().setNameFormat("SingularityExecutorKillThread-%d").build());
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
    }, hardKillAfterMillis, TimeUnit.MILLISECONDS);
  }
  
  public ExecutorService getExecutorService() {
    return scheduledExecutorService;
  }
  
}
