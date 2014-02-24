package com.hubspot.singularity.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;

public class SingularityCleanupPoller {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityCleanupPoller.class);

  private final SingularityCleaner cleaner;
  private final SingularityConfiguration configuration;
  private final ScheduledExecutorService executorService;
  private final SingularityAbort abort;
  private final SingularityCloser closer;
  
  @Inject
  public SingularityCleanupPoller(SingularityConfiguration configuration, SingularityCleaner cleaner, SingularityAbort abort, SingularityCloser closer) {
    this.cleaner = cleaner;
    this.abort = abort;
    this.configuration = configuration;
    this.closer = closer;
    
    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityCleanupPoller-%d").build());
  }
  
  public void start(final SingularityMesosSchedulerDelegator mesosScheduler) {
    LOG.info(String.format("Starting a cleanup poller with a %s second delay", configuration.getCleanupEverySeconds()));
    
    executorService.scheduleWithFixedDelay(new Runnable() {
      
      @Override
      public void run() {
        mesosScheduler.lock();
        
        try {
          cleaner.drainCleanupQueue();
        
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
  
  public void stop() {
    closer.shutdown(getClass().getName(), executorService, 1);
  }
  
}
