package com.hubspot.singularity.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;

public class SingularityCleanupPoller {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityCleanupPoller.class);

  private final SingularityScheduler scheduler;
  private final SingularityConfiguration configuration;
  private final ScheduledExecutorService executorService;
  private final SingularityAbort abort;
  
  @Inject
  public SingularityCleanupPoller(SingularityConfiguration configuration, SingularityScheduler scheduler, SingularityAbort abort) {
    this.scheduler = scheduler;
    this.abort = abort;
    this.configuration = configuration;
  
    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityCleanupPoller-%d").build());
  }
  
  public void start(final SingularityMesosSchedulerDelegator mesosScheduler) {
    LOG.info(String.format("Starting a cleanup poller with a %s second delay", configuration.getCleanupEverySeconds()));
    
    executorService.scheduleWithFixedDelay(new Runnable() {
      
      @Override
      public void run() {
        mesosScheduler.lock();
        
        try {
          scheduler.drainCleanupQueue();
        
        } catch (Throwable t) {
          LOG.error("Caught an exception while draining cleanup queue -- aborting", t);
          abort.abort();
        } finally {
          mesosScheduler.release();
        }
      }
    }, 
    configuration.getCleanupEverySeconds(), configuration.getCleanupEverySeconds(), TimeUnit.SECONDS);
  }
  
  private final int WAIT_SECONDS = 1;
  
  public void stop() {
    LOG.info(String.format("Stopping cleanup poller (waiting %s seconds) ... ", WAIT_SECONDS));
    
    try {
      executorService.shutdownNow();
      executorService.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS);
    } catch (Throwable t) {
      LOG.warn("While shutting down cleanup poller", t);
    }
  }
  
}
