package com.hubspot.singularity.executor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.executor.config.SingularityExecutorLogging;
import com.hubspot.singularity.executor.config.SingularityExecutorModule;

public class SingularityExecutorKiller {

  private final long hardKillAfterMillis;
  
  private final ScheduledExecutorService scheduledExecutorService;
  private final SingularityExecutorLogging logging;

  @Inject
  public SingularityExecutorKiller(@Named(SingularityExecutorModule.HARD_KILL_AFTER_MILLIS) String hardKillAfterMillis,  @Named(SingularityExecutorModule.NUM_CORE_KILL_THREADS) String killThreads, SingularityExecutorLogging logging) {
    this.hardKillAfterMillis = Long.parseLong(hardKillAfterMillis);
    this.logging = logging;
    
    this.scheduledExecutorService = Executors.newScheduledThreadPool(Integer.parseInt(killThreads), new ThreadFactoryBuilder().setNameFormat("SingularityExecutorKillThread-%d").build());
  }
  
  public void kill(final SingularityExecutorTaskHolder taskHolder) {
    // make it so that the task can not make progress
    taskHolder.getTask().markKilled();
    taskHolder.getFuture().cancel(true);
    
    scheduledExecutorService.schedule(new Runnable() {
      
      @Override
      public void run() {
        taskHolder.getTask().destroyProcessIfActive();
        
        logging.stopTaskLogger(taskHolder.getTask().getTaskId(), taskHolder.getTask().getLog());
      }
    }, hardKillAfterMillis, TimeUnit.MILLISECONDS);
  }
  
  public void shutdown() {
    scheduledExecutorService.shutdown();
  }
  
}
