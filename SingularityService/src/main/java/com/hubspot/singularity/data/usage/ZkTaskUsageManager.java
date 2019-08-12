package com.hubspot.singularity.data.usage;

import java.util.List;
import java.util.Optional;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.InvalidSingularityTaskIdException;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.CuratorAsyncManager;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class ZkTaskUsageManager extends CuratorAsyncManager implements TaskUsageManager {
  private static final Logger LOG = LoggerFactory.getLogger(ZkTaskUsageManager.class);

  private static final String ROOT_PATH = "/usage";

  private static final String TASK_PATH = ROOT_PATH + "/tasks";

  private static final String USAGE_HISTORY_PATH_KEY = "history";

  private final Transcoder<SingularityTaskUsage> taskUsageTranscoder;

  @Inject
  public ZkTaskUsageManager(CuratorFramework curator,
                            SingularityConfiguration configuration,
                            MetricRegistry metricRegistry,
                            Transcoder<SingularityTaskUsage> taskUsageTranscoder) {
    super(curator, configuration, metricRegistry);
    this.taskUsageTranscoder = taskUsageTranscoder;
  }

  private String getTaskUsagePath(SingularityTaskId taskId) {
    return ZKPaths.makePath(TASK_PATH, taskId.getRequestId(), taskId.getId());
  }

  private String getTaskUsageHistoryPath(SingularityTaskId taskId) {
    return ZKPaths.makePath(getTaskUsagePath(taskId), USAGE_HISTORY_PATH_KEY);
  }

  private String getSpecificTaskUsagePath(SingularityTaskId taskId, long timestamp) {
    return ZKPaths.makePath(getTaskUsageHistoryPath(taskId), Long.toString(timestamp));
  }

  public void deleteTaskUsage(SingularityTaskId taskId) {
    delete(getTaskUsagePath(taskId));
  }

  public void deleteSpecificTaskUsage(SingularityTaskId taskId, long timestamp) {
    delete(getSpecificTaskUsagePath(taskId, timestamp));
  }

  public void saveSpecificTaskUsage(SingularityTaskId taskId, SingularityTaskUsage usage) {
    save(getSpecificTaskUsagePath(taskId, usage.getTimestamp()), usage, taskUsageTranscoder);
  }

  public List<SingularityTaskUsage> getTaskUsage(SingularityTaskId taskId) {
    List<SingularityTaskUsage> children = getAsyncChildren(getTaskUsageHistoryPath(taskId), taskUsageTranscoder);
    children.sort(TASK_USAGE_COMPARATOR_TIMESTAMP_ASC);
    return children;
  }

  @VisibleForTesting
  public int countTasksWithUsage() {
    return getChildren(TASK_PATH).stream()
        .map((requestId) -> checkExists(ZKPaths.makePath(TASK_PATH, requestId)))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .mapToInt(Stat::getNumChildren)
        .sum();
  }

  public void cleanOldUsages(List<SingularityTaskId> activeTaskIds) {

    for (String requestId : getChildren(TASK_PATH)) {
      // clean for inactive tasks
      for (String taskIdString : getChildren(ZKPaths.makePath(TASK_PATH, requestId))) {
        SingularityTaskId taskId;
        try {
          taskId = SingularityTaskId.valueOf(taskIdString);
          if (activeTaskIds.contains(taskId)) {
            // prune old usages for active tasks
            getChildren(getTaskUsageHistoryPath(taskId)).stream()
                .map(Long::parseLong)
                .sorted((t1, t2) -> Long.compare(t2, t1))
                .skip(configuration.getNumUsageToKeep())
                .forEach((timestamp) -> {
                  delete(getSpecificTaskUsagePath(taskId, timestamp));
                });
            continue;
          }
        } catch (InvalidSingularityTaskIdException e) {
          LOG.warn("{} is not a valid task id, will remove task usage from zookeeper", taskIdString);
        }
        SingularityDeleteResult result = delete(ZKPaths.makePath(TASK_PATH, requestId, taskIdString)); // manually constructed in case SingularityTaskId.valueOf fails

        LOG.debug("Deleted obsolete task usage {} - {}", taskIdString, result);
      }
    }
  }
}
