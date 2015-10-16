package com.hubspot.singularity.logwatcher.driver;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;
import com.hubspot.singularity.logwatcher.TailMetadataListener;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;
import com.hubspot.singularity.logwatcher.tailer.SingularityLogWatcherTailer;
import com.hubspot.singularity.runner.base.sentry.SingularityRunnerExceptionNotifier;
import com.hubspot.singularity.runner.base.shared.SingularityDriver;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class SingularityLogWatcherDriver implements TailMetadataListener, SingularityDriver {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLogWatcherDriver.class);

  private final SimpleStore store;
  private final LogForwarder logForwarder;
  private final SingularityLogWatcherConfiguration configuration;
  private final ExecutorService tailService;
  private final ScheduledExecutorService retryService;
  private final Map<TailMetadata, SingularityLogWatcherTailer> tailers;
  private final SingularityRunnerExceptionNotifier exceptionNotifier;

  private volatile boolean shutdown;
  private final Lock tailersLock;

  @Inject
  public SingularityLogWatcherDriver(SimpleStore store, SingularityLogWatcherConfiguration configuration, LogForwarder logForwarder, SingularityRunnerExceptionNotifier exceptionNotifier) {
    this.store = store;
    this.logForwarder = logForwarder;
    this.configuration = configuration;
    this.tailers = Maps.newConcurrentMap();
    this.tailService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SingularityLogWatcherTailer-%d").build());
    this.retryService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityLogWatcherRetry-%d").build());
    this.shutdown = false;
    this.tailersLock = new ReentrantLock();
    this.exceptionNotifier = exceptionNotifier;

    this.store.registerListener(this);
  }

  private boolean tail(final TailMetadata tail) {
    final Optional<SingularityLogWatcherTailer> maybeTailer = buildTailer(tail);

    if (!maybeTailer.isPresent()) {
      return false;
    }

    final SingularityLogWatcherTailer tailer = maybeTailer.get();

    tailService.submit(new Runnable() {

      @Override
      public void run() {
        try {
          tailer.watch();

          if (!shutdown) {
            LOG.info("Consuming tail: {}", tail);

            tailer.consumeStream();
            store.markConsumed(tail);
          }
        } catch (Throwable t) {
          exceptionNotifier.notify(t, ImmutableMap.of("tailFilename", tail.getFilename()));
          if (shutdown) {
            LOG.error("Exception tailing {} while shutting down", tail, t);
          } else {
            LOG.error("Exception tailing {}, will retry in {}", tail, JavaUtils.durationFromMillis(TimeUnit.SECONDS.toMillis(configuration.getRetryDelaySeconds())), t);

            tailLater(tail);
          }
        } finally {
          tailer.close();

          tailers.remove(tail);
        }
      }
    });

    tailers.put(tail, tailer);
    return true;
  }

  private void tailLater(final TailMetadata tail) {
    retryService.schedule(new Runnable() {

      @Override
      public void run() {
        LOG.debug("Retrying {}", tail);
        try {
          tailChanged(tail);
        } catch (Throwable unexpected) {
          LOG.error("Unexpected exception for {} while attempting retry", tail, unexpected);
          exceptionNotifier.notify(unexpected, ImmutableMap.of("tailFilename", tail.getFilename()));
        }
      }
    }, configuration.getRetryDelaySeconds(), TimeUnit.SECONDS);
  }

  @Override
  public void startAndWait() {
    final long start = System.currentTimeMillis();

    int success = 0;
    int total = 0;

    tailersLock.lock();

    try {
      if (shutdown) {
        LOG.info("Not starting, was already shutdown");
        return;
      }

      for (TailMetadata tail : store.getTails()) {
        if (tail(tail)) {
          success++;
        } else {
          tailLater(tail);
        }
        total++;
      }
    } finally {
      tailersLock.unlock();
    }

    LOG.info("Started {} tail(s) out of {} in {}", success, total, JavaUtils.duration(start));

    store.start();
  }

  public boolean markShutdown() {
    tailersLock.lock();
    try {
      if (shutdown) {
        return false;
      }
      shutdown = true;
      return true;
    } finally {
      tailersLock.unlock();
    }
  }

  @Override
  public void shutdown() {
    final long start = System.currentTimeMillis();

    LOG.info("Shutting down with {} tailer(s)", tailers.size());

    if (!markShutdown()) {
      LOG.info("Already shutdown, canceling redundant call");
      return;
    }

    retryService.shutdownNow();

    for (SingularityLogWatcherTailer tailer : tailers.values()) {
      tailer.stop();
    }

    tailService.shutdown();

    try {
      tailService.awaitTermination(1L, TimeUnit.DAYS);
    } catch (Throwable t) {
      LOG.error("While awaiting tail service", t);
      exceptionNotifier.notify(t, Collections.<String, String>emptyMap());
    }

    try {
      store.close();
    } catch (Throwable t) {
      LOG.error("While closing store", t);
      exceptionNotifier.notify(t, Collections.<String, String>emptyMap());
    }

    LOG.info("Shutdown after {}", JavaUtils.duration(start));
  }

  private Optional<SingularityLogWatcherTailer> buildTailer(TailMetadata tail) {
    try {
      SingularityLogWatcherTailer tailer = new SingularityLogWatcherTailer(tail, configuration, store, logForwarder);
      return Optional.of(tailer);
    } catch (Throwable t) {
      LOG.warn("Couldn't create a tailer for {}", tail, t);
      exceptionNotifier.notify(t, ImmutableMap.of("tailFilename", tail.getFilename()));
      return Optional.absent();
    }
  }

  @Override
  public void tailChanged(TailMetadata tailMetadata) {
    tailersLock.lock();

    try {
      if (shutdown) {
        LOG.info("Not handling notification {}, shutting down...", tailMetadata);
        return;
      }

      final SingularityLogWatcherTailer tailer = tailers.get(tailMetadata);

      if (tailer != null) {
        if (tailMetadata.isFinished()) {
          tailer.stop();
        } else {
          LOG.info("Ignoring notification about {} since we already had a tailer for it", tailMetadata);
        }
      } else {
        tail(tailMetadata);
      }
    } finally {
      tailersLock.unlock();
    }
  }

}
