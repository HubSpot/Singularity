package com.hubspot.singularity.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;

@Singleton
public class SingularityWebCache {

  private volatile Map<SingularityPendingTaskId, SingularityPendingTask> cachedPendingTasks;
  private volatile long lastPendingTaskCache;

  private volatile List<SingularityTaskCleanup> cachedTaskCleanup;
  private volatile long lastTaskCleanupCache;

  private volatile Map<SingularityTaskId, SingularityTask> cachedActiveTasks;
  private volatile long lastActiveTaskCache;

  private volatile  Map<String, SingularityRequestWithState> cachedRequests;
  private volatile long lastRequestsCache;

  private volatile List<SingularityRequestGroup> cachedRequestGroups;
  private volatile long lastRequestGroupsCache;

  private volatile Map<String, RequestUtilization> cachedRequestUtilizations;
  private volatile long lastRequestUtilizationCache;

  private final long cacheForMillis;

  private final Meter cleanupHitMeter;
  private final Meter cleanupMissMeter;

  private final Meter pendingHitMeter;
  private final Meter pendingMissMeter;

  private final Meter activeHitMeter;
  private final Meter activeMissMeter;

  private final Meter requestsHitMeter;
  private final Meter requestsMissMeter;

  private final Meter requestGroupsHitMeter;
  private final Meter requestGroupsMissMeter;

  private final Meter requestUtilizationHitMeter;
  private final Meter requestUtilizationMissMeter;

  @Inject
  public SingularityWebCache(SingularityConfiguration configuration, MetricRegistry metrics) {
    this.cacheForMillis = configuration.getCacheForWebForMillis();

    this.cleanupHitMeter = metrics.meter("zk.web.caches.cleanup.hits");
    this.cleanupMissMeter = metrics.meter("zk.web.caches.cleanup.miss");

    this.pendingHitMeter = metrics.meter("zk.web.caches.pending.hits");
    this.pendingMissMeter = metrics.meter("zk.web.caches.pending.miss");

    this.activeHitMeter = metrics.meter("zk.web.caches.active.hits");
    this.activeMissMeter = metrics.meter("zk.web.caches.active.miss");

    this.requestsHitMeter = metrics.meter("zk.web.caches.requests.hits");
    this.requestsMissMeter = metrics.meter("zk.web.caches.requests.miss");

    this.requestGroupsHitMeter = metrics.meter("zk.web.caches.requests.hits");
    this.requestGroupsMissMeter = metrics.meter("zk.web.caches.requests.miss");

    this.requestUtilizationHitMeter = metrics.meter("zk.web.caches.utilization.hits");
    this.requestUtilizationMissMeter = metrics.meter("zk.web.caches.utilization.miss");
  }

  public boolean useCachedPendingTasks() {
    return useCache(lastPendingTaskCache);
  }

  public boolean useCachedCleanupTasks() {
    return useCache(lastTaskCleanupCache);
  }

  public boolean useCachedActiveTasks() {
    return useCache(lastActiveTaskCache);
  }

  public boolean useCachedRequests() {
    return useCache(lastRequestsCache);
  }

  public boolean useCachedRequestGroups() {
    return useCache(lastRequestGroupsCache);
  }

  public boolean useCachedRequestUtilizations() {
    return useCache(lastRequestUtilizationCache);
  }

  private boolean useCache(long lastCache) {
    return lastCache >= 0 && (System.currentTimeMillis() - lastCache) < cacheForMillis;
  }

  public List<SingularityPendingTask> getPendingTasks() {
    pendingHitMeter.mark();
    return new ArrayList<>(cachedPendingTasks.values());
  }

  public List<SingularityPendingTaskId> getPendingTaskIds() {
    pendingHitMeter.mark();
    return new ArrayList<>(cachedPendingTasks.keySet());
  }

  public List<SingularityTaskCleanup> getCleanupTasks() {
    cleanupHitMeter.mark();
    return new ArrayList<>(cachedTaskCleanup);
  }

  public List<SingularityTaskId> getActiveTaskIds() {
    activeHitMeter.mark();
    return new ArrayList<>(cachedActiveTasks.keySet());
  }

  public List<SingularityTask> getActiveTasks() {
    activeHitMeter.mark();
    return new ArrayList<>(cachedActiveTasks.values());
  }

  public List<SingularityRequestWithState> getRequests() {
    requestsHitMeter.mark();
    return new ArrayList<>(cachedRequests.values());
  }

  public Map<String, RequestUtilization> getRequestUtilizations() {
    requestUtilizationHitMeter.mark();
    return new HashMap<>(cachedRequestUtilizations);
  }

  public Optional<SingularityRequestWithState> getRequest(String requestId) {
    return Optional.fromNullable(cachedRequests.get(requestId));
  }

  public List<SingularityRequestGroup> getRequestGroups() {
    requestGroupsHitMeter.mark();
    return new ArrayList<>(cachedRequestGroups);
  }

  public void cacheTaskCleanup(List<SingularityTaskCleanup> newTaskCleanup) {
    cleanupMissMeter.mark();
    cachedTaskCleanup = new ArrayList<>(newTaskCleanup);
    lastTaskCleanupCache = System.currentTimeMillis();
  }

  public void cachePendingTasks(List<SingularityPendingTask> pendingTasks) {
    pendingMissMeter.mark();
    Map<SingularityPendingTaskId, SingularityPendingTask> newPendingTasks = new HashMap<>(pendingTasks.size());
    for (SingularityPendingTask pendingTask : pendingTasks) {
      newPendingTasks.put(pendingTask.getPendingTaskId(), pendingTask);
    }
    cachedPendingTasks = newPendingTasks;
    lastPendingTaskCache = System.currentTimeMillis();
  }

  public void cacheActiveTasks(List<SingularityTask> activeTasks) {
    activeMissMeter.mark();
    Map<SingularityTaskId, SingularityTask> newActiveTasks = new HashMap<>(activeTasks.size());
    for (SingularityTask activeTask : activeTasks) {
      newActiveTasks.put(activeTask.getTaskId(), activeTask);
    }
    cachedActiveTasks = newActiveTasks;
    lastActiveTaskCache = System.currentTimeMillis();
  }

  public void cacheRequests(List<SingularityRequestWithState> requests) {
    requestsMissMeter.mark();
    Map<String, SingularityRequestWithState> newRequests = new HashMap<>(requests.size());
    for (SingularityRequestWithState request : requests) {
      newRequests.put(request.getRequest().getId(), request);
    }
    cachedRequests = newRequests;
    lastRequestsCache = System.currentTimeMillis();
  }

  public void cacheRequestGroups(List<SingularityRequestGroup> requestGroups) {
    requestGroupsMissMeter.mark();
    cachedRequestGroups = new ArrayList<>(requestGroups);
    lastRequestGroupsCache = System.currentTimeMillis();
  }

  public void cacheRequestUtilizations(Map<String, RequestUtilization> requestUtilizations) {
    requestUtilizationMissMeter.mark();
    cachedRequestUtilizations = new HashMap<>(requestUtilizations);
    lastRequestUtilizationCache = System.currentTimeMillis();
  }

}
