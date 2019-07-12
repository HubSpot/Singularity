package com.hubspot.singularity.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityDeploy;
import com.hubspot.singularity.SingularityDeployKey;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityRequestDeployState;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.SingularityRequestWithState;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularitySlaveUsageWithId;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.CacheConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;

import io.atomix.core.Atomix;
import io.atomix.core.map.DistributedMap;
import io.atomix.core.set.DistributedSet;
import io.atomix.core.value.AtomicValue;

@Singleton
public class SingularityCache {
  private static final Logger LOG = LoggerFactory.getLogger(SingularityCache.class);
  private static final Timer TIMER = new Timer();

  private final CacheUtils cacheUtils;
  private final AtomixProvider atomixProvider;
  private final CacheConfiguration cacheConfiguration;
  private final Cache<SingularityTaskId, SingularityTask> taskCache;
  private final Cache<SingularityDeployKey, SingularityDeploy> deployCache;

  private Atomix atomix;
  private DistributedMap<SingularityPendingTaskId, SingularityPendingTask> pendingTaskIdToPendingTask;
  private DistributedSet<SingularityTaskId> activeTaskIds;
  private DistributedMap<String, SingularityRequestWithState> requests;
  private DistributedMap<String, SingularityRequestGroup> requestGroups;
  private DistributedMap<SingularityTaskId, SingularityTaskCleanup> cleanupTasks;
  private DistributedMap<String, SingularityRequestDeployState> requestIdToDeployState;
  private DistributedMap<SingularityTaskId, SingularityKilledTaskIdRecord> killedTasks;
  private DistributedMap<SingularityTaskId, SingularityHistoryUpdates> historyUpdates;
  private DistributedMap<String, SingularitySlave> slaves;
  private DistributedMap<String, SingularityRack> racks;
  private DistributedSet<SingularityPendingTaskId> pendingTaskIdsToDelete;
  private DistributedMap<String, RequestUtilization> requestUtilizations;
  private DistributedMap<String, SingularitySlaveUsageWithId> slaveUsages;
  private AtomicValue<SingularityState> state;
  private AtomicValue<Long> lastUpdate;

  private final AtomicLong lastMeasuredLag;
  private final ReentrantLock startupLock;
  private volatile boolean leader;

  @Inject
  public SingularityCache(CacheUtils cacheUtils,
                          AtomixProvider atomixProvider,
                          SingularityConfiguration configuration) {
    this.leader = false;
    this.lastMeasuredLag = new AtomicLong(0);
    this.startupLock = new ReentrantLock();
    this.cacheUtils = cacheUtils;
    this.atomixProvider = atomixProvider;
    this.cacheConfiguration = configuration.getCacheConfiguration();
    this.taskCache = CacheBuilder.newBuilder()
        .maximumSize(cacheConfiguration.getTaskCacheMaxSize())
        .concurrencyLevel(2)
        .initialCapacity(cacheConfiguration.getTaskCacheInitialSize())
        .expireAfterAccess(cacheConfiguration.getCacheTasksForMillis(), TimeUnit.MILLISECONDS)
        .build();
    this.deployCache = CacheBuilder.newBuilder()
        .maximumSize(cacheConfiguration.getTaskCacheMaxSize())
        .concurrencyLevel(2)
        .initialCapacity(cacheConfiguration.getTaskCacheInitialSize())
        .expireAfterAccess(cacheConfiguration.getCacheTasksForMillis(), TimeUnit.MILLISECONDS)
        .build();
  }

