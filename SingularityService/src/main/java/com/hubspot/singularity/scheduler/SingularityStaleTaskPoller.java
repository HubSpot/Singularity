package com.hubspot.singularity.scheduler;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAbort;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.MetadataManager;
import com.hubspot.singularity.mesos.SingularityMesosSchedulerDelegator;

public class SingularityStaleTaskPoller {
  
  private final static Logger LOG = LoggerFactory.getLogger(SingularityStaleTaskPoller.class);

  private final SingularityConfiguration configuration;
  private final ScheduledExecutorService executorService;
  private final SingularityAbort abort;
  private final SingularityCloser closer;
  private final MetadataManager metadataManager;
  private final SingularityStaleTaskChecker staleTaskChecker;
  
  @Inject
  public SingularityStaleTaskPoller(SingularityConfiguration configuration, SingularityCloser closer, SingularityAbort abort, SingularityStaleTaskChecker staleTaskChecker, MetadataManager metadataManger) {
    this.abort = abort;
    this.closer = closer;
    this.configuration = configuration;
    this.metadataManager = metadataManger;
    this.staleTaskChecker = staleTaskChecker;
    
    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityStaleTaskPoller-%d").build());
  }
  
  public void start(final SingularityMesosSchedulerDelegator mesosScheduler) {
    LOG.info(String.format("Starting a stale task poller with a %s second delay", configuration.getWarnAfterTasksDoNotRunDefaultSeconds()));
    
    executorService.scheduleWithFixedDelay(new Runnable() {
      
      @Override
      public void run() {
        final long start = System.currentTimeMillis();
        int numStaleTasks = 0;
        
        LOG.info("Running stale task checker...");
        
        mesosScheduler.lock();
    
        Optional<Long> lastTimestamp = metadataManager.getLastCheckTimestamp();
        long now = System.currentTimeMillis();
        
        LOG.debug(String.format("Checking tasks for staleness, last check at %s, current time %s", lastTimestamp, now));
        
        try {
          numStaleTasks = staleTaskChecker.checkForStaleTasks(lastTimestamp, now);
          
          metadataManager.saveLastCheckTimestamp(now);
        } catch (Throwable t) {
          LOG.error("Caught an exception while checking for stale tasks -- aborting", t);
          abort.abort();
        } finally {
          mesosScheduler.release();
        
          LOG.info(String.format("Found %s stale tasks in %s", numStaleTasks, DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start)));
        }
      }
    }, 
    
    configuration.getWarnAfterTasksDoNotRunDefaultSeconds(), configuration.getWarnAfterTasksDoNotRunDefaultSeconds(), TimeUnit.SECONDS);
  }
  
  public void stop() {
    closer.shutdown(getClass().getName(), executorService, 1);
  }
  
}
