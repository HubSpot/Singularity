package com.hubspot.singularity.data;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.mesos.Protos.TaskStatus;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.ExtendedTaskState;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityKilledTaskIdRecord;
import com.hubspot.singularity.SingularityLoadBalancerUpdate;
import com.hubspot.singularity.SingularityMainModule;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityKilledTaskIdRecordTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityLoadBalancerUpdateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityPendingTaskIdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskCleanupTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHealthcheckResultTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHistoryUpdateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskIdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskStatusTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskTranscoder;
import com.hubspot.singularity.data.transcoders.StringTranscoder;

@Singleton
public class TaskManager extends CuratorAsyncManager {

  private static final String TASKS_ROOT = "/tasks";

  private static final String ACTIVE_PATH_ROOT = TASKS_ROOT + "/active";
  private static final String LAST_ACTIVE_TASK_STATUSES_PATH_ROOT = TASKS_ROOT + "/statuses";
  private static final String PENDING_PATH_ROOT = TASKS_ROOT + "/scheduled";
  private static final String CLEANUP_PATH_ROOT = TASKS_ROOT + "/cleanup";
  private static final String LB_CLEANUP_PATH_ROOT = TASKS_ROOT + "/lbcleanup";
  private static final String DRIVER_KILLED_PATH_ROOT = TASKS_ROOT + "/killed";

  private static final String HISTORY_PATH_ROOT = TASKS_ROOT + "/history";

  private static final String LAST_HEALTHCHECK_KEY = "LAST_HEALTHCHECK";
  private static final String DIRECTORY_KEY = "DIRECTORY";
  private static final String TASK_KEY = "TASK";

  private static final String LOAD_BALANCER_PRE_KEY = "LOAD_BALANCER_";

  private static final String HEALTHCHECKS_PATH = "/healthchecks";
  private static final String UPDATES_PATH = "/updates";

  private final SingularityTaskHealthcheckResultTranscoder healthcheckResultTranscoder;
  private final SingularityTaskCleanupTranscoder taskCleanupTranscoder;
  private final SingularityTaskTranscoder taskTranscoder;
  private final SingularityTaskStatusTranscoder taskStatusTranscoder;
  private final SingularityTaskIdTranscoder taskIdTranscoder;
  private final SingularityKilledTaskIdRecordTranscoder killedTaskIdRecordTranscoder;
  private final SingularityPendingTaskIdTranscoder pendingTaskIdTranscoder;
  private final SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder;
  private final SingularityLoadBalancerUpdateTranscoder taskLoadBalancerUpdateTranscoder;
  private final Function<SingularityPendingTaskId, SingularityPendingTask> pendingTaskIdToPendingTaskFunction;
  private final WebhookManager webhookManager;
  private final String serverId;

  @Inject
  public TaskManager(SingularityConfiguration configuration, CuratorFramework curator, WebhookManager webhookManager, SingularityPendingTaskIdTranscoder pendingTaskIdTranscoder,
      SingularityTaskIdTranscoder taskIdTranscoder, SingularityLoadBalancerUpdateTranscoder taskLoadBalancerHistoryUpdateTranscoder, SingularityTaskStatusTranscoder taskStatusTranscoder,
      SingularityTaskHealthcheckResultTranscoder healthcheckResultTranscoder, SingularityTaskTranscoder taskTranscoder, SingularityTaskCleanupTranscoder taskCleanupTranscoder,
      SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder, SingularityKilledTaskIdRecordTranscoder killedTaskIdRecordTranscoder,
      @Named(SingularityMainModule.SERVER_ID_PROPERTY) String serverId) {
    super(curator, configuration.getZookeeperAsyncTimeout());

    this.healthcheckResultTranscoder = healthcheckResultTranscoder;
    this.taskTranscoder = taskTranscoder;
    this.taskStatusTranscoder = taskStatusTranscoder;
    this.killedTaskIdRecordTranscoder = killedTaskIdRecordTranscoder;
    this.taskCleanupTranscoder = taskCleanupTranscoder;
    this.taskHistoryUpdateTranscoder = taskHistoryUpdateTranscoder;
    this.taskIdTranscoder = taskIdTranscoder;
    this.pendingTaskIdTranscoder = pendingTaskIdTranscoder;
    this.taskLoadBalancerUpdateTranscoder = taskLoadBalancerHistoryUpdateTranscoder;
    this.webhookManager = webhookManager;

    this.serverId = serverId;

    this.pendingTaskIdToPendingTaskFunction = new Function<SingularityPendingTaskId, SingularityPendingTask>() {

      @Override
      public SingularityPendingTask apply(SingularityPendingTaskId input) {
        Optional<String> maybeCmdLineArgs = Optional.absent();

        if ((input.getPendingType() == PendingType.ONEOFF) || (input.getPendingType() == PendingType.IMMEDIATE)) {
          maybeCmdLineArgs = getStringData(ZKPaths.makePath(PENDING_PATH_ROOT, input.getId()));
        }

        return new SingularityPendingTask(input, maybeCmdLineArgs);
      }
    };
  }

