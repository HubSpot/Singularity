package com.hubspot.singularity.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.mesos.Protos.TaskState;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.LoadBalancerRequestType;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.LoadBalancerState;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.LoadBalancerStateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityPendingTaskIdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskCleanupTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHealthcheckResultTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHistoryUpdateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskIdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskTranscoder;
import com.hubspot.singularity.data.transcoders.StringTranscoder;

public class TaskManager extends CuratorAsyncManager {
  
  private final static String TASKS_ROOT = "/tasks";
  
  private final static String ACTIVE_PATH_ROOT = TASKS_ROOT + "/active";
  private final static String SCHEDULED_PATH_ROOT = TASKS_ROOT + "/scheduled";
  private final static String CLEANUP_PATH_ROOT = TASKS_ROOT + "/cleanup";
  private final static String LB_CLEANUP_PATH_ROOT = TASKS_ROOT + "/lbcleanup";  
  
  private final static String HISTORY_PATH_ROOT = TASKS_ROOT + "/history";
  
  private final static String LAST_HEALTHCHECK_KEY = "LAST_HEALTHCHECK";
  private final static String DIRECTORY_KEY = "DIRECTORY";
  private final static String TASK_KEY = "TASK";
  private final static String LOAD_BALANCER_PRE_KEY = "LOAD_BALANCER_";
  
  private final static String UPDATES_PATH = "/updates";
  
  private final SingularityTaskHealthcheckResultTranscoder healthcheckResultTranscoder;
  private final SingularityTaskCleanupTranscoder taskCleanupTranscoder;
  private final SingularityTaskTranscoder taskTranscoder;
  private final SingularityTaskIdTranscoder taskIdTranscoder;
  private final SingularityPendingTaskIdTranscoder pendingTaskIdTranscoder;
  private final SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder;
  private final ObjectMapper objectMapper;
  private final Function<SingularityPendingTaskId, SingularityPendingTask> pendingTaskIdToPendingTaskFunction;
  
  @Inject
  public TaskManager(SingularityConfiguration configuration, CuratorFramework curator, ObjectMapper objectMapper, SingularityPendingTaskIdTranscoder pendingTaskIdTranscoder, SingularityTaskIdTranscoder taskIdTranscoder, 
      SingularityTaskHealthcheckResultTranscoder healthcheckResultTranscoder, SingularityTaskTranscoder taskTranscoder, SingularityTaskCleanupTranscoder taskCleanupTranscoder, SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout());
  
    this.healthcheckResultTranscoder = healthcheckResultTranscoder;
    this.taskTranscoder = taskTranscoder;
    this.taskCleanupTranscoder = taskCleanupTranscoder;
    this.taskHistoryUpdateTranscoder = taskHistoryUpdateTranscoder;
    this.taskIdTranscoder = taskIdTranscoder;
    this.pendingTaskIdTranscoder = pendingTaskIdTranscoder;
    
