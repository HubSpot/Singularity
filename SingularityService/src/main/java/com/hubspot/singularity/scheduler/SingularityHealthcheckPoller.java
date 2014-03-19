package com.hubspot.singularity.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityHealthcheckPoller {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityHealthcheckPoller.class);

  private final SingularityConfiguration configuration;
  private final ScheduledExecutorService executorService;
  private final SingularityAbort abort;
  private final SingularityCloser closer;
  private final SingularityHealthchecker healthchecker;
  
  @Inject
  public SingularityHealthcheckPoller(SingularityHealthchecker healthchecker, SingularityConfiguration configuration, SingularityAbort abort, SingularityCloser closer) {
    this.abort = abort;
    this.healthchecker = healthchecker;
    this.configuration = configuration;
    this.closer = closer;
    
    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityHealthcheckPoller-%d").build());
  }
  
  public void start() {
    LOG.info(String.format("Starting a healthcheck poller with a %s second delay", configuration.getSendHealthchecksEverySeconds()));
    
    executorService.scheduleWithFixedDelay(new Runnable() {
      
      @Override
      public void run() {
        
        try {
          final long now = System.currentTimeMillis();
         
          int numHealthChecks = 0;
          
          // pull health checks
          // check if its time 
          // if its time healthcheck 
          // PROFIT
          
          
          LOG.info(String.format("Sent %s healthchecks in %s", numHealthChecks, DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - now)));
        } catch (Throwable t) {
          LOG.error("Caught an exception while checking deploys -- aborting", t);
          abort.abort();
        }
      }
    },
    
    configuration.getSendHealthchecksEverySeconds(), configuration.getSendHealthchecksEverySeconds(), TimeUnit.SECONDS);
  }
  
  public void stop() {
    closer.shutdown(getClass().getName(), executorService, 1);
  }
  
}
