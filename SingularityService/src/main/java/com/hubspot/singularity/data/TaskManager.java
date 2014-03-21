package com.hubspot.singularity.data;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NodeExistsException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Function;
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
import com.hubspot.singularity.SingularityTaskHealthcheckResult;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskState;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.transcoders.SingularityPendingTaskIdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskCleanupTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskHistoryUpdateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskIdTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskStateTranscoder;
import com.hubspot.singularity.data.transcoders.SingularityTaskTranscoder;
import com.hubspot.singularity.data.transcoders.StringTranscoder;

public class TaskManager extends CuratorAsyncManager {
  
  private final static String TASKS_ROOT = "/tasks";
  
  private final static String ACTIVE_PATH_ROOT = TASKS_ROOT + "/active";
  private final static String SCHEDULED_PATH_ROOT = TASKS_ROOT + "/scheduled";
  private final static String CLEANUP_PATH_ROOT = TASKS_ROOT + "/cleanup";
    
  private final static String STATE_PATH_ROOT = TASKS_ROOT + "/state";
  
  private final static String LAST_HEALTHCHECK_KEY = "LAST_HEALTHCHECK";
  private final static String DIRECTORY_KEY = "DIRECTORY";
  private final static String TASK_KEY = "TASK";
  
  private final static String UPDATES_PATH = "/updates";
  
  private final SingularityTaskStateTranscoder taskStateTranscoder;
  private final SingularityTaskCleanupTranscoder taskCleanupTranscoder;
  private final SingularityTaskTranscoder taskTranscoder;
  private final SingularityTaskIdTranscoder taskIdTranscoder;
  private final SingularityPendingTaskIdTranscoder pendingTaskIdTranscoder;
  private final SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder;
  private final ObjectMapper objectMapper;
  private final Function<SingularityPendingTaskId, SingularityPendingTask> pendingTaskIdToPendingTaskFunction;
  
  @Inject
  public TaskManager(SingularityConfiguration configuration, CuratorFramework curator, ObjectMapper objectMapper, SingularityPendingTaskIdTranscoder pendingTaskIdTranscoder, SingularityTaskIdTranscoder taskIdTranscoder, 
      SingularityTaskStateTranscoder taskStateTranscoder, SingularityTaskTranscoder taskTranscoder, SingularityTaskCleanupTranscoder taskCleanupTranscoder, SingularityTaskHistoryUpdateTranscoder taskHistoryUpdateTranscoder) {
    super(curator, configuration.getZookeeperAsyncTimeout());
  
    this.taskStateTranscoder = taskStateTranscoder;
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
  
  private String getHealthcheckPath(String taskId) {
    return ZKPaths.makePath(getStatePath(taskId), LAST_HEALTHCHECK_KEY);
  }
  
  private String getTaskPath(String taskId) {
    return ZKPaths.makePath(getStatePath(taskId), TASK_KEY);
  }
  
  private String getDirectoryPath(String taskId) {
    return ZKPaths.makePath(getStatePath(taskId), DIRECTORY_KEY);
  }
  
  private String getUpdatesPath(String taskId) {
    return ZKPaths.makePath(getStatePath(taskId), UPDATES_PATH);
  }
  
  private String getStatePath(String taskId) {
    return ZKPaths.makePath(STATE_PATH_ROOT, taskId);
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

  public void updateTaskDirectory(String taskId, String directory) {
    save(getDirectoryPath(taskId), Optional.of(JavaUtils.toBytes(directory)));
  }
  
  public Optional<String> getDirectory(String taskId) {
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
  
  public List<SingularityTaskHistoryUpdate> getTaskHistoryUpdatesForTask(SingularityTaskId taskId) {
    return getAsyncChildren(getUpdatesPath(taskId.getId()), taskHistoryUpdateTranscoder);
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
  
  public SingularityCreateResult saveTaskHistoryUpdate(SingularityTaskHistoryUpdate taskHistoryUpdate) {
    try {
      curator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT_SEQUENTIAL).forPath(getUpdatesPath(taskHistoryUpdate.getTaskId().getId()), taskHistoryUpdate.getAsBytes(objectMapper));
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
  
  public Optional<SingularityTask> getTask(String taskId) {
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

  private void launchTaskPrivate(SingularityTask task) throws Exception {
    final String scheduledPath = getScheduledPath(task.getTaskRequest().getPendingTask().getPendingTaskId().getId());
    final String activePath = getActivePath(task.getTaskId().getId());
    
    curator.delete().forPath(scheduledPath);
    
    final byte[] data = task.getAsBytes(objectMapper);
    
    // TODO
    curator.create().creatingParentsIfNeeded().forPath(activePath, data);
    curator.create().creatingParentsIfNeeded().forPath(getTaskPath(task.getTaskId().getId()), data);
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
