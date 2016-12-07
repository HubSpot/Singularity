package com.hubspot.singularity.data;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorTransactionFinal;
import org.apache.curator.utils.ZKPaths;
import org.apache.mesos.Protos.TaskStatus;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
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
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.SingularityTaskIdHolder;
import com.hubspot.singularity.SingularityTaskMetadata;
import com.hubspot.singularity.SingularityTaskShellCommandHistory;
import com.hubspot.singularity.SingularityTaskShellCommandRequest;
import com.hubspot.singularity.SingularityTaskShellCommandRequestId;
import com.hubspot.singularity.SingularityTaskShellCommandUpdate;
import com.hubspot.singularity.SingularityTaskStatusHolder;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.IdTranscoder;
import com.hubspot.singularity.data.transcoders.StringTranscoder;
import com.hubspot.singularity.data.transcoders.Transcoder;
import com.hubspot.singularity.event.SingularityEventListener;

@Singleton
public class TaskManager extends CuratorAsyncManager {

  private static final Logger LOG = LoggerFactory.getLogger(CuratorAsyncManager.class);

  private static final String TASKS_ROOT = "/tasks";

  private static final String ACTIVE_PATH_ROOT = TASKS_ROOT + "/active";
  private static final String LAST_ACTIVE_TASK_STATUSES_PATH_ROOT = TASKS_ROOT + "/statuses";
  private static final String PENDING_PATH_ROOT = TASKS_ROOT + "/scheduled";
  private static final String CLEANUP_PATH_ROOT = TASKS_ROOT + "/cleanup";
  private static final String LB_CLEANUP_PATH_ROOT = TASKS_ROOT + "/lbcleanup";
  private static final String DRIVER_KILLED_PATH_ROOT = TASKS_ROOT + "/killed";
  private static final String FINISHED_TASK_MAIL_QUEUE = TASKS_ROOT + "/mailqueue";
  private static final String SHELL_REQUESTS_QUEUE_PATH_ROOT = TASKS_ROOT + "/shellqueue";

  private static final String HISTORY_PATH_ROOT = TASKS_ROOT + "/history";

  private static final String LAST_HEALTHCHECK_KEY = "LAST_HEALTHCHECK";
  private static final String DIRECTORY_KEY = "DIRECTORY";
  private static final String CONTAINER_ID_KEY = "CONTAINER_ID";
  private static final String TASK_KEY = "TASK";
  private static final String NOTIFIED_OVERDUE_TO_FINISH_KEY = "NOTIFIED_OVERDUE_TO_FINISH";

  private static final String LOAD_BALANCER_PRE_KEY = "LOAD_BALANCER_";

  private static final String SHELLS_PATH = "/shells";
  private static final String SHELL_REQUEST_KEY = "REQUEST";
  private static final String SHELL_UPDATES_PATH = "/updates";

  private static final String HEALTHCHECKS_PATH = "/healthchecks";
  private static final String HEALTHCHECKS_FINISHED_PATH = "/healthchecks-finished";
  private static final String STARTUP_HEALTHCHECK_PATH_SUFFIX = "-STARTUP";

  private static final String METADATA_PATH = "/metadata";
  private static final String UPDATES_PATH = "/updates";

  private final Transcoder<SingularityTaskHealthcheckResult> healthcheckResultTranscoder;
  private final Transcoder<SingularityTaskCleanup> taskCleanupTranscoder;
  private final Transcoder<SingularityTask> taskTranscoder;
  private final Transcoder<SingularityTaskStatusHolder> taskStatusTranscoder;
  private final Transcoder<SingularityKilledTaskIdRecord> killedTaskIdRecordTranscoder;
  private final Transcoder<SingularityTaskHistoryUpdate> taskHistoryUpdateTranscoder;
  private final Transcoder<SingularityLoadBalancerUpdate> taskLoadBalancerUpdateTranscoder;
  private final Transcoder<SingularityPendingTask> pendingTaskTranscoder;
  private final Transcoder<SingularityTaskShellCommandRequest> taskShellCommandRequestTranscoder;
  private final Transcoder<SingularityTaskShellCommandUpdate> taskShellCommandUpdateTranscoder;
  private final Transcoder<SingularityTaskMetadata> taskMetadataTranscoder;

