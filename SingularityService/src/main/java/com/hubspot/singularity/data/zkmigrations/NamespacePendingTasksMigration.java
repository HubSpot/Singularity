package com.hubspot.singularity.data.zkmigrations;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityPendingTaskId;

public class NamespacePendingTasksMigration extends ZkDataMigration {
  private static final Logger LOG = LoggerFactory.getLogger(NamespacePendingTasksMigration.class);

  private static final String PENDING_TASK_ROOT = "/tasks/scheduled";

  private final CuratorFramework curatorFramework;

  @Inject
  public NamespacePendingTasksMigration(CuratorFramework curatorFramework) {
    super(13);
    this.curatorFramework = curatorFramework;
  }

  @Override
  public void applyMigration() {
    try {
      if (curatorFramework.checkExists().forPath(PENDING_TASK_ROOT) != null) {
        List<String> currentPendingTasks = curatorFramework.getChildren().forPath(PENDING_TASK_ROOT);
        for (String taskIdString : currentPendingTasks) {
          try {
            SingularityPendingTaskId pendingTaskId = SingularityPendingTaskId.valueOf(taskIdString);
            String oldPath = ZKPaths.makePath(PENDING_TASK_ROOT, taskIdString);
            byte[] oldData = curatorFramework.getData().forPath(oldPath);
            String newPath = ZKPaths.makePath(PENDING_TASK_ROOT, pendingTaskId.getRequestId(), taskIdString);
            if (curatorFramework.checkExists().forPath(newPath) != null) {
              curatorFramework.setData().forPath(newPath, oldData);
            } else {
              curatorFramework.create().creatingParentsIfNeeded().forPath(newPath, oldData);
            }
            curatorFramework.delete().forPath(oldPath);
          } catch (InvalidSingularityTaskIdException e) {
            LOG.warn("Found invalid task id {}, will skip", taskIdString);
          }
        }
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
}
