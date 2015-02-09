package com.hubspot.singularity.event;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.hubspot.singularity.event.SingularityEventModule.LISTENER_THREADPOOL_NAME;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityEventController implements SingularityEventListener {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityEventController.class);

  private final Set<SingularityEventListener> eventListeners;
  private final ListeningExecutorService listenerExecutorService;
  private final boolean waitForListeners;

  @Inject
  SingularityEventController(final Set<SingularityEventListener> eventListeners, final SingularityConfiguration configuration,
      @Named(LISTENER_THREADPOOL_NAME) final ScheduledExecutorService listenerExecutorService) {
    this.eventListeners = ImmutableSet.copyOf(checkNotNull(eventListeners, "eventListeners is null"));
    this.listenerExecutorService = MoreExecutors.listeningDecorator(checkNotNull(listenerExecutorService, "listenerExecutorService is null"));
    this.waitForListeners = configuration.isWaitForListeners();
  }

  @Override
  public void requestHistoryEvent(final SingularityRequestHistory singularityRequestHistory) {
    ImmutableSet.Builder<ListenableFuture<Void>> builder = ImmutableSet.builder();

    for (final SingularityEventListener eventListener : eventListeners) {
      builder.add(listenerExecutorService.submit(new Callable<Void>() {
        @Override
        public Void call() {
          eventListener.requestHistoryEvent(singularityRequestHistory);
          return null;
        }
      }));
    }

    processFutures(builder.build());
  }

  @Override
  public void taskHistoryUpdateEvent(final SingularityTaskHistoryUpdate singularityTaskHistoryUpdate) {
    ImmutableSet.Builder<ListenableFuture<Void>> builder = ImmutableSet.builder();

    for (final SingularityEventListener eventListener : eventListeners) {
      builder.add(listenerExecutorService.submit(new Callable<Void>() {
        @Override
        public Void call() {
          eventListener.taskHistoryUpdateEvent(singularityTaskHistoryUpdate);
          return null;
        }
      }));
    }


    processFutures(builder.build());
  }

  @Override
  public void deployHistoryEvent(final SingularityDeployUpdate singularityDeployUpdate) {
    ImmutableSet.Builder<ListenableFuture<Void>> builder = ImmutableSet.builder();

    for (final SingularityEventListener eventListener : eventListeners) {
      builder.add(listenerExecutorService.submit(new Callable<Void>() {
        @Override
        public Void call() {
          eventListener.deployHistoryEvent(singularityDeployUpdate);
          return null;
        }
      }));
    }

    processFutures(builder.build());
  }

  private void processFutures(Iterable<? extends ListenableFuture<?>> futures)
  {
    if (waitForListeners) {
      try {
        Futures.allAsList(futures).get();
      } catch (ExecutionException e) {
        LOG.warn("While waiting for event listeners", e.getCause());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
