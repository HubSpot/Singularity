package com.hubspot.singularity.data.zkmigrations;

import java.util.Collections;

import javax.inject.Singleton;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityPendingRequest.PendingType;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityPendingTaskId;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.transcoders.StringTranscoder;

@Singleton
public class SingularityPendingTaskIdMigration extends ZkDataMigration {

  private static final Logger LOG = LoggerFactory.getLogger(SingularityPendingTaskIdMigration.class);

  private final CuratorFramework curator;
  private final TaskManager taskManager;

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
      if (curator.checkExists().forPath(TaskManager.PENDING_PATH_ROOT) == null) {
        return;
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    try {
      for (String pendingTaskId : curator.getChildren().forPath(TaskManager.PENDING_PATH_ROOT)) {
        SingularityPendingTaskId newPendingTaskId = createFrom(pendingTaskId, start);
        if (!newPendingTaskId.toString().equals(pendingTaskId)) {
          LOG.info("Migrating {} to {}", pendingTaskId, newPendingTaskId);

          Optional<String> cmdLineArgs = getCmdLineArgs(pendingTaskId);

          taskManager.createPendingTasks(Collections.singletonList(new SingularityPendingTask(newPendingTaskId, cmdLineArgs)));

          curator.delete().forPath(ZKPaths.makePath(TaskManager.PENDING_PATH_ROOT, pendingTaskId));
        }
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private Optional<String> getCmdLineArgs(String pendingTaskId) throws Exception {
    byte[] data = curator.getData().forPath(ZKPaths.makePath(TaskManager.PENDING_PATH_ROOT, pendingTaskId));

    if (data != null && data.length > 0) {
      return Optional.of(StringTranscoder.STRING_TRANSCODER.transcode(data));
    }

    return Optional.absent();
  }

  public SingularityPendingTaskId createFrom(String string, long createdAt) {
    if (Character.isDigit(string.charAt(string.length() - 1))) {
      LOG.warn("Not migrating {} - it appears to be migrated already", string);
      return SingularityPendingTaskId.fromString(string);
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
