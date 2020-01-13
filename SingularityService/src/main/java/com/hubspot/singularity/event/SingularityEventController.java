package com.hubspot.singularity.event;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.CrashLoopInfo;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityManagedScheduledExecutorServiceFactory;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskWebhook;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityEventController implements SingularityEventListener {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityEventController.class);

  private final Set<SingularityEventSender> eventListeners;
  private final ListeningExecutorService listenerExecutorService;
  private final boolean waitForListeners;

  @Inject
  SingularityEventController(final Set<SingularityEventSender> eventListeners,
                             final SingularityConfiguration configuration,
                             final SingularityManagedScheduledExecutorServiceFactory scheduledExecutorServiceFactory) {
    this.eventListeners = ImmutableSet.copyOf(checkNotNull(eventListeners, "eventListeners is null"));
    this.listenerExecutorService = MoreExecutors.listeningDecorator(checkNotNull(scheduledExecutorServiceFactory.get("event-listener", configuration.getListenerThreadpoolSize()), "listenerExecutorService is null"));
    this.waitForListeners = configuration.isWaitForListeners();
  }

  @Override
  public void requestHistoryEvent(final SingularityRequestHistory singularityRequestHistory) {
    ImmutableSet.Builder<ListenableFuture<Void>> builder = ImmutableSet.builder();

    for (final SingularityEventSender eventListener : eventListeners) {
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
  public void taskHistoryUpdateEvent(final SingularityTaskWebhook singularityTaskWebhook) {
    ImmutableSet.Builder<ListenableFuture<Void>> builder = ImmutableSet.builder();

    for (final SingularityEventSender eventListener : eventListeners) {
      builder.add(listenerExecutorService.submit(new Callable<Void>() {
        @Override
        public Void call() {
          eventListener.taskWebhookEvent(singularityTaskWebhook);
          return null;
        }
      }));
    }


    processFutures(builder.build());
  }

  @Override
  public void deployHistoryEvent(final SingularityDeployUpdate singularityDeployUpdate) {
    ImmutableSet.Builder<ListenableFuture<Void>> builder = ImmutableSet.builder();

    for (final SingularityEventSender eventListener : eventListeners) {
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

  @Override
  public void crashLoopEvent(final CrashLoopInfo crashLoopUpdate) {
    ImmutableSet.Builder<ListenableFuture<Void>> builder = ImmutableSet.builder();

    for (final SingularityEventSender eventListener : eventListeners) {
      builder.add(listenerExecutorService.submit(new Callable<Void>() {
        @Override
        public Void call() {
          eventListener.crashLoopEvent(crashLoopUpdate);
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