  private final IdTranscoder<SingularityPendingTaskId> pendingTaskIdTranscoder;
  private final IdTranscoder<SingularityTaskId> taskIdTranscoder;

  private final ZkCache<SingularityTask> taskCache;

  private final SingularityEventListener singularityEventListener;
  private final String serverId;

  @Inject
  public TaskManager(CuratorFramework curator, SingularityConfiguration configuration, MetricRegistry metricRegistry, SingularityEventListener singularityEventListener,
      IdTranscoder<SingularityPendingTaskId> pendingTaskIdTranscoder, IdTranscoder<SingularityTaskId> taskIdTranscoder, Transcoder<SingularityLoadBalancerUpdate> taskLoadBalancerHistoryUpdateTranscoder,
      Transcoder<SingularityTaskStatusHolder> taskStatusTranscoder, Transcoder<SingularityTaskHealthcheckResult> healthcheckResultTranscoder, Transcoder<SingularityTask> taskTranscoder,
      Transcoder<SingularityTaskCleanup> taskCleanupTranscoder, Transcoder<SingularityTaskHistoryUpdate> taskHistoryUpdateTranscoder, Transcoder<SingularityPendingTask> pendingTaskTranscoder,
      Transcoder<SingularityKilledTaskIdRecord> killedTaskIdRecordTranscoder, Transcoder<SingularityTaskShellCommandRequest> taskShellCommandRequestTranscoder,
      Transcoder<SingularityTaskShellCommandUpdate> taskShellCommandUpdateTranscoder,  Transcoder<SingularityTaskMetadata> taskMetadataTranscoder,
      ZkCache<SingularityTask> taskCache, @Named(SingularityMainModule.SERVER_ID_PROPERTY) String serverId) {
    super(curator, configuration, metricRegistry);

    this.healthcheckResultTranscoder = healthcheckResultTranscoder;
    this.taskTranscoder = taskTranscoder;
    this.taskStatusTranscoder = taskStatusTranscoder;
    this.killedTaskIdRecordTranscoder = killedTaskIdRecordTranscoder;
    this.taskCleanupTranscoder = taskCleanupTranscoder;
    this.taskHistoryUpdateTranscoder = taskHistoryUpdateTranscoder;
    this.taskIdTranscoder = taskIdTranscoder;
    this.pendingTaskTranscoder = pendingTaskTranscoder;
    this.taskShellCommandRequestTranscoder = taskShellCommandRequestTranscoder;
    this.pendingTaskIdTranscoder = pendingTaskIdTranscoder;
    this.taskLoadBalancerUpdateTranscoder = taskLoadBalancerHistoryUpdateTranscoder;
    this.singularityEventListener = singularityEventListener;
    this.taskCache = taskCache;
    this.taskShellCommandUpdateTranscoder = taskShellCommandUpdateTranscoder;
    this.taskMetadataTranscoder = taskMetadataTranscoder;

    this.serverId = serverId;
  }

  // since we can't use creatingParentsIfNeeded in transactions
  public void createRequiredParents() {
    create(HISTORY_PATH_ROOT);
    create(ACTIVE_PATH_ROOT);
  }

