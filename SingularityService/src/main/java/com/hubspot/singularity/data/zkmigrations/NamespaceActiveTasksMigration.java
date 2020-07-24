package com.hubspot.singularity.data.zkmigrations;

import com.google.inject.Inject;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityTaskId;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NamespaceActiveTasksMigration extends ZkDataMigration {
  private static final Logger LOG = LoggerFactory.getLogger(
    NamespaceActiveTasksMigration.class
  );

  private static final String ACTIVE_TASKS_ROOT = "/tasks/active";
  private static final String ACTIVE_STATUSES_ROOT = "/tasks/statuses";

  private final CuratorFramework curatorFramework;

  @Inject
  public NamespaceActiveTasksMigration(CuratorFramework curatorFramework) {
    super(14);
    this.curatorFramework = curatorFramework;
  }

  @Override
  public void applyMigration() {
    try {
      if (curatorFramework.checkExists().forPath(ACTIVE_TASKS_ROOT) != null) {
        List<String> currentActive = curatorFramework
          .getChildren()
          .forPath(ACTIVE_STATUSES_ROOT);
        for (String taskIdString : currentActive) {
          try {
            SingularityTaskId taskId = SingularityTaskId.valueOf(taskIdString);
            String oldPath = ZKPaths.makePath(ACTIVE_STATUSES_ROOT, taskIdString);
            byte[] oldData = curatorFramework.getData().forPath(oldPath);
            String newPath = ZKPaths.makePath(
              ACTIVE_STATUSES_ROOT,
              taskId.getRequestId(),
              taskIdString
            );
            if (curatorFramework.checkExists().forPath(newPath) != null) {
              curatorFramework.setData().forPath(newPath, oldData);
            } else {
              curatorFramework
                .create()
                .creatingParentsIfNeeded()
                .forPath(newPath, oldData);
            }
            curatorFramework.delete().forPath(oldPath);
          } catch (InvalidSingularityTaskIdException e) {
            LOG.warn(
              "Found invalid task id {}. This is likely because the migration did not finish successfully on a previous run. Will continue to migrate additional nodes",
              taskIdString
            );
          }
        }

        curatorFramework.delete().deletingChildrenIfNeeded().forPath(ACTIVE_TASKS_ROOT);
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }
}
