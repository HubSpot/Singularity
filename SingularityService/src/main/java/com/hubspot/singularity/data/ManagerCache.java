package com.hubspot.singularity.data;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagerCache<K, V> {
  private static final Logger LOG = LoggerFactory.getLogger(ManagerCache.class);

  private final Cache<K, V> cache;

  @Inject
  public ManagerCache(SingularityConfiguration configuration) {
    cache =
      Caffeine
        .newBuilder()
        .expireAfterWrite(configuration.getCaffeineCacheTtl(), TimeUnit.SECONDS)
        .build();
  }

  public V getIfPresent(K key) {
    V values = cache.getIfPresent(key);
    if (values != null) {
      LOG.trace("Grabbed values for {} from cache", key);
    } else {
      LOG.trace("{} not in cache", key);
    }

    return values;
  }

  public void put(K key, V value) {
    cache.put(key, value);
    LOG.trace("Setting cache value for {} in cache", key);
  }
}
