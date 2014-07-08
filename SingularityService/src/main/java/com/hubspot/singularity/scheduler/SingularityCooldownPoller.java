package com.hubspot.singularity.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;

public class SingularityCooldownPoller {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityCooldownPoller.class);

  private final SingularityCooldownChecker checker;
  private final SingularityConfiguration configuration;
  private final ScheduledExecutorService executorService;
  private final SingularityAbort abort;
  private final SingularityCloser closer;
  
  @Inject
  public SingularityCooldownPoller(SingularityConfiguration configuration, SingularityCooldownChecker checker, SingularityAbort abort, SingularityCloser closer) {
    this.checker = checker;
    this.abort = abort;
    this.configuration = configuration;
    this.closer = closer;
    
    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityCooldownPoller-%d").build());
  }
  
  public void start(final SingularityMesosSchedulerDelegator mesosScheduler) {
    final long checkCooldownEveryMillis = TimeUnit.MINUTES.toMillis(configuration.getCooldownExpiresAfterMinutes()) / 2;
    
    LOG.info("Starting a cooldown poller with a {} delay", JavaUtils.durationFromMillis(checkCooldownEveryMillis));
    
    executorService.scheduleWithFixedDelay(new Runnable() {
      
      @Override
      public void run() {
        mesosScheduler.lock();
        
        try {
          checker.checkCooldowns();
          
        } catch (Throwable t) {
          LOG.error("Caught an exception while checking cooldown queue -- aborting", t);
          abort.abort();
        } finally {
          mesosScheduler.release();
        }
      }
    }, 
    checkCooldownEveryMillis, checkCooldownEveryMillis, TimeUnit.MILLISECONDS);
  }
  
  public void stop() {
    closer.shutdown(getClass().getName(), executorService, 1);
  }
  
}