    this.objectMapper = objectMapper;
    this.pendingTaskIdToPendingTaskFunction = new Function<SingularityPendingTaskId, SingularityPendingTask>() {

      @Override
      public SingularityPendingTask apply(SingularityPendingTaskId input) {
        Optional<String> maybeCmdLineArgs = Optional.absent();
        
        if (input.getPendingTypeEnum() == PendingType.ONEOFF) {
          maybeCmdLineArgs = getStringData(ZKPaths.makePath(SCHEDULED_PATH_ROOT, input.getId()));
        }
        
        return new SingularityPendingTask(input, maybeCmdLineArgs);
      }
    };
  }
  
  private String getHealthcheckPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), LAST_HEALTHCHECK_KEY);
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
  
  private String getUpdatePath(SingularityTaskId taskId, TaskState state) {
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

  private String getScheduledPath(String taskId) {
    return ZKPaths.makePath(SCHEDULED_PATH_ROOT, taskId);
  }
  
  private String getCleanupPath(String taskId) {
    return ZKPaths.makePath(CLEANUP_PATH_ROOT, taskId);
  }
  
  public int getNumCleanupTasks() {
    return getNumChildren(CLEANUP_PATH_ROOT);
  }
  
  public int getNumActiveTasks() {
    return getNumChildren(ACTIVE_PATH_ROOT);
  }
  
  public int getNumScheduledTasks() {
    return getNumChildren(SCHEDULED_PATH_ROOT);
  }
  
  public void saveLoadBalancerState(SingularityTaskId taskId, LoadBalancerRequestType requestType, Optional<LoadBalancerState> loadBalancerState) {
    Optional<byte[]> data = Optional.absent();
    if (loadBalancerState.isPresent()) {
      data = Optional.of(LoadBalancerStateTranscoder.LOAD_BALANCER_STATE_TRANSCODER.getBytes(loadBalancerState.get()));
    }
    
    save(getLoadBalancerStatePath(taskId, requestType), data);
  }

  public void updateTaskDirectory(SingularityTaskId taskId, String directory) {
    save(getDirectoryPath(taskId), Optional.of(JavaUtils.toBytes(directory)));
  }
  
  public Optional<String> getDirectory(SingularityTaskId taskId) {
    return getData(getDirectoryPath(taskId), StringTranscoder.STRING_TRANSCODER);
  }
  
  public void saveHealthcheckResult(SingularityTaskHealthcheckResult healthcheckResult) {
    save(getHealthcheckPath(healthcheckResult.getTaskId()), Optional.of(healthcheckResult.getAsBytes(objectMapper)));
  }
  
  public void persistScheduleTasks(List<SingularityPendingTask> tasks) {
    try {
      for (SingularityPendingTask task : tasks) {
        persistTask(task);
      }
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void persistTask(SingularityPendingTask task) throws Exception {
    final String pendingPath = getScheduledPath(task.getPendingTaskId().getId());

    Optional<byte[]> data = Optional.absent();
    
    if (task.getMaybeCmdLineArgs().isPresent()) {
      data = Optional.of(JavaUtils.toBytes(task.getMaybeCmdLineArgs().get()));
    }
    
    create(pendingPath, data);
  }
  
  public List<SingularityTaskId> getAllTaskIds() {
    final List<String> requestIds = getChildren(HISTORY_PATH_ROOT);
    
    return getChildrenAsIdsForParents(HISTORY_PATH_ROOT, requestIds, taskIdTranscoder);
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
    return getAsyncChildren(getUpdatesPath(taskId), taskHistoryUpdateTranscoder);
  }
  
  public Multimap<SingularityTaskId, SingularityTaskHistoryUpdate> getTaskHistoryUpdates(Collection<SingularityTaskId> taskIds) {
    Multimap<SingularityTaskId, SingularityTaskHistoryUpdate> map = ArrayListMultimap.create();
    
    for (SingularityTaskId taskId : taskIds) {
      map.putAll(taskId, getTaskHistoryUpdates(taskId));
    }
    
    return map;
  }
  
  public Optional<SingularityTaskHealthcheckResult> getHealthcheckResult(SingularityTaskId taskId) {
    return getData(getHealthcheckPath(taskId), healthcheckResultTranscoder);
  }
  
  public Map<SingularityTaskId, SingularityTaskHealthcheckResult> getHealthcheckResults(Collection<SingularityTaskId> taskIds) {
    List<String> paths = Lists.newArrayListWithCapacity(taskIds.size());
    for (SingularityTaskId taskId : taskIds) {
      paths.add(getHealthcheckPath(taskId));
    }
    
    List<SingularityTaskHealthcheckResult> healthcheckResults = getAsync("healthchecks_by_ids", paths, healthcheckResultTranscoder);
    
    return Maps.uniqueIndex(healthcheckResults, healthcheckResultTranscoder);
  }
  
  public SingularityCreateResult saveTaskHistoryUpdate(SingularityTaskHistoryUpdate taskHistoryUpdate) {
    return create(getUpdatePath(taskHistoryUpdate.getTaskId(), taskHistoryUpdate.getTaskStateEnum()), Optional.of(taskHistoryUpdate.getAsBytes(objectMapper)));
  }
  
  public boolean isActiveTask(String taskId) {
    final String path = getActivePath(taskId);
    
    return exists(path);
  }
  
  public List<SingularityTaskId> getTaskIdsForRequest(String requestId) {
    return getChildrenAsIds(getRequestPath(requestId), taskIdTranscoder);
  }
  
  public List<SingularityTaskId> getInActiveTaskIdsForRequest(String requestId) {
    return getTaskIdsForRequest(requestId, false);
  }

  public List<SingularityTaskId> getActiveTaskIdsForRequest(String requestId) {
    return getTaskIdsForRequest(requestId, true);
  }
  
  private List<SingularityTaskId> getTaskIdsForRequest(String requestId, boolean active) {
    final List<SingularityTaskId> requestTaskIds = getTaskIdsForRequest(requestId);
    final List<String> paths = Lists.newArrayListWithCapacity(requestTaskIds.size());
    
    for (SingularityTaskId taskId : requestTaskIds) {
      paths.add(getActivePath(taskId.getId()));
    }
    
    final List<SingularityTaskId> activeTaskIds = exists(ACTIVE_PATH_ROOT, paths, taskIdTranscoder);
    
    if (active) {
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
    Optional<SingularityTaskHealthcheckResult> lastHealthcheckResult = getHealthcheckResult(taskId);
    Optional<LoadBalancerState> addLoadBalancerState = getLoadBalancerState(taskId, LoadBalancerRequestType.ADD);
    Optional<LoadBalancerState> removeLoadBalancerState = getLoadBalancerState(taskId, LoadBalancerRequestType.REMOVE);
    
    return Optional.of(new SingularityTaskHistory(taskUpdates, directory, lastHealthcheckResult, task.get(), addLoadBalancerState, removeLoadBalancerState));
  }
  
  public Optional<LoadBalancerState> getLoadBalancerState(SingularityTaskId taskId, LoadBalancerRequestType requestType) {
    return getData(getLoadBalancerStatePath(taskId, requestType), LoadBalancerStateTranscoder.LOAD_BALANCER_STATE_TRANSCODER);
  }
  
  public Optional<SingularityTask> getActiveTask(String taskId) {
    final String path = getActivePath(taskId);
    
    return getData(path, taskTranscoder);
  }
  
  public Optional<SingularityTask> getTask(SingularityTaskId taskId) {
    final String path = getTaskPath(taskId);
    
    return getData(path, taskTranscoder);
  }
  
  public List<SingularityPendingTask> getScheduledTasks() {
    return Lists.transform(getChildrenAsIds(SCHEDULED_PATH_ROOT, pendingTaskIdTranscoder), pendingTaskIdToPendingTaskFunction);
  }
  
  public void launchTask(SingularityTask task) {
    try {
      launchTaskPrivate(task);
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
  
  private void launchTaskPrivate(SingularityTask task) throws Exception {
    final String scheduledPath = getScheduledPath(task.getTaskRequest().getPendingTask().getPendingTaskId().getId());
    final String activePath = getActivePath(task.getTaskId().getId());
    
    curator.delete().forPath(scheduledPath);
    
    final byte[] data = task.getAsBytes(objectMapper);
    
    // TODO - what if it fails also

    curator.create().creatingParentsIfNeeded().forPath(getTaskPath(task.getTaskId()), data);
    curator.create().creatingParentsIfNeeded().forPath(activePath, data);
  }
  
  public List<SingularityTaskId> getLBCleanupTasks() {
    return getChildrenAsIds(LB_CLEANUP_PATH_ROOT, taskIdTranscoder);
  }
  
  private String getLBCleanupPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(LB_CLEANUP_PATH_ROOT, taskId.getId());
  }
  
  public void deleteLBCleanupTask(SingularityTaskId taskId) {
    delete(getLBCleanupPath(taskId));
  }
  
  public SingularityCreateResult createLBCleanupTask(SingularityTaskId taskId) {
    return create(getLBCleanupPath(taskId));
  }
  
  public SingularityCreateResult createCleanupTask(SingularityTaskCleanup cleanupTask) {
    return create(getCleanupPath(cleanupTask.getTaskId().getId()), Optional.of(cleanupTask.getAsBytes(objectMapper)));
  }
  
  public void deleteActiveTask(String taskId) {
    delete(getActivePath(taskId));
  }
  
  public void deleteScheduledTask(String taskId) {
    delete(getScheduledPath(taskId));
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
