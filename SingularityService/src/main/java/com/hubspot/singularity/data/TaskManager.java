package com.hubspot.singularity.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NodeExistsException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularitySlave;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskCleanup;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityTaskCleanupTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHistoryUpdateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskStateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskTranscoder;

public class TaskManager extends CuratorAsyncManager {
  
  private final ObjectMapper objectMapper;
  private final SingularityTaskStateTranscoder taskStateTranscoder;
  private final SingularityTaskCleanupTranscoder taskCleanupTranscoder;
  private final SingularityTaskTranscoder taskTranscoder;
  private final SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder;
  
  private final static String TASKS_ROOT = "/tasks";
  
  private final static String ACTIVE_PATH_ROOT = TASKS_ROOT + "/active";
  private final static String SCHEDULED_PATH_ROOT = TASKS_ROOT + "/scheduled";
  private final static String CLEANUP_PATH_ROOT = TASKS_ROOT + "/cleanup";
  
  private final static String HISTORY_PATH_ROOT = TASKS_ROOT + "/history";
  
  private final static String STATE_KEY = "STATE";
  private final static String UPDATES_PATH = "/updates";
    
  @Inject
  public TaskManager(SingularityConfiguration configuration, CuratorFramework curator, ObjectMapper objectMapper, SingularityTaskStateTranscoder taskStateTranscoder, SingularityTaskTranscoder taskTranscoder, SingularityTaskCleanupTranscoder taskCleanupTranscoder, SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout());
  
    this.taskStateTranscoder = taskStateTranscoder;
    this.taskTranscoder = taskTranscoder;
    this.taskCleanupTranscoder = taskCleanupTranscoder;
    this.taskHistoryUpdateTranscoder = taskHistoryUpdateTranscoder;
    this.objectMapper = objectMapper;
  }
  
  // TODO this.
  private String getStatePath(String taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), STATE_KEY);
  }
  
  private String getUpdatesPath(String taskId) {
    return ZKPaths.makePath(getHistoryPath(taskId), UPDATES_PATH);
  }
  
  private String getHistoryPath(String taskId) {
    return ZKPaths.makePath(HISTORY_PATH_ROOT, taskId);
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

    if (task.getMaybeCmdLineArgs().isPresent()) {
      curator.create().creatingParentsIfNeeded().forPath(pendingPath, JavaUtils.toBytes(task.getMaybeCmdLineArgs().get()));
    } else {
      curator.create().creatingParentsIfNeeded().forPath(pendingPath);
    }
  }
  
  private List<SingularityTaskId> getTaskIds(String root) {
    List<String> taskIds = getChildren(root);
    List<SingularityTaskId> taskIdsObjs = Lists.newArrayListWithCapacity(taskIds.size());

    for (String taskId : taskIds) {
      SingularityTaskId taskIdObj = SingularityTaskId.fromString(taskId);
      taskIdsObjs.add(taskIdObj);
    }
    
    return taskIdsObjs;
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
    
  public Multimap<SingularityTaskId, SingularityTaskHistoryUpdate> getTaskHistoryUpdates(Collection<SingularityTaskId> taskIds) {
    List<String> paths = Lists.newArrayListWithCapacity(taskIds.size());
    for (SingularityTaskId taskId : taskIds) {
      paths.add(getUpdatesPath(taskId.getId()));
    }
    
    List<SingularityTaskHistoryUpdate> updates = getAsync("updates_by_ids", paths, taskHistoryUpdateTranscoder);
    
    return Multimaps.index(updates, taskHistoryUpdateTranscoder);
  }
  
  public Map<SingularityTaskId, SingularityTaskState> getTaskState(Collection<SingularityTaskId> taskIds) {
    List<String> paths = Lists.newArrayListWithCapacity(taskIds.size());
    for (SingularityTaskId taskId : taskIds) {
      paths.add(getStatePath(taskId.getId()));
    }
    
    List<SingularityTaskState> taskStates = getAsync("states_by_ids", paths, taskStateTranscoder);
    
    return Maps.uniqueIndex(taskStates, taskStateTranscoder);
  }
  
  public List<SingularityTaskHistoryUpdate> getTaskHistoryForTask(String strTaskId) {
    return getAsyncChildren(getActivePath(strTaskId), taskHistoryUpdateTranscoder);
  }
  
  public SingularityCreateResult saveTaskHistoryUpdate(SingularityTaskId taskId, SingularityTaskHistoryUpdate taskHistoryUpdate) {
    try {
      curator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(getUpdatesPath(taskId.getId()), taskHistoryUpdate.getAsBytes(objectMapper));
    } catch (NodeExistsException nee) {
      return SingularityCreateResult.EXISTED;
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
    
    return SingularityCreateResult.CREATED;
  }
  
  public boolean isActiveTask(String taskId) {
    final String path = getActivePath(taskId);
    
    return exists(path);
  }
  
  public Optional<SingularityTask> getActiveTask(String taskId) {
    final String path = getActivePath(taskId);
    
    return getData(path, taskTranscoder);
  }

  public List<SingularityPendingTask> getScheduledTasks() {
    final List<String> taskIds = getChildren(SCHEDULED_PATH_ROOT);
    final List<SingularityPendingTask> tasks = Lists.newArrayListWithCapacity(taskIds.size());
    
    for (String taskId : taskIds) {
      SingularityPendingTaskId taskIdObj = SingularityPendingTaskId.fromString(taskId);
      Optional<String> maybeCmdLineArgs = Optional.absent();
      
      if (taskIdObj.getPendingTypeEnum() == PendingType.ONEOFF) {
        maybeCmdLineArgs = getStringData(ZKPaths.makePath(SCHEDULED_PATH_ROOT, taskId));
      }
      
      tasks.add(new SingularityPendingTask(taskIdObj, maybeCmdLineArgs));
    }
    
    return tasks;
  }
  
  public void launchTask(SingularityTask task) {
    try {
      launchTaskPrivate(task);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void launchTaskPrivate(SingularityTask task) throws Exception {
    final String scheduledPath = getScheduledPath(task.getTaskRequest().getPendingTask().getPendingTaskId().getId());
    final String activePath = getActivePath(task.getTaskId().toString());
    
    curator.delete().forPath(scheduledPath);
    
    curator.create().creatingParentsIfNeeded().forPath(activePath, task.getAsBytes(objectMapper));
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
  
}
