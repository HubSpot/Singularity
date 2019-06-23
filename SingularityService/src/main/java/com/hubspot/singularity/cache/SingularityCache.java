package com.hubspot.singularity.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.CacheConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.atomix.core.Atomix;
import io.atomix.core.map.DistributedMap;
import io.atomix.core.set.DistributedSet;

@Singleton
public class SingularityCache {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityCache.class);

  private final Atomix atomix;
  private final CacheConfiguration cacheConfiguration;

  private DistributedMap<SingularityPendingTaskId, SingularityPendingTask> pendingTaskIdToPendingTask;
  private DistributedSet<SingularityTaskId> activeTaskIds;
  private DistributedMap<String, SingularityRequestWithState> requests;
  private DistributedMap<SingularityTaskId, SingularityTaskCleanup> cleanupTasks;
  private DistributedMap<String, SingularityRequestDeployState> requestIdToDeployState;
  private DistributedMap<SingularityTaskId, SingularityKilledTaskIdRecord> killedTasks;
  private DistributedMap<SingularityTaskId, SingularityHistoryUpdates> historyUpdates;
  private DistributedMap<String, SingularitySlave> slaves;
  private DistributedMap<String, SingularityRack> racks;
  private DistributedSet<SingularityPendingTaskId> pendingTaskIdsToDelete;
  private DistributedMap<String, RequestUtilization> requestUtilizations;
  private DistributedMap<String, SingularitySlaveUsageWithId> slaveUsages;

  private volatile boolean leader;

  @Inject
  public SingularityCache(Atomix atomix,
                          SingularityConfiguration configuration) {
    this.leader = false;
    this.atomix = atomix;
    this.cacheConfiguration = configuration.getCacheConfiguration();
  }

  public void setup() {
    this.pendingTaskIdToPendingTask = CacheObjectBuilder.newAtomixMap(
        atomix,
        "pendingTaskIdToPendingTask",
        SingularityPendingTaskId.class,
        SingularityPendingTask.class,
        cacheConfiguration.getPendingTaskCacheSize());
    this.activeTaskIds = CacheObjectBuilder.newAtomixSet(
        atomix,
        "activeTaskIds",
        SingularityTaskId.class
    );
    this.requests = CacheObjectBuilder.newAtomixMap(
        atomix,
        "requests",
        String.class,
        SingularityRequestWithState.class,
        cacheConfiguration.getRequestCacheSize());
    this.cleanupTasks = CacheObjectBuilder.newAtomixMap(
        atomix,
        "cleanupTasks",
        SingularityTaskId.class,
        SingularityTaskCleanup.class,
        cacheConfiguration.getCleanupTasksCacheSize());
    this.requestIdToDeployState = CacheObjectBuilder.newAtomixMap(
        atomix,
        "requestIdToDeployState",
        String.class,
        SingularityRequestDeployState.class,
        cacheConfiguration.getRequestCacheSize());
    this.killedTasks = CacheObjectBuilder.newAtomixMap(
        atomix,
        "killedTasks",
        SingularityTaskId.class,
        SingularityKilledTaskIdRecord.class,
        cacheConfiguration.getRequestCacheSize());
    this.historyUpdates = CacheObjectBuilder.newAtomixMap(
        atomix,
        "historyUpdates",
        SingularityTaskId.class,
        SingularityHistoryUpdates.class,
        cacheConfiguration.getHistoryUpdateCacheSize());
    this.slaves = CacheObjectBuilder.newAtomixMap(
        atomix,
        "slaves",
        String.class,
        SingularitySlave.class,
        cacheConfiguration.getSlaveCacheSize());
    this.racks = CacheObjectBuilder.newAtomixMap(
        atomix,
        "racks",
        String.class,
        SingularityRack.class,
        cacheConfiguration.getRackCacheSize());
    this.pendingTaskIdsToDelete = CacheObjectBuilder.newAtomixSet(
        atomix,
        "pendingTaskIdsToDelete",
        SingularityPendingTaskId.class
    );
    this.requestUtilizations = CacheObjectBuilder.newAtomixMap(
        atomix,
        "requestUtilizations",
        String.class,
        RequestUtilization.class,
        cacheConfiguration.getRequestCacheSize());
    this.slaveUsages = CacheObjectBuilder.newAtomixMap(
        atomix,
        "slaveUsages",
        String.class,
        SingularitySlaveUsageWithId.class,
        cacheConfiguration.getSlaveCacheSize());
    // TODO caffeine cache to replace ZkCache
  }

  public void markLeader() {
    leader = true;
  }

  public void markNotLeader() {
    leader = false;
  }

  public void close() {
    leader = false;
    // TODO - close individual maps + shut down atomix
  }

  public boolean isLeader() {
    return leader;
  }


  // Loading in initial data
  // TODO - clear each first
  public void cachePendingTasks(List<SingularityPendingTask> pendingTasks) {
    pendingTasks.forEach((t) -> pendingTaskIdToPendingTask.put(t.getPendingTaskId(), t));
  }

  public void cachePendingTasksToDelete(List<SingularityPendingTaskId> pendingTaskIds) {
    pendingTaskIdsToDelete.addAll(pendingTaskIds);
  }

  public void cacheActiveTaskIds(List<SingularityTaskId> activeTaskIds) {
    activeTaskIds.forEach(this.activeTaskIds::add);
  }

  public void cacheRequests(List<SingularityRequestWithState> requestsWithState) {
    requestsWithState.forEach((r) -> requests.put(r.getRequest().getId(), r));
  }

  public void cacheCleanupTasks(List<SingularityTaskCleanup> cleanups) {
    cleanups.forEach((c) -> cleanupTasks.put(c.getTaskId(), c));
  }

  public void cacheRequestDeployStates(Map<String, SingularityRequestDeployState> requestDeployStates) {
    requestIdToDeployState.putAll(requestDeployStates);
  }

  public void cacheKilledTasks(List<SingularityKilledTaskIdRecord> killedTasks) {
    killedTasks.forEach((k) -> this.killedTasks.put(k.getTaskId(), k));
  }

  public void cacheTaskHistoryUpdates(Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> historyUpdates) {
    historyUpdates.entrySet().stream().forEach((e) ->
        this.historyUpdates.put(
            e.getKey(),
            new SingularityHistoryUpdates(e.getValue().stream()
                .collect(Collectors.toMap((u) -> u.getTaskState(), (u) -> u))))
    );
  }

  public void cacheSlaves(List<SingularitySlave> slaves) {
    slaves.forEach((s) -> this.slaves.put(s.getId(), s));
  }

  public void cacheRacks(List<SingularityRack> racks) {
    racks.forEach((r) -> this.racks.put(r.getId(), r));
  }

  public void cacheRequestUtilizations(Map<String, RequestUtilization> requestUtilizations) {
    this.requestUtilizations.putAll(requestUtilizations);
  }

  public void cacheSlaveUsages(Map<String, SingularitySlaveUsageWithId> slaveUsages) {
    this.slaveUsages.putAll(slaveUsages);
  }

  // Methods to actually access the data
  public List<SingularityPendingTask> getPendingTasks() {
    return new ArrayList<>(pendingTaskIdToPendingTask.values());
  }

  public List<SingularityPendingTaskId> getPendingTaskIds() {
    return new ArrayList<>(pendingTaskIdToPendingTask.keySet());
  }

  public List<SingularityPendingTaskId> getPendingTaskIdsForRequest(String requestId) {
    Set<SingularityPendingTaskId> allPendingTaskIds = new HashSet<>(pendingTaskIdToPendingTask.keySet());
    return allPendingTaskIds.stream()
        .filter(t -> t.getRequestId().equals(requestId))
        .collect(Collectors.toList());
  }

  public List<SingularityPendingTaskId> getPendingTaskIdsToDelete() {
    synchronized (pendingTaskIdsToDelete) {
      return new ArrayList<>(pendingTaskIdsToDelete);
    }
  }

  public void markPendingTaskForDeletion(SingularityPendingTaskId taskId) {
    if (!leader) {
      LOG.warn("markPendingTaskForDeletion {}, but not leader", taskId);
      return;
    }
    pendingTaskIdsToDelete.add(taskId);
  }

  public void deletePendingTask(SingularityPendingTaskId pendingTaskId) {
    if (!leader) {
      LOG.warn("deletePendingTask {}, but not leader", pendingTaskId);
      return;
    }
    if (pendingTaskIdsToDelete.contains(pendingTaskId)) {
      pendingTaskIdsToDelete.remove(pendingTaskId);
    }
    pendingTaskIdToPendingTask.remove(pendingTaskId);
  }

  public Optional<SingularityPendingTask> getPendingTask(SingularityPendingTaskId pendingTaskId) {
    return Optional.fromNullable(pendingTaskIdToPendingTask.get(pendingTaskId));
  }

  public void savePendingTask(SingularityPendingTask pendingTask) {
    if (!leader) {
      LOG.warn("savePendingTask {}, but not leader", pendingTask);
      return;
    }

    pendingTaskIdToPendingTask.put(pendingTask.getPendingTaskId(), pendingTask);
  }

  public void deleteActiveTaskId(SingularityTaskId taskId) {
    if (!leader) {
      LOG.warn("deleteActiveTask {}, but not leader", taskId);
      return;
    }

    activeTaskIds.remove(taskId);
  }

  public List<SingularityTaskId> exists(List<SingularityTaskId> taskIds) {
    return taskIds.stream()
        .filter(this.activeTaskIds::contains)
        .collect(Collectors.toList());
  }

  public List<SingularityTaskId> getActiveTaskIds() {
    return new ArrayList<>(activeTaskIds);
  }

  public List<SingularityTaskId> getActiveTaskIdsForRequest(String requestId) {
    Set<SingularityTaskId> allActiveTaskIds;
    synchronized (activeTaskIds) {
      allActiveTaskIds = new HashSet<>(activeTaskIds);
    }
    return allActiveTaskIds.stream()
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

  public boolean isActiveTask(SingularityTaskId taskId) {
    return activeTaskIds.contains(taskId);
  }

  public void putActiveTask(SingularityTaskId taskId) {
    if (!leader) {
      LOG.warn("putActiveTask {}, but not leader", taskId);
      return;
    }

    activeTaskIds.add(taskId);
  }

  public List<SingularityRequestWithState> getRequests() {
    return new ArrayList<>(requests.values());
  }

  public Optional<SingularityRequestWithState> getRequest(String requestId) {
    return Optional.fromNullable(requests.get(requestId));
  }

  public void putRequest(SingularityRequestWithState requestWithState) {
    if (!leader) {
      LOG.warn("putRequest {}, but not leader", requestWithState.getRequest().getId());
      return;
    }

    requests.put(requestWithState.getRequest().getId(), requestWithState);
  }

  public void deleteRequest(String reqeustId) {
    if (!leader) {
      LOG.warn("deleteRequest {}, but not leader", reqeustId);
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
    if (!leader) {
      LOG.warn("deleteTaskCleanup {}, but not leader", taskId);
      return;
    }

    cleanupTasks.remove(taskId);
  }

  public void saveTaskCleanup(SingularityTaskCleanup cleanup) {
    if (!leader) {
      LOG.warn("saveTaskCleanup {}, but not leader", cleanup);
      return;
    }

    cleanupTasks.put(cleanup.getTaskId(), cleanup);
  }

  public void createTaskCleanupIfNotExists(SingularityTaskCleanup cleanup) {
    if (!leader) {
      LOG.warn("createTaskCleanupIfNotExists {}, but not leader", cleanup);
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
    Map<String, SingularityRequestDeployState> allDeployStates = new HashMap<>(requestIdToDeployState);
    return allDeployStates.entrySet().stream()
        .filter((e) -> requestIds.contains(e.getKey()))
        .collect(Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())
    );
  }

  public void deleteRequestDeployState(String requestId) {
    if (!leader) {
      LOG.warn("deleteRequestDeployState {}, but not leader", requestId);
      return;
    }

    requestIdToDeployState.remove(requestId);
  }

  public void putRequestDeployState(SingularityRequestDeployState requestDeployState) {
    if (!leader) {
      LOG.warn("putRequestDeployState {}, but not leader", requestDeployState.getRequestId());
      return;
    }

    requestIdToDeployState.put(requestDeployState.getRequestId(), requestDeployState);
  }

  public List<SingularityKilledTaskIdRecord> getKilledTasks() {
    return new ArrayList<>(killedTasks.values());
  }

  public void addKilledTask(SingularityKilledTaskIdRecord killedTask) {
    if (!leader) {
      LOG.warn("addKilledTask {}, but not leader", killedTask.getTaskId().getId());
      return;
    }
    killedTasks.put(killedTask.getTaskId(), killedTask);
  }

  public void deleteKilledTask(SingularityTaskId killedTaskId) {
    if (!leader) {
      LOG.warn("deleteKilledTask {}, but not leader", killedTaskId.getId());
      return;
    }
    killedTasks.remove(killedTaskId);
  }

  public List<SingularityTaskHistoryUpdate> getTaskHistoryUpdates(SingularityTaskId taskId) {
    List<SingularityTaskHistoryUpdate> updates = new ArrayList<>(Optional.fromNullable(historyUpdates.get(taskId).getHistoryUpdates()).or(new HashMap<>()).values());
    Collections.sort(updates);
    return updates;
  }

  public Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> getTaskHistoryUpdates(Collection<SingularityTaskId> taskIds) {
    Map<SingularityTaskId, SingularityHistoryUpdates> allHistoryUpdates = new HashMap<>(historyUpdates);
    return allHistoryUpdates.entrySet().stream()
        .filter((e) -> taskIds.contains(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, (e) -> new ArrayList<>(e.getValue().getHistoryUpdates().values()))
    );
  }

  public void saveTaskHistoryUpdate(SingularityTaskHistoryUpdate taskHistoryUpdate, boolean overwrite) {
    if (!leader) {
      LOG.warn("saveTaskHistoryUpdate {}, but not leader", taskHistoryUpdate);
      return;
    }
    historyUpdates.putIfAbsent(taskHistoryUpdate.getTaskId(), new SingularityHistoryUpdates(new HashMap<>()));
    if (overwrite) {
      historyUpdates.get(taskHistoryUpdate.getTaskId()).getHistoryUpdates().put(taskHistoryUpdate.getTaskState(), taskHistoryUpdate);
    } else {
      historyUpdates.get(taskHistoryUpdate.getTaskId()).getHistoryUpdates().putIfAbsent(taskHistoryUpdate.getTaskState(), taskHistoryUpdate);
    }
  }

  public void deleteTaskHistoryUpdate(SingularityTaskId taskId, ExtendedTaskState state) {
    if (!leader) {
      LOG.warn("deleteTaskHistoryUpdate {}, but not leader", taskId);
      return;
    }
    historyUpdates.getOrDefault(taskId, new SingularityHistoryUpdates(new HashMap<>())).getHistoryUpdates().remove(state);
  }

  public void deleteTaskHistory(SingularityTaskId taskId) {
    if (!leader) {
      LOG.warn("deleteTaskHistory {}, but not leader", taskId);
      return;
    }
    historyUpdates.remove(taskId);
  }

  public List<SingularitySlave> getSlaves() {
    return new ArrayList<>(slaves.values());
  }

  public Optional<SingularitySlave> getSlave(String slaveId) {
    return Optional.fromNullable(slaves.get(slaveId));
  }

  public void putSlave(SingularitySlave slave) {
    if (!leader) {
      LOG.warn("putSlave {}, but not leader", slave);
    }

    slaves.put(slave.getId(), slave);
  }

  public void removeSlave(String slaveId) {
    if (!leader) {
      LOG.warn("remove slave {}, but not leader", slaveId);
      return;
    }
    slaves.remove(slaveId);
  }

  public List<SingularityRack> getRacks() {
    return new ArrayList<>(racks.values());
  }

  public Optional<SingularityRack> getRack(String rackId) {
    return Optional.fromNullable(racks.get(rackId));
  }

  public void putRack(SingularityRack rack) {
    if (!leader) {
      LOG.warn("putSlave {}, but not leader", rack);
    }

    racks.put(rack.getId(), rack);
  }

  public void removeRack(String rackId) {
    if (!leader) {
      LOG.warn("remove rack {}, but not leader", rackId);
      return;
    }
    racks.remove(rackId);
  }

  public void putRequestUtilization(RequestUtilization requestUtilization) {
    if (!leader) {
      LOG.warn("putRequestUtilization {}, but not leader", requestUtilization);
    }

    requestUtilizations.put(requestUtilization.getRequestId(), requestUtilization);
  }

  public void removeRequestUtilization(String requestId) {
    if (!leader) {
      LOG.warn("removeRequestUtilization {}, but not leader", requestId);
      return;
    }
    requestUtilizations.remove(requestId);
  }

  public Map<String, RequestUtilization> getRequestUtilizations() {
    return new HashMap<>(requestUtilizations);
  }

  public void putSlaveUsage(SingularitySlaveUsageWithId slaveUsage) {
    if (!leader) {
      LOG.warn("putSlaveUsage {}, but not leader", slaveUsage);
    }

    slaveUsages.put(slaveUsage.getSlaveId(), slaveUsage);
  }

  public void removeSlaveUsage(String slaveId) {
    if (!leader) {
      LOG.warn("removeSlaveUsage {}, but not leader", slaveId);
      return;
    }
    slaveUsages.remove(slaveId);
  }

  public Map<String, SingularitySlaveUsageWithId> getSlaveUsages() {
    return new HashMap<>(slaveUsages);
  }

  public Optional<SingularitySlaveUsageWithId> getSlaveUsage(String slaveId) {
    return Optional.fromNullable(slaveUsages.get(slaveId));
  }
}
