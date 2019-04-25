package com.hubspot.singularity.data.usage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.CuratorAsyncManager;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class ZkTaskUsageManager extends CuratorAsyncManager implements TaskUsageManager {

  private static final String ROOT_PATH = "/usage";

  private static final String TASK_PATH = ROOT_PATH + "/tasks";

  private static final String USAGE_HISTORY_PATH_KEY = "history";
  private static final String CURRENT_USAGE_NODE_KEY = "CURRENT";

  private final Transcoder<SingularityTaskUsage> taskUsageTranscoder;
  private final Transcoder<SingularityTaskCurrentUsageWithId> taskCurrentUsageTranscoder;

  @Inject
  public ZkTaskUsageManager(CuratorFramework curator,
                            SingularityConfiguration configuration,
                            MetricRegistry metricRegistry,
                            Transcoder<SingularityTaskUsage> taskUsageTranscoder,
                            Transcoder<SingularityTaskCurrentUsageWithId> taskCurrentUsageTranscoder) {
    super(curator, configuration, metricRegistry);
    this.taskUsageTranscoder = taskUsageTranscoder;
    this.taskCurrentUsageTranscoder = taskCurrentUsageTranscoder;
  }

  private String getTaskUsagePath(String taskId) {
    return ZKPaths.makePath(TASK_PATH, taskId);
  }

  private String getTaskUsageHistoryPath(String taskId) {
    return ZKPaths.makePath(getTaskUsagePath(taskId), USAGE_HISTORY_PATH_KEY);
  }

  private String getSpecificTaskUsagePath(String taskId, double timestamp) {
    return ZKPaths.makePath(getTaskUsageHistoryPath(taskId), Double.toString(timestamp));
  }

  private String getCurrentTaskUsagePath(String taskId) {
    return ZKPaths.makePath(getTaskUsagePath(taskId), CURRENT_USAGE_NODE_KEY);
  }

  public void deleteTaskUsage(String taskId) {
    delete(getTaskUsagePath(taskId));
  }

  public void deleteSpecificTaskUsage(String taskId, double timestamp) {
    delete(getSpecificTaskUsagePath(taskId, timestamp));
  }

  public void saveCurrentTaskUsage(SingularityTaskCurrentUsageWithId usageWithId) {
    set(getCurrentTaskUsagePath(usageWithId.getTaskId().getId()), usageWithId, taskCurrentUsageTranscoder);
  }

  public void saveSpecificTaskUsage(String taskId, SingularityTaskUsage usage) {
    save(getSpecificTaskUsagePath(taskId, usage.getTimestamp()), usage, taskUsageTranscoder);
  }

  public List<SingularityTaskUsage> getTaskUsage(String taskId) {
    List<SingularityTaskUsage> children = getAsyncChildren(getTaskUsageHistoryPath(taskId), taskUsageTranscoder);
    children.sort(TASK_USAGE_COMPARATOR_TIMESTAMP_ASC);
    return children;
  }

  public List<String> getTaskUsagePaths(String taskId) {
    List<String> children = getChildren(getTaskUsageHistoryPath(taskId));
    children.sort(TASK_USAGE_PATH_COMPARATOR_TIMESTAMP_ASC);
    return children;
  }

  public Map<String, SingularityTaskCurrentUsageWithId> getTaskCurrentUsages(List<SingularityTaskId> taskIds) {
    List<String> paths = new ArrayList<>(taskIds.size());
    for (SingularityTaskId taskId : taskIds) {
      paths.add(getCurrentTaskUsagePath(taskId.getId()));
    }

    return getAsyncWithPath("getTaskCurrentUsages", paths, taskCurrentUsageTranscoder);
  }
}
