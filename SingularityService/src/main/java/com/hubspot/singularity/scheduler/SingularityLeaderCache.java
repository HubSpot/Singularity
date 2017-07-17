package com.hubspot.singularity.scheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;

@Singleton
public class SingularityLeaderCache {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityLeaderCache.class);

  private Map<SingularityPendingTaskId, SingularityPendingTask> pendingTaskIdToPendingTask;
  private Set<SingularityTaskId> activeTaskIds;
  private Map<String, SingularityRequestWithState> requests;
  private Map<SingularityTaskId, SingularityTaskCleanup> cleanupTasks;
  private Map<String, SingularityRequestDeployState> requestIdToDeployState;
  private Map<SingularityTaskId, SingularityKilledTaskIdRecord> killedTasks;
  private Map<SingularityTaskId, Map<ExtendedTaskState, SingularityTaskHistoryUpdate>> historyUpdates;
  private Map<String, SingularitySlave> slaves;
  private Map<String, SingularityRack> racks;

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

  public void cacheRequestDeployStates(Map<String, SingularityRequestDeployState> requestDeployStates) {
    this.requestIdToDeployState = new ConcurrentHashMap<>(requestDeployStates.size());
    requestIdToDeployState.putAll(requestDeployStates);
  }

  public void cacheKilledTasks(List<SingularityKilledTaskIdRecord> killedTasks) {
    this.killedTasks = new ConcurrentHashMap<>(killedTasks.size());
    killedTasks.forEach((k) -> this.killedTasks.put(k.getTaskId(), k));
  }

  public void cacheTaskHistoryUpdates(Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> historyUpdates) {
    this.historyUpdates = new ConcurrentHashMap<>(historyUpdates.size());
    historyUpdates.entrySet().stream().forEach((e) ->
        this.historyUpdates.put(
            e.getKey(),
            e.getValue().stream()
                .collect(Collectors.toMap((u) -> u.getTaskState(), (u) -> u)))
    );
  }

  public void cacheSlaves(List<SingularitySlave> slaves) {
    this.slaves = slaves.stream().collect(Collectors.toConcurrentMap(SingularitySlave::getId, Function.identity()));
  }

  public void cacheRacks(List<SingularityRack> racks) {
    this.racks = racks.stream().collect(Collectors.toConcurrentMap(SingularityRack::getId, Function.identity()));
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

  public List<SingularityTaskId> getActiveTaskIdsForRequest(String requestId) {
    return activeTaskIds.stream()
        .filter(t -> t.getRequestId().equals(requestId))
        .collect(Collectors.toList());
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

  public void deleteRequest(String reqeustId) {
    if (!active) {
      LOG.warn("deleteRequest {}, but not active", reqeustId);
      return;
    }

    requests.remove(reqeustId);
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
    if (!active) {
      LOG.warn("deleteTaskCleanup {}, but not active", taskId);
      return;
    }

    cleanupTasks.remove(taskId);
  }

  public void saveTaskCleanup(SingularityTaskCleanup cleanup) {
    if (!active) {
      LOG.warn("saveTaskCleanup {}, but not active", cleanup);
      return;
    }

    cleanupTasks.put(cleanup.getTaskId(), cleanup);
  }

  public void createTaskCleanupIfNotExists(SingularityTaskCleanup cleanup) {
    if (!active) {
      LOG.warn("createTaskCleanupIfNotExists {}, but not active", cleanup);
      return;
    }

    cleanupTasks.putIfAbsent(cleanup.getTaskId(), cleanup);
  }

  public Optional<SingularityRequestDeployState> getRequestDeployState(String requestId) {
    return Optional.fromNullable(requestIdToDeployState.get(requestId));
  }

  public Map<String, SingularityRequestDeployState> getRequestDeployStateByRequestId() {
    return new HashMap<>(requestIdToDeployState);
  }

  public Map<String, SingularityRequestDeployState> getRequestDeployStateByRequestId(Collection<String> requestIds) {
    return new HashMap<>(requestIdToDeployState.entrySet().stream()
        .filter((e) -> requestIds.contains(e.getKey()))
        .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue()))
    );
  }

  public void deleteRequestDeployState(String requestId) {
    if (!active) {
      LOG.warn("deleteRequestDeployState {}, but not active", requestId);
      return;
    }

    requestIdToDeployState.remove(requestId);
  }

  public void putRequestDeployState(SingularityRequestDeployState requestDeployState) {
    if (!active) {
      LOG.warn("putRequestDeployState {}, but not active", requestDeployState.getRequestId());
      return;
    }

    requestIdToDeployState.put(requestDeployState.getRequestId(), requestDeployState);
  }

  public List<SingularityKilledTaskIdRecord> getKilledTasks() {
    return new ArrayList<>(killedTasks.values());
  }

  public void addKilledTask(SingularityKilledTaskIdRecord killedTask) {
    if (!active) {
      LOG.warn("addKilledTask {}, but not active", killedTask.getTaskId().getId());
      return;
    }
    killedTasks.put(killedTask.getTaskId(), killedTask);
  }

  public void deleteKilledTask(SingularityTaskId killedTaskId) {
    if (!active) {
      LOG.warn("deleteKilledTask {}, but not active", killedTaskId.getId());
      return;
    }
    killedTasks.remove(killedTaskId);
  }

  public List<SingularityTaskHistoryUpdate> getTaskHistoryUpdates(SingularityTaskId taskId) {
    List<SingularityTaskHistoryUpdate> updates = new ArrayList<>(Optional.fromNullable(historyUpdates.get(taskId)).or(new HashMap<>()).values());
    Collections.sort(updates);
    return updates;
  }

  public Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> getTaskHistoryUpdates(Collection<SingularityTaskId> taskIds) {
    return new HashMap<>(historyUpdates.entrySet()
        .stream()
        .filter((e) -> taskIds.contains(e.getKey()))
        .collect(Collectors.toMap((e) -> e.getKey(), (e) -> new ArrayList<>(e.getValue().values())))
    );
  }

  public void saveTaskHistoryUpdate(SingularityTaskHistoryUpdate taskHistoryUpdate, boolean overwrite) {
    if (!active) {
      LOG.warn("saveTaskHistoryUpdate {}, but not active", taskHistoryUpdate);
      return;
    }
    historyUpdates.putIfAbsent(taskHistoryUpdate.getTaskId(), new ConcurrentHashMap<>());
    if (overwrite) {
      historyUpdates.get(taskHistoryUpdate.getTaskId()).put(taskHistoryUpdate.getTaskState(), taskHistoryUpdate);
    } else {
      historyUpdates.get(taskHistoryUpdate.getTaskId()).putIfAbsent(taskHistoryUpdate.getTaskState(), taskHistoryUpdate);
    }
  }

  public void deleteTaskHistoryUpdate(SingularityTaskId taskId, ExtendedTaskState state) {
    if (!active) {
      LOG.warn("deleteTaskHistoryUpdate {}, but not active", taskId);
      return;
    }
    historyUpdates.getOrDefault(taskId, new HashMap<>()).remove(state);
  }

  public void deleteTaskHistory(SingularityTaskId taskId) {
    if (!active) {
      LOG.warn("deleteTaskHistory {}, but not active", taskId);
      return;
    }
    historyUpdates.remove(taskId);
  }

  public Collection<SingularitySlave> getSlaves() {
    return slaves.values();
  }

  public Optional<SingularitySlave> getSlave(String slaveId) {
    return Optional.fromNullable(slaves.get(slaveId));
  }

  public void putSlave(SingularitySlave slave) {
    if (!active) {
      LOG.warn("putSlave {}, but not active", slave);
    }

    slaves.put(slave.getId(), slave);
  }

  public Collection<SingularityRack> getRacks() {
    return racks.values();
  }

  public Optional<SingularityRack> getRack(String rackName) {
    return Optional.fromNullable(racks.get(rackName));
  }

  public void putRack(SingularityRack rack) {
    if (!active) {
      LOG.warn("putSlave {}, but not active", rack);
    }

    racks.put(rack.getId(), rack);
  }
}
