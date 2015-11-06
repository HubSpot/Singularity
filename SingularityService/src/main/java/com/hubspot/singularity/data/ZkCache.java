package com.hubspot.singularity.data;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ZkCache<T> {

  private final Cache<String, T> cache;
  private final Meter hitMeter;
  private final Meter missMeter;

  public ZkCache(int maxSize, int initialSize, long millisToExpireAfterAccess, MetricRegistry registry, String name) {
    cache = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .concurrencyLevel(2)
        .initialCapacity(initialSize)
        .expireAfterAccess(millisToExpireAfterAccess, TimeUnit.MILLISECONDS)
        .build();

    this.hitMeter = registry.meter(String.format("zk.caches.%s.hits", name));
    this.missMeter = registry.meter(String.format("zk.caches.%s.miss", name));

    registry.register(String.format("zk.caches.%s.size", name), new Gauge<Long>() {
      @Override
      public Long getValue() {
          return cache.size();
      }});
  }

  public Optional<T> get(String path) {
    T fromCache = cache.getIfPresent(path);

    if (fromCache == null) {
      missMeter.mark();
    } else {
      hitMeter.mark();
    }

    return Optional.fromNullable(fromCache);
  }

  public void delete(String path) {
    cache.invalidate(path);
  }

  public void set(String path, T object) {
    cache.put(path, object);
  }

}


