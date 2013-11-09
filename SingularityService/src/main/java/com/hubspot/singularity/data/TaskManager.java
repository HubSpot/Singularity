package com.hubspot.singularity.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskId;

public class TaskManager extends CuratorManager {

  private final static Logger LOG = LoggerFactory.getLogger(TaskManager.class);
  
  private final ObjectMapper objectMapper;
  
  private final static String ACTIVE_PATH_ROOT = "/tasks";
  private final static String ACTIVE_PATH_FORMAT = ACTIVE_PATH_ROOT + "/%s";

  private final static String SCHEDULED_PATH_ROOT = "/scheduled";
  private final static String SCHEDULED_PATH_FORMAT = SCHEDULED_PATH_ROOT + "/%s";
    
  @Inject
  public TaskManager(CuratorFramework curator, ObjectMapper objectMapper) {
    super(curator);
    this.objectMapper = objectMapper;
  }
  
  private String getActivePath(String taskId) {
    return String.format(ACTIVE_PATH_FORMAT, taskId);
  }

  private String getScheduledPath(String taskId) {
    return String.format(SCHEDULED_PATH_FORMAT, taskId);
  }
  
  public int getNumActiveTasks() {
    return getNumChildren(ACTIVE_PATH_ROOT);
  }
  
  public int getNumScheduledTasks() {
    return getNumChildren(SCHEDULED_PATH_ROOT);
  }
  
  public void persistScheduleTasks(List<SingularityPendingTaskId> taskIds) {
    try {
      for (SingularityPendingTaskId taskId : taskIds) {
        persistTaskId(taskId);
      }
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void persistTaskId(SingularityPendingTaskId taskId) throws Exception {
    final String pendingPath = getScheduledPath(taskId.toString());

    curator.create().creatingParentsIfNeeded().forPath(pendingPath);
  }
  
  public List<SingularityTaskId> getActiveTaskIds() {
    List<String> taskIds = getChildren(ACTIVE_PATH_ROOT);
    List<SingularityTaskId> taskIdsObjs = Lists.newArrayListWithCapacity(taskIds.size());

    for (String taskId : taskIds) {
      SingularityTaskId taskIdObj = SingularityTaskId.fromString(taskId);
      taskIdsObjs.add(taskIdObj);
    }
    
    return taskIdsObjs;
  }
  
  public Optional<SingularityTask> getActiveTask(String taskId) {
    final String path = getActivePath(taskId);
    
    try {
      byte[] data = curator.getData().forPath(path);
      
      SingularityTask task = SingularityTask.getTaskFromData(data, objectMapper);
      
      return Optional.of(task);
    } catch (NoNodeException nne) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e); 
    }
  }
  
  public List<SingularityTask> getActiveTasks() {
    List<SingularityTaskId> taskIds = getActiveTaskIds();
    
    try {
      List<SingularityTask> tasks = Lists.newArrayListWithCapacity(taskIds.size());
      
      for (SingularityTaskId taskId : taskIds) {
        Optional<SingularityTask> maybeTask = getActiveTask(taskId.toString());
        
        if (maybeTask.isPresent()) {
          tasks.add(maybeTask.get());
        } else {
          LOG.info(String.format("Expected active node %s but it wasn't there", taskId));
        }
      }

      return tasks;
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  public List<SingularityPendingTaskId> getScheduledTasks() {
    List<String> taskIds = getChildren(SCHEDULED_PATH_ROOT);
    List<SingularityPendingTaskId> taskIdsObjs = Lists.newArrayListWithCapacity(taskIds.size());

    for (String taskId : taskIds) {
      SingularityPendingTaskId taskIdObj = SingularityPendingTaskId.fromString(taskId);
      taskIdsObjs.add(taskIdObj);
    }
    
    return taskIdsObjs;
  }
  
  public void launchTask(SingularityTask task) {
    try {
      launchTaskPrivate(task);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void launchTaskPrivate(SingularityTask task) throws Exception {
    final String scheduledPath = getScheduledPath(task.getTaskRequest().getPendingTaskId().toString());
    final String activePath = getActivePath(task.getTaskId().toString());
    
    curator.delete().forPath(scheduledPath);
    
    curator.create().creatingParentsIfNeeded().forPath(activePath, task.getTaskData(objectMapper));
  }
  
  public void deleteActiveTask(String taskId) {
    delete(getActivePath(taskId));
  }
  
  public void deleteScheduledTask(String taskId) {
    delete(getScheduledPath(taskId));
  }
  
}
