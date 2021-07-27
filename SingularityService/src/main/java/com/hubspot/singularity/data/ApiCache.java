package com.hubspot.singularity.data;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiCache<K, V> {
  private static final Logger LOG = LoggerFactory.getLogger(ApiCache.class);

  public final boolean isEnabled;
  private final AtomicReference<Map<K, V>> zkValues;
  private final Supplier<Map<K, V>> supplyMap;

  @Inject
  public ApiCache(
    boolean isEnabled,
    int cacheTtl,
    Supplier<Map<K, V>> supplyMap,
    ScheduledExecutorService executor
  ) {
    this.isEnabled = isEnabled;
    this.supplyMap = supplyMap;
    this.zkValues = new AtomicReference<>(new HashMap<>());

    if (this.isEnabled) {
      executor.scheduleAtFixedRate(this::reloadZkValues, 0, cacheTtl, TimeUnit.SECONDS);
    }
  }

  private void reloadZkValues() {
    LOG.debug("Reloading values for map from ZooKeeper");
    try {
      Map<K, V> newZkValues = supplyMap.get();
      zkValues.set(newZkValues);
    } catch (Exception e) {
      LOG.warn("Reloading ApiCache failed: {}", e.getMessage());
    }
  }

  public V get(K key) {
    return this.zkValues.get().get(key);
  }

  public Map<K, V> getAll() {
    Map<K, V> allValues = this.zkValues.get();
    if (allValues.isEmpty()) {
      LOG.debug("ApiCache getAll returned empty");
    }
    return allValues;
  }

  public Map<K, V> getAll(Collection<K> keys) {
    Map<K, V> allValues = this.zkValues.get();
    Map<K, V> filteredValues = keys
      .stream()
      .filter(allValues::containsKey)
      .collect(Collectors.toMap(Function.identity(), allValues::get));

    if (filteredValues.isEmpty()) {
      LOG.debug("ApiCache getAll returned empty for {}", keys);
    }

    return filteredValues;
  }

  public boolean isEnabled() {
    return isEnabled;
  }
}
