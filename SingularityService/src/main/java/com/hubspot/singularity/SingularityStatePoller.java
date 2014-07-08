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
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityStatePoller implements SingularityCloseable {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityStatePoller.class);

  private final StateManager stateManager;
  private final long saveStateEverySeconds;
  
  private final SingularityCloser closer;
  private final SingularityExceptionNotifier exceptionNotifier;
  
  private ScheduledExecutorService executorService;
  private Runnable stateUpdateRunnable;
  
  @Inject
  public SingularityStatePoller(StateManager stateManager, SingularityConfiguration configuration, SingularityCloser closer, SingularityExceptionNotifier exceptionNotifier) {
    this.stateManager = stateManager;
    this.saveStateEverySeconds = configuration.getSaveStateEverySeconds();
    this.closer = closer;
    this.exceptionNotifier = exceptionNotifier;
  }
  
  public void start(final SingularityLeaderController managed, final SingularityAbort abort) {
    final SingularityStateGenerator generator = new SingularityStateGenerator(managed);
    
    LOG.info(String.format("Starting a state poller that will report every %s seconds", saveStateEverySeconds));
   
    this.executorService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityStatePoller-%d").build());
    
    stateUpdateRunnable = new Runnable() {
      
      @Override
      public void run() {
        try {
          final SingularityHostState state = generator.getState();
          LOG.trace("Saving state in ZK: " + state);
          stateManager.save(state);
        } catch (Throwable t) {
          LOG.error("Caught exception while saving state", t);
          exceptionNotifier.notify(t);
          abort.abort();
        }
      }
    };
  
    this.executorService.scheduleWithFixedDelay(stateUpdateRunnable, 0, saveStateEverySeconds, TimeUnit.SECONDS);
  }
  
  public void updateStateNow() { 
    this.executorService.execute(stateUpdateRunnable);
  }
  
  @Override
  public void close() {
    closer.shutdown(getClass().getName(), executorService);
  }

}