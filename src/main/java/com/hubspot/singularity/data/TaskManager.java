package com.hubspot.singularity.data;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityRequest;

public class TaskManager {

  private final CuratorFramework curator;

  private final static String HISTORY_PATH_FORMAT = "/history/%s";

  private final static String ACTIVE_PATH_FORMAT = "/tasks/%s";

  private final static String PENDING_PATH_ROOT = "/pending";
  private final static String PENDING_PATH_FORMAT = PENDING_PATH_ROOT + "/%s";

  public static final ObjectMapper MAPPER = new ObjectMapper();

  @Inject
  public TaskManager(CuratorFramework curator) {
    this.curator = curator;
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
    return MAPPER.writeValueAsBytes(task);
  }

  private SingularityTask getTaskFromData(byte[] data) throws Exception {
    return MAPPER.readValue(data, SingularityTask.class);
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
}
