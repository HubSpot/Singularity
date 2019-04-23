package com.hubspot.singularity.data.usage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityTaskCurrentUsage;
import com.hubspot.singularity.SingularityTaskCurrentUsageWithId;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.data.CuratorAsyncManager;
import com.hubspot.singularity.data.transcoders.Transcoder;

@Singleton
public class ZkTaskUsageManager extends CuratorAsyncManager {

  private static final String ROOT_PATH = "/usage";

  private static final String TASK_PATH = ROOT_PATH + "/tasks";

  private static final String USAGE_HISTORY_PATH_KEY = "history";
  private static final String CURRENT_USAGE_NODE_KEY = "CURRENT";

  private final Transcoder<SingularityTaskUsage> taskUsageTranscoder;
  private final Transcoder<SingularityTaskCurrentUsage> taskCurrentUsageTranscoder;

  @Inject
  public ZkTaskUsageManager(CuratorFramework curator,
                            SingularityConfiguration configuration,
                            MetricRegistry metricRegistry,
                            Transcoder<SingularityTaskUsage> taskUsageTranscoder,
                            Transcoder<SingularityTaskCurrentUsage> taskCurrentUsageTranscoder) {
    super(curator, configuration, metricRegistry);
    this.taskUsageTranscoder = taskUsageTranscoder;
    this.taskCurrentUsageTranscoder = taskCurrentUsageTranscoder;
  }

  // /tasks/<taskid>/CURRENT
  private String getTaskIdFromCurrentUsagePath(String path) {
    return path.substring(path.indexOf(TASK_PATH) + TASK_PATH.length() + 1, path.lastIndexOf("/"));
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

  public SingularityDeleteResult deleteTaskUsage(String taskId) {
    return delete(getTaskUsagePath(taskId));
  }

  public SingularityDeleteResult deleteSpecificTaskUsage(String taskId, double timestamp) {
    return delete(getSpecificTaskUsagePath(taskId, timestamp));
  }

  public SingularityCreateResult saveCurrentTaskUsage(String taskId, SingularityTaskCurrentUsage usage) {
    return set(getCurrentTaskUsagePath(taskId), usage, taskCurrentUsageTranscoder);
  }

  public SingularityCreateResult saveSpecificTaskUsage(String taskId, SingularityTaskUsage usage) {
    return save(getSpecificTaskUsagePath(taskId, usage.getTimestamp()), usage, taskUsageTranscoder);
  }

  private static final Comparator<SingularityTaskUsage> TASK_USAGE_COMPARATOR_TIMESTAMP_ASC = Comparator.comparingDouble(SingularityTaskUsage::getTimestamp);

  public List<SingularityTaskUsage> getTaskUsage(String taskId) {
    List<SingularityTaskUsage> children = getAsyncChildren(getTaskUsageHistoryPath(taskId), taskUsageTranscoder);
    children.sort(TASK_USAGE_COMPARATOR_TIMESTAMP_ASC);
    return children;
  }

  private static final Comparator<String> TASK_USAGE_PATH_COMPARATOR_TIMESTAMP_ASC = Comparator.comparingDouble(Double::parseDouble);

  public List<String> getTaskUsagePaths(String taskId) {
    List<String> children = getChildren(getTaskUsageHistoryPath(taskId));
    children.sort(TASK_USAGE_PATH_COMPARATOR_TIMESTAMP_ASC);
    return children;
  }

  public List<SingularityTaskCurrentUsageWithId> getTaskCurrentUsages(List<SingularityTaskId> taskIds) {
    List<String> paths = new ArrayList<>(taskIds.size());
    for (SingularityTaskId taskId : taskIds) {
      paths.add(getCurrentTaskUsagePath(taskId.getId()));
    }

    Map<String, SingularityTaskCurrentUsage> currentTaskUsages = getAsyncWithPath("getTaskCurrentUsages", paths, taskCurrentUsageTranscoder);
    List<SingularityTaskCurrentUsageWithId> currentTaskUsagesWithIds = new ArrayList<>(paths.size());
    for (Entry<String, SingularityTaskCurrentUsage> entry : currentTaskUsages.entrySet()) {
      currentTaskUsagesWithIds.add(new SingularityTaskCurrentUsageWithId(SingularityTaskId.valueOf(getTaskIdFromCurrentUsagePath(entry.getKey())), entry.getValue()));
    }

    return currentTaskUsagesWithIds;
  }

}