  public void setup() throws Exception {
    startupLock.lock();
    try {
      if (atomix != null) {
        LOG.debug("Atomix setup already finished on another thread");
        return;
      }
      LOG.debug("Starting atomix");
      atomix = atomixProvider.get();
      atomix.start().get(cacheConfiguration.getAtomixStartTimeoutSeconds(), TimeUnit.SECONDS);
      this.pendingTaskIdToPendingTask = cacheUtils.newAtomixMap(
          atomix,
          "pendingTaskIdToPendingTask",
          SingularityPendingTaskId.class,
          SingularityPendingTask.class,
          cacheConfiguration.getPendingTaskCacheSize());
      this.activeTaskIds = cacheUtils.newAtomixSet(
          atomix,
          "activeTaskIds",
          SingularityTaskId.class
      );
      this.requests = cacheUtils.newAtomixMap(
          atomix,
          "requests",
          String.class,
          SingularityRequestWithState.class,
          cacheConfiguration.getRequestCacheSize());
      this.requestGroups = cacheUtils.newAtomixMap(
          atomix,
          "requestGroups",
          String.class,
          SingularityRequestGroup.class,
          cacheConfiguration.getRequestCacheSize());
      this.cleanupTasks = cacheUtils.newAtomixMap(
          atomix,
          "cleanupTasks",
          SingularityTaskId.class,
          SingularityTaskCleanup.class,
          cacheConfiguration.getCleanupTasksCacheSize());
      this.requestIdToDeployState = cacheUtils.newAtomixMap(
          atomix,
          "requestIdToDeployState",
          String.class,
          SingularityRequestDeployState.class,
          cacheConfiguration.getRequestCacheSize());
      this.killedTasks = cacheUtils.newAtomixMap(
          atomix,
          "killedTasks",
          SingularityTaskId.class,
          SingularityKilledTaskIdRecord.class,
          cacheConfiguration.getRequestCacheSize());
      this.historyUpdates = cacheUtils.newAtomixMap(
          atomix,
          "historyUpdates",
          SingularityTaskId.class,
          SingularityHistoryUpdates.class,
          cacheConfiguration.getHistoryUpdateCacheSize());
      this.slaves = cacheUtils.newAtomixMap(
          atomix,
          "slaves",
          String.class,
          SingularitySlave.class,
          cacheConfiguration.getSlaveCacheSize());
      this.racks = cacheUtils.newAtomixMap(
          atomix,
          "racks",
          String.class,
          SingularityRack.class,
          cacheConfiguration.getRackCacheSize());
      this.pendingTaskIdsToDelete = cacheUtils.newAtomixSet(
          atomix,
          "pendingTaskIdsToDelete",
          SingularityPendingTaskId.class
      );
      this.requestUtilizations = cacheUtils.newAtomixMap(
          atomix,
          "requestUtilizations",
          String.class,
          RequestUtilization.class,
          cacheConfiguration.getRequestCacheSize());
      this.slaveUsages = cacheUtils.newAtomixMap(
          atomix,
          "slaveUsages",
          String.class,
          SingularitySlaveUsageWithId.class,
          cacheConfiguration.getSlaveCacheSize());
      this.state = cacheUtils.newAtomicValue(atomix, "state", SingularityState.class);
      this.lastUpdate = cacheUtils.newAtomicValue(atomix, "lastUpdate", Long.class);
      lastUpdate.addListener((update) -> lastMeasuredLag.set(System.currentTimeMillis() - update.newValue()));
    } finally {
      startupLock.unlock();
    }
  }

  void markLeader() {
    leader = true;
    TIMER.schedule(new TimerTask() {
      @Override
      public void run() {
        lastUpdate.set(System.currentTimeMillis());
      }
    }, 0L, 1000L);
  }

  void markNotLeader() {
    leader = false;
  }

  // FOR TESTING ONLY!!!!!
  public void clear() {
    pendingTaskIdToPendingTask.clear();
    activeTaskIds.clear();
    requests.clear();
    requestGroups.clear();
    cleanupTasks.clear();
    requestIdToDeployState.clear();
    killedTasks.clear();
    historyUpdates.clear();
    slaves.clear();
    racks.clear();
    pendingTaskIdsToDelete.clear();
    requestUtilizations.clear();
    slaveUsages.clear();
    // state.clear()
    deployCache.invalidateAll();
    taskCache.invalidateAll();
  }

  public void close() {
    leader = false;
    TIMER.cancel();
    pendingTaskIdToPendingTask.close();
    activeTaskIds.close();
    requests.close();
    requestGroups.close();
    cleanupTasks.close();
    requestIdToDeployState.close();
    killedTasks.close();
    historyUpdates.close();
    slaves.close();
    racks.close();
    pendingTaskIdsToDelete.close();
    requestUtilizations.close();
    slaveUsages.close();
    state.close();
    atomix.stop().join();
  }

