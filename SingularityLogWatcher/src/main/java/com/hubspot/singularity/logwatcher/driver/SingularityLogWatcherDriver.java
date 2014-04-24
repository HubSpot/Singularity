package com.hubspot.singularity.logwatcher.driver;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.logwatcher.LogForwarder;
import com.hubspot.singularity.logwatcher.SimpleStore;
import com.hubspot.singularity.logwatcher.SimpleStore.StoreException;
import com.hubspot.singularity.logwatcher.TailMetadata;
import com.hubspot.singularity.logwatcher.TailMetadataListener;
import com.hubspot.singularity.logwatcher.config.SingularityLogWatcherConfiguration;
import com.hubspot.singularity.logwatcher.tailer.SingularityLogWatcherTailer;

public class SingularityLogWatcherDriver implements TailMetadataListener {

  private final static Logger LOG = LoggerFactory.getLogger(SingularityLogWatcherDriver.class);

  private final SimpleStore store;
  private final LogForwarder logForwarder;
  private final SingularityLogWatcherConfiguration configuration;
  private final ExecutorService tailService;
  private final Map<TailMetadata, SingularityLogWatcherTailer> tailers;
  
  private volatile boolean shutdown;
  
  @Inject
  public SingularityLogWatcherDriver(SimpleStore store, SingularityLogWatcherConfiguration configuration, LogForwarder logForwarder) {
    this.store = store;
    this.logForwarder = logForwarder;
    this.configuration = configuration;
    this.tailers = Maps.newConcurrentMap();
    this.tailService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("SingularityLogWatcherTailThread-%d").build());
    this.shutdown = false;
  }
  
  private synchronized void tail(final TailMetadata tail) {
    final SingularityLogWatcherTailer tailer = buildTailer(tail);

    tailService.submit(new Runnable() {
      
      @Override
      public void run() {
        try {
          tailer.watch();
          
          if (!shutdown) {
            tailer.consumeStream();
            store.markConsumed(tail);
          }
        } catch (StoreException storeException) {
          LOG.error("While tailing: {}", tail, storeException);
          shutdown();
        } catch (Throwable t) {
          LOG.error("While tailing {}, will not retry until JVM is restarted or a new notification is posted", tail, t);
        } finally {
          tailer.close();

          tailers.remove(tail);
        }
      }
    });
    
    tailers.put(tail, tailer);
  }
  
  public synchronized void start() {
    final long start = System.currentTimeMillis();
    
    for (TailMetadata tail : store.getTails()) {
      tail(tail);
    }
    
    LOG.info("Started {} tails in {}", tailers.size(), JavaUtils.duration(start));
  }

  public synchronized void markShutdown() {
    this.shutdown = true;
  }
  
  public void shutdown() {
    final long start = System.currentTimeMillis();
    
    LOG.info("Shutting down with {} tailers", tailers.size());
    
    markShutdown();
    
    for (SingularityLogWatcherTailer tailer : tailers.values()) {
      tailer.stop();
    }
    
    tailService.shutdown();
    
    try {
      tailService.awaitTermination(1L, TimeUnit.DAYS);
    } catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
    
    LOG.info("Shutdown {} tailers after {}", tailers.size(), JavaUtils.duration(start));
  }
  
  private SingularityLogWatcherTailer buildTailer(TailMetadata tail) {
    return new SingularityLogWatcherTailer(tail, configuration, store, logForwarder);
  }
  
  @Override
  public synchronized void addedTailMetadata(TailMetadata newTailMetadata) {
    if (shutdown) {
      LOG.info("Not tailing {}, shutting down...", newTailMetadata);
      return;
    }
    
    tail(newTailMetadata);
  }

  @Override
  public synchronized void stopTail(final TailMetadata tailMetadataToStop) {
    if (shutdown) {
      LOG.info("Not stopping {}, shutting down...", tailMetadataToStop);
      return;
    }

    final SingularityLogWatcherTailer tailer = tailers.get(tailMetadataToStop);
    
    if (tailer == null) {
      LOG.warn("Didn't have a tailer to stop for tailmetadata {}", tailMetadataToStop);
      return;
    }
    
    tailer.stop();
  }

}
