package com.hubspot.singularity.data.zkmigrations;

import java.util.Collections;
import java.util.Optional;

import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.api.request.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.api.task.SingularityPendingTaskBuilder;
import com.hubspot.singularity.api.task.SingularityPendingTaskId;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.StringTranscoder;
import com.hubspot.singularity.exceptions.InvalidSingularityTaskIdException;

@Singleton
public class SingularityPendingTaskIdMigration extends ZkDataMigration {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityPendingTaskIdMigration.class);

  private final CuratorFramework curator;
  private final TaskManager taskManager;

  private final String PENDING_TASKS_ROOT = "/tasks/scheduled";

  @Inject
  public SingularityPendingTaskIdMigration(CuratorFramework curator, TaskManager taskManager) {
    super(2);
    this.curator = curator;
    this.taskManager = taskManager;
  }

  @Override
  public void applyMigration() {
    final long start = System.currentTimeMillis();

    try {
      if (curator.checkExists().forPath(PENDING_TASKS_ROOT) == null) {
        return;
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    try {
      for (String pendingTaskId : curator.getChildren().forPath(PENDING_TASKS_ROOT)) {
        SingularityPendingTaskId newPendingTaskId = createFrom(pendingTaskId, start);
        if (!newPendingTaskId.toString().equals(pendingTaskId)) {
          LOG.info("Migrating {} to {}", pendingTaskId, newPendingTaskId);

          Optional<String> cmdLineArgs = getCmdLineArgs(pendingTaskId);

          taskManager.savePendingTask(
              new SingularityPendingTaskBuilder()
                  .setPendingTaskId(newPendingTaskId)
                  .setCmdLineArgsList(cmdLineArgs.isPresent() ? Optional.of(Collections.singletonList(cmdLineArgs.get())) : Optional.empty())
                  .build());

          curator.delete().forPath(ZKPaths.makePath(PENDING_TASKS_ROOT, pendingTaskId));
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Optional<String> getCmdLineArgs(String pendingTaskId) throws Exception {
    byte[] data = curator.getData().forPath(ZKPaths.makePath(PENDING_TASKS_ROOT, pendingTaskId));

    if (data != null && data.length > 0) {
      return Optional.of(StringTranscoder.INSTANCE.fromBytes(data));
    }

    return Optional.empty();
  }

  public SingularityPendingTaskId createFrom(String string, long createdAt) {
    if (Character.isDigit(string.charAt(string.length() - 1))) {
      LOG.warn("Not migrating {} - it appears to be migrated already", string);
      return SingularityPendingTaskId.valueOf(string);
    }

    String[] splits = null;

    try {
      splits = JavaUtils.reverseSplit(string, 5, "-");
    } catch (IllegalStateException ise) {
      throw new InvalidSingularityTaskIdException(String.format("PendingTaskId %s was invalid (%s)", string, ise.getMessage()));
    }

    try {
      final String requestId = splits[0];
      final String deployId = splits[1];
      final long nextRunAt = Long.parseLong(splits[2]);
      final int instanceNo = Integer.parseInt(splits[3]);
      final PendingType pendingType = PendingType.valueOf(splits[4]);

      return new SingularityPendingTaskId(requestId, deployId, nextRunAt, instanceNo, pendingType, createdAt);
    } catch (IllegalArgumentException e) {
      throw new InvalidSingularityTaskIdException(String.format("PendingTaskId %s had an invalid parameter (%s)", string, e.getMessage()));
    }
  }


}