  private String getLastHealthcheckPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), LAST_HEALTHCHECK_KEY);
  }

  private String getMetadataParentPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), METADATA_PATH);
  }

  private String getTaskMetadataPath(SingularityTaskMetadata taskMetadata) {
    return ZKPaths.makePath(getMetadataParentPath(taskMetadata.getTaskId()), String.format("%s-%s", taskMetadata.getTimestamp(), taskMetadata.getType()));
  }

  private String getHealthcheckParentPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), HEALTHCHECKS_PATH);
  }

  private String getHealthchecksFinishedPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), HEALTHCHECKS_FINISHED_PATH);
  }

  private String getLastActiveTaskStatusPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(LAST_ACTIVE_TASK_STATUSES_PATH_ROOT, taskId.getId());
  }

  private String getHealthcheckPath(SingularityTaskHealthcheckResult healthcheck) {
    return ZKPaths.makePath(getHealthcheckParentPath(healthcheck.getTaskId()), String.format("%s%s", Long.toString(healthcheck.getTimestamp()), healthcheck.isStartup() ? STARTUP_HEALTHCHECK_PATH_SUFFIX : ""));
  }

  private String getShellRequestQueuePath(SingularityTaskShellCommandRequest shellRequest) {
    return ZKPaths.makePath(SHELL_REQUESTS_QUEUE_PATH_ROOT, shellRequest.getId().getId());
  }

  private String getFinishedTaskMailQueuePath(SingularityTaskId taskId) {
    return ZKPaths.makePath(FINISHED_TASK_MAIL_QUEUE, taskId.getId());
  }

  private String getShellsParentPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), SHELLS_PATH);
  }

  private String getShellHistoryParentPath(SingularityTaskShellCommandRequestId shellRequestId) {
    return ZKPaths.makePath(getShellsParentPath(shellRequestId.getTaskId()), shellRequestId.getSubIdForTaskHistory());
  }

  private String getShellHistoryRequestPath(SingularityTaskShellCommandRequestId shellRequestId) {
    return ZKPaths.makePath(getShellHistoryParentPath(shellRequestId), SHELL_REQUEST_KEY);
  }

  private String getShellHistoryUpdateParentPath(SingularityTaskShellCommandRequestId shellRequestId) {
    return ZKPaths.makePath(getShellHistoryParentPath(shellRequestId), SHELL_UPDATES_PATH);
  }

  private String getShellHistoryUpdatePath(SingularityTaskShellCommandUpdate shellUpdate) {
    return ZKPaths.makePath(getShellHistoryUpdateParentPath(shellUpdate.getShellRequestId()), shellUpdate.getUpdateType().name());
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

  private String getContainerIdPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), CONTAINER_ID_KEY);
  }

  private String getNotifiedOverduePath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), NOTIFIED_OVERDUE_TO_FINISH_KEY);
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
    save(getDirectoryPath(taskId), Optional.of(directory.getBytes(UTF_8)));
  }

  public void saveContainerId(SingularityTaskId taskId, String containerId) {
    save(getContainerIdPath(taskId), Optional.of(containerId.getBytes(UTF_8)));
  }

  @Timed
  public void saveLastActiveTaskStatus(SingularityTaskStatusHolder taskStatus) {
    save(getLastActiveTaskStatusPath(taskStatus.getTaskId()), taskStatus, taskStatusTranscoder);
  }

  public Optional<String> getDirectory(SingularityTaskId taskId) {
    return getData(getDirectoryPath(taskId), StringTranscoder.INSTANCE);
  }

  public Optional<String> getContainerId(SingularityTaskId taskId) {
    return getData(getContainerIdPath(taskId), StringTranscoder.INSTANCE);
  }

  public void saveHealthcheckResult(SingularityTaskHealthcheckResult healthcheckResult) {
    if (canSaveNewHealthcheck(healthcheckResult)) {
      final Optional<byte[]> bytes = Optional.of(healthcheckResultTranscoder.toBytes(healthcheckResult));

      save(getHealthcheckPath(healthcheckResult), bytes);
      save(getLastHealthcheckPath(healthcheckResult.getTaskId()), bytes);
    } else {
      LOG.warn("Healthchecks have finished, could not save new result {}", healthcheckResult);
    }
  }

  private boolean canSaveNewHealthcheck(SingularityTaskHealthcheckResult healthcheckResult) {
    return !exists(getHealthchecksFinishedPath(healthcheckResult.getTaskId()));
  }

  public void markHealthchecksFinished(SingularityTaskId taskId) {
    create(getHealthchecksFinishedPath(taskId));
  }

  public SingularityCreateResult savePendingTask(SingularityPendingTask task) {
    final String pendingPath = getPendingPath(task.getPendingTaskId());

    return save(pendingPath, task, pendingTaskTranscoder);
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

  public Optional<SingularityTaskCleanup> getTaskCleanup(String taskId) {
    return getData(getCleanupPath(taskId), taskCleanupTranscoder);
  }

  public List<SingularityTask> getActiveTasks() {
    List<String> children = Lists.transform(getChildrenAsIds(ACTIVE_PATH_ROOT, taskIdTranscoder), new Function<SingularityTaskId, String>() {

      @Override
      public String apply(SingularityTaskId taskId) {
        return getTaskPath(taskId);
      }

    });

    return getAsync("active_tasks", children, taskTranscoder, taskCache);
  }

  public List<SingularityTaskStatusHolder> getLastActiveTaskStatuses() {
    return getAsyncChildren(LAST_ACTIVE_TASK_STATUSES_PATH_ROOT, taskStatusTranscoder);
  }

  @Timed
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

  public List<SingularityTask> getTasksOnSlave(Collection<SingularityTaskId> activeTaskIds, SingularitySlave slave) {
    final List<SingularityTask> tasks = Lists.newArrayList();
    final String sanitizedHost = JavaUtils.getReplaceHyphensWithUnderscores(slave.getHost());

    for (SingularityTaskId activeTaskId : activeTaskIds) {
      if (activeTaskId.getSanitizedHost().equals(sanitizedHost)) {
        Optional<SingularityTask> maybeTask = getTask(activeTaskId);
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
    Map<String, SingularityTaskId> pathsMap = Maps.newHashMap();
    for (SingularityTaskId taskId : taskIds) {
      pathsMap.put(getHistoryPath(taskId), taskId);
    }

    return getAsyncNestedChildDataAsMap(HISTORY_PATH_ROOT, pathsMap, UPDATES_PATH, taskHistoryUpdateTranscoder);
  }

  public int getNumHealthchecks(SingularityTaskId taskId) {
    return getNumChildren(getHealthcheckParentPath(taskId));
  }

  public int getNumNonstartupHealthchecks(SingularityTaskId taskId) {
    int numChecks = 0;
    List<String> checks = getChildren(getHealthcheckParentPath(taskId));
    for (String check : checks) {
      if (!check.endsWith(STARTUP_HEALTHCHECK_PATH_SUFFIX)) {
        numChecks++;
      }
    }
    return numChecks;
  }

  public List<SingularityTaskHealthcheckResult> getHealthcheckResults(SingularityTaskId taskId) {
    List<SingularityTaskHealthcheckResult> healthcheckResults = getAsyncChildren(getHealthcheckParentPath(taskId), healthcheckResultTranscoder);
    Collections.sort(healthcheckResults);
    return healthcheckResults;
  }

  public void clearStartupHealthchecks(SingularityTaskId taskId) {
    Optional<SingularityTaskHealthcheckResult> maybeLastHealthcheck = getLastHealthcheck(taskId);
    String parentPath = getHealthcheckParentPath(taskId);
    for (String healthcheckPath : getChildren(parentPath)) {
      String fullPath = ZKPaths.makePath(parentPath, healthcheckPath);
      if (healthcheckPath.endsWith(STARTUP_HEALTHCHECK_PATH_SUFFIX) && (!maybeLastHealthcheck.isPresent() || !getHealthcheckPath(maybeLastHealthcheck.get()).equals(fullPath))) {
        delete(fullPath);
      }
    }
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

    return Maps.uniqueIndex(healthcheckResults, SingularityTaskIdHolder.getTaskIdFunction());
  }

  public SingularityCreateResult saveTaskHistoryUpdate(SingularityTaskHistoryUpdate taskHistoryUpdate) {
    return saveTaskHistoryUpdate(taskHistoryUpdate, false);
  }

  @Timed
  public SingularityCreateResult saveTaskHistoryUpdate(SingularityTaskHistoryUpdate taskHistoryUpdate, boolean overwriteExisting) {
    singularityEventListener.taskHistoryUpdateEvent(taskHistoryUpdate);

    if (overwriteExisting) {
      return save(getUpdatePath(taskHistoryUpdate.getTaskId(), taskHistoryUpdate.getTaskState()), taskHistoryUpdate, taskHistoryUpdateTranscoder);
    } else {
      return create(getUpdatePath(taskHistoryUpdate.getTaskId(), taskHistoryUpdate.getTaskState()), taskHistoryUpdate, taskHistoryUpdateTranscoder);
    }
  }

  public SingularityDeleteResult deleteTaskHistoryUpdate(SingularityTaskId taskId, ExtendedTaskState state, Optional<SingularityTaskHistoryUpdate> previousStateUpdate) {
    if (previousStateUpdate.isPresent()) {
      singularityEventListener.taskHistoryUpdateEvent(previousStateUpdate.get());
    }
    return delete(getUpdatePath(taskId, state));
  }

  public boolean isActiveTask(String taskId) {
    return exists(getActivePath(taskId));
  }

  public List<SingularityTaskId> getTaskIdsForRequest(String requestId) {
    return getChildrenAsIds(getRequestPath(requestId), taskIdTranscoder);
  }

  public Optional<SingularityTaskId> getTaskByRunId(String requestId, String runId) {
    Map<SingularityTaskId, SingularityTask> activeTasks = getTasks(getActiveTaskIdsForRequest(requestId));
    for (Map.Entry<SingularityTaskId, SingularityTask> entry : activeTasks.entrySet()) {
      if (entry.getValue().getTaskRequest().getPendingTask().getRunId().isPresent() && entry.getValue().getTaskRequest().getPendingTask().getRunId().get().equals(runId)) {
        return Optional.of(entry.getKey());
      }
    }
    return Optional.absent();
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

    return exists(ACTIVE_PATH_ROOT, paths, taskIdTranscoder);
  }

  public int getNumLaunchingTasks() {
    List<SingularityTaskId> activeTaskIds = getActiveTaskIds();

    final Map<String, SingularityTaskId> paths = Maps.newHashMapWithExpectedSize(activeTaskIds.size());

    for (SingularityTaskId taskId : activeTaskIds) {
      paths.put(getUpdatePath(taskId, ExtendedTaskState.TASK_RUNNING), taskId);
    }

    return notExists(ACTIVE_PATH_ROOT, paths).size();
  }

  public List<SingularityTaskId> filterInactiveTaskIds(List<SingularityTaskId> taskIds) {
    final Map<String, SingularityTaskId> pathsMap = Maps.newHashMap();

    for (SingularityTaskId taskId : taskIds) {
      pathsMap.put(getActivePath(taskId.getId()), taskId);
    }

    return notExists(ACTIVE_PATH_ROOT, pathsMap);
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

  public List<SingularityTaskId> getTaskIdsForDeploy(String requestId, final String deployId, TaskFilter taskFilter) {
    List<SingularityTaskId> requestTaskIds = getTaskIdsForRequest(requestId, taskFilter);
    final Iterable<SingularityTaskId> deployTaskIds = Iterables.filter(requestTaskIds, new Predicate<SingularityTaskId>() {
      @Override
      public boolean apply(SingularityTaskId input) {
        return input.getDeployId().equals(deployId);
      }
    });
    return ImmutableList.copyOf(deployTaskIds);
  }

  public List<SingularityTaskId> getActiveTaskIdsForDeploy(String requestId, final String deployId) {
    return getTaskIdsForDeploy(requestId, deployId, TaskFilter.ACTIVE);
  }

  public List<SingularityTaskId> getInactiveTaskIdsForDeploy(String requestId, final String deployId) {
    return getTaskIdsForDeploy(requestId, deployId, TaskFilter.INACTIVE);
  }

  public List<SingularityTaskId> getInactiveTaskIds(List<String> requestIds) {
    List<String> paths = Lists.newArrayListWithCapacity(requestIds.size());
    for (String requestId : requestIds) {
      paths.add(getRequestPath(requestId));
    }

    List<SingularityTaskId> taskIds = getChildrenAsIdsForParents("requestIds", paths, taskIdTranscoder);

    return filterInactiveTaskIds(taskIds);
  }

  public Optional<SingularityTaskHistory> getTaskHistory(SingularityTaskId taskId) {
    final Optional<SingularityTask> task = getTaskCheckCache(taskId, true);

    if (!task.isPresent()) {
      return Optional.absent();
    }

    List<SingularityTaskHistoryUpdate> taskUpdates = getTaskHistoryUpdates(taskId);
    Optional<String> directory = getDirectory(taskId);
    Optional<String> containerId = getContainerId(taskId);
    List<SingularityTaskHealthcheckResult> healthchecks = getHealthcheckResults(taskId);

    List<SingularityLoadBalancerUpdate> loadBalancerUpdates = Lists.newArrayListWithCapacity(2);

    checkLoadBalancerHistory(loadBalancerUpdates, taskId, LoadBalancerRequestType.ADD);
    checkLoadBalancerHistory(loadBalancerUpdates, taskId, LoadBalancerRequestType.REMOVE);

    List<SingularityTaskShellCommandHistory> shellCommandHistory = getTaskShellCommandHistory(taskId);

    List<SingularityTaskMetadata> taskMetadata = getTaskMetadata(taskId);

    return Optional.of(new SingularityTaskHistory(taskUpdates, directory, containerId, healthchecks, task.get(), loadBalancerUpdates, shellCommandHistory, taskMetadata));
  }

  private List<SingularityTaskShellCommandHistory> getTaskShellCommandHistory(SingularityTaskId taskId) {
    List<SingularityTaskShellCommandRequest> shellRequests = getTaskShellCommandRequestsForTask(taskId);
    List<SingularityTaskShellCommandHistory> shellCommandHistory = new ArrayList<>(shellRequests.size());

    for (SingularityTaskShellCommandRequest shellRequest : shellRequests) {
      shellCommandHistory.add(new SingularityTaskShellCommandHistory(shellRequest, getTaskShellCommandUpdates(shellRequest.getId())));
    }

    return shellCommandHistory;
  }

  private List<SingularityTaskMetadata> getTaskMetadata(SingularityTaskId taskId) {
    List<SingularityTaskMetadata> taskMetadata = getAsyncChildren(getMetadataParentPath(taskId), taskMetadataTranscoder);
    Collections.sort(taskMetadata);
    return taskMetadata;
  }

  private void checkLoadBalancerHistory(List<SingularityLoadBalancerUpdate> loadBalancerUpdates, SingularityTaskId taskId, LoadBalancerRequestType lbRequestType) {
    Optional<SingularityLoadBalancerUpdate> lbHistory = getLoadBalancerState(taskId, lbRequestType);

    if (lbHistory.isPresent()) {
      loadBalancerUpdates.add(lbHistory.get());
    }
  }

  public boolean hasNotifiedOverdue(SingularityTaskId taskId) {
    return checkExists(getNotifiedOverduePath(taskId)).isPresent();
  }

  public void saveNotifiedOverdue(SingularityTaskId taskId) {
    save(getNotifiedOverduePath(taskId), Optional.<byte[]> absent());
  }

  public Optional<SingularityLoadBalancerUpdate> getLoadBalancerState(SingularityTaskId taskId, LoadBalancerRequestType requestType) {
    return getData(getLoadBalancerStatePath(taskId, requestType), taskLoadBalancerUpdateTranscoder);
  }

  public Optional<SingularityPendingTask> getPendingTask(SingularityPendingTaskId pendingTaskId) {
    return getData(getPendingPath(pendingTaskId), pendingTaskTranscoder);
  }

  private Optional<SingularityTask> getTaskCheckCache(SingularityTaskId taskId, boolean shouldCheckExists) {
    final String path = getTaskPath(taskId);

    return getData(path, taskTranscoder, taskCache, shouldCheckExists);
  }

  @Timed
  public Optional<SingularityTask> getTask(SingularityTaskId taskId) {
    return getTaskCheckCache(taskId, false);
  }

  public boolean taskExistsInZk(SingularityTaskId taskId) {
    return checkExists(getTaskPath(taskId)).isPresent();
  }

  public List<SingularityPendingTaskId> getPendingTaskIds() {
    return getChildrenAsIds(PENDING_PATH_ROOT, pendingTaskIdTranscoder);
  }

  public List<SingularityPendingTask> getPendingTasks() {
    return getAsyncChildren(PENDING_PATH_ROOT, pendingTaskTranscoder);
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

    return Maps.uniqueIndex(getAsync("tasks_by_ids", paths, taskTranscoder, taskCache), SingularityTaskIdHolder.getTaskIdFunction());
  }

  private void createTaskAndDeletePendingTaskPrivate(SingularityTask task) throws Exception {
    delete(getPendingPath(task.getTaskRequest().getPendingTask().getPendingTaskId()));

    final long now = System.currentTimeMillis();

    String msg = String.format("Task launched because of %s", task.getTaskRequest().getPendingTask().getPendingTaskId().getPendingType().name());

    if (task.getTaskRequest().getPendingTask().getUser().isPresent()) {
      msg = String.format("%s by %s", msg, task.getTaskRequest().getPendingTask().getUser().get());
    }

    if (task.getTaskRequest().getPendingTask().getMessage().isPresent()) {
      msg = String.format("%s (%s)", msg, task.getTaskRequest().getPendingTask().getMessage().get());
    }

    saveTaskHistoryUpdate(new SingularityTaskHistoryUpdate(task.getTaskId(), now, ExtendedTaskState.TASK_LAUNCHED, Optional.of(msg), Optional.<String>absent()));
    saveLastActiveTaskStatus(new SingularityTaskStatusHolder(task.getTaskId(), Optional.<TaskStatus>absent(), now, serverId, Optional.of(task.getOffer().getSlaveId().getValue())));

    try {
      final String path = getTaskPath(task.getTaskId());

      CuratorTransactionFinal transaction = curator.inTransaction().create().forPath(path, taskTranscoder.toBytes(task)).and();

      transaction.create().forPath(getActivePath(task.getTaskId().getId())).and().commit();

      taskCache.set(path, task);
    } catch (KeeperException.NodeExistsException nee) {
      LOG.error("Task or active path already existed for {}", task.getTaskId());
    }
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

  @Timed
  public SingularityDeleteResult deleteLastActiveTaskStatus(SingularityTaskId taskId) {
    return delete(getLastActiveTaskStatusPath(taskId));
  }

  public SingularityCreateResult saveTaskMetadata(SingularityTaskMetadata taskMetadata) {
    return save(getTaskMetadataPath(taskMetadata), taskMetadata, taskMetadataTranscoder);
  }

  public SingularityCreateResult saveTaskShellCommandRequestToQueue(SingularityTaskShellCommandRequest shellRequest) {
    return save(getShellRequestQueuePath(shellRequest), shellRequest, taskShellCommandRequestTranscoder);
  }

  public SingularityCreateResult saveTaskFinishedInMailQueue(SingularityTaskId taskId) {
    return save(getFinishedTaskMailQueuePath(taskId), Optional.<byte[]>absent());
  }

  public List<SingularityTaskId> getTaskFinishedMailQueue() {
    return getChildrenAsIds(FINISHED_TASK_MAIL_QUEUE, taskIdTranscoder);
  }

  public SingularityDeleteResult deleteFinishedTaskMailQueue(SingularityTaskId taskId) {
    return delete(getFinishedTaskMailQueuePath(taskId));
  }

  public SingularityCreateResult saveTaskShellCommandRequestToTask(SingularityTaskShellCommandRequest shellRequest) {
    return save(getShellHistoryRequestPath(shellRequest.getId()), shellRequest, taskShellCommandRequestTranscoder);
  }

  public SingularityCreateResult saveTaskShellCommandUpdate(SingularityTaskShellCommandUpdate shellUpdate) {
    return save(getShellHistoryUpdatePath(shellUpdate), shellUpdate, taskShellCommandUpdateTranscoder);
  }

  public List<SingularityTaskShellCommandUpdate> getTaskShellCommandUpdates(SingularityTaskShellCommandRequestId shellRequestId) {
    return getAsyncChildren(getShellHistoryUpdateParentPath(shellRequestId), taskShellCommandUpdateTranscoder);
  }

  public List<SingularityTaskShellCommandRequest> getTaskShellCommandRequestsForTask(SingularityTaskId taskId) {
    final String parentPath = getShellsParentPath(taskId);
    List<String> children = getChildren(parentPath);
    List<String> paths = Lists.newArrayListWithCapacity(children.size());

    for (String child : children) {
      paths.add(ZKPaths.makePath(parentPath, ZKPaths.makePath(child, SHELL_REQUEST_KEY)));
    }

    List<SingularityTaskShellCommandRequest> shellRequests = getAsync(parentPath, paths, taskShellCommandRequestTranscoder);

    Collections.sort(shellRequests);

    return shellRequests;
  }

  public List<SingularityTaskShellCommandRequest> getAllQueuedTaskShellCommandRequests() {
    return getAsyncChildren(SHELL_REQUESTS_QUEUE_PATH_ROOT, taskShellCommandRequestTranscoder);
  }

  public SingularityDeleteResult deleteTaskShellCommandRequestFromQueue(SingularityTaskShellCommandRequest shellRequest) {
    return delete(getShellRequestQueuePath(shellRequest));
  }

  public SingularityCreateResult saveTaskCleanup(SingularityTaskCleanup cleanup) {
    saveTaskHistoryUpdate(cleanup);

    return save(getCleanupPath(cleanup.getTaskId().getId()), cleanup, taskCleanupTranscoder);
  }

  private void saveTaskHistoryUpdate(SingularityTaskCleanup cleanup) {
    StringBuilder msg = new StringBuilder(cleanup.getCleanupType().name());

    if (cleanup.getUser().isPresent()) {
      msg.append(" by ");
      msg.append(cleanup.getUser().get());
    }

    if (cleanup.getMessage().isPresent()) {
      msg.append(" - ");
      msg.append(cleanup.getMessage().get());
    }

    saveTaskHistoryUpdate(new SingularityTaskHistoryUpdate(cleanup.getTaskId(), cleanup.getTimestamp(), ExtendedTaskState.TASK_CLEANING, Optional.of(msg.toString()), Optional.<String>absent()), true);
  }

  public SingularityCreateResult createTaskCleanup(SingularityTaskCleanup cleanup) {
    final SingularityCreateResult result = create(getCleanupPath(cleanup.getTaskId().getId()), cleanup, taskCleanupTranscoder);

    if (result == SingularityCreateResult.CREATED) {
      saveTaskHistoryUpdate(cleanup);
    }

    return result;
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

  public SingularityDeleteResult deleteTaskHistory(SingularityTaskId taskId) {
    taskCache.delete(getTaskPath(taskId));

    return delete(getHistoryPath(taskId));
  }

}
