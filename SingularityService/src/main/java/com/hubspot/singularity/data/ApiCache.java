package com.hubspot.singularity.data;

import com.google.inject.Inject;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiCache<K, V> {
  private static final Logger LOG = LoggerFactory.getLogger(ApiCache.class);

  private final boolean isEnabled;
  private final AtomicReference<Map<K, V>> zkValues;
  private final Supplier<Map<K, V>> supplyMap;
  private final int cacheTtl;
  private final ScheduledExecutorService executor;

  private LeaderLatch leaderLatch;

  private ScheduledFuture<?> reloadingFuture;

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
    this.cacheTtl = cacheTtl;
    this.executor = executor;
  }

  public void startReloader(LeaderLatch leaderLatch) {
    this.leaderLatch = leaderLatch;
    if (isEnabled) {
      if (leaderLatch.hasLeadership()) {
        LOG.debug("Not doing initial ApiCache load");
      } else {
        reloadZkValues();
      }
      reloadingFuture =
        executor.scheduleAtFixedRate(this::reloadZkValues, 0, cacheTtl, TimeUnit.SECONDS);
    }
  }

  public void stopReloader() {
    if (reloadingFuture != null) {
      reloadingFuture.cancel(true);
    }
  }

  private void reloadZkValues() {
    if (!leaderLatch.hasLeadership()) {
      try {
        Map<K, V> newZkValues = supplyMap.get();
        if (!newZkValues.isEmpty()) {
          zkValues.set(newZkValues);
        } else {
          LOG.warn("Empty values on cache reload, keeping old values");
        }
      } catch (Exception e) {
        LOG.warn("Reloading ApiCache failed: {}", e.getMessage());
      }
    } else {
      if (!zkValues.get().isEmpty()) {
        LOG.debug("Clearing ZK values because instance is leader");
        zkValues.get().clear();
      }
    }
  }

  public V get(K key) {
    V value = this.zkValues.get().get(key);

    if (value == null) {
      LOG.trace("ApiCache returned null for {}", key);
    }

    return value;
  }

  public Map<K, V> getAll() {
    Map<K, V> allValues = this.zkValues.get();
    if (allValues.isEmpty()) {
      LOG.trace("ApiCache getAll returned empty");
    } else {
      LOG.trace("getAll returned {} values", allValues.size());
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
      LOG.trace("ApiCache getAll returned empty for {}", keys);
    } else {
      LOG.trace(
        "getAll returned {} for {} amount requested",
        filteredValues.size(),
        keys.size()
      );
    }

    return filteredValues;
  }

  public boolean isEnabled() {
    return isEnabled;
  }
}
