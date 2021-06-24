package com.hubspot.singularity.data;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.hubspot.singularity.config.SingularityConfiguration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagerCache<K, V> {
  private static final Logger LOG = LoggerFactory.getLogger(ManagerCache.class);

  public final boolean isEnabled;
  private final int cacheTtl;
  private LoadingCache<K, V> cache;

  @Inject
  public ManagerCache(
    SingularityConfiguration configuration,
    Function<? super K, V> loader
  ) {
    isEnabled = configuration.useCaffeineCache();
    cacheTtl = configuration.getCaffeineCacheTtl();
    cache =
      Caffeine
        .newBuilder()
        .expireAfterWrite(cacheTtl, TimeUnit.SECONDS)
        .build(loader::apply);
  }

  public V get(K key) {
    if (!isEnabled) {
      return null;
    }

    V values = cache.get(key);
    if (values != null) {
      LOG.trace("Grabbed values for {} from cache", key);
    } else {
      LOG.trace("{} not in cache, setting", key);
    }

    return values;
  }

  public Map<K, V> getAll(@Nonnull Iterable<? extends K> keys) {
    if (!isEnabled) {
      return null;
    }

    Map<K, V> values = cache.getAll(keys);
    if (values.isEmpty()) {
      LOG.trace("Grabbed mapped values for {} from cache", keys);
    }

    return values.isEmpty() ? null : values;
  }
}
