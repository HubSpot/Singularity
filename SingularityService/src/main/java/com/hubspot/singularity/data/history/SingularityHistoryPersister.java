package com.hubspot.singularity.data.history;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityCloseable;
import com.hubspot.singularity.SingularityCloser;
import com.hubspot.singularity.Utils;
import com.hubspot.singularity.config.SingularityConfiguration;

public class SingularityHistoryPersister implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityHistoryPersister.class);
  
  private final SingularityTaskHistoryPersister taskPersister;
  private final SingularityDeployHistoryPersister deployPersister;
  private final ScheduledExecutorService executorService;
  private final SingularityCloser closer;
  private final SingularityConfiguration configuration;
  
  @Inject
  public SingularityHistoryPersister(SingularityTaskHistoryPersister taskPersister, SingularityDeployHistoryPersister deployPersister, SingularityConfiguration configuration, SingularityCloser closer) {
    this.taskPersister = taskPersister;
    this.deployPersister = deployPersister;
    this.closer = closer;
    this.configuration = configuration;
    
    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityHistoryPersister-%d").build());
  }
  
  @Override
  public void close() {
    closer.shutdown(getClass().getName(), executorService, 1);
  }
  
  public void start() {
    LOG.info("Starting a history persister with a {} delay", Utils.durationFromMillis(TimeUnit.SECONDS.toMillis(configuration.getPersistHistoryEverySeconds())));
    
    executorService.scheduleWithFixedDelay(new Runnable() {
      
      @Override
      public void run() {
        try {
          taskPersister.checkInactiveTaskIds();
        } catch (Throwable t) {
          LOG.error("While persisting task history", t);
        }
        try {
          deployPersister.checkInactiveDeploys();
        } catch (Throwable t) {
          LOG.error("While persisting deploy history", t);
        }
      }
    },
    configuration.getPersistHistoryEverySeconds(), configuration.getPersistHistoryEverySeconds(), TimeUnit.SECONDS);
  }

}
