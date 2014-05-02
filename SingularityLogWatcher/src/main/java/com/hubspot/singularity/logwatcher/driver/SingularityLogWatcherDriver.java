package com.hubspot.singularity.logwatcher.driver;

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
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;
import com.hubspot.singularity.logwatcher.TailMetadataListener;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;
import com.hubspot.singularity.logwatcher.logrotate.LogrotateTemplateManager;
import com.hubspot.singularity.logwatcher.tailer.SingularityLogWatcherTailer;
import com.hubspot.singularity.runner.base.shared.TailMetadata;

public class SingularityLogWatcherDriver implements TailMetadataListener {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogWatcherDriver.class);

  private final SimpleStore store;
  private final LogForwarder logForwarder;
  private final SingularityLogWatcherConfiguration configuration;
  private final ExecutorService tailService;
  private final ScheduledExecutorService retryService;
  private final LogrotateTemplateManager logrotateTemplateManager;
  private final Map<TailMetadata, SingularityLogWatcherTailer> tailers;
  
  private volatile boolean shutdown;
  private final Lock tailersLock;
  
  @Inject
  public SingularityLogWatcherDriver(SimpleStore store,  LogrotateTemplateManager logrotateTemplateManager, SingularityLogWatcherConfiguration configuration, LogForwarder logForwarder) {
    this.store = store;
    this.logForwarder = logForwarder;
    this.configuration = configuration;
    this.logrotateTemplateManager = logrotateTemplateManager;
    this.tailers = Maps.newConcurrentMap();
    this.tailService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SingularityLogWatcherTailer-%d").build());
    this.retryService = Executors.newScheduledThreadPool(1, new ThreadFactoryBuilder().setNameFormat("SingularityLogWatcherRetry-%d").build());
    this.shutdown = false;
    this.tailersLock = new ReentrantLock();
    
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
        }
      }
    }, configuration.getRetryDelaySeconds(), TimeUnit.SECONDS);
  }
  
  public void start() {
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

  public void markShutdown() {
    tailersLock.lock();
    this.shutdown = true;
    tailersLock.unlock();
  }
  
  public void shutdown() {
    final long start = System.currentTimeMillis();
    
    LOG.info("Shutting down with {} tailer(s)", tailers.size());
    
    markShutdown();
    
    retryService.shutdownNow();
    
    for (SingularityLogWatcherTailer tailer : tailers.values()) {
      tailer.stop();
    }
    
    tailService.shutdown();
    
    try {
      tailService.awaitTermination(1L, TimeUnit.DAYS);
    } catch (Throwable t) {
      LOG.error("While awaiting tail service", t);
    }
    
    try {
      store.close();
    } catch (Throwable t) {
      LOG.error("While closing store", t);
    }
    
    LOG.info("Shutdown after {}", JavaUtils.duration(start));
  }
  
  private Optional<SingularityLogWatcherTailer> buildTailer(TailMetadata tail) {
    try {
      SingularityLogWatcherTailer tailer = new SingularityLogWatcherTailer(tail, configuration, logrotateTemplateManager, store, logForwarder);
      return Optional.of(tailer);
    } catch (Throwable t) {
      LOG.warn("Couldn't create a tailer for {}", tail, t);
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
