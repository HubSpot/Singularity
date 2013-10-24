package com.hubspot.singularity.data;

import java.util.Collections;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;

public class TaskManager {

  private final static Logger LOG = LoggerFactory.getLogger(TaskManager.class);
  
  private final CuratorFramework curator;
  private final ObjectMapper objectMapper;
  
  private final static String HISTORY_ROOT_FORMAT = "/history/%s";
  private final static String HISTORY_PATH_FORMAT = HISTORY_ROOT_FORMAT + "/%s";

  private final static String ACTIVE_PATH_ROOT = "/tasks";
  private final static String ACTIVE_PATH_FORMAT = ACTIVE_PATH_ROOT + "/%s";

  private final static String PENDING_PATH_ROOT = "/pending";
  private final static String PENDING_PATH_FORMAT = PENDING_PATH_ROOT + "/%s";
    
  @Inject
  public TaskManager(CuratorFramework curator, ObjectMapper objectMapper) {
    this.curator = curator;
    this.objectMapper = objectMapper;
  }

  private String getHistoryPath(String taskId, String statusUpdate) {
    return String.format(HISTORY_PATH_FORMAT, taskId, statusUpdate);
  }
  
  private String getHistoryRootPath(String taskId) {
    return String.format(HISTORY_ROOT_FORMAT, taskId);
  }
  
  private String getActivePath(String taskId) {
    return String.format(ACTIVE_PATH_FORMAT, taskId);
  }

  private String getPendingPath(String taskId) {
    return String.format(PENDING_PATH_FORMAT, taskId);
  }

  public void persistScheduleTasks(List<SingularityTaskId> taskIds) {
    try {
      for (SingularityTaskId taskId : taskIds) {
        persistTaskId(taskId);
      }
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void persistTaskId(SingularityTaskId taskId) throws Exception {
    final String pendingPath = getPendingPath(taskId.toString());

    curator.create().creatingParentsIfNeeded().forPath(pendingPath);
  }
  
  private List<SingularityTaskId> getTasksForRoot(String root) {
    try {
      List<String> taskIds = curator.getChildren().forPath(root);
      List<SingularityTaskId> tasksIdsObjs = Lists.newArrayListWithCapacity(taskIds.size());
      
      for (String taskId : taskIds) {
        SingularityTaskId taskIdObj = SingularityTaskId.fromString(taskId);
        tasksIdsObjs.add(taskIdObj);
      }

      return tasksIdsObjs;
    } catch (NoNodeException nne) {
      return Collections.emptyList();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  public List<SingularityTaskId> getActiveTaskIds() {
    return getTasksForRoot(ACTIVE_PATH_ROOT);
  }
  
  public List<SingularityTask> getActiveTasks() {
    List<SingularityTaskId> taskIds = getActiveTaskIds();
    
    try {
      List<SingularityTask> tasks = Lists.newArrayListWithCapacity(taskIds.size());
      
      for (SingularityTaskId taskId : taskIds) {
        final String path = getActivePath(taskId.toString());
        
        try {
          byte[] data = curator.getData().forPath(path);
          
          SingularityTask task = SingularityTask.getTaskFromData(data, objectMapper);
          
          tasks.add(task);
        } catch (NoNodeException nne) {
          LOG.info(String.format("Expected active node %s but it wasn't there.", path));
        }
      }

      return tasks;
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  public List<SingularityTaskId> getPendingTasks() {
    return getTasksForRoot(PENDING_PATH_ROOT);
  }

  public void launchTask(SingularityTask task) {
    try {
      launchTaskPrivate(task);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void launchTaskPrivate(SingularityTask task) throws Exception {
    final String pendingPath = getPendingPath(task.getTaskId().toString());
    final String activePath = getActivePath(task.getTaskId().toString());
    
    curator.delete().forPath(pendingPath);
    
    curator.create().creatingParentsIfNeeded().forPath(activePath, task.getTaskData(objectMapper));
  }
  
  private List<SingularityHistory> getHistoryPrivate(String taskId) throws Exception {
    final String parentPath = getHistoryRootPath(taskId);
    List<String> historyNodes = null;
    
    try {
      historyNodes = curator.getChildren().forPath(parentPath);
    } catch (NoNodeException nee) {
      return Collections.emptyList();
    }
    
    final List<SingularityHistory> histories = Lists.newArrayListWithCapacity(historyNodes.size());
    
    for (String historyNode : historyNodes) {
      final String path = ZKPaths.makePath(parentPath, historyNode);
      
      Stat stat = curator.checkExists().forPath(path);
      byte[] payload = curator.getData().forPath(ZKPaths.makePath(parentPath, historyNode));
      
      Optional<String> maybeMessage = null;
      
      if (payload == null || payload.length == 0) {
        maybeMessage = Optional.absent();
      } else {
        maybeMessage = Optional.of(JavaUtils.toString(payload));
      }
      
      histories.add(new SingularityHistory(stat.getCtime(), historyNode, maybeMessage));
    }
    
    return histories;
  }
  
  public List<SingularityHistory> getHistory(String taskId) {
    try {
      return getHistoryPrivate(taskId);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  } 
    
  public void deleteActiveTask(String taskId) {
    final String activePath = getActivePath(taskId);
    
    try {
      curator.delete().forPath(activePath);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
  public void recordStatus(String statusUpdate, String taskId, Optional<String> message) {
    byte[] payload = null;
    
    if (message.isPresent()) {
      payload = JavaUtils.toBytes(message.get());
    } else {
      payload = JavaUtils.toBytes("");
    }
    
    try {
      curator.create().creatingParentsIfNeeded().forPath(getHistoryPath(taskId, statusUpdate), payload);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
}
