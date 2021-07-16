package com.hubspot.singularity.data;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiCache<K, V> {
  private static final Logger LOG = LoggerFactory.getLogger(ApiCache.class);

  public final boolean isEnabled;
  private final LoadingCache<K, V> cache;

  @Inject
  public ApiCache(boolean isEnabled, int cacheTtl, CacheLoader<K, V> loader) {
    this.isEnabled = isEnabled;
    cache =
      Caffeine.newBuilder().refreshAfterWrite(cacheTtl, TimeUnit.SECONDS).build(loader);
  }

  public V get(K key) {
    V values = cache.get(key);
    if (values != null) {
      LOG.debug("Grabbed values for {} from cache", key);
    } else {
      LOG.debug("{} not in cache, setting", key);
    }

    return values;
  }

  public Map<K, V> getAll(@Nonnull Iterable<? extends K> keys) {
    Map<K, V> values = cache.getAll(keys);
    if (!values.isEmpty()) {
      LOG.debug("Grabbed {} mapped values from cache", values.size());
    }

    return values;
  }

  public boolean isEnabled() {
    return isEnabled;
  }
}
