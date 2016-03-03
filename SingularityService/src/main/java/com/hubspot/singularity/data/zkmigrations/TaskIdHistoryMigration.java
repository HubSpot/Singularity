package com.hubspot.singularity.data.zkmigrations;

import java.util.List;
import java.util.Map;

import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityTask;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskIdHistory;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class TaskIdHistoryMigration extends ZkDataMigration {

  private static final String TASK_HISTORY_ROOT = "/tasks/history";

  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final CuratorFramework curator;
  private final Transcoder<SingularityTaskIdHistory> taskIdHistoryTranscoder;

  @Inject
  public TaskIdHistoryMigration(TaskManager taskManager, RequestManager requestManager, CuratorFramework curator, Transcoder<SingularityTaskIdHistory> taskIdHistoryTranscoder) {
    super(10);
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.curator = curator;
    this.taskIdHistoryTranscoder = taskIdHistoryTranscoder;
  }

  @Override
  public void applyMigration() {
    try {
      if (curator.checkExists().forPath(TASK_HISTORY_ROOT) == null) {
        return;
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    try {
      for (String requestId : requestManager.getAllRequestIds()) {
        List<SingularityTaskId> taskIds = taskManager.getTaskIdsForRequest(requestId);
        Map<SingularityTaskId, SingularityTask> tasks = taskManager.getTasks(taskIds);
        Map<SingularityTaskId, List<SingularityTaskHistoryUpdate>> map = taskManager.getTaskHistoryUpdates(taskIds);


        for (SingularityTaskId taskId : taskIds) {
          List<SingularityTaskHistoryUpdate> historyUpdates = map.get(taskId);
          SingularityTask task = tasks.get(taskId);
          if (task != null) {
            SingularityTaskIdHistory history = SingularityTaskIdHistory.fromTaskIdAndTaskAndUpdates(taskId, task, historyUpdates);
            if (curator.checkExists().forPath(ZKPaths.makePath(TASK_HISTORY_ROOT, requestId, taskId.getId())) != null) {
              curator.setData().forPath(ZKPaths.makePath(TASK_HISTORY_ROOT, requestId, taskId.getId()), taskIdHistoryTranscoder.toBytes(history));
            }
          }
        }
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

}