  public boolean isLeader() {
    return leader;
  }

  public long getLag() {
    return leader ? 0 : lastMeasuredLag.get();
  }

  // Loading in initial data. Sync entries of the maps to avoid the extra network caused by clear + putAll
  public void cachePendingTasks(List<SingularityPendingTask> pendingTasks) {
    CacheUtils.syncMaps(
        pendingTaskIdToPendingTask,
        pendingTasks.stream().collect(Collectors.toMap(SingularityPendingTask::getPendingTaskId, Function.identity()))
    );
  }

  public void cachePendingTasksToDelete(List<SingularityPendingTaskId> pendingTaskIds) {
    CacheUtils.syncCollections(pendingTaskIdsToDelete, pendingTaskIds);
  }

  public void cacheActiveTaskIds(List<SingularityTaskId> active) {
    CacheUtils.syncCollections(activeTaskIds, active);
  }

  public void cacheRequests(List<SingularityRequestWithState> requestsWithState) {
    CacheUtils.syncMaps(
        requests,
        requestsWithState.stream().collect(Collectors.toMap((r) -> r.getRequest().getId(), Function.identity()))
    );
  }

  public void cacheRequestGroups(List<SingularityRequestGroup> requestGroups) {
    CacheUtils.syncMaps(
        this.requestGroups,
        requestGroups.stream().collect(Collectors.toMap(SingularityRequestGroup::getId, Function.identity()))
    );
  }

  public void cacheCleanupTasks(List<SingularityTaskCleanup> cleanups) {
    CacheUtils.syncMaps(
        cleanupTasks,
        cleanups.stream().collect(Collectors.toMap(SingularityTaskCleanup::getTaskId, Function.identity()))
    );
  }

  public void cacheRequestDeployStates(Map<String, SingularityRequestDeployState> requestDeployStates) {
    CacheUtils.syncMaps(requestIdToDeployState, requestDeployStates);
  }

  public void cacheKilledTasks(List<SingularityKilledTaskIdRecord> killed) {
    CacheUtils.syncMaps(
        killedTasks,
        killed.stream().collect(Collectors.toMap(SingularityKilledTaskIdRecord::getTaskId, Function.identity()))
    );
  }

