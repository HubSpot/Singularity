package com.hubspot.singularity.data;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequest;

public class TaskManager {

  private final CuratorFramework curator;
  private final ObjectMapper objectMapper;
  
  private final static String HISTORY_ROOT_FORMAT = "/history/%s";
  private final static String HISTORY_PATH_FORMAT = HISTORY_ROOT_FORMAT + "/%s";

  private final static String ACTIVE_PATH_FORMAT = "/tasks/%s";

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
  
  private String getActivePath(SingularityTask task) {
    return String.format(ACTIVE_PATH_FORMAT, task.getGuid());
  }

  private String getPendingPath(SingularityTask task) {
    return String.format(PENDING_PATH_FORMAT, task.getGuid());
  }

  public SingularityTask persistRequest(SingularityRequest request) {
    try {
      return persistRequestPrivate(request);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private byte[] getTaskData(SingularityTask task) throws Exception {
    return objectMapper.writeValueAsBytes(task);
  }

  private SingularityTask getTaskFromData(byte[] data) throws Exception {
    return objectMapper.readValue(data, SingularityTask.class);
  }

  private SingularityTask persistRequestPrivate(SingularityRequest request) throws Exception {
    final String guid = UUID.randomUUID().toString();

    final SingularityTask task = new SingularityTask(request, guid);

    final String pendingPath = getPendingPath(task);

    curator.create().creatingParentsIfNeeded().forPath(pendingPath, getTaskData(task));
  
    return task;
  }

  public List<SingularityTask> getPendingTasks() {
    try {
      List<String> taskGuids = curator.getChildren().forPath(PENDING_PATH_ROOT);
      List<SingularityTask> tasks = Lists.newArrayListWithCapacity(taskGuids.size());
      
      for (String taskGuid : taskGuids) {
        SingularityTask request = getTaskFromData(curator.getData().forPath(ZKPaths.makePath(PENDING_PATH_ROOT, taskGuid)));
        tasks.add(request);
      }

      return tasks;
    } catch (NoNodeException nne) {
      return Collections.emptyList();
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  public void launchTask(SingularityTask task) {
    try {
      launchTaskPrivate(task);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }

  private void launchTaskPrivate(SingularityTask task) throws Exception {
    final String pendingPath = getPendingPath(task);
    final String activePath = getActivePath(task);
    
    curator.delete().forPath(pendingPath);
    
    curator.create().creatingParentsIfNeeded().forPath(activePath, getTaskData(task));
  }
  
  private byte[] toBytes(String string) {
    try {
      return string.getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }
  
  private String toString(byte[] bytes) {
    try {
      return new String(bytes, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
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
        maybeMessage = Optional.of(toString(payload));
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
    
  public void recordStatus(String statusUpdate, String taskId, Optional<String> message) {
    byte[] payload = null;
    
    if (message.isPresent()) {
      payload = toBytes(message.get());
    } else {
      payload = toBytes("");
    }
    
    try {
      curator.create().creatingParentsIfNeeded().forPath(getHistoryPath(taskId, statusUpdate), payload);
    } catch (Throwable t) {
      throw Throwables.propagate(t);
    }
  }
  
}
