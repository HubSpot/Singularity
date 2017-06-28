package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

public class ZkChildrenCache {

  private static final Logger LOG = LoggerFactory.getLogger(ZkChildrenCache.class);

  private List<String> cache;

  private final Meter hitMeter;
  private final Meter refreshMeter;
  private final Meter clearMeter;

  private int cversion;

  private final CuratorFramework curator;
  private final Lock lock;

  public ZkChildrenCache(CuratorFramework curator, String name, MetricRegistry registry) {
    this.cache = new ArrayList<>();
    this.cversion = -1;
    this.curator = curator;
    this.lock = new ReentrantLock();

    this.hitMeter = registry.meter(String.format("zk.children.caches.%s.hits", name));
    this.refreshMeter = registry.meter(String.format("zk.children.caches.%s.refresh", name));
    this.clearMeter = registry.meter(String.format("zk.children.caches.%s.clears", name));

    registry.register(String.format("zk.children.caches.%s.size", name), new Gauge<Long>() {
      @Override
      public Long getValue() {
          return Long.valueOf(cache.size());
      }});
  }

  public void lock() {
    lock.lock();
  }

  public void unlock() {
    lock.unlock();
  }

  public List<String> getCache() {
    hitMeter.mark();
    return new ArrayList<>(cache);
  }

  private int getCurrentChildVersion(String path) {
    Stat stat = null;

    try {
      stat = curator.checkExists().forPath(path);
    } catch (Exception e) {
      LOG.error("While checking stat for {}", path, e);
      return -1;
    }

    if (stat != null) {
      return stat.getCversion();
    }

    return -1;
  }

  public void clearCache() {
    this.clearMeter.mark();
    this.cversion = -1;
  }

  public void setCache(List<String> newCache) {
    this.refreshMeter.mark();
    this.cache = new ArrayList<>(newCache);
  }

  public boolean checkCacheUpToDate(String path) {
    if (cversion == -1) {
      return false;
    }

    int newVersion = getCurrentChildVersion(path);

    boolean cacheUpToDate = newVersion == cversion;

    cversion = newVersion;

    return cacheUpToDate;
  }

}