  public void cacheTaskHistoryUpdates(Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> historyUpdates) {
    CacheUtils.syncMaps(
        this.historyUpdates,
        historyUpdates.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                (e) -> new SingularityHistoryUpdates(e.getValue().stream()
                    .collect(Collectors.toMap(SingularityTaskHistoryUpdate::getTaskState, Function.identity())))
            )));
  }

  public void cacheSlaves(List<SingularitySlave> slaves) {
    CacheUtils.syncMaps(
        this.slaves,
        slaves.stream().collect(Collectors.toMap(SingularitySlave::getId, Function.identity()))
    );
  }

  public void cacheRacks(List<SingularityRack> racks) {
    CacheUtils.syncMaps(
        this.racks,
        racks.stream().collect(Collectors.toMap(SingularityRack::getId, Function.identity()))
    );
  }

  public void cacheRequestUtilizations(Map<String, RequestUtilization> requestUtilizations) {
    CacheUtils.syncMaps(this.requestUtilizations, requestUtilizations);
  }

  public void cacheSlaveUsages(Map<String, SingularitySlaveUsageWithId> slaveUsages) {
    CacheUtils.syncMaps(this.slaveUsages, slaveUsages);
  }

  // Methods to access the data
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
    return activeTaskIds.stream()
        .filter(t -> t.getRequestId().equals(requestId))
        .collect(Collectors.toList());
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

  public void deleteRequest(String requestId) {
    if (!leader) {
      LOG.warn("deleteRequest {}, but not leader", requestId);
      return;
    }

    requests.remove(requestId);
  }

  public List<SingularityRequestGroup> getRequestGroups() {
    return new ArrayList<>(requestGroups.values());
  }

  public Optional<SingularityRequestGroup> getRequestGroup(String id) {
    return Optional.fromNullable(requestGroups.get(id));
  }

  public void putRequestGroup(SingularityRequestGroup requestGroup) {
    if (!leader) {
      LOG.warn("putRequest {}, but not leader", requestGroup.getId());
      return;
    }

    requestGroups.put(requestGroup.getId(), requestGroup);
  }

  public void deleteRequestGroup(String id) {
    if (!leader) {
      LOG.warn("deleteRequest {}, but not leader", id);
      return;
    }

    requestGroups.remove(id);
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
    List<SingularityTaskHistoryUpdate> updates = new ArrayList<>(Optional.fromNullable(historyUpdates.get(taskId)).or(new SingularityHistoryUpdates(new HashMap<>())).getHistoryUpdates().values());
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
    Map<ExtendedTaskState, SingularityTaskHistoryUpdate> updates = historyUpdates.getOrDefault(taskHistoryUpdate.getTaskId(), new SingularityHistoryUpdates(new HashMap<>())).getHistoryUpdates();

    if (overwrite) {
      updates.put(taskHistoryUpdate.getTaskState(), taskHistoryUpdate);
    } else {
      updates.putIfAbsent(taskHistoryUpdate.getTaskState(), taskHistoryUpdate);
    }
    historyUpdates.put(taskHistoryUpdate.getTaskId(), new SingularityHistoryUpdates(updates));
  }

  public void deleteTaskHistoryUpdate(SingularityTaskId taskId, ExtendedTaskState state) {
    if (!leader) {
      LOG.warn("deleteTaskHistoryUpdate {}, but not leader", taskId);
      return;
    }
    Map<ExtendedTaskState, SingularityTaskHistoryUpdate> updates = historyUpdates.getOrDefault(taskId, new SingularityHistoryUpdates(new HashMap<>())).getHistoryUpdates();
    updates.remove(state);
    historyUpdates.put(taskId, new SingularityHistoryUpdates(updates));
  }

  public void deleteTaskHistory(SingularityTaskId taskId) {
    if (!leader) {
      LOG.warn("deleteTaskHistory {}, but not leader", taskId);
      return;
    }
    historyUpdates.remove(taskId);
    taskCache.invalidate(taskId);
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

  // Guava cached items
  public void putTask(SingularityTask task) {
    taskCache.put(task.getTaskId(), task);
  }

  public Optional<SingularityTask> getTask(SingularityTaskId taskId) {
    return Optional.fromNullable(taskCache.getIfPresent(taskId));
  }

  public Optional<SingularityTask> getTask(SingularityTaskId taskId, Function<SingularityTaskId, Optional<SingularityTask>> load) {
    SingularityTask maybeCached = taskCache.getIfPresent(taskId);
    if (maybeCached != null) {
      return Optional.of(maybeCached);
    }
    Optional<SingularityTask> fetched = load.apply(taskId);
    if (fetched.isPresent()) {
      taskCache.put(taskId, fetched.get());
    }
    return fetched;
  }

  public void putDeploy(SingularityDeploy deploy) {
    deployCache.put(new SingularityDeployKey(deploy.getRequestId(), deploy.getId()), deploy);
  }

  public Optional<SingularityDeploy> getDeploy(SingularityDeployKey deployKey) {
    return Optional.fromNullable(deployCache.getIfPresent(deployKey));
  }

  public Optional<SingularityDeploy> getDeploy(SingularityDeployKey deployKey, Function<SingularityDeployKey, Optional<SingularityDeploy>> load) {
    SingularityDeploy maybeCached = deployCache.getIfPresent(deployKey);
    if (maybeCached != null) {
      return Optional.of(maybeCached);
    }
    Optional<SingularityDeploy> fetched = load.apply(deployKey);
    if (fetched.isPresent()) {
      deployCache.put(deployKey, fetched.get());
    }
    return fetched;
  }

  public void setState(SingularityState state) {
    this.state.set(state);
  }

  public SingularityState getState() {
    return state.get();
  }
}
