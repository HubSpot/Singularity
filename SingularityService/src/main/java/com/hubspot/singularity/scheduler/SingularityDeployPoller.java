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
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;

public class SingularityDeployPoller {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityDeployPoller.class);

  private final SingularityConfiguration configuration;
  private final ScheduledExecutorService executorService;
  private final SingularityAbort abort;
  private final SingularityCloser closer;
  private final SingularityDeployChecker deployChecker;
  
  @Inject
  public SingularityDeployPoller(SingularityDeployChecker deployChecker, SingularityConfiguration configuration, SingularityAbort abort, SingularityCloser closer) {
    this.abort = abort;
    this.deployChecker = deployChecker;
    this.configuration = configuration;
    this.closer = closer;
    
    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityDeployPoller-%d").build());
  }
  
  public void start(final SingularityMesosSchedulerDelegator mesosScheduler) {
    LOG.info(String.format("Starting a deploy poller with a %s second delay", configuration.getCheckDeploysEverySeconds()));
    
    executorService.scheduleWithFixedDelay(new Runnable() {
      
      @Override
      public void run() {
        mesosScheduler.lock();
        
        try {
          final long now = System.currentTimeMillis();
          
          final int numDeploys = deployChecker.checkDeploys();
       
          LOG.info(String.format("Checked %s deploys in %s", numDeploys, DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - now)));
        } catch (Throwable t) {
          LOG.error("Caught an exception while checking deploys -- aborting", t);
          abort.abort();
        } finally {
          mesosScheduler.release();
        }
      }
    },
    
    configuration.getCheckDeploysEverySeconds(), configuration.getCheckDeploysEverySeconds(), TimeUnit.SECONDS);
  }
  
  public void stop() {
    closer.shutdown(getClass().getName(), executorService, 1);
  }
  
}
