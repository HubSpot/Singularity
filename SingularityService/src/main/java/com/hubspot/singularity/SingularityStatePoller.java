package com.hubspot.singularity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.StateManager;

public class SingularityStatePoller {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityStatePoller.class);

  private final StateManager stateManager;
  private final long saveStateEverySeconds;
  
  private ScheduledExecutorService executorService;
  
  @Inject
  public SingularityStatePoller(StateManager stateManager, SingularityConfiguration configuration) {
    this.stateManager = stateManager;
    this.saveStateEverySeconds = configuration.getSaveStateEverySeconds();
  }
  
  public void start(final SingularityManaged managed, final SingularityAbort abort) {
    final SingularityStateGenerator generator = new SingularityStateGenerator(managed);
    
    LOG.info(String.format("Starting a state poller that will report every %s seconds", saveStateEverySeconds));
   
    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityStatePoller-%d").build());
    
    this.executorService.scheduleWithFixedDelay(new Runnable() {
      
      @Override
      public void run() {
        try {
          final SingularityHostState state = generator.getState();
          LOG.trace("Saving state in ZK: " + state);
          stateManager.save(state);
        } catch (Throwable t) {
          LOG.error("Caught exception while saving state", t);
          abort.abort();
        }
      }
    }, 0, saveStateEverySeconds, TimeUnit.SECONDS);
  }
  
  private final int WAIT_SECONDS = 1;
  
  public void stop() {
    LOG.info(String.format("Stopping state poller (waiting %s seconds) ... ", WAIT_SECONDS));
    
    try {
      executorService.shutdownNow();
      executorService.awaitTermination(WAIT_SECONDS, TimeUnit.SECONDS);
    } catch (Throwable t) {
      LOG.warn("While shutting down state poller", t);
    }
  }

}