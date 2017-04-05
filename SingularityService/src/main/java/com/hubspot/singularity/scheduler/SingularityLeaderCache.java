package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskId;

@Singleton
public class SingularityLeaderCache {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderCache.class);

  private Map<SingularityPendingTaskId, SingularityPendingTask> pendingTaskIdToPendingTask;
  private Set<SingularityTaskId> activeTaskIds;
  private Map<String, SingularityRequestWithState> requests;
  private Map<SingularityTaskId, SingularityTaskCleanup> cleanupTasks;

  private volatile boolean active;

  @Inject
  public SingularityLeaderCache() {
    this.active = false;
  }

  public void activate() {
    active = true;
  }

  public void cachePendingTasks(List<SingularityPendingTask> pendingTasks) {
    this.pendingTaskIdToPendingTask = new ConcurrentHashMap<>(pendingTasks.size());
    pendingTasks.forEach((t) -> pendingTaskIdToPendingTask.put(t.getPendingTaskId(), t));
  }

  public void cacheActiveTaskIds(List<SingularityTaskId> activeTaskIds) {
    this.activeTaskIds = Collections.synchronizedSet(new HashSet<SingularityTaskId>(activeTaskIds.size()));
    activeTaskIds.forEach(this.activeTaskIds::add);
  }

  public void cacheRequests(List<SingularityRequestWithState> requestsWithState) {
    this.requests = new ConcurrentHashMap<>(requestsWithState.size());
    requestsWithState.forEach((r) -> requests.put(r.getRequest().getId(), r));
  }

  public void cacheCleanupTasks(List<SingularityTaskCleanup> cleanups) {
    this.cleanupTasks = new ConcurrentHashMap<>(cleanups.size());
    cleanups.forEach((c) -> cleanupTasks.put(c.getTaskId(), c));
  }

  public void stop() {
    active = false;
  }

  public boolean active() {
    return active;
  }

  public List<SingularityPendingTask> getPendingTasks() {
    return new ArrayList<>(pendingTaskIdToPendingTask.values());
  }

  public List<SingularityPendingTaskId> getPendingTaskIds() {
    return new ArrayList<>(pendingTaskIdToPendingTask.keySet());
  }

  public void deletePendingTask(SingularityPendingTaskId pendingTaskId) {
    if (!active) {
      LOG.warn("deletePendingTask {}, but not active", pendingTaskId);
      return;
    }

    pendingTaskIdToPendingTask.remove(pendingTaskId);
  }

  public Optional<SingularityPendingTask> getPendingTask(SingularityPendingTaskId pendingTaskId) {
    return Optional.fromNullable(pendingTaskIdToPendingTask.get(pendingTaskId));
  }

  public void savePendingTask(SingularityPendingTask pendingTask) {
    if (!active) {
      LOG.warn("savePendingTask {}, but not active", pendingTask);
      return;
    }

    pendingTaskIdToPendingTask.put(pendingTask.getPendingTaskId(), pendingTask);
  }

  public void deleteActiveTaskId(String taskId) {
    if (!active) {
      LOG.warn("deleteActiveTask {}, but not active", taskId);
      return;
    }

    activeTaskIds.remove(SingularityTaskId.valueOf(taskId));
  }

  public List<SingularityTaskId> exists(List<SingularityTaskId> taskIds) {
    List<SingularityTaskId> activeTaskIds = new ArrayList<>(taskIds.size());
    for (SingularityTaskId taskId : taskIds) {
      if (this.activeTaskIds.contains(taskId)) {
        activeTaskIds.add(taskId);
      }
    }
    return activeTaskIds;
  }

  public List<SingularityTaskId> getActiveTaskIds() {
    return new ArrayList<>(activeTaskIds);
  }

  public List<String> getActiveTaskIdsAsStrings() {
    List<SingularityTaskId> localActiveTaskIds = getActiveTaskIds();
    List<String> strings = new ArrayList<>(localActiveTaskIds.size());
    for (SingularityTaskId taskId : localActiveTaskIds) {
      strings.add(taskId.getId());
    }
    return strings;
  }

  public List<SingularityTaskId> getInactiveTaskIds(List<SingularityTaskId> taskIds) {
    List<SingularityTaskId> inactiveTaskIds = new ArrayList<>(taskIds.size());
    for (SingularityTaskId taskId : taskIds) {
      if (!activeTaskIds.contains(taskId)) {
        inactiveTaskIds.add(taskId);
      }
    }
    return inactiveTaskIds;
  }

  public int getNumActiveTasks() {
    return activeTaskIds.size();
  }

  public int getNumPendingTasks() {
    return pendingTaskIdToPendingTask.size();
  }

  public boolean isActiveTask(String taskId) {
    return activeTaskIds.contains(SingularityTaskId.valueOf(taskId));
  }

  public void putActiveTask(SingularityTask task) {
    if (!active) {
      LOG.warn("putActiveTask {}, but not active", task.getTaskId());
      return;
    }

    activeTaskIds.add(task.getTaskId());
  }

  public List<SingularityRequestWithState> getRequests() {
    return new ArrayList<>(requests.values());
  }

  public Optional<SingularityRequestWithState> getRequest(String requestId) {
    return Optional.fromNullable(requests.get(requestId));
  }

  public void putRequest(SingularityRequestWithState requestWithState) {
    if (!active) {
      LOG.warn("putRequest {}, but not active", requestWithState.getRequest().getId());
      return;
    }

    requests.put(requestWithState.getRequest().getId(), requestWithState);
  }

  public List<SingularityTaskCleanup> getCleanupTasks() {
    return new ArrayList<>(cleanupTasks.values());
  }

  public List<SingularityTaskId> getCleanupTaskIds() {
    return new ArrayList<>(cleanupTasks.keySet());
  }

  public Optional<SingularityTaskCleanup> getTaskCleanup(SingularityTaskId taskId) {
    return Optional.fromNullable(cleanupTasks.get(taskId));
  }

  public void deleteTaskCleanup(SingularityTaskId taskId) {
    cleanupTasks.remove(taskId);
  }

  public void saveTaskCleanup(SingularityTaskCleanup cleanup) {
    cleanupTasks.put(cleanup.getTaskId(), cleanup);
  }

  public void createTaskCleanupIfNotExists(SingularityTaskCleanup cleanup) {
    cleanupTasks.putIfAbsent(cleanup.getTaskId(), cleanup);
  }
}