  private String getLastHealthcheckPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), LAST_HEALTHCHECK_KEY);
  }

  private String getHealthcheckParentPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), HEALTHCHECKS_PATH);
  }

  private String getLastActiveTaskStatusPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(LAST_ACTIVE_TASK_STATUSES_PATH_ROOT, taskId.getId());
  }

  private String getHealthcheckPath(SingularityTaskHealthcheckResult healthcheck) {
    return ZKPaths.makePath(getHealthcheckParentPath(healthcheck.getTaskId()), Long.toString(healthcheck.getTimestamp()));
  }

  private String getTaskPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), TASK_KEY);
  }

  private String getLoadBalancerStatePath(SingularityTaskId taskId, LoadBalancerRequestType requestType) {
    return ZKPaths.makePath(getHistoryPath(taskId), LOAD_BALANCER_PRE_KEY + requestType.name());
  }

  private String getDirectoryPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), DIRECTORY_KEY);
  }

  private String getUpdatesPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), UPDATES_PATH);
  }

  private String getUpdatePath(SingularityTaskId taskId, ExtendedTaskState state) {
    return ZKPaths.makePath(getUpdatesPath(taskId), state.name());
  }

  private String getRequestPath(String requestId) {
    return ZKPaths.makePath(HISTORY_PATH_ROOT, requestId);
  }

  private String getHistoryPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getRequestPath(taskId.getRequestId()), taskId.getId());
  }

  private String getActivePath(String taskId) {
    return ZKPaths.makePath(ACTIVE_PATH_ROOT, taskId);
  }

  private String getPendingPath(SingularityPendingTaskId pendingTaskId) {
    return ZKPaths.makePath(PENDING_PATH_ROOT, pendingTaskId.getId());
  }

  private String getCleanupPath(String taskId) {
    return ZKPaths.makePath(CLEANUP_PATH_ROOT, taskId);
  }

  public int getNumCleanupTasks() {
    return getNumChildren(CLEANUP_PATH_ROOT);
  }

  public int getNumLbCleanupTasks() {
    return getNumChildren(LB_CLEANUP_PATH_ROOT);
  }

  public int getNumActiveTasks() {
    return getNumChildren(ACTIVE_PATH_ROOT);
  }

  public int getNumScheduledTasks() {
    return getNumChildren(PENDING_PATH_ROOT);
  }

  public void saveLoadBalancerState(SingularityTaskId taskId, LoadBalancerRequestType requestType, SingularityLoadBalancerUpdate lbUpdate) {
    Preconditions.checkState(requestType != LoadBalancerRequestType.DEPLOY);

    save(getLoadBalancerStatePath(taskId, requestType), lbUpdate, taskLoadBalancerUpdateTranscoder);
  }

  public void saveTaskDirectory(SingularityTaskId taskId, String directory) {
    save(getDirectoryPath(taskId), Optional.of(JavaUtils.toBytes(directory)));
  }

  public void saveLastActiveTaskStatus(SingularityTaskStatusHolder taskStatus) {
    save(getLastActiveTaskStatusPath(taskStatus.getTaskId()), taskStatus, taskStatusTranscoder);
  }

  public Optional<String> getDirectory(SingularityTaskId taskId) {
    return getData(getDirectoryPath(taskId), StringTranscoder.STRING_TRANSCODER);
  }

  public void saveHealthcheckResult(SingularityTaskHealthcheckResult healthcheckResult) {
    final Optional<byte[]> bytes = Optional.of(healthcheckResultTranscoder.toBytes(healthcheckResult));

    save(getHealthcheckPath(healthcheckResult), bytes);
    save(getLastHealthcheckPath(healthcheckResult.getTaskId()), bytes);
  }

  public void createPendingTasks(List<SingularityPendingTask> tasks) {
    try {
      for (SingularityPendingTask task : tasks) {
        createPendingTask(task);
      }
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void createPendingTask(SingularityPendingTask task) throws Exception {
    final String pendingPath = getPendingPath(task.getPendingTaskId());

    Optional<byte[]> data = Optional.absent();

    if (task.getMaybeCmdLineArgs().isPresent()) {
      data = Optional.of(JavaUtils.toBytes(task.getMaybeCmdLineArgs().get()));
    }

    create(pendingPath, data);
  }

  public List<SingularityTaskId> getAllTaskIds() {
    final List<String> requestIds = getChildren(HISTORY_PATH_ROOT);
    final List<String> paths = Lists.newArrayListWithCapacity(requestIds.size());

    for (String requestId : requestIds) {
      paths.add(getRequestPath(requestId));
    }

    return getChildrenAsIdsForParents(HISTORY_PATH_ROOT, paths, taskIdTranscoder);
  }

  private List<SingularityTaskId> getTaskIds(String root) {
    return getChildrenAsIds(root, taskIdTranscoder);
  }

  public List<SingularityTaskId> getActiveTaskIds() {
    return getTaskIds(ACTIVE_PATH_ROOT);
  }

  public List<SingularityTaskId> getCleanupTaskIds() {
    return getTaskIds(CLEANUP_PATH_ROOT);
  }

  public List<SingularityTaskCleanup> getCleanupTasks() {
    return getAsyncChildren(CLEANUP_PATH_ROOT, taskCleanupTranscoder);
  }

  public List<SingularityTask> getActiveTasks() {
    return getAsyncChildren(ACTIVE_PATH_ROOT, taskTranscoder);
  }

  public List<SingularityTaskStatusHolder> getLastActiveTaskStatuses() {
    return getAsyncChildren(LAST_ACTIVE_TASK_STATUSES_PATH_ROOT, taskStatusTranscoder);
  }

  public Optional<SingularityTaskStatusHolder> getLastActiveTaskStatus(SingularityTaskId taskId) {
    return getData(getLastActiveTaskStatusPath(taskId), taskStatusTranscoder);
  }

  public List<SingularityTaskStatusHolder> getLastActiveTaskStatusesFor(Collection<SingularityTaskId> activeTaskIds) {
    List<String> paths = Lists.newArrayListWithExpectedSize(activeTaskIds.size());
    for (SingularityTaskId taskId : activeTaskIds) {
      paths.add(getLastActiveTaskStatusPath(taskId));
    }
    return getAsync(LAST_ACTIVE_TASK_STATUSES_PATH_ROOT, paths, taskStatusTranscoder);
  }

  public List<SingularityTask> getTasksOnSlave(List<SingularityTaskId> activeTaskIds, SingularitySlave slave) {
    List<SingularityTask> tasks = Lists.newArrayList();

    for (SingularityTaskId activeTaskId : activeTaskIds) {
      if (activeTaskId.getHost().equals(slave.getHost())) {
        Optional<SingularityTask> maybeTask = getActiveTask(activeTaskId.getId());
        if (maybeTask.isPresent() && slave.getId().equals(maybeTask.get().getOffer().getSlaveId().getValue())) {
          tasks.add(maybeTask.get());
        }
      }
    }

    return tasks;
  }

  public List<SingularityTaskHistoryUpdate> getTaskHistoryUpdates(SingularityTaskId taskId) {
    List<SingularityTaskHistoryUpdate> updates = getAsyncChildren(getUpdatesPath(taskId), taskHistoryUpdateTranscoder);
    Collections.sort(updates);
    return updates;
  }

  public Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> getTaskHistoryUpdates(Collection<SingularityTaskId> taskIds) {
    Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> map = Maps.newHashMapWithExpectedSize(taskIds.size());

    for (SingularityTaskId taskId : taskIds) {
      map.put(taskId, getTaskHistoryUpdates(taskId));
    }

    return map;
  }

  public List<SingularityTaskHealthcheckResult> getHealthcheckResults(SingularityTaskId taskId) {
    List<SingularityTaskHealthcheckResult> healthcheckResults = getAsyncChildren(getHealthcheckParentPath(taskId), healthcheckResultTranscoder);
    Collections.sort(healthcheckResults);
    return healthcheckResults;
  }

  public Optional<SingularityTaskHealthcheckResult> getLastHealthcheck(SingularityTaskId taskId) {
    return getData(getLastHealthcheckPath(taskId), healthcheckResultTranscoder);
  }

  public Map<SingularityTaskId, SingularityTaskHealthcheckResult> getLastHealthcheck(Collection<SingularityTaskId> taskIds) {
    List<String> paths = Lists.newArrayListWithCapacity(taskIds.size());
    for (SingularityTaskId taskId : taskIds) {
      paths.add(getLastHealthcheckPath(taskId));
    }

    List<SingularityTaskHealthcheckResult> healthcheckResults = getAsync("healthchecks_by_ids", paths, healthcheckResultTranscoder);

    return Maps.uniqueIndex(healthcheckResults, healthcheckResultTranscoder);
  }

  public SingularityCreateResult saveTaskHistoryUpdate(SingularityTaskHistoryUpdate taskHistoryUpdate) {
    webhookManager.enqueueTaskUpdate(taskHistoryUpdate);

    return create(getUpdatePath(taskHistoryUpdate.getTaskId(), taskHistoryUpdate.getTaskState()), taskHistoryUpdate, taskHistoryUpdateTranscoder);
  }

  public boolean isActiveTask(String taskId) {
    final String path = getActivePath(taskId);

    return exists(path);
  }

  public List<SingularityTaskId> getTaskIdsForRequest(String requestId) {
    return getChildrenAsIds(getRequestPath(requestId), taskIdTranscoder);
  }

  private enum TaskFilter {
    ACTIVE, INACTIVE;
  }

  public List<SingularityTaskId> getInactiveTaskIdsForRequest(String requestId) {
    return getTaskIdsForRequest(requestId, TaskFilter.INACTIVE);
  }

  public List<SingularityTaskId> getActiveTaskIdsForRequest(String requestId) {
    return getTaskIdsForRequest(requestId, TaskFilter.ACTIVE);
  }

  public List<SingularityTaskId> filterActiveTaskIds(List<SingularityTaskId> taskIds) {
    final List<String> paths = Lists.newArrayListWithCapacity(taskIds.size());

    for (SingularityTaskId taskId : taskIds) {
      paths.add(getActivePath(taskId.getId()));
    }

    final List<SingularityTaskId> activeTaskIds = exists(ACTIVE_PATH_ROOT, paths, taskIdTranscoder);

    return activeTaskIds;
  }

  private List<SingularityTaskId> getTaskIdsForRequest(String requestId, TaskFilter taskFilter) {
    final List<SingularityTaskId> requestTaskIds = getTaskIdsForRequest(requestId);
    final List<SingularityTaskId> activeTaskIds = filterActiveTaskIds(requestTaskIds);

    if (taskFilter == TaskFilter.ACTIVE) {
      return activeTaskIds;
    }

    Iterables.removeAll(requestTaskIds, activeTaskIds);

    return requestTaskIds;
  }

  public Optional<SingularityTaskHistory> getTaskHistory(SingularityTaskId taskId) {
    final Optional<SingularityTask> task = getTask(taskId);

    if (!task.isPresent()) {
      return Optional.absent();
    }

    List<SingularityTaskHistoryUpdate> taskUpdates = getTaskHistoryUpdates(taskId);
    Optional<String> directory = getDirectory(taskId);
    List<SingularityTaskHealthcheckResult> healthchecks = getHealthcheckResults(taskId);

    List<SingularityLoadBalancerUpdate> loadBalancerUpdates = Lists.newArrayListWithCapacity(2);

    checkLoadBalancerHistory(loadBalancerUpdates, taskId, LoadBalancerRequestType.ADD);
    checkLoadBalancerHistory(loadBalancerUpdates, taskId, LoadBalancerRequestType.REMOVE);

    return Optional.of(new SingularityTaskHistory(taskUpdates, directory, healthchecks, task.get(), loadBalancerUpdates));
  }

  private void checkLoadBalancerHistory(List<SingularityLoadBalancerUpdate> loadBalancerUpdates, SingularityTaskId taskId, LoadBalancerRequestType lbRequestType) {
    Optional<SingularityLoadBalancerUpdate> lbHistory = getLoadBalancerState(taskId, lbRequestType);

    if (lbHistory.isPresent()) {
      loadBalancerUpdates.add(lbHistory.get());
    }
  }

  public Optional<SingularityLoadBalancerUpdate> getLoadBalancerState(SingularityTaskId taskId, LoadBalancerRequestType requestType) {
    return getData(getLoadBalancerStatePath(taskId, requestType), taskLoadBalancerUpdateTranscoder);
  }

  public SingularityPendingTask getPendingTask(SingularityPendingTaskId pendingTaskId) {
    return pendingTaskIdToPendingTaskFunction.apply(pendingTaskId);
  }

  public Optional<SingularityTask> getActiveTask(String taskId) {
    final String path = getActivePath(taskId);

    return getData(path, taskTranscoder);
  }

  public Optional<SingularityTask> getTask(SingularityTaskId taskId) {
    final String path = getTaskPath(taskId);

    return getData(path, taskTranscoder);
  }

  public List<SingularityPendingTaskId> getPendingTaskIds() {
    return getChildrenAsIds(PENDING_PATH_ROOT, pendingTaskIdTranscoder);
  }

  public List<SingularityPendingTask> getPendingTasks() {
    return Lists.transform(getPendingTaskIds(), pendingTaskIdToPendingTaskFunction);
  }

  public void createTaskAndDeletePendingTask(SingularityTask task) {
    try {
      createTaskAndDeletePendingTaskPrivate(task);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  public Map<SingularityTaskId, SingularityTask> getTasks(Iterable<SingularityTaskId> taskIds) {
    final List<String> paths = Lists.newArrayList();

    for (SingularityTaskId taskId : taskIds) {
      paths.add(getTaskPath(taskId));
    }

    return Maps.uniqueIndex(getAsync("tasks_by_ids", paths, taskTranscoder), taskTranscoder);
  }

  private void createTaskAndDeletePendingTaskPrivate(SingularityTask task) throws Exception {
    final String scheduledPath = getPendingPath(task.getTaskRequest().getPendingTask().getPendingTaskId());
    final String activePath = getActivePath(task.getTaskId().getId());

    curator.delete().forPath(scheduledPath);

    final byte[] data = taskTranscoder.toBytes(task);

    // TODO - right now, for consistency, we double write this. should review this and check for what happens in failure cases

    final long now = System.currentTimeMillis();

    saveTaskHistoryUpdate(new SingularityTaskHistoryUpdate(task.getTaskId(), now, ExtendedTaskState.TASK_LAUNCHED, Optional.<String> absent()));
    saveLastActiveTaskStatus(new SingularityTaskStatusHolder(task.getTaskId(), Optional.<TaskStatus> absent(), now, serverId));

    curator.create().creatingParentsIfNeeded().forPath(getTaskPath(task.getTaskId()), data);
    curator.create().creatingParentsIfNeeded().forPath(activePath, data);
  }

  public List<SingularityTaskId> getLBCleanupTasks() {
    return getChildrenAsIds(LB_CLEANUP_PATH_ROOT, taskIdTranscoder);
  }

  private String getLBCleanupPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(LB_CLEANUP_PATH_ROOT, taskId.getId());
  }

  private String getKilledPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(DRIVER_KILLED_PATH_ROOT, taskId.getId());
  }

  public SingularityDeleteResult deleteLBCleanupTask(SingularityTaskId taskId) {
    return delete(getLBCleanupPath(taskId));
  }

  public SingularityCreateResult createLBCleanupTask(SingularityTaskId taskId) {
    return create(getLBCleanupPath(taskId));
  }

  public SingularityCreateResult saveKilledRecord(SingularityKilledTaskIdRecord killedTaskIdRecord) {
    return save(getKilledPath(killedTaskIdRecord.getTaskId()), killedTaskIdRecord, killedTaskIdRecordTranscoder);
  }

  public List<SingularityKilledTaskIdRecord> getKilledTaskIdRecords() {
    return getAsyncChildren(DRIVER_KILLED_PATH_ROOT, killedTaskIdRecordTranscoder);
  }

  public SingularityDeleteResult deleteKilledRecord(SingularityTaskId taskId) {
    return delete(getKilledPath(taskId));
  }

  public SingularityDeleteResult deleteLastActiveTaskStatus(SingularityTaskId taskId) {
    return delete(getLastActiveTaskStatusPath(taskId));
  }

  public SingularityCreateResult createCleanupTask(SingularityTaskCleanup cleanupTask) {
    StringBuilder msg = new StringBuilder(cleanupTask.getCleanupType().name());

    if (cleanupTask.getUser().isPresent()) {
      msg.append(" - ");
      msg.append(cleanupTask.getUser().get());
    }

    saveTaskHistoryUpdate(new SingularityTaskHistoryUpdate(cleanupTask.getTaskId(), cleanupTask.getTimestamp(), ExtendedTaskState.TASK_CLEANING, Optional.of(msg.toString())));

    return create(getCleanupPath(cleanupTask.getTaskId().getId()), cleanupTask, taskCleanupTranscoder);
  }

  public void deleteActiveTask(String taskId) {
    delete(getActivePath(taskId));
  }

  public void deletePendingTask(SingularityPendingTaskId pendingTaskId) {
    delete(getPendingPath(pendingTaskId));
  }

  public void deleteCleanupTask(String taskId) {
    delete(getCleanupPath(taskId));
  }

  public void deleteTaskHistory(SingularityTaskId taskId) {
    try {
      curator.delete().deletingChildrenIfNeeded().forPath(getHistoryPath(taskId));
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

}
