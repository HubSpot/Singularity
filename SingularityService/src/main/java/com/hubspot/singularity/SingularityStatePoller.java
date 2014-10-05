package com.hubspot.singularity;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.StateManager;
import com.hubspot.singularity.sentry.SingularityExceptionNotifier;

public class SingularityStatePoller extends SingularityCloseable<ScheduledExecutorService> {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityStatePoller.class);

  private final StateManager stateManager;
  private final long saveStateEverySeconds;

  private final SingularityExceptionNotifier exceptionNotifier;

  private Optional<ScheduledExecutorService> executorService;
  private Runnable stateUpdateRunnable;

  @Inject
  public SingularityStatePoller(StateManager stateManager, SingularityConfiguration configuration, SingularityCloser closer, SingularityExceptionNotifier exceptionNotifier) {
    super(closer);

    this.executorService = Optional.absent();
    this.stateManager = stateManager;
    this.saveStateEverySeconds = configuration.getSaveStateEverySeconds();
    this.exceptionNotifier = exceptionNotifier;
  }

  public void start(final SingularityLeaderController managed, final SingularityAbort abort) {
    final SingularityStateGenerator generator = new SingularityStateGenerator(managed);

    LOG.info(String.format("Starting a state poller that will report every %s seconds", saveStateEverySeconds));

    this.executorService = Optional.of(Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityStatePoller-%d").build()));

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

    this.executorService.get().scheduleWithFixedDelay(stateUpdateRunnable, 0, saveStateEverySeconds, TimeUnit.SECONDS);
  }

  public void updateStateNow() {
    if (!executorService.isPresent()) {
      LOG.warn("Asked to update state, but executor service wasn't present");
      return;
    }

    this.executorService.get().execute(stateUpdateRunnable);
  }

  @Override
  public Optional<ScheduledExecutorService> getExecutorService() {
    return executorService;
  }

}
